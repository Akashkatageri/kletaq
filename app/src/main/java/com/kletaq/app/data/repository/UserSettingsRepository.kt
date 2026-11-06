package com.kletaq.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kletaq.app.core.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val themeMode: String = "light",
    val dailyFocusGoalMinutes: Int = 25,
    val soundEffectsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val morningReminderTime: String = "9:00 AM",
    val morningReminderEnabled: Boolean = true,
    val eveningStreakProtectionEnabled: Boolean = true,
    val semesterBreakMode: Boolean = false,
    val weeklyStudySchedule: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"),
    val semesterStartDate: String = "",
    val semesterEndDate: String = "",
    val vacationModeActive: Boolean = false,
    val vacationStartDate: String = "",
    val vacationEndDate: String = "",
    val vacationReason: String = "",
    val semesterBreaksJson: String = "[]",
    val studyShields: Int = 3,
    val examSprintModeEnabled: Boolean = false,
    val starredPriorityTopics: Set<String> = emptySet(),
    val manualTopicPagesJson: String = "{}"
)

object UserSettingsRepository {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DAILY_FOCUS_GOAL_KEY = intPreferencesKey("daily_focus_goal_minutes")
    private val SOUND_EFFECTS_KEY = booleanPreferencesKey("sound_effects_enabled")
    private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback_enabled")
    private val MORNING_REMINDER_TIME_KEY = stringPreferencesKey("morning_reminder_time")
    private val MORNING_REMINDER_ENABLED_KEY = booleanPreferencesKey("morning_reminder_enabled")
    private val EVENING_STREAK_PROTECTION_KEY = booleanPreferencesKey("evening_streak_protection_enabled")
    private val SEMESTER_BREAK_MODE_KEY = booleanPreferencesKey("semester_break_mode")
    private val WEEKLY_STUDY_SCHEDULE_KEY = stringPreferencesKey("weekly_study_schedule")
    private val SEMESTER_START_DATE_KEY = stringPreferencesKey("semester_start_date")
    private val SEMESTER_END_DATE_KEY = stringPreferencesKey("semester_end_date")
    private val VACATION_MODE_ACTIVE_KEY = booleanPreferencesKey("vacation_mode_active")
    private val VACATION_START_DATE_KEY = stringPreferencesKey("vacation_start_date")
    private val VACATION_END_DATE_KEY = stringPreferencesKey("vacation_end_date")
    private val VACATION_REASON_KEY = stringPreferencesKey("vacation_reason")
    private val SEMESTER_BREAKS_JSON_KEY = stringPreferencesKey("semester_breaks_json")
    private val STUDY_SHIELDS_KEY = intPreferencesKey("study_shields")
    private val EXAM_SPRINT_MODE_KEY = booleanPreferencesKey("exam_sprint_mode_enabled")
    private val STARRED_PRIORITY_TOPICS_KEY = androidx.datastore.preferences.core.stringSetPreferencesKey("starred_priority_topics")
    private val MANUAL_TOPIC_PAGES_KEY = stringPreferencesKey("manual_topic_pages_json")

    private var appContext: Context? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private var _userSettingsFlow: Flow<UserSettings>? = null
    var userSettingsState: StateFlow<UserSettings> = kotlinx.coroutines.flow.MutableStateFlow(UserSettings()).stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )
    val userSettings: StateFlow<UserSettings> get() = userSettingsState

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        val flow = appContext!!.dataStore.data.map { prefs ->
            val themeModeStr = prefs[THEME_MODE_KEY] ?: "light"
            val focusGoal = prefs[DAILY_FOCUS_GOAL_KEY] ?: 25
            val sound = prefs[SOUND_EFFECTS_KEY] ?: true
            val haptic = prefs[HAPTIC_FEEDBACK_KEY] ?: true
            val morningTime = prefs[MORNING_REMINDER_TIME_KEY] ?: "9:00 AM"
            val morningEnabled = prefs[MORNING_REMINDER_ENABLED_KEY] ?: true
            val eveningEnabled = prefs[EVENING_STREAK_PROTECTION_KEY] ?: true
            val semBreak = prefs[SEMESTER_BREAK_MODE_KEY] ?: false
            val scheduleStr = prefs[WEEKLY_STUDY_SCHEDULE_KEY] ?: "Monday,Tuesday,Wednesday,Thursday,Friday,Saturday"
            val scheduleList = scheduleStr.split(",").filter { it.isNotBlank() }
            val semStart = prefs[SEMESTER_START_DATE_KEY] ?: ""
            val semEnd = prefs[SEMESTER_END_DATE_KEY] ?: ""
            val vacActive = prefs[VACATION_MODE_ACTIVE_KEY] ?: false
            val vacStart = prefs[VACATION_START_DATE_KEY] ?: ""
            val vacEnd = prefs[VACATION_END_DATE_KEY] ?: ""
            val vacReason = prefs[VACATION_REASON_KEY] ?: ""
            val breaksJson = prefs[SEMESTER_BREAKS_JSON_KEY] ?: "[]"
            val shields = prefs[STUDY_SHIELDS_KEY] ?: 3
            val examSprint = prefs[EXAM_SPRINT_MODE_KEY] ?: false
            val starredTopics = prefs[STARRED_PRIORITY_TOPICS_KEY] ?: emptySet()
            val manualPages = prefs[MANUAL_TOPIC_PAGES_KEY] ?: "{}"

            ThemeManager.setThemeByName(themeModeStr)

            UserSettings(
                themeMode = themeModeStr,
                dailyFocusGoalMinutes = focusGoal,
                soundEffectsEnabled = sound,
                hapticFeedbackEnabled = haptic,
                morningReminderTime = morningTime,
                morningReminderEnabled = morningEnabled,
                eveningStreakProtectionEnabled = eveningEnabled,
                semesterBreakMode = semBreak,
                weeklyStudySchedule = scheduleList,
                semesterStartDate = semStart,
                semesterEndDate = semEnd,
                vacationModeActive = vacActive,
                vacationStartDate = vacStart,
                vacationEndDate = vacEnd,
                vacationReason = vacReason,
                semesterBreaksJson = breaksJson,
                studyShields = shields,
                examSprintModeEnabled = examSprint,
                starredPriorityTopics = starredTopics,
                manualTopicPagesJson = manualPages
            )
        }

        _userSettingsFlow = flow
        userSettingsState = flow.stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSettings()
        )
    }

    fun updateThemeMode(mode: String) {
        ThemeManager.setThemeByName(mode)
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[THEME_MODE_KEY] = mode
            }
        }
    }

    fun updateDailyFocusGoal(minutes: Int) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[DAILY_FOCUS_GOAL_KEY] = minutes
            }
        }
    }

    fun updateSoundEffectsEnabled(enabled: Boolean) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[SOUND_EFFECTS_KEY] = enabled
            }
        }
    }

    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[HAPTIC_FEEDBACK_KEY] = enabled
            }
        }
    }

    fun updateMorningReminderTime(time: String) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[MORNING_REMINDER_TIME_KEY] = time
            }
            appContext?.let { ctx ->
                val currentSettings = userSettingsState.value
                com.kletaq.app.notifications.NotificationWorkScheduler.updateDailyReminderWork(
                    context = ctx,
                    preferredReminderTime = time,
                    notificationsEnabled = currentSettings.morningReminderEnabled
                )
                com.kletaq.app.notifications.AlarmReceiver.scheduleMorningReminder(ctx)
            }
        }
    }

    fun updateMorningReminderEnabled(enabled: Boolean) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[MORNING_REMINDER_ENABLED_KEY] = enabled
            }
            appContext?.let { ctx ->
                val currentSettings = userSettingsState.value
                com.kletaq.app.notifications.NotificationWorkScheduler.updateDailyReminderWork(
                    context = ctx,
                    preferredReminderTime = currentSettings.morningReminderTime,
                    notificationsEnabled = enabled
                )
                com.kletaq.app.notifications.AlarmReceiver.scheduleMorningReminder(ctx)
            }
        }
    }

    fun updateEveningStreakProtectionEnabled(enabled: Boolean) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[EVENING_STREAK_PROTECTION_KEY] = enabled
            }
            appContext?.let { ctx ->
                if (enabled) {
                    com.kletaq.app.notifications.AlarmReceiver.scheduleEveningStreakWarning(ctx)
                }
            }
        }
    }

    fun updateSemesterBreakMode(enabled: Boolean) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[SEMESTER_BREAK_MODE_KEY] = enabled
            }
        }
    }

    fun updateStudyCalendarConfig(
        dailyFocusGoalMinutes: Int,
        weeklyStudySchedule: List<String>,
        semesterStartDate: String,
        semesterEndDate: String,
        vacationModeActive: Boolean,
        vacationStartDate: String,
        vacationEndDate: String,
        vacationReason: String,
        semesterBreaksJson: String
    ) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[DAILY_FOCUS_GOAL_KEY] = dailyFocusGoalMinutes
                prefs[WEEKLY_STUDY_SCHEDULE_KEY] = weeklyStudySchedule.joinToString(",")
                prefs[SEMESTER_START_DATE_KEY] = semesterStartDate
                prefs[SEMESTER_END_DATE_KEY] = semesterEndDate
                prefs[VACATION_MODE_ACTIVE_KEY] = vacationModeActive
                prefs[VACATION_START_DATE_KEY] = vacationStartDate
                prefs[VACATION_END_DATE_KEY] = vacationEndDate
                prefs[VACATION_REASON_KEY] = vacationReason
                prefs[SEMESTER_BREAKS_JSON_KEY] = semesterBreaksJson
            }
        }
    }

    fun updateExamSprintMode(enabled: Boolean) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                prefs[EXAM_SPRINT_MODE_KEY] = enabled
            }
        }
    }

    fun toggleTopicPriority(topicId: String) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                val current = prefs[STARRED_PRIORITY_TOPICS_KEY] ?: emptySet()
                val updated = if (current.contains(topicId)) current - topicId else current + topicId
                prefs[STARRED_PRIORITY_TOPICS_KEY] = updated
            }
        }
    }

    fun setManualTopicPageRange(topicId: String, startPage: Int, endPage: Int) {
        repositoryScope.launch {
            appContext?.dataStore?.edit { prefs ->
                val currentJson = prefs[MANUAL_TOPIC_PAGES_KEY] ?: "{}"
                val json = try {
                    org.json.JSONObject(currentJson)
                } catch (_: Exception) {
                    org.json.JSONObject()
                }
                json.put(topicId, "$startPage-$endPage")
                prefs[MANUAL_TOPIC_PAGES_KEY] = json.toString()
            }
        }
    }

    fun getManualTopicPageRange(topicId: String): IntRange? {
        val jsonStr = userSettingsState.value.manualTopicPagesJson
        if (jsonStr.isBlank() || jsonStr == "{}") return null
        return try {
            val json = org.json.JSONObject(jsonStr)
            if (json.has(topicId)) {
                val rangeStr = json.getString(topicId)
                val parts = rangeStr.split("-")
                if (parts.size == 2) {
                    val s = parts[0].toIntOrNull() ?: 1
                    val e = parts[1].toIntOrNull() ?: s
                    s..e
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

