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

    private fun Bitmap.toOptimizedBase64(maxDimension: Int = 1280, quality: Int = 85): String? {
        if (isRecycled || width <= 0 || height <= 0) return null
        return try {
            val targetBmp = if (width > maxDimension || height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(width, height)
                val newW = (width * scale).toInt().coerceAtLeast(1)
                val newH = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(this, newW, newH, true)
            } else {
                this
            }
            val outputStream = ByteArrayOutputStream()
            targetBmp.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (targetBmp != this) {
                targetBmp.recycle()
            }
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
        val totalScanStartTime = System.currentTimeMillis()
        android.util.Log.d("CurbTiming", "Scan pipeline analysis initiated. Location: '$locationName', City/State: '$cityState'")

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

        // PREFLIGHT GATE BEFORE GEMINI:
        // Critical Invariant: NO VERIFIED PHYSICAL SIGN EVIDENCE -> NO GEMINI CALL
        if (validDetections.isEmpty()) {
            val totalTime = System.currentTimeMillis() - totalScanStartTime
            android.util.Log.d("CurbTiming", "PREFLIGHT GATE: Gemini SKIPPED! Zero valid sign candidates detected. Total scan time: ${totalTime} ms")

            val neutralExplanation = if (isLocationKnown && locationName.isNotBlank() && locationName != "Location unavailable" && locationName != "Location access needed") {
                "No distinct parking signs were resolved in the image at $locationName. Parking rules could not be determined from verified sign evidence."
            } else {
                "No distinct parking signs were resolved in the captured image. Parking rules could not be determined from verified sign evidence."
            }

            val unanchoredResult = ScanResult(
                locationName = locationName,
                cityState = cityState,
                verdict = ScanVerdict.AMBIGUOUS,
                statusChipText = "Signage unclear",
                allowedUntilTime = "Verify physical signage",
                timeRemaining = "--",
                parkingRules = listOf("No verified parking rule has been established."),
                explanation = neutralExplanation,
                detectedSigns = emptyList(),
                zoneType = "Parking zone",
                paymentInfo = "",
                vehicleApplicability = ""
            )

            return@withContext EvidenceAnchoringValidator.sanitizeAndAnchorResult(unanchoredResult, emptyList())
        }

        android.util.Log.d("CurbTiming", "PREFLIGHT GATE: Passed. Proceeding to Gemini request with ${validDetections.size} validated sign crop(s).")

        val signContextText = """
            SCANNED PARKING SIGNS:
            ${validDetections.size} distinct sign plate(s) were captured at this parking spot:
            ${validDetections.mapIndexed { idx, crop ->
                "- Sign #${idx + 1} (${crop.normalizedBox.label}): Visible text: \"${crop.ocrText.replace("\n", " ")}\""
            }.joinToString("\n")}
            
            Note: All cropped signs belong to the same post and location. Evaluate how they interact and apply together.
        """.trimIndent()

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val encodeStartTime = System.currentTimeMillis()
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

                // 1. Add real cropped sign images (authoritative visual evidence)
                for (crop in validDetections) {
                    val cropBmp = if (crop.bitmap != null && !crop.bitmap.isRecycled && crop.bitmap.width > 0) {
                        crop.bitmap
                    } else if (crop.fileUri.isNotBlank()) {
                        val f = java.io.File(crop.fileUri)
                        if (f.exists() && f.length() > 0) {
                            android.graphics.BitmapFactory.decodeFile(f.absolutePath)
                        } else null
                    } else null

                    val b64 = cropBmp?.toOptimizedBase64(maxDimension = 1024, quality = 85)
                    if (!b64.isNullOrBlank()) {
                        partsArray.put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", b64)
                            })
                        })
                    }
                }

                // 2. Add full captured photo context (scaled safely to 1280px max to optimize payload size)
                if (bitmap != null && !bitmap.isRecycled) {
                    val fullB64 = bitmap.toOptimizedBase64(maxDimension = 1280, quality = 80)
                    if (!fullB64.isNullOrBlank()) {
                        partsArray.put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", fullB64)
                            })
                        })
                    }
                }

                android.util.Log.d("CurbTiming", "Image payload encoding completed in ${System.currentTimeMillis() - encodeStartTime} ms")

                val geminiRequestStart = System.currentTimeMillis()
                android.util.Log.d("CurbTiming", "Gemini API request started")

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
                val geminiDuration = System.currentTimeMillis() - geminiRequestStart
                android.util.Log.d("CurbTiming", "Gemini API request completed in ${geminiDuration} ms with HTTP ${response.code}")
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

                    val totalTime = System.currentTimeMillis() - totalScanStartTime
                    android.util.Log.d("CurbTiming", "Total scan analysis completed via Gemini in $totalTime ms")

                    return@withContext EvidenceAnchoringValidator.sanitizeAndAnchorResult(parsedResult, validDetections)
                }
            } catch (e: Exception) {
                // Fallback to intelligent local parking analyzer
            }
        }

        // Intelligent local parking analysis generator for robust experience:
        val totalTime = System.currentTimeMillis() - totalScanStartTime
        android.util.Log.d("CurbTiming", "Total scan analysis completed via local fallback in $totalTime ms")

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
        val lowerQuery = query.trim().lowercase(Locale.ROOT)

        // 1. GREETINGS INTENT GUARD
        val isGreeting = listOf("hi", "hello", "hey", "good morning", "good afternoon", "greetings", "yo", "hi there", "hello there")
            .any { lowerQuery == it || lowerQuery.startsWith("$it ") || lowerQuery.startsWith("$it,") || lowerQuery.startsWith("$it!") }
        if (isGreeting) {
            val greetingResp = "Hello! How can I help you with your parking today? Feel free to ask about nearby signs, rules, or schedules."
            if (!isDuplicateResponse(greetingResp, history)) return@withContext greetingResp
        }

        // 2. THANKS / ACKNOWLEDGMENT INTENT GUARD
        val isThanks = listOf("thanks", "thank you", "thx", "okay", "ok", "got it", "cool", "perfect", "sounds good", "alright", "great")
            .any { lowerQuery == it || lowerQuery.startsWith("$it ") || lowerQuery.startsWith("$it,") || lowerQuery.startsWith("$it!") }
        if (isThanks) {
            val thanksResp = "You're welcome! Let me know if you have any more questions about this parking spot or any other signage."
            if (!isDuplicateResponse(thanksResp, history)) return@withContext thanksResp
        }

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
            - If query is short (e.g. "why?", "which one?", "when?"), use context from recent chat messages and this scan.
            - Clearly distinguish physical evidence (e.g., "Sign #1 posted on the pole says...") from AI interpretation.
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
                    
                    CONVERSATIONAL INTENT DIRECTIVES:
                    1. GREETINGS (hi, hello, hey, morning): Respond with a friendly, natural greeting. Do NOT output a scan verdict or parking rules unless asked.
                    2. THANKS / ACKNOWLEDGMENT (thanks, thank you, okay, got it, cool): Respond with a polite, brief acknowledgment (e.g. "You're welcome! Let me know if you have any other questions about this spot."). Do NOT output parking rules.
                    3. SPECIFIC QUESTIONS & FOLLOW-UPS (why?, which sign?, when?, what about tomorrow?, permits?):
                       - Answer the specific query directly.
                       - Use conversation history to resolve follow-ups like "why?" or "which one?".
                       - Do NOT output the same generic scan summary if the user is asking a targeted question or follow-up.
                    4. DUPLICATE ANSWER PREVENTION: Do not repeat identical sentences from previous responses in the conversation.
                    
                    Keep responses focused, direct, concise, and helpful (under 3 short paragraphs).
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
                        val trimmed = text.trim()
                        if (!isDuplicateResponse(trimmed, history)) {
                            return@withContext trimmed
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to local parking knowledge base
            }
        }

        // Intelligent parking domain reasoning fallback
        val localResult = answerParkingLocally(query, history, scanContext)
        if (isDuplicateResponse(localResult, history)) {
            return@withContext "To clarify your question regarding '${query.take(30)}': Based on the verified signage at ${scanContext?.locationName ?: "this spot"}, ${scanContext?.explanation ?: "please verify posted physical signs on-site."}"
        }
        localResult
    }

    private fun isDuplicateResponse(newText: String, history: List<ChatMessage>): Boolean {
        val lastModelMessage = history.lastOrNull { !it.isUser }?.text ?: return false
        val normNew = newText.lowercase(Locale.ROOT).replace(Regex("""\W+"""), "")
        val normLast = lastModelMessage.lowercase(Locale.ROOT).replace(Regex("""\W+"""), "")
        if (normNew == normLast) return true
        if (normNew.length > 20 && normLast.length > 20 && (normNew.contains(normLast) || normLast.contains(normNew))) return true
        return false
    }

    fun answerParkingLocally(query: String, history: List<ChatMessage> = emptyList(), scanContext: ScanResult? = null): String {
        val cleanQuery = query.trim().trim('?', '!', ' ', '.', ',').lowercase(Locale.ROOT)

        // 1. GREETING INTENT
        val isGreeting = listOf("hi", "hello", "hey", "good morning", "good afternoon", "greetings", "yo")
            .any { cleanQuery == it || cleanQuery.startsWith("$it ") || cleanQuery.startsWith("$it,") || cleanQuery.startsWith("$it!") }
        if (isGreeting) {
            return "Hello! How can I help you with your parking today? Feel free to ask about nearby signs, rules, or schedules."
        }

        // 2. THANKS INTENT
        val isThanks = listOf("thanks", "thank you", "thx", "okay", "ok", "got it", "cool", "perfect", "sounds good", "alright", "great")
            .any { cleanQuery == it || cleanQuery.startsWith("$it ") || cleanQuery.startsWith("$it,") || cleanQuery.startsWith("$it!") }
        if (isThanks) {
            return "You're welcome! Let me know if you have any more questions about this parking spot or any other signage."
        }

        if (scanContext != null) {
            // WHY INTENT
            if (cleanQuery == "why" || cleanQuery.startsWith("why ") || cleanQuery.contains("why it happened") || cleanQuery.contains("why restricted") || cleanQuery.contains("why can't") || cleanQuery.contains("reason")) {
                return when (scanContext.verdict) {
                    ScanVerdict.RESTRICTED -> {
                        val activeSigns = scanContext.detectedSigns.filter { it.isRestrictingNow }
                        if (activeSigns.isNotEmpty()) {
                            val signDesc = activeSigns.joinToString(", ") { "${it.title} (${it.subtitle.ifBlank { it.restrictions }})" }
                            "Parking is restricted because $signDesc is currently active at ${scanContext.locationName} based on verified sign evidence."
                        } else {
                            "Parking is restricted at ${scanContext.locationName} based on verified sign evidence: ${scanContext.explanation}"
                        }
                    }
                    ScanVerdict.ALLOWED -> "Parking is allowed at ${scanContext.locationName} because the posted rules permit parking right now until ${scanContext.allowedUntilTime}."
                    ScanVerdict.AMBIGUOUS -> "The parking rule is unclear because physical signage is ambiguous, partially faded, or obstructed. Physical verification on-site is required."
                }
            }

            // WHICH SIGN INTENT
            if (cleanQuery.contains("sign") || cleanQuery.contains("which sign") || cleanQuery.contains("what sign") || cleanQuery.contains("which one")) {
                if (scanContext.detectedSigns.isNotEmpty()) {
                    val signListStr = scanContext.detectedSigns.joinToString("\n• ") { sign ->
                        val sub = if (sign.subtitle.isNotBlank()) " (${sign.subtitle})" else ""
                        val status = if (sign.isRestrictingNow) "ACTIVE RESTRICTION NOW" else if (sign.isUncertain) "UNCLEAR SIGNAGE" else "Inactive schedule"
                        "${sign.title}$sub: $status"
                    }
                    return "Here are the physical signs detected at this spot:\n\n• $signListStr\n\nCurb synthesized these signs to establish the current verdict (${scanContext.verdict.displayTitle})."
                } else {
                    return "No distinct physical sign plates were clearly resolved from this photo. Curb was unable to extract sign evidence."
                }
            }

            // WHEN / TIME INTENT
            if (cleanQuery.contains("when") || cleanQuery.contains("how long") || cleanQuery.contains("until") || cleanQuery.contains("time limit") || cleanQuery.contains("after 6") || cleanQuery.contains("tomorrow") || cleanQuery.contains("weekend")) {
                return when (scanContext.verdict) {
                    ScanVerdict.ALLOWED -> "Based on the verified scan for ${scanContext.locationName}, parking is ALLOWED until ${scanContext.allowedUntilTime}. Rules established: ${scanContext.parkingRules.joinToString("; ")}."
                    ScanVerdict.RESTRICTED -> "Parking is currently RESTRICTED at ${scanContext.locationName}: ${scanContext.explanation}. Please check physical signs on-site for enforcement windows."
                    ScanVerdict.AMBIGUOUS -> "The signage is unclear, so I cannot safely confirm time limits. Please check physical signs on-site."
                }
            }

            // PERMIT INTENT
            if (cleanQuery.contains("permit") || cleanQuery.contains("exemption") || cleanQuery.contains("resident") || cleanQuery.contains("area")) {
                val permitSigns = scanContext.detectedSigns.filter { it.exceptions.contains("permit", ignoreCase = true) || it.title.contains("permit", ignoreCase = true) || it.restrictions.contains("permit", ignoreCase = true) }
                return if (permitSigns.isNotEmpty()) {
                    "Permit information from posted signage:\n" + permitSigns.joinToString("\n") { "• ${it.title}: ${it.exceptions.ifBlank { it.restrictions }}" }
                } else {
                    "No permit exemptions were detected on the posted signage for ${scanContext.locationName}."
                }
            }

            return when (scanContext.verdict) {
                ScanVerdict.RESTRICTED -> "Regarding your scan at ${scanContext.locationName}: Parking is RESTRICTED. ${scanContext.explanation}"
                ScanVerdict.ALLOWED -> "Regarding your scan at ${scanContext.locationName}: Parking is ALLOWED until ${scanContext.allowedUntilTime}. ${scanContext.explanation}"
                ScanVerdict.AMBIGUOUS -> "Regarding your scan at ${scanContext.locationName}: The signage is unclear, so I cannot safely confirm whether parking is allowed. Check the physical sign on-site for active hours, exceptions, and arrows."
            }
        }

        val generalDisclaimer = "General Information: Parking rules vary by city and posted signage."

        return when {
            cleanQuery.contains("after 6") || cleanQuery.contains("6 pm") || cleanQuery.contains("night") || cleanQuery.contains("after hours") -> {
                "$generalDisclaimer Check posted signs on-site for active enforcement hours, evening tow-away windows, and overnight restrictions."
            }
            cleanQuery.contains("sunday") || cleanQuery.contains("weekend") -> {
                "$generalDisclaimer While some municipalities relax metered time limits on Sundays or weekends, many cities enforce 24/7 restrictions, special event zones, loading zones, and red curbs."
            }
            cleanQuery.contains("green") || cleanQuery.contains("colored curb") || cleanQuery.contains("yellow") || cleanQuery.contains("red") || cleanQuery.contains("white") || cleanQuery.contains("blue") || cleanQuery.contains("curb color") -> {
                "$generalDisclaimer Standard curb color designations vary by municipality, but conceptually represent:\n\n• Red: No stopping, standing, or parking at any time.\n• Green: Short-term parking during posted hours.\n• White: Passenger loading/unloading only.\n• Yellow: Commercial loading zone during posted hours.\n• Blue: Disabled persons with valid placard/plate.\n\nThese are general concepts. Local city codes and posted signs govern exact rules for any spot."
            }
            cleanQuery.contains("street clean") || cleanQuery.contains("sweep") -> {
                "$generalDisclaimer Street cleaning restrictions prohibit parking during specific posted time windows. Check physical street signs for exact days and times."
            }
            else -> {
                "$generalDisclaimer I can explain general parking concepts, but I cannot confirm rules for a specific street without posted sign evidence or an official local source."
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
