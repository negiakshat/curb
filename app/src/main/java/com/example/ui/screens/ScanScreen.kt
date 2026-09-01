package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.SampleSignPreset
import com.example.data.remote.GeminiService
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.RadiusSmall
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    isProcessing: Boolean,
    processingStatusText: String,
    onCaptureImage: (Bitmap?) -> Unit,
    onPresetSelected: (SampleSignPreset) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showHelpSheet by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    val hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                onCaptureImage(bitmap)
            } catch (e: Exception) {
                onCaptureImage(null)
            }
        }
    }

    // FANCY MATERIAL DESIGN 3 SCANNING ANIMATION CONTROLLERS
    val infiniteTransition = rememberInfiniteTransition(label = "m3_scan_animation")

    // 1. Smooth Laser Sweep (Top to bottom cyclic scan)
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_sweep"
    )

    // 2. Corner Reticle Breathing / Pulse
    val reticlePulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticle_pulse"
    )

    // 3. Radar wave expansion
    val radarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_wave"
    )

    // 4. Bounding Box Glow Pulse
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounding_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBlack)
            .testTag("scan_screen")
    ) {
        // FULL SCREEN CAMERA VIEWFINDER
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
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

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                (ctx as androidx.lifecycle.LifecycleOwner),
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            // Camera bind fallback
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Viewfinder fallback placeholder
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
                        text = "Point toward parking signage or select a test sign below.",
                        fontSize = 14.sp,
                        color = CurbWhite.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // FANCY M3 CAMERA SCANNING OVERLAY CANVAS (Laser, Reticles, Radar Sweep)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 220.dp, start = 24.dp, end = 24.dp)
        ) {
            val width = maxWidth
            val height = maxHeight

            // Canvas for corner brackets, laser beam & radar pulse
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cornerLen = 32.dp.toPx()
                val strokeW = 3.5.dp.toPx()
                val cornerRad = RadiusHero.toPx()

                val reticleColor = Color.White.copy(alpha = 0.4f + (reticlePulse * 0.55f))

                // Top-Left Reticle
                drawLine(
                    color = reticleColor,
                    start = Offset(0f, cornerLen),
                    end = Offset(0f, 0f),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(0f, 0f),
                    end = Offset(cornerLen, 0f),
                    strokeWidth = strokeW
                )

                // Top-Right Reticle
                drawLine(
                    color = reticleColor,
                    start = Offset(w - cornerLen, 0f),
                    end = Offset(w, 0f),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(w, 0f),
                    end = Offset(w, cornerLen),
                    strokeWidth = strokeW
                )

                // Bottom-Left Reticle
                drawLine(
                    color = reticleColor,
                    start = Offset(0f, h - cornerLen),
                    end = Offset(0f, h),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(0f, h),
                    end = Offset(cornerLen, h),
                    strokeWidth = strokeW
                )

                // Bottom-Right Reticle
                drawLine(
                    color = reticleColor,
                    start = Offset(w - cornerLen, h),
                    end = Offset(w, h),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(w, h),
                    end = Offset(w, h - cornerLen),
                    strokeWidth = strokeW
                )

                // Expanding Radar Wave Ring from center
                val center = Offset(w / 2f, h / 2f)
                val maxRadius = Math.min(w, h) * 0.45f
                val currentRadius = maxRadius * radarProgress
                val radarAlpha = (1f - radarProgress).coerceIn(0f, 1f) * 0.4f

                drawCircle(
                    color = Color.White.copy(alpha = radarAlpha),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Subtle Center Crosshair Targeting Ticks
                val chLen = 10.dp.toPx()
                val chGap = 8.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(center.x - chGap - chLen, center.y),
                    end = Offset(center.x - chGap, center.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(center.x + chGap, center.y),
                    end = Offset(center.x + chGap + chLen, center.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(center.x, center.y - chGap - chLen),
                    end = Offset(center.x, center.y - chGap),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(center.x, center.y + chGap),
                    end = Offset(center.x, center.y + chGap + chLen),
                    strokeWidth = 2.dp.toPx()
                )

                // Dynamic Laser Beam with Trailing Gradient Sweep
                val laserY = h * laserProgress
                val trailHeight = 60.dp.toPx()

                // Trailing gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFE88A6B).copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.35f)
                        ),
                        startY = (laserY - trailHeight).coerceAtLeast(0f),
                        endY = laserY
                    ),
                    topLeft = Offset(0f, (laserY - trailHeight).coerceAtLeast(0f)),
                    size = Size(w, trailHeight.coerceAtMost(laserY))
                )

                // Primary glowing laser line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFFB4A2),
                            Color.White,
                            Color(0xFFFFB4A2),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, laserY),
                    end = Offset(w, laserY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // AI DETECTED SIGN BOUNDING BOXES (Standardized RadiusNested = 16.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Detected Sign 1: Time Limit
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .border(
                            BorderStroke(
                                2.dp,
                                Color.White.copy(alpha = borderAlpha)
                            ),
                            RoundedCornerShape(RadiusNested)
                        )
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(RadiusNested))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(RadiusSmall),
                                    color = Color.White.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = "Sign 1: Time Limit",
                                        color = CurbBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(RadiusSmall),
                                    color = CurbSuccess.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = "99.1% Confidence",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "2 HOUR PARKING • 8 AM - 6 PM",
                                color = CurbWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detected Sign 2: Street Cleaning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .border(
                            BorderStroke(
                                2.dp,
                                Color.White.copy(alpha = borderAlpha)
                            ),
                            RoundedCornerShape(RadiusNested)
                        )
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(RadiusNested))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(RadiusSmall),
                                    color = Color.White.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = "Sign 2: Street Cleaning",
                                        color = CurbBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(RadiusSmall),
                                    color = BentoPeach
                                ) {
                                    Text(
                                        text = "Rule Tracked",
                                        color = BentoPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "NO PARKING • TUE & THU 8-10 AM",
                                color = CurbWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // TOP CONTROLS (Back / Close & Help "?")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(CurbBlack.copy(alpha = 0.5f), CircleShape)
                    .testTag("scan_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close",
                    tint = CurbWhite
                )
            }

            Surface(
                shape = RoundedCornerShape(RadiusChip),
                color = CurbBlack.copy(alpha = 0.55f),
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
                            .background(CurbSuccess, CircleShape)
                    )
                    Text(
                        text = "AI Sign Detection Active",
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
                    .background(CurbBlack.copy(alpha = 0.5f), CircleShape)
                    .testTag("scan_help_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Scan Help",
                    tint = CurbWhite
                )
            }
        }

        // BOTTOM CAPTURE CONTROLS & PRESET REALISTIC SIGNS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preset Sign Quick Selector (Convenient testing selector with equal RadiusChip)
            Text(
                text = "Sample test signs:",
                color = CurbWhite.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GeminiService.PRESET_SIGNS) { preset ->
                    Surface(
                        shape = RoundedCornerShape(RadiusChip),
                        color = CurbBlack.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, CurbWhite.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onPresetSelected(preset) }
                            .testTag("preset_sign_${preset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BentoPeach,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = preset.title.split("—").firstOrNull()?.trim() ?: preset.title,
                                color = CurbWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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

                // Large Central Shutter Button with tactile pulsing ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CurbWhite)
                        .clickable {
                            val capture = imageCapture
                            if (capture != null) {
                                capture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer: ByteBuffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            image.close()
                                            onCaptureImage(bitmap)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            onCaptureImage(null)
                                        }
                                    }
                                )
                            } else {
                                onCaptureImage(null)
                            }
                        }
                        .testTag("shutter_capture_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .border(BorderStroke(3.dp, CurbBlack), CircleShape)
                    )
                }

                // Symmetrical Flashlight / Assist Button
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
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Toggle Flash",
                        tint = if (flashEnabled) BentoPrimary else CurbWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // SCAN PROCESSING MODAL OVERLAY
        AnimatedVisibility(
            visible = isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CurbBlack.copy(alpha = 0.9f)),
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Analyzing local municipal rules & conditions",
                        fontSize = 14.sp,
                        color = CurbWhite.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // SCAN HELP BOTTOM SHEET (RadiusHero = 28.dp)
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
                    text = "How to scan",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                HelpTipRow(number = "1", title = "Point toward parking signs", desc = "Position your camera so all signs on the pole are within the frame.")
                Spacer(modifier = Modifier.height(14.dp))
                HelpTipRow(number = "2", title = "Keep signs visible & clear", desc = "Ensure text, hours, days, and directional arrows are reasonably legible.")
                Spacer(modifier = Modifier.height(14.dp))
                HelpTipRow(number = "3", title = "Multi-sign analysis", desc = "Curb analyzes combinations of street cleaning, meters, tow-away, and permit signs together.")

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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = CurbOnSurfaceVariant
            )
        }
    }
}
