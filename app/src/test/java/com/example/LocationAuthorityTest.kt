package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.detection.LocalSignCrop
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.data.remote.GeminiService
import com.example.util.ParkingAuthority
import com.example.util.ParkingTimerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationAuthorityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testA_CoordinatesOnlyWithNoSignEvidence_ReturnsAmbiguous() {
        val scan = ScanResult(
            locationName = "Current Location",
            cityState = "37.7749, -122.4194",
            verdict = ScanVerdict.ALLOWED, // Attempting unverified ALLOWED
            parkingRules = listOf("Assumed 2 hour parking")
        )

        val sanitized = ParkingAuthority.sanitizeAndEnforceAuthority(scan, emptyList())
        assertEquals("Coordinates with no sign evidence must be AMBIGUOUS", ScanVerdict.AMBIGUOUS, sanitized.verdict)
        assertEquals("Allowed until time must be reset to 'Verify physical signage'", "Verify physical signage", sanitized.allowedUntilTime)
        assertEquals("Time remaining must be '--'", "--", sanitized.timeRemaining)
        assertTrue("Parking rules must indicate no verified rule established", sanitized.parkingRules.first().contains("No verified parking rule"))
    }

    @Test
    fun testB_CityStreetNameWithNoSignEvidence_ReturnsAmbiguous() {
        val scan = ScanResult(
            locationName = "Market St & 4th St",
            cityState = "San Francisco, CA",
            verdict = ScanVerdict.RESTRICTED, // Attempting unverified RESTRICTED
            parkingRules = listOf("San Francisco city center rule")
        )

        val sanitized = ParkingAuthority.sanitizeAndEnforceAuthority(scan, emptyList())
        assertEquals("Street/City with no sign evidence must be AMBIGUOUS", ScanVerdict.AMBIGUOUS, sanitized.verdict)
        assertEquals("Signage unclear", sanitized.statusChipText)
        assertTrue("Explanation must mention missing sign evidence", sanitized.explanation.contains("No distinct parking signs were resolved"))
    }

    @Test
    fun testC_GeocodedLocationCannotCreateAllowedResult() {
        val rawScan = GeminiService.generateIntelligentScanResult(
            locationName = "123 Mission Street",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = emptyList() // No sign crops
        )

        assertEquals("Geocoded location without sign crops cannot produce ALLOWED", ScanVerdict.AMBIGUOUS, rawScan.verdict)
        assertFalse("Allowed until time cannot be confident", rawScan.allowedUntilTime == "2:00 PM")
        assertEquals("Verify physical signage", rawScan.allowedUntilTime)
    }

    @Test
    fun testD_GeocodedLocationCannotCreateRestrictedResult() {
        val rawScan = ScanResult(
            locationName = "Financial District",
            cityState = "San Francisco, CA",
            verdict = ScanVerdict.RESTRICTED,
            parkingRules = listOf("Commercial Zone")
        )

        val sanitized = ParkingAuthority.sanitizeAndEnforceAuthority(rawScan, emptyList())
        assertEquals("Geocoded location without sign crops cannot produce RESTRICTED", ScanVerdict.AMBIGUOUS, sanitized.verdict)
    }

    @Test
    fun testE_LocationOnlyInputCannotAuthorizeTimer() {
        val locationOnlyScan = ScanResult(
            locationName = "Downtown Parking Spot",
            cityState = "San Jose, CA",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "5:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("Derived location rule"),
            detectedSigns = emptyList() // No physical sign evidence
        )

        val canAuthorize = ParkingAuthority.canAuthorizeTimer(locationOnlyScan)
        assertFalse("Location-only scan must not authorize a parking timer", canAuthorize)

        val timerConfig = ParkingTimerCalculator.calculateConfig(
            ParkingAuthority.sanitizeAndEnforceAuthority(locationOnlyScan, emptyList())
        )
        assertFalse("Timer calculator must reject sanitized location-only scan", timerConfig.isValidAllowed)
    }

    @Test
    fun testF_VerifiedPhysicalSignWithLocationContext_SignGovernsVerdict() {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val validCrop = LocalSignCrop(
            id = "sign_1",
            bitmap = dummyBitmap,
            normalizedBox = SignBoundingBox("1", 0.1f, 0.1f, 0.9f, 0.9f, "2 HOUR PARKING", "2 HOUR PARKING 8AM - 6PM", 0.95f),
            ocrText = "2 HOUR PARKING 8AM TO 6PM",
            fileUri = "/cache/real_sign_1.jpg",
            isDemo = false
        )

        val rawScan = GeminiService.generateIntelligentScanResult(
            locationName = "Unknown/Unverified Commercial Spot",
            cityState = "Oakland, CA",
            isLocationKnown = true,
            localDetections = listOf(validCrop)
        )

        assertEquals("Verified sign evidence governs verdict (ALLOWED)", ScanVerdict.ALLOWED, rawScan.verdict)
        assertEquals("Location is preserved as display context", "Unknown/Unverified Commercial Spot", rawScan.locationName)
        assertEquals("Oakland, CA", rawScan.cityState)
        assertTrue("Timer authority is valid when verified sign evidence exists", ParkingAuthority.canAuthorizeTimer(rawScan))
    }

    @Test
    fun testG_ConflictingLocationAssumptionsVsSignEvidence_SignGovernsWithoutOverride() {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val signCrop = LocalSignCrop(
            id = "sign_2",
            bitmap = dummyBitmap,
            normalizedBox = SignBoundingBox("2", 0.1f, 0.1f, 0.9f, 0.9f, "NO PARKING TOW AWAY", "NO PARKING TOW AWAY ZONE", 0.98f),
            ocrText = "NO PARKING TOW AWAY ZONE",
            fileUri = "/cache/real_tow_away.jpg",
            isDemo = false
        )

        // Location context says "Metered Parking Spot" (optimistic display label), but physical sign says TOW AWAY
        val rawScan = GeminiService.generateIntelligentScanResult(
            locationName = "Metered Free Parking Spot",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = listOf(signCrop)
        )

        assertEquals("Sign evidence (TOW AWAY) governs over optimistic location label", ScanVerdict.RESTRICTED, rawScan.verdict)
        assertFalse("Location assumption cannot override TOW AWAY to ALLOWED", rawScan.verdict == ScanVerdict.ALLOWED)
    }

    @Test
    fun testH_ValidSignWithUnavailableLocation_AnalyzesNormally() {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val validCrop = LocalSignCrop(
            id = "sign_3",
            bitmap = dummyBitmap,
            normalizedBox = SignBoundingBox("3", 0.1f, 0.1f, 0.9f, 0.9f, "1 HOUR PARKING", "1 HOUR PARKING MON-FRI", 0.92f),
            ocrText = "1 HOUR PARKING 9AM TO 5PM MON-FRI",
            fileUri = "/cache/real_sign_3.jpg",
            isDemo = false
        )

        val rawScan = GeminiService.generateIntelligentScanResult(
            locationName = "Location unavailable",
            cityState = "",
            isLocationKnown = false,
            localDetections = listOf(validCrop)
        )

        assertEquals("Valid sign analyzes normally even when location is unavailable", ScanVerdict.ALLOWED, rawScan.verdict)
        assertTrue("Timer can be authorized based on sign evidence without location", ParkingAuthority.canAuthorizeTimer(rawScan))
    }

    @Test
    fun testI_MissingScanWithKnownLocation_NeutralAmbiguousState() {
        val missingScan = ScanResult(
            locationName = "789 Broadway Ave",
            cityState = "San Diego, CA",
            verdict = ScanVerdict.AMBIGUOUS,
            statusChipText = "Signage unclear",
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("No verified parking rule has been established."),
            explanation = "No active scan result available. Please capture a parking sign photo.",
            detectedSigns = emptyList()
        )

        assertEquals("Missing scan fallback with known location stays AMBIGUOUS", ScanVerdict.AMBIGUOUS, missingScan.verdict)
        assertFalse("Missing scan cannot authorize a timer", ParkingAuthority.canAuthorizeTimer(missingScan))
    }
}
