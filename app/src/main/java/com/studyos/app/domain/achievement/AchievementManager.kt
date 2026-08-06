package com.studyos.app.domain.achievement

import com.studyos.app.data.model.UserStats

object AchievementManager {

    fun calculate(stats: UserStats): List<Achievement> {
        val claimedIds = stats.claimedAchievementIds.toSet()

        // 1. Freshman Scout (First Lesson Completed)
        val firstLessonProgress = (stats.topicsCompleted + stats.studySessions).coerceAtMost(1)
        val firstLessonUnlocked = firstLessonProgress >= 1

        // 2. Streak Sentinel (7-Day Streak)
        val streak7Progress = stats.streak.coerceAtMost(7)
        val streak7Unlocked = stats.streak >= 7

        // 3. Focus Titan (10 Hours of Focus = 600 Mins)
        val focusTitanProgress = stats.totalFocusMinutes.coerceAtMost(600)
        val focusTitanUnlocked = stats.totalFocusMinutes >= 600

        // 4. Algorithm Overlord (Complete 15 DSA Topics)
        val dsaProgress = stats.dsaCompletedTopicsCount.coerceAtMost(15)
        val dsaUnlocked = stats.dsaCompletedTopicsCount >= 15

        // 5. Semester Survivor (Finish First Semester)
        val semProgress = stats.completedSemesters.size.coerceAtMost(1)
        val semUnlocked = stats.completedSemesters.isNotEmpty()

        // 6. Productivity Master (Complete 100 Tasks)
        val tasksProgress = stats.completedTasksCount.coerceAtMost(100)
        val tasksUnlocked = stats.completedTasksCount >= 100

        // 7. Streak Starter (3-Day Streak)
        val streak3Progress = stats.streak.coerceAtMost(3)
        val streak3Unlocked = stats.streak >= 3

        // 8. Knowledge Warrior (Reach 500 XP)
        val xp500Progress = stats.xp.coerceAtMost(500)
        val xp500Unlocked = stats.xp >= 500

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
                id = "algorithm_overlord",
                title = "Algorithm Overlord",
                description = "Master 15 Data Structure topics",
                iconEmoji = "🧠",
                xpReward = 200,
                target = 15,
                progress = dsaProgress,
                unlocked = dsaUnlocked,
                claimed = claimedIds.contains("algorithm_overlord")
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
                id = "knowledge_warrior",
                title = "Knowledge Warrior",
                description = "Reach 500 XP",
                iconEmoji = "⚔️",
                xpReward = 150,
                target = 500,
                progress = xp500Progress,
                unlocked = xp500Unlocked,
                claimed = claimedIds.contains("knowledge_warrior")
            )
        )
    }
}
