package com.studyos.app.features.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyos.app.data.model.BacklogPlan
import com.studyos.app.data.repository.BacklogPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BacklogPlanViewModel @Inject constructor(
    private val backlogPlanRepository: BacklogPlanRepository
) : ViewModel() {

    private val _activePlan = MutableStateFlow<BacklogPlan?>(null)
    val activePlan: StateFlow<BacklogPlan?> = _activePlan.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeActivePlan()
    }

    fun observeActivePlan() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _activePlan.value = null
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                backlogPlanRepository.observeActivePlan(currentUser.uid).collect { plan ->
                    _activePlan.value = plan
                    _isLoading.value = false
                }
            } catch (_: Exception) {
                _activePlan.value = null
                _isLoading.value = false
            }
        }
    }

    fun savePlan(
        subjectId: String,
        subjectName: String,
        semester: Int,
        studyDaysPerWeek: Int,
        sessionMinutes: Int,
        selectedUnitIds: List<String>
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            val newPlan = BacklogPlan(
                subjectId = subjectId,
                subjectName = subjectName,
                semester = semester,
                studyDaysPerWeek = studyDaysPerWeek,
                sessionMinutes = sessionMinutes,
                selectedUnitIds = selectedUnitIds,
                isActive = true
            )
            backlogPlanRepository.savePlan(currentUser.uid, newPlan)
        }
    }

    fun deactivatePlan(planId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            backlogPlanRepository.deactivatePlan(currentUser.uid, planId)
        }
    }

    fun markTopicCompletedInPlan(planId: String, topicId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            backlogPlanRepository.markTopicCompletedInPlan(currentUser.uid, planId, topicId)
        }
    }
}
