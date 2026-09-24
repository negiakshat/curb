package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.CurbSectionHeader
import com.example.ui.components.CurbSettingsDivider
import com.example.ui.components.CurbSettingsRow
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
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
    onSavedPlacesClicked: () -> Unit = {},
    onPrivacyPolicyClicked: () -> Unit = {},
    onTermsOfServiceClicked: () -> Unit = {},
    onLogoutClicked: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .testTag("you_screen"),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 100.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PAGE HEADER
                Column {
                    Text(
                        text = "You",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your Curb profile, preferences & account.",
                        fontSize = 12.sp,
                        color = BentoTextSecondary,
                        lineHeight = 16.sp
                    )
                }

                // 1. PROFILE HERO TILE
                Card(
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar container
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(BentoPrimaryDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.name.take(1).uppercase(Locale.ROOT),
                                    color = BentoWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = userProfile.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(RadiusChip),
                                        color = if (isPro) BentoPeach else BentoSand
                                    ) {
                                        Text(
                                            text = if (isPro) "PRO" else "FREE",
                                            color = if (isPro) BentoPrimaryDark else BentoTextDark,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                val subtitleText = if (userProfile.email.isNotBlank()) {
                                    userProfile.email
                                } else {
                                    "Curb Account"
                                }

                                Text(
                                    text = subtitleText,
                                    fontSize = 12.sp,
                                    color = BentoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 2. PRO HERO BANNER
                CurbProBanner(
                    isPro = isPro,
                    onClick = onPaymentSubscriptionClicked
                )

                // 3. ACCOUNT SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurbSectionHeader(title = "ACCOUNT")
                    Card(
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            CurbSettingsRow(
                                icon = Icons.Default.PersonOutline,
                                title = "Account Information",
                                subtitle = "Name, email & profile details",
                                onClick = onAccountInfoClicked,
                                testTag = "setting_account_info"
                            )
                            CurbSettingsDivider()
                            CurbSettingsRow(
                                icon = Icons.Default.CreditCard,
                                title = "Payment & Subscription",
                                subtitle = if (isPro) "Curb Pro active • Manage plan & quotas" else "Upgrade to Pro • View plans & promo codes",
                                onClick = onPaymentSubscriptionClicked,
                                testTag = "setting_payment_subscription"
                            )
                        }
                    }
                }

                // 4. PREFERENCES SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurbSectionHeader(title = "PREFERENCES")
                    Card(
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            CurbSettingsRow(
                                icon = Icons.Default.NotificationsNone,
                                title = "Notification Settings",
                                subtitle = "Parking alerts & push notifications",
                                onClick = onNotificationsClicked,
                                testTag = "setting_notifications"
                            )
                            CurbSettingsDivider()
                            CurbSettingsRow(
                                icon = Icons.Default.BookmarkBorder,
                                title = "Saved Places",
                                subtitle = "Pinned parking locations & notes",
                                onClick = onSavedPlacesClicked,
                                testTag = "setting_saved_places"
                            )
                        }
                    }
                }

                // 5. SUPPORT & INFORMATION SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurbSectionHeader(title = "SUPPORT & INFORMATION")
                    Card(
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            CurbSettingsRow(
                                icon = Icons.Default.HelpOutline,
                                title = "Help & Support",
                                subtitle = "FAQs, contact support & app feedback",
                                onClick = onHelpSupportClicked,
                                testTag = "setting_help_support"
                            )
                            CurbSettingsDivider()
                            CurbSettingsRow(
                                icon = Icons.Default.Shield,
                                title = "Privacy Policy",
                                subtitle = "Data protection & privacy rights",
                                onClick = onPrivacyPolicyClicked,
                                testTag = "setting_privacy_policy"
                            )
                            CurbSettingsDivider()
                            CurbSettingsRow(
                                icon = Icons.Default.Description,
                                title = "Terms of Service",
                                subtitle = "Terms & conditions of use",
                                onClick = onTermsOfServiceClicked,
                                testTag = "setting_terms_of_service"
                            )
                            CurbSettingsDivider()
                            CurbSettingsRow(
                                icon = Icons.Default.Info,
                                title = "About Curb",
                                subtitle = "Version 1.0.0",
                                onClick = onAboutCurbClicked,
                                testTag = "setting_about_curb"
                            )
                        }
                    }
                }

                // 6. LOG OUT ACTION CARD
                Card(
                    shape = RoundedCornerShape(RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CurbSettingsRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Log Out",
                        subtitle = "End current session on this device",
                        onClick = { showLogoutDialog = true },
                        tint = CurbError,
                        textColor = CurbError,
                        testTag = "setting_logout"
                    )
                }
            }
        }
    }

    // LOGOUT CONFIRMATION DIALOG
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(RadiusCard),
            containerColor = BentoWhite,
            title = {
                Text(
                    text = "Log Out?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BentoTextPrimary
                )
            },
            text = {
                Text(
                    text = "Logging out will end your current session. Your saved places and parking history will remain safely stored on this device.",
                    color = BentoTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
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
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = BentoTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CurbProBanner(
    isPro: Boolean,
    onClick: () -> Unit
) {
    if (isPro) {
        Card(
            shape = RoundedCornerShape(RadiusCard),
            colors = CardDefaults.cardColors(containerColor = BentoSand),
            border = BorderStroke(1.dp, BentoBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BentoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BentoWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Curb Pro Member",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )
                        Text(
                            text = "Unlimited sign scans & AI assistant active",
                            fontSize = 11.sp,
                            color = BentoTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(RadiusCard),
            colors = CardDefaults.cardColors(containerColor = BentoPeach),
            border = BorderStroke(1.dp, BentoBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BentoPrimaryDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BentoWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upgrade to Curb Pro",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark
                        )
                        Text(
                            text = "Get unlimited scans, AI advice & parking alerts",
                            fontSize = 11.sp,
                            color = BentoTextDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(RadiusChip),
                    color = BentoPrimaryDark
                ) {
                    Text(
                        text = "UPGRADE",
                        color = BentoWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
