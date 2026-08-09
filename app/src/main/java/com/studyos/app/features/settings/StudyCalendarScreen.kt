package com.studyos.app.features.settings

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.BorderColor
import com.studyos.app.core.theme.CardSurface
import com.studyos.app.core.theme.PrimaryAccentColor
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.core.theme.StreakOrange
import com.studyos.app.core.theme.SuccessGreen
import com.studyos.app.core.theme.TextPrimary
import com.studyos.app.core.theme.TextSecondary
import com.studyos.app.data.repository.UserSettingsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

data class SemesterBreak(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyCalendarScreen(
    onBackClick: () -> Unit = {},
    isOnboarding: Boolean = false,
    onCalendarCompleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsState by UserSettingsRepository.userSettingsState.collectAsState()
    val authUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val userRepository = remember { com.studyos.app.data.repository.UserRepositoryImpl(com.google.firebase.firestore.FirebaseFirestore.getInstance()) }
    val coroutineScope = rememberCoroutineScope()

    val daysOfWeek = remember {
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    }

    // Local state initializations
    var preferredReminderTime by remember(settingsState) {
        mutableStateOf(settingsState.morningReminderTime)
    }
    var reminderEnabled by remember(settingsState) {
        mutableStateOf(settingsState.morningReminderEnabled)
    }

    var dailyGoalMinutes by remember(settingsState) {
        mutableStateOf(settingsState.dailyFocusGoalMinutes)
    }
    var isCustomGoal by remember(settingsState) {
        mutableStateOf(!listOf(15, 25, 45, 60, 90).contains(settingsState.dailyFocusGoalMinutes))
    }
    var customGoalText by remember(settingsState) {
        mutableStateOf(if (isCustomGoal) settingsState.dailyFocusGoalMinutes.toString() else "45")
    }

    val selectedDays = remember(settingsState) {
        mutableStateListOf<String>().apply {
            addAll(settingsState.weeklyStudySchedule)
        }
    }

    var semesterStartDate by remember(settingsState) {
        mutableStateOf(settingsState.semesterStartDate)
    }
    var semesterEndDate by remember(settingsState) {
        mutableStateOf(settingsState.semesterEndDate)
    }

    var vacationActive by remember(settingsState) {
        mutableStateOf(settingsState.vacationModeActive)
    }
    var vacationStart by remember(settingsState) {
        mutableStateOf(settingsState.vacationStartDate)
    }
    var vacationEnd by remember(settingsState) {
        mutableStateOf(settingsState.vacationEndDate)
    }
    var vacationReason by remember(settingsState) {
        mutableStateOf(settingsState.vacationReason)
    }

    // Semester breaks list
    val semesterBreaks = remember(settingsState) {
        mutableStateListOf<SemesterBreak>().apply {
            try {
                val jsonArray = JSONArray(settingsState.semesterBreaksJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    add(
                        SemesterBreak(
                            id = obj.optString("id", "break-$i"),
                            name = obj.optString("name", ""),
                            startDate = obj.optString("startDate", ""),
                            endDate = obj.optString("endDate", "")
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    var newBreakName by remember { mutableStateOf("") }
    var newBreakStart by remember { mutableStateOf("") }
    var newBreakEnd by remember { mutableStateOf("") }

    // Native Date Picker Dialog Helper
    val showDatePicker = { initialDateStr: String, onDateSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        if (initialDateStr.isNotBlank()) {
            val parts = initialDateStr.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val d = parts[2].toIntOrNull()
                if (y != null && m != null && d != null) {
                    cal.set(y, m - 1, d)
                }
            }
        }
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    val saveLocalConfig = {
        val finalGoal = if (isCustomGoal) {
            customGoalText.toIntOrNull() ?: 25
        } else {
            dailyGoalMinutes
        }

        val breaksArray = JSONArray()
        semesterBreaks.forEach { brk ->
            val obj = JSONObject()
            obj.put("id", brk.id)
            obj.put("name", brk.name)
            obj.put("startDate", brk.startDate)
            obj.put("endDate", brk.endDate)
            breaksArray.put(obj)
        }

        UserSettingsRepository.updateStudyCalendarConfig(
            dailyFocusGoalMinutes = finalGoal,
            weeklyStudySchedule = selectedDays.toList(),
            semesterStartDate = semesterStartDate,
            semesterEndDate = semesterEndDate,
            vacationModeActive = vacationActive,
            vacationStartDate = vacationStart,
            vacationEndDate = vacationEnd,
            vacationReason = vacationReason,
            semesterBreaksJson = breaksArray.toString()
        )
        UserSettingsRepository.updateMorningReminderTime(preferredReminderTime)
        UserSettingsRepository.updateMorningReminderEnabled(reminderEnabled)
    }

    val handleSave: () -> Unit = {
        saveLocalConfig()
        if (reminderEnabled && preferredReminderTime.isNotBlank()) {
            com.studyos.app.notifications.NotificationWorkScheduler.updateDailyReminderWork(
                context = context,
                preferredReminderTime = preferredReminderTime,
                notificationsEnabled = true
            )
        } else {
            com.studyos.app.notifications.NotificationWorkScheduler.cancelDailyReminderWork(context)
        }
        if (authUser != null) {
            coroutineScope.launch {
                userRepository.saveCalendarPreferences(
                    uid = authUser.uid,
                    studyDaysPerWeek = selectedDays.size,
                    preferredReminderTime = if (reminderEnabled) preferredReminderTime else "",
                    configured = true
                )
            }
        }
        Toast.makeText(context, "Study Calendar configuration saved!", Toast.LENGTH_SHORT).show()
        onBackClick()
    }

    val handleSaveOnboarding: () -> Unit = {
        saveLocalConfig()
        if (reminderEnabled && preferredReminderTime.isNotBlank()) {
            com.studyos.app.notifications.NotificationWorkScheduler.updateDailyReminderWork(
                context = context,
                preferredReminderTime = preferredReminderTime,
                notificationsEnabled = true
            )
        } else {
            com.studyos.app.notifications.NotificationWorkScheduler.cancelDailyReminderWork(context)
        }
        if (authUser != null) {
            coroutineScope.launch {
                userRepository.saveCalendarPreferences(
                    uid = authUser.uid,
                    studyDaysPerWeek = selectedDays.size,
                    preferredReminderTime = if (reminderEnabled) preferredReminderTime else "",
                    configured = true
                )
                userRepository.setCalendarConfigured(authUser.uid, true)
                Toast.makeText(context, "Study Calendar configured!", Toast.LENGTH_SHORT).show()
                onCalendarCompleted()
            }
        } else {
            Toast.makeText(context, "Study Calendar configured!", Toast.LENGTH_SHORT).show()
            onCalendarCompleted()
        }
    }

    val handleSkipOnboarding: () -> Unit = {
        com.studyos.app.notifications.NotificationWorkScheduler.cancelDailyReminderWork(context)
        if (authUser != null) {
            coroutineScope.launch {
                userRepository.saveCalendarPreferences(
                    uid = authUser.uid,
                    studyDaysPerWeek = selectedDays.size,
                    preferredReminderTime = "",
                    configured = false
                )
                userRepository.setCalendarConfigured(authUser.uid, false)
                onCalendarCompleted()
            }
        } else {
            onCalendarCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isOnboarding) "Set up your study calendar" else "Study Calendar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isOnboarding) "Configure semester dates, daily focus goal & reminder schedule" else "Semester dates, schedules & vacation mode",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    if (!isOnboarding) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isOnboarding) {
                        OutlinedButton(
                            onClick = handleSkipOnboarding,
                            modifier = Modifier.weight(0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Skip for now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = handleSaveOnboarding,
                            modifier = Modifier.weight(0.6f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Continue", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier.weight(0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = handleSave,
                            modifier = Modifier.weight(0.6f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. DAILY FOCUS GOAL CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DAILY FOCUS GOAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreen,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Presets Grid / FlowRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 25, 45, 60, 90).forEach { mins ->
                                val isSelected = !isCustomGoal && dailyGoalMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        isCustomGoal = false
                                        dailyGoalMinutes = mins
                                    },
                                    label = { Text("$mins min", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SuccessGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = SuccessGreen,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = BorderColor,
                                        selectedBorderColor = SuccessGreen
                                    )
                                )
                            }

                            FilterChip(
                                selected = isCustomGoal,
                                onClick = { isCustomGoal = true },
                                label = { Text("Custom", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SuccessGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = SuccessGreen,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isCustomGoal,
                                    borderColor = BorderColor,
                                    selectedBorderColor = SuccessGreen
                                )
                            )
                        }

                        if (isCustomGoal) {
                            OutlinedTextField(
                                value = customGoalText,
                                onValueChange = { customGoalText = it },
                                label = { Text("Custom Minutes / Day", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SuccessGreen,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Text(
                            text = "Daily Focus Goal represents the minimum focus time required to increment and maintain your Academic Study Streak.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 2. WEEKLY STUDY SCHEDULE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = null,
                                tint = PrimaryAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WEEKLY STUDY SCHEDULE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryAccentColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "Select days counted towards your focus streak. Days left unselected are treated as free rest days and will never break your streak!",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        // 7 Days Multi-Select Grid
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            daysOfWeek.chunked(2).forEach { rowDays ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowDays.forEach { day ->
                                        val isSelected = selectedDays.contains(day)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) PrimaryAccentColor.copy(alpha = 0.12f)
                                                    else CardSurface
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) PrimaryAccentColor.copy(alpha = 0.6f) else BorderColor,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (isSelected) {
                                                        if (selectedDays.size > 1) selectedDays.remove(day)
                                                    } else {
                                                        selectedDays.add(day)
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = day,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) PrimaryAccentColor else TextSecondary
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSelected) PrimaryAccentColor else Color.Transparent,
                                                    border = if (!isSelected) BorderStroke(1.dp, BorderColor) else null,
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. ACADEMIC STUDY SHIELDS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrimaryAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACADEMIC STUDY SHIELDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryAccentColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Shields Remaining:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(3) { idx ->
                                    val hasShield = settingsState.studyShields > idx
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (hasShield) PrimaryAccentColor else BorderColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Academic Study Shields protect your streak when missing a scheduled study day. You receive 3 shields per semester, which reset automatically each new semester!",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 4. SEMESTER CALENDAR DATES CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SEMESTER CALENDAR DATES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start Date Picker Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSurface)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        showDatePicker(semesterStartDate) { selected ->
                                            semesterStartDate = selected
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("SEMESTER START", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = PurpleAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = semesterStartDate.ifBlank { "Select Date" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (semesterStartDate.isNotBlank()) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }

                            // End Date Picker Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSurface)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        showDatePicker(semesterEndDate) { selected ->
                                            semesterEndDate = selected
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("SEMESTER END", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = PurpleAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = semesterEndDate.ifBlank { "Select Date" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (semesterEndDate.isNotBlank()) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Academic study streaks do not carry over across semester boundary dates. Focus stats aggregate per semester.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 5. VACATION MODE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FlightTakeoff,
                                    contentDescription = null,
                                    tint = StreakOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VACATION MODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StreakOrange,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Switch(
                                checked = vacationActive,
                                onCheckedChange = { vacationActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StreakOrange
                                )
                            )
                        }

                        Text(
                            text = "Planning to go off-grid? Enable Vacation Mode to pause your Academic Study Streak safely. It will not break or advance until you return!",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        AnimatedVisibility(
                            visible = vacationActive,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Vacation Start Date
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardSurface)
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                            .clickable {
                                                showDatePicker(vacationStart) { selected ->
                                                    vacationStart = selected
                                                }
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text("VACATION START", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Event,
                                                    contentDescription = null,
                                                    tint = StreakOrange,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = vacationStart.ifBlank { "Select Start" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (vacationStart.isNotBlank()) TextPrimary else TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Vacation End Date
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardSurface)
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                            .clickable {
                                                showDatePicker(vacationEnd) { selected ->
                                                    vacationEnd = selected
                                                }
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text("VACATION END", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Event,
                                                    contentDescription = null,
                                                    tint = StreakOrange,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = vacationEnd.ifBlank { "Select End" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (vacationEnd.isNotBlank()) TextPrimary else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = vacationReason,
                                    onValueChange = { vacationReason = it },
                                    label = { Text("Reason for vacation", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. Family Trip, Trekking", fontSize = 11.sp, color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StreakOrange,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StreakOrange.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, StreakOrange.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = StreakOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Max vacation duration is strictly 30 days. You will be protected from streak breaks during these dates.",
                                            fontSize = 10.sp,
                                            color = TextPrimary,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. CONFIGURE SEMESTER BREAKS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONFIGURE SEMESTER BREAKS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (semesterBreaks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                semesterBreaks.forEach { brk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardSurface)
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = brk.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${brk.startDate} to ${brk.endDate}",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        IconButton(
                                            onClick = { semesterBreaks.remove(brk) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No scheduled breaks configured for this semester.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Add break interface
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newBreakName,
                                onValueChange = { newBreakName = it },
                                label = { Text("Break Name (e.g. Winter Break)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleAccent,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Break Start Date Picker
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .clickable {
                                            showDatePicker(newBreakStart) { selected ->
                                                newBreakStart = selected
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("BREAK START", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = newBreakStart.ifBlank { "Select Start" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (newBreakStart.isNotBlank()) TextPrimary else TextSecondary
                                            )
                                        }
                                    }
                                }

                                // Break End Date Picker
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .clickable {
                                            showDatePicker(newBreakEnd) { selected ->
                                                newBreakEnd = selected
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("BREAK END", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = newBreakEnd.ifBlank { "Select End" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (newBreakEnd.isNotBlank()) TextPrimary else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (newBreakName.isNotBlank() && newBreakStart.isNotBlank() && newBreakEnd.isNotBlank()) {
                                        semesterBreaks.add(
                                            SemesterBreak(
                                                id = "break-${System.currentTimeMillis()}",
                                                name = newBreakName.trim(),
                                                startDate = newBreakStart.trim(),
                                                endDate = newBreakEnd.trim()
                                            )
                                        )
                                        newBreakName = ""
                                        newBreakStart = ""
                                        newBreakEnd = ""
                                    } else {
                                        Toast.makeText(context, "Fill in break name and dates", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Break Segment",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
