package com.example.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite

@Composable
fun PaymentSubscriptionScreen(
    isPro: Boolean,
    onUpgradeToPro: () -> Unit,
    onRestorePurchases: () -> Unit,
    onBack: () -> Unit
) {
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
                    cornerRadius = 24.dp,
                    backgroundColor = CurbSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPro) CurbBlack else CurbSurfaceVariant
                        ) {
                            Text(
                                text = if (isPro) "ACTIVE" else "FREE",
                                color = if (isPro) CurbWhite else CurbOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PRO FEATURES CARD
                CurbCard(
                    cornerRadius = 24.dp,
                    backgroundColor = CurbSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurbLogo(symbolSize = 24.dp, fontSize = 18)
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

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "$4.99 / month or $39.99 / year",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ProFeatureItem(text = "Unlimited multi-sign AI photo scans")
                        Spacer(modifier = Modifier.height(10.dp))
                        ProFeatureItem(text = "Instant contradictory sign resolution")
                        Spacer(modifier = Modifier.height(10.dp))
                        ProFeatureItem(text = "Smart parking timer expiration reminders")
                        Spacer(modifier = Modifier.height(10.dp))
                        ProFeatureItem(text = "24/7 dedicated Ask Curb AI Assistant")
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
                .background(CurbBlack, CircleShape),
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
            fontSize = 14.sp,
            color = CurbOnSurface
        )
    }
}
