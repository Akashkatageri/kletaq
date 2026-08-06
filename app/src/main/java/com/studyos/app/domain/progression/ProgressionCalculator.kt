package com.studyos.app.domain.progression

enum class TopicDifficulty {
    EASY, MEDIUM, HARD
}

enum class AchievementTier {
    COMMON, RARE, EPIC, LEGENDARY, ULTRA_RARE
}

object ProgressionCalculator {

    /**
     * Exponential Level Progression Formula:
     * Level 1: 0 XP
     * Level 2: 100 XP
     * Level 3: 250 XP
     * Level 4: 475 XP
     * Level 5: 805 XP
     * Level 10: 5200 XP
     * Level 20: 32000 XP
     * Level 50: 150000 XP
     */
    fun xpForLevel(level: Int): Long {
        if (level <= 1) return 0L
        return (100 * Math.pow(1.5, (level - 1).toDouble())).toLong()
    }

    fun calculateLevel(totalXp: Long): Int {
        if (totalXp <= 0L) return 1
        var level = 1
        while (true) {
            val nextLevelReq = xpForLevel(level + 1)
            if (totalXp < nextLevelReq) break
            level++
        }
        return level
    }

    fun xpForTopicCompletion(difficulty: TopicDifficulty): Long {
        return when (difficulty) {
            TopicDifficulty.EASY -> 80L
            TopicDifficulty.MEDIUM -> 100L
            TopicDifficulty.HARD -> 140L
        }
    }

    fun xpForModuleCompletion(): Long = 250L

    fun xpForSpacedRepetition(
        isOverdue: Boolean = false,
        isHardMastery: Boolean = false,
        isQueueCompleted: Boolean = false,
        reviewStreakDays: Int = 0
    ): Long {
        var xp = 15L // Base review
        if (isOverdue) xp += 15L
        if (isHardMastery) xp += 25L
        if (isQueueCompleted) xp += 50L

        val streakBonus = (reviewStreakDays * 10L).coerceAtMost(50L)
        xp += streakBonus

        return xp
    }

    fun xpForFocusSession(
        sessionMinutes: Int,
        dailyGoalMinutes: Int,
        previousFocusMinutesToday: Int
    ): Long {
        if (sessionMinutes <= 0) return 0L

        // Base focus session XP: 1 XP for every 2 minutes studied (min 10 XP per session)
        val baseFocusXp = (sessionMinutes / 2L).coerceAtLeast(10L)
        val newTotalMinutesToday = previousFocusMinutesToday + sessionMinutes
        val goalAlreadyMet = previousFocusMinutesToday >= dailyGoalMinutes
        val goalMetNow = newTotalMinutesToday >= dailyGoalMinutes

        var xpGained = baseFocusXp

        // Daily focus goal reached (+25 XP bonus)
        if (!goalAlreadyMet && goalMetNow && dailyGoalMinutes > 0) {
            xpGained += 25L
        }

        // Additional 30 minutes blocks (+10 XP per block bonus)
        val prevBlocks = previousFocusMinutesToday / 30
        val newBlocks = newTotalMinutesToday / 30
        val addedBlocks = (newBlocks - prevBlocks).coerceAtLeast(0)
        xpGained += (addedBlocks * 10L)

        // Calculate current daily focus XP cap (100 XP max per day)
        return xpGained.coerceAtMost(100L)
    }

    fun xpForAchievement(tier: AchievementTier): Long {
        return when (tier) {
            AchievementTier.COMMON -> 100L
            AchievementTier.RARE -> 350L
            AchievementTier.EPIC -> 1000L
            AchievementTier.LEGENDARY -> 2500L
            AchievementTier.ULTRA_RARE -> 10000L
        }
    }
}
