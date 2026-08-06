package com.studyos.app.features.journey.components

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.data.repository.NoteRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonBottomSheet(
    lesson: LessonNode,
    onDismiss: () -> Unit,
    onStartLesson: (LessonNode) -> Unit,
    onStartFocus: (LessonNode) -> Unit = {},
    onToggleRevision: (LessonNode) -> Unit,
    onToggleBookmark: (LessonNode) -> Unit,
    onMarkComplete: ((LessonNode) -> Unit)? = null,
    onResetNode: ((LessonNode) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val allNotes by NoteRepository.notes.collectAsState()
    val topicNotes = remember(allNotes, lesson.title) {
        NoteRepository.getNotesForTopic(lesson.title)
    }

    var showNotesExpanded by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.height(14.dp))

            // Topic Action Items (Requirement 4 Integration)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Focus session item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onStartFocus(lesson)
                            onDismiss()
                        },
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

                // Your Notes item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNotesExpanded = !showNotesExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "•", fontSize = 16.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your notes (${topicNotes.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showNotesExpanded) "(tap to hide)" else "(tap to view)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expanded Notes for Topic
                AnimatedVisibility(visible = showNotesExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 6.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (topicNotes.isEmpty()) {
                            Text(
                                text = "No notes saved for this topic yet. Tap Quick Note (+) to add one!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            topicNotes.forEach { note ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(0.5.dp, PurpleAccent.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "📝 ${note.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = note.content, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bookmarks item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleBookmark(lesson) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "•", fontSize = 16.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (lesson.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bookmarks (${if (lesson.isBookmarked) 1 else 0})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Primary CTAs: Start/Review & Mark Complete / Reset Node
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (lesson.status == LessonStatus.COMPLETED) {
                    ThemedInkActionButton(
                        text = "Review Lesson",
                        icon = Icons.Default.MenuBook,
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                        onClick = {
                            onStartLesson(lesson)
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
                    ThemedInkActionButton(
                        text = "Start Lesson",
                        icon = Icons.Default.PlayArrow,
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White,
                        onClick = {
                            onStartLesson(lesson)
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
