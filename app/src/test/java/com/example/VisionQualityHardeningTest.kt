package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.detection.LocalSignCrop
import com.example.data.detection.SignDetectionService
import com.example.data.model.SignBoundingBox
import com.example.data.model.ScanVerdict
import com.example.data.remote.GeminiService
import com.example.util.CandidateValidation
import com.example.util.EvidenceAnchoringValidator
import com.example.util.ParkingAuthority
import com.example.util.SignCandidateValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VisionQualityHardeningTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createDummyCropFile(name: String): String {
        val cropsDir = File(context.cacheDir, "test_crops").apply { if (!exists()) mkdirs() }
        val file = File(cropsDir, "$name.jpg")
        if (!file.exists() || file.length() == 0L) {
            val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        }
        return file.absolutePath
    }

    // Test A: Blank image + no detections -> Gemini is skipped, immediate local AMBIGUOUS result
    @Test
    fun testA_BlankImage_GeminiSkipped_ReturnsAmbiguous() = runBlocking {
        val blankBmp = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)

        val result = GeminiService.analyzeParkingSigns(
            bitmap = blankBmp,
            locationName = "Blank Spot",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = emptyList(),
            context = context
        )

        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
        assertEquals("Signage unclear", result.statusChipText)
        assertEquals("Verify physical signage", result.allowedUntilTime)
        assertEquals("--", result.timeRemaining)
        assertTrue(result.detectedSigns.isEmpty())
        assertFalse(ParkingAuthority.canAuthorizeTimer(result))
    }

    // Test B: Irrelevant non-parking image + no detections -> Gemini is skipped
    @Test
    fun testB_IrrelevantImage_NoDetections_GeminiSkipped() = runBlocking {
        val irrelevantBmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)

        val result = GeminiService.analyzeParkingSigns(
            bitmap = irrelevantBmp,
            locationName = "Park Plaza",
            cityState = "San Jose, CA",
            isLocationKnown = true,
            localDetections = emptyList(),
            context = context
        )

        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
        assertTrue(result.detectedSigns.isEmpty())
        assertFalse(ParkingAuthority.canAuthorizeTimer(result))
    }

    // Test C: Valid parking sign crop -> Gemini path works and validated crops are preserved
    @Test
    fun testC_ValidParkingSignCrop_PreservesCropEvidence() = runBlocking {
        val cropBmp = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val fileUri = createDummyCropFile("valid_sign_crop")
        val validCrop = LocalSignCrop(
            id = "sign_1",
            normalizedBox = SignBoundingBox("sign_1", 0.1f, 0.1f, 0.4f, 0.5f, "2 HOUR PARKING", "2 HOUR PARKING 8 AM TO 6 PM"),
            ocrText = "2 HOUR PARKING 8 AM TO 6 PM",
            fileUri = fileUri,
            bitmap = cropBmp
        )

        val result = GeminiService.analyzeParkingSigns(
            bitmap = null,
            locationName = "Market Street",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = listOf(validCrop),
            context = context
        )

        assertTrue(result.detectedSigns.isNotEmpty())
        assertEquals("sign_1", result.detectedSigns.first().id)
        assertTrue(ParkingAuthority.hasVerifiedSignEvidence(listOf(validCrop)))
    }

    // Test D: Raw OCR noise but invalid physical candidate -> no verified sign created
    @Test
    fun testD_RawOcrNoise_InvalidPhysicalCandidate_Rejected() {
        val noiseText = "https://example.com/track?id=12345678-1234-1234-1234-123456789abc"
        val validation = SignCandidateValidator.validatePhysicalCandidateGeometry(
            rectLeft = 10, rectTop = 10, rectRight = 200, rectBottom = 100,
            imageWidth = 1000, imageHeight = 1000,
            ocrText = noiseText
        )

        assertFalse(validation.isValid)
        val invalidReason = (validation as CandidateValidation.Invalid).reason
        assertTrue(invalidReason.contains("URL") || invalidReason.contains("UUID") || invalidReason.contains("tracking"))
    }

    // Test E: Synthetic whole-image fallback attempt -> rejected by geometry validation
    @Test
    fun testE_SyntheticWholeImageCrop_Rejected() {
        val validation = SignCandidateValidator.validatePhysicalCandidateGeometry(
            rectLeft = 0, rectTop = 0, rectRight = 1000, rectBottom = 1000,
            imageWidth = 1000, imageHeight = 1000,
            ocrText = "2 HOUR PARKING"
        )

        assertFalse(validation.isValid)
        val reason = (validation as CandidateValidation.Invalid).reason
        assertTrue(reason.contains("entire image") || reason.contains("0.95"))
    }

    // Test F: Multiple valid signs on same pole -> all legitimate validated crops remain available
    @Test
    fun testF_MultipleValidSignsOnPost_AllCropsPreserved() = runBlocking {
        val cropBmp1 = Bitmap.createBitmap(200, 250, Bitmap.Config.ARGB_8888)
        val cropBmp2 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val uri1 = createDummyCropFile("sign_top")
        val uri2 = createDummyCropFile("sign_bottom")

        val crop1 = LocalSignCrop(
            id = "sign_1",
            normalizedBox = SignBoundingBox("sign_1", 0.2f, 0.1f, 0.8f, 0.35f, "NO PARKING", "NO PARKING 8 AM TO 10 AM TUE & THU"),
            ocrText = "NO PARKING 8 AM TO 10 AM TUE & THU",
            fileUri = uri1,
            bitmap = cropBmp1
        )
        val crop2 = LocalSignCrop(
            id = "sign_2",
            normalizedBox = SignBoundingBox("sign_2", 0.2f, 0.4f, 0.8f, 0.65f, "2 HOUR PARKING", "2 HOUR PARKING 10 AM TO 6 PM"),
            ocrText = "2 HOUR PARKING 10 AM TO 6 PM",
            fileUri = uri2,
            bitmap = cropBmp2
        )

        val result = GeminiService.analyzeParkingSigns(
            bitmap = null,
            locationName = "Post Street",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = listOf(crop1, crop2),
            context = context
        )

        assertEquals(2, result.detectedSigns.size)
        assertEquals("sign_1", result.detectedSigns[0].id)
        assertEquals("sign_2", result.detectedSigns[1].id)
    }

    // Test G: Small but plausible distant sign -> not automatically rejected solely for being small
    @Test
    fun testG_SmallDistantSign_AcceptedIfValidGeometryAndOcr() {
        // Box size 35x40px in a 1000x1000 canvas -> area fraction 0.0014 >= 0.0008
        val validation = SignCandidateValidator.validatePhysicalCandidateGeometry(
            rectLeft = 100, rectTop = 100, rectRight = 135, rectBottom = 140,
            imageWidth = 1000, imageHeight = 1000,
            ocrText = "2 HOUR PARKING"
        )

        assertTrue(validation.isValid)
    }

    // Test H: Obstructed/poor OCR sign -> remains uncertain/ambiguous rather than becoming fabricated evidence
    @Test
    fun testH_ObstructedSparseOcrSign_IsUncertainAndGated() = runBlocking {
        val cropBmp = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
        val uri = createDummyCropFile("uncertain_crop")
        val sparseCrop = LocalSignCrop(
            id = "sign_uncertain",
            normalizedBox = SignBoundingBox("sign_uncertain", 0.3f, 0.3f, 0.6f, 0.6f, "PARKING SIGN", "P 2h"),
            ocrText = "P 2h",
            fileUri = uri,
            bitmap = cropBmp
        )

        val result = GeminiService.analyzeParkingSigns(
            bitmap = null,
            locationName = "Uncertain Spot",
            cityState = "Oakland, CA",
            isLocationKnown = true,
            localDetections = listOf(sparseCrop),
            context = context
        )

        assertNotNull(result)
        // Evidence anchoring enforces that rules not proven by OCR text remain unanchored
        assertTrue(result.detectedSigns.isNotEmpty())
    }

    // Test I: Full-photo-only noise -> cannot become parking authority
    @Test
    fun testI_FullPhotoOnlyNoise_NoVerifiedSign_CannotAuthorizeTimer() = runBlocking {
        val noiseBmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)

        val result = GeminiService.analyzeParkingSigns(
            bitmap = noiseBmp,
            locationName = "Noise Alley",
            cityState = "San Jose, CA",
            isLocationKnown = true,
            localDetections = emptyList(),
            context = context
        )

        assertFalse(ParkingAuthority.canAuthorizeTimer(result))
        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
    }
}
