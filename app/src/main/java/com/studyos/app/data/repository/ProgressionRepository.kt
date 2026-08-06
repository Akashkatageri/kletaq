package com.studyos.app.data.repository

import com.studyos.app.data.model.LeaderboardEntry
import com.studyos.app.data.model.UserStats
import com.studyos.app.domain.progression.AchievementTier
import com.studyos.app.domain.progression.TopicDifficulty

interface ProgressionRepository {
    suspend fun getUserStats(uid: String): Result<UserStats>
    suspend fun awardXp(uid: String, amount: Long, source: String, referenceId: String = ""): Result<UserStats>
    suspend fun deductXp(uid: String, amount: Long, source: String, referenceId: String = ""): Result<UserStats>
    suspend fun completeSubtopic(uid: String, subtopicId: String, topicId: String, isWithinEstimatedTime: Boolean): Result<UserStats>
    suspend fun completeTopic(uid: String, topicId: String, difficulty: TopicDifficulty): Result<UserStats>
    suspend fun resetTopic(uid: String, topicId: String, difficulty: TopicDifficulty): Result<UserStats>
    suspend fun completeModule(uid: String, moduleId: String): Result<UserStats>
    suspend fun finishReview(uid: String, isOverdue: Boolean = false, isHardMastery: Boolean = false, isQueueCompleted: Boolean = false, reviewStreakDays: Int = 0): Result<UserStats>
    suspend fun finishFocusSession(uid: String, focusMinutes: Int, dailyGoalMinutes: Int): Result<UserStats>
    suspend fun claimAchievement(uid: String, achievementId: String, tier: AchievementTier): Result<UserStats>
    suspend fun evaluateDailyStreak(uid: String, isFocusGoalMet: Boolean = false, topicsCompletedToday: Int = 0, isReviewQueueCompleted: Boolean = false): Result<UserStats>
    suspend fun archiveSemesterStats(uid: String, semesterId: Int): Result<Unit>
    suspend fun getWeeklyLeaderboard(limit: Int = 50): Result<List<LeaderboardEntry>>
    suspend fun getMonthlyLeaderboard(limit: Int = 50): Result<List<LeaderboardEntry>>
    suspend fun getAllTimeLeaderboard(limit: Int = 50): Result<List<LeaderboardEntry>>
}
