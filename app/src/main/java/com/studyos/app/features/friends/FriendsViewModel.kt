package com.studyos.app.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyos.app.data.model.LeaderboardEntry
import com.studyos.app.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {

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

    private val _sentRequests = MutableStateFlow<Set<String>>(emptySet())
    val sentRequests: StateFlow<Set<String>> = _sentRequests.asStateFlow()

    init {
        loadGlobalLeaderboard(category = "xp")
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
        viewModelScope.launch {
            friendRepository.sendFriendRequest(currentUserId, targetUserId)
            _sentRequests.value = _sentRequests.value + targetUserId
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

    fun loadGlobalLeaderboard(category: String = "xp") {
        viewModelScope.launch {
            _isLeaderboardLoading.value = true
            val result = friendRepository.getGlobalLeaderboard(category)
            _globalLeaderboard.value = result.getOrDefault(emptyList())
            _isLeaderboardLoading.value = false
        }
    }
}
