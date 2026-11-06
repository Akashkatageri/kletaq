package com.kletaq.app.features.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.R
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.isAppInDarkTheme
import com.kletaq.app.data.model.UserStats

@Composable
fun HeroCompanionCard(
    userName: String,
    userStats: UserStats,
    subtitleText: String,
    studyWhyText: String,
    onEditWhyClick: () -> Unit,
    onToggleClassicView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme

    // Select dynamic panda avatar based on streak and hour
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val isStreakLit = userStats.isStreakActiveToday && userStats.studyStreak > 0
    val pandaRes = remember(userStats.studyStreak, isStreakLit, currentHour) {
        when {
            userStats.studyStreak >= 7 -> R.drawable.panda_streak
            isStreakLit -> R.drawable.panda_energetic
            currentHour >= 22 || currentHour < 5 -> R.drawable.panda_sleepy
            else -> R.drawable.panda_welcome
        }
    }

    // Dynamic mascot speech tip
    val mascotSpeech = remember(isStreakLit, userStats.studyStreak, studyWhyText) {
        when {
            studyWhyText.isNotBlank() -> "\"$studyWhyText\""
            isStreakLit -> "🔥 Awesome job keeping your streak alive today!"
            userStats.studyStreak == 0 -> "Let's kick off Day 1 of your study streak today! 🚀"
            else -> "A quick 15-min focus sprint protects your streak today!"
        }
    }

    val level = userStats.currentLevel.coerceAtLeast(1)
    val league = userStats.currentLeague.ifBlank { "Bronze" }
    val levelXp = userStats.currentLevelXp
    val targetLevelXp = userStats.nextLevelXp.coerceAtLeast(1L)
    val progressFraction = (levelXp.toFloat() / targetLevelXp.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "hero_xp_progress"
    )

    val backgroundBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1B4B),
                Color(0xFF18153A),
                Color(0xFF0F172A)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEEF2FF),
                Color(0xFFE0E7FF),
                Color(0xFFF5F3FF)
            )
        )
    }

    val cardBorderColor = if (isDark) Color(0xFF6366F1).copy(alpha = 0.35f) else Color(0xFF818CF8).copy(alpha = 0.45f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top row: Mascot Avatar + Speech Bubble + Switch Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Panda Companion Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF312E81) else Color.White)
                            .border(BorderStroke(1.5.dp, Color(0xFF6366F1)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = pandaRes),
                            contentDescription = "Panda Companion",
                            modifier = Modifier.size(54.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Speech bubble with user inspiration
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.95f))
                            .clickable { onEditWhyClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PANDA COACH",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6366F1),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Edit Reason ✎",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mascotSpeech,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Middle: Level & League Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LVL $level SCHOLAR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "$league League",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Quick Switch Pill to Classic View
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (isDark) Color(0xFF312E81) else Color(0xFFC7D2FE)).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { onToggleClassicView() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch to Classic",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Classic",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }

                // Bottom: Level XP Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NEXT LEVEL PROGRESS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$levelXp / $targetLevelXp XP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF6366F1),
                        trackColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
