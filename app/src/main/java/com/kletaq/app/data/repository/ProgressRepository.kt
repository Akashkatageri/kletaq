package com.kletaq.app.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.data.model.AchievementModel
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.domain.achievement.AchievementManager
import com.kletaq.app.domain.streak.StreakManager
import com.kletaq.app.widgets.data.WidgetDataHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationSchedulerEngine: NotificationSchedulerEngine,
    @ApplicationContext private val context: Context
) {
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun updateWidgetCache(stats: UserStats) {
        val todayKey = getTodayDateString()
        val todayXp = stats.dailyXp[todayKey] ?: 0L
        WidgetDataHelper.saveStats(
            ctx = context,
            streak = stats.studyStreak,
            todayXp = todayXp,
            totalXp = stats.totalXp,
            level = stats.currentLevel,
            completedTasks = stats.completedTasksCount,
            currentSubject = "Study"
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            WidgetDataHelper.refreshWidgets(context)
        }
    }

    /**
     * Listens to the canonical stats document stats/{userId}.
     */
    fun getUserStatsFlow(userId: String): Flow<UserStats> = callbackFlow {
        if (userId.isBlank()) {
            trySend(UserStats())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("stats").document(userId)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(UserStats())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val stats = snapshot.toUserStatsSafe() ?: UserStats()
                updateWidgetCache(stats)
                trySend(stats)
            } else {
                trySend(UserStats())
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun getUserStats(userId: String): Result<UserStats> {
        return try {
            val ref = firestore.collection("stats").document(userId)
            val snapshot = ref.get().await()
            if (snapshot.exists()) {
                val stats = snapshot.toUserStatsSafe() ?: UserStats()
                Result.success(stats)
            } else {
                val initial = UserStats()
                ref.set(initial).await()
                Result.success(initial)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordTaskCompleted(
        userId: String,
        taskTitle: String,
        xpReward: Long,
        studyMinutes: Int
    ): Result<UserStats> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID empty"))
        return try {
            val statsRef = firestore.collection("stats").document(userId)
            val sessionId = "task_${UUID.randomUUID()}"
            val sessionRef = firestore.collection("users").document(userId).collection("processedSessions").document(sessionId)

            val updatedStats = firestore.runTransaction { tx ->
                val statsSnap = tx.get(statsRef)
                val current = if (statsSnap.exists()) statsSnap.toUserStatsSafe() ?: UserStats() else UserStats()

                val nowTimestamp = System.currentTimeMillis()
                val streakResult = StreakManager.computeNextStreak(
                    currentStreak = current.studyStreak,
                    longestStreak = current.longestStreak,
                    lastStudyDate = current.lastStudyDate,
                    nowTimestamp = nowTimestamp
                )

                val newTotalXp = current.totalXp + xpReward
                val newWeeklyXp = current.weeklyXp + xpReward
                val newMonthlyXp = current.monthlyXp + xpReward
                val newLevel = (newTotalXp / 250).toInt() + 1

                val todayKey = getTodayDateString()
                val dailyXpMap = current.dailyXp.toMutableMap()
                dailyXpMap[todayKey] = (dailyXpMap[todayKey] ?: 0L) + xpReward

                val dailyMinsMap = current.dailyStudyMinutes.toMutableMap()
                dailyMinsMap[todayKey] = (dailyMinsMap[todayKey] ?: 0) + studyMinutes

                val updated = current.copy(
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    weeklyXp = newWeeklyXp,
                    monthlyXp = newMonthlyXp,
                    studyStreak = streakResult.currentStreak,
                    longestStreak = streakResult.longestStreak,
                    lastStudyDate = streakResult.lastStudyDate,
                    totalStudyMinutes = current.totalStudyMinutes + studyMinutes,
                    dailyXp = dailyXpMap,
                    dailyStudyMinutes = dailyMinsMap,
                    completedTasksCount = current.completedTasksCount + 1
                )

                tx.set(sessionRef, mapOf("processedAt" to FieldValue.serverTimestamp(), "source" to "Task Completed: $taskTitle"))
                tx.set(statsRef, updated, SetOptions.merge())
                tx.update(statsRef, "updatedAt", FieldValue.serverTimestamp())
                updated
            }.await()

            updateWidgetCache(updatedStats)
            checkAndUnlockAchievements(userId, updatedStats)
            Result.success(updatedStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordTopicCompleted(
        userId: String,
        topicId: String,
        xpReward: Long = 50L,
        studyMinutes: Int = 25
    ): Result<UserStats> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID empty"))
        return try {
            val statsRef = firestore.collection("stats").document(userId)
            val sessionId = "topic_$topicId"
            val sessionRef = firestore.collection("users").document(userId).collection("processedSessions").document(sessionId)

            val updatedStats = firestore.runTransaction { tx ->
                val sessionSnap = tx.get(sessionRef)
                if (sessionSnap.exists()) {
                    val statsSnap = tx.get(statsRef)
                    return@runTransaction if (statsSnap.exists()) statsSnap.toUserStatsSafe() ?: UserStats() else UserStats()
                }

                val statsSnap = tx.get(statsRef)
                val current = if (statsSnap.exists()) statsSnap.toUserStatsSafe() ?: UserStats() else UserStats()

                val nowTimestamp = System.currentTimeMillis()
                val streakResult = StreakManager.computeNextStreak(
                    currentStreak = current.studyStreak,
                    longestStreak = current.longestStreak,
                    lastStudyDate = current.lastStudyDate,
                    nowTimestamp = nowTimestamp
                )

                val newTotalXp = current.totalXp + xpReward
                val newLevel = (newTotalXp / 250).toInt() + 1

                val todayKey = getTodayDateString()
                val dailyXpMap = current.dailyXp.toMutableMap()
                dailyXpMap[todayKey] = (dailyXpMap[todayKey] ?: 0L) + xpReward

                val dailyMinsMap = current.dailyStudyMinutes.toMutableMap()
                dailyMinsMap[todayKey] = (dailyMinsMap[todayKey] ?: 0) + studyMinutes

                val updated = current.copy(
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    studyStreak = streakResult.currentStreak,
                    longestStreak = streakResult.longestStreak,
                    totalTopicsCompleted = current.totalTopicsCompleted + 1,
                    totalStudyMinutes = current.totalStudyMinutes + studyMinutes,
                    dailyXp = dailyXpMap,
                    dailyStudyMinutes = dailyMinsMap,
                    lastStudyDate = streakResult.lastStudyDate
                )

                tx.set(sessionRef, mapOf("processedAt" to FieldValue.serverTimestamp(), "source" to "Topic Completed: $topicId"))
                tx.set(statsRef, updated, SetOptions.merge())
                tx.update(statsRef, "updatedAt", FieldValue.serverTimestamp())
                updated
            }.await()

            updateWidgetCache(updatedStats)
            checkAndUnlockAchievements(userId, updatedStats)
            Result.success(updatedStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAchievementsFlow(userId: String): Flow<List<AchievementModel>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users")
            .document(userId)
            .collection("achievements")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AchievementModel::class.java)?.copy(id = doc.id)
                }
                trySend(list)
            }
        }

        awaitClose { listener.remove() }
    }

    private suspend fun checkAndUnlockAchievements(userId: String, stats: UserStats) {
        try {
            val achievementsRef = firestore.collection("users")
                .document(userId)
                .collection("achievements")

            val calculated = AchievementManager.calculate(stats)
            val defaultAchievements = calculated.map { ach ->
                AchievementModel(
                    id = ach.id,
                    title = ach.title,
                    description = ach.description,
                    iconEmoji = ach.iconEmoji,
                    category = ach.category,
                    tier = ach.tier.name,
                    unlocked = ach.unlocked,
                    unlockedAt = if (ach.unlocked) Date() else null,
                    progress = ach.progress,
                    target = ach.target
                )
            }

            val batch = firestore.batch()
            for (item in defaultAchievements) {
                val doc = achievementsRef.document(item.id)
                batch.set(doc, item, SetOptions.merge())

                if (item.unlocked && (item.id == "streak_sentinel" || item.id == "streak_30" || item.id == "focus_titan" || item.id == "xp_1000" || item.id == "semester_survivor")) {
                    notificationSchedulerEngine.evaluateMilestoneUnlockedAlert(
                        userId = userId,
                        title = "🏆 Achievement Unlocked: ${item.title}",
                        message = item.description
                    )
                }
            }
            batch.commit().await()
        } catch (_: Exception) {}
    }
}
