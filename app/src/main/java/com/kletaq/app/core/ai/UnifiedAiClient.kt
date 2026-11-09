package com.kletaq.app.core.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.kletaq.app.features.chat.TutorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ParsedDocumentPage(
    val pageNumber: Int,
    val extractedText: String,
    val formulas: List<String> = emptyList(),
    val handwrittenNotes: String = ""
)

data class ParsedDocumentResult(
    val title: String,
    val pages: List<ParsedDocumentPage>,
    val rawCombinedText: String
)

object UnifiedAiClient {
    private const val TAG = "UnifiedAiClient"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Parses raw JSON or markdown-wrapped JSON returned by Gemini/vision models into [ParsedDocumentResult].
     */
    fun parseDocumentResultJson(jsonText: String): ParsedDocumentResult? {
        if (jsonText.isBlank()) return null
        val clean = jsonText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonStart = clean.indexOf('{')
        val jsonEnd = clean.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            // Fallback: If no valid JSON block found, preserve entire raw response as single page
            return ParsedDocumentResult(
                title = "Document",
                pages = listOf(ParsedDocumentPage(pageNumber = 1, extractedText = clean)),
                rawCombinedText = clean
            )
        }

        val jsonSubstring = clean.substring(jsonStart, jsonEnd + 1)
        return try {
            val root = JSONObject(jsonSubstring)
            val title = root.optString("title", "Document")
            val pagesArray = root.optJSONArray("pages") ?: JSONArray()
            val pagesList = mutableListOf<ParsedDocumentPage>()

            for (i in 0 until pagesArray.length()) {
                val pageObj = pagesArray.getJSONObject(i)
                val pageNumber = pageObj.optInt("page_number", i + 1)
                val extractedText = pageObj.optString("extracted_text", "")
                val handwritten = pageObj.optString("handwritten_notes", "")

                val formulasList = mutableListOf<String>()
                val formulasArray = pageObj.optJSONArray("formulas")
                if (formulasArray != null) {
                    for (j in 0 until formulasArray.length()) {
                        val f = formulasArray.optString(j)
                        if (f.isNotBlank()) formulasList.add(f)
                    }
                }

                pagesList.add(
                    ParsedDocumentPage(
                        pageNumber = pageNumber,
                        extractedText = extractedText,
                        formulas = formulasList,
                        handwrittenNotes = handwritten
                    )
                )
            }

            val combined = pagesList.joinToString("\n\n") { p ->
                buildString {
                    if (pagesList.size > 1) append("--- Page ${p.pageNumber} ---\n")
                    append(p.extractedText)
                    if (p.handwrittenNotes.isNotBlank()) {
                        append("\n[Notes]: ").append(p.handwrittenNotes)
                    }
                }
            }.trim()

            ParsedDocumentResult(
                title = title,
                pages = pagesList,
                rawCombinedText = combined.ifBlank { clean }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing document JSON: ${e.message}")
            ParsedDocumentResult(
                title = "Document",
                pages = listOf(ParsedDocumentPage(pageNumber = 1, extractedText = clean)),
                rawCombinedText = clean
            )
        }
    }

    /**
     * Standardized academic document parser that sends page bitmaps to Gemini with LaTeX formula extraction instructions.
     */
    suspend fun parseDocumentWithGemini(
        bitmaps: List<Bitmap>,
        documentContext: String = ""
    ): Result<ParsedDocumentResult> = withContext(Dispatchers.IO) {
        if (bitmaps.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No page bitmaps provided for parsing"))
        }

        val systemInstruction = """
            You are an expert academic document parser, mathematician, and transcriber.
            Analyze the attached document page images.
            Extract the content faithfully and completely.
            - If there are mathematical, physics, or scientific formulas, write them cleanly using LaTeX notation (e.g. \int_{0}^{\infty} e^{-x} dx, \frac{d}{dx}[f(x)], \sum_{i=1}^n x_i, \mathbf{F} = m\mathbf{a}).
            - If there is handwritten text or sketches, transcribe them accurately.
            - DO NOT summarize or skip sections. Transcribe completely.
            - Output strictly in this JSON format without markdown code blocks:
            {
              "title": "Title or primary topic of the document",
              "pages": [
                {
                  "page_number": 1,
                  "extracted_text": "Exact transcribed text and formulas in natural reading order",
                  "formulas": ["LaTeX formula 1", "LaTeX formula 2"],
                  "handwritten_notes": "Transcribed handwritten notes or annotations"
                }
              ]
            }
        """.trimIndent()

        val userPrompt = if (documentContext.isNotBlank()) {
            "Context: $documentContext\nExtract all text and formulas from the attached document pages into the structured JSON schema."
        } else {
            "Extract all text, formulas (in LaTeX), and notes from the attached document pages into the structured JSON schema."
        }

        val aiResult = generateContent(
            systemInstruction = systemInstruction,
            userPrompt = userPrompt,
            temperature = 0.1f,
            maxTokens = 8192,
            images = bitmaps
        )

        val rawResponse = aiResult.getOrNull()
        if (!rawResponse.isNullOrBlank()) {
            val parsed = parseDocumentResultJson(rawResponse)
            if (parsed != null) {
                return@withContext Result.success(parsed)
            }
        }

        Result.failure(aiResult.exceptionOrNull() ?: Exception("Failed to transcribe document with Gemini"))
    }

    /**
     * Streams an AI chat response.
     * Uses user's configured API key pool with automatic failover on 429 rate-limits.
     * If no custom keys are configured or all keys are rate-limited, falls back to built-in Firebase AI.
     */
    fun streamChat(
        systemInstruction: String,
        history: List<TutorMessage>,
        userMessage: String,
        temperature: Float = 0.3f,
        maxTokens: Int = 2048
    ): Flow<String> = flow {
        val attempts = mutableSetOf<String>()
        var success = false

        while (!success) {
            val availableKeys = SecureAiPreferences.getAvailableKeys().filter { it.id !in attempts }
            if (availableKeys.isEmpty()) {
                // No custom keys or all pool keys exhausted -> Built-in Firebase AI fallback
                Log.d(TAG, "No available custom keys left in pool. Using built-in Firebase AI.")
                streamBuiltInFallback(systemInstruction, history, userMessage, this)
                success = true
                break
            }

            val keyEntry = SecureAiPreferences.getNextKey() ?: availableKeys.first()
            attempts.add(keyEntry.id)

            try {
                when (keyEntry.provider) {
                    AiProviderType.GEMINI -> {
                        streamGemini(keyEntry, systemInstruction, history, userMessage, temperature, maxTokens, this)
                        success = true
                    }
                    AiProviderType.GROQ,
                    AiProviderType.OPENROUTER,
                    AiProviderType.OPENAI,
                    AiProviderType.DEEPSEEK,
                    AiProviderType.CUSTOM_OPENAI -> {
                        streamOpenAiCompatible(keyEntry, systemInstruction, history, userMessage, temperature, maxTokens, this)
                        success = true
                    }
                }
            } catch (e: RateLimitException) {
                Log.w(TAG, "Key '${keyEntry.label}' hit rate limit (HTTP 429). Triggering auto-failover...")
                SecureAiPreferences.markRateLimited(keyEntry.id, 60_000L)
                // Continue loop to retry with next key in pool
            } catch (e: Exception) {
                Log.e(TAG, "Key '${keyEntry.label}' failed: ${e.message}. Triggering auto-failover...", e)
                SecureAiPreferences.updateLatency(keyEntry.id, 0L, e.message)
                // If it's a critical auth error or connection failure, try next key
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming completion call (with optional Multimodal Bitmaps for scanned PDFs & handwritten exams).
     */
    suspend fun generateContent(
        systemInstruction: String,
        userPrompt: String,
        temperature: Float = 0.2f,
        maxTokens: Int = 4096,
        images: List<Bitmap> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val attempts = mutableSetOf<String>()
        while (true) {
            val availableKeys = SecureAiPreferences.getAvailableKeys().filter { it.id !in attempts }
            if (availableKeys.isEmpty()) {
                // Fallback to built-in Firebase AI
                return@withContext generateContentBuiltIn(systemInstruction, userPrompt, images)
            }

            val keyEntry = SecureAiPreferences.getNextKey() ?: availableKeys.first()
            attempts.add(keyEntry.id)

            try {
                val result = when (keyEntry.provider) {
                    AiProviderType.GEMINI -> generateContentGemini(keyEntry, systemInstruction, userPrompt, temperature, maxTokens, images)
                    else -> generateContentOpenAiCompatible(keyEntry, systemInstruction, userPrompt, temperature, maxTokens, images)
                }
                return@withContext Result.success(result)
            } catch (e: RateLimitException) {
                Log.w(TAG, "Key '${keyEntry.label}' rate limited during generation. Trying next key...")
                SecureAiPreferences.markRateLimited(keyEntry.id, 60_000L)
            } catch (e: Exception) {
                Log.e(TAG, "Key '${keyEntry.label}' error: ${e.message}", e)
                SecureAiPreferences.updateLatency(keyEntry.id, 0L, e.message)
            }
        }
        @Suppress("UNREACHABLE_CODE")
        Result.failure(Exception("All AI providers exhausted"))
    }

    /**
     * Tests / verifies an API key by sending a tiny test prompt.
     * Returns Result with latency and sample answer, or throws an error.
     */
    suspend fun verifyKey(entry: AiKeyEntry): Result<String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val responseText = when (entry.provider) {
                AiProviderType.GEMINI -> {
                    generateContentGemini(
                        entry = entry,
                        systemInstruction = "You are an API verification bot.",
                        userPrompt = "Reply with exactly: 'OK'",
                        temperature = 0.0f,
                        maxTokens = 10
                    )
                }
                else -> {
                    generateContentOpenAiCompatible(
                        entry = entry,
                        systemInstruction = "You are an API verification bot.",
                        userPrompt = "Reply with exactly: 'OK'",
                        temperature = 0.0f,
                        maxTokens = 10
                    )
                }
            }
            val latency = System.currentTimeMillis() - startTime
            SecureAiPreferences.updateLatency(entry.id, latency, null)
            Result.success("Verified (${latency}ms): ${responseText.trim()}")
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val msg = e.message ?: "Verification failed"
            SecureAiPreferences.updateLatency(entry.id, latency, msg)
            Result.failure(e)
        }
    }

    // --- GEMINI DIRECT REST IMPLEMENTATION ---

    private suspend fun streamGemini(
        entry: AiKeyEntry,
        systemInstruction: String,
        history: List<TutorMessage>,
        userMessage: String,
        temperature: Float,
        maxTokens: Int,
        collector: kotlinx.coroutines.flow.FlowCollector<String>
    ) {
        val model = entry.model.ifBlank { entry.provider.defaultModel }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=${entry.apiKey.trim()}"

        val requestJson = JSONObject()

        // System Instruction
        if (systemInstruction.isNotBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray().apply { put(JSONObject().put("text", systemInstruction)) }
            sysObj.put("parts", sysParts)
            requestJson.put("systemInstruction", sysObj)
        }

        // Contents
        val contentsArray = JSONArray()
        for (msg in history.takeLast(6)) {
            val role = if (msg.role == "user") "user" else "model"
            val itemObj = JSONObject()
            itemObj.put("role", role)
            itemObj.put("parts", JSONArray().apply { put(JSONObject().put("text", msg.text)) })
            contentsArray.put(itemObj)
        }
        // Current user message
        val currentObj = JSONObject()
        currentObj.put("role", "user")
        currentObj.put("parts", JSONArray().apply { put(JSONObject().put("text", userMessage)) })
        contentsArray.put(currentObj)
        requestJson.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", temperature)
        genConfig.put("maxOutputTokens", maxTokens)
        requestJson.put("generationConfig", genConfig)

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        handleHttpResponse(response, entry)

        val body = response.body ?: throw Exception("Empty response body from Gemini")
        BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.startsWith("data:")) {
                    val jsonStr = currentLine.removePrefix("data:").trim()
                    if (jsonStr.isNotBlank() && jsonStr != "[DONE]") {
                        try {
                            val chunkObj = JSONObject(jsonStr)
                            val candidates = chunkObj.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val candidate = candidates.getJSONObject(0)
                                val content = candidate.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val text = parts.getJSONObject(0).optString("text")
                                    if (text.isNotEmpty()) {
                                        collector.emit(text)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing Gemini SSE chunk: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun generateContentGemini(
        entry: AiKeyEntry,
        systemInstruction: String,
        userPrompt: String,
        temperature: Float,
        maxTokens: Int,
        images: List<Bitmap> = emptyList()
    ): String {
        val model = entry.model.ifBlank { entry.provider.defaultModel }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${entry.apiKey.trim()}"

        val requestJson = JSONObject()
        if (systemInstruction.isNotBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray().apply { put(JSONObject().put("text", systemInstruction)) }
            sysObj.put("parts", sysParts)
            requestJson.put("systemInstruction", sysObj)
        }

        val contentsArray = JSONArray()
        val currentObj = JSONObject()
        currentObj.put("role", "user")

        val partsArray = JSONArray()
        for (bmp in images) {
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val inlineObj = JSONObject()
            inlineObj.put("inlineData", JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", b64)
            })
            partsArray.put(inlineObj)
        }
        partsArray.put(JSONObject().put("text", userPrompt))
        currentObj.put("parts", partsArray)
        contentsArray.put(currentObj)
        requestJson.put("contents", contentsArray)

        val genConfig = JSONObject()
        genConfig.put("temperature", temperature)
        genConfig.put("maxOutputTokens", maxTokens)
        requestJson.put("generationConfig", genConfig)

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        handleHttpResponse(response, entry)

        val bodyStr = response.body?.string().orEmpty()
        val json = JSONObject(bodyStr)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text")
            }
        }
        throw Exception("No text returned by Gemini")
    }

    // --- OPENAI-COMPATIBLE REST (GROQ, OPENROUTER, DEEPSEEK, OPENAI) ---

    private fun resolveEndpoint(entry: AiKeyEntry): String {
        val base = when {
            entry.customBaseUrl.isNotBlank() -> entry.customBaseUrl.trim().removeSuffix("/")
            else -> entry.provider.defaultBaseUrl.trim().removeSuffix("/")
        }
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private suspend fun streamOpenAiCompatible(
        entry: AiKeyEntry,
        systemInstruction: String,
        history: List<TutorMessage>,
        userMessage: String,
        temperature: Float,
        maxTokens: Int,
        collector: kotlinx.coroutines.flow.FlowCollector<String>
    ) {
        val endpoint = resolveEndpoint(entry)
        val model = entry.model.ifBlank { entry.provider.defaultModel }

        val requestJson = JSONObject()
        requestJson.put("model", model)
        requestJson.put("stream", true)
        requestJson.put("temperature", temperature)
        requestJson.put("max_tokens", maxTokens)

        val messagesArray = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }
        for (msg in history.takeLast(6)) {
            val role = if (msg.role == "user") "user" else "assistant"
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", msg.text)
            })
        }
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })
        requestJson.put("messages", messagesArray)

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${entry.apiKey.trim()}")
            .addHeader("Content-Type", "application/json")

        if (entry.provider == AiProviderType.OPENROUTER) {
            reqBuilder.addHeader("HTTP-Referer", "https://kletaq.5122006.xyz")
            reqBuilder.addHeader("X-Title", "Kletaq Student Companion")
        }

        val request = reqBuilder
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        handleHttpResponse(response, entry)

        val body = response.body ?: throw Exception("Empty response body from ${entry.provider.displayName}")
        BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.startsWith("data:")) {
                    val jsonStr = currentLine.removePrefix("data:").trim()
                    if (jsonStr == "[DONE]") break
                    if (jsonStr.isNotBlank()) {
                        try {
                            val chunkObj = JSONObject(jsonStr)
                            val choices = chunkObj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val delta = choice.optJSONObject("delta")
                                val text = delta?.optString("content")
                                if (!text.isNullOrEmpty()) {
                                    collector.emit(text)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing OpenAI SSE chunk: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun generateContentOpenAiCompatible(
        entry: AiKeyEntry,
        systemInstruction: String,
        userPrompt: String,
        temperature: Float,
        maxTokens: Int,
        images: List<Bitmap> = emptyList()
    ): String {
        val endpoint = resolveEndpoint(entry)
        val model = entry.model.ifBlank { entry.provider.defaultModel }

        val requestJson = JSONObject()
        requestJson.put("model", model)
        requestJson.put("stream", false)
        requestJson.put("temperature", temperature)
        requestJson.put("max_tokens", maxTokens)

        val messagesArray = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }

        if (images.isEmpty()) {
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        } else {
            val contentArray = JSONArray()
            for (bmp in images) {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$b64")
                    })
                })
            }
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", userPrompt)
            })
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            })
        }
        requestJson.put("messages", messagesArray)

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${entry.apiKey.trim()}")
            .addHeader("Content-Type", "application/json")

        if (entry.provider == AiProviderType.OPENROUTER) {
            reqBuilder.addHeader("HTTP-Referer", "https://kletaq.5122006.xyz")
            reqBuilder.addHeader("X-Title", "Kletaq Student Companion")
        }

        val request = reqBuilder
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        handleHttpResponse(response, entry)

        val bodyStr = response.body?.string().orEmpty()
        val json = JSONObject(bodyStr)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val choice = choices.getJSONObject(0)
            val message = choice.optJSONObject("message")
            val content = message?.optString("content")
            if (!content.isNullOrEmpty()) {
                return content
            }
        }
        throw Exception("No content returned by ${entry.provider.displayName}")
    }

    // --- BUILT-IN FIREBASE AI FALLBACK ---

    private suspend fun streamBuiltInFallback(
        systemInstruction: String,
        history: List<TutorMessage>,
        userMessage: String,
        collector: kotlinx.coroutines.flow.FlowCollector<String>
    ) {
        val modelsToTry = listOf("gemini-3.5-flash", "gemini-2.5-pro", "gemini-2.5-flash")
        val recent = history.takeLast(4).dropWhile { it.role != "user" }
        var streamed = false

        for (modelName in modelsToTry) {
            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig { maxOutputTokens = 1000; temperature = 0.2f },
                    systemInstruction = content { text(systemInstruction) }
                )
                val chat = model.startChat(history = recent.map { message -> content(role = message.role) { text(message.text.take(1000)) } })
                chat.sendMessageStream(userMessage).collect { response ->
                    response.text?.let { chunk -> collector.emit(chunk) }
                }
                streamed = true
                break
            } catch (e: Exception) {
                Log.w(TAG, "streamBuiltInFallback model $modelName failed: ${e.message}")
            }
        }
        if (!streamed) {
            Log.w(TAG, "All built-in Firebase AI stream models failed")
            collector.emit("AI service temporarily unavailable. Please try again shortly or configure a custom API key in Settings -> AI Keys.")
        }
    }

    private suspend fun generateContentBuiltIn(
        systemInstruction: String,
        userPrompt: String,
        images: List<Bitmap> = emptyList()
    ): Result<String> {
        val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        var lastException: Exception? = null

        for (modelName in modelsToTry) {
            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig { maxOutputTokens = 4096; temperature = 0.2f },
                    systemInstruction = content { text(systemInstruction) }
                )
                val contentObj = content {
                    images.forEach { image(it) }
                    text(userPrompt)
                }
                val response = model.generateContent(contentObj)
                val text = response.text ?: throw Exception("Empty response from built-in AI ($modelName)")
                return Result.success(text)
            } catch (e: Exception) {
                Log.w(TAG, "Built-in model $modelName failed: ${e.message}")
                lastException = e
            }
        }

        Log.e(TAG, "All built-in Firebase AI models failed: ${lastException?.message}")
        return Result.failure(lastException ?: Exception("Firebase AI service unavailable. Please configure a custom API key in Settings -> AI Keys."))
    }

    private fun handleHttpResponse(response: Response, entry: AiKeyEntry) {
        if (response.isSuccessful) return
        val code = response.code
        val errorBody = response.body?.string().orEmpty()
        if (code == 429) {
            throw RateLimitException("HTTP 429 Rate limit exceeded on ${entry.label}: $errorBody")
        }
        if (code in 500..599) {
            throw ServerErrorException("HTTP $code Server overloaded on ${entry.label}: $errorBody")
        }
        throw Exception("HTTP $code error from ${entry.provider.displayName}: $errorBody")
    }
}

class RateLimitException(message: String) : Exception(message)
class ServerErrorException(message: String) : Exception(message)

