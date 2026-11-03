package com.kletaq.app.features.flashcards.engine

import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardDeck
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import kotlin.math.max
import kotlin.math.roundToInt

object AnkiScheduler {

    const val MINIMUM_EASE = 1.30
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_DAY = 86_400_000L

    fun scheduleNextReview(
        card: Flashcard,
        rating: FlashcardReviewRating,
        deck: FlashcardDeck = FlashcardDeck(),
        now: Long = System.currentTimeMillis()
    ): Flashcard {
        val learningSteps = deck.learningStepsMinutes.ifEmpty { listOf(1, 10) }
        val graduatingDays = deck.graduatingIntervalDays.coerceAtLeast(1)
        val easyDays = deck.easyIntervalDays.coerceAtLeast(2)

        return when (card.queueState) {
            CardQueueState.NEW, CardQueueState.LEARNING, CardQueueState.RELEARNING -> {
                handleLearningCard(card, rating, learningSteps, graduatingDays, easyDays, now)
            }
            CardQueueState.REVIEW -> {
                handleReviewCard(card, rating, graduatingDays, easyDays, now)
            }
        }
    }

    private fun handleLearningCard(
        card: Flashcard,
        rating: FlashcardReviewRating,
        steps: List<Int>,
        graduatingDays: Int,
        easyDays: Int,
        now: Long
    ): Flashcard {
        val currentStep = card.learningStepIndex.coerceIn(0, max(0, steps.size - 1))

        return when (rating) {
            FlashcardReviewRating.AGAIN -> {
                val stepMinutes = steps.firstOrNull() ?: 1
                card.copy(
                    queueState = CardQueueState.LEARNING,
                    learningStepIndex = 0,
                    intervalDays = 0,
                    dueTimestamp = now + (stepMinutes * MILLIS_PER_MINUTE),
                    reps = card.reps + 1,
                    lastReviewedTimestamp = now,
                    updatedAt = now
                )
            }
            FlashcardReviewRating.HARD -> {
                val currentMinutes = steps.getOrNull(currentStep) ?: 1
                val nextMinutes = steps.getOrNull(currentStep + 1) ?: (currentMinutes * 2)
                val hardMinutes = ((currentMinutes + nextMinutes) / 2).coerceAtLeast(currentMinutes)
                card.copy(
                    queueState = CardQueueState.LEARNING,
                    dueTimestamp = now + (hardMinutes * MILLIS_PER_MINUTE),
                    reps = card.reps + 1,
                    lastReviewedTimestamp = now,
                    updatedAt = now
                )
            }
            FlashcardReviewRating.GOOD -> {
                if (currentStep + 1 < steps.size) {
                    val nextMinutes = steps[currentStep + 1]
                    card.copy(
                        queueState = CardQueueState.LEARNING,
                        learningStepIndex = currentStep + 1,
                        dueTimestamp = now + (nextMinutes * MILLIS_PER_MINUTE),
                        reps = card.reps + 1,
                        lastReviewedTimestamp = now,
                        updatedAt = now
                    )
                } else {
                    // Graduate to Review
                    val fuzzedDays = applyFuzz(graduatingDays, card.id)
                    card.copy(
                        queueState = CardQueueState.REVIEW,
                        learningStepIndex = 0,
                        intervalDays = fuzzedDays,
                        dueTimestamp = now + (fuzzedDays * MILLIS_PER_DAY),
                        reps = card.reps + 1,
                        lastReviewedTimestamp = now,
                        updatedAt = now
                    )
                }
            }
            FlashcardReviewRating.EASY -> {
                // Graduate immediately with easy bonus
                val fuzzedDays = applyFuzz(easyDays, card.id)
                card.copy(
                    queueState = CardQueueState.REVIEW,
                    learningStepIndex = 0,
                    intervalDays = fuzzedDays,
                    easeFactor = (card.easeFactor + 0.15).coerceAtLeast(MINIMUM_EASE),
                    dueTimestamp = now + (fuzzedDays * MILLIS_PER_DAY),
                    reps = card.reps + 1,
                    lastReviewedTimestamp = now,
                    updatedAt = now
                )
            }
        }
    }

    private fun handleReviewCard(
        card: Flashcard,
        rating: FlashcardReviewRating,
        graduatingDays: Int,
        easyDays: Int,
        now: Long
    ): Flashcard {
        val currentInterval = card.intervalDays.coerceAtLeast(1)

        return when (rating) {
            FlashcardReviewRating.AGAIN -> {
                val newEase = (card.easeFactor - 0.20).coerceAtLeast(MINIMUM_EASE)
                val relearnInterval = 1
                card.copy(
                    queueState = CardQueueState.RELEARNING,
                    learningStepIndex = 0,
                    lapses = card.lapses + 1,
                    easeFactor = newEase,
                    intervalDays = relearnInterval,
                    dueTimestamp = now + (10 * MILLIS_PER_MINUTE), // 10 min relearn step
                    reps = card.reps + 1,
                    lastReviewedTimestamp = now,
                    updatedAt = now
                )
            }
            FlashcardReviewRating.HARD -> {
                val newEase = (card.easeFactor - 0.15).coerceAtLeast(MINIMUM_EASE)
                val newInterval = max(currentInterval + 1, (currentInterval * 1.2).roundToInt())
                card.copy(
                    queueState = CardQueueState.REVIEW,
                    easeFactor = newEase,
                    intervalDays = newInterval,
                    dueTimestamp = now + (newInterval * MILLIS_PER_DAY),
                    reps = card.reps + 1,
                    lastReviewedTimestamp = now,
                    updatedAt = now
                )
            }
            FlashcardReviewRating.GOOD -> {
                val rawInterval = max(currentInterval + 1, (currentInterval * card.easeFactor).roundToInt())
                val fuzzedInterval = applyFuzz(rawInterval, card.id)
                card.copy(
                    queueState = CardQueueState.REVIEW,
                    intervalDays = fuzzedInterval,
                    dueTimestamp = now + (fuzzedInterval * MILLIS_PER_DAY),
                    reps = card.reps + 1,
                    lastReviewedTimestamp = now,
                    updatedAt = now
                )
            }
            FlashcardReviewRating.EASY -> {
                val newEase = (card.easeFactor + 0.15).coerceAtLeast(MINIMUM_EASE)
                val rawInterval = max(currentInterval + 2, (currentInterval * card.easeFactor * 1.30).roundToInt())
                val fuzzedInterval = applyFuzz(rawInterval, card.id)
                card.copy(
                    queueState = CardQueueState.REVIEW,
                    easeFactor = newEase,
                    intervalDays = fuzzedInterval,
                    dueTimestamp = now + (fuzzedInterval * MILLIS_PER_DAY),
                    reps = card.reps + 1,
                    lastReviewedTimestamp = now,
                    updatedAt = now
                )
            }
        }
    }

    fun getEstimatedIntervalLabel(
        card: Flashcard,
        rating: FlashcardReviewRating,
        deck: FlashcardDeck = FlashcardDeck(),
        now: Long = System.currentTimeMillis()
    ): String {
        val simulated = scheduleNextReview(card, rating, deck, now)
        val diffMillis = simulated.dueTimestamp - now
        val diffMins = (diffMillis / MILLIS_PER_MINUTE).toInt()
        val diffDays = (diffMillis / MILLIS_PER_DAY).toInt()

        return when {
            diffMins <= 1 -> "<1m"
            diffMins < 60 -> "${diffMins}m"
            diffDays < 1 -> "<1d"
            diffDays == 1 -> "1d"
            diffDays < 30 -> "${diffDays}d"
            diffDays < 365 -> "${(diffDays / 30.0).roundToInt()}mo"
            else -> "${(diffDays / 365.0).roundToInt()}y"
        }
    }

    private fun applyFuzz(intervalDays: Int, seedKey: String): Int {
        if (intervalDays <= 4) return intervalDays
        val hash = kotlin.math.abs(seedKey.hashCode())
        val deltaRange = max(1, (intervalDays * 0.05).roundToInt())
        val delta = (hash % (2 * deltaRange + 1)) - deltaRange
        return max(1, intervalDays + delta)
    }
}

