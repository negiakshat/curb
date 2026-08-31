package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite

@Composable
fun PermissionsFlowScreen(
    onPermissionsFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) } // 1: Location, 2: Camera, 3: Photos

    // Real Native Permission Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Whether granted or denied, gracefully advance to next step
        currentStep = 2
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Advance to step 3
        currentStep = 3
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionsFinished()
    }

    val (icon, title, explanation, buttonText) = when (currentStep) {
        1 -> Quad(
            Icons.Default.LocationOn,
            "Enable Location Access",
            "Curb uses your location to understand the parking context around you and provide accurate local regulation advice.",
            "Allow Location"
        )
        2 -> Quad(
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))

            CurbLogo(symbolSize = 32.dp, fontSize = 24)

            Spacer(modifier = Modifier.height(40.dp))

            // Step Indicator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (step in 1..3) {
                    val isDone = step < currentStep
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                color = if (isDone || isCurrent) CurbBlack else CurbSurfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

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

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = explanation,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = CurbOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        1 -> {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        2 -> {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                        3 -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                photoLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            } else {
                                photoLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    }
                },
                testTag = "permission_allow_button"
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    if (currentStep < 3) {
                        currentStep++
                    } else {
                        onPermissionsFinished()
                    }
                },
                modifier = Modifier.testTag("skip_permission_button")
            ) {
                Text(
                    text = "Not now",
                    color = CurbOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
