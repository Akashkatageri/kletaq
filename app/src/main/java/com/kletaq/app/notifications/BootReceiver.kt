package com.kletaq.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.data.repository.UserSettingsRepository

/**
 * BroadcastReceiver triggered on system boot or app update.
 * Automatically re-arms exact alarms for daily morning reminders,
 * evening streak protection, and active scheduled task alarms.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("BootReceiver", "Boot/update received with action: $action. Re-arming alarms...")

        UserSettingsRepository.initialize(context)
        TaskRepository.initialize(context)

        // Re-arm daily morning agenda reminder
        AlarmReceiver.scheduleMorningReminder(context)

        // Re-arm evening streak protection warning
        AlarmReceiver.scheduleEveningStreakWarning(context)

        // Reschedule WorkManager fallback workers
        NotificationWorkScheduler.scheduleMidnightStreakWork(context)
        NotificationWorkScheduler.scheduleDailyStudyReminder(context)
    }
}
