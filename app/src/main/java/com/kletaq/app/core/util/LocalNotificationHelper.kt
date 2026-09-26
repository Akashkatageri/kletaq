package com.kletaq.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kletaq.app.MainActivity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalNotificationHelper {

    private const val CHANNEL_STUDY = "study_reminders_channel"
    private const val CHANNEL_STREAK = "streak_warnings_channel"
    private const val CHANNEL_MILESTONE = "milestone_alerts_channel"
    private const val CHANNEL_SOCIAL = "social_alerts_channel"
    private const val PREFS_TRACKER = "klytaq_notification_tracker"

    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val studyChannel = NotificationChannel(
                CHANNEL_STUDY,
                "Study & Exam Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily study reminders, upcoming exams, and assignment alerts"
                enableVibration(true)
                enableLights(true)
            }

            val streakChannel = NotificationChannel(
                CHANNEL_STREAK,
                "Streak Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your study streak is about to expire"
                enableVibration(true)
                enableLights(true)
            }

            val milestoneChannel = NotificationChannel(
                CHANNEL_MILESTONE,
                "Milestones & XP",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Quest completions, level ups, and achievement unlocks"
                enableVibration(true)
                enableLights(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL,
                "Social & Friend Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Friend requests, study buddy invites, and social leaderboard updates"
                enableVibration(true)
                enableLights(true)
            }

            manager.createNotificationChannel(studyChannel)
            manager.createNotificationChannel(streakChannel)
            manager.createNotificationChannel(milestoneChannel)
            manager.createNotificationChannel(socialChannel)
        }
    }

    fun triggerTestNotification(context: Context): Result<Unit> {
        createNotificationChannels(context)

        if (!isPermissionGranted(context)) {
            return Result.failure(
                SecurityException("Notification permission denied. Please enable notifications in Android Settings.")
            )
        }

        return try {
            showNotification(
                context = context,
                title = "🧪 Test Notification",
                message = "Local device notifications are working properly on Kletaq!",
                type = "study"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shouldShowDailyReminder(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastDate = prefs.getString("last_date_$type", "")
        return lastDate != today
    }

    fun recordReminderShown(context: Context, type: String) {
        val prefs = context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        prefs.edit().putString("last_date_$type", today).apply()
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "study",
        entityId: String? = null,
        entityType: String? = null
    ) {
        createNotificationChannels(context)

        if (!isPermissionGranted(context)) return

        val channelId = when (type.lowercase()) {
            "streak" -> CHANNEL_STREAK
            "xp", "quest" -> CHANNEL_MILESTONE
            "friend_request", "social", "friend" -> CHANNEL_SOCIAL
            else -> CHANNEL_STUDY
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!entityId.isNullOrBlank()) putExtra("entityId", entityId)
            if (!entityType.isNullOrBlank()) putExtra("entityType", entityType)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.kletaq.app.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("LocalNotificationHelper", "SecurityException posting notification", e)
        }
    }
}
