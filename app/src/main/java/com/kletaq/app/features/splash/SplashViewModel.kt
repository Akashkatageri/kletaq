package com.kletaq.app.features.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kletaq.app.data.repository.AuthRepository
import com.kletaq.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashDestination {
    LOADING,
    GOOGLE_SIGN_IN,
    LEGAL,
    UNIVERSITY,
    SCHOOL,
    BRANCH,
    SCHEME,
    SEMESTER,
    CYCLE,
    BACKLOG,
    USERNAME,
    HOME
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.LOADING)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkAuthStateAndProfile()
    }

    fun checkAuthStateAndProfile() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            if (user == null) {
                Log.d("AuthFlow", "User signed out")
                _destination.value = SplashDestination.GOOGLE_SIGN_IN
                return@launch
            }

            val uid = user.uid
            Log.d("AuthFlow", "UID = $uid")

            val profileResult = userRepository.getUserProfile(uid)
            val profile = profileResult.getOrNull()

            val docExists = profile != null
            Log.d("AuthFlow", "Document exists = $docExists")
            Log.d("AuthFlow", "Profile = $profile")
            Log.d("AuthFlow", "hasCompletedOnboarding = ${profile?.hasCompletedOnboarding}")

            if (profile != null && profile.hasCompletedOnboarding) {
                Log.d("AuthFlow", "Destination = Home")
                _destination.value = SplashDestination.HOME
            } else {
                Log.d("AuthFlow", "Destination = Onboarding")
                if (profile == null) {
                    _destination.value = SplashDestination.LEGAL
                } else {
                    val isSchool = profile.educationLevel == "SCHOOL" || profile.schoolGrade.isNotBlank()
                    val isVtu = profile.university.equals("VTU", ignoreCase = true)
                    val showCycle = isVtu &&
                            (profile.scheme.contains("2022") || profile.scheme.contains("2025") || profile.scheme.contains("2021")) &&
                            profile.semester == 2

                    when {
                        !profile.termsAccepted || !profile.privacyAccepted -> _destination.value = SplashDestination.LEGAL
                        profile.university.isBlank() -> _destination.value = SplashDestination.UNIVERSITY
                        isSchool && profile.schoolGrade.isBlank() -> _destination.value = SplashDestination.SCHOOL
                        !isSchool && profile.branch.isBlank() -> _destination.value = SplashDestination.BRANCH
                        !isSchool && isVtu && profile.scheme.isBlank() -> _destination.value = SplashDestination.SCHEME
                        !isSchool && profile.semester <= 0 -> _destination.value = SplashDestination.SEMESTER
                        !isSchool && showCycle && profile.firstYearCycle.isBlank() -> _destination.value = SplashDestination.CYCLE
                        profile.username.isBlank() -> _destination.value = SplashDestination.USERNAME
                        else -> _destination.value = SplashDestination.LEGAL
                    }
                }
            }
        }
    }
}
