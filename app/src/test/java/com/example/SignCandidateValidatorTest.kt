package com.example

import com.example.util.SignCandidateValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignCandidateValidatorTest {

    @Test
    fun testRejectsGarbageHashesAndUuids() {
        val garbageHash = "ea32937f8sxsrAPpeQnvd1788672188600"
        val uuid = "123e4567-e89b-12d3-a456-426614174000"

        assertFalse("Should reject machine hash", SignCandidateValidator.validateOcr(garbageHash).isValid)
        assertFalse("Should reject UUID", SignCandidateValidator.validateOcr(uuid).isValid)
    }

    @Test
    fun testRejectsUrlsAndWebAddresses() {
        val url1 = "https://example.com/track/12345?utm_source=camera"
        val url2 = "www.parking-app.io/scan"

        assertFalse("Should reject HTTP URL", SignCandidateValidator.validateOcr(url1).isValid)
        assertFalse("Should reject WWW URL", SignCandidateValidator.validateOcr(url2).isValid)
    }

    @Test
    fun testRejectsFilenamesAndImageMetadata() {
        val filename = "IMG_20260906_031127.jpg"
        val filePath = "file:///sdcard/DCIM/crop_123.png"

        assertFalse("Should reject filename", SignCandidateValidator.validateOcr(filename).isValid)
        assertFalse("Should reject image path", SignCandidateValidator.validateOcr(filePath).isValid)
    }

    @Test
    fun testRejectsCodeAndJsonFragments() {
        val jsonCode = "{\"status\": \"ok\", \"id\": 123}"
        val jsCode = "function processSign() { return false; }"

        assertFalse("Should reject JSON", SignCandidateValidator.validateOcr(jsonCode).isValid)
        assertFalse("Should reject code", SignCandidateValidator.validateOcr(jsCode).isValid)
    }

    @Test
    fun testAcceptsValidParkingSigns() {
        val sign1 = "NO PARKING 8 AM TO 6 PM MON-FRI"
        val sign2 = "2 HOUR PARKING 9AM - 5PM"
        val sign3 = "TOW-AWAY NO STOPPING 7AM - 9AM"
        val sign4 = "PERMIT HOLDER EXEMPT AREA G"

        assertTrue("Should accept standard no parking sign", SignCandidateValidator.validateOcr(sign1).isValid)
        assertTrue("Should accept 2 hour limit sign", SignCandidateValidator.validateOcr(sign2).isValid)
        assertTrue("Should accept tow away sign", SignCandidateValidator.validateOcr(sign3).isValid)
        assertTrue("Should accept permit zone sign", SignCandidateValidator.validateOcr(sign4).isValid)
    }

    @Test
    fun testSanitizesOcrTextCorrectly() {
        val rawWithGarbage = "2 HOUR PARKING 8 AM - 6 PM https://track.com/abc123456"
        val sanitized = SignCandidateValidator.sanitizeOcrText(rawWithGarbage)

        assertTrue("Sanitized text should contain parking rule", sanitized.contains("2 HOUR PARKING"))
        assertFalse("Sanitized text should strip URL", sanitized.contains("https://"))
    }
}
