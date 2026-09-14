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

    @Test
    fun testSanitizeTextEightRequiredCases() {
        // 1. Pure URL
        val pureUrl = "https://images.bannerbear.com/sample.jpg"
        val res1 = SignCandidateValidator.sanitizeText(pureUrl)
        org.junit.Assert.assertEquals("Sign text could not be confidently read.", res1)

        // 2. Parking text + URL
        val parkingPlusUrl = "2 HOUR PARKING 8 AM TO 6 PM https://bannerbear.com/img"
        val res2 = SignCandidateValidator.sanitizeText(parkingPlusUrl)
        org.junit.Assert.assertEquals("2 HOUR PARKING 8 AM TO 6 PM", res2)

        // 3. Encoded URL
        val encodedUrl = "parking sign&imgurl=https%3A%2F%2Fimages.bannerbear.com%2Fsample.jpg"
        val res3 = SignCandidateValidator.sanitizeText(encodedUrl)
        assertTrue("Should preserve parking sign text", res3.contains("parking sign"))
        assertFalse("Should strip encoded URL", res3.contains("https%3A"))

        // 4. Image CDN URL
        val cdnUrl = "http://cdn.example.com/sign.png"
        val res4 = SignCandidateValidator.sanitizeText(cdnUrl)
        org.junit.Assert.assertEquals("Sign text could not be confidently read.", res4)

        // 5. Hash + parking text
        val hashPlusParking = "9a8b7c6d5e4f3a2b1c0d 2 HOUR PARKING"
        val res5 = SignCandidateValidator.sanitizeText(hashPlusParking)
        org.junit.Assert.assertEquals("2 HOUR PARKING", res5)

        // 6. Valid parking schedule
        val schedule = "MON-FRI 9 AM-5 PM"
        val res6 = SignCandidateValidator.sanitizeText(schedule)
        org.junit.Assert.assertEquals("MON-FRI 9 AM-5 PM", res6)

        // 7. &imgurl= corruption
        val imgurlCorruption = "NO PARKING MON-FRI &imgurl=https%3A%2F%2Fbannerbear.com"
        val res7 = SignCandidateValidator.sanitizeText(imgurlCorruption)
        org.junit.Assert.assertEquals("NO PARKING MON-FRI", res7)

        // 8. Long machine token
        val machineToken = "abcdef1234567890abcdef1234567890"
        val res8 = SignCandidateValidator.sanitizeText(machineToken)
        org.junit.Assert.assertEquals("Sign text could not be confidently read.", res8)
    }
}
