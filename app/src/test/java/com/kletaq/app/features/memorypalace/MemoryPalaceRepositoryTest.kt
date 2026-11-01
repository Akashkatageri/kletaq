package com.kletaq.app.features.memorypalace

import com.kletaq.app.features.memorypalace.data.MemoryPalaceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryPalaceRepositoryTest {

    @Test
    fun testExamSprintReviewProgression_cleanRecall() {
        val examTime = System.currentTimeMillis() + 86400000L // 24 hours from now

        // Stage 0 -> Stage 1 (+30 mins)
        val (nextTime1, nextStage1) = MemoryPalaceRepository.calculateNextReview(
            mode = "EXAM_SPRINT",
            currentStage = 0,
            examTimestamp = examTime,
            rememberedCleanly = true
        )
        assertEquals(1, nextStage1)
        assertTrue(nextTime1 > System.currentTimeMillis() + 25 * 60 * 1000L)
        assertTrue(nextTime1 < System.currentTimeMillis() + 35 * 60 * 1000L)

        // Stage 1 -> Stage 2 (Evening / pre-sleep)
        val (nextTime2, nextStage2) = MemoryPalaceRepository.calculateNextReview(
            mode = "EXAM_SPRINT",
            currentStage = 1,
            examTimestamp = examTime,
            rememberedCleanly = true
        )
        assertEquals(2, nextStage2)
        assertTrue(nextTime2 > nextTime1)

        // Stage 2 -> Stage 3 (Morning-of exam, 2 hours before)
        val (nextTime3, nextStage3) = MemoryPalaceRepository.calculateNextReview(
            mode = "EXAM_SPRINT",
            currentStage = 2,
            examTimestamp = examTime,
            rememberedCleanly = true
        )
        assertEquals(3, nextStage3)
        // Should be approximately examTime - 2 hours
        val twoHoursBefore = examTime - 2 * 3600 * 1000L
        assertTrue(Math.abs(nextTime3 - twoHoursBefore) < 60000L)
    }

    @Test
    fun testExamSprintReviewProgression_struggledReschedule() {
        val examTime = System.currentTimeMillis() + 86400000L

        // Struggled at stage 1 -> stays at stage 1 and sets quick 15-minute retry
        val (retryTime, retryStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "EXAM_SPRINT",
            currentStage = 1,
            examTimestamp = examTime,
            rememberedCleanly = false
        )
        assertEquals(1, retryStage)
        assertTrue(retryTime > System.currentTimeMillis() + 10 * 60 * 1000L)
        assertTrue(retryTime < System.currentTimeMillis() + 20 * 60 * 1000L)
    }

    @Test
    fun testSemesterMasteryProgression() {
        // Stage 0 -> Stage 1 (1 day)
        val (nextTime1, nextStage1) = MemoryPalaceRepository.calculateNextReview(
            mode = "SEMESTER",
            currentStage = 0,
            examTimestamp = null,
            rememberedCleanly = true
        )
        assertEquals(1, nextStage1)
        assertTrue(nextTime1 > System.currentTimeMillis() + 23 * 3600 * 1000L)

        // Stage 1 -> Stage 2 (3 days)
        val (nextTime2, nextStage2) = MemoryPalaceRepository.calculateNextReview(
            mode = "SEMESTER",
            currentStage = 1,
            examTimestamp = null,
            rememberedCleanly = true
        )
        assertEquals(2, nextStage2)
        assertTrue(nextTime2 > System.currentTimeMillis() + 2 * 24 * 3600 * 1000L)

        // Struggled at stage 2 -> regresses to stage 1 and sets 4-hour retry
        val (retryTime, retryStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "SEMESTER",
            currentStage = 2,
            examTimestamp = null,
            rememberedCleanly = false
        )
        assertEquals(1, retryStage)
        assertTrue(retryTime > System.currentTimeMillis() + 3 * 3600 * 1000L)
    }
}

