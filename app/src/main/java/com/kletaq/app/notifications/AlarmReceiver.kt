package com.kletaq.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.data.repository.UserSettingsRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Exact-alarm broadcast receiver for bulletproof daily study reminders,
 * evening streak protection warnings, and Google Calendar-style task alarms.
 * Uses AlarmManager.setExactAndAllowWhileIdle to bypass Android Doze mode.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val ACTION_MORNING_REMINDER = "com.kletaq.app.ACTION_MORNING_REMINDER"
        const val ACTION_EVENING_STREAK = "com.kletaq.app.ACTION_EVENING_STREAK"
        const val ACTION_TASK_REMINDER = "com.kletaq.app.ACTION_TASK_REMINDER"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_SUBJECT = "extra_task_subject"

        private const val REQ_CODE_MORNING = 1001
        private const val REQ_CODE_EVENING = 1002

        /**
         * Schedules the daily morning agenda reminder at user's preferred time.
         */
        fun scheduleMorningReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val userSettings = UserSettingsRepository.userSettings.value

            if (!userSettings.morningReminderEnabled) {
                cancelAlarm(context, ACTION_MORNING_REMINDER, REQ_CODE_MORNING)
                return
            }

            // Parse preferred reminder time (e.g. "9:00 AM", "09:00", etc.)
            val targetTime = parseTimeString(userSettings.morningReminderTime) ?: LocalTime.of(9, 0)
            val now = LocalDateTime.now()
            var scheduleDateTime = LocalDateTime.of(LocalDate.now(), targetTime)
            if (now.isAfter(scheduleDateTime)) {
                scheduleDateTime = scheduleDateTime.plusDays(1)
            }

            val triggerMillis = scheduleDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_MORNING_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQ_CODE_MORNING,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setExactAlarm(alarmManager, triggerMillis, pendingIntent)
            Log.d(TAG, "Morning study reminder scheduled for: $scheduleDateTime ($triggerMillis)")
        }

        /**
         * Schedules the evening streak protection alert (daily at 7:00 PM).
         */
        fun scheduleEveningStreakWarning(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = LocalDateTime.now()
            var scheduleDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(19, 0)) // 7:00 PM
            if (now.isAfter(scheduleDateTime)) {
                scheduleDateTime = scheduleDateTime.plusDays(1)
            }

            val triggerMillis = scheduleDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_EVENING_STREAK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQ_CODE_EVENING,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setExactAlarm(alarmManager, triggerMillis, pendingIntent)
            Log.d(TAG, "Evening streak warning scheduled for: $scheduleDateTime ($triggerMillis)")
        }

        /**
         * Schedules a task reminder alarm at exact scheduled time.
         */
        fun scheduleTaskReminder(context: Context, taskId: String, title: String, subject: String?, triggerEpochMillis: Long) {
            if (triggerEpochMillis <= System.currentTimeMillis()) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_TASK_REMINDER
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, title)
                putExtra(EXTRA_TASK_SUBJECT, subject ?: "")
            }

            val reqCode = taskId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setExactAlarm(alarmManager, triggerEpochMillis, pendingIntent)
            Log.d(TAG, "Task reminder scheduled for task: $title at epoch: $triggerEpochMillis")
        }

        fun cancelTaskReminder(context: Context, taskId: String) {
            cancelAlarm(context, ACTION_TASK_REMINDER, taskId.hashCode())
            Log.d(TAG, "Task reminder cancelled for task id: $taskId")
        }

        private fun setExactAlarm(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm permission restricted, falling back to windowed alarm", e)
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }

        private fun cancelAlarm(context: Context, action: String, reqCode: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AlarmReceiver::class.java).apply { this.action = action }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }

        private fun parseTimeString(timeStr: String): LocalTime? {
            if (timeStr.isBlank()) return null
            return try {
                val clean = timeStr.trim().uppercase()
                when {
                    clean.contains("AM") || clean.contains("PM") -> {
                        val fmt = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US)
                        LocalTime.parse(clean, fmt)
                    }
                    clean.contains(":") -> {
                        val parts = clean.split(":")
                        LocalTime.of(parts[0].toInt(), parts[1].toInt())
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Alarm fired with action: $action")

        when (action) {
            ACTION_MORNING_REMINDER -> {
                handleMorningReminder(context)
                // Reschedule for next day
                scheduleMorningReminder(context)
            }
            ACTION_EVENING_STREAK -> {
                handleEveningStreakWarning(context)
                // Reschedule for next day
                scheduleEveningStreakWarning(context)
            }
            ACTION_TASK_REMINDER -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: ""
                val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Study Task"
                val taskSubject = intent.getStringExtra(EXTRA_TASK_SUBJECT)

                val message = if (!taskSubject.isNullOrBlank()) {
                    "$taskSubject • Due now. Tap to review & complete."
                } else {
                    "Scheduled study task is due now."
                }

                LocalNotificationHelper.showNotification(
                    context = context,
                    title = "📅 Task Due: $taskTitle",
                    message = message,
                    type = "study",
                    entityId = taskId,
                    entityType = "task"
                )
            }
        }
    }

    private fun handleMorningReminder(context: Context) {
        val todayIso = LocalDate.now().toString()
        val todayTasks = TaskRepository.tasks.value.filter {
            !it.isDeleted && !it.isDoneToday && (it.scheduledDate == todayIso || it.dueDateText.equals("Today", ignoreCase = true))
        }

        val title = "☀️ Good Morning! Time to Study"
        val message = if (todayTasks.isNotEmpty()) {
            "You have ${todayTasks.size} task${if (todayTasks.size > 1) "s" else ""} scheduled for today. Let's make progress!"
        } else {
            "Keep up your study momentum! Start a 25-minute focus session today."
        }

        LocalNotificationHelper.showNotification(
            context = context,
            title = title,
            message = message,
            type = "study"
        )
    }

    private fun handleEveningStreakWarning(context: Context) {
        // Send urgent streak protection reminder
        LocalNotificationHelper.showNotification(
            context = context,
            title = "🔥 Streak Protection Warning!",
            message = "Your study streak expires at midnight! Log a quick focus session to keep it safe.",
            type = "streak"
        )
    }
}
