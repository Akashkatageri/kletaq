package com.kletaq.app.features.tasks

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.repository.HabitTrackerRepository
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.domain.export.HabitExportManager
import com.kletaq.app.domain.model.HabitTaskItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyHabitTrackerScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val isArchived = HabitTrackerRepository.isMonthArchived(selectedYearMonth)

    val habitItemsFlow = remember(selectedYearMonth) {
        HabitTrackerRepository.getHabitItemsForMonth(selectedYearMonth)
    }
    val habitItems by habitItemsFlow.collectAsState(initial = emptyList())

    val creationTimeMs = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.metadata?.creationTimestamp ?: System.currentTimeMillis()
    }
    val minYearMonth = remember(creationTimeMs) {
        val date = java.time.Instant.ofEpochMilli(creationTimeMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val ym = YearMonth.of(date.year, date.monthValue)
        if (ym.isAfter(YearMonth.now())) YearMonth.now() else ym
    }

    var showExportSheet by remember { mutableStateOf(false) }

    val totalDays = selectedYearMonth.lengthOfMonth()
    val monthName = selectedYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val monthYearText = "${monthName.uppercase()} ${selectedYearMonth.year}"

    val activeItems = habitItems.filterNot { it.isDeleted }
    val deletedItems = habitItems.filter { it.isDeleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Monthly Habit Tracker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = PurpleAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month Header Selector Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedYearMonth = selectedYearMonth.minusMonths(1) },
                        enabled = selectedYearMonth.isAfter(minYearMonth)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Prev Month",
                            tint = if (selectedYearMonth.isAfter(minYearMonth)) TextPrimary else Color.LightGray
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthYearText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        if (isArchived) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = "Archived", modifier = Modifier.size(10.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Archived Month (Read-only)",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Current Month",
                                fontSize = 10.sp,
                                color = PurpleAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { selectedYearMonth = selectedYearMonth.plusMonths(1) },
                        enabled = selectedYearMonth.isBefore(YearMonth.now())
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = if (selectedYearMonth.isBefore(YearMonth.now())) TextPrimary else Color.LightGray
                        )
                    }
                }
            }

            // Summary Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalCheckmarks = activeItems.sumOf { it.completedDaysCount }
                val avgPercentage = if (activeItems.isNotEmpty()) activeItems.map { it.completionPercentage }.average().toInt() else 0
                val topStreak = activeItems.maxOfOrNull { it.currentStreak } ?: 0

                HabitStatChip(modifier = Modifier.weight(1f), label = "Habits", value = "${activeItems.size}")
                HabitStatChip(modifier = Modifier.weight(1.1f), label = "Checkmarks", value = "$totalCheckmarks")
                HabitStatChip(modifier = Modifier.weight(1.1f), label = "Avg Progress", value = "$avgPercentage%")
                HabitStatChip(modifier = Modifier.weight(1.1f), label = "Top Streak", value = "🔥 ${topStreak}d")
            }

            if (habitItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks found for $monthName ${selectedYearMonth.year}.\nTasks created in the app will automatically appear here!",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                }
            } else {
                // Synchronized Horizontal Scroll Grid
                val horizontalScrollState = rememberScrollState()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    // Header Row (Task Name + Days 1..31)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TASK / HABIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                modifier = Modifier
                                    .width(150.dp)
                                    .padding(start = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .horizontalScroll(horizontalScrollState)
                                    .padding(end = 8.dp)
                            ) {
                                for (day in 1..totalDays) {
                                    val isToday = !isArchived &&
                                            selectedYearMonth == YearMonth.now() &&
                                            day == LocalDate.now().dayOfMonth

                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isToday) PurpleAccent else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$day",
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isToday) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Task Rows
                    items(activeItems, key = { it.taskId }) { item ->
                        HabitGridRow(
                            item = item,
                            yearMonth = selectedYearMonth,
                            totalDays = totalDays,
                            isArchived = isArchived,
                            horizontalScrollState = horizontalScrollState,
                            onToggleDay = { day ->
                                val today = LocalDate.now()
                                val yesterday = today.minusDays(1)
                                val cellDate = try { LocalDate.of(selectedYearMonth.year, selectedYearMonth.monthValue, day) } catch (_: Exception) { null }
                                val isEditable = !isArchived && !item.isDeleted && cellDate != null && (cellDate == today || cellDate == yesterday)

                                if (isEditable) {
                                    val dateIso = cellDate!!.toString()
                                    TaskRepository.toggleTaskCompletedForDate(item.taskId, dateIso)
                                }
                            }
                        )
                    }

                    // Deleted Tasks Section
                    if (deletedItems.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "DELETED TASKS (HISTORICAL LOG)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        items(deletedItems, key = { "deleted_${it.taskId}" }) { item ->
                            HabitGridRow(
                                item = item,
                                yearMonth = selectedYearMonth,
                                totalDays = totalDays,
                                isArchived = true, // Read-only for deleted tasks
                                horizontalScrollState = horizontalScrollState,
                                onToggleDay = {}
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Export Bottom Sheet
    if (showExportSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Export Habit Tracker",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Export $monthYearText tracker history formatted with completion rates, streaks, and day grid summaries.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showExportSheet = false
                            HabitExportManager.exportToPdf(context, selectedYearMonth, habitItems)
                        },
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = InkPaperBorder.heavyBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Export as PDF Document", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Styled printable grid matching Kletaq branding", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showExportSheet = false
                            HabitExportManager.exportToCsv(context, selectedYearMonth, habitItems)
                        },
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = InkPaperBorder.heavyBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "CSV", tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Export as CSV Spreadsheet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Compatible with Excel, Google Sheets & Notion", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun HabitStatChip(modifier: Modifier = Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        }
    }
}

@Composable
fun HabitGridRow(
    item: HabitTaskItem,
    yearMonth: YearMonth,
    totalDays: Int,
    isArchived: Boolean,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onToggleDay: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(6.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Frozen Header Column
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = if (item.isDeleted) "~~${item.title}~~" else item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isDeleted) Color.Gray else TextPrimary,
                    textDecoration = if (item.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${item.completedDaysCount}/$totalDays (${item.completionPercentage}%)",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.currentStreak > 0) {
                        Text(
                            text = "🔥 ${item.currentStreak}d",
                            fontSize = 10.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Days Matrix Columns
            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScrollState)
                    .padding(end = 8.dp)
            ) {
                val today = LocalDate.now()
                val yesterday = today.minusDays(1)

                for (day in 1..totalDays) {
                    val cellDate = try { LocalDate.of(yearMonth.year, yearMonth.monthValue, day) } catch (_: Exception) { null }
                    val dateIso = cellDate?.toString() ?: String.format("%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, day)
                    val isChecked = item.completedDates.contains(dateIso)
                    val isTodayCell = !isArchived && cellDate == today
                    val isEditable = !isArchived && !item.isDeleted && cellDate != null && (cellDate == today || cellDate == yesterday)
                    val isDisabledCell = !isEditable

                    HabitGridCell(
                        day = day,
                        isChecked = isChecked,
                        isDisabled = isDisabledCell,
                        isToday = isTodayCell,
                        onToggle = { onToggleDay(day) }
                    )
                }
            }
        }
    }
}

@Composable
fun HabitGridCell(
    day: Int,
    isChecked: Boolean,
    isDisabled: Boolean,
    isToday: Boolean = false,
    onToggle: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1.1f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isChecked -> Color(0xFF10B981)
            isToday -> Color(0xFFEFF6FF)
            isDisabled -> Color(0xFFF8FAFC)
            else -> Color(0xFFFFFFFF)
        }
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isChecked -> Color(0xFF059669)
            isToday -> Color(0xFF3B82F6)
            isDisabled -> Color(0xFFE2E8F0)
            else -> Color(0xFFCBD5E1)
        }
    )

    Box(
        modifier = Modifier
            .width(36.dp)
            .height(36.dp)
            .padding(3.dp)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(BorderStroke(if (isToday && !isChecked) 1.5.dp else 1.dp, borderColor), RoundedCornerShape(6.dp))
            .clickable(enabled = !isDisabled) { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Text(
                text = "✓",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        } else {
            Box(
                modifier = Modifier
                    .size(if (isToday) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(if (isToday) Color(0xFF3B82F6) else Color(0xFFCBD5E1))
            )
        }
    }
}
