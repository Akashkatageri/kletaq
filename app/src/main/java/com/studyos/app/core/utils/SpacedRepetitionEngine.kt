package com.studyos.app.core.utils

import com.studyos.app.data.model.LearningDifficulty
import com.studyos.app.data.model.Revision
import com.studyos.app.data.model.RevisionHistoryEntry
import com.studyos.app.data.model.ReviewRating
import com.studyos.app.data.model.ReviewStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

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

    fun createInitialRevision(
        topicId: String,
        subjectId: String,
        subjectName: String,
        topicName: String,
        difficulty: LearningDifficulty = LearningDifficulty.MEDIUM
    ): Revision {
        val today = getTodayDateString()
        val interval = when (difficulty) {
            LearningDifficulty.EASY -> 3
            LearningDifficulty.MEDIUM -> 2
            LearningDifficulty.HARD -> 1
        }
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
            ReviewRating.FORGOT -> {
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
            // Default 45 seconds (0.75 mins) per topic review
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
