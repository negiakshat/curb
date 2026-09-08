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
import com.example.util.ParkingAuthority
import com.example.util.SemanticConsistencyValidator
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
class SemanticConsistencyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createCrop(id: String, ocrText: String): LocalSignCrop {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        return LocalSignCrop(
            id = id,
            bitmap = dummyBitmap,
            normalizedBox = SignBoundingBox(id, 0.1f, 0.1f, 0.9f, 0.9f, "Sign Plate", ocrText, 0.95f),
            ocrText = ocrText,
            fileUri = "/cache/real_crop_$id.jpg",
            isDemo = false
        )
    }

    @Test
    fun testA_AllowedVerdictWithActiveNoParkEvidence_BecomesRestricted() {
        val crop = createCrop("crop_1", "NO PARKING TOW AWAY ZONE")
        val scan = ScanResult(
            locationName = "Main St",
            verdict = ScanVerdict.ALLOWED, // Contradictory verdict
            allowedUntilTime = "5:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("No Parking Tow Away Zone"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Tow Away Zone", croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals("Contradictory ALLOWED verdict with active NO PARK evidence must become RESTRICTED", ScanVerdict.RESTRICTED, result.verdict)
        assertEquals("No parking permitted", result.allowedUntilTime)
        assertEquals("--", result.timeRemaining)
        assertEquals("No parking", result.statusChipText)
    }

    @Test
    fun testB_RestrictedVerdictWithPopulatedAllowedUntilTime_Neutralized() {
        val crop = createCrop("crop_1", "NO PARKING STREET SWEEPING THURSDAY")
        val scan = ScanResult(
            locationName = "Pine St",
            verdict = ScanVerdict.RESTRICTED,
            allowedUntilTime = "6:00 PM", // Populated allowed time on RESTRICTED scan
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("Street Sweeping Thursday"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Street Sweeping", croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals("RESTRICTED verdict must neutralize allowedUntilTime to 'No parking permitted'", "No parking permitted", result.allowedUntilTime)
        assertEquals("RESTRICTED verdict must neutralize timeRemaining to '--'", "--", result.timeRemaining)
    }

    @Test
    fun testC_AmbiguousVerdictWithPopulatedTimeRemaining_NeutralizedToDashDash() {
        val crop = createCrop("crop_1", "UNCLEAR SIGN TEXT")
        val scan = ScanResult(
            locationName = "Market St",
            verdict = ScanVerdict.AMBIGUOUS,
            allowedUntilTime = "4:00 PM",
            timeRemaining = "1h 30m remaining",
            parkingRules = listOf("Signage unclear"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Unclear Sign", isUncertain = true, croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
        assertEquals("Verify physical signage", result.allowedUntilTime)
        assertEquals("--", result.timeRemaining)
    }

    @Test
    fun testD_AllowedVerdictWithNoVerifiedDuration_TimerNotAuthorized() {
        val crop = createCrop("crop_1", "PERMIT PARKING ONLY AREA G")
        val scan = ScanResult(
            locationName = "Sutter St",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("Area G Permit Parking"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Permit Parking", croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(ScanVerdict.ALLOWED, result.verdict)
        assertFalse("Timer must not be authorized when no verified usable duration exists", SemanticConsistencyValidator.canAuthorizeTimer(result))
        assertFalse("ParkingAuthority rejects timer without duration", ParkingAuthority.canAuthorizeTimer(result))
    }

    @Test
    fun testE_RestrictedVerdictTimerRequest_Rejected() {
        val crop = createCrop("crop_1", "NO PARKING ANY TIME")
        val scan = ScanResult(
            locationName = "Mission St",
            verdict = ScanVerdict.RESTRICTED,
            allowedUntilTime = "No parking permitted",
            timeRemaining = "--",
            parkingRules = listOf("No parking any time"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "No Parking", isRestrictingNow = true, croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        assertFalse("Timer must be rejected for RESTRICTED scan", SemanticConsistencyValidator.canAuthorizeTimer(scan))
        assertFalse("ParkingAuthority rejects timer for RESTRICTED scan", ParkingAuthority.canAuthorizeTimer(scan))
    }

    @Test
    fun testF_UnsupportedPaymentInfoWithNonPaymentSign_NeutralizedToEmpty() {
        val crop = createCrop("crop_1", "2 HOUR PARKING 8 AM TO 6 PM")
        val scan = ScanResult(
            locationName = "Geary Blvd",
            verdict = ScanVerdict.ALLOWED,
            paymentInfo = "Pay $2.50/hr at meter", // Unsupported payment info!
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "2 Hour Parking", croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertTrue("Payment info must be cleared when unsupported by sign OCR", result.paymentInfo.isBlank())
    }

    @Test
    fun testG_SignMarkedUncertain_CannotBeSoleBasisForConfidentVerdict() {
        val crop = createCrop("crop_1", "PARTIALLY OBSCURRED SIGN")
        val scan = ScanResult(
            locationName = "Post St",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "5:00 PM",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("Obscured rule"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Obscured Sign", isUncertain = true, croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals("Uncertain sign must downgrade scan to AMBIGUOUS", ScanVerdict.AMBIGUOUS, result.verdict)
        assertEquals("Verify physical signage", result.allowedUntilTime)
        assertEquals("--", result.timeRemaining)
    }

    @Test
    fun testH_ConflictingSignEvidence_DowngradedToAmbiguous() {
        val crop1 = createCrop("crop_1", "NO PARKING ANY TIME")
        val crop2 = createCrop("crop_2", "2 HOUR PARKING 8 AM TO 6 PM")
        val scan = ScanResult(
            locationName = "Corner Spot",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("No parking any time", "2 Hour Parking 8 AM - 6 PM"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "No Parking", isRestrictingNow = true, croppedImageUri = crop1.fileUri, rawText = crop1.ocrText),
                DetectedSign(id = "crop_2", title = "2 Hour Parking", croppedImageUri = crop2.fileUri, rawText = crop2.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop1, crop2))
        assertEquals("Conflicting sign evidence must produce AMBIGUOUS verdict", ScanVerdict.AMBIGUOUS, result.verdict)
        assertEquals("Signage unclear", result.statusChipText)
        assertEquals("Verify physical signage", result.allowedUntilTime)
    }

    @Test
    fun testI_EvidenceBackedAllowedRuleWithValidDuration_CoherentAllowedResult() {
        val crop = createCrop("crop_1", "2 HOUR PARKING 8 AM TO 6 PM")
        val scan = ScanResult(
            locationName = "Valid Spot",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "2 Hour Parking", croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(ScanVerdict.ALLOWED, result.verdict)
        assertEquals("6:00 PM", result.allowedUntilTime)
        assertEquals("2h 00m remaining", result.timeRemaining)
        assertTrue("Coherent ALLOWED scan can authorize timer", SemanticConsistencyValidator.canAuthorizeTimer(result))
    }

    @Test
    fun testJ_EvidenceBackedRestrictionWithNoPermission_CoherentRestrictedResult() {
        val crop = createCrop("crop_1", "NO PARKING STREET SWEEPING MON 8 AM TO 10 AM")
        val scan = ScanResult(
            locationName = "Sweeping Zone",
            verdict = ScanVerdict.RESTRICTED,
            allowedUntilTime = "No parking permitted",
            timeRemaining = "--",
            parkingRules = listOf("Street Sweeping Monday 8 AM - 10 AM"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Street Sweeping", isRestrictingNow = true, croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(ScanVerdict.RESTRICTED, result.verdict)
        assertEquals("No parking permitted", result.allowedUntilTime)
        assertEquals("--", result.timeRemaining)
        assertEquals("No parking", result.statusChipText)
        assertFalse("RESTRICTED result cannot authorize timer", SemanticConsistencyValidator.canAuthorizeTimer(result))
    }

    @Test
    fun testK_ParkingRulesContradictFinalVerdict_ContradictionRemoved() {
        val crop = createCrop("crop_1", "NO PARKING TOW AWAY ZONE")
        val scan = ScanResult(
            locationName = "Restricted Street",
            verdict = ScanVerdict.RESTRICTED,
            allowedUntilTime = "No parking permitted",
            timeRemaining = "--",
            parkingRules = listOf("No Parking Tow Away Zone", "2 Hour Parking allowed"), // Contradictory rule!
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Tow Away Zone", isRestrictingNow = true, croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals(ScanVerdict.RESTRICTED, result.verdict)
        assertTrue("Contradictory permission claim must be removed from parkingRules", result.parkingRules.none { it.contains("2 Hour Parking allowed") })
    }

    @Test
    fun testL_ExplanationAndStatusChipContradictVerdict_Normalized() {
        val crop = createCrop("crop_1", "NO PARKING TOW AWAY ZONE")
        val scan = ScanResult(
            locationName = "Restricted Street",
            verdict = ScanVerdict.RESTRICTED,
            statusChipText = "Parking allowed", // Contradictory chip!
            allowedUntilTime = "No parking permitted",
            timeRemaining = "--",
            explanation = "Parking is allowed at this spot.", // Contradictory explanation!
            parkingRules = listOf("No Parking Tow Away Zone"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Tow Away Zone", isRestrictingNow = true, croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(scan, listOf(crop))
        assertEquals("No parking", result.statusChipText)
        assertFalse("Explanation must not claim parking is allowed when RESTRICTED", result.explanation.contains("Parking is allowed"))
    }

    @Test
    fun testM_GeminiAndLocalFallbackProduceSameSemanticInvariants() {
        val crop = createCrop("crop_1", "2 HOUR PARKING 8 AM TO 6 PM")

        val rawGeminiResult = ScanResult(
            locationName = "Test Spot",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "2 Hour Parking", croppedImageUri = crop.fileUri, rawText = crop.ocrText)
            )
        )

        val geminiAnchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, listOf(crop))
        val localAnchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, listOf(crop))

        assertEquals(geminiAnchored.verdict, localAnchored.verdict)
        assertEquals(geminiAnchored.allowedUntilTime, localAnchored.allowedUntilTime)
        assertEquals(geminiAnchored.timeRemaining, localAnchored.timeRemaining)
        assertEquals(geminiAnchored.statusChipText, localAnchored.statusChipText)
    }

    @Test
    fun testN_DemoSampleResultRemainsUnaffected() {
        val demoScan = ScanResult(
            locationName = "Demo Metered Spot",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            detectedSigns = listOf(
                DetectedSign(id = "demo_1", title = "2 HOUR PARKING", isDemo = true, croppedImageUri = "/cache/demo_1.jpg")
            ),
            isDemo = true
        )

        val result = SemanticConsistencyValidator.enforceSemanticConsistency(demoScan, emptyList())
        assertTrue("Demo scan remains demo", result.isDemo)
        assertEquals("Demo scan retains verdict", ScanVerdict.ALLOWED, result.verdict)
        assertEquals("Demo scan retains allowed until time", "6:00 PM", result.allowedUntilTime)
        assertTrue("Demo scan retains timer eligibility", SemanticConsistencyValidator.canAuthorizeTimer(result))
    }
}
