package com.kletaq.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.model.NotificationModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationCoordinator @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val _notifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notifications: StateFlow<List<NotificationModel>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var currentUserId: String? = null
    private var isInitialSnapshot = true

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                if (currentUserId != user.uid) {
                    startObserving(user.uid)
                }
            } else {
                stopObserving()
            }
        }

        // Also check current user upon construction
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            if (uid.isNotBlank()) {
                startObserving(uid)
            }
        }
    }

    var isFocusSessionActive: Boolean = false
    var isLessonActive: Boolean = false
    var isReviewActive: Boolean = false

    fun isUserStudying(): Boolean = isFocusSessionActive || isLessonActive || isReviewActive

    fun shouldSendNotification(
        type: String,
        config: com.kletaq.app.core.config.AdaptiveEngineConfig = com.kletaq.app.core.config.AdaptiveEngineConfig()
    ): Boolean {
        // Never interrupt an active Focus Session, active Lesson, or active Review Session
        if (config.suppressNotificationsWhileStudying && isUserStudying()) {
            return false
        }
        return LocalNotificationHelper.shouldShowDailyReminder(context, type)
    }

    @Synchronized
    fun startObserving(userId: String) {
        if (userId.isBlank()) return
        if (currentUserId == userId && listenerRegistration != null) return

        stopObserving()
        currentUserId = userId
        isInitialSnapshot = true

        val ref = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        listenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NotificationModel::class.java)?.copy(id = doc.id)
            }

            if (isInitialSnapshot) {
                // Initial snapshot populates state only; NO foreground local notifications triggered for existing records
                isInitialSnapshot = false
            } else {
                // Only documents added AFTER the initial snapshot trigger a foreground local notification banner
                snapshot.documentChanges.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val notif = change.document.toObject(NotificationModel::class.java)
                        if (!notif.read && notif.title.isNotBlank() && shouldSendNotification(notif.type)) {
                            LocalNotificationHelper.showNotification(
                                context = context,
                                title = notif.title,
                                message = notif.message,
                                type = notif.type,
                                entityId = notif.entityId,
                                entityType = notif.entityType
                            )
                        }
                    }
                }
            }

            _notifications.value = list
            _unreadCount.value = list.count { !it.read }
        }
    }

    @Synchronized
    fun stopObserving() {
        listenerRegistration?.remove()
        listenerRegistration = null
        currentUserId = null
        isInitialSnapshot = true
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }
}
