package com.kletaq.app.core.ai

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
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

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
     * Non-streaming completion call (useful for LabManualAiParser, ModelPaperAiSolver, etc.)
     */
    suspend fun generateContent(
        systemInstruction: String,
        userPrompt: String,
        temperature: Float = 0.2f,
        maxTokens: Int = 4096
    ): Result<String> = withContext(Dispatchers.IO) {
        val attempts = mutableSetOf<String>()
        while (true) {
            val availableKeys = SecureAiPreferences.getAvailableKeys().filter { it.id !in attempts }
            if (availableKeys.isEmpty()) {
                // Fallback to built-in Firebase AI
                return@withContext generateContentBuiltIn(systemInstruction, userPrompt)
            }

            val keyEntry = SecureAiPreferences.getNextKey() ?: availableKeys.first()
            attempts.add(keyEntry.id)

            try {
                val result = when (keyEntry.provider) {
                    AiProviderType.GEMINI -> generateContentGemini(keyEntry, systemInstruction, userPrompt, temperature, maxTokens)
                    else -> generateContentOpenAiCompatible(keyEntry, systemInstruction, userPrompt, temperature, maxTokens)
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
        maxTokens: Int
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
        currentObj.put("parts", JSONArray().apply { put(JSONObject().put("text", userPrompt)) })
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
        maxTokens: Int
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
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
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
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite",
            generationConfig = generationConfig { maxOutputTokens = 500; temperature = 0.2f },
            systemInstruction = content { text(systemInstruction) }
        )

        val recent = history.takeLast(4).dropWhile { it.role != "user" }
        val chat = model.startChat(history = recent.map { message -> content(role = message.role) { text(message.text.take(1000)) } })
        chat.sendMessageStream(userMessage).collect { response ->
            response.text?.let { chunk -> collector.emit(chunk) }
        }
    }

    private suspend fun generateContentBuiltIn(
        systemInstruction: String,
        userPrompt: String
    ): Result<String> {
        return try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-lite",
                generationConfig = generationConfig { maxOutputTokens = 2048; temperature = 0.2f },
                systemInstruction = content { text(systemInstruction) }
            )
            val response = model.generateContent(userPrompt)
            val text = response.text ?: throw Exception("Empty response from built-in AI")
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

