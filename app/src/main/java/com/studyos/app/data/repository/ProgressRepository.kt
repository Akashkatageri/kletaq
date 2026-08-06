package com.studyos.app.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studyos.app.data.model.AchievementModel
import com.studyos.app.data.model.UserStats
import com.studyos.app.data.model.toUserStatsSafe
import com.studyos.app.domain.achievement.AchievementManager
import com.studyos.app.widgets.data.WidgetDataHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
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

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
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
    }

    fun getUserStatsFlow(userId: String): Flow<UserStats> = callbackFlow {
        if (userId.isBlank()) {
            trySend(UserStats())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users")
            .document(userId)
            .collection("stats")
            .document("user_stats")

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
            val ref = firestore.collection("users")
                .document(userId)
                .collection("stats")
                .document("user_stats")

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
            val ref = firestore.collection("users")
                .document(userId)
                .collection("stats")
                .document("user_stats")

            val todayKey = getTodayDateString()
            val yesterdayKey = getYesterdayDateString()

            val updatedStats = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                val current = if (snapshot.exists()) snapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                val newTotalXp = current.totalXp + xpReward
                val newWeeklyXp = current.weeklyXp + xpReward
                val newMonthlyXp = current.monthlyXp + xpReward
                val newLevel = (newTotalXp / 250).toInt() + 1

                val lastStudyDateMillis = current.lastStudyDate
                val lastStudyLocalDate = if (lastStudyDateMillis > 0L) {
                    Instant.ofEpochMilli(lastStudyDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                } else null
                val todayLocalDate = java.time.LocalDate.now()
                val yesterdayLocalDate = todayLocalDate.minusDays(1)

                val newStreak = when (lastStudyLocalDate) {
                    todayLocalDate -> current.studyStreak
                    yesterdayLocalDate -> current.studyStreak + 1
                    else -> 1
                }
                val newLongestStreak = maxOf(current.longestStreak, newStreak)

                val dailyXpMap = current.dailyXp.toMutableMap()
                dailyXpMap[todayKey] = (dailyXpMap[todayKey] ?: 0L) + xpReward

                val dailyMinsMap = current.dailyStudyMinutes.toMutableMap()
                dailyMinsMap[todayKey] = (dailyMinsMap[todayKey] ?: 0) + studyMinutes

                val updated = current.copy(
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    weeklyXp = newWeeklyXp,
                    monthlyXp = newMonthlyXp,
                    studyStreak = newStreak,
                    longestStreak = newLongestStreak,
                    totalStudyMinutes = current.totalStudyMinutes + studyMinutes,
                    dailyXp = dailyXpMap,
                    dailyStudyMinutes = dailyMinsMap,
                    lastStudyDate = System.currentTimeMillis(),
                    completedTasksCount = current.completedTasksCount + 1
                )

                transaction.set(ref, updated, SetOptions.merge())
                transaction.update(ref, "updatedAt", FieldValue.serverTimestamp())
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
            val ref = firestore.collection("users")
                .document(userId)
                .collection("stats")
                .document("user_stats")

            val todayKey = getTodayDateString()
            val yesterdayKey = getYesterdayDateString()

            val updatedStats = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                val current = if (snapshot.exists()) snapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                val newTotalXp = current.totalXp + xpReward
                val newLevel = (newTotalXp / 250).toInt() + 1

                val lastStudyDateMillis2 = current.lastStudyDate
                val lastStudyLocalDate2 = if (lastStudyDateMillis2 > 0L) {
                    Instant.ofEpochMilli(lastStudyDateMillis2).atZone(ZoneId.systemDefault()).toLocalDate()
                } else null
                val todayLocalDate2 = java.time.LocalDate.now()
                val yesterdayLocalDate2 = todayLocalDate2.minusDays(1)

                val newStreak = when (lastStudyLocalDate2) {
                    todayLocalDate2 -> current.studyStreak
                    yesterdayLocalDate2 -> current.studyStreak + 1
                    else -> 1
                }
                val newLongestStreak = maxOf(current.longestStreak, newStreak)

                val dailyXpMap = current.dailyXp.toMutableMap()
                dailyXpMap[todayKey] = (dailyXpMap[todayKey] ?: 0L) + xpReward

                val dailyMinsMap = current.dailyStudyMinutes.toMutableMap()
                dailyMinsMap[todayKey] = (dailyMinsMap[todayKey] ?: 0) + studyMinutes

                val updated = current.copy(
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    studyStreak = newStreak,
                    longestStreak = newLongestStreak,
                    totalTopicsCompleted = current.totalTopicsCompleted + 1,
                    totalStudyMinutes = current.totalStudyMinutes + studyMinutes,
                    dailyXp = dailyXpMap,
                    dailyStudyMinutes = dailyMinsMap,
                    lastStudyDate = System.currentTimeMillis()
                )

                transaction.set(ref, updated, SetOptions.merge())
                transaction.update(ref, "updatedAt", FieldValue.serverTimestamp())
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

                // Trigger system notification for MAJOR achievements
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
