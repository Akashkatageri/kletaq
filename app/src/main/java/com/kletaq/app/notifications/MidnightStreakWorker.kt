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
import com.kletaq.app.widgets.data.WidgetDataHelper
import kotlinx.coroutines.tasks.await

/**
 * Duolingo-style automated Midnight Streak Worker.
 * Executes automatically at 12:01 AM every day without requiring the user to open the app.
 * - If the student missed yesterday completely: resets streak to 0 in Firestore & local widgets.
 * - If the student studied yesterday: keeps the streak count, resets today's XP to 0 so the flame greys out until today's session is done.
 * - Always reschedules itself for the next midnight.
 */
class MidnightStreakWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object {
        private const val TAG = "MidnightStreakWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Midnight streak evaluation running...")
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("stats").document(currentUser.uid).get().await()
                val stats = snapshot.toUserStatsSafe()

                if (stats != null) {
                    val nowTimestamp = System.currentTimeMillis()
                    val isExpired = StreakManager.isStreakExpired(stats.lastStudyDate, nowTimestamp)
                    val currentStreak = stats.studyStreak

                    if (isExpired && currentStreak > 0) {
                        if (stats.shieldsRemaining > 0) {
                            // Consume shield to protect streak
                            val newShields = stats.shieldsRemaining - 1
                            db.collection("stats").document(currentUser.uid).update(
                                mapOf(
                                    "shieldsRemaining" to newShields,
                                    "lastStudyDate" to (nowTimestamp - 86_400_000L) // Set to yesterday to stay protected for today
                                )
                            ).await()

                            WidgetDataHelper.saveStats(
                                ctx = applicationContext,
                                streak = currentStreak,
                                todayXp = 0L,
                                totalXp = stats.totalXp,
                                level = stats.currentLevel,
                                completedTasks = 0,
                                currentSubject = "Study"
                            )
                            WidgetDataHelper.refreshWidgets(applicationContext)

                            if (LocalNotificationHelper.isPermissionGranted(applicationContext)) {
                                LocalNotificationHelper.showNotification(
                                    context = applicationContext,
                                    title = "🛡️ Streak Shield Used!",
                                    message = "Your $currentStreak-day streak was protected while you were away! Keep studying today.",
                                    type = "streak"
                                )
                            }
                        } else {
                            // Streak broken - reset to 0 in Firestore
                            Log.d(TAG, "User missed yesterday without shields. Resetting streak to 0.")
                            db.collection("stats").document(currentUser.uid).update(
                                mapOf("studyStreak" to 0)
                            ).await()

                            WidgetDataHelper.saveStats(
                                ctx = applicationContext,
                                streak = 0,
                                todayXp = 0L,
                                totalXp = stats.totalXp,
                                level = stats.currentLevel,
                                completedTasks = 0,
                                currentSubject = "Study"
                            )
                            WidgetDataHelper.refreshWidgets(applicationContext)

                            if (LocalNotificationHelper.isPermissionGranted(applicationContext)) {
                                LocalNotificationHelper.showNotification(
                                    context = applicationContext,
                                    title = "💔 Streak Reset",
                                    message = "You missed yesterday and your study streak reset to 0. Start a fresh streak today!",
                                    type = "streak"
                                )
                            }
                        }
                    } else {
                        // Yesterday was studied, or streak was already 0.
                        // Reset today's XP to 0 for the new day so the flame is greyed out today until completed.
                        val effective = StreakManager.getEffectiveStreak(stats.studyStreak, stats.lastStudyDate, nowTimestamp)
                        WidgetDataHelper.saveStats(
                            ctx = applicationContext,
                            streak = effective,
                            todayXp = 0L,
                            totalXp = stats.totalXp,
                            level = stats.currentLevel,
                            completedTasks = 0,
                            currentSubject = "Study"
                        )
                        WidgetDataHelper.refreshWidgets(applicationContext)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating midnight streak", e)
            }
        }

        // Perpetually reschedule for the next midnight
        try {
            NotificationWorkScheduler.scheduleMidnightStreakWork(applicationContext)
        } catch (_: Exception) {}

        return Result.success()
    }
}
