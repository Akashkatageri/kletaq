package com.kletaq.app.features.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kletaq.app.core.theme.InkPaperBorder
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.data.repository.ProgressionRepositoryImpl
import com.kletaq.app.data.repository.UserSettingsRepository
import com.kletaq.app.domain.progression.ProgressionCalculator

import com.kletaq.app.features.focus.components.AmbientTimerView
import com.kletaq.app.features.focus.components.CircularTimerHero
import com.kletaq.app.features.focus.components.CustomDurationSheet
import com.kletaq.app.features.focus.components.FocusStatsCard
import com.kletaq.app.features.focus.components.SessionCompleteDialog
import com.kletaq.app.features.focus.components.SessionMode
import com.kletaq.app.features.focus.components.SessionTypeSelector
import com.kletaq.app.features.focus.components.TimerControlsRow
import com.kletaq.app.features.focus.components.TopicSelectionSheet
import com.kletaq.app.features.journey.components.ExamPrep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(
    topicName: String? = null,
    subjectName: String? = null,
    semesterName: String? = null,
    examPrep: ExamPrep? = null
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userSettingsState by UserSettingsRepository.userSettingsState.collectAsState()
    var userStats by remember { mutableStateOf(UserStats()) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    // Live Snapshot Listener for UserStats & UserProfile
    DisposableEffect(currentUser) {
        var statsRegistration: ListenerRegistration? = null
        var profileRegistration: ListenerRegistration? = null
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            statsRegistration = db.collection("stats").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val stats = snapshot.toUserStatsSafe()
                        if (stats != null) {
                            userStats = stats
                        }
                    }
                }
            profileRegistration = db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        userProfile = snapshot.toObject(UserProfile::class.java)
                    }
                }
        }
        onDispose {
            statsRegistration?.remove()
            profileRegistration?.remove()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedMode by FocusTimerManager.selectedMode.collectAsState()
    val customMinutes by FocusTimerManager.customMinutes.collectAsState()
    val totalSeconds by FocusTimerManager.totalDurationSeconds.collectAsState()
    val remainingSeconds by FocusTimerManager.remainingSeconds.collectAsState()
    val isRunning by FocusTimerManager.isRunning.collectAsState()
    val currentTopicName by FocusTimerManager.currentTopicName.collectAsState()
    val currentSubjectName by FocusTimerManager.currentSubjectName.collectAsState()
    val currentSemesterName by FocusTimerManager.currentSemesterName.collectAsState()
    val currentExamPrep by FocusTimerManager.currentExamPrep.collectAsState()
    val showCompletionDialog by FocusTimerManager.showCompletionDialog.collectAsState()
    val completedStudiedMinutes by FocusTimerManager.completedStudiedMinutes.collectAsState()

    // Sync input parameters with global manager
    LaunchedEffect(topicName, subjectName, semesterName, examPrep) {
        if (!topicName.isNullOrBlank() || examPrep != null) {
            FocusTimerManager.setTopic(topicName, subjectName, semesterName, examPrep)
        }
    }

    // Keep screen awake while focus timer is actively running so device does not sleep or lock
    DisposableEffect(isRunning) {
        val activity = context as? android.app.Activity
        if (isRunning) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var showCustomDurationSheet by remember { mutableStateOf(false) }
    var showTopicSelectionSheet by remember { mutableStateOf(false) }
    var xpEarnedThisSession by remember { mutableStateOf(0L) }
    var isAmbientMode by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Exit ambient mode when timer finishes so user sees completion dialog
    LaunchedEffect(showCompletionDialog) {
        if (showCompletionDialog) {
            isAmbientMode = false
        }
    }

    // Auto-dim / sleep to pure black OLED display after 45 seconds of user inactivity while timer is running
    LaunchedEffect(isRunning, isAmbientMode) {
        while (isRunning && !isAmbientMode) {
            delay(5000L)
            if (System.currentTimeMillis() - lastInteractionTime > 45000L) {
                isAmbientMode = true
            }
        }
    }

    val activeDurationMinutes = if (selectedMode == SessionMode.CUSTOM) customMinutes else selectedMode.defaultMinutes
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val minutesLeft = remainingSeconds / 60
    val secondsLeft = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutesLeft, secondsLeft)
    val currentStudiedMinutes = (totalSeconds - remainingSeconds) / 60

    // Dynamic session XP calculation using ProgressionCalculator rules
    val currentSessionXp = if (currentStudiedMinutes >= 1) {
        ProgressionCalculator.xpForFocusSession(
            sessionMinutes = currentStudiedMinutes,
            dailyGoalMinutes = activeDurationMinutes,
            previousFocusMinutesToday = userStats.totalFocusMinutes
        )
    } else {
        ProgressionCalculator.xpForFocusSession(
            sessionMinutes = activeDurationMinutes,
            dailyGoalMinutes = activeDurationMinutes,
            previousFocusMinutesToday = userStats.totalFocusMinutes
        )
    }

    // Award real XP when session completes and update persisted stats instantly
    LaunchedEffect(showCompletionDialog) {
        if (showCompletionDialog && currentUser != null) {
            val studiedMinutes = completedStudiedMinutes.coerceAtLeast(1)
            val optimisticXp = com.kletaq.app.domain.progression.ProgressionCalculator.xpForFocusSession(
                sessionMinutes = studiedMinutes,
                dailyGoalMinutes = activeDurationMinutes,
                previousFocusMinutesToday = userStats.totalFocusMinutes
            )
            xpEarnedThisSession = optimisticXp
            val newTotalXp = userStats.totalXp + optimisticXp
            userStats = userStats.copy(
                totalXp = newTotalXp,
                currentLevel = com.kletaq.app.domain.progression.ProgressionCalculator.calculateLevel(newTotalXp),
                totalFocusMinutes = userStats.totalFocusMinutes + studiedMinutes,
                studySessions = userStats.studySessions + 1
            )

            val uid = currentUser.uid
            scope.launch(Dispatchers.IO) {
                try {
                    val repo = ProgressionRepositoryImpl(FirebaseFirestore.getInstance())
                    repo.finishFocusSession(
                        uid = uid,
                        focusMinutes = studiedMinutes,
                        dailyGoalMinutes = activeDurationMinutes
                    )
                } catch (_: Exception) {}
            }
        }
    }

    if (isAmbientMode) {
        AmbientTimerView(
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds,
            isRunning = isRunning,
            topicName = currentTopicName,
            sessionModeName = selectedMode.label,
            onTogglePlayPause = {
                if (isRunning) FocusTimerManager.pause() else FocusTimerManager.start()
            },
            onWake = {
                isAmbientMode = false
                lastInteractionTime = System.currentTimeMillis()
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Focus Session Header Card with soft neumorphism elevation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.HeavyShape,
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = InkPaperBorder.heavyBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FOCUS SESSION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Ambient OLED Sleep Mode button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                isAmbientMode = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "OLED Sleep",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "OLED Sleep",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(8.dp))

                val hasTopic = !currentTopicName.isNullOrBlank() && currentTopicName != "No topic selected"

                if (hasTopic) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTopicName ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            val subText = listOfNotNull(currentSemesterName, currentSubjectName)
                                .joinToString(" • ")
                            if (subText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { showTopicSelectionSheet = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change Topic",
                                tint = PurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No topic selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Choose a subject and topic to link this session",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showTopicSelectionSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleAccent.copy(alpha = 0.12f),
                                contentColor = PurpleAccent
                            ),
                            elevation = null,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Select Topic +",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 1. Session Mode Selector
        SessionTypeSelector(
            selectedMode = selectedMode,
            customMinutes = customMinutes,
            onModeSelected = { mode ->
                FocusTimerManager.setMode(mode)
            },
            onCustomClick = { showCustomDurationSheet = true }
        )

        if (currentExamPrep == null) {
            CircularTimerHero(
                timeFormatted = timeFormatted,
                progress = progress,
                xpEarned = if (xpEarnedThisSession > 0) xpEarnedThisSession.toInt() else currentSessionXp.toInt(),
                streakDays = userStats.effectiveStreak,
                isStreakActiveToday = userStats.isStreakActiveToday
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.HeavyShape,
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = InkPaperBorder.heavyBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    CircularTimerHero(
                        timeFormatted = timeFormatted,
                        progress = progress,
                        xpEarned = if (xpEarnedThisSession > 0) xpEarnedThisSession.toInt() else currentSessionXp.toInt(),
                        streakDays = userStats.effectiveStreak,
                        isStreakActiveToday = userStats.isStreakActiveToday,
                        compact = true,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "STUDY WHILE THE TIMER RUNS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurpleAccent,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    currentExamPrep?.let { content ->
                        FocusStudySection("LEARN", content.learnSummary)
                        FocusStudySection(
                            "QUESTIONS & ANSWERS",
                            buildString {
                                if (content.practiceQuestions.isNotEmpty()) {
                                    append("PRACTICE QUESTIONS:\n")
                                    content.practiceQuestions.forEachIndexed { index, question ->
                                        append("${index + 1}. $question\n")
                                    }
                                    append("\n")
                                }
                                if (content.fiveMarkAnswer.isNotBlank()) {
                                    append("MODEL ANSWER & KEY POINTS:\n")
                                    append(content.fiveMarkAnswer)
                                }
                            }.trim()
                        )
                        FocusStudySection("QUICK REVISION", content.recallPrompt)
                    }
                }
            }
        }

        // 3. Timer Control Row
        TimerControlsRow(
            isRunning = isRunning,
            onStartPauseClick = {
                if (isRunning) FocusTimerManager.pause() else FocusTimerManager.start()
            },
            onStopClick = {
                FocusTimerManager.reset()
            }
        )

        // 4. Focus Stats Card (Reads REAL userStats.totalFocusMinutes & userSettings target)
        val todayHoursText = String.format("%.1fh", userStats.totalFocusMinutes / 60.0)
        val targetHoursText = String.format("%.1fh", userSettingsState.dailyFocusGoalMinutes / 60.0)

        FocusStatsCard(
            todayHours = todayHoursText,
            completedSessions = userStats.studySessions,
            targetHours = targetHoursText
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
    }



    if (showCustomDurationSheet) {
        CustomDurationSheet(
            initialMinutes = customMinutes,
            onDismiss = { showCustomDurationSheet = false },
            onDurationSelected = { minutes ->
                FocusTimerManager.setCustomDuration(minutes)
            }
        )
    }

    if (showTopicSelectionSheet) {
        val effectiveSemester = userProfile?.semester?.takeIf { it > 0 }
            ?: userProfile?.currentSemester?.takeIf { it > 0 }
            ?: 1
        TopicSelectionSheet(
            onDismiss = { showTopicSelectionSheet = false },
            userBranch = userProfile?.branch.orEmpty(),
            userSemester = effectiveSemester,
            completedSemesters = userStats.completedSemesters,
            completedTopicKeys = userStats.completedTopicKeys.toSet(),
            backlogSubjects = userProfile?.backlogSubjects.orEmpty(),
            onTopicSelected = { tName, sName, semName ->
                FocusTimerManager.setTopic(tName, sName, semName, null)
            }
        )
    }

    if (showCompletionDialog) {
        SessionCompleteDialog(
            minutesStudied = completedStudiedMinutes,
            xpEarned = xpEarnedThisSession.toInt(),
            streakDays = userStats.effectiveStreak,
            onContinueClick = {
                FocusTimerManager.dismissCompletionDialog()
            }
        )
    }
}

@Composable
private fun FocusStudySection(title: String, body: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = PurpleAccent.copy(alpha = 0.06f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PurpleAccent,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextPrimary
            )
        }
    }
}
