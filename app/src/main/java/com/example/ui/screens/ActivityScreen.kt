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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbSegmentedStatusBar
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface

@Composable
fun ActivityScreen(
    scans: List<ScanResult>,
    onScanClicked: (ScanResult) -> Unit
) {
    val allowedCount = remember(scans) { scans.count { it.verdict == ScanVerdict.ALLOWED } }
    val restrictedCount = remember(scans) { scans.count { it.verdict == ScanVerdict.RESTRICTED } }
    val ambiguousCount = remember(scans) { scans.count { it.verdict == ScanVerdict.AMBIGUOUS } }
    val totalScans = scans.size.coerceAtLeast(1)
    val clearRate = ((allowedCount.toFloat() / totalScans) * 100).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .testTag("activity_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // TOP HEADING
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Activity",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // COMPACT ANALYTICS BANNER
                CurbCard(
                    cornerRadius = RadiusCard,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "${scans.size} Scans | $clearRate% Clear Rate | $ambiguousCount Ambiguous",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
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

                Text(
                    text = "History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
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
                        backgroundColor = CurbSurface
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
                                color = CurbOnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your parking scans will appear here.",
                                fontSize = 14.sp,
                                color = CurbOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(scans) { scan ->
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
        }
    }
}
