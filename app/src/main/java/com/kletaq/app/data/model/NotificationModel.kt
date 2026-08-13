package com.kletaq.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class NotificationType(val value: String) {
    STUDY("study"),
    EXAM("exam"),
    ASSIGNMENT("assignment"),
    STREAK("streak"),
    QUEST("quest"),
    XP("xp");

    companion object {
        fun fromValue(value: String): NotificationType {
            return values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: STUDY
        }
    }
}

data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "study",
    val read: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null,
    val actionUrl: String? = null,
    val entityId: String? = null,
    val entityType: String? = null
)
