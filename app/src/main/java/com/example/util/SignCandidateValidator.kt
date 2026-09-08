package com.example.util

import java.util.Locale

sealed class CandidateValidation {
    object Valid : CandidateValidation()
    data class Invalid(val reason: String) : CandidateValidation()

    val isValid: Boolean get() = this is Valid
}

object SignCandidateValidator {

    // Common garbage / machine-generated patterns
    private val URL_REGEX = Regex("""(?i)\b(https?://|www\.|ftp://|[a-z0-9-]+\.(com|org|net|io|gov|edu|co|app)\b/?)""")
    private val UUID_REGEX = Regex("""(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")
    private val HASH_TOKEN_REGEX = Regex("""(?i)\b[a-f0-9]{16,}\b|\b[a-zA-Z0-9_-]{24,}\b""")
    private val UNIX_TIMESTAMP_REGEX = Regex("""\b1[6-9]\d{11,}\b|:\d{10,}:?""")
    private val FILE_PATH_REGEX = Regex("""(?i)\b(IMG_\d+|DCIM|\.jpe?g|\.png|\.json|file://|crop_live_|crop_\d+)\b""")
    private val CODE_FRAGMENT_REGEX = Regex("""(?i)(\{"|"\s*:|function\s*\(|var\s+|let\s+|const\s+|</?[a-z]+>)""")

    // Parking domain vocabulary keywords
    private val PARKING_KEYWORDS = setOf(
        "PARK", "PARKING", "STOP", "STOPPING", "STAND", "STANDING",
        "TOW", "TOW-AWAY", "TOWAWAY", "METER", "PAY", "PERMIT", "RESIDENT",
        "ZONE", "CLEAN", "CLEANING", "SWEEP", "SWEEPING", "LOADING",
        "PASSENGER", "COMMERCIAL", "EXCEPT", "ONLY", "LIMIT", "RESERVED",
        "NO", "BUS", "TAXI", "DISABLED", "HANDICAPPED", "PLACARD", "VALET",
        "BLVD", "STREET", "AVE", "WAY", "DRIVE", "HWY", "CITY", "MUNICIPAL",
        "DEPT", "POLICE", "FINE", "VIOLATION", "CURB", "HOUR", "HR", "HRS",
        "MIN", "MINUTES", "SEC", "AM", "PM", "MON", "TUE", "WED", "THU",
        "FRI", "SAT", "SUN", "DAILY", "HOLIDAY", "HOLIDAYS", "EXCEPTED",
        "ENFORCED", "SCHEDULE", "TEMPORARY", "CONSTRUCTION", "TIME", "TIMES"
    )

    private val SCHEDULE_TIME_REGEX = Regex("""(?i)\b(\d{1,2}(:\d{2})?\s*(AM|PM|A\.M\.|P\.M\.)|\d+\s*(HR|HOUR|HRS|MIN|MINUTE)|\d{1,2}\s*-\s*\d{1,2})\b""")

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

        // 7. Check if string has any meaningful parking language or schedule patterns
        if (containsExplicitParkingRule(text)) {
            return CandidateValidation.Valid
        }

        // If string is a short sign symbol, hour designation, or permit/zone pattern, accept
        val isShortSignSymbol = text.uppercase(Locale.ROOT).trim().matches(
            Regex("""(?i)^(P|\d{1,2}\s*(H|HR|HRS|M|MIN|MINS)|[P]\s*\d{1,2}\s*(H|HR|HRS|M|MIN|MINS)?|\d{1,2}\s*-\s*\d{1,2}|ZONE\s*[A-Z0-9]+|PERMIT\s*[A-Z0-9]+|NO\s*PARKING|NO\s*STOPPING|TOW\s*AWAY)$""")
        )
        if (isShortSignSymbol) {
            return CandidateValidation.Valid
        }

        // If string has words, check if any word is a parking keyword
        val uppercaseWords = text.uppercase(Locale.ROOT)
            .split(Regex("""[^A-Z0-9]+"""))
            .filter { it.isNotBlank() }

        val hasKeyword = uppercaseWords.any { word -> PARKING_KEYWORDS.contains(word) }
        if (hasKeyword) {
            return CandidateValidation.Valid
        }

        // If string is short (e.g. "PARK", "STOP") or has valid parking words, accept
        if (uppercaseWords.size <= 2 && uppercaseWords.any { it.length >= 2 && ("PARK".contains(it) || "STOP".contains(it) || "TOW".contains(it) || "METER".contains(it) || it == "NO" || it == "HR" || it == "P") }) {
            return CandidateValidation.Valid
        }

        return CandidateValidation.Invalid("No recognizable parking sign vocabulary or schedule detected")
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

        return CandidateValidation.Valid
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

        if (words.any { PARKING_KEYWORDS.contains(it) }) return true
        if (SCHEDULE_TIME_REGEX.containsMatchIn(sanitized)) return true
        return false
    }

    /**
     * Cleans OCR text by stripping out any stray machine metadata or hash tokens,
     * while preserving legitimate parking schedule text.
     */
    fun sanitizeOcrText(rawText: String): String {
        var text = rawText
            .replace(URL_REGEX, "")
            .replace(UUID_REGEX, "")
            .replace(HASH_TOKEN_REGEX, "")
            .replace(UNIX_TIMESTAMP_REGEX, "")
            .replace(FILE_PATH_REGEX, "")
            .trim()

        text = text.replace(Regex("""\s+"""), " ")
            .trim(' ', ':', ';', ',', '-', '_')

        return if (text.isBlank() || !containsExplicitParkingRule(text)) {
            "Unclear Sign Rule"
        } else {
            text
        }
    }
}
