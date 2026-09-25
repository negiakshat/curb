package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SavedPlace
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.RadiusHero
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlaceDetailSheet(
    place: SavedPlace,
    onDismiss: () -> Unit,
    onCheckSignAgain: (SavedPlace) -> Unit,
    onEditSpot: ((SavedPlace) -> Unit)? = null,
    onDelete: (Long) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val lastCheckedText = remember(place.lastCheckedAt) {
        val lastTime = if (place.lastCheckedAt > 0) place.lastCheckedAt else place.timestamp
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        sdf.format(Date(lastTime))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BentoCanvas,
        scrimColor = BentoPrimaryDark.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimaryDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BentoTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = place.address.ifBlank { "Location recorded" },
                            fontSize = 13.sp,
                            color = BentoTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BentoTextSecondary
                    )
                }
            }

            // Sign Image Banner (if available)
            if (place.signImageUri.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                        .background(BentoWhite)
                ) {
                    AsyncImage(
                        model = place.signImageUri,
                        contentDescription = "Scanned Sign Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Parking Intelligence Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BentoWhite)
                    .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PARKING RULE INTELLIGENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = BentoTextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CurbSuccessContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = place.parkingVerdict.ifBlank { "VERIFIED SCAN" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbSuccess
                            )
                        }
                    }

                    Text(
                        text = place.parkingRuleSummary.ifBlank { "No explicit parking rule summary saved for this spot." },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BentoPrimaryDark
                    )

                    if (place.parkingSchedule.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CurbSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = place.parkingSchedule,
                                fontSize = 13.sp,
                                color = BentoPrimaryDark
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last checked",
                            fontSize = 12.sp,
                            color = BentoTextSecondary
                        )
                        Text(
                            text = lastCheckedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = BentoPrimaryDark
                        )
                    }

                    if (place.reminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reminder",
                                fontSize = 12.sp,
                                color = BentoTextSecondary
                            )
                            Text(
                                text = "Active · ${place.reminderMinutesBefore}m before limit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CurbSuccess
                            )
                        }
                    }
                }
            }

            // User Note Section (if present)
            if (place.parkingNote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BentoWhite)
                        .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "YOUR NOTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = BentoTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = place.parkingNote,
                            fontSize = 14.sp,
                            color = BentoPrimaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CurbPrimaryButton(
                    text = "EDIT THIS SPOT",
                    onClick = {
                        onDismiss()
                        if (onEditSpot != null) {
                            onEditSpot(place)
                        } else {
                            onCheckSignAgain(place)
                        }
                    },
                    backgroundColor = BentoPrimaryDark,
                    contentColor = BentoWhite,
                    leadingIcon = Icons.Default.Edit,
                    testTag = "edit_saved_spot_button"
                )

                OutlinedButton(
                    onClick = {
                        showDeleteConfirmation = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("delete_saved_spot_button"),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = BentoWhite,
                        contentColor = CurbError
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CurbError)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = CurbError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DELETE THIS SPOT",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = CurbError
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteSavedSpotConfirmationDialog(
            spotName = place.name,
            onConfirmDelete = {
                showDeleteConfirmation = false
                onDismiss()
                onDelete(place.id)
            },
            onDismiss = {
                showDeleteConfirmation = false
            }
        )
    }
}
