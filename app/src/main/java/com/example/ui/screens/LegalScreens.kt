package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("privacy_policy_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("privacy_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = "Privacy Policy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            item {
                Text(
                    text = "Last updated: August 2026",
                    fontSize = 13.sp,
                    color = CurbOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        LegalSection(
                            title = "1. Information We Process",
                            content = "Curb processes images of parking signs you scan solely to interpret parking rules, determine time allowances, and generate parking guidance. We do not sell your personal data."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LegalSection(
                            title = "2. Location Data",
                            content = "When permitted, Curb uses approximate device location to resolve city-specific municipal rules and street sweeping schedules."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LegalSection(
                            title = "3. On-Device Storage",
                            content = "Your scan history, timers, and saved places are stored securely on your local device via encrypted Room databases."
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TermsOfServiceScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("terms_of_service_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("terms_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = "Terms of Service",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            item {
                Text(
                    text = "Last updated: August 2026",
                    fontSize = 13.sp,
                    color = CurbOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        LegalSection(
                            title = "1. Parking Assistant Disclaimer",
                            content = "Curb provides automated AI-assisted interpretation of parking signage. Users remain solely responsible for verifying physical signage, painted curb markings, temporary paper construction notices, and municipal regulations before parking."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LegalSection(
                            title = "2. Citations & Towing",
                            content = "Curb and its developers assume no liability for parking tickets, citations, towing fees, or property damage resulting from parking decisions."
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LegalSection(title: String, content: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = CurbOnSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = content,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = CurbOnSurfaceVariant
    )
}
