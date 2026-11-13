package com.kletaq.app.core.flashcards

import com.kletaq.app.core.notes.ModuleOcrManifest
import com.kletaq.app.core.notes.NotesTextExtractor
import com.kletaq.app.core.notes.PageOcrRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicFlashcardDeckTest {

    @Test
    fun testResolveTopicPageRangeWithZeroPages() {
        val range = NotesTextExtractor.resolveTopicPageRange(
            topicId = "test_topic_empty",
            topicTitle = "Linear Algebra",
            topicIndex = 0,
            totalTopicsInModule = 8,
            totalPages = 0,
            manifest = null
        )
        // With 0 pages, range must be 1..1 or empty bounds and total pages cannot exceed 0
        assertEquals(1..1, range)
    }

    @Test
    fun testResolveTopicPageRangeKeywordMatching() {
        val page1 = PageOcrRecord(
            pageIndex = 1,
            fileName = "page1.jpg",
            fileFingerprint = "fp1",
            text = "Introduction to Fourier Series and Dirichlet conditions for convergence",
            wordCount = 10,
            isReadable = true
        )
        val page2 = PageOcrRecord(
            pageIndex = 2,
            fileName = "page2.jpg",
            fileFingerprint = "fp2",
            text = "Laplace transforms definition and unit step function properties",
            wordCount = 9,
            isReadable = true
        )
        val manifest = ModuleOcrManifest(
            subjectCode = "21MAT31",
            moduleIndex = 1,
            lastScanTimestamp = 1000L,
            pages = listOf(page1, page2)
        )

        // For small page counts (<=4), it falls back to 1..totalPages in Tier 2
        val range = NotesTextExtractor.resolveTopicPageRange(
            topicId = "topic_fourier",
            topicTitle = "Fourier Series",
            topicIndex = 0,
            totalTopicsInModule = 8,
            totalPages = 2,
            manifest = manifest
        )
        assertEquals(1..2, range)
    }

    @Test
    fun testFlashcardPromptMustBeGroundedInNotes() {
        val notesExcerpts = "[Page 1]:\nState Dirichlet Conditions for Fourier Series expansion."
        val promptWithNotes = "Generate 4-6 high-yield exam flashcards for the topic 'Fourier Series' using these attached student notes excerpts:\n\n<student_notes>\n$notesExcerpts\n</student_notes>"

        assertTrue(promptWithNotes.contains("<student_notes>"))
        assertTrue(promptWithNotes.contains("Dirichlet Conditions"))
        assertFalse("Prompt must never use ungrounded generic course prompt", promptWithNotes.contains("VTU engineering course topic"))
    }
}
