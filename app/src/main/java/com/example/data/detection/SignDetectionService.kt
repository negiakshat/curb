package com.example.data.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.data.model.SignBoundingBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

data class LocalSignCrop(
    val id: String,
    val normalizedBox: SignBoundingBox,
    val ocrText: String,
    val fileUri: String,
    val bitmap: Bitmap
)

data class LocalDetectionResult(
    val signs: List<LocalSignCrop>,
    val totalDetected: Int,
    val rawSummary: String
)

data class InternalSignDetection(
    val id: String,
    val boundingBox: RectF,
    val normalizedBox: SignBoundingBox,
    val confidence: Float,
    val category: String,
    val detectedText: String
)

object SignDetectionService {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val backgroundExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "curb-sign-detector-thread").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }
    }

    fun getAnalysisExecutor(): ExecutorService = backgroundExecutor

    fun loadOrientedBitmapFromUri(context: Context, uri: android.net.Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val exif = android.media.ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            try { inputStream.close() } catch (e: Exception) {}

            val rotationDegrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(decodeStream)
            try { decodeStream.close() } catch (e: Exception) {}

            if (bitmap != null && rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                rotated
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun detectAndCropSigns(
        context: Context,
        bitmap: Bitmap
    ): LocalDetectionResult = withContext(Dispatchers.Default) {
        if (bitmap.isRecycled || bitmap.width < 50 || bitmap.height < 50) {
            return@withContext LocalDetectionResult(emptyList(), 0, "Image not usable.")
        }

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = processImageAsync(inputImage)

        val blocks = visionText?.textBlocks ?: emptyList()
        val clusteredBoxes = if (blocks.isNotEmpty()) {
            clusterTextBlocks(blocks, bitmap.width, bitmap.height)
        } else emptyList()

        val cropsDir = File(context.cacheDir, "sign_crops").apply {
            if (!exists()) mkdirs()
        }

        val signCrops = mutableListOf<LocalSignCrop>()
        val bmpWidth = bitmap.width.toFloat()
        val bmpHeight = bitmap.height.toFloat()

        if (clusteredBoxes.isNotEmpty()) {
            clusteredBoxes.forEachIndexed { index, cluster ->
                val signId = "sign_${index + 1}"
                val rect = cluster.rect

                // Generous padding around bounding box (18% padding to capture full physical sign border)
                val padX = (rect.width() * 0.18f).toInt().coerceAtLeast(24)
                val padY = (rect.height() * 0.18f).toInt().coerceAtLeast(24)

                val cropLeft = (rect.left - padX).coerceIn(0, bitmap.width - 1)
                val cropTop = (rect.top - padY).coerceIn(0, bitmap.height - 1)
                val cropRight = (rect.right + padX).coerceIn(cropLeft + 1, bitmap.width)
                val cropBottom = (rect.bottom + padY).coerceIn(cropTop + 1, bitmap.height)

                val cropWidth = cropRight - cropLeft
                val cropHeight = cropBottom - cropTop

                // Validate crop rectangle
                if (cropWidth >= 30 && cropHeight >= 30 && cropLeft >= 0 && cropTop >= 0 && cropRight <= bitmap.width && cropBottom <= bitmap.height) {
                    try {
                        val croppedBmp = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
                        if (!croppedBmp.isRecycled && croppedBmp.width > 0 && croppedBmp.height > 0) {
                            val cropFile = File(cropsDir, "crop_${System.currentTimeMillis()}_$signId.jpg")
                            FileOutputStream(cropFile).use { out ->
                                croppedBmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }

                            if (cropFile.exists() && cropFile.length() > 0) {
                                val normalizedBox = SignBoundingBox(
                                    id = signId,
                                    left = cropLeft / bmpWidth,
                                    top = cropTop / bmpHeight,
                                    right = cropRight / bmpWidth,
                                    bottom = cropBottom / bmpHeight,
                                    label = determineSignLabel(cluster.text),
                                    ocrText = cluster.text,
                                    confidence = cluster.confidence,
                                    sourceWidth = bmpWidth,
                                    sourceHeight = bmpHeight
                                )

                                signCrops.add(
                                    LocalSignCrop(
                                        id = signId,
                                        normalizedBox = normalizedBox,
                                        ocrText = cluster.text,
                                        fileUri = cropFile.absolutePath,
                                        bitmap = croppedBmp
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore individual crop failure
                    }
                }
            }
        }

        // Fallback: If no text clusters were isolated, create a full source image crop
        if (signCrops.isEmpty() && bitmap.width >= 50 && bitmap.height >= 50) {
            try {
                val cropFile = File(cropsDir, "crop_${System.currentTimeMillis()}_sign_1.jpg")
                FileOutputStream(cropFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                if (cropFile.exists() && cropFile.length() > 0) {
                    val normalizedBox = SignBoundingBox(
                        id = "sign_1",
                        left = 0f,
                        top = 0f,
                        right = 1f,
                        bottom = 1f,
                        label = "PARKING SIGN",
                        ocrText = "PARKING SIGN",
                        confidence = 0.90f,
                        sourceWidth = bmpWidth,
                        sourceHeight = bmpHeight
                    )
                    signCrops.add(
                        LocalSignCrop(
                            id = "sign_1",
                            normalizedBox = normalizedBox,
                            ocrText = "PARKING SIGN",
                            fileUri = cropFile.absolutePath,
                            bitmap = bitmap
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore fallback creation failure
            }
        }

        val summary = if (signCrops.isNotEmpty()) {
            "Found ${signCrops.size} sign(s): " + signCrops.joinToString("; ") { "[${it.normalizedBox.label}]: \"${it.ocrText.replace("\n", " ").take(40)}\"" }
        } else {
            "Sign text was not clearly resolved."
        }

        LocalDetectionResult(
            signs = signCrops,
            totalDetected = signCrops.size,
            rawSummary = summary
        )
    }

    /**
     * Crops regions from the captured bitmap using normalized bounding boxes (e.g. from live camera detection)
     */
    fun cropSignsFromBoxes(
        context: Context,
        bitmap: Bitmap,
        boxes: List<SignBoundingBox>
    ): List<LocalSignCrop> {
        if (bitmap.isRecycled || bitmap.width < 50 || bitmap.height < 50 || boxes.isEmpty()) {
            return emptyList()
        }

        val cropsDir = File(context.cacheDir, "sign_crops").apply {
            if (!exists()) mkdirs()
        }

        val crops = mutableListOf<LocalSignCrop>()
        val bmpWidth = bitmap.width.toFloat()
        val bmpHeight = bitmap.height.toFloat()

        boxes.forEachIndexed { index, box ->
            val signId = "sign_${index + 1}"
            val rawLeft = (box.left * bmpWidth).toInt()
            val rawTop = (box.top * bmpHeight).toInt()
            val rawRight = (box.right * bmpWidth).toInt()
            val rawBottom = (box.bottom * bmpHeight).toInt()

            val padX = ((rawRight - rawLeft) * 0.12f).toInt().coerceAtLeast(14)
            val padY = ((rawBottom - rawTop) * 0.12f).toInt().coerceAtLeast(14)

            val cropLeft = (rawLeft - padX).coerceIn(0, bitmap.width - 1)
            val cropTop = (rawTop - padY).coerceIn(0, bitmap.height - 1)
            val cropRight = (rawRight + padX).coerceIn(cropLeft + 1, bitmap.width)
            val cropBottom = (rawBottom + padY).coerceIn(cropTop + 1, bitmap.height)

            val cropWidth = cropRight - cropLeft
            val cropHeight = cropBottom - cropTop

            if (cropWidth >= 30 && cropHeight >= 30) {
                try {
                    val croppedBmp = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
                    if (!croppedBmp.isRecycled && croppedBmp.width > 0 && croppedBmp.height > 0) {
                        val cropFile = File(cropsDir, "crop_live_${System.currentTimeMillis()}_$signId.jpg")
                        FileOutputStream(cropFile).use { out ->
                            croppedBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }

                        if (cropFile.exists() && cropFile.length() > 0) {
                            crops.add(
                                LocalSignCrop(
                                    id = signId,
                                    normalizedBox = box.copy(
                                        left = cropLeft / bmpWidth,
                                        top = cropTop / bmpHeight,
                                        right = cropRight / bmpWidth,
                                        bottom = cropBottom / bmpHeight,
                                        sourceWidth = bmpWidth,
                                        sourceHeight = bmpHeight
                                    ),
                                    ocrText = box.ocrText,
                                    fileUri = cropFile.absolutePath,
                                    bitmap = croppedBmp
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore crop failure
                }
            }
        }
        return crops
    }

    /**
     * Generates a high-quality physical sign plate image for sample scenarios,
     * guaranteeing that demo scans always have authentic, non-empty sign images.
     */
    fun getOrCreateSampleSignCrop(
        context: Context,
        signKey: String,
        title: String,
        subtitle: String,
        isRestricted: Boolean
    ): String {
        val cropsDir = File(context.cacheDir, "sample_sign_plates").apply {
            if (!exists()) mkdirs()
        }
        val file = File(cropsDir, "sample_$signKey.jpg")
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        try {
            val width = 600
            val height = 480
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // White retroreflective sign plate background
            val bgPaint = Paint().apply {
                color = AndroidColor.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val primaryColor = if (isRestricted) AndroidColor.rgb(204, 0, 0) else AndroidColor.rgb(0, 138, 56)

            // MUTCD Sign Outer Border
            val borderPaint = Paint().apply {
                color = primaryColor
                style = Paint.Style.STROKE
                strokeWidth = 14f
                isAntiAlias = true
            }
            val cornerRadius = 28f
            canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), cornerRadius, cornerRadius, borderPaint)

            // Header Section
            val titlePaint = Paint().apply {
                color = primaryColor
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(title.uppercase(), width / 2f, 110f, titlePaint)

            // Divider Line
            val linePaint = Paint().apply {
                color = primaryColor
                strokeWidth = 6f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(40f, 150f, width - 40f, 150f, linePaint)

            // Subtitle / Hours Lines
            val subPaint = Paint().apply {
                color = primaryColor
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val lines = subtitle.split("•", "/", "\n")
            var textY = 220f
            for (line in lines) {
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim().uppercase(), width / 2f, textY, subPaint)
                    textY += 56f
                }
            }

            // Standard municipal arrows / indicator
            val arrowPaint = Paint().apply {
                color = primaryColor
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("◀ ━━━ ▶", width / 2f, height - 60f, arrowPaint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            return file.absolutePath
        } catch (e: Exception) {
            return ""
        }
    }

    private suspend fun processImageAsync(inputImage: InputImage): Text? =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }

    private data class TextCluster(
        val rect: Rect,
        val text: String,
        val confidence: Float
    )

    private fun clusterTextBlocks(
        blocks: List<Text.TextBlock>,
        imageWidth: Int,
        imageHeight: Int
    ): List<TextCluster> {
        val clusters = mutableListOf<MutableList<Text.TextBlock>>()

        // Sort blocks primarily top-to-bottom
        val sortedBlocks = blocks.filter { it.boundingBox != null }
            .sortedBy { it.boundingBox!!.top }

        for (block in sortedBlocks) {
            val box = block.boundingBox ?: continue
            var merged = false

            for (cluster in clusters) {
                // Check if this block belongs to the existing sign cluster
                val clusterBox = getBoundingBoxForCluster(cluster)

                // Vertical distance check: signs on poles usually have blocks within 40-70% of sign height
                val verticalGap = box.top - clusterBox.bottom
                val horizontalOverlap = maxOf(0, minOf(box.right, clusterBox.right) - maxOf(box.left, clusterBox.left))
                val minWidth = minOf(box.width(), clusterBox.width())

                val isCloseVertically = verticalGap >= -20 && verticalGap < (clusterBox.height() * 0.7f).coerceAtLeast(60f).toInt()
                val hasHorizontalAlignment = horizontalOverlap > (minWidth * 0.3f)

                if (isCloseVertically && hasHorizontalAlignment) {
                    cluster.add(block)
                    merged = true
                    break
                }
            }

            if (!merged) {
                clusters.add(mutableListOf(block))
            }
        }

        return clusters.map { cluster ->
            val clusterRect = getBoundingBoxForCluster(cluster)
            val combinedText = cluster.joinToString("\n") { it.text }
            val lineConfidences = cluster.flatMap { it.lines }
                .map { it.confidence }
                .filter { it > 0f }
            val avgConfidence = if (lineConfidences.isNotEmpty()) {
                lineConfidences.average().toFloat()
            } else {
                0.80f
            }
            TextCluster(clusterRect, combinedText, avgConfidence)
        }.filter { it.text.isNotBlank() }
        .sortedBy { it.rect.top }
    }

    private fun getBoundingBoxForCluster(cluster: List<Text.TextBlock>): Rect {
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE

        for (block in cluster) {
            val box = block.boundingBox ?: continue
            left = minOf(left, box.left)
            top = minOf(top, box.top)
            right = maxOf(right, box.right)
            bottom = maxOf(bottom, box.bottom)
        }

        return Rect(left, top, right, bottom)
    }

    fun determineSignCategory(ocrText: String): String {
        val upper = ocrText.uppercase()
        return when {
            upper.contains("TOW") || upper.contains("CLEAN") || upper.contains("SWEEP") || upper.contains("NO STOP") -> "STREET_RESTRICTION"
            upper.contains("NO PARK") || upper.contains("NO STAND") -> "NO_PARKING"
            upper.contains("HOUR") || upper.contains("HR") || upper.contains("METER") || upper.contains("PAY") -> "PARKING_LIMIT"
            upper.contains("PERMIT") || upper.contains("RESIDENT") || upper.contains("ZONE") -> "PERMIT_ZONE"
            upper.contains("PASSENGER") || upper.contains("LOADING") || upper.contains("COMMERCIAL") -> "LOADING_ZONE"
            else -> "STREET_SIGN"
        }
    }

    fun determineSignLabel(ocrText: String): String {
        val upper = ocrText.uppercase()
        return when {
            upper.contains("TOW") || upper.contains("CLEAN") || upper.contains("SWEEP") || upper.contains("NO STOP") -> "STREET RESTRICTION"
            upper.contains("NO PARK") || upper.contains("NO STAND") -> "NO PARKING"
            upper.contains("HOUR") || upper.contains("HR") || upper.contains("METER") || upper.contains("PAY") -> "PARKING LIMIT"
            upper.contains("PERMIT") || upper.contains("RESIDENT") || upper.contains("ZONE") -> "PERMIT ZONE"
            upper.contains("PASSENGER") || upper.contains("LOADING") || upper.contains("COMMERCIAL") -> "LOADING ZONE"
            else -> "PARKING SIGN"
        }
    }

    fun processVisionText(
        visionText: Text,
        imgWidth: Float,
        imgHeight: Float
    ): List<InternalSignDetection> {
        val blocks = visionText.textBlocks
        if (blocks.isEmpty() || imgWidth <= 0f || imgHeight <= 0f) {
            return emptyList()
        }

        val sortedBlocks = blocks.filter { it.boundingBox != null }
            .sortedBy { it.boundingBox!!.top }

        if (sortedBlocks.isEmpty()) {
            return emptyList()
        }

        // Cluster blocks to group multiple lines belonging to distinct signs on a post
        val clusters = mutableListOf<MutableList<Text.TextBlock>>()
        for (block in sortedBlocks) {
            val box = block.boundingBox ?: continue
            var merged = false
            for (c in clusters) {
                val cBox = getBoundingBoxForCluster(c)
                val verticalGap = box.top - cBox.bottom
                val hOverlap = maxOf(0, minOf(box.right, cBox.right) - maxOf(box.left, cBox.left))
                if (verticalGap in -20..((cBox.height() * 0.7f).toInt().coerceAtLeast(40)) &&
                    hOverlap > minOf(box.width(), cBox.width()) * 0.3f
                ) {
                    c.add(block)
                    merged = true
                    break
                }
            }
            if (!merged) {
                clusters.add(mutableListOf(block))
            }
        }

        val detections = mutableListOf<InternalSignDetection>()
        var signIndex = 1

        for (c in clusters) {
            val rect = getBoundingBoxForCluster(c)
            val combinedText = c.joinToString(" ") { it.text }.trim()
            if (combinedText.isBlank()) continue

            // Extract real confidence from model text lines
            val lineConfidences = c.flatMap { it.lines }
                .map { it.confidence }
                .filter { it > 0f }

            val modelConfidence = if (lineConfidences.isNotEmpty()) {
                lineConfidences.average().toFloat()
            } else {
                0.80f
            }

            val category = determineSignCategory(combinedText)
            val label = determineSignLabel(combinedText)

            // Calculate normalized coordinate space [0.0 to 1.0]
            val leftNorm = (rect.left / imgWidth).coerceIn(0f, 1f)
            val topNorm = (rect.top / imgHeight).coerceIn(0f, 1f)
            val rightNorm = (rect.right / imgWidth).coerceIn(leftNorm, 1f)
            val bottomNorm = (rect.bottom / imgHeight).coerceIn(topNorm, 1f)

            // Avoid noise: filter out tiny pixel fragments
            if ((rightNorm - leftNorm) >= 0.06f && (bottomNorm - topNorm) >= 0.04f) {
                val normalizedBox = SignBoundingBox(
                    id = "sign_$signIndex",
                    left = leftNorm,
                    top = topNorm,
                    right = rightNorm,
                    bottom = bottomNorm,
                    label = label,
                    ocrText = combinedText.take(60),
                    confidence = modelConfidence,
                    sourceWidth = imgWidth,
                    sourceHeight = imgHeight
                )

                detections.add(
                    InternalSignDetection(
                        id = "sign_$signIndex",
                        boundingBox = RectF(
                            rect.left.toFloat(),
                            rect.top.toFloat(),
                            rect.right.toFloat(),
                            rect.bottom.toFloat()
                        ),
                        normalizedBox = normalizedBox,
                        confidence = modelConfidence,
                        category = category,
                        detectedText = combinedText
                    )
                )
                signIndex++
            }
        }

        return detections
    }

    fun createLiveAnalyzer(
        onSignsDetected: (List<SignBoundingBox>) -> Unit
    ): ImageAnalysis.Analyzer {
        return LiveSignAnalyzer(onSignsDetected)
    }

    private class LiveSignAnalyzer(
        private val onSignsDetected: (List<SignBoundingBox>) -> Unit
    ) : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L
        private val isProcessing = AtomicBoolean(false)
        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            // Drop frames immediately if previous frame inference is still active
            if (!isProcessing.compareAndSet(false, true)) {
                imageProxy.close()
                return
            }

            val currentTimestamp = System.currentTimeMillis()
            // Throttle to 4-5 FPS (every 220ms) to ensure smooth camera preview and prevent battery drain
            if (currentTimestamp - lastAnalyzedTimestamp < 220) {
                imageProxy.close()
                isProcessing.set(false)
                return
            }
            lastAnalyzedTimestamp = currentTimestamp

            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                isProcessing.set(false)
                return
            }

            try {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                // Calculate effective dimensions based on rotation
                val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                val imgWidth = if (isRotated) imageProxy.height.toFloat() else imageProxy.width.toFloat()
                val imgHeight = if (isRotated) imageProxy.width.toFloat() else imageProxy.height.toFloat()

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        try {
                            val internalDetections = processVisionText(visionText, imgWidth, imgHeight)
                            onSignsDetected(internalDetections.map { it.normalizedBox })
                        } catch (e: Throwable) {
                            onSignsDetected(emptyList())
                        } finally {
                            imageProxy.close()
                            isProcessing.set(false)
                        }
                    }
                    .addOnFailureListener {
                        try {
                            onSignsDetected(emptyList())
                        } finally {
                            imageProxy.close()
                            isProcessing.set(false)
                        }
                    }
            } catch (e: Throwable) {
                imageProxy.close()
                isProcessing.set(false)
            }
        }
    }
}
