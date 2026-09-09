package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Vibrant Theme Palette matching Curb design identity
private val TimerTrackGreen = Color(0xFFE2F6EA)
private val TimerProgressGreen = Color(0xFF1DB954)
private val TimerTextDark = Color(0xFF121212)
private val TimerTextMuted = Color(0xFF757575)
private val TimerCardBg = Color(0xFFFFFFFF)
private val TimerBannerBg = Color(0xFFEDF7F1)
private val TimerBannerGreen = Color(0xFF1B873F)
private val TimerDividerColor = Color(0xFFF1F1F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingTimerScreen(
    activeSession: ActiveParkingSession?,
    savedParkingSpot: ParkingSpot? = null,
    isSavingParkingSpot: Boolean = false,
    parkingSpotSaveError: String? = null,
    onSaveParkingSpot: () -> Unit = {},
    onNavigateToFindMyCar: () -> Unit = {},
    onStartQuickTimer: (Int, String) -> Unit,
    onEndSession: (Long) -> Unit,
    onExtendSession: (Long, Int, Long) -> Unit,
    onUpdateReminder: (Long, Int) -> Unit,
    onBack: () -> Unit
) {
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
    var showMoreMenu by remember { mutableStateOf(false) }
    var isNotificationBannerVisible by remember { mutableStateOf(true) }

    // State for reminder duration selection
    var selectedReminderMinutes by remember(activeSession?.reminderMinutesBefore) {
        mutableIntStateOf(activeSession?.reminderMinutesBefore ?: 15)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("parking_timer_screen")
    ) {
        // 1. TOP APP BAR: Back button | "Parking Timer" | 3-dots Overflow Menu
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
                    .background(TimerCardBg)
                    .border(1.dp, BentoBorder, CircleShape)
                    .clickable { onBack() }
                    .testTag("timer_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TimerTextDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Parking Timer",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TimerTextDark
            )

            // Circular More Menu Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TimerCardBg)
                    .border(1.dp, BentoBorder, CircleShape)
                    .clickable { showMoreMenu = true }
                    .testTag("timer_more_menu_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    tint = TimerTextDark,
                    modifier = Modifier.size(22.dp)
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Set Custom Reminder") },
                        leadingIcon = { Icon(Icons.Default.NotificationsNone, null) },
                        onClick = {
                            showMoreMenu = false
                            showReminderSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View Parking Rules") },
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        onClick = {
                            showMoreMenu = false
                            showRulesSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Parking Details") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            showMoreMenu = false
                            Toast.makeText(
                                context,
                                "Parking location & expiry copied to clipboard!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    if (activeSession != null && activeSession.isActive) {
                        DropdownMenuItem(
                            text = { Text("End Session", color = CurbError) },
                            onClick = {
                                showMoreMenu = false
                                onEndSession(activeSession.id)
                            }
                        )
                    }
                }
            }
        }

        // MAIN SCROLLABLE CONTENT AREA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSession != null && activeSession.isActive) {
                // Pre-calculated timer state
                val totalDuration = (activeSession.endTime - activeSession.startTime).coerceAtLeast(1000L)
                val remaining = (activeSession.endTime - currentTimeMillis)
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

                val expiryTimeStr = if (activeSession.allowedUntilTime.isNotBlank()) {
                    activeSession.allowedUntilTime
                } else {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(activeSession.endTime))
                }

                val semanticModeLabel = when (activeSession.timerMode) {
                    "TIMED_LIMIT" -> "VERIFIED TIME LIMIT"
                    "CLOCK_CUTOFF" -> "VERIFIED CUTOFF TIME"
                    "METERED_WITHOUT_VERIFIED_TIME_LIMIT" -> "VERIFIED METER REQUIREMENT"
                    else -> if (activeSession.timerBasis.isNotBlank()) activeSession.timerBasis.uppercase() else "VERIFIED PARKING RULE"
                }

                val ruleText = when {
                    activeSession.parkingRuleSummary.isNotBlank() -> activeSession.parkingRuleSummary
                    activeSession.notes.isNotBlank() -> activeSession.notes
                    activeSession.timerBasis.isNotBlank() -> activeSession.timerBasis
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
                    colors = CardDefaults.cardColors(containerColor = TimerCardBg),
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
                                    .background(if (isExpired) CurbErrorContainer else TimerBannerBg)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isExpired) CurbError else TimerBannerGreen)
                                )
                                Text(
                                    text = if (isExpired) "SESSION EXPIRED" else "ACTIVE PARKING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpired) CurbError else TimerBannerGreen,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = semanticModeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TimerTextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Remaining Time Display (Hero Information)
                        Text(
                            text = remainingText,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) CurbError else TimerTextDark,
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
                                tint = if (isExpired) CurbError else TimerTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isExpired) "Expired at $expiryTimeStr" else "Expires at $expiryTimeStr",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExpired) CurbError else TimerTextDark
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
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(TimerBannerBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = TimerBannerGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "VERIFIED PARKING RULE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TimerTextMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ruleText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TimerTextDark,
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
                    colors = CardDefaults.cardColors(containerColor = TimerCardBg),
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
                                    .background(if (savedParkingSpot != null) Color(0xFFE6F4EA) else BentoCanvas)
                                    .border(1.dp, if (savedParkingSpot != null) Color(0xFFCEEAD6) else BentoBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (savedParkingSpot != null) Icons.Default.CheckCircle else Icons.Default.Place,
                                    contentDescription = null,
                                    tint = if (savedParkingSpot != null) Color(0xFF137333) else TimerTextDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeSession.locationName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TimerTextDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (savedParkingSpot != null) "Parking spot saved" else "Spot location recorded",
                                    fontSize = 12.sp,
                                    color = if (savedParkingSpot != null) Color(0xFF137333) else TimerTextMuted,
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
                    colors = CardDefaults.cardColors(containerColor = TimerCardBg),
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
                        val startedTimeStr = startedSdf.format(Date(activeSession.startTime))

                        TimerDetailRow(
                            icon = Icons.Default.AccessTime,
                            title = "Started at",
                            subtitle = startedTimeStr,
                            trailingContent = {
                                Text(
                                    text = "Today",
                                    fontSize = 13.sp,
                                    color = TimerTextMuted
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = TimerDividerColor,
                            thickness = 1.dp
                        )

                        // Row 2: Reminder
                        val reminderText = if (selectedReminderMinutes > 0) {
                            "$selectedReminderMinutes min before expiry"
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
                                    tint = TimerTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = TimerDividerColor,
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
                                    tint = TimerTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }

                // Notification Alert Banner
                AnimatedVisibility(
                    visible = isNotificationBannerVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusCard),
                        color = TimerBannerBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TimerBannerGreen,
                                    modifier = Modifier.size(20.dp)
                                )

                                Column {
                                    Text(
                                        text = "Smart push alerts enabled",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TimerTextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "We'll notify you before your parking limit expires.",
                                        fontSize = 12.sp,
                                        color = TimerTextMuted
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isNotificationBannerVisible = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification banner",
                                    tint = TimerTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

            } else {
                // ==========================================
                // NO ACTIVE SESSION: QUICK START LAUNCHER
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusHero)),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = TimerCardBg),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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
                                tint = BentoPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Active Parking Session",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TimerTextDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Select your parking duration below to start an interactive timer with smart street notifications.",
                            fontSize = 13.sp,
                            color = TimerTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "QUICK DURATION PRESETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TimerTextMuted,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickPresetPill(
                                label = "30m",
                                minutes = 30,
                                onClick = { onStartQuickTimer(30, "30m limit") },
                                modifier = Modifier.weight(1f)
                            )
                            QuickPresetPill(
                                label = "1h",
                                minutes = 60,
                                onClick = { onStartQuickTimer(60, "1h limit") },
                                modifier = Modifier.weight(1f)
                            )
                            QuickPresetPill(
                                label = "2h 15m",
                                minutes = 135,
                                isPrimary = true,
                                onClick = { onStartQuickTimer(135, "2h 30m limit") },
                                modifier = Modifier.weight(1.3f)
                            )
                            QuickPresetPill(
                                label = "4h",
                                minutes = 240,
                                onClick = { onStartQuickTimer(240, "4h limit") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // ==========================================
        // 7. PRIMARY STICKY ACTIONS
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BentoCanvas)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeSession != null && activeSession.isActive) {
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
                    onClick = { onEndSession(activeSession.id) },
                    modifier = Modifier.weight(1.25f),
                    backgroundColor = CurbError,
                    testTag = "end_parking_session_button"
                )
            } else {
                CurbPrimaryButton(
                    text = "Start Parking Session (2h 15m)",
                    onClick = { onStartQuickTimer(135, "2h 30m limit") },
                    leadingIcon = Icons.Default.Add,
                    backgroundColor = BentoPrimaryDark,
                    testTag = "start_quick_session_button"
                )
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 1: ADD TIME (EXTEND SESSION)
    // ==========================================
    if (showAddTimeSheet && activeSession != null) {
        val sheetState = rememberModalBottomSheetState()
        val maxAllowed = activeSession.maxAllowedEndTimeMillis
        val maxExtensionMillis = if (maxAllowed != null) {
            if (maxAllowed == Long.MAX_VALUE) Long.MAX_VALUE else (maxAllowed - activeSession.endTime).coerceAtLeast(0L)
        } else 0L

        ModalBottomSheet(
            onDismissRequest = { showAddTimeSheet = false },
            sheetState = sheetState,
            containerColor = TimerCardBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                    color = TimerTextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (maxExtensionMillis <= 0L) {
                    Text(
                        text = "This session has reached the maximum authorized time limit for this spot. Extension is not permitted.",
                        fontSize = 13.sp,
                        color = TimerTextMuted,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Choose how many minutes to add to your current session.",
                        fontSize = 13.sp,
                        color = TimerTextMuted,
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
                        onExtendSession(activeSession.id, 15, activeSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 15 minutes to session", Toast.LENGTH_SHORT).show()
                    }
                    val canAdd30 = maxExtensionMillis >= 30 * 60 * 1000L
                    AddTimeOptionButton("+30 min", 30, enabled = canAdd30, modifier = Modifier.weight(1f)) {
                        onExtendSession(activeSession.id, 30, activeSession.endTime)
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
                        onExtendSession(activeSession.id, 60, activeSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 1 hour to session", Toast.LENGTH_SHORT).show()
                    }
                    val canAdd120 = maxExtensionMillis >= 120 * 60 * 1000L
                    AddTimeOptionButton("+2 hours", 120, enabled = canAdd120, modifier = Modifier.weight(1f)) {
                        onExtendSession(activeSession.id, 120, activeSession.endTime)
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
    if (showReminderSheet && activeSession != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showReminderSheet = false },
            sheetState = sheetState,
            containerColor = TimerCardBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                    color = TimerTextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Receive a push alert before your parking limit expires.",
                    fontSize = 13.sp,
                    color = TimerTextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                val reminderOptions = listOf(
                    5 to "5 minutes before",
                    10 to "10 minutes before",
                    15 to "15 minutes before (Recommended)",
                    30 to "30 minutes before",
                    0 to "Turn off reminders"
                )

                reminderOptions.forEach { (mins, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedReminderMinutes = mins
                                onUpdateReminder(activeSession.id, mins)
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
                            fontWeight = if (selectedReminderMinutes == mins) FontWeight.Bold else FontWeight.Normal,
                            color = TimerTextDark
                        )
                        RadioButton(
                            selected = selectedReminderMinutes == mins,
                            onClick = {
                                selectedReminderMinutes = mins
                                onUpdateReminder(activeSession.id, mins)
                                showReminderSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = TimerProgressGreen)
                        )
                    }
                    HorizontalDivider(color = TimerDividerColor)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 3: PARKING RULES BREAKDOWN
    // ==========================================
    if (showRulesSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showRulesSheet = false },
            sheetState = sheetState,
            containerColor = TimerCardBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                            .background(TimerBannerBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TimerBannerGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Active Parking Rules",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TimerTextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BentoCanvas,
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val activeRuleStr = activeSession?.parkingRuleSummary?.ifBlank { null }
                            ?: activeSession?.notes?.ifBlank { null }
                            ?: "2 HR PARKING"
                        RuleItem("ACTIVE REGULATION", activeRuleStr)
                        Spacer(modifier = Modifier.height(10.dp))
                        RuleItem("NOTIFICATIONS", "Smart push notifications will alert you prior to restriction enforcement.")
                        Spacer(modifier = Modifier.height(10.dp))
                        RuleItem("HOLIDAY EXCEPTIONS", "Free parking on Sundays and major City Holidays unless posted otherwise.")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showRulesSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TimerTextDark)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold, color = BentoWhite)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 4: INTERACTIVE MAP & PIN
    // ==========================================
    if (showMapSheet && activeSession != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showMapSheet = false },
            sheetState = sheetState,
            containerColor = TimerCardBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                    color = TimerTextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (activeSession.notes.isNotBlank()) "${activeSession.locationName} • ${activeSession.notes}" else activeSession.locationName,
                    fontSize = 13.sp,
                    color = TimerTextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Simulated Map Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
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
                                    .background(TimerProgressGreen)
                                    .border(2.dp, TimerCardBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = TimerCardBg,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Spot GPS: 37.7879° N, 122.4075° W",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TimerTextDark
                            )
                            Text(
                                text = "2 min walk from current location",
                                fontSize = 11.sp,
                                color = TimerTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Location copied to clipboard", Toast.LENGTH_SHORT).show()
                            showMapSheet = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.dp, BentoBorder)
                    ) {
                        Text("Copy Address", fontWeight = FontWeight.Bold, color = TimerTextDark)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Opening walking directions...", Toast.LENGTH_SHORT).show()
                            showMapSheet = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TimerTextDark)
                    ) {
                        Icon(Icons.Default.Directions, null, tint = BentoWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Navigate", fontWeight = FontWeight.Bold, color = BentoWhite)
                    }
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
    icon: ImageVector,
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
                    tint = TimerTextDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TimerTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TimerTextMuted
                )
            }
        }

        trailingContent()
    }
}

@Composable
private fun QuickPresetPill(
    label: String,
    minutes: Int,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isPrimary) BentoPeach else BentoCanvas,
        border = BorderStroke(1.dp, if (isPrimary) BentoPrimary else BentoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) BentoPrimaryDark else TimerTextDark
            )
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(16.dp),
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
                color = if (enabled) TimerTextDark else TimerTextMuted.copy(alpha = 0.5f)
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
            color = TimerTextDark,
            lineHeight = 18.sp
        )
    }
}

