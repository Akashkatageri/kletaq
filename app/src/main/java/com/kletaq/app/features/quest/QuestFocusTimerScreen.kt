package com.kletaq.app.features.quest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import com.kletaq.app.core.notes.CustomNotesManager
import com.kletaq.app.core.notes.ModuleNotesRepository
import com.kletaq.app.core.notes.ModuleScanState
import com.kletaq.app.data.repository.KletaqAcademicRepository
import com.kletaq.app.features.chat.TopicChatEntry
import com.kletaq.app.features.focus.components.AmbientTimerView
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.lesson.components.HandwrittenNotesCard
import com.kletaq.app.features.lesson.components.HandwrittenNotesViewerDialog
import com.kletaq.app.features.lesson.components.UploadNotesDialog
import com.kletaq.app.features.quest.components.QuestCompletionDialog
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuestFocusTimerScreen(
    questionTitle: String,
    studyContent: String = "",
    notesPages: List<String> = emptyList(),
    isCustomUpload: Boolean = false,
    initialMinutes: Int = 30,
    topicId: String = "",
    subjectCode: String = "",
    onBackClick: (() -> Unit)? = null,
    onFinish: (isCompletedOnTime: Boolean, isExtendedTime: Boolean, isSkipped: Boolean) -> Unit
) {
    val totalSeconds = remember(initialMinutes) { initialMinutes * 60 }
    var remainingSeconds by remember(initialMinutes) { mutableIntStateOf(totalSeconds) }
    var isRunning by remember(initialMinutes) { mutableStateOf(true) }
    var isExtended by remember(initialMinutes) { mutableStateOf(false) }
    var isAmbientMode by remember { mutableStateOf(false) }

    var targetEndTimeMs by remember(initialMinutes) {
        mutableLongStateOf(System.currentTimeMillis() + (totalSeconds * 1000L))
    }

    var showCompletionPrompt by remember { mutableStateOf(false) }
    var showNotesViewer by remember { mutableStateOf(false) }
    var activeViewerPage by remember { mutableIntStateOf(0) }
    var showUploadDialog by remember { mutableStateOf(false) }

    var currentNotesPages by remember(notesPages) { mutableStateOf(notesPages) }
    var currentIsCustomUpload by remember(isCustomUpload) { mutableStateOf(isCustomUpload) }

    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val soundManager = remember(context) {
        com.kletaq.app.core.sound.SoundEffectsManager(context).apply { initialize() }
    }
    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    val effectiveSubjectCode = remember(topicId, subjectCode) {
        if (subjectCode.isNotBlank()) subjectCode
        else {
            KletaqAcademicRepository.getSemesters().asSequence().flatMap { sem ->
                sem.subjects.asSequence().flatMap { subj ->
                    subj.units.asSequence().flatMap { unit ->
                        unit.lessons.asSequence().map { lesson -> subj.id to lesson.id }
                    }
                }
            }.firstOrNull { it.second == topicId }?.first.orEmpty()
        }
    }

    val moduleIndex = remember(topicId) {
        if (topicId.isNotBlank()) CustomNotesManager.resolveModuleIndex(topicId) else 1
    }
    val scanState by ModuleNotesRepository.getScanState(context, effectiveSubjectCode, moduleIndex).collectAsState()

    val scopedRange = remember(topicId, currentNotesPages.size, scanState) {
        val manifest = (scanState as? com.kletaq.app.core.notes.ModuleScanState.Ready)?.manifest
        com.kletaq.app.core.notes.NotesTextExtractor.resolveTopicPageRange(
            topicId = topicId,
            topicTitle = questionTitle,
            topicIndex = 0,
            totalTopicsInModule = 8,
            totalPages = currentNotesPages.size,
            manifest = manifest
        )
    }

    var showQuitConfirmation by remember { mutableStateOf(false) }
    var wasRunningBeforePrompt by remember { mutableStateOf(false) }

    fun promptQuit() {
        if (isRunning) {
            wasRunningBeforePrompt = true
            val diffMs = targetEndTimeMs - System.currentTimeMillis()
            remainingSeconds = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
            isRunning = false
        } else {
            wasRunningBeforePrompt = false
        }
        showQuitConfirmation = true
    }

    BackHandler(enabled = true) {
        if (showQuitConfirmation) {
            showQuitConfirmation = false
            if (wasRunningBeforePrompt) {
                targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                isRunning = true
            }
        } else if (showCompletionPrompt) {
            showCompletionPrompt = false
        } else if (showNotesViewer) {
            showNotesViewer = false
        } else if (showUploadDialog) {
            showUploadDialog = false
        } else if (isAmbientMode) {
            isAmbientMode = false
        } else {
            promptQuit()
        }
    }

    val activity = context as? Activity
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen awake while studying, release when paused or disposed
    DisposableEffect(isRunning) {
        if (isRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = true
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = false
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = false
        }
    }

    // Lifecycle observer: immediately recalculate timer from wall-clock if screen wakes back up
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isRunning) {
                val diffMs = targetEndTimeMs - System.currentTimeMillis()
                val leftSecs = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                remainingSeconds = leftSecs
                if (leftSecs <= 0) {
                    isRunning = false
                    soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.TIMER_COMPLETE)
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showCompletionPrompt = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Robust wall-clock timer loop: cannot drift or freeze
    LaunchedEffect(isRunning) {
        if (isRunning) {
            targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
            while (isRunning && remainingSeconds > 0) {
                delay(500L)
                val diffMs = targetEndTimeMs - System.currentTimeMillis()
                val leftSecs = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                remainingSeconds = leftSecs
                if (leftSecs <= 0) {
                    isRunning = false
                    soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.TIMER_COMPLETE)
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showCompletionPrompt = true
                    break
                }
            }
        }
    }

    if (isAmbientMode) {
        // Battery-optimized OLED pitch black display with minimal brightness
        AmbientTimerView(
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds,
            isRunning = isRunning,
            topicName = questionTitle,
            sessionModeName = "Focus Quest",
            onTogglePlayPause = {
                if (isRunning) {
                    val diffMs = targetEndTimeMs - System.currentTimeMillis()
                    remainingSeconds = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                    isRunning = false
                } else {
                    targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                    isRunning = true
                }
            },
            onWake = {
                isAmbientMode = false
            }
        )
    } else {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val progress = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat() else 1f

        val currentStage = when {
            progress < 0.25f -> 1
            progress < 0.75f -> 2
            else -> 3
        }
        val (currentStageLabel, currentStageColor) = when (currentStage) {
            1 -> "1. Scan Notes" to Color(0xFF0D9488)
            2 -> "2. Master Derivation" to PurpleAccent
            else -> "3. Active Recall" to Color(0xFFF59E0B)
        }

        val animatedProgress by animateFloatAsState(targetValue = progress, label = "TimerProgress")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Current lesson title & optional back navigation & Battery Eco Mode button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = { promptQuit() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PurpleAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "FOCUS MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Battery Saver / Eco Mode Toggle Button
                    IconButton(
                        onClick = { isAmbientMode = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Battery Saver Eco Mode",
                            tint = PurpleAccent
                        )
                    }
                }

            Text(
                text = questionTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            // Parkinson's Law Timer status badge (2x Bonus XP)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (!isExtended) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (!isExtended) Color(0xFF10B981).copy(alpha = 0.4f) else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isExtended) "⚡ Parkinson's Timer: 2x XP if finished on time!" else "⏱ Standard XP (Time extended)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isExtended) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Compact timer with Play/Pause on left, Timer in center, and "+5 min" on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause toggle
                Surface(
                    onClick = {
                        if (isRunning) {
                            val diffMs = targetEndTimeMs - System.currentTimeMillis()
                            remainingSeconds = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                            isRunning = false
                        } else {
                            targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                            isRunning = true
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Circular countdown timer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(136.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )

                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (!isExtended) currentStageColor else Color(0xFFEF4444),
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = if (isRunning) currentStageLabel else "PAUSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) currentStageColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                // "+5m" / Need More Time button RIGHT NEXT TO THE TIMER
                Surface(
                    onClick = {
                        soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.BUTTON_CLICK)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        remainingSeconds += 5 * 60
                        targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                        isExtended = true
                        isRunning = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = PurpleAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.2.dp, PurpleAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.size(width = 56.dp, height = 52.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "+5m",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent
                        )
                        Text(
                            text = "More",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurpleAccent
                        )
                    }
                }
            }

            // 3-Stage Parkinson's Progress Chips (Scan -> Master -> Recall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple(1, "1. Scan", Color(0xFF0D9488)),
                    Triple(2, "2. Master", PurpleAccent),
                    Triple(3, "3. Recall", Color(0xFFF59E0B))
                ).forEach { (stageNum, label, color) ->
                    val isActive = currentStage == stageNum
                    val isPassed = currentStage > stageNum
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) color.copy(alpha = 0.18f) else if (isPassed) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (isActive) color else if (isPassed) color.copy(alpha = 0.4f) else Color.Transparent)
                    ) {
                        Text(
                            text = if (isPassed) "✓ $label" else label,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) color else if (isPassed) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }



            // Prominently display uploaded/module handwritten notes while timer runs
            if (currentNotesPages.isNotEmpty()) {
                HandwrittenNotesCard(
                    pages = currentNotesPages,
                    onOpenPage = { page ->
                        activeViewerPage = page
                        showNotesViewer = true
                    },
                    isCustomUpload = currentIsCustomUpload,
                    scopedRange = scopedRange,
                    topicTitle = questionTitle,
                    onUpdateManualRange = { start, end ->
                        com.kletaq.app.data.repository.UserSettingsRepository.setManualTopicPageRange(topicId, start, end)
                    }
                )
            }

            // Note-Grounded AI Tutor Card
            FocusTimerNoteAiCard(
                topicId = topicId,
                title = questionTitle,
                scanState = scanState,
                onUploadNotes = { showUploadDialog = true },
                onRetryScan = {
                    ModuleNotesRepository.ensureScanned(
                        context = context,
                        subjectCode = effectiveSubjectCode,
                        moduleIndex = moduleIndex,
                        forceRefresh = true
                    )
                }
            )

            // Bottom CTA: MARK COMPLETE Button
            Button(
                onClick = {
                    isRunning = false
                    showCompletionPrompt = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 28.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!isExtended) "MARK COMPLETE (2X XP ⚡)" else "MARK COMPLETE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        if (showUploadDialog) {
            UploadNotesDialog(
                subjectCode = effectiveSubjectCode,
                moduleIndex = moduleIndex,
                onDismiss = { showUploadDialog = false },
                onNotesUploaded = { newPages ->
                    currentNotesPages = newPages
                    currentIsCustomUpload = true
                    showUploadDialog = false
                }
            )
        }

        if (showNotesViewer && currentNotesPages.isNotEmpty()) {
            HandwrittenNotesViewerDialog(
                pages = currentNotesPages,
                initialPage = activeViewerPage,
                topicTitle = questionTitle,
                scopedRange = scopedRange,
                onDismiss = { showNotesViewer = false }
            )
        }

        if (showCompletionPrompt) {
            QuestCompletionDialog(
                questionTitle = questionTitle,
                isParkinsonBonus = !isExtended,
                onCompleted = {
                    soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.XP_GAIN)
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showCompletionPrompt = false
                    onFinish(!isExtended, isExtended, false)
                },
                onDismiss = {
                    showCompletionPrompt = false
                    targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                    isRunning = true
                },
                onSkip = {
                    showCompletionPrompt = false
                    onFinish(false, false, true)
                }
            )
        }

        if (showQuitConfirmation) {
            QuestQuitConfirmationDialog(
                questionTitle = questionTitle,
                onConfirmQuit = {
                    showQuitConfirmation = false
                    isRunning = false
                    onBackClick?.invoke()
                },
                onDismiss = {
                    showQuitConfirmation = false
                    if (wasRunningBeforePrompt) {
                        targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                        isRunning = true
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun FocusTimerNoteAiCard(
    topicId: String,
    title: String,
    scanState: ModuleScanState,
    onUploadNotes: () -> Unit,
    onRetryScan: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
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
                    }
                    Text(
                        text = "Answers doubts strictly based on your attached module notes with [Page X] citations.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                                text = "Attach your module PDF or photos above to enable note-grounded AI answers.",
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
                }
            }
        }
    }
}

@Composable
fun QuestQuitConfirmationDialog(
    questionTitle: String,
    onConfirmQuit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
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


