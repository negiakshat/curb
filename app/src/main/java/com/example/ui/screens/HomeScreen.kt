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
import androidx.compose.runtime.remember
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
    onAskCurbClicked: () -> Unit,
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

    val allowedCount = recentScans.count { it.verdict == ScanVerdict.ALLOWED }
    val restrictedCount = recentScans.count { it.verdict == ScanVerdict.RESTRICTED }
    val ambiguousCount = recentScans.count { it.verdict == ScanVerdict.AMBIGUOUS }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // TOP APP BAR & USER IDENTITY
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CurbLogo(symbolSize = 30.dp, fontSize = 22, tint = BentoPrimaryDark)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNotificationsClicked,
                        modifier = Modifier.testTag("notification_bell_button")
                    ) {
                        androidx.compose.foundation.layout.Box {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = BentoTextPrimary
                            )
                            if (hasUnreadNotifications) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(CurbError, shape = androidx.compose.foundation.shape.CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            }
        }

        // GREETING & DATE HEADER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                val (dotColor, locationLabel) = remember(userLocationResult) {
                    when (userLocationResult) {
                        is com.example.data.location.UserLocationResult.Success -> {
                            val loc = userLocationResult
                            val cityOrLocality = loc.cityState.ifBlank { loc.locationName }
                            Pair(CurbSuccess, if (cityOrLocality.isNotBlank() && cityOrLocality != "Current Location") cityOrLocality else "Location active")
                        }
                        is com.example.data.location.UserLocationResult.PermissionRequired -> {
                            Pair(Color(0xFFFF9800), "Location unavailable")
                        }
                        is com.example.data.location.UserLocationResult.Unavailable -> {
                            val msg = userLocationResult.message
                            if (msg.contains("Checking", ignoreCase = true) || msg.contains("Getting", ignoreCase = true)) {
                                Pair(Color(0xFFFF9800), "Getting location…")
                            } else {
                                Pair(Color(0xFFFF9800), "Location unavailable")
                            }
                        }
                    }
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
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                            color = BentoTextSecondary
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
            Spacer(modifier = Modifier.height(12.dp))
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
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isPro) {
                                BentoPillBadge(
                                    text = "PRO • UNLIMITED",
                                    backgroundColor = BentoPrimaryDark,
                                    textColor = BentoPeach
                                )
                            } else {
                                BentoPillBadge(
                                    text = usageInfo.displayText.uppercase(),
                                    backgroundColor = if (usageInfo.isLimitReached) CurbErrorContainer else BentoSand,
                                    textColor = if (usageInfo.isLimitReached) CurbError else BentoPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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

                        Spacer(modifier = Modifier.height(14.dp))

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
        if (activeSession != null && activeSession.isActive && activeSession.remainingMillis > 0) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BentoPeach),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalParking,
                                        contentDescription = null,
                                        tint = BentoPrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
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
                                    Text(
                                        text = activeSession.remainingFormatted,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoWhite
                                    )
                                    Text(
                                        text = activeSession.locationName,
                                        fontSize = 12.sp,
                                        color = BentoWhite.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = BentoPeach
                                ) {
                                    Text(
                                        text = "VIEW TIMER",
                                        color = BentoPrimaryDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                if (savedParkingSpot != null) {
                                    Surface(
                                        shape = RoundedCornerShape(RadiusChip),
                                        color = BentoWhite.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable { onFindMyCarClicked() }
                                    ) {
                                        Text(
                                            text = "FIND CAR",
                                            color = BentoWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // USEFUL SHORTCUTS
        item {
            Spacer(modifier = Modifier.height(12.dp))
            if (savedParkingSpot != null) {
                // 2-COLUMN ROW WHEN BOTH SAVED SPOTS AND CAR LOCATION EXIST
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onSavedPlacesClicked() }
                            .testTag("bento_saved_places_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoSand),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
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

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "Saved Spots",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = "Quick access",
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onFindMyCarClicked() }
                            .testTag("bento_find_my_car_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoSand),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
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

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "Find My Car",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = "Parked location",
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                // SINGLE FULL-WIDTH CARD WHEN NO SAVED CAR LOCATION
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(RadiusCard))
                            .clickable { onSavedPlacesClicked() }
                            .testTag("bento_saved_places_tile"),
                        shape = RoundedCornerShape(RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = BentoSand),
                        border = BorderStroke(1.dp, BentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BentoWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "Saved Spots",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = "Keep track of frequent parking locations",
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // BENTO SUMMARY SECTION: RECENT SCANS
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (recentScans.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    CurbCard(
                        cornerRadius = 24.dp,
                        backgroundColor = BentoWhite,
                        borderColor = BentoBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                        }
                    }
                }
            }

            items(recentScans.take(3)) { scan ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                    ) {
                    RecentScanCard(
                        scan = scan,
                        onClick = { onScanResultClicked(scan) }
                    )
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    CurbCard(
                        cornerRadius = 16.dp,
                        backgroundColor = BentoWhite,
                        borderColor = BentoBorder
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BentoSand),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "No recent scans",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = "Tap 'Start instant scan' above to check a spot.",
                                    fontSize = 11.sp,
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

@Composable
fun RecentScanCard(
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

    CurbCard(
        cornerRadius = 24.dp,
        backgroundColor = BentoWhite,
        borderColor = BentoBorder,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )

                Column {
                    Text(
                        text = scan.locationName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = BentoTextSecondary
                    )
                }
            }

            CurbVerdictBadge(verdict = scan.verdict)
        }
    }
}
