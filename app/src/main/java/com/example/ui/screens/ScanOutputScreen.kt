package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurbNote
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbNoteDialog
import com.example.ui.components.CurbNoteSection
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.components.CurbSecondaryButton
import com.example.ui.components.CurbTertiaryButton
import com.example.ui.components.ParkingVerdictCard
import com.example.ui.components.StartSessionConfirmationSheet
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.RadiusCard
import com.example.util.ParkingTimerCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOutputScreen(
    scanResult: ScanResult,
    note: CurbNote? = null,
    savedPlaces: List<com.example.data.model.SavedPlace> = emptyList(),
    rescanTargetPlace: com.example.data.model.SavedPlace? = null,
    isPro: Boolean = false,
    onSaveNote: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {},
    onSavePlace: (com.example.data.model.SavedPlace, (com.example.data.repository.SavePlaceResult) -> Unit) -> Unit = { place, callback ->
        callback(com.example.data.repository.SavePlaceResult.Success(place.copy(id = 1L)))
    },
    onViewSavedPlaces: () -> Unit = {},
    onViewDetails: () -> Unit,
    onStartParkingSession: (durationMinutes: Int, allowedUntilTime: String, timerBasis: String, ruleSummary: String) -> Unit,
    onAskCurb: () -> Unit,
    onRetake: () -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showExportProSheet by remember { mutableStateOf(false) }
    var showNotesProSheet by remember { mutableStateOf(false) }
    var showSavedPlacesProSheet by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showStartConfirmationSheet by remember { mutableStateOf(false) }
    var showSaveSpotSheet by remember { mutableStateOf(false) }
    var savedPlaceForSuccessDialog by remember { mutableStateOf<com.example.data.model.SavedPlace?>(null) }
    var duplicateSavedSpot by remember { mutableStateOf<com.example.data.model.SavedPlace?>(null) }

    var isContentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(scanResult) {
        isContentVisible = true
    }

    val timerConfig = remember(scanResult) {
        ParkingTimerCalculator.calculateConfig(scanResult)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .testTag("scan_output_screen")
    ) {
        // TOP APP BAR (Bento Styling)
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
                        .testTag("scan_output_back_button"),
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
                    text = scanResult.locationName,
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
                            exportScanResult(context, scanResult)
                        } else {
                            showExportProSheet = true
                        }
                    }
                    .testTag("scan_output_export_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Export Scan",
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
            // 1. OVERALL PARKING DECISION (HERO BANNER)
            item {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(animationSpec = tween(350)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(350)
                    )
                ) {
                    Column {
                        ParkingVerdictCard(
                            scanResult = scanResult,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // 2. PARKING DETAILS SUMMARY CARD
            item {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(animationSpec = tween(450)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(450)
                    )
                ) {
                    Column {
                        ParkingDetailsCard(
                            scanResult = scanResult,
                            onViewDetails = onViewDetails
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // 3. PERSONAL NOTE SECTION
            item {
                Text(
                    text = "Personal note",
                    fontSize = 16.sp,
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
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // BOTTOM ACTIONS (Unified System per State)
        val hasUnresolvedAmbiguity = scanResult.detectedSigns.any { it.isUncertain } ||
                scanResult.explanation.contains("conflicting", ignoreCase = true) ||
                scanResult.explanation.contains("unclear", ignoreCase = true) ||
                scanResult.explanation.contains("unresolved", ignoreCase = true)

        val isRealScan = !scanResult.isDemo && scanResult.locationName != "Location unavailable"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BentoCanvas)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isRealScan) {
                CurbSecondaryButton(
                    text = if (rescanTargetPlace != null) "UPDATE THIS SPOT" else "SAVE THIS SPOT",
                    onClick = {
                        val canSaveFree = isPro || rescanTargetPlace != null || savedPlaces.size < 3
                        if (canSaveFree) {
                            showSaveSpotSheet = true
                        } else {
                            showSavedPlacesProSheet = true
                        }
                    },
                    testTag = "save_this_spot_button"
                )
            }

            when (scanResult.verdict) {
                ScanVerdict.ALLOWED -> {
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
                            testTag = "start_parking_session_unrestricted_button"
                        )
                    } else {
                        CurbPrimaryButton(
                            text = "Start parking session (${timerConfig.formattedDuration})",
                            onClick = {
                                showStartConfirmationSheet = true
                            },
                            backgroundColor = BentoPrimaryDark,
                            testTag = "start_parking_session_button"
                        )
                    }

                    CurbTertiaryButton(
                        text = "Retake scan",
                        onClick = onRetake,
                        testTag = "retake_scan_button"
                    )
                }
                ScanVerdict.RESTRICTED -> {
                    CurbPrimaryButton(
                        text = "Retake scan",
                        onClick = onRetake,
                        backgroundColor = BentoPrimaryDark,
                        testTag = "retake_scan_button"
                    )
                }
                ScanVerdict.AMBIGUOUS -> {
                    CurbPrimaryButton(
                        text = "Retake scan",
                        onClick = onRetake,
                        backgroundColor = BentoPrimaryDark,
                        testTag = "retake_scan_button"
                    )
                }
            }
        }
    }

    if (showSaveSpotSheet) {
        com.example.ui.components.SaveSpotBottomSheet(
            scanResult = scanResult,
            rescanTargetPlace = rescanTargetPlace,
            onDismiss = { showSaveSpotSheet = false },
            onSave = { savedPlace ->
                onSavePlace(savedPlace) { result ->
                    when (result) {
                        is com.example.data.repository.SavePlaceResult.Success -> {
                            savedPlaceForSuccessDialog = result.savedPlace
                        }
                        is com.example.data.repository.SavePlaceResult.Duplicate -> {
                            duplicateSavedSpot = result.existingPlace
                        }
                        is com.example.data.repository.SavePlaceResult.Error -> {
                            android.widget.Toast.makeText(context, result.message.ifBlank { "Could not save spot. Please try again." }, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                showSaveSpotSheet = false
            }
        )
    }

    savedPlaceForSuccessDialog?.let { savedSpot ->
        com.example.ui.components.SavedSpotSuccessDialog(
            placeName = savedSpot.name,
            onViewSavedPlaces = {
                savedPlaceForSuccessDialog = null
                onViewSavedPlaces()
            },
            onDone = {
                savedPlaceForSuccessDialog = null
            }
        )
    }

    duplicateSavedSpot?.let { _ ->
        com.example.ui.components.DuplicateSavedSpotDialog(
            onViewSavedSpot = {
                duplicateSavedSpot = null
                onViewSavedPlaces()
            },
            onDone = {
                duplicateSavedSpot = null
            }
        )
    }

    if (showSavedPlacesProSheet) {
        CurbProFeatureBottomSheet(
            title = "Save Unlimited Parking Spots",
            supportingText = "Save as many parking locations and sign intelligence records as you need with Curb Pro.",
            icon = androidx.compose.material.icons.Icons.Default.Edit,
            onGetPro = onUpgradeToPro,
            onDismiss = { showSavedPlacesProSheet = false }
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
}

private fun exportScanResult(context: Context, scan: ScanResult) {
    val builder = StringBuilder()
    builder.append("CURB SPOT REPORT: ${scan.locationName}\n")
    builder.append("Verdict: ${scan.verdict.name} (${scan.verdict.displayTitle})\n")
    builder.append("Allowed Until: ${scan.allowedUntilTime}\n")
    builder.append("Zone Type: ${scan.zoneType}\n")
    builder.append("Payment: ${scan.paymentInfo}\n\n")
    if (scan.parkingRules.isNotEmpty()) {
        builder.append("Parking Rules:\n")
        scan.parkingRules.forEach { rule ->
            builder.append("• $rule\n")
        }
        builder.append("\n")
    }
    builder.append("Curb Overview:\n${scan.explanation}\n")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Curb Scan: ${scan.locationName}")
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Export Parking Spot Details"))
}

@Composable
private fun ParkingDetailsCard(
    scanResult: ScanResult,
    onViewDetails: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(RadiusCard),
        colors = CardDefaults.cardColors(containerColor = BentoWhite),
        border = BorderStroke(1.dp, BentoBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parking_details_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BentoSand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BentoPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "PARKING DETAILS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = BentoPrimaryDark
                )
            }

            // 1. CURRENT STATUS
            val statusText = when (scanResult.verdict) {
                ScanVerdict.ALLOWED -> if (scanResult.statusChipText.isNotBlank() && scanResult.statusChipText != "Signage unclear") scanResult.statusChipText else "Parking allowed under current rules"
                ScanVerdict.RESTRICTED -> if (scanResult.statusChipText.isNotBlank() && scanResult.statusChipText != "Signage unclear") scanResult.statusChipText else "No parking allowed at this time"
                ScanVerdict.AMBIGUOUS -> if (scanResult.statusChipText.isNotBlank()) scanResult.statusChipText else "Rule unclear — verify physical signage"
            }
            val statusColor = when (scanResult.verdict) {
                ScanVerdict.ALLOWED -> CurbSuccess
                ScanVerdict.RESTRICTED -> CurbError
                ScanVerdict.AMBIGUOUS -> CurbWarning
            }
            DetailSectionRow(
                label = "CURRENT STATUS",
                value = statusText,
                valueColor = statusColor
            )

            // 2. WHEN THIS RULE APPLIES
            val whenAppliesText = remember(scanResult) {
                val applicableFromSign = scanResult.detectedSigns
                    .map { it.applicableDaysHours }
                    .firstOrNull { it.isNotBlank() }
                if (!applicableFromSign.isNullOrBlank()) {
                    applicableFromSign
                } else {
                    scanResult.detectedSigns
                        .map { it.subtitle }
                        .firstOrNull { it.isNotBlank() && !it.contains("allowed", ignoreCase = true) && !it.contains("restricted", ignoreCase = true) }
                        .orEmpty()
                }
            }
            if (whenAppliesText.isNotBlank()) {
                DetailSectionRow(
                    label = "WHEN THIS RULE APPLIES",
                    value = whenAppliesText
                )
            }

            // 3. ALLOWED UNTIL
            val allowedUntilText = remember(scanResult) {
                if (scanResult.verdict == ScanVerdict.ALLOWED) {
                    val allowedTime = scanResult.allowedUntilTime
                    if (allowedTime.isNotBlank() && !allowedTime.contains("Verify physical signage", ignoreCase = true)) {
                        allowedTime
                    } else null
                } else null
            }
            if (!allowedUntilText.isNullOrBlank()) {
                DetailSectionRow(
                    label = "ALLOWED UNTIL",
                    value = allowedUntilText
                )
            }

            // 4. MAXIMUM STAY
            val durationSources = remember(scanResult) {
                buildList {
                    addAll(scanResult.parkingRules)
                    scanResult.detectedSigns.forEach { sign ->
                        if (sign.ruleText.isNotBlank()) add(sign.ruleText)
                        if (sign.restrictions.isNotBlank()) add(sign.restrictions)
                        if (sign.subtitle.isNotBlank()) add(sign.subtitle)
                        if (sign.title.isNotBlank()) add(sign.title)
                    }
                }
            }
            val maxStayText = remember(durationSources) {
                findExplicitDuration(durationSources)
            }
            if (!maxStayText.isNullOrBlank()) {
                DetailSectionRow(
                    label = "MAXIMUM STAY",
                    value = maxStayText
                )
            }

            // 4. PAYMENT
            if (scanResult.paymentInfo.isNotBlank()) {
                DetailSectionRow(
                    label = "PAYMENT",
                    value = scanResult.paymentInfo
                )
            }

            // 5. VEHICLE / PERMIT APPLICABILITY
            if (scanResult.vehicleApplicability.isNotBlank()) {
                DetailSectionRow(
                    label = "VEHICLE & PERMIT APPLICABILITY",
                    value = scanResult.vehicleApplicability
                )
            }

            // 6. KEY PARKING RULES
            val rules = remember(scanResult) {
                scanResult.parkingRules
                    .filter { it.isNotBlank() && !it.contains("No verified parking rule", ignoreCase = true) }
                    .distinct()
            }
            if (rules.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "KEY PARKING RULES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = BentoTextSecondary
                    )
                    rules.forEach { rule ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimaryDark
                            )
                            Text(
                                text = rule,
                                fontSize = 14.sp,
                                color = BentoTextPrimary,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // 7. WHY CURB DECIDED THIS
            if (scanResult.explanation.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "WHY CURB DECIDED THIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = BentoTextSecondary
                    )
                    Text(
                        text = scanResult.explanation,
                        fontSize = 13.sp,
                        color = BentoTextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            // 8. VIEW FULL EVIDENCE LINK
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BentoSand)
                    .clickable { onViewDetails() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("view_details_card"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = BentoPrimaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "View full evidence & details (${scanResult.detectedSigns.size} sign(s) read)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimaryDark
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BentoPrimaryDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailSectionRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = BentoTextPrimary
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = BentoTextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            lineHeight = 19.sp
        )
    }
}

private fun findExplicitDuration(sources: List<String>): String? {
    val durationRegex = Regex(
        """(?i)\b(?:max(?:imum)?\s+(?:stay\s+|limit\s+|parking\s+)?)?(\d+(?:\.\d+)?\s*(?:-\s*)?(?:hours?|hrs?|minutes?|mins?))\b"""
    )

    for (source in sources) {
        if (source.isBlank()) continue
        val match = durationRegex.find(source)
        if (match != null) {
            val matchedValue = match.groupValues[1].trim()
            if (matchedValue.isNotBlank()) {
                return formatDurationString(matchedValue)
            }
        }
    }
    return null
}

private fun formatDurationString(raw: String): String {
    val cleaned = raw.lowercase().trim()
    val numberMatch = Regex("""^(\d+(?:\.\d+)?)""").find(cleaned) ?: return raw
    val numStr = numberMatch.groupValues[1]
    val num = numStr.toDoubleOrNull() ?: 0.0

    return when {
        cleaned.contains("hour") || cleaned.contains("hr") -> {
            if (num == 1.0) "$numStr hour" else "$numStr hours"
        }
        cleaned.contains("minute") || cleaned.contains("min") -> {
            if (num == 1.0) "$numStr minute" else "$numStr minutes"
        }
        else -> raw
    }
}
