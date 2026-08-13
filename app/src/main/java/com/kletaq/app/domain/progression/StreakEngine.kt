package com.kletaq.app.domain.progression

data class StreakEvaluationResult(
    val newStreak: Int,
    val newShieldsRemaining: Int,
    val isStreakPreserved: Boolean,
    val wasShieldConsumed: Boolean,
    val isStreakReset: Boolean,
    val isFrozenForBreak: Boolean = false
)

object StreakEngine {

    fun isDayCompleted(
        dailyXpGained: Long,
        isFocusGoalMet: Boolean,
        topicsCompletedToday: Int,
        isReviewQueueCompleted: Boolean
    ): Boolean {
        return dailyXpGained >= 40L ||
            isFocusGoalMet ||
            topicsCompletedToday >= 2 ||
            isReviewQueueCompleted
    }

    fun evaluateStreak(
        currentStreak: Int,
        currentShields: Int,
        isDayCompleted: Boolean,
        isBreakModeActive: Boolean = false
    ): StreakEvaluationResult {
        // Rule: During a semester break or vacation mode:
        // ❌ Don't increment streak
        // ❌ Don't reset streak
        // ❌ Don't consume shields
        // ✅ Freeze and preserve exact current streak value
        if (isBreakModeActive) {
            return StreakEvaluationResult(
                newStreak = currentStreak,
                newShieldsRemaining = currentShields,
                isStreakPreserved = true,
                wasShieldConsumed = false,
                isStreakReset = false,
                isFrozenForBreak = true
            )
        }

        if (isDayCompleted) {
            val updatedStreak = currentStreak + 1
            return StreakEvaluationResult(
                newStreak = updatedStreak,
                newShieldsRemaining = currentShields,
                isStreakPreserved = true,
                wasShieldConsumed = false,
                isStreakReset = false,
                isFrozenForBreak = false
            )
        }

        // Day was missed - check study shield protection
        if (currentShields > 0) {
            return StreakEvaluationResult(
                newStreak = currentStreak, // Streak protected
                newShieldsRemaining = currentShields - 1,
                isStreakPreserved = true,
                wasShieldConsumed = true,
                isStreakReset = false,
                isFrozenForBreak = false
            )
        }

        // Shields depleted - reset streak to 0
        return StreakEvaluationResult(
            newStreak = 0,
            newShieldsRemaining = 0,
            isStreakPreserved = false,
            wasShieldConsumed = false,
            isStreakReset = true,
            isFrozenForBreak = false
        )
    }
}
