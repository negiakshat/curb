package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SampleSignPreset
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.repository.CurbRepository
import com.example.util.ParkingTimerCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerAuthorityTest {

    private lateinit var app: Application
    private lateinit var repository: CurbRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        repository = CurbRepository(app)
        runBlocking {
            repository.clearAllData()
        }
    }

    @Test
    fun `test A - AMBIGUOUS scan cannot start timer`() = runBlocking {
        val ambiguousScan = ScanResult(
            verdict = ScanVerdict.AMBIGUOUS,
            statusChipText = "Rule unclear",
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("Sign unreadable"),
            explanation = "Sign text obscured"
        )
        val scanId = repository.saveScan(ambiguousScan)

        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "Ambiguous Spot",
            durationMinutes = 60
        )

        assertEquals(-1L, sessionId)
        val active = repository.activeSession.first()
        assertNull(active)
    }

    @Test
    fun `test B - RESTRICTED scan cannot start timer`() = runBlocking {
        val restrictedScan = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            statusChipText = "No Parking",
            allowedUntilTime = "No parking allowed",
            timeRemaining = "0m",
            parkingRules = listOf("No Parking Any Time"),
            explanation = "Active restriction in place"
        )
        val scanId = repository.saveScan(restrictedScan)

        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "Restricted Spot",
            durationMinutes = 60
        )

        assertEquals(-1L, sessionId)
        val active = repository.activeSession.first()
        assertNull(active)
    }

    @Test
    fun `test C - ALLOWED scan with 2h verified limit cannot create 3h session`() = runBlocking {
        val allowed2hScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Updated just now",
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "Park up to 2 hours"
        )
        val scanId = repository.saveScan(allowed2hScan)

        // Try to request 3 hours (180 mins)
        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "2 Hour Spot",
            durationMinutes = 180
        )

        assertTrue(sessionId > 0)
        val active = repository.activeSession.first()
        assertNotNull(active)

        // Verify clamped to 2 hours (120 mins) from start time
        val maxDurationMillis = 120 * 60 * 1000L
        val actualDurationMillis = active!!.endTime - active.startTime
        assertEquals(maxDurationMillis, actualDurationMillis)
        assertNotNull(active.maxAllowedEndTimeMillis)
        assertEquals(active.startTime + maxDurationMillis, active.maxAllowedEndTimeMillis)
    }

    @Test
    fun `test D - Extension cannot exceed verified maximum`() = runBlocking {
        val allowed2hScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Updated just now",
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "Park up to 2 hours"
        )
        val scanId = repository.saveScan(allowed2hScan)

        // Start 1 hour session (60 mins out of 120 max)
        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "2 Hour Spot",
            durationMinutes = 60
        )

        var active = repository.activeSession.first()!!
        assertEquals(active.startTime + 60 * 60 * 1000L, active.endTime)

        // Try extending by 120 mins (which would total 180 mins, exceeding 120 mins)
        val extended = repository.extendActiveSession(sessionId, 120, active.endTime)
        assertTrue(extended)

        active = repository.activeSession.first()!!
        // End time must be clamped to maxAllowedEndTimeMillis (120 mins from startTime)
        assertEquals(active.maxAllowedEndTimeMillis, active.endTime)

        // Further extension when already at max should fail
        val extendAgain = repository.extendActiveSession(sessionId, 15, active.endTime)
        assertFalse(extendAgain)
    }

    @Test
    fun `test E - Unknown duration cannot default to 120m`() = runBlocking {
        val unknownScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Updated just now",
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("Rule details unspecified"),
            explanation = "No time limit digit found"
        )
        val scanId = repository.saveScan(unknownScan)

        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "Unknown Rule Spot",
            durationMinutes = 0
        )

        assertEquals(-1L, sessionId)
        val active = repository.activeSession.first()
        assertNull(active)
    }

    @Test
    fun `test F - Valid ALLOWED timer starts and counts down correctly`() = runBlocking {
        val allowedScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Updated just now",
            allowedUntilTime = "1 Hour Parking",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("1 Hour Parking 9 AM - 5 PM"),
            explanation = "1 hour limit"
        )
        val scanId = repository.saveScan(allowedScan)

        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "1 Hour Spot",
            durationMinutes = 60
        )

        assertTrue(sessionId > 0)
        val active = repository.activeSession.first()!!
        assertTrue(active.isActive)
        assertEquals(60 * 60 * 1000L, active.endTime - active.startTime)
        assertTrue(active.remainingMillis > 0)
    }

    @Test
    fun `test G - Persisted active session retains maximum authority after reload`() = runBlocking {
        val allowedScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Updated just now",
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "2 hour limit"
        )
        val scanId = repository.saveScan(allowedScan)

        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "Persistent Spot",
            durationMinutes = 60
        )

        // Instantiate new repository instance to simulate app restart
        val newRepo = CurbRepository(app)
        val reloadedSession = newRepo.activeSession.first()

        assertNotNull(reloadedSession)
        assertNotNull(reloadedSession!!.maxAllowedEndTimeMillis)
        assertEquals(reloadedSession.startTime + 120 * 60 * 1000L, reloadedSession.maxAllowedEndTimeMillis)

        // Attempt extension using new repository instance
        val extended = newRepo.extendActiveSession(sessionId, 120, reloadedSession.endTime)
        assertTrue(extended)

        val finalSession = newRepo.activeSession.first()!!
        assertEquals(finalSession.maxAllowedEndTimeMillis, finalSession.endTime)
    }

    @Test
    fun `test H - Demo sample timer behavior remains intact`() = runBlocking {
        val preset = SampleSignPreset(
            id = "preset_allowed",
            title = "2-Hour Metered Zone",
            previewDescription = "Standard daytime parking",
            simulatedVerdict = ScanVerdict.ALLOWED,
            locationName = "Downtown Metered Spot",
            allowedUntil = "2 Hour Parking",
            rules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "Sample preset for 2 hour parking",
            detectedSigns = emptyList()
        )
        val scanResult = ScanResult(
            locationName = preset.locationName,
            cityState = "San Francisco, CA",
            verdict = preset.simulatedVerdict,
            statusChipText = "Updated just now",
            allowedUntilTime = preset.allowedUntil,
            timeRemaining = "2h 00m remaining",
            parkingRules = preset.rules,
            explanation = preset.explanation
        )
        val scanId = repository.saveScan(scanResult)

        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = preset.locationName,
            durationMinutes = 120
        )

        assertTrue(sessionId > 0)
        val active = repository.activeSession.first()!!
        assertTrue(active.isActive)
        assertEquals(120 * 60 * 1000L, active.endTime - active.startTime)
    }
}
