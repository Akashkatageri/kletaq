package com.kletaq.app.features.tasks.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.domain.model.StudyTask
import com.kletaq.app.domain.model.TaskPriority

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskSheet(
    onDismiss: () -> Unit,
    onTaskCreated: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    var taskTitle by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Data Structures & Algorithms") }
    var topicTitle by remember { mutableStateOf("") }
    var selectedDueText by remember { mutableStateOf("Today") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedDurationMin by remember { mutableIntStateOf(30) }
    var taskDescription by remember { mutableStateOf("") }

    val quickSuggestions = listOf(
        "Revise AVL Trees",
        "Finish Chemistry Module 3",
        "Solve 20 DSA problems",
        "Complete Physics assignment"
    )

    val subjects = listOf(
        "Data Structures & Algorithms",
        "Discrete Mathematics",
        "Applied Chemistry",
        "Operating Systems",
        "Computer Networks"
    )

    val dueOptions = listOf("Today", "Tomorrow", "Next Class", "This Weekend")
    val durationOptions = listOf(15, 30, 60, 90)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddTask,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Create Study Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Plan targeted study activities & assignments",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Suggestions Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "QUICK SUGGESTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSuggestions.forEach { suggestion ->
                        Surface(
                            modifier = Modifier.clickable { taskTitle = suggestion },
                            shape = RoundedCornerShape(10.dp),
                            color = PurpleAccent.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, PurpleAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "+ $suggestion",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PurpleAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 1. Task Title Input
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g. Solve 20 Graph Traversal Problems") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            // 2. Subject Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SUBJECT *",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { subject ->
                        val isSelected = subject == selectedSubject
                        Surface(
                            modifier = Modifier.clickable { selectedSubject = subject },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = subject,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. Optional Topic Title Input
            OutlinedTextField(
                value = topicTitle,
                onValueChange = { topicTitle = it },
                label = { Text("Optional Topic / Module") },
                placeholder = { Text("e.g. Graph Algorithms or Module 2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            // 4. Due Date & Time Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DUE DATE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dueOptions.forEach { option ->
                        val isSelected = option == selectedDueText
                        Surface(
                            modifier = Modifier.clickable { selectedDueText = option },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.dp, PurpleAccent) else null
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 5. Priority Selector (Low, Medium, High)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PRIORITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.values().forEach { priority ->
                        val isSelected = priority == selectedPriority
                        val priorityColor = Color(priority.colorHex)

                        Surface(
                            modifier = Modifier.clickable { selectedPriority = priority },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) priorityColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.dp, priorityColor) else null
                        ) {
                            Text(
                                text = priority.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) priorityColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 6. Estimated Duration Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ESTIMATED DURATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationOptions.forEach { min ->
                        val isSelected = min == selectedDurationMin
                        Surface(
                            modifier = Modifier.clickable { selectedDurationMin = min },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.dp, PurpleAccent) else null
                        ) {
                            Text(
                                text = if (min >= 60) "${min / 60} h" else "${min} min",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 7. Optional Description
            OutlinedTextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                label = { Text("Optional Notes / Details") },
                placeholder = { Text("Add extra detail or specific steps...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons (Cancel / Create Task)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = InkPaperBorder.MediumShape
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            val newTask = StudyTask(
                                title = taskTitle.trim(),
                                subjectName = selectedSubject,
                                topicTitle = topicTitle.ifBlank { null },
                                dueDateText = selectedDueText,
                                priority = selectedPriority,
                                estimatedDurationMin = selectedDurationMin,
                                description = taskDescription.ifBlank { null }
                            )
                            TaskRepository.addTask(newTask)
                            onTaskCreated()
                            onDismiss()
                        }
                    },
                    enabled = taskTitle.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Create Task", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
