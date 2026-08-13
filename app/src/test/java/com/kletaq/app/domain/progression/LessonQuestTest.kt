package com.kletaq.app.domain.progression

import com.kletaq.app.data.model.DynamicTopicQuest
import com.kletaq.app.data.model.QuestSection
import com.kletaq.app.data.model.QuestSectionType
import com.kletaq.app.data.model.SourceType
import com.kletaq.app.data.model.SubjectType
import com.kletaq.app.domain.quest.AcademicQuestGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonQuestTest {

    @Test
    fun testQuestProgressCalculation() {
        val sections = listOf(
            QuestSection("s1", "Concepts", QuestSectionType.CONCEPTS, source = "VTU Syllabus", sourceType = SourceType.OFFICIAL_SYLLABUS, isCompleted = true),
            QuestSection("s2", "Formulas", QuestSectionType.FORMULAS, source = "VTU Syllabus", sourceType = SourceType.OFFICIAL_SYLLABUS, isCompleted = true),
            QuestSection("s3", "PYQs", QuestSectionType.VTU_PYQS, source = "VTU Exam Papers", sourceType = SourceType.VTU_PYQ, isCompleted = false),
            QuestSection("s4", "Practice", QuestSectionType.PRACTICE_PROBLEMS, source = "VTU Model Papers", sourceType = SourceType.OFFICIAL_SYLLABUS, isCompleted = false)
        )

        val quest = DynamicTopicQuest(
            topicId = "topic_math",
            title = "Partial Differentiation",
            subjectName = "Math",
            semesterName = "Sem 2",
            subjectType = SubjectType.MATHEMATICS,
            sections = sections
        )

        assertEquals(2, quest.completedCount)
        assertEquals(4, quest.totalCount)
        assertEquals(0.5f, quest.progressPercentage, 0.001f)
        assertFalse(quest.isFullyMastered)
    }

    @Test
    fun testQuestFullMastery() {
        val sections = listOf(
            QuestSection("s1", "Concepts", QuestSectionType.CONCEPTS, source = "VTU Syllabus", sourceType = SourceType.OFFICIAL_SYLLABUS, isCompleted = true),
            QuestSection("s2", "Formulas", QuestSectionType.FORMULAS, source = "VTU Syllabus", sourceType = SourceType.OFFICIAL_SYLLABUS, isCompleted = true)
        )

        val quest = DynamicTopicQuest(
            topicId = "topic_math",
            title = "Partial Differentiation",
            subjectName = "Math",
            semesterName = "Sem 2",
            subjectType = SubjectType.MATHEMATICS,
            sections = sections
        )

        assertEquals(2, quest.completedCount)
        assertEquals(1.0f, quest.progressPercentage, 0.001f)
        assertTrue(quest.isFullyMastered)
    }

    // ━━━━━━━━━━━━━━━━ Subject Type Detection Tests ━━━━━━━━━━━━━━━━

    @Test
    fun testSubjectDetection_Mathematics() {
        assertEquals(SubjectType.MATHEMATICS, AcademicQuestGenerator.detectSubjectType("Engineering Mathematics I"))
        assertEquals(SubjectType.MATHEMATICS, AcademicQuestGenerator.detectSubjectType("Calculus and Linear Algebra"))
        assertEquals(SubjectType.MATHEMATICS, AcademicQuestGenerator.detectSubjectType("Transform Calculus"))
    }

    @Test
    fun testSubjectDetection_Programming() {
        assertEquals(SubjectType.PROGRAMMING, AcademicQuestGenerator.detectSubjectType("Python Programming"))
        assertEquals(SubjectType.PROGRAMMING, AcademicQuestGenerator.detectSubjectType("C Programming"))
        assertEquals(SubjectType.PROGRAMMING, AcademicQuestGenerator.detectSubjectType("Data Structures and Algorithms"))
    }

    @Test
    fun testSubjectDetection_Physics() {
        assertEquals(SubjectType.PHYSICS, AcademicQuestGenerator.detectSubjectType("Engineering Physics"))
        assertEquals(SubjectType.PHYSICS, AcademicQuestGenerator.detectSubjectType("Quantum Mechanics"))
    }

    @Test
    fun testSubjectDetection_Chemistry() {
        assertEquals(SubjectType.CHEMISTRY, AcademicQuestGenerator.detectSubjectType("Engineering Chemistry"))
        assertEquals(SubjectType.CHEMISTRY, AcademicQuestGenerator.detectSubjectType("Electrochemistry and Corrosion"))
    }

    @Test
    fun testSubjectDetection_Theory() {
        assertEquals(SubjectType.THEORY, AcademicQuestGenerator.detectSubjectType("Professional Ethics"))
        assertEquals(SubjectType.THEORY, AcademicQuestGenerator.detectSubjectType("Indian Constitution"))
    }
}
