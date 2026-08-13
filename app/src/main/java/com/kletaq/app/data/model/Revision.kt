package com.kletaq.app.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RevisionHistoryEntry(
    val date: String = "",
    val rating: String = "" // "again", "forgot", "hard", "good", "easy"
)

enum class ReviewRating(val value: String) {
    AGAIN("again"),
    FORGOT("forgot"),
    HARD("hard"),
    GOOD("good"),
    EASY("easy");

    companion object {
        fun fromValue(value: String): ReviewRating {
            if (value.equals("forgot", ignoreCase = true)) return AGAIN
            return entries.find { it.value.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) } ?: GOOD
        }
        fun fromString(str: String): ReviewRating = fromValue(str)
    }
}

enum class TopicDifficulty(val value: String) {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    companion object {
        fun fromString(str: String): TopicDifficulty {
            return entries.firstOrNull { it.value.equals(str, ignoreCase = true) || it.name.equals(str, ignoreCase = true) }
                ?: MEDIUM
        }
        fun fromValue(value: String): TopicDifficulty = fromString(value)
    }
}

enum class LearningDifficulty(val value: String) {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    companion object {
        fun fromValue(value: String): LearningDifficulty {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

@IgnoreExtraProperties
data class Revision(
    val id: String = "",
    val topicId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val topicName: String = "",
    val completed: Boolean = false,
    val learningDifficulty: String = "medium", // "easy", "medium", "hard"
    val repetitions: Int = 0,
    val interval: Int = 1,
    val lastReviewed: String = "", // "YYYY-MM-DD"
    val nextReview: String = "", // "YYYY-MM-DD"
    val status: String = "scheduled", // "scheduled", "due"
    val completedAt: String = "", // "YYYY-MM-DD"
    val dueDate: String = "",
    val examImportant: Boolean = false,
    val history: List<RevisionHistoryEntry> = emptyList()
)

data class ReviewStats(
    val reviewsDueToday: Int = 0,
    val reviewsCompletedToday: Int = 0,
    val retentionPercentage: Int = 95,
    val totalReviewsCompleted: Int = 0,
    val totalScheduled: Int = 0
)
