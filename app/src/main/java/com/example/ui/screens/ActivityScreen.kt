package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbEmptyState
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.components.CurbSegmentedStatusBar
import com.example.ui.components.CurbVerdictBadge
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    scans: List<ScanResult>,
    isPro: Boolean = false,
    onScanClicked: (ScanResult) -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var showExportProSheet by remember { mutableStateOf(false) }
    var showHistoryProSheet by remember { mutableStateOf(false) }

    val allowedCount = remember(scans) { scans.count { it.verdict == ScanVerdict.ALLOWED } }
    val restrictedCount = remember(scans) { scans.count { it.verdict == ScanVerdict.RESTRICTED } }
    val ambiguousCount = remember(scans) { scans.count { it.verdict == ScanVerdict.AMBIGUOUS } }
    val totalScans = scans.size.coerceAtLeast(1)
    val clearRate = ((allowedCount.toFloat() / totalScans) * 100).toInt()

    val accessibleScans = if (isPro) scans else scans.take(5)
    val lockedScansCount = if (isPro) 0 else (scans.size - 5).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .testTag("activity_screen"),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP HEADER & EXPORT ACTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("activity_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = BentoTextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Activity",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Parking history & scan analytics",
                                fontSize = 12.sp,
                                color = BentoTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // EXPORT ACTION BUTTON
                    Surface(
                        shape = RoundedCornerShape(RadiusChip),
                        color = if (isPro) BentoSand else BentoSand.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusChip))
                            .clickable {
                                if (isPro) {
                                    exportScansAsReport(context, scans)
                                } else {
                                    showExportProSheet = true
                                }
                            }
                            .testTag("activity_export_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPro) Icons.Outlined.FileDownload else Icons.Default.Lock,
                                contentDescription = if (isPro) "Export" else "Export locked",
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Export",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimaryDark
                            )
                        }
                    }
                }
            }
        }

        // BENTO ANALYTICS DASHBOARD CARD
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusHero)),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Bento Metric Tile 1: Total Scans
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(RadiusCard),
                                colors = CardDefaults.cardColors(containerColor = BentoSand),
                                border = BorderStroke(1.dp, BentoBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "TOTAL SCANS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${scans.size}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPrimaryDark
                                    )
                                }
                            }

                            // Bento Metric Tile 2: Clear Rate
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(RadiusCard),
                                colors = CardDefaults.cardColors(containerColor = BentoPeach.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, BentoBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "CLEAR RATE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$clearRate%",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPrimaryDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "VERDICT BREAKDOWN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$allowedCount allowed • $restrictedCount restricted • $ambiguousCount unclear",
                                fontSize = 10.sp,
                                color = BentoTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        CurbSegmentedStatusBar(
                            allowedCount = allowedCount.coerceAtLeast(if (scans.isEmpty()) 1 else 0),
                            restrictedCount = restrictedCount,
                            ambiguousCount = ambiguousCount
                        )
                    }
                }
            }
        }

        // HISTORY SECTION HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    letterSpacing = (-0.3).sp
                )

                if (!isPro && scans.size > 5) {
                    Surface(
                        shape = RoundedCornerShape(RadiusChip),
                        color = BentoSand,
                        border = BorderStroke(1.dp, BentoBorder)
                    ) {
                        Text(
                            text = "Showing 5 of ${scans.size} (Free)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BentoTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // HISTORY BENTO CONTAINER CARD
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusHero)),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (scans.isEmpty()) {
                            CurbEmptyState(
                                title = "No scans yet",
                                subtitle = "Your parking scans will appear here."
                            )
                        } else {
                            accessibleScans.forEachIndexed { index, scan ->
                                ActivityScanRowItem(
                                    scan = scan,
                                    onClick = { onScanClicked(scan) }
                                )
                                if (index < accessibleScans.lastIndex || (!isPro && lockedScansCount > 0)) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = BentoBorder.copy(alpha = 0.6f),
                                        thickness = 1.dp
                                    )
                                }
                            }

                            // LOCKED HISTORY TILE INSIDE BENTO SURFACE FOR FREE USERS
                            if (!isPro && lockedScansCount > 0) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(RadiusCard))
                                        .clickable { showHistoryProSheet = true }
                                        .testTag("locked_scan_history_card"),
                                    shape = RoundedCornerShape(RadiusCard),
                                    colors = CardDefaults.cardColors(containerColor = BentoSand),
                                    border = BorderStroke(1.dp, BentoBorder),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(BentoPeach.copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = BentoPrimaryDark,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Older scan history ($lockedScansCount hidden)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoTextPrimary
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = "Unlock your complete parking history with Curb Pro.",
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                color = BentoTextSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(RadiusChip),
                                            color = BentoPrimaryDark
                                        ) {
                                            Text(
                                                text = "UNLOCK",
                                                color = BentoWhite,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // EXPORT PRO BOTTOM SHEET
    if (showExportProSheet) {
        CurbProFeatureBottomSheet(
            title = "Export with Curb Pro",
            supportingText = "Keep and share your parking records whenever you need them.",
            icon = Icons.Default.FileDownload,
            onGetPro = onUpgradeToPro,
            onDismiss = { showExportProSheet = false }
        )
    }

    // HISTORY PRO BOTTOM SHEET
    if (showHistoryProSheet) {
        CurbProFeatureBottomSheet(
            title = "Unlock Your Complete Scan History",
            supportingText = "Access all past parking analyses, meter scans, and verification records with Curb Pro.",
            icon = Icons.Default.History,
            onGetPro = onUpgradeToPro,
            onDismiss = { showHistoryProSheet = false }
        )
    }
}

@Composable
private fun ActivityScanRowItem(
    scan: ScanResult,
    onClick: () -> Unit
) {
    val dateStr = remember(scan.timestamp) {
        val now = System.currentTimeMillis()
        val diff = now - scan.timestamp
        when {
            diff < 24 * 60 * 60 * 1000L -> "Today · " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(scan.timestamp))
            diff < 48 * 60 * 60 * 1000L -> "Yesterday · " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(scan.timestamp))
            else -> SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(scan.timestamp))
        }
    }

    val (statusColor, statusText) = when (scan.verdict) {
        ScanVerdict.ALLOWED -> Pair(CurbSuccess, "Parking allowed")
        ScanVerdict.RESTRICTED -> Pair(CurbError, "Parking restricted")
        ScanVerdict.AMBIGUOUS -> Pair(CurbWarning, "Rule unclear")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusNested))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Line 1: Location name on left, Time/date on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = scan.locationName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dateStr,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BentoTextSecondary,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }

        // Line 2: Verdict description on left, CurbVerdictBadge on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CurbVerdictBadge(verdict = scan.verdict)
        }
    }
}

private fun exportScansAsReport(context: Context, scans: List<ScanResult>) {
    val builder = StringBuilder()
    builder.append("CURB PARKING HISTORY REPORT\n")
    builder.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
    builder.append("Total Scans: ${scans.size}\n\n")
    builder.append("----------------------------------------\n")

    scans.forEachIndexed { index, scan ->
        val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(scan.timestamp))
        builder.append("${index + 1}. ${scan.locationName}\n")
        builder.append("   Date: $dateStr\n")
        builder.append("   Verdict: ${scan.verdict.name} (${scan.verdict.displayTitle})\n")
        builder.append("   Allowed Until: ${scan.allowedUntilTime}\n")
        builder.append("   Zone: ${scan.zoneType}\n")
        builder.append("   Payment: ${scan.paymentInfo}\n")
        if (scan.parkingRules.isNotEmpty()) {
            builder.append("   Rules: ${scan.parkingRules.joinToString(" | ")}\n")
        }
        builder.append("----------------------------------------\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Curb Parking History Export (${scans.size} spots)")
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Export Parking History"))
}

