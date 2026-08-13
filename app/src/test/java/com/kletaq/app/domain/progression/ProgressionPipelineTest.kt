package com.kletaq.app.domain.progression

import com.kletaq.app.data.model.UserStats
import com.kletaq.app.domain.streak.StreakManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProgressionPipelineTest {

    private val zoneId = ZoneId.of("UTC")

    private fun localDateToTimestamp(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .plusHours(hour.toLong())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Simulation of the completeStudySession pipeline logic for unit testing pure state transitions.
     */
    private fun simulateCompleteStudySession(
        current: UserStats,
        processedSessionIds: MutableSet<String>,
        sessionId: String,
        xpAmount: Long,
        focusMinutes: Int,
        nowTimestamp: Long
    ): UserStats {
        if (processedSessionIds.contains(sessionId)) {
            // Idempotent return - no changes applied for duplicate session IDs
            return current
        }

        // 1. Calculate streak BEFORE updating lastStudyDate
        val streakResult = StreakManager.computeNextStreak(
            currentStreak = current.studyStreak,
            longestStreak = current.longestStreak,
            lastStudyDate = current.lastStudyDate,
            nowTimestamp = nowTimestamp,
            zoneId = zoneId
        )

        // 2. Calculate XP & level
        val newTotalXp = current.totalXp + xpAmount
        val newLevel = ProgressionCalculator.calculateLevel(newTotalXp)

        val updated = current.copy(
            totalXp = newTotalXp,
            currentLevel = newLevel,
            studyStreak = streakResult.currentStreak,
            longestStreak = streakResult.longestStreak,
            lastStudyDate = streakResult.lastStudyDate,
            totalFocusMinutes = current.totalFocusMinutes + focusMinutes,
            totalStudyMinutes = current.totalStudyMinutes + focusMinutes,
            studySessions = current.studySessions + 1
        )

        processedSessionIds.add(sessionId)
        return updated
    }

    @Test
    fun testFirstStudySession_IncrementsStreakToOneAndAwardsXP() {
        val processedSessions = mutableSetOf<String>()
        val initialStats = UserStats()
        val timestamp = localDateToTimestamp(2026, 8, 1)

        val result = simulateCompleteStudySession(
            current = initialStats,
            processedSessionIds = processedSessions,
            sessionId = "session_1",
            xpAmount = 50L,
            focusMinutes = 25,
            nowTimestamp = timestamp
        )

        assertEquals(1, result.studyStreak)
        assertEquals(1, result.longestStreak)
        assertEquals(50L, result.totalXp)
        assertEquals(25, result.totalFocusMinutes)
        assertEquals(1, result.studySessions)
        assertEquals(timestamp, result.lastStudyDate)
    }

    @Test
    fun testSameDayMultipleCompletions_KeepsStreakUnchangedAndAwardsXP() {
        val processedSessions = mutableSetOf<String>()
        val initialStats = UserStats()
        val morningTime = localDateToTimestamp(2026, 8, 1, hour = 9)
        val eveningTime = localDateToTimestamp(2026, 8, 1, hour = 20)

        // Morning completion
        val morningStats = simulateCompleteStudySession(
            current = initialStats,
            processedSessionIds = processedSessions,
            sessionId = "session_morning",
            xpAmount = 50L,
            focusMinutes = 25,
            nowTimestamp = morningTime
        )
        assertEquals(1, morningStats.studyStreak)
        assertEquals(50L, morningStats.totalXp)

        // Evening completion on the same day
        val eveningStats = simulateCompleteStudySession(
            current = morningStats,
            processedSessionIds = processedSessions,
            sessionId = "session_evening",
            xpAmount = 30L,
            focusMinutes = 15,
            nowTimestamp = eveningTime
        )

        // Streak MUST remain 1 on same day
        assertEquals(1, eveningStats.studyStreak)
        assertEquals(1, eveningStats.longestStreak)
        // XP and focus minutes accumulate cleanly
        assertEquals(80L, eveningStats.totalXp)
        assertEquals(40, eveningStats.totalFocusMinutes)
        assertEquals(2, eveningStats.studySessions)
    }

    @Test
    fun testConsecutiveDayCompletion_IncrementsStreakToTwo() {
        val processedSessions = mutableSetOf<String>()
        val initialStats = UserStats()
        val day1Time = localDateToTimestamp(2026, 8, 1)
        val day2Time = localDateToTimestamp(2026, 8, 2)

        val day1Stats = simulateCompleteStudySession(
            current = initialStats,
            processedSessionIds = processedSessions,
            sessionId = "session_day1",
            xpAmount = 50L,
            focusMinutes = 25,
            nowTimestamp = day1Time
        )
        assertEquals(1, day1Stats.studyStreak)

        val day2Stats = simulateCompleteStudySession(
            current = day1Stats,
            processedSessionIds = processedSessions,
            sessionId = "session_day2",
            xpAmount = 50L,
            focusMinutes = 25,
            nowTimestamp = day2Time
        )

        // Streak MUST increment to 2 on consecutive day
        assertEquals(2, day2Stats.studyStreak)
        assertEquals(2, day2Stats.longestStreak)
        assertEquals(100L, day2Stats.totalXp)
    }

    @Test
    fun testMissedDayReset_ResetsStreakToOneAndPreservesLongestStreak() {
        val processedSessions = mutableSetOf<String>()
        val day1Time = localDateToTimestamp(2026, 8, 1)
        val day4Time = localDateToTimestamp(2026, 8, 4) // Missed day 2 & 3

        val highStreakStats = UserStats(
            studyStreak = 10,
            longestStreak = 15,
            lastStudyDate = day1Time,
            totalXp = 500L
        )

        val result = simulateCompleteStudySession(
            current = highStreakStats,
            processedSessionIds = processedSessions,
            sessionId = "session_after_break",
            xpAmount = 50L,
            focusMinutes = 25,
            nowTimestamp = day4Time
        )

        assertEquals(1, result.studyStreak) // Reset to 1 after missed day
        assertEquals(15, result.longestStreak) // Preserves historical longest streak!
        assertEquals(550L, result.totalXp)
    }

    @Test
    fun testDuplicateSessionId_IdempotencyPreventsDoubleRewards() {
        val processedSessions = mutableSetOf<String>()
        val initialStats = UserStats()
        val timestamp = localDateToTimestamp(2026, 8, 1)
        val sessionId = "unique_session_123"

        val firstPass = simulateCompleteStudySession(
            current = initialStats,
            processedSessionIds = processedSessions,
            sessionId = sessionId,
            xpAmount = 100L,
            focusMinutes = 30,
            nowTimestamp = timestamp
        )
        assertEquals(100L, firstPass.totalXp)
        assertEquals(1, firstPass.studySessions)

        // Duplicate retry call with exact same sessionId
        val secondPass = simulateCompleteStudySession(
            current = firstPass,
            processedSessionIds = processedSessions,
            sessionId = sessionId,
            xpAmount = 100L,
            focusMinutes = 30,
            nowTimestamp = timestamp
        )

        // Must NOT double-award XP or sessions
        assertEquals(100L, secondPass.totalXp)
        assertEquals(1, secondPass.studySessions)
        assertEquals(firstPass, secondPass)
    }

    @Test
    fun testXPAndStreakUpdatedTogetherInSingleState() {
        val processedSessions = mutableSetOf<String>()
        val day1 = localDateToTimestamp(2026, 8, 1)
        val day2 = localDateToTimestamp(2026, 8, 2)

        val day1Stats = simulateCompleteStudySession(
            current = UserStats(),
            processedSessionIds = processedSessions,
            sessionId = "s1",
            xpAmount = 100L,
            focusMinutes = 20,
            nowTimestamp = day1
        )

        val day2Stats = simulateCompleteStudySession(
            current = day1Stats,
            processedSessionIds = processedSessions,
            sessionId = "s2",
            xpAmount = 150L,
            focusMinutes = 30,
            nowTimestamp = day2
        )

        assertEquals(2, day2Stats.studyStreak)
        assertEquals(250L, day2Stats.totalXp)
        assertEquals(2, day2Stats.studySessions)
        assertEquals(day2, day2Stats.lastStudyDate)
    }
}
