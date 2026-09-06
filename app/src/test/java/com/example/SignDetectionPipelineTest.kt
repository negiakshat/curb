package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.detection.LocalSignCrop
import com.example.data.detection.SignDetectionService
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.data.remote.GeminiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SignDetectionPipelineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // 1. One sign in frame
    @Test
    fun testSingleSignCropGeneration() {
        val testBitmap = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
        val box = SignBoundingBox(
            id = "sign_1",
            left = 0.2f,
            top = 0.3f,
            right = 0.8f,
            bottom = 0.6f,
            label = "2 HR PARKING",
            ocrText = "2 HOUR PARKING 8AM TO 6PM"
        )

        val crops = SignDetectionService.cropSignsFromBoxes(context, testBitmap, listOf(box))
        assertEquals(1, crops.size)
        val crop = crops.first()
        assertEquals("sign_1", crop.id)
        assertTrue(File(crop.fileUri).exists())
        assertTrue(File(crop.fileUri).length() > 0)
        assertNotNull(crop.bitmap)
        assertTrue(crop.bitmap.width > 0)
        assertTrue(crop.bitmap.height > 0)
    }

    // 2. Multiple signs on a single post
    @Test
    fun testMultipleSignsOnPost() {
        val testBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val box1 = SignBoundingBox(
            id = "sign_1",
            left = 0.25f,
            top = 0.15f,
            right = 0.75f,
            bottom = 0.35f,
            label = "NO PARKING",
            ocrText = "NO PARKING 8AM - 10AM TUE & THU"
        )
        val box2 = SignBoundingBox(
            id = "sign_2",
            left = 0.25f,
            top = 0.40f,
            right = 0.75f,
            bottom = 0.60f,
            label = "2 HR PARKING",
            ocrText = "2 HOUR PARKING 8AM - 6PM MON - FRI"
        )

        val crops = SignDetectionService.cropSignsFromBoxes(context, testBitmap, listOf(box1, box2))
        assertEquals(2, crops.size)
        assertTrue(File(crops[0].fileUri).exists())
        assertTrue(File(crops[1].fileUri).exists())
        assertTrue(crops[0].fileUri != crops[1].fileUri)
    }

    // 3. No sign in frame (graceful handling, no fake dummy signs)
    @Test
    fun testNoSignsInFrame() = runBlocking {
        val blankBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)

        val detectionResult = SignDetectionService.detectAndCropSigns(context, blankBitmap)
        assertTrue(detectionResult.signs.isEmpty())
        assertEquals(0, detectionResult.totalDetected)
        assertEquals("No parking sign detected.", detectionResult.rawSummary)

        val result = GeminiService.generateIntelligentScanResult(
            locationName = "Blank Spot",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = emptyList()
        )

        // Must not invent fake signs with empty images
        assertTrue(result.detectedSigns.isEmpty())
        assertEquals(ScanVerdict.AMBIGUOUS, result.verdict)
        assertTrue(result.explanation.contains("No distinct parking signs"))
    }

    // 4. Distant / small sign
    @Test
    fun testDistantSmallSignClamping() {
        val testBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val smallBox = SignBoundingBox(
            id = "sign_small",
            left = 0.45f,
            top = 0.45f,
            right = 0.55f,
            bottom = 0.55f,
            label = "PARKING",
            ocrText = "PARK"
        )

        val crops = SignDetectionService.cropSignsFromBoxes(context, testBitmap, listOf(smallBox))
        assertEquals(1, crops.size)
        val cropFile = File(crops[0].fileUri)
        assertTrue(cropFile.exists())
        assertTrue(cropFile.length() > 0)
    }

    // 5. Rotation handling (coordinate padding & boundaries)
    @Test
    fun testRotatedBoundsStayWithinImage() {
        val wideBitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val nearEdgeBox = SignBoundingBox(
            id = "sign_edge",
            left = 0.01f,
            top = 0.01f,
            right = 0.99f,
            bottom = 0.99f,
            label = "TOW-AWAY ZONE",
            ocrText = "TOW-AWAY"
        )

        val crops = SignDetectionService.cropSignsFromBoxes(context, wideBitmap, listOf(nearEdgeBox))
        assertEquals(1, crops.size)
        assertTrue(File(crops[0].fileUri).exists())
    }

    // 6. Diverse aspect ratios (tall, wide, square)
    @Test
    fun testDiverseAspectRatios() {
        val tallBitmap = Bitmap.createBitmap(720, 1600, Bitmap.Config.ARGB_8888)
        val wideBitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
        val squareBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)

        val box = SignBoundingBox("s", 0.2f, 0.2f, 0.8f, 0.8f, "SIGN", "PARKING")

        val tallCrops = SignDetectionService.cropSignsFromBoxes(context, tallBitmap, listOf(box))
        val wideCrops = SignDetectionService.cropSignsFromBoxes(context, wideBitmap, listOf(box))
        val squareCrops = SignDetectionService.cropSignsFromBoxes(context, squareBitmap, listOf(box))

        assertEquals(1, tallCrops.size)
        assertEquals(1, wideCrops.size)
        assertEquals(1, squareCrops.size)
    }

    // 7. Fallback when network/Gemini is unavailable
    @Test
    fun testFallbackIntelligentSynthesisPreservesRealCrops() {
        val testBitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        val box = SignBoundingBox("s1", 0.1f, 0.1f, 0.9f, 0.5f, "2 HR PARKING", "2 HOUR PARKING MON-FRI")
        val crops = SignDetectionService.cropSignsFromBoxes(context, testBitmap, listOf(box))

        val fallbackResult = GeminiService.generateIntelligentScanResult(
            locationName = "Downtown Meter",
            cityState = "San Francisco, CA",
            isLocationKnown = true,
            localDetections = crops
        )

        assertEquals(1, fallbackResult.detectedSigns.size)
        val detected = fallbackResult.detectedSigns.first()
        assertNotNull(detected.croppedImageUri)
        assertTrue(File(detected.croppedImageUri!!).exists())
        assertEquals(ScanVerdict.ALLOWED, fallbackResult.verdict)
    }

    // 8. Repeated scans (caches clean up or write without collisions)
    @Test
    fun testRepeatedScansDoNotCollide() {
        val bmp = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val box = SignBoundingBox("s", 0.2f, 0.2f, 0.7f, 0.7f, "PARKING", "PARKING")

        val scan1 = SignDetectionService.cropSignsFromBoxes(context, bmp, listOf(box))
        val scan2 = SignDetectionService.cropSignsFromBoxes(context, bmp, listOf(box))

        assertEquals(1, scan1.size)
        assertEquals(1, scan2.size)
        assertTrue(File(scan1[0].fileUri).exists())
        assertTrue(File(scan2[0].fileUri).exists())
    }
}
