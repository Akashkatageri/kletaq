package com.studyos.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studyos.app.data.model.LeaderboardEntry
import com.studyos.app.data.model.UserStats
import com.studyos.app.data.model.toUserStatsSafe
import com.studyos.app.domain.progression.AchievementTier
import com.studyos.app.domain.progression.ProgressionCalculator
import com.studyos.app.domain.progression.StreakEngine
import com.studyos.app.domain.progression.TopicDifficulty
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProgressionRepository {

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    override suspend fun getUserStats(uid: String): Result<UserStats> {
        return try {
            val statsRef = firestore.collection("stats").document(uid)
            val snapshot = statsRef.get().await()
            if (snapshot.exists()) {
                val stats = snapshot.toUserStatsSafe() ?: UserStats()
                Result.success(stats)
            } else {
                // Initialize default stats document using server timestamp
                val initialStats = UserStats()
                statsRef.set(initialStats).await()
                statsRef.update("updatedAt", FieldValue.serverTimestamp()).await()
                Result.success(initialStats)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun awardXp(
        uid: String,
        amount: Long,
        source: String,
        referenceId: String
    ): Result<UserStats> {
        if (amount <= 0L) return getUserStats(uid)
        return try {
            val statsRef = firestore.collection("stats").document(uid)
            val todayKey = getTodayDateString()

            val updatedStats = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(statsRef)
                val currentStats = if (snapshot.exists()) snapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                val newTotalXp = currentStats.totalXp + amount
                val newWeeklyXp = currentStats.weeklyXp + amount
                val newMonthlyXp = currentStats.monthlyXp + amount
                val newLevel = ProgressionCalculator.calculateLevel(newTotalXp)

                val currentDailyXp = currentStats.dailyXp.toMutableMap()
                val todayXp = (currentDailyXp[todayKey] ?: 0L) + amount
                currentDailyXp[todayKey] = todayXp

                val currentDailyMins = currentStats.dailyStudyMinutes.toMutableMap()
                val studyMinsToAdd = 20
                val todayMins = (currentDailyMins[todayKey] ?: 0) + studyMinsToAdd
                currentDailyMins[todayKey] = todayMins

                val updated = currentStats.copy(
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    weeklyXp = newWeeklyXp,
                    monthlyXp = newMonthlyXp,
                    totalStudyMinutes = currentStats.totalStudyMinutes + studyMinsToAdd,
                    dailyXp = currentDailyXp,
                    dailyStudyMinutes = currentDailyMins,
                    lastStudyDate = System.currentTimeMillis()
                )

                val userStatsRef = firestore.collection("users").document(uid).collection("stats").document("user_stats")
                transaction.set(statsRef, updated, SetOptions.merge())
                transaction.set(userStatsRef, updated, SetOptions.merge())
                transaction.update(statsRef, "updatedAt", FieldValue.serverTimestamp())
                transaction.update(userStatsRef, "updatedAt", FieldValue.serverTimestamp())
                updated
            }.await()

            // Audit record in subcollection: users/{uid}/xpHistory/{txId}
            val txRef = firestore.collection("users").document(uid).collection("xpHistory").document()
            val txData = mapOf(
                "id" to txRef.id,
                "amount" to amount,
                "source" to source,
                "referenceId" to referenceId,
                "timestamp" to FieldValue.serverTimestamp()
            )
            txRef.set(txData).await()

            updateLeaderboards(uid, updatedStats)
            Result.success(updatedStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deductXp(
        uid: String,
        amount: Long,
        source: String,
        referenceId: String
    ): Result<UserStats> {
        if (amount <= 0L) return getUserStats(uid)
        return try {
            val statsRef = firestore.collection("stats").document(uid)
            val todayKey = getTodayDateString()

            val updatedStats = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(statsRef)
                val currentStats = if (snapshot.exists()) snapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                val newTotalXp = (currentStats.totalXp - amount).coerceAtLeast(0L)
                val newWeeklyXp = (currentStats.weeklyXp - amount).coerceAtLeast(0L)
                val newMonthlyXp = (currentStats.monthlyXp - amount).coerceAtLeast(0L)
                val newLevel = ProgressionCalculator.calculateLevel(newTotalXp)

                val currentDailyXp = currentStats.dailyXp.toMutableMap()
                val todayXp = ((currentDailyXp[todayKey] ?: 0L) - amount).coerceAtLeast(0L)
                currentDailyXp[todayKey] = todayXp

                val updated = currentStats.copy(
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    weeklyXp = newWeeklyXp,
                    monthlyXp = newMonthlyXp,
                    dailyXp = currentDailyXp
                )

                val userStatsRef = firestore.collection("users").document(uid).collection("stats").document("user_stats")
                transaction.set(statsRef, updated, SetOptions.merge())
                transaction.set(userStatsRef, updated, SetOptions.merge())
                transaction.update(statsRef, "updatedAt", FieldValue.serverTimestamp())
                transaction.update(userStatsRef, "updatedAt", FieldValue.serverTimestamp())
                updated
            }.await()

            // Audit record for deduction
            val txRef = firestore.collection("users").document(uid).collection("xpHistory").document()
            val txData = mapOf(
                "id" to txRef.id,
                "amount" to -amount,
                "source" to source,
                "referenceId" to referenceId,
                "timestamp" to FieldValue.serverTimestamp()
            )
            txRef.set(txData).await()

            updateLeaderboards(uid, updatedStats)
            Result.success(updatedStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeSubtopic(
        uid: String,
        subtopicId: String,
        topicId: String,
        isWithinEstimatedTime: Boolean
    ): Result<UserStats> {
        return try {
            val subtopicRef = firestore.collection("users").document(uid).collection("completedSubtopics").document(subtopicId)
            val subtopicSnapshot = subtopicRef.get().await()

            val xpAmount = if (isWithinEstimatedTime) 35L else 28L

            // Record subtopic completion in subcollection
            subtopicRef.set(
                mapOf(
                    "subtopicId" to subtopicId,
                    "topicId" to topicId,
                    "completedAt" to FieldValue.serverTimestamp(),
                    "isWithinEstimatedTime" to isWithinEstimatedTime
                )
            ).await()

            // Update streak using StreakManager when a quest section is completed
            try {
                com.studyos.app.domain.streak.StreakManager(firestore).updateStreak(uid)
            } catch (_: Exception) {}

            // Award XP to stats
            awardXp(
                uid = uid,
                amount = xpAmount,
                source = if (isWithinEstimatedTime) "Subtopic Completed On-Time" else "Subtopic Completed (Extended)",
                referenceId = subtopicId
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeTopic(
        uid: String,
        topicId: String,
        difficulty: TopicDifficulty
    ): Result<UserStats> {
        return try {
            val topicRef = firestore.collection("users").document(uid).collection("completedTopics").document(topicId)
            val topicSnapshot = topicRef.get().await()

            // Deduplication check: A topic can only grant XP once
            if (topicSnapshot.exists()) {
                return getUserStats(uid)
            }

            // Record topic completion in subcollection
            topicRef.set(
                mapOf(
                    "topicId" to topicId,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Update topic count & completedTopicKeys in both stats documents
            val statsRef = firestore.collection("stats").document(uid)
            val userStatsRef = firestore.collection("users").document(uid).collection("stats").document("user_stats")

            firestore.runTransaction { tx ->
                val snap = tx.get(statsRef)
                val current = if (snap.exists()) snap.toUserStatsSafe() ?: UserStats() else UserStats()
                val updatedKeys = (current.completedTopicKeys + topicId).distinct()
                val updates = mapOf(
                    "totalTopicsCompleted" to current.totalTopicsCompleted + 1,
                    "completedTopicKeys" to updatedKeys
                )
                tx.set(statsRef, updates, SetOptions.merge())
                tx.set(userStatsRef, updates, SetOptions.merge())
            }.await()

            val topicXp = ProgressionCalculator.xpForTopicCompletion(difficulty)
            val result = awardXp(uid, topicXp, "Topic Completed", topicId)

            evaluateDailyStreak(uid, topicsCompletedToday = 1)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetTopic(
        uid: String,
        topicId: String,
        difficulty: TopicDifficulty
    ): Result<UserStats> {
        return try {
            val topicRef = firestore.collection("users").document(uid).collection("completedTopics").document(topicId)
            val topicSnapshot = topicRef.get().await()

            if (!topicSnapshot.exists()) {
                return getUserStats(uid)
            }

            topicRef.delete().await()

            val topicXp = ProgressionCalculator.xpForTopicCompletion(difficulty)
            deductXp(uid, topicXp, "Topic Reset", topicId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeModule(
        uid: String,
        moduleId: String
    ): Result<UserStats> {
        return try {
            val moduleRef = firestore.collection("users").document(uid).collection("completedModules").document(moduleId)
            val moduleSnapshot = moduleRef.get().await()

            if (moduleSnapshot.exists()) {
                return getUserStats(uid)
            }

            moduleRef.set(
                mapOf(
                    "moduleId" to moduleId,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val statsRef = firestore.collection("stats").document(uid)
            firestore.runTransaction { tx ->
                val snap = tx.get(statsRef)
                val current = if (snap.exists()) snap.toUserStatsSafe() ?: UserStats() else UserStats()
                tx.update(statsRef, "totalModulesCompleted", current.totalModulesCompleted + 1)
            }.await()

            val moduleXp = ProgressionCalculator.xpForModuleCompletion()
            awardXp(uid, moduleXp, "Module Completed Bonus", moduleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finishReview(
        uid: String,
        isOverdue: Boolean,
        isHardMastery: Boolean,
        isQueueCompleted: Boolean,
        reviewStreakDays: Int
    ): Result<UserStats> {
        return try {
            val statsRef = firestore.collection("stats").document(uid)
            firestore.runTransaction { tx ->
                val snap = tx.get(statsRef)
                val current = if (snap.exists()) snap.toUserStatsSafe() ?: UserStats() else UserStats()
                tx.update(statsRef, "totalReviewsCompleted", current.totalReviewsCompleted + 1)
            }.await()

            val reviewXp = ProgressionCalculator.xpForSpacedRepetition(
                isOverdue = isOverdue,
                isHardMastery = isHardMastery,
                isQueueCompleted = isQueueCompleted,
                reviewStreakDays = reviewStreakDays
            )
            val result = awardXp(uid, reviewXp, "Spaced Repetition Review", "")

            if (isQueueCompleted) {
                evaluateDailyStreak(uid, isReviewQueueCompleted = true)
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finishFocusSession(
        uid: String,
        focusMinutes: Int,
        dailyGoalMinutes: Int
    ): Result<UserStats> {
        return try {
            val statsRef = firestore.collection("stats").document(uid)
            val currentStats = getUserStats(uid).getOrDefault(UserStats())

            val focusXp = ProgressionCalculator.xpForFocusSession(
                sessionMinutes = focusMinutes,
                dailyGoalMinutes = dailyGoalMinutes,
                previousFocusMinutesToday = currentStats.totalFocusMinutes
            )

            firestore.runTransaction { tx ->
                val snap = tx.get(statsRef)
                val current = if (snap.exists()) snap.toUserStatsSafe() ?: UserStats() else UserStats()
                tx.update(
                    statsRef,
                    mapOf(
                        "totalFocusMinutes" to current.totalFocusMinutes + focusMinutes,
                        "studySessions" to current.studySessions + 1
                    )
                )
            }.await()

            val result = awardXp(uid, focusXp, "Focus Session Complete", "${focusMinutes}m")
            val isGoalMet = (currentStats.totalFocusMinutes + focusMinutes) >= dailyGoalMinutes

            evaluateDailyStreak(uid, isFocusGoalMet = isGoalMet)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun claimAchievement(
        uid: String,
        achievementId: String,
        tier: AchievementTier
    ): Result<UserStats> {
        return try {
            val achRef = firestore.collection("users").document(uid).collection("achievements").document(achievementId)
            val achSnapshot = achRef.get().await()

            if (achSnapshot.exists()) {
                return getUserStats(uid)
            }

            achRef.set(
                mapOf(
                    "achievementId" to achievementId,
                    "tier" to tier.name,
                    "claimedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val statsRef = firestore.collection("stats").document(uid)
            firestore.runTransaction { tx ->
                val snap = tx.get(statsRef)
                val current = if (snap.exists()) snap.toUserStatsSafe() ?: UserStats() else UserStats()
                tx.update(statsRef, "achievementsUnlocked", current.achievementsUnlocked + 1)
            }.await()

            val achXp = ProgressionCalculator.xpForAchievement(tier)
            awardXp(uid, achXp, "Achievement Unlocked: $achievementId", achievementId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun evaluateDailyStreak(
        uid: String,
        isFocusGoalMet: Boolean,
        topicsCompletedToday: Int,
        isReviewQueueCompleted: Boolean
    ): Result<UserStats> {
        return try {
            val streakManager = com.studyos.app.domain.streak.StreakManager(firestore)
            val streakData = streakManager.updateStreak(uid)

            val statsRef = firestore.collection("stats").document(uid)
            val updatedStats = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(statsRef)
                val currentStats = if (snapshot.exists()) snapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                val updated = currentStats.copy(
                    studyStreak = streakData.currentStreak,
                    longestStreak = maxOf(currentStats.longestStreak, streakData.longestStreak)
                )

                transaction.set(statsRef, updated, SetOptions.merge())
                transaction.update(statsRef, "updatedAt", FieldValue.serverTimestamp())
                updated
            }.await()

            updateLeaderboards(uid, updatedStats)
            Result.success(updatedStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun archiveSemesterStats(
        uid: String,
        semesterId: Int
    ): Result<Unit> {
        return try {
            val statsRef = firestore.collection("stats").document(uid)
            val currentStats = getUserStats(uid).getOrDefault(UserStats())

            val semesterDocRef = firestore.collection("semesterStats").document("${uid}_sem${semesterId}")
            val archiveData = mapOf(
                "uid" to uid,
                "semesterId" to semesterId,
                "archivedXp" to currentStats.totalXp,
                "archivedStreak" to currentStats.studyStreak,
                "archivedTopics" to currentStats.totalTopicsCompleted,
                "archivedAt" to FieldValue.serverTimestamp()
            )
            semesterDocRef.set(archiveData).await()

            // Semester boundary rule: reset study shields to 3 per semester
            firestore.runTransaction { tx ->
                tx.update(statsRef, "shieldsRemaining", 3)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateLeaderboards(uid: String, stats: UserStats) {
        try {
            val userRef = firestore.collection("users").document(uid)
            val userSnapshot = userRef.get().await()
            val username = userSnapshot.getString("username") ?: userSnapshot.getString("name") ?: "Student"
            val photoUrl = userSnapshot.getString("photoUrl") ?: ""

            val entry = LeaderboardEntry(
                uid = uid,
                username = username,
                photoUrl = photoUrl,
                totalXp = stats.totalXp,
                currentLevel = stats.currentLevel,
                streak = stats.studyStreak
            )

            val weeklyRef = firestore.collection("leaderboards").document("weekly").collection("entries").document(uid)
            val monthlyRef = firestore.collection("leaderboards").document("monthly").collection("entries").document(uid)
            val alltimeRef = firestore.collection("leaderboards").document("alltime").collection("entries").document(uid)

            weeklyRef.set(entry.copy(weeklyXp = stats.weeklyXp)).await()
            monthlyRef.set(entry.copy(monthlyXp = stats.monthlyXp)).await()
            alltimeRef.set(entry.copy(totalXp = stats.totalXp)).await()
        } catch (_: Exception) {}
    }

    override suspend fun getWeeklyLeaderboard(limit: Int): Result<List<LeaderboardEntry>> {
        return fetchLeaderboard("weekly", limit)
    }

    override suspend fun getMonthlyLeaderboard(limit: Int): Result<List<LeaderboardEntry>> {
        return fetchLeaderboard("monthly", limit)
    }

    override suspend fun getAllTimeLeaderboard(limit: Int): Result<List<LeaderboardEntry>> {
        return fetchLeaderboard("alltime", limit)
    }

    private suspend fun fetchLeaderboard(type: String, limit: Int): Result<List<LeaderboardEntry>> {
        return try {
            val orderField = when (type) {
                "weekly" -> "weeklyXp"
                "monthly" -> "monthlyXp"
                else -> "totalXp"
            }
            val snapshot = firestore.collection("leaderboards")
                .document(type)
                .collection("entries")
                .orderBy(orderField, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val entries = snapshot.documents.mapIndexed { index, doc ->
                val entry = doc.toObject(LeaderboardEntry::class.java) ?: LeaderboardEntry()
                entry.copy(rank = index + 1)
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
