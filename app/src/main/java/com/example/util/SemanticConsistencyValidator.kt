package com.example.util

import com.example.data.detection.LocalSignCrop
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import java.util.Locale

/**
 * AI Semantic-Consistency Gate (P1-03).
 *
 * Invariant: NO REAL SCAN MAY EXPOSE A COMBINATION OF FIELDS THAT DISAGREES WITH ITS FINAL VERDICT.
 *
 * Enforces internal field-level consistency across:
 * - verdict
 * - detectedSigns
 * - parkingRules
 * - explanation
 * - statusChipText
 * - allowedUntilTime
 * - timeRemaining
 * - paymentInfo
 * - vehicleApplicability
 * - timer eligibility
 */
object SemanticConsistencyValidator {

    fun enforceSemanticConsistency(
        rawResult: ScanResult,
        validDetections: List<LocalSignCrop> = emptyList()
    ): ScanResult {
        // Demo scans remain untouched in their isolated demo path
        if (rawResult.isDemo) {
            return rawResult
        }

        val signs = rawResult.detectedSigns
        val combinedOcrText = (validDetections.map { it.ocrText } + signs.map { it.rawText })
            .joinToString(" ").uppercase(Locale.US)

        val combinedRulesText = rawResult.parkingRules.joinToString(" ").uppercase(Locale.US)

        val ocrHasRestricting = isRestrictingText(combinedOcrText)
        val ocrHasPermission = isPermissionText(combinedOcrText)
        val ocrHasExemption = combinedOcrText.contains("EXEMPT") || combinedOcrText.contains("PERMIT") || combinedOcrText.contains("EXCEPT")
        val rulesHasRestricting = isRestrictingText(combinedRulesText)

        val hasUncertainSign = signs.any { it.isUncertain }
        val hasMultipleSignsWithConflict = signs.size >= 2 && signs.any { it.isRestrictingNow || isRestrictingText(it.rawText) } && signs.any { isPermissionText(it.rawText) } && !ocrHasExemption

        // 1. Determine Verdict
        var finalVerdict = rawResult.verdict

        if (hasUncertainSign || hasMultipleSignsWithConflict) {
            finalVerdict = ScanVerdict.AMBIGUOUS
        } else if (rawResult.verdict == ScanVerdict.RESTRICTED && (ocrHasRestricting || rulesHasRestricting)) {
            // Respect RESTRICTED verdict if supported by restriction OCR or rules
            finalVerdict = ScanVerdict.RESTRICTED
        } else if (ocrHasRestricting && !ocrHasPermission) {
            // Physical sign is purely restricting (e.g. NO PARKING TOW AWAY) -> RESTRICTED
            finalVerdict = ScanVerdict.RESTRICTED
        } else if (rawResult.verdict == ScanVerdict.ALLOWED && ocrHasRestricting && !ocrHasExemption) {
            // Verdict claims ALLOWED but physical sign has active restriction -> RESTRICTED
            finalVerdict = ScanVerdict.RESTRICTED
        } else if (ocrHasPermission && !ocrHasRestricting && finalVerdict != ScanVerdict.AMBIGUOUS) {
            // Physical sign is purely permission (e.g. 2 HOUR PARKING) -> ALLOWED
            finalVerdict = ScanVerdict.ALLOWED
        }

        // 2. Normalize Payment Info
        val hasPayment = EvidenceAnchoringValidator.hasPaymentEvidence(combinedOcrText)
        val sanitizedPaymentInfo = if (hasPayment) rawResult.paymentInfo.ifBlank { "Pay at meter or kiosk" } else ""

        // 3. Normalize Signs status & attributes
        val sanitizedSigns = signs.map { sign ->
            val isRestricting = sign.isRestrictingNow || isRestrictingText(sign.rawText) || isRestrictingText(sign.restrictions)
            val badge = when {
                finalVerdict == ScanVerdict.AMBIGUOUS || sign.isUncertain -> "Rule Unclear"
                finalVerdict == ScanVerdict.RESTRICTED || isRestricting -> "Active Restriction"
                hasPayment -> "Metered Parking"
                else -> "Parking Permitted"
            }
            sign.copy(
                isRestrictingNow = (finalVerdict == ScanVerdict.RESTRICTED || isRestricting) && finalVerdict != ScanVerdict.AMBIGUOUS,
                statusBadge = badge
            )
        }

        // 4. Enforce Verdict-Specific Field Consistency
        return when (finalVerdict) {
            ScanVerdict.AMBIGUOUS -> {
                val sanitizedRules = rawResult.parkingRules
                    .filterNot { isConfidentPermissionClaim(it) || isConfidentRestrictionClaim(it) }
                    .ifEmpty { listOf("No verified parking rule has been established.") }

                rawResult.copy(
                    verdict = ScanVerdict.AMBIGUOUS,
                    statusChipText = "Signage unclear",
                    allowedUntilTime = "Verify physical signage",
                    timeRemaining = "--",
                    parkingRules = sanitizedRules,
                    paymentInfo = sanitizedPaymentInfo,
                    explanation = normalizeExplanation(rawResult.explanation, ScanVerdict.AMBIGUOUS),
                    detectedSigns = sanitizedSigns
                )
            }

            ScanVerdict.RESTRICTED -> {
                val sanitizedRules = rawResult.parkingRules
                    .filterNot { isConfidentPermissionClaim(it) }
                    .ifEmpty { listOf("No parking permitted at this location.") }

                rawResult.copy(
                    verdict = ScanVerdict.RESTRICTED,
                    statusChipText = "No parking",
                    allowedUntilTime = "No parking permitted",
                    timeRemaining = "--",
                    parkingRules = sanitizedRules,
                    paymentInfo = sanitizedPaymentInfo,
                    explanation = normalizeExplanation(rawResult.explanation, ScanVerdict.RESTRICTED),
                    detectedSigns = sanitizedSigns
                )
            }

            ScanVerdict.ALLOWED -> {
                val sanitizedRules = rawResult.parkingRules
                    .filterNot { isConfidentRestrictionClaim(it) }
                    .ifEmpty { listOf("Parking permitted according to posted signage.") }

                // Check for usable duration in rawResult or extract from OCR
                val extractedTime = extractAllowedUntilFromOcr(combinedOcrText)
                val (allowedUntil, remaining) = when {
                    isUsableClockTimeOrDuration(rawResult.allowedUntilTime) && rawResult.timeRemaining != "--" -> {
                        Pair(rawResult.allowedUntilTime, rawResult.timeRemaining)
                    }
                    extractedTime != null -> {
                        extractedTime
                    }
                    else -> {
                        Pair("Verify physical signage", "--")
                    }
                }

                rawResult.copy(
                    verdict = ScanVerdict.ALLOWED,
                    statusChipText = if (hasPayment) "Metered parking" else "Parking allowed",
                    allowedUntilTime = allowedUntil,
                    timeRemaining = remaining,
                    parkingRules = sanitizedRules,
                    paymentInfo = sanitizedPaymentInfo,
                    explanation = normalizeExplanation(rawResult.explanation, ScanVerdict.ALLOWED),
                    detectedSigns = sanitizedSigns
                )
            }
        }
    }

    /**
     * Authoritative decision for timer validity.
     */
    fun canAuthorizeTimer(scanResult: ScanResult?): Boolean {
        if (scanResult == null) return false
        if (scanResult.isDemo) return true
        if (scanResult.verdict != ScanVerdict.ALLOWED) return false

        if (scanResult.allowedUntilTime == "Verify physical signage" ||
            scanResult.allowedUntilTime == "No parking permitted" ||
            scanResult.allowedUntilTime == "No duration limit" ||
            scanResult.allowedUntilTime.isBlank()
        ) {
            return false
        }

        if (scanResult.timeRemaining == "--" || scanResult.timeRemaining.isBlank()) {
            return false
        }

        if (scanResult.detectedSigns.any { it.isUncertain || it.isRestrictingNow }) {
            return false
        }

        return scanResult.parkingRules.isNotEmpty() &&
                scanResult.parkingRules.none {
                    it.contains("No verified parking rule", ignoreCase = true) ||
                            it.contains("Assumed", ignoreCase = true) ||
                            it.contains("Derived", ignoreCase = true) ||
                            it.contains("No parking", ignoreCase = true)
                }
    }

    private fun extractAllowedUntilFromOcr(ocrText: String): Pair<String, String>? {
        if (ocrText.isBlank()) return null
        val upper = ocrText.uppercase(Locale.US)

        val hourMatch = Regex("(\\d+)\\s*(?:HOUR|HR|HRS)").find(upper)
        if (hourMatch != null) {
            val hours = hourMatch.groupValues[1].toIntOrNull() ?: 2
            return Pair("In $hours ${if (hours == 1) "hour" else "hours"}", "${hours}h 00m remaining")
        }
        val minMatch = Regex("(\\d+)\\s*(?:MIN|MINUTE|MINS)").find(upper)
        if (minMatch != null) {
            val mins = minMatch.groupValues[1].toIntOrNull() ?: 30
            return Pair("In $mins mins", "${mins}m remaining")
        }
        val pmMatch = Regex("(\\d{1,2}(?::\\d{2})?\\s*PM)").find(upper)
        if (pmMatch != null) {
            val clock = pmMatch.groupValues[1]
            return Pair(clock, "Until $clock")
        }
        return null
    }

    private fun isRestrictingText(upperText: String): Boolean {
        val restrictingKeywords = listOf(
            "NO PARK", "NO STOP", "TOW AWAY", "TOW-AWAY", "STREET CLEAN",
            "STREET SWEEP", "CONSTRUCTION", "NO STANDING", "BUS STOP", "LOADING ONLY"
        )
        return restrictingKeywords.any { upperText.contains(it) }
    }

    private fun isPermissionText(upperText: String): Boolean {
        val permissionKeywords = listOf(
            "2 HOUR", "1 HOUR", "3 HOUR", "PARKING ALLOWED", "PARKING PERMITTED",
            "PERMIT PARKING", "PAY AT METER", "METERED PARKING", "LIMIT"
        )
        return permissionKeywords.any { upperText.contains(it) }
    }

    private fun isConfidentPermissionClaim(rule: String): Boolean {
        val upper = rule.uppercase(Locale.US)
        return upper.contains("PARKING ALLOWED") || upper.contains("PERMITTED UNTIL") ||
                upper.contains("YOU CAN PARK") || upper.contains("2 HOUR PARKING ALLOWED")
    }

    private fun isConfidentRestrictionClaim(rule: String): Boolean {
        val upper = rule.uppercase(Locale.US)
        return upper.contains("NO PARKING") || upper.contains("TOW AWAY") ||
                upper.contains("STREET SWEEPING") || upper.contains("NO STOPPING")
    }

    private fun isUsableClockTimeOrDuration(timeStr: String): Boolean {
        if (timeStr.isBlank() || timeStr == "Verify physical signage" || timeStr == "No parking permitted" || timeStr == "--") {
            return false
        }
        val hasDigits = timeStr.any { it.isDigit() }
        val hasClockOrDuration = timeStr.contains("AM", ignoreCase = true) ||
                timeStr.contains("PM", ignoreCase = true) ||
                timeStr.contains("h", ignoreCase = true) ||
                timeStr.contains("m", ignoreCase = true) ||
                timeStr.contains("rem", ignoreCase = true)
        return hasDigits && hasClockOrDuration
    }

    private fun normalizeExplanation(currentExp: String, verdict: ScanVerdict): String {
        return when (verdict) {
            ScanVerdict.AMBIGUOUS -> {
                if (currentExp.contains("allowed", ignoreCase = true) || currentExp.contains("restricted", ignoreCase = true)) {
                    "Sign evidence is unclear or conflicting. Please verify posted physical signage before parking."
                } else currentExp.ifBlank { "Sign evidence is unclear or ambiguous." }
            }
            ScanVerdict.RESTRICTED -> {
                if (currentExp.contains("allowed", ignoreCase = true)) {
                    "Parking is restricted at this location based on posted sign evidence."
                } else currentExp.ifBlank { "Parking is currently restricted based on physical sign rules." }
            }
            ScanVerdict.ALLOWED -> {
                if (currentExp.contains("restricted", ignoreCase = true) || currentExp.contains("no parking", ignoreCase = true)) {
                    "Parking is allowed according to posted sign regulations."
                } else currentExp.ifBlank { "Parking is allowed at this location." }
            }
        }
    }
}
