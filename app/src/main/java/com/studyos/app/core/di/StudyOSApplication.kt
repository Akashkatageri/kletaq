package com.studyos.app.core.di

import android.app.Application
import com.studyos.app.core.util.LocalNotificationHelper
import com.studyos.app.data.repository.NotificationCoordinator
import com.studyos.app.data.repository.ProgressRepository
import com.studyos.app.data.repository.UserSettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import com.google.firebase.auth.FirebaseAuth
import com.studyos.app.data.repository.UserRepository
import com.studyos.app.widgets.data.WidgetDataHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class StudyOSApplication : Application() {

    @Inject
    lateinit var notificationCoordinator: NotificationCoordinator

    @Inject
    lateinit var progressRepository: ProgressRepository

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate() {
        super.onCreate()
        LocalNotificationHelper.createNotificationChannels(this)
        UserSettingsRepository.initialize(this)

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            if (currentUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val statsResult = userRepository.getUserStats(currentUser.uid)
                    val stats = statsResult.getOrNull()
                    if (stats != null) {
                        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val todayXp = stats.dailyXp[todayKey] ?: 0L
                        WidgetDataHelper.saveStats(
                            ctx = this@StudyOSApplication,
                            streak = stats.studyStreak,
                            todayXp = todayXp,
                            totalXp = stats.totalXp,
                            level = stats.currentLevel,
                            completedTasks = stats.completedTasksCount,
                            currentSubject = "Study"
                        )
                        WidgetDataHelper.refreshWidgets(this@StudyOSApplication)
                    }
                }
            } else {
                WidgetDataHelper.clearAndRefresh(this@StudyOSApplication)
            }
        }
    }
}
