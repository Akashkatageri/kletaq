package com.kletaq.app.features.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.isAppInDarkTheme

@Composable
fun GamifiedStatsHud(
    streakDays: Int,
    isStreakActiveToday: Boolean,
    shieldsCount: Int,
    totalXp: Long,
    todayXp: Long = 0L,
    activeTasksCount: Int = 0,
    onTasksClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val isLit = isStreakActiveToday && streakDays > 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Pod 1: Flame Streak Shrine ---
        val streakGradient = if (isLit) {
            listOf(Color(0xFFEA580C), Color(0xFFF59E0B))
        } else {
            if (isDark) listOf(Color(0xFF332014), Color(0xFF26190F)) else listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLit) Color.Transparent else if (isDark) Color(0xFF1E1711) else Color(0xFFFFFDF5)
            ),
            border = BorderStroke(
                1.2.dp,
                if (isLit) Color(0xFFF97316).copy(alpha = 0.8f) else Color(0xFFF59E0B).copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isLit) 4.dp else 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isLit) Modifier.background(Brush.verticalGradient(streakGradient))
                        else Modifier
                    )
                    .padding(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isLit) "🔥" else "⏳",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${streakDays}d Streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLit) Color.White else if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                    )
                    Text(
                        text = if (isLit) "Lit Today!" else "Needs Study",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLit) Color.White.copy(alpha = 0.85f) else Color(0xFF9CA3AF)
                    )
                }
            }
        }

        // --- Pod 2: Shield Fortress ---
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF0F7FF)
            ),
            border = BorderStroke(
                1.2.dp,
                if (shieldsCount > 0) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🛡️", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$shieldsCount/3 Shields",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                )

                // Battery charge blocks
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isFilled = i <= shieldsCount
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isFilled) Color(0xFF3B82F6)
                                    else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                )
                        )
                    }
                }
            }
        }

        // --- Pod 3: Daily XP Target Ring ---
        val dailyGoal = 100f
        val effectiveTodayXp = if (todayXp > 0L) todayXp else (totalXp % 100L).coerceAtLeast(20L)
        val xpProgress = (effectiveTodayXp.toFloat() / dailyGoal).coerceIn(0f, 1f)
        val animatedXpProgress by animateFloatAsState(
            targetValue = xpProgress,
            animationSpec = tween(durationMillis = 600),
            label = "daily_xp_ring"
        )

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF19102E) else Color(0xFFFAF5FF)
            ),
            border = BorderStroke(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedXpProgress },
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF8B5CF6),
                        trackColor = if (isDark) Color(0xFF3B2D54) else Color(0xFFE9D5FF),
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "⚡",
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${totalXp} XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
                )
                Text(
                    text = "+${effectiveTodayXp} today",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6)
                )
            }
        }
    }
}
