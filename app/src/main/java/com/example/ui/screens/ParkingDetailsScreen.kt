package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CurbNote
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbNoteDialog
import com.example.ui.components.CurbNoteSection
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbSecondaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.components.CurbVerdictBadge
import com.example.ui.components.IndividualSignDetailSheet
import com.example.ui.components.StartSessionConfirmationSheet
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.CurbWarningContainer
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.util.ParkingTimerCalculator
import java.io.File

@Composable
fun ParkingDetailsScreen(
    scanResult: ScanResult,
    note: CurbNote? = null,
    isPro: Boolean = false,
    onSaveNote: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {},
    onStartParkingSession: (durationMinutes: Int, allowedUntilTime: String, timerBasis: String, ruleSummary: String) -> Unit,
    onReportIssue: () -> Unit,
    onAskAboutThisSign: () -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showExportProSheet by remember { mutableStateOf(false) }
    var showNotesProSheet by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showStartConfirmationSheet by remember { mutableStateOf(false) }
    var selectedSignForDetail by remember { mutableStateOf<DetectedSign?>(null) }

    val timerConfig = remember(scanResult) {
        ParkingTimerCalculator.calculateConfig(scanResult)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .testTag("parking_details_screen")
    ) {
        // 1. TOP APP BAR (Compact Bento Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Circular Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BentoWhite)
                        .border(1.dp, BentoBorder, CircleShape)
                        .clickable { onBack() }
                        .testTag("details_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BentoTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Parking details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Circular Export Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BentoWhite)
                    .border(1.dp, BentoBorder, CircleShape)
                    .clickable {
                        if (isPro) {
                            exportSingleScanDetails(context, scanResult)
                        } else {
                            showExportProSheet = true
                        }
                    }
                    .testTag("details_export_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Export Spot",
                    tint = BentoTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 2. INTRO CONTEXT
            item {
                Text(
                    text = "Everything Curb read, organized in one place.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = BentoTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. DECISION CONTEXT HERO SUMMARY
            item {
                Card(
                    shape = RoundedCornerShape(RadiusCard),
                    colors = CardDefaults.cardColors(
                        containerColor = when (scanResult.verdict) {
                            ScanVerdict.ALLOWED -> CurbSuccessContainer
                            ScanVerdict.RESTRICTED -> CurbErrorContainer
                            ScanVerdict.AMBIGUOUS -> CurbWarningContainer
                        }
                    ),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CurbVerdictBadge(verdict = scanResult.verdict)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (scanResult.verdict) {
                                    ScanVerdict.ALLOWED -> if (scanResult.allowedUntilTime.isNotBlank()) "Allowed until ${scanResult.allowedUntilTime}" else "Parking permitted"
                                    ScanVerdict.RESTRICTED -> scanResult.parkingRules.firstOrNull() ?: "Active restriction in effect"
                                    ScanVerdict.AMBIGUOUS -> "Signage unreadable or ambiguous"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            if (scanResult.verdict == ScanVerdict.ALLOWED && scanResult.timeRemaining.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${scanResult.timeRemaining} remaining",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CurbSuccess
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. PRIMARY EVIDENCE — SIGNS CURB READ (HEADER)
            item {
                Text(
                    text = if (scanResult.detectedSigns.isNotEmpty()) "Signs Curb read (${scanResult.detectedSigns.size})" else "Physical sign evidence",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (scanResult.detectedSigns.isNotEmpty()) {
                    Text(
                        text = "Tap any sign to view its sharp crop and detailed breakdown.",
                        fontSize = 12.sp,
                        color = BentoTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5. SIGN EVIDENCE BENTO GRID CARDS
            if (scanResult.detectedSigns.isNotEmpty()) {
                val signPairs = scanResult.detectedSigns.chunked(2)
                signPairs.forEachIndexed { pairIndex, pair ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEachIndexed { itemInPairIndex, sign ->
                                val overallIndex = pairIndex * 2 + itemInPairIndex
                                SignEvidenceBentoCard(
                                    sign = sign,
                                    index = overallIndex,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sign_evidence_card_$overallIndex"),
                                    onClick = { selectedSignForDetail = sign }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No clear physical sign plates could be extracted from this image. Please retake the photo with direct alignment and good lighting.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = BentoTextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 6. WHY THIS DECISION (RULE SYNTHESIS)
            item {
                Text(
                    text = "Why this decision",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val activeSigns = scanResult.detectedSigns.filter { it.isRestrictingNow }
                val inactiveSigns = scanResult.detectedSigns.filter { !it.isRestrictingNow && !it.isUncertain }
                val synthesisText = when (scanResult.verdict) {
                    ScanVerdict.RESTRICTED -> {
                        if (activeSigns.isNotEmpty()) {
                            val signNames = activeSigns.joinToString(", ") { "Sign ${it.id.ifBlank { "plate" }}" }
                            "$signNames imposes an active restriction during this current time window, taking precedence over other posted schedules."
                        } else {
                            "An active street rule or municipal regulation prohibits stopping or parking during the current time window."
                        }
                    }
                    ScanVerdict.ALLOWED -> {
                        if (inactiveSigns.isNotEmpty()) {
                            "Posted restrictions (such as street cleaning or peak hours) are inactive right now. Permissive rules apply."
                        } else {
                            "Signage permits parking under the posted schedule with no active prohibition at this time."
                        }
                    }
                    ScanVerdict.AMBIGUOUS -> {
                        "Physical signage is faded, partially obscured, or conflicting. Check physical street signs before leaving your vehicle."
                    }
                }

                Card(
                    shape = RoundedCornerShape(RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BentoTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "RULE SYNTHESIS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = BentoTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = synthesisText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = BentoTextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 7. COMPLETE PARKING RULES
            val activeRulesList = scanResult.parkingRules.filter {
                scanResult.verdict == ScanVerdict.RESTRICTED || !it.contains("Inactive", ignoreCase = true)
            }
            val inactiveSignsList = scanResult.detectedSigns.filter { !it.isRestrictingNow && !it.isUncertain }

            if (activeRulesList.isNotEmpty() || inactiveSignsList.isNotEmpty()) {
                item {
                    Text(
                        text = "Complete parking rules",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (activeRulesList.isNotEmpty()) {
                                Text(
                                    text = "CURRENT ACTIVE CONDITIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (scanResult.verdict == ScanVerdict.RESTRICTED) CurbError else CurbSuccess
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                activeRulesList.forEach { rule ->
                                    Text(
                                        text = "• $rule",
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = BentoTextPrimary,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }

                            if (activeRulesList.isNotEmpty() && inactiveSignsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BentoBorder, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (inactiveSignsList.isNotEmpty()) {
                                Text(
                                    text = "INACTIVE SCHEDULES & FUTURE RULES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = BentoTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                inactiveSignsList.forEach { sign ->
                                    val scheduleText = sign.applicableDaysHours.ifBlank { sign.subtitle }
                                    val ruleText = sign.restrictions.ifBlank { sign.ruleText }
                                    Text(
                                        text = "• ${sign.title}: $scheduleText ($ruleText)",
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = BentoTextSecondary,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 8. ABOUT THIS SPOT
            val hasLocation = scanResult.locationName.isNotBlank() && scanResult.locationName != "Unknown Location"
            val hasPayment = scanResult.paymentInfo.isNotBlank() && scanResult.paymentInfo != "Unknown" && scanResult.paymentInfo != "N/A"
            val hasVehicle = scanResult.vehicleApplicability.isNotBlank() && scanResult.vehicleApplicability != "Unknown"

            if (hasLocation || hasPayment || hasVehicle) {
                item {
                    Text(
                        text = "About this spot",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (hasLocation) {
                                SpotInfoRow(
                                    icon = Icons.Default.LocationOn,
                                    label = "Location",
                                    value = if (scanResult.cityState.isNotBlank()) "${scanResult.locationName}, ${scanResult.cityState}" else scanResult.locationName
                                )
                            }
                            if (hasPayment) {
                                if (hasLocation) Spacer(modifier = Modifier.height(12.dp))
                                SpotInfoRow(
                                    icon = Icons.Default.CreditCard,
                                    label = scanResult.zoneType.ifBlank { "Payment" },
                                    value = scanResult.paymentInfo
                                )
                            }
                            if (hasVehicle) {
                                if (hasLocation || hasPayment) Spacer(modifier = Modifier.height(12.dp))
                                SpotInfoRow(
                                    icon = Icons.Default.DirectionsCar,
                                    label = "Vehicle Applicability",
                                    value = scanResult.vehicleApplicability
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 9. CURB EXPLANATION
            item {
                Text(
                    text = "Curb explanation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scanResult.explanation,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = BentoTextPrimary,
                        modifier = Modifier.padding(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                CurbSecondaryButton(
                    text = "ASK ABOUT THIS SIGN",
                    onClick = onAskAboutThisSign,
                    testTag = "contextual_copilot_button"
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 10. PERSONAL NOTE
            item {
                Text(
                    text = "Personal note",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                CurbNoteSection(
                    note = note,
                    isPro = isPro,
                    onAddOrEditNote = {
                        showNoteDialog = true
                    },
                    onProLocked = {
                        showNotesProSheet = true
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 11. REPORT ISSUE LINK
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onReportIssue,
                        modifier = Modifier.testTag("report_issue_button")
                    ) {
                        Text(
                            text = "Need to report an issue with this spot?",
                            color = BentoTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 12. BOTTOM STICKY ACTION
        if (scanResult.verdict == ScanVerdict.ALLOWED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BentoCanvas)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (timerConfig.isUnrestricted) {
                    CurbPrimaryButton(
                        text = "Parked (No time limit)",
                        onClick = {
                            onStartParkingSession(
                                0,
                                "No time limit",
                                "Unrestricted parking",
                                timerConfig.ruleSummary
                            )
                        },
                        backgroundColor = BentoPrimaryDark,
                        testTag = "details_start_session_unrestricted_button"
                    )
                } else {
                    CurbPrimaryButton(
                        text = "Start parking session (${timerConfig.formattedDuration})",
                        onClick = {
                            showStartConfirmationSheet = true
                        },
                        backgroundColor = BentoPrimaryDark,
                        testTag = "details_start_session_button"
                    )
                }
            }
        }
    }

    if (showStartConfirmationSheet) {
        StartSessionConfirmationSheet(
            locationName = scanResult.locationName,
            timerConfig = timerConfig,
            onConfirmStartTimer = { durationMins, allowedUntil, basis, rules ->
                showStartConfirmationSheet = false
                onStartParkingSession(durationMins, allowedUntil, basis, rules)
            },
            onDismiss = {
                showStartConfirmationSheet = false
            }
        )
    }

    if (showExportProSheet) {
        CurbProFeatureBottomSheet(
            title = "Export with Curb Pro",
            supportingText = "Keep and share your parking records whenever you need them.",
            icon = Icons.Outlined.FileDownload,
            onGetPro = onUpgradeToPro,
            onDismiss = { showExportProSheet = false }
        )
    }

    if (showNotesProSheet) {
        CurbProFeatureBottomSheet(
            title = "Save Notes with Curb Pro",
            supportingText = "Keep personal reminders with your saved places and parking scans.",
            icon = Icons.Default.Edit,
            onGetPro = onUpgradeToPro,
            onDismiss = { showNotesProSheet = false }
        )
    }

    if (showNoteDialog) {
        CurbNoteDialog(
            initialText = note?.text ?: "",
            isEditing = note != null,
            onSave = { newText ->
                onSaveNote(newText)
                showNoteDialog = false
            },
            onDelete = if (note != null) {
                {
                    onDeleteNote()
                    showNoteDialog = false
                }
            } else null,
            onDismiss = {
                showNoteDialog = false
            }
        )
    }

    // INDIVIDUAL SIGN DETAIL SHEET
    selectedSignForDetail?.let { sign ->
        IndividualSignDetailSheet(
            sign = sign,
            overallVerdict = scanResult.verdict,
            onDismiss = { selectedSignForDetail = null }
        )
    }
}

@Composable
private fun SignEvidenceBentoCard(
    sign: DetectedSign,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(RadiusCard),
        colors = CardDefaults.cardColors(containerColor = BentoWhite),
        border = BorderStroke(1.dp, BentoBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: ID + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sign.id.isNotBlank()) "SIGN ${sign.id.uppercase()}" else "SIGN ${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextSecondary
                )

                val (badgeText, badgeBg, badgeFg) = when {
                    sign.isUncertain -> Triple("Uncertain", CurbWarningContainer, CurbWarning)
                    sign.isRestrictingNow -> Triple("Active", CurbErrorContainer, CurbError)
                    !sign.isRestrictingNow -> Triple("Inactive", BentoSand, BentoTextSecondary)
                    else -> Triple("Rule", BentoSand, BentoTextSecondary)
                }

                Surface(
                    shape = RoundedCornerShape(RadiusChip),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeFg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thumbnail Crop or Placeholder
            if (!sign.croppedImageUri.isNullOrBlank() && File(sign.croppedImageUri).exists()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(RadiusHero - 12.dp))
                        .background(BentoSand),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(sign.croppedImageUri),
                        contentDescription = "Sign crop ${index + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(RadiusHero - 12.dp))
                        .background(BentoSand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = null,
                        tint = BentoTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = sign.title.ifBlank { "Parking Regulation" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val daysHours = sign.applicableDaysHours.ifBlank { sign.subtitle }
            if (daysHours.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = daysHours,
                    fontSize = 11.sp,
                    color = BentoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val mainRule = sign.restrictions.ifBlank { sign.ruleText }
            if (mainRule.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mainRule,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = BentoTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETAILS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoPrimaryDark,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = BentoPrimaryDark,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun exportSingleScanDetails(context: Context, scan: ScanResult) {
    val builder = StringBuilder()
    builder.append("CURB SPOT REPORT: ${scan.locationName}\n")
    builder.append("City/State: ${scan.cityState}\n")
    builder.append("Verdict: ${scan.verdict.name} (${scan.verdict.displayTitle})\n")
    builder.append("Allowed Until: ${scan.allowedUntilTime} (${scan.timeRemaining})\n")
    builder.append("Zone Type: ${scan.zoneType}\n")
    builder.append("Payment: ${scan.paymentInfo}\n")
    builder.append("Vehicle Rule: ${scan.vehicleApplicability}\n\n")
    if (scan.parkingRules.isNotEmpty()) {
        builder.append("Parking Rules:\n")
        scan.parkingRules.forEach { rule ->
            builder.append("• $rule\n")
        }
        builder.append("\n")
    }
    builder.append("AI Explanation:\n${scan.explanation}\n")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Curb Parking Spot: ${scan.locationName}")
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Parking Spot Details"))
}

@Composable
private fun SpotInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BentoSand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BentoPrimaryDark,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = BentoTextSecondary
            )
        }
    }
}
