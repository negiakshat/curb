package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScanUsageInfo
import com.example.data.remote.SubscriptionUiState
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.BentoBeige
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.RadiusSmall

@Composable
fun PaymentSubscriptionScreen(
    isPro: Boolean,
    usageInfo: ScanUsageInfo,
    subscriptionState: SubscriptionUiState,
    onUpgradeToPro: () -> Unit,
    onRestorePurchases: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("payment_subscription_screen")
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
                modifier = Modifier.testTag("payment_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = "Payment & Subscription",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // CURRENT PLAN CARD
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current Plan",
                                    fontSize = 13.sp,
                                    color = CurbOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPro) "Curb Pro" else "Curb Free Plan",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CurbOnSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(RadiusSmall),
                                color = if (isPro) BentoPrimaryDark else BentoSand
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    if (isPro) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CurbWhite,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isPro) "ACTIVE" else "FREE",
                                        color = if (isPro) CurbWhite else BentoTextDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // USAGE INFO
                        if (!isPro) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BentoBeige, RoundedCornerShape(RadiusNested))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Monthly AI Scans",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CurbOnSurface
                                    )
                                    Text(
                                        text = "${usageInfo.scansUsed} / ${usageInfo.monthlyLimit} used",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPrimaryDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { usageInfo.fractionUsed },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (usageInfo.isLimitReached) CurbBlack else BentoPrimaryDark,
                                    trackColor = BentoBorder
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "${usageInfo.displayText} this month • Resets automatically next month",
                                    fontSize = 11.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BentoSand, RoundedCornerShape(RadiusNested))
                                    .padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(BentoPrimaryDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = BentoPeach,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Unlimited AI sign scans unlocked",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )
                                    Text(
                                        text = "Full Pro access active with real-time assistance",
                                        fontSize = 11.sp,
                                        color = CurbOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // PRO FEATURES CARD
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurbLogo(symbolSize = 22.dp, fontSize = 16)
                            Surface(
                                shape = RoundedCornerShape(RadiusSmall),
                                color = BentoPrimaryDark
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

                        Spacer(modifier = Modifier.height(12.dp))

                        val pricingSummary = if (subscriptionState.packages.isNotEmpty()) {
                            val weekly = subscriptionState.packages.find { it.id.contains("weekly") }?.priceString ?: "$2.99"
                            val monthly = subscriptionState.packages.find { it.id.contains("monthly") }?.priceString ?: "$4.99"
                            val annual = subscriptionState.packages.find { it.id.contains("annual") }?.priceString ?: "$29.99"
                            "$weekly/wk • $monthly/mo • $annual/yr"
                        } else {
                            "$2.99 / week • $4.99 / month • $29.99 / year"
                        }

                        Text(
                            text = pricingSummary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ProFeatureItem(text = "Unlimited multi-sign AI photo scans")
                        Spacer(modifier = Modifier.height(8.dp))
                        ProFeatureItem(text = "Advanced AI parking assistance & conflict resolution")
                        Spacer(modifier = Modifier.height(8.dp))
                        ProFeatureItem(text = "Smart parking timer expiration reminders")
                        Spacer(modifier = Modifier.height(8.dp))
                        ProFeatureItem(text = "Full Curb Pro experience & priority processing")
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isPro) {
                    CurbPrimaryButton(
                        text = "Upgrade to Curb Pro",
                        onClick = onUpgradeToPro,
                        testTag = "upgrade_pro_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    CurbPrimaryButton(
                        text = "Manage Subscription",
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/account/subscriptions")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore if browser/store cannot be launched
                            }
                        },
                        testTag = "manage_subscription_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                TextButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.testTag("restore_purchases_button")
                ) {
                    Text(
                        text = "Restore Purchases",
                        color = CurbOnSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(BentoPrimaryDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CurbWhite,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            color = CurbOnSurface
        )
    }
}
