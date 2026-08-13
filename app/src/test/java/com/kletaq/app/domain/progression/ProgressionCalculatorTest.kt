package com.kletaq.app.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionCalculatorTest {

    @Test
    fun testExponentialXpForLevelFormula() {
        // Level 1: 0 XP
        assertEquals(0L, ProgressionCalculator.xpForLevel(1))

        // Level 2: 150 XP (100 * 1.5^1)
        assertEquals(150L, ProgressionCalculator.xpForLevel(2))

        // Level 3: 225 XP (100 * 1.5^2)
        assertEquals(225L, ProgressionCalculator.xpForLevel(3))

        // Level 4: 337 XP (100 * 1.5^3)
        assertEquals(337L, ProgressionCalculator.xpForLevel(4))

        // Level 5: 506 XP (100 * 1.5^4)
        assertEquals(506L, ProgressionCalculator.xpForLevel(5))

        // Level 10: 3844 XP (100 * 1.5^9)
        assertEquals(3844L, ProgressionCalculator.xpForLevel(10))

        // Level 20: 221683 XP (100 * 1.5^19)
        assertEquals(221683L, ProgressionCalculator.xpForLevel(20))
    }

    @Test
    fun testCalculateLevel() {
        assertEquals(1, ProgressionCalculator.calculateLevel(0L))
        assertEquals(1, ProgressionCalculator.calculateLevel(149L))
        assertEquals(2, ProgressionCalculator.calculateLevel(150L))
        assertEquals(3, ProgressionCalculator.calculateLevel(225L))
        assertEquals(4, ProgressionCalculator.calculateLevel(337L))
        assertEquals(5, ProgressionCalculator.calculateLevel(506L))
    }

    @Test
    fun testTopicRewards() {
        assertEquals(80L, ProgressionCalculator.xpForTopicCompletion(TopicDifficulty.EASY))
        assertEquals(100L, ProgressionCalculator.xpForTopicCompletion(TopicDifficulty.MEDIUM))
        assertEquals(140L, ProgressionCalculator.xpForTopicCompletion(TopicDifficulty.HARD))
    }

    @Test
    fun testModuleBonus() {
        assertEquals(250L, ProgressionCalculator.xpForModuleCompletion())
    }

    @Test
    fun testSpacedRepetitionRewards() {
        // Base review
        assertEquals(15L, ProgressionCalculator.xpForSpacedRepetition())

        // Overdue + Hard + Queue + 3 day streak
        // 15 (base) + 15 (overdue) + 25 (hard) + 50 (queue) + 30 (streak) = 135
        assertEquals(135L, ProgressionCalculator.xpForSpacedRepetition(
            isOverdue = true,
            isHardMastery = true,
            isQueueCompleted = true,
            reviewStreakDays = 3
        ))

        // Max streak multiplier capped at +50
        // 15 + 0 + 0 + 0 + 50 = 65
        assertEquals(65L, ProgressionCalculator.xpForSpacedRepetition(
            reviewStreakDays = 10
        ))
    }

    @Test
    fun testFocusTimerRewards() {
        // 25 min Pomodoro session with 25 min goal (12 base + 25 goal met = 37 XP)
        assertEquals(37L, ProgressionCalculator.xpForFocusSession(
            sessionMinutes = 25,
            dailyGoalMinutes = 25,
            previousFocusMinutesToday = 0
        ))

        // 30 min session hitting 60 min goal (15 base + 25 goal met + 10 block = 50 XP)
        assertEquals(50L, ProgressionCalculator.xpForFocusSession(
            sessionMinutes = 30,
            dailyGoalMinutes = 60,
            previousFocusMinutesToday = 30
        ))
    }

    @Test
    fun testAchievementTierRewards() {
        assertEquals(100L, ProgressionCalculator.xpForAchievement(AchievementTier.COMMON))
        assertEquals(350L, ProgressionCalculator.xpForAchievement(AchievementTier.RARE))
        assertEquals(1000L, ProgressionCalculator.xpForAchievement(AchievementTier.EPIC))
        assertEquals(2500L, ProgressionCalculator.xpForAchievement(AchievementTier.LEGENDARY))
        assertEquals(10000L, ProgressionCalculator.xpForAchievement(AchievementTier.ULTRA_RARE))
    }
}
