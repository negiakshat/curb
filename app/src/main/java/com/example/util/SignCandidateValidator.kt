package com.example.util

import java.util.Locale

enum class OcrQuality {
    CLEAR,
    PARTIAL,
    WEAK
}

sealed class CandidateValidation {
    open class Valid(val quality: OcrQuality = OcrQuality.CLEAR) : CandidateValidation() {
        companion object : Valid(OcrQuality.CLEAR)
    }
    data class Invalid(val reason: String) : CandidateValidation()

    val isValid: Boolean get() = this is Valid
}

object SignCandidateValidator {

    // Common garbage / machine-generated patterns
    private val URL_REGEX = Regex("""(?i)\b(https?://|www\.|ftp://|[a-z0-9-]+\.(com|org|net|io|gov|edu|co|app)\b/?)""")
    private val UUID_REGEX = Regex("""(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")
    private val HASH_TOKEN_REGEX = Regex("""(?i)\b[a-f0-9]{16,}\b|\b[a-zA-Z0-9_-]{24,}\b""")
    private val UNIX_TIMESTAMP_REGEX = Regex("""\b1[6-9]\d{11,}\b|:\d{10,}:?""")
    private val FILE_PATH_REGEX = Regex("""(?i)\b(IMG_\d+|DCIM|\.jpe?g|\.png|\.json|file://|crop_live_|crop_\d+)""")
    private val CODE_FRAGMENT_REGEX = Regex("""(?i)(\{"|"\s*:|function\b|var\s+|let\s+|const\s+|</?[a-z]+>)""")

    // CRITICAL ISSUE 2: Generic civic & non-parking UI vocabulary that must NOT alone constitute parking-sign evidence.
    private val GENERIC_CIVIC_WORDS = setOf(
        "CITY", "STREET", "POLICE", "FINE", "CURB", "DEPT", "MUNICIPAL",
        "CITY HALL", "DEPARTMENT", "PUBLIC", "GOVERNMENT", "OFFICE",
        "BUREAU", "AGENCY", "SANITATION", "TRANSPORT", "TRANSIT",
        "INFRASTRUCTURE", "UTILITIES", "ADMIN", "DIVISION",
        "CHATGPT", "NEW CHAT", "LIBRARY", "PROJECTS", "SCHEDULED", "PLUGINS",
        "CODE", "PINNED", "DEVTOOLS", "HACKATHONS", "GROUP 2", "NO GROUP 2",
        "WELCOME", "GUIDE", "APP", "MAP"
    )

    // High confidence parking domain vocabulary keywords — words that genuinely indicate parking rules
    private val PARKING_KEYWORDS = setOf(
        "PARK", "PARKING", "STOP", "STOPPING", "STAND", "STANDING",
        "TOW", "TOW-AWAY", "TOWAWAY", "METER", "PAY", "PERMIT", "RESIDENT",
        "ZONE", "CLEAN", "CLEANING", "SWEEP", "SWEEPING", "LOADING",
        "PASSENGER", "COMMERCIAL", "LIMIT", "RESERVED",
        "NO", "BUS", "TAXI", "DISABLED", "HANDICAPPED", "PLACARD", "VALET",
        "HOUR", "HR", "HRS", "MIN", "MINUTES"
    )

    // CRITICAL ISSUE 2: Strong parking-specific patterns that genuinely indicate a parking rule.
    private val PARKING_RULE_PATTERNS = listOf(
        Regex("""(?i)\b\d+\s*(?:HOUR|HR|HRS|MIN|MINUTE|MINS)(?:\s+(?:PARKING|LIMIT|PERMITTED))?\b"""),
        Regex("""(?i)\b(?:NO\s*(?:PARK|PARKING|STOP|STOPPING|STAND|STANDING|ENTRY))\b"""),
        Regex("""(?i)\b(?:TOW|TOW-AWAY|TOWAWAY)(?:\s*ZONE|\s*AWAY)?\b"""),
        Regex("""(?i)\b(?:STREET\s*CLEAN(?:ING)?|STREET\s*SWEEP(?:ING)?|SWEEPING|CLEANING)\b"""),
        Regex("""(?i)\b(?:PERMIT\s+PARKING|PERMIT\s*(?:PARKING\s*)?(?:ONLY|REQUIRED|ZONE|AREA|HOLDERS)?)\b"""),
        Regex("""(?i)\b(?:METER|PAY|PAYMENT|COIN|KIOSK)(?:\s*PARKING|\s*STATION)?\b"""),
        Regex("""(?i)\b(?:LOADING|COMMERCIAL|PASSENGER)\s+(?:ONLY|ZONE)\b"""),
        Regex("""(?i)\b(?:DISABLED|HANDICAPPED)\s+(?:PARKING|ZONE|ONLY|PLACARD)\b"""),
        Regex("""(?i)\b\d{1,2}(?::\d{2})?\s*(?:AM|PM)\s*(?:TO|-|UNTIL|THROUGH)\s*\d{1,2}(?::\d{2})?\s*(?:AM|PM)\b"""),
        Regex("""(?i)\b(?:LIMIT|LIMITED)\s+\d+\s*(?:HOUR|HR|HRS|MIN)\b""")
    )

    private val SCHEDULE_TIME_REGEX = Regex("""(?i)\b(\d{1,2}(:\d{2})?\s*(AM|PM|A\.M\.|P\.M\.)\s*(TO|-|UNTIL)\s*\d{1,2}(:\d{2})?\s*(AM|PM|A\.M\.|P\.M\.)|\d+\s*(HR|HOUR|HRS|MIN|MINUTE))\b""")

    fun validateOcr(rawText: String): CandidateValidation {
        if (rawText.isBlank()) {
            return CandidateValidation.Invalid("Empty text")
        }

        val text = rawText.trim()

        // 1. Check for explicit code, file paths, or JSON
        if (CODE_FRAGMENT_REGEX.containsMatchIn(text)) {
            return CandidateValidation.Invalid("Contains machine code, JSON, or system markup")
        }

        if (FILE_PATH_REGEX.containsMatchIn(text)) {
            return CandidateValidation.Invalid("Contains filename or image metadata path")
        }

        // 2. Check for URLs or tracking parameter strings
        val textWithoutUrls = text.replace(URL_REGEX, "").trim()
        if (URL_REGEX.containsMatchIn(text)) {
            val textWithoutUrlPath = textWithoutUrls.replace(Regex("""(?i)^[a-z0-9_/?:&=-]+"""), "").trim()
            if (textWithoutUrlPath.isBlank() || !containsExplicitParkingRule(textWithoutUrlPath)) {
                return CandidateValidation.Invalid("Contains web URL or tracking address without parking rule")
            }
        }

        // 3. Check for UUIDs
        if (UUID_REGEX.containsMatchIn(text)) {
            return CandidateValidation.Invalid("Contains UUID string")
        }

        // 4. Check for long continuous hashes or high-entropy tokens
        val textWithoutHashes = textWithoutUrls.replace(HASH_TOKEN_REGEX, "").trim()
        if (HASH_TOKEN_REGEX.containsMatchIn(text) && !containsExplicitParkingRule(textWithoutHashes)) {
            return CandidateValidation.Invalid("Contains machine hash or tracking token")
        }

        // 5. Check for raw Unix timestamps
        if (UNIX_TIMESTAMP_REGEX.containsMatchIn(text)) {
            return CandidateValidation.Invalid("Contains raw Unix timestamp metadata")
        }

        // 6. High non-alphanumeric / symbol ratio check
        val alphaNumCount = text.count { it.isLetterOrDigit() || it.isWhitespace() }
        val symbolRatio = 1.0f - (alphaNumCount.toFloat() / text.length.coerceAtLeast(1))
        if (symbolRatio > 0.45f && !containsExplicitParkingRule(text)) {
            return CandidateValidation.Invalid("Corrupted OCR with excessive symbols or special characters")
        }

        // 7. Check if string has strong parking-rule patterns
        if (hasStrongParkingRulePattern(text)) {
            return CandidateValidation.Valid(OcrQuality.CLEAR)
        }

        // If string is a short sign symbol or hour designation, accept
        val upperText = text.uppercase(Locale.ROOT).trim()
        val isShortSignSymbol = upperText.matches(
            Regex("""(?i)^(P|\d{1,2}\s*(H|HR|HRS|M|MIN|MINS)|[P]\s*\d{1,2}\s*(H|HR|HRS|M|MIN|MINS)?|\d{1,2}\s*-\s*\d{1,2}|ZONE\s*[A-Z0-9]+|PERMIT\s*[A-Z0-9]+|NO\s*PARKING|NO\s*STOPPING|TOW\s*AWAY)$""")
        )
        if (isShortSignSymbol) {
            return CandidateValidation.Valid(OcrQuality.CLEAR)
        }

        val uppercaseWords = upperText
            .split(Regex("""[^A-Z0-9]+"""))
            .filter { it.isNotBlank() }

        // Check for strong combinations
        val hasStrongParkingCombination = hasStrongParkingCombination(uppercaseWords, upperText)
        if (hasStrongParkingCombination) {
            return CandidateValidation.Valid(OcrQuality.CLEAR)
        }

        // Reject generic civic or non-parking text
        val parkingWords = uppercaseWords.filter { PARKING_KEYWORDS.contains(it) }
        val hasGenericCivicOnly = uppercaseWords.all { GENERIC_CIVIC_WORDS.contains(it) } || parkingWords.all { GENERIC_CIVIC_WORDS.contains(it) }

        if (hasGenericCivicOnly || parkingWords.isEmpty()) {
            return CandidateValidation.Invalid("No parking-domain evidence or generic civic text without parking rule")
        }

        // If string has legitimate parking keywords (e.g. "NO PARKING", "PARK"), accept as PARTIAL/WEAK
        if (parkingWords.isNotEmpty()) {
            val hasExplicitParkingAnchor = uppercaseWords.any { it == "PARK" || it == "PARKING" || it == "STOP" || it == "STOPPING" || it == "TOW" || it == "METER" || it == "PERMIT" || it == "LOADING" || it == "SWEEPING" }
            if (hasExplicitParkingAnchor) {
                return CandidateValidation.Valid(OcrQuality.PARTIAL)
            }
        }

        return CandidateValidation.Invalid("No recognizable parking sign vocabulary or schedule detected")
    }

    /**
     * CRITICAL ISSUE 2: Checks whether the text contains a strong parking-rule pattern
     * that genuinely indicates a parking regulation (not just a civic label).
     */
    private fun hasStrongParkingRulePattern(text: String): Boolean {
        val upper = text.uppercase(Locale.US)
        return PARKING_RULE_PATTERNS.any { it.containsMatchIn(upper) }
    }

    /**
     * CRITICAL ISSUE 2: Checks for a strong combination of parking-specific words
     * that together clearly indicate a parking rule (not just a single generic word).
     *
     * Examples of strong combinations:
     * - "NO PARKING" (NO + PARKING)
     * - "PERMIT ONLY" (PERMIT + ONLY)
     * - "2 HOUR" (digit + HOUR)
     * - "TOW AWAY" (TOW + AWAY)
     * - "STREET CLEANING" (but not just "STREET" alone)
     */
    private fun hasStrongParkingCombination(words: List<String>, fullText: String): Boolean {
        // Strong two-word combinations
        val strongPairs = listOf(
            setOf("NO", "PARKING"), setOf("NO", "PARK"), setOf("NO", "STOPPING"),
            setOf("NO", "STOP"), setOf("NO", "STANDING"),
            setOf("TOW", "AWAY"), setOf("TOW", "AWAY"), setOf("TOW-AWAY", "ZONE"),
            setOf("PERMIT", "ONLY"), setOf("PERMIT", "REQUIRED"), setOf("PERMIT", "ZONE"),
            setOf("PERMIT", "AREA"), setOf("PERMIT", "HOLDERS"),
            setOf("STREET", "CLEANING"), setOf("STREET", "SWEEPING"),
            setOf("COMMERCIAL", "LOADING"), setOf("COMMERCIAL", "ZONE"),
            setOf("LOADING", "ZONE"), setOf("LOADING", "ONLY"),
            setOf("PASSENGER", "LOADING"), setOf("PASSENGER", "ONLY"),
            setOf("RESIDENT", "PERMIT"), setOf("RESIDENT", "ZONE"),
            setOf("BUS", "STOP"), setOf("TAXI", "STAND"),
            setOf("DISABLED", "PARKING"), setOf("HANDICAPPED", "ZONE"),
            setOf("TIME", "LIMIT"), setOf("PAY", "METER"),
            setOf("METER", "PARKING"), setOf("CLEANING", "ZONE"),
            setOf("SWEEPING", "ZONE"), setOf("SWEEPING", "DAY"),
            setOf("HOLIDAY", "EXCEPTION"), setOf("HOLIDAY", "EXCEPTED")
        )

        val wordSet = words.toSet()
        if (strongPairs.any { pair -> pair.all { wordSet.contains(it) } }) {
            return true
        }

        // CRITICAL ISSUE 2: "PERMIT" alone with a zone/area designation like "AREA G"
        // is a valid parking rule indicator, but "PERMIT" by itself is not sufficient.
        // "RESIDENT PERMIT ONLY" should work; "PERMIT" alone should not.
        val hasPermitWithZone = words.contains("PERMIT") &&
                (words.any { it.startsWith("AREA") || it.startsWith("ZONE") } ||
                        fullText.contains(Regex("""(?i)PERMIT\s+(?:ONLY|REQUIRED|ZONE|AREA)""")))

        if (hasPermitWithZone) return true

        // Check for "CITY HALL" — this is NOT a parking rule
        if (words == listOf("CITY", "HALL") || words == listOf("CITY")) return false
        if (words == listOf("POLICE")) return false
        if (words == listOf("DEPT")) return false
        if (words == listOf("MUNICIPAL")) return false
        if (words == listOf("FINE")) return false

        return false
    }

    fun isDemoOrSampleCrop(fileUri: String? = null, isDemo: Boolean = false, candidateId: String = ""): Boolean {
        if (isDemo) return true
        if (candidateId.startsWith("preset_") || candidateId.startsWith("sample_")) return true
        if (fileUri != null && (fileUri.contains("sample_sign_plates") || fileUri.contains("preset_") || fileUri.contains("sample_"))) return true
        return false
    }

    /**
     * Validates physical sign candidate geometry, relative canvas area, aspect ratio,
     * and OCR text content before confirming a candidate for sign cropping.
     *
     * OCR text alone is ONLY a candidate signal, NOT proof of a physical sign.
     */
    fun validatePhysicalCandidateGeometry(
        rectLeft: Int,
        rectTop: Int,
        rectRight: Int,
        rectBottom: Int,
        imageWidth: Int,
        imageHeight: Int,
        ocrText: String,
        isDemo: Boolean = false,
        fileUri: String? = null,
        candidateId: String = ""
    ): CandidateValidation {
        if (isDemoOrSampleCrop(fileUri, isDemo, candidateId)) {
            return CandidateValidation.Invalid("Sample or demo preset crop cannot be accepted as real physical candidate")
        }

        if (imageWidth <= 0 || imageHeight <= 0) {
            return CandidateValidation.Invalid("Invalid image dimensions ($imageWidth x $imageHeight)")
        }

        val width = rectRight - rectLeft
        val height = rectBottom - rectTop

        if (width <= 0 || height <= 0) {
            return CandidateValidation.Invalid("Invalid candidate box dimensions (${width}x${height})")
        }

        // 1. OCR text validity check (reject code, URLs, hashes, UUIDs, non-parking garbage)
        val ocrValidation = validateOcr(ocrText)
        if (!ocrValidation.isValid) {
            return ocrValidation
        }

        // 2. Minimum absolute candidate size (preserve small distant signs down to 28x20px if OCR is valid)
        if (width < 28 || height < 20) {
            return CandidateValidation.Invalid("Candidate dimensions too small (${width}x${height}px)")
        }

        // 3. Minimum candidate area relative to image (preserve small distant signs down to 0.0008f)
        val imageArea = imageWidth.toFloat() * imageHeight.toFloat()
        val candidateArea = width.toFloat() * height.toFloat()
        val areaFraction = candidateArea / imageArea

        if (areaFraction < 0.0008f) {
            return CandidateValidation.Invalid("Candidate relative area too small (${String.format(Locale.US, "%.4f", areaFraction)} < 0.0008)")
        }

        // 4. Maximum area check (Do not use entire input bitmap as a sign crop)
        if (areaFraction > 0.95f) {
            return CandidateValidation.Invalid("Candidate covers entire image without distinct sign boundary (${String.format(Locale.US, "%.2f", areaFraction)} > 0.95)")
        }

        // 5. Plausible rectangular aspect ratio check
        val aspect = width.toFloat() / height.toFloat()
        if (aspect < 0.12f || aspect > 4.5f) {
            return CandidateValidation.Invalid("Implausible sign aspect ratio (${String.format(Locale.US, "%.2f", aspect)})")
        }

        val quality = (ocrValidation as? CandidateValidation.Valid)?.quality ?: OcrQuality.CLEAR
        return CandidateValidation.Valid(quality)
    }

    fun containsExplicitParkingRule(text: String): Boolean {
        val sanitized = text
            .replace(URL_REGEX, "")
            .replace(UUID_REGEX, "")
            .replace(HASH_TOKEN_REGEX, "")
            .replace(FILE_PATH_REGEX, "")
            .trim()
        if (sanitized.isBlank()) return false

        val words = sanitized.uppercase(Locale.ROOT)
            .split(Regex("""[^A-Z0-9]+"""))
            .filter { it.isNotBlank() }

        if (hasStrongParkingRulePattern(sanitized)) return true
        if (words.any { PARKING_KEYWORDS.contains(it) }) return true
        if (SCHEDULE_TIME_REGEX.containsMatchIn(sanitized)) return true
        return false
    }

    /**
     * Cleans OCR text by stripping out any stray machine metadata, web URLs,
     * encoded parameter strings, hashes, or UUID tokens, while preserving
     * legitimate parking schedule text and titles.
     */
    private val URL_ENCODED_REGEX = Regex("""(?i)(https?%3A%2F%2F|%2F|%3A|%3F|%3D|%26|%20)""")
    private val URL_PARAM_GARBAGE_REGEX = Regex("""(?i)(&?imgurl=[^&\s]*|&?url=[^&\s]*|\?imgurl=[^&\s]*|\?url=[^&\s]*|&?utm_[a-z]+=[^&\s]*)""")
    private val PURE_URL_REGEX = Regex("""(?i)(https?://[^\s]+|www\.[^\s]+|ftp://[^\s]+|file://[^\s]+|[a-z0-9-]+\.(com|org|net|io|gov|edu|co|app|site|xyz|info|bear)\b[^\s]*)""")
    private val UUID_REGEX_STRICT = Regex("""(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b""")
    private val LONG_MACHINE_TOKEN_REGEX = Regex("""(?i)\b[a-f0-9]{16,}\b|\b[a-zA-Z0-9_-]{20,}\b""")
    private val UNIX_TIMESTAMP_STRICT_REGEX = Regex("""\b1[6-9]\d{11,}\b|:\d{10,}:?""")
    private val FILE_PATH_STRICT_REGEX = Regex("""(?i)\b(IMG_\d+|DCIM|\.jpe?g|\.png|\.webp|\.json|file://|crop_live_[^\s]*|crop_\d+[^\s]*|preset_[^\s]*|sample_[^\s]*)\b""")
    private val CODE_OR_JSON_REGEX = Regex("""(?i)(\{.*?\}|\[.*?\]|"[a-zA-Z0-9_-]+"\s*:|<[^>]+>)""")

    fun sanitizeText(
        rawText: String?,
        fallbackIfEmpty: String = "Sign text could not be confidently read."
    ): String {
        if (rawText.isNullOrBlank()) return fallbackIfEmpty

        var cleaned = rawText
        // 1. Remove JSON / code fragments
        cleaned = CODE_OR_JSON_REGEX.replace(cleaned, " ")

        // 2. Remove URL parameters & encoded URL strings (e.g. &imgurl=https%3A%2F%2F...)
        cleaned = URL_PARAM_GARBAGE_REGEX.replace(cleaned, " ")
        cleaned = URL_ENCODED_REGEX.replace(cleaned, " ")

        // 3. Remove web URLs, domains, CDN addresses
        cleaned = PURE_URL_REGEX.replace(cleaned, " ")
        cleaned = URL_REGEX.replace(cleaned, " ")

        // 4. Remove UUIDs & filenames / file path metadata
        cleaned = UUID_REGEX_STRICT.replace(cleaned, " ")
        cleaned = FILE_PATH_STRICT_REGEX.replace(cleaned, " ")
        cleaned = FILE_PATH_REGEX.replace(cleaned, " ")

        // 5. Remove long machine tokens / hashes
        cleaned = LONG_MACHINE_TOKEN_REGEX.replace(cleaned, " ")
        cleaned = HASH_TOKEN_REGEX.replace(cleaned, " ")
        cleaned = UNIX_TIMESTAMP_STRICT_REGEX.replace(cleaned, " ")

        // 6. Condense whitespace and strip orphan symbols
        cleaned = cleaned
            .replace(Regex("""[&?=#%_<>{}\[\]"\\]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ':', ';', ',', '-', '.', '_')

        if (cleaned.isBlank()) return fallbackIfEmpty

        // 7. Check if remaining string has any recognizable parking vocabulary or schedule
        val hasDigitsOrLetters = cleaned.any { it.isLetterOrDigit() }
        if (!hasDigitsOrLetters) return fallbackIfEmpty

        if (containsExplicitParkingRule(cleaned) || validateOcr(cleaned).isValid) {
            return cleaned
        }

        val words = cleaned.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.all { w -> w.all { c -> c.isLetterOrDigit() || c == '-' || c == '/' || c == '&' } && w.length < 20 }) {
            return cleaned
        }

        return fallbackIfEmpty
    }

    /**
     * Cleans OCR text by stripping out any stray machine metadata or hash tokens,
     * while preserving legitimate parking schedule text.
     */
    fun sanitizeOcrText(rawText: String): String {
        return sanitizeText(rawText, "Unreadable sign text")
    }
}
