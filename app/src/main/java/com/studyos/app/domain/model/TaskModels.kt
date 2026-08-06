package com.studyos.app.domain.model

enum class TaskPriority(val label: String, val colorHex: Long) {
    LOW("Low", 0xFF10B981),
    MEDIUM("Medium", 0xFFF59E0B),
    HIGH("High", 0xFFEF4444)
}

enum class TaskCategory(val label: String, val emoji: String) {
    STUDY("Study", "📚"),
    PERSONAL("Personal", "👤"),
    PROJECT("Project", "💻"),
    EVENT("Event", "📅")
}

enum class RepeatSchedule(val label: String) {
    NONE("No Repeat (Once)"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    EVERY_LECTURE("Every lecture"),
    BEFORE_EXAM("Before exam"),
    WEEKLY_REVISION("Weekly revision"),
    SPACED_REPETITION("Spaced repetition"),
    CUSTOM("Custom")
}

data class StudyTask(
    val id: String = System.currentTimeMillis().toString(),
    val title: String = "",
    val category: TaskCategory = TaskCategory.STUDY,
    val repeatSchedule: RepeatSchedule = RepeatSchedule.NONE,
    val dueDateText: String = "Today",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val subjectName: String? = null,
    val topicTitle: String? = null,
    val estimatedDurationMin: Int = 30,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
