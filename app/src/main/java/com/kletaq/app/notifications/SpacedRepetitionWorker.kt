package com.kletaq.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.model.MemoryCard
import com.kletaq.app.data.repository.UserSettingsRepository
import kotlinx.coroutines.tasks.await

class SpacedRepetitionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return Result.success()

        if (!LocalNotificationHelper.isPermissionGranted(applicationContext)) {
            return Result.success()
        }

        val settings = UserSettingsRepository.userSettingsState.value
        if (!settings.morningReminderEnabled || settings.morningReminderTime.isBlank()) {
            return Result.success()
        }

        if (!LocalNotificationHelper.shouldShowDailyReminder(applicationContext, "spaced_repetition")) {
            return Result.success()
        }

        // Verify that there are ACTUALLY due review cards in Firestore before sending notification
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()
            val cardsSnapshot = firestore.collection("users")
                .document(currentUser.uid)
                .collection("memory_cards")
                .whereGreaterThan("topicId", "")
                .get()
                .await()

            val dueCards = cardsSnapshot.documents.mapNotNull { it.toObject(MemoryCard::class.java) }
                .filter { it.nextReview > 0L && it.nextReview <= now }

            if (dueCards.isNotEmpty()) {
                val dueCount = dueCards.size
                LocalNotificationHelper.showNotification(
                    context = applicationContext,
                    title = "📚 Spaced Repetition Due",
                    message = "You have $dueCount study card${if (dueCount > 1) "s" else ""} ready for review today! Keep your memory sharp.",
                    type = "study"
                )
                LocalNotificationHelper.recordReminderShown(applicationContext, "spaced_repetition")
            }

            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
