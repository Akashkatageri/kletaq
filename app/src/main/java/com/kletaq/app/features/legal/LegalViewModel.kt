package com.kletaq.app.features.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kletaq.app.data.repository.AuthRepository
import com.kletaq.app.data.repository.LegalRepository
import com.kletaq.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LegalUiState {
    object Idle : LegalUiState()
    object Submitting : LegalUiState()
    object Success : LegalUiState()
    data class Error(val message: String) : LegalUiState()
}

@HiltViewModel
class LegalViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val legalRepository: LegalRepository
) : ViewModel() {

    private val _termsChecked = MutableStateFlow(false)
    val termsChecked: StateFlow<Boolean> = _termsChecked.asStateFlow()

    private val _privacyChecked = MutableStateFlow(false)
    val privacyChecked: StateFlow<Boolean> = _privacyChecked.asStateFlow()

    private val _uiState = MutableStateFlow<LegalUiState>(LegalUiState.Idle)
    val uiState: StateFlow<LegalUiState> = _uiState.asStateFlow()

    val isContinueEnabled: Boolean
        get() = _termsChecked.value && _privacyChecked.value

    fun toggleTermsAccepted(accepted: Boolean) {
        _termsChecked.value = accepted
    }

    fun togglePrivacyAccepted(accepted: Boolean) {
        _privacyChecked.value = accepted
    }

    fun submitLegalAcceptance() {
        val user = authRepository.currentUser
        if (user == null) {
            _uiState.value = LegalUiState.Error("User not authenticated")
            return
        }

        viewModelScope.launch {
            _uiState.value = LegalUiState.Submitting
            val result = userRepository.updateLegalAcceptance(
                uid = user.uid,
                termsAccepted = _termsChecked.value,
                privacyAccepted = _privacyChecked.value
            )
            result.onSuccess {
                _uiState.value = LegalUiState.Success
            }.onFailure { ex ->
                _uiState.value = LegalUiState.Error(ex.localizedMessage ?: "Failed to save legal acceptance")
            }
        }
    }

    fun getTermsText(): String = legalRepository.getTermsDocument()
    fun getPrivacyText(): String = legalRepository.getPrivacyDocument()
}
