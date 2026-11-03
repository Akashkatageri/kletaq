package com.kletaq.app.features.memorypalace

import com.kletaq.app.features.memorypalace.ai.MemoryPalaceAiHelper
import com.kletaq.app.features.memorypalace.data.MemoryPalaceRepository
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MemoryPalaceFlowchartTest {

    @Test
    fun testThreeTierRecall_examSprint() {
        val examTime = System.currentTimeMillis() + 86400000L

        // HARD rating at stage 1 -> stays at stage 1 with 15 min retry
        val (hardTime, hardStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "EXAM_SPRINT",
            currentStage = 1,
            examTimestamp = examTime,
            rating = MemoryPalaceRepository.RecallRating.HARD
        )
        assertEquals(1, hardStage)
        assertTrue(hardTime > System.currentTimeMillis() + 10 * 60 * 1000L)
        assertTrue(hardTime < System.currentTimeMillis() + 20 * 60 * 1000L)

        // GOOD rating at stage 0 -> advances to stage 1 (+30 min)
        val (goodTime, goodStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "EXAM_SPRINT",
            currentStage = 0,
            examTimestamp = examTime,
            rating = MemoryPalaceRepository.RecallRating.GOOD
        )
        assertEquals(1, goodStage)
        assertTrue(goodTime > System.currentTimeMillis() + 25 * 60 * 1000L)
        assertTrue(goodTime < System.currentTimeMillis() + 35 * 60 * 1000L)

        // EASY rating at stage 0 -> advances to stage 1 (+45 min, 1.5x interval bonus)
        val (easyTime, easyStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "EXAM_SPRINT",
            currentStage = 0,
            examTimestamp = examTime,
            rating = MemoryPalaceRepository.RecallRating.EASY
        )
        assertEquals(1, easyStage)
        assertTrue(easyTime > goodTime)
        assertTrue(easyTime > System.currentTimeMillis() + 40 * 60 * 1000L)
        assertTrue(easyTime < System.currentTimeMillis() + 50 * 60 * 1000L)
    }

    @Test
    fun testThreeTierRecall_semesterMastery() {
        // GOOD rating at stage 0 -> Day 1 (24 hours)
        val (goodTime, goodStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "SEMESTER",
            currentStage = 0,
            examTimestamp = null,
            rating = MemoryPalaceRepository.RecallRating.GOOD
        )
        assertEquals(1, goodStage)
        assertTrue(goodTime > System.currentTimeMillis() + 23 * 3600 * 1000L)

        // EASY rating at stage 0 -> Day 1.5 (36 hours, 1.5x interval bonus)
        val (easyTime, easyStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "SEMESTER",
            currentStage = 0,
            examTimestamp = null,
            rating = MemoryPalaceRepository.RecallRating.EASY
        )
        assertEquals(1, easyStage)
        assertTrue(easyTime > goodTime)
        assertTrue(easyTime > System.currentTimeMillis() + 35 * 3600 * 1000L)

        // HARD rating at stage 2 -> regresses to stage 1 with 4 hour retry
        val (hardTime, hardStage) = MemoryPalaceRepository.calculateNextReview(
            mode = "SEMESTER",
            currentStage = 2,
            examTimestamp = null,
            rating = MemoryPalaceRepository.RecallRating.HARD
        )
        assertEquals(1, hardStage)
        assertTrue(hardTime > System.currentTimeMillis() + 3 * 3600 * 1000L)
    }

    @Test
    fun testStepEditingAndPreviewState() {
        val initialStep = PalaceStep(
            id = UUID.randomUUID().toString(),
            derivationId = "test_deriv",
            stepNumber = 1,
            locusOrderIndex = 1,
            stepTitle = "Initial Postulate",
            formulaLatex = "E = mc^2",
            sensoryStory = "Old story",
            examTrap = "Old trap",
            pageIndex = 0
        )

        val updatedStep = initialStep.copy(
            stepTitle = "Refined Energy Postulate",
            sensoryStory = "At your desk, an Einstein statue glows neon purple.",
            formulaLatex = "E^2 = (pc)^2 + (m_0 c^2)^2"
        )

        assertEquals("Refined Energy Postulate", updatedStep.stepTitle)
        assertEquals("E^2 = (pc)^2 + (m_0 c^2)^2", updatedStep.formulaLatex)
        assertTrue(updatedStep.sensoryStory.contains("Einstein"))
    }
}

