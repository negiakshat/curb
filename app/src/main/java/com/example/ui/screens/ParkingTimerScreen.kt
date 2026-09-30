package com.example.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.ActiveParkingSession
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

    // Live update ticker for smooth countdown
    LaunchedEffect(effectiveSession?.endTime, effectiveSession?.isActive) {
        if (effectiveSession != null && effectiveSession.isActive) {
            while (true) {
                val now = System.currentTimeMillis()
                currentTimeMillis = now
                val remaining = effectiveSession.endTime - now
                if (remaining <= 0) {
                    break
                }
                // Update frequency: if < 10 mins (600000ms), update every 1 second (1000ms), else every 10 seconds (10000ms) to save battery and prevent leaks
                val delayTime = if (remaining < 600000L) 1000L else 10000L
                delay(delayTime)
            }
        }
    }

    // Modal state controllers
    var showAddTimeSheet by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showRulesSheet by remember { mutableStateOf(false) }

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
            val totalDuration = (effectiveSession.endTime - effectiveSession.startTime).coerceAtLeast(1000L)
            val remaining = (effectiveSession.endTime - currentTimeMillis)
            val isExpired = remaining <= 0

            // MAIN SCROLLABLE CONTENT AREA (ACTIVE SESSION)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
                var canScheduleExact by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            alarmManager?.canScheduleExactAlarms() == true
                        } else {
                            true
                        }
                    )
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                alarmManager?.canScheduleExactAlarms() == true
                            } else {
                                true
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact && !isExpired) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusCard))
                            .testTag("timer_exact_alarm_warning_card"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, CurbError),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Precise Notifications Disabled",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CurbError
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Curb needs permission to schedule precise parking alerts so you don't get ticketed. Tap below to enable them in settings.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = BentoTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CurbPrimaryButton(
                                text = "ENABLE PRECISE ALERTS",
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:" + context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:" + context.packageName)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                },
                                backgroundColor = CurbError,
                                testTag = "timer_enable_exact_alarms_button"
                            )
                        }
                    }
                }

                // Pre-calculated timer state
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
                        Column {
                            Text(
                                text = if (isExpired) "Expired" else com.example.util.ParkingTimerFormatter.formatRemainingTime(remaining),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpired) CurbError else BentoTextPrimary,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isExpired) {
                                Text(
                                    text = "remaining",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextSecondary,
                                    letterSpacing = (-0.5).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

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
                                        color = BentoTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. SESSION LOCATION CARD
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusCard))
                        .testTag("timer_location_bar"),
                    shape = RoundedCornerShape(RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BentoSand),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalParking,
                                contentDescription = null,
                                tint = BentoPrimaryDark,
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
                                text = "Spot location recorded",
                                fontSize = 12.sp,
                                color = BentoTextSecondary
                            )
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
                                    color = BentoTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = BentoBorder,
                            thickness = 1.dp
                        )

                        // Row 2: Reminder
                        val sessionDurationMins = ((effectiveSession.endTime - effectiveSession.startTime) / 60000L).toInt()
                        val presets = com.example.util.ParkingTimerFormatter.getValidReminderPresets(sessionDurationMins)
                        val hasPresets = presets.isNotEmpty()

                        val currentReminderMins = effectiveSession.reminderMinutesBefore
                        val reminderText = if (isExpired) {
                            "No reminder"
                        } else if (!hasPresets) {
                            "No reminder available"
                        } else if (currentReminderMins > 0 && currentReminderMins < sessionDurationMins) {
                            "$currentReminderMins min before expiry"
                        } else {
                            "Disabled"
                        }

                        val canEditReminder = !isExpired && hasPresets

                        TimerDetailRow(
                            icon = Icons.Default.NotificationsNone,
                            title = "Notification Reminder",
                            subtitle = reminderText,
                            isClickable = canEditReminder,
                            onClick = { if (canEditReminder) showReminderSheet = true },
                            trailingContent = {
                                if (canEditReminder) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Edit reminder",
                                        tint = BentoTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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

                Spacer(modifier = Modifier.height(72.dp))
            }

            // STICKY BOTTOM ACTIONS FOR ACTIVE SESSION
            if (!isExpired) {
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

                val sessionDurationMins = ((effectiveSession.endTime - effectiveSession.startTime) / 60000L).toInt()
                val presets = com.example.util.ParkingTimerFormatter.getValidReminderPresets(sessionDurationMins)
                val reminderOptions = presets.map { mins ->
                    val label = when (mins) {
                        15 -> "15 minutes before (Recommended)"
                        else -> "$mins minutes before"
                    }
                    mins to label
                } + (0 to "Turn off reminders")

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
    }}

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
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon Box (Fixed Size)
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

        Spacer(modifier = Modifier.width(14.dp))

        // Middle Content (Flexes/Shrinks first)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = BentoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Content (No-wrap, maintains full size, never squeezed)
        Box(
            modifier = Modifier
                .wrapContentWidth(Alignment.End)
                .width(IntrinsicSize.Max),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailingContent()
        }
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
