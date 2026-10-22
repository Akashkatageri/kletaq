package com.kletaq.app.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.isAppInDarkTheme
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.domain.model.StudyTask
import com.kletaq.app.domain.model.TaskPriority

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyTasksSection(
    tasksList: List<StudyTask> = emptyList(),
    onAddTaskClick: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme
    var taskToDelete by remember { mutableStateOf<StudyTask?>(null) }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            shape = RoundedCornerShape(18.dp),
            title = { Text("Delete Task?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete '${task.title}'? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        TaskRepository.deleteTask(task.id)
                        taskToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    val pendingTasks = remember(tasksList) {
        tasksList.filter { !it.isDoneToday }
            .sortedWith(
                compareByDescending<StudyTask> { it.priority == TaskPriority.HIGH }
                    .thenByDescending { it.priority == TaskPriority.MEDIUM }
                    .thenByDescending { it.createdAt }
            )
    }

    val completedTasks = remember(tasksList) {
        tasksList.filter { it.isDoneToday }
            .sortedByDescending { it.createdAt }
    }

    val totalCount = tasksList.size
    var showCompletedSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header Row: "Today's Tasks" + "Open Calendar" link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Tasks",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddTaskClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Open Calendar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
            }
        }

        // Card container with tasks, completed section, and compact "+ Add Task" button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.15f else 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Pending tasks list or empty/caught up banner
                if (pendingTasks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (totalCount > 0) "All caught up for today! 🎉" else "No tasks scheduled yet",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (totalCount > 0) "Great work staying on track." else "Tap below to add your first study task.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    pendingTasks.take(5).forEachIndexed { index, task ->
                        CompactTaskRow(
                            task = task,
                            isDark = isDark,
                            onToggle = { TaskRepository.toggleTaskCompleted(task.id) },
                            onLongClick = { taskToDelete = task }
                        )

                        if (index < pendingTasks.take(5).size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.12f else 0.06f),
                                thickness = 0.75.dp
                            )
                        }
                    }
                }

                // Completed tasks collapsible section
                if (completedTasks.isNotEmpty()) {
                    if (pendingTasks.isNotEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.12f else 0.06f),
                            thickness = 0.75.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCompletedSection = !showCompletedSection }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Completed today (${completedTasks.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Icon(
                            imageVector = if (showCompletedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showCompletedSection) "Collapse completed" else "Expand completed",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showCompletedSection,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            completedTasks.forEachIndexed { index, task ->
                                CompactTaskRow(
                                    task = task,
                                    isDark = isDark,
                                    onToggle = { TaskRepository.toggleTaskCompleted(task.id) },
                                    onLongClick = { taskToDelete = task }
                                )

                                if (index < completedTasks.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.12f else 0.06f),
                                        thickness = 0.75.dp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact "+ Add Task" Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PurpleAccent.copy(alpha = if (isDark) 0.15f else 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAddTaskClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Task",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactTaskRow(
    task: StudyTask,
    isDark: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    val (priorityBg, priorityText) = when (task.priority) {
        TaskPriority.HIGH -> {
            if (isDark) Color(0xFF3B1212) to Color(0xFFF87171)
            else Color(0xFFFEE2E2) to Color(0xFFEF4444)
        }
        TaskPriority.MEDIUM -> {
            if (isDark) Color(0xFF3B2609) to Color(0xFFFBBF24)
            else Color(0xFFFEF3C7) to Color(0xFFD97706)
        }
        TaskPriority.LOW -> {
            if (isDark) Color(0xFF0C243B) to Color(0xFF38BDF8)
            else Color(0xFFE0F2FE) to Color(0xFF0284C7)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded square checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (task.isDoneToday) PurpleAccent else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (task.isDoneToday) PurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.45f else 0.35f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (task.isDoneToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = if (task.isDoneToday) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (task.isDoneToday) TextSecondary.copy(alpha = 0.6f) else TextPrimary,
                    textDecoration = if (task.isDoneToday) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = if (task.isDoneToday) 0.5f else 0.8f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = task.dueDateText,
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = if (task.isDoneToday) 0.5f else 0.8f),
                        textDecoration = if (task.isDoneToday) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Priority Pill Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = priorityBg
        ) {
            Text(
                text = task.priority.label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = priorityText
            )
        }
    }
}
