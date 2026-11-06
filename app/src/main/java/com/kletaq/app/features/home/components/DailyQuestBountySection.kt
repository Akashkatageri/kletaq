package com.kletaq.app.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.isAppInDarkTheme
import com.kletaq.app.domain.model.StudyTask
import com.kletaq.app.data.repository.TaskRepository

data class DailyBountyItem(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val isCompleted: Boolean,
    val iconEmoji: String
)

@Composable
fun DailyQuestBountySection(
    tasksList: List<StudyTask>,
    streakActiveToday: Boolean,
    todayXp: Long,
    onAddTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    var isExpanded by remember { mutableStateOf(true) }

    // Dynamic daily bounties based on student activity
    val dailyBounties = remember(streakActiveToday, todayXp) {
        listOf(
            DailyBountyItem(
                id = "bounty_study",
                title = "Lab & Core Mastery",
                description = "Complete 1 logic kernel or syllabus topic",
                xpReward = 50,
                isCompleted = streakActiveToday,
                iconEmoji = "🎯"
            ),
            DailyBountyItem(
                id = "bounty_xp",
                title = "Daily XP Surge",
                description = "Earn 50+ XP in study sessions today",
                xpReward = 25,
                isCompleted = todayXp >= 50L || streakActiveToday,
                iconEmoji = "⚡"
            ),
            DailyBountyItem(
                id = "bounty_focus",
                title = "Focus Defense",
                description = "Complete 1 Pomodoro or review 5 cards",
                xpReward = 25,
                isCompleted = streakActiveToday,
                iconEmoji = "⏱️"
            )
        )
    }

    val completedBountiesCount = dailyBounties.count { it.isCompleted }
    val chestProgress = (completedBountiesCount.toFloat() / dailyBounties.size.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF131B2E) else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(
            1.2.dp,
            if (isDark) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color(0xFF6366F1).copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title + Add Quest Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚔️ DAILY BOUNTY BOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6366F1),
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$completedBountiesCount/3 Done",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onAddTaskClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Task",
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "New Quest",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isExpanded = !isExpanded }
                    )
                }
            }

            // Daily Mystery Chest Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (completedBountiesCount == 3) "🎁" else "📦",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (completedBountiesCount == 3) "Daily Loot Chest Unlocked! 🎉" else "Daily Loot Chest (Unlocks at 3)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (completedBountiesCount == 3) Color(0xFF10B981) else Color(0xFF1E293B)
                            )
                            Text(
                                text = "+100 Bonus XP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { chestProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (completedBountiesCount == 3) Color(0xFF10B981) else Color(0xFFF59E0B),
                            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // System Quests
                    dailyBounties.forEach { bounty ->
                        BountyRow(bounty = bounty)
                    }

                    // User Custom Tasks (Side Quests)
                    if (tasksList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SIDE QUESTS (${tasksList.count { !it.isDoneToday }} PENDING)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        tasksList.take(3).forEach { task ->
                            UserTaskRow(task = task)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BountyRow(bounty: DailyBountyItem) {
    val isDark = isAppInDarkTheme

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (bounty.isCompleted) {
            if (isDark) Color(0xFF062E20) else Color(0xFFECFDF5)
        } else {
            if (isDark) Color(0xFF1A2234) else Color.White
        },
        border = BorderStroke(
            1.dp,
            if (bounty.isCompleted) Color(0xFF10B981).copy(alpha = 0.4f)
            else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark Icon
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (bounty.isCompleted) Color(0xFF10B981)
                        else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (bounty.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(text = bounty.iconEmoji, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bounty.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (bounty.isCompleted) {
                        if (isDark) Color(0xFF86EFAC) else Color(0xFF065F46)
                    } else {
                        if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                    },
                    textDecoration = if (bounty.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = bounty.description,
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (bounty.isCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (bounty.isCompleted) "CLAIMED" else "+${bounty.xpReward} XP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (bounty.isCompleted) Color(0xFF10B981) else Color(0xFFD97706),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun UserTaskRow(task: StudyTask) {
    val isDark = isAppInDarkTheme

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (task.isDoneToday) {
            if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
        } else {
            if (isDark) Color(0xFF1E293B) else Color.White
        },
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { TaskRepository.toggleTaskCompleted(task.id) }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (task.isDoneToday) Color(0xFF10B981)
                        else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (task.isDoneToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isDoneToday) Color.Gray else if (isDark) Color.White else Color(0xFF1E293B),
                    textDecoration = if (task.isDoneToday) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!task.subjectName.isNullOrBlank()) {
                    Text(
                        text = task.subjectName,
                        fontSize = 9.sp,
                        color = Color(0xFF6366F1)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF6366F1).copy(alpha = 0.1f)
            ) {
                Text(
                    text = "+15 XP",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
