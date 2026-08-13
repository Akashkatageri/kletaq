package com.kletaq.app.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kletaq.app.data.model.LeaderboardEntry
import com.kletaq.app.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RequestState {
    object Idle : RequestState()
    object Sending : RequestState()
    object Sent : RequestState()
    data class Error(val message: String) : RequestState()
}

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    notificationCoordinator: com.kletaq.app.data.repository.NotificationCoordinator
) : ViewModel() {

    val unreadNotificationCount: StateFlow<Int> = notificationCoordinator.unreadCount

    val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val friends: StateFlow<List<LeaderboardEntry>> =
        friendRepository.getFriendsFlow(currentUserId)
            .let { flow ->
                val state = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
                viewModelScope.launch {
                    flow.collect { state.value = it }
                }
                state.asStateFlow()
            }

    val friendRequests: StateFlow<List<LeaderboardEntry>> =
        friendRepository.getFriendRequestsFlow(currentUserId)
            .let { flow ->
                val state = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
                viewModelScope.launch {
                    flow.collect { state.value = it }
                }
                state.asStateFlow()
            }

    private val _searchResults = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val searchResults: StateFlow<List<LeaderboardEntry>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _globalLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val globalLeaderboard: StateFlow<List<LeaderboardEntry>> = _globalLeaderboard.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    private val _requestStates = MutableStateFlow<Map<String, RequestState>>(emptyMap())
    val requestStates: StateFlow<Map<String, RequestState>> = _requestStates.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    init {
        loadGlobalLeaderboard(category = "streak")
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val result = friendRepository.searchUsers(query, currentUserId)
            _searchResults.value = result.getOrDefault(emptyList())
            _isSearching.value = false
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        if (targetUserId.isBlank()) return

        if (targetUserId == currentUserId) {
            val msg = "Cannot add yourself as a friend"
            _requestStates.value = _requestStates.value + (targetUserId to RequestState.Error(msg))
            _snackbarMessage.value = msg
            return
        }

        if (friends.value.any { it.uid == targetUserId }) {
            val msg = "Already friends with this student"
            _requestStates.value = _requestStates.value + (targetUserId to RequestState.Error(msg))
            _snackbarMessage.value = msg
            return
        }

        val currentState = _requestStates.value[targetUserId]
        if (currentState is RequestState.Sending || currentState is RequestState.Sent) {
            return // Prevent duplicate requests
        }

        viewModelScope.launch {
            _requestStates.value = _requestStates.value + (targetUserId to RequestState.Sending)
            val result = friendRepository.sendFriendRequest(currentUserId, targetUserId)
            result.onSuccess {
                _requestStates.value = _requestStates.value + (targetUserId to RequestState.Sent)
            }.onFailure { e ->
                android.util.Log.e("FriendsViewModel", "sendFriendRequest failed for target $targetUserId", e)
                val errorMessage = e.localizedMessage ?: "Failed to send friend request"
                _requestStates.value = _requestStates.value + (targetUserId to RequestState.Error(errorMessage))
                _snackbarMessage.value = errorMessage
            }
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        viewModelScope.launch {
            friendRepository.acceptFriendRequest(currentUserId, fromUserId)
        }
    }

    fun rejectFriendRequest(fromUserId: String) {
        viewModelScope.launch {
            friendRepository.rejectFriendRequest(currentUserId, fromUserId)
        }
    }

    fun loadGlobalLeaderboard(category: String = "streak") {
        viewModelScope.launch {
            _isLeaderboardLoading.value = true
            val result = friendRepository.getGlobalLeaderboard(category)
            _globalLeaderboard.value = result.getOrDefault(emptyList())
            _isLeaderboardLoading.value = false
        }
    }
}
