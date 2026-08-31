package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbSecondaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant

@Composable
fun HelpSupportScreen(
    onPrivacyClicked: () -> Unit,
    onTermsClicked: () -> Unit,
    onContactSupport: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("help_support_screen")
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("help_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = "Help & Support",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Frequently Asked Questions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
            }

            item {
                FaqItem(
                    question = "How does Curb interpret multiple signs?",
                    answer = "Curb analyzes the entire pole simultaneously, cross-referencing daytime time limits, street cleaning hours, tow-away commute restrictions, and permit exemptions against the exact current day and time."
                )
            }

            item {
                FaqItem(
                    question = "What if a sign is faded or temporary?",
                    answer = "When signs have conflicting directional arrows or temporary paper notices taped over metal signs, Curb returns 'Rule unclear — Verify Locally' to prioritize your safety against unexpected tickets."
                )
            }

            item {
                FaqItem(
                    question = "Do timers automatically notify me?",
                    answer = "Yes, when you enable Push Notifications in Settings, Curb alerts you 15 minutes before your allowed parking duration expires."
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Legal & Policies",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                CurbCard(
                    cornerRadius = 20.dp,
                    backgroundColor = CurbSurface
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPrivacyClicked() }
                                .padding(20.dp)
                                .testTag("help_privacy_policy_link"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Privacy Policy",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CurbOnSurface
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CurbOnSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(horizontal = 20.dp)
                                .background(CurbSurfaceVariant)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTermsClicked() }
                                .padding(20.dp)
                                .testTag("help_terms_service_link"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terms of Service",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CurbOnSurface
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CurbOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                CurbSecondaryButton(
                    text = "Contact Support Team",
                    onClick = onContactSupport,
                    testTag = "contact_support_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }

    CurbCard(
        cornerRadius = 20.dp,
        backgroundColor = CurbSurface,
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CurbBlack
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = answer,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = CurbOnSurfaceVariant
                )
            }
        }
    }
}
