package com.kletaq.app.domain.progression

enum class LeagueTier(
    val id: String,
    val displayName: String,
    val gem: String,
    val graduationWeeklyXp: Long,
    val nextTierId: String?
) {
    BRONZE("bronze", "Bronze", "🥉", 200L, "silver"),
    SILVER("silver", "Silver", "🥈", 450L, "gold"),
    GOLD("gold", "Gold", "🥇", 800L, "sapphire"),
    SAPPHIRE("sapphire", "Sapphire", "💎", 1200L, "diamond"),
    DIAMOND("diamond", "Diamond", "👑", Long.MAX_VALUE, null);

    companion object {
        fun fromString(name: String?): LeagueTier {
            val clean = name?.trim()?.lowercase() ?: "bronze"
            return entries.firstOrNull { it.id == clean || it.displayName.lowercase() == clean } ?: BRONZE
        }
    }
}

object LeagueManager {
    const val PROMOTION_ZONE_RANK = 10

    /**
     * Evaluates whether a user qualifies for promotion to the next league tier.
     * Evaluates:
     * 1. Weekly XP graduation threshold (e.g. Bronze -> Silver at 250 XP)
     * 2. Weekly Tournament rollover (top 10 promotion or active rollover with >= 100 XP)
     * 3. Lifetime XP baseline guarantee (ensures dedicated learners advance appropriately)
     */
    fun evaluateLeague(
        currentLeagueStr: String,
        weeklyXp: Long,
        totalXp: Long = 0L,
        isWeeklyReset: Boolean = false,
        prevWeeklyXp: Long = 0L,
        rankInLeague: Int = 0
    ): LeagueTier {
        val currentTier = LeagueTier.fromString(currentLeagueStr)
        var evaluatedTier = currentTier

        // 1. Weekly Tournament Rollover Promotion
        if (isWeeklyReset) {
            val wasQualifiedInPrevWeek = (rankInLeague in 1..PROMOTION_ZONE_RANK) || (prevWeeklyXp >= 100L)
            if (wasQualifiedInPrevWeek && evaluatedTier.nextTierId != null) {
                evaluatedTier = LeagueTier.fromString(evaluatedTier.nextTierId)
            }
        }

        // 2. Weekly XP Graduation Thresholds
        while (evaluatedTier.nextTierId != null && weeklyXp >= evaluatedTier.graduationWeeklyXp) {
            val nextTier = LeagueTier.fromString(evaluatedTier.nextTierId)
            if (nextTier == evaluatedTier) break
            evaluatedTier = nextTier
        }

        // 3. Lifetime XP Baseline Guarantee
        val minimumTierByTotalXp = when {
            totalXp >= 5000L -> LeagueTier.DIAMOND
            totalXp >= 2500L -> LeagueTier.SAPPHIRE
            totalXp >= 1000L -> LeagueTier.GOLD
            totalXp >= 350L -> LeagueTier.SILVER
            else -> LeagueTier.BRONZE
        }

        if (minimumTierByTotalXp.ordinal > evaluatedTier.ordinal) {
            evaluatedTier = minimumTierByTotalXp
        }

        // Tiers never demote below the user's previously earned tier
        return if (evaluatedTier.ordinal >= currentTier.ordinal) evaluatedTier else currentTier
    }

    fun getNextLeague(currentLeagueStr: String): LeagueTier? {
        val current = LeagueTier.fromString(currentLeagueStr)
        return current.nextTierId?.let { LeagueTier.fromString(it) }
    }

    fun xpNeededForNextLeague(currentLeagueStr: String, weeklyXp: Long): Long {
        val current = LeagueTier.fromString(currentLeagueStr)
        if (current.graduationWeeklyXp == Long.MAX_VALUE) return 0L
        return (current.graduationWeeklyXp - weeklyXp).coerceAtLeast(0L)
    }
}
