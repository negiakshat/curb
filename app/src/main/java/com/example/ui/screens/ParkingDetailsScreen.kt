package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.CurbVerdictBadge
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite

@Composable
fun ParkingDetailsScreen(
    scanResult: ScanResult,
    note: CurbNote? = null,
    isPro: Boolean = false,
    onSaveNote: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {},
    onStartParkingSession: () -> Unit,
    onReportIssue: () -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showExportProSheet by remember { mutableStateOf(false) }
    var showNotesProSheet by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

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
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // HEADER & SUBTITLE
            item {
                Text(
                    text = "Here’s everything we found about this spot.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = CurbOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // SECTION 1: PARKING STATUS / TIME
            item {
                CurbCard(
                    cornerRadius = 24.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (scanResult.verdict == ScanVerdict.ALLOWED) "Allowed until" else "Parking status",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = CurbOnSurfaceVariant
                            )
                            CurbVerdictBadge(verdict = scanResult.verdict)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (scanResult.verdict == ScanVerdict.ALLOWED) scanResult.allowedUntilTime else scanResult.verdict.displayTitle,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )

                        if (scanResult.verdict == ScanVerdict.ALLOWED) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scanResult.timeRemaining,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CurbSuccess
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION 2: PARKING RULES BREAKDOWN
            item {
                Text(
                    text = "Parking rules",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        scanResult.parkingRules.forEachIndexed { idx, rule ->
                            val parts = rule.split(":")
                            if (parts.size >= 2) {
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = parts[0].trim(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = parts.subList(1, parts.size).joinToString(":").trim(),
                                        fontSize = 14.sp,
                                        color = CurbOnSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = rule,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = CurbOnSurface,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            if (idx < scanResult.parkingRules.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(CurbSurfaceVariant)
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION 3: ABOUT THIS SPOT
            item {
                Text(
                    text = "About this spot",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        SpotInfoRow(
                            icon = Icons.Default.LocationOn,
                            label = "Location",
                            value = if (scanResult.cityState.isNotBlank()) "${scanResult.locationName}, ${scanResult.cityState}" else scanResult.locationName
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SpotInfoRow(
                            icon = Icons.Default.CreditCard,
                            label = scanResult.zoneType,
                            value = scanResult.paymentInfo
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SpotInfoRow(
                            icon = Icons.Default.DirectionsCar,
                            label = "Vehicle Type",
                            value = scanResult.vehicleApplicability
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // SECTION 3.5: PERSONAL NOTE
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

            // SECTION 4: SIGNS SCANNED
            if (scanResult.detectedSigns.isNotEmpty()) {
                item {
                    Text(
                        text = "Signs scanned",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scanResult.detectedSigns) { sign ->
                            CurbCard(
                                cornerRadius = 18.dp,
                                backgroundColor = CurbSurfaceVariant,
                                modifier = Modifier.width(220.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CurbBlack
                                    ) {
                                        Text(
                                            text = "SIGN ${sign.id}",
                                            color = CurbWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = sign.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sign.subtitle,
                                        fontSize = 12.sp,
                                        color = CurbOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // SECTION 5: CURB AI EXPLANATION
            item {
                Text(
                    text = "Curb AI explanation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurfaceVariant
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

            // OPTIONAL REPORT ISSUE LINK
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

        // BOTTOM ACTION (Start parking session if allowed)
        if (scanResult.verdict == ScanVerdict.ALLOWED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                CurbPrimaryButton(
                    text = "Start parking session",
                    onClick = onStartParkingSession,
                    testTag = "details_start_session_button"
                )
            }
        }
    }

    if (showExportProSheet) {
        CurbProFeatureBottomSheet(
            title = "Export with Curb Pro",
            supportingText = "Keep and share your parking records whenever you need them.",
            icon = Icons.Default.FileDownload,
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
