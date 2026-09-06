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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurbNote
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbNoteDialog
import com.example.ui.components.CurbNoteSection
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.components.CurbSecondaryButton
import com.example.ui.components.ParkingVerdictCard
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.RadiusCard

import com.example.ui.components.StartSessionConfirmationSheet
import com.example.util.ParkingTimerCalculator
import com.example.util.ParkingTimerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOutputScreen(
    scanResult: ScanResult,
    note: CurbNote? = null,
    isPro: Boolean = false,
    onSaveNote: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {},
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
    var showNoteDialog by remember { mutableStateOf(false) }
    var showStartConfirmationSheet by remember { mutableStateOf(false) }

    val timerConfig = remember(scanResult) {
        ParkingTimerCalculator.calculateConfig(scanResult)
    }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
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
                    color = CurbOnSurface,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = {
                    if (isPro) {
                        exportScanResult(context, scanResult)
                    } else {
                        showExportProSheet = true
                    }
                },
                modifier = Modifier.testTag("scan_output_export_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Export Scan",
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
            // 1. OVERALL PARKING DECISION (HERO BANNER)
            item {
                ParkingVerdictCard(
                    scanResult = scanResult,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 2. EVIDENCE & FULL DETAILS BANNER
            item {
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface,
                    modifier = Modifier
                        .clickable { onViewDetails() }
                        .testTag("view_details_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(CurbSurfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = CurbBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "View full evidence & details",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CurbOnSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${scanResult.detectedSigns.size} sign(s) read • Complete rule analysis",
                                    fontSize = 12.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View Details",
                            tint = CurbOnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 3. PERSONAL NOTE SECTION
            item {
                Text(
                    text = "Personal note",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            testTag = "start_parking_session_unrestricted_button"
                        )
                    } else {
                        CurbPrimaryButton(
                            text = "Start parking session (${timerConfig.formattedDuration})",
                            onClick = {
                                showStartConfirmationSheet = true
                            },
                            testTag = "start_parking_session_button"
                        )
                    }

                    if (hasUnresolvedAmbiguity) {
                        CurbSecondaryButton(
                            text = "Clarify with Curb AI",
                            onClick = onAskCurb,
                            testTag = "clarify_curb_ai_button"
                        )
                    } else {
                        CurbSecondaryButton(
                            text = "Retake scan",
                            onClick = onRetake,
                            testTag = "retake_scan_button"
                        )
                    }
                }
                ScanVerdict.RESTRICTED -> {
                    CurbPrimaryButton(
                        text = "Retake scan",
                        onClick = onRetake,
                        testTag = "retake_scan_button"
                    )
                    CurbSecondaryButton(
                        text = "Clarify with Curb AI",
                        onClick = onAskCurb,
                        testTag = "clarify_curb_ai_button"
                    )
                }
                ScanVerdict.AMBIGUOUS -> {
                    CurbPrimaryButton(
                        text = "Retake scan",
                        onClick = onRetake,
                        testTag = "retake_scan_button"
                    )
                    CurbSecondaryButton(
                        text = "Clarify with Curb AI",
                        onClick = onAskCurb,
                        testTag = "clarify_curb_ai_button"
                    )
                }
            }
        }
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
