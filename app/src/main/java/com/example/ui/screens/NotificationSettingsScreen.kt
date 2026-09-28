package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbTopAppBar
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbOutline
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun NotificationSettingsScreen(
    pushEnabled: Boolean,
    onTogglePush: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
    var canScheduleExact by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager?.canScheduleExactAlarms() == true
            } else {
                true
            }
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager?.canScheduleExactAlarms() == true
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("notification_settings_screen")
    ) {
        // TOP BAR
        CurbTopAppBar(
            title = "Notification Settings",
            onBack = onBack,
            backTestTag = "notification_settings_back_button"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Manage your device alerts and reminder preferences.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = CurbOnSurfaceVariant
            )

            // PUSH NOTIFICATIONS TOGGLE
            CurbCard(
                cornerRadius = 20.dp,
                backgroundColor = CurbSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Push Notifications",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Receive notifications on your device",
                            fontSize = 13.sp,
                            color = CurbOnSurfaceVariant
                        )
                    }

                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { onTogglePush(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CurbWhite,
                            checkedTrackColor = CurbBlack,
                            uncheckedThumbColor = CurbOutline,
                            uncheckedTrackColor = CurbSurfaceVariant
                        ),
                        modifier = Modifier.testTag("push_notifications_switch")
                    )
                }
            }

            if (pushEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                    CurbCard(
                        cornerRadius = 20.dp,
                        backgroundColor = CurbSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Precise Alarms Required",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbOnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Curb needs permission to schedule precise parking alarms. Without this, notifications may be delayed by Android to save battery.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = CurbOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", context.packageName, null)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CurbBlack,
                                    contentColor = CurbWhite
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("enable_exact_alarms_button")
                            ) {
                                Text("Enable Precise Alerts")
                            }
                        }
                    }
                }

                Text(
                    text = "Active Alerts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Active Session Alerts",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbOnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Expiration and reminder alerts are automatically scheduled whenever you start or extend a parking session. Custom reminder timing (e.g. 15m, 30m) is configured on the Parking Timer.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = CurbOnSurfaceVariant
                            )
                        }

                        Column {
                            Text(
                                text = "Saved Spot Reminders",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbOnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reminders for saved parking locations use the verified parking rules and time limits for that specific spot.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = CurbOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
