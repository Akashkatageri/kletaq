package com.kletaq.app.features.journey.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.focus.components.CustomDurationSheet
import com.kletaq.app.data.repository.AcademicContentRepository
import com.kletaq.app.core.util.MathFormattedText
import com.kletaq.app.core.notes.CustomNotesManager
import com.kletaq.app.features.lesson.components.HandwrittenNotesCard
import com.kletaq.app.features.lesson.components.HandwrittenNotesViewerDialog
import com.kletaq.app.features.lesson.components.UploadNotesDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.AutoAwesome
import com.kletaq.app.core.notes.ModuleNotesRepository
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonBottomSheet(
    lesson: LessonNode,
    onDismiss: () -> Unit,
    onStartLesson: (LessonNode, Int) -> Unit,
    onStartFocus: (LessonNode) -> Unit = {},
    onToggleRevision: (LessonNode) -> Unit,
    onToggleBookmark: (LessonNode) -> Unit = {},
    onMarkComplete: ((LessonNode) -> Unit)? = null,
    onResetNode: ((LessonNode) -> Unit)? = null,
    isReview: Boolean = false
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val studyPack = remember(lesson.id, lesson.title) {
        AcademicContentRepository.getStudyPack(
            topicId = lesson.id,
            topicTitle = lesson.title
        )
    }
    val moduleIndex = remember(lesson.id) { CustomNotesManager.resolveModuleIndex(lesson.id) }
    var customNotes by remember(lesson.id, studyPack.subjectCode) {
        mutableStateOf(CustomNotesManager.getNotesForModule(context, studyPack.subjectCode, moduleIndex))
    }
    val effectiveNotes = if (customNotes.isNotEmpty()) customNotes else studyPack.handwrittenNotePages

    var showNotesViewer by remember { mutableStateOf(false) }
    var activeViewerPage by remember { mutableIntStateOf(0) }
    var showUploadDialog by remember { mutableStateOf(false) }

    val scanState by com.kletaq.app.core.notes.ModuleNotesRepository.getScanState(context, studyPack.subjectCode, moduleIndex).collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets.ime }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            // 1. Lesson Type Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "LESSON ${lesson.lessonNumber}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = lesson.category.emoji, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lesson.category.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Full Topic Title inside sheet
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = lesson.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HandwrittenNotesCard(
                pages = effectiveNotes,
                isCustomUpload = customNotes.isNotEmpty(),
                onOpenPage = { page ->
                    activeViewerPage = page
                    showNotesViewer = true
                },
                onUploadNotes = { showUploadDialog = true },
                onDeleteNotes = {
                    CustomNotesManager.deleteNotesForModule(context, studyPack.subjectCode, moduleIndex)
                    customNotes = emptyList()
                }
            )
            Spacer(modifier = Modifier.height(14.dp))

            NoteAiStatusCard(
                scanState = scanState,
                onUploadNotes = { showUploadDialog = true },
                onRetryScan = { ModuleNotesRepository.ensureScanned(context, studyPack.subjectCode, moduleIndex, forceRefresh = true) }
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Topic Action Items: Focus session
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStartFocus(lesson)
                        onDismiss()
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "•", fontSize = 16.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Start focus session",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Primary CTAs: Start/Review & Mark Complete / Reset Node
            com.kletaq.app.features.chat.TopicChatEntry(lesson.id, lesson.title)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isCompleted = lesson.status == LessonStatus.COMPLETED || isReview
                if (isCompleted) {
                    ThemedInkActionButton(
                        text = "Review Lesson",
                        icon = Icons.Default.MenuBook,
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                        onClick = {
                            onStartLesson(lesson, 20)
                            onDismiss()
                        }
                    )

                    if (onResetNode != null) {
                        ThemedInkActionButton(
                            text = "Reset Node (-80 XP)",
                            icon = Icons.Default.Refresh,
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                            onClick = {
                                onResetNode(lesson)
                                onDismiss()
                            }
                        )
                    }
                } else {
                    if (effectiveNotes.isNotEmpty()) {
                        ThemedInkActionButton(
                            text = "VIEW NOTES (${effectiveNotes.size} PAGES) 📄",
                            icon = Icons.Default.MenuBook,
                            containerColor = PurpleAccent,
                            contentColor = Color.White,
                            onClick = {
                                activeViewerPage = 0
                                showNotesViewer = true
                            }
                        )
                    }

                    ThemedInkActionButton(
                        text = "START LEARNING ⚡",
                        icon = Icons.Default.PlayArrow,
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White,
                        onClick = {
                            onStartLesson(lesson, 20)
                            onDismiss()
                        }
                    )

                    if (onMarkComplete != null) {
                        ThemedInkActionButton(
                            text = "Mark Complete (+80 XP)",
                            icon = Icons.Default.CheckCircle,
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            onClick = {
                                onMarkComplete(lesson)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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
private fun NoteAiStatusCard(
    scanState: com.kletaq.app.core.notes.ModuleScanState,
    onUploadNotes: () -> Unit,
    onRetryScan: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (scanState) {
                is com.kletaq.app.core.notes.ModuleScanState.Ready -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = PurpleAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NOTE AI READY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PurpleAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "AI will answer doubts strictly grounded in your attached classroom notes.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is com.kletaq.app.core.notes.ModuleScanState.Scanning -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PurpleAccent,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SCANNING NOTES FOR AI...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Extracting text on-device: ${scanState.scannedPages}/${scanState.totalPages} pages (${(scanState.progress * 100).toInt()}%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is com.kletaq.app.core.notes.ModuleScanState.NoNotes -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NOTE AI ASSISTANT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Attach module notes above to enable AI note scanning and grounded answers.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = onUploadNotes,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Attach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is com.kletaq.app.core.notes.ModuleScanState.ScanFailed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SCAN FAILED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = scanState.error,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = onRetryScan,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemedInkActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val translateY by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "translateY"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shadowOffset"
    )

    val blackBorder = BorderStroke(1.75.dp, Color(0xFF1A1A1A))
    val pillShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
    ) {
        // 1. Black 3D Bottom Pedestal Shadow
        Surface(
            shape = pillShape,
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = shadowOffset)
        ) {}

        // 2. Top Interactive Button Cap with Solid Black Border
        Surface(
            shape = pillShape,
            color = containerColor,
            contentColor = contentColor,
            border = blackBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = translateY)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
