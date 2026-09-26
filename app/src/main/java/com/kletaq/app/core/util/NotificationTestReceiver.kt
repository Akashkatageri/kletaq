package com.kletaq.app.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver to test local device notifications via adb or internal broadcasts.
 * Example trigger:
 * adb shell am broadcast -a com.kletaq.app.TEST_NOTIFICATION
 */
class NotificationTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i("NotificationTestReceiver", "Received test notification broadcast")
        val title = intent?.getStringExtra("title") ?: "🔥 Kletaq Streak Alert"
        val message = intent?.getStringExtra("message") ?: "Keep your study momentum alive! Complete today's quest."
        val type = intent?.getStringExtra("type") ?: "streak"

        LocalNotificationHelper.showNotification(
            context = context,
            title = title,
            message = message,
            type = type
        )
    }
}
