package com.kletaq.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AchievementModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconEmoji: String = "🏆",
    val category: String = "General",
    val tier: String = "COMMON",
    val unlocked: Boolean = false,
    @ServerTimestamp
    val unlockedAt: Date? = null,
    val progress: Int = 0,
    val target: Int = 1
)
