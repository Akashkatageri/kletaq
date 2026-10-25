package com.kletaq.app.core.di

import android.app.Application
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.repository.NotificationCoordinator
import com.kletaq.app.data.repository.ProgressRepository
import com.kletaq.app.data.repository.UserSettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.data.repository.UserRepository
import com.kletaq.app.widgets.data.WidgetDataHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.data.model.LeaderboardEntry
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class KletaqApplication : Application() {

    companion object {
        lateinit var appContext: android.content.Context
            private set
    }

    @Inject
    lateinit var notificationCoordinator: NotificationCoordinator

    @Inject
    lateinit var progressRepository: ProgressRepository

    @Inject
    lateinit var userRepository: UserRepository

    private var statsListenerRegistration: ListenerRegistration? = null
    private var lastSyncedLeaderboardEntry: LeaderboardEntry? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val fixedDebugSecret = "48b067d5-86f7-4148-9177-3e1a0b3879a6"
            val app = com.google.firebase.FirebaseApp.initializeApp(this) ?: com.google.firebase.FirebaseApp.getInstance()
            val persistenceKey = app.persistenceKey
            val prefs = getSharedPreferences("com.google.firebase.appcheck.debug.store.$persistenceKey", MODE_PRIVATE)
            prefs.edit().putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", fixedDebugSecret).apply()

            val factory = com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
            android.util.Log.i("KletaqApplication", "Installed DebugAppCheckProvider with shared token: $fixedDebugSecret")
        } catch (e: Exception) {
            android.util.Log.w("KletaqApplication", "Could not initialize AppCheck provider: ${e.message}")
        }
        appContext = applicationContext
        LocalNotificationHelper.createNotificationChannels(this)
        UserSettingsRepository.initialize(this)
        com.kletaq.app.data.repository.TaskRepository.initialize(this)
        com.kletaq.app.features.focus.FocusTimerManager.initialize(this)
        com.kletaq.app.notifications.NotificationWorkScheduler.scheduleMidnightStreakWork(this)
        com.kletaq.app.notifications.NotificationWorkScheduler.scheduleStreakGraceWarningWork(this)
        com.kletaq.app.notifications.NotificationWorkScheduler.scheduleDailyStudyReminder(this)
        com.kletaq.app.notifications.AlarmReceiver.scheduleMorningReminder(this)
        com.kletaq.app.notifications.AlarmReceiver.scheduleEveningStreakWarning(this)

        // Purge old downloaded APK updates from cache on startup to prevent storage bloat
        try {
            com.kletaq.app.core.update.AppUpdateManager.cleanUpdateCache(this)
        } catch (_: Exception) {}

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            statsListenerRegistration?.remove()
            statsListenerRegistration = null
            lastSyncedLeaderboardEntry = null

            if (currentUser != null) {
                // Sync online module notes from Cloudflare R2 / Firestore
                com.kletaq.app.core.notes.CloudflareR2Uploader.listenToCloudModuleNotes(this@KletaqApplication)

                // Reconcile any un-deducted phantom study minutes from previously reset topics
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val progressionRepo = com.kletaq.app.data.repository.ProgressionRepositoryImpl(FirebaseFirestore.getInstance())
                        progressionRepo.reconcileStudyStats(currentUser.uid)
                    } catch (_: Exception) {}
                }

                // Real-time snapshot listener on user stats to guarantee instant widget sync on login & updates
                val db = FirebaseFirestore.getInstance()
                statsListenerRegistration = db.collection("stats").document(currentUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null || !snapshot.exists()) {
                            return@addSnapshotListener
                        }
                        val stats = snapshot.toUserStatsSafe() ?: return@addSnapshotListener
                        val nowTimestamp = System.currentTimeMillis()
                        val zoneId = java.time.ZoneId.systemDefault()
                        val todayKey = com.kletaq.app.domain.streak.StreakManager.getEffectiveDateKey(nowTimestamp, zoneId)
                        val todayXp = stats.dailyXp[todayKey] ?: 0L
                        val isLitToday = stats.isStreakActiveToday

                        val pendingUpdates = mutableMapOf<String, Any>()

                        // 0. Hard Shield Normalization: Clamp legacy accounts exceeding MAX_SHIELD_CAP (3)
                        var activeShields = minOf(com.kletaq.app.domain.streak.StreakManager.MAX_SHIELD_CAP, stats.shieldsRemaining)
                        if (stats.shieldsRemaining > com.kletaq.app.domain.streak.StreakManager.MAX_SHIELD_CAP) {
                            pendingUpdates["shieldsRemaining"] = activeShields
                        }

                        // 1. Weekly Free Shields Check: Auto-refill to 3 when <= 1 (capped at 3)
                        var showRefillNotification = false
                        var refilledShieldCount = 0
                        val weeklyGrant = com.kletaq.app.domain.streak.StreakManager.calculateWeeklyShieldGrant(
                            currentShields = activeShields,
                            lastWeeklyShieldWeek = stats.lastWeeklyShieldWeek,
                            nowTimestamp = nowTimestamp,
                            zoneId = zoneId
                        )
                        if (weeklyGrant != null) {
                            val (grantedShields, currentWeek) = weeklyGrant
                            pendingUpdates["lastWeeklyShieldWeek"] = currentWeek
                            if (grantedShields > activeShields) {
                                showRefillNotification = true
                                refilledShieldCount = grantedShields
                                activeShields = grantedShields
                                pendingUpdates["shieldsRemaining"] = grantedShields
                            }
                        }

                        // 2. Autonomous Streak & Shield Evaluation (respecting 4:00 AM study rollover boundary)
                        var streakNotification: Triple<String, String, String>? = null
                        var resolvedStreak = stats.studyStreak

                        if (isLitToday) {
                            // User studied today! If streak was reset to 0 or lastStudyDate was unrecorded, restore it
                            if (stats.studyStreak == 0 || stats.lastStudyDate <= 0L) {
                                resolvedStreak = maxOf(stats.studyStreak, 1)
                                pendingUpdates["studyStreak"] = resolvedStreak
                                pendingUpdates["lastStudyDate"] = nowTimestamp
                            }
                        } else if (stats.studyStreak > 0 && stats.lastStudyDate > 0L) {
                            val lastDate = com.kletaq.app.domain.streak.StreakManager.getEffectiveStudyDate(stats.lastStudyDate, zoneId)
                            val nowDate = com.kletaq.app.domain.streak.StreakManager.getEffectiveStudyDate(nowTimestamp, zoneId)
                            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastDate, nowDate)

                            if (daysBetween >= 2L) {
                                val missedDays = (daysBetween - 1).toInt()
                                if (activeShields >= missedDays) {
                                    // Shield protects the streak!
                                    val newShields = activeShields - missedDays
                                    val protectedLastStudyDate = nowTimestamp - 86_400_000L
                                    activeShields = newShields
                                    pendingUpdates["shieldsRemaining"] = newShields
                                    pendingUpdates["lastStudyDate"] = protectedLastStudyDate
                                    streakNotification = Triple(
                                        "🛡️ Streak Shield Used!",
                                        "Your ${stats.studyStreak}-day streak was protected while you were away! Keep studying today.",
                                        "streak"
                                    )
                                } else {
                                    // Shields insufficient to cover all missed days -> streak resets to 0.
                                    // Student's remaining shields are preserved for their next streak!
                                    resolvedStreak = 0
                                    pendingUpdates["studyStreak"] = 0
                                    streakNotification = Triple(
                                        "💔 Streak Reset",
                                        "You missed days and your study streak reset to 0. Start a fresh streak today with your shields ready!",
                                        "streak"
                                    )
                                }
                            }
                        }

                        // Single atomic update to Firestore
                        if (pendingUpdates.isNotEmpty()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    db.collection("stats").document(currentUser.uid).update(pendingUpdates).await()
                                    if (showRefillNotification && LocalNotificationHelper.isPermissionGranted(this@KletaqApplication)) {
                                        LocalNotificationHelper.showNotification(
                                            context = this@KletaqApplication,
                                            title = "🛡️ Weekly Shields Refilled!",
                                            message = "Your streak shields were refilled for this week! Total shields: $refilledShieldCount/3.",
                                            type = "streak"
                                        )
                                    }
                                    if (streakNotification != null && LocalNotificationHelper.isPermissionGranted(this@KletaqApplication)) {
                                        LocalNotificationHelper.showNotification(
                                            context = this@KletaqApplication,
                                            title = streakNotification.first,
                                            message = streakNotification.second,
                                            type = streakNotification.third
                                        )
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        val widgetStreak = if (pendingUpdates.containsKey("studyStreak")) {
                            resolvedStreak
                        } else {
                            stats.effectiveStreak
                        }

                        WidgetDataHelper.saveStats(
                            ctx = this@KletaqApplication,
                            streak = widgetStreak,
                            todayXp = todayXp,
                            totalXp = stats.totalXp,
                            level = stats.currentLevel,
                            completedTasks = stats.completedTasksCount,
                            currentSubject = "Study"
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            WidgetDataHelper.refreshWidgets(this@KletaqApplication)
                            try {
                                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                                val username = userDoc.getString("username") ?: userDoc.getString("name") ?: currentUser.displayName ?: "Student"
                                val photoUrl = userDoc.getString("photoUrl") ?: currentUser.photoUrl?.toString() ?: ""
                                val globalEntry = LeaderboardEntry(
                                    uid = currentUser.uid,
                                    username = username,
                                    photoUrl = photoUrl,
                                    currentLevel = stats.currentLevel,
                                    totalXp = stats.totalXp,
                                    weeklyXp = stats.effectiveWeeklyXp,
                                    monthlyXp = stats.monthlyXp,
                                    streak = widgetStreak,
                                    league = stats.currentLeague
                                )
                                val shouldUpdate = lastSyncedLeaderboardEntry?.let { prev ->
                                    prev.totalXp != globalEntry.totalXp ||
                                    prev.weeklyXp != globalEntry.weeklyXp ||
                                    prev.streak != globalEntry.streak ||
                                    prev.currentLevel != globalEntry.currentLevel ||
                                    prev.league != globalEntry.league ||
                                    prev.username != globalEntry.username ||
                                    prev.photoUrl != globalEntry.photoUrl
                                } ?: true

                                if (shouldUpdate) {
                                    db.collection("leaderboards").document("global").collection("entries").document(currentUser.uid)
                                        .set(globalEntry, SetOptions.merge())
                                    lastSyncedLeaderboardEntry = globalEntry
                                }
                            } catch (_: Exception) {}
                        }
                    }
            } else {
                lastSyncedLeaderboardEntry = null
                // Immediate full wipe & refresh on account sign-out/switch per AGENTS.md Rule 3
                WidgetDataHelper.clearAndRefresh(this@KletaqApplication)
            }
        }
    }
}
