package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CurbNote
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbNoteDialog
import com.example.ui.components.CurbNoteSection
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.components.CurbVerdictBadge
import com.example.ui.components.ParkingVerdictCard
import com.example.ui.components.IndividualSignDetailSheet
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
import com.example.ui.theme.RadiusNested
import java.io.File

import com.example.ui.components.StartSessionConfirmationSheet
import com.example.util.ParkingTimerCalculator
import com.example.util.ParkingTimerConfig

@Composable
fun ParkingDetailsScreen(
    scanResult: ScanResult,
    note: CurbNote? = null,
    isPro: Boolean = false,
    onSaveNote: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {},
    onStartParkingSession: (durationMinutes: Int, allowedUntilTime: String, timerBasis: String, ruleSummary: String) -> Unit,
    onReportIssue: () -> Unit,
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
            .background(CurbBackground)
            .statusBarsPadding()
            .testTag("parking_details_screen")
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CurbOnSurface
                    )
                }
                Text(
                    text = "Parking details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
            }

            IconButton(
                onClick = {
                    if (isPro) {
                        exportSingleScanDetails(context, scanResult)
                    } else {
                        showExportProSheet = true
                    }
                },
                modifier = Modifier.testTag("details_export_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Export Spot",
                    tint = CurbBlack
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // HEADER & SUBTITLE
            item {
                Text(
                    text = "Here’s the complete evidence and rule analysis for this spot.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = CurbOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION A: COMPACT DECISION CONTEXT
            item {
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = when (scanResult.verdict) {
                        ScanVerdict.ALLOWED -> CurbSuccessContainer
                        ScanVerdict.RESTRICTED -> CurbErrorContainer
                        ScanVerdict.AMBIGUOUS -> CurbWarningContainer
                    },
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
                                color = CurbOnSurface
                            )
                            if (scanResult.verdict == ScanVerdict.ALLOWED && scanResult.timeRemaining.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${scanResult.timeRemaining} remaining",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CurbSuccess
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // SECTION B: PRIMARY EVIDENCE — SIGNS CURB READ
            item {
                Text(
                    text = if (scanResult.detectedSigns.isNotEmpty()) "Signs Curb read (${scanResult.detectedSigns.size})" else "Physical sign evidence",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (scanResult.detectedSigns.isNotEmpty()) {
                    Text(
                        text = "Tap any sign to view its sharp crop and detailed breakdown.",
                        fontSize = 12.sp,
                        color = CurbOnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (scanResult.detectedSigns.isNotEmpty()) {
                itemsIndexed(scanResult.detectedSigns) { index, sign ->
                    Box(modifier = Modifier.padding(bottom = 12.dp)) {
                        CurbCard(
                            cornerRadius = RadiusNested,
                            backgroundColor = CurbSurface,
                            modifier = Modifier.clickable {
                                selectedSignForDetail = sign
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Real Cropped Sign Plate Thumbnail
                                if (!sign.croppedImageUri.isNullOrBlank() && File(sign.croppedImageUri).exists()) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 84.dp, height = 84.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CurbSurfaceVariant),
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
                                            .size(width = 84.dp, height = 84.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CurbSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CropFree,
                                            contentDescription = null,
                                            tint = CurbOnSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (sign.id.isNotBlank()) "SIGN ${sign.id.uppercase()}" else "SIGN ${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CurbOnSurfaceVariant
                                        )

                                        val (badgeText, badgeBg, badgeFg) = when {
                                            sign.isUncertain -> Triple("Uncertain", CurbWarningContainer, CurbWarning)
                                            sign.isRestrictingNow -> Triple("Active Restriction", CurbErrorContainer, CurbError)
                                            !sign.isRestrictingNow -> Triple("Inactive Schedule", CurbSurfaceVariant, CurbOnSurfaceVariant)
                                            else -> Triple("Individual Rule", CurbSurfaceVariant, CurbOnSurfaceVariant)
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

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = sign.title.ifBlank { "Parking Regulation" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )

                                    val daysHours = sign.applicableDaysHours.ifBlank { sign.subtitle }
                                    if (daysHours.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = daysHours,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = CurbOnSurfaceVariant
                                        )
                                    }

                                    val mainRule = sign.restrictions.ifBlank { sign.ruleText }
                                    if (mainRule.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = mainRule,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = CurbOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    CurbCard(
                        cornerRadius = RadiusCard,
                        backgroundColor = CurbSurface
                    ) {
                        Text(
                            text = "No clear physical sign plates could be extracted from this image. Please retake the photo with direct alignment and good lighting.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = CurbOnSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // SECTION C: WHY THIS DECISION (RULE SYNTHESIS)
            item {
                Text(
                    text = "Why this decision",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
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

                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CurbOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Rule Synthesis",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbOnSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = synthesisText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = CurbOnSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // SECTION D: COMPLETE PARKING RULES (STRICTLY NO FALLBACKS)
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
                        color = CurbOnSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CurbCard(
                        cornerRadius = RadiusCard,
                        backgroundColor = CurbSurface
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
                                        color = CurbOnSurface,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }

                            if (activeRulesList.isNotEmpty() && inactiveSignsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(CurbSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (inactiveSignsList.isNotEmpty()) {
                                Text(
                                    text = "INACTIVE SCHEDULES & FUTURE RULES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = CurbOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                inactiveSignsList.forEach { sign ->
                                    val scheduleText = sign.applicableDaysHours.ifBlank { sign.subtitle }
                                    val ruleText = sign.restrictions.ifBlank { sign.ruleText }
                                    Text(
                                        text = "• ${sign.title}: $scheduleText ($ruleText)",
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = CurbOnSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // SECTION E: ABOUT THIS SPOT (ONLY KNOWN NON-EMPTY FIELDS)
            val hasLocation = scanResult.locationName.isNotBlank() && scanResult.locationName != "Unknown Location"
            val hasPayment = scanResult.paymentInfo.isNotBlank() && scanResult.paymentInfo != "Unknown" && scanResult.paymentInfo != "N/A"
            val hasVehicle = scanResult.vehicleApplicability.isNotBlank() && scanResult.vehicleApplicability != "Unknown"

            if (hasLocation || hasPayment || hasVehicle) {
                item {
                    Text(
                        text = "About this spot",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CurbCard(
                        cornerRadius = RadiusCard,
                        backgroundColor = CurbSurface
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
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // SECTION E: CURB AI EXPLANATION
            item {
                Text(
                    text = "Curb AI explanation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface
                ) {
                    Text(
                        text = scanResult.explanation,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = CurbOnSurface,
                        modifier = Modifier.padding(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // PERSONAL NOTE
            item {
                Text(
                    text = "Personal note",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

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

            // REPORT ISSUE LINK
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
                            color = CurbOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // BOTTOM ACTION
        if (scanResult.verdict == ScanVerdict.ALLOWED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
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
                        testTag = "details_start_session_unrestricted_button"
                    )
                } else {
                    CurbPrimaryButton(
                        text = "Start parking session (${timerConfig.formattedDuration})",
                        onClick = {
                            showStartConfirmationSheet = true
                        },
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
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CurbBlack,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = CurbOnSurfaceVariant
            )
        }
    }
}
