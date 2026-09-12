package com.example.viewmodel.coordinators

import android.location.Location
import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
import com.example.data.remote.WalkingRoute
import com.example.data.remote.WalkingRouteService
import com.example.data.repository.CurbRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationSpotCoordinator(
    private val repository: CurbRepository,
    private val locationService: LocationService,
    private val coroutineScope: CoroutineScope
) {
    private val _userLocationState = MutableStateFlow<UserLocationResult>(
        if (locationService.hasLocationPermission()) UserLocationResult.Unavailable("Checking location…")
        else UserLocationResult.PermissionRequired()
    )
    val userLocationState: StateFlow<UserLocationResult> = _userLocationState.asStateFlow()

    private val _currentUserLocation = MutableStateFlow<UserLocationResult?>(null)
    val currentUserLocation: StateFlow<UserLocationResult?> = _currentUserLocation.asStateFlow()

    private val _isSavingParkingSpot = MutableStateFlow(false)
    val isSavingParkingSpot: StateFlow<Boolean> = _isSavingParkingSpot.asStateFlow()

    private val _parkingSpotSaveError = MutableStateFlow<String?>(null)
    val parkingSpotSaveError: StateFlow<String?> = _parkingSpotSaveError.asStateFlow()

    private val walkingRouteService = WalkingRouteService()
    private val _walkingRouteState = MutableStateFlow<WalkingRoute?>(null)
    val walkingRouteState: StateFlow<WalkingRoute?> = _walkingRouteState.asStateFlow()

    private var liveLocationJob: Job? = null
    private var fetchRouteJob: Job? = null
    private var lastRouteFetchLat: Double? = null
    private var lastRouteFetchLng: Double? = null

    init {
        if (locationService.hasLocationPermission()) {
            refreshLocation()
        }
    }

    fun refreshLocation() {
        coroutineScope.launch {
            if (!locationService.hasLocationPermission()) {
                _userLocationState.value = UserLocationResult.PermissionRequired()
                return@launch
            }
            val result = locationService.fetchCurrentLocation()
            _userLocationState.value = result
        }
    }

    fun refreshCurrentLocation(onResult: (UserLocationResult) -> Unit = {}) {
        coroutineScope.launch {
            if (!locationService.hasLocationPermission()) {
                val req = UserLocationResult.PermissionRequired()
                _currentUserLocation.value = req
                onResult(req)
                return@launch
            }
            val loc = locationService.fetchCurrentLocation()
            _currentUserLocation.value = loc
            onResult(loc)
        }
    }

    fun startLiveLocationUpdates() {
        stopLiveLocationUpdates()
        liveLocationJob = coroutineScope.launch {
            if (!locationService.hasLocationPermission()) {
                _userLocationState.value = UserLocationResult.PermissionRequired()
                return@launch
            }
            locationService.getLocationUpdates(intervalMs = 3000L).collect { result ->
                _userLocationState.value = result
            }
        }
    }

    fun stopLiveLocationUpdates() {
        liveLocationJob?.cancel()
        liveLocationJob = null
    }

    fun updateWalkingRouteIfNeeded(userLat: Double, userLng: Double, carLat: Double, carLng: Double) {
        val lastLat = lastRouteFetchLat
        val lastLng = lastRouteFetchLng

        val needsFetch = if (lastLat == null || lastLng == null || _walkingRouteState.value == null) {
            true
        } else {
            val dist = FloatArray(1)
            Location.distanceBetween(userLat, userLng, lastLat, lastLng, dist)
            dist[0] > 8.0
        }

        if (needsFetch) {
            fetchRouteJob?.cancel()
            fetchRouteJob = coroutineScope.launch {
                lastRouteFetchLat = userLat
                lastRouteFetchLng = userLng
                val route = walkingRouteService.getWalkingRoute(
                    startLat = userLat,
                    startLng = userLng,
                    endLat = carLat,
                    endLng = carLng
                )
                _walkingRouteState.value = route
            }
        }
    }

    fun saveCurrentParkingSpot(
        sessionId: Long? = null,
        isDemo: Boolean? = null,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        coroutineScope.launch {
            _isSavingParkingSpot.value = true
            _parkingSpotSaveError.value = null

            val targetSession = if (sessionId != null && sessionId > 0) repository.getSessionById(sessionId) else null
            val effectiveIsDemo = isDemo ?: (targetSession?.isDemo == true)

            if (!locationService.hasLocationPermission()) {
                val errorMsg = "Location permission is required to save your parking spot."
                _isSavingParkingSpot.value = false
                _parkingSpotSaveError.value = errorMsg
                onResult(false, errorMsg)
                return@launch
            }

            val result = locationService.fetchCurrentLocation()
            when (result) {
                is UserLocationResult.Success -> {
                    repository.saveParkingSpot(
                        latitude = result.latitude,
                        longitude = result.longitude,
                        accuracy = result.accuracy,
                        timestamp = result.timestamp,
                        locationName = result.locationName,
                        sessionId = sessionId,
                        isDemo = effectiveIsDemo
                    )
                    _isSavingParkingSpot.value = false
                    _parkingSpotSaveError.value = null
                    onResult(true, null)
                }
                is UserLocationResult.PermissionRequired -> {
                    _isSavingParkingSpot.value = false
                    _parkingSpotSaveError.value = result.message
                    onResult(false, result.message)
                }
                is UserLocationResult.Unavailable -> {
                    _isSavingParkingSpot.value = false
                    _parkingSpotSaveError.value = result.message
                    onResult(false, result.message)
                }
            }
        }
    }

    fun clearParkingSpotSaveError() {
        _parkingSpotSaveError.value = null
    }

    fun clearSavedParkingSpot(isDemo: Boolean = false) {
        coroutineScope.launch {
            repository.clearActiveParkingSpots(isDemo = isDemo)
        }
    }
}
