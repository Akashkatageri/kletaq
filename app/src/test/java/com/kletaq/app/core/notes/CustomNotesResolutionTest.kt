package com.kletaq.app.core.notes

import com.kletaq.app.core.syllabus.SyllabusAiImporter
import com.kletaq.app.data.repository.AcademicContentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CustomNotesResolutionTest {

    @Test
    fun testCustomSubjectCodeResolutionDoesNotHijackToBuiltIn() {
        // 21MAT11 should resolve to 21MAT11, never hijacked to 1BCS301
        val code1 = AcademicContentRepository.resolveSubjectCode(
            topicId = "21MAT11_M1_T1",
            subjectCode = "21MAT11",
            subjectName = "Engineering Mathematics"
        )
        assertEquals("21MAT11", code1)

        // 18CS32 should resolve to 18CS32
        val code2 = AcademicContentRepository.resolveSubjectCode(
            topicId = "18CS32_M2_T3",
            subjectCode = "18CS32",
            subjectName = "Data Structures and Applications"
        )
        assertEquals("18CS32", code2)

        // Built-in aliases still normalize as expected
        val bcs301 = AcademicContentRepository.resolveSubjectCode(
            topicId = "1BCS301_M1_T1",
            subjectCode = "BCS301",
            subjectName = "Mathematics for Computer Science"
        )
        assertEquals("1BCS301", bcs301)
    }

    @Test
    fun testTopicIdPrefixResolution() {
        // When subjectCode is empty, but topicId has prefix like 21MAT11_M1_T1
        val code = AcademicContentRepository.resolveSubjectCode(
            topicId = "21MAT11_M1_T1",
            subjectCode = "",
            subjectName = ""
        )
        assertEquals("21MAT11", code)
    }

    @Test
    fun testSyllabusAiJsonParserCleansTopicTitles() {
        val json = """
            {
                "code": "21MAT11",
                "name": "Engineering Mathematics",
                "units": [
                    {
                        "unitNumber": 1,
                        "title": "Calculus and Linear Algebra",
                        "topics": [
                            { "title": "01. Partial Differentiation", "estimatedMinutes": 25 },
                            { "title": "Topic 2: Taylor Series", "estimatedMinutes": 30 },
                            { "title": "03 - Jacobians", "estimatedMinutes": 20 }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val parsed = SyllabusAiImporter.parseJsonToImportedSubject(json, 3)
        assertNotNull(parsed)
        assertEquals("21MAT11", parsed!!.code)
        assertEquals("Engineering Mathematics", parsed.name)
        assertEquals(1, parsed.units.size)
        assertEquals(3, parsed.units[0].topics.size)

        // Verifying topic prefixes are cleanly stripped
        assertEquals("Partial Differentiation", parsed.units[0].topics[0].title)
        assertEquals("Taylor Series", parsed.units[0].topics[1].title)
        assertEquals("Jacobians", parsed.units[0].topics[2].title)
    }

    @Test
    fun testFallbackParserDoesNotFabricateUnitsWhenSingleModule() {
        val text = """
            21MAT11
            Engineering Mathematics
            Module 1: Calculus
            Partial Differentiation
            Taylor Series
            Jacobians
        """.trimIndent()

        val parsed = SyllabusAiImporter.fallbackParseSyllabus(text, 3)
        assertEquals("21MAT11", parsed.code)
        assertEquals(1, parsed.units.size)
        assertEquals("Calculus", parsed.units[0].title)
        assertEquals(3, parsed.units[0].topics.size)
    }

    @Test
    fun testNotePageRetrieverWithUploadedNotesManifest() {
        val pages = listOf(
            PageOcrRecord(1, "p1.jpg", "fp1", "Engineering Mathematics partial derivatives Euler theorem", 6, true),
            PageOcrRecord(2, "p2.jpg", "fp2", "Taylor series expansion and Maclaurin series proof", 8, true),
            PageOcrRecord(3, "p3.jpg", "fp3", "Jacobians of two and three variables problem solutions", 8, true)
        )
        val manifest = ModuleOcrManifest("21MAT11", 1, System.currentTimeMillis(), pages)

        // Query for Jacobians
        val result = NotePageRetriever.retrieveTopPages(
            query = "How to solve Jacobians?",
            topicTitle = "Jacobians",
            manifest = manifest,
            maxPages = 2,
            topicId = "21MAT11_M1_T3"
        )
        assertEquals(true, result.hasSufficientEvidence)
        assertEquals(true, result.citedPageIndices.contains(3))
        assertEquals(true, result.formattedExcerpts.contains("[Page 3]"))
    }

    @Test
    fun testTopicChatSubjectIdResolution() {
        // Explicit subjectId takes precedence
        val explicit = com.kletaq.app.features.chat.TopicChatRepository.resolveSubjectId(
            topicId = "some_node_id",
            explicitSubjectId = "21MAT11"
        )
        assertEquals("21MAT11", explicit)

        // Custom topic ID format (e.g. 21MAT11_M1_T1) resolves custom code even without explicitSubjectId
        val inferred = com.kletaq.app.features.chat.TopicChatRepository.resolveSubjectId(
            topicId = "21MAT11_M1_T1",
            explicitSubjectId = null
        )
        assertEquals("21MAT11", inferred)
    }
}

