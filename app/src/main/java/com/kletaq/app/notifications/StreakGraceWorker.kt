package com.kletaq.app.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.domain.streak.StreakManager
import kotlinx.coroutines.tasks.await

/**
 * Autonomous 03:30 AM Streak Grace Worker.
 * Executes automatically at 03:30 AM (30 minutes before the 04:00 AM daily rollover boundary).
 * Dispatches a silent, low-priority notification (no audio chime/vibration) to prevent waking
 * sleeping students, while alerting active night-owl students that their streak flame is about to expire.
 */
class StreakGraceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object {
        private const val TAG = "StreakGraceWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Streak Grace Worker running at 03:30 AM...")
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("stats").document(currentUser.uid).get().await()
                val stats = snapshot.toUserStatsSafe()

                if (stats != null) {
                    val nowTimestamp = System.currentTimeMillis()
                    val zoneId = java.time.ZoneId.systemDefault()

                    val hasStudiedToday = StreakManager.hasStudiedToday(
                        lastStudyDate = stats.lastStudyDate,
                        currentTimestamp = nowTimestamp,
                        zoneId = zoneId
                    )

                    val streak = stats.studyStreak
                    if (!hasStudiedToday && streak > 0) {
                        Log.d(TAG, "User has active streak ($streak) but hasn't studied today before 4 AM cutoff.")
                        if (LocalNotificationHelper.isPermissionGranted(applicationContext)) {
                            LocalNotificationHelper.showNotification(
                                context = applicationContext,
                                title = "🛡️ 30 Minutes Left!",
                                message = "Complete a quick review before 4:00 AM to preserve your $streak-day streak flame!",
                                type = "streak",
                                isSilentLowPriority = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating streak grace warning", e)
            }
        }

        // Perpetually reschedule for the next 03:30 AM
        try {
            NotificationWorkScheduler.scheduleStreakGraceWarningWork(applicationContext)
        } catch (_: Exception) {}

        return Result.success()
    }
}
