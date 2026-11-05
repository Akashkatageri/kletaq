package com.kletaq.app.features.flashcards

import com.kletaq.app.features.flashcards.engine.FsrsScheduler
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.DeckPreset
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsSchedulerTest {

    private val now = 1700000000000L

    @Test
    fun testInitialReview_RatingsAssignStabilityAndDifficulty() {
        val card = Flashcard(id = "c1", reps = 0)

        // Rating Good (grade 3)
        val goodResult = FsrsScheduler.scheduleReview(card, FlashcardReviewRating.GOOD, now = now)
        assertEquals(CardQueueState.REVIEW, goodResult.queueState)
        assertTrue("Good stability should be around 3.17", goodResult.stability > 3.0)
        assertTrue("Difficulty should be between 1 and 10", goodResult.difficulty in 1.0..10.0)
        assertEquals(1, goodResult.reps)
        assertEquals(0, goodResult.lapses)

        // Rating Again (grade 1)
        val againResult = FsrsScheduler.scheduleReview(card, FlashcardReviewRating.AGAIN, now = now)
        assertEquals(CardQueueState.RELEARNING, againResult.queueState)
        assertTrue("Again stability should be < 1.0", againResult.stability < 1.0)
        assertEquals(1, againResult.lapses)
    }

    @Test
    fun testRetrievability_DecaysOverTime() {
        val stability = 10.0 // 10 days stability
        val r0 = FsrsScheduler.retrievability(0, stability)
        val r5 = FsrsScheduler.retrievability(5, stability)
        val r10 = FsrsScheduler.retrievability(10, stability)

        assertEquals(1.0, r0, 0.001)
        assertTrue("R should decrease over time", r5 < r0)
        assertTrue("R should decrease further at 10 days", r10 < r5)
        assertEquals(0.90, r10, 0.02) // By definition, at t=S, R is approximately target retention (~0.90)
    }

    @Test
    fun testNextInterval_RetentionControlsInterval() {
        val stability = 20.0
        val intervalAt90 = FsrsScheduler.nextInterval(stability, 0.90)
        val intervalAt80 = FsrsScheduler.nextInterval(stability, 0.80)
        val intervalAt95 = FsrsScheduler.nextInterval(stability, 0.95)

        assertTrue("Higher retention target means shorter intervals", intervalAt95 < intervalAt90)
        assertTrue("Lower retention target means longer intervals", intervalAt80 > intervalAt90)
    }

    @Test
    fun testLeechDetection_MarksAndSuspends() {
        val card = Flashcard(id = "c_leech", reps = 8, lapses = 7)
        val preset = DeckPreset(leechThreshold = 8, leechAction = "SUSPEND")

        val result = FsrsScheduler.scheduleReview(card, FlashcardReviewRating.AGAIN, preset = preset, now = now)
        assertTrue("Card should be marked as leech", result.isLeech)
        assertTrue("Card should be suspended on reaching leech threshold", result.isSuspended)
        assertEquals(8, result.lapses)
    }
}

