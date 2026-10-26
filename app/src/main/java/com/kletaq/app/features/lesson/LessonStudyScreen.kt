package com.kletaq.app.features.lesson

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.data.model.ExamModelAnswer
import com.kletaq.app.data.model.FormulaItem
import com.kletaq.app.data.model.QuizQuestion
import com.kletaq.app.data.model.TopicStudyPack
import com.kletaq.app.features.chat.TopicChatEntry
import com.kletaq.app.core.util.MathFormattedText
import com.kletaq.app.core.notes.CustomNotesManager
import com.kletaq.app.features.lesson.components.HandwrittenNotesCard
import com.kletaq.app.features.lesson.components.HandwrittenNotesViewerDialog
import com.kletaq.app.features.lesson.components.UploadNotesDialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Modern Continuous-Scroll Lesson Study Screen.
 * Transforms raw nodes into rich, scannable, interactive study sessions.
 * Features:
 * - Simple Intuition + Technical Core Concept
 * - Highlighted Code Snippets with 1-Tap Copy Button
 * - Formatted Mathematical Equations
 * - Expandable VTU Exam Model Answers with Marking Tips
 * - Interactive 2-Question Self-Check Quiz
 * - Integrated Focus Timer duration selection
 * - Preserved Topic-level AI Doubt Chat
 */
@Composable
fun LessonStudyScreen(
    studyPack: TopicStudyPack,
    subjectName: String,
    semesterName: String,
    initialEstimatedMinutes: Int = 20,
    isCompleted: Boolean = false,
    onBackClick: () -> Unit,
    onStartFocusTimer: (chosenMinutes: Int) -> Unit,
    onMarkCompleted: () -> Unit,
    onAwardXp: (amount: Long, source: String) -> Unit = { _, _ -> }
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val soundManager = remember(context) {
        com.kletaq.app.core.sound.SoundEffectsManager(context).apply { initialize() }
    }
    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    var selectedMinutes by remember(studyPack.topicId) { mutableIntStateOf(initialEstimatedMinutes) }

    val moduleIndex = remember(studyPack.topicId) { CustomNotesManager.resolveModuleIndex(studyPack.topicId) }
    var customNotes by remember(studyPack.topicId, studyPack.subjectCode) {
        mutableStateOf(CustomNotesManager.getNotesForModule(context, studyPack.subjectCode, moduleIndex))
    }
    val effectiveNotes = if (customNotes.isNotEmpty()) customNotes else studyPack.handwrittenNotePages

    var showNotesViewer by remember { mutableStateOf(false) }
    var activeViewerPage by remember { mutableIntStateOf(0) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showCustomTimerDialog by remember { mutableStateOf(false) }

    val scanState by com.kletaq.app.core.notes.ModuleNotesRepository.getScanState(context, studyPack.subjectCode, moduleIndex).collectAsState()

    val isMathSubject = remember(studyPack.subjectCode, subjectName) {
        val cleanCode = studyPack.subjectCode.uppercase()
        val cleanName = subjectName.lowercase()
        cleanName.contains("math") ||
            cleanName.contains("calculus") ||
            cleanName.contains("probability") ||
            cleanName.contains("statistics") ||
            cleanName.contains("distribution") ||
            cleanName.contains("differential") ||
            cleanName.contains("discrete") ||
            cleanName.contains("linear algebra") ||
            cleanCode.contains("MAT") ||
            cleanCode.contains("BCS301") ||
            cleanCode.contains("1BCS301") ||
            cleanCode.contains("BMAT")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subjectName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                        Text(
                            text = studyPack.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (studyPack.isAiGenerated) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (studyPack.isAiGenerated) Color(0xFF6366F1).copy(alpha = 0.3f) else Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (studyPack.isAiGenerated) "AI Study Pack" else "Verified Syllabus",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (studyPack.isAiGenerated) Color(0xFF6366F1) else Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // AI Doubt Chat Entry
                TopicChatEntry(
                    topicId = studyPack.topicId,
                    title = studyPack.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        // Scrollable Continuous Study Story Flow
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Classroom / Module Handwritten Notes (Prominently at the top)
            HandwrittenNotesCard(
                pages = effectiveNotes,
                isCustomUpload = customNotes.isNotEmpty(),
                onOpenPage = { pageIndex ->
                    activeViewerPage = pageIndex
                    showNotesViewer = true
                },
                onUploadNotes = { showUploadDialog = true },
                onDeleteNotes = {
                    CustomNotesManager.deleteNotesForModule(context, studyPack.subjectCode, moduleIndex)
                    customNotes = emptyList()
                }
            )

            // Section 1.5: AI Note-Grounded Topic Flashcards
            com.kletaq.app.features.flashcards.TopicFlashcardDeckCard(
                topicId = studyPack.topicId,
                topicTitle = studyPack.title,
                subjectCode = studyPack.subjectCode,
                moduleIndex = moduleIndex
            )

            // Section 2: Note-Grounded AI Study Assistant
            StudyScreenNoteAiCard(
                topicId = studyPack.topicId,
                title = studyPack.title,
                scanState = scanState,
                onUploadNotes = { showUploadDialog = true },
                onRetryScan = {
                    com.kletaq.app.core.notes.ModuleNotesRepository.ensureScanned(context, studyPack.subjectCode, moduleIndex, forceRefresh = true)
                }
            )


            // Section 3: Addictive 30-Second Active Recall Micro-Quiz
            // Condition: ONLY shown when notes are uploaded AND not a Maths subject
            if (!isMathSubject && customNotes.isNotEmpty() && studyPack.quickQuiz.isNotEmpty()) {
                com.kletaq.app.features.lesson.components.QuickQuizSection(
                    quiz = studyPack.quickQuiz,
                    topicTitle = studyPack.title,
                    isFromUploadedNotes = true,
                    onAwardXp = onAwardXp
                )
            }

            // Section 4: Mathematical Formulas & Equations (if present)
            if (studyPack.formulas.isNotEmpty()) {
                FormulasCard(studyPack.formulas)
            }

            // Section 5: University Exam Model Questions (7/8M Derivations & Numerical Solvers)
            if (studyPack.examQuestions.isNotEmpty()) {
                com.kletaq.app.features.lesson.components.ExamQuestionsSection(
                    questions = studyPack.examQuestions,
                    onDerivationMastered = { marks ->
                        val xpBonus = (marks * 5).toLong()
                        soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.XP_GAIN)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onAwardXp(xpBonus, "Derivation Mastered: ${studyPack.title}")
                        Toast.makeText(context, "Derivation Mastered! +$xpBonus XP earned 🔥", Toast.LENGTH_SHORT).show()
                    },
                    onScoreSubmitted = { score, maxMarks ->
                        val xpBonus = (score * 3).toLong()
                        soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.XP_GAIN)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onAwardXp(xpBonus, "Exam Self-Score: ${studyPack.title}")
                        Toast.makeText(context, "Self-Score Recorded: $score / $maxMarks Marks (+$xpBonus XP) 🔥", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

        }

        // Bottom Action Bar: Focus Timer & Completion (2 Buttons Only)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus Button - Opens Custom Timer Picker
                Button(
                    onClick = {
                        soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.BUTTON_CLICK)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        showCustomTimerDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Complete Button
                Button(
                    onClick = {
                        soundManager.playSound(com.kletaq.app.core.sound.SoundEffectType.XP_GAIN)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onMarkCompleted()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showCustomTimerDialog) {
        CustomFocusTimerDialog(
            initialMinutes = selectedMinutes,
            onDismiss = { showCustomTimerDialog = false },
            onConfirm = { chosenMinutes ->
                selectedMinutes = chosenMinutes
                showCustomTimerDialog = false
                onStartFocusTimer(chosenMinutes)
            }
        )
    }

    if (showUploadDialog) {
        UploadNotesDialog(
            subjectCode = studyPack.subjectCode,
            moduleIndex = moduleIndex,
            onDismiss = { showUploadDialog = false },
            onNotesUploaded = { newPages ->
                customNotes = newPages
            }
        )
    }

    if (showNotesViewer && effectiveNotes.isNotEmpty()) {
        HandwrittenNotesViewerDialog(
            pages = effectiveNotes,
            initialPage = activeViewerPage,
            topicTitle = studyPack.title,
            onDismiss = { showNotesViewer = false }
        )
    }
}

@Composable
fun CustomFocusTimerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var durationMinutes by remember { mutableIntStateOf(initialMinutes.coerceIn(5, 120)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set Focus Duration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose study session time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stepper + Big Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { if (durationMinutes > 5) durationMinutes -= 5 },
                        enabled = durationMinutes > 5,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease 5m")
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$durationMinutes",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = PurpleAccent
                        )
                        Text(
                            text = "MINUTES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    FilledTonalIconButton(
                        onClick = { if (durationMinutes < 120) durationMinutes += 5 },
                        enabled = durationMinutes < 120,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase 5m")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slider (5m to 120m, 5-minute increments)
                Slider(
                    value = durationMinutes.toFloat(),
                    onValueChange = { durationMinutes = ((it / 5).toInt() * 5).coerceIn(5, 120) },
                    valueRange = 5f..120f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = PurpleAccent,
                        activeTrackColor = PurpleAccent,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 20, 25, 30, 45, 60).forEach { preset ->
                        val isSelected = durationMinutes == preset
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { durationMinutes = preset }
                        ) {
                            Text(
                                text = "${preset}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 7.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Cancel & Start Focus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = { onConfirm(durationMinutes) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Focus",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyScreenNoteAiCard(
    topicId: String,
    title: String,
    scanState: com.kletaq.app.core.notes.ModuleScanState,
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
                            is com.kletaq.app.core.notes.ModuleScanState.Ready -> {
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
                            is com.kletaq.app.core.notes.ModuleScanState.Scanning -> {
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
                            is com.kletaq.app.core.notes.ModuleScanState.NoNotes -> {
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
                            is com.kletaq.app.core.notes.ModuleScanState.ScanFailed -> {
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
                is com.kletaq.app.core.notes.ModuleScanState.Ready -> {
                    Text(
                        text = "Ask questions, clarify derivations, or request explanations. If something isn't in your notes, the AI will let you know.",
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
                is com.kletaq.app.core.notes.ModuleScanState.Scanning -> {
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
                is com.kletaq.app.core.notes.ModuleScanState.NoNotes -> {
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
                                text = "Attach your module PDF or photos above to enable note-grounded AI answers and unlock active recall quizzes.",
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
                is com.kletaq.app.core.notes.ModuleScanState.ScanFailed -> {
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
private fun FormulasCard(formulas: List<FormulaItem>) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Key Formulas & Governing Equations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            formulas.forEach { item ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = item.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PurpleAccent.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        MathFormattedText(
                            text = item.expression,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PurpleAccent
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

