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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import com.example.ui.theme.CurbWhite
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
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    var frozenPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPreviewFrozen by remember { mutableStateOf(false) }

    LaunchedEffect(isProcessing, scanError) {
        if (!isProcessing && scanError == null) {
            frozenPreviewBitmap = null
            isPreviewFrozen = false
        }
    }

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
        // 1. REAL FULL-SCREEN CAMERA VIEWFINDER WITH REAL-TIME ON-DEVICE SIGN ANALYSIS
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                    }
                    previewViewRef = previewView
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
            ScanCameraUnavailable()
        }

        // Camera lifecycle cleanup — unbind camera when leaving composition
        DisposableEffect(Unit) {
            onDispose {
                frozenPreviewBitmap = null
                isPreviewFrozen = false
                try {
                    activeCamera = null
                    cameraProviderRef?.unbindAll()
                } catch (_: Exception) {}
            }
        }

        // 2. FROZEN PREVIEW OVERLAY AT SHUTTER TAP MOMENT
        if (isPreviewFrozen && frozenPreviewBitmap != null) {
            Image(
                bitmap = frozenPreviewBitmap!!.asImageBitmap(),
                contentDescription = "Frozen Camera Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. SUBTLE PROCESSING SCRIM & SPINNER (NO CARD, NO LARGE TEXT)
        AnimatedVisibility(
            visible = isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CurbBlack.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BentoPrimary,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // 4. MINIMAL FLOATING TOP HUD
        ScanTopHud(
            onBack = onBack,
            onHelp = { showHelpSheet = true }
        )

        // 5. FLOATING BOTTOM CAMERA CONTROLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Photo picker button — first-class alternative capture path
                IconButton(
                    onClick = {
                        if (!isProcessing && !isPreviewFrozen) {
                            galleryLauncher.launch("image/*")
                        }
                    },
                    enabled = !isProcessing && !isPreviewFrozen,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isProcessing || isPreviewFrozen) CurbWhite.copy(alpha = 0.12f)
                            else CurbWhite.copy(alpha = 0.22f)
                        )
                        .border(1.dp, CurbWhite.copy(alpha = 0.28f), CircleShape)
                        .testTag("gallery_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick photo from gallery",
                        tint = if (isProcessing || isPreviewFrozen) CurbWhite.copy(alpha = 0.5f) else CurbWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Large Shutter Button with animated detection feedback
                val isSignDetected = liveDetectedBoxes.isNotEmpty()

                val ringColor by animateColorAsState(
                    targetValue = if (isSignDetected) ScanSuccessOnDark else BentoPrimaryDark,
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
                    targetValue = if (isSignDetected) 6.dp else 3.dp,
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
                        .background(if (isProcessing || isPreviewFrozen) CurbWhite.copy(alpha = 0.6f) else CurbWhite)
                        .clickable(enabled = !isProcessing && !isPreviewFrozen) {
                            val capture = imageCapture

                            // STEP 1 — immediate visual freeze
                            val previewBitmap = previewViewRef?.bitmap
                            if (previewBitmap != null) {
                                frozenPreviewBitmap = previewBitmap
                                isPreviewFrozen = true
                            } else {
                                android.util.Log.d("CurbPipeline", "CurbPipeline: Preview freeze frame unavailable")
                            }

                            // STEP 2 — snapshot live detection state
                            val capturedBoxesSnapshot = liveDetectedBoxes.toList()

                            // STEP 3 — start REAL high-quality capture
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
                val flashBurstScale by animateFloatAsState(
                    targetValue = if (flashEnabled) 1f else 0.5f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f),
                    label = "flashBurstScale"
                )
                val flashBurstAlpha by animateFloatAsState(
                    targetValue = if (flashEnabled) 1f else 0f,
                    animationSpec = spring(),
                    label = "flashBurstAlpha"
                )

                IconButton(
                    onClick = {
                        if (!isProcessing && !isPreviewFrozen) {
                            flashEnabled = !flashEnabled
                        }
                    },
                    enabled = !isProcessing && !isPreviewFrozen,
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (flashBurstAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = flashBurstScale
                                        scaleY = flashBurstScale
                                        alpha = flashBurstAlpha
                                    }
                                    .background(
                                        color = BentoPrimary.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                            )
                        }

                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = "Toggle Flash",
                            tint = if (flashEnabled) BentoPrimaryDark else CurbWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // CAMERA ERROR BANNER
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
                onDismiss = {
                    frozenPreviewBitmap = null
                    isPreviewFrozen = false
                    onClearScanError()
                }
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
 * Clean, lightweight top HUD containing only Back (left) and Help (right).
 */
@Composable
private fun ScanTopHud(
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

        IconButton(
            onClick = onHelp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
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
 * Camera unavailable state.
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
