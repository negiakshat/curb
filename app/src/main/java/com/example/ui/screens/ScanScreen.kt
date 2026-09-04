package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.detection.SignDetectionService
import com.example.data.local.ScanUsageInfo
import com.example.data.model.SampleSignPreset
import com.example.data.model.SignBoundingBox
import com.example.data.remote.GeminiService
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbSecondaryButton
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    isProcessing: Boolean,
    processingStatusText: String,
    usageInfo: ScanUsageInfo = ScanUsageInfo(0),
    isPro: Boolean = false,
    onCaptureImage: (Bitmap?, List<SignBoundingBox>) -> Unit,
    onPresetSelected: (SampleSignPreset) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showHelpSheet by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }

    val hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var liveDetectedBoxes by remember { mutableStateOf<List<SignBoundingBox>>(emptyList()) }

    // Synchronize physical torch with state
    LaunchedEffect(flashEnabled, activeCamera) {
        try {
            activeCamera?.cameraControl?.enableTorch(flashEnabled)
        } catch (e: Exception) {
            // Flash not supported or permission issue
        }
    }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                onCaptureImage(bitmap, emptyList())
            } catch (e: Exception) {
                onCaptureImage(null, emptyList())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBlack)
            .testTag("scan_screen")
    ) {
        // REAL FULL-SCREEN CAMERA VIEWFINDER WITH REAL-TIME ON-DEVICE SIGN ANALYSIS
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            // Real on-device ML Kit image analyzer
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(
                                SignDetectionService.getAnalysisExecutor(),
                                SignDetectionService.createLiveAnalyzer { boxes ->
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        liveDetectedBoxes = boxes
                                    }
                                }
                            )

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            val cameraInstance = cameraProvider.bindToLifecycle(
                                (ctx as androidx.lifecycle.LifecycleOwner),
                                cameraSelector,
                                preview,
                                capture,
                                imageAnalysis
                            )
                            activeCamera = cameraInstance
                        } catch (e: Exception) {
                            // Camera bind fallback
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Camera permission fallback placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = CurbWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sign Viewfinder Active",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Point your camera directly at posted parking signs to scan.",
                        fontSize = 14.sp,
                        color = CurbWhite.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // REAL ON-DEVICE BOUNDING BOX OVERLAY (Clean white outline around real detected signs)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Draw clean white outline around each real detected sign
            liveDetectedBoxes.forEach { box ->
                val left = box.left * w
                val top = box.top * h
                val right = box.right * w
                val bottom = box.bottom * h
                val boxWidth = right - left
                val boxHeight = bottom - top

                if (boxWidth > 10 && boxHeight > 10) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        // TOP CONTROLS & HUD STATUS BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(CurbBlack.copy(alpha = 0.6f), CircleShape)
                        .testTag("scan_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = CurbWhite
                    )
                }

                // Usage quota chip
                Surface(
                    shape = RoundedCornerShape(RadiusChip),
                    color = CurbBlack.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isPro || !usageInfo.isLimitReached) CurbSuccess else Color(0xFFFF5252),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isPro) "Curb Pro • Unlimited" else usageInfo.displayText,
                            color = CurbWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = { showHelpSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(CurbBlack.copy(alpha = 0.6f), CircleShape)
                        .testTag("scan_help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Scan Help",
                        tint = CurbWhite
                    )
                }
            }
        }

        // BOTTOM CAPTURE CONTROLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Shutter Button & Symmetrical Gallery / Flash Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Photo picker button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .background(CurbWhite.copy(alpha = 0.2f), CircleShape)
                        .testTag("gallery_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick photo",
                        tint = CurbWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Large Shutter Button with tactile lock ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CurbWhite)
                        .clickable {
                            val capture = imageCapture
                            val capturedBoxesSnapshot = liveDetectedBoxes.toList()
                            if (capture != null) {
                                capture.takePicture(
                                    SignDetectionService.getAnalysisExecutor(),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            try {
                                                val rotationDegrees = image.imageInfo.rotationDegrees
                                                val buffer: ByteBuffer = image.planes[0].buffer
                                                val bytes = ByteArray(buffer.remaining())
                                                buffer.get(bytes)
                                                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                if (bitmap != null && rotationDegrees != 0) {
                                                    val matrix = Matrix()
                                                    matrix.postRotate(rotationDegrees.toFloat())
                                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                                }
                                                android.util.Log.d(
                                                    "CurbPipeline",
                                                    "Camera picture taken: bmp=${bitmap?.width}x${bitmap?.height}, liveBoxes=${capturedBoxesSnapshot.size}"
                                                )
                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                    onCaptureImage(bitmap, capturedBoxesSnapshot)
                                                }
                                            } catch (e: Throwable) {
                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                    onCaptureImage(null, emptyList())
                                                }
                                            } finally {
                                                image.close()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                onCaptureImage(null, emptyList())
                                            }
                                        }
                                    }
                                )
                            } else {
                                onCaptureImage(null, emptyList())
                            }
                        }
                        .testTag("shutter_capture_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .border(
                                BorderStroke(
                                    3.dp,
                                    if (liveDetectedBoxes.isNotEmpty()) Color.White else CurbBlack
                                ),
                                CircleShape
                            )
                    )
                }

                // Symmetrical Flashlight Toggle
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (flashEnabled) BentoPeach else CurbWhite.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .testTag("flash_toggle_button")
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (flashEnabled) BentoPrimary else CurbWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // REAL HYBRID PROCESSING MODAL OVERLAY
        AnimatedVisibility(
            visible = isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CurbBlack.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = BentoPeach,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(52.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = processingStatusText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbWhite,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Checking parking regulations…",
                        fontSize = 13.sp,
                        color = CurbWhite.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // SCAN HELP BOTTOM SHEET
    if (showHelpSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHelpSheet = false },
            sheetState = sheetState,
            containerColor = CurbSurface,
            shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "How Curb Works",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                HelpTipRow(
                    number = "1",
                    title = "Target parking signs",
                    desc = "Curb highlights signs in your camera view to isolate each restriction."
                )
                Spacer(modifier = Modifier.height(14.dp))
                HelpTipRow(
                    number = "2",
                    title = "Read posted rules",
                    desc = "Each sign plate is analyzed against the current day and time to check if parking is allowed."
                )
                Spacer(modifier = Modifier.height(14.dp))
                HelpTipRow(
                    number = "3",
                    title = "Get a clear verdict",
                    desc = "Tow-away hours, street cleaning schedules, and meter limits on the post are resolved into one clear answer."
                )

                Spacer(modifier = Modifier.height(24.dp))

                CurbPrimaryButton(
                    text = "Got it",
                    onClick = { showHelpSheet = false },
                    testTag = "help_got_it_button"
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HelpTipRow(number: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(CurbBlack, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = CurbWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = CurbOnSurfaceVariant
            )
        }
    }
}
