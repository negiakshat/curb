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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbOutline
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite

@Composable
fun NotificationsScreen(
    pushEnabled: Boolean,
    onTogglePush: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showPermissionDialog by remember { mutableStateOf(false) }
    var expirationAlerts by remember { mutableStateOf(true) }
    var streetCleaningAlerts by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("notifications_screen")
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("notifications_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = "Notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Manage your parking alerts and reminder preferences.",
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
                        onCheckedChange = { checked ->
                            if (checked && !pushEnabled) {
                                showPermissionDialog = true
                            } else {
                                onTogglePush(checked)
                            }
                        },
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
                Text(
                    text = "Notification Types",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Timer Expiration Reminders",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CurbOnSurface
                                )
                                Text(
                                    text = "Alert 15 mins before parking expires",
                                    fontSize = 12.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                            Switch(
                                checked = expirationAlerts,
                                onCheckedChange = { expirationAlerts = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CurbWhite,
                                    checkedTrackColor = CurbBlack
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Street Cleaning Alerts",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CurbOnSurface
                                )
                                Text(
                                    text = "Morning warnings for sweeping days",
                                    fontSize = 12.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                            Switch(
                                checked = streetCleaningAlerts,
                                onCheckedChange = { streetCleaningAlerts = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CurbWhite,
                                    checkedTrackColor = CurbBlack
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Allow Notifications?", fontWeight = FontWeight.Bold) },
            text = { Text("Curb would like to send you notifications for parking timer expirations and active street sweeping warnings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        onTogglePush(true)
                    }
                ) {
                    Text("Allow", color = CurbBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Don't Allow", color = CurbOnSurfaceVariant)
                }
            }
        )
    }
}
