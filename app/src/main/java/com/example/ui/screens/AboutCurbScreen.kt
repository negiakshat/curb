package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbLogo
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant

@Composable
fun AboutCurbScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("about_curb_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("about_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = "About Curb AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CurbLogo(symbolSize = 44.dp, fontSize = 30)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Version 1.0.0 (Build 2026.08)",
                        fontSize = 13.sp,
                        color = CurbOnSurfaceVariant
                    )
                }
            }

            item {
                CurbCard(
                    cornerRadius = 24.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Smart Municipal Reasoning",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Curb uses multimodal Gemini AI to decipher complex, multi-layered parking signs in seconds. From metered spaces to tricky street sweeping schedules and commute tow-away zones, Curb tells you clearly if you can park right now.",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = CurbOnSurfaceVariant
                        )
                    }
                }
            }

            item {
                CurbCard(
                    cornerRadius = 24.dp,
                    backgroundColor = CurbSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Designed for Clarity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Built with a minimal monochrome design, generous typography, and strict light aesthetic so you can read parking rules at a glance under direct sunlight.",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = CurbBlack.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
