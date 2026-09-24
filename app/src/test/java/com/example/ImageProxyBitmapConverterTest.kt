package com.example

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import com.example.data.detection.ImageProxyBitmapConverter
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
class ImageProxyBitmapConverterTest {

    private fun createSyntheticYuvImageProxy(
        width: Int,
        height: Int,
        yRowStride: Int = width,
        yPixelStride: Int = 1,
        uvRowStride: Int = width / 2,
        uvPixelStride: Int = 1,
        rotationDegrees: Int = 0,
        format: Int = ImageFormat.YUV_420_888
    ): TestImageProxy {
        val ySize = yRowStride * height
        val uvHeight = height / 2
        val uSize = uvRowStride * uvHeight
        val vSize = uvRowStride * uvHeight

        val yBuffer = ByteBuffer.allocateDirect(ySize)
        val uBuffer = ByteBuffer.allocateDirect(uSize)
        val vBuffer = ByteBuffer.allocateDirect(vSize)

        // Fill with synthetic YUV neutral values
        for (i in 0 until ySize) {
            yBuffer.put(128.toByte())
        }
        for (i in 0 until uSize) {
            uBuffer.put(128.toByte())
        }
        for (i in 0 until vSize) {
            vBuffer.put(128.toByte())
        }

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        val yPlane = TestPlaneProxy(yBuffer, yRowStride, yPixelStride)
        val uPlane = TestPlaneProxy(uBuffer, uvRowStride, uvPixelStride)
        val vPlane = TestPlaneProxy(vBuffer, uvRowStride, uvPixelStride)

        return TestImageProxy(
            width = width,
            height = height,
            format = format,
            planes = arrayOf(yPlane, uPlane, vPlane),
            rotationDegrees = rotationDegrees
        )
    }

    @Test
    fun testSyntheticYuvFrameProducesValidBitmap() {
        val imageProxy = createSyntheticYuvImageProxy(width = 100, height = 100)
        val result = ImageProxyBitmapConverter.convert(imageProxy)

        assertNotNull(result.bitmap)
        assertEquals(100, result.bitmap?.width)
        assertEquals(100, result.bitmap?.height)
        assertNull(result.errorMessage)
        assertTrue(imageProxy.isClosed)
    }

    @Test
    fun testDifferentRowStrideProducesValidBitmap() {
        // RowStride padded to 128 for width 100
        val imageProxy = createSyntheticYuvImageProxy(
            width = 100,
            height = 100,
            yRowStride = 128,
            uvRowStride = 64
        )
        val result = ImageProxyBitmapConverter.convert(imageProxy)

        assertNotNull(result.bitmap)
        assertEquals(100, result.bitmap?.width)
        assertEquals(100, result.bitmap?.height)
        assertTrue(imageProxy.isClosed)
    }

    @Test
    fun testUvPixelStrideTwoProducesValidBitmap() {
        // UV pixelStride = 2 (interleaved UV planes)
        val imageProxy = createSyntheticYuvImageProxy(
            width = 100,
            height = 100,
            uvPixelStride = 2,
            uvRowStride = 100
        )
        val result = ImageProxyBitmapConverter.convert(imageProxy)

        assertNotNull(result.bitmap)
        assertEquals(100, result.bitmap?.width)
        assertEquals(100, result.bitmap?.height)
        assertTrue(imageProxy.isClosed)
    }

    @Test
    fun testRotatedImageProxy() {
        val imageProxy = createSyntheticYuvImageProxy(
            width = 200,
            height = 100,
            rotationDegrees = 90
        )
        val result = ImageProxyBitmapConverter.convert(imageProxy)

        assertNotNull(result.bitmap)
        // 200x100 rotated 90 degrees becomes 100x200
        assertEquals(100, result.bitmap?.width)
        assertEquals(200, result.bitmap?.height)
        assertTrue(imageProxy.isClosed)
    }

    @Test
    fun testUnsupportedFormatControlledFailure() {
        val imageProxy = createSyntheticYuvImageProxy(
            width = 100,
            height = 100,
            format = 99999 // invalid/unsupported format
        )
        val result = ImageProxyBitmapConverter.convert(imageProxy)

        assertNull(result.bitmap)
        assertNotNull(result.errorMessage)
        assertEquals("Couldn't process the captured photo. Please try again.", result.errorMessage)
        assertTrue(imageProxy.isClosed)
    }

    @Test
    fun testNullOrInvalidBuffersControlledFailure() {
        val invalidPlane = TestPlaneProxy(ByteBuffer.allocateDirect(0), 0, 0)
        val imageProxy = TestImageProxy(
            width = 100,
            height = 100,
            format = ImageFormat.YUV_420_888,
            planes = arrayOf(invalidPlane, invalidPlane, invalidPlane),
            rotationDegrees = 0
        )

        val result = ImageProxyBitmapConverter.convert(imageProxy)

        // Handled gracefully without crash or unhandled exception
        assertTrue(imageProxy.isClosed)
    }

    // Helper Test Classes
    internal class TestPlaneProxy(
        private val buffer: ByteBuffer,
        private val rowStride: Int,
        private val pixelStride: Int
    ) : ImageProxy.PlaneProxy {
        override fun getBuffer(): ByteBuffer = buffer
        override fun getRowStride(): Int = rowStride
        override fun getPixelStride(): Int = pixelStride
    }

    internal class TestImageInfo(private val rotation: Int) : ImageInfo {
        private val tagBundleInstance = object : androidx.camera.core.impl.TagBundle(emptyMap()) {}

        override fun getRotationDegrees(): Int = rotation
        override fun getSensorToBufferTransformMatrix(): Matrix = Matrix()
        override fun getTimestamp(): Long = 0L
        override fun getTagBundle(): androidx.camera.core.impl.TagBundle = tagBundleInstance
        override fun populateExifData(builder: androidx.camera.core.impl.utils.ExifData.Builder) {}
    }

    internal class TestImageProxy(
        private val width: Int,
        private val height: Int,
        private val format: Int,
        private val planes: Array<ImageProxy.PlaneProxy>,
        private val rotationDegrees: Int
    ) : ImageProxy {
        var isClosed = false

        private val info = TestImageInfo(rotationDegrees)

        override fun close() {
            isClosed = true
        }

        override fun getCropRect(): Rect = Rect(0, 0, width, height)
        override fun setCropRect(rect: Rect?) {}
        override fun getFormat(): Int = format
        override fun getHeight(): Int = height
        override fun getWidth(): Int = width
        override fun getPlanes(): Array<ImageProxy.PlaneProxy> = planes
        override fun getImageInfo(): ImageInfo = info
        override fun getImage(): Image? = null
    }
}
