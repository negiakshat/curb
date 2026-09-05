package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.RadiusCard
import com.example.util.ParkingTimerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionConfirmationSheet(
    locationName: String,
    timerConfig: ParkingTimerConfig,
    onConfirmStartTimer: (durationMinutes: Int, allowedUntilTime: String, timerBasis: String, ruleSummary: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BentoCanvas,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("start_session_confirmation_sheet")
        ) {
            // SHEET HEADER
            Text(
                text = "Confirm Parking Session",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = CurbOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = locationName,
                    fontSize = 13.sp,
                    color = CurbOnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRIMARY DECISION BANNER (Green/Sand card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusCard),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF7F1)),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC8E6C9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1B873F),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = timerConfig.confirmationHeadline,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B873F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timerConfig.confirmationSubtext,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = CurbOnSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BREAKDOWN DETAILS CARD
            Surface(
                shape = RoundedCornerShape(RadiusCard),
                color = CurbSurface,
                border = BorderStroke(1.dp, BentoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CurbOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Timer Duration",
                                fontSize = 14.sp,
                                color = CurbOnSurface
                            )
                        }
                        Text(
                            text = timerConfig.formattedDuration,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = CurbOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Allowed Until",
                                fontSize = 14.sp,
                                color = CurbOnSurface
                            )
                        }
                        Text(
                            text = timerConfig.allowedUntilTimeFormatted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CurbOnSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CurbOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Timer Basis",
                                fontSize = 14.sp,
                                color = CurbOnSurface
                            )
                        }
                        Text(
                            text = timerConfig.timerBasis,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CurbOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ACTION BUTTONS
            CurbPrimaryButton(
                text = "START TIMER (${timerConfig.formattedDuration.uppercase()})",
                onClick = {
                    onConfirmStartTimer(
                        timerConfig.calculatedMinutes,
                        timerConfig.allowedUntilTimeFormatted,
                        timerConfig.timerBasis,
                        timerConfig.ruleSummary
                    )
                },
                testTag = "confirm_start_timer_button"
            )

            Spacer(modifier = Modifier.height(8.dp))

            CurbSecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "cancel_start_timer_button"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
