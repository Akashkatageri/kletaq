package com.kletaq.app.features.flashcards

import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClozeParserTest {

    @Test
    fun testMultiClozeSiblingGeneration() {
        val noteText = "{{c1::Mitochondria}} is the {{c2::powerhouse::organelle}} of the cell."
        val baseCard = Flashcard(
            id = "note_123",
            topicId = "bio_101",
            tags = listOf("Biology")
        )

        val siblings = ClozeParser.generateSiblingCards("note_123", noteText, baseCard)
        assertEquals(2, siblings.size)

        val c1 = siblings[0]
        assertEquals("note_123_c1", c1.id)
        assertEquals("note_123", c1.noteId)
        assertEquals(FlashcardType.CLOZE, c1.cardType)
        assertEquals("[...] is the powerhouse of the cell.", c1.front)
        assertEquals("[Mitochondria] is the powerhouse of the cell.", c1.back)
        assertEquals("Mitochondria", c1.typedAnswer)

        val c2 = siblings[1]
        assertEquals("note_123_c2", c2.id)
        assertEquals("note_123", c2.noteId)
        assertEquals(FlashcardType.CLOZE, c2.cardType)
        assertEquals("Mitochondria is the [organelle] of the cell.", c2.front)
        assertEquals("Mitochondria is the [powerhouse] of the cell.", c2.back)
        assertEquals("powerhouse", c2.typedAnswer)
        assertEquals("organelle", c2.hint)
    }

    @Test
    fun testReversibleCardGeneration() {
        val baseCard = Flashcard(
            id = "note_rev_1",
            topicId = "lang_1",
            frontImagePaths = listOf("front_diagram.jpg"),
            backImagePaths = listOf("back_diagram.jpg")
        )

        val pair = ClozeParser.generateReversibleCards("note_rev_1", "Bonjour", "Hello", baseCard)
        assertEquals(2, pair.size)

        val fwd = pair[0]
        assertEquals("note_rev_1_fwd", fwd.id)
        assertEquals("Bonjour", fwd.front)
        assertEquals("Hello", fwd.back)
        assertEquals(listOf("front_diagram.jpg"), fwd.frontImagePaths)

        val rev = pair[1]
        assertEquals("note_rev_1_rev", rev.id)
        assertEquals("Hello", rev.front)
        assertEquals("Bonjour", rev.back)
        assertEquals(listOf("back_diagram.jpg"), rev.frontImagePaths)
        assertEquals(listOf("front_diagram.jpg"), rev.backImagePaths)
    }
}

