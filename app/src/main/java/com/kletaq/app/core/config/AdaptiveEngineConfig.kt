package com.kletaq.app.core.config

import com.kletaq.app.data.model.MemoryHealth
import com.kletaq.app.data.model.ReviewRating
import com.kletaq.app.data.model.TopicDifficulty

/**
 * Data-driven, fully configurable configuration for Kletaq Adaptive Learning Engine.
 * ZERO hardcoded business logic or magic numbers anywhere in the application.
 */
data class AdaptiveEngineConfig(
    // ── Initial Interval Matrix in Days: (Difficulty x Confidence (1..5)) ────────────
    val initialIntervalMatrixDays: Map<TopicDifficulty, Map<Int, Int>> = mapOf(
        TopicDifficulty.EASY to mapOf(
            1 to 1, 2 to 2, 3 to 3, 4 to 5, 5 to 7
        ),
        TopicDifficulty.MEDIUM to mapOf(
            1 to 0, 2 to 1, 3 to 2, 4 to 3, 5 to 5
        ),
        TopicDifficulty.HARD to mapOf(
            1 to 0, 2 to 0, 3 to 1, 4 to 2, 5 to 3
        )
    ),

    // ── SM-2 Parameters ─────────────────────────────────────────────────────────────
    val defaultInitialEaseFactor: Double = 2.5,
    val minEaseFactor: Double = 1.3,
    val maxEaseFactor: Double = 3.5,

    val easeFactorModifiers: Map<ReviewRating, Double> = mapOf(
        ReviewRating.AGAIN to -0.20,
        ReviewRating.HARD to -0.15,
        ReviewRating.GOOD to 0.00,
        ReviewRating.EASY to 0.15
    ),

    val lapseIntervalResetDays: Int = 1,

    // ── Memory Health Retention Decay Parameters ────────────────────────────────────
    val memoryHealthThresholds: Map<MemoryHealth, Double> = mapOf(
        MemoryHealth.FRESH to 0.90,   // Retention >= 90%
        MemoryHealth.STABLE to 0.75,  // Retention >= 75%
        MemoryHealth.FADING to 0.60,  // Retention >= 60%
        MemoryHealth.WEAK to 0.40,    // Retention >= 40%
        MemoryHealth.CRITICAL to 0.00 // Retention < 40%
    ),

    // ── Priority Score Recommendation Weights ───────────────────────────────────────
    val weightImportance: Double = 2.5,
    val weightMemoryHealth: Double = 3.0,
    val weightExamProximity: Double = 4.0,
    val weightOverdue: Double = 3.5,
    val weightJourneyProgress: Double = 1.5,
    val weightAtomicHabit: Double = 2.0,
    val penaltyRecentlyCompleted: Double = 5.0,

    // ── Exam Mode Compression Parameters ──────────────────────────────────────────
    val examProximityDaysThreshold: Int = 7,
    val examModeIntervalCompressionFactor: Double = 0.4,

    // ── Notification & Cooldown Rules ──────────────────────────────────────────────
    val notificationCooldownHours: Long = 8,
    val maxDailyNotificationsCount: Int = 2,
    val suppressNotificationsWhileStudying: Boolean = true,
    val neverMissTwiceTinyHabitMinutes: Int = 5,

    // ── Habit & XP Rewards ────────────────────────────────────────────────────────
    val lessonCompletionBaseXp: Long = 50L,
    val reviewSessionBaseXp: Long = 20L
)
