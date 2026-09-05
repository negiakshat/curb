package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ActiveParkingSession
import com.example.data.model.ParkingSpot
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.RadiusSmall
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
import kotlin.math.cos
import kotlin.math.sin

// Precise Vibrant Green Palette matching the screenshot layout
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
        // TOP APP BAR: Back button | "Parking Timer" | 3-dots More Menu
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
                // Pre-calculated values for the cards
                val totalDuration = (activeSession.endTime - activeSession.startTime).coerceAtLeast(1000L)
                val remaining = (activeSession.endTime - currentTimeMillis)
                val isExpired = remaining <= 0
                val rawProgress = if (isExpired) 0f else (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                val animatedProgress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "timer_progress"
                )

                val totalSeconds = (remaining / 1000).coerceAtLeast(0L)
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val timeDisplay = if (isExpired) "0m" else if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

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

                // ==========================================
                // 1. TOP HERO CARD (CIRCULAR GAUGE & LOCATION)
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
                            .padding(top = 22.dp, bottom = 18.dp, start = 18.dp, end = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header pill (Active vs Expired)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isExpired) CurbErrorContainer else TimerBannerBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isExpired) "!" else "P",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpired) CurbError else TimerBannerGreen
                                )
                            }

                            Text(
                                text = if (isExpired) "Restriction active • Expired" else "Active parking",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExpired) CurbError else TimerBannerGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // CIRCULAR GAUGE (Exact visual recreation)
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 11.dp.toPx()
                                val diameter = size.minDimension - strokeWidth
                                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                                val arcSize = Size(diameter, diameter)

                                // 1. Background full track
                                drawArc(
                                    color = if (isExpired) CurbErrorContainer else TimerTrackGreen,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )

                                // 2. Foreground active progress arc
                                val sweep = if (isExpired) 360f else animatedProgress * 360f
                                drawArc(
                                    color = if (isExpired) CurbError else TimerProgressGreen,
                                    startAngle = -90f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // 3. Indicator knob at the progress endpoint
                                val radius = diameter / 2f
                                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                                val endAngleRad = Math.toRadians((-90f + sweep).toDouble())
                                val knobCenter = Offset(
                                    x = centerOffset.x + (radius * cos(endAngleRad)).toFloat(),
                                    y = centerOffset.y + (radius * sin(endAngleRad)).toFloat()
                                )

                                drawCircle(
                                    color = TimerCardBg,
                                    radius = strokeWidth * 0.85f,
                                    center = knobCenter
                                )
                                drawCircle(
                                    color = if (isExpired) CurbError else TimerProgressGreen,
                                    radius = strokeWidth * 0.55f,
                                    center = knobCenter
                                )
                            }

                            // Center Content Inside Circle
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isExpired) "Status" else "Time remaining",
                                    fontSize = 13.sp,
                                    color = TimerTextMuted,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (isExpired) "EXPIRED" else timeDisplay,
                                    fontSize = if (isExpired) 26.sp else 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpired) CurbError else TimerTextDark,
                                    letterSpacing = (-0.5).sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Limit: $limitDisplay",
                                    fontSize = 13.sp,
                                    color = TimerTextMuted
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (isExpired) "Expired at" else "Expires at",
                                    fontSize = 12.sp,
                                    color = TimerTextMuted
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = activeSession.allowedUntilTime,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpired) CurbError else TimerProgressGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // LOCATION ROW (Inside Hero Card)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BentoCanvas,
                            border = BorderStroke(1.dp, BentoBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMapSheet = true }
                                .testTag("timer_location_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(TimerCardBg)
                                            .border(1.dp, BentoBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = TimerTextDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = activeSession.locationName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TimerTextDark
                                        )
                                        Text(
                                            text = if (activeSession.notes.isNotBlank()) activeSession.notes else "Active parking spot",
                                            fontSize = 12.sp,
                                            color = TimerTextMuted
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = TimerCardBg,
                                    border = BorderStroke(1.dp, BentoBorder),
                                    modifier = Modifier.clip(RoundedCornerShape(RadiusChip))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = TimerTextDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "View on map",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TimerTextDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // SAVE PARKING SPOT ACTION
                // ==========================================
                if (savedParkingSpot != null) {
                    Surface(
                        shape = RoundedCornerShape(RadiusCard),
                        color = TimerCardBg,
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_parking_spot_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE6F4EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF137333),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Parking spot saved",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TimerTextDark
                                )
                                if (savedParkingSpot.locationName.isNotBlank()) {
                                    Text(
                                        text = savedParkingSpot.locationName,
                                        fontSize = 12.sp,
                                        color = TimerTextMuted
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToFindMyCar,
                                shape = RoundedCornerShape(RadiusCard),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BentoPrimaryDark,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("find_my_car_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FIND MY CAR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
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
                            shape = RoundedCornerShape(RadiusCard),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BentoPrimaryDark,
                                contentColor = Color.White,
                                disabledContainerColor = TimerCardBg,
                                disabledContentColor = TimerTextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_parking_spot_button")
                        ) {
                            if (isSavingParkingSpot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Saving parking spot…",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SAVE MY PARKING SPOT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (!parkingSpotSaveError.isNullOrBlank()) {
                            Text(
                                text = parkingSpotSaveError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .testTag("parking_spot_save_error")
                            )
                        }
                    }
                }

                // ==========================================
                // 2. DETAILS LIST CARD
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
                            .padding(vertical = 8.dp)
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

                        // Row 2: Parking limit
                        TimerDetailRow(
                            icon = Icons.Default.Timer,
                            title = "Parking limit",
                            subtitle = limitDisplay,
                            trailingContent = {
                                Text(
                                    text = "Metered parking",
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

                        // Row 3: Reminder
                        val reminderText = if (selectedReminderMinutes > 0) {
                            "$selectedReminderMinutes min before expiry"
                        } else {
                            "Disabled"
                        }

                        TimerDetailRow(
                            icon = Icons.Default.NotificationsNone,
                            title = "Reminder",
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

                        // Row 4: Parking rules
                        TimerDetailRow(
                            icon = Icons.Default.Info,
                            title = "Parking rules",
                            subtitle = "2 Hour Parking, 8 AM – 6 PM, Mon – Fri",
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

                // ==========================================
                // 3. NOTIFICATION BANNER
                // ==========================================
                AnimatedVisibility(
                    visible = isNotificationBannerVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = TimerBannerBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TimerBannerGreen,
                                    modifier = Modifier.size(22.dp)
                                )

                                Column {
                                    Text(
                                        text = "We’ll notify you before your time runs out.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TimerTextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "You can update reminders in Settings.",
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
        // 4. BOTTOM ACTION BUTTONS
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
                // LEFT BUTTON: "Add time" (White/Sand outlined card button)
                OutlinedButton(
                    onClick = { showAddTimeSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("add_time_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = TimerCardBg,
                        contentColor = TimerTextDark
                    ),
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Text(
                        text = "Add time",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TimerTextDark
                    )
                }

                // RIGHT BUTTON: "End parking session" (Black filled button)
                Button(
                    onClick = { onEndSession(activeSession.id) },
                    modifier = Modifier
                        .weight(1.25f)
                        .height(54.dp)
                        .testTag("end_parking_session_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TimerTextDark,
                        contentColor = BentoWhite
                    )
                ) {
                    Text(
                        text = "End parking session",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoWhite
                    )
                }
            } else {
                Button(
                    onClick = { onStartQuickTimer(135, "2h 30m limit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_quick_session_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TimerTextDark,
                        contentColor = BentoWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = BentoWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Parking Session (2h 15m)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoWhite
                    )
                }
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET 1: ADD TIME (EXTEND SESSION)
    // ==========================================
    if (showAddTimeSheet && activeSession != null) {
        val sheetState = rememberModalBottomSheetState()
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
                Text(
                    text = "Choose how many minutes to add to your current session.",
                    fontSize = 13.sp,
                    color = TimerTextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AddTimeOptionButton("+15 min", 15, modifier = Modifier.weight(1f)) {
                        onExtendSession(activeSession.id, 15, activeSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 15 minutes to session", Toast.LENGTH_SHORT).show()
                    }
                    AddTimeOptionButton("+30 min", 30, modifier = Modifier.weight(1f)) {
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
                    AddTimeOptionButton("+1 hour", 60, modifier = Modifier.weight(1f)) {
                        onExtendSession(activeSession.id, 60, activeSession.endTime)
                        showAddTimeSheet = false
                        Toast.makeText(context, "Added 1 hour to session", Toast.LENGTH_SHORT).show()
                    }
                    AddTimeOptionButton("+2 hours", 120, modifier = Modifier.weight(1f)) {
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
                        RuleItem("2 HR PARKING", "8:00 AM – 6:00 PM, Mon – Fri. Meter rate $3.50/hr applies.")
                        Spacer(modifier = Modifier.height(10.dp))
                        RuleItem("STREET SWEEPING", "Tue & Thu, 8:00 AM – 10:00 AM. Vehicles will be ticketed/towed.")
                        Spacer(modifier = Modifier.height(10.dp))
                        RuleItem("FREE PARKING", "Evenings after 6:00 PM and all day Sundays & City Holidays.")
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
                    color = TimerTextMuted
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
                                text = "2 min walk (180 ft from current spot)",
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = BentoCanvas,
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TimerTextDark
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
