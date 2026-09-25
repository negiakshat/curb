package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SavedPlace
import com.example.data.model.ScanResult
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.CurbWarningContainer
import com.example.util.SemanticConsistencyValidator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSpotBottomSheet(
    scanResult: ScanResult,
    rescanTargetPlace: SavedPlace? = null,
    onDismiss: () -> Unit,
    onSave: (SavedPlace) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val initialName = remember(rescanTargetPlace, scanResult) {
        if (!rescanTargetPlace?.name.isNullOrBlank()) {
            rescanTargetPlace!!.name
        } else {
            val rawLoc = scanResult.locationName.ifBlank { "Parking Spot" }
            if (rawLoc.contains("&")) {
                rawLoc.split("&").firstOrNull()?.trim() ?: rawLoc
            } else rawLoc
        }
    }

    val initialNote = remember(rescanTargetPlace) {
        rescanTargetPlace?.parkingNote ?: ""
    }

    val initialReminderEnabled = remember(rescanTargetPlace) {
        rescanTargetPlace?.reminderEnabled ?: false
    }

    val initialReminderMins = remember(rescanTargetPlace) {
        rescanTargetPlace?.reminderMinutesBefore ?: 15
    }

    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf(initialNote) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    var selectedReminderMins by remember { mutableIntStateOf(initialReminderMins) }

    val context = LocalContext.current
    val locationService = remember(context) { com.example.data.location.LocationService(context) }
    var currentLat by remember(rescanTargetPlace) { mutableStateOf<Double?>(rescanTargetPlace?.latitude) }
    var currentLng by remember(rescanTargetPlace) { mutableStateOf<Double?>(rescanTargetPlace?.longitude) }

    LaunchedEffect(rescanTargetPlace) {
        if (locationService.hasLocationPermission()) {
            try {
                val result = locationService.fetchCurrentLocation()
                if (result is com.example.data.location.UserLocationResult.Success) {
                    currentLat = result.latitude
                    currentLng = result.longitude
                }
            } catch (_: Exception) {}
        }
    }

    val hasUsableSchedule = remember(scanResult) {
        SemanticConsistencyValidator.canAuthorizeTimer(scanResult) &&
                SemanticConsistencyValidator.isUsableClockTimeOrDuration(scanResult.allowedUntilTime)
    }

    val ruleSummary = remember(scanResult) {
        if (scanResult.parkingRules.isNotEmpty()) {
            scanResult.parkingRules.first()
        } else {
            scanResult.statusChipText
        }
    }

    val scheduleText = remember(scanResult) {
        if (scanResult.allowedUntilTime.isNotBlank() && scanResult.allowedUntilTime != "Verify physical signage") {
            "Allowed until ${scanResult.allowedUntilTime}"
        } else ""
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BentoPeach),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = BentoPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (rescanTargetPlace != null) "Update Saved Spot" else "Save Parking Intelligence",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark
                        )
                        Text(
                            text = "Save sign rule, location, and reminder",
                            fontSize = 12.sp,
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

            // Intelligence Card Summary Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BentoWhite)
                    .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!scanResult.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = scanResult.imageUri,
                            contentDescription = "Parking Sign",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoPeach)
                                .border(1.dp, BentoBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Signpost,
                                contentDescription = null,
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scanResult.locationName.ifBlank { "Scanned Location" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ruleSummary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CurbSuccess
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Last checked · Just now",
                            fontSize = 11.sp,
                            color = BentoTextSecondary
                        )
                    }
                }
            }

            // Form Field 1: Spot Name Input
            Column {
                Text(
                    text = "SPOT NAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = BentoTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Home, Office, Gym, Mission St") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_place_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BentoWhite,
                        unfocusedContainerColor = BentoWhite,
                        focusedBorderColor = BentoPrimaryDark,
                        unfocusedBorderColor = BentoBorder
                    ),
                    singleLine = true
                )
            }

            // Form Field 2: Parking Reminder Toggle / Schedule
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BentoWhite)
                    .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                if (hasUsableSchedule) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CurbSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Parking Rule Reminder",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BentoPrimaryDark
                                )
                                Text(
                                    text = if (scheduleText.isNotBlank()) scheduleText else "Alert before rule limit expires",
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            modifier = Modifier.testTag("saved_place_reminder_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoWhite,
                                checkedTrackColor = CurbSuccess
                            )
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "REMIND ME BEFORE LIMIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                val selected = selectedReminderMins == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) CurbSuccessContainer else BentoCanvas)
                                        .border(
                                            1.dp,
                                            if (selected) CurbSuccess else BentoBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedReminderMins = mins }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) CurbSuccess else BentoPrimaryDark
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CurbWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Reminder unavailable for this sign",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoPrimaryDark
                            )
                            Text(
                                text = "Curb could not verify a reliable time window from the scan.",
                                fontSize = 11.sp,
                                color = BentoTextSecondary
                            )
                        }
                    }
                }
            }

            // Form Field 3: Parking Note Input
            Column {
                Text(
                    text = "PERSONAL NOTE (OPTIONAL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = BentoTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("e.g. Permit A required, level P2 near pillar B4") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_place_note_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BentoWhite,
                        unfocusedContainerColor = BentoWhite,
                        focusedBorderColor = BentoPrimaryDark,
                        unfocusedBorderColor = BentoBorder
                    ),
                    minLines = 2,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Save Action Button
            CurbPrimaryButton(
                text = if (rescanTargetPlace != null) "UPDATE THIS SPOT" else "SAVE THIS SPOT",
                onClick = {
                    val finalName = name.ifBlank { scanResult.locationName.ifBlank { "Parking Spot" } }
                    val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
                    val currentFormatted = sdf.format(Date())

                    val savedPlace = SavedPlace(
                        id = rescanTargetPlace?.id ?: 0L,
                        name = finalName,
                        address = scanResult.locationName.ifBlank { scanResult.cityState },
                        parkingNote = note.trim(),
                        timestamp = rescanTargetPlace?.timestamp ?: System.currentTimeMillis(),
                        latitude = currentLat,
                        longitude = currentLng,
                        scanResultId = scanResult.id,
                        parkingRuleSummary = ruleSummary,
                        parkingSchedule = scheduleText,
                        parkingVerdict = scanResult.verdict.name,
                        signImageUri = scanResult.imageUri ?: "",
                        lastCheckedAt = System.currentTimeMillis(),
                        reminderEnabled = reminderEnabled && hasUsableSchedule,
                        reminderMinutesBefore = selectedReminderMins,
                        reminderScheduleText = scheduleText
                    )
                    onSave(savedPlace)
                },
                backgroundColor = BentoPrimaryDark,
                contentColor = BentoWhite,
                leadingIcon = Icons.Default.Check,
                testTag = "save_spot_button"
            )
        }
    }
}
