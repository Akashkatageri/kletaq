package com.studyos.app.widgets.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight SharedPreferences cache for widget data.
 * Written by the app whenever UserStats snapshot fires;
 * read by Glance widgets without needing Firebase.
 */
object WidgetDataHelper {

    private const val PREFS_NAME = "klytaq_widget_prefs"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /* ---------- write (called from app) ---------- */

    fun saveStats(
        ctx: Context,
        streak: Int,
        todayXp: Long,
        totalXp: Long,
        level: Int,
        completedTasks: Int,
        currentSubject: String
    ) {
        prefs(ctx).edit()
            .putInt("streak", streak)
            .putLong("todayXp", todayXp)
            .putLong("totalXp", totalXp)
            .putInt("level", level)
            .putInt("completedTasks", completedTasks)
            .putString("currentSubject", currentSubject)
            .putLong("lastUpdated", System.currentTimeMillis())
            .apply()
    }

    /* ---------- read (called from widgets) ---------- */

    fun getStreak(ctx: Context): Int = prefs(ctx).getInt("streak", 0)
    fun getTodayXp(ctx: Context): Long = prefs(ctx).getLong("todayXp", 0L)
    fun getTotalXp(ctx: Context): Long = prefs(ctx).getLong("totalXp", 0L)
    fun getLevel(ctx: Context): Int = prefs(ctx).getInt("level", 1)
    fun getCompletedTasks(ctx: Context): Int = prefs(ctx).getInt("completedTasks", 0)
    fun getCurrentSubject(ctx: Context): String = prefs(ctx).getString("currentSubject", "No subject") ?: "No subject"

    fun getMotivationalMessage(): String {
        val messages = listOf(
            "Keep going! 🐼",
            "Study a little today 🌱",
            "You're doing great ✨",
            "One step at a time 🎋",
            "Proud of you! 🐾",
            "Stay curious 📚",
            "Almost there! 🌟",
            "Panda believes in you 💜"
        )
        return messages.random()
    }

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    enum class TimePeriod { MORNING, AFTERNOON, EVENING }

    data class WidgetTheme(
        val period: TimePeriod,
        val backgroundDrawableResId: Int,
        val backgroundColor: androidx.compose.ui.graphics.Color,
        val cardOverlayColor: androidx.compose.ui.graphics.Color,
        val primaryTextColor: androidx.compose.ui.graphics.Color,
        val secondaryTextColor: androidx.compose.ui.graphics.Color,
        val accentColor: androidx.compose.ui.graphics.Color,
        val greetingText: String
    )

    fun getCurrentWidgetTheme(): WidgetTheme {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> WidgetTheme(
                period = TimePeriod.MORNING,
                backgroundDrawableResId = com.studyos.app.R.drawable.widget_bg_morning,
                backgroundColor = androidx.compose.ui.graphics.Color(0xFF0284C7), // Vibrant Morning Sky Blue
                cardOverlayColor = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                primaryTextColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                secondaryTextColor = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                accentColor = androidx.compose.ui.graphics.Color(0xFFBAE6FD),
                greetingText = "Good morning!"
            )
            in 12..17 -> WidgetTheme(
                period = TimePeriod.AFTERNOON,
                backgroundDrawableResId = com.studyos.app.R.drawable.widget_bg_afternoon,
                backgroundColor = androidx.compose.ui.graphics.Color(0xFF7C3AED), // Vibrant Afternoon Purple
                cardOverlayColor = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                primaryTextColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                secondaryTextColor = androidx.compose.ui.graphics.Color(0xFFF3E8FF),
                accentColor = androidx.compose.ui.graphics.Color(0xFFE9D5FF),
                greetingText = "Good afternoon!"
            )
            else -> WidgetTheme(
                period = TimePeriod.EVENING,
                backgroundDrawableResId = com.studyos.app.R.drawable.widget_bg_evening,
                backgroundColor = androidx.compose.ui.graphics.Color(0xFF1E1B4B), // Deep Evening Night Violet
                cardOverlayColor = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                primaryTextColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                secondaryTextColor = androidx.compose.ui.graphics.Color(0xFFC7D2FE),
                accentColor = androidx.compose.ui.graphics.Color(0xFFA5B4FC),
                greetingText = "Good evening!"
            )
        }
    }
}
