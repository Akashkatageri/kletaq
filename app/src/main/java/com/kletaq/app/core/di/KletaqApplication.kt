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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class KletaqApplication : Application() {

    @Inject
    lateinit var notificationCoordinator: NotificationCoordinator

    @Inject
    lateinit var progressRepository: ProgressRepository

    @Inject
    lateinit var userRepository: UserRepository

    private var statsListenerRegistration: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        LocalNotificationHelper.createNotificationChannels(this)
        UserSettingsRepository.initialize(this)
        com.kletaq.app.data.repository.TaskRepository.initialize(this)

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

                        WidgetDataHelper.saveStats(
                            ctx = this@KletaqApplication,
                            streak = stats.studyStreak,
                            todayXp = todayXp,
                            totalXp = stats.totalXp,
                            level = stats.currentLevel,
                            completedTasks = stats.completedTasksCount,
                            currentSubject = "Study"
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            WidgetDataHelper.refreshWidgets(this@KletaqApplication)
                        }
                    }
            } else {
                // Immediate full wipe & refresh on account sign-out/switch per AGENTS.md Rule 3
                WidgetDataHelper.clearAndRefresh(this@KletaqApplication)
            }
        }
    }
}
