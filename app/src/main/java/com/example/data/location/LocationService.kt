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
import android.util.Log
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
import kotlinx.coroutines.launch
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

    @Volatile private var cachedGeocodedLat: Double? = null
    @Volatile private var cachedGeocodedLng: Double? = null
    @Volatile private var cachedGeocodeTime: Long = 0L
    @Volatile private var cachedGeocodedTriple: Triple<String, String, String>? = null

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

    fun shouldReverseGeocode(lat: Double, lng: Double, now: Long): Boolean {
        val lastLat = cachedGeocodedLat
        val lastLng = cachedGeocodedLng
        val lastTriple = cachedGeocodedTriple
        if (lastLat == null || lastLng == null || lastTriple == null) return true

        val dist = FloatArray(1)
        Location.distanceBetween(lat, lng, lastLat, lastLng, dist)
        val distanceMeters = dist[0]
        val elapsedMs = now - cachedGeocodeTime

        val needsGeocode = distanceMeters >= 50f || elapsedMs >= 30000L
        if (!needsGeocode) {
            Log.d("LocationService", "Reverse-geocode skipped due to distance/cooldown (dist=${distanceMeters}m, elapsed=${elapsedMs}ms)")
        }
        return needsGeocode
    }

    fun updateCachedLocationForTest(lat: Double, lng: Double, time: Long, triple: Triple<String, String, String>) {
        cachedGeocodedLat = lat
        cachedGeocodedLng = lng
        cachedGeocodeTime = time
        cachedGeocodedTriple = triple
    }

    fun getLocationUpdates(intervalMs: Long = 5000L): Flow<UserLocationResult> = callbackFlow {
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
                launch {
                    processAndEmitLocation(loc, "FUSED")
                }
            }

            private suspend fun processAndEmitLocation(loc: Location, source: String) {
                val lat = loc.latitude
                val lng = loc.longitude
                val accuracy = if (loc.hasAccuracy()) loc.accuracy else null
                val time = if (loc.time > 0) loc.time else System.currentTimeMillis()

                Log.d("LocationService", "Location update received from $source: raw lat=$lat, lng=$lng, acc=$accuracy")

                val needsGeocode = shouldReverseGeocode(lat, lng, time)
                val labelTriple = if (needsGeocode) {
                    reverseGeocode(lat, lng)
                } else {
                    cachedGeocodedTriple ?: Triple("Location active", "", "Location active")
                }

                val userRes = UserLocationResult.Success(
                    latitude = lat,
                    longitude = lng,
                    locationName = labelTriple.first,
                    cityState = labelTriple.second,
                    formattedDisplay = labelTriple.third,
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
            Log.d("LocationService", "Fused location updates request failed: ${e.message}")
        }

        var locationManagerListener: LocationListener? = null
        if (!isFusedActive) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val listener = LocationListener { loc ->
                        launch {
                            val lat = loc.latitude
                            val lng = loc.longitude
                            val accuracy = if (loc.hasAccuracy()) loc.accuracy else null
                            val time = if (loc.time > 0) loc.time else System.currentTimeMillis()

                            Log.d("LocationService", "Location update received from GPS_PROVIDER: raw lat=$lat, lng=$lng, acc=$accuracy")

                            val needsGeocode = shouldReverseGeocode(lat, lng, time)
                            val labelTriple = if (needsGeocode) {
                                reverseGeocode(lat, lng)
                            } else {
                                cachedGeocodedTriple ?: Triple("Location active", "", "Location active")
                            }

                            trySend(
                                UserLocationResult.Success(
                                    latitude = lat,
                                    longitude = lng,
                                    locationName = labelTriple.first,
                                    cityState = labelTriple.second,
                                    formattedDisplay = labelTriple.third,
                                    timestamp = time,
                                    accuracy = accuracy
                                )
                            )
                        }
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
                Log.d("LocationService", "LocationManager updates request failed: ${e.message}")
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
            var source = "FUSED (getCurrentLocation)"

            // 1. Try Google Play Services FusedLocationProviderClient getCurrentLocation (Fresh single-shot fix with HIGH_ACCURACY)
            try {
                val cts = CancellationTokenSource()
                val task = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                )
                rawLocation = task.awaitTask()
            } catch (e: Exception) {
                Log.d("LocationService", "getCurrentLocation high accuracy failed: ${e.message}")
            }

            // 2. Fallback to fusedLocationClient.lastLocation
            if (rawLocation == null) {
                try {
                    source = "FUSED (lastLocation)"
                    rawLocation = fusedLocationClient.lastLocation.awaitTask()
                } catch (e: Exception) {
                    Log.d("LocationService", "lastLocation failed: ${e.message}")
                }
            }

            // 3. Fallback to standard Android LocationManager
            if (rawLocation == null) {
                try {
                    source = "LocationManager fallback"
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
                    Log.d("LocationService", "LocationManager fallback failed: ${e.message}")
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

            Log.d("LocationService", "Location source: $source, raw lat=$lat, lng=$lng, acc=$accuracy")

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
                    Log.d("LocationService", "Geocoder not present on device")
                    val fallback = cachedGeocodedTriple ?: Triple("Location active", "", "Location active")
                    return@withContext fallback
                }

                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        try {
                            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(results: MutableList<Address>) {
                                    if (continuation.isActive) continuation.resume(results)
                                }
                                override fun onError(errorMessage: String?) {
                                    if (continuation.isActive) continuation.resume(emptyList())
                                }
                            })
                        } catch (e: Throwable) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    }
                } else {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1)
                    } catch (e: Throwable) {
                        emptyList()
                    }
                }

                val address = addresses?.firstOrNull()
                if (address != null) {
                    val thoroughfare = address.thoroughfare
                    val subThoroughfare = address.subThoroughfare
                    val featureName = address.featureName
                    val subLocality = address.subLocality

                    val streetAddress = when {
                        !subThoroughfare.isNullOrBlank() && !thoroughfare.isNullOrBlank() -> "$subThoroughfare $thoroughfare"
                        !thoroughfare.isNullOrBlank() -> thoroughfare
                        !featureName.isNullOrBlank() && featureName != thoroughfare && !featureName.matches(Regex("""^\d+$""")) -> featureName
                        !subLocality.isNullOrBlank() -> subLocality
                        else -> null
                    }

                    val locality = address.locality ?: address.subAdminArea ?: address.subLocality
                    val adminArea = address.adminArea
                    val country = address.countryName

                    val locationName = streetAddress ?: locality ?: "Location active"

                    val cityState = when {
                        !locality.isNullOrBlank() && !adminArea.isNullOrBlank() -> "$locality, $adminArea"
                        !locality.isNullOrBlank() && !country.isNullOrBlank() -> "$locality, $country"
                        !adminArea.isNullOrBlank() -> adminArea
                        !country.isNullOrBlank() -> country
                        else -> ""
                    }

                    val formattedDisplay = if (!streetAddress.isNullOrBlank() && !cityState.isNullOrBlank()) {
                        "$streetAddress, $cityState"
                    } else if (!cityState.isNullOrBlank()) {
                        cityState
                    } else {
                        locationName
                    }

                    cachedGeocodedLat = latitude
                    cachedGeocodedLng = longitude
                    cachedGeocodeTime = System.currentTimeMillis()
                    val resultTriple = Triple(locationName, cityState, formattedDisplay)
                    cachedGeocodedTriple = resultTriple

                    Log.d("LocationService", "Reverse geocode performed: lat=$latitude, lng=$longitude -> resolved: street/locality=$locationName, cityState=$cityState")
                    return@withContext resultTriple
                }
            } catch (e: Exception) {
                Log.d("LocationService", "Reverse geocode failed for lat=$latitude, lng=$longitude: ${e.message}")
            }

            val fallback = cachedGeocodedTriple ?: Triple("Location active", "", "Location active")
            return@withContext fallback
        }

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
        try {
            addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }
            addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
            addOnCanceledListener {
                if (cont.isActive) cont.resume(null)
            }
        } catch (_: Throwable) {
            if (cont.isActive) cont.resume(null)
        }
    }
}
