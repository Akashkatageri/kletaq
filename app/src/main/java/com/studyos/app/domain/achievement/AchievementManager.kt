package com.studyos.app.domain.achievement

import com.studyos.app.data.model.UserStats

object AchievementManager {

    fun calculate(stats: UserStats): List<Achievement> {
        val claimedIds = stats.claimedAchievementIds.toSet()

        // 1. Freshman Scout (First Lesson/Topic Completed)
        val firstLessonProgress = (stats.totalTopicsCompleted + stats.studySessions + stats.completedTasksCount).coerceAtMost(1)
        val firstLessonUnlocked = firstLessonProgress >= 1

        // 2. Streak Starter (3-Day Streak)
        val streak3Progress = stats.studyStreak.coerceAtMost(3)
        val streak3Unlocked = stats.studyStreak >= 3

        // 3. Streak Sentinel (7-Day Streak)
        val streak7Progress = stats.studyStreak.coerceAtMost(7)
        val streak7Unlocked = stats.studyStreak >= 7

        // 4. Streak Unstoppable (30-Day Streak)
        val streak30Progress = stats.studyStreak.coerceAtMost(30)
        val streak30Unlocked = stats.studyStreak >= 30

        // 5. Focus Titan (10 Hours of Focus = 600 Mins)
        val focusTitanProgress = stats.totalFocusMinutes.coerceAtMost(600)
        val focusTitanUnlocked = stats.totalFocusMinutes >= 600

        // 6. Knowledge Warrior (Reach 500 XP)
        val xp500Progress = stats.totalXp.toInt().coerceAtMost(500)
        val xp500Unlocked = stats.totalXp >= 500

        // 7. XP Master (Earn 1,000 Total XP)
        val xp1000Progress = stats.totalXp.toInt().coerceAtMost(1000)
        val xp1000Unlocked = stats.totalXp >= 1000

        // 8. Backlog Slayer (Complete 5 Backlog Topics)
        val backlogProgress = stats.completedBacklogs.coerceAtMost(5)
        val backlogUnlocked = stats.completedBacklogs >= 5

        // 9. Productivity Master (Complete 100 Tasks)
        val tasksProgress = stats.completedTasksCount.coerceAtMost(100)
        val tasksUnlocked = stats.completedTasksCount >= 100

        // 10. Semester Survivor (Finish Semester)
        val semProgress = stats.completedSemesters.size.coerceAtMost(1)
        val semUnlocked = stats.completedSemesters.isNotEmpty()

        // 11. Algorithm Overlord (Complete 15 DSA Topics)
        val dsaProgress = stats.dsaCompletedTopicsCount.coerceAtMost(15)
        val dsaUnlocked = stats.dsaCompletedTopicsCount >= 15

        return listOf(
            Achievement(
                id = "freshman_scout",
                title = "Freshman Scout",
                description = "Complete your first study topic or lesson",
                iconEmoji = "🎯",
                xpReward = 50,
                target = 1,
                progress = firstLessonProgress,
                unlocked = firstLessonUnlocked,
                claimed = claimedIds.contains("freshman_scout")
            ),
            Achievement(
                id = "streak_starter",
                title = "On Fire",
                description = "Maintain a 3-day study streak",
                iconEmoji = "🥉",
                xpReward = 50,
                target = 3,
                progress = streak3Progress,
                unlocked = streak3Unlocked,
                claimed = claimedIds.contains("streak_starter")
            ),
            Achievement(
                id = "streak_sentinel",
                title = "Streak Sentinel",
                description = "Maintain a 7-day study streak",
                iconEmoji = "🔥",
                xpReward = 100,
                target = 7,
                progress = streak7Progress,
                unlocked = streak7Unlocked,
                claimed = claimedIds.contains("streak_sentinel")
            ),
            Achievement(
                id = "streak_30",
                title = "Unstoppable",
                description = "Reach a 30-day study streak",
                iconEmoji = "⚡",
                xpReward = 250,
                target = 30,
                progress = streak30Progress,
                unlocked = streak30Unlocked,
                claimed = claimedIds.contains("streak_30")
            ),
            Achievement(
                id = "focus_titan",
                title = "Focus Titan",
                description = "Complete 10 hours of focus sessions",
                iconEmoji = "⏱️",
                xpReward = 150,
                target = 600,
                progress = focusTitanProgress,
                unlocked = focusTitanUnlocked,
                claimed = claimedIds.contains("focus_titan")
            ),
            Achievement(
                id = "knowledge_warrior",
                title = "Knowledge Warrior",
                description = "Reach 500 XP",
                iconEmoji = "⚔️",
                xpReward = 150,
                target = 500,
                progress = xp500Progress,
                unlocked = xp500Unlocked,
                claimed = claimedIds.contains("knowledge_warrior")
            ),
            Achievement(
                id = "xp_1000",
                title = "XP Master",
                description = "Earn 1,000 Total XP",
                iconEmoji = "🌟",
                xpReward = 300,
                target = 1000,
                progress = xp1000Progress,
                unlocked = xp1000Unlocked,
                claimed = claimedIds.contains("xp_1000")
            ),
            Achievement(
                id = "backlog_slayer",
                title = "Backlog Slayer",
                description = "Complete 5 backlog topics",
                iconEmoji = "🗡️",
                xpReward = 200,
                target = 5,
                progress = backlogProgress,
                unlocked = backlogUnlocked,
                claimed = claimedIds.contains("backlog_slayer")
            ),
            Achievement(
                id = "productivity_master",
                title = "Productivity Master",
                description = "Complete 100 study tasks",
                iconEmoji = "⚡",
                xpReward = 300,
                target = 100,
                progress = tasksProgress,
                unlocked = tasksUnlocked,
                claimed = claimedIds.contains("productivity_master")
            ),
            Achievement(
                id = "semester_survivor",
                title = "Semester Survivor",
                description = "Finish all requirements for an entire academic semester",
                iconEmoji = "🎓",
                xpReward = 250,
                target = 1,
                progress = semProgress,
                unlocked = semUnlocked,
                claimed = claimedIds.contains("semester_survivor")
            ),
            Achievement(
                id = "algorithm_overlord",
                title = "Algorithm Overlord",
                description = "Master 15 Data Structure topics",
                iconEmoji = "🧠",
                xpReward = 200,
                target = 15,
                progress = dsaProgress,
                unlocked = dsaUnlocked,
                claimed = claimedIds.contains("algorithm_overlord")
            )
        )
    }
}
