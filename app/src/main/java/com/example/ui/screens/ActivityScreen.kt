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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.components.CurbSegmentedStatusBar
import com.example.ui.theme.BentoBeige
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoBorderStrong
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    scans: List<ScanResult>,
    isPro: Boolean = false,
    onScanClicked: (ScanResult) -> Unit,
    onUpgradeToPro: () -> Unit = {}
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
            .background(CurbBackground)
            .statusBarsPadding()
            .testTag("activity_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // TOP HEADING & EXPORT ACTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )

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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "Export",
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isPro) "Export" else "Export 🔒",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // COMPACT ANALYTICS BANNER
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = BentoWhite,
                    borderColor = BentoBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "${scans.size} Scans | $clearRate% Clear Rate | $ambiguousCount Ambiguous",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Thin segmented verdict bar (Green / Red / Amber)
                        CurbSegmentedStatusBar(
                            allowedCount = allowedCount.coerceAtLeast(if (scans.isEmpty()) 1 else 0),
                            restrictedCount = restrictedCount,
                            ambiguousCount = ambiguousCount
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
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
        }

        if (scans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    CurbCard(
                        cornerRadius = RadiusCard,
                        backgroundColor = BentoWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No scans yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your parking scans will appear here.",
                                fontSize = 14.sp,
                                color = BentoTextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            // ACCESSIBLE SCANS (Full list for Pro, top 5 for Free)
            items(accessibleScans) { scan ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 5.dp)
                ) {
                    RecentScanCard(
                        scan = scan,
                        onClick = { onScanClicked(scan) }
                    )
                }
            }

            // LOCKED HISTORY CARD FOR FREE USERS
            if (!isPro && lockedScansCount > 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusCard))
                                .clickable { showHistoryProSheet = true }
                                .testTag("locked_scan_history_card"),
                            shape = RoundedCornerShape(RadiusCard),
                            colors = CardDefaults.cardColors(containerColor = BentoSand),
                            border = BorderStroke(1.dp, BentoBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(BentoPeach.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = BentoPrimaryDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Older scan history ($lockedScansCount hidden)",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoTextPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = "Unlock your complete parking history with Curb Pro.",
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
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
