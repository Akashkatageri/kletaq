package com.kletaq.app.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.model.toUserStatsSafe
import kotlinx.coroutines.tasks.await

/**
 * Daily Study Reminder & Streak Expiration Warning Worker.
 * Fires daily at 7:00 PM.
 * - If the student hasn't studied today and has an active streak: sends an urgent streak expiration warning.
 * - If the student hasn't studied today and has no streak: sends a daily study invitation.
 * - Self-reschedules for the next day.
 */
class DailyStudyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object {
        private const val TAG = "DailyStudyReminder"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Daily study reminder worker running...")
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null && LocalNotificationHelper.isPermissionGranted(applicationContext)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("stats").document(currentUser.uid).get().await()
                val stats = snapshot.toUserStatsSafe()

                if (stats != null) {
                    val studiedToday = stats.isStreakActiveToday

                    if (!studiedToday) {
                        val effectiveStreak = stats.effectiveStreak
                        if (effectiveStreak > 0) {
                            LocalNotificationHelper.showNotification(
                                context = applicationContext,
                                title = "🔥 Streak Expiration Warning!",
                                message = "Your $effectiveStreak-day study streak will expire at midnight. Log a study session now!",
                                type = "streak"
                            )
                        } else {
                            LocalNotificationHelper.showNotification(
                                context = applicationContext,
                                title = "📚 Time for Today's Study!",
                                message = "Keep your momentum going! Complete a quick topic or focus session today.",
                                type = "study"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking study status in reminder worker", e)
            }
        }

        // Reschedule for next day 7:00 PM
        try {
            NotificationWorkScheduler.scheduleDailyStudyReminder(applicationContext)
        } catch (_: Exception) {}

        return Result.success()
    }
}
