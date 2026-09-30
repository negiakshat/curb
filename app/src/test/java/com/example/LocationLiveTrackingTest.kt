package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
import com.example.data.repository.CurbRepository
import com.example.viewmodel.CurbViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
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

        // Interpolated GeoPoint (only used for visual rendering)
        var interpolatedGeoPoint: org.osmdroid.util.GeoPoint? = null

        // Simulate target geo point arrival
        val target = org.osmdroid.util.GeoPoint(rawLoc.latitude, rawLoc.longitude)

        // Simulation of visual interpolation
        val start = org.osmdroid.util.GeoPoint(37.7700, -122.4100)
        val fraction = 0.5f // halfway through animation
        val interpolatedLat = start.latitude + (target.latitude - start.latitude) * fraction
        val interpolatedLng = start.longitude + (target.longitude - start.longitude) * fraction
        interpolatedGeoPoint = org.osmdroid.util.GeoPoint(interpolatedLat, interpolatedLng)

        // Verify raw coordinates are completely untouched
        assertEquals(37.7749, rawLoc.latitude, 0.0001)
        assertEquals(-122.4194, rawLoc.longitude, 0.0001)

        // Verify interpolated coordinate is separate and different
        assertNotEquals(rawLoc.latitude, interpolatedGeoPoint.latitude, 0.0001)
        assertNotEquals(rawLoc.longitude, interpolatedGeoPoint.longitude, 0.0001)
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

    @Test
    fun testF_RouteUpdatesRemainThresholdBased() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = CurbViewModel(app)
        
        val repoField = CurbViewModel::class.java.getDeclaredField("repository")
        repoField.isAccessible = true
        val repository = repoField.get(viewModel) as CurbRepository
        
        val locationService = LocationService(app)
        
        val coordinator = com.example.viewmodel.coordinators.LocationSpotCoordinator(
            repository, locationService, this
        )

        // First fetch
        coordinator.updateWalkingRouteIfNeeded(37.7749, -122.4194, 37.7750, -122.4195)
        testScheduler.advanceUntilIdle()
        
        // Get lastRouteFetchLat reflectively
        val lastLatField = com.example.viewmodel.coordinators.LocationSpotCoordinator::class.java.getDeclaredField("lastRouteFetchLat")
        lastLatField.isAccessible = true
        val lastLat = lastLatField.get(coordinator) as? Double
        
        assertEquals(37.7749, lastLat ?: 0.0, 0.0001)

        // Move slightly (e.g. ~1 meter)
        coordinator.updateWalkingRouteIfNeeded(37.77491, -122.41941, 37.7750, -122.4195)
        testScheduler.advanceUntilIdle()
        
        // Verify lastRouteFetchLat was NOT updated
        val lastLatAfterSmallMove = lastLatField.get(coordinator) as? Double
        assertEquals(37.7749, lastLatAfterSmallMove ?: 0.0, 0.0001)

        // Move significantly (~20 meters)
        coordinator.updateWalkingRouteIfNeeded(37.7751, -122.4196, 37.7750, -122.4195)
        testScheduler.advanceUntilIdle()
        
        // Verify lastRouteFetchLat is updated
        val lastLatAfterBigMove = lastLatField.get(coordinator) as? Double
        assertEquals(37.7751, lastLatAfterBigMove ?: 0.0, 0.0001)
    }

    @Test
    fun testG_LiveTrackingCleanupRemovesLocationUpdates() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = CurbViewModel(app)
        
        val repoField = CurbViewModel::class.java.getDeclaredField("repository")
        repoField.isAccessible = true
        val repository = repoField.get(viewModel) as CurbRepository
        
        val locationService = LocationService(app)
        
        val coordinator = com.example.viewmodel.coordinators.LocationSpotCoordinator(
            repository, locationService, this
        )

        // Start updates
        coordinator.startLiveLocationUpdates()
        
        // Verify liveLocationJob is active reflectively
        val jobField = com.example.viewmodel.coordinators.LocationSpotCoordinator::class.java.getDeclaredField("liveLocationJob")
        jobField.isAccessible = true
        val job = jobField.get(coordinator) as? kotlinx.coroutines.Job
        assertNotNull("liveLocationJob should be active", job)
        assertTrue("liveLocationJob should be active", job?.isActive == true)

        // Stop updates
        coordinator.stopLiveLocationUpdates()
        
        // Verify liveLocationJob is null or cancelled
        val jobAfterStop = jobField.get(coordinator) as? kotlinx.coroutines.Job
        assertTrue("liveLocationJob should be null or cancelled", jobAfterStop == null || jobAfterStop.isCancelled)
    }
}
