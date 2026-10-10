package com.kletaq.app.features.tasks.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.domain.model.RepeatSchedule
import com.kletaq.app.domain.model.StudyTask
import com.kletaq.app.domain.model.TaskCategory
import com.kletaq.app.domain.model.TaskPriority
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskSheet(
    initialDate: String? = null,
    onDismiss: () -> Unit,
    onTaskCreated: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var taskTitle by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(
            try {
                if (initialDate != null) LocalDate.parse(initialDate) else LocalDate.now()
            } catch (e: Exception) {
                LocalDate.now()
            }
        )
    }

    var hasSpecificTime by remember { mutableStateOf(true) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(18, 0)) }
    var selectedReminderMinutes by remember { mutableStateOf<Int?>(15) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var taskDescription by remember { mutableStateOf("") }

    // Date Picker Dialog Launcher
    fun openDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    // Time Picker Dialog Launcher
    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
                hasSpecificTime = true
            },
            selectedTime.hour,
            selectedTime.minute,
            false // 12-hour AM/PM format
        ).show()
    }

    val today = remember { LocalDate.now() }
    val dateDisplayText = remember(selectedDate) {
        when (selectedDate) {
            today -> "Today • " + selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
            today.plusDays(1) -> "Tomorrow • " + selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
            today.minusDays(1) -> "Yesterday • " + selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
            else -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()))
        }
    }

    val timeDisplayText = remember(selectedTime) {
        selectedTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                        text = "Create Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Schedule event with date and notification alarm",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Task Title Input
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g. Project Review or Meeting") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            // 2. Selectable Date (Google Calendar Style)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DUE DATE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Clickable Date Card
                Card(
                    onClick = { openDatePicker() },
                    shape = InkPaperBorder.MediumShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = PurpleAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = dateDisplayText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurpleAccent.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Select",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                            }
                        }
                    }
                }
            }

            // 3. Time & Reminder
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOTIFICATION & REMINDER TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Toggle All-day vs Specific Time
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            modifier = Modifier.clickable { hasSpecificTime = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasSpecificTime) PurpleAccent.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (hasSpecificTime) BorderStroke(1.dp, PurpleAccent) else null
                        ) {
                            Text(
                                text = "Time",
                                fontSize = 11.sp,
                                fontWeight = if (hasSpecificTime) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasSpecificTime) PurpleAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier.clickable { hasSpecificTime = false },
                            shape = RoundedCornerShape(8.dp),
                            color = if (!hasSpecificTime) PurpleAccent.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (!hasSpecificTime) BorderStroke(1.dp, PurpleAccent) else null
                        ) {
                            Text(
                                text = "All day",
                                fontSize = 11.sp,
                                fontWeight = if (!hasSpecificTime) FontWeight.Bold else FontWeight.Normal,
                                color = if (!hasSpecificTime) PurpleAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (hasSpecificTime) {
                    // Custom Time Selector Card
                    Card(
                        onClick = { openTimePicker() },
                        shape = InkPaperBorder.MediumShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Custom Time",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = timeDisplayText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to pick custom time (AM/PM)",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PurpleAccent.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Select Time",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                }
                            }
                        }
                    }

                    // Reminder Options
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "REMIND ME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val reminderOptions = listOf(
                            "At time (0m)" to 0,
                            "10m before" to 10,
                            "15m before" to 15,
                            "30m before" to 30,
                            "1h before" to 60,
                            "No alarm" to null
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reminderOptions.forEach { (label, minutes) ->
                                val isSelected = selectedReminderMinutes == minutes
                                Surface(
                                    modifier = Modifier.clickable { selectedReminderMinutes = minutes },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PurpleAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = if (isSelected) BorderStroke(1.dp, PurpleAccent) else null
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Priority Selector (Low, Medium, High)
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

            // 5. Optional Description / Notes
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

            Spacer(modifier = Modifier.height(4.dp))

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
                            val scheduledTimeStr = if (hasSpecificTime) {
                                selectedTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
                            } else null

                            val newTask = StudyTask(
                                title = taskTitle.trim(),
                                category = TaskCategory.EVENT,
                                repeatSchedule = RepeatSchedule.NONE,
                                dueDateText = selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
                                priority = selectedPriority,
                                subjectName = null,
                                topicTitle = null,
                                estimatedDurationMin = 0,
                                description = taskDescription.ifBlank { null },
                                scheduledDate = selectedDate.toString(),
                                scheduledTime = scheduledTimeStr,
                                reminderMinutesBefore = if (hasSpecificTime) selectedReminderMinutes else null
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
