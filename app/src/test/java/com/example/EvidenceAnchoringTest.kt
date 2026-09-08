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
class EvidenceAnchoringTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createValidCrop(id: String, ocrText: String, label: String = "Sign Plate"): LocalSignCrop {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        return LocalSignCrop(
            id = id,
            bitmap = dummyBitmap,
            normalizedBox = SignBoundingBox(id, 0.1f, 0.1f, 0.9f, 0.9f, label, ocrText, 0.95f),
            ocrText = ocrText,
            fileUri = "/cache/real_crop_$id.jpg",
            isDemo = false
        )
    }

    @Test
    fun testA_OneValidatedSignCrop_GeminiReturnsOneSign_ExactlyOneDetectedSign() {
        val crop = createValidCrop("crop_1", "2 HOUR PARKING 8 AM TO 6 PM")
        val validCrops = listOf(crop)

        val rawGeminiResult = ScanResult(
            locationName = "Main St",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "crop_1",
                    title = "2-Hour Daytime Limit",
                    subtitle = "Mon-Fri 8 AM - 6 PM",
                    applicableDaysHours = "Monday-Friday 8 AM - 6 PM",
                    restrictions = "2 hour max stay",
                    croppedImageUri = crop.fileUri,
                    rawText = crop.ocrText
                )
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        assertEquals("Exactly 1 DetectedSign must be returned", 1, anchored.detectedSigns.size)
        assertEquals("Sign ID must match valid crop", "crop_1", anchored.detectedSigns[0].id)
        assertEquals("Crop URI must match valid crop", crop.fileUri, anchored.detectedSigns[0].croppedImageUri)
    }

    @Test
    fun testB_OneValidatedSignCrop_GeminiReturnsThreeSigns_OnlyOneSurvives() {
        val crop = createValidCrop("crop_1", "NO PARKING TOW AWAY ZONE")
        val validCrops = listOf(crop)

        val rawGeminiResult = ScanResult(
            locationName = "Market St",
            verdict = ScanVerdict.RESTRICTED,
            parkingRules = listOf("No Parking Tow Away Zone"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Tow Away Zone", croppedImageUri = crop.fileUri),
                DetectedSign(id = "invented_sign_2", title = "Invented Meter Sign", croppedImageUri = "/cache/invented_2.jpg"),
                DetectedSign(id = "invented_sign_3", title = "Invented Permit Sign", croppedImageUri = "/cache/invented_3.jpg")
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        assertEquals("Only 1 evidence-backed DetectedSign must survive", 1, anchored.detectedSigns.size)
        assertEquals("Surviving sign must match crop ID", "crop_1", anchored.detectedSigns[0].id)
    }

    @Test
    fun testC_GeminiReturnsSignIdNotMappingToValidatedCrop_RejectsOrNeutralizesIt() {
        val crop = createValidCrop("crop_101", "COMMERCIAL LOADING ONLY 7 AM TO 4 PM")
        val validCrops = listOf(crop)

        val rawGeminiResult = ScanResult(
            locationName = "Mission St",
            verdict = ScanVerdict.RESTRICTED,
            parkingRules = listOf("Commercial loading only"),
            detectedSigns = listOf(
                DetectedSign(id = "unrecognized_id_999", title = "Unknown Sign", croppedImageUri = "/cache/unknown.jpg")
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        assertEquals(1, anchored.detectedSigns.size)
        assertEquals("Sign ID must be neutralized to valid crop ID", "crop_101", anchored.detectedSigns[0].id)
        assertEquals("Image URI must be anchored to valid crop URI", crop.fileUri, anchored.detectedSigns[0].croppedImageUri)
    }

    @Test
    fun testD_GeminiInventsScheduleUnsupportedByOcr_ScheduleResetToEmpty() {
        // Crop OCR contains NO schedule or day/time text
        val crop = createValidCrop("crop_1", "NO PARKING")
        val validCrops = listOf(crop)

        val rawGeminiResult = ScanResult(
            locationName = "Pine St",
            verdict = ScanVerdict.RESTRICTED,
            parkingRules = listOf("No Parking"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "crop_1",
                    title = "No Parking",
                    subtitle = "Mon-Fri 8 AM - 6 PM", // Invented schedule!
                    applicableDaysHours = "Monday through Friday 8:00 AM to 6:00 PM", // Invented schedule!
                    restrictions = "No parking",
                    croppedImageUri = crop.fileUri
                )
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        assertTrue("Invented schedule must be reset/cleared when unsupported by OCR", anchored.detectedSigns[0].applicableDaysHours.isBlank())
        assertTrue("Invented subtitle schedule must be reset/cleared", anchored.detectedSigns[0].subtitle.isBlank())
    }

    @Test
    fun testE_GeminiInventsPaymentRequirementUnsupportedBySign_PaymentInfoRemainsEmpty() {
        val crop = createValidCrop("crop_1", "NO PARKING TOW AWAY ZONE")
        val validCrops = listOf(crop)

        val rawGeminiResult = ScanResult(
            locationName = "Geary Blvd",
            verdict = ScanVerdict.RESTRICTED,
            paymentInfo = "Pay at meter $2.00/hr (Coins or App)", // Invented payment requirement!
            parkingRules = listOf("No Parking Tow Away Zone"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Tow Away Zone", croppedImageUri = crop.fileUri)
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        assertTrue("Payment info must remain empty if unsupported by validated sign OCR", anchored.paymentInfo.isBlank())
    }

    @Test
    fun testF_GeminiInventsTimeLimitUnsupportedBySign_NoTimerAuthorityCreated() {
        val crop = createValidCrop("crop_1", "TEMPORARY CONSTRUCTION NO PARKING")
        val validCrops = listOf(crop)

        val rawGeminiResult = ScanResult(
            locationName = "Post St",
            verdict = ScanVerdict.ALLOWED, // Gemini wrongly set ALLOWED
            allowedUntilTime = "5:00 PM", // Invented time limit
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("Constructed daytime limit"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Construction Sign", croppedImageUri = crop.fileUri)
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        assertFalse("No timer authority created for invented time limit", ParkingAuthority.canAuthorizeTimer(anchored))

        val timerConfig = ParkingTimerCalculator.calculateConfig(anchored)
        assertFalse("Timer calculator rejects result without verified time limit evidence", timerConfig.isValidAllowed)
    }

    @Test
    fun testG_GeminiInventsAllowedUntilTime_NeutralizedUnlessSupportedByEvidence() {
        val crop = createValidCrop("crop_1", "PERMIT PARKING ONLY AREA G")
        val validCrops = listOf(crop)

        val rawGeminiResult = ScanResult(
            locationName = "Sutter St",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "4:00 PM", // Invented clock cutoff unsupported by permit sign
            timeRemaining = "1h 30m remaining",
            parkingRules = listOf("Area G Permit Parking"),
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "Permit Parking", croppedImageUri = crop.fileUri)
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        assertEquals("Invented allowedUntilTime must be reset to 'Verify physical signage'", "Verify physical signage", anchored.allowedUntilTime)
        assertEquals("Time remaining must be reset to '--'", "--", anchored.timeRemaining)
    }

    @Test
    fun testH_FullPhotoContainsUnrelatedText_UnrelatedTextCannotBecomeParkingAuthority() {
        val crop = createValidCrop("crop_1", "2 HOUR PARKING 8 AM TO 6 PM")
        val validCrops = listOf(crop)

        // Gemini noticed a storefront in full photo saying "MUNI BUS STOP NO STOPPING AT ANY TIME"
        val rawGeminiResult = ScanResult(
            locationName = "Bus Stop Spot",
            verdict = ScanVerdict.RESTRICTED,
            parkingRules = listOf("MUNI bus stop no stopping at any time"), // Unrelated background text!
            detectedSigns = listOf(
                DetectedSign(id = "crop_1", title = "2 Hour Parking", croppedImageUri = crop.fileUri)
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, validCrops)
        // Rule about "MUNI bus stop" should be filtered out because keywords are missing from crop OCR
        assertTrue("Unrelated full-photo text rules must be filtered out", anchored.parkingRules.none { it.contains("MUNI bus stop", ignoreCase = true) })
    }

    @Test
    fun testI_NoValidatedPhysicalSignEvidence_ReturnsAmbiguousAndNoDetectedSigns() {
        val rawGeminiResult = ScanResult(
            locationName = "Unknown Spot",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "2:00 PM",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("Assumed rule"),
            detectedSigns = listOf(
                DetectedSign(id = "fake_sign_1", title = "Imagined Sign", croppedImageUri = "/cache/fake.jpg")
            )
        )

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawGeminiResult, emptyList())
        assertEquals("Without validated physical sign evidence, verdict must be AMBIGUOUS", ScanVerdict.AMBIGUOUS, anchored.verdict)
        assertTrue("Detected signs must be empty when no validated sign crops exist", anchored.detectedSigns.isEmpty())
        assertFalse("No timer authority when sign evidence is missing", ParkingAuthority.canAuthorizeTimer(anchored))
    }

    @Test
    fun testJ_DemoSampleSignsRemainIsolatedAndWork() {
        val demoScanResult = ScanResult(
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

        val anchored = EvidenceAnchoringValidator.sanitizeAndAnchorResult(demoScanResult, emptyList())
        assertTrue("Demo scan remains demo", anchored.isDemo)
        assertEquals("Demo scan retains verdict", ScanVerdict.ALLOWED, anchored.verdict)
        assertEquals("Demo scan retains allowed until time", "6:00 PM", anchored.allowedUntilTime)
        assertTrue("Demo scan retains timer authority", ParkingAuthority.canAuthorizeTimer(anchored))
    }
}
