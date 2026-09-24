package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbTopAppBar
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite

@Composable
fun AboutCurbScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("about_curb_screen")
    ) {
        CurbTopAppBar(
            title = "About Curb",
            onBack = onBack,
            backTestTag = "about_back_button"
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Logo & Version Info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CurbLogo(symbolSize = 44.dp, fontSize = 30)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BentoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Build 2026.08",
                        fontSize = 12.sp,
                        color = BentoTextSecondary
                    )
                }
            }

            // Brand Statement Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Parking should be simple.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Curb helps you understand parking signs, restrictions, and time limits before you leave your car.",
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = BentoTextSecondary
                        )
                    }
                }
            }

            // What Curb Does
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "What Curb does",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(2.dp))
                            FeatureRow(
                                icon = Icons.Default.CropFree,
                                title = "Reads the sign",
                                description = "Curb identifies the parking information that matters."
                            )
                            FeatureRow(
                                icon = Icons.Default.Rule,
                                title = "Explains the rule",
                                description = "Curb turns confusing restrictions into clear, useful information."
                            )
                            FeatureRow(
                                icon = Icons.Default.Schedule,
                                title = "Keeps you on time",
                                description = "Curb tracks an active parking session and reminds you before your limit."
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }

            // Brand Story Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Made for real parking moments.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Because the hardest part of parking is often understanding the sign before you walk away.",
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = BentoTextSecondary
                        )
                    }
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Curb · Version 1.0.0",
                        fontSize = 12.sp,
                        color = BentoTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BentoPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BentoPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = BentoTextSecondary
            )
        }
    }
}
