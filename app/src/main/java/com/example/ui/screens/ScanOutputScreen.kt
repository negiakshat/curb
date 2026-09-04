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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import java.io.File
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbNoteDialog
import com.example.ui.components.CurbNoteSection
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOutputScreen(
    scanResult: ScanResult,
    note: CurbNote? = null,
    isPro: Boolean = false,
    onSaveNote: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {},
    onViewDetails: () -> Unit,
    onStartParkingSession: () -> Unit,
    onAskCurb: () -> Unit,
    onRetake: () -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showExportProSheet by remember { mutableStateOf(false) }
    var showNotesProSheet by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedSignForPreview by remember { mutableStateOf<DetectedSign?>(null) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            // PERSONAL NOTE SECTION (CURB PRO)
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
                Spacer(modifier = Modifier.height(18.dp))
            }

            // SCANNED SIGNS LIST (Shows localized signs and cropped plates)
            if (scanResult.detectedSigns.isNotEmpty()) {
                item {
                    Text(
                        text = "Scanned signs (${scanResult.detectedSigns.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                itemsIndexed(scanResult.detectedSigns) { index, sign ->
                    Box(modifier = Modifier.padding(bottom = 10.dp)) {
                        CurbCard(
                            cornerRadius = RadiusNested,
                            backgroundColor = CurbSurface,
                            modifier = Modifier.clickable {
                                selectedSignForPreview = sign
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Real Cropped Sign Plate Thumbnail
                                if (!sign.croppedImageUri.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 80.dp, height = 74.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CurbSurfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = File(sign.croppedImageUri),
                                            contentDescription = "Sign crop ${index + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 64.dp)
                                            .clip(RoundedCornerShape(10.dp))
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
                                            text = "SIGN ${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CurbOnSurfaceVariant
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(RadiusChip),
                                            color = if (sign.isRestrictingNow) CurbErrorContainer else CurbSuccessContainer
                                        ) {
                                            Text(
                                                text = if (sign.isRestrictingNow) "Restricting now" else "Permitted",
                                                color = if (sign.isRestrictingNow) CurbError else CurbSuccess,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = sign.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = sign.subtitle,
                                        fontSize = 12.sp,
                                        color = CurbOnSurfaceVariant
                                    )

                                    if (sign.ruleText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = sign.ruleText,
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

    // SIGN CROP INSPECTION BOTTOM SHEET
    selectedSignForPreview?.let { sign ->
        val inspectSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedSignForPreview = null },
            sheetState = inspectSheetState,
            containerColor = CurbSurface,
            shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sign Detail Analysis",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(RadiusChip),
                        color = if (sign.isRestrictingNow) CurbErrorContainer else CurbSuccessContainer
                    ) {
                        Text(
                            text = if (sign.isRestrictingNow) "Restricting now" else "Permitted",
                            color = if (sign.isRestrictingNow) CurbError else CurbSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High-resolution sign plate crop preview
                if (!sign.croppedImageUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = sign.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sign.subtitle,
                            fontSize = 14.sp,
                            color = CurbOnSurfaceVariant
                        )

                        if (sign.ruleText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = sign.ruleText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = CurbOnSurface
                            )
                        }

                        if (sign.rawText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Transcribed text:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sign.rawText,
                                fontSize = 12.sp,
                                color = CurbOnSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                CurbPrimaryButton(
                    text = "Close",
                    onClick = { selectedSignForPreview = null },
                    testTag = "close_sign_inspection_button"
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
    builder.append("AI Explanation:\n${scan.explanation}\n")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Curb Scan: ${scan.locationName}")
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Export Parking Spot Details"))
}
