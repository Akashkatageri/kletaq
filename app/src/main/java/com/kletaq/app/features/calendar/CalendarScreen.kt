package com.kletaq.app.features.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.domain.model.RepeatSchedule
import com.kletaq.app.domain.model.StudyTask
import com.kletaq.app.domain.model.TaskCategory
import com.kletaq.app.domain.model.TaskPriority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

enum class CalendarViewMode(val label: String) {
    SCHEDULE("Schedule"),
    WEEK("Week"),
    MONTH("Month")
}

private val BrandPurple = Color(0xFF4C3FB8)
private val BrandPurpleLight = Color(0xFFF3F1FD)
private val BrandPurpleAccent = Color(0xFF5B45E0)
private val SoftGreen = Color(0xFFEDF8F2)
private val GreenAccent = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val tasks by TaskRepository.tasks.collectAsState()
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedView by remember { mutableStateOf(CalendarViewMode.SCHEDULE) }
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<StudyTask?>(null) }
    var showMonthPickerDropdown by remember { mutableStateOf(false) }

    // User profile observation for personalized subtitle ("Aanya's space for a little more balance")
    val authUser = remember { FirebaseAuth.getInstance().currentUser }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    DisposableEffect(authUser?.uid) {
        var listenerReg: ListenerRegistration? = null
        if (authUser != null) {
            listenerReg = FirebaseFirestore.getInstance()
                .collection("users")
                .document(authUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        userProfile = snapshot.toObject(UserProfile::class.java)
                    }
                }
        }
        onDispose {
            listenerReg?.remove()
        }
    }

    val displayName = remember(userProfile, authUser) {
        userProfile?.username?.takeIf { it.isNotBlank() }
            ?: authUser?.displayName?.takeIf { it.isNotBlank() }?.split(" ")?.firstOrNull()
            ?: "Student"
    }

    // Active tasks grouped by effective ISO date YYYY-MM-DD
    val tasksByDate = remember(tasks) {
        tasks.filterNot { it.isDeleted }.groupBy { it.effectiveDateIso }
    }

    val selectedDateIso = selectedDate.toString()
    val tasksForSelectedDate = tasksByDate[selectedDateIso].orEmpty()

    // Delete task dialog confirmation
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
                        Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Bottom bar matching screenshot: "A gentle nudge, right on time." + "+ Add task" pill button
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "A gentle nudge, right on time.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = { showAddPlanDialog = true },
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPurpleAccent,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add task",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Header: Back button + "Your calendar" + Subtitle + Profile circle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Your calendar",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "$displayName's space for a little more balance.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Top right profile outline circle icon
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Month Selector & Date Navigator (e.g. "October 2026 v" + Today + < + >)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMonthPickerDropdown = !showMonthPickerDropdown }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            val monthDisplayName = currentYearMonth.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
                            Text(
                                text = "$monthDisplayName ${currentYearMonth.year}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Month",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Month picker quick dropdown
                        DropdownMenu(
                            expanded = showMonthPickerDropdown,
                            onDismissRequest = { showMonthPickerDropdown = false }
                        ) {
                            val months = listOf(
                                "January", "February", "March", "April", "May", "June",
                                "July", "August", "September", "October", "November", "December"
                            )
                            months.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        currentYearMonth = YearMonth.of(currentYearMonth.year, idx + 1)
                                        showMonthPickerDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // "Today" pill button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val now = LocalDate.now()
                                    selectedDate = now
                                    currentYearMonth = YearMonth.now()
                                }
                        ) {
                            Text(
                                text = "Today",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // < previous button
                        IconButton(
                            onClick = {
                                if (selectedView == CalendarViewMode.MONTH) {
                                    currentYearMonth = currentYearMonth.minusMonths(1)
                                } else {
                                    selectedDate = selectedDate.minusWeeks(1)
                                    currentYearMonth = YearMonth.from(selectedDate)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // > next button
                        IconButton(
                            onClick = {
                                if (selectedView == CalendarViewMode.MONTH) {
                                    currentYearMonth = currentYearMonth.plusMonths(1)
                                } else {
                                    selectedDate = selectedDate.plusWeeks(1)
                                    currentYearMonth = YearMonth.from(selectedDate)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Day-of-Week strip (M, T, W, T, F, S, S) with selected royal purple pill
                CalendarWeekStrip(
                    selectedDate = selectedDate,
                    tasksByDate = tasksByDate,
                    onDateSelected = { date ->
                        selectedDate = date
                        currentYearMonth = YearMonth.from(date)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Segmented View Switcher ("Schedule", "Week", "Month")
                CalendarSegmentedControl(
                    selectedView = selectedView,
                    onViewSelected = { selectedView = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Dynamic Content View (Schedule, Week, or Month)
                when (selectedView) {
                    CalendarViewMode.SCHEDULE -> {
                        CalendarScheduleView(
                            selectedDate = selectedDate,
                            today = today,
                            tasks = tasksForSelectedDate,
                            onToggleTask = { taskId ->
                                TaskRepository.toggleTaskCompletedForDate(taskId, selectedDateIso)
                            },
                            onDeleteTask = { task ->
                                taskToDelete = task
                            },
                            onAddNewTask = { showAddPlanDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    CalendarViewMode.WEEK -> {
                        CalendarWeekView(
                            selectedDate = selectedDate,
                            today = today,
                            tasksByDate = tasksByDate,
                            onDateSelected = { date ->
                                selectedDate = date
                                selectedView = CalendarViewMode.SCHEDULE
                            },
                            onAddNewTask = { showAddPlanDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    CalendarViewMode.MONTH -> {
                        CalendarMonthGridView(
                            yearMonth = currentYearMonth,
                            selectedDate = selectedDate,
                            today = today,
                            tasksByDate = tasksByDate,
                            onDateSelected = { date ->
                                selectedDate = date
                                selectedView = CalendarViewMode.SCHEDULE
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }

        // 6. Make A Little Plan Modal (STRICTLY NO BOTTOMSHEET)
        // Uses an in-screen animated sheet-style overlay card with scrim & BackHandler
        CalendarMakePlanOverlay(
            visible = showAddPlanDialog,
            initialDate = selectedDate,
            displayName = displayName,
            onDismiss = { showAddPlanDialog = false },
            onTaskCreated = { newTask ->
                TaskRepository.addTask(newTask)
                showAddPlanDialog = false
                Toast.makeText(context, "Task planned! 🎯", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * 7-Day horizontal week strip: M, T, W, T, F, S, S
 * When a day is selected, it turns into an elongated royal purple pill with white text and dot!
 */
@Composable
private fun CalendarWeekStrip(
    selectedDate: LocalDate,
    tasksByDate: Map<String, List<StudyTask>>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val monday = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val weekDays = remember(monday) {
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { date ->
            val isSelected = date == selectedDate
            val dayName = date.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault())
            val dayNumber = date.dayOfMonth.toString()
            val hasTasks = tasksByDate[date.toString()]?.isNotEmpty() == true

            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (isSelected) BrandPurple else Color.Transparent
                    )
                    .clickable { onDateSelected(date) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = dayNumber,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .background(Color.White, CircleShape)
                        )
                    } else if (hasTasks) {
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .background(BrandPurpleAccent, CircleShape)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(3.5.dp))
                    }
                }
            }
        }
    }
}

/**
 * Segmented Pill Control: "Schedule", "Week", "Month"
 */
@Composable
private fun CalendarSegmentedControl(
    selectedView: CalendarViewMode,
    onViewSelected: (CalendarViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) Color(0xFF222227) else Color(0xFFF0F1F5)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .padding(2.5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CalendarViewMode.values().forEach { mode ->
            val isSelected = selectedView == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            if (isDark) Color(0xFF32323A) else Color.White
                        } else Color.Transparent
                    )
                    .clickable { onViewSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        if (isDark) Color.White else BrandPurple
                    } else TextSecondary
                )
            }
        }
    }
}

/**
 * Schedule Timeline View:
 * Shows section header ("Today, 23 October · 3 classes · 2 tasks · A little room to breathe. · IST")
 * Current time indicator line ("10:24 ---●-------------------")
 * Detailed cards for classes & study tasks.
 */
@Composable
private fun CalendarScheduleView(
    selectedDate: LocalDate,
    today: LocalDate,
    tasks: List<StudyTask>,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    onAddNewTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelectedToday = selectedDate == today
    val now = remember { LocalTime.now() }
    val currentTimeString = remember { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val timeZoneShort = remember {
        ZoneId.systemDefault().getDisplayName(JTextStyle.SHORT, Locale.getDefault())
    }

    val sortedTasks = remember(tasks) {
        tasks.sortedBy { it.scheduledTime ?: "23:59" }
    }

    val totalItems = sortedTasks.size

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val dateTitle = if (isSelectedToday) {
                    "Today, ${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())}"
                } else {
                    "${selectedDate.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault())}, ${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())}"
                }

                Text(
                    text = dateTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val subtitle = if (totalItems == 0) {
                    "No scheduled items · A little room to breathe."
                } else {
                    "$totalItems ${if (totalItems == 1) "item" else "items"} · A little room to breathe."
                }

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = timeZoneShort,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (sortedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Text(
                        text = "No tasks for this day",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "A little room to breathe.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onAddNewTask,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPurple.copy(alpha = 0.12f),
                            contentColor = BrandPurple
                        ),
                        elevation = null,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val nowTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            val firstFutureIndex = if (isSelectedToday) {
                sortedTasks.indexOfFirst { (it.scheduledTime ?: "23:59") >= nowTimeStr }
            } else -1

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sortedTasks.forEachIndexed { index, task ->
                    if (isSelectedToday && index == firstFutureIndex) {
                        item(key = "current_time_indicator") {
                            CurrentTimeIndicatorRow(currentTime = currentTimeString)
                        }
                    }

                    val isUpNext = isSelectedToday && index == firstFutureIndex && !task.isCompleted

                    item(key = task.id) {
                        TimelineItemCard(
                            task = task,
                            isUpNext = isUpNext,
                            onToggle = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }

                if (isSelectedToday && firstFutureIndex == -1 && sortedTasks.isNotEmpty()) {
                    item(key = "current_time_indicator_bottom") {
                        CurrentTimeIndicatorRow(currentTime = currentTimeString)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

/**
 * Current Time Indicator:
 * Left shows purple current time (e.g. "10:24"), followed by purple circle dot and horizontal line
 */
@Composable
private fun CurrentTimeIndicatorRow(currentTime: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentTime,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPurpleAccent,
            modifier = Modifier.width(52.dp)
        )

        Box(
            modifier = Modifier
                .size(6.dp)
                .background(BrandPurpleAccent, CircleShape)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(BrandPurpleAccent)
        )
    }
}

/**
 * Timeline Item Card:
 * Supports class / lecture style (colored left bar, checkmark on right),
 * and study task style (checkbox on left, reminder badge, time range).
 */
@Composable
private fun TimelineItemCard(
    task: StudyTask,
    isUpNext: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isClassOrLecture = task.category == TaskCategory.STUDY && task.subjectName != null && task.description != null

    val (startTimeDisplay, endTimeDisplay) = remember(task.scheduledTime, task.estimatedDurationMin) {
        val sTime = task.scheduledTime
        if (sTime != null) {
            try {
                val start = LocalTime.parse(sTime)
                val end = start.plusMinutes(task.estimatedDurationMin.toLong())
                Pair(
                    start.format(DateTimeFormatter.ofPattern("HH:mm")),
                    end.format(DateTimeFormatter.ofPattern("HH:mm"))
                )
            } catch (e: Exception) {
                Pair(sTime, "")
            }
        } else {
            Pair("All day", "")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(52.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = startTimeDisplay,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (endTimeDisplay.isNotBlank()) {
                Text(
                    text = endTimeDisplay,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        val cardBg = when {
            task.isDoneToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            isUpNext -> BrandPurpleLight
            task.title.contains("Lab", ignoreCase = true) -> SoftGreen
            else -> CardSurface
        }

        val leftBarColor = when {
            isUpNext -> BrandPurpleAccent
            task.title.contains("Lab", ignoreCase = true) -> GreenAccent
            else -> Color(0xFF94A3B8)
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(
                0.6.dp,
                if (task.isDoneToday) Color(0xFF10B981).copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isClassOrLecture || isUpNext) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(34.dp)
                            .background(leftBarColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isDoneToday) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (task.isDoneToday) "Completed" else "Pending",
                            tint = if (task.isDoneToday) Color(0xFF10B981) else TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isDoneToday) TextSecondary else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isDoneToday) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitleText = task.description?.takeIf { it.isNotBlank() }
                        ?: task.subjectName?.takeIf { it.isNotBlank() }
                        ?: task.topicTitle

                    if (!subtitleText.isNullOrBlank()) {
                        Text(
                            text = subtitleText,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isUpNext) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BrandPurpleAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "UP NEXT · 10 min before",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurpleAccent
                            )
                        }
                    } else if (task.reminderMinutesBefore != null && !task.isDoneToday) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            val reminderLabel = when (task.reminderMinutesBefore) {
                                0 -> "At start"
                                10 -> "10 min before"
                                30 -> "30 min before"
                                else -> "${task.reminderMinutesBefore} min before"
                            }
                            Text(
                                text = reminderLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                if (task.isDoneToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Week Overview: Displays cards for all 7 days of the selected week.
 */
@Composable
private fun CalendarWeekView(
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<String, List<StudyTask>>,
    onDateSelected: (LocalDate) -> Unit,
    onAddNewTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monday = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val weekDays = remember(monday) {
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(weekDays) { day ->
            val dayIso = day.toString()
            val dayTasks = tasksByDate[dayIso].orEmpty()
            val isDayToday = day == today
            val isSelected = day == selectedDate

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onDateSelected(day) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) BrandPurpleLight else CardSurface
                ),
                border = BorderStroke(
                    0.5.dp,
                    if (isSelected) BrandPurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${day.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault())}, ${day.dayOfMonth} ${day.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDayToday) BrandPurpleAccent else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dayTasks.size} ${if (dayTasks.size == 1) "task" else "tasks"}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    if (dayTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        dayTasks.take(3).forEach { task ->
                            Text(
                                text = "• ${task.title}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (dayTasks.size > 3) {
                            Text(
                                text = "+ ${dayTasks.size - 3} more",
                                fontSize = 11.sp,
                                color = BrandPurpleAccent
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

/**
 * Month Grid Calendar View
 */
@Composable
private fun CalendarMonthGridView(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<String, List<StudyTask>>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)

    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Weekday headers: M, T, W, T, F, S, S
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysHeader = listOf("M", "T", "W", "T", "F", "S", "S")
                    daysHeader.forEach { d ->
                        Text(
                            text = d,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.width(38.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                var dayCounter = 1
                val totalCells = ((daysInMonth + firstDayOfWeek - 1 + 6) / 7) * 7

                for (row in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 1..7) {
                            val cellIndex = row * 7 + col
                            if (cellIndex < firstDayOfWeek || dayCounter > daysInMonth) {
                                Box(modifier = Modifier.size(38.dp))
                            } else {
                                val cellDate = yearMonth.atDay(dayCounter)
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == today
                                val hasTasks = tasksByDate[cellDate.toString()]?.isNotEmpty() == true

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> BrandPurple
                                                isToday -> BrandPurple.copy(alpha = 0.12f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(cellDate) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayCounter.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> BrandPurple
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (hasTasks) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .background(
                                                        if (isSelected) Color.White else BrandPurpleAccent,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

/**
 * 🔒 "Make a little plan" Modal (Add Task & Reminder)
 * STRICTLY NO BOTTOMSHEET!
 * Uses an in-screen animated overlay with dim scrim, drag handle, clean rounded container,
 * smooth native DatePicker & TimePicker integration, and full keyboard IME padding.
 */
@Composable
private fun CalendarMakePlanOverlay(
    visible: Boolean,
    initialDate: LocalDate,
    displayName: String,
    onDismiss: () -> Unit,
    onTaskCreated: (StudyTask) -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    BackHandler(enabled = visible) { onDismiss() }

    var taskTitle by remember { mutableStateOf("") }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf(LocalTime.of(17, 0)) } // 5:00 PM
    var durationMinutes by remember { mutableIntStateOf(60) } // 6:00 PM
    var selectedRepeat by remember { mutableStateOf(RepeatSchedule.NONE) }
    var reminderOptionIndex by remember { mutableIntStateOf(1) } // 0: At start, 1: 10 min before, 2: 30 min before, 3: Custom
    var showRepeatDropdown by remember { mutableStateOf(false) }

    val endTime = remember(startTime, durationMinutes) {
        startTime.plusMinutes(durationMinutes.toLong())
    }

    val timeZoneShort = remember {
        ZoneId.systemDefault().getDisplayName(JTextStyle.SHORT, Locale.getDefault())
    }

    fun launchDatePicker() {
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

    fun launchTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startTime = LocalTime.of(hourOfDay, minute)
            },
            startTime.hour,
            startTime.minute,
            false // 12-hour AM/PM format
        ).show()
    }

    val reminderMinutesBefore = when (reminderOptionIndex) {
        0 -> 0
        1 -> 10
        2 -> 30
        else -> 60
    }

    val notificationTimeFormatted = remember(startTime, reminderMinutesBefore) {
        val notifyTime = startTime.minusMinutes(reminderMinutesBefore.toLong())
        notifyTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        // Darkened Scrim (tapping outside dismisses)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )

        // Slide-up Card Container
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .clickable(enabled = false) {}, // Prevent click-through
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top drag pill indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header: "Make a little plan" + Close X button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Make a little plan",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "We'll help you remember, $displayName.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. TASK TITLE CARD (Clean title field, without hardcoded tags)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, BrandPurpleAccent)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TASK TITLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurpleAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(BrandPurpleAccent),
                            decorationBox = { innerTextField ->
                                if (taskTitle.isEmpty()) {
                                    Text(
                                        text = "Finish ER diagram assignment",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. DATE ROW
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { launchDatePicker() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "DATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. START -> END TIME ROW
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { launchTimePicker() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "START",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = startTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )

                        Column {
                            Text(
                                text = "END",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = endTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = timeZoneShort,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. REPEAT ROW
                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showRepeatDropdown = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Repeat,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "REPEAT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    val repeatTitle = when (selectedRepeat) {
                                        RepeatSchedule.NONE -> "Does not repeat"
                                        RepeatSchedule.DAILY -> "Every day"
                                        RepeatSchedule.WEEKDAYS -> "Every weekday"
                                        RepeatSchedule.WEEKLY -> "Every week"
                                        else -> selectedRepeat.label
                                    }
                                    Text(
                                        text = repeatTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showRepeatDropdown,
                        onDismissRequest = { showRepeatDropdown = false }
                    ) {
                        listOf(
                            RepeatSchedule.NONE to "Does not repeat",
                            RepeatSchedule.DAILY to "Every day",
                            RepeatSchedule.WEEKDAYS to "Every weekday",
                            RepeatSchedule.WEEKLY to "Every week",
                            RepeatSchedule.MONTHLY to "Every month"
                        ).forEach { (sched, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedRepeat = sched
                                    showRepeatDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. REMIND ME SECTION
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = BrandPurpleAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Remind me",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reminder options row: "At start", "10 min before", "30 min before", "Custom"
                val reminderOptions = listOf("At start", "10 min before", "30 min before", "Custom")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderOptions.forEachIndexed { index, label ->
                        val isOptionSelected = reminderOptionIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isOptionSelected) BrandPurpleLight else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (isOptionSelected) 1.5.dp else 0.7.dp,
                                    color = if (isOptionSelected) BrandPurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { reminderOptionIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOptionSelected) BrandPurpleAccent else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Helper text: "You'll get a notification today at 4:50 PM."
                Text(
                    text = "You'll get a notification today at $notificationTimeFormatted.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 6. SAVE TASK BUTTON
                Button(
                    onClick = {
                        val finalTitle = taskTitle.ifBlank { "Study Session" }
                        val newTask = StudyTask(
                            id = UUID.randomUUID().toString(),
                            title = finalTitle,
                            scheduledDate = selectedDate.toString(),
                            scheduledTime = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            estimatedDurationMin = durationMinutes,
                            reminderMinutesBefore = reminderMinutesBefore,
                            repeatSchedule = selectedRepeat,
                            priority = TaskPriority.MEDIUM,
                            category = TaskCategory.STUDY
                        )
                        onTaskCreated(newTask)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPurpleAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save task",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer note: "$displayName's calendar · Only you can see this"
                Text(
                    text = "$displayName's calendar · Only you can see this",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
