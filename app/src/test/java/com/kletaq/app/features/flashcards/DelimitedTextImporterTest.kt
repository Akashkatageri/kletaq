package com.kletaq.app.features.flashcards

import com.kletaq.app.features.flashcards.data.apkg.DelimitedTextImporter
import com.kletaq.app.features.flashcards.model.FlashcardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class DelimitedTextImporterTest {

    @Test
    fun testParseTsv_BasicCards() {
        val tsvData = """
            #separator:tab
            #html:true
            #tags column:3
            Capital of France	Paris	geography;europe
            What is 2 + 2?	4	math;basic
        """.trimIndent()

        val reader = BufferedReader(StringReader(tsvData))
        val cards = DelimitedTextImporter.parseStream(reader, targetDeckId = "deck_geo", targetDeckName = "Geography")

        assertEquals(2, cards.size)
        assertEquals("Capital of France", cards[0].front)
        assertEquals("Paris", cards[0].back)
        assertEquals(listOf("geography", "europe"), cards[0].tags)
        assertEquals("deck_geo", cards[0].deckId)
        assertEquals(FlashcardType.BASIC, cards[0].cardType)

        assertEquals("What is 2 + 2?", cards[1].front)
        assertEquals("4", cards[1].back)
        assertEquals(listOf("math", "basic"), cards[1].tags)
    }

    @Test
    fun testParseCsv_WithQuotes() {
        val csvData = """
            front,back,tags
            "Who said ""I think, therefore I am""?","René Descartes","philosophy"
            "Simple Question","Simple Answer","test"
        """.trimIndent()

        val reader = BufferedReader(StringReader(csvData))
        val cards = DelimitedTextImporter.parseStream(reader)

        assertEquals(2, cards.size)
        assertEquals("Who said \"I think, therefore I am\"?", cards[0].front)
        assertEquals("René Descartes", cards[0].back)
        assertEquals(listOf("philosophy"), cards[0].tags)
    }

    @Test
    fun testParseCloze_AutoDetect() {
        val tsvData = """
            The capital of {{c1::Japan}} is {{c2::Tokyo}}.		geography
        """.trimIndent()

        val reader = BufferedReader(StringReader(tsvData))
        val cards = DelimitedTextImporter.parseStream(reader)

        assertEquals(1, cards.size)
        assertEquals(FlashcardType.CLOZE, cards[0].cardType)
        assertTrue(cards[0].clozeText.contains("{{c1::Japan}}"))
    }
}

