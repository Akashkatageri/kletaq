package com.studyos.app.domain.pipeline

import com.studyos.app.data.database.SyllabusDatabase
import com.studyos.app.data.model.Confidence
import com.studyos.app.data.model.QuestSection
import com.studyos.app.data.model.QuestSectionType
import com.studyos.app.data.model.SourceType
import com.studyos.app.data.model.SubjectType
import com.studyos.app.domain.quest.AcademicQuestGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicQuestPipelineTest {

    // ━━━━━━━━━━━━━━━━ 1. Hallucination Prevention & Zero-Invented Content ━━━━━━━━━━━━━━━━

    @Test
    fun testHallucinationPrevention_NoInventedContent() {
        val quest = AcademicQuestGenerator.generateQuestForTopic(
            topicId = "pd_01",
            topicTitle = "Partial Differentiation",
            subjectName = "Engineering Mathematics II",
            semesterName = "Semester 2"
        )

        // Every section must be backed by a real academic source
        quest.sections.forEach { section ->
            assertTrue(
                "Section '${section.title}' lacks source attribution",
                section.source.isNotBlank()
            )
            assertTrue(
                "Section '${section.title}' has invalid LOW confidence",
                section.confidence != Confidence.LOW
            )
        }
    }

    // ━━━━━━━━━━━━━━━━ 2. Syllabus Scope Filtering (Allowed vs Forbidden) ━━━━━━━━━━━━━━━━

    @Test
    fun testSyllabusFilter_RejectsForbiddenConcepts() {
        val syllabusChunk = SyllabusDatabase.getSyllabusChunk(
            topicId = "pd_01",
            topicTitle = "Partial Differentiation",
            subjectName = "Engineering Mathematics II"
        )

        val rawConcepts = listOf(
            "Partial derivatives",
            "Chain rule",
            "Green's theorem", // Forbidden
            "Jacobian matrix",
            "Tensor calculus" // Forbidden
        )

        val filterResult = SyllabusFilter.filterConcepts(rawConcepts, syllabusChunk)

        assertEquals(3, filterResult.allowedConcepts.size)
        assertTrue(filterResult.allowedConcepts.contains("Partial derivatives"))
        assertTrue(filterResult.allowedConcepts.contains("Chain rule"))

        assertEquals(2, filterResult.rejectedConcepts.size)
        assertTrue(filterResult.rejectedConcepts.contains("Green's theorem"))
        assertTrue(filterResult.rejectedConcepts.contains("Tensor calculus"))

        assertFalse(filterResult.isSyllabusCompliant)
    }

    // ━━━━━━━━━━━━━━━━ 3. Confidence Level Scoring ━━━━━━━━━━━━━━━━

    @Test
    fun testConfidenceScoring_HighMediumLow() {
        // In Syllabus AND in PYQ -> HIGH 🟢
        val highConf = ConfidenceScorer.calculateConfidence(
            isInSyllabus = true,
            hasPYQOccurrence = true,
            sourceType = SourceType.VTU_PYQ
        )
        assertEquals(Confidence.HIGH, highConf)

        // In Syllabus ONLY -> MEDIUM 🟡
        val medConf = ConfidenceScorer.calculateConfidence(
            isInSyllabus = true,
            hasPYQOccurrence = false,
            sourceType = SourceType.OFFICIAL_SYLLABUS
        )
        assertEquals(Confidence.MEDIUM, medConf)

        // External ONLY -> LOW 🔴
        val lowConf = ConfidenceScorer.calculateConfidence(
            isInSyllabus = false,
            hasPYQOccurrence = false,
            sourceType = SourceType.YOUTUBE_LECTURE
        )
        assertEquals(Confidence.LOW, lowConf)
    }

    // ━━━━━━━━━━━━━━━━ 4. Quest Validator Certification ━━━━━━━━━━━━━━━━

    @Test
    fun testQuestValidator_CertifiesValidQuests() {
        val validQuest = AcademicQuestGenerator.generateQuestForTopic(
            topicId = "dsa_recursion_memo",
            topicTitle = "Recursion and Memoization",
            subjectName = "Python Programming",
            semesterName = "Semester 1"
        )

        val result = QuestValidator.validateQuest(validQuest)
        assertTrue("Quest validation failed: ${result.validationErrors}", result.isValid)
    }

    @Test
    fun testQuestValidator_FlagsLowConfidenceOrMissingSource() {
        val invalidQuest = AcademicQuestGenerator.generateQuestForTopic(
            topicId = "phys_wo",
            topicTitle = "Wave Optics",
            subjectName = "Engineering Physics",
            semesterName = "Semester 1"
        )

        val corruptedSections = invalidQuest.sections + listOf(
            QuestSection(
                id = "bad_sec",
                title = "Bad External Section",
                type = QuestSectionType.CONCEPTS,
                source = "", // Missing source
                sourceType = SourceType.YOUTUBE_LECTURE,
                confidence = Confidence.LOW // Low confidence
            )
        )

        val badQuest = invalidQuest.copy(sections = corruptedSections)
        val result = QuestValidator.validateQuest(badQuest)

        assertFalse(result.isValid)
        assertTrue(result.validationErrors.isNotEmpty())
    }

    // ━━━━━━━━━━━━━━━━ 5. Subject Classification ━━━━━━━━━━━━━━━━

    @Test
    fun testSubjectClassification_AllTypes() {
        assertEquals(SubjectType.MATHEMATICS, AcademicQuestGenerator.detectSubjectType("Engineering Mathematics II"))
        assertEquals(SubjectType.PROGRAMMING, AcademicQuestGenerator.detectSubjectType("Python Programming"))
        assertEquals(SubjectType.PHYSICS, AcademicQuestGenerator.detectSubjectType("Engineering Physics"))
        assertEquals(SubjectType.CHEMISTRY, AcademicQuestGenerator.detectSubjectType("Engineering Chemistry"))
        assertEquals(SubjectType.THEORY, AcademicQuestGenerator.detectSubjectType("Indian Constitution"))
    }
}
