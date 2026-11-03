package com.kletaq.app.features.flashcards

import com.kletaq.app.features.flashcards.engine.AnkiScheduler
import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardDeck
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnkiSchedulerTest {

    private val now = 1700000000000L

    @Test
    fun testClozeParser_MaskingAndReveal() {
        val raw = "The capital of France is {{c1::Paris::City}}."
        assertTrue(ClozeParser.hasValidCloze(raw))
        assertEquals(listOf(1), ClozeParser.extractClozeNumbers(raw))

        val question = ClozeParser.renderQuestion(raw, 1)
        assertEquals("The capital of France is [City].", question)

        val answer = ClozeParser.renderAnswer(raw, 1)
        assertEquals("The capital of France is [Paris].", answer)
    }

    @Test
    fun testClozeParser_MultiIndex() {
        val raw = "{{c1::Mitochondria}} is the {{c2::powerhouse}} of the cell."
        assertEquals(listOf(1, 2), ClozeParser.extractClozeNumbers(raw))

        // When testing cloze 1, cloze 2 should show its real text
        val q1 = ClozeParser.renderQuestion(raw, 1)
        assertEquals("[...] is the powerhouse of the cell.", q1)

        val q2 = ClozeParser.renderQuestion(raw, 2)
        assertEquals("Mitochondria is the [...] of the cell.", q2)
    }

    @Test
    fun testNewCard_AgainResetsToFirstStep() {
        val card = Flashcard(id = "c1", queueState = CardQueueState.NEW)
        val deck = FlashcardDeck(learningStepsMinutes = listOf(1, 10))

        val result = AnkiScheduler.scheduleNextReview(card, FlashcardReviewRating.AGAIN, deck, now)
        assertEquals(CardQueueState.LEARNING, result.queueState)
        assertEquals(0, result.learningStepIndex)
        assertEquals(now + 60_000L, result.dueTimestamp)
        assertEquals(1, result.reps)
    }

    @Test
    fun testNewCard_GoodAdvancesStepThenGraduates() {
        val card = Flashcard(id = "c1", queueState = CardQueueState.NEW)
        val deck = FlashcardDeck(learningStepsMinutes = listOf(1, 10), graduatingIntervalDays = 1)

        // Step 0 -> Step 1 (10 min)
        val step1 = AnkiScheduler.scheduleNextReview(card, FlashcardReviewRating.GOOD, deck, now)
        assertEquals(CardQueueState.LEARNING, step1.queueState)
        assertEquals(1, step1.learningStepIndex)
        assertEquals(now + 600_000L, step1.dueTimestamp)

        // Step 1 -> Graduate to Review (1 day)
        val graduated = AnkiScheduler.scheduleNextReview(step1, FlashcardReviewRating.GOOD, deck, now)
        assertEquals(CardQueueState.REVIEW, graduated.queueState)
        assertTrue(graduated.intervalDays >= 1)
    }

    @Test
    fun testNewCard_EasyGraduatesImmediatelyWithBonusDays() {
        val card = Flashcard(id = "c1", queueState = CardQueueState.NEW, easeFactor = 2.5)
        val deck = FlashcardDeck(easyIntervalDays = 4)

        val result = AnkiScheduler.scheduleNextReview(card, FlashcardReviewRating.EASY, deck, now)
        assertEquals(CardQueueState.REVIEW, result.queueState)
        assertTrue(result.intervalDays >= 4)
        assertTrue(result.easeFactor > 2.5)
    }

    @Test
    fun testReviewCard_AgainLapsePenalty() {
        val card = Flashcard(
            id = "c1",
            queueState = CardQueueState.REVIEW,
            intervalDays = 10,
            easeFactor = 2.50,
            lapses = 0
        )

        val result = AnkiScheduler.scheduleNextReview(card, FlashcardReviewRating.AGAIN, now = now)
        assertEquals(CardQueueState.RELEARNING, result.queueState)
        assertEquals(1, result.lapses)
        assertEquals(2.30, result.easeFactor, 0.001)
        assertEquals(1, result.intervalDays)
    }

    @Test
    fun testReviewCard_HardIntervalAndEase() {
        val card = Flashcard(
            id = "c1",
            queueState = CardQueueState.REVIEW,
            intervalDays = 10,
            easeFactor = 2.50
        )

        val result = AnkiScheduler.scheduleNextReview(card, FlashcardReviewRating.HARD, now = now)
        assertEquals(CardQueueState.REVIEW, result.queueState)
        assertEquals(12, result.intervalDays)
        assertEquals(2.35, result.easeFactor, 0.001)
    }

    @Test
    fun testReviewCard_GoodStandardGrowth() {
        val card = Flashcard(
            id = "c1",
            queueState = CardQueueState.REVIEW,
            intervalDays = 10,
            easeFactor = 2.50
        )

        val result = AnkiScheduler.scheduleNextReview(card, FlashcardReviewRating.GOOD, now = now)
        assertEquals(CardQueueState.REVIEW, result.queueState)
        // 10 * 2.5 = 25 (with fuzz ±1)
        assertTrue(result.intervalDays in 23..27)
        assertEquals(2.50, result.easeFactor, 0.001)
    }

    @Test
    fun testIntervalLabels() {
        val card = Flashcard(id = "c1", queueState = CardQueueState.NEW)
        val deck = FlashcardDeck(learningStepsMinutes = listOf(1, 10))

        assertEquals("<1m", AnkiScheduler.getEstimatedIntervalLabel(card, FlashcardReviewRating.AGAIN, deck, now))
        assertEquals("10m", AnkiScheduler.getEstimatedIntervalLabel(card, FlashcardReviewRating.GOOD, deck, now))
        assertEquals("4d", AnkiScheduler.getEstimatedIntervalLabel(card, FlashcardReviewRating.EASY, deck, now))
    }
}

