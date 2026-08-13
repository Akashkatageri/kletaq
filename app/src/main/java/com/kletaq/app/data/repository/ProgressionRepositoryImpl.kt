package com.kletaq.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.data.model.LeaderboardEntry
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.domain.progression.AchievementTier
import com.kletaq.app.domain.progression.ProgressionCalculator
import com.kletaq.app.domain.progression.TopicDifficulty
import com.kletaq.app.domain.streak.StreakManager
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
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
                val initialStats = UserStats()
                statsRef.set(initialStats).await()
                statsRef.update("updatedAt", FieldValue.serverTimestamp()).await()
                Result.success(initialStats)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Single Atomic Completion Pipeline for all study activities in Klytaq.
     *
     * In ONE transaction:
     *   1. Checks idempotency via users/{uid}/processedSessions/{sessionId}
     *   2. Reads stats/{uid} document ONCE
     *   3. Computes streak BEFORE updating lastStudyDate using pure StreakManager
     *   4. Computes XP, level, daily habits, histograms & counters
     *   5. Writes stats/{uid} ONCE
     */
    override suspend fun completeStudySession(
        uid: String,
        sessionId: String,
        xpAmount: Long,
        focusMinutes: Int,
        sessionSource: String,
        referenceId: String,
        nowTimestamp: Long
    ): Result<UserStats> {
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("User ID empty"))
        val effectiveSessionId = sessionId.ifBlank { UUID.randomUUID().toString() }

        return try {
            val statsRef = firestore.collection("stats").document(uid)
            val sessionRef = firestore.collection("users").document(uid).collection("processedSessions").document(effectiveSessionId)

            var isAlreadyProcessed = false

            val updatedStats = firestore.runTransaction { tx ->
                // 1. Idempotency Check: check if sessionId was already processed
                val sessionSnap = tx.get(sessionRef)
                if (sessionSnap.exists()) {
                    isAlreadyProcessed = true
                    val statsSnap = tx.get(statsRef)
                    return@runTransaction if (statsSnap.exists()) statsSnap.toUserStatsSafe() ?: UserStats() else UserStats()
                }

                // 2. Read current stats document ONCE
                val statsSnap = tx.get(statsRef)
                val current = if (statsSnap.exists()) statsSnap.toUserStatsSafe() ?: UserStats() else UserStats()

                // 3. Compute Streak BEFORE updating lastStudyDate using pure StreakManager
                val streakResult = StreakManager.computeNextStreak(
                    currentStreak = current.studyStreak,
                    longestStreak = current.longestStreak,
                    lastStudyDate = current.lastStudyDate,
                    nowTimestamp = nowTimestamp
                )

                // 4. Compute XP & Level
                val newTotalXp = (current.totalXp + xpAmount).coerceAtLeast(0L)
                val newWeeklyXp = (current.weeklyXp + xpAmount).coerceAtLeast(0L)
                val newMonthlyXp = (current.monthlyXp + xpAmount).coerceAtLeast(0L)
                val newLevel = ProgressionCalculator.calculateLevel(newTotalXp)

                // Dates & Keys for Daily Tracking
                val date = Date(nowTimestamp)
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                val hourKey = SimpleDateFormat("HH", Locale.US).format(date)
                val weekdayKey = SimpleDateFormat("EEE", Locale.US).format(date)

                // 5. Daily XP & Daily Minutes
                val dailyXpMap = current.dailyXp.toMutableMap()
                dailyXpMap[todayKey] = (dailyXpMap[todayKey] ?: 0L) + xpAmount

                val dailyMinsMap = current.dailyStudyMinutes.toMutableMap()
                dailyMinsMap[todayKey] = (dailyMinsMap[todayKey] ?: 0) + focusMinutes

                // 6. Histograms
                val hourHist = current.hourHistogram.toMutableMap()
                hourHist[hourKey] = (hourHist[hourKey] ?: 0) + 1

                val weekdayHist = current.weekdayHistogram.toMutableMap()
                weekdayHist[weekdayKey] = (weekdayHist[weekdayKey] ?: 0) + 1

                // 7. Daily Habits tracking
                val dailyHabitsMap = current.dailyHabits.toMutableMap()
                val existingTodayHabit = dailyHabitsMap[todayKey]?.toMutableMap() ?: mutableMapOf<String, Any>()
                existingTodayHabit["studied"] = true
                existingTodayHabit["focusMinutes"] = ((existingTodayHabit["focusMinutes"] as? Number)?.toInt() ?: 0) + focusMinutes
                existingTodayHabit["xpEarned"] = ((existingTodayHabit["xpEarned"] as? Number)?.toLong() ?: 0L) + xpAmount
                if (sessionSource.contains("Topic", ignoreCase = true) || sessionSource.contains("Subtopic", ignoreCase = true)) {
                    existingTodayHabit["questsCompleted"] = ((existingTodayHabit["questsCompleted"] as? Number)?.toInt() ?: 0) + 1
                }
                dailyHabitsMap[todayKey] = existingTodayHabit

                // 8. Construct updated UserStats
                val updated = current.copy(
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    weeklyXp = newWeeklyXp,
                    monthlyXp = newMonthlyXp,
                    studyStreak = streakResult.currentStreak,
                    longestStreak = streakResult.longestStreak,
                    lastStudyDate = streakResult.lastStudyDate,
                    totalStudyMinutes = current.totalStudyMinutes + focusMinutes,
                    totalFocusMinutes = current.totalFocusMinutes + focusMinutes,
                    studySessions = current.studySessions + (if (focusMinutes > 0 || xpAmount > 0) 1 else 0),
                    dailyXp = dailyXpMap,
                    dailyStudyMinutes = dailyMinsMap,
                    dailyHabits = dailyHabitsMap,
                    hourHistogram = hourHist,
                    weekdayHistogram = weekdayHist
                )

                // 9. Mark session processed
                tx.set(sessionRef, mapOf("processedAt" to FieldValue.serverTimestamp(), "source" to sessionSource))

                // 10. Write stats ONCE to canonical document path
                tx.set(statsRef, updated, SetOptions.merge())
                tx.update(statsRef, "updatedAt", FieldValue.serverTimestamp())
                updated
            }.await()

            if (!isAlreadyProcessed) {
                // Record XP history audit trail
                if (xpAmount != 0L) {
                    val txRef = firestore.collection("users").document(uid).collection("xpHistory").document()
                    txRef.set(
                        mapOf(
                            "id" to txRef.id,
                            "sessionId" to effectiveSessionId,
                            "amount" to xpAmount,
                            "source" to sessionSource,
                            "referenceId" to referenceId,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                updateLeaderboards(uid, updatedStats)
            }

            Result.success(updatedStats)
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
        return completeStudySession(
            uid = uid,
            sessionId = UUID.randomUUID().toString(),
            xpAmount = amount,
            focusMinutes = 0,
            sessionSource = source,
            referenceId = referenceId
        )
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

                transaction.set(statsRef, updated, SetOptions.merge())
                transaction.update(statsRef, "updatedAt", FieldValue.serverTimestamp())
                updated
            }.await()

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

            completeStudySession(
                uid = uid,
                sessionId = "subtopic_$subtopicId",
                xpAmount = xpAmount,
                focusMinutes = 20,
                sessionSource = if (isWithinEstimatedTime) "Subtopic Completed On-Time" else "Subtopic Completed (Extended)",
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

            if (topicSnapshot.exists()) {
                return getUserStats(uid)
            }

            topicRef.set(
                mapOf(
                    "topicId" to topicId,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val statsRef = firestore.collection("stats").document(uid)
            firestore.runTransaction { tx ->
                val snap = tx.get(statsRef)
                val current = if (snap.exists()) snap.toUserStatsSafe() ?: UserStats() else UserStats()
                val updatedKeys = (current.completedTopicKeys + topicId).distinct()
                val updates = mapOf(
                    "totalTopicsCompleted" to current.totalTopicsCompleted + 1,
                    "completedTopicKeys" to updatedKeys
                )
                tx.set(statsRef, updates, SetOptions.merge())
            }.await()

            val topicXp = ProgressionCalculator.xpForTopicCompletion(difficulty)
            completeStudySession(
                uid = uid,
                sessionId = "topic_$topicId",
                xpAmount = topicXp,
                focusMinutes = 25,
                sessionSource = "Topic Completed",
                referenceId = topicId
            )
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
            completeStudySession(
                uid = uid,
                sessionId = "module_$moduleId",
                xpAmount = moduleXp,
                focusMinutes = 0,
                sessionSource = "Module Completed Bonus",
                referenceId = moduleId
            )
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

            completeStudySession(
                uid = uid,
                sessionId = "review_${UUID.randomUUID()}",
                xpAmount = reviewXp,
                focusMinutes = 15,
                sessionSource = "Spaced Repetition Review",
                referenceId = ""
            )
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
            val currentStats = getUserStats(uid).getOrDefault(UserStats())
            val focusXp = ProgressionCalculator.xpForFocusSession(
                sessionMinutes = focusMinutes,
                dailyGoalMinutes = dailyGoalMinutes,
                previousFocusMinutesToday = currentStats.totalFocusMinutes
            )

            completeStudySession(
                uid = uid,
                sessionId = "focus_${System.currentTimeMillis()}_$focusMinutes",
                xpAmount = focusXp,
                focusMinutes = focusMinutes,
                sessionSource = "Focus Session Complete",
                referenceId = "${focusMinutes}m"
            )
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
            completeStudySession(
                uid = uid,
                sessionId = "achievement_${achievementId}_${tier.name}",
                xpAmount = achXp,
                focusMinutes = 0,
                sessionSource = "Achievement Unlocked: $achievementId",
                referenceId = achievementId
            )
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
        return completeStudySession(
            uid = uid,
            sessionId = "evaluate_streak_${System.currentTimeMillis()}",
            xpAmount = 0L,
            focusMinutes = 0,
            sessionSource = "Daily Streak Evaluation",
            referenceId = ""
        )
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
