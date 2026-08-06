package com.studyos.app.features.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.studyos.app.data.repository.AuthRepository
import com.studyos.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun handleGoogleAccountResult(
        email: String?,
        displayName: String?,
        photoUrl: String?,
        idToken: String?
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val projectId = try { FirebaseApp.getInstance().options.projectId } catch (_: Exception) { "unknown" }
            Log.d("FirebaseProject", "Active Firebase Project ID: $projectId")

            // 1. Try Google ID Token if present
            if (!idToken.isNullOrBlank()) {
                val googleResult = authRepository.signInWithGoogleToken(idToken)
                if (googleResult.isSuccess) {
                    val user = googleResult.getOrThrow()
                    saveUserAndProceed(user, email, displayName, photoUrl)
                    return@launch
                }
            }

            // 2. Try Email/Password Account Creation
            val fallbackResult = authRepository.signInWithFallbackAccount(email, displayName)
            fallbackResult.onSuccess { user ->
                saveUserAndProceed(user, email, displayName, photoUrl)
            }.onFailure { ex ->
                // 3. Try Anonymous Auth
                val anonResult = authRepository.signInAnonymously()
                anonResult.onSuccess { user ->
                    saveUserAndProceed(user, email, displayName, photoUrl)
                }.onFailure {
                    _uiState.value = AuthUiState.Error(ex.localizedMessage ?: "Sign-in failed")
                }
            }
        }
    }

    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithCredential(credential)
            result.onSuccess { user ->
                saveUserAndProceed(user, null, null, null)
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(exception.localizedMessage ?: "Authentication failed")
            }
        }
    }

    private suspend fun saveUserAndProceed(
        user: FirebaseUser,
        overrideEmail: String?,
        overrideName: String?,
        overridePhoto: String?
    ) {
        val projectId = try { FirebaseApp.getInstance().options.projectId } catch (_: Exception) { "unknown" }
        Log.d("FirebaseProject", "Connected Project ID: $projectId")
        Log.d("FirebaseUser", "User UID: ${user.uid}")

        val email = overrideEmail ?: user.email ?: ""
        val name = overrideName ?: user.displayName ?: "Student"
        val photo = overridePhoto ?: user.photoUrl?.toString() ?: ""

        val saveResult = userRepository.saveInitialUser(
            uid = user.uid,
            email = email,
            displayName = name,
            photoUrl = photo
        )
        if (saveResult.isSuccess) {
            Log.d("FirestoreDebug", "saveInitialUser succeeded for uid: ${user.uid}")
            _uiState.value = AuthUiState.Success(user)
        } else {
            val err = saveResult.exceptionOrNull()
            Log.e("FirestoreError", "saveInitialUser failed for uid: ${user.uid}", err)
            _uiState.value = AuthUiState.Error(
                err?.localizedMessage ?: "Failed to save user profile"
            )
        }
    }
}
