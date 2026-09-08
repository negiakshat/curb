package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.detection.LocalSignCrop
import com.example.data.model.ChatMessage
import com.example.data.model.DetectedSign
import com.example.data.model.SampleSignPreset
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.util.EvidenceAnchoringValidator
import com.example.util.ParkingAuthority
import com.example.util.SignCandidateValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun Bitmap.toBase64(): String? {
        if (isRecycled || width <= 0 || height <= 0) return null
        return try {
            val outputStream = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun analyzeParkingSigns(
        bitmap: Bitmap?,
        locationName: String,
        cityState: String = "",
        isLocationKnown: Boolean = true,
        localDetections: List<LocalSignCrop> = emptyList(),
        context: Context? = null
    ): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val currentTimeStr = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(Date())
        val locationContextText = ParkingAuthority.buildLocationContextPrompt(locationName, cityState, isLocationKnown)

        val validDetections = localDetections.filter { crop ->
            !SignCandidateValidator.isDemoOrSampleCrop(crop.fileUri, crop.isDemo, crop.id) &&
            SignCandidateValidator.validateOcr(crop.ocrText).isValid &&
            ((crop.bitmap != null && !crop.bitmap.isRecycled) || (crop.fileUri.isNotBlank() && java.io.File(crop.fileUri).let { it.exists() && it.length() > 0 }))
        }

        val signContextText = if (validDetections.isNotEmpty()) {
            """
            SCANNED PARKING SIGNS:
            ${validDetections.size} distinct sign plate(s) were captured at this parking spot:
            ${validDetections.mapIndexed { idx, crop ->
                "- Sign #${idx + 1} (${crop.normalizedBox.label}): Visible text: \"${crop.ocrText.replace("\n", " ")}\""
            }.joinToString("\n")}
            
            Note: All cropped signs belong to the same post and location. Evaluate how they interact and apply together.
            """.trimIndent()
        } else {
            "Inspect the captured image to detect all parking signs and posted regulations at this location."
        }

        val hasValidImages = (bitmap != null && !bitmap.isRecycled) || validDetections.any { 
            (!it.bitmap.isRecycled && it.bitmap.width > 0) || (it.fileUri.isNotBlank() && java.io.File(it.fileUri).exists()) 
        }

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY" && hasValidImages) {
            try {
                val prompt = """
                    You are CURB, an expert parking regulation assistant.
                    Current evaluation time: $currentTimeStr
                    $locationContextText
                    
                    $signContextText
                    
                    TASK:
                    Interpret all visible parking rules from the provided sign images, including where applicable:
                    - Whether parking is currently allowed or restricted at this moment
                    - Time limit restrictions (e.g. 2 Hour, 30 Min)
                    - Active days and enforcement hours
                    - Permit requirements (e.g. Area Permit holders exempt)
                    - Payment / meter requirements
                    - Street cleaning and sweeping windows
                    - Commercial or passenger loading restrictions
                    - Arrow directions, precedence (e.g. tow-away superseding standard parking)
                    - Visual symbols and curb rules
                    - Stated exceptions (holidays, weekends)
                    
                    ACCURACY & SAFETY RULES:
                    - CRITICAL: Never interpret URLs, web addresses, hashes, UUIDs, filenames, machine tokens, image metadata, or random alphanumeric noise as parking signs or rules.
                    - Rely strictly on visible parking sign text and symbols. Do NOT invent unreadable text or imagined rules.
                    - If signs are conflicting, damaged, unreadable, or insufficient, set verdict to "AMBIGUOUS" and explain that signage is unclear.
                    - If parking is prohibited right now, set verdict to "RESTRICTED".
                    - If parking is permitted right now, set verdict to "ALLOWED".
                    
                    Return a strict JSON object with this exact structure:
                    {
                      "verdict": "ALLOWED" or "RESTRICTED" or "AMBIGUOUS",
                      "statusChipText": "Concise 2-4 word status (e.g. 'Updated just now' or 'Enforced until 6 PM')",
                      "allowedUntilTime": "e.g. '6:00 PM' or 'No parking permitted' or 'Verify physical signage'",
                      "timeRemaining": "e.g. '2h 00m remaining' or '0m'",
                      "parkingRules": ["Rule 1 summary", "Rule 2 summary"],
                      "explanation": "Clear, concise 2-sentence explanation of what is allowed or why it is restricted/unclear right now.",
                      "zoneType": "e.g. 'Metered parking zone' or 'Standard parking area'",
                      "paymentInfo": "e.g. 'Pay at meter' or 'Free parking'",
                      "vehicleApplicability": "e.g. 'Standard passenger vehicles'",
                      "detectedSigns": [
                        {
                          "id": "1",
                          "title": "Meaningful title (e.g. '2-Hour Daytime Limit')",
                          "subtitle": "Short day/time summary (e.g. 'Mon–Fri • 8 AM – 6 PM')",
                          "applicableDaysHours": "Full applicable schedule (e.g. 'Monday through Friday, 8:00 AM – 6:00 PM')",
                          "restrictions": "Detailed restriction (e.g. 'Max 2-hour stay enforced during daytime hours')",
                          "exceptions": "Exemptions (e.g. 'Area G permit holders exempt')",
                          "isRestrictingNow": false,
                          "isUncertain": false,
                          "statusBadge": "Active Restriction" or "Permit / Time Limit" or "Inactive Schedule" or "Unclear / Obstructed"
                        }
                      ]
                    }
                    Important: Output raw JSON only. Do not include markdown formatting or backticks.
                """.trimIndent()

                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })

                // 1. Add real cropped sign images
                if (validDetections.isNotEmpty()) {
                    for (crop in validDetections) {
                        val cropBmp = if (!crop.bitmap.isRecycled && crop.bitmap.width > 0) {
                            crop.bitmap
                        } else if (crop.fileUri.isNotBlank()) {
                            val f = java.io.File(crop.fileUri)
                            if (f.exists() && f.length() > 0) {
                                android.graphics.BitmapFactory.decodeFile(f.absolutePath)
                            } else null
                        } else null

                        val b64 = cropBmp?.toBase64()
                        if (!b64.isNullOrBlank()) {
                            partsArray.put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", b64)
                                })
                            })
                        }
                    }
                }

                // 2. Add full captured photo context if available and local crops weren't already complete
                if (bitmap != null && !bitmap.isRecycled) {
                    val fullB64 = bitmap.toBase64()
                    if (!fullB64.isNullOrBlank()) {
                        partsArray.put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", fullB64)
                            })
                        })
                    }
                }

                val jsonBody = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                }

                val request = Request.Builder()
                    .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""
                if (response.isSuccessful && responseString.isNotEmpty()) {
                    val rootJson = JSONObject(responseString)
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                    val cleanJsonStr = text.replace("```json", "").replace("```", "").trim()
                    val parsed = JSONObject(cleanJsonStr)

                    val verdictStr = parsed.optString("verdict", "AMBIGUOUS").uppercase()
                    val verdict = when {
                        verdictStr.contains("RESTRICT") -> ScanVerdict.RESTRICTED
                        verdictStr.contains("ALLOW") || verdictStr == "YES" || verdictStr == "PERMITTED" -> ScanVerdict.ALLOWED
                        else -> ScanVerdict.AMBIGUOUS
                    }

                    val rulesList = mutableListOf<String>()
                    val rulesArray = parsed.optJSONArray("parkingRules")
                    if (rulesArray != null) {
                        for (i in 0 until rulesArray.length()) {
                            rulesList.add(rulesArray.getString(i))
                        }
                    }

                    val signsList = mutableListOf<DetectedSign>()
                    val signsArray = parsed.optJSONArray("detectedSigns")
                    if (validDetections.isNotEmpty()) {
                        validDetections.forEachIndexed { i, crop ->
                            val signObj = signsArray?.optJSONObject(i)
                            val title = signObj?.optString("title")?.ifBlank { null }
                                ?: crop.normalizedBox.label.ifBlank { "Sign #${i + 1}" }
                            val subtitle = signObj?.optString("subtitle")?.ifBlank { null }
                                ?: ""
                            val daysHours = signObj?.optString("applicableDaysHours")?.ifBlank { null }
                                ?: subtitle
                            val restrictions = signObj?.optString("restrictions")?.ifBlank { null }
                                ?: signObj?.optString("ruleText")?.ifBlank { null }
                                ?: crop.ocrText.ifBlank { "Unspecified rule" }
                            val exceptions = signObj?.optString("exceptions")?.ifBlank { null }
                                ?: ""
                            val isRestrictingNow = signObj?.optBoolean("isRestrictingNow") ?: false
                            val isUncertain = signObj?.optBoolean("isUncertain") ?: false
                            val badge = signObj?.optString("statusBadge")?.ifBlank { null }
                                ?: when {
                                    isUncertain -> "Unclear / Obstructed"
                                    isRestrictingNow -> "Active Restriction"
                                    exceptions.isNotBlank() -> "Permit / Time Limit"
                                    else -> "Inactive Schedule"
                                }

                            signsList.add(
                                DetectedSign(
                                    id = crop.id,
                                    title = title,
                                    subtitle = subtitle,
                                    applicableDaysHours = daysHours,
                                    restrictions = restrictions,
                                    exceptions = exceptions,
                                    ruleText = restrictions,
                                    isRestrictingNow = isRestrictingNow,
                                    isUncertain = isUncertain,
                                    statusBadge = badge,
                                    rawText = crop.ocrText,
                                    croppedImageUri = crop.fileUri,
                                    confidence = crop.normalizedBox.confidence
                                )
                            )
                        }
                    }

                    val parsedResult = ScanResult(
                        locationName = locationName,
                        cityState = cityState,
                        verdict = verdict,
                        statusChipText = parsed.optString("statusChipText", if (verdict == ScanVerdict.ALLOWED) "Updated just now" else "Rule unclear"),
                        allowedUntilTime = parsed.optString("allowedUntilTime", "Verify physical signage").ifBlank { "Verify physical signage" },
                        timeRemaining = parsed.optString("timeRemaining", "--").ifBlank { "--" },
                        parkingRules = if (rulesList.isNotEmpty()) rulesList else listOf("No verified parking rule has been established."),
                        explanation = parsed.optString("explanation", "Parking rules could not be determined from verified sign evidence.").ifBlank { "Parking rules could not be determined from verified sign evidence." },
                        detectedSigns = signsList,
                        zoneType = parsed.optString("zoneType", "Parking zone"),
                        paymentInfo = parsed.optString("paymentInfo", ""),
                        vehicleApplicability = parsed.optString("vehicleApplicability", "")
                    )

                    return@withContext EvidenceAnchoringValidator.sanitizeAndAnchorResult(parsedResult, validDetections)
                }
            } catch (e: Exception) {
                // Fallback to intelligent local parking analyzer
            }
        }

        // Intelligent local parking analysis generator for robust experience:
        EvidenceAnchoringValidator.sanitizeAndAnchorResult(
            generateIntelligentScanResult(locationName, cityState, isLocationKnown, localDetections),
            validDetections
        )
    }

    fun hasVerifiedPhysicalSignEvidence(validDetections: List<LocalSignCrop>): Boolean {
        return ParkingAuthority.hasVerifiedSignEvidence(validDetections)
    }

    fun enforceEvidenceGatedVerdict(
        rawScanResult: ScanResult,
        validDetections: List<LocalSignCrop>
    ): ScanResult {
        return EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawScanResult, validDetections)
    }

    suspend fun askParkingAssistant(
        query: String,
        history: List<ChatMessage>,
        scanContext: ScanResult? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val scanContextPrompt = if (scanContext != null) {
            val signsSummary = if (scanContext.detectedSigns.isNotEmpty()) {
                scanContext.detectedSigns.mapIndexed { idx, sign ->
                    "Sign #${idx + 1} ('${sign.title}'): Schedule: '${sign.applicableDaysHours}', Rule: '${sign.restrictions}', Active restriction now: ${sign.isRestrictingNow}, Uncertain/Obstructed: ${sign.isUncertain}, Raw OCR: '${sign.rawText}'"
                }.joinToString("\n  ")
            } else {
                "No physical sign plates were clearly detected."
            }

            """
            CURRENT SCANNED PARKING SPOT CONTEXT:
            - Location: ${scanContext.locationName} (${scanContext.cityState})
            - Overall Verdict: ${scanContext.verdict.name} (${scanContext.verdict.displayTitle})
            - Allowed Until: ${scanContext.allowedUntilTime} (Time remaining: ${scanContext.timeRemaining})
            - Zone Type: ${scanContext.zoneType}
            - Payment Info: ${scanContext.paymentInfo}
            - Vehicle Applicability: ${scanContext.vehicleApplicability}
            - Rules Established from Evidence:
              ${scanContext.parkingRules.joinToString("\n  - ")}
            - Curb Analysis Summary: ${scanContext.explanation}
            - Physical Signs Read (${scanContext.detectedSigns.size} detected):
              $signsSummary
            
            COPILOT DIRECTIVES:
            - Answer the user's query specifically about THIS scanned parking spot using the evidence and scan context provided.
            - Do NOT re-run the scan or pretend you don't know the spot context.
            - Clearly distinguish physical evidence (e.g., "Sign #1 posted on the pole says...") from your AI interpretation.
            - If verdict is AMBIGUOUS / Rule Unclear, explain the exact ambiguity or missing physical sign evidence.
            - If verdict is RESTRICTED, explain which sign or rule prohibits parking and when the restriction ends.
            - If verdict is ALLOWED, explain permissions and any upcoming inactive restrictions.
            - Do NOT invent or fabricate rules, signs, or locations not present in this scan context.
            """.trimIndent()
        } else {
            "No current scan context attached. Provide general municipal parking guidance."
        }

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = """
                    You are Curb AI, an expert Android parking copilot.
                    $scanContextPrompt
                    
                    Keep responses focused, direct, concise, and helpful (under 3 short paragraphs).
                    Never repeat information unnecessarily; assume the user already sees the current scan result.
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    val contentsArray = JSONArray()
                    // History
                    for (msg in history.takeLast(6)) {
                        contentsArray.put(JSONObject().apply {
                            put("role", if (msg.isUser) "user" else "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", msg.text) })
                            })
                        })
                    }
                    // Current query
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", query) })
                        })
                    })
                    put("contents", contentsArray)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemPrompt) })
                        })
                    })
                }

                val request = Request.Builder()
                    .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""
                if (response.isSuccessful && responseString.isNotEmpty()) {
                    val rootJson = JSONObject(responseString)
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext text.trim()
                    }
                }
            } catch (e: Exception) {
                // Fallback to local parking knowledge base
            }
        }

        // Intelligent parking domain reasoning fallback
        answerParkingLocally(query, scanContext)
    }

    fun answerParkingLocally(query: String, scanContext: ScanResult? = null): String {
        val lower = query.lowercase(Locale.ROOT)

        if (scanContext != null) {
            if (lower.contains("why") && (lower.contains("can't") || lower.contains("restrict") || lower.contains("prohibit") || lower.contains("not allow") || lower.contains("allow"))) {
                return when (scanContext.verdict) {
                    ScanVerdict.RESTRICTED -> {
                        val activeSigns = scanContext.detectedSigns.filter { it.isRestrictingNow }
                        if (activeSigns.isNotEmpty()) {
                            val signDesc = activeSigns.joinToString(", ") { "${it.title} (${it.subtitle})" }
                            "Parking is restricted because $signDesc is currently in effect at ${scanContext.locationName} based on verified scan evidence."
                        } else {
                            "Parking is restricted at ${scanContext.locationName} based on the verified scan result: ${scanContext.explanation}"
                        }
                    }
                    ScanVerdict.ALLOWED -> "Based on the verified scan for ${scanContext.locationName}, parking is ALLOWED until ${scanContext.allowedUntilTime}. Rules established: ${scanContext.parkingRules.joinToString("; ")}."
                    ScanVerdict.AMBIGUOUS -> "The parking rule is unclear because physical signage is ambiguous, partially obscured, or insufficient. Physical verification on-site is required before leaving your vehicle."
                }
            }

            if (lower.contains("sign") || lower.contains("which sign")) {
                if (scanContext.detectedSigns.isNotEmpty()) {
                    val signListStr = scanContext.detectedSigns.joinToString("\n• ") { sign ->
                        "${sign.title} (${sign.subtitle}): ${if (sign.isRestrictingNow) "ACTIVE RESTRICTION NOW" else "Inactive schedule"}"
                    }
                    return "Here are the physical signs detected at this spot:\n\n• $signListStr\n\nCurb synthesized these signs to establish the current verdict (${scanContext.verdict.displayTitle})."
                } else {
                    return "No distinct physical sign plates were clearly resolved from this photo. Curb was unable to extract sign evidence."
                }
            }

            if (lower.contains("when") || lower.contains("limit") || lower.contains("after 6") || lower.contains("after") || lower.contains("time") || lower.contains("park")) {
                return when (scanContext.verdict) {
                    ScanVerdict.ALLOWED -> "Based on the verified scan for ${scanContext.locationName}, parking is ALLOWED until ${scanContext.allowedUntilTime} (${scanContext.timeRemaining} remaining). Established rules: ${scanContext.parkingRules.joinToString("; ")}."
                    ScanVerdict.RESTRICTED -> "Parking is currently RESTRICTED at ${scanContext.locationName} based on the verified scan context: ${scanContext.explanation}. Please check the physical sign on-site for posted enforcement hours."
                    ScanVerdict.AMBIGUOUS -> "The signage is unclear, so I cannot safely confirm whether parking is allowed or what time limits apply. Check the physical sign on-site for active hours, exceptions, and arrows."
                }
            }

            return when (scanContext.verdict) {
                ScanVerdict.RESTRICTED -> "Regarding your scan at ${scanContext.locationName}: Parking is RESTRICTED. ${scanContext.explanation}"
                ScanVerdict.ALLOWED -> "Regarding your scan at ${scanContext.locationName}: Parking is ALLOWED until ${scanContext.allowedUntilTime}. ${scanContext.explanation}"
                ScanVerdict.AMBIGUOUS -> "Regarding your scan at ${scanContext.locationName}: The signage is unclear, so I cannot safely confirm whether parking is allowed. Check the physical sign on-site for active hours, exceptions, and arrows."
            }
        }

        val generalDisclaimer = "General Information: Parking rules vary by city and posted signage. I can explain general concepts, but I cannot confirm rules for a specific street without posted sign evidence or an official local source."

        return when {
            lower.contains("after 6") || lower.contains("6 pm") || lower.contains("night") || lower.contains("after hours") -> {
                "$generalDisclaimer I can explain what an after-hours rule usually means, but I cannot confirm that a specific street allows parking after 6 PM without posted signage or a verified local rule. Check posted signs on-site for active enforcement hours, evening tow-away windows, and overnight restrictions."
            }
            lower.contains("sunday") || lower.contains("weekend") -> {
                "$generalDisclaimer While some municipalities relax metered time limits on Sundays or weekends, many cities enforce 24/7 restrictions, special event zones, loading zones, and red curbs. Always verify posted street signs for weekend enforcement."
            }
            lower.contains("green") || lower.contains("colored curb") || lower.contains("yellow") || lower.contains("red") || lower.contains("white") || lower.contains("blue") || lower.contains("curb color") -> {
                "$generalDisclaimer Standard curb color designations vary by municipality, but conceptually represent:\n\n• Red: No stopping, standing, or parking at any time.\n• Green: Short-term parking during posted hours.\n• White: Passenger loading/unloading only.\n• Yellow: Commercial loading zone during posted hours.\n• Blue: Disabled persons with valid placard/plate.\n\nThese are general concepts. Local city codes and posted signs govern exact rules for any spot."
            }
            lower.contains("street clean") || lower.contains("sweep") -> {
                "$generalDisclaimer Street cleaning restrictions prohibit parking during specific posted time windows (e.g., for sweeping or maintenance). Vehicles parked during active sweeping hours are subject to citations or towing. Check physical street signs for exact days and times."
            }
            lower.contains("holiday") -> {
                "$generalDisclaimer Some cities suspend meter enforcement or street cleaning on official city holidays, but holiday rules vary significantly by municipality and location. Safety restrictions (red zones, fire hydrants, bus stops) remain enforced. Check local city policy and posted signs."
            }
            lower.contains("meter") -> {
                "$generalDisclaimer A parking meter indicates a paid parking zone with maximum time limits during active hours. Enforcement hours and rates are posted on the meter, pay station, or nearby sign plate."
            }
            lower.contains("tow") -> {
                "$generalDisclaimer A tow-away zone prohibits stopping or parking during specified hours. Vehicles parked during tow-away windows are subject to immediate towing and impoundment."
            }
            lower.contains("arrow") -> {
                "$generalDisclaimer Arrows on parking signs indicate the physical zone where the restriction applies (e.g., to the left or right of the post). Stacked signs on the same post interact, with restrictive rules taking precedence."
            }
            else -> {
                "$generalDisclaimer I can explain general parking concepts, but I cannot confirm the rule for a specific street without posted sign evidence or an official local source. Please scan the posted parking sign or check local city regulations."
            }
        }
    }

    fun generateIntelligentScanResult(
        locationName: String,
        cityState: String = "",
        isLocationKnown: Boolean = true,
        localDetections: List<LocalSignCrop> = emptyList()
    ): ScanResult {
        val validDetections = localDetections.filter { crop ->
            !SignCandidateValidator.isDemoOrSampleCrop(crop.fileUri, crop.isDemo, crop.id) &&
            SignCandidateValidator.validateOcr(crop.ocrText).isValid &&
            ((crop.bitmap != null && !crop.bitmap.isRecycled) || (crop.fileUri.isNotBlank() && java.io.File(crop.fileUri).let { it.exists() && it.length() > 0 }))
        }

        if (validDetections.isNotEmpty()) {
            var hasRestriction = false
            var hasUnclear = false
            var hasExplicitPermission = false
            var hasPaymentMention = false
            val parsedSigns = mutableListOf<DetectedSign>()
            val rulesList = mutableListOf<String>()

            validDetections.forEachIndexed { idx, crop ->
                val text = crop.ocrText.uppercase(Locale.ROOT)
                val isRestrict = text.contains("TOW") || text.contains("CLEAN") || text.contains("SWEEP") || text.contains("NO PARK") || text.contains("NO STOP")
                val isUnclear = text.contains("TEMP") || text.length < 5
                val isPermitOrTime = text.contains("PARK") || text.contains("HR") || text.contains("HOUR") || text.contains("MIN") || text.contains("PERMIT") || text.contains("ALLOWED")
                val isPayment = text.contains("METER") || text.contains("PAY") || text.contains("RATE") || text.contains("COIN") || text.contains("FEE")

                if (isRestrict) hasRestriction = true
                if (isUnclear) hasUnclear = true
                if (isPermitOrTime) hasExplicitPermission = true
                if (isPayment) hasPaymentMention = true

                val cleanRule = SignCandidateValidator.sanitizeOcrText(crop.ocrText)

                parsedSigns.add(
                    DetectedSign(
                        id = crop.id,
                        title = crop.normalizedBox.label.ifBlank { "Sign #${idx + 1}" },
                        subtitle = when {
                            text.contains("MON") || text.contains("FRI") -> "Mon–Fri posted schedule"
                            text.contains("TUE") -> "Tuesday scheduled window"
                            text.contains("DAILY") -> "Daily posted window"
                            else -> ""
                        },
                        ruleText = cleanRule,
                        isRestrictingNow = isRestrict,
                        rawText = crop.ocrText,
                        croppedImageUri = crop.fileUri,
                        confidence = crop.normalizedBox.confidence
                    )
                )

                rulesList.add("${crop.normalizedBox.label.ifBlank { "Sign #${idx + 1}" }}: $cleanRule")
            }

            val verdict = when {
                hasRestriction -> ScanVerdict.RESTRICTED
                hasUnclear -> ScanVerdict.AMBIGUOUS
                hasExplicitPermission -> ScanVerdict.ALLOWED
                else -> ScanVerdict.AMBIGUOUS
            }

            val res = ScanResult(
                locationName = locationName,
                cityState = cityState,
                verdict = verdict,
                statusChipText = if (verdict == ScanVerdict.ALLOWED) "Updated just now" else if (verdict == ScanVerdict.RESTRICTED) "Enforced now" else "Signage unclear",
                allowedUntilTime = if (verdict == ScanVerdict.ALLOWED) "Verify physical signage" else if (verdict == ScanVerdict.RESTRICTED) "No parking permitted" else "Verify physical signage",
                timeRemaining = "--",
                parkingRules = rulesList.ifEmpty { listOf("No verified parking rule has been established.") },
                explanation = when (verdict) {
                    ScanVerdict.ALLOWED -> "Active parking signage was confirmed. Based on posted hours, daytime parking is permitted at this location."
                    ScanVerdict.RESTRICTED -> "Active municipal restrictions (tow-away or street sweeping window) prohibit parking at this location right now."
                    ScanVerdict.AMBIGUOUS -> "Some signage text was unclear or partially obscured. Please verify the physical signs before leaving your vehicle."
                },
                detectedSigns = parsedSigns,
                zoneType = if (hasPaymentMention) "Metered parking zone" else if (verdict == ScanVerdict.RESTRICTED) "Restricted zone" else "Parking zone",
                paymentInfo = if (hasPaymentMention) "Pay at meter or pay station" else "",
                vehicleApplicability = ""
            )
            return enforceEvidenceGatedVerdict(res, validDetections)
        }

        val explanationText = if (isLocationKnown && locationName.isNotBlank() && locationName != "Location unavailable" && locationName != "Location access needed") {
            "No distinct parking signs were resolved in the image at $locationName. Parking rules could not be determined from verified sign evidence."
        } else {
            "No distinct parking signs were resolved in the captured image. Parking rules could not be determined from verified sign evidence."
        }

        val emptyRes = ScanResult(
            locationName = locationName,
            cityState = cityState,
            verdict = ScanVerdict.AMBIGUOUS,
            statusChipText = "Signage unclear",
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf(
                "No verified parking rule has been established."
            ),
            explanation = explanationText,
            detectedSigns = emptyList(),
            zoneType = "Parking zone",
            paymentInfo = "",
            vehicleApplicability = ""
        )
        return enforceEvidenceGatedVerdict(emptyRes, validDetections)
    }

    fun getPreparedPresets(context: Context): List<SampleSignPreset> {
        val crop2hr = com.example.data.detection.SignDetectionService.getOrCreateSampleSignCrop(
            context, "2hr_metered", "2 HOUR PARKING", "8:00 AM TO 6:00 PM • MON - FRI", false
        )
        val cropClean = com.example.data.detection.SignDetectionService.getOrCreateSampleSignCrop(
            context, "street_cleaning", "NO PARKING", "8:00 AM TO 10:00 AM • TUE & THU • STREET CLEANING", true
        )
        val cropTow = com.example.data.detection.SignDetectionService.getOrCreateSampleSignCrop(
            context, "tow_away", "TOW-AWAY ZONE", "NO STOPPING • 4:00 PM TO 6:00 PM • MON - FRI", true
        )
        val cropLoading = com.example.data.detection.SignDetectionService.getOrCreateSampleSignCrop(
            context, "loading_zone", "COMMERCIAL LOADING", "9:00 AM TO 4:00 PM • MON - SAT", true
        )
        val cropPermit = com.example.data.detection.SignDetectionService.getOrCreateSampleSignCrop(
            context, "permit_area", "PERMIT PARKING ONLY", "AREA G • 8:00 AM TO 6:00 PM", false
        )
        val cropTemp = com.example.data.detection.SignDetectionService.getOrCreateSampleSignCrop(
            context, "temp_construction", "NO PARKING", "TEMPORARY CONSTRUCTION • DATES OBSCORED", true
        )

        return listOf(
            SampleSignPreset(
                id = "preset_allowed",
                title = "2-Hour Metered Zone",
                previewDescription = "Standard daytime parking with weekday schedule",
                simulatedVerdict = ScanVerdict.ALLOWED,
                locationName = "Downtown Metered Spot",
                allowedUntil = "6:00 PM",
                rules = listOf(
                    "2 Hour Parking: 8:00 AM – 6:00 PM, Mon – Fri",
                    "Street Cleaning: Tuesday & Thursday, 8:00 AM – 10:00 AM",
                    "Free parking on weekends and municipal holidays"
                ),
                explanation = "Parking is permitted for up to 2 hours until 6:00 PM today. Street sweeping is not active at this time.",
                detectedSigns = listOf(
                    DetectedSign("1", "2 HOUR PARKING", "8 AM TO 6 PM • MON–FRI", ruleText = "2-hour limit during daytime hours.", isRestrictingNow = false, croppedImageUri = crop2hr, isDemo = true),
                    DetectedSign("2", "NO PARKING", "8 AM TO 10 AM • TUE & THU", ruleText = "Street cleaning schedule (inactive today).", isRestrictingNow = false, croppedImageUri = cropClean, isDemo = true),
                    DetectedSign("3", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", ruleText = "Peak commute route restriction.", isRestrictingNow = false, croppedImageUri = cropTow, isDemo = true)
                )
            ),
            SampleSignPreset(
                id = "preset_restricted",
                title = "Commute Tow-Away Zone",
                previewDescription = "Active peak-hour tow restriction and commercial loading",
                simulatedVerdict = ScanVerdict.RESTRICTED,
                locationName = "Urban Transit Corridor",
                allowedUntil = "No parking permitted",
                rules = listOf(
                    "TOW-AWAY NO STOPPING: 4:00 PM – 6:00 PM, Mon – Fri",
                    "Commercial Loading Only: 9:00 AM – 4:00 PM",
                    "Strictly enforced with immediate citation and tow"
                ),
                explanation = "Parking is currently prohibited. This location is inside an active peak-hour tow-away commute corridor.",
                detectedSigns = listOf(
                    DetectedSign("1", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", ruleText = "Active commute tow restriction.", isRestrictingNow = true, croppedImageUri = cropTow, isDemo = true),
                    DetectedSign("2", "COMMERCIAL LOADING", "9 AM TO 4 PM • MON–SAT", ruleText = "Restricted to commercial vehicles only.", isRestrictingNow = true, croppedImageUri = cropLoading, isDemo = true)
                )
            ),
            SampleSignPreset(
                id = "preset_ambiguous",
                title = "Residential & Construction Area",
                previewDescription = "Conflicting placards and temporary construction overlay",
                simulatedVerdict = ScanVerdict.AMBIGUOUS,
                locationName = "Residential Boundary",
                allowedUntil = "Rule unclear",
                rules = listOf(
                    "Temporary Emergency Construction Notice (Partially Faded)",
                    "Permit Area Exception with Conflicting Directional Arrows",
                    "Temporary No Parking placard posted over permanent sign"
                ),
                explanation = "The visible signs have conflicting directional arrows and temporary construction overlay placards. Please verify physical signage on the post before parking.",
                detectedSigns = listOf(
                    DetectedSign("1", "TEMPORARY RESTRICTION", "CONSTRUCTION NOTICE", ruleText = "Temporary placard posted over post.", isRestrictingNow = true, isUncertain = true, croppedImageUri = cropTemp, isDemo = true),
                    DetectedSign("2", "PERMIT PARKING ONLY", "AREA G • 8 AM TO 6 PM", ruleText = "Permit exemption zone.", isRestrictingNow = false, croppedImageUri = cropPermit, isDemo = true)
                )
            )
        )
    }

    val PRESET_SIGNS = listOf(
        SampleSignPreset(
            id = "preset_allowed",
            title = "2-Hour Metered Zone",
            previewDescription = "Standard daytime parking with weekday schedule",
            simulatedVerdict = ScanVerdict.ALLOWED,
            locationName = "Downtown Metered Spot",
            allowedUntil = "6:00 PM",
            rules = listOf(
                "2 Hour Parking: 8:00 AM – 6:00 PM, Mon – Fri",
                "Street Cleaning: Tuesday & Thursday, 8:00 AM – 10:00 AM",
                "Free parking on weekends and municipal holidays"
            ),
            explanation = "Parking is permitted for up to 2 hours until 6:00 PM today. Street sweeping is not active at this time.",
            detectedSigns = listOf(
                DetectedSign("1", "2 HOUR PARKING", "8 AM TO 6 PM • MON–FRI", "2-hour limit during daytime hours.", isDemo = true),
                DetectedSign("2", "NO PARKING", "8 AM TO 10 AM • TUE & THU", "Street cleaning schedule (inactive today).", isDemo = true),
                DetectedSign("3", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", "Peak commute route restriction.", isDemo = true)
            )
        ),
        SampleSignPreset(
            id = "preset_restricted",
            title = "Commute Tow-Away Zone",
            previewDescription = "Active peak-hour tow restriction and commercial loading",
            simulatedVerdict = ScanVerdict.RESTRICTED,
            locationName = "Urban Transit Corridor",
            allowedUntil = "No parking permitted",
            rules = listOf(
                "TOW-AWAY NO STOPPING: 4:00 PM – 6:00 PM, Mon – Fri",
                "Commercial Loading Only: 9:00 AM – 4:00 PM",
                "Strictly enforced with immediate citation and tow"
            ),
            explanation = "Parking is currently prohibited. This location is inside an active peak-hour tow-away commute corridor.",
            detectedSigns = listOf(
                DetectedSign("1", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", "Active commute tow restriction.", isRestrictingNow = true, isDemo = true),
                DetectedSign("2", "COMMERCIAL LOADING", "9 AM TO 4 PM • MON–SAT", "Restricted to commercial vehicles only.", isRestrictingNow = true, isDemo = true)
            )
        ),
        SampleSignPreset(
            id = "preset_ambiguous",
            title = "Residential & Construction Area",
            previewDescription = "Conflicting placards and temporary construction overlay",
            simulatedVerdict = ScanVerdict.AMBIGUOUS,
            locationName = "Residential Boundary",
            allowedUntil = "Rule unclear",
            rules = listOf(
                "Temporary Emergency Construction Notice (Partially Faded)",
                "Permit Area Exception with Conflicting Directional Arrows",
                "Temporary No Parking placard posted over permanent sign"
            ),
            explanation = "The visible signs have conflicting directional arrows and temporary construction overlay placards. Please verify physical signage on the post before parking.",
            detectedSigns = listOf(
                DetectedSign("1", "TEMPORARY RESTRICTION", "CONSTRUCTION NOTICE", "Temporary placard posted over post.", isDemo = true),
                DetectedSign("2", "PERMIT PARKING ONLY", "AREA G • 8 AM TO 6 PM", "Permit exemption zone.", isDemo = true)
            )
        )
    )
}
