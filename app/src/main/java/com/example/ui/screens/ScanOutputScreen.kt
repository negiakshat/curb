package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbSecondaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
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
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.CurbWhite

@Composable
fun ScanOutputScreen(
    scanResult: ScanResult,
    onViewDetails: () -> Unit,
    onStartParkingSession: () -> Unit,
    onAskCurb: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .testTag("scan_output_screen")
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("scan_output_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = scanResult.locationName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // VERDICT HEADER & ICON
            item {
                when (scanResult.verdict) {
                    ScanVerdict.ALLOWED -> {
                        // YES STATE HEADER
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(CurbSuccessContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CurbSuccess,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Yes, you can park here",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CurbOnSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "You can park here under the rules that apply right now.",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        }
                    }
                    ScanVerdict.RESTRICTED -> {
                        // NO STATE HEADER
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(CurbErrorContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = CurbError,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "No, parking is restricted",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CurbOnSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Active restrictions prohibit parking at this location right now.",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        }
                    }
                    ScanVerdict.AMBIGUOUS -> {
                        // AMBIGUOUS STATE HEADER
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(CurbWarningContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CurbWarning,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Rule unclear — Verify Locally",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CurbOnSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Contradictory or obscured signage detected. Please verify before parking.",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // STATUS CHIP
                Surface(
                    shape = RoundedCornerShape(RadiusChip),
                    color = CurbSurfaceVariant
                ) {
                    Text(
                        text = scanResult.statusChipText,
                        color = CurbOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // MAIN INFO CARD (For YES state)
            if (scanResult.verdict == ScanVerdict.ALLOWED) {
                item {
                    CurbCard(
                        cornerRadius = RadiusCard,
                        backgroundColor = CurbSurface
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Allowed until",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = CurbOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scanResult.allowedUntilTime,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbOnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${scanResult.timeRemaining} • Standard Zone",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CurbSuccess
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // PARKING RULES / RELEVANT RESTRICTION
            if (scanResult.verdict != ScanVerdict.AMBIGUOUS) {
                item {
                    Text(
                        text = if (scanResult.verdict == ScanVerdict.ALLOWED) "Parking rules" else "Active restrictions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CurbCard(
                        cornerRadius = RadiusCard,
                        backgroundColor = CurbSurface
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            scanResult.parkingRules.forEachIndexed { index, rule ->
                                Text(
                                    text = "• $rule",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = CurbOnSurface,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // CURB AI OVERVIEW / EXPLANATION SECTION
            item {
                Text(
                    text = "Curb AI overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = scanResult.explanation,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = CurbOnSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // SCANNED SIGNS LIST (For YES and NO states)
            if (scanResult.verdict != ScanVerdict.AMBIGUOUS && scanResult.detectedSigns.isNotEmpty()) {
                item {
                    Text(
                        text = "Scanned signs (${scanResult.detectedSigns.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(scanResult.detectedSigns) { sign ->
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        CurbCard(
                            cornerRadius = RadiusNested,
                            backgroundColor = CurbSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sign.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sign.subtitle,
                                        fontSize = 12.sp,
                                        color = CurbOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        // BOTTOM ACTIONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (scanResult.verdict) {
                ScanVerdict.ALLOWED -> {
                    CurbPrimaryButton(
                        text = "Start parking session",
                        onClick = onStartParkingSession,
                        testTag = "start_parking_session_button"
                    )
                    CurbSecondaryButton(
                        text = "View details",
                        onClick = onViewDetails,
                        testTag = "view_details_button"
                    )
                }
                ScanVerdict.RESTRICTED -> {
                    CurbPrimaryButton(
                        text = "View details",
                        onClick = onViewDetails,
                        testTag = "view_details_button"
                    )
                }
                ScanVerdict.AMBIGUOUS -> {
                    CurbPrimaryButton(
                        text = "Ask Curb AI",
                        onClick = onAskCurb,
                        testTag = "ask_curb_ai_button"
                    )
                    CurbSecondaryButton(
                        text = "Retake",
                        onClick = onRetake,
                        testTag = "retake_scan_button"
                    )
                }
            }
        }
    }
}
