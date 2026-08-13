package com.kletaq.app.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kletaq.app.data.repository.AuthRepository
import com.kletaq.app.data.repository.SyllabusRepository
import com.kletaq.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StepUiState {
    object Idle : StepUiState()
    object Loading : StepUiState()
    object Success : StepUiState()
    data class Error(val message: String) : StepUiState()
}

enum class BacklogChoice {
    NONE,
    NO_BACKLOGS,
    HAS_BACKLOGS
}

data class BacklogSubjectItem(
    val id: String,
    val title: String,
    val yearName: String, // "FIRST YEAR", "SECOND YEAR", etc.
    val semesterNumber: Int,
    val isSelected: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val syllabusRepository: SyllabusRepository
) : ViewModel() {

    // --- University Step State ---
    private val _selectedUniversity = MutableStateFlow("VTU")
    val selectedUniversity: StateFlow<String> = _selectedUniversity.asStateFlow()

    private val _universityState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val universityState: StateFlow<StepUiState> = _universityState.asStateFlow()

    // --- Branch Step State ---
    private val _selectedBranch = MutableStateFlow("ISE")
    val selectedBranch: StateFlow<String> = _selectedBranch.asStateFlow()

    private val _branchState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val branchState: StateFlow<StepUiState> = _branchState.asStateFlow()

    // --- Scheme Step State ---
    private val _selectedScheme = MutableStateFlow("2025 Scheme")
    val selectedScheme: StateFlow<String> = _selectedScheme.asStateFlow()

    private val _schemeState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val schemeState: StateFlow<StepUiState> = _schemeState.asStateFlow()

    // --- Semester Step State ---
    private val _selectedSemester = MutableStateFlow(2)
    val selectedSemester: StateFlow<Int> = _selectedSemester.asStateFlow()
    private var _semesterExplicitlySet = false

    private val _semesterState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val semesterState: StateFlow<StepUiState> = _semesterState.asStateFlow()

    // --- Cycle Step State (Sem 2 + VTU + Scheme check) ---
    private val _selectedCycle = MutableStateFlow("") // empty by default until selected or loaded
    val selectedCycle: StateFlow<String> = _selectedCycle.asStateFlow()

    init {
        val user = authRepository.currentUser
        if (user != null) {
            viewModelScope.launch {
                val profile = userRepository.getUserProfile(user.uid).getOrNull()
                if (profile != null && profile.firstYearCycle.isNotBlank()) {
                    _selectedCycle.value = profile.firstYearCycle
                    android.util.Log.d("OnboardingCycleCheck", "Loaded existing firstYearCycle: '${profile.firstYearCycle}'")
                }
            }
        }
    }

    private val _cycleState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val cycleState: StateFlow<StepUiState> = _cycleState.asStateFlow()

    // --- Backlogs Step State ---
    private val _backlogChoice = MutableStateFlow(BacklogChoice.NONE)
    val backlogChoice: StateFlow<BacklogChoice> = _backlogChoice.asStateFlow()

    private val _backlogSubjects = MutableStateFlow<List<BacklogSubjectItem>>(emptyList())
    val backlogSubjects: StateFlow<List<BacklogSubjectItem>> = _backlogSubjects.asStateFlow()

    private val _backlogSearchQuery = MutableStateFlow("")
    val backlogSearchQuery: StateFlow<String> = _backlogSearchQuery.asStateFlow()

    private val _backlogState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val backlogState: StateFlow<StepUiState> = _backlogState.asStateFlow()

    fun selectBacklogChoice(choice: BacklogChoice) {
        _backlogChoice.value = choice
        if (choice == BacklogChoice.NO_BACKLOGS) {
            clearAllBacklogs()
        }
    }

    // --- Username Step State ---
    private val _usernameInput = MutableStateFlow("")
    val usernameInput: StateFlow<String> = _usernameInput.asStateFlow()

    private val _usernameValidationError = MutableStateFlow<String?>(null)
    val usernameValidationError: StateFlow<String?> = _usernameValidationError.asStateFlow()

    private val _isUsernameAvailable = MutableStateFlow<Boolean?>(null)
    val isUsernameAvailable: StateFlow<Boolean?> = _isUsernameAvailable.asStateFlow()

    private val _usernameState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val usernameState: StateFlow<StepUiState> = _usernameState.asStateFlow()

    private var usernameDebounceJob: Job? = null

    // --- Import Syllabus Step State ---
    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _importState = MutableStateFlow<StepUiState>(StepUiState.Idle)
    val importState: StateFlow<StepUiState> = _importState.asStateFlow()

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{3,20}$")

    fun calculateTotalSteps(): Int {
        val uni = _selectedUniversity.value
        val scheme = _selectedScheme.value
        val sem = _selectedSemester.value
        val showCycle = (uni.contains("VTU") || uni == "VTU") &&
                (scheme.contains("2022") || scheme.contains("2025") || scheme.contains("2021")) &&
                sem == 2

        return when {
            sem == 1 -> 8
            sem == 2 && showCycle -> 10
            sem == 2 && !showCycle -> 9
            else -> 9
        }
    }

    fun shouldShowCycleStep(): Boolean {
        val sem = _selectedSemester.value
        val cycle = _selectedCycle.value
        val shouldShow = sem >= 2 && cycle.isBlank()
        android.util.Log.d("OnboardingCycleCheck", "selectedSemester = $sem, firstYearCycle = '$cycle', shouldShowCycleStep = $shouldShow")
        return shouldShow
    }

    fun selectUniversity(uni: String) { _selectedUniversity.value = uni }
    fun submitUniversity() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _universityState.value = StepUiState.Loading
            userRepository.setUniversity(user.uid, _selectedUniversity.value)
            _universityState.value = StepUiState.Success
        }
    }

    fun selectBranch(branch: String) { _selectedBranch.value = branch }
    fun submitBranch() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _branchState.value = StepUiState.Loading
            android.util.Log.d("BacklogDebug", "Saved branch: ${_selectedBranch.value}")
            userRepository.setBranch(user.uid, _selectedBranch.value)
            _branchState.value = StepUiState.Success
        }
    }

    fun selectScheme(scheme: String) { _selectedScheme.value = scheme }
    fun submitScheme() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _schemeState.value = StepUiState.Loading
            userRepository.setScheme(user.uid, _selectedScheme.value)
            _schemeState.value = StepUiState.Success
        }
    }

    fun selectSemester(sem: Int) {
        _selectedSemester.value = sem
        _semesterExplicitlySet = true
    }
    fun submitSemester() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _semesterState.value = StepUiState.Loading
            userRepository.setSemester(user.uid, _selectedSemester.value)
            loadBacklogSubjectsForSemester(_selectedSemester.value)
            _semesterState.value = StepUiState.Success
        }
    }

    fun selectCycle(cycle: String) { _selectedCycle.value = cycle }
    fun submitCycle() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _cycleState.value = StepUiState.Loading
            val formattedCycle = _selectedCycle.value.lowercase().trim()
            userRepository.setFirstYearCycle(user.uid, formattedCycle)
            loadBacklogSubjectsForSemester(_selectedSemester.value)
            _cycleState.value = StepUiState.Success
        }
    }

    fun ensureBacklogSubjectsLoaded() {
        val user = authRepository.currentUser
        viewModelScope.launch {
            if (user != null) {
                val profile = userRepository.getUserProfile(user.uid).getOrNull()
                if (profile != null) {
                    // Only load from Firestore if user hasn't explicitly selected a semester during this session
                    if (!_semesterExplicitlySet && profile.semester > 0) {
                        _selectedSemester.value = profile.semester
                        android.util.Log.d("BacklogDebug", "Restored semester from Firestore: ${profile.semester}")
                    }
                    if (_selectedBranch.value.isBlank() && profile.branch.isNotBlank()) {
                        _selectedBranch.value = profile.branch
                    }
                    if (_selectedUniversity.value.isBlank() && profile.university.isNotBlank()) {
                        _selectedUniversity.value = profile.university
                    }
                    if (_selectedScheme.value.isBlank() && profile.scheme.isNotBlank()) {
                        _selectedScheme.value = profile.scheme
                    }
                    if (_selectedCycle.value.isBlank() && profile.firstYearCycle.isNotBlank()) {
                        _selectedCycle.value = profile.firstYearCycle
                    }
                }
            }
            android.util.Log.d("BacklogDebug", "Loaded branch: ${_selectedBranch.value}")
            android.util.Log.d("BacklogDebug", "Loaded semester: ${_selectedSemester.value}")
            loadBacklogSubjectsForSemester(_selectedSemester.value)
            android.util.Log.d("BacklogDebug", "Loaded ${backlogSubjects.value.size} subjects for sem ${_selectedSemester.value}")
        }
    }

    private fun loadBacklogSubjectsForSemester(semester: Int) {
        if (semester <= 1) {
            _backlogSubjects.value = emptyList()
            return
        }

        val uni = _selectedUniversity.value
        val scheme = _selectedScheme.value
        val branch = _selectedBranch.value
        val cycle = _selectedCycle.value

        val backlogResult = syllabusRepository.getBacklogPriorSemesters(
            university = uni,
            scheme = scheme,
            branch = branch,
            semester = semester,
            cycle = cycle
        )

        backlogResult.onSuccess { groups ->
            val items = mutableListOf<BacklogSubjectItem>()
            groups.forEach { group ->
                val yearName = when (group.semester) {
                    1, 2 -> "FIRST YEAR"
                    3, 4 -> "SECOND YEAR"
                    5, 6 -> "THIRD YEAR"
                    else -> "FOURTH YEAR"
                }
                group.subjects.forEach { sub ->
                    items.add(
                        BacklogSubjectItem(
                            id = sub.id,
                            title = sub.title,
                            yearName = yearName,
                            semesterNumber = group.semester
                        )
                    )
                }
            }
            _backlogSubjects.value = items
        }.onFailure {
            _backlogSubjects.value = emptyList()
        }
    }

    fun toggleBacklogSubject(id: String) {
        _backlogSubjects.value = _backlogSubjects.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun clearAllBacklogs() {
        _backlogSubjects.value = _backlogSubjects.value.map { it.copy(isSelected = false) }
    }

    fun updateBacklogSearch(query: String) {
        _backlogSearchQuery.value = query
    }

    fun submitBacklogs() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _backlogState.value = StepUiState.Loading
            val selectedIds = if (_backlogChoice.value == BacklogChoice.NO_BACKLOGS) {
                emptyList()
            } else {
                _backlogSubjects.value.filter { it.isSelected }.map { it.title }
            }
            userRepository.setBacklogSubjects(user.uid, selectedIds)
            _backlogState.value = StepUiState.Success
        }
    }

    fun onUsernameChanged(input: String) {
        _usernameInput.value = input
        _isUsernameAvailable.value = null
        usernameDebounceJob?.cancel()

        if (input.isBlank()) {
            _usernameValidationError.value = null
            return
        }

        val error = com.kletaq.app.core.util.UsernameValidator.getValidationError(input)
        if (error != null) {
            _usernameValidationError.value = error
            return
        }

        _usernameValidationError.value = null

        usernameDebounceJob = viewModelScope.launch {
            delay(400L)
            val result = userRepository.isUsernameAvailable(input)
            result.onSuccess { available ->
                _isUsernameAvailable.value = available
                if (!available) {
                    _usernameValidationError.value = "Username is already taken"
                }
            }.onFailure {
                _usernameValidationError.value = "Could not verify username availability"
            }
        }
    }

    fun submitUsername() {
        val user = authRepository.currentUser ?: return
        val username = _usernameInput.value.trim()

        if (_usernameValidationError.value != null || username.isBlank()) return

        viewModelScope.launch {
            _usernameState.value = StepUiState.Loading
            val result = userRepository.setUsername(user.uid, username)
            result.onSuccess {
                _usernameState.value = StepUiState.Success
            }.onFailure { ex ->
                _usernameState.value = StepUiState.Error(ex.localizedMessage ?: "Failed to save username")
            }
        }
    }

    fun startSyllabusImport() {
        viewModelScope.launch {
            _importState.value = StepUiState.Loading
            _importProgress.value = 0.1f

            val branch = _selectedBranch.value
            val semester = _selectedSemester.value

            delay(300L)
            _importProgress.value = 0.4f
            val result = syllabusRepository.importSyllabus(branch, semester)
            delay(400L)
            _importProgress.value = 0.8f
            delay(300L)
            _importProgress.value = 1.0f

            result.onSuccess {
                val user = authRepository.currentUser
                if (user != null) {
                    userRepository.setOnboardingCompleted(user.uid, true)
                }
                _importState.value = StepUiState.Success
            }.onFailure { ex ->
                _importState.value = StepUiState.Error(ex.localizedMessage ?: "Syllabus import failed")
            }
        }
    }

    // --- Calendar Preferences State ---
    private val _studyDaysPerWeek = MutableStateFlow(5)
    val studyDaysPerWeek: StateFlow<Int> = _studyDaysPerWeek.asStateFlow()

    private val _preferredReminderTime = MutableStateFlow("")
    val preferredReminderTime: StateFlow<String> = _preferredReminderTime.asStateFlow()

    fun selectStudyDaysPerWeek(days: Int) {
        _studyDaysPerWeek.value = days
    }

    fun selectPreferredReminderTime(time: String) {
        _preferredReminderTime.value = time
    }

    fun submitCalendarPreferences() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            val days = _studyDaysPerWeek.value
            val reminder = _preferredReminderTime.value
            userRepository.saveCalendarPreferences(user.uid, days, reminder, configured = true)
            if (reminder.isNotBlank()) {
                com.kletaq.app.data.repository.UserSettingsRepository.updateMorningReminderTime(reminder)
                com.kletaq.app.data.repository.UserSettingsRepository.updateMorningReminderEnabled(true)
            }
        }
    }

    fun skipCalendarPreferences() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _preferredReminderTime.value = ""
            userRepository.saveCalendarPreferences(user.uid, 5, "", configured = false)
        }
    }

    fun setCalendarConfigured(configured: Boolean) {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            userRepository.setCalendarConfigured(user.uid, configured)
        }
    }

    fun resetStepStates() {
        _universityState.value = StepUiState.Idle
        _branchState.value = StepUiState.Idle
        _schemeState.value = StepUiState.Idle
        _semesterState.value = StepUiState.Idle
        _cycleState.value = StepUiState.Idle
        _backlogState.value = StepUiState.Idle
        _usernameState.value = StepUiState.Idle
        _importState.value = StepUiState.Idle
    }
}
