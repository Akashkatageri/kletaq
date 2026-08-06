package com.studyos.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.studyos.app.data.model.UserStats
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSchedulerEngine @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val firestore: FirebaseFirestore
) {
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    suspend fun evaluateStreakWarningIfNeeded(userId: String, stats: UserStats) {
        if (userId.isBlank()) return

        // Check if user studied today by comparing lastStudyDate (epoch millis) to today's date
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val studiedToday = if (stats.lastStudyDate > 0L) {
            val lastDate = java.time.Instant.ofEpochMilli(stats.lastStudyDate)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            lastDate == java.time.LocalDate.now()
        } else false

        // If user hasn't studied today and current hour >= 18 (6 PM)
        if (!studiedToday && hour >= 18 && stats.studyStreak > 0) {
            val settings = notificationRepository.getNotificationSettingsFlow(userId)
            notificationRepository.createNotification(
                userId = userId,
                title = "🔥 Streak Expiration Warning!",
                message = "Your ${stats.studyStreak}-day study streak will expire at midnight. Log a study session now!",
                type = "streak",
                entityType = "streak"
            )
        }
    }

    suspend fun evaluateDueRevisionsAlert(userId: String, dueCount: Int) {
        if (userId.isBlank() || dueCount <= 0) return
        notificationRepository.createNotification(
            userId = userId,
            title = "📚 Spaced Repetition Due",
            message = "You have $dueCount study cards ready for review today. Keep your memory sharp!",
            type = "study",
            entityType = "spaced_repetition"
        )
    }

    suspend fun evaluateMilestoneUnlockedAlert(userId: String, title: String, message: String) {
        if (userId.isBlank()) return
        notificationRepository.createNotification(
            userId = userId,
            title = title,
            message = message,
            type = "xp",
            entityType = "achievement"
        )
    }

    suspend fun checkUpcomingExams(userId: String) {
        if (userId.isBlank()) return
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("exams")
                .whereGreaterThanOrEqualTo("date", Date())
                .get()
                .await()

            for (doc in snapshot.documents) {
                val title = doc.getString("title") ?: "Upcoming Exam"
                notificationRepository.createNotification(
                    userId = userId,
                    title = "📝 Upcoming Exam Reminder",
                    message = "Don't forget to prepare for '$title' coming up soon!",
                    type = "exam",
                    entityId = doc.id,
                    entityType = "exam"
                )
            }
        } catch (_: Exception) {}
    }
}
