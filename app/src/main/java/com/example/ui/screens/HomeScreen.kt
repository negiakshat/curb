package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScanUsageInfo
import com.example.data.model.ActiveParkingSession
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.UserProfile
import com.example.ui.components.BentoPillBadge
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbSegmentedStatusBar
import com.example.ui.components.CurbVerdictBadge
import com.example.ui.theme.BentoBeige
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoBorderStrong
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
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.CurbWarningContainer
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.RadiusSmall
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HomeScreen(
    userProfile: UserProfile,
    isPro: Boolean = userProfile.isPro,
    activeSession: ActiveParkingSession?,
    savedParkingSpot: com.example.data.model.ParkingSpot? = null,
    recentScans: List<ScanResult>,
    usageInfo: ScanUsageInfo = ScanUsageInfo(0),
    userLocationResult: com.example.data.location.UserLocationResult = com.example.data.location.UserLocationResult.Unavailable("Checking location…"),
    onScanClicked: () -> Unit,
    onParkingTimerClicked: () -> Unit,
    onFindMyCarClicked: () -> Unit = {},
    onSavedPlacesClicked: () -> Unit,
    onActivityClicked: () -> Unit,
    onAskCurbClicked: () -> Unit = {},
    onScanResultClicked: (ScanResult) -> Unit,
    onNotificationsClicked: () -> Unit,
    hasUnreadNotifications: Boolean = false,
    onProfileClicked: () -> Unit,
    onUpgradeToProClicked: () -> Unit = {}
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val todayDateStr = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    // 1-second ticker for live active timer update
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activeSession?.id, activeSession?.isActive) {
        if (activeSession?.isActive == true) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    val liveRemainingMillis = remember(nowMillis, activeSession) {
        if (activeSession != null && activeSession.isActive) {
            maxOf(0L, activeSession.endTime - nowMillis)
        } else {
            0L
        }
    }

    val liveRemainingFormatted = remember(nowMillis, activeSession) {
        if (activeSession != null && activeSession.isActive) {
            val totalSeconds = liveRemainingMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) {
                "${hours}h ${minutes}m remaining"
            } else {
                "${minutes}m remaining"
            }
        } else {
            ""
        }
    }

    val allowedCount = recentScans.count { it.verdict == ScanVerdict.ALLOWED }
    val restrictedCount = recentScans.count { it.verdict == ScanVerdict.RESTRICTED }
    val ambiguousCount = recentScans.count { it.verdict == ScanVerdict.AMBIGUOUS }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP APP BAR & USER IDENTITY
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CurbLogo(symbolSize = 30.dp, fontSize = 22, tint = BentoPrimaryDark)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onProfileClicked,
                            modifier = Modifier.testTag("home_profile_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BentoSand),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.name.take(1).uppercase(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimaryDark
                                )
                            }
                        }

                        IconButton(
                            onClick = onNotificationsClicked,
                            modifier = Modifier.testTag("notification_bell_button")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "Notifications",
                                    tint = BentoTextPrimary
                                )
                                if (hasUnreadNotifications) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(CurbError, shape = CircleShape)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val (dotColor, locationLabel) = remember(userLocationResult) {
                    formatHomeLocationLabel(userLocationResult)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BentoSand,
                        border = BorderStroke(1.dp, BentoBorder)
                    ) {
                        Text(
                            text = todayDateStr.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(dotColor, CircleShape)
                        )

                        Text(
                            text = locationLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BentoTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$greeting, ${userProfile.name}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Your AI parking co-pilot is ready.",
                    fontSize = 13.sp,
                    color = BentoTextSecondary,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // BENTO GRID HERO TILE: SCAN THIS SPOT (PRIMARY ACTION)
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusHero))
                        .clickable { onScanClicked() }
                        .testTag("scan_this_spot_card"),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = BentoPeach),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (isPro) {
                                BentoPillBadge(
                                    text = "PRO • UNLIMITED",
                                    backgroundColor = BentoPrimaryDark,
                                    textColor = BentoPeach
                                )
                            } else {
                                val remainingCount = usageInfo.scansRemaining
                                val counterLabel = if (usageInfo.isLimitReached) {
                                    "0 SCANS LEFT"
                                } else {
                                    "$remainingCount ${if (remainingCount == 1) "SCAN LEFT" else "SCANS LEFT"}"
                                }
                                BentoPillBadge(
                                    text = counterLabel,
                                    backgroundColor = if (usageInfo.isLimitReached) CurbErrorContainer else BentoPrimaryDark.copy(alpha = 0.12f),
                                    textColor = if (usageInfo.isLimitReached) CurbError else BentoPrimaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Scan parking signs",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark,
                            letterSpacing = (-0.3).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Point camera at signs to check real-time rules, street cleaning, and meter limits.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = BentoTextDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PROMINENT SINGLE FULL-WIDTH CTA BUTTON
                        CurbPrimaryButton(
                            text = "START INSTANT SCAN",
                            onClick = onScanClicked,
                            leadingIcon = Icons.Default.CameraAlt,
                            backgroundColor = BentoPrimaryDark,
                            contentColor = BentoWhite,
                            testTag = "home_scan_primary_button"
                        )
                    }
                }
            }
        }

        // ACTIVE PARKING SESSION PROMINENT BANNER (IF SESSION IS ACTIVE)
        if (activeSession != null && activeSession.isActive && liveRemainingMillis > 0) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onParkingTimerClicked() }
                            .testTag("bento_active_parking_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoPrimaryDark),
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BentoPeach),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalParking,
                                        contentDescription = null,
                                        tint = BentoPrimaryDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(CurbSuccess, CircleShape)
                                        )
                                        Text(
                                            text = "PARKED NOW",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoPeach,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = liveRemainingFormatted,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoWhite
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeSession.locationName,
                                        fontSize = 12.sp,
                                        color = BentoWhite.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (savedParkingSpot != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(RadiusChip))
                                            .clickable { onParkingTimerClicked() },
                                        shape = RoundedCornerShape(RadiusChip),
                                        color = BentoPeach
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "VIEW TIMER",
                                                color = BentoPrimaryDark,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(RadiusChip))
                                            .clickable { onFindMyCarClicked() },
                                        shape = RoundedCornerShape(RadiusChip),
                                        color = BentoWhite.copy(alpha = 0.2f)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "FIND CAR",
                                                color = BentoWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(RadiusChip))
                                        .clickable { onParkingTimerClicked() },
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = BentoPeach
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "VIEW TIMER",
                                            color = BentoPrimaryDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECONDARY BENTO GRID (2x2 MATRIX): SAVED SPOTS, ASK CURB AI, TIMER, FIND CAR / HISTORY
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ROW 1: Saved Spots + Parking Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tile 1: Saved Spots
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onSavedPlacesClicked() }
                            .testTag("bento_saved_places_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoSand),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BentoWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Saved Spots",
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Quick locations",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = BentoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Tile 2: Parking Timer
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onParkingTimerClicked() }
                            .testTag("bento_parking_timer_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoSand),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BentoWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Parking Timer",
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (activeSession?.isActive == true) "Session active" else "Set countdown",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = BentoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ROW 2: Find My Car + Activity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tile 3: Find My Car
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onFindMyCarClicked() }
                            .testTag("bento_find_my_car_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoSand),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BentoWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = BentoPrimaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Find My Car",
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (savedParkingSpot != null) "Parked location" else "Save & locate",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = BentoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Tile 4: Activity History
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onActivityClicked() }
                            .testTag("bento_activity_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoSand),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BentoWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Activity",
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Scan history",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = BentoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // ACTIVITY BENTO SECTION: VERDICT SUMMARY + RECENT SCANS INTEGRATED
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary,
                        letterSpacing = (-0.3).sp
                    )

                    if (recentScans.isNotEmpty()) {
                        Text(
                            text = "View all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary,
                            modifier = Modifier
                                .clickable { onActivityClicked() }
                                .testTag("view_all_activity_link")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Integrated Activity Bento Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusHero)),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (recentScans.isNotEmpty()) {
                            // Verdict Summary Bar Top Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "VERDICT SUMMARY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$allowedCount allowed • $restrictedCount restricted",
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            CurbSegmentedStatusBar(
                                allowedCount = allowedCount,
                                restrictedCount = restrictedCount,
                                ambiguousCount = ambiguousCount
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Recent Scans Rows inside the bento surface
                            recentScans.take(3).forEachIndexed { index, scan ->
                                RecentScanRowItem(
                                    scan = scan,
                                    onClick = { onScanResultClicked(scan) }
                                )
                                if (index < recentScans.take(3).lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = BentoBorder.copy(alpha = 0.5f),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        } else {
                            // Clean empty state inside the Activity Bento card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BentoSand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = BentoPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "No recent scans",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextPrimary
                                    )
                                    Text(
                                        text = "Tap 'Start instant scan' above to check a spot.",
                                        fontSize = 12.sp,
                                        color = BentoTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentScanRowItem(
    scan: ScanResult,
    onClick: () -> Unit
) {
    val dateStr = remember(scan.timestamp) {
        val now = System.currentTimeMillis()
        val diff = now - scan.timestamp
        when {
            diff < 24 * 60 * 60 * 1000L -> "Today • " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(scan.timestamp))
            diff < 48 * 60 * 60 * 1000L -> "Yesterday • " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(scan.timestamp))
            else -> SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(scan.timestamp))
        }
    }

    val (statusColor, statusText) = when (scan.verdict) {
        ScanVerdict.ALLOWED -> Pair(CurbSuccess, "Parking allowed")
        ScanVerdict.RESTRICTED -> Pair(CurbError, "Parking restricted")
        ScanVerdict.AMBIGUOUS -> Pair(CurbWarning, "Rule unclear")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusNested))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 2.dp),
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
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.locationName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = BentoTextSecondary
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = BentoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        CurbVerdictBadge(verdict = scan.verdict)
    }
}

@Composable
fun RecentScanCard(
    scan: ScanResult,
    onClick: () -> Unit
) {
    RecentScanRowItem(scan = scan, onClick = onClick)
}

fun selectUserFacingLocationLabel(
    locationName: String,
    cityState: String,
    formattedDisplay: String
): String {
    val rawCoordPattern = Regex("""^\-?\d+(\.\d+)?\s*,\s*\-?\d+(\.\d+)?$""")
    val gpsPattern = Regex("""^GPS\s*\(.*\)""", RegexOption.IGNORE_CASE)

    fun isValidLabel(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.equals("Current Location", ignoreCase = true)) return false
        if (trimmed.equals("Location unavailable", ignoreCase = true)) return false
        if (rawCoordPattern.matches(trimmed)) return false
        if (gpsPattern.containsMatchIn(trimmed)) return false
        if (trimmed.contains("IP", ignoreCase = true) || trimmed.contains("network-provider", ignoreCase = true)) return false
        return true
    }

    if (isValidLabel(locationName)) return locationName.trim()
    if (isValidLabel(cityState)) return cityState.trim()
    if (isValidLabel(formattedDisplay)) return formattedDisplay.trim()

    return "Location active"
}

fun formatHomeLocationLabel(result: com.example.data.location.UserLocationResult): Pair<Color, String> {
    return when (result) {
        is com.example.data.location.UserLocationResult.Success -> {
            val label = selectUserFacingLocationLabel(
                locationName = result.locationName,
                cityState = result.cityState,
                formattedDisplay = result.formattedDisplay
            )
            Pair(CurbSuccess, label)
        }
        is com.example.data.location.UserLocationResult.PermissionRequired -> {
            Pair(Color(0xFFFF9800), "Location unavailable")
        }
        is com.example.data.location.UserLocationResult.Unavailable -> {
            val msg = result.message
            if (msg.contains("Checking", ignoreCase = true) || msg.contains("Getting", ignoreCase = true)) {
                Pair(Color(0xFFFF9800), "Getting location…")
            } else {
                Pair(Color(0xFFFF9800), "Location unavailable")
            }
        }
    }
}
