package com.studyos.app.domain.streak

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class StreakData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStudyDate: Long = 0L
)

/**
 * Production Streak Manager for Klytaq.
 *
 * Rules:
 *   - Same day completion -> streak unchanged.
 *   - Next day completion -> streak +1.
 *   - Missed one or more days -> reset streak to 1.
 *   - First completion ever -> streak = 1.
 *
 * Storage: users/{uid}/stats & stats/{uid}
 */
class StreakManager(
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
) {

    /**
     * Pure calculation logic for unit testing.
     */
    fun computeNextStreak(
        currentStreak: Int,
        longestStreak: Int,
        lastStudyDate: Long,
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakData {
        if (lastStudyDate <= 0L) {
            // First completion ever
            return StreakData(
                currentStreak = 1,
                longestStreak = maxOf(longestStreak, 1),
                lastStudyDate = nowTimestamp
            )
        }

        val lastDate = Instant.ofEpochMilli(lastStudyDate).atZone(zoneId).toLocalDate()
        val nowDate = Instant.ofEpochMilli(nowTimestamp).atZone(zoneId).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(lastDate, nowDate)

        return when {
            daysBetween == 0L -> {
                // Same day completion -> streak unchanged
                StreakData(
                    currentStreak = if (currentStreak <= 0) 1 else currentStreak,
                    longestStreak = maxOf(longestStreak, if (currentStreak <= 0) 1 else currentStreak),
                    lastStudyDate = nowTimestamp
                )
            }
            daysBetween == 1L -> {
                // Consecutive day completion -> streak + 1
                val newStreak = (if (currentStreak <= 0) 0 else currentStreak) + 1
                StreakData(
                    currentStreak = newStreak,
                    longestStreak = maxOf(longestStreak, newStreak),
                    lastStudyDate = nowTimestamp
                )
            }
            else -> {
                // Missed one or more days -> reset streak to 1
                StreakData(
                    currentStreak = 1,
                    longestStreak = maxOf(longestStreak, 1),
                    lastStudyDate = nowTimestamp
                )
            }
        }
    }

    suspend fun updateStreak(
        uid: String,
        currentTimestamp: Long = System.currentTimeMillis()
    ): StreakData {
        val db = firestore ?: return StreakData()

        val statsRef = db.collection("stats").document(uid)
        val userStatsRef = db.collection("users").document(uid).collection("stats").document("current")

        var currentStreak = 0
        var longestStreak = 0
        var lastStudyDate = 0L

        try {
            val snapshot = statsRef.get().await()
            if (snapshot.exists()) {
                currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: snapshot.getLong("studyStreak")?.toInt() ?: 0
                longestStreak = snapshot.getLong("longestStreak")?.toInt() ?: currentStreak
                lastStudyDate = snapshot.getLong("lastStudyDate") ?: 0L
            }
        } catch (_: Exception) {}

        val newStreakData = computeNextStreak(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastStudyDate = lastStudyDate,
            nowTimestamp = currentTimestamp
        )

        val updateMap = mapOf(
            "currentStreak" to newStreakData.currentStreak,
            "studyStreak" to newStreakData.currentStreak,
            "longestStreak" to newStreakData.longestStreak,
            "lastStudyDate" to newStreakData.lastStudyDate
        )

        try {
            statsRef.set(updateMap, SetOptions.merge()).await()
            userStatsRef.set(updateMap, SetOptions.merge()).await()
        } catch (_: Exception) {}

        return newStreakData
    }

    suspend fun getCurrentStreak(uid: String): Int {
        val db = firestore ?: return 0
        try {
            val snapshot = db.collection("stats").document(uid).get().await()
            if (snapshot.exists()) {
                val streak = snapshot.getLong("currentStreak")?.toInt() ?: snapshot.getLong("studyStreak")?.toInt() ?: 0
                val lastStudyDate = snapshot.getLong("lastStudyDate") ?: 0L

                if (lastStudyDate > 0L) {
                    val lastDate = Instant.ofEpochMilli(lastStudyDate).atZone(ZoneId.systemDefault()).toLocalDate()
                    val nowDate = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
                    val daysBetween = ChronoUnit.DAYS.between(lastDate, nowDate)

                    if (daysBetween > 1L) {
                        return 0 // Reset if missed
                    }
                }
                return streak
            }
        } catch (_: Exception) {}
        return 0
    }

    suspend fun getLongestStreak(uid: String): Int {
        val db = firestore ?: return 0
        try {
            val snapshot = db.collection("stats").document(uid).get().await()
            if (snapshot.exists()) {
                return snapshot.getLong("longestStreak")?.toInt() ?: snapshot.getLong("currentStreak")?.toInt() ?: 0
            }
        } catch (_: Exception) {}
        return 0
    }

    suspend fun hasStudiedToday(
        uid: String,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Boolean {
        val db = firestore ?: return false
        try {
            val snapshot = db.collection("stats").document(uid).get().await()
            if (snapshot.exists()) {
                val lastStudyDate = snapshot.getLong("lastStudyDate") ?: 0L
                if (lastStudyDate > 0L) {
                    val lastDate = Instant.ofEpochMilli(lastStudyDate).atZone(ZoneId.systemDefault()).toLocalDate()
                    val nowDate = Instant.ofEpochMilli(currentTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                    return ChronoUnit.DAYS.between(lastDate, nowDate) == 0L
                }
            }
        } catch (_: Exception) {}
        return false
    }
}
