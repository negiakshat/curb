package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.core.content.ContextCompat
import com.example.data.model.ActiveParkingSession
import com.example.data.model.ParkingSpot
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbSecondaryButton
import com.example.ui.theme.BentoBeige
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingTimerScreen(
    activeSession: ActiveParkingSession?,
    targetSession: ActiveParkingSession? = null,
    savedParkingSpot: ParkingSpot? = null,
    isSavingParkingSpot: Boolean = false,
    parkingSpotSaveError: String? = null,
    onSaveParkingSpot: () -> Unit = {},
    onNavigateToFindMyCar: () -> Unit = {},
    onStartQuickTimer: (Int, String) -> Unit,
    canStartQuickTimer: Boolean = false,
    onEndSession: (Long) -> Unit,
    onExtendSession: (Long, Int, Long) -> Unit,
    onUpdateReminder: (Long, Int) -> Unit,
    onBack: () -> Unit
) {
    val effectiveSession = targetSession ?: activeSession
    val context = LocalContext.current
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            onSaveParkingSpot()
        } else {
            Toast.makeText(context, "Location permission is required to save your spot", Toast.LENGTH_SHORT).show()
        }
    }

    // Live update ticker for smooth countdown
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    // Modal state controllers
    var showAddTimeSheet by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showRulesSheet by remember { mutableStateOf(false) }
    var showMapSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("parking_timer_screen")
    ) {
        // 1. TOP APP BAR: Back button | "Parking Timer"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Back Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BentoWhite)
                    .border(1.dp, BentoBorder, CircleShape)
                    .clickable { onBack() }
                    .testTag("timer_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BentoTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Parking Timer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Equal 44.dp width invisible placeholder to balance [ Back ] title alignment
            Box(modifier = Modifier.size(44.dp))
        }

        if (effectiveSession != null && effectiveSession.isActive) {
            // MAIN SCROLLABLE CONTENT AREA (ACTIVE SESSION)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pre-calculated timer state
                val totalDuration = (effectiveSession.endTime - effectiveSession.startTime).coerceAtLeast(1000L)
                val remaining = (effectiveSession.endTime - currentTimeMillis)
                val isExpired = remaining <= 0

                val totalSeconds = (remaining / 1000).coerceAtLeast(0L)
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60

                val remainingText = if (isExpired) {
                    "Expired"
                } else if (hours > 0) {
                    "${hours}h ${minutes}m remaining"
                } else {
                    "${minutes}m remaining"
                }

                val totalSecs = totalDuration / 1000
                val totalHours = totalSecs / 3600
                val totalMins = (totalSecs % 3600) / 60
                val limitDisplay = if (totalHours > 0 && totalMins > 0) {
                    "${totalHours}h ${totalMins}m"
                } else if (totalHours > 0) {
                    "${totalHours}h"
                } else {
                    "${totalMins}m"
                }

                val expiryTimeStr = if (effectiveSession.allowedUntilTime.isNotBlank()) {
                    effectiveSession.allowedUntilTime
                } else {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(effectiveSession.endTime))
                }

                val semanticModeLabel = when (effectiveSession.timerMode) {
                    "TIMED_LIMIT" -> "VERIFIED TIME LIMIT"
                    "CLOCK_CUTOFF" -> "VERIFIED CUTOFF TIME"
                    "METERED_WITHOUT_VERIFIED_TIME_LIMIT" -> "VERIFIED METER REQUIREMENT"
                    else -> if (effectiveSession.timerBasis.isNotBlank()) effectiveSession.timerBasis.uppercase() else "VERIFIED PARKING RULE"
                }

                val ruleText = when {
                    effectiveSession.parkingRuleSummary.isNotBlank() -> effectiveSession.parkingRuleSummary
                    effectiveSession.notes.isNotBlank() -> effectiveSession.notes
                    effectiveSession.timerBasis.isNotBlank() -> effectiveSession.timerBasis
                    else -> "$limitDisplay limit"
                }

                // ==========================================
                // 2. PRIMARY TIMER HERO CARD
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusHero))
                        .testTag("timer_hero_card"),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Header Status Pill & Semantic Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(RadiusChip))
                                    .background(if (isExpired) CurbErrorContainer else CurbSuccessContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isExpired) CurbError else CurbSuccess)
                                )
                                Text(
                                    text = if (isExpired) "SESSION EXPIRED" else "ACTIVE PARKING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpired) CurbError else CurbSuccess,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = semanticModeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Remaining Time Display (Hero Information)
                        Text(
                            text = remainingText,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) CurbError else BentoTextPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Exact Local Expiry Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (isExpired) CurbError else BentoTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isExpired) "Expired at $expiryTimeStr" else "Expires at $expiryTimeStr",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExpired) CurbError else BentoTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. RULE CONTEXT (Verified regulation)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusCard),
                            color = BentoCanvas,
                            border = BorderStroke(1.dp, BentoBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(BentoSand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = BentoPrimaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "VERIFIED PARKING RULE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ruleText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BentoTextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. UNIFIED PARKING LOCATION & SPOT CARD
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusCard))
                        .testTag(if (savedParkingSpot != null) "saved_parking_spot_card" else "timer_location_bar"),
                    shape = RoundedCornerShape(RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (savedParkingSpot != null) CurbSuccessContainer else BentoSand),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (savedParkingSpot != null) Icons.Default.CheckCircle else Icons.Default.Place,
                                    contentDescription = null,
                                    tint = if (savedParkingSpot != null) CurbSuccess else BentoPrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = effectiveSession.locationName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (savedParkingSpot != null) "Parking spot saved" else "Spot location recorded",
                                    fontSize = 12.sp,
                                    color = if (savedParkingSpot != null) CurbSuccess else BentoTextSecondary,
                                    fontWeight = if (savedParkingSpot != null) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (savedParkingSpot != null) {
                            CurbPrimaryButton(
                                text = "FIND MY CAR",
                                onClick = onNavigateToFindMyCar,
                                leadingIcon = Icons.Default.Place,
                                backgroundColor = BentoPrimaryDark,
                                testTag = "find_my_car_button"
                            )
                        } else {
                            CurbSecondaryButton(
                                text = if (isSavingParkingSpot) "Saving parking spot…" else "SAVE MY PARKING SPOT",
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        onSaveParkingSpot()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                enabled = !isSavingParkingSpot,
                                leadingIcon = Icons.Default.Place,
                                testTag = "save_parking_spot_button"
                            )

                            if (!parkingSpotSaveError.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = parkingSpotSaveError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .testTag("parking_spot_save_error")
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 4. SESSION DETAILS (Clean Compact List)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusCard))
                        .testTag("timer_details_card"),
                    shape = RoundedCornerShape(RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Row 1: Started at
                        val startedSdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val startedTimeStr = startedSdf.format(Date(effectiveSession.startTime))

                        TimerDetailRow(
                            icon = Icons.Default.AccessTime,
                            title = "Started at",
                            subtitle = startedTimeStr,
                            trailingContent = {
                                Text(
                                    text = "Today",
                                    fontSize = 13.sp,
                                    color = BentoTextSecondary
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = BentoBorder,
                            thickness = 1.dp
                        )

                        // Row 2: Reminder
                        val currentReminderMins = effectiveSession.reminderMinutesBefore
                        val reminderText = if (currentReminderMins > 0) {
                            "$currentReminderMins min before expiry"
                        } else {
                            "Disabled"
                        }

                        TimerDetailRow(
                            icon = Icons.Default.NotificationsNone,
                            title = "Notification Reminder",
                            subtitle = reminderText,
                            isClickable = true,
                            onClick = { showReminderSheet = true },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Edit reminder",
                                    tint = BentoTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = BentoBorder,
                            thickness = 1.dp
                        )

                        // Row 3: Verified Rules
                        TimerDetailRow(
                            icon = Icons.Default.Info,
                            title = "Parking Regulations",
                            subtitle = "View active sign rules",
                            isClickable = true,
                            onClick = { showRulesSheet = true },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "View rules",
                                    tint = BentoTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // STICKY BOTTOM ACTIONS FOR ACTIVE SESSION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BentoCanvas)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT BUTTON: "Add time"
                CurbSecondaryButton(
                    text = "Add time",
                    onClick = { showAddTimeSheet = true },
                    modifier = Modifier.weight(1f),
                    testTag = "add_time_button"
                )

                // RIGHT BUTTON: "End parking session"
                CurbPrimaryButton(
                    text = "End session",
                    onClick = { onEndSession(effectiveSession.id) },
                    modifier = Modifier.weight(1.25f),
                    backgroundColor = CurbError,
                    testTag = "end_parking_session_button"
                )
            }
        } else {
            // ==========================================
            // NO ACTIVE SESSION: CENTERED EMPTY STATE
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BentoSand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalParking,
                        contentDescription = null,
                        tint = BentoPrimaryDark,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Active Parking Session",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Scan a parking sign first.",
                    fontSize = 14.sp,
                    color = BentoTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 1: ADD TIME (EXTEND SESSION)
    // ==========================================
    if (showAddTimeSheet && effectiveSession?.isActive == true) {
        val sheetState = rememberModalBottomSheetState()
        val maxAllowed = effectiveSession.maxAllowedEndTimeMillis
        val maxExtensionMillis = if (maxAllowed != null) {
            if (maxAllowed == Long.MAX_VALUE) Long.MAX_VALUE else (maxAllowed - effectiveSession.endTime).coerceAtLeast(0L)
        } else 0L

        ModalBottomSheet(
            onDismissRequest = { showAddTimeSheet = false },
            sheetState = sheetState,
            containerColor = BentoWhite,
            shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Extend Parking Time",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (maxExtensionMillis <= 0L) {
                    Text(
                        text = "This session has reached the maximum authorized time limit for this spot. Extension is not permitted.",
                        fontSize = 13.sp,
                        color = BentoTextSecondary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Choose how many minutes to add to your current session.",
                        fontSize = 13.sp,
                        color = BentoTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val canAdd15 = maxExtensionMillis >= 15 * 60 * 1000L
                    AddTimeOptionButton("+15 min", 15, enabled = canAdd15, modifier = Modifier.weight(1f)) {
                        onExtendSession(effectiveSession.id, 15, effectiveSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 15 minutes to session", Toast.LENGTH_SHORT).show()
                    }
                    val canAdd30 = maxExtensionMillis >= 30 * 60 * 1000L
                    AddTimeOptionButton("+30 min", 30, enabled = canAdd30, modifier = Modifier.weight(1f)) {
                        onExtendSession(effectiveSession.id, 30, effectiveSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 30 minutes to session", Toast.LENGTH_SHORT).show()
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val canAdd60 = maxExtensionMillis >= 60 * 60 * 1000L
                    AddTimeOptionButton("+1 hour", 60, enabled = canAdd60, modifier = Modifier.weight(1f)) {
                        onExtendSession(effectiveSession.id, 60, effectiveSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 1 hour to session", Toast.LENGTH_SHORT).show()
                    }
                    val canAdd120 = maxExtensionMillis >= 120 * 60 * 1000L
                    AddTimeOptionButton("+2 hours", 120, enabled = canAdd120, modifier = Modifier.weight(1f)) {
                        onExtendSession(effectiveSession.id, 120, effectiveSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 2 hours to session", Toast.LENGTH_SHORT).show()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 2: REMINDER SETTINGS
    // ==========================================
    if (showReminderSheet && effectiveSession?.isActive == true) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showReminderSheet = false },
            sheetState = sheetState,
            containerColor = BentoWhite,
            shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Notification Reminder",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Receive a push alert before your parking limit expires.",
                    fontSize = 13.sp,
                    color = BentoTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                val reminderOptions = listOf(
                    5 to "5 minutes before",
                    10 to "10 minutes before",
                    15 to "15 minutes before (Recommended)",
                    30 to "30 minutes before",
                    0 to "Turn off reminders"
                )

                val currentSelectedMins = effectiveSession.reminderMinutesBefore
                reminderOptions.forEach { (mins, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable {
                                onUpdateReminder(effectiveSession.id, mins)
                                showReminderSheet = false
                                Toast.makeText(
                                    context,
                                    if (mins > 0) "Reminder set for $mins min before expiry" else "Reminder disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (currentSelectedMins == mins) FontWeight.Bold else FontWeight.Normal,
                            color = BentoTextPrimary
                        )
                        RadioButton(
                            selected = currentSelectedMins == mins,
                            onClick = {
                                onUpdateReminder(effectiveSession.id, mins)
                                showReminderSheet = false
                                Toast.makeText(
                                    context,
                                    if (mins > 0) "Reminder set for $mins min before expiry" else "Reminder disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = BentoPrimary)
                        )
                    }
                    HorizontalDivider(color = BentoBorder)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 3: PARKING RULES BREAKDOWN
    // ==========================================
    if (showRulesSheet && effectiveSession?.isActive == true) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showRulesSheet = false },
            sheetState = sheetState,
            containerColor = BentoWhite,
            shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BentoSand),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BentoPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Active Parking Rules",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(RadiusCard),
                    color = BentoCanvas,
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val activeRuleStr = effectiveSession?.parkingRuleSummary?.ifBlank { null }
                            ?: effectiveSession?.notes?.ifBlank { null }
                            ?: "No parking rule recorded for this session."
                        RuleItem("ACTIVE REGULATION", activeRuleStr)
                        Spacer(modifier = Modifier.height(10.dp))
                        RuleItem("NOTIFICATIONS", "Smart push notifications will alert you prior to restriction enforcement.")
                        Spacer(modifier = Modifier.height(10.dp))
                        RuleItem("HOLIDAY EXCEPTIONS", "No additional holiday exception detected.")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                CurbPrimaryButton(
                    text = "Got it",
                    onClick = { showRulesSheet = false },
                    backgroundColor = BentoPrimaryDark,
                    contentColor = BentoWhite,
                    testTag = "rules_sheet_got_it_button"
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 4: INTERACTIVE MAP & PIN
    // ==========================================
    if (showMapSheet && effectiveSession?.isActive == true) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showMapSheet = false },
            sheetState = sheetState,
            containerColor = BentoWhite,
            shape = RoundedCornerShape(topStart = RadiusHero, topEnd = RadiusHero)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Parked Location",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (effectiveSession.notes.isNotBlank()) "${effectiveSession.locationName} • ${effectiveSession.notes}" else effectiveSession.locationName,
                    fontSize = 13.sp,
                    color = BentoTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Map Placeholder Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(RadiusCard),
                    color = BentoSand,
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BentoPrimaryDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = BentoWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = savedParkingSpot?.let { spot ->
                                    "Spot GPS: " + String.format(Locale.US, "%.5f°, %.5f°", spot.latitude, spot.longitude)
                                } ?: "Location unavailable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Text(
                                text = "Walking distance unavailable",
                                fontSize = 11.sp,
                                color = BentoTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CurbSecondaryButton(
                        text = "Copy Address",
                        onClick = {
                            Toast.makeText(context, "Location copied to clipboard", Toast.LENGTH_SHORT).show()
                            showMapSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "copy_address_button"
                    )

                    CurbPrimaryButton(
                        text = "Navigate",
                        onClick = {
                            Toast.makeText(context, "Opening walking directions...", Toast.LENGTH_SHORT).show()
                            showMapSheet = false
                        },
                        leadingIcon = Icons.Default.Directions,
                        backgroundColor = BentoPrimaryDark,
                        contentColor = BentoWhite,
                        modifier = Modifier.weight(1f),
                        testTag = "navigate_walking_button"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// HELPER COMPOSABLES
// -------------------------------------------------------------------------------------

@Composable
private fun TimerDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {},
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BentoCanvas),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BentoTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = BentoTextSecondary
                )
            }
        }

        trailingContent()
    }
}

@Composable
private fun AddTimeOptionButton(
    text: String,
    minutes: Int,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(RadiusNested))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(RadiusNested),
        color = if (enabled) BentoCanvas else BentoCanvas.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (enabled) BentoBorder else BentoBorder.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) BentoTextPrimary else BentoTextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun RuleItem(title: String, description: String) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = BentoPrimaryDark,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = BentoTextPrimary,
            lineHeight = 18.sp
        )
    }
}
