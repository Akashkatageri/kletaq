package com.kletaq.app.domain.journey

import com.kletaq.app.core.config.AdaptiveEngineConfig
import com.kletaq.app.data.model.MemoryCard
import com.kletaq.app.data.model.MemoryHealth
import com.kletaq.app.data.model.UserStats

data class JourneyRecommendation(
    val title: String,
    val description: String,
    val actionType: String, // "lesson", "review", "habit", "exam_prep"
    val targetId: String,
    val priorityScore: Double
)

/**
 * Data-driven Priority Recommendation Engine for Kletaq Journey.
 * Calculates task priority scores using configurable weights from AdaptiveEngineConfig.
 */
object AdaptiveJourneyEngine {

    fun computePriorityScore(
        card: MemoryCard,
        userStats: UserStats,
        isExamApproaching: Boolean = false,
        isRecentlyCompleted: Boolean = false,
        config: AdaptiveEngineConfig = AdaptiveEngineConfig(),
        nowTimestamp: Long = System.currentTimeMillis()
    ): Double {
        val importanceWeight = card.importanceScore * config.weightImportance

        val healthEnum = MemoryHealth.fromString(card.memoryHealth)
        val healthMultiplier = when (healthEnum) {
            MemoryHealth.CRITICAL -> 3.0
            MemoryHealth.WEAK -> 2.0
            MemoryHealth.FADING -> 1.2
            MemoryHealth.STABLE -> 0.5
            MemoryHealth.FRESH -> 0.1
        }
        val memoryHealthWeight = healthMultiplier * config.weightMemoryHealth

        val isOverdue = card.nextReview > 0L && card.nextReview <= nowTimestamp
        val overdueWeight = if (isOverdue) config.weightOverdue else 0.0

        val examWeight = if (isExamApproaching) config.weightExamProximity else 0.0
        val journeyProgressWeight = (1.0f - userStats.questCompletionPercentage).toDouble() * config.weightJourneyProgress
        val habitWeight = if (userStats.studyStreak == 0) config.weightAtomicHabit else 0.5

        val recentPenalty = if (isRecentlyCompleted) config.penaltyRecentlyCompleted else 0.0

        return (importanceWeight + memoryHealthWeight + overdueWeight + examWeight + journeyProgressWeight + habitWeight) - recentPenalty
    }

    fun recommendBestAction(
        memoryCards: List<MemoryCard>,
        userStats: UserStats,
        isExamApproaching: Boolean = false,
        config: AdaptiveEngineConfig = AdaptiveEngineConfig(),
        nowTimestamp: Long = System.currentTimeMillis()
    ): JourneyRecommendation {
        val overdueCards = memoryCards.filter { it.nextReview > 0L && it.nextReview <= nowTimestamp }
        if (overdueCards.isNotEmpty()) {
            val topOverdue = overdueCards.maxByOrNull {
                computePriorityScore(it, userStats, isExamApproaching, config = config, nowTimestamp = nowTimestamp)
            }!!
            val score = computePriorityScore(topOverdue, userStats, isExamApproaching, config = config, nowTimestamp = nowTimestamp)
            return JourneyRecommendation(
                title = "Review ${topOverdue.topicName.ifBlank { "Weak Topic" }}",
                description = "Memory health is fading. Take a 2-minute review to restore retention!",
                actionType = "review",
                targetId = topOverdue.topicId,
                priorityScore = score
            )
        }

        val weakCards = memoryCards.filter {
            val h = MemoryHealth.fromString(it.memoryHealth)
            h == MemoryHealth.WEAK || h == MemoryHealth.CRITICAL
        }
        if (weakCards.isNotEmpty()) {
            val topWeak = weakCards.maxByOrNull {
                computePriorityScore(it, userStats, isExamApproaching, config = config, nowTimestamp = nowTimestamp)
            }!!
            val score = computePriorityScore(topWeak, userStats, isExamApproaching, config = config, nowTimestamp = nowTimestamp)
            return JourneyRecommendation(
                title = "Revise ${topWeak.topicName.ifBlank { "Weak Topic" }}",
                description = "Strengthen your foundation before moving forward.",
                actionType = "review",
                targetId = topWeak.topicId,
                priorityScore = score
            )
        }

        // Default Journey Lesson Recommendation
        return JourneyRecommendation(
            title = "Continue Journey Lesson",
            description = "Keep up your ${userStats.studyStreak}-day study streak! Complete your next quest topic.",
            actionType = "lesson",
            targetId = "next_journey_topic",
            priorityScore = 1.0
        )
    }
}
