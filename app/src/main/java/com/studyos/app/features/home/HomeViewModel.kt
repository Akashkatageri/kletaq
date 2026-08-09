package com.studyos.app.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyos.app.data.model.Revision
import com.studyos.app.data.model.ReviewRating
import com.studyos.app.data.model.ReviewStats
import com.studyos.app.data.repository.SpacedRepetitionRepository
import com.studyos.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val spacedRepetitionRepository: SpacedRepetitionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _dailyQueue = MutableStateFlow<List<Revision>>(emptyList())
    val dailyQueue: StateFlow<List<Revision>> = _dailyQueue.asStateFlow()

    private val _reviewStats = MutableStateFlow(ReviewStats())
    val reviewStats: StateFlow<ReviewStats> = _reviewStats.asStateFlow()

    init {
        loadSpacedRepetitionData()
    }

    fun loadSpacedRepetitionData() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val queueResult = spacedRepetitionRepository.getDailyReviewQueue(uid)
            val statsResult = spacedRepetitionRepository.getReviewStats(uid)

            queueResult.getOrNull()?.let { queue ->
                _dailyQueue.value = queue
            }
            statsResult.getOrNull()?.let { stats ->
                _reviewStats.value = stats
            }
        }
    }

    fun recordReview(topicId: String, rating: ReviewRating) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            Log.d("HomeViewModel", "Recording review for topic $topicId with rating ${rating.value}")
            val recordResult = spacedRepetitionRepository.recordReview(uid, topicId, rating)
            if (recordResult.isSuccess) {
                // Award 15 XP for completing a revision
                userRepository.getUserStats(uid).getOrNull()?.let { currentStats ->
                    val newXp = currentStats.xp + 15
                    // Refresh local queue & stats
                    loadSpacedRepetitionData()
                }
            }
        }
    }
    fun updateStudyWhy(why: String, isPinned: Boolean) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            userRepository.updateStudyWhy(uid, why, isPinned)
        }
    }

    fun updateStudyWhyPinned(isPinned: Boolean) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            userRepository.updateStudyWhyPinned(uid, isPinned)
        }
    }

    fun deleteStudyWhy() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            userRepository.deleteStudyWhy(uid)
        }
    }
}
