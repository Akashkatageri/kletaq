package com.kletaq.app.data.model

data class QuestSubtopic(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 30,
    val prerequisiteId: String? = null
)

data class LessonQuest(
    val topicId: String,
    val topicTitle: String,
    val subjectName: String,
    val semesterName: String,
    val iconEmoji: String = "📐",
    val subtopics: List<QuestSubtopic> = emptyList()
) {
    val completedCount: Int
        get() = subtopics.count { it.isCompleted }

    val totalCount: Int
        get() = subtopics.size

    val progressPercentage: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    val isFullyMastered: Boolean
        get() = totalCount > 0 && completedCount == totalCount
}
