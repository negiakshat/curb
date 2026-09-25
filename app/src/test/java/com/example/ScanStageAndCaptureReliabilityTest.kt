package com.example

import android.graphics.ImageFormat
import com.example.data.detection.ImageProxyBitmapConverter
import com.example.data.model.ScanProcessingStage
import com.example.util.EvidenceAnchoringValidator
import com.example.util.SemanticConsistencyValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanStageAndCaptureReliabilityTest {

    @Test
    fun testScanProcessingStageStatusTextMapping() {
        assertEquals("Reading your sign…", ScanProcessingStage.CAPTURED.statusText)
        assertEquals("Finding the sign…", ScanProcessingStage.LOCAL_DETECTION.statusText)
        assertEquals("Reading your sign…", ScanProcessingStage.CROP_CREATION.statusText)
        assertEquals("Reading the parking rules…", ScanProcessingStage.LOCATION_RESOLUTION.statusText)
        assertEquals("Checking the details…", ScanProcessingStage.GEMINI_REQUEST.statusText)
        assertEquals("Verifying the result…", ScanProcessingStage.EVIDENCE_VALIDATION.statusText)
    }

    @Test
    fun testRegexGroupValuesSafeAccessNoException() {
        // Test extracting clock hours with safe group values
        val ocrWithTime = "PARKING 8 AM TO 6 PM"
        val hours = EvidenceAnchoringValidator.extractClockHours(ocrWithTime)
        assertTrue(hours.contains("8AM"))
        assertTrue(hours.contains("6PM"))

        // Test malformed text without groups
        val malformed = "NO PARKING AT ALL TIMES"
        val malformedHours = EvidenceAnchoringValidator.extractClockHours(malformed)
        assertTrue(malformedHours.isEmpty())
    }

    @Test
    fun testImageProxyConverterHandlesUnsupportedFormatGracefully() {
        val yBuffer = ByteBuffer.allocateDirect(100)
        val uBuffer = ByteBuffer.allocateDirect(50)
        val vBuffer = ByteBuffer.allocateDirect(50)

        val yPlane = ImageProxyBitmapConverterTest.TestPlaneProxy(yBuffer, 10, 1)
        val uPlane = ImageProxyBitmapConverterTest.TestPlaneProxy(uBuffer, 5, 1)
        val vPlane = ImageProxyBitmapConverterTest.TestPlaneProxy(vBuffer, 5, 1)

        val dummyProxy = ImageProxyBitmapConverterTest.TestImageProxy(
            width = 10,
            height = 10,
            format = 99999, // Unsupported format
            planes = arrayOf(yPlane, uPlane, vPlane),
            rotationDegrees = 0
        )

        val result = ImageProxyBitmapConverter.convert(dummyProxy)
        assertNull(result.bitmap)
        assertEquals("Couldn't process the captured photo. Please try again.", result.errorMessage)
        assertTrue(dummyProxy.isClosed)
    }
}
