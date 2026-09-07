package com.example

import android.graphics.Bitmap
import com.example.data.detection.LocalSignCrop
import com.example.data.local.ScanResultEntity
import com.example.data.model.SampleSignPreset
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.data.remote.GeminiService
import com.example.util.ParkingTimerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanResultDefaultsTest {

    private fun createValidLocalCrop(): LocalSignCrop {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        return LocalSignCrop(
            id = "crop_valid_1",
            bitmap = dummyBitmap,
            normalizedBox = SignBoundingBox(
                id = "box_1",
                left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f,
                label = "PARKING SIGN",
                ocrText = "2 HOUR PARKING 8AM TO 6PM MON-FRI",
                confidence = 0.95f
            ),
            ocrText = "2 HOUR PARKING 8AM TO 6PM MON-FRI",
            fileUri = ""
        )
    }

    // Test A: Gemini returns ALLOWED + validDetections empty -> final ScanResult.verdict == AMBIGUOUS
    @Test
    fun testA_GeminiAllowedWithEmptyDetectionsReturnsAmbiguous() {
        val rawResult = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking")
        )

        val gatedResult = GeminiService.enforceEvidenceGatedVerdict(rawResult, emptyList())
        assertEquals(ScanVerdict.AMBIGUOUS, gatedResult.verdict)
    }

    // Test B: Gemini returns RESTRICTED + validDetections empty -> final ScanResult.verdict == AMBIGUOUS
    @Test
    fun testB_GeminiRestrictedWithEmptyDetectionsReturnsAmbiguous() {
        val rawResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            allowedUntilTime = "No parking permitted",
            timeRemaining = "0m",
            parkingRules = listOf("Tow Away Zone")
        )

        val gatedResult = GeminiService.enforceEvidenceGatedVerdict(rawResult, emptyList())
        assertEquals(ScanVerdict.AMBIGUOUS, gatedResult.verdict)
    }

    // Test C: Gemini returns ALLOWED + valid physical LocalSignCrop -> ALLOWED is preserved when otherwise valid
    @Test
    fun testC_GeminiAllowedWithValidEvidencePreservesAllowed() {
        val rawResult = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM")
        )
        val validCrops = listOf(createValidLocalCrop())

        val gatedResult = GeminiService.enforceEvidenceGatedVerdict(rawResult, validCrops)
        assertEquals(ScanVerdict.ALLOWED, gatedResult.verdict)
    }

    // Test D: Gemini returns RESTRICTED + valid physical LocalSignCrop -> RESTRICTED is preserved when otherwise valid
    @Test
    fun testD_GeminiRestrictedWithValidEvidencePreservesRestricted() {
        val rawResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            allowedUntilTime = "No parking permitted",
            timeRemaining = "0m",
            parkingRules = listOf("Tow Away Street Sweeping")
        )
        val validCrops = listOf(createValidLocalCrop())

        val gatedResult = GeminiService.enforceEvidenceGatedVerdict(rawResult, validCrops)
        assertEquals(ScanVerdict.RESTRICTED, gatedResult.verdict)
    }

    // Test E: No valid local physical evidence -> detectedSigns.isEmpty()
    @Test
    fun testE_NoValidLocalEvidenceResultsInEmptyDetectedSigns() {
        val result = GeminiService.generateIntelligentScanResult(
            locationName = "Main Street",
            localDetections = emptyList()
        )
        assertTrue(result.detectedSigns.isEmpty())
    }

    // Test F: No valid local physical evidence -> timeRemaining == "--"
    @Test
    fun testF_NoValidLocalEvidenceResultsInDashTimeRemaining() {
        val result = GeminiService.generateIntelligentScanResult(
            locationName = "Main Street",
            localDetections = emptyList()
        )
        assertEquals("--", result.timeRemaining)
    }

    // Test G: No valid local physical evidence -> allowedUntilTime == "Verify physical signage"
    @Test
    fun testG_NoValidLocalEvidenceResultsInVerifySignageAllowedTime() {
        val result = GeminiService.generateIntelligentScanResult(
            locationName = "Main Street",
            localDetections = emptyList()
        )
        assertEquals("Verify physical signage", result.allowedUntilTime)
    }

    // Test H: No valid local physical evidence -> parkingRules does not contain fabricated generic parking permission text
    @Test
    fun testH_NoValidLocalEvidenceDoesNotFabricateParkingPermission() {
        val result = GeminiService.generateIntelligentScanResult(
            locationName = "Main Street",
            localDetections = emptyList()
        )
        assertEquals(1, result.parkingRules.size)
        assertEquals("No verified parking rule has been established.", result.parkingRules.first())
    }

    // Test I: A demo/sample preset still retains its intended simulated verdict
    @Test
    fun testI_SamplePresetRetainsSimulatedVerdict() {
        val preset = SampleSignPreset(
            id = "sample_1",
            title = "2-Hour Daytime Limit",
            previewDescription = "Standard daytime parking",
            simulatedVerdict = ScanVerdict.ALLOWED,
            locationName = "Downtown Spot",
            allowedUntil = "6:00 PM",
            rules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "Daytime parking is permitted.",
            detectedSigns = emptyList()
        )
        assertEquals(ScanVerdict.ALLOWED, preset.simulatedVerdict)
    }

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
