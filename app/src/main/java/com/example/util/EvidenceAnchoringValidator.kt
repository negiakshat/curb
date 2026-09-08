package com.example.util

import com.example.data.detection.LocalSignCrop
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import java.util.Locale

/**
 * Production validator for AI Evidence Anchoring (P1-02).
 *
 * Invariants:
 * 1. NO VALIDATED SIGN EVIDENCE -> NO VERIFIED PARKING RULE.
 * 2. EVERY VERIFIED PARKING RULE MUST BE TRACEABLE TO VALIDATED PHYSICAL SIGN EVIDENCE.
 * 3. Exactly 1 DetectedSign per validated LocalSignCrop.
 * 4. Fields unsupported by OCR/crop evidence (schedules, payment, limits, rules) are neutralized or reset.
 */
object EvidenceAnchoringValidator {

    /**
     * Anchors and validates a ScanResult against the list of validated LocalSignCrop evidence.
     */
    fun sanitizeAndAnchorResult(
        rawScanResult: ScanResult,
        validDetections: List<LocalSignCrop>
    ): ScanResult {
        // Demo scans follow their isolated demo preset path
        if (rawScanResult.isDemo) {
            return rawScanResult
        }

        // Rule 12 & 15: Without valid physical sign evidence, result must remain AMBIGUOUS
        if (!ParkingAuthority.hasVerifiedSignEvidence(validDetections)) {
            return ParkingAuthority.sanitizeAndEnforceAuthority(rawScanResult, validDetections, isLiveScanPipeline = true)
        }

        val combinedOcrText = validDetections.joinToString(" ") { it.ocrText }.uppercase(Locale.US)

        // Rule 3, 4, 5: Exactly 1 DetectedSign per validated LocalSignCrop
        val anchoredSigns = mutableListOf<DetectedSign>()
        validDetections.forEachIndexed { idx, crop ->
            // Match candidate sign from Gemini JSON output by crop.id or by index idx
            val candidateSign = rawScanResult.detectedSigns.firstOrNull { it.id == crop.id }
                ?: rawScanResult.detectedSigns.getOrNull(idx)

            val validatedSign = anchorSignToCrop(candidateSign, crop)
            anchoredSigns.add(validatedSign)
        }

        // Check if sign OCR contains explicit restriction text without permission text
        val hasRestrictionOcr = isRestrictingText(combinedOcrText)
        val hasPermissionOcr = isPermissionText(combinedOcrText)

        var sanitizedVerdict = rawScanResult.verdict
        if (sanitizedVerdict == ScanVerdict.ALLOWED && hasRestrictionOcr && !hasPermissionOcr) {
            sanitizedVerdict = ScanVerdict.RESTRICTED
        }

        // Rule 10: paymentInfo must only be populated if validated sign evidence explicitly supports payment/meter requirements
        val hasPaymentEvidence = hasPaymentEvidence(combinedOcrText)
        val sanitizedPaymentInfo = if (hasPaymentEvidence) {
            rawScanResult.paymentInfo.ifBlank { "Pay at meter or station" }
        } else {
            "" // Reset to empty if unsupported by sign evidence
        }

        // Rule 8 & 9: allowedUntilTime & timeRemaining must only be populated when evidence establishes a usable time rule
        val hasTimeRule = hasTimeRuleEvidence(combinedOcrText)
        var sanitizedAllowedUntil = rawScanResult.allowedUntilTime
        var sanitizedTimeRemaining = rawScanResult.timeRemaining

        if (sanitizedVerdict == ScanVerdict.RESTRICTED) {
            sanitizedAllowedUntil = "No parking permitted"
            sanitizedTimeRemaining = "--"
        } else if (!hasTimeRule && sanitizedAllowedUntil != "Verify physical signage") {
            sanitizedAllowedUntil = "Verify physical signage"
            sanitizedTimeRemaining = "--"
        }

        // Rule 11: parkingRules must be derived from validated sign evidence
        val anchoredRules = filterRulesToEvidence(rawScanResult.parkingRules, validDetections, combinedOcrText)

        // Sanitize zoneType
        val sanitizedZoneType = when {
            hasPaymentEvidence -> "Metered parking zone"
            sanitizedVerdict == ScanVerdict.RESTRICTED -> "Restricted zone"
            else -> "Parking zone"
        }

        val anchoredResult = rawScanResult.copy(
            verdict = sanitizedVerdict,
            detectedSigns = anchoredSigns,
            paymentInfo = sanitizedPaymentInfo,
            allowedUntilTime = sanitizedAllowedUntil,
            timeRemaining = sanitizedTimeRemaining,
            parkingRules = anchoredRules,
            zoneType = sanitizedZoneType,
            vehicleApplicability = filterVehicleApplicability(rawScanResult.vehicleApplicability, combinedOcrText)
        )

        // Pass anchored result through SemanticConsistencyValidator
        val semanticallyConsistentResult = SemanticConsistencyValidator.enforceSemanticConsistency(anchoredResult, validDetections)

        // Run through ParkingAuthority for final location vs sign authority check
        return ParkingAuthority.sanitizeAndEnforceAuthority(semanticallyConsistentResult, validDetections, isLiveScanPipeline = true)
    }

    private fun anchorSignToCrop(candidateSign: DetectedSign?, crop: LocalSignCrop): DetectedSign {
        val cropOcr = crop.ocrText
        val cropOcrUpper = cropOcr.uppercase(Locale.US)
        val defaultTitle = crop.normalizedBox.label.ifBlank { "Sign #${crop.id}" }

        if (candidateSign == null) {
            return DetectedSign(
                id = crop.id,
                title = defaultTitle,
                subtitle = extractScheduleFromOcr(cropOcr),
                applicableDaysHours = extractScheduleFromOcr(cropOcr),
                restrictions = cropOcr.ifBlank { "Unspecified rule" },
                exceptions = "",
                ruleText = SignCandidateValidator.sanitizeOcrText(cropOcr),
                isRestrictingNow = isRestrictingText(cropOcrUpper),
                isUncertain = false,
                statusBadge = if (isRestrictingText(cropOcrUpper)) "Active Restriction" else "Posted Sign",
                rawText = cropOcr,
                croppedImageUri = crop.fileUri,
                confidence = crop.normalizedBox.confidence
            )
        }

        // Validate candidate fields against crop OCR evidence
        val candidateSchedule = candidateSign.applicableDaysHours.ifBlank { candidateSign.subtitle }
        val validatedSchedule = if (isScheduleSupportedByEvidence(candidateSchedule, cropOcrUpper)) {
            candidateSchedule
        } else {
            "" // Reset unsupported schedule to empty
        }

        val candidateExceptions = candidateSign.exceptions
        val validatedExceptions = if (isExceptionsSupportedByEvidence(candidateExceptions, cropOcrUpper)) {
            candidateExceptions
        } else {
            "" // Reset unsupported exceptions to empty
        }

        val candidateRestrictions = candidateSign.restrictions.ifBlank { candidateSign.ruleText }
        val validatedRestrictions = if (isRestrictionSupportedByEvidence(candidateRestrictions, cropOcrUpper)) {
            candidateRestrictions
        } else {
            SignCandidateValidator.sanitizeOcrText(cropOcr)
        }

        return candidateSign.copy(
            id = crop.id, // Strictly tie to crop ID
            croppedImageUri = crop.fileUri, // Strictly tie to crop URI
            rawText = cropOcr, // Strictly tie to crop OCR
            confidence = crop.normalizedBox.confidence,
            title = candidateSign.title.ifBlank { defaultTitle },
            subtitle = validatedSchedule,
            applicableDaysHours = validatedSchedule,
            restrictions = validatedRestrictions,
            ruleText = validatedRestrictions,
            exceptions = validatedExceptions
        )
    }

    fun hasPaymentEvidence(combinedOcrText: String): Boolean {
        val upper = combinedOcrText.uppercase(Locale.US)
        val paymentKeywords = listOf("METER", "PAY", "RATE", "COIN", "FEE", "PARKMOBILE", "KIOSK", "APP", "MUNICIPAL PAY", "COINS")
        return paymentKeywords.any { upper.contains(it) }
    }

    fun hasTimeRuleEvidence(combinedOcrText: String): Boolean {
        val upper = combinedOcrText.uppercase(Locale.US)
        val timeKeywords = listOf(
            "HOUR", "HR", "MIN", "MINUTE", "AM", "PM", "LIMIT",
            "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "DAILY", "SCHEDULE"
        )
        val hasDigits = upper.any { it.isDigit() }
        return timeKeywords.any { upper.contains(it) } || (hasDigits && (upper.contains("PARK") || upper.contains("CLEAN") || upper.contains("SWEEP") || upper.contains("TOW")))
    }

    fun isScheduleSupportedByEvidence(schedule: String, cropOcrUpper: String): Boolean {
        if (schedule.isBlank()) return true
        val upperSchedule = schedule.uppercase(Locale.US)
        val dayKeywords = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "DAILY", "WEEKDAY", "WEEKEND")
        val hasScheduleDaysInCandidate = dayKeywords.any { upperSchedule.contains(it) }
        val hasDigitsInCandidate = upperSchedule.any { it.isDigit() }

        if (!hasScheduleDaysInCandidate && !hasDigitsInCandidate) return true

        if (hasScheduleDaysInCandidate) {
            val hasDaysInCrop = dayKeywords.any { cropOcrUpper.contains(it) }
            if (!hasDaysInCrop) return false
        }

        if (hasDigitsInCandidate) {
            val hasDigitsInCrop = cropOcrUpper.any { it.isDigit() } || cropOcrUpper.contains("AM") || cropOcrUpper.contains("PM")
            if (!hasDigitsInCrop) return false
        }

        return true
    }

    fun isExceptionsSupportedByEvidence(exceptions: String, cropOcrUpper: String): Boolean {
        if (exceptions.isBlank()) return true
        val upperExceptions = exceptions.uppercase(Locale.US)
        val exceptionKeywords = listOf("EXCEPT", "EXEMPT", "PERMIT", "HOLIDAY", "SUNDAY", "AREA")
        val candidateHasKeywords = exceptionKeywords.any { upperExceptions.contains(it) }
        if (!candidateHasKeywords) return true

        return exceptionKeywords.any { cropOcrUpper.contains(it) }
    }

    fun isRestrictionSupportedByEvidence(restriction: String, cropOcrUpper: String): Boolean {
        if (restriction.isBlank()) return true
        val upper = restriction.uppercase(Locale.US)
        if (upper.contains("TOW") && !cropOcrUpper.contains("TOW") && !cropOcrUpper.contains("NO STOP")) return false
        if ((upper.contains("CLEAN") || upper.contains("SWEEP")) && !cropOcrUpper.contains("CLEAN") && !cropOcrUpper.contains("SWEEP")) return false
        if (upper.contains("PERMIT") && !cropOcrUpper.contains("PERMIT") && !cropOcrUpper.contains("AREA")) return false
        return true
    }

    private fun isRestrictingText(ocrUpper: String): Boolean {
        return ocrUpper.contains("TOW") || ocrUpper.contains("CLEAN") || ocrUpper.contains("SWEEP") ||
                ocrUpper.contains("NO PARK") || ocrUpper.contains("NO STOP") || ocrUpper.contains("CONSTRUCTION")
    }

    private fun isPermissionText(ocrUpper: String): Boolean {
        return ocrUpper.contains("HOUR") || ocrUpper.contains("HR") || ocrUpper.contains("MIN") ||
                ocrUpper.contains("PARK") && !ocrUpper.contains("NO PARK") || ocrUpper.contains("PERMIT")
    }

    private fun extractScheduleFromOcr(ocrText: String): String {
        val upper = ocrText.uppercase(Locale.US)
        val matchDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "DAILY").filter { upper.contains(it) }
        val matchTimes = Regex("\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM)", RegexOption.IGNORE_CASE).findAll(ocrText).map { it.value }.toList()

        return when {
            matchDays.isNotEmpty() && matchTimes.isNotEmpty() -> "${matchDays.joinToString("-")} ${matchTimes.joinToString(" to ")}"
            matchDays.isNotEmpty() -> matchDays.joinToString(", ")
            matchTimes.isNotEmpty() -> matchTimes.joinToString(" - ")
            else -> ""
        }
    }

    private fun filterRulesToEvidence(
        rules: List<String>,
        validDetections: List<LocalSignCrop>,
        combinedOcrText: String
    ): List<String> {
        val anchoredRules = rules.filter { rule ->
            if (rule.contains("No verified parking rule", ignoreCase = true)) return@filter true
            val ruleUpper = rule.uppercase(Locale.US)
            val keywords = ruleUpper.split(Regex("\\W+")).filter { it.length > 3 }
            if (keywords.isEmpty()) return@filter true

            keywords.any { kw -> combinedOcrText.contains(kw) }
        }

        if (anchoredRules.isNotEmpty()) return anchoredRules

        return validDetections.mapIndexed { idx, crop ->
            "${crop.normalizedBox.label.ifBlank { "Sign #${idx + 1}" }}: ${SignCandidateValidator.sanitizeOcrText(crop.ocrText)}"
        }
    }

    private fun filterVehicleApplicability(applicability: String, combinedOcrText: String): String {
        if (applicability.isBlank()) return ""
        val upperApp = applicability.uppercase(Locale.US)
        val vehicleTypes = listOf("COMMERCIAL", "PASSENGER", "TRUCK", "BUS", "TAXI", "MOTORCYCLE")
        val hasVehicleKeyword = vehicleTypes.any { upperApp.contains(it) }
        if (!hasVehicleKeyword) return ""

        return if (vehicleTypes.any { combinedOcrText.contains(it) }) applicability else ""
    }
}
