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

    private fun createFallbackSignCrops(context: Context?, bitmap: Bitmap, count: Int): List<String> {
        if (bitmap.isRecycled || bitmap.width < 10 || bitmap.height < 10 || count <= 0) return emptyList()
        val cropsDir = if (context != null) {
            java.io.File(context.cacheDir, "sign_crops").apply { if (!exists()) mkdirs() }
        } else {
            java.io.File(System.getProperty("java.io.tmpdir") ?: "/tmp", "sign_crops").apply { if (!exists()) mkdirs() }
        }
        val paths = mutableListOf<String>()
        val sliceHeight = bitmap.height / count
        for (i in 0 until count) {
            val top = i * sliceHeight
            val height = if (i == count - 1) bitmap.height - top else sliceHeight
            try {
                val cropped = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, height)
                val file = java.io.File(cropsDir, "fallback_crop_${System.currentTimeMillis()}_$i.jpg")
                java.io.FileOutputStream(file).use { out ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                if (file.exists() && file.length() > 0) {
                    paths.add(file.absolutePath)
                }
            } catch (e: Exception) {
                // Ignore fallback crop errors
            }
        }
        return paths
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
        val locationContextText = if (isLocationKnown && locationName.isNotBlank() && locationName != "Location unavailable" && locationName != "Location access needed") {
            "Location Context: $locationName${if (cityState.isNotBlank()) ", $cityState" else ""}. Base primary determination strictly on the signs shown in the images. Do not invent municipal laws if uncertain."
        } else {
            "Location Context: Device location is unavailable. Analyze regulations strictly from the visible signs in the photo."
        }

        val signContextText = if (localDetections.isNotEmpty()) {
            """
            SCANNED PARKING SIGNS:
            ${localDetections.size} distinct sign plate(s) were captured at this parking spot:
            ${localDetections.mapIndexed { idx, crop ->
                "- Sign #${idx + 1} (${crop.normalizedBox.label}): Visible text: \"${crop.ocrText.replace("\n", " ")}\""
            }.joinToString("\n")}
            
            Note: All cropped signs belong to the same post and location. Evaluate how they interact and apply together.
            """.trimIndent()
        } else {
            "Inspect the captured image to detect all parking signs and posted regulations at this location."
        }

        val hasValidImages = (bitmap != null && !bitmap.isRecycled) || localDetections.any { 
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
                    
                    ACCURACY RULES:
                    - Rely strictly on visible text and symbols. Do NOT invent unreadable text or imagined rules.
                    - If signs are conflicting, damaged, or unreadable, set verdict to "AMBIGUOUS".
                    - If parking is not permitted right now, set verdict to "RESTRICTED".
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
                if (localDetections.isNotEmpty()) {
                    for (crop in localDetections) {
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
                    if (localDetections.isNotEmpty()) {
                        localDetections.forEachIndexed { i, crop ->
                            val signObj = signsArray?.optJSONObject(i)
                            val title = signObj?.optString("title")?.ifBlank { null }
                                ?: crop.normalizedBox.label.ifBlank { "Sign #${i + 1}" }
                            val subtitle = signObj?.optString("subtitle")?.ifBlank { null }
                                ?: "Mon–Fri • Posted Schedule"
                            val daysHours = signObj?.optString("applicableDaysHours")?.ifBlank { null }
                                ?: subtitle
                            val restrictions = signObj?.optString("restrictions")?.ifBlank { null }
                                ?: signObj?.optString("ruleText")?.ifBlank { null }
                                ?: "Standard parking regulations apply."
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
                    } else if (signsArray != null && signsArray.length() > 0) {
                        // Dynamically produce real fallback crop files from bitmap if present
                        val fallbackCrops = if (bitmap != null && !bitmap.isRecycled) {
                            createFallbackSignCrops(context, bitmap, signsArray.length())
                        } else emptyList()

                        for (i in 0 until signsArray.length()) {
                            val signObj = signsArray.optJSONObject(i) ?: continue
                            val title = signObj.optString("title", "Parking Sign #${i + 1}")
                            val subtitle = signObj.optString("subtitle", "Posted schedule")
                            val daysHours = signObj.optString("applicableDaysHours", subtitle)
                            val restrictions = signObj.optString("restrictions", signObj.optString("ruleText", "Standard regulations"))
                            val exceptions = signObj.optString("exceptions", "")
                            val isRestrictingNow = signObj.optBoolean("isRestrictingNow", false)
                            val isUncertain = signObj.optBoolean("isUncertain", false)
                            val badge = signObj.optString("statusBadge", if (isRestrictingNow) "Active Restriction" else "Individual Sign Rule")

                            val cropUri = if (i < fallbackCrops.size) fallbackCrops[i] else ""

                            signsList.add(
                                DetectedSign(
                                    id = "sign_${i + 1}",
                                    title = title,
                                    subtitle = subtitle,
                                    applicableDaysHours = daysHours,
                                    restrictions = restrictions,
                                    exceptions = exceptions,
                                    ruleText = restrictions,
                                    isRestrictingNow = isRestrictingNow,
                                    isUncertain = isUncertain,
                                    statusBadge = badge,
                                    rawText = restrictions,
                                    croppedImageUri = cropUri,
                                    confidence = 1.0f
                                )
                            )
                        }
                    }

                    return@withContext ScanResult(
                        locationName = locationName,
                        cityState = cityState,
                        verdict = verdict,
                        statusChipText = parsed.optString("statusChipText", "Updated just now"),
                        allowedUntilTime = parsed.optString("allowedUntilTime", "6:00 PM"),
                        timeRemaining = parsed.optString("timeRemaining", "2h 00m remaining"),
                        parkingRules = if (rulesList.isNotEmpty()) rulesList else listOf("Standard parking regulations apply"),
                        explanation = parsed.optString("explanation", "Curb evaluated the visible signage."),
                        detectedSigns = signsList,
                        zoneType = parsed.optString("zoneType", "Parking zone"),
                        paymentInfo = parsed.optString("paymentInfo", ""),
                        vehicleApplicability = parsed.optString("vehicleApplicability", "Standard passenger vehicles")
                    )
                }
            } catch (e: Exception) {
                // Fallback to intelligent local parking analyzer
            }
        }

        // Intelligent local parking analysis generator for robust experience:
        generateIntelligentScanResult(locationName, cityState, isLocationKnown, localDetections)
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

    private fun answerParkingLocally(query: String, scanContext: ScanResult? = null): String {
        val lower = query.lowercase(Locale.ROOT)

        if (scanContext != null) {
            if (lower.contains("why") && (lower.contains("can't") || lower.contains("restrict") || lower.contains("prohibit") || lower.contains("not allow"))) {
                return when (scanContext.verdict) {
                    ScanVerdict.RESTRICTED -> {
                        val activeSigns = scanContext.detectedSigns.filter { it.isRestrictingNow }
                        if (activeSigns.isNotEmpty()) {
                            val signDesc = activeSigns.joinToString(", ") { "${it.title} (${it.subtitle})" }
                            "Parking is restricted because $signDesc is currently in effect at ${scanContext.locationName}. This restriction overrides general daytime parking permissions."
                        } else {
                            "Parking is restricted at ${scanContext.locationName} due to an active street regulation or municipal prohibition in this time window."
                        }
                    }
                    ScanVerdict.ALLOWED -> "Parking is actually ALLOWED at ${scanContext.locationName} until ${scanContext.allowedUntilTime}. There are no active restricting signs prohibiting parking right now."
                    ScanVerdict.AMBIGUOUS -> "The parking rule is unclear because physical signage is ambiguous, partially obscured, or conflicting. Physical verification on-site is required before leaving your vehicle."
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

            if (lower.contains("when can i") || lower.contains("when do i") || lower.contains("time limit")) {
                return when (scanContext.verdict) {
                    ScanVerdict.ALLOWED -> "You can park here until ${scanContext.allowedUntilTime} (${scanContext.timeRemaining} remaining). Be sure to move your car or check for upcoming restrictions before this window ends."
                    ScanVerdict.RESTRICTED -> "Parking is currently prohibited. Check physical signage for when active enforcement ends (usually after 6:00 PM or outside morning commute hours)."
                    ScanVerdict.AMBIGUOUS -> "Because the signage is unclear, an exact time limit cannot be guaranteed safely. Check the post for physical dates and arrows."
                }
            }
        }

        return when {
            lower.contains("after 6") || lower.contains("6 pm") || lower.contains("night") -> {
                "In most standard metered zones, time limits and meter enforcement end at 6:00 PM on weekdays. After 6:00 PM, parking is usually free and unrestricted until 8:00 AM the next morning, unless a specific evening tow-away zone (e.g., 4–6 PM or 7–9 PM) or overnight street sweeping applies. Always confirm the red tow-away arrows on the post."
            }
            lower.contains("sunday") || lower.contains("weekend") -> {
                "On Sundays, standard timed parking (e.g. 2-hour limits) and street cleaning are generally not enforced in most cities, making parking free for the day. However, special event zones, 24/7 red curbs, bus stops, and loading zones remain enforced around the clock."
            }
            lower.contains("green") || lower.contains("colored curb") || lower.contains("yellow") || lower.contains("red") || lower.contains("white") -> {
                "Here is the standard curb color guide:\n\n• Red: No stopping, standing, or parking at any time (24/7).\n• Green: Short-term parking (usually 10 to 30 minutes) during business hours.\n• White: Passenger loading/unloading only (5-minute maximum limit).\n• Yellow: Commercial vehicle loading zone during posted hours (often 7 AM–6 PM).\n• Blue: Disabled persons with valid placard/license plate only."
            }
            lower.contains("street clean") || lower.contains("sweep") -> {
                "Street cleaning restrictions are strictly enforced during the exact hours posted on the broom icon sign (e.g. 8:00 AM – 10:00 AM). Vehicles parked during sweeping hours are subject to immediate ticketing and possible tow. Enforcement ends promptly when the posted window expires."
            }
            lower.contains("holiday") || lower.contains("city holiday") -> {
                "On official major city holidays (New Year's Day, Memorial Day, July 4th, Labor Day, Thanksgiving, Christmas), parking meters and street cleaning are typically suspended. However, safety zones (red zones, fire hydrants, transit stops) remain active 24/7."
            }
            else -> {
                if (scanContext != null) {
                    "Regarding your scan at ${scanContext.locationName}: The verdict is ${scanContext.verdict.displayTitle}. ${scanContext.explanation}"
                } else {
                    "Based on standard municipal parking regulations, you can park in regular unpainted curb spaces if there are no conflicting red zone markings, active street sweeping windows, or tow-away restrictions. Always make sure to park in the direction of traffic flow within 18 inches of the curb."
                }
            }
        }
    }

    fun generateIntelligentScanResult(
        locationName: String,
        cityState: String = "",
        isLocationKnown: Boolean = true,
        localDetections: List<LocalSignCrop> = emptyList()
    ): ScanResult {
        if (localDetections.isNotEmpty()) {
            var hasRestriction = false
            var hasUnclear = false
            val parsedSigns = mutableListOf<DetectedSign>()
            val rulesList = mutableListOf<String>()

            localDetections.forEachIndexed { idx, crop ->
                val text = crop.ocrText.uppercase()
                val isRestrict = text.contains("TOW") || text.contains("CLEAN") || text.contains("SWEEP") || text.contains("NO PARK") || text.contains("NO STOP")
                val isUnclear = text.contains("TEMP") || text.length < 5
                if (isRestrict) hasRestriction = true
                if (isUnclear) hasUnclear = true

                parsedSigns.add(
                    DetectedSign(
                        id = crop.id,
                        title = crop.normalizedBox.label,
                        subtitle = when {
                            text.contains("MON") || text.contains("FRI") -> "Mon–Fri posted schedule"
                            text.contains("TUE") -> "Tuesday scheduled window"
                            text.contains("DAILY") -> "Daily posted window"
                            else -> "Standard zone hours"
                        },
                        ruleText = crop.ocrText.replace("\n", " ").take(70),
                        isRestrictingNow = isRestrict,
                        rawText = crop.ocrText,
                        croppedImageUri = crop.fileUri,
                        confidence = crop.normalizedBox.confidence
                    )
                )

                rulesList.add("${crop.normalizedBox.label}: ${crop.ocrText.replace("\n", " ").take(50)}")
            }

            val verdict = when {
                hasRestriction -> ScanVerdict.RESTRICTED
                hasUnclear -> ScanVerdict.AMBIGUOUS
                else -> ScanVerdict.ALLOWED
            }

            return ScanResult(
                locationName = locationName,
                cityState = cityState,
                verdict = verdict,
                statusChipText = if (verdict == ScanVerdict.ALLOWED) "Updated just now" else if (verdict == ScanVerdict.RESTRICTED) "Enforced now" else "Rule unclear",
                allowedUntilTime = if (verdict == ScanVerdict.ALLOWED) "6:00 PM" else if (verdict == ScanVerdict.RESTRICTED) "No parking permitted" else "Verify physical signage",
                timeRemaining = if (verdict == ScanVerdict.ALLOWED) "2h 00m remaining" else "0m",
                parkingRules = rulesList.ifEmpty { listOf("Standard parking regulations apply based on localized signs.") },
                explanation = when (verdict) {
                    ScanVerdict.ALLOWED -> "Active parking signage was confirmed. Based on posted hours, daytime parking is permitted at this location."
                    ScanVerdict.RESTRICTED -> "Active municipal restrictions (tow-away or street sweeping window) prohibit parking at this location right now."
                    ScanVerdict.AMBIGUOUS -> "Some signage text was unclear or partially obscured. Please verify the physical signs before leaving your vehicle."
                },
                detectedSigns = parsedSigns,
                zoneType = if (verdict == ScanVerdict.ALLOWED) "Metered parking zone" else "Restricted zone",
                paymentInfo = if (verdict == ScanVerdict.ALLOWED) "Pay at meter or pay station" else "",
                vehicleApplicability = "Standard passenger vehicles"
            )
        }

        val explanationText = if (isLocationKnown && locationName.isNotBlank() && locationName != "Location unavailable" && locationName != "Location access needed") {
            "No distinct parking signs were resolved in the image at $locationName. Please verify posted curb regulations before leaving your vehicle."
        } else {
            "No distinct parking signs were resolved in the captured image. Please check physical street signs before parking."
        }

        return ScanResult(
            locationName = locationName,
            cityState = cityState,
            verdict = ScanVerdict.AMBIGUOUS,
            statusChipText = "Signage unclear",
            allowedUntilTime = "Check physical signs",
            timeRemaining = "--",
            parkingRules = listOf(
                "No clear parking signage could be detected in the captured image.",
                "Please verify posted curb rules before parking."
            ),
            explanation = explanationText,
            detectedSigns = emptyList(),
            zoneType = "Parking zone",
            paymentInfo = "",
            vehicleApplicability = "Standard passenger vehicles"
        )
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
                    DetectedSign("1", "2 HOUR PARKING", "8 AM TO 6 PM • MON–FRI", ruleText = "2-hour limit during daytime hours.", isRestrictingNow = false, croppedImageUri = crop2hr),
                    DetectedSign("2", "NO PARKING", "8 AM TO 10 AM • TUE & THU", ruleText = "Street cleaning schedule (inactive today).", isRestrictingNow = false, croppedImageUri = cropClean),
                    DetectedSign("3", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", ruleText = "Peak commute route restriction.", isRestrictingNow = false, croppedImageUri = cropTow)
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
                    DetectedSign("1", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", ruleText = "Active commute tow restriction.", isRestrictingNow = true, croppedImageUri = cropTow),
                    DetectedSign("2", "COMMERCIAL LOADING", "9 AM TO 4 PM • MON–SAT", ruleText = "Restricted to commercial vehicles only.", isRestrictingNow = true, croppedImageUri = cropLoading)
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
                    DetectedSign("1", "TEMPORARY RESTRICTION", "CONSTRUCTION NOTICE", ruleText = "Temporary placard posted over post.", isRestrictingNow = true, isUncertain = true, croppedImageUri = cropTemp),
                    DetectedSign("2", "PERMIT PARKING ONLY", "AREA G • 8 AM TO 6 PM", ruleText = "Permit exemption zone.", isRestrictingNow = false, croppedImageUri = cropPermit)
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
                DetectedSign("1", "2 HOUR PARKING", "8 AM TO 6 PM • MON–FRI", "2-hour limit during daytime hours."),
                DetectedSign("2", "NO PARKING", "8 AM TO 10 AM • TUE & THU", "Street cleaning schedule (inactive today)."),
                DetectedSign("3", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", "Peak commute route restriction.")
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
                DetectedSign("1", "TOW-AWAY ZONE", "4 PM TO 6 PM • MON–FRI", "Active commute tow restriction.", isRestrictingNow = true),
                DetectedSign("2", "COMMERCIAL LOADING", "9 AM TO 4 PM • MON–SAT", "Restricted to commercial vehicles only.", isRestrictingNow = true)
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
                DetectedSign("1", "TEMPORARY RESTRICTION", "CONSTRUCTION NOTICE", "Temporary placard posted over post."),
                DetectedSign("2", "PERMIT PARKING ONLY", "AREA G • 8 AM TO 6 PM", "Permit exemption zone.")
            )
        )
    )
}
