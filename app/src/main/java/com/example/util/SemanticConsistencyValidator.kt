package com.example.util

import com.example.data.detection.LocalSignCrop
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import java.util.Calendar
import java.util.Date
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
/**
 * P0 SCAN RELIABILITY FIX: Deterministic, LLM-independent extraction of objective
 * parking time evidence (maximum stay, clock cutoff, applicable days) from any text.
 *
 * Used to decide whether a Gemini ALLOWED result is objectively time-anchored
 * (real sign data) versus fragile prose. Weak/uncertain local OCR is SUPPORTING
 * evidence and must not veto a coherent Gemini interpretation that carries this
 * objective evidence.
 */
data class ObjectiveTimeEvidence(
    val maxStayMinutes: Int? = null,
    val cutoffTime: String? = null,
    val applicableDays: List<String> = emptyList()
)

object SemanticConsistencyValidator {

    private val DAY_KEYWORDS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "DAILY", "WEEKDAY", "WEEKEND")

    /**
     * Extracts objective time evidence deterministically:
     * - maximum stay (e.g. "2 HOUR PARKING" -> 120 minutes)
     * - cutoff time (e.g. "8AM-6PM" -> "6PM", last clock time in the text)
     * - applicable days (e.g. "EXCEPT SAT & SUN" -> [SAT, SUN])
     */
    fun extractObjectiveTimeEvidence(text: String): ObjectiveTimeEvidence {
        if (text.isBlank()) return ObjectiveTimeEvidence()
        val upper = text.uppercase(Locale.US)

        val maxStayMinutes = Regex("""(\d+)\s*(HOUR|HR|HRS|MIN|MINUTE|MINS)""").find(upper)?.let { match ->
            val value = match.groupValues[1].toIntOrNull() ?: return@let null
            val unit = match.groupValues[2]
            when {
                unit.startsWith("H") -> value * 60
                unit.startsWith("MIN") -> value
                unit == "MINS" -> value
                else -> null
            }
        }

        val cutoffTime = Regex("""\d{1,2}(?::\d{2})?\s*(?:AM|PM|A\.M\.|P\.M\.)""")
            .findAll(upper)
            .lastOrNull()
            ?.value
            ?.replace(".", "")
            ?.replace(Regex("""\s+"""), "")
            ?.trim()

        val applicableDays = DAY_KEYWORDS.filter { upper.contains(it) }

        return ObjectiveTimeEvidence(
            maxStayMinutes = maxStayMinutes,
            cutoffTime = cutoffTime,
            applicableDays = applicableDays
        )
    }

    /**
     * True when the given text contains objective, deterministic time evidence
     * (a parsed maximum stay or a clock cutoff). Pure prose without any time
     * anchor returns false.
     */
    fun hasObjectiveTimeEvidence(text: String): Boolean {
        val evidence = extractObjectiveTimeEvidence(text)
        return evidence.maxStayMinutes != null || evidence.cutoffTime != null
    }

    /**
     * Strongest objective anchor: a deterministic parsed MAXIMUM STAY
     * (e.g. "2 HOUR PARKING" -> 120 minutes). Used to authorize overriding the
     * uncertain-crop veto — a bare clock time in prose is not sufficient, because
     * it can be invented, while a posted duration is the core limit a timer needs.
     */
    fun hasMaxStayEvidence(text: String): Boolean {
        return extractObjectiveTimeEvidence(text).maxStayMinutes != null
    }

    private fun geminiObjectiveTimeText(rawResult: ScanResult): String {
        return buildString {
            append(rawResult.allowedUntilTime).append(" ")
            append(rawResult.timeRemaining).append(" ")
            rawResult.parkingRules.forEach { append(it).append(" ") }
            rawResult.detectedSigns.forEach {
                append(it.title).append(" ")
                append(it.subtitle).append(" ")
                append(it.applicableDaysHours).append(" ")
                append(it.exceptions).append(" ")
                append(it.restrictions).append(" ")
                append(it.ruleText).append(" ")
            }
        }
    }

    /**
     * Scan-level objective time evidence: deterministic time anchors across the
     * Gemini visual interpretation fields (rules, allowed-until, sign text).
     * Used when every local crop is uncertain and a coherent Gemini ALLOWED
     * result must be judged on objective evidence rather than crop quality.
     */
    private fun scanObjectiveTimeText(scanResult: ScanResult): String {
        return buildString {
            append(geminiObjectiveTimeText(scanResult))
            scanResult.detectedSigns.forEach {
                append(" ").append(it.title).append(" ").append(it.restrictions).append(" ").append(it.ruleText)
            }
        }
    }

    /** Scan-level overload of [hasMaxStayEvidence]. */
    fun hasMaxStayEvidence(scanResult: ScanResult): Boolean {
        return hasMaxStayEvidence(scanObjectiveTimeText(scanResult))
    }

    fun enforceSemanticConsistency(
        rawResult: ScanResult,
        validDetections: List<LocalSignCrop> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis()
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

        // Code-level guard: prevent RESTRICTED when objective timed-parking evidence + active schedule exists
        val hasTimedParkingEvidence = EXPLICIT_TIMED_PARKING_REGEX.containsMatchIn(combinedOcrText)
        val activeSchedule = if (signs.isNotEmpty()) {
            signs.any { sign ->
                val schedText = sign.applicableDaysHours.ifBlank { sign.subtitle }
                val signText = (sign.title + " " + sign.restrictions + " " + sign.rawText).uppercase(Locale.US)
                EXPLICIT_TIMED_PARKING_REGEX.containsMatchIn(signText) && isTimeWithinSchedule(schedText, currentTimeMillis)
            }
        } else {
            validDetections.any { crop ->
                val ocrUpper = crop.ocrText.uppercase(Locale.US)
                val matchDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "DAILY").filter { ocrUpper.contains(it) }
                val matchTimes = Regex("\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM)", RegexOption.IGNORE_CASE).findAll(crop.ocrText).map { it.value }.toList()
                val schedText = when {
                    matchDays.isNotEmpty() && matchTimes.isNotEmpty() -> "${matchDays.joinToString("-")} ${matchTimes.joinToString(" to ")}"
                    matchDays.isNotEmpty() -> matchDays.joinToString(", ")
                    matchTimes.isNotEmpty() -> matchTimes.joinToString(" - ")
                    else -> ""
                }
                EXPLICIT_TIMED_PARKING_REGEX.containsMatchIn(ocrUpper) && isTimeWithinSchedule(schedText, currentTimeMillis)
            }
        }
        val hasActiveHardRestriction = ocrHasRestricting || rulesHasRestricting || signs.any { sign ->
            sign.isRestrictingNow && isHardProhibition(sign)
        }

        if (rawResult.verdict == ScanVerdict.RESTRICTED && hasTimedParkingEvidence && activeSchedule && !hasActiveHardRestriction && !hasMultipleSignsWithConflict) {
            finalVerdict = ScanVerdict.ALLOWED
        }

        if (hasUncertainSign || hasMultipleSignsWithConflict) {
            // P0 SCAN RELIABILITY FIX: Weak/uncertain local OCR is SUPPORTING evidence,
            // not an automatic veto. A coherent Gemini ALLOWED result anchored by
            // OBJECTIVE evidence (deterministically parsed maximum stay) survives.
            // Genuine restrictions still downgrade to RESTRICTED, conflicting signs
            // remain AMBIGUOUS, and prose without a parsed max stay stays AMBIGUOUS.
            finalVerdict = when {
                rawResult.verdict == ScanVerdict.RESTRICTED && (ocrHasRestricting || rulesHasRestricting) ->
                    ScanVerdict.RESTRICTED
                (ocrHasRestricting || rulesHasRestricting) && !ocrHasPermission && !ocrHasExemption ->
                    ScanVerdict.RESTRICTED
                hasMultipleSignsWithConflict ->
                    ScanVerdict.AMBIGUOUS
                rawResult.verdict == ScanVerdict.ALLOWED && hasMaxStayEvidence(geminiObjectiveTimeText(rawResult)) ->
                    ScanVerdict.ALLOWED
                else -> ScanVerdict.AMBIGUOUS
            }
        } else if (rawResult.verdict == ScanVerdict.RESTRICTED && (ocrHasRestricting || rulesHasRestricting)) {
            // Respect RESTRICTED verdict if supported by restriction OCR or rules
            finalVerdict = ScanVerdict.RESTRICTED
        } else if (ocrHasRestricting && !ocrHasPermission) {
            // Physical sign is purely restricting (e.g. NO PARKING TOW AWAY) -> RESTRICTED
            finalVerdict = ScanVerdict.RESTRICTED
        } else if (rawResult.verdict == ScanVerdict.ALLOWED && ocrHasRestricting && !ocrHasExemption) {
            // Verdict claims ALLOWED but physical sign has active restriction -> RESTRICTED
            finalVerdict = ScanVerdict.RESTRICTED
        } else if (ocrHasPermission && !ocrHasRestricting && finalVerdict == ScanVerdict.ALLOWED) {
            // CRITICAL ISSUE 5 FIX: "PERMIT" alone is NOT permission — it requires
            // either a time-based permission pattern (e.g., "2 HOUR PARKING")
            // or explicit parking permission keywords (e.g., "PARKING PERMITTED").
            // "PERMIT PARKING ONLY" means permit holders only — not general permission.
            // "PERMIT REQUIRED" means you need a permit — not permission for everyone.
            if (ocrHasExplicitTimeBasedPermission(combinedOcrText)) {
                finalVerdict = ScanVerdict.ALLOWED
            } else if (ocrHasPermitOnlyOrRequired(combinedOcrText)) {
                // CRITICAL ISSUE 5: Permit-only signs mean unknown user eligibility.
                // Prefer AMBIGUOUS rather than claiming permission.
                finalVerdict = ScanVerdict.AMBIGUOUS
            }
            // Otherwise, leave verdict unchanged
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
                    explanation = normalizeExplanation(rawResult.explanation, ScanVerdict.AMBIGUOUS, hasSigns = sanitizedSigns.isNotEmpty()),
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

                val evidence = ParkingTimeEvidenceBuilder.buildParkingTimeEvidence(rawResult)
                val (allowedUntil, remaining) = when (evidence.type) {
                    ParkingTimeEvidenceType.BOTH -> {
                        val formattedCutoff = evidence.cutoffTimeString ?: "Verify physical signage"
                        Pair(formattedCutoff, "${evidence.durationMinutes?.let { ParkingTimeEvidenceBuilder.formatMinutesToDisplay(it) } ?: "2h 00m"} remaining")
                    }
                    ParkingTimeEvidenceType.POSTED_DURATION -> {
                        val mins = evidence.durationMinutes ?: 120
                        val durationStr = ParkingTimeEvidenceBuilder.formatMinutesToDisplay(mins)
                        Pair("$durationStr Parking", "$durationStr remaining")
                    }
                    ParkingTimeEvidenceType.CLOCK_CUTOFF -> {
                        val formattedCutoff = evidence.cutoffTimeString ?: "Verify physical signage"
                        Pair(formattedCutoff, "Until $formattedCutoff")
                    }
                    ParkingTimeEvidenceType.UNKNOWN -> {
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

    private fun isHardProhibition(sign: DetectedSign): Boolean {
        val text = "${sign.title} ${sign.subtitle} ${sign.restrictions} ${sign.ruleText} ${sign.rawText}".uppercase(Locale.US)
        return isRestrictingText(text)
    }

    /**
     * Authoritative decision for timer validity.
     */
    fun canAuthorizeTimer(scanResult: ScanResult?): Boolean {
        if (scanResult == null) return false
        if (scanResult.isDemo) return true
        if (scanResult.verdict != ScanVerdict.ALLOWED) return false

        val evidence = ParkingTimeEvidenceBuilder.buildParkingTimeEvidence(scanResult)
        if (evidence.type == ParkingTimeEvidenceType.UNKNOWN) {
            return false
        }

        if (scanResult.detectedSigns.any { it.isUncertain || (it.isRestrictingNow && isHardProhibition(it)) }) {
            // Hard prohibitions on restricting signs remain an absolute block.
            if (scanResult.detectedSigns.any { it.isRestrictingNow && isHardProhibition(it) }) {
                return false
            }
            // P0 SCAN RELIABILITY FIX: uncertain signs are supporting evidence, not a
            // veto — authorization is still granted when a deterministic MAXIMUM STAY
            // (objective evidence) anchors the coherent ALLOWED result.
            if (!hasMaxStayEvidence(scanResult)) {
                return false
            }
        }

        return scanResult.parkingRules.isNotEmpty() &&
                scanResult.parkingRules.none {
                    it.contains("No verified parking rule", ignoreCase = true) ||
                            it.contains("Assumed", ignoreCase = true) ||
                            it.contains("Derived", ignoreCase = true) ||
                            it.contains("No parking", ignoreCase = true)
                }
    }

    /**
     * CRITICAL ISSUE 5 FIX: Determines whether the OCR contains an explicit time-based
     * permission pattern (e.g., "2 HOUR PARKING", "PARKING ALLOWED 8AM-6PM").
     *
     * "PERMIT PARKING ONLY" is NOT time-based permission — it means permit holders only.
     * "PERMIT REQUIRED" is NOT permission — it means a permit is needed.
     * Only genuine time limits or explicit permission phrases count.
     */
    /**
     * CRITICAL ISSUE 5 FIX: Determines whether the OCR contains an explicit time-based
     * permission pattern (e.g., "2 HOUR PARKING", "PARKING ALLOWED 8AM-6PM").
     *
     * "PERMIT PARKING ONLY" is NOT time-based permission — it means permit holders only.
     * "PERMIT REQUIRED" is NOT permission — it means a permit is needed.
     * Only genuine time limits or explicit permission phrases count.
     */
    private fun ocrHasExplicitTimeBasedPermission(ocrUpper: String): Boolean {
        // Strong time-based permission signals
        val hasTimeLimit = Regex("""\d+\s*(?:HOUR|HR|HRS|MIN|MINUTE)""").containsMatchIn(ocrUpper)
        val hasParkingAllowed = Regex("""PARKING\s+(?:ALLOWED|PERMITTED)""").containsMatchIn(ocrUpper)
        val hasMeterOrPay = Regex("""(?:METER|PAY|PAYMENT|KIOSK)""").containsMatchIn(ocrUpper)
        val hasTimeRange = Regex("""\d{1,2}\s*(?:AM|PM|A\.M\.|P\.M\.)\s*(?:-|TO|THROUGH)\s*\d{1,2}\s*(?:AM|PM)""").containsMatchIn(ocrUpper)
        val hasScheduleDays = Regex("""(?:MON|TUE|WED|THU|FRI|SAT|SUN)""").containsMatchIn(ocrUpper)

        // If it's a permit-restricted sign, it's NOT time-based permission for the general public
        if (isPermitOnlyOrRequired(ocrUpper)) {
            return false
        }

        // Must have either a time limit, a time range, or explicit "PARKING ALLOWED/PERMITTED"
        return hasTimeLimit || hasParkingAllowed || (hasTimeRange && hasScheduleDays) || (hasMeterOrPay && hasTimeRange)
    }

    /**
     * CRITICAL ISSUE 5: Detects permit-only or permit-required text that must NOT
     * become ALLOWED for an unknown user.
     */
    private fun ocrHasPermitOnlyOrRequired(ocrUpper: String): Boolean {
        return isPermitOnlyOrRequired(ocrUpper)
    }

    private fun isPermitOnlyOrRequired(ocrUpper: String): Boolean {
        val isPermitOnly = Regex("""PERMIT\s+(?:PARKING\s+)?ONLY""").containsMatchIn(ocrUpper)
        val isPermitRequired = Regex("""PERMIT\s+REQUIRED""").containsMatchIn(ocrUpper)
        val isResidentOnly = Regex("""RESIDENT\s+(?:PERMIT\s+)?ONLY""").containsMatchIn(ocrUpper)
        return isPermitOnly || isPermitRequired || isResidentOnly
    }

    private fun extractAllowedUntilFromOcr(ocrText: String): Pair<String, String>? {
        if (ocrText.isBlank()) return null
        val upper = ocrText.uppercase(Locale.US)

        val hourMatch = Regex("""(\d+)\s*(?:HOUR|HR|HRS)""").find(upper)
        if (hourMatch != null) {
            val hours = hourMatch.groupValues.getOrNull(1)?.toIntOrNull() ?: 2
            return Pair("In $hours ${if (hours == 1) "hour" else "hours"}", "${hours}h 00m remaining")
        }
        val minMatch = Regex("""(\d+)\s*(?:MIN|MINUTE|MINS)""").find(upper)
        if (minMatch != null) {
            val mins = minMatch.groupValues.getOrNull(1)?.toIntOrNull() ?: 30
            return Pair("In $mins mins", "${mins}m remaining")
        }
        val pmMatch = Regex("""(\d{1,2}(?::\d{2})?\s*PM)""").find(upper)
        if (pmMatch != null) {
            val clock = pmMatch.groupValues.getOrNull(1) ?: ""
            return Pair(clock, "Until $clock")
        }
        return null
    }

    private val EXPLICIT_TIMED_PARKING_REGEX = Regex("""\d+\s*(?:MINUTE|MINUTES|HOUR|HOURS)\s+PARKING""")

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
            "PERMIT PARKING", "PAY AT METER", "METERED PARKING", "LIMIT",
            "PERMIT", "RESIDENT"
        )
        return permissionKeywords.any { upperText.contains(it) } || EXPLICIT_TIMED_PARKING_REGEX.containsMatchIn(upperText)
    }

    fun isTimeWithinSchedule(scheduleText: String, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (scheduleText.isBlank()) return true
        val lower = scheduleText.lowercase(Locale.US)
        
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val today = nowCal.get(Calendar.DAY_OF_WEEK)
        
        val containsDays = lower.contains("mon") || lower.contains("tue") || 
                           lower.contains("wed") || lower.contains("thu") || 
                           lower.contains("fri") || lower.contains("sat") || 
                           lower.contains("sun") || lower.contains("daily") ||
                           lower.contains("all days") || lower.contains("every day") ||
                           lower.contains("weekday") || lower.contains("weekend")
                           
        if (containsDays) {
            var isDayValid = false
            if (lower.contains("daily") || lower.contains("all days") || lower.contains("every day")) {
                isDayValid = true
            } else if (lower.contains("weekday")) {
                isDayValid = today in Calendar.MONDAY..Calendar.FRIDAY
            } else if (lower.contains("weekend")) {
                isDayValid = today == Calendar.SATURDAY || today == Calendar.SUNDAY
            } else {
                val monToFri = lower.contains("mon-fri") || lower.contains("mon to fri") || lower.contains("mon–fri")
                val monToSat = lower.contains("mon-sat") || lower.contains("mon to sat") || lower.contains("mon–sat")
                if (monToFri && today in Calendar.MONDAY..Calendar.FRIDAY) {
                    isDayValid = true
                } else if (monToSat && today in Calendar.MONDAY..Calendar.SATURDAY) {
                    isDayValid = true
                } else {
                    if (lower.contains("mon") && today == Calendar.MONDAY) isDayValid = true
                    if (lower.contains("tue") && today == Calendar.TUESDAY) isDayValid = true
                    if (lower.contains("wed") && today == Calendar.WEDNESDAY) isDayValid = true
                    if (lower.contains("thu") && today == Calendar.THURSDAY) isDayValid = true
                    if (lower.contains("fri") && today == Calendar.FRIDAY) isDayValid = true
                    if (lower.contains("sat") && today == Calendar.SATURDAY) isDayValid = true
                    if (lower.contains("sun") && today == Calendar.SUNDAY) isDayValid = true
                }
            }
            if (!isDayValid) return false
        }
        
        val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)\s*(?:-|to|–|—)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)""")
        val match = timeRegex.find(lower)
        if (match != null) {
            val startHr = match.groupValues[1].toInt()
            val startMin = match.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: 0
            val startAmPm = match.groupValues[3]
            val endHr = match.groupValues[4].toInt()
            val endMin = match.groupValues[5].ifBlank { "0" }.toIntOrNull() ?: 0
            val endAmPm = match.groupValues[6]
            
            var startHour = startHr
            if (startAmPm == "pm" && startHour < 12) startHour += 12
            if (startAmPm == "am" && startHour == 12) startHour = 0
            
            var endHour = endHr
            if (endAmPm == "pm" && endHour < 12) endHour += 12
            if (endAmPm == "am" && endHour == 12) endHour = 0
            
            val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
            val currentMin = nowCal.get(Calendar.MINUTE)
            
            val startTotalMin = startHour * 60 + startMin
            val endTotalMin = endHour * 60 + endMin
            val currentTotalMin = currentHour * 60 + currentMin
            
            return if (startTotalMin <= endTotalMin) {
                currentTotalMin in startTotalMin..endTotalMin
            } else {
                currentTotalMin >= startTotalMin || currentTotalMin <= endTotalMin
            }
        }
        
        return true
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

    fun isUsableClockTimeOrDuration(timeStr: String): Boolean {
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

    private fun normalizeExplanation(currentExp: String, verdict: ScanVerdict, hasSigns: Boolean = false): String {
        return when (verdict) {
            ScanVerdict.AMBIGUOUS -> {
                if (hasSigns) {
                    "Parking signage was detected, but the text or regulations could not be clearly verified. Please verify physical signage before parking."
                } else if (currentExp.contains("allowed", ignoreCase = true) || currentExp.contains("restricted", ignoreCase = true) || currentExp.isBlank()) {
                    "Sign evidence is unclear or ambiguous. Please verify physical signage before parking."
                } else {
                    currentExp
                }
            }
            ScanVerdict.RESTRICTED -> {
                if (currentExp.contains("allowed", ignoreCase = true) || currentExp.isBlank()) {
                    "Parking is restricted at this location based on posted sign evidence."
                } else {
                    currentExp
                }
            }
            ScanVerdict.ALLOWED -> {
                if (currentExp.contains("restricted", ignoreCase = true) || currentExp.contains("no parking", ignoreCase = true) || currentExp.isBlank()) {
                    "Parking is allowed according to posted sign regulations."
                } else {
                    currentExp
                }
            }
        }
    }
}
