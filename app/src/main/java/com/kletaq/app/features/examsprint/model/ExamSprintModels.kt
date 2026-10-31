package com.kletaq.app.features.examsprint.model

/**
 * 🎯 Data models for the Exam Sprint feature.
 * Supports targeted prep for upcoming internals/midterms from assignment pools.
 */
data class ExamSprint(
    val id: String,
    val title: String,
    val subjectCode: String,
    val subjectName: String,
    val examDateMillis: Long,
    val targetTestQuestionsCount: Int = 5,
    val targetTestDurationMinutes: Int = 60,
    val questions: List<SprintQuestion> = emptyList(),
    val mockTestsTaken: Int = 0,
    val bestScore: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalQuestions: Int
        get() = questions.size

    val masteredQuestionsCount: Int
        get() = questions.count { it.masteryStatus == QuestionMastery.MASTERED }

    val needsReviewCount: Int
        get() = questions.count { it.masteryStatus == QuestionMastery.NEEDS_REVIEW }

    val unattemptedCount: Int
        get() = questions.count { it.masteryStatus == QuestionMastery.UNATTEMPTED }

    val masteryPercentage: Float
        get() = if (questions.isEmpty()) 0f else (masteredQuestionsCount.toFloat() / questions.size.toFloat()) * 100f

    val daysRemaining: Int
        get() {
            val diffMillis = examDateMillis - System.currentTimeMillis()
            return if (diffMillis <= 0) 0 else ((diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1)
        }
}

data class SprintQuestion(
    val id: String,
    val questionNumber: Int,
    val questionText: String,
    val marks: Int = 10,
    val topicOrModule: String = "",
    val studentAnswer: String? = null,
    val aiModelAnswer: String = "",
    val isUserAnswerCustomized: Boolean = false,
    val quickRecall: String = "",
    val markingRubric: List<String> = emptyList(),
    val masteryStatus: QuestionMastery = QuestionMastery.UNATTEMPTED,
    val lastPracticedAt: Long? = null
) {
    /** The effective answer displayed to the student (student's answer takes priority if present) */
    val effectiveAnswer: String
        get() = if (!studentAnswer.isNullOrBlank()) studentAnswer else aiModelAnswer
}

enum class QuestionMastery {
    UNATTEMPTED,
    NEEDS_REVIEW,
    MASTERED
}

data class SprintMockTestSession(
    val id: String,
    val sprintId: String,
    val questions: List<SprintQuestion>,
    val durationMinutes: Int,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val completedTimeMillis: Long? = null,
    val scoredMarks: Map<String, Int> = emptyMap(),
    val totalScored: Int = 0,
    val maxMarks: Int = 50
)

