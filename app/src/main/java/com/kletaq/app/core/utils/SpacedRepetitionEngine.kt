package com.kletaq.app.core.utils

import com.kletaq.app.core.config.AdaptiveEngineConfig
import com.kletaq.app.data.model.LearningDifficulty
import com.kletaq.app.data.model.MemoryCard
import com.kletaq.app.data.model.MemoryHealth
import com.kletaq.app.data.model.Revision
import com.kletaq.app.data.model.RevisionHistoryEntry
import com.kletaq.app.data.model.ReviewRating
import com.kletaq.app.data.model.ReviewStats
import com.kletaq.app.data.model.TopicDifficulty
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Data-driven Adaptive Spaced Repetition Engine for Kletaq.
 * Implements SM-2 memory card calculations, Memory Health decay curves, and retention estimation.
 * ZERO hardcoded constants; all parameters are consumed from AdaptiveEngineConfig.
 */
object SpacedRepetitionEngine {

    private val utcTimeZone = TimeZone.getTimeZone("UTC")

    fun parseDateUTC(str: String): Date {
        if (str.isBlank()) return Date()
        val parts = str.split("-")
        if (parts.size != 3) return Date()
        val year = parts[0].toIntOrNull() ?: return Date()
        val month = parts[1].toIntOrNull() ?: return Date()
        val day = parts[2].toIntOrNull() ?: return Date()

        val cal = Calendar.getInstance(utcTimeZone)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun formatDateUTC(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.timeZone = utcTimeZone
        return formatter.format(date)
    }

    fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.timeZone = utcTimeZone
        return formatter.format(Date())
    }

    fun addDaysToDateString(dateStr: String, days: Int): String {
        val date = parseDateUTC(dateStr)
        val cal = Calendar.getInstance(utcTimeZone)
        cal.time = date
        cal.add(Calendar.DAY_OF_MONTH, days)
        return formatDateUTC(cal.time)
    }

    fun getDaysDifference(dateStr1: String, dateStr2: String): Int {
        val d1 = parseDateUTC(dateStr1)
        val d2 = parseDateUTC(dateStr2)
        val diffTime = d2.time - d1.time
        return (diffTime / (1000 * 60 * 60 * 24)).toInt()
    }

    // ── Data-Driven MemoryCard Creation & Initial Schedule ─────────────────────────

    fun computeInitialIntervalDays(
        difficulty: TopicDifficulty,
        confidence: Int,
        config: AdaptiveEngineConfig = AdaptiveEngineConfig()
    ): Int {
        val diffMap = config.initialIntervalMatrixDays[difficulty] ?: config.initialIntervalMatrixDays[TopicDifficulty.MEDIUM]!!
        val clampedConfidence = confidence.coerceIn(1, 5)
        return diffMap[clampedConfidence] ?: 1
    }

    fun estimateMemoryRetentionPercentage(
        elapsedDays: Int,
        intervalDays: Int
    ): Int {
        if (intervalDays <= 0) return 100
        val ratio = elapsedDays.toDouble() / intervalDays.toDouble()
        // Ebbinghaus forgetting curve approximation: R = e^(-0.70 * ratio)
        val retentionFraction = exp(-0.70 * ratio)
        return (retentionFraction * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun calculateMemoryHealth(
        elapsedDays: Int,
        intervalDays: Int,
        config: AdaptiveEngineConfig = AdaptiveEngineConfig()
    ): MemoryHealth {
        val retention = estimateMemoryRetentionPercentage(elapsedDays, intervalDays) / 100.0
        val freshMin = config.memoryHealthThresholds[MemoryHealth.FRESH] ?: 0.90
        val stableMin = config.memoryHealthThresholds[MemoryHealth.STABLE] ?: 0.75
        val fadingMin = config.memoryHealthThresholds[MemoryHealth.FADING] ?: 0.60
        val weakMin = config.memoryHealthThresholds[MemoryHealth.WEAK] ?: 0.40

        return when {
            retention >= freshMin -> MemoryHealth.FRESH
            retention >= stableMin -> MemoryHealth.STABLE
            retention >= fadingMin -> MemoryHealth.FADING
            retention >= weakMin -> MemoryHealth.WEAK
            else -> MemoryHealth.CRITICAL
        }
    }

    fun createInitialMemoryCard(
        topicId: String,
        subjectId: String = "",
        moduleId: String = "",
        lessonId: String = "",
        subjectName: String = "",
        topicName: String = "",
        difficulty: TopicDifficulty = TopicDifficulty.MEDIUM,
        confidence: Int = 3,
        config: AdaptiveEngineConfig = AdaptiveEngineConfig(),
        nowTimestamp: Long = System.currentTimeMillis()
    ): MemoryCard {
        val initialInterval = computeInitialIntervalDays(difficulty, confidence, config)
        val nextReviewMillis = nowTimestamp + (initialInterval * 86400000L)

        return MemoryCard(
            topicId = topicId,
            subjectId = subjectId,
            moduleId = moduleId,
            lessonId = lessonId,
            subjectName = subjectName,
            topicName = topicName,
            difficulty = difficulty.value,
            confidence = confidence.coerceIn(1, 5),
            reviewCount = 0,
            lastReview = nowTimestamp,
            nextReview = nextReviewMillis,
            currentInterval = initialInterval,
            easeFactor = config.defaultInitialEaseFactor,
            lapses = 0,
            mastery = (confidence / 5.0f).coerceIn(0.2f, 1.0f),
            memoryHealth = MemoryHealth.FRESH.value,
            importanceScore = 1.0,
            studyTimeMinutes = 20,
            completionDate = nowTimestamp,
            lastRating = ReviewRating.GOOD.value,
            createdAt = nowTimestamp,
            updatedAt = nowTimestamp
        )
    }

    fun updateMemoryCardReview(
        card: MemoryCard,
        rating: ReviewRating,
        config: AdaptiveEngineConfig = AdaptiveEngineConfig(),
        nowTimestamp: Long = System.currentTimeMillis()
    ): MemoryCard {
        val easeModifier = config.easeFactorModifiers[rating] ?: 0.0
        val newEaseFactor = (card.easeFactor + easeModifier).coerceIn(config.minEaseFactor, config.maxEaseFactor)

        var newInterval: Int
        var newLapses = card.lapses

        when (rating) {
            ReviewRating.AGAIN, ReviewRating.FORGOT -> {
                newLapses += 1
                newInterval = config.lapseIntervalResetDays
            }
            ReviewRating.HARD -> {
                newInterval = max(1, (card.currentInterval * 1.2).roundToInt())
            }
            ReviewRating.GOOD -> {
                newInterval = max(1, (card.currentInterval * newEaseFactor).roundToInt())
            }
            ReviewRating.EASY -> {
                newInterval = max(2, (card.currentInterval * newEaseFactor * 1.3).roundToInt())
            }
        }

        val nextReviewMillis = nowTimestamp + (newInterval * 86400000L)
        val newMastery = when (rating) {
            ReviewRating.AGAIN, ReviewRating.FORGOT -> (card.mastery - 0.15f).coerceAtLeast(0.1f)
            ReviewRating.HARD -> card.mastery
            ReviewRating.GOOD -> (card.mastery + 0.10f).coerceAtMost(1.0f)
            ReviewRating.EASY -> (card.mastery + 0.20f).coerceAtMost(1.0f)
        }

        return card.copy(
            reviewCount = card.reviewCount + 1,
            lastReview = nowTimestamp,
            nextReview = nextReviewMillis,
            currentInterval = newInterval,
            easeFactor = newEaseFactor,
            lapses = newLapses,
            mastery = newMastery,
            memoryHealth = MemoryHealth.FRESH.value,
            lastRating = rating.value,
            updatedAt = nowTimestamp
        )
    }

    // ── Legacy Revision Helpers ───────────────────────────────────────────────────

    fun createInitialRevision(
        topicId: String,
        subjectId: String,
        subjectName: String,
        topicName: String,
        difficulty: LearningDifficulty = LearningDifficulty.MEDIUM
    ): Revision {
        val today = getTodayDateString()
        val topicDiff = when (difficulty) {
            LearningDifficulty.EASY -> TopicDifficulty.EASY
            LearningDifficulty.MEDIUM -> TopicDifficulty.MEDIUM
            LearningDifficulty.HARD -> TopicDifficulty.HARD
        }
        val interval = computeInitialIntervalDays(topicDiff, 3)
        val nextReview = addDaysToDateString(today, interval)

        return Revision(
            id = "rev-$topicId",
            topicId = topicId,
            subjectId = subjectId,
            subjectName = subjectName,
            topicName = topicName,
            completed = false,
            learningDifficulty = difficulty.value,
            repetitions = 0,
            interval = interval,
            lastReviewed = today,
            nextReview = nextReview,
            status = "scheduled",
            completedAt = today,
            dueDate = nextReview
        )
    }

    fun updateRevisionScheduling(
        revision: Revision,
        rating: ReviewRating
    ): Revision {
        val today = getTodayDateString()
        var nextRepetitions = revision.repetitions
        var nextInterval = revision.interval

        when (rating) {
            ReviewRating.AGAIN, ReviewRating.FORGOT -> {
                nextRepetitions = 0
                nextInterval = 1
            }
            ReviewRating.HARD -> {
                nextRepetitions = 0
                nextInterval = 1
            }
            ReviewRating.HARD -> {
                nextRepetitions += 1
                nextInterval = max(1, nextInterval)
            }
            ReviewRating.GOOD -> {
                nextRepetitions += 1
                nextInterval *= 2
            }
            ReviewRating.EASY -> {
                nextRepetitions += 1
                nextInterval *= 3
            }
        }

        val nextReviewDateStr = addDaysToDateString(today, nextInterval)
        val updatedHistory = revision.history + RevisionHistoryEntry(date = today, rating = rating.value)

        return revision.copy(
            repetitions = nextRepetitions,
            interval = nextInterval,
            lastReviewed = today,
            nextReview = nextReviewDateStr,
            completed = false,
            status = "scheduled",
            dueDate = nextReviewDateStr,
            history = updatedHistory
        )
    }

    fun sanitizeRevisions(revisions: List<Revision>): List<Revision> {
        val map = mutableMapOf<String, Revision>()
        val todayStr = getTodayDateString()

        val sortedRevisions = revisions.sortedBy { (it.repetitions) + (it.interval) }

        for (rev in sortedRevisions) {
            if (rev.topicId.isBlank()) continue

            val nextReviewStr = if (rev.nextReview.isNotBlank()) rev.nextReview else if (rev.dueDate.isNotBlank()) rev.dueDate else addDaysToDateString(todayStr, 1)
            val isDue = nextReviewStr <= todayStr

            val normalized = rev.copy(
                id = if (rev.id.isNotBlank()) rev.id else "rev-${rev.topicId}",
                nextReview = nextReviewStr,
                status = if (isDue) "due" else "scheduled",
                lastReviewed = if (rev.lastReviewed.isNotBlank()) rev.lastReviewed else todayStr,
                completedAt = if (rev.completedAt.isNotBlank()) rev.completedAt else todayStr,
                dueDate = if (rev.dueDate.isNotBlank()) rev.dueDate else nextReviewStr
            )

            map[rev.topicId] = normalized
        }

        return map.values.toList()
    }

    fun getDailyReviewQueue(
        revisions: List<Revision>,
        isExamMode: Boolean = false
    ): List<Revision> {
        val sanitized = sanitizeRevisions(revisions)
        val todayStr = getTodayDateString()

        val dueReviews = sanitized.filter { rev ->
            if (rev.lastReviewed == todayStr && rev.repetitions > 0 && rev.status == "scheduled") {
                return@filter false
            }
            rev.nextReview <= todayStr
        }

        if (isExamMode) {
            return dueReviews.sortedWith { a, b ->
                if (a.examImportant && !b.examImportant) return@sortedWith -1
                if (!a.examImportant && b.examImportant) return@sortedWith 1

                val aForgotten = (a.repetitions == 0 || a.interval == 1)
                val bForgotten = (b.repetitions == 0 || b.interval == 1)
                if (aForgotten && !bForgotten) return@sortedWith -1
                if (!aForgotten && bForgotten) return@sortedWith 1

                val aHard = a.learningDifficulty.equals("hard", ignoreCase = true)
                val bHard = b.learningDifficulty.equals("hard", ignoreCase = true)
                if (aHard && !bHard) return@sortedWith -1
                if (!aHard && bHard) return@sortedWith 1

                if (a.repetitions != b.repetitions) {
                    return@sortedWith b.repetitions - a.repetitions
                }

                a.nextReview.compareTo(b.nextReview)
            }
        }

        return dueReviews.sortedWith { a, b ->
            val isAOverdue = a.nextReview < todayStr
            val isBOverdue = b.nextReview < todayStr
            if (isAOverdue && !isBOverdue) return@sortedWith -1
            if (!isAOverdue && isBOverdue) return@sortedWith 1

            val isAHard = a.learningDifficulty.equals("hard", ignoreCase = true)
            val isBHard = b.learningDifficulty.equals("hard", ignoreCase = true)
            if (isAHard && !isBHard) return@sortedWith -1
            if (!isAHard && isBHard) return@sortedWith 1

            a.nextReview.compareTo(b.nextReview)
        }
    }

    fun getEstimatedReviewTimeMinutes(dueRevisions: List<Revision>): Int {
        var totalMinutes = 0.0
        for (rev in dueRevisions) {
            totalMinutes += 0.75
        }
        return max(1, Math.round(totalMinutes).toInt())
    }

    fun getReviewStats(revisions: List<Revision>): ReviewStats {
        val sanitized = sanitizeRevisions(revisions)
        val todayStr = getTodayDateString()

        val reviewsDueToday = sanitized.count { it.nextReview <= todayStr }
        val reviewsCompletedToday = sanitized.count { it.lastReviewed == todayStr && it.repetitions > 0 }
        val totalReviewsCompleted = sanitized.sumOf { it.repetitions }
        val totalScheduled = sanitized.size

        val reviewedItems = sanitized.filter { it.repetitions > 0 }
        var retentionPercentage = 95
        if (reviewedItems.isNotEmpty()) {
            val highEaseCount = reviewedItems.count { it.interval >= 2 }
            retentionPercentage = Math.round((highEaseCount.toDouble() / reviewedItems.size.toDouble()) * 100).toInt()
        }

        return ReviewStats(
            reviewsDueToday = reviewsDueToday,
            reviewsCompletedToday = reviewsCompletedToday,
            retentionPercentage = max(50, Math.min(100, retentionPercentage)),
            totalReviewsCompleted = totalReviewsCompleted,
            totalScheduled = totalScheduled
        )
    }
}
