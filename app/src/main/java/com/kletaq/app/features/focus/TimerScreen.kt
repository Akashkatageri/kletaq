package com.kletaq.app.features.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kletaq.app.core.theme.InkPaperBorder
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.core.notes.ModuleNotesRepository
import com.kletaq.app.core.notes.ModuleScanState
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.data.repository.KletaqAcademicRepository
import com.kletaq.app.data.repository.ProgressionRepositoryImpl
import com.kletaq.app.data.repository.UserSettingsRepository
import com.kletaq.app.domain.progression.ProgressionCalculator
import com.kletaq.app.features.chat.TopicChatEntry
import com.kletaq.app.features.lesson.components.UploadNotesDialog

import com.kletaq.app.core.util.MathFormattedText
import com.kletaq.app.data.repository.AcademicContentRepository
import com.kletaq.app.data.repository.toExamPrep
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
import com.kletaq.app.features.lesson.components.HandwrittenNotesCard
import com.kletaq.app.features.lesson.components.HandwrittenNotesViewerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(
    topicName: String? = null,
    subjectName: String? = null,
    semesterName: String? = null,
    examPrep: ExamPrep? = null,
    onBackClick: () -> Unit = {}
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
    val activity = context as? android.app.Activity
    val view = androidx.compose.ui.platform.LocalView.current

    // Keep screen awake while study timer is running
    DisposableEffect(isRunning) {
        if (isRunning) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = true
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = false
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = false
        }
    }

    val currentTopicName by FocusTimerManager.currentTopicName.collectAsState()
    val hasTopic = !currentTopicName.isNullOrBlank() && currentTopicName != "No topic selected"
    val currentSubjectName by FocusTimerManager.currentSubjectName.collectAsState()
    val currentSemesterName by FocusTimerManager.currentSemesterName.collectAsState()
    val currentExamPrep by FocusTimerManager.currentExamPrep.collectAsState()
    val showCompletionDialog by FocusTimerManager.showCompletionDialog.collectAsState()
    val completedStudiedMinutes by FocusTimerManager.completedStudiedMinutes.collectAsState()

    var showNotesViewer by remember { mutableStateOf(false) }
    var activeViewerPage by remember { mutableIntStateOf(0) }

    val currentTopicStudyPack = remember(currentTopicName, currentSubjectName) {
        val topic = currentTopicName
        if (!topic.isNullOrBlank()) {
            AcademicContentRepository.getStudyPack(
                topicId = topic,
                topicTitle = topic,
                subjectName = currentSubjectName.orEmpty()
            )
        } else null
    }

    val moduleIndex = remember(currentTopicName) {
        com.kletaq.app.core.notes.CustomNotesManager.resolveModuleIndex(currentTopicName ?: "")
    }
    val customNotes = remember(currentSubjectName, currentTopicName, moduleIndex) {
        val subj = currentSubjectName.orEmpty()
        if (subj.isNotBlank()) {
            com.kletaq.app.core.notes.CustomNotesManager.getNotesForModule(context, subj, moduleIndex)
        } else emptyList()
    }
    val effectiveNotes = if (customNotes.isNotEmpty()) customNotes else currentTopicStudyPack?.handwrittenNotePages.orEmpty()

    val effectiveSubjectCode = remember(currentTopicName, currentSubjectName) {
        val subj = currentSubjectName.orEmpty()
        if (subj.isNotBlank()) {
            val semSubjects = KletaqAcademicRepository.getSemesters().flatMap { it.subjects }
            semSubjects.firstOrNull { it.name.equals(subj, ignoreCase = true) || it.id.equals(subj, ignoreCase = true) }?.id ?: subj
        } else {
            val topic = currentTopicName.orEmpty()
            if (topic.isNotBlank()) {
                KletaqAcademicRepository.getSemesters().asSequence().flatMap { sem ->
                    sem.subjects.asSequence().flatMap { s ->
                        s.units.asSequence().flatMap { u ->
                            u.lessons.asSequence().map { l -> s.id to l.id }
                        }
                    }
                }.firstOrNull { it.second == topic }?.first.orEmpty()
            } else ""
        }
    }

    val scanState by if (effectiveSubjectCode.isNotBlank()) {
        ModuleNotesRepository.getScanState(context, effectiveSubjectCode, moduleIndex).collectAsState()
    } else {
        remember { mutableStateOf<ModuleScanState>(ModuleScanState.NoNotes) }
    }

    var showUploadDialog by remember { mutableStateOf(false) }

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

    var showQuitConfirmDialog by remember { mutableStateOf(false) }
    var wasRunningBeforeQuitPrompt by remember { mutableStateOf(false) }
    var pendingBackAfterQuit by remember { mutableStateOf(false) }

    fun promptQuit(isNavigatingBack: Boolean) {
        val hasProgress = isRunning || (totalSeconds > 0 && remainingSeconds < totalSeconds)
        if (hasProgress) {
            wasRunningBeforeQuitPrompt = isRunning
            if (isRunning) {
                FocusTimerManager.pause()
            }
            pendingBackAfterQuit = isNavigatingBack
            isAmbientMode = false
            showQuitConfirmDialog = true
        } else {
            if (isNavigatingBack) {
                onBackClick()
            } else {
                FocusTimerManager.reset()
            }
        }
    }

    BackHandler(enabled = true) {
        promptQuit(isNavigatingBack = true)
    }

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
                            IconButton(
                                onClick = { promptQuit(isNavigatingBack = true) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
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

        CircularTimerHero(
            timeFormatted = timeFormatted,
            progress = progress,
            xpEarned = if (xpEarnedThisSession > 0) xpEarnedThisSession.toInt() else currentSessionXp.toInt(),
            streakDays = userStats.effectiveStreak,
            isStreakActiveToday = userStats.isStreakActiveToday
        )

        if (effectiveNotes.isNotEmpty()) {
            HandwrittenNotesCard(
                pages = effectiveNotes,
                onOpenPage = { page ->
                    activeViewerPage = page
                    showNotesViewer = true
                },
                isCustomUpload = customNotes.isNotEmpty()
            )
        }

        // 2. Note-Grounded AI Tutor Card
        TimerScreenNoteAiCard(
            topicId = currentTopicName.orEmpty().ifBlank { "quick_focus_session" },
            title = currentTopicName.orEmpty().ifBlank { "Focus Study Assistant" },
            hasTopic = hasTopic,
            scanState = scanState,
            onSelectTopic = { showTopicSelectionSheet = true },
            onUploadNotes = { showUploadDialog = true },
            onRetryScan = {
                if (effectiveSubjectCode.isNotBlank()) {
                    ModuleNotesRepository.ensureScanned(
                        context = context,
                        subjectCode = effectiveSubjectCode,
                        moduleIndex = moduleIndex,
                        forceRefresh = true
                    )
                }
            }
        )

        // 3. Timer Control Row
        TimerControlsRow(
            isRunning = isRunning,
            onStartPauseClick = {
                if (isRunning) FocusTimerManager.pause() else FocusTimerManager.start()
            },
            onStopClick = {
                promptQuit(isNavigatingBack = false)
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
                val resolvedStudyPack = AcademicContentRepository.getStudyPack(
                    topicId = tName,
                    topicTitle = tName,
                    subjectName = sName
                )
                FocusTimerManager.setTopic(tName, sName, semName, resolvedStudyPack.toExamPrep())
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

    if (showNotesViewer && effectiveNotes.isNotEmpty()) {
        HandwrittenNotesViewerDialog(
            pages = effectiveNotes,
            initialPage = activeViewerPage,
            topicTitle = currentTopicName ?: currentTopicStudyPack?.title ?: "Module Notes",
            onDismiss = { showNotesViewer = false }
        )
    }

    if (showUploadDialog && effectiveSubjectCode.isNotBlank()) {
        UploadNotesDialog(
            subjectCode = effectiveSubjectCode,
            moduleIndex = moduleIndex,
            onDismiss = { showUploadDialog = false },
            onNotesUploaded = { _ ->
                showUploadDialog = false
            }
        )
    }

    if (showQuitConfirmDialog) {
        FocusQuitConfirmationDialog(
            onDismiss = {
                showQuitConfirmDialog = false
                if (wasRunningBeforeQuitPrompt) {
                    FocusTimerManager.start()
                }
                pendingBackAfterQuit = false
            },
            onConfirmQuit = {
                showQuitConfirmDialog = false
                FocusTimerManager.reset()
                if (pendingBackAfterQuit) {
                    onBackClick()
                }
                pendingBackAfterQuit = false
            }
        )
    }
}

@Composable
private fun FocusQuitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmQuit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = "Quit Focus Session?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Are you sure you want to quit? Your timer progress will be lost.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Button: Keep Focusing
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Keep Focusing",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Button: Quit and discard progress
                TextButton(
                    onClick = onConfirmQuit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Quit & Lose Progress",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}


@Composable
private fun TimerScreenNoteAiCard(
    topicId: String,
    title: String,
    hasTopic: Boolean,
    scanState: ModuleScanState,
    onSelectTopic: () -> Unit,
    onUploadNotes: () -> Unit,
    onRetryScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "NOTE-GROUNDED AI TUTOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent
                        )
                        if (hasTopic) {
                            when (scanState) {
                                is ModuleScanState.Ready -> {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${scanState.totalPages} Pages Indexed",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                is ModuleScanState.Scanning -> {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PurpleAccent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Scanning ${(scanState.progress * 100).toInt()}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                is ModuleScanState.NoNotes -> {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "No Notes",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                is ModuleScanState.ScanFailed -> {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Scan Failed",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PurpleAccent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Ready to Assist",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (hasTopic) "Answers doubts strictly based on your attached module notes with [Page X] citations."
                        else "Ask doubts or select a topic to link notes for citation-grounded tutoring.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!hasTopic) {
                Text(
                    text = "Ask questions anytime during your focus session, or select a topic to ground AI answers directly in your classroom notes.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                TopicChatEntry(
                    topicId = "quick_focus_session",
                    title = "Focus Study Assistant",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                when (scanState) {
                    is ModuleScanState.Ready -> {
                        Text(
                            text = "Ask questions, clarify derivations, or request explanations from your notes while you study.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TopicChatEntry(
                            topicId = topicId,
                            title = title,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is ModuleScanState.Scanning -> {
                        LinearProgressIndicator(
                            progress = { scanState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PurpleAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scanning page ${scanState.scannedPages} of ${scanState.totalPages} on-device...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ModuleScanState.NoNotes -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Attach module notes above to enable citations.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onUploadNotes,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Attach Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TopicChatEntry(
                            topicId = topicId,
                            title = title,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is ModuleScanState.ScanFailed -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Failed scanning notes. Tap retry to process with on-device OCR.",
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onRetryScan,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TopicChatEntry(
                            topicId = topicId,
                            title = title,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
