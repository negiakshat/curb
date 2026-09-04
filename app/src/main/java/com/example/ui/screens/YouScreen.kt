package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.CurbCard
import androidx.compose.material.icons.filled.Edit
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite
import java.util.Locale

@Composable
fun YouScreen(
    userProfile: UserProfile,
    isPro: Boolean = userProfile.isPro,
    onAccountInfoClicked: () -> Unit,
    onNotificationsClicked: () -> Unit,
    onPaymentSubscriptionClicked: () -> Unit,
    onHelpSupportClicked: () -> Unit,
    onAboutCurbClicked: () -> Unit,
    onLogoutClicked: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .testTag("you_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "You",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PROFILE HEADER CARD
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface,
                    borderColor = BentoBorder,
                    onClick = onAccountInfoClicked
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar container with subtle edit badge indicator
                            Box(modifier = Modifier.size(52.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(CurbBlack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userProfile.name.take(1).uppercase(Locale.ROOT),
                                        color = CurbWhite,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(BentoPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = CurbWhite,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = userProfile.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )

                                    if (isPro) {
                                        Surface(
                                            shape = RoundedCornerShape(RadiusSmall),
                                            color = BentoPrimary
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = CurbWhite,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(RadiusSmall),
                                            color = BentoSand
                                        ) {
                                            Text(
                                                text = "FREE",
                                                color = BentoTextDark,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                val subtitleText = if (userProfile.email.isNotBlank()) {
                                    userProfile.email
                                } else {
                                    "Tap to edit profile & details"
                                }

                                Text(
                                    text = subtitleText,
                                    fontSize = 13.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BentoSand.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Edit Profile",
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Settings & Preferences",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // GROUPED SETTINGS MENU (Account Info, Notifications, Payment & Subscription, Help & Support, About Curb AI)
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        SettingsRow(
                            icon = Icons.Default.PersonOutline,
                            title = "Account Information",
                            onClick = onAccountInfoClicked,
                            testTag = "setting_account_info"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.NotificationsNone,
                            title = "Notifications",
                            onClick = onNotificationsClicked,
                            testTag = "setting_notifications"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.CreditCard,
                            title = "Payment & Subscription",
                            onClick = onPaymentSubscriptionClicked,
                            testTag = "setting_payment_subscription"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.HelpOutline,
                            title = "Help & Support",
                            onClick = onHelpSupportClicked,
                            testTag = "setting_help_support"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "About Curb AI",
                            onClick = onAboutCurbClicked,
                            testTag = "setting_about_curb"
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Log Out",
                            onClick = { showLogoutDialog = true },
                            tint = CurbError,
                            textColor = CurbError,
                            testTag = "setting_logout"
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Log Out?",
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
            },
            text = {
                Text(
                    text = "Logging out will end your current session. Your saved places and parking history will remain safely stored on this device.",
                    color = CurbOnSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClicked()
                    },
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Log Out", color = CurbError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel", color = CurbOnSurfaceVariant)
                }
            },
            containerColor = CurbSurface
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = CurbBlack,
    textColor: androidx.compose.ui.graphics.Color = CurbOnSurface,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(CurbSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = CurbOnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 20.dp)
            .background(CurbSurfaceVariant)
    )
}
