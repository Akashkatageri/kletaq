package com.studyos.app.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.data.model.NotificationModel
import com.studyos.app.data.model.NotificationSettings
import com.studyos.app.data.repository.AuthRepository
import com.studyos.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val currentUserId: String
        get() = authRepository.currentUser?.uid ?: ""

    val notifications: StateFlow<List<NotificationModel>> =
        notificationRepository.getNotificationsFlow(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> =
        notificationRepository.getUnreadCountFlow(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val settings: StateFlow<NotificationSettings> =
        notificationRepository.getNotificationSettingsFlow(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationSettings())

    fun markAsRead(id: String) {
        val uid = currentUserId
        if (uid.isNotBlank() && id.isNotBlank()) {
            viewModelScope.launch {
                notificationRepository.markAsRead(uid, id)
            }
        }
    }

    fun markAllAsRead() {
        val uid = currentUserId
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                notificationRepository.markAllAsRead(uid)
            }
        }
    }

    fun deleteNotification(id: String) {
        val uid = currentUserId
        if (uid.isNotBlank() && id.isNotBlank()) {
            viewModelScope.launch {
                notificationRepository.deleteNotification(uid, id)
            }
        }
    }

    fun deleteAllNotifications() {
        val uid = currentUserId
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                notificationRepository.deleteAllNotifications(uid)
            }
        }
    }

    fun updateSettings(newSettings: NotificationSettings) {
        val uid = currentUserId
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                notificationRepository.updateNotificationSettings(uid, newSettings)
            }
        }
    }
}
