package com.kletaq.app.core.ai

import java.util.UUID

enum class AiProviderType(
    val displayName: String,
    val defaultModel: String,
    val defaultBaseUrl: String,
    val isFree: Boolean,
    val keyPrefixHint: String,
    val setupUrl: String
) {
    GEMINI(
        displayName = "Google Gemini (Free)",
        defaultModel = "gemini-3.1-flash-lite",
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        isFree = true,
        keyPrefixHint = "AIzaSy...",
        setupUrl = "https://aistudio.google.com/app/apikey"
    ),
    GROQ(
        displayName = "Groq Cloud (Free)",
        defaultModel = "llama-3.3-70b-versatile",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        isFree = true,
        keyPrefixHint = "gsk_...",
        setupUrl = "https://console.groq.com/keys"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultModel = "openrouter/free",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        isFree = true,
        keyPrefixHint = "sk-or-v1-...",
        setupUrl = "https://openrouter.ai/keys"
    ),
    DEEPSEEK(
        displayName = "DeepSeek Official",
        defaultModel = "deepseek-chat",
        defaultBaseUrl = "https://api.deepseek.com",
        isFree = false,
        keyPrefixHint = "sk-...",
        setupUrl = "https://platform.deepseek.com/api_keys"
    ),
    OPENAI(
        displayName = "OpenAI (ChatGPT)",
        defaultModel = "gpt-4o-mini",
        defaultBaseUrl = "https://api.openai.com/v1",
        isFree = false,
        keyPrefixHint = "sk-...",
        setupUrl = "https://platform.openai.com/api-keys"
    ),
    CUSTOM_OPENAI(
        displayName = "Custom Endpoint (OpenAI-compatible)",
        defaultModel = "default",
        defaultBaseUrl = "",
        isFree = true,
        keyPrefixHint = "Any key",
        setupUrl = ""
    );

    companion object {
        fun detectProvider(apiKey: String): AiProviderType {
            val trimmed = apiKey.trim()
            return when {
                trimmed.startsWith("AIzaSy") -> GEMINI
                trimmed.startsWith("gsk_") -> GROQ
                trimmed.startsWith("sk-or-v1-") -> OPENROUTER
                trimmed.startsWith("sk-") -> {
                    // Default to OpenAI unless user switches to DeepSeek
                    OPENAI
                }
                else -> GEMINI
            }
        }

        fun getAvailableModels(provider: AiProviderType): List<String> = when (provider) {
            GEMINI -> listOf(
                "gemini-3.1-flash-lite",
                "gemini-3.0-flash",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.0-flash"
            )
            GROQ -> listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "qwen/qwen3.8-27b",
                "deepseek-r1-distill-llama-70b"
            )
            OPENROUTER -> listOf(
                "openrouter/free",
                "deepseek/deepseek-r1:free",
                "meta-llama/llama-3.3-70b-instruct:free",
                "google/gemini-2.5-flash",
                "deepseek/deepseek-chat",
                "anthropic/claude-3.7-sonnet",
                "anthropic/claude-3.5-haiku"
            )
            DEEPSEEK -> listOf(
                "deepseek-chat",
                "deepseek-reasoner"
            )
            OPENAI -> listOf(
                "gpt-4o-mini",
                "gpt-4o",
                "o3-mini"
            )
            CUSTOM_OPENAI -> listOf(
                "default"
            )
        }
    }
}

enum class KeyPoolingStrategy(
    val title: String,
    val description: String
) {
    FAILOVER(
        title = "Auto-Failover (Recommended)",
        description = "Uses primary key first. Instantly switches to backup keys if a key hits rate limits (HTTP 429)."
    ),
    ROUND_ROBIN(
        title = "Round-Robin Load Balancing",
        description = "Distributes queries evenly across all active keys to prevent any single key from hitting rate limits."
    )
}

data class AiKeyEntry(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val provider: AiProviderType,
    val apiKey: String,
    val model: String = provider.defaultModel,
    val customBaseUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isRateLimitedUntil: Long = 0L,
    val lastLatencyMs: Long = 0L,
    val lastError: String? = null
) {
    fun isAvailable(): Boolean {
        if (!isActive || apiKey.isBlank()) return false
        return System.currentTimeMillis() >= isRateLimitedUntil
    }

    fun maskedKey(): String {
        val trimmed = apiKey.trim()
        if (trimmed.length <= 8) return "••••••••"
        return "${trimmed.take(4)}••••••••${trimmed.takeLast(4)}"
    }
}

data class UserAiConfig(
    val isEnabled: Boolean = true,
    val strategy: KeyPoolingStrategy = KeyPoolingStrategy.FAILOVER,
    val keys: List<AiKeyEntry> = emptyList()
) {
    val activeKeys: List<AiKeyEntry> get() = keys.filter { it.isActive && it.apiKey.isNotBlank() }
    val hasCustomKeys: Boolean get() = activeKeys.isNotEmpty()
}

