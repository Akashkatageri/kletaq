package com.kletaq.app.features.focus

import com.kletaq.app.features.focus.components.SessionMode
import com.kletaq.app.features.journey.components.ExamPrep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Global Focus Timer Manager.
 * Retains timer state across navigation tabs (Home, Journey, Friends, Profile).
 * Uses wall-clock timestamp synchronization to remain accurate even when screens sleep or re-compose.
 */
object FocusTimerManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null
    private var targetEndTimeMs: Long = 0L
    private var appContext: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private val _selectedMode = MutableStateFlow(SessionMode.POMODORO)
    val selectedMode: StateFlow<SessionMode> = _selectedMode.asStateFlow()

    private val _customMinutes = MutableStateFlow(30)
    val customMinutes: StateFlow<Int> = _customMinutes.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(SessionMode.POMODORO.defaultMinutes * 60)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(SessionMode.POMODORO.defaultMinutes * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentTopicName = MutableStateFlow<String?>(null)
    val currentTopicName: StateFlow<String?> = _currentTopicName.asStateFlow()

    private val _currentSubjectName = MutableStateFlow<String?>(null)
    val currentSubjectName: StateFlow<String?> = _currentSubjectName.asStateFlow()

    private val _currentSemesterName = MutableStateFlow<String?>(null)
    val currentSemesterName: StateFlow<String?> = _currentSemesterName.asStateFlow()

    private val _currentExamPrep = MutableStateFlow<ExamPrep?>(null)
    val currentExamPrep: StateFlow<ExamPrep?> = _currentExamPrep.asStateFlow()

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

    private val _completedStudiedMinutes = MutableStateFlow(0)
    val completedStudiedMinutes: StateFlow<Int> = _completedStudiedMinutes.asStateFlow()

    fun setTopic(topic: String?, subject: String?, semester: String?, examPrep: ExamPrep?) {
        if (!topic.isNullOrBlank()) _currentTopicName.value = topic
        if (!subject.isNullOrBlank()) _currentSubjectName.value = subject
        if (!semester.isNullOrBlank()) _currentSemesterName.value = semester
        if (examPrep != null) _currentExamPrep.value = examPrep
    }

    fun setMode(mode: SessionMode) {
        if (_isRunning.value) pause()
        _selectedMode.value = mode
        val mins = if (mode == SessionMode.CUSTOM) _customMinutes.value else mode.defaultMinutes
        _totalDurationSeconds.value = mins * 60
        _remainingSeconds.value = mins * 60
    }

    fun setCustomDuration(minutes: Int) {
        if (_isRunning.value) pause()
        _customMinutes.value = minutes
        _selectedMode.value = SessionMode.CUSTOM
        _totalDurationSeconds.value = minutes * 60
        _remainingSeconds.value = minutes * 60
    }

    fun start() {
        if (_isRunning.value) return
        if (_remainingSeconds.value <= 0) {
            reset()
        }

        _isRunning.value = true
        targetEndTimeMs = System.currentTimeMillis() + (_remainingSeconds.value * 1000L)

        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isRunning.value) {
                delay(500L)
                val diffMs = targetEndTimeMs - System.currentTimeMillis()
                val leftSecs = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                _remainingSeconds.value = leftSecs

                if (leftSecs <= 0) {
                    _isRunning.value = false
                    val studiedMins = (_totalDurationSeconds.value / 60).coerceAtLeast(1)
                    _completedStudiedMinutes.value = studiedMins
                    _showCompletionDialog.value = true
                    appContext?.let { ctx ->
                        com.kletaq.app.core.util.LocalNotificationHelper.showNotification(
                            context = ctx,
                            title = "⏰ Focus Session Complete!",
                            message = "Awesome effort! You completed $studiedMins minutes of study.",
                            type = "xp"
                        )
                    }
                    break
                }
            }
        }
    }

    fun pause() {
        tickerJob?.cancel()
        tickerJob = null
        if (_isRunning.value) {
            val diffMs = targetEndTimeMs - System.currentTimeMillis()
            _remainingSeconds.value = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
            _isRunning.value = false
        }
    }

    fun reset() {
        pause()
        val mins = if (_selectedMode.value == SessionMode.CUSTOM) _customMinutes.value else _selectedMode.value.defaultMinutes
        _totalDurationSeconds.value = mins * 60
        _remainingSeconds.value = mins * 60
    }

    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
        reset()
    }
}
