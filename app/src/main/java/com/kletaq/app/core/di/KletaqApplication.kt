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
        com.kletaq.app.notifications.NotificationWorkScheduler.scheduleMidnightStreakWork(this)
        com.kletaq.app.notifications.NotificationWorkScheduler.scheduleDailyStudyReminder(this)

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            statsListenerRegistration?.remove()
            statsListenerRegistration = null

            if (currentUser != null) {
                // Real-time snapshot listener on user stats to guarantee instant widget sync on login & updates
                val db = FirebaseFirestore.getInstance()
                statsListenerRegistration = db.collection("stats").document(currentUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null || !snapshot.exists()) {
                            return@addSnapshotListener
                        }
                        val stats = snapshot.toUserStatsSafe() ?: return@addSnapshotListener
                        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val todayXp = stats.dailyXp[todayKey] ?: 0L
                        val effectiveStreak = stats.effectiveStreak
                        val isLitToday = stats.isStreakActiveToday

                        if (isLitToday) {
                            // User studied today! If streak was reset to 0 or lastStudyDate was unrecorded, restore it
                            if (stats.studyStreak == 0 || stats.lastStudyDate <= 0L) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val restoredStreak = maxOf(stats.studyStreak, 1)
                                        db.collection("stats").document(currentUser.uid).update(
                                            mapOf(
                                                "studyStreak" to restoredStreak,
                                                "lastStudyDate" to System.currentTimeMillis()
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }
                            }
                        } else if (stats.studyStreak > 0 && effectiveStreak == 0) {
                            // Truly expired (missed yesterday and did NOT study today)
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    db.collection("stats").document(currentUser.uid).update(
                                        mapOf("studyStreak" to 0)
                                    )
                                } catch (_: Exception) {}
                            }
                        }

                        WidgetDataHelper.saveStats(
                            ctx = this@KletaqApplication,
                            streak = effectiveStreak,
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
                                    weeklyXp = stats.weeklyXp,
                                    monthlyXp = stats.monthlyXp,
                                    streak = effectiveStreak
                                )
                                db.collection("leaderboards").document("global").collection("entries").document(currentUser.uid)
                                    .set(globalEntry, SetOptions.merge())
                            } catch (_: Exception) {}
                        }
                    }
            } else {
                // Immediate full wipe & refresh on account sign-out/switch per AGENTS.md Rule 3
                WidgetDataHelper.clearAndRefresh(this@KletaqApplication)
            }
        }
    }
}
