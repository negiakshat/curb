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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite
import java.util.Locale

@Composable
fun YouScreen(
    userProfile: UserProfile,
    onAccountInfoClicked: () -> Unit,
    onNotificationsClicked: () -> Unit,
    onPaymentSubscriptionClicked: () -> Unit,
    onHelpSupportClicked: () -> Unit,
    onAboutCurbClicked: () -> Unit
) {
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
                    cornerRadius = 24.dp,
                    backgroundColor = CurbSurface,
                    onClick = onAccountInfoClicked
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CurbBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.name.take(1).uppercase(Locale.ROOT),
                                    color = CurbWhite,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
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

                                    if (userProfile.isPro) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CurbBlack
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = CurbWhite,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = userProfile.email,
                                    fontSize = 14.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Edit",
                            tint = CurbOnSurfaceVariant
                        )
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
                    cornerRadius = 24.dp,
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
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
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
                    tint = CurbBlack,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CurbOnSurface
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
