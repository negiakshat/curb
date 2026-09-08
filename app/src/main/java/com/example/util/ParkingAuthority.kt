package com.example.util

import com.example.data.detection.LocalSignCrop
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict

/**
 * Authority boundary for parking determinations.
 *
 * Core Invariant:
 * LOCATION = CONTEXT ONLY
 * VERIFIED PHYSICAL SIGN EVIDENCE = PARKING-RULE AUTHORITY
 */
object ParkingAuthority {

    /**
     * Determines whether there is verified physical sign evidence present.
     */
    fun hasVerifiedSignEvidence(localDetections: List<LocalSignCrop>): Boolean {
        return localDetections.isNotEmpty() && localDetections.any { crop ->
            !SignCandidateValidator.isDemoOrSampleCrop(crop.fileUri, crop.isDemo, crop.id) &&
                    SignCandidateValidator.validateOcr(crop.ocrText).isValid &&
                    ((crop.bitmap != null && !crop.bitmap.isRecycled) ||
                            (crop.fileUri.isNotBlank() && java.io.File(crop.fileUri).let { it.exists() && it.length() > 0 }))
        }
    }

    /**
     * Builds standardized location context text for Gemini model prompts.
     * Enforces that location context is purely informational and not parking rule evidence.
     */
    fun buildLocationContextPrompt(
        locationName: String,
        cityState: String,
        isLocationKnown: Boolean
    ): String {
        return if (isLocationKnown && locationName.isNotBlank() &&
            locationName != "Location unavailable" && locationName != "Location access needed"
        ) {
            """
            USER LOCATION CONTEXT (FOR DISPLAY/GEOGRAPHIC NAME ONLY):
            Spot Location: $locationName${if (cityState.isNotBlank()) ", $cityState" else ""}
            
            CRITICAL AUTHORITY RULE:
            - User location context is purely informational and IS NOT EVIDENCE of any parking regulation.
            - You MUST base all parking rule determinations STRICTLY AND EXCLUSIVELY on VERIFIED PHYSICAL SIGN EVIDENCE shown in the image or cropped signs provided.
            - Location context (GPS coordinates, city, street name) MUST NEVER be used to infer, assume, or invent municipal parking rules, restrictions, or allowances.
            - If no clear physical parking signs are visible or if sign evidence is missing/unclear, set verdict strictly to "AMBIGUOUS".
            """.trimIndent()
        } else {
            """
            USER LOCATION CONTEXT: Device location is unavailable.
            Analyze regulations strictly and exclusively from the visible signs in the photo.
            """.trimIndent()
        }
    }

    /**
     * Enforces the Location vs Sign Authority invariant on any ScanResult.
     */
    fun sanitizeAndEnforceAuthority(
        scanResult: ScanResult,
        localDetections: List<LocalSignCrop> = emptyList(),
        isLiveScanPipeline: Boolean = false
    ): ScanResult {
        if (scanResult.isDemo) {
            return scanResult
        }

        // 1. Live camera/image scan pipeline evaluation
        if (isLiveScanPipeline || localDetections.isNotEmpty()) {
            if (!hasVerifiedSignEvidence(localDetections)) {
                return enforceAmbiguousFallback(scanResult)
            }
        } else if (scanResult.detectedSigns.isNotEmpty()) {
            val hasValidDetectedSigns = scanResult.detectedSigns.any { sign ->
                !SignCandidateValidator.isDemoOrSampleCrop(sign.croppedImageUri, sign.isDemo, sign.id) &&
                        SignCandidateValidator.validateOcr(sign.rawText.ifBlank { sign.title }).isValid
            }
            if (!hasValidDetectedSigns) {
                return enforceAmbiguousFallback(scanResult)
            }
        } else {
            // Standalone scanResult without detectedSigns or localDetections
            val isLocationDerivedOrUnverified = scanResult.parkingRules.isEmpty() ||
                    scanResult.parkingRules.any { rule ->
                        rule.contains("No verified parking rule", ignoreCase = true) ||
                                rule.contains("Assumed", ignoreCase = true) ||
                                rule.contains("Derived", ignoreCase = true) ||
                                rule.contains("location", ignoreCase = true) ||
                                rule.contains("city center rule", ignoreCase = true)
                    } ||
                    scanResult.allowedUntilTime == "Verify physical signage"

            if (isLocationDerivedOrUnverified) {
                return enforceAmbiguousFallback(scanResult)
            }
        }

        // 2. If verdict is ALLOWED, ensure parking rules are valid and non-empty
        if (scanResult.verdict == ScanVerdict.ALLOWED) {
            val hasValidRules = scanResult.parkingRules.isNotEmpty() &&
                    scanResult.parkingRules.none {
                        it.contains("No verified parking rule", ignoreCase = true) ||
                                it.contains("Assumed", ignoreCase = true) ||
                                it.contains("Derived", ignoreCase = true) ||
                                it.contains("location", ignoreCase = true) ||
                                it.contains("city center rule", ignoreCase = true)
                    }

            if (!hasValidRules) {
                return enforceAmbiguousFallback(scanResult)
            }
        }

        return SemanticConsistencyValidator.enforceSemanticConsistency(scanResult, localDetections)
    }

    private fun enforceAmbiguousFallback(scanResult: ScanResult): ScanResult {
        val locationContextStr = if (scanResult.locationName.isNotBlank() &&
            scanResult.locationName != "Current Location" &&
            scanResult.locationName != "Location unavailable" &&
            scanResult.locationName != "Location access needed"
        ) {
            " at ${scanResult.locationName}"
        } else ""

        return scanResult.copy(
            verdict = ScanVerdict.AMBIGUOUS,
            statusChipText = "Signage unclear",
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("No verified parking rule has been established."),
            explanation = "No distinct parking signs were resolved in the image$locationContextStr. Location data provides geographic context only and cannot determine parking rules.",
            detectedSigns = emptyList(),
            paymentInfo = "",
            vehicleApplicability = ""
        )
    }

    /**
     * Verifies if a ScanResult possesses valid authority to authorize a parking timer.
     */
    fun canAuthorizeTimer(scanResult: ScanResult?): Boolean {
        if (scanResult == null) return false
        if (scanResult.verdict != ScanVerdict.ALLOWED) return false
        if (scanResult.isDemo) return true

        if (!SemanticConsistencyValidator.canAuthorizeTimer(scanResult)) {
            return false
        }

        if (scanResult.detectedSigns.isNotEmpty()) {
            return scanResult.detectedSigns.any { sign ->
                !SignCandidateValidator.isDemoOrSampleCrop(sign.croppedImageUri, sign.isDemo, sign.id) &&
                        SignCandidateValidator.validateOcr(sign.rawText.ifBlank { sign.title }).isValid
            }
        }

        return scanResult.parkingRules.isNotEmpty() &&
                scanResult.parkingRules.none {
                    it.contains("No verified parking rule", ignoreCase = true) ||
                            it.contains("Assumed", ignoreCase = true) ||
                            it.contains("Derived", ignoreCase = true) ||
                            it.contains("location", ignoreCase = true) ||
                            it.contains("city center rule", ignoreCase = true)
                }
    }
}
