package com.kletaq.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val currentLevel: Int = 1,
    val totalXp: Long = 0L,
    val weeklyXp: Long = 0L,
    val monthlyXp: Long = 0L,
    val streak: Int = 0,
    val rank: Int = 0,
    @ServerTimestamp
    val updatedAt: Date? = null
)
