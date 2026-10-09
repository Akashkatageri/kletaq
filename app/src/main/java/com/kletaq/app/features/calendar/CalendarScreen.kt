package com.kletaq.app.features.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.domain.model.StudyTask
import com.kletaq.app.domain.model.TaskPriority
import com.kletaq.app.features.tasks.components.CreateTaskSheet
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit = {}
) {
    val tasks by TaskRepository.tasks.collectAsState()
    val today = remember { LocalDate.now() }

    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }
    var showCreateTaskSheet by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<StudyTask?>(null) }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${task.title}'? This action cannot be undone.") },
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
                    Text("Cancel")
                }
            }
        )
    }

    // Group active tasks by effective ISO date YYYY-MM-DD
    val tasksByDate = remember(tasks) {
        tasks.filterNot { it.isDeleted }.groupBy { it.effectiveDateIso }
    }

    val selectedDateIso = selectedDate.toString()
    val tasksForSelectedDate = tasksByDate[selectedDateIso].orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calendar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Jump to Today button
                    TextButton(
                        onClick = {
                            currentYearMonth = YearMonth.now()
                            selectedDate = LocalDate.now()
                        }
                    ) {
                        Text(
                            text = "Today",
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTaskSheet = true },
                containerColor = PurpleAccent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task for Date")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // 1. Month Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monthName = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                Text(
                    text = "$monthName ${currentYearMonth.year}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row {
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            // 2. Day-of-Week Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Month Grid
            MonthCalendarGrid(
                yearMonth = currentYearMonth,
                selectedDate = selectedDate,
                today = today,
                tasksByDate = tasksByDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Agenda Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateFormatted = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                Column {
                    Text(
                        text = dateFormatted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (selectedDate == today) "Today's Agenda" else "Scheduled Activities",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                if (tasksForSelectedDate.isNotEmpty()) {
                    val doneCount = tasksForSelectedDate.count { it.isDoneToday }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (doneCount == tasksForSelectedDate.size) Color(0xFF10B981).copy(alpha = 0.15f) else PurpleAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$doneCount/${tasksForSelectedDate.size} Completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (doneCount == tasksForSelectedDate.size) Color(0xFF10B981) else PurpleAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Tasks List for Selected Date
            if (tasksForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Text(
                            text = "No tasks for this day",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap the + button to schedule tasks, events, or reminders",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showCreateTaskSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleAccent.copy(alpha = 0.12f),
                                contentColor = PurpleAccent
                            ),
                            elevation = null,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasksForSelectedDate, key = { it.id }) { task ->
                        CalendarTaskItem(
                            task = task,
                            onToggle = { TaskRepository.toggleTaskCompleted(task.id) },
                            onDelete = { taskToDelete = task }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showCreateTaskSheet) {
        CreateTaskSheet(
            initialDate = selectedDate.toString(),
            onDismiss = { showCreateTaskSheet = false },
            onTaskCreated = {
                showCreateTaskSheet = false
            }
        )
    }
}

@Composable
private fun MonthCalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<String, List<StudyTask>>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)

    Card(
        modifier = modifier,
        shape = InkPaperBorder.MediumShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = InkPaperBorder.mediumBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            var dayCounter = 1
            val totalCells = ((daysInMonth + firstDayOfWeek - 1 + 6) / 7) * 7

            for (row in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 1..7) {
                        val cellIndex = row * 7 + col
                        if (cellIndex < firstDayOfWeek || dayCounter > daysInMonth) {
                            // Blank filler box
                            Box(modifier = Modifier.size(42.dp))
                        } else {
                            val cellDate = yearMonth.atDay(dayCounter)
                            val isSelected = cellDate == selectedDate
                            val isToday = cellDate == today
                            val taskList = tasksByDate[cellDate.toString()].orEmpty()
                            val hasTasks = taskList.isNotEmpty()

                            CalendarDayCell(
                                dayNumber = dayCounter,
                                isSelected = isSelected,
                                isToday = isToday,
                                hasTasks = hasTasks,
                                taskCount = taskList.size,
                                hasHighPriority = taskList.any { it.priority == TaskPriority.HIGH },
                                onClick = { onDateSelected(cellDate) }
                            )
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasTasks: Boolean,
    taskCount: Int,
    hasHighPriority: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> PurpleAccent
                    isToday -> PurpleAccent.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> Color.White
                    isToday -> PurpleAccent
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Task indicator dots
            if (hasTasks) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = when {
                                    isSelected -> Color.White
                                    hasHighPriority -> Color(0xFFEF4444)
                                    else -> Color(0xFF10B981)
                                },
                                shape = CircleShape
                            )
                    )
                    if (taskCount > 1) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color(0xFFF59E0B),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTaskItem(
    task: StudyTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDoneToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else CardSurface
        ),
        border = BorderStroke(
            0.5.dp,
            if (task.isDoneToday) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isDoneToday) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (task.isDoneToday) "Completed" else "Pending",
                    tint = if (task.isDoneToday) Color(0xFF10B981) else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (task.isDoneToday) TextSecondary else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isDoneToday) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (!task.subjectName.isNullOrBlank()) {
                        Text(
                            text = task.subjectName,
                            fontSize = 11.sp,
                            color = PurpleAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = "•", fontSize = 11.sp, color = TextSecondary)
                    }

                    if (!task.scheduledTime.isNullOrBlank()) {
                        val formattedTime = remember(task.scheduledTime) {
                            try {
                                val t = java.time.LocalTime.parse(task.scheduledTime)
                                t.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault()))
                            } catch (e: Exception) {
                                task.scheduledTime
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Scheduled Time",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF59E0B)
                        )
                        if (task.reminderMinutesBefore != null && task.reminderMinutesBefore > 0) {
                            val remText = if (task.reminderMinutesBefore >= 60) "${task.reminderMinutesBefore / 60}h before" else "${task.reminderMinutesBefore}m before"
                            Text(
                                text = "($remText)",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Text(
                            text = "All day",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
