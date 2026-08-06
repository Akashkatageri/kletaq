package com.studyos.app.domain.achievement

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val xpReward: Int,
    val target: Int,
    val progress: Int,
    val unlocked: Boolean,
    val claimed: Boolean
)
