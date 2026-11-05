package com.kletaq.app.features.flashcards

import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardDeck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashcardDeckTest {

    @Test
    fun testDeckCreationAndSubdecks() {
        val rootDeck = FlashcardRepository.createDeck("Engineering")
        assertNotNull(rootDeck)
        assertEquals("Engineering", rootDeck.name)
        assertEquals("Engineering", rootDeck.displayName)
        assertFalse(rootDeck.isSubdeck)

        val subDeck = FlashcardRepository.createDeck("Computer Science", parentDeckId = rootDeck.id)
        assertNotNull(subDeck)
        assertEquals("Engineering::Computer Science", subDeck.name)
        assertEquals("Computer Science", subDeck.displayName)
        assertTrue(subDeck.isSubdeck)
        assertEquals(rootDeck.id, subDeck.parentDeckId)

        // Rename deck
        FlashcardRepository.renameDeck(subDeck.id, "CS & AI")
        val updated = FlashcardRepository.getDeck(subDeck.id)
        assertEquals("CS & AI", updated?.name)

        // Delete decks
        FlashcardRepository.deleteDeck(subDeck.id)
        assertNull(FlashcardRepository.getDeck(subDeck.id))
        FlashcardRepository.deleteDeck(rootDeck.id)
        assertNull(FlashcardRepository.getDeck(rootDeck.id))
    }

    @Test
    fun testSiblingCardBurying() {
        val noteId = "note_cloze_siblings_123"
        val card1 = Flashcard(id = "card_c1", noteId = noteId, front = "Front 1")
        val card2 = Flashcard(id = "card_c2", noteId = noteId, front = "Front 2")
        val card3 = Flashcard(id = "card_unrelated", noteId = "note_unrelated_456", front = "Front 3")

        FlashcardRepository.saveCard(card1)
        FlashcardRepository.saveCard(card2)
        FlashcardRepository.saveCard(card3)

        // Bury siblings of card1
        FlashcardRepository.burySiblingCards("card_c1")

        // card2 must be buried because it shares noteId
        val retrievedCard2 = FlashcardRepository.getCard("card_c2")
        assertTrue(retrievedCard2?.isBuried == true)

        // card3 should not be buried
        val retrievedCard3 = FlashcardRepository.getCard("card_unrelated")
        assertFalse(retrievedCard3?.isBuried == true)

        // Unbury all
        FlashcardRepository.unburyAll()
        assertFalse(FlashcardRepository.getCard("card_c2")?.isBuried == true)

        FlashcardRepository.deleteCard("card_c1")
        FlashcardRepository.deleteCard("card_c2")
        FlashcardRepository.deleteCard("card_unrelated")
    }

    @Test
    fun testMultiImagePaths() {
        val card = Flashcard(
            id = "card_multi_img",
            front = "What is shown in figures?",
            frontImagePaths = listOf("card_img_1.jpg", "card_img_2.jpg"),
            backImagePaths = listOf("card_img_ans_1.jpg", "card_img_ans_2.jpg", "card_img_ans_3.jpg"),
            audioPath = "card_audio_1.m4a"
        )
        FlashcardRepository.saveCard(card)

        val retrieved = FlashcardRepository.getCard("card_multi_img")
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.allFrontImages?.size)
        assertEquals(3, retrieved?.allBackImages?.size)
        assertEquals("card_audio_1.m4a", retrieved?.audioPath)

        // Check JSON export preserves multi-images and audio
        val json = FlashcardRepository.exportToJson()
        assertTrue(json.contains("card_img_1.jpg"))
        assertTrue(json.contains("card_img_ans_3.jpg"))
        assertTrue(json.contains("card_audio_1.m4a"))

        FlashcardRepository.deleteCard("card_multi_img")
    }
}

