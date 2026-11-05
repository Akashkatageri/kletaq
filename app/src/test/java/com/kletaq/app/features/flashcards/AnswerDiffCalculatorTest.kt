package com.kletaq.app.features.flashcards

import com.kletaq.app.features.flashcards.engine.AnswerDiffCalculator
import com.kletaq.app.features.flashcards.engine.DiffType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerDiffCalculatorTest {

    @Test
    fun testExactMatch() {
        val result = AnswerDiffCalculator.computeDiff("Photosynthesis", "Photosynthesis")
        assertTrue(result.isExactMatch)
        assertEquals(1.0f, result.accuracy, 0.001f)
        assertTrue(result.tokens.all { it.type == DiffType.MATCH })
    }

    @Test
    fun testCaseInsensitiveExactMatch() {
        val result = AnswerDiffCalculator.computeDiff("photosynthesis", "Photosynthesis")
        assertTrue(result.isExactMatch)
        assertEquals(1.0f, result.accuracy, 0.001f)
    }

    @Test
    fun testTypoAndMissingChars() {
        val result = AnswerDiffCalculator.computeDiff("Mitochondrion", "Mitochondria")
        assertFalse(result.isExactMatch)
        assertTrue(result.accuracy > 0.8f) // Very close match

        // Should detect extra 'o', 'n' and missing 'a'
        assertTrue(result.tokens.any { it.type == DiffType.EXTRA })
        assertTrue(result.tokens.any { it.type == DiffType.MISSING })
    }

    @Test
    fun testEmptyTypedAnswer() {
        val result = AnswerDiffCalculator.computeDiff("", "Oxygen")
        assertFalse(result.isExactMatch)
        assertEquals(0.0f, result.accuracy, 0.001f)
        assertTrue(result.tokens.all { it.type == DiffType.MISSING })
    }
}

