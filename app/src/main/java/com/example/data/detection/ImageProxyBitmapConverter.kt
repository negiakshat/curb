package com.example.data.detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy

data class ImageConversionResult(
    val bitmap: Bitmap?,
    val errorMessage: String? = null
)

object ImageProxyBitmapConverter {
    private const val TAG = "CurbCapture"

    /**
     * Converts an [ImageProxy] from CameraX to a rotated, ready-to-use [Bitmap].
     * Always safely closes [ImageProxy] in a finally block.
     */
    fun convert(image: ImageProxy): ImageConversionResult {
        var rawBitmap: Bitmap? = null
        var conversionError: String? = null

        val format = image.format
        val width = image.width
        val height = image.height
        val rotationDegrees = image.imageInfo.rotationDegrees

        Log.d(TAG, "CurbCapture: format=$format size=${width}x${height} rotation=$rotationDegrees")

        try {
            when (format) {
                ImageFormat.JPEG -> {
                    rawBitmap = convertJpegToBitmap(image)
                }
                ImageFormat.YUV_420_888 -> {
                    rawBitmap = convertYuv420ToBitmap(image)
                }
                else -> {
                    conversionError = "Couldn't process the captured photo. Please try again."
                    Log.w(TAG, "CurbCapture: Unsupported image format: $format")
                }
            }

            if (rawBitmap == null && conversionError == null) {
                conversionError = "Couldn't process the captured photo. Please try again."
            }

            // Apply rotation metadata from CameraX if needed
            val finalBitmap = if (rawBitmap != null && rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                if (rotated != rawBitmap) {
                    rawBitmap.recycle()
                }
                rotated
            } else {
                rawBitmap
            }

            if (finalBitmap != null) {
                Log.d(TAG, "CurbCapture: YUV conversion succeeded bitmap=${finalBitmap.width}x${finalBitmap.height}")
                return ImageConversionResult(finalBitmap, null)
            } else {
                Log.e(TAG, "CurbCapture: YUV conversion FAILED reason=$conversionError")
                return ImageConversionResult(null, conversionError ?: "Couldn't process the captured photo. Please try again.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "CurbCapture: YUV conversion FAILED reason=${e.javaClass.simpleName}: ${e.message}", e)
            return ImageConversionResult(null, "Couldn't process the captured photo. Please try again.")
        } finally {
            try {
                image.close()
            } catch (e: Throwable) {
                Log.w(TAG, "CurbCapture: Error closing ImageProxy: ${e.message}")
            }
        }
    }

    private fun convertJpegToBitmap(image: ImageProxy): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null
        val buffer = planes[0].buffer ?: return null
        buffer.rewind()
        val remaining = buffer.remaining()
        if (remaining <= 0) return null
        val bytes = ByteArray(remaining)
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Robust YUV_420_888 to ARGB_8888 Bitmap converter.
     * Accounts for rowStride, pixelStride, plane buffer bounds, and varying UV pixelStrides (1 or 2).
     */
    private fun convertYuv420ToBitmap(image: ImageProxy): Bitmap? {
        val planes = image.planes
        if (planes.size < 3) return null

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer ?: return null
        val uBuffer = uPlane.buffer ?: return null
        val vBuffer = vPlane.buffer ?: return null

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        val width = image.width
        val height = image.height

        if (width <= 0 || height <= 0) return null

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride

        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val yLimit = yBuffer.limit()
        val uLimit = uBuffer.limit()
        val vLimit = vBuffer.limit()

        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val yRowOffset = y * yRowStride
            val uRowOffset = (y shr 1) * uRowStride
            val vRowOffset = (y shr 1) * vRowStride

            for (x in 0 until width) {
                val yIndex = yRowOffset + x * yPixelStride
                val uIndex = uRowOffset + (x shr 1) * uPixelStride
                val vIndex = vRowOffset + (x shr 1) * vPixelStride

                val yVal = if (yIndex in 0 until yLimit) (yBuffer.get(yIndex).toInt() and 0xFF) else 0
                val uVal = if (uIndex in 0 until uLimit) ((uBuffer.get(uIndex).toInt() and 0xFF) - 128) else 0
                val vVal = if (vIndex in 0 until vLimit) ((vBuffer.get(vIndex).toInt() and 0xFF) - 128) else 0

                // BT.601 YUV to RGB Conversion
                val r = (yVal + (1436 * vVal shr 10)).coerceIn(0, 255)
                val g = (yVal - (352 * uVal + 731 * vVal shr 10)).coerceIn(0, 255)
                val b = (yVal + (1814 * uVal shr 10)).coerceIn(0, 255)

                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
