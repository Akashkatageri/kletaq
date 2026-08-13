package com.kletaq.app.features.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kletaq.app.data.model.AchievementModel
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.repository.AuthRepository
import com.kletaq.app.data.repository.ProgressRepository
import com.kletaq.app.domain.achievement.AchievementManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HeatmapDayTile(
    val dateString: String,
    val minutes: Int,
    val intensityLevel: Int // 0 (none), 1 (light), 2 (medium), 3 (vibrant), 4 (high)
)

data class WeeklyChartBar(
    val dayLabel: String, // "Mon", "Tue", etc.
    val dateString: String,
    val minutes: Int,
    val isToday: Boolean = false
)

data class XpPoint(
    val dateLabel: String,
    val xp: Long
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val currentUserId: String
        get() = authRepository.currentUser?.uid ?: ""

    val userStats: StateFlow<UserStats> =
        progressRepository.getUserStatsFlow(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())

    val achievements: StateFlow<List<AchievementModel>> = userStats.map { stats ->
        val calculated = AchievementManager.calculate(stats)
        calculated.map { ach ->
            AchievementModel(
                id = ach.id,
                title = ach.title,
                description = ach.description,
                iconEmoji = ach.iconEmoji,
                category = ach.category,
                tier = ach.tier.name,
                unlocked = ach.unlocked,
                progress = ach.progress,
                target = ach.target
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 1. Calculate Consistency Score (0-100 based on active study days in past 30 days)
    val consistencyScore: StateFlow<Int> = userStats.map { stats ->
        val past30Days = getPastNDateStrings(30)
        val activeDaysCount = past30Days.count { dateStr ->
            (stats.dailyStudyMinutes[dateStr] ?: 0) > 0 || (stats.dailyXp[dateStr] ?: 0L) > 0L
        }
        ((activeDaysCount / 30f) * 100).toInt().coerceIn(0, 100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 2. Generate 7-row x 12-week GitHub Contribution Heatmap Matrix (84 days)
    val heatmapTiles: StateFlow<List<HeatmapDayTile>> = userStats.map { stats ->
        val totalDays = 84 // 12 weeks
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -(totalDays - 1))
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val tiles = mutableListOf<HeatmapDayTile>()
        for (i in 0 until totalDays) {
            val dateStr = sdf.format(cal.time)
            val mins = stats.dailyStudyMinutes[dateStr] ?: 0
            val intensity = when {
                mins == 0 -> 0
                mins < 15 -> 1
                mins < 45 -> 2
                mins < 90 -> 3
                else -> 4
            }
            tiles.add(HeatmapDayTile(dateString = dateStr, minutes = mins, intensityLevel = intensity))
            cal.add(Calendar.DATE, 1)
        }
        tiles
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Generate Mon-Sun Weekly Bar Chart Data
    val weeklyBars: StateFlow<List<WeeklyChartBar>> = userStats.map { stats ->
        val cal = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        // Set to current week's Monday
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val bars = mutableListOf<WeeklyChartBar>()
        for (i in 0..6) {
            val dateStr = sdf.format(cal.time)
            val mins = stats.dailyStudyMinutes[dateStr] ?: 0
            bars.add(
                WeeklyChartBar(
                    dayLabel = dayNames[i],
                    dateString = dateStr,
                    minutes = mins,
                    isToday = dateStr == todayStr
                )
            )
            cal.add(Calendar.DATE, 1)
        }
        bars
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Generate XP History Graph Trend Data (Past 7 Days)
    val xpTrendPoints: StateFlow<List<XpPoint>> = userStats.map { stats ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -6)
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfLabel = SimpleDateFormat("EEE", Locale.US)

        val points = mutableListOf<XpPoint>()
        for (i in 0..6) {
            val dateStr = sdfDate.format(cal.time)
            val label = sdfLabel.format(cal.time)
            val xp = stats.dailyXp[dateStr] ?: 0L
            points.add(XpPoint(dateLabel = label, xp = xp))
            cal.add(Calendar.DATE, 1)
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getPastNDateStrings(n: Int): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0 until n) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DATE, -1)
        }
        return list
    }
}
