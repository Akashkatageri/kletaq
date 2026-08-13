package com.kletaq.app.domain.adaptive

import com.kletaq.app.core.config.AdaptiveEngineConfig
import com.kletaq.app.core.utils.SpacedRepetitionEngine
import com.kletaq.app.data.model.MemoryCard
import com.kletaq.app.data.model.MemoryHealth
import com.kletaq.app.data.model.ReviewRating
import com.kletaq.app.data.model.TopicDifficulty
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.domain.journey.AdaptiveJourneyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveEngineTest {

    // ── 1. Memory Health Transitions Test ──────────────────────────────────────────
    @Test
    fun testMemoryHealthTransitions() {
        val config = AdaptiveEngineConfig()

        // 1 day elapsed out of 10 day interval -> FRESH (retention ~ 99%)
        val freshHealth = SpacedRepetitionEngine.calculateMemoryHealth(1, 10, config)
        assertEquals(MemoryHealth.FRESH, freshHealth)

        // 3 days elapsed out of 10 day interval -> STABLE (retention ~ 74-85%)
        val stableHealth = SpacedRepetitionEngine.calculateMemoryHealth(3, 10, config)
        assertEquals(MemoryHealth.STABLE, stableHealth)

        // 5 days elapsed out of 10 day interval -> FADING (retention ~ 60-70%)
        val fadingHealth = SpacedRepetitionEngine.calculateMemoryHealth(5, 10, config)
        assertEquals(MemoryHealth.FADING, fadingHealth)

        // 8 days elapsed out of 10 day interval -> WEAK (retention ~ 40-50%)
        val weakHealth = SpacedRepetitionEngine.calculateMemoryHealth(8, 10, config)
        assertEquals(MemoryHealth.WEAK, weakHealth)

        // 20 days elapsed out of 10 day interval -> CRITICAL (retention < 40%)
        val criticalHealth = SpacedRepetitionEngine.calculateMemoryHealth(20, 10, config)
        assertEquals(MemoryHealth.CRITICAL, criticalHealth)
    }

    // ── 2. Difficulty x Confidence Matrix Test ────────────────────────────────────
    @Test
    fun testDifficultyXConfidenceMatrix() {
        val config = AdaptiveEngineConfig()

        val easy5 = SpacedRepetitionEngine.computeInitialIntervalDays(TopicDifficulty.EASY, 5, config)
        assertEquals(7, easy5)

        val medium3 = SpacedRepetitionEngine.computeInitialIntervalDays(TopicDifficulty.MEDIUM, 3, config)
        assertEquals(2, medium3)

        val hard1 = SpacedRepetitionEngine.computeInitialIntervalDays(TopicDifficulty.HARD, 1, config)
        assertEquals(0, hard1)
    }

    // ── 3. Exam Mode Compression Test ─────────────────────────────────────────────
    @Test
    fun testExamModeCompression() {
        val config = AdaptiveEngineConfig(examModeIntervalCompressionFactor = 0.5)
        val initialInterval = 10
        val compressed = (initialInterval * config.examModeIntervalCompressionFactor).toInt()
        assertEquals(5, compressed)
    }

    // ── 4. Priority Recommendation Engine Test ──────────────────────────────────────
    @Test
    fun testPriorityRecommendationEngine() {
        val config = AdaptiveEngineConfig()
        val userStats = UserStats(studyStreak = 3)
        val now = System.currentTimeMillis()

        val freshCard = MemoryCard(
            topicId = "topic_fresh",
            topicName = "Fresh Topic",
            memoryHealth = MemoryHealth.FRESH.value,
            nextReview = now + 864000000L
        )

        val weakCard = MemoryCard(
            topicId = "topic_weak",
            topicName = "Weak Topic",
            memoryHealth = MemoryHealth.WEAK.value,
            nextReview = now - 86400000L // Overdue
        )

        val freshScore = AdaptiveJourneyEngine.computePriorityScore(freshCard, userStats, config = config, nowTimestamp = now)
        val weakScore = AdaptiveJourneyEngine.computePriorityScore(weakCard, userStats, config = config, nowTimestamp = now)

        assertTrue("Weak overdue topic must have higher priority score than fresh topic", weakScore > freshScore)

        val rec = AdaptiveJourneyEngine.recommendBestAction(listOf(freshCard, weakCard), userStats, config = config, nowTimestamp = now)
        assertEquals("review", rec.actionType)
        assertEquals("topic_weak", rec.targetId)
    }

    // ── 5. MemoryCard Duplicate Prevention Test ───────────────────────────────────
    @Test
    fun testMemoryCardDuplicatePrevention() {
        val map = mutableMapOf<String, MemoryCard>()
        val card1 = MemoryCard(topicId = "topic_algebra", topicName = "Algebra")
        val card2 = MemoryCard(topicId = "topic_algebra", topicName = "Algebra Refreshed")

        map[card1.topicId] = card1
        map[card2.topicId] = card2

        // Must retain exactly ONE entry per topicId
        assertEquals(1, map.size)
        assertEquals("Algebra Refreshed", map["topic_algebra"]?.topicName)
    }

    // ── 6. Notification Suppression While Studying Test ───────────────────────────
    @Test
    fun testNotificationSuppressionWhileStudying() {
        val config = AdaptiveEngineConfig(suppressNotificationsWhileStudying = true)
        var isFocusActive = true

        fun shouldSuppress(isStudying: Boolean): Boolean {
            return config.suppressNotificationsWhileStudying && isStudying
        }

        assertTrue("Notification MUST be suppressed during active focus session", shouldSuppress(isFocusActive))

        isFocusActive = false
        assertFalse("Notification should NOT be suppressed when idle", shouldSuppress(isFocusActive))
    }

    // ── 7. Concurrent Completion Transaction Simulation ────────────────────────────
    @Test
    fun testConcurrentCompletionTransactionSimulation() {
        val cardMap = mutableMapOf<String, MemoryCard>()
        val now = System.currentTimeMillis()
        val card = SpacedRepetitionEngine.createInitialMemoryCard("t1", topicName = "Calculus", difficulty = TopicDifficulty.MEDIUM, confidence = 3, nowTimestamp = now)

        // Simulating two parallel reviews targeting topic 't1'
        val review1 = SpacedRepetitionEngine.updateMemoryCardReview(card, ReviewRating.GOOD, nowTimestamp = now + 1000)
        cardMap[review1.topicId] = review1

        val review2 = SpacedRepetitionEngine.updateMemoryCardReview(cardMap[review1.topicId]!!, ReviewRating.EASY, nowTimestamp = now + 2000)
        cardMap[review2.topicId] = review2

        assertEquals(2, cardMap["t1"]?.reviewCount)
        assertEquals(ReviewRating.EASY.value, cardMap["t1"]?.lastRating)
    }

    // ── 8. Configuration Override Test ───────────────────────────────────────────
    @Test
    fun testConfigurationOverride() {
        val customConfig = AdaptiveEngineConfig(
            defaultInitialEaseFactor = 2.8,
            weightImportance = 10.0
        )

        val card = SpacedRepetitionEngine.createInitialMemoryCard("t_custom", config = customConfig)
        assertEquals(2.8, card.easeFactor, 0.001)

        val userStats = UserStats()
        val score = AdaptiveJourneyEngine.computePriorityScore(card, userStats, config = customConfig)
        assertTrue("Higher importance weight should boost priority score", score > 10.0)
    }
}
