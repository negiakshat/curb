package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.detection.LocalSignCrop
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.util.EvidenceAnchoringValidator
import com.example.util.OcrQuality
import com.example.util.ParkingAuthority
import com.example.util.ParkingTimerCalculator
import com.example.util.ParkingTimeEvidenceBuilder
import com.example.util.ParkingTimeEvidenceType
import com.example.util.SemanticConsistencyValidator
import com.example.util.TimerSemanticMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0 SCAN RELIABILITY REGRESSION TESTS.
 *
 * Core failure being pinned:
 * Weak/uncertain local OCR overrode a coherent Gemini visual interpretation and forced
 * AMBIGUOUS, causing correct signs like "2 HOUR PARKING / 8AM-6PM / EXCEPT SAT & SUN" to
 * show "RULE UNCLEAR" and block the timer.
 *
 * The fix makes weak/uncertain OCR SUPPORTING evidence: a coherent Gemini ALLOWED result
 * anchored by a deterministically parsed maximum stay survives; genuine restrictions,
 * conflicting signs, and prose without objective time evidence still become AMBIGUOUS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanReliabilityP0Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun uncertainCrop(
        id: String = "crop_1",
        ocrText: String = "2 H0UR PARKIN"
    ): LocalSignCrop {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        return LocalSignCrop(
            id = id,
            bitmap = bitmap,
            normalizedBox = SignBoundingBox(id, 0.1f, 0.1f, 0.9f, 0.9f, "Sign Plate", ocrText, 0.5f),
            ocrText = ocrText,
            fileUri = "/cache/real_crop_$id.jpg",
            isDemo = false,
            ocrQuality = OcrQuality.WEAK,
            isUncertain = true
        )
    }

    /** Real-world P0 sign: "2 HOUR PARKING, 8AM-6PM, EXCEPT SAT & SUN", weak OCR crop. */
    private fun twoHourGeminiScan(signUncertain: Boolean = true): ScanResult {
        val crop = uncertainCrop()
        return ScanResult(
            locationName = "Market St",
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Parking allowed",
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM", "Except Saturday & Sunday"),
            explanation = "2 hour parking permitted between 8 AM and 6 PM except weekends.",
            detectedSigns = listOf(
                DetectedSign(
                    id = "crop_1",
                    title = "2 Hour Parking",
                    subtitle = "8AM-6PM EXCEPT SAT & SUN",
                    applicableDaysHours = "8AM-6PM EXCEPT SAT & SUN",
                    restrictions = "2 Hour Parking",
                    ruleText = "2 Hour Parking 8 AM - 6 PM",
                    isUncertain = signUncertain,
                    rawText = crop.ocrText,
                    croppedImageUri = crop.fileUri
                )
            )
        )
    }

    // ============================================================
    // 1. Deterministic objective evidence extraction
    // ============================================================

    @Test
    fun test1_objectiveEvidence_parsedFromVerifiedSignText() {
        val evidence = SemanticConsistencyValidator.extractObjectiveTimeEvidence(
            "2 HOUR PARKING 8AM-6PM EXCEPT SAT & SUN"
        )
        assertEquals(120, evidence.maxStayMinutes)
        assertEquals("6PM", evidence.cutoffTime)
        assertTrue(evidence.applicableDays.contains("SAT"))
        assertTrue(evidence.applicableDays.contains("SUN"))
    }

    @Test
    fun test2_objectiveEvidence_whitespaceTolerant() {
        // Weak OCR often collapses/loses spacing: "2HOUR", "8AM"
        val evidence = SemanticConsistencyValidator.extractObjectiveTimeEvidence("2HOUR PARKING 8AM-6PM")
        assertEquals(120, evidence.maxStayMinutes)
        assertEquals("6PM", evidence.cutoffTime)
    }

    @Test
    fun test3_proseWithoutTimeAnchor_hasNoObjectiveEvidence() {
        assertFalse(
            SemanticConsistencyValidator.hasMaxStayEvidence(
                "Parking may be permitted near the civic center"
            )
        )
        assertFalse(
            SemanticConsistencyValidator.hasObjectiveTimeEvidence(
                "Standard parking rules assumed for this area"
            )
        )
    }

    // ============================================================
    // 2. The exact P0 failure: weak OCR must NOT veto coherent Gemini ALLOWED
    // ============================================================

    @Test
    fun test4_uncertainOcr_doesNotDowngradeMaxStayAnchoredGeminiAllowed() {
        val scan = twoHourGeminiScan()
        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(uncertainCrop()))

        assertEquals(
            "Weak OCR must not force AMBIGUOUS on a max-stay-anchored Gemini ALLOWED result",
            ScanVerdict.ALLOWED,
            result.verdict
        )
        assertEquals("Parking allowed", result.statusChipText)
        assertEquals("6:00 PM", result.allowedUntilTime)
        assertEquals("2h 00m remaining", result.timeRemaining)
    }

    @Test
    fun test5_uncertainOcr_doesNotBlockTimerAuthorization() {
        val scan = twoHourGeminiScan()
        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(uncertainCrop()))

        assertTrue(
            "Timer must be authorized for the verified 2-hour limit",
            SemanticConsistencyValidator.canAuthorizeTimer(result)
        )
        assertTrue(ParkingAuthority.canAuthorizeTimer(result))
    }

    @Test
    fun test6_uncertainOcr_doesNotBlockTimerCalculation() {
        val scan = twoHourGeminiScan()
        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(uncertainCrop()))
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertTrue("Timer must start for the real 2-hour sign", config.canStart)
        assertEquals(TimerSemanticMode.TIMED_LIMIT, config.mode)
        assertEquals(120, config.calculatedMinutes)
        assertEquals("2h 00m", config.formattedDuration)
        assertNotNull(config.maxAllowedEndTimeMillis)
    }

    @Test
    fun test7_realSignResult_isNotNoFixedTimeLimit() {
        val scan = twoHourGeminiScan()
        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(uncertainCrop()))
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertTrue(config.canStart)
        assertTrue(
            "A verified 2-hour sign must never yield a no-fixed-time-limit result",
            config.mode != TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT &&
                    config.mode != TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED
        )
    }

    @Test
    fun test8_geminiVisualEvidence_usableWhenSignUncertain() {
        val scan = twoHourGeminiScan()
        val evidence = ParkingTimeEvidenceBuilder.buildParkingTimeEvidence(scan)

        assertTrue(
            "Uncertain crop must not veto Gemini visual evidence for a max-stay-anchored rule",
            evidence.type == ParkingTimeEvidenceType.POSTED_DURATION ||
                    evidence.type == ParkingTimeEvidenceType.BOTH
        )
        assertEquals(120, evidence.durationMinutes)
    }

    @Test
    fun test9_fullPipeline_uncertainCropWithCoherentGemini_staysAllowed() {
        val crop = uncertainCrop(ocrText = "2 H0UR 8AM")
        val scan = twoHourGeminiScan()
        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(scan, listOf(crop))

        assertEquals(
            "End-to-end: weak local OCR must not downgrade the coherent verified result",
            ScanVerdict.ALLOWED,
            anchored.verdict
        )
        assertTrue(
            "End-to-end timer authority must be preserved",
            ParkingAuthority.canAuthorizeTimer(anchored) &&
                    SemanticConsistencyValidator.canAuthorizeTimer(anchored)
        )
    }

    // ============================================================
    // 3. Genuine conflicts / restrictions / unclear evidence STILL ambiguous
    // ============================================================

    @Test
    fun test10_conflictingSigns_stillAmbiguous_evenWithMaxStayText() {
        val crop1 = uncertainCrop("crop_1", "NO PARKING ANY TIME")
        val crop2 = uncertainCrop("crop_2", "2 HOUR PARKING 8 AM TO 6 PM")
        val scan = ScanResult(
            locationName = "Corner Spot",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("No parking any time", "2 Hour Parking 8 AM - 6 PM"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "No Parking", isRestrictingNow = true, isUncertain = true, rawText = crop1.ocrText),
                DetectedSign(id = "crop_2", title = "2 Hour Parking", isUncertain = true, rawText = crop2.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop1, crop2))
        assertEquals("Conflicting signs must remain AMBIGUOUS", ScanVerdict.AMBIGUOUS, result.verdict)
        assertFalse(SemanticConsistencyValidator.canAuthorizeTimer(result))
    }

    @Test
    fun test11_uncertainSignWithoutMaxStay_stillAmbiguous() {
        val crop = uncertainCrop(ocrText = "PARTIALLY OBSCURED SIGN")
        val scan = ScanResult(
            locationName = "Post St",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "5:00 PM",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("Obscured rule"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Obscured Sign", isUncertain = true, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(
            "Prose without objective max-stay evidence must still become AMBIGUOUS",
            ScanVerdict.AMBIGUOUS,
            result.verdict
        )
        assertEquals("Verify physical signage", result.allowedUntilTime)
        assertFalse(SemanticConsistencyValidator.canAuthorizeTimer(result))
    }

    @Test
    fun test12_activeRestriction_stillRestricted_evenWithUncertainCrop() {
        val crop = uncertainCrop("crop_1", "N0 PARKING T0WAY")
        val scan = ScanResult(
            locationName = "Sweep Spot",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("No Parking Tow Away Zone"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Tow Away", isRestrictingNow = true, isUncertain = true, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals("A genuine restriction must still produce RESTRICTED", ScanVerdict.RESTRICTED, result.verdict)
        assertFalse(ParkingAuthority.canAuthorizeTimer(result))
    }

    @Test
    fun test13_ambiguousVerdict_stillAmbiguous_withUncertainSign() {
        val crop = uncertainCrop(ocrText = "UNCLEAR OBSCURED SIGN")
        val scan = ScanResult(
            locationName = "Ambiguous Spot",
            verdict = ScanVerdict.AMBIGUOUS,
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("Signage unclear"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Unclear Sign", isUncertain = true, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
        assertFalse(SemanticConsistencyValidator.canAuthorizeTimer(result))
    }

    @Test
    fun test14_uncertainSignWithMaxStay_cannotBypassAmbiguousVerdict() {
        // Gemini verdict itself is AMBIGUOUS -> must stay AMBIGUOUS even with max-stay text
        val crop = uncertainCrop()
        val scan = twoHourGeminiScan().copy(verdict = ScanVerdict.AMBIGUOUS)

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
        assertFalse(ParkingAuthority.canAuthorizeTimer(result))
    }

    // ============================================================
    // 4. Whitespace-robust evidence matching
    // ============================================================

    @Test
    fun test15_scheduleValidation_toleratesWeakOcrSpacing() {
        assertTrue(
            "Claimed '2 Hour Parking' must match weak OCR '2HOUR PARKING' (whitespace collapsed)",
            EvidenceAnchoringValidator.isScheduleSupportedByEvidence(
                "2 Hour Parking 8 AM - 6 PM",
                "2HOUR PARKING 8AM-6PM"
            )
        )
        assertFalse(
            "A genuinely invented duration must still be rejected",
            EvidenceAnchoringValidator.isScheduleSupportedByEvidence(
                "4 Hour Parking",
                "2HOUR PARKING 8AM-6PM"
            )
        )
    }
}
