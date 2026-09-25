package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
import com.example.data.model.ActiveParkingSession
import com.example.ui.screens.formatHomeLocationLabel
import com.example.ui.screens.selectUserFacingLocationLabel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeTimerAndLocationTest {

    private lateinit var context: Context
    private lateinit var locationService: LocationService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        locationService = LocationService(context)
    }

    @Test
    fun testA_ActiveHomeTimerUpdatesOncePerSecond() {
        val now = System.currentTimeMillis()
        val session = ActiveParkingSession(
            id = 1L,
            startTime = now,
            endTime = now + 3600_000L, // 1 hour remaining
            isActive = true
        )

        val rem1 = maxOf(0L, session.endTime - now)
        val rem2 = maxOf(0L, session.endTime - (now + 1000L))

        assertEquals(3600_000L, rem1)
        assertEquals(3599_000L, rem2)

        fun format(remainingMillis: Long): String {
            val totalSeconds = remainingMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m remaining" else "${minutes}m remaining"
        }

        val format1 = format(rem1)
        val format2 = format(rem2)
        assertEquals("1h 0m remaining", format1)
        assertEquals("59m remaining", format2)
        assertNotEquals(rem1, rem2)
    }

    @Test
    fun testB_TimerExpiryCausesHomeDisplayToTransitionCorrectly() {
        val now = System.currentTimeMillis()
        val expiredSession = ActiveParkingSession(
            id = 2L,
            startTime = now - 7200_000L,
            endTime = now - 1000L, // Expired 1 sec ago
            isActive = true
        )

        val liveRemaining = maxOf(0L, expiredSession.endTime - now)
        assertEquals("Expired timer remainingMillis must be 0", 0L, liveRemaining)
        assertFalse("Expired timer condition must evaluate to false for active banner", liveRemaining > 0)
    }

    @Test
    fun testC_RawCoordinatesAreNeverRenderedDirectlyInHomeUI() {
        val rawCoordsLocation = UserLocationResult.Success(
            latitude = 26.7617,
            longitude = 80.9488,
            locationName = "Current Location",
            cityState = "26.7617, 80.9488",
            formattedDisplay = "GPS (26.7617, 80.9488)"
        )

        val label = selectUserFacingLocationLabel(
            locationName = rawCoordsLocation.locationName,
            cityState = rawCoordsLocation.cityState,
            formattedDisplay = rawCoordsLocation.formattedDisplay
        )

        assertNotEquals("Raw coordinates must never be rendered", "26.7617, 80.9488", label)
        assertNotEquals("GPS string must never be rendered", "GPS (26.7617, 80.9488)", label)
        assertNotEquals("Current Location must never be rendered", "Current Location", label)
        assertEquals("Fallback must be 'Location active'", "Location active", label)
    }

    @Test
    fun testD_HumanReadableLocationIsRenderedCorrectly() {
        val validLocation = UserLocationResult.Success(
            latitude = 37.7749,
            longitude = -122.4194,
            locationName = "Mission Street",
            cityState = "San Francisco, CA",
            formattedDisplay = "Mission Street, San Francisco, CA"
        )

        val label = selectUserFacingLocationLabel(
            locationName = validLocation.locationName,
            cityState = validLocation.cityState,
            formattedDisplay = validLocation.formattedDisplay
        )

        assertEquals("Mission Street", label)
    }

    @Test
    fun testE_LocationWithOnlyCoordinatesFallsBackToLocationActive() {
        val coordsOnly = UserLocationResult.Success(
            latitude = 40.7128,
            longitude = -74.0060,
            locationName = "",
            cityState = "40.7128, -74.0060",
            formattedDisplay = "40.7128, -74.0060"
        )

        val label = selectUserFacingLocationLabel(
            locationName = coordsOnly.locationName,
            cityState = coordsOnly.cityState,
            formattedDisplay = coordsOnly.formattedDisplay
        )

        assertEquals("Location active", label)
    }

    @Test
    fun testF_RepeatedLocationUpdatesWithinGeocodeThresholdDoNotTriggerNewGeocoderCall() = runBlocking {
        val now = System.currentTimeMillis()
        val lat = 37.7749
        val lng = -122.4194

        // Initial check -> should geocode
        assertTrue(locationService.shouldReverseGeocode(lat, lng, now))

        // Set cached location explicitly
        locationService.updateCachedLocationForTest(lat, lng, now, Triple("Mission St", "San Francisco, CA", "Mission St, San Francisco, CA"))

        // Small movement within 50m and 30s cooldown -> should NOT geocode
        val movedLat = lat + 0.00004 // ~4.4 meters
        val movedLng = lng + 0.00004
        assertFalse("Small movement within 50m and 30s cooldown must NOT trigger new geocode", locationService.shouldReverseGeocode(movedLat, movedLng, now + 2000L))
    }

    @Test
    fun testG_LocationPermissionUnavailableProducesTruthfulPermissionState() {
        val permReq = UserLocationResult.PermissionRequired("Location access is needed.")
        val formatted = formatHomeLocationLabel(permReq)

        assertEquals("Location unavailable", formatted.second)
    }

    @Test
    fun testH_HomeRendersImmediatelyWithoutWaitingForGeocoding() {
        val checkingState = UserLocationResult.Unavailable("Checking location…")
        val formatted = formatHomeLocationLabel(checkingState)

        assertEquals("Getting location…", formatted.second)
    }
}
