package com.kletaq.app.core.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import com.kletaq.app.core.di.KletaqApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object SecureAiPreferences {
    private const val TAG = "SecureAiPreferences"
    private const val PREFS_NAME = "kletaq_secure_ai_prefs"
    private const val KEY_CONFIG_JSON = "ai_config_json"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _configFlow = MutableStateFlow(UserAiConfig())
    val configFlow: StateFlow<UserAiConfig> = _configFlow.asStateFlow()

    private var roundRobinIndex = 0
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadConfig(context)
    }

    private fun getContext(): Context {
        return KletaqApplication.appContext
    }

    private fun loadConfig(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_CONFIG_JSON, null)
            if (jsonStr != null) {
                val parsed = parseJson(jsonStr)
                _configFlow.value = parsed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading AI config: ${e.message}", e)
        }
    }

    private fun persistConfig(config: UserAiConfig) {
        _configFlow.value = config
        scope.launch {
            try {
                val prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val jsonStr = serializeJson(config)
                prefs.edit().putString(KEY_CONFIG_JSON, jsonStr).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting AI config: ${e.message}", e)
            }
        }
    }

    @Synchronized
    fun addKey(entry: AiKeyEntry) {
        val current = _configFlow.value
        val updated = current.keys.filter { it.id != entry.id } + entry
        persistConfig(current.copy(keys = updated))
    }

    @Synchronized
    fun updateKey(entry: AiKeyEntry) {
        val current = _configFlow.value
        val updated = current.keys.map { if (it.id == entry.id) entry else it }
        persistConfig(current.copy(keys = updated))
    }

    @Synchronized
    fun removeKey(id: String) {
        val current = _configFlow.value
        val updated = current.keys.filter { it.id != id }
        persistConfig(current.copy(keys = updated))
    }

    @Synchronized
    fun setStrategy(strategy: KeyPoolingStrategy) {
        val current = _configFlow.value
        persistConfig(current.copy(strategy = strategy))
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        val current = _configFlow.value
        persistConfig(current.copy(isEnabled = enabled))
    }

    @Synchronized
    fun markRateLimited(keyId: String, durationMs: Long = 60_000L) {
        val now = System.currentTimeMillis()
        val current = _configFlow.value
        val updated = current.keys.map { key ->
            if (key.id == keyId) key.copy(isRateLimitedUntil = now + durationMs, lastError = "Rate limited (Cooling down)") else key
        }
        persistConfig(current.copy(keys = updated))
    }

    @Synchronized
    fun updateLatency(keyId: String, latencyMs: Long, error: String? = null) {
        val current = _configFlow.value
        val updated = current.keys.map { key ->
            if (key.id == keyId) key.copy(lastLatencyMs = latencyMs, lastError = error) else key
        }
        persistConfig(current.copy(keys = updated))
    }

    @Synchronized
    fun getAvailableKeys(): List<AiKeyEntry> {
        val config = _configFlow.value
        if (!config.isEnabled) return emptyList()
        return config.activeKeys.filter { it.isAvailable() }
    }

    @Synchronized
    fun getNextKey(): AiKeyEntry? {
        val available = getAvailableKeys()
        if (available.isEmpty()) return null

        val strategy = _configFlow.value.strategy
        return when (strategy) {
            KeyPoolingStrategy.ROUND_ROBIN -> {
                val idx = (roundRobinIndex++) % available.size
                available[idx]
            }
            KeyPoolingStrategy.FAILOVER -> {
                // Priority order: first available key in pool
                available.first()
            }
        }
    }

    fun hasCustomKeys(): Boolean {
        val config = _configFlow.value
        return config.isEnabled && config.hasCustomKeys
    }

    // --- JSON Serialization & Light Obfuscation ---

    private fun obfuscate(text: String): String {
        return Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    private fun deobfuscate(encoded: String): String {
        return try {
            String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encoded
        }
    }

    private fun serializeJson(config: UserAiConfig): String {
        val root = JSONObject()
        root.put("isEnabled", config.isEnabled)
        root.put("strategy", config.strategy.name)

        val keysArray = JSONArray()
        for (key in config.keys) {
            val keyObj = JSONObject()
            keyObj.put("id", key.id)
            keyObj.put("label", key.label)
            keyObj.put("provider", key.provider.name)
            keyObj.put("apiKey", obfuscate(key.apiKey))
            keyObj.put("model", key.model)
            keyObj.put("customBaseUrl", key.customBaseUrl)
            keyObj.put("isActive", key.isActive)
            keyObj.put("createdAt", key.createdAt)
            keyObj.put("lastLatencyMs", key.lastLatencyMs)
            keysArray.put(keyObj)
        }
        root.put("keys", keysArray)
        return root.toString()
    }

    private fun parseJson(jsonStr: String): UserAiConfig {
        val root = JSONObject(jsonStr)
        val isEnabled = root.optBoolean("isEnabled", true)
        val strategyStr = root.optString("strategy", KeyPoolingStrategy.FAILOVER.name)
        val strategy = try {
            KeyPoolingStrategy.valueOf(strategyStr)
        } catch (e: Exception) {
            KeyPoolingStrategy.FAILOVER
        }

        val keysList = mutableListOf<AiKeyEntry>()
        val keysArray = root.optJSONArray("keys")
        if (keysArray != null) {
            for (i in 0 until keysArray.length()) {
                val keyObj = keysArray.getJSONObject(i)
                val id = keyObj.optString("id")
                val label = keyObj.optString("label", "API Key ${i + 1}")
                val providerStr = keyObj.optString("provider", AiProviderType.GEMINI.name)
                val provider = try {
                    AiProviderType.valueOf(providerStr)
                } catch (e: Exception) {
                    AiProviderType.GEMINI
                }
                val rawKey = keyObj.optString("apiKey", "")
                val apiKey = deobfuscate(rawKey)
                val rawModel = keyObj.optString("model", provider.defaultModel)
                val model = when (rawModel) {
                    "mixtral-8x7b-32768" -> "llama-3.3-70b-versatile"
                    "gemini-2.0-flash-lite-preview-02-05" -> "gemini-2.5-flash"
                    "google/gemini-2.0-flash-exp:free" -> "openrouter/free"
                    else -> rawModel
                }
                val customBaseUrl = keyObj.optString("customBaseUrl", "")
                val isActive = keyObj.optBoolean("isActive", true)
                val createdAt = keyObj.optLong("createdAt", System.currentTimeMillis())
                val lastLatencyMs = keyObj.optLong("lastLatencyMs", 0L)

                keysList.add(
                    AiKeyEntry(
                        id = id,
                        label = label,
                        provider = provider,
                        apiKey = apiKey,
                        model = model,
                        customBaseUrl = customBaseUrl,
                        isActive = isActive,
                        createdAt = createdAt,
                        lastLatencyMs = lastLatencyMs
                    )
                )
            }
        }

        return UserAiConfig(
            isEnabled = isEnabled,
            strategy = strategy,
            keys = keysList
        )
    }
}

