package com.kletaq.app.domain.achievement

import com.kletaq.app.domain.progression.AchievementTier

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val category: String,
    val tier: AchievementTier,
    val xpReward: Int,
    val target: Int,
    val progress: Int,
    val unlocked: Boolean,
    val claimed: Boolean
)
