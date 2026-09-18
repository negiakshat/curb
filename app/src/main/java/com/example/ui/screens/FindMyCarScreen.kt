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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MapCardBg = Color(0xFFFFFFFF)
private val MapTextDark = Color(0xFF0F172A)
private val MapTextMuted = Color(0xFF64748B)
private val MapAccentGreen = Color(0xFF16A34A)

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Find My Car",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MapTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("find_my_car_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MapTextDark
                        )
                    }
                },
                actions = {
                    if (savedParkingSpot != null) {
                        IconButton(
                            onClick = {
                                isRefreshingLoc = true
                                onRefreshLocation()
                                isRefreshingLoc = false
                            },
                            modifier = Modifier.testTag("refresh_location_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Location",
                                tint = BentoPrimaryDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoSand
                )
            )
        },
        containerColor = BentoSand,
        modifier = Modifier.testTag("find_my_car_screen")
    ) { innerPadding ->
        if (savedParkingSpot == null) {
            // EMPTY STATE when no parking spot saved
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MapCardBg),
                    border = BorderStroke(1.dp, BentoBorder),
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
                                tint = MapTextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "No Parking Spot Saved",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MapTextDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Save your parking spot on the Parking Timer screen after parking, then come back here to locate your car.",
                            fontSize = 14.sp,
                            color = MapTextMuted,
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
                            "$feet ft away (${meters.toInt()} m)"
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
                    .fillMaxSize()
                    .padding(innerPadding)
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
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, BentoBorder),
                    shadowElevation = 4.dp,
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
                                .background(MapAccentGreen)
                        )
                        Text(
                            text = if (successUserLoc != null) "LIVE TRACKING ACTIVE" else "CONNECTING GPS...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MapTextDark,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // BOTTOM CONTAINER: FLOATING CONTROLS + BOTTOM INFORMATION CARD
                // Stacked together to guarantee floating controls sit directly above the card with zero overlap on any screen!
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
                                color = MapCardBg,
                                border = BorderStroke(1.dp, BentoBorder),
                                shadowElevation = 6.dp,
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
                                color = MapCardBg,
                                border = BorderStroke(1.dp, BentoBorder),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("floating_center_me_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Center on Me",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // BOTTOM INFO OVERLAY PANEL
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("find_my_car_info_card"),
                        shape = RoundedCornerShape(28.dp),
                        color = MapCardBg,
                        border = BorderStroke(1.dp, BentoBorder),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Header: Prominent ETA / Status title
                            val headerTitle = remember(distanceAndWalk) {
                                if (distanceAndWalk.second != null) {
                                    "YOUR CAR IS ${distanceAndWalk.second?.uppercase()?.replace(" WALK", " AWAY")}"
                                } else if (distanceAndWalk.first == "You're at your car") {
                                    "YOU ARE AT YOUR CAR"
                                } else {
                                    "YOUR PARKED CAR"
                                }
                            }

                            Text(
                                text = headerTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoPrimaryDark,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Location Name & Saved Time
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
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Car",
                                        tint = BentoPrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (savedParkingSpot.locationName.isNotBlank()) savedParkingSpot.locationName else "Saved Parking Spot",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MapTextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val savedTimeStr = remember(savedParkingSpot.timestamp) {
                                        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                                        "Saved at ${sdf.format(Date(savedParkingSpot.timestamp))}"
                                    }
                                    Text(
                                        text = savedTimeStr,
                                        fontSize = 12.sp,
                                        color = MapTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Distance Badge & Walking status
                            if (distanceAndWalk.first != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (distanceAndWalk.second == null) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, if (distanceAndWalk.second == null) Color(0xFFBBF7D0) else Color(0xFFDBEAFE))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (distanceAndWalk.second == null) Icons.Default.CheckCircle else Icons.Default.CompassCalibration,
                                                    contentDescription = null,
                                                    tint = if (distanceAndWalk.second == null) Color(0xFF166534) else Color(0xFF2563EB),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = distanceAndWalk.first ?: "",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (distanceAndWalk.second == null) Color(0xFF166534) else Color(0xFF1E40AF)
                                                )
                                            }

                                            distanceAndWalk.second?.let { walkStr ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                                        contentDescription = "Walking directions",
                                                        tint = Color(0xFF2563EB),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = walkStr,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF1E40AF)
                                                    )
                                                }
                                            }
                                        }

                                        if (distanceAndWalk.third) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Straight-line distance (approximate route)",
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E40AF).copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            } else if (savedParkingSpot != null && userLocationState == null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = BentoPrimaryDark
                                    )
                                    Text(
                                        text = "Locating your position...",
                                        fontSize = 12.sp,
                                        color = MapTextMuted
                                    )
                                }
                            } else if (userLocationState is UserLocationResult.PermissionRequired) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Location permission is needed to calculate distance to your car.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            } else if (userLocationState is UserLocationResult.Unavailable) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = userLocationState.message,
                                    fontSize = 12.sp,
                                    color = MapTextMuted,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // PRIMARY CTA: GET DIRECTIONS
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // SECONDARY ACTIONS ROW: SHARE LOCATION & PARKING TIMER
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
                                        contentColor = MapTextDark
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
                                            tint = MapTextDark
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
                                        contentColor = MapTextDark
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
                                            tint = MapTextDark
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
