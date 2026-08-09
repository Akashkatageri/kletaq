package com.studyos.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.studyos.app.core.util.LocalNotificationHelper
import com.studyos.app.data.repository.UserSettingsRepository

class SpacedRepetitionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Exit safely if signed out
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            return Result.success()
        }

        // 2. Respect system POST_NOTIFICATIONS permission
        if (!LocalNotificationHelper.isPermissionGranted(applicationContext)) {
            return Result.success()
        }

        // 3. Respect user settings
        val settings = UserSettingsRepository.userSettingsState.value
        if (!settings.morningReminderEnabled || settings.morningReminderTime.isBlank()) {
            return Result.success()
        }

        // 4. Prevent duplicate daily reminders
        if (!LocalNotificationHelper.shouldShowDailyReminder(applicationContext, "spaced_repetition")) {
            return Result.success()
        }

        // 5. Post daily spaced-repetition reminder
        LocalNotificationHelper.showNotification(
            context = applicationContext,
            title = "📚 Spaced Repetition Due",
            message = "You have study cards ready for review today! Keep your memory sharp.",
            type = "study"
        )
        LocalNotificationHelper.recordReminderShown(applicationContext, "spaced_repetition")

        return Result.success()
    }
}
