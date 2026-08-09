package com.studyos.app.features.focus

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
import com.studyos.app.core.theme.InkPaperBorder
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.studyos.app.core.theme.CardSurface
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.core.theme.TextPrimary
import com.studyos.app.core.theme.TextSecondary
import com.studyos.app.data.model.UserStats
import com.studyos.app.data.model.toUserStatsSafe
import com.studyos.app.data.repository.ProgressionRepositoryImpl
import com.studyos.app.data.repository.UserSettingsRepository
import com.studyos.app.domain.progression.ProgressionCalculator

import com.studyos.app.features.focus.components.CircularTimerHero
import com.studyos.app.features.focus.components.CustomDurationSheet
import com.studyos.app.features.focus.components.FocusStatsCard
import com.studyos.app.features.focus.components.SessionCompleteDialog
import com.studyos.app.features.focus.components.SessionMode
import com.studyos.app.features.focus.components.SessionTypeSelector
import com.studyos.app.features.focus.components.TimerControlsRow
import com.studyos.app.features.focus.components.TopicSelectionSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(
    topicName: String? = null,
    subjectName: String? = null,
    semesterName: String? = null
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userSettingsState by UserSettingsRepository.userSettingsState.collectAsState()
    var userStats by remember { mutableStateOf(UserStats()) }

    // Live Snapshot Listener for UserStats
    DisposableEffect(currentUser) {
        var listenerRegistration: ListenerRegistration? = null
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("stats").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val stats = snapshot.toUserStatsSafe()
                        if (stats != null) {
                            userStats = stats
                        }
                    }
                }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    var currentTopicName by remember(topicName) { mutableStateOf(topicName) }
    var currentSubjectName by remember(subjectName) { mutableStateOf(subjectName) }
    var currentSemesterName by remember(semesterName) { mutableStateOf(semesterName) }

    var selectedMode by remember { mutableStateOf(SessionMode.POMODORO) }
    var customMinutes by remember { mutableIntStateOf(30) }

    val activeDurationMinutes = if (selectedMode == SessionMode.CUSTOM) customMinutes else selectedMode.defaultMinutes
    var remainingSeconds by remember(selectedMode, customMinutes) { mutableIntStateOf(activeDurationMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }


    var showCustomDurationSheet by remember { mutableStateOf(false) }
    var showTopicSelectionSheet by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var xpEarnedThisSession by remember { mutableStateOf(0L) }

    val totalSeconds = activeDurationMinutes * 60
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



    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        } else if (isRunning && remainingSeconds == 0) {
            isRunning = false
            val studiedMinutes = (totalSeconds - remainingSeconds) / 60
            if (studiedMinutes >= 1) {
                showCompletionDialog = true
            }
        }
    }

    // Award real XP when session completes and update persisted stats
    LaunchedEffect(showCompletionDialog) {
        if (showCompletionDialog && currentUser != null) {
            val studiedMinutes = ((totalSeconds - remainingSeconds) / 60).coerceAtLeast(1)
            val repo = ProgressionRepositoryImpl(FirebaseFirestore.getInstance())
            val initialTotalXp = userStats.totalXp
            val result = repo.finishFocusSession(
                uid = currentUser.uid,
                focusMinutes = studiedMinutes,
                dailyGoalMinutes = activeDurationMinutes
            )
            result.onSuccess { updatedStats ->
                userStats = updatedStats
                xpEarnedThisSession = (updatedStats.totalXp - initialTotalXp).coerceAtLeast(0L)
            }
        }
    }

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
                selectedMode = mode
                isRunning = false
            },
            onCustomClick = { showCustomDurationSheet = true }
        )

        // 2. Circular Timer Hero (Reads REAL userStats.studyStreak and real calculated XP!)
        CircularTimerHero(
            timeFormatted = timeFormatted,
            progress = progress,
            xpEarned = if (xpEarnedThisSession > 0) xpEarnedThisSession.toInt() else currentSessionXp.toInt(),
            streakDays = userStats.studyStreak
        )

        // 3. Timer Control Row
        TimerControlsRow(
            isRunning = isRunning,
            onStartPauseClick = { isRunning = !isRunning },
            onStopClick = {
                val studiedMinutes = (totalSeconds - remainingSeconds) / 60
                isRunning = false
                if (studiedMinutes >= 1) {
                    showCompletionDialog = true
                } else {
                    remainingSeconds = activeDurationMinutes * 60
                }
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



    if (showCustomDurationSheet) {
        CustomDurationSheet(
            initialMinutes = customMinutes,
            onDismiss = { showCustomDurationSheet = false },
            onDurationSelected = { minutes ->
                customMinutes = minutes
                selectedMode = SessionMode.CUSTOM
                isRunning = false
            }
        )
    }

    if (showTopicSelectionSheet) {
        TopicSelectionSheet(
            onDismiss = { showTopicSelectionSheet = false },
            onTopicSelected = { tName, sName, semName ->
                currentTopicName = tName
                currentSubjectName = sName
                currentSemesterName = semName
            }
        )
    }

    if (showCompletionDialog) {
        SessionCompleteDialog(
            minutesStudied = (totalSeconds - remainingSeconds) / 60,
            xpEarned = xpEarnedThisSession.toInt(),
            streakDays = userStats.studyStreak,
            onContinueClick = {
                showCompletionDialog = false
                remainingSeconds = activeDurationMinutes * 60
            }
        )
    }
}
