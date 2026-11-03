package com.kletaq.app.features.flashcards

import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.model.CardFlag
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import com.kletaq.app.features.flashcards.model.FlashcardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashcardRepositoryTest {

    @Test
    fun testCardCRUD_SaveAndRetrieve() {
        val card = Flashcard(
            id = "test_c1",
            topicId = "topic_math_1",
            topicTitle = "Linear Algebra",
            front = "What is a matrix rank?",
            back = "Maximum number of linearly independent column vectors.",
            cardType = FlashcardType.BASIC
        )

        FlashcardRepository.saveCard(card)
        val retrieved = FlashcardRepository.getCard("test_c1")
        assertNotNull(retrieved)
        assertEquals("Linear Algebra", retrieved?.topicTitle)

        val topicCards = FlashcardRepository.getCardsForTopic("topic_math_1")
        assertTrue(topicCards.any { it.id == "test_c1" })

        FlashcardRepository.deleteCard("test_c1")
        assertNull(FlashcardRepository.getCard("test_c1"))
    }

    @Test
    fun testRecordReviewAndUndo() {
        val card = Flashcard(
            id = "test_undo_c1",
            topicId = "topic_cs_1",
            queueState = CardQueueState.NEW,
            reps = 0
        )
        FlashcardRepository.saveCard(card)

        // Record Good review
        val reviewed = FlashcardRepository.recordReview("test_undo_c1", FlashcardReviewRating.GOOD)
        assertEquals(CardQueueState.LEARNING, reviewed.queueState)
        assertEquals(1, reviewed.reps)
        assertTrue(FlashcardRepository.canUndo())

        // Undo
        val undone = FlashcardRepository.undoLastReview()
        assertNotNull(undone)
        assertEquals(CardQueueState.NEW, undone?.queueState)
        assertEquals(0, undone?.reps)

        FlashcardRepository.deleteCard("test_undo_c1")
    }

    @Test
    fun testToggleSuspendAndMark() {
        val card = Flashcard(id = "test_flag_c1", isSuspended = false, isMarked = false)
        FlashcardRepository.saveCard(card)

        FlashcardRepository.toggleSuspend("test_flag_c1")
        assertTrue(FlashcardRepository.getCard("test_flag_c1")?.isSuspended == true)

        FlashcardRepository.toggleMark("test_flag_c1")
        assertTrue(FlashcardRepository.getCard("test_flag_c1")?.isMarked == true)

        FlashcardRepository.setFlag("test_flag_c1", CardFlag.RED)
        assertEquals(CardFlag.RED, FlashcardRepository.getCard("test_flag_c1")?.flagColor)

        FlashcardRepository.deleteCard("test_flag_c1")
    }

    @Test
    fun testJsonExportImport() {
        val card = Flashcard(
            id = "test_export_c1",
            front = "Test Q",
            back = "Test A",
            tags = listOf("Tag1", "Tag2")
        )
        FlashcardRepository.saveCard(card)

        val json = FlashcardRepository.exportToJson()
        assertTrue(json.contains("test_export_c1"))
        assertTrue(json.contains("Test Q"))

        // Test import
        val importedCount = FlashcardRepository.importFromJson(json)
        assertTrue(importedCount >= 1)

        FlashcardRepository.deleteCard("test_export_c1")
    }
}

