package com.kletaq.app.features.journey.components

import androidx.compose.runtime.Immutable

enum class LessonStatus {
    COMPLETED,
    CURRENT,
    AVAILABLE,
    LOCKED
}

enum class LessonCategory(val label: String, val emoji: String) {
    CODING("Coding Lesson", "💻"),
    THEORY("Theory Lesson", "📖"),
    REVISION("Revision Lesson", "🧠"),
    LAB("Lab Exercise", "🧪"),
    QUIZ("Quiz", "❓")
}

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

@Immutable
data class Prerequisite(
    val title: String,
    val isCompleted: Boolean
)

@Immutable
data class LessonNode(
    val id: String,
    val lessonNumber: Int,
    val title: String,
    val shortTitle: String = "",
    val description: String,
    val status: LessonStatus,
    val category: LessonCategory = LessonCategory.CODING,
    val durationMinutes: Int = 15,
    val xpReward: Int = 50,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val prerequisites: List<Prerequisite> = emptyList(),
    val learnPoints: List<String> = emptyList(),
    val isBookmarked: Boolean = false,
    val isAddedToRevision: Boolean = false
)

@Immutable
data class UnitJourney(
    val id: String,
    val unitNumber: Int,
    val title: String,
    val isExpanded: Boolean = true,
    val lessons: List<LessonNode> = emptyList()
)

@Immutable
data class SubjectJourney(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val completedCount: Int,
    val totalCount: Int,
    val isBacklog: Boolean = false,
    val units: List<UnitJourney> = emptyList()
)

@Immutable
data class SemesterJourney(
    val id: String,
    val semesterNumber: Int,
    val name: String,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false,
    val progress: Float,
    val completedSubjectsCount: Int = 0,
    val subjectCount: Int,
    val subjects: List<SubjectJourney> = emptyList()
)
