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

    fun scheduleMidnightStreakWork(context: Context) {
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1) // 12:01 AM
        val initialDelaySeconds = java.time.Duration.between(now, nextMidnight).seconds.coerceAtLeast(10L)

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
    }
}
