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

    fun cancelDailyReminderWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DAILY_REMINDER)
    }
}
