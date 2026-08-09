package com.studyos.app.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.data.model.NotificationModel
import com.studyos.app.data.model.NotificationSettings
import com.studyos.app.data.repository.AuthRepository
import com.studyos.app.data.repository.NotificationCoordinator
import com.studyos.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    val notificationCoordinator: NotificationCoordinator
) : ViewModel() {

    val currentUserId: String
        get() = authRepository.currentUser?.uid ?: ""

    val notifications: StateFlow<List<NotificationModel>> = notificationCoordinator.notifications
    val unreadCount: StateFlow<Int> = notificationCoordinator.unreadCount

    val settings: StateFlow<NotificationSettings> =
        notificationRepository.getNotificationSettingsFlow(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationSettings())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

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
                val result = notificationRepository.deleteAllNotifications(uid)
                result.onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to clear notifications."
                }
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
