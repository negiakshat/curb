package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SubscriptionPackageInfo
import com.example.data.remote.SubscriptionUiState
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProSuccessDialog
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoBorderStrong
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusNested
import com.example.viewmodel.PromoCodeResult

private enum class PaywallSuccessType {
    REAL_PURCHASE,
    JUDGE_PROMO,
    RESTORE_SUCCESS
}

@Composable
fun CurbProPaywallScreen(
    subscriptionState: SubscriptionUiState,
    onPurchase: (Activity, SubscriptionPackageInfo, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onRestorePurchases: (onResult: (Boolean, String) -> Unit) -> Unit,
    onApplyPromoCode: (String) -> PromoCodeResult,
    onTermsClicked: () -> Unit,
    onPrivacyClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    val packages = subscriptionState.packages
    var selectedPackageId by remember(packages) {
        val annualPkg = packages.find { it.isBestValue }
        mutableStateOf(annualPkg?.id ?: packages.firstOrNull()?.id ?: "annual")
    }

    var successDialogType by remember { mutableStateOf<PaywallSuccessType?>(null) }
    var promoCodeInput by remember { mutableStateOf("") }
    var promoCodeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subscriptionState.errorMessage) {
        subscriptionState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("curb_pro_paywall_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR (CLOSE BUTTON)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(40.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BentoSand,
                        border = BorderStroke(1.dp, BentoBorder)
                    ) {
                        Text(
                            text = "UPGRADE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("paywall_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CurbOnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // BRANDING HEADER
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CurbBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BentoPeach,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CURB PRO",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = CurbOnSurface,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Park smarter. Worry less.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = CurbOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // BENEFITS LIST CARD
            item {
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PaywallBenefitItem(
                            title = "Unlimited AI parking scans",
                            subtitle = "Never hit a scan limit. Scan every sign on any block."
                        )
                        PaywallBenefitItem(
                            title = "Advanced AI parking assistance",
                            subtitle = "Complex multi-sign conflict resolution & real-time answers."
                        )
                        PaywallBenefitItem(
                            title = "Full scan history & saved places",
                            subtitle = "Keep all your scan reports and manage unlimited saved parking spots."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // PACKAGE SELECTION TITLE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose your plan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // SUBSCRIPTION PACKAGES
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    packages.forEach { pkg ->
                        val isSelected = pkg.id == selectedPackageId
                        PackageOptionCard(
                            packageInfo = pkg,
                            isSelected = isSelected,
                            onSelect = { selectedPackageId = pkg.id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // PROMO CODE SECTION
            item {
                CurbCard(
                    cornerRadius = RadiusNested,
                    backgroundColor = BentoSand.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Have a promo code?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promoCodeInput,
                                onValueChange = {
                                    promoCodeInput = it
                                    if (promoCodeError != null) promoCodeError = null
                                },
                                placeholder = {
                                    Text("Enter promo code", fontSize = 13.sp, color = CurbOnSurfaceVariant)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("paywall_promo_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BentoPrimaryDark,
                                    unfocusedBorderColor = BentoBorder,
                                    focusedContainerColor = CurbWhite,
                                    unfocusedContainerColor = CurbWhite
                                )
                            )

                            Button(
                                onClick = {
                                    val result = onApplyPromoCode(promoCodeInput)
                                    if (result is PromoCodeResult.Success) {
                                        promoCodeError = null
                                        successDialogType = PaywallSuccessType.JUDGE_PROMO
                                    } else if (result is PromoCodeResult.Error) {
                                        promoCodeError = result.message
                                    }
                                },
                                enabled = promoCodeInput.isNotBlank() && !subscriptionState.isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BentoPrimaryDark,
                                    contentColor = CurbWhite
                                ),
                                modifier = Modifier.testTag("paywall_promo_apply_button")
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (promoCodeError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = promoCodeError ?: "",
                                color = CurbError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("paywall_promo_error")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ACTIONS
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val selectedPackage = packages.find { it.id == selectedPackageId } ?: packages.firstOrNull()

                    CurbPrimaryButton(
                        text = if (subscriptionState.isLoading) "Processing…" else "Get Curb Pro",
                        onClick = {
                            if (activity != null && selectedPackage != null && !subscriptionState.isLoading) {
                                onPurchase(
                                    activity,
                                    selectedPackage,
                                    {
                                        successDialogType = PaywallSuccessType.REAL_PURCHASE
                                    },
                                    { error ->
                                        // Error handled via snackbar
                                    }
                                )
                            }
                        },
                        enabled = !subscriptionState.isLoading && selectedPackage != null,
                        testTag = "paywall_get_pro_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("paywall_maybe_later_button")
                    ) {
                        Text(
                            text = "Maybe later",
                            color = CurbOnSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            if (!subscriptionState.isLoading) {
                                onRestorePurchases { success, msg ->
                                    if (success) {
                                        successDialogType = PaywallSuccessType.RESTORE_SUCCESS
                                    }
                                }
                            }
                        },
                        enabled = !subscriptionState.isLoading,
                        modifier = Modifier.testTag("paywall_restore_purchases_button")
                    ) {
                        Text(
                            text = "Restore Purchases",
                            color = BentoPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // LEGAL LINKS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terms of Use",
                            fontSize = 12.sp,
                            color = CurbOnSurfaceVariant,
                            modifier = Modifier
                                .clickable { onTermsClicked() }
                                .padding(4.dp)
                                .testTag("paywall_terms_link")
                        )
                        Text(
                            text = " • ",
                            fontSize = 12.sp,
                            color = CurbOnSurfaceVariant
                        )
                        Text(
                            text = "Privacy Policy",
                            fontSize = 12.sp,
                            color = CurbOnSurfaceVariant,
                            modifier = Modifier
                                .clickable { onPrivacyClicked() }
                                .padding(4.dp)
                                .testTag("paywall_privacy_link")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // LOADING OVERLAY
        if (subscriptionState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CurbSurface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BentoPrimaryDark,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Connecting to Store…",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = CurbOnSurface
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // SUCCESS CELEBRATION MODAL WITH CONFETTI
        successDialogType?.let { type ->
            CurbProSuccessDialog(
                isJudgeCode = type == PaywallSuccessType.JUDGE_PROMO,
                isRestore = type == PaywallSuccessType.RESTORE_SUCCESS,
                onContinue = {
                    successDialogType = null
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun PaywallBenefitItem(
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(CurbBlack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CurbWhite,
                modifier = Modifier.size(13.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = CurbOnSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun PackageOptionCard(
    packageInfo: SubscriptionPackageInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = when {
        isSelected -> BentoPrimaryDark
        packageInfo.isBestValue -> BentoBorderStrong
        else -> BentoBorder
    }

    val backgroundColor = when {
        isSelected -> BentoPeach.copy(alpha = 0.35f)
        packageInfo.isBestValue -> BentoSand.copy(alpha = 0.5f)
        else -> CurbSurface
    }

    Surface(
        shape = RoundedCornerShape(RadiusNested),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("package_card_${packageInfo.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = if (isSelected) 6.dp else 1.5.dp,
                                color = if (isSelected) BentoPrimaryDark else BentoBorderStrong,
                                shape = CircleShape
                            )
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = packageInfo.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbOnSurface
                            )

                            if (packageInfo.isBestValue) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BentoPrimaryDark
                                ) {
                                    Text(
                                        text = "BEST VALUE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbWhite,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = packageInfo.billingDetail,
                            fontSize = 12.sp,
                            color = CurbOnSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = packageInfo.priceString,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = CurbOnSurface
                    )
                    Text(
                        text = packageInfo.period,
                        fontSize = 12.sp,
                        color = CurbOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
