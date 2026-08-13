package com.kletaq.app.data.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserStats(
    val totalXp: Long = 0L,
    val currentLevel: Int = 1,

    val weeklyXp: Long = 0L,
    val monthlyXp: Long = 0L,

    val studyStreak: Int = 0,
    val longestStreak: Int = 0,

    val shieldsRemaining: Int = 3,

    val totalTopicsCompleted: Int = 0,
    val totalModulesCompleted: Int = 0,
    val totalSubjectsCompleted: Int = 0,

    val totalTopics: Int = 0,
    val totalModules: Int = 0,
    val totalSubjects: Int = 0,

    val completedBacklogs: Int = 0,
    val totalBacklogs: Int = 0,

    val totalStudyMinutes: Int = 0,
    val totalFocusMinutes: Int = 0,
    val totalReviewsCompleted: Int = 0,

    val achievementsUnlocked: Int = 0,

    val dailyXp: Map<String, Long> = emptyMap(),
    val dailyStudyMinutes: Map<String, Int> = emptyMap(),
    val dailyHabits: Map<String, Map<String, Any>> = emptyMap(),
    val hourHistogram: Map<String, Int> = emptyMap(),
    val weekdayHistogram: Map<String, Int> = emptyMap(),
    val lastStudyDate: Long = 0L,

    val completedSemesters: List<Int> = emptyList(),
    val friends: List<String> = emptyList(),

    // Backwards compatibility legacy fields
    val completedTopicKeys: List<String> = emptyList(),
    val claimedAchievementIds: List<String> = emptyList(),
    val studySessions: Int = 0,
    val dsaCompletedTopicsCount: Int = 0,
    val completedTasksCount: Int = 0,

    @ServerTimestamp
    val updatedAt: Date? = null
) {
    @get:Exclude
    val currentLevelBaseXp: Long
        get() {
            if (currentLevel <= 1) return 0L
            return (100 * Math.pow(1.5, (currentLevel - 1).toDouble())).toLong()
        }

    @get:Exclude
    val nextLevelTargetXp: Long
        get() = (100 * Math.pow(1.5, currentLevel.toDouble())).toLong()

    @get:Exclude
    val currentLevelXp: Long
        get() = (totalXp - currentLevelBaseXp).coerceAtLeast(0L)

    @get:Exclude
    val nextLevelXp: Long
        get() = (nextLevelTargetXp - currentLevelBaseXp).coerceAtLeast(1L)

    // Dynamic calculated helpers (Not stored in Firestore)
    @get:Exclude
    val semesterCompletionPercentage: Float
        get() = if (totalTopics > 0) (totalTopicsCompleted.toFloat() / totalTopics).coerceIn(0f, 1f) else 0f

    @get:Exclude
    val questCompletionPercentage: Float
        get() = if (totalModules > 0) (totalModulesCompleted.toFloat() / totalModules).coerceIn(0f, 1f) else 0f

    @get:Exclude
    val backlogCompletionPercentage: Float
        get() = if (totalBacklogs > 0) (completedBacklogs.toFloat() / totalBacklogs).coerceIn(0f, 1f) else 0f

    @get:Exclude
    val totalStudyHours: Double
        get() = totalStudyMinutes / 60.0

    // Legacy accessors
    @get:Exclude
    val xp: Int
        get() = totalXp.toInt()

    @get:Exclude
    val streak: Int
        get() = studyStreak

    @get:Exclude
    val level: Int
        get() = currentLevel

    @get:Exclude
    val topicsCompleted: Int
        get() = totalTopicsCompleted
}

/**
 * Safe deserialization extension for UserStats.
 * Handles the mixed-type `lastStudyDate` field (Long in some documents, String in others)
 * by normalizing it to Long before Firestore deserialization.
 */
fun DocumentSnapshot.toUserStatsSafe(): UserStats? {
    if (!exists()) return null
    return try {
        toObject(UserStats::class.java)
    } catch (e: RuntimeException) {
        // lastStudyDate has mixed types in Firestore (Long vs String)
        // Fall back to manual construction with the problematic field excluded
        val data = data ?: return null
        val fixedData = data.toMutableMap()
        when (val raw = fixedData["lastStudyDate"]) {
            is String -> {
                fixedData["lastStudyDate"] = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw)?.time ?: 0L
                } catch (_: Exception) { 0L }
            }
        }
        try {
            @Suppress("UNCHECKED_CAST")
            com.google.firebase.firestore.util.CustomClassMapper.convertToCustomClass(
                fixedData, UserStats::class.java, reference
            )
        } catch (_: Exception) {
            null
        }
    }
}
