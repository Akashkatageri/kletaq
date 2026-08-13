package com.kletaq.app.domain.backlog

import com.kletaq.app.data.model.BacklogPlan
import com.kletaq.app.features.journey.components.LessonCategory
import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.LessonStatus
import com.kletaq.app.features.journey.components.SubjectJourney
import com.kletaq.app.features.journey.components.UnitJourney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BacklogPlanCalculatorTest {

    private fun createSampleSubject(): SubjectJourney {
        val u1Lessons = listOf(
            LessonNode("u1_l1", 1, "Topic 1.1", "", "Desc 1", LessonStatus.AVAILABLE, LessonCategory.CODING),
            LessonNode("u1_l2", 2, "Topic 1.2", "", "Desc 2", LessonStatus.LOCKED, LessonCategory.CODING)
        )
        val u2Lessons = listOf(
            LessonNode("u2_l1", 1, "Topic 2.1", "", "Desc 3", LessonStatus.LOCKED, LessonCategory.THEORY),
            LessonNode("u2_l2", 2, "Topic 2.2", "", "Desc 4", LessonStatus.LOCKED, LessonCategory.THEORY)
        )
        val units = listOf(
            UnitJourney("unit1", 1, "Unit 1 Title", true, u1Lessons),
            UnitJourney("unit2", 2, "Unit 2 Title", true, u2Lessons)
        )
        return SubjectJourney("sub1", "Data Structures", "📚", 0, 4, isBacklog = true, units = units)
    }

    @Test
    fun testFindNextUnfinishedTopic_returnsFirstUnfinishedLesson() {
        val subject = createSampleSubject()
        val plan = BacklogPlan(
            id = "plan1",
            subjectId = "sub1",
            subjectName = "Data Structures",
            selectedUnitIds = listOf("unit1", "unit2")
        )

        val nextTopic = BacklogPlanCalculator.findNextUnfinishedTopic(plan, subject)
        assertNotNull(nextTopic)
        assertEquals("unit1", nextTopic?.unitId)
        assertEquals("u1_l1", nextTopic?.topicId)
    }

    @Test
    fun testFindNextUnfinishedTopic_skipsCompletedLesson() {
        val subject = createSampleSubject()
        val plan = BacklogPlan(
            id = "plan1",
            subjectId = "sub1",
            subjectName = "Data Structures",
            selectedUnitIds = listOf("unit1", "unit2"),
            completedTopicIds = listOf("u1_l1")
        )

        val nextTopic = BacklogPlanCalculator.findNextUnfinishedTopic(plan, subject)
        assertNotNull(nextTopic)
        assertEquals("unit1", nextTopic?.unitId)
        assertEquals("u1_l2", nextTopic?.topicId)
    }

    @Test
    fun testFindNextUnfinishedTopic_respectsSelectedUnits() {
        val subject = createSampleSubject()
        val plan = BacklogPlan(
            id = "plan1",
            subjectId = "sub1",
            subjectName = "Data Structures",
            selectedUnitIds = listOf("unit2") // only Unit 2 selected
        )

        val nextTopic = BacklogPlanCalculator.findNextUnfinishedTopic(plan, subject)
        assertNotNull(nextTopic)
        assertEquals("unit2", nextTopic?.unitId)
        assertEquals("u2_l1", nextTopic?.topicId)
    }

    @Test
    fun testCalculatePlanProgress_mergesCompletedSources() {
        val subject = createSampleSubject()
        val plan = BacklogPlan(
            id = "plan1",
            subjectId = "sub1",
            subjectName = "Data Structures",
            selectedUnitIds = listOf("unit1", "unit2"),
            completedTopicIds = listOf("u1_l1")
        )
        // u1_l1 completed in plan, u2_l1 completed in global completedTopicKeys
        val completedTopicKeys = setOf("u2_l1")

        val result = BacklogPlanCalculator.calculatePlanProgress(plan, subject, completedTopicKeys = completedTopicKeys)
        assertEquals(4, result.totalCount)
        assertEquals(2, result.completedCount)
        assertEquals(0.5f, result.progressFraction, 0.001f)
    }

    @Test
    fun testCalculatePlanProgress_allCompleted() {
        val subject = createSampleSubject()
        val plan = BacklogPlan(
            id = "plan1",
            subjectId = "sub1",
            subjectName = "Data Structures",
            selectedUnitIds = listOf("unit1"),
            completedTopicIds = listOf("u1_l1", "u1_l2")
        )

        val result = BacklogPlanCalculator.calculatePlanProgress(plan, subject)
        assertEquals(2, result.totalCount)
        assertEquals(2, result.completedCount)
        assertEquals(1.0f, result.progressFraction, 0.001f)
        assertNull(BacklogPlanCalculator.findNextUnfinishedTopic(plan, subject))
    }
}
