package com.kletaq.app.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInAiModelConfigTest {

    @Test
    fun builtInFallbackModels_useActiveFreeHigherModels() {
        assertEquals(
            listOf("gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite"),
            UnifiedAiClient.BUILT_IN_FALLBACK_MODELS
        )
        assertFalse(UnifiedAiClient.BUILT_IN_FALLBACK_MODELS.any { it.contains("1.5") || it.contains("2.0") })
    }

    @Test
    fun geminiProviderConfig_defaultsToActiveFreeFlagship() {
        assertEquals("gemini-3.8-flash", AiProviderType.GEMINI.defaultModel)
        val models = AiProviderType.getAvailableModels(AiProviderType.GEMINI)
        assertTrue(models.containsAll(listOf("gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite")))
    }
}

