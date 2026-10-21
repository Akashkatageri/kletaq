package com.kletaq.app.data.model

import com.google.firebase.firestore.Exclude

data class ClassmateActivity(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val title: String = "",
    val subjectName: String = "",
    val type: String = "study", // "study", "focus", "quiz", "streak", "revision"
    val xpEarned: Long = 0L,
    val durationMinutes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String = "",
    val cheerCount: Int = 0,
    val cheeredUserIds: List<String> = emptyList(),

    @get:Exclude
    val isCurrentUser: Boolean = false,
    @get:Exclude
    val isCheeredByMe: Boolean = false
)
