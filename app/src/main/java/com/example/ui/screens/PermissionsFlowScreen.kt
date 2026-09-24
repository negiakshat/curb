package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.OnboardingHeader
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurfaceVariant

private data class PermissionDialogData(
    val title: String,
    val message: String,
    val isPermanent: Boolean
)

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun checkLocationPermissionGranted(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun checkCameraPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

private fun checkPhotosPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val full = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val visual = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        full || visual
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionsFlowScreen(
    onPermissionsFinished: () -> Unit,
    onBackToNameSetup: () -> Unit = {}
) {
    val context = LocalContext.current

    // Initial step determination
    val initialStep = remember {
        when {
            !checkLocationPermissionGranted(context) -> 2
            !checkCameraPermissionGranted(context) -> 3
            !checkPhotosPermissionGranted(context) -> 4
            else -> 4
        }
    }

    var currentStep by remember { mutableIntStateOf(initialStep) }
    var permissionDialogData by remember { mutableStateOf<PermissionDialogData?>(null) }

    // If all permissions are already granted when opening
    LaunchedEffect(Unit) {
        if (checkLocationPermissionGranted(context) &&
            checkCameraPermissionGranted(context) &&
            checkPhotosPermissionGranted(context)
        ) {
            onPermissionsFinished()
        }
    }

    // Auto-advance when returning from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                when (currentStep) {
                    2 -> if (checkLocationPermissionGranted(context)) currentStep = 3
                    3 -> if (checkCameraPermissionGranted(context)) currentStep = 4
                    4 -> if (checkPhotosPermissionGranted(context)) onPermissionsFinished()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Native Permission Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (checkLocationPermissionGranted(context)) {
            currentStep = 3
        } else {
            val activity = findActivity(context)
            val isPermanent = activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)

            permissionDialogData = PermissionDialogData(
                title = "Location permission is required",
                message = if (isPermanent)
                    "Enable Location permission in Settings to continue using Curb."
                else
                    "Curb uses your location to understand the parking context around you and provide accurate local regulation advice.",
                isPermanent = isPermanent
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (checkCameraPermissionGranted(context)) {
            currentStep = 4
        } else {
            val activity = findActivity(context)
            val isPermanent = activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

            permissionDialogData = PermissionDialogData(
                title = "Camera permission is required",
                message = if (isPermanent)
                    "Enable Camera permission in Settings to continue using Curb."
                else
                    "Curb uses your camera to scan parking signs, detect restrictions, and understand their rules instantly.",
                isPermanent = isPermanent
            )
        }
    }

    val targetPhotoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (checkPhotosPermissionGranted(context)) {
            onPermissionsFinished()
        } else {
            val activity = findActivity(context)
            val isPermanent = activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, targetPhotoPermission)

            permissionDialogData = PermissionDialogData(
                title = "Photo permission is required",
                message = if (isPermanent)
                    "Enable Photo permission in Settings to continue using Curb."
                else
                    "Allow Curb to access your photo library so you can select and analyze saved parking sign photos.",
                isPermanent = isPermanent
            )
        }
    }

    val (icon, title, explanation, buttonText) = when (currentStep) {
        2 -> Quad(
            Icons.Default.LocationOn,
            "Enable Location Access",
            "Curb uses your location to understand the parking context around you and provide accurate local regulation advice.",
            "Allow Location"
        )
        3 -> Quad(
            Icons.Default.CameraAlt,
            "Enable Camera Access",
            "Curb uses your camera to scan parking signs, detect restrictions, and understand their rules instantly.",
            "Allow Camera"
        )
        else -> Quad(
            Icons.Default.PhotoLibrary,
            "Enable Photo Access",
            "Allow Curb to access your photo library so you can select and analyze saved parking sign photos.",
            "Allow Photos & Finish"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // SHARED ONBOARDING HEADER (STEPS 2, 3, 4 OF 4)
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = 4,
                onBack = {
                    when (currentStep) {
                        2 -> onBackToNameSetup()
                        3 -> currentStep = 2
                        4 -> currentStep = 3
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            CurbLogo(symbolSize = 32.dp, fontSize = 24)

            Spacer(modifier = Modifier.height(32.dp))

            // Icon card
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(CurbSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = CurbBlack
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = explanation,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = CurbOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            CurbCard(
                cornerRadius = 16.dp,
                backgroundColor = CurbSurfaceVariant
            ) {
                Text(
                    text = "You can change these permissions at any time in your Android system settings.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = CurbOnSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurbPrimaryButton(
                text = buttonText,
                onClick = {
                    when (currentStep) {
                        2 -> {
                            if (checkLocationPermissionGranted(context)) {
                                currentStep = 3
                            } else {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                        3 -> {
                            if (checkCameraPermissionGranted(context)) {
                                currentStep = 4
                            } else {
                                cameraLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        4 -> {
                            if (checkPhotosPermissionGranted(context)) {
                                onPermissionsFinished()
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    photoLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    photoLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        }
                    }
                },
                testTag = "permission_allow_button"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Custom Permission Required Dialog
    if (permissionDialogData != null) {
        val dialogInfo = permissionDialogData!!
        AlertDialog(
            onDismissRequest = { /* Force user to tap OK */ },
            title = {
                Text(
                    text = dialogInfo.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CurbOnSurface
                )
            },
            text = {
                Text(
                    text = dialogInfo.message,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = CurbOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val isPermanent = dialogInfo.isPermanent
                        permissionDialogData = null
                        if (isPermanent) {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                    },
                    modifier = Modifier.testTag("permission_dialog_ok_button")
                ) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimaryDark
                    )
                }
            },
            containerColor = BentoWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
