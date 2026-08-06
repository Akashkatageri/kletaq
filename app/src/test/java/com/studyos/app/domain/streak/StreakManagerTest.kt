package com.studyos.app.domain.streak

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StreakManagerTest {

    private val streakManager = StreakManager()
    private val zoneId = ZoneId.of("UTC")

    private fun localDateToTimestamp(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .plusHours(hour.toLong())
            .toInstant()
            .toEpochMilli()
    }

    // ━━━━━━━━━━━━━━━━ 1. First Study Day Test ━━━━━━━━━━━━━━━━

    @Test
    fun testFirstStudyDay_SetsStreakToOne() {
        val now = localDateToTimestamp(2026, 8, 1)

        val result = streakManager.computeNextStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastStudyDate = 0L,
            nowTimestamp = now,
            zoneId = zoneId
        )

        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
        assertEquals(now, result.lastStudyDate)
    }

    // ━━━━━━━━━━━━━━━━ 2. Consecutive Days Test ━━━━━━━━━━━━━━━━

    @Test
    fun testConsecutiveDays_IncrementsStreak() {
        val day1 = localDateToTimestamp(2026, 8, 1)
        val day2 = localDateToTimestamp(2026, 8, 2)
        val day3 = localDateToTimestamp(2026, 8, 3)

        // Day 1
        val res1 = streakManager.computeNextStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastStudyDate = 0L,
            nowTimestamp = day1,
            zoneId = zoneId
        )
        assertEquals(1, res1.currentStreak)
        assertEquals(1, res1.longestStreak)

        // Day 2
        val res2 = streakManager.computeNextStreak(
            currentStreak = res1.currentStreak,
            longestStreak = res1.longestStreak,
            lastStudyDate = res1.lastStudyDate,
            nowTimestamp = day2,
            zoneId = zoneId
        )
        assertEquals(2, res2.currentStreak)
        assertEquals(2, res2.longestStreak)

        // Day 3
        val res3 = streakManager.computeNextStreak(
            currentStreak = res2.currentStreak,
            longestStreak = res2.longestStreak,
            lastStudyDate = res2.lastStudyDate,
            nowTimestamp = day3,
            zoneId = zoneId
        )
        assertEquals(3, res3.currentStreak)
        assertEquals(3, res3.longestStreak)
    }

    // ━━━━━━━━━━━━━━━━ 3. Same-Day Completion Test ━━━━━━━━━━━━━━━━

    @Test
    fun testSameDayCompletion_DoesNotIncrementStreak() {
        val morning = localDateToTimestamp(2026, 8, 1, hour = 9)
        val evening = localDateToTimestamp(2026, 8, 1, hour = 20)

        // Morning completion
        val res1 = streakManager.computeNextStreak(
            currentStreak = 5,
            longestStreak = 10,
            lastStudyDate = localDateToTimestamp(2026, 7, 31),
            nowTimestamp = morning,
            zoneId = zoneId
        )
        assertEquals(6, res1.currentStreak)

        // Evening completion on the same day
        val res2 = streakManager.computeNextStreak(
            currentStreak = res1.currentStreak,
            longestStreak = res1.longestStreak,
            lastStudyDate = res1.lastStudyDate,
            nowTimestamp = evening,
            zoneId = zoneId
        )
        assertEquals(6, res2.currentStreak) // Must remain 6!
        assertEquals(10, res2.longestStreak)
        assertEquals(evening, res2.lastStudyDate)
    }

    // ━━━━━━━━━━━━━━━━ 4. Missed Day Reset Test ━━━━━━━━━━━━━━━━

    @Test
    fun testMissedDay_ResetsStreakToOneAndPreservesLongestStreak() {
        val lastStudy = localDateToTimestamp(2026, 8, 1)
        val afterBreak = localDateToTimestamp(2026, 8, 4) // Missed Aug 2 and Aug 3

        val result = streakManager.computeNextStreak(
            currentStreak = 7,
            longestStreak = 14,
            lastStudyDate = lastStudy,
            nowTimestamp = afterBreak,
            zoneId = zoneId
        )

        assertEquals(1, result.currentStreak) // Resets to 1
        assertEquals(14, result.longestStreak) // Preserves longest streak!
        assertEquals(afterBreak, result.lastStudyDate)
    }
}
