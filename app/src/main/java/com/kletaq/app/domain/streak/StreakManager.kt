package com.kletaq.app.domain.streak

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.Locale

data class StreakResult(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStudyDate: Long = 0L,
    val earnedToday: Boolean = false,
    val shieldsRemaining: Int = 0,
    val shieldsConsumed: Int = 0,
    val wasShieldUsed: Boolean = false
)

typealias StreakData = StreakResult

/**
 * Pure calculation utility for study streaks and shields in Kletaq.
 * Side-effect free, no Firebase/Firestore dependency.
 */
object StreakManager {

    const val WEEKLY_FREE_SHIELDS = 2
    const val MAX_SHIELD_CAP = 3

    /**
     * Computes the new streak state when a study session is completed.
     *
     * Rules:
     * - First completion ever: Streak becomes 1, longest 1, 0 shields used.
     * - Same day (daysBetween == 0): Streak unchanged, 0 shields used.
     * - Consecutive day (daysBetween == 1): Streak + 1, 0 shields used.
     * - Missed day(s) (daysBetween >= 2):
     *     - If user had an active streak (currentStreak > 0) and enough shields (shieldsRemaining >= missedDays):
     *         Streak is preserved and incremented: currentStreak + 1
     *         Shields consumed: missedDays
     *         Shields remaining: shieldsRemaining - missedDays
     *         wasShieldUsed: true
     *     - If currentStreak <= 0:
     *         User is starting Day 1 (never started or streak was 0).
     *         Shields are NEVER consumed! 0 shields consumed. Streak becomes 1.
     *     - If currentStreak > 0 but insufficient shields (shieldsRemaining < missedDays):
     *         Streak breaks and resets to 1 (new streak starting today).
     *         Shields depleted to 0.
     */
    fun computeNextStreak(
        currentStreak: Int,
        longestStreak: Int,
        lastStudyDate: Long,
        shieldsRemaining: Int = 0,
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakResult {
        if (lastStudyDate <= 0L) {
            // First completion ever
            return StreakResult(
                currentStreak = 1,
                longestStreak = maxOf(longestStreak, 1),
                lastStudyDate = nowTimestamp,
                earnedToday = true,
                shieldsRemaining = shieldsRemaining,
                shieldsConsumed = 0,
                wasShieldUsed = false
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
                    earnedToday = false,
                    shieldsRemaining = shieldsRemaining,
                    shieldsConsumed = 0,
                    wasShieldUsed = false
                )
            }
            daysBetween == 1L -> {
                // Consecutive day completion -> streak + 1
                val newStreak = (if (currentStreak <= 0) 0 else currentStreak) + 1
                StreakResult(
                    currentStreak = newStreak,
                    longestStreak = maxOf(longestStreak, newStreak),
                    lastStudyDate = nowTimestamp,
                    earnedToday = true,
                    shieldsRemaining = shieldsRemaining,
                    shieldsConsumed = 0,
                    wasShieldUsed = false
                )
            }
            else -> {
                // Missed one or more days
                val missedDays = (daysBetween - 1).toInt()
                if (currentStreak > 0 && shieldsRemaining >= missedDays) {
                    // Protected by streak shield(s)!
                    val newStreak = currentStreak + 1
                    val newShields = shieldsRemaining - missedDays
                    StreakResult(
                        currentStreak = newStreak,
                        longestStreak = maxOf(longestStreak, newStreak),
                        lastStudyDate = nowTimestamp,
                        earnedToday = true,
                        shieldsRemaining = newShields,
                        shieldsConsumed = missedDays,
                        wasShieldUsed = true
                    )
                } else if (currentStreak <= 0) {
                    // Zero-streak rule: User never started or streak was already 0.
                    // Shields are NEVER consumed!
                    StreakResult(
                        currentStreak = 1,
                        longestStreak = maxOf(longestStreak, 1),
                        lastStudyDate = nowTimestamp,
                        earnedToday = true,
                        shieldsRemaining = shieldsRemaining,
                        shieldsConsumed = 0,
                        wasShieldUsed = false
                    )
                } else {
                    // Shields depleted or insufficient -> streak resets to 1 starting today
                    StreakResult(
                        currentStreak = 1,
                        longestStreak = maxOf(longestStreak, 1),
                        lastStudyDate = nowTimestamp,
                        earnedToday = true,
                        shieldsRemaining = 0,
                        shieldsConsumed = 0,
                        wasShieldUsed = false
                    )
                }
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

    fun isStreakActiveToday(
        lastStudyDate: Long,
        currentTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean = hasStudiedToday(lastStudyDate, currentTimestamp, zoneId)

    /**
     * Duolingo-style streak evaluation:
     * - daysBetween == 0: Studied today -> returns currentStreak
     * - daysBetween == 1: Studied yesterday, haven't studied yet today -> returns currentStreak (shown greyed out)
     * - daysBetween >= 2:
     *     - If user has active streak and sufficient shields: returns currentStreak (protected by shield!)
     *     - If shields depleted or currentStreak <= 0: returns 0
     */
    fun getEffectiveStreak(
        currentStreak: Int,
        lastStudyDate: Long,
        shieldsRemaining: Int = 0,
        currentTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        if (currentStreak <= 0 || lastStudyDate <= 0L) return 0
        val lastDate = Instant.ofEpochMilli(lastStudyDate).atZone(zoneId).toLocalDate()
        val nowDate = Instant.ofEpochMilli(currentTimestamp).atZone(zoneId).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(lastDate, nowDate)

        return when {
            daysBetween == 0L -> currentStreak
            daysBetween == 1L -> currentStreak
            daysBetween >= 2L -> {
                val missedDays = (daysBetween - 1).toInt()
                if (shieldsRemaining >= missedDays) {
                    currentStreak // Streak preserved by shields!
                } else {
                    0 // Truly expired
                }
            }
            else -> 0
        }
    }

    /**
     * Determines whether streak is truly expired and cannot be salvaged by shields.
     * Returns true ONLY if days missed exceed available shields and an active streak existed.
     */
    fun isStreakExpired(
        lastStudyDate: Long,
        shieldsRemaining: Int = 0,
        currentTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (lastStudyDate <= 0L) return false
        val lastDate = Instant.ofEpochMilli(lastStudyDate).atZone(zoneId).toLocalDate()
        val nowDate = Instant.ofEpochMilli(currentTimestamp).atZone(zoneId).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(lastDate, nowDate)
        if (daysBetween < 2L) return false
        val missedDays = (daysBetween - 1).toInt()
        return shieldsRemaining < missedDays
    }

    /**
     * Returns the ISO-8601 week string, e.g. "2026-W42".
     * ISO weeks start on Monday.
     */
    fun getCurrentIsoWeek(
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val localDate = Instant.ofEpochMilli(nowTimestamp).atZone(zoneId).toLocalDate()
        val weekBasedYear = localDate.get(IsoFields.WEEK_BASED_YEAR)
        val weekOfWeekBasedYear = localDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return String.format(Locale.US, "%04d-W%02d", weekBasedYear, weekOfWeekBasedYear)
    }

    /**
     * Calculates weekly free shields grant:
     * - Returns null if user already received grant for this week.
     * - Auto-refills to MAX_SHIELD_CAP (3) whenever shields are at 1 or below (<= 1).
     * - Otherwise adds +2 shields capped at MAX_SHIELD_CAP (3).
     */
    fun calculateWeeklyShieldGrant(
        currentShields: Int,
        lastWeeklyShieldWeek: String,
        nowTimestamp: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<Int, String>? {
        val currentWeek = getCurrentIsoWeek(nowTimestamp, zoneId)
        if (lastWeeklyShieldWeek == currentWeek) {
            return null // Already granted this week
        }
        val newShieldCount = if (currentShields <= 1) {
            MAX_SHIELD_CAP
        } else {
            minOf(MAX_SHIELD_CAP, currentShields + WEEKLY_FREE_SHIELDS)
        }
        return Pair(newShieldCount, currentWeek)
    }
}

