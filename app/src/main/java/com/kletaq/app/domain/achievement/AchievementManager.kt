package com.kletaq.app.domain.achievement

import com.kletaq.app.data.model.UserStats
import com.kletaq.app.domain.progression.AchievementTier
import com.kletaq.app.domain.progression.ProgressionCalculator

object AchievementManager {

    fun calculate(stats: UserStats): List<Achievement> {
        val claimedIds = stats.claimedAchievementIds.toSet()

        // --- Category 1: Onboarding & Basics ---
        val freshmanProgress = (stats.totalTopicsCompleted + stats.studySessions + stats.completedTasksCount).coerceAtMost(1)
        val firstQuestProgress = stats.completedTasksCount.coerceAtMost(1)
        val focusNoviceProgress = stats.totalFocusMinutes.coerceAtMost(25)

        // --- Category 2: Streaks & Consistency ---
        val streak3Progress = stats.studyStreak.coerceAtMost(3)
        val streak7Progress = stats.studyStreak.coerceAtMost(7)
        val streak14Progress = stats.studyStreak.coerceAtMost(14)
        val streak30Progress = stats.studyStreak.coerceAtMost(30)
        val streak100Progress = stats.studyStreak.coerceAtMost(100)

        // --- Category 3: Focus & Deep Work ---
        val focus5hProgress = stats.totalFocusMinutes.coerceAtMost(300)
        val focusTitanProgress = stats.totalFocusMinutes.coerceAtMost(600)
        val focus50hProgress = stats.totalFocusMinutes.coerceAtMost(3000)

        // --- Category 4: Academics & Mastery ---
        val topic10Progress = stats.totalTopicsCompleted.coerceAtMost(10)
        val topic50Progress = stats.totalTopicsCompleted.coerceAtMost(50)
        val backlogProgress = stats.completedBacklogs.coerceAtMost(5)
        val dsaProgress = stats.dsaCompletedTopicsCount.coerceAtMost(15)
        val semProgress = stats.completedSemesters.size.coerceAtMost(1)

        // --- Category 5: XP & Level Progression ---
        val xp500Progress = stats.totalXp.toInt().coerceAtMost(500)
        val xp1000Progress = stats.totalXp.toInt().coerceAtMost(1000)
        val xp5000Progress = stats.totalXp.toInt().coerceAtMost(5000)

        // --- Category 6: Productivity Master ---
        val tasks100Progress = stats.completedTasksCount.coerceAtMost(100)

        val list = listOf(
            // Onboarding & Basics
            Achievement(
                id = "freshman_scout",
                title = "Freshman Scout",
                description = "Complete your first study topic or lesson",
                iconEmoji = "🎯",
                category = "General",
                tier = AchievementTier.COMMON,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.COMMON).toInt(),
                target = 1,
                progress = freshmanProgress,
                unlocked = freshmanProgress >= 1,
                claimed = claimedIds.contains("freshman_scout")
            ),
            Achievement(
                id = "first_quest",
                title = "First Quest",
                description = "Complete your first daily study task",
                iconEmoji = "🌱",
                category = "Productivity",
                tier = AchievementTier.COMMON,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.COMMON).toInt(),
                target = 1,
                progress = firstQuestProgress,
                unlocked = firstQuestProgress >= 1,
                claimed = claimedIds.contains("first_quest")
            ),
            Achievement(
                id = "focus_novice",
                title = "Focus Novice",
                description = "Log your first 25-minute Pomodoro focus session",
                iconEmoji = "⏱️",
                category = "Focus",
                tier = AchievementTier.COMMON,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.COMMON).toInt(),
                target = 25,
                progress = focusNoviceProgress,
                unlocked = focusNoviceProgress >= 25,
                claimed = claimedIds.contains("focus_novice")
            ),

            // Streaks
            Achievement(
                id = "streak_starter",
                title = "On Fire",
                description = "Maintain a 3-day study streak",
                iconEmoji = "🥉",
                category = "Streaks",
                tier = AchievementTier.COMMON,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.COMMON).toInt(),
                target = 3,
                progress = streak3Progress,
                unlocked = streak3Progress >= 3,
                claimed = claimedIds.contains("streak_starter")
            ),
            Achievement(
                id = "streak_sentinel",
                title = "Streak Sentinel",
                description = "Maintain a 7-day study streak",
                iconEmoji = "🔥",
                category = "Streaks",
                tier = AchievementTier.RARE,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.RARE).toInt(),
                target = 7,
                progress = streak7Progress,
                unlocked = streak7Progress >= 7,
                claimed = claimedIds.contains("streak_sentinel")
            ),
            Achievement(
                id = "streak_14",
                title = "Two-Week Warrior",
                description = "Maintain a 14-day continuous study streak",
                iconEmoji = "⚡",
                category = "Streaks",
                tier = AchievementTier.EPIC,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.EPIC).toInt(),
                target = 14,
                progress = streak14Progress,
                unlocked = streak14Progress >= 14,
                claimed = claimedIds.contains("streak_14")
            ),
            Achievement(
                id = "streak_30",
                title = "Unstoppable",
                description = "Reach a 30-day study streak milestone",
                iconEmoji = "🏆",
                category = "Streaks",
                tier = AchievementTier.LEGENDARY,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.LEGENDARY).toInt(),
                target = 30,
                progress = streak30Progress,
                unlocked = streak30Progress >= 30,
                claimed = claimedIds.contains("streak_30")
            ),
            Achievement(
                id = "streak_100",
                title = "Century Centurion",
                description = "Achieve a monumental 100-day study streak",
                iconEmoji = "💯",
                category = "Streaks",
                tier = AchievementTier.ULTRA_RARE,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.ULTRA_RARE).toInt(),
                target = 100,
                progress = streak100Progress,
                unlocked = streak100Progress >= 100,
                claimed = claimedIds.contains("streak_100")
            ),

            // Focus & Time
            Achievement(
                id = "focus_5h",
                title = "Deep Worker",
                description = "Log 5 hours of total focus time",
                iconEmoji = "🧘",
                category = "Focus",
                tier = AchievementTier.RARE,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.RARE).toInt(),
                target = 300,
                progress = focus5hProgress,
                unlocked = focus5hProgress >= 300,
                claimed = claimedIds.contains("focus_5h")
            ),
            Achievement(
                id = "focus_titan",
                title = "Focus Titan",
                description = "Complete 10 hours of focus sessions",
                iconEmoji = "⏳",
                category = "Focus",
                tier = AchievementTier.RARE,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.RARE).toInt(),
                target = 600,
                progress = focusTitanProgress,
                unlocked = focusTitanProgress >= 600,
                claimed = claimedIds.contains("focus_titan")
            ),
            Achievement(
                id = "focus_50h",
                title = "Focus Grandmaster",
                description = "Log 50 hours of total focus time",
                iconEmoji = "💎",
                category = "Focus",
                tier = AchievementTier.LEGENDARY,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.LEGENDARY).toInt(),
                target = 3000,
                progress = focus50hProgress,
                unlocked = focus50hProgress >= 3000,
                claimed = claimedIds.contains("focus_50h")
            ),

            // Academics
            Achievement(
                id = "topic_10",
                title = "Subject Explorer",
                description = "Complete 10 academic study topics",
                iconEmoji = "📖",
                category = "Academics",
                tier = AchievementTier.COMMON,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.COMMON).toInt(),
                target = 10,
                progress = topic10Progress,
                unlocked = topic10Progress >= 10,
                claimed = claimedIds.contains("topic_10")
            ),
            Achievement(
                id = "topic_50",
                title = "Knowledge Scholar",
                description = "Complete 50 academic study topics",
                iconEmoji = "📚",
                category = "Academics",
                tier = AchievementTier.EPIC,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.EPIC).toInt(),
                target = 50,
                progress = topic50Progress,
                unlocked = topic50Progress >= 50,
                claimed = claimedIds.contains("topic_50")
            ),
            Achievement(
                id = "backlog_slayer",
                title = "Backlog Slayer",
                description = "Clear 5 backlog topics",
                iconEmoji = "🗡️",
                category = "Academics",
                tier = AchievementTier.RARE,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.RARE).toInt(),
                target = 5,
                progress = backlogProgress,
                unlocked = backlogProgress >= 5,
                claimed = claimedIds.contains("backlog_slayer")
            ),
            Achievement(
                id = "algorithm_overlord",
                title = "Algorithm Overlord",
                description = "Master 15 Data Structure & Algorithm topics",
                iconEmoji = "🧠",
                category = "Academics",
                tier = AchievementTier.EPIC,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.EPIC).toInt(),
                target = 15,
                progress = dsaProgress,
                unlocked = dsaProgress >= 15,
                claimed = claimedIds.contains("algorithm_overlord")
            ),
            Achievement(
                id = "semester_survivor",
                title = "Semester Survivor",
                description = "Finish all requirements for an entire academic semester",
                iconEmoji = "🎓",
                category = "Academics",
                tier = AchievementTier.LEGENDARY,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.LEGENDARY).toInt(),
                target = 1,
                progress = semProgress,
                unlocked = semProgress >= 1,
                claimed = claimedIds.contains("semester_survivor")
            ),

            // Progression
            Achievement(
                id = "knowledge_warrior",
                title = "Knowledge Warrior",
                description = "Reach 500 Total XP",
                iconEmoji = "⚔️",
                category = "Progression",
                tier = AchievementTier.COMMON,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.COMMON).toInt(),
                target = 500,
                progress = xp500Progress,
                unlocked = xp500Progress >= 500,
                claimed = claimedIds.contains("knowledge_warrior")
            ),
            Achievement(
                id = "xp_1000",
                title = "XP Master",
                description = "Earn 1,000 Total XP",
                iconEmoji = "🌟",
                category = "Progression",
                tier = AchievementTier.RARE,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.RARE).toInt(),
                target = 1000,
                progress = xp1000Progress,
                unlocked = xp1000Progress >= 1000,
                claimed = claimedIds.contains("xp_1000")
            ),
            Achievement(
                id = "xp_5000",
                title = "XP Legend",
                description = "Earn 5,000 Total XP",
                iconEmoji = "👑",
                category = "Progression",
                tier = AchievementTier.EPIC,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.EPIC).toInt(),
                target = 5000,
                progress = xp5000Progress,
                unlocked = xp5000Progress >= 5000,
                claimed = claimedIds.contains("xp_5000")
            ),

            // Productivity
            Achievement(
                id = "productivity_master",
                title = "Productivity Master",
                description = "Complete 100 study tasks",
                iconEmoji = "⚡",
                category = "Productivity",
                tier = AchievementTier.EPIC,
                xpReward = ProgressionCalculator.xpForAchievement(AchievementTier.EPIC).toInt(),
                target = 100,
                progress = tasks100Progress,
                unlocked = tasks100Progress >= 100,
                claimed = claimedIds.contains("productivity_master")
            )
        )

        return list
    }
}
