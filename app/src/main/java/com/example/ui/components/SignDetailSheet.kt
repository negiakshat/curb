package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DetectedSign
import com.example.data.model.ScanVerdict
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.CurbWarningContainer
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualSignDetailSheet(
    sign: DetectedSign,
    overallVerdict: ScanVerdict,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRawText by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CurbSurface,
        shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // SHEET TOP HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sign.id.isNotBlank()) "Sign ${sign.id}" else "Scanned Sign Detail",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )

                val (badgeColor, textColor, badgeText) = remember(sign.isRestrictingNow, overallVerdict) {
                    when {
                        sign.isRestrictingNow -> Triple(CurbErrorContainer, CurbError, "Active Restriction Sign")
                        overallVerdict == ScanVerdict.RESTRICTED -> Triple(CurbSurfaceVariant, CurbOnSurfaceVariant, "Currently Inactive Sign")
                        overallVerdict == ScanVerdict.ALLOWED -> Triple(CurbSuccessContainer, CurbSuccess, "Permit / Time Limit Sign")
                        else -> Triple(CurbWarningContainer, CurbWarning, "Unclear Sign Rule")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(RadiusChip),
                    color = badgeColor
                ) {
                    Text(
                        text = badgeText,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. REAL ACCURATE CROPPED SIGN IMAGE
            if (!sign.croppedImageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CurbSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(sign.croppedImageUri),
                        contentDescription = sign.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CurbSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = CurbOnSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cropped Sign Image",
                            fontSize = 13.sp,
                            color = CurbOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. WHAT THIS SIGN MEANS
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "WHAT THIS SIGN MEANS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = CurbOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sign.title.ifBlank { "Parking Sign" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
            }

            // 3. APPLICABLE DAYS & HOURS
            val daysHoursText = sign.applicableDaysHours.ifBlank { sign.subtitle }
            if (daysHoursText.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "APPLICABLE DAYS & HOURS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CurbOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = daysHoursText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CurbOnSurface
                    )
                }
            }

            // 4. RESTRICTION & RULE
            val ruleExplanationText = sign.restrictions.ifBlank { sign.ruleText }
            if (ruleExplanationText.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "RESTRICTION & RULE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CurbOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ruleExplanationText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = CurbOnSurface
                    )
                }
            }

            // 5. EXCEPTIONS & EXEMPTIONS
            if (sign.exceptions.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "EXCEPTIONS & EXEMPTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CurbOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sign.exceptions,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = CurbOnSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. OVERALL VS INDIVIDUAL STATUS EXPLANATION
            CurbCard(
                cornerRadius = RadiusCard,
                backgroundColor = CurbSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CurbOnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "How this sign applies right now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val contextMessage = when {
                            sign.isUncertain ->
                                "This physical sign is faded, partially obscured, or unreadable. Treat this spot with caution and check nearby signs."
                            sign.isRestrictingNow ->
                                "This sign is currently ACTIVE and is the primary restriction prohibiting parking at this location right now."
                            overallVerdict == ScanVerdict.RESTRICTED ->
                                "This sign rule is not currently restricting right now, but another detected restriction at this location prohibits parking."
                            overallVerdict == ScanVerdict.ALLOWED ->
                                "This sign defines the time limit or permission conditions for when you park here."
                            else ->
                                "This sign's text could not be verified with 100% certainty. Please inspect physical signage locally."
                        }
                        Text(
                            text = contextMessage,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = CurbOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            CurbPrimaryButton(
                text = "Close",
                onClick = onDismiss,
                testTag = "close_sign_detail_button"
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
