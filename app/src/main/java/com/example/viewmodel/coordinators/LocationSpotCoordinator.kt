package com.example.viewmodel.coordinators

import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
import com.example.data.repository.CurbRepository
import kotlinx.coroutines.CoroutineScope
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

    init {
        if (locationService.hasLocationPermission()) {
            coroutineScope.launch {
                try {
                    refreshLocation()
                } catch (_: Throwable) {
                    // Ignore startup location refresh errors
                }
            }
        }
    }

    fun refreshLocation() {
        coroutineScope.launch {
            try {
                if (!locationService.hasLocationPermission()) {
                    _userLocationState.value = UserLocationResult.PermissionRequired()
                    return@launch
                }
                val result = locationService.fetchCurrentLocation()
                if (result is UserLocationResult.Success) {
                    _userLocationState.value = result
                } else {
                    if (_userLocationState.value !is UserLocationResult.Success) {
                        _userLocationState.value = result
                    }
                }
            } catch (e: Throwable) {
                if (_userLocationState.value !is UserLocationResult.Success) {
                    _userLocationState.value = UserLocationResult.Unavailable("Location unavailable")
                }
            }
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
}
