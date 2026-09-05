package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.CurbWarningContainer
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip

@Composable
fun ParkingVerdictCard(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    val config = when (scanResult.verdict) {
        ScanVerdict.ALLOWED -> VerdictCardConfig(
            backgroundColor = CurbSuccessContainer,
            iconBgColor = CurbSuccess,
            iconVector = Icons.Default.CheckCircle,
            verdictLabel = "Parking allowed",
            supportingText = "You can park here under current rules.",
            section1Header = "ALLOWED UNTIL",
            section1HeaderColor = CurbOnSurfaceVariant,
            section1PrimaryText = if (isUnrestrictedResult(scanResult)) "No Time Limit" else scanResult.allowedUntilTime.ifBlank { "Active Schedule" },
            section1PrimaryColor = CurbSuccess,
            section1ChipText = if (!isUnrestrictedResult(scanResult)) scanResult.timeRemaining.takeIf { it.isNotBlank() } else null,
            section1ChipBgColor = CurbSuccessContainer,
            section1ChipTextColor = CurbSuccess,
            section2Header = "WHY PARKING IS ALLOWED",
            section2Content = scanResult.explanation.ifBlank { "Active parking signage permits parking under the current posted schedule." }
        )
        ScanVerdict.RESTRICTED -> VerdictCardConfig(
            backgroundColor = CurbErrorContainer,
            iconBgColor = CurbError,
            iconVector = Icons.Default.Error,
            verdictLabel = "Parking restricted",
            supportingText = "An active rule prohibits parking right now.",
            section1Header = "ACTIVE RESTRICTION IN EFFECT",
            section1HeaderColor = CurbError,
            section1PrimaryText = scanResult.parkingRules.firstOrNull() ?: "Active zone or municipal restrictions prohibit parking at this location.",
            section1PrimaryColor = CurbOnSurface,
            section1ChipText = null,
            section1ChipBgColor = CurbErrorContainer,
            section1ChipTextColor = CurbError,
            section2Header = "WHY PARKING IS RESTRICTED",
            section2Content = scanResult.explanation.ifBlank { "Posted signage prohibits parking or stopping during the current time window." }
        )
        ScanVerdict.AMBIGUOUS -> VerdictCardConfig(
            backgroundColor = CurbWarningContainer,
            iconBgColor = CurbWarning,
            iconVector = Icons.Default.Warning,
            verdictLabel = "Rule unclear",
            supportingText = "Signage is obscured, faded, or incomplete.",
            section1Header = "VERIFY BEFORE PARKING",
            section1HeaderColor = CurbWarning,
            section1PrimaryText = "Check physical street signs before leaving your vehicle.",
            section1PrimaryColor = CurbOnSurface,
            section1ChipText = null,
            section1ChipBgColor = CurbWarningContainer,
            section1ChipTextColor = CurbWarning,
            section2Header = "REASON FOR UNCERTAINTY",
            section2Content = scanResult.explanation.ifBlank { "Signage text was partially obscured, faded, or incomplete, preventing a definitive ruling." }
        )
    }

    CurbCard(
        cornerRadius = 20.dp,
        backgroundColor = config.backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .testTag("parking_verdict_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. HEADER: ICON CONTAINER + VERDICT LABEL + SUPPORTING SUBTITLE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(config.iconBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.iconVector,
                        contentDescription = null,
                        tint = CurbWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.verdictLabel,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = config.supportingText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = CurbOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. CONTEXT & TIMING SECTION (Responsive for small/narrow screens)
            Surface(
                shape = RoundedCornerShape(RadiusCard),
                color = CurbWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = config.section1Header,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = config.section1HeaderColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (scanResult.verdict == ScanVerdict.ALLOWED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = config.section1PrimaryText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = config.section1PrimaryColor,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            config.section1ChipText?.let { chipText ->
                                Surface(
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = config.section1ChipBgColor,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = chipText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = config.section1ChipTextColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = config.section1PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 20.sp,
                            color = config.section1PrimaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. IMMEDIATE REASON ("WHY?")
            Surface(
                shape = RoundedCornerShape(RadiusCard),
                color = CurbWhite.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = config.section2Header,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CurbOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = config.section2Content,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = CurbOnSurface
                    )
                }
            }
        }
    }
}

private fun isUnrestrictedResult(scanResult: ScanResult): Boolean {
    return scanResult.allowedUntilTime.contains("Unrestricted", ignoreCase = true) ||
            scanResult.allowedUntilTime.contains("No Limit", ignoreCase = true) ||
            scanResult.timeRemaining.contains("Unrestricted", ignoreCase = true) ||
            scanResult.timeRemaining.contains("No Limit", ignoreCase = true)
}

private data class VerdictCardConfig(
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val iconBgColor: androidx.compose.ui.graphics.Color,
    val iconVector: ImageVector,
    val verdictLabel: String,
    val supportingText: String,
    val section1Header: String,
    val section1HeaderColor: androidx.compose.ui.graphics.Color,
    val section1PrimaryText: String,
    val section1PrimaryColor: androidx.compose.ui.graphics.Color,
    val section1ChipText: String?,
    val section1ChipBgColor: androidx.compose.ui.graphics.Color,
    val section1ChipTextColor: androidx.compose.ui.graphics.Color,
    val section2Header: String,
    val section2Content: String
)
