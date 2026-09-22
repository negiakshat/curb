package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.location.UserLocationResult
import com.example.data.model.ParkingSpot
import com.example.data.remote.WalkingRoute
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@Composable
fun FindMyCarScreen(
    savedParkingSpot: ParkingSpot?,
    userLocationState: UserLocationResult?,
    walkingRoute: WalkingRoute? = null,
    onRefreshLocation: () -> Unit,
    onNavigateBack: () -> Unit,
    onStartLiveTracking: () -> Unit = {},
    onStopLiveTracking: () -> Unit = {},
    onUpdateWalkingRoute: (Double, Double, Double, Double) -> Unit = { _, _, _, _ -> },
    onNavigateToParkingTimer: () -> Unit = {}
) {
    val context = LocalContext.current
    var isRefreshingLoc by remember { mutableStateOf(false) }

    // Request location permissions if not granted
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            onStartLiveTracking()
        } else {
            Toast.makeText(context, "Location permission is required for live tracking to your car", Toast.LENGTH_SHORT).show()
        }
    }

    // Start continuous location updates when screen is open and spot exists
    DisposableEffect(savedParkingSpot) {
        if (savedParkingSpot != null) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onStartLiveTracking()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        onDispose {
            onStopLiveTracking()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .testTag("find_my_car_screen")
    ) {
        // TOP HEADER (Compact Bento Header Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Circular Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BentoWhite)
                        .border(1.dp, BentoBorder, CircleShape)
                        .clickable { onNavigateBack() }
                        .testTag("find_my_car_back_button"),
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
                    text = "Find My Car",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (savedParkingSpot != null) {
                // Circular Refresh Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BentoWhite)
                        .border(1.dp, BentoBorder, CircleShape)
                        .clickable {
                            isRefreshingLoc = true
                            onRefreshLocation()
                            isRefreshingLoc = false
                        }
                        .testTag("refresh_location_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Location",
                        tint = BentoTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (savedParkingSpot == null) {
            // EMPTY STATE when no parking spot saved
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(RadiusHero),
                    colors = CardDefaults.cardColors(containerColor = BentoWhite),
                    border = BorderStroke(1.dp, BentoBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("no_spot_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
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
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "No Parking Spot Saved",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Save your parking spot from the Parking Timer screen after parking to locate your car here.",
                            fontSize = 14.sp,
                            color = BentoTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CurbPrimaryButton(
                            text = "GO TO PARKING TIMER",
                            onClick = onNavigateToParkingTimer,
                            leadingIcon = Icons.Default.Place,
                            backgroundColor = BentoPrimaryDark,
                            testTag = "go_to_timer_button"
                        )
                    }
                }
            }
        } else {
            // ACTIVE MAP VIEW
            var mapViewRef by remember { mutableStateOf<MapView?>(null) }

            // Extract success user location if available
            val successUserLoc = userLocationState as? UserLocationResult.Success

            // Trigger walking route fetch/update when user position or car spot changes
            LaunchedEffect(savedParkingSpot, successUserLoc) {
                if (savedParkingSpot != null && successUserLoc != null) {
                    onUpdateWalkingRoute(
                        successUserLoc.latitude,
                        successUserLoc.longitude,
                        savedParkingSpot.latitude,
                        savedParkingSpot.longitude
                    )
                }
            }

            // Calculate distance & walking time
            val distanceAndWalk = remember(savedParkingSpot, successUserLoc, walkingRoute) {
                if (savedParkingSpot != null && successUserLoc != null) {
                    val meters = walkingRoute?.distanceMeters ?: run {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            successUserLoc.latitude,
                            successUserLoc.longitude,
                            savedParkingSpot.latitude,
                            savedParkingSpot.longitude,
                            results
                        )
                        results[0].toDouble()
                    }
                    val feet = (meters * 3.28084).toInt()
                    val miles = meters / 1609.34

                    if (meters < 6.0 || feet < 20) {
                        // User is effectively at their car
                        Triple("You're at your car", null, false)
                    } else {
                        val isReal = walkingRoute?.isRealFootRoute ?: false
                        val distanceText = if (feet < 528) {
                            "${meters.toInt()} m away ($feet ft)"
                        } else {
                            String.format(Locale.US, "%.1f mi away (%.1f km)", miles, meters / 1000.0)
                        }

                        val walkSeconds = walkingRoute?.durationSeconds ?: (meters / 1.33)
                        val walkMinutes = Math.max(1, Math.round(walkSeconds / 60.0).toInt())
                        val walkText = if (isReal) "$walkMinutes min walk" else "~$walkMinutes min walk"

                        Triple(distanceText, walkText, !isReal)
                    }
                } else {
                    Triple(null, null, false)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // REAL MAP CANVAS (OsmDroid MapView)
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(18.5)

                            val carGeoPoint = GeoPoint(savedParkingSpot.latitude, savedParkingSpot.longitude)
                            controller.setCenter(carGeoPoint)

                            mapViewRef = this
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()

                        // 0. WALKING ROUTE POLYLINE (Drawn under markers)
                        val currentRoute = walkingRoute
                        if (currentRoute != null && currentRoute.points.size >= 2) {
                            val polyline = Polyline(mapView).apply {
                                setPoints(currentRoute.points)
                                outlinePaint.color = android.graphics.Color.parseColor("#2563EB") // High-contrast Blue
                                outlinePaint.strokeWidth = 14f
                                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                outlinePaint.isAntiAlias = true
                            }
                            mapView.overlays.add(polyline)
                        }

                        // 1. CAR MARKER (Fixed at saved parking coordinates)
                        val carPoint = GeoPoint(savedParkingSpot.latitude, savedParkingSpot.longitude)
                        val carMarker = Marker(mapView).apply {
                            position = carPoint
                            title = "Car Location"
                            snippet = if (savedParkingSpot.locationName.isNotBlank()) savedParkingSpot.locationName else "Saved Parking Spot"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_car_pin)
                        }
                        mapView.overlays.add(carMarker)

                        // 2. YOU MARKER (Dynamically updated with user real-time location)
                        if (successUserLoc != null) {
                            val userPoint = GeoPoint(successUserLoc.latitude, successUserLoc.longitude)
                            val userMarker = Marker(mapView).apply {
                                position = userPoint
                                title = "YOU"
                                snippet = "Your Current Location"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = ContextCompat.getDrawable(context, R.drawable.ic_user_pin)
                            }
                            mapView.overlays.add(userMarker)
                        }

                        mapView.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("find_my_car_map_view")
                )

                // LIVE TRACKING BADGE OVERLAY
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BentoWhite.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, BentoBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .testTag("live_tracking_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CurbSuccess)
                        )
                        Text(
                            text = if (successUserLoc != null) "LIVE LOCATION" else "CONNECTING GPS...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // BOTTOM CONTAINER: FLOATING CONTROLS + BOTTOM BENTO PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // FLOATING MAP CONTROLS (Center on Car / Center on Me)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                onClick = {
                                    mapViewRef?.controller?.animateTo(
                                        GeoPoint(savedParkingSpot.latitude, savedParkingSpot.longitude)
                                    )
                                },
                                shape = CircleShape,
                                color = BentoWhite,
                                border = BorderStroke(1.dp, BentoBorder),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("floating_center_car_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Center on Car",
                                        tint = BentoPrimaryDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    if (successUserLoc != null) {
                                        mapViewRef?.controller?.animateTo(
                                            GeoPoint(successUserLoc.latitude, successUserLoc.longitude)
                                        )
                                    } else {
                                        onStartLiveTracking()
                                    }
                                },
                                shape = CircleShape,
                                color = BentoWhite,
                                border = BorderStroke(1.dp, BentoBorder),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("floating_center_me_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Center on Me",
                                        tint = BentoPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // BOTTOM BENTO OVERLAY PANEL
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("find_my_car_info_card"),
                        shape = RoundedCornerShape(RadiusHero),
                        color = BentoWhite,
                        border = BorderStroke(1.dp, BentoBorder),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // LEVEL 1: IMMEDIATE STATE (ETA / DISTANCE / AT CAR)
                            if (distanceAndWalk.first == "You're at your car") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CurbSuccess,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "You're at your car",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPrimaryDark
                                    )
                                }
                            } else if (distanceAndWalk.second != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = distanceAndWalk.second ?: "",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPrimaryDark
                                    )
                                    distanceAndWalk.first?.let { distStr ->
                                        Text(
                                            text = distStr,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BentoTextSecondary
                                        )
                                    }
                                }
                            } else if (userLocationState == null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = BentoPrimaryDark
                                    )
                                    Text(
                                        text = "Locating your position…",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BentoTextSecondary
                                    )
                                }
                            } else if (userLocationState is UserLocationResult.PermissionRequired) {
                                Text(
                                    text = "Location permission required for route",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BentoTextSecondary
                                )
                            } else {
                                Text(
                                    text = "Walking route unavailable",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BentoTextSecondary
                                )
                            }

                            // LEVEL 2: LOCATION INFORMATION (Name + Saved time)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BentoSand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = BentoPrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (savedParkingSpot.locationName.isNotBlank()) savedParkingSpot.locationName else "Saved Parking Spot",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val timeDiffMinutes = remember(savedParkingSpot.timestamp) {
                                        ((System.currentTimeMillis() - savedParkingSpot.timestamp) / 60000).coerceAtLeast(0)
                                    }
                                    val savedAgoText = remember(timeDiffMinutes) {
                                        if (timeDiffMinutes < 1) "Saved just now"
                                        else if (timeDiffMinutes < 60) "Saved $timeDiffMinutes min ago"
                                        else {
                                            val hours = timeDiffMinutes / 60
                                            "Saved $hours ${if (hours == 1L) "hour" else "hours"} ago"
                                        }
                                    }

                                    Text(
                                        text = savedAgoText,
                                        fontSize = 12.sp,
                                        color = BentoTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // LEVEL 3: ROUTE METADATA (only when route data exists and not at car)
                            if (distanceAndWalk.third && distanceAndWalk.second != null) {
                                Surface(
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = BentoSand,
                                    border = BorderStroke(1.dp, BentoBorder)
                                ) {
                                    Text(
                                        text = "Straight-line distance (approximate route)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BentoTextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // LEVEL 4: ACTIONS (Primary Directions CTA + Secondary Actions Row)
                            CurbPrimaryButton(
                                text = "GET DIRECTIONS",
                                onClick = {
                                    try {
                                        val geoUri = Uri.parse("geo:${savedParkingSpot.latitude},${savedParkingSpot.longitude}?q=${savedParkingSpot.latitude},${savedParkingSpot.longitude}(Car+Location)")
                                        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${savedParkingSpot.latitude},${savedParkingSpot.longitude}"))
                                            context.startActivity(fallbackIntent)
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening directions...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = Icons.Default.Navigation,
                                backgroundColor = BentoPrimaryDark,
                                testTag = "find_my_car_directions_button"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val locationName = if (savedParkingSpot.locationName.isNotBlank()) savedParkingSpot.locationName else "Saved Spot"
                                            val shareText = "Here is my parked car location ($locationName): https://maps.google.com/?q=${savedParkingSpot.latitude},${savedParkingSpot.longitude}"
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Car Location"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Unable to share location", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("find_my_car_share_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BentoBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = BentoTextPrimary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = BentoTextPrimary
                                        )
                                        Text(
                                            text = "SHARE",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onNavigateToParkingTimer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("find_my_car_timer_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BentoBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = BentoTextPrimary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = BentoTextPrimary
                                        )
                                        Text(
                                            text = "TIMER",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // DISPOSE OF MAPVIEW ON SCREEN EXIT
                DisposableEffect(Unit) {
                    onDispose {
                        mapViewRef?.onDetach()
                    }
                }
            }
        }
    }
}
