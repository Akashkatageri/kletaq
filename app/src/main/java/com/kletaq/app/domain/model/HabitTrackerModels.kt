package com.kletaq.app.domain.model

data class DeletedTaskSnapshot(
    val taskId: String = "",
    val title: String = "",
    val categoryLabel: String = "Study",
    val categoryEmoji: String = "📚",
    val priorityLabel: String = "Medium",
    val completedDates: List<String> = emptyList(),
    val deletedAt: Long = System.currentTimeMillis()
)

data class MonthlyHabitArchiveDoc(
    val monthKey: String = "",
    val year: Int = 2026,
    val month: Int = 8,
    val taskCompletions: Map<String, List<String>> = emptyMap(),
    val deletedTaskSnapshots: Map<String, DeletedTaskSnapshot> = emptyMap(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class HabitTaskItem(
    val taskId: String,
    val title: String,
    val categoryEmoji: String,
    val categoryLabel: String,
    val completedDates: Set<String>,
    val isDeleted: Boolean = false,
    val completedDaysCount: Int = 0,
    val totalDaysInMonth: Int = 31,
    val completionPercentage: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)
