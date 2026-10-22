package com.kletaq.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationWorkScheduler {

    private const val WORK_NAME_DAILY_REMINDER = "spaced_repetition_daily_reminder"

    fun updateDailyReminderWork(context: Context, preferredReminderTime: String, notificationsEnabled: Boolean) {
        val workManager = WorkManager.getInstance(context)

        if (!notificationsEnabled || preferredReminderTime.isBlank()) {
            workManager.cancelUniqueWork(WORK_NAME_DAILY_REMINDER)
            return
        }

        val request = PeriodicWorkRequestBuilder<SpacedRepetitionWorker>(
            24, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private const val WORK_NAME_MIDNIGHT_STREAK = "midnight_streak_evaluation"
    private const val WORK_NAME_STREAK_GRACE = "streak_grace_warning_work"

    /**
     * Schedules the autonomous streak evaluation worker at 04:01 AM (immediately following
     * the 04:00 AM daily study rollover boundary).
     */
    fun scheduleMidnightStreakWork(context: Context) {
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        var targetTime = now.toLocalDate().atTime(4, 1).atZone(zone) // 4:01 AM rollover
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelaySeconds = java.time.Duration.between(now, targetTime).seconds.coerceAtLeast(10L)

        val request = androidx.work.OneTimeWorkRequestBuilder<MidnightStreakWorker>()
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .addTag("midnight_streak_check")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_MIDNIGHT_STREAK,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Schedules a silent, low-priority pre-cutoff grace alert at 03:30 AM (30 minutes before
     * the 04:00 AM daily rollover boundary).
     */
    fun scheduleStreakGraceWarningWork(context: Context) {
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        var targetTime = now.toLocalDate().atTime(3, 30).atZone(zone) // 3:30 AM grace check
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelaySeconds = java.time.Duration.between(now, targetTime).seconds.coerceAtLeast(10L)

        val request = androidx.work.OneTimeWorkRequestBuilder<StreakGraceWorker>()
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .addTag("streak_grace_warning")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_STREAK_GRACE,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private const val WORK_NAME_DAILY_STUDY_REMINDER = "daily_study_reminder_work"

    fun scheduleDailyStudyReminder(context: Context) {
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
        var targetTime = now.toLocalDate().atTime(19, 0).atZone(now.zone) // 7:00 PM today
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1) // 7:00 PM tomorrow
        }
        val initialDelaySeconds = java.time.Duration.between(now, targetTime).seconds.coerceAtLeast(10L)

        val request = androidx.work.OneTimeWorkRequestBuilder<DailyStudyReminderWorker>()
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .addTag("daily_study_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_DAILY_STUDY_REMINDER,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelDailyReminderWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY_REMINDER)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY_STUDY_REMINDER)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_STREAK_GRACE)
    }
}
