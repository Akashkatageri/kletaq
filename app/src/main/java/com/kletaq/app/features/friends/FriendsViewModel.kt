package com.kletaq.app.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kletaq.app.data.model.FriendshipStatus
import com.kletaq.app.data.model.LeaderboardEntry
import com.kletaq.app.data.model.UserPublicProfile
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
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

    private val _leaderboardCategory = MutableStateFlow("streak") // "streak" or "xp"
    val leaderboardCategory: StateFlow<String> = _leaderboardCategory.asStateFlow()

    private val _requestStates = MutableStateFlow<Map<String, RequestState>>(emptyMap())
    val requestStates: StateFlow<Map<String, RequestState>> = _requestStates.asStateFlow()

    private val _selectedUserProfile = MutableStateFlow<UserPublicProfile?>(null)
    val selectedUserProfile: StateFlow<UserPublicProfile?> = _selectedUserProfile.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    init {
        loadGlobalLeaderboard(category = "streak")
    }

    fun setLeaderboardCategory(category: String) {
        _leaderboardCategory.value = category
        loadGlobalLeaderboard(category = category)
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

    fun openUserProfile(targetUid: String, fallbackEntry: LeaderboardEntry? = null) {
        if (targetUid.isBlank()) return

        // 1. Immediately populate from fallbackEntry for instantaneous responsiveness
        if (fallbackEntry != null) {
            val initialStatus = when {
                targetUid == currentUserId -> FriendshipStatus.SELF
                friends.value.any { it.uid == targetUid } -> FriendshipStatus.FRIENDS
                friendRequests.value.any { it.uid == targetUid } -> FriendshipStatus.REQUEST_RECEIVED
                requestStates.value[targetUid] is RequestState.Sent -> FriendshipStatus.REQUEST_SENT
                else -> FriendshipStatus.NOT_FRIENDS
            }

            _selectedUserProfile.value = UserPublicProfile(
                uid = targetUid,
                username = fallbackEntry.username,
                photoUrl = fallbackEntry.photoUrl,
                stats = UserStats(
                    totalXp = fallbackEntry.totalXp,
                    studyStreak = fallbackEntry.streak,
                    currentLevel = fallbackEntry.currentLevel,
                    weeklyXp = fallbackEntry.weeklyXp,
                    monthlyXp = fallbackEntry.monthlyXp
                ),
                rank = fallbackEntry.rank,
                friendshipStatus = initialStatus
            )
        }

        // 2. Fetch full verified profile & stats asynchronously
        viewModelScope.launch {
            _isLoadingProfile.value = true
            val result = friendRepository.getUserPublicProfile(
                currentUserId = currentUserId,
                targetUserId = targetUid,
                fallbackRank = fallbackEntry?.rank ?: 0
            )
            result.onSuccess { fullProfile ->
                _selectedUserProfile.value = fullProfile
            }
            _isLoadingProfile.value = false
        }
    }

    fun closeUserProfile() {
        _selectedUserProfile.value = null
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
                _snackbarMessage.value = "Friend request sent!"
                // Reactively update the modal state if open
                if (_selectedUserProfile.value?.uid == targetUserId) {
                    _selectedUserProfile.value = _selectedUserProfile.value?.copy(
                        friendshipStatus = FriendshipStatus.REQUEST_SENT
                    )
                }
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
            val result = friendRepository.acceptFriendRequest(currentUserId, fromUserId)
            result.onSuccess {
                _snackbarMessage.value = "Friend request accepted!"
                if (_selectedUserProfile.value?.uid == fromUserId) {
                    _selectedUserProfile.value = _selectedUserProfile.value?.copy(
                        friendshipStatus = FriendshipStatus.FRIENDS
                    )
                }
            }
        }
    }

    fun rejectFriendRequest(fromUserId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectFriendRequest(currentUserId, fromUserId)
            result.onSuccess {
                _snackbarMessage.value = "Friend request declined"
                if (_selectedUserProfile.value?.uid == fromUserId) {
                    _selectedUserProfile.value = _selectedUserProfile.value?.copy(
                        friendshipStatus = FriendshipStatus.NOT_FRIENDS
                    )
                }
            }
        }
    }

    private var leaderboardJob: Job? = null

    fun loadGlobalLeaderboard(category: String = _leaderboardCategory.value) {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            if (_globalLeaderboard.value.isEmpty()) {
                _isLeaderboardLoading.value = true
            }
            friendRepository.getGlobalLeaderboardFlow(category).collect { entries ->
                _globalLeaderboard.value = entries
                _isLeaderboardLoading.value = false
            }
        }
    }
}
