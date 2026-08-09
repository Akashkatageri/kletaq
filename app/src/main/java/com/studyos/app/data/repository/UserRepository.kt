package com.studyos.app.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studyos.app.data.model.UserProfile
import com.studyos.app.data.model.UserStats
import com.studyos.app.data.model.toUserStatsSafe
import com.studyos.app.widgets.data.WidgetDataHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    suspend fun getUserProfile(uid: String): Result<UserProfile?>
    suspend fun getUserStats(uid: String): Result<UserStats?>
    suspend fun saveInitialUser(uid: String, email: String, displayName: String, photoUrl: String): Result<Unit>
    suspend fun updateLegalAcceptance(uid: String, termsAccepted: Boolean, privacyAccepted: Boolean): Result<Unit>
    suspend fun isUsernameAvailable(username: String): Result<Boolean>
    suspend fun setUsername(uid: String, username: String): Result<Unit>
    suspend fun setUniversity(uid: String, university: String): Result<Unit>
    suspend fun setBranch(uid: String, branch: String): Result<Unit>
    suspend fun setScheme(uid: String, scheme: String): Result<Unit>
    suspend fun setSemester(uid: String, semester: Int): Result<Unit>
    suspend fun setFirstYearCycle(uid: String, cycle: String): Result<Unit>
    suspend fun setBacklogSubjects(uid: String, backlogs: List<String>): Result<Unit>
    suspend fun setCalendarConfigured(uid: String, configured: Boolean): Result<Unit>
    suspend fun saveCalendarPreferences(uid: String, studyDaysPerWeek: Int, preferredReminderTime: String, configured: Boolean): Result<Unit>
    suspend fun updateStudyWhy(uid: String, why: String, isPinned: Boolean): Result<Unit>
    suspend fun updateStudyWhyPinned(uid: String, isPinned: Boolean): Result<Unit>
    suspend fun deleteStudyWhy(uid: String): Result<Unit>
    suspend fun setOnboardingCompleted(uid: String, completed: Boolean): Result<Unit>
    suspend fun migrateDemoAccountIfNeeded(uid: String): Result<Unit>
    suspend fun markTopicCompleted(uid: String, topicKey: String, xpEarned: Int = 80): Result<Unit>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context? = null
) : UserRepository {

    private fun syncWidgetCache(stats: UserStats?) {
        val ctx = context ?: return
        if (stats == null) return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayXp = stats.dailyXp[todayKey] ?: 0L
        WidgetDataHelper.saveStats(
            ctx = ctx,
            streak = stats.studyStreak,
            todayXp = todayXp,
            totalXp = stats.totalXp,
            level = stats.currentLevel,
            completedTasks = stats.completedTasksCount,
            currentSubject = "Study"
        )
        CoroutineScope(Dispatchers.IO).launch {
            WidgetDataHelper.refreshWidgets(ctx)
        }
    }

    private companion object {
        private const val TAG_DEBUG = "FirestoreDebug"
        private const val TAG_ERROR = "FirestoreError"
    }

    override suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val projectId = FirebaseApp.getInstance().options.projectId
            Log.d("FirebaseProject", "Active Project ID: $projectId")
            Log.d("AuthFlow", "Fetching users/$uid from Firestore...")

            val snapshot = firestore.collection("users").document(uid).get().await()
            Log.d("AuthFlow", "UID = $uid")
            Log.d("AuthFlow", "Document exists = ${snapshot.exists()}")

            if (snapshot.exists()) {
                var profile = snapshot.toObject(UserProfile::class.java)

                // Migration: Auto-heal existing users with complete onboarding fields
                if (profile != null && !profile.hasCompletedOnboarding) {
                    val hasUsername = profile.username.isNotBlank()
                    val hasUniversity = profile.university.isNotBlank()
                    val hasBranch = profile.branch.isNotBlank()
                    val hasSemester = profile.semester > 0

                    if (hasUsername && hasUniversity && hasBranch && hasSemester) {
                        Log.d("AuthFlow", "Migration triggered for users/$uid: Existing user with complete onboarding fields detected. Healing hasCompletedOnboarding = true.")
                        try {
                            firestore.collection("users").document(uid).set(
                                mapOf("hasCompletedOnboarding" to true),
                                SetOptions.merge()
                            ).await()
                            profile = profile.copy(hasCompletedOnboarding = true)
                            Log.d("AuthFlow", "Migration SUCCESSFUL: users/$uid hasCompletedOnboarding updated to true.")
                        } catch (migEx: Exception) {
                            Log.e("AuthFlow", "Migration FAILED for users/$uid", migEx)
                        }
                    }
                }

                Log.d("AuthFlow", "Profile = $profile")
                Log.d("AuthFlow", "hasCompletedOnboarding = ${profile?.hasCompletedOnboarding}")
                Result.success(profile)
            } else {
                Log.d("AuthFlow", "Profile = null")
                Log.d("AuthFlow", "hasCompletedOnboarding = false")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: getUserProfile for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserStats(uid: String): Result<UserStats?> {
        return try {
            Log.d(TAG_DEBUG, "Reading document stats/$uid")
            val snapshot = firestore.collection("stats").document(uid).get().await()
            if (snapshot.exists()) {
                val stats = snapshot.toUserStatsSafe()
                syncWidgetCache(stats)
                Log.d(TAG_DEBUG, "Read successful: stats/$uid exists")
                Result.success(stats)
            } else {
                Log.d(TAG_DEBUG, "Read completed: stats/$uid does not exist. Auto-creating stats/$uid in Firestore...")
                val initialStats = mapOf(
                    "xp" to 0,
                    "streak" to 0,
                    "level" to 1,
                    "studySessions" to 0,
                    "topicsCompleted" to 0,
                    "totalFocusMinutes" to 0,
                    "completedTasksCount" to 0,
                    "dsaCompletedTopicsCount" to 0,
                    "completedSemesters" to emptyList<Int>(),
                    "friends" to emptyList<String>(),
                    "claimedAchievementIds" to emptyList<String>(),
                    "completedTopicKeys" to emptyList<String>()
                )
                try {
                    firestore.collection("stats").document(uid).set(initialStats, SetOptions.merge()).await()
                    Log.d(TAG_DEBUG, "Write SUCCESSFUL: stats/$uid document created dynamically!")
                } catch (writeEx: Exception) {
                    Log.e(TAG_ERROR, "FAILED writing stats/$uid during getUserStats: ${writeEx.message}", writeEx)
                }
                Result.success(UserStats())
            }
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: getUserStats for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun saveInitialUser(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String
    ): Result<Unit> {
        return try {
            val projectId = FirebaseApp.getInstance().options.projectId
            Log.d("FirebaseProject", "Active Project ID: $projectId")
            Log.d("AuthFlow", "UID = $uid")

            val userRef = firestore.collection("users").document(uid)
            val userSnap = userRef.get().await()
            Log.d("AuthFlow", "Document exists = ${userSnap.exists()}")

            if (userSnap.exists()) {
                var existingProfile = userSnap.toObject(UserProfile::class.java)

                // Migration check during saveInitialUser check
                if (existingProfile != null && !existingProfile.hasCompletedOnboarding) {
                    val hasUsername = existingProfile.username.isNotBlank()
                    val hasUniversity = existingProfile.university.isNotBlank()
                    val hasBranch = existingProfile.branch.isNotBlank()
                    val hasSemester = existingProfile.semester > 0

                    if (hasUsername && hasUniversity && hasBranch && hasSemester) {
                        Log.d("AuthFlow", "Migration triggered in saveInitialUser for users/$uid: Auto-healing hasCompletedOnboarding to true.")
                        try {
                            userRef.set(mapOf("hasCompletedOnboarding" to true), SetOptions.merge()).await()
                            existingProfile = existingProfile.copy(hasCompletedOnboarding = true)
                        } catch (migEx: Exception) {
                            Log.e("AuthFlow", "Migration FAILED in saveInitialUser for users/$uid", migEx)
                        }
                    }
                }

                Log.d("AuthFlow", "Profile = $existingProfile")
                Log.d("AuthFlow", "hasCompletedOnboarding = ${existingProfile?.hasCompletedOnboarding}")
                Log.d(TAG_DEBUG, "users/$uid already exists. Skipping saveInitialUser creation.")
                return Result.success(Unit)
            }

            // BRAND-NEW USERS ONLY:
            Log.d("AuthFlow", "Brand-new user detected: initializing users/$uid with hasCompletedOnboarding = false")
            val initialData = mapOf(
                "uid" to uid,
                "email" to email,
                "photoUrl" to photoUrl,
                "hasCompletedOnboarding" to false,
                "onboardingVersion" to 2,
                "createdAt" to FieldValue.serverTimestamp()
            )
            userRef.set(initialData, SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: new users/$uid document created.")

            val statsRef = firestore.collection("stats").document(uid)
            val statsSnap = statsRef.get().await()
            if (!statsSnap.exists()) {
                Log.d(TAG_DEBUG, "stats/$uid missing. Creating initial stats/$uid...")
                val initialStats = mapOf(
                    "xp" to 0,
                    "streak" to 0,
                    "level" to 1,
                    "studySessions" to 0,
                    "topicsCompleted" to 0,
                    "totalFocusMinutes" to 0,
                    "completedTasksCount" to 0,
                    "dsaCompletedTopicsCount" to 0,
                    "completedSemesters" to emptyList<Int>(),
                    "friends" to emptyList<String>(),
                    "claimedAchievementIds" to emptyList<String>(),
                    "completedTopicKeys" to emptyList<String>()
                )
                statsRef.set(initialStats, SetOptions.merge()).await()
                Log.d(TAG_DEBUG, "Write SUCCESSFUL: stats/$uid created.")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: saveInitialUser for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun migrateDemoAccountIfNeeded(uid: String): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val isDemo = if (userSnap.exists()) userSnap.getBoolean("isDemoAccount") ?: false else false

            if (isDemo) {
                Log.d(TAG_DEBUG, "migrateDemoAccountIfNeeded triggered for demo uid: $uid")
                val statsRef = firestore.collection("stats").document(uid)
                val resetStats = mapOf(
                    "xp" to 0,
                    "streak" to 0,
                    "level" to 1,
                    "studySessions" to 0,
                    "topicsCompleted" to 0,
                    "totalFocusMinutes" to 0,
                    "completedTasksCount" to 0,
                    "dsaCompletedTopicsCount" to 0,
                    "completedSemesters" to emptyList<Int>(),
                    "friends" to emptyList<String>(),
                    "claimedAchievementIds" to emptyList<String>(),
                    "completedTopicKeys" to emptyList<String>()
                )
                statsRef.set(resetStats, SetOptions.merge()).await()
                userRef.set(
                    mapOf(
                        "isDemoAccount" to false,
                        "friends" to emptyList<String>()
                    ),
                    SetOptions.merge()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: migrateDemoAccountIfNeeded for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun markTopicCompleted(
        uid: String,
        topicKey: String,
        xpEarned: Int
    ): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting transaction write for stats/$uid topicKey: $topicKey...")
            val statsRef = firestore.collection("stats").document(uid)
            val userStatsRef = firestore.collection("users").document(uid).collection("stats").document("user_stats")
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(statsRef)
                val currentStats = if (snapshot.exists()) snapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val updatedKeys = (currentStats.completedTopicKeys + topicKey).distinct()
                val xpEarnedLong = xpEarned.toLong()
                val newTotalXp = currentStats.totalXp + xpEarnedLong
                val newLevel = com.studyos.app.domain.progression.ProgressionCalculator.calculateLevel(newTotalXp)
                val newTopicsCompleted = updatedKeys.size

                val dailyXpMap = currentStats.dailyXp.toMutableMap()
                dailyXpMap[todayKey] = (dailyXpMap[todayKey] ?: 0L) + xpEarnedLong

                val dailyMinsMap = currentStats.dailyStudyMinutes.toMutableMap()
                val studyMinsToAdd = 20
                dailyMinsMap[todayKey] = (dailyMinsMap[todayKey] ?: 0) + studyMinsToAdd

                val updates = mapOf(
                    "completedTopicKeys" to updatedKeys,
                    "totalXp" to newTotalXp,
                    "xp" to newTotalXp.toInt(),
                    "currentLevel" to newLevel,
                    "level" to newLevel,
                    "topicsCompleted" to newTopicsCompleted,
                    "totalTopicsCompleted" to newTopicsCompleted,
                    "totalStudyMinutes" to currentStats.totalStudyMinutes + studyMinsToAdd,
                    "dailyXp" to dailyXpMap,
                    "dailyStudyMinutes" to dailyMinsMap,
                    "lastStudyDate" to System.currentTimeMillis()
                )
                transaction.set(statsRef, updates, SetOptions.merge())
                transaction.set(userStatsRef, updates, SetOptions.merge())
            }.await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: stats/$uid topic completion transaction")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: markTopicCompleted for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun updateLegalAcceptance(
        uid: String,
        termsAccepted: Boolean,
        privacyAccepted: Boolean
    ): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid legal acceptance...")
            firestore.collection("users").document(uid).set(
                mapOf(
                    "termsAccepted" to termsAccepted,
                    "privacyAccepted" to privacyAccepted,
                    "acceptedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid legal acceptance")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: updateLegalAcceptance for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val cleanUsername = username.lowercase().trim()
            Log.d(TAG_DEBUG, "Reading usernames/$cleanUsername")
            val doc = firestore.collection("usernames").document(cleanUsername).get().await()
            Result.success(!doc.exists())
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: isUsernameAvailable for $username", e)
            Result.failure(e)
        }
    }

    override suspend fun setUsername(uid: String, username: String): Result<Unit> {
        return try {
            val cleanUsername = username.lowercase().trim()
            Log.d(TAG_DEBUG, "Attempting write: usernames/$cleanUsername and users/$uid...")
            val usernameRef = firestore.collection("usernames").document(cleanUsername)

            usernameRef.set(mapOf("uid" to uid), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: usernames/$cleanUsername")

            firestore.collection("users").document(uid).set(
                mapOf("username" to cleanUsername),
                SetOptions.merge()
            ).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid username")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setUsername for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setUniversity(uid: String, university: String): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid university...")
            firestore.collection("users").document(uid).set(mapOf("university" to university), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid university")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setUniversity for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setBranch(uid: String, branch: String): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid branch...")
            firestore.collection("users").document(uid).set(mapOf("branch" to branch), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid branch")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setBranch for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setScheme(uid: String, scheme: String): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid scheme...")
            firestore.collection("users").document(uid).set(mapOf("scheme" to scheme), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid scheme")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setScheme for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setSemester(uid: String, semester: Int): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid semester $semester...")
            firestore.collection("users").document(uid).set(
                mapOf(
                    "semester" to semester,
                    "currentSemester" to semester
                ),
                SetOptions.merge()
            ).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid semester $semester")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setSemester for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setFirstYearCycle(uid: String, cycle: String): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid firstYearCycle...")
            firestore.collection("users").document(uid).set(mapOf("firstYearCycle" to cycle), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid firstYearCycle")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setFirstYearCycle for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setBacklogSubjects(uid: String, backlogs: List<String>): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid backlogSubjects...")
            firestore.collection("users").document(uid).set(mapOf("backlogSubjects" to backlogs), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid backlogSubjects")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setBacklogSubjects for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setCalendarConfigured(uid: String, configured: Boolean): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid calendarConfigured...")
            firestore.collection("users").document(uid).set(mapOf("calendarConfigured" to configured), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid calendarConfigured")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setCalendarConfigured for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun saveCalendarPreferences(
        uid: String,
        studyDaysPerWeek: Int,
        preferredReminderTime: String,
        configured: Boolean
    ): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid calendar preferences...")
            val updates = mapOf(
                "calendarConfigured" to configured,
                "studyDaysPerWeek" to studyDaysPerWeek,
                "preferredReminderTime" to preferredReminderTime
            )
            firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid calendar preferences")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: saveCalendarPreferences for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun updateStudyWhy(uid: String, why: String, isPinned: Boolean): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid studyWhy...")
            val cleanWhy = why.trim()
            val updates = mapOf(
                "studyWhy" to cleanWhy,
                "isStudyWhyPinned" to isPinned,
                "studyWhyUpdatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid studyWhy")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: updateStudyWhy for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun updateStudyWhyPinned(uid: String, isPinned: Boolean): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid isStudyWhyPinned $isPinned...")
            val updates = mapOf(
                "isStudyWhyPinned" to isPinned,
                "studyWhyUpdatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid isStudyWhyPinned $isPinned")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: updateStudyWhyPinned for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteStudyWhy(uid: String): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting delete: users/$uid studyWhy...")
            val updates = mapOf(
                "studyWhy" to "",
                "isStudyWhyPinned" to false,
                "studyWhyUpdatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid studyWhy deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: deleteStudyWhy for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun setOnboardingCompleted(uid: String, completed: Boolean): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Attempting write: users/$uid onboarding state $completed...")
            firestore.collection("users").document(uid).set(mapOf("hasCompletedOnboarding" to completed), SetOptions.merge()).await()
            Log.d(TAG_DEBUG, "Write SUCCESSFUL: users/$uid onboarding state")

            val statsRef = firestore.collection("stats").document(uid)
            val statsSnap = statsRef.get().await()
            if (!statsSnap.exists()) {
                Log.d(TAG_DEBUG, "stats/$uid missing during onboarding completion. Creating stats/$uid now...")
                val initialStats = mapOf(
                    "xp" to 0,
                    "streak" to 0,
                    "level" to 1,
                    "studySessions" to 0,
                    "topicsCompleted" to 0,
                    "totalFocusMinutes" to 0,
                    "completedTasksCount" to 0,
                    "dsaCompletedTopicsCount" to 0,
                    "completedSemesters" to emptyList<Int>(),
                    "friends" to emptyList<String>(),
                    "claimedAchievementIds" to emptyList<String>(),
                    "completedTopicKeys" to emptyList<String>()
                )
                statsRef.set(initialStats, SetOptions.merge()).await()
                Log.d(TAG_DEBUG, "Write SUCCESSFUL: stats/$uid created during onboarding completion!")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Operation failed: setOnboardingCompleted for $uid", e)
            Result.failure(e)
        }
    }
}
