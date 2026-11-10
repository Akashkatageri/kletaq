package com.kletaq.app.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.FrozenIceBg
import com.kletaq.app.core.theme.FrozenIceColor
import com.kletaq.app.core.theme.StreakOrangeAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.isAppInDarkTheme
import java.time.LocalDate

enum class StreakDayState {
    COMPLETED,  // Lit orange flame
    FROZEN,     // Missed day protected by shield (ice blue ❄️)
    PENDING,    // Today awaiting study (dashed ring)
    UPCOMING    // Future day of the week
}

@Composable
fun WeeklyStreakCard(
    streakDays: Int = 0,
    isStreakActiveToday: Boolean = false,
    shieldsRemaining: Int = 3,
    dailyXp: Map<String, Long> = emptyMap(),
    dailyStudyMinutes: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val today = remember { LocalDate.now() }
    val currentDayOfWeek = remember(today) { today.dayOfWeek.value } // 1 (Mon) .. 7 (Sun)
    val startOfWeek = remember(today, currentDayOfWeek) {
        today.minusDays((currentDayOfWeek - 1).toLong())
    }

    val cardBg = if (isDark) Color(0xFF161922) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF282D3D) else Color(0xFFE5E7EB)

    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

    // Determine states for 7 days of the current week (1..7)
    val dayStates = remember(currentDayOfWeek, isStreakActiveToday, streakDays, shieldsRemaining, dailyXp, dailyStudyMinutes) {
        (1..7).map { dayNum ->
            val targetDate = startOfWeek.plusDays((dayNum - 1).toLong())
            val dateIso = targetDate.toString()
            val hadStudyActivity = (dailyXp[dateIso] ?: 0L) > 0L || (dailyStudyMinutes[dateIso] ?: 0) > 0

            when {
                targetDate.isAfter(today) -> {
                    StreakDayState.UPCOMING
                }
                targetDate.isEqual(today) -> {
                    if (isStreakActiveToday || hadStudyActivity) StreakDayState.COMPLETED else StreakDayState.PENDING
                }
                else -> {
                    // Past day in the current week
                    if (hadStudyActivity) {
                        StreakDayState.COMPLETED // Fire ONLY for days studied
                    } else {
                        val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(targetDate, today).toInt()
                        if (streakDays >= daysAgo) {
                            StreakDayState.FROZEN // Protected by streak freeze
                        } else {
                            StreakDayState.UPCOMING
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (streakDays > 0) "$streakDays day streak" else "Start your streak",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isStreakActiveToday) "Streak extended for today! 🔥" else "Finish one focus run today to keep it alive",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isStreakActiveToday) Color(0xFFECFDF5) else Color(0xFFFFF0E6),
                    border = BorderStroke(
                        0.75.dp,
                        if (isStreakActiveToday) Color(0xFFA7F3D0) else Color(0xFFFFD8BF)
                    )
                ) {
                    Text(
                        text = if (isStreakActiveToday) "Active" else "Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStreakActiveToday) Color(0xFF059669) else StreakOrangeAccent,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // 7 Days of the Week Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayLetters.forEachIndexed { index, letter ->
                    val state = dayStates.getOrElse(index) { StreakDayState.UPCOMING }
                    val isToday = (index + 1) == currentDayOfWeek

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Day Indicator Circle
                        when (state) {
                            StreakDayState.COMPLETED -> {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(StreakOrangeAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🔥", fontSize = 13.sp)
                                }
                            }
                            StreakDayState.FROZEN -> {
                                // Ice / Frozen Day
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF0F2D4A) else FrozenIceBg)
                                        .border(1.5.dp, FrozenIceColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "❄️", fontSize = 12.sp)
                                }
                            }
                            StreakDayState.PENDING -> {
                                // Dashed Orange Circle
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF26180D) else Color(0xFFFFF7ED))
                                        .border(1.5.dp, StreakOrangeAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StreakOrangeAccent
                                    )
                                }
                            }
                            StreakDayState.UPCOMING -> {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF222736) else Color(0xFFF1F3F7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Day Letter
                        Text(
                            text = letter,
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) StreakOrangeAccent else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

