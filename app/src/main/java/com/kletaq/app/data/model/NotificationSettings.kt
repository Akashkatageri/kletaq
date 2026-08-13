package com.kletaq.app.data.model

data class NotificationSettings(
    val studyReminders: Boolean = true,
    val streakWarnings: Boolean = true,
    val examReminders: Boolean = true,
    val assignmentNotifications: Boolean = true,
    val dailyTaskNotifications: Boolean = true
)
