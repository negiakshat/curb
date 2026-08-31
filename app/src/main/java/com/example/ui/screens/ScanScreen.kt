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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.SampleSignPreset
import com.example.data.remote.GeminiService
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite
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
    var hasCameraPermission by remember {
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

    // Scanning bounding box pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
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
                    .background(Color(0xFF141414)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
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

        // AI SIGN DETECTION BOUNDARIES OVERLAY
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, bottom = 220.dp, start = 32.dp, end = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Detected Sign 1 Bounding Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .border(
                        BorderStroke(
                            2.dp,
                            CurbWhite.copy(alpha = borderAlpha)
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CurbWhite.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "Sign 1: Time Limit",
                                color = CurbBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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

            // Detected Sign 2 Bounding Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .border(
                        BorderStroke(
                            2.dp,
                            CurbWhite.copy(alpha = borderAlpha)
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CurbWhite.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "Sign 2: Street Cleaning",
                                color = CurbBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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

            Text(
                text = "Align Signs in View",
                color = CurbWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

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
            // Preset Sign Quick Selector (Convenient testing selector)
            Text(
                text = "Sample test signs:",
                color = CurbWhite.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GeminiService.PRESET_SIGNS) { preset ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CurbBlack.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, CurbWhite.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onPresetSelected(preset) }
                            .testTag("preset_sign_${preset.id}")
                    ) {
                        Text(
                            text = preset.title.split("—").firstOrNull()?.trim() ?: preset.title,
                            color = CurbWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Shutter Button & Gallery Button
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

                // Large Central Shutter Button
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

                // Spacer to balance row
                Box(modifier = Modifier.size(52.dp))
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
                    .background(CurbBlack.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = CurbWhite,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
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

    // SCAN HELP BOTTOM SHEET
    if (showHelpSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHelpSheet = false },
            sheetState = sheetState,
            containerColor = CurbSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
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
