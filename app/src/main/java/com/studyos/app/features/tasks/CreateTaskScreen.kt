package com.studyos.app.features.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.data.repository.TaskRepository
import com.studyos.app.domain.model.RepeatSchedule
import com.studyos.app.domain.model.StudyTask
import com.studyos.app.domain.model.TaskCategory
import com.studyos.app.features.tasks.components.TaskDetailsSheet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTaskScreen(
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val existingTasks by TaskRepository.tasks.collectAsState()

    var taskTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.STUDY) }
    var selectedRepeat by remember { mutableStateOf(RepeatSchedule.NONE) }

    var selectedTaskForDetails by remember { mutableStateOf<StudyTask?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.AddTask,
                contentDescription = null,
                tint = PurpleAccent,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Create Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Scrollable Form & Existing Tasks List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- SECTION 1: QUICK TASK CREATION FORM ---
            CardForm(
                taskTitle = taskTitle,
                onTitleChange = { taskTitle = it },
                selectedCategory = selectedCategory,
                onCategorySelect = { selectedCategory = it },
                selectedRepeat = selectedRepeat,
                onRepeatSelect = { selectedRepeat = it },
                onCreateClick = {
                    if (taskTitle.isNotBlank()) {
                        val newTask = StudyTask(
                            title = taskTitle.trim(),
                            category = selectedCategory,
                            repeatSchedule = selectedRepeat,
                            dueDateText = "Today",
                            subjectName = if (selectedCategory == TaskCategory.STUDY) "General Study" else null
                        )
                        TaskRepository.addTask(newTask)
                        taskTitle = "" // Clear input for next task capture
                    }
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // --- SECTION 2: EXISTING TASKS SECTION ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXISTING TASKS (${existingTasks.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                    Text(
                        text = "Tap task or ⋮ for details",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (existingTasks.isEmpty()) {
                    Text(
                        text = "No tasks yet. Create one above!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    existingTasks.forEach { task ->
                        TaskCardItem(
                            task = task,
                            onToggleClick = { TaskRepository.toggleTaskCompleted(task.id) },
                            onCardClick = { selectedTaskForDetails = task }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Bottom Sheet for Task Details Editing
    selectedTaskForDetails?.let { task ->
        TaskDetailsSheet(
            task = task,
            onDismiss = { selectedTaskForDetails = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardForm(
    taskTitle: String,
    onTitleChange: (String) -> Unit,
    selectedCategory: TaskCategory,
    onCategorySelect: (TaskCategory) -> Unit,
    selectedRepeat: RepeatSchedule,
    onRepeatSelect: (RepeatSchedule) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Task Title Input
        OutlinedTextField(
            value = taskTitle,
            onValueChange = onTitleChange,
            label = { Text("Task Title *") },
            placeholder = { Text("e.g. Finish Chemistry Assignment") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleAccent,
                focusedLabelColor = PurpleAccent
            )
        )

        // 2. Category Selector (Optional)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "CATEGORY (OPTIONAL)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(TaskCategory.values()) { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        modifier = Modifier
                            .widthIn(min = 96.dp)
                            .clickable { onCategorySelect(category) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${category.emoji} ${category.label}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Repeat Schedule Selector (Optional)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "REPEAT SCHEDULE (OPTIONAL)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    RepeatSchedule.NONE,
                    RepeatSchedule.DAILY,
                    RepeatSchedule.WEEKDAYS,
                    RepeatSchedule.WEEKLY,
                    RepeatSchedule.EVERY_LECTURE,
                    RepeatSchedule.BEFORE_EXAM
                ).forEach { schedule ->
                    val isSelected = schedule == selectedRepeat
                    Surface(
                        modifier = Modifier
                            .widthIn(min = 96.dp)
                            .clickable { onRepeatSelect(schedule) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PurpleAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent) else null
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = schedule.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Create Task Action Button
        Button(
            onClick = onCreateClick,
            enabled = taskTitle.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleAccent,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Task", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
