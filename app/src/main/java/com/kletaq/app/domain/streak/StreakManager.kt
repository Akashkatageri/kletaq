package com.kletaq.app.domain.streak

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class StreakResult(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStudyDate: Long = 0L,
    val earnedToday: Boolean = false
)

typealias StreakData = StreakResult

/**
 * Pure calculation utility for study streaks in Klytaq.
 * Side-effect free, no Firebase/Firestore dependency.
 */
object StreakManager {

    fun computeNextStreak(
        currentStreak: Int,
        longestStreak: Int,
        lastStudyDate: Long,
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakResult {
        if (lastStudyDate <= 0L) {
            // First completion ever
            return StreakResult(
                currentStreak = 1,
                longestStreak = maxOf(longestStreak, 1),
                lastStudyDate = nowTimestamp,
                earnedToday = true
            )
        }

        val lastDate = Instant.ofEpochMilli(lastStudyDate).atZone(zoneId).toLocalDate()
        val nowDate = Instant.ofEpochMilli(nowTimestamp).atZone(zoneId).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(lastDate, nowDate)

        return when {
            daysBetween == 0L -> {
                // Same day completion -> streak unchanged
                val streak = if (currentStreak <= 0) 1 else currentStreak
                StreakResult(
                    currentStreak = streak,
                    longestStreak = maxOf(longestStreak, streak),
                    lastStudyDate = nowTimestamp,
                    earnedToday = false
                )
            }
            daysBetween == 1L -> {
                // Consecutive day completion -> streak + 1
                val newStreak = (if (currentStreak <= 0) 0 else currentStreak) + 1
                StreakResult(
                    currentStreak = newStreak,
                    longestStreak = maxOf(longestStreak, newStreak),
                    lastStudyDate = nowTimestamp,
                    earnedToday = true
                )
            }
            else -> {
                // Missed one or more days -> reset streak to 1
                StreakResult(
                    currentStreak = 1,
                    longestStreak = maxOf(longestStreak, 1),
                    lastStudyDate = nowTimestamp,
                    earnedToday = true
                )
            }
        }
    }

    fun hasStudiedToday(
        lastStudyDate: Long,
        currentTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (lastStudyDate <= 0L) return false
        val lastDate = Instant.ofEpochMilli(lastStudyDate).atZone(zoneId).toLocalDate()
        val nowDate = Instant.ofEpochMilli(currentTimestamp).atZone(zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(lastDate, nowDate) == 0L
    }
}
