package com.kletaq.app.features.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.kletaq.app.core.theme.InkPaperBorder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.domain.model.RepeatSchedule
import com.kletaq.app.domain.model.StudyTask
import com.kletaq.app.domain.model.TaskCategory
import com.kletaq.app.domain.model.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsSheet(
    task: StudyTask,
    onDismiss: () -> Unit
) {
    var taskTitle by remember(task) { mutableStateOf(task.title) }
    var selectedPriority by remember(task) { mutableStateOf(task.priority) }
    var selectedCategory by remember(task) { mutableStateOf(task.category) }
    var selectedRepeat by remember(task) { mutableStateOf(task.repeatSchedule) }

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Bar: "Task Details" and "X" Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Task Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Title Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Title",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.MediumShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        focusedLabelColor = PurpleAccent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // 3. Row 1: Priority & Category Dropdowns (2 Columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Priority Dropdown
                DropdownSelector(
                    modifier = Modifier.weight(1f),
                    label = "Priority",
                    selectedText = selectedPriority.label,
                    options = TaskPriority.values().map { it.label },
                    onOptionSelected = { selectedLabel ->
                        selectedPriority = TaskPriority.values().first { it.label == selectedLabel }
                    }
                )

                // Category Dropdown
                DropdownSelector(
                    modifier = Modifier.weight(1f),
                    label = "Category",
                    selectedText = selectedCategory.label,
                    options = TaskCategory.values().map { it.label },
                    onOptionSelected = { selectedLabel ->
                        selectedCategory = TaskCategory.values().first { it.label == selectedLabel }
                    }
                )
            }

            // 4. Row 2: Repeat Schedule Dropdown (Full Width)
            DropdownSelector(
                modifier = Modifier.fillMaxWidth(),
                label = "Repeat Schedule",
                selectedText = selectedRepeat.label,
                options = listOf(
                    RepeatSchedule.NONE,
                    RepeatSchedule.DAILY,
                    RepeatSchedule.WEEKDAYS,
                    RepeatSchedule.WEEKLY,
                    RepeatSchedule.MONTHLY,
                    RepeatSchedule.EVERY_LECTURE,
                    RepeatSchedule.BEFORE_EXAM
                ).map { it.label },
                onOptionSelected = { selectedLabel ->
                    selectedRepeat = RepeatSchedule.values().firstOrNull { it.label == selectedLabel } ?: RepeatSchedule.NONE
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // 5. Action Buttons (Cancel & Save)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Save Button
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            val updated = task.copy(
                                title = taskTitle.trim(),
                                priority = selectedPriority,
                                category = selectedCategory,
                                repeatSchedule = selectedRepeat
                            )
                            TaskRepository.updateTask(updated)
                            onDismiss()
                        }
                    },
                    enabled = taskTitle.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(
                        text = "Save",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun DropdownSelector(
    modifier: Modifier = Modifier,
    label: String,
    selectedText: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = InkPaperBorder.MediumShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (option == selectedText) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
