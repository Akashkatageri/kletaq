package com.kletaq.app.features.flashcards.engine

import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.DeckPreset
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * FSRS (Free Spaced Repetition Scheduler) v4.5 / v5 implementation.
 * Calculates memory stability (S), item difficulty (D), and retrievability (R)
 * to schedule optimal review intervals based on a desired retention rate.
 */
object FsrsScheduler {

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_DAY = 86_400_000L

    // Default FSRS-4.5 weights (w0 to w16)
    private val DEFAULT_W = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.691,   // w0..w3: Initial stability for Again, Hard, Good, Easy
        7.1949, 0.5345,                     // w4..w5: Initial difficulty
        1.4604, 0.0046,                     // w6..w7: Difficulty update
        1.5457, 0.1192, 1.0192,             // w8..w10: Stability update on recall
        1.9395, 0.11, 0.29, 0.23,           // w11..w14: Stability update on forget
        0.23, 2.94                          // w15..w16: Hard penalty & Easy bonus
    )

    private const val FACTOR = 19.0 / 81.0
    private const val DECAY = -0.5

    fun retrievability(elapsedDays: Int, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + FACTOR * (elapsedDays / stability)).pow(DECAY)
    }

    fun nextInterval(stability: Double, desiredRetention: Double): Int {
        if (stability <= 0.0) return 1
        val r = desiredRetention.coerceIn(0.70, 0.98)
        val factorR = (r.pow(1.0 / DECAY) - 1.0) / FACTOR
        val days = (stability * factorR).roundToInt()
        return max(1, days)
    }

    fun scheduleReview(
        card: Flashcard,
        rating: FlashcardReviewRating,
        preset: DeckPreset = DeckPreset(),
        now: Long = System.currentTimeMillis()
    ): Flashcard {
        val grade = when (rating) {
            FlashcardReviewRating.AGAIN -> 1
            FlashcardReviewRating.HARD -> 2
            FlashcardReviewRating.GOOD -> 3
            FlashcardReviewRating.EASY -> 4
        }

        val elapsed = if (card.lastReviewedTimestamp > 0L) {
            max(0, ((now - card.lastReviewedTimestamp) / MILLIS_PER_DAY).toInt())
        } else 0

        val isNew = card.queueState == CardQueueState.NEW && card.reps == 0
        val baseStability = if (card.stability > 0.0) card.stability else DEFAULT_W[grade - 1]
        val baseDifficulty = if (card.difficulty > 0.0) card.difficulty else (DEFAULT_W[4] - exp(DEFAULT_W[5] * (grade - 1)) + 1.0).coerceIn(1.0, 10.0)

        val newDifficulty: Double
        val newStability: Double
        val newLapses: Int
        val isLeech: Boolean

        if (isNew) {
            // Initial stability and difficulty
            newStability = DEFAULT_W[grade - 1]
            newDifficulty = (DEFAULT_W[4] - exp(DEFAULT_W[5] * (grade - 1)) + 1.0).coerceIn(1.0, 10.0)
            newLapses = if (grade == 1) 1 else 0
            isLeech = newLapses >= preset.leechThreshold
        } else {
            val prevR = retrievability(elapsed, baseStability)
            val d0Good = DEFAULT_W[4] - exp(DEFAULT_W[5] * 2) + 1.0
            val deltaD = -DEFAULT_W[6] * (grade - 3)
            val rawD = baseDifficulty + deltaD
            newDifficulty = (DEFAULT_W[7] * d0Good + (1.0 - DEFAULT_W[7]) * rawD).coerceIn(1.0, 10.0)

            if (grade == 1) {
                // Lapse
                newLapses = card.lapses + 1
                isLeech = newLapses >= preset.leechThreshold
                val sForget = DEFAULT_W[11] *
                        baseDifficulty.pow(-DEFAULT_W[12]) *
                        ((baseStability + 1.0).pow(DEFAULT_W[13]) - 1.0) *
                        exp((1.0 - prevR) * DEFAULT_W[14])
                newStability = min(baseStability, max(0.1, sForget))
            } else {
                newLapses = card.lapses
                isLeech = card.isLeech || newLapses >= preset.leechThreshold
                val hardPenalty = if (grade == 2) DEFAULT_W[15] else 1.0
                val easyBonus = if (grade == 4) DEFAULT_W[16] else 1.0
                val sRecall = baseStability * (1.0 + exp(DEFAULT_W[8]) *
                        (11.0 - baseDifficulty) *
                        baseStability.pow(-DEFAULT_W[9]) *
                        (exp((1.0 - prevR) * DEFAULT_W[10]) - 1.0) *
                        hardPenalty * easyBonus)
                newStability = max(baseStability, sRecall)
            }
        }

        val rawInterval = nextInterval(newStability, preset.desiredRetention)
        val cappedInterval = min(preset.maximumIntervalDays, rawInterval)

        val newQueueState = when (grade) {
            1 -> CardQueueState.RELEARNING
            else -> CardQueueState.REVIEW
        }

        val dueTimestamp = if (grade == 1) {
            val relearnMins = preset.relearningStepsMinutes.firstOrNull() ?: 10
            now + (relearnMins * MILLIS_PER_MINUTE)
        } else {
            now + (cappedInterval * MILLIS_PER_DAY)
        }

        val shouldSuspend = isLeech && preset.leechAction.equals("SUSPEND", ignoreCase = true)

        return card.copy(
            queueState = newQueueState,
            stability = newStability,
            difficulty = newDifficulty,
            elapsedDays = elapsed,
            scheduledDays = cappedInterval,
            intervalDays = if (grade == 1) 0 else cappedInterval,
            dueTimestamp = dueTimestamp,
            reps = card.reps + 1,
            lapses = newLapses,
            isLeech = isLeech,
            isSuspended = card.isSuspended || shouldSuspend,
            lastReviewedTimestamp = now,
            updatedAt = now
        )
    }

    fun getEstimatedIntervalLabel(
        card: Flashcard,
        rating: FlashcardReviewRating,
        preset: DeckPreset = DeckPreset(),
        now: Long = System.currentTimeMillis()
    ): String {
        val simulated = scheduleReview(card, rating, preset, now)
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
}

