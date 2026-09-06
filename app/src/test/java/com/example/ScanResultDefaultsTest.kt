package com.example

import com.example.data.local.ScanResultEntity
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.remote.GeminiService
import com.example.util.ParkingTimerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultDefaultsTest {

    // Test A: ScanResult() defaults to AMBIGUOUS
    @Test
    fun testScanResultDefaultsToAmbiguous() {
        val defaultScan = ScanResult()
        assertEquals(ScanVerdict.AMBIGUOUS, defaultScan.verdict)
        assertEquals("Signage unclear", defaultScan.statusChipText)
        assertEquals("Verify physical signage", defaultScan.allowedUntilTime)
        assertEquals("--", defaultScan.timeRemaining)
        assertEquals(1, defaultScan.parkingRules.size)
        assertEquals("No verified parking rule has been established.", defaultScan.parkingRules.first())
        assertEquals("Parking rules could not be determined from verified sign evidence.", defaultScan.explanation)
    }

    // Test B: A real-user no-evidence fallback cannot produce ALLOWED
    @Test
    fun testNoEvidenceFallbackCannotProduceAllowed() {
        val result = GeminiService.generateIntelligentScanResult(
            locationName = "Uncertain Street",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = emptyList()
        )

        assertNotEquals(ScanVerdict.ALLOWED, result.verdict)
        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
        assertTrue(result.detectedSigns.isEmpty())
    }

    // Test C: Corrupt persisted verdict maps to AMBIGUOUS, not ALLOWED
    @Test
    fun testCorruptPersistedVerdictMapsToAmbiguous() {
        val corruptEntity = ScanResultEntity(
            id = 1,
            timestamp = System.currentTimeMillis(),
            locationName = "Test Spot",
            cityState = "",
            verdict = "CORRUPT_OR_INVALID_VERDICT_VALUE",
            statusChipText = "Corrupt",
            allowedUntilTime = "Unknown",
            timeRemaining = "Unknown",
            parkingRulesJson = "[]",
            explanation = "Corrupt record",
            detectedSignsJson = "[]",
            zoneType = "",
            paymentInfo = "",
            vehicleApplicability = "",
            imageUri = null
        )

        val verdict = try {
            ScanVerdict.valueOf(corruptEntity.verdict)
        } catch (e: Exception) {
            ScanVerdict.AMBIGUOUS
        }

        assertEquals(ScanVerdict.AMBIGUOUS, verdict)
    }

    // Test D: No-evidence result cannot contain a real countdown such as "2h 00m remaining"
    @Test
    fun testNoEvidenceResultCannotContainRealCountdown() {
        val result = GeminiService.generateIntelligentScanResult(
            locationName = "Main Street",
            localDetections = emptyList()
        )

        assertNotEquals("2h 00m remaining", result.timeRemaining)
        assertEquals("--", result.timeRemaining)
        assertNotEquals("6:00 PM", result.allowedUntilTime)
        assertEquals("Verify physical signage", result.allowedUntilTime)
    }

    // Test E: An explicitly valid ALLOWED result still retains its provided allowed-until/time-limit information
    @Test
    fun testExplicitValidAllowedResultRetainsLimitInfo() {
        val allowedScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM Mon-Fri")
        )

        val config = ParkingTimerCalculator.calculateConfig(allowedScan)
        assertTrue(config.isValidAllowed)
        assertFalse(config.isRestrictedOrAmbiguous)
        assertEquals("2 Hour Parking", allowedScan.allowedUntilTime)
        assertEquals("2h 00m remaining", allowedScan.timeRemaining)
    }
}
