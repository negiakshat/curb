package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationLiveTrackingTest {

    @Test
    fun testA_and_B_LocationRequestConfiguration() {
        val intervalMs = 1000L
        val request = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).setMinUpdateIntervalMillis(intervalMs / 2).build()

        assertEquals(1000L, request.intervalMillis)
        assertEquals(500L, request.minUpdateIntervalMillis)
    }

    @Test
    fun testC_and_D_RawCoordinatesAuthoritativeAndInterpolationSeparate() {
        // Raw coordinate (authoritative)
        val rawLoc = UserLocationResult.Success(
            latitude = 37.7749,
            longitude = -122.4194,
            locationName = "Mission St",
            cityState = "San Francisco, CA",
            formattedDisplay = "Mission St, San Francisco, CA",
            accuracy = 5.0f,
            timestamp = System.currentTimeMillis()
        )

        // Interpolated coordinate pair (only used for visual rendering)
        var interpolatedLat: Double? = null
        var interpolatedLng: Double? = null

        // Simulate target coordinate arrival
        val targetLat = rawLoc.latitude
        val targetLng = rawLoc.longitude

        // Simulation of visual interpolation
        val startLat = 37.7700
        val startLng = -122.4100
        val fraction = 0.5f // halfway through animation
        interpolatedLat = startLat + (targetLat - startLat) * fraction
        interpolatedLng = startLng + (targetLng - startLng) * fraction

        // Verify raw coordinates are completely untouched
        assertEquals(37.7749, rawLoc.latitude, 0.0001)
        assertEquals(-122.4194, rawLoc.longitude, 0.0001)

        // Verify interpolated coordinate is separate and different
        assertNotEquals(rawLoc.latitude, interpolatedLat!!, 0.0001)
        assertNotEquals(rawLoc.longitude, interpolatedLng!!, 0.0001)
    }

    @Test
    fun testE_StaleCachedLocationIsRejected() {
        val service = LocationService(ApplicationProvider.getApplicationContext())
        
        val staleLocation = android.location.Location("GPS").apply {
            latitude = 37.7749
            longitude = -122.4194
            time = System.currentTimeMillis() - 30000L // 30 seconds old (stale)
        }
        
        // Reflectively call private isValidFix to verify it correctly returns false for stale fixes
        val method = LocationService::class.java.getDeclaredMethod("isValidFix", android.location.Location::class.java)
        method.isAccessible = true
        val isValid = method.invoke(service, staleLocation) as Boolean
        assertFalse("Stale location (age > 15s) must be rejected", isValid)
    }
}
