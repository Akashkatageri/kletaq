package com.studyos.app.widgets.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.studyos.app.widgets.ui.PandaDashboardWidget
import com.studyos.app.widgets.ui.PandaMiniWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Lightweight SharedPreferences cache for widget data.
 * Written by the app whenever UserStats snapshot fires;
 * read by Glance widgets without needing Firebase.
 */
object WidgetDataHelper {

    private const val TAG = "WidgetSync"
    private const val PREFS_NAME = "klytaq_widget_prefs"

    val STREAK_KEY = intPreferencesKey("streak")
    val TODAY_XP_KEY = longPreferencesKey("todayXp")
    val COMPLETED_TASKS_KEY = intPreferencesKey("completedTasks")
    val SUBJECT_KEY = stringPreferencesKey("subject")

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
        Log.d(TAG, "Cache write: streak=$streak, todayXp=$todayXp, completedTasks=$completedTasks")
        prefs(ctx).edit()
            .putInt("streak", streak)
            .putLong("todayXp", todayXp)
            .putLong("totalXp", totalXp)
            .putInt("level", level)
            .putInt("completedTasks", completedTasks)
            .putString("currentSubject", currentSubject)
            .putLong("lastUpdated", System.currentTimeMillis())
            .commit()
    }

    fun clear(ctx: Context) {
        Log.d(TAG, "Cache write: clearing widget data on logout")
        prefs(ctx).edit().clear().commit()
    }

    fun clearAndRefresh(ctx: Context) {
        clear(ctx)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val glanceManager = GlanceAppWidgetManager(ctx)
                glanceManager.getGlanceIds(PandaMiniWidget::class.java).forEach { glanceId ->
                    updateAppWidgetState(ctx, glanceId) { prefs ->
                        prefs.clear()
                    }
                    PandaMiniWidget().update(ctx, glanceId)
                }
                glanceManager.getGlanceIds(PandaDashboardWidget::class.java).forEach { glanceId ->
                    updateAppWidgetState(ctx, glanceId) { prefs ->
                        prefs.clear()
                    }
                    PandaDashboardWidget().update(ctx, glanceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing Glance preferences on logout", e)
            }
            refreshWidgets(ctx)
        }
    }

    suspend fun refreshWidgets(ctx: Context) {
        try {
            val streak = getStreak(ctx)
            val todayXp = getTodayXp(ctx)
            val completedTasks = getCompletedTasks(ctx)
            val subject = getCurrentSubject(ctx)

            val glanceManager = GlanceAppWidgetManager(ctx)

            // Update PandaMiniWidget instances
            val miniGlanceIds = glanceManager.getGlanceIds(PandaMiniWidget::class.java)
            miniGlanceIds.forEach { glanceId ->
                updateAppWidgetState(ctx, glanceId) { prefs ->
                    prefs[STREAK_KEY] = streak
                }
                PandaMiniWidget().update(ctx, glanceId)
            }

            // Update PandaDashboardWidget instances
            val dashGlanceIds = glanceManager.getGlanceIds(PandaDashboardWidget::class.java)
            dashGlanceIds.forEach { glanceId ->
                updateAppWidgetState(ctx, glanceId) { prefs ->
                    prefs[STREAK_KEY] = streak
                    prefs[TODAY_XP_KEY] = todayXp
                    prefs[COMPLETED_TASKS_KEY] = completedTasks
                    prefs[SUBJECT_KEY] = subject
                }
                PandaDashboardWidget().update(ctx, glanceId)
            }

            PandaMiniWidget().updateAll(ctx)
            PandaDashboardWidget().updateAll(ctx)

            // Force immediate update broadcast on custom launchers (e.g., OxygenOS/OnePlus, OneUI)
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(ctx)

            val miniComponent = android.content.ComponentName(ctx, com.studyos.app.widgets.ui.PandaMiniWidgetReceiver::class.java)
            val miniIds = appWidgetManager.getAppWidgetIds(miniComponent)
            if (miniIds.isNotEmpty()) {
                val intent = android.content.Intent(ctx, com.studyos.app.widgets.ui.PandaMiniWidgetReceiver::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, miniIds)
                }
                ctx.sendBroadcast(intent)
            }

            val dashComponent = android.content.ComponentName(ctx, com.studyos.app.widgets.ui.PandaDashboardWidgetReceiver::class.java)
            val dashIds = appWidgetManager.getAppWidgetIds(dashComponent)
            if (dashIds.isNotEmpty()) {
                val intent = android.content.Intent(ctx, com.studyos.app.widgets.ui.PandaDashboardWidgetReceiver::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, dashIds)
                }
                ctx.sendBroadcast(intent)
            }

            Log.d(TAG, "Widget refresh triggered for ${miniGlanceIds.size} mini and ${dashGlanceIds.size} dashboard widgets")
        } catch (e: Exception) {
            Log.e(TAG, "Failure during widget refresh: ${e.message}", e)
        }
    }

    /* ---------- read (called from widgets) ---------- */

    fun getStreak(ctx: Context): Int = prefs(ctx).getInt("streak", 0)
    fun getTodayXp(ctx: Context): Long = prefs(ctx).getLong("todayXp", 0L)
    fun getTotalXp(ctx: Context): Long = prefs(ctx).getLong("totalXp", 0L)
    fun getLevel(ctx: Context): Int = prefs(ctx).getInt("level", 1)
    fun getCompletedTasks(ctx: Context): Int = prefs(ctx).getInt("completedTasks", 0)
    fun getCurrentSubject(ctx: Context): String = prefs(ctx).getString("currentSubject", "No subject") ?: "No subject"
    fun hasStudiedToday(ctx: Context): Boolean = getTodayXp(ctx) > 0L || getCompletedTasks(ctx) > 0

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

    enum class PandaMood(
        val id: String,
        val title: String,
        val text: String,
        val backgroundResId: Int,
        val pandaDrawableResId: Int,
        val startColorHex: String,
        val endColorHex: String
    ) {
        NO_STUDY_TODAY(
            id = "no_study",
            title = "No study today",
            text = "Ready when you are",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_no_study,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_ready,
            startColorHex = "#8B5CF6",
            endColorHex = "#2563EB"
        ),
        STREAK_ACTIVE(
            id = "streak_active",
            title = "Streak active",
            text = "Keep the streak alive!",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_streak_active,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_streak,
            startColorHex = "#FF6B00",
            endColorHex = "#EC4899"
        ),
        BACKLOG_SESSION_NEXT(
            id = "backlog_next",
            title = "Backlog session next",
            text = "Next: Numerical Methods",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_backlog_session,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_study,
            startColorHex = "#4F46E5",
            endColorHex = "#2563EB"
        ),
        GOAL_COMPLETED(
            id = "goal_completed",
            title = "Goal completed",
            text = "You did it today!",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_goal_completed,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_complete,
            startColorHex = "#10B981",
            endColorHex = "#0D9488"
        ),
        EVENING_NOT_STUDIED(
            id = "evening_not_studied",
            title = "Evening, not studied",
            text = "A little study goes far",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_evening_not_studied,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_sleepy,
            startColorHex = "#0F172A",
            endColorHex = "#7C3AED"
        ),
        RETURNED_AFTER_DAYS(
            id = "returned_away",
            title = "Returned after days away",
            text = "Welcome back",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_returned_away,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_welcome,
            startColorHex = "#F59E0B",
            endColorHex = "#F43F5E"
        ),
        EXAM_REVISION(
            id = "exam_revision",
            title = "Exam / revision period",
            text = "Revision time",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_exam_revision,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_revision,
            startColorHex = "#EF4444",
            endColorHex = "#8B5CF6"
        ),
        NO_CURRENT_PLAN(
            id = "no_plan",
            title = "No current plan",
            text = "Pick your next subject",
            backgroundResId = com.studyos.app.R.drawable.widget_bg_no_plan,
            pandaDrawableResId = com.studyos.app.R.drawable.panda_curious,
            startColorHex = "#0EA5E9",
            endColorHex = "#A855F7"
        );

        companion object {
            fun fromId(id: String?): PandaMood? = values().find { it.id == id }
        }
    }

    fun getMood(context: Context): PandaMood {
        val streak = getStreak(context)
        val todayXp = getTodayXp(context)
        val completedTasks = getCompletedTasks(context)
        val currentSubject = getCurrentSubject(context)
        val daysAway = prefs(context).getInt("days_away", 0)
        val isRevisionActive = prefs(context).getBoolean("is_revision_active", false)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return when {
            isRevisionActive -> PandaMood.EXAM_REVISION
            daysAway >= 2 -> PandaMood.RETURNED_AFTER_DAYS
            completedTasks >= 3 || todayXp >= 100 -> PandaMood.GOAL_COMPLETED
            streak > 0 -> PandaMood.STREAK_ACTIVE
            currentSubject.isNotBlank() && currentSubject != "No subject" -> PandaMood.BACKLOG_SESSION_NEXT
            hour >= 18 && todayXp == 0L -> PandaMood.EVENING_NOT_STUDIED
            currentSubject.isBlank() || currentSubject == "No subject" -> PandaMood.NO_CURRENT_PLAN
            else -> PandaMood.NO_STUDY_TODAY
        }
    }

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

