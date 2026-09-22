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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.detection.SignDetectionService
import com.example.data.local.ScanUsageInfo
import com.example.data.model.SampleSignPreset
import com.example.data.model.SignBoundingBox
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import java.nio.ByteBuffer

// Success accent that stays legible on dark, translucent camera overlays
private val ScanSuccessOnDark = Color(0xFF6BDF8B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    isProcessing: Boolean,
    processingStatusText: String,
    scanError: String? = null,
    onClearScanError: () -> Unit = {},
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
    // CRITICAL ISSUE 9: Surface camera bind failure instead of silently swallowing
    var cameraError by remember { mutableStateOf<String?>(null) }
    // CRITICAL ISSUE 8: Camera lifecycle cleanup — track the camera provider for disposal
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

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
                val bitmap = SignDetectionService.loadOrientedBitmapFromUri(context, uri)
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
                            cameraProviderRef = cameraProvider
                        } catch (e: Exception) {
                            // CRITICAL ISSUE 9: Surface camera bind failure
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                cameraError = "Camera initialization failed. Please grant camera permission and try again."
                            }
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // TRUTHFUL CAMERA UNAVAILABLE STATE — never claim the viewfinder is active
            ScanCameraUnavailable()
        }

        // CRITICAL ISSUE 8: Camera lifecycle cleanup — unbind camera when leaving composition
        DisposableEffect(Unit) {
            onDispose {
                try {
                    activeCamera = null
                    cameraProviderRef?.unbindAll()
                } catch (_: Exception) {}
            }
        }

        // SUBTLE STATIC SIGN FRAMING GUIDE (internal ML boxes are never drawn)
        if (hasCameraPermission) {
            ScanFramingGuide(
                isSignDetected = liveDetectedBoxes.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-24).dp)
            )
        }

        // MINIMAL FLOATING TOP HUD
        ScanTopHud(
            isPro = isPro,
            usageInfo = usageInfo,
            onBack = onBack,
            onHelp = { showHelpSheet = true }
        )

        // FLOATING BOTTOM CAMERA CONTROLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScanMicroCaption(isSignDetected = liveDetectedBoxes.isNotEmpty())

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Photo picker button — first-class alternative capture path
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CurbWhite.copy(alpha = 0.22f))
                        .border(1.dp, CurbWhite.copy(alpha = 0.28f), CircleShape)
                        .testTag("gallery_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick photo from gallery",
                        tint = CurbWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Large Shutter Button with animated detection feedback
                val isSignDetected = liveDetectedBoxes.isNotEmpty()

                val ringColor by animateColorAsState(
                    targetValue = if (isSignDetected) CurbSuccess else BentoPrimaryDark,
                    animationSpec = spring(),
                    label = "shutterRingColor"
                )
                val shutterScale by animateFloatAsState(
                    targetValue = if (isSignDetected) 1.05f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = 300f
                    ),
                    label = "shutterScale"
                )
                val ringStrokeDp by animateDpAsState(
                    targetValue = if (isSignDetected) 5.dp else 3.dp,
                    animationSpec = spring(),
                    label = "shutterRingStroke"
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = shutterScale
                            scaleY = shutterScale
                        }
                        .shadow(10.dp, CircleShape)
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
                            .size(64.dp)
                            .border(
                                BorderStroke(ringStrokeDp, ringColor),
                                CircleShape
                            )
                    )
                }

                // Symmetrical Flashlight Toggle
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (flashEnabled) BentoPeach else CurbWhite.copy(alpha = 0.22f)
                        )
                        .border(
                            1.dp,
                            if (flashEnabled) BentoPeach else CurbWhite.copy(alpha = 0.28f),
                            CircleShape
                        )
                        .testTag("flash_toggle_button")
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (flashEnabled) BentoPrimaryDark else CurbWhite,
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
                    .background(CurbBlack.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(RadiusHero),
                    color = BentoWhite,
                    border = BorderStroke(1.dp, BentoBorder),
                    shadowElevation = 14.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BentoPrimary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(46.dp)
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        AnimatedContent(
                            targetState = processingStatusText,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "processingStatusTextAnimation"
                        ) { targetText ->
                            Text(
                                text = targetText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Checking parking regulations…",
                            fontSize = 13.sp,
                            color = BentoTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // CAMERA ERROR BANNER (ISSUE 9)
        AnimatedVisibility(
            visible = cameraError != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp, start = 16.dp, end = 16.dp)
        ) {
            ScanAlert(
                message = cameraError ?: "",
                onDismiss = { cameraError = null }
            )
        }

        // SCAN ERROR BANNER
        AnimatedVisibility(
            visible = scanError != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp, start = 16.dp, end = 16.dp)
        ) {
            ScanAlert(
                message = scanError ?: "",
                onDismiss = onClearScanError
            )
        }
    }

    // SCAN HELP BOTTOM SHEET
    if (showHelpSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHelpSheet = false },
            sheetState = sheetState,
            containerColor = BentoWhite,
            shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "How Curb Works",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Three steps from viewfinder to a clear parking verdict.",
                    fontSize = 13.sp,
                    color = BentoTextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                HelpTipRow(
                    number = "1",
                    title = "Target parking signs",
                    desc = "Curb highlights signs in your camera view to isolate each restriction."
                )
                Spacer(modifier = Modifier.height(16.dp))
                HelpTipRow(
                    number = "2",
                    title = "Read posted rules",
                    desc = "Each sign plate is analyzed against the current day and time to check if parking is allowed."
                )
                Spacer(modifier = Modifier.height(16.dp))
                HelpTipRow(
                    number = "3",
                    title = "Get a clear verdict",
                    desc = "Tow-away hours, street cleaning schedules, and meter limits on the post are resolved into one clear answer."
                )

                Spacer(modifier = Modifier.height(26.dp))

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

/**
 * Compact floating Bento header. Back / identity / quota / help only — the camera stays dominant.
 */
@Composable
private fun ScanTopHud(
    isPro: Boolean,
    usageInfo: ScanUsageInfo,
    onBack: () -> Unit,
    onHelp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .shadow(4.dp, CircleShape)
                .background(BentoWhite.copy(alpha = 0.88f), CircleShape)
                .border(1.dp, BentoBorder.copy(alpha = 0.8f), CircleShape)
                .testTag("scan_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close",
                tint = BentoPrimaryDark,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CURB",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = CurbWhite
            )
            Text(
                text = "SCAN",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                color = CurbWhite.copy(alpha = 0.72f)
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScanQuotaIndicator(usageInfo = usageInfo, isPro = isPro)

            IconButton(
                onClick = onHelp,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, CircleShape)
                    .background(BentoWhite.copy(alpha = 0.88f), CircleShape)
                    .border(1.dp, BentoBorder.copy(alpha = 0.8f), CircleShape)
                    .testTag("scan_help_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Scan Help",
                    tint = BentoPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Truthful scan quota visualization driven by the real [ScanUsageInfo].
 * Free users get a ring with their remaining credits; Pro is a badge, never a fake number.
 */
@Composable
private fun ScanQuotaIndicator(usageInfo: ScanUsageInfo, isPro: Boolean) {
    if (isPro) {
        Surface(
            shape = RoundedCornerShape(RadiusChip),
            color = BentoWhite.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.8f)),
            shadowElevation = 4.dp,
            modifier = Modifier.testTag("scan_usage_indicator")
        ) {
            Text(
                text = "PRO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = BentoPrimaryDark,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    } else {
        val remaining = usageInfo.scansRemaining
        val limitReached = usageInfo.isLimitReached
        val remainingFraction =
            if (usageInfo.monthlyLimit > 0) remaining.toFloat() / usageInfo.monthlyLimit.toFloat() else 0f
        val ringColor = if (limitReached) CurbError else CurbSuccess

        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(4.dp, CircleShape)
                .background(BentoWhite.copy(alpha = 0.88f), CircleShape)
                .border(1.dp, BentoBorder.copy(alpha = 0.8f), CircleShape)
                .semantics {
                    contentDescription = if (limitReached) {
                        "No scans remaining this month"
                    } else {
                        "$remaining scans remaining"
                    }
                }
                .testTag("scan_usage_indicator"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                drawArc(
                    color = BentoBorder,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * remainingFraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Text(
                text = "$remaining",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BentoPrimaryDark
            )
        }
    }
}

/**
 * Static four-corner framing guide. Never draws raw ML bounding boxes; it only brightens
 * when the real live detection list is non-empty.
 */
@Composable
private fun ScanFramingGuide(
    isSignDetected: Boolean,
    modifier: Modifier = Modifier
) {
    val guideAlpha by animateFloatAsState(
        targetValue = if (isSignDetected) 0.95f else 0.4f,
        animationSpec = spring(),
        label = "guideAlpha"
    )
    val guideColor = if (isSignDetected) BentoPeach else CurbWhite

    Box(
        modifier = modifier.size(width = 240.dp, height = 168.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bracket = 30.dp.toPx()
            val strokeWidth = 2.5.dp.toPx()
            val color = guideColor.copy(alpha = guideAlpha)
            val right = size.width
            val bottom = size.height

            // Top-left
            drawLine(color, Offset(0f, 0f), Offset(bracket, 0f), strokeWidth, StrokeCap.Round)
            drawLine(color, Offset(0f, 0f), Offset(0f, bracket), strokeWidth, StrokeCap.Round)
            // Top-right
            drawLine(color, Offset(right, 0f), Offset(right - bracket, 0f), strokeWidth, StrokeCap.Round)
            drawLine(color, Offset(right, 0f), Offset(right, bracket), strokeWidth, StrokeCap.Round)
            // Bottom-left
            drawLine(color, Offset(0f, bottom), Offset(bracket, bottom), strokeWidth, StrokeCap.Round)
            drawLine(color, Offset(0f, bottom), Offset(0f, bottom - bracket), strokeWidth, StrokeCap.Round)
            // Bottom-right
            drawLine(color, Offset(right, bottom), Offset(right - bracket, bottom), strokeWidth, StrokeCap.Round)
            drawLine(color, Offset(right, bottom), Offset(right, bottom - bracket), strokeWidth, StrokeCap.Round)
        }

        Text(
            text = "PARKING SIGN",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.5.sp,
            color = CurbWhite.copy(alpha = guideAlpha * 0.8f)
        )
    }
}

/**
 * Tiny contextual instruction driven by the real live detection result.
 */
@Composable
private fun ScanMicroCaption(isSignDetected: Boolean) {
    val dotColor by animateColorAsState(
        targetValue = if (isSignDetected) ScanSuccessOnDark else CurbWhite.copy(alpha = 0.5f),
        animationSpec = spring(),
        label = "captionDotColor"
    )

    Surface(
        shape = RoundedCornerShape(RadiusChip),
        color = CurbBlack.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            if (isSignDetected) ScanSuccessOnDark.copy(alpha = 0.55f) else CurbWhite.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )

            AnimatedContent(
                targetState = isSignDetected,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "scanCaptionAnimation"
            ) { detected ->
                Text(
                    text = if (detected) "Sign detected — tap to scan" else "Point at a parking sign",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (detected) ScanSuccessOnDark else CurbWhite.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Compact Bento error alert used for both camera and scan failures.
 */
@Composable
private fun ScanAlert(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(RadiusNested),
        color = BentoWhite,
        border = BorderStroke(1.dp, CurbError.copy(alpha = 0.35f)),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(CurbErrorContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CurbError,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = message,
                color = BentoTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Truthful camera-permission state. Camera authorization is requested elsewhere in the app,
 * so this screen explains the state and points at the working gallery path instead of
 * pretending the viewfinder is running.
 */
@Composable
private fun ScanCameraUnavailable() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(BentoWhite.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NoPhotography,
                    contentDescription = null,
                    tint = CurbWhite,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Camera access needed",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CurbWhite,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Allow camera access to scan parking signs.",
                fontSize = 14.sp,
                color = CurbWhite.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "You can still scan a photo from your gallery below.",
                fontSize = 12.sp,
                color = CurbWhite.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
                .background(BentoSand, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = BentoPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = BentoTextSecondary
            )
        }
    }
}
