package com.kletaq.app.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.model.NotificationModel
import com.kletaq.app.data.model.NotificationSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    fun getNotificationsFlow(userId: String): Flow<List<NotificationModel>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationModel::class.java)?.copy(id = doc.id)
                }
                trySend(list)
            }
        }

        awaitClose { listener.remove() }
    }

    fun getUnreadCountFlow(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereEqualTo("read", false)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(0)
                return@addSnapshotListener
            }
            trySend(snapshot?.size() ?: 0)
        }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllNotifications(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        actionUrl: String? = null,
        entityId: String? = null,
        entityType: String? = null
    ): Result<String> {
        return try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document()

            val notification = mapOf(
                "id" to docRef.id,
                "title" to title,
                "message" to message,
                "type" to type,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "actionUrl" to actionUrl,
                "entityId" to entityId,
                "entityType" to entityType
            )

            docRef.set(notification).await()

            // Also post Android System Tray Notification Banner
            LocalNotificationHelper.showNotification(
                context = context,
                title = title,
                message = message,
                type = type,
                entityId = entityId,
                entityType = entityType
            )

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getNotificationSettingsFlow(userId: String): Flow<NotificationSettings> = callbackFlow {
        if (userId.isBlank()) {
            trySend(NotificationSettings())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users")
            .document(userId)
            .collection("settings")
            .document("notifications")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(NotificationSettings())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val settings = snapshot.toObject(NotificationSettings::class.java) ?: NotificationSettings()
                trySend(settings)
            } else {
                trySend(NotificationSettings())
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun updateNotificationSettings(userId: String, settings: NotificationSettings): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("notifications")
                .set(settings, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
