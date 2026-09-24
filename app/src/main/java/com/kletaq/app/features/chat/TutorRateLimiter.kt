package com.kletaq.app.features.chat

import android.content.Context
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class TutorRateLimitException(val seconds: Long) : Exception("Please wait $seconds seconds")

/** Local anti-spam guard, not a replacement for the Firebase AI Logic server quota. */
internal object TutorRateLimiter {
    private val lock = Any()
    const val REQUESTS_PER_MINUTE = 5

    suspend fun reserve(uid: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val prefs = FirebaseApp.getInstance().applicationContext.getSharedPreferences("topic_tutor_limits", Context.MODE_PRIVATE)
            val key = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }
            val now = System.currentTimeMillis()
            val recent = prefs.getString(key, "").orEmpty().split(',').mapNotNull(String::toLongOrNull)
                .filter { now - it < 60_000 }.sorted()
            if (recent.size >= REQUESTS_PER_MINUTE) {
                throw TutorRateLimitException(((recent.first() + 60_000 - now + 999) / 1000).coerceAtLeast(1))
            }
            check(prefs.edit().putString(key, (recent + now).joinToString(",")).commit()) { "Could not save request limit" }
        }
    }
}
