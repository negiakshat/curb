package com.example.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

sealed class UserLocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val locationName: String,
        val cityState: String,
        val formattedDisplay: String,
        val timestamp: Long = System.currentTimeMillis(),
        val accuracy: Float? = null
    ) : UserLocationResult()

    data class PermissionRequired(
        val message: String = "Location access is needed for location-based parking information."
    ) : UserLocationResult()

    data class Unavailable(
        val message: String = "Unable to determine your current location. Please verify location is enabled on your device."
    ) : UserLocationResult()
}

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    fun getLocationUpdates(intervalMs: Long = 3000L): Flow<UserLocationResult> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(UserLocationResult.PermissionRequired())
            close()
            return@callbackFlow
        }

        // Emit initial location fix immediately if available
        try {
            val initialRes = fetchCurrentLocation()
            trySend(initialRes)
        } catch (e: Exception) {
            // Ignore failure on initial fetch
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val lat = loc.latitude
                val lng = loc.longitude
                val accuracy = if (loc.hasAccuracy()) loc.accuracy else null
                val time = if (loc.time > 0) loc.time else System.currentTimeMillis()

                val coordsStr = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                val userRes = UserLocationResult.Success(
                    latitude = lat,
                    longitude = lng,
                    locationName = "Current Location",
                    cityState = coordsStr,
                    formattedDisplay = "GPS ($coordsStr)",
                    timestamp = time,
                    accuracy = accuracy
                )
                trySend(userRes)
            }
        }

        var isFusedActive = false
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isFusedActive = true
        } catch (e: Exception) {
            // Fused client update request failed
        }

        var locationManagerListener: LocationListener? = null
        if (!isFusedActive) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val listener = LocationListener { loc ->
                        val lat = loc.latitude
                        val lng = loc.longitude
                        val accuracy = if (loc.hasAccuracy()) loc.accuracy else null
                        val time = if (loc.time > 0) loc.time else System.currentTimeMillis()
                        val coordsStr = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                        trySend(
                            UserLocationResult.Success(
                                latitude = lat,
                                longitude = lng,
                                locationName = "Current Location",
                                cityState = coordsStr,
                                formattedDisplay = "GPS ($coordsStr)",
                                timestamp = time,
                                accuracy = accuracy
                            )
                        )
                    }
                    locationManagerListener = listener
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        intervalMs,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        awaitClose {
            if (isFusedActive) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
            if (locationManagerListener != null) {
                try {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    locationManager?.removeUpdates(locationManagerListener)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    suspend fun fetchCurrentLocation(): UserLocationResult = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext UserLocationResult.PermissionRequired()
        }

        try {
            var rawLocation: Location? = null

            // 1. Try Google Play Services FusedLocationProviderClient getCurrentLocation (Fresh single-shot fix)
            try {
                val cts = CancellationTokenSource()
                val task = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                )
                rawLocation = task.awaitTask()
            } catch (e: Exception) {
                // Ignore and try fallback
            }

            // 2. Fallback to fusedLocationClient.lastLocation
            if (rawLocation == null) {
                try {
                    rawLocation = fusedLocationClient.lastLocation.awaitTask()
                } catch (e: Exception) {
                    // Ignore and try fallback
                }
            }

            // 3. Fallback to standard Android LocationManager
            if (rawLocation == null) {
                try {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    if (locationManager != null) {
                        val gpsLoc = try {
                            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            } else null
                        } catch (e: SecurityException) { null }

                        val networkLoc = try {
                            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            } else null
                        } catch (e: SecurityException) { null }

                        rawLocation = when {
                            gpsLoc != null && networkLoc != null -> {
                                if (gpsLoc.time >= networkLoc.time) gpsLoc else networkLoc
                            }
                            gpsLoc != null -> gpsLoc
                            else -> networkLoc
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            if (rawLocation == null) {
                return@withContext UserLocationResult.Unavailable(
                    "Unable to determine your current location. Please verify location is enabled."
                )
            }

            val lat = rawLocation.latitude
            val lng = rawLocation.longitude
            val accuracy = if (rawLocation.hasAccuracy()) rawLocation.accuracy else null
            val time = if (rawLocation.time > 0) rawLocation.time else System.currentTimeMillis()

            // 4. Reverse-geocode to human-readable address
            val geocoded = reverseGeocode(lat, lng)
            return@withContext UserLocationResult.Success(
                latitude = lat,
                longitude = lng,
                locationName = geocoded.first,
                cityState = geocoded.second,
                formattedDisplay = geocoded.third,
                timestamp = time,
                accuracy = accuracy
            )
        } catch (e: Exception) {
            return@withContext UserLocationResult.Unavailable(
                "Unable to determine your current location: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Triple<String, String, String> =
        withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    val coordsStr = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
                    return@withContext Triple("Current Location", coordsStr, "Near $coordsStr")
                }

                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        try {
                            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(results: MutableList<Address>) {
                                    continuation.resume(results)
                                }
                                override fun onError(errorMessage: String?) {
                                    continuation.resume(emptyList())
                                }
                            })
                        } catch (e: Exception) {
                            continuation.resume(emptyList())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                val address = addresses?.firstOrNull()
                if (address != null) {
                    // Thoroughfare (street name e.g. "Mission St", "Oak Ave", "Hazratganj")
                    val thoroughfare = address.thoroughfare
                    val subThoroughfare = address.subThoroughfare
                    val streetAddress = when {
                        !subThoroughfare.isNullOrBlank() && !thoroughfare.isNullOrBlank() -> "$subThoroughfare $thoroughfare"
                        !thoroughfare.isNullOrBlank() -> thoroughfare
                        !address.featureName.isNullOrBlank() && address.featureName != thoroughfare -> address.featureName
                        !address.subLocality.isNullOrBlank() -> address.subLocality
                        else -> null
                    }

                    val locality = address.locality ?: address.subAdminArea ?: address.subLocality
                    val adminArea = address.adminArea
                    val country = address.countryName

                    val locationName = streetAddress ?: locality ?: "Current Spot"

                    val cityState = when {
                        !locality.isNullOrBlank() && !adminArea.isNullOrBlank() -> "$locality, $adminArea"
                        !locality.isNullOrBlank() && !country.isNullOrBlank() -> "$locality, $country"
                        !adminArea.isNullOrBlank() -> adminArea
                        !country.isNullOrBlank() -> country
                        else -> "Local Zone"
                    }

                    val formattedDisplay = if (!streetAddress.isNullOrBlank() && !cityState.isNullOrBlank()) {
                        "$streetAddress, $cityState"
                    } else if (!cityState.isNullOrBlank()) {
                        cityState
                    } else {
                        locationName
                    }

                    return@withContext Triple(locationName, cityState, formattedDisplay)
                }
            } catch (e: Exception) {
                // Geocoder network or service failure
            }

            val fallbackDisplay = String.format(Locale.US, "GPS (%.3f, %.3f)", latitude, longitude)
            return@withContext Triple("Current Location", fallbackDisplay, fallbackDisplay)
        }

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }
}
