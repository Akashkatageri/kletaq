package com.kletaq.app.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakEngineTest {

    @Test
    fun testIsDayCompletedConditions() {
        // Condition 1: 40+ XP
        assertTrue(StreakEngine.isDayCompleted(dailyXpGained = 40, isFocusGoalMet = false, topicsCompletedToday = 0, isReviewQueueCompleted = false))
        assertFalse(StreakEngine.isDayCompleted(dailyXpGained = 39, isFocusGoalMet = false, topicsCompletedToday = 0, isReviewQueueCompleted = false))

        // Condition 2: Focus goal met
        assertTrue(StreakEngine.isDayCompleted(dailyXpGained = 0, isFocusGoalMet = true, topicsCompletedToday = 0, isReviewQueueCompleted = false))

        // Condition 3: 2+ topics completed
        assertTrue(StreakEngine.isDayCompleted(dailyXpGained = 0, isFocusGoalMet = false, topicsCompletedToday = 2, isReviewQueueCompleted = false))
        assertFalse(StreakEngine.isDayCompleted(dailyXpGained = 0, isFocusGoalMet = false, topicsCompletedToday = 1, isReviewQueueCompleted = false))

        // Condition 4: Review queue completed
        assertTrue(StreakEngine.isDayCompleted(dailyXpGained = 0, isFocusGoalMet = false, topicsCompletedToday = 0, isReviewQueueCompleted = true))
    }

    @Test
    fun testStreakIncrementOnDayCompleted() {
        val result = StreakEngine.evaluateStreak(
            currentStreak = 5,
            currentShields = 3,
            isDayCompleted = true
        )

        assertEquals(6, result.newStreak)
        assertEquals(3, result.newShieldsRemaining)
        assertTrue(result.isStreakPreserved)
        assertFalse(result.wasShieldConsumed)
        assertFalse(result.isStreakReset)
    }

    @Test
    fun testShieldConsumedOnMissedDay() {
        val result = StreakEngine.evaluateStreak(
            currentStreak = 10,
            currentShields = 2,
            isDayCompleted = false
        )

        assertEquals(10, result.newStreak) // Streak preserved by shield
        assertEquals(1, result.newShieldsRemaining)
        assertTrue(result.isStreakPreserved)
        assertTrue(result.wasShieldConsumed)
        assertFalse(result.isStreakReset)
    }

    @Test
    fun testStreakResetWhenShieldsDepleted() {
        val result = StreakEngine.evaluateStreak(
            currentStreak = 15,
            currentShields = 0,
            isDayCompleted = false
        )

        assertEquals(0, result.newStreak) // Streak reset to 0
        assertEquals(0, result.newShieldsRemaining)
        assertFalse(result.isStreakPreserved)
        assertFalse(result.wasShieldConsumed)
        assertTrue(result.isStreakReset)
    }

    @Test
    fun testBreakModeStreakFrozen() {
        // 25-day streak before winter break
        val result = StreakEngine.evaluateStreak(
            currentStreak = 25,
            currentShields = 3,
            isDayCompleted = false,
            isBreakModeActive = true
        )

        // Streak must be frozen at exactly 25 days (not 26, not reset to 0, 0 shields consumed)
        assertEquals(25, result.newStreak)
        assertEquals(3, result.newShieldsRemaining)
        assertTrue(result.isStreakPreserved)
        assertFalse(result.wasShieldConsumed)
        assertFalse(result.isStreakReset)
        assertTrue(result.isFrozenForBreak)
    }
}
