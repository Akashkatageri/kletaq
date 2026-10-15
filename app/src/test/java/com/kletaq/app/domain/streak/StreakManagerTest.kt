package com.kletaq.app.domain.streak

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StreakManagerTest {

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

        val result = StreakManager.computeNextStreak(
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
        val res1 = StreakManager.computeNextStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastStudyDate = 0L,
            nowTimestamp = day1,
            zoneId = zoneId
        )
        assertEquals(1, res1.currentStreak)
        assertEquals(1, res1.longestStreak)

        // Day 2
        val res2 = StreakManager.computeNextStreak(
            currentStreak = res1.currentStreak,
            longestStreak = res1.longestStreak,
            lastStudyDate = res1.lastStudyDate,
            nowTimestamp = day2,
            zoneId = zoneId
        )
        assertEquals(2, res2.currentStreak)
        assertEquals(2, res2.longestStreak)

        // Day 3
        val res3 = StreakManager.computeNextStreak(
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
        val res1 = StreakManager.computeNextStreak(
            currentStreak = 5,
            longestStreak = 10,
            lastStudyDate = localDateToTimestamp(2026, 7, 31),
            nowTimestamp = morning,
            zoneId = zoneId
        )
        assertEquals(6, res1.currentStreak)

        // Evening completion on the same day
        val res2 = StreakManager.computeNextStreak(
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

        val result = StreakManager.computeNextStreak(
            currentStreak = 7,
            longestStreak = 14,
            lastStudyDate = lastStudy,
            shieldsRemaining = 0, // No shields
            nowTimestamp = afterBreak,
            zoneId = zoneId
        )

        assertEquals(1, result.currentStreak) // Resets to 1
        assertEquals(14, result.longestStreak) // Preserves longest streak!
        assertEquals(afterBreak, result.lastStudyDate)
        assertEquals(0, result.shieldsRemaining)
        assertEquals(false, result.wasShieldUsed)
    }

    // ━━━━━━━━━━━━━━━━ 5. Shield Protection Tests ━━━━━━━━━━━━━━━━

    @Test
    fun testMissedDayWithShield_PreservesAndIncrementsStreak() {
        val lastStudy = localDateToTimestamp(2026, 8, 1)
        val studyDay = localDateToTimestamp(2026, 8, 3) // Missed Aug 2 (1 missed day)

        val result = StreakManager.computeNextStreak(
            currentStreak = 5,
            longestStreak = 10,
            lastStudyDate = lastStudy,
            shieldsRemaining = 2,
            nowTimestamp = studyDay,
            zoneId = zoneId
        )

        assertEquals(6, result.currentStreak) // Preserved & incremented!
        assertEquals(10, result.longestStreak)
        assertEquals(1, result.shieldsRemaining) // 1 shield consumed (2 - 1)
        assertEquals(1, result.shieldsConsumed)
        assertEquals(true, result.wasShieldUsed)
    }

    @Test
    fun testMissedTwoDaysWithTwoShields_PreservesStreak() {
        val lastStudy = localDateToTimestamp(2026, 8, 1)
        val studyDay = localDateToTimestamp(2026, 8, 4) // Missed Aug 2 & Aug 3 (2 missed days)

        val result = StreakManager.computeNextStreak(
            currentStreak = 8,
            longestStreak = 12,
            lastStudyDate = lastStudy,
            shieldsRemaining = 3,
            nowTimestamp = studyDay,
            zoneId = zoneId
        )

        assertEquals(9, result.currentStreak) // Preserved & incremented!
        assertEquals(12, result.longestStreak)
        assertEquals(1, result.shieldsRemaining) // 2 shields consumed (3 - 2)
        assertEquals(2, result.shieldsConsumed)
        assertEquals(true, result.wasShieldUsed)
    }

    @Test
    fun testMissedTwoDaysWithInsufficientShields_ResetsStreak() {
        val lastStudy = localDateToTimestamp(2026, 8, 1)
        val studyDay = localDateToTimestamp(2026, 8, 4) // Missed 2 days, but only 1 shield

        val result = StreakManager.computeNextStreak(
            currentStreak = 8,
            longestStreak = 12,
            lastStudyDate = lastStudy,
            shieldsRemaining = 1,
            nowTimestamp = studyDay,
            zoneId = zoneId
        )

        assertEquals(1, result.currentStreak) // Resets to 1
        assertEquals(12, result.longestStreak)
        assertEquals(0, result.shieldsRemaining) // Depleted
        assertEquals(false, result.wasShieldUsed)
    }

    // ━━━━━━━━━━━━━━━━ 6. Zero-Streak Immunity Test ━━━━━━━━━━━━━━━━

    @Test
    fun testZeroStreakImmunity_NeverConsumesShieldsWhenUserNeverStarted() {
        val lastStudy = localDateToTimestamp(2026, 8, 1)
        val studyDay = localDateToTimestamp(2026, 8, 10) // 9 days later

        val result = StreakManager.computeNextStreak(
            currentStreak = 0, // Streak is 0
            longestStreak = 0,
            lastStudyDate = lastStudy,
            shieldsRemaining = 3,
            nowTimestamp = studyDay,
            zoneId = zoneId
        )

        assertEquals(1, result.currentStreak)
        assertEquals(3, result.shieldsRemaining) // Shields MUST NOT be consumed!
        assertEquals(0, result.shieldsConsumed)
        assertEquals(false, result.wasShieldUsed)
    }

    // ━━━━━━━━━━━━━━━━ 7. Effective Streak with Shield Tests ━━━━━━━━━━━━━━━━

    @Test
    fun testEffectiveStreak_PreservedWhenShieldsAvailable() {
        val lastStudy = localDateToTimestamp(2026, 8, 1)
        val today = localDateToTimestamp(2026, 8, 3) // Missed yesterday

        val effectiveWithShield = StreakManager.getEffectiveStreak(
            currentStreak = 10,
            lastStudyDate = lastStudy,
            shieldsRemaining = 1,
            currentTimestamp = today,
            zoneId = zoneId
        )
        assertEquals(10, effectiveWithShield) // Protected by shield

        val effectiveWithoutShield = StreakManager.getEffectiveStreak(
            currentStreak = 10,
            lastStudyDate = lastStudy,
            shieldsRemaining = 0,
            currentTimestamp = today,
            zoneId = zoneId
        )
        assertEquals(0, effectiveWithoutShield) // Truly expired
    }

    // ━━━━━━━━━━━━━━━━ 8. Weekly Shield Grant Tests ━━━━━━━━━━━━━━━━

    @Test
    fun testWeeklyShieldGrant_AddsTwoShieldsCappedAtThree() {
        val now = localDateToTimestamp(2026, 8, 1) // ISO week 2026-W31
        val currentWeek = StreakManager.getCurrentIsoWeek(now, zoneId)

        // Grant from old week: 0 -> 2 shields
        val grant1 = StreakManager.calculateWeeklyShieldGrant(
            currentShields = 0,
            lastWeeklyShieldWeek = "2026-W30",
            nowTimestamp = now,
            zoneId = zoneId
        )
        org.junit.Assert.assertNotNull(grant1)
        assertEquals(2, grant1?.first)
        assertEquals(currentWeek, grant1?.second)

        // Grant from old week capped at 3: 2 -> 3 shields (capping at MAX_SHIELD_CAP = 3)
        val grant2 = StreakManager.calculateWeeklyShieldGrant(
            currentShields = 2,
            lastWeeklyShieldWeek = "2026-W30",
            nowTimestamp = now,
            zoneId = zoneId
        )
        org.junit.Assert.assertNotNull(grant2)
        assertEquals(3, grant2?.first) // Capped at MAX_SHIELD_CAP = 3

        // Same week: no duplicate grant
        val grantSameWeek = StreakManager.calculateWeeklyShieldGrant(
            currentShields = 4,
            lastWeeklyShieldWeek = currentWeek,
            nowTimestamp = now,
            zoneId = zoneId
        )
        org.junit.Assert.assertNull(grantSameWeek)
    }
}

