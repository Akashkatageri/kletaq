package com.kletaq.app.features.home.components

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.TealPrimary
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.isAppInDarkTheme

@Composable
fun HeaderSection(
    userName: String = "Learner",
    subtitleText: String = "VTU CSE • Semester 2",
    totalXp: Long = 0L,
    streakDays: Int = 0,
    isStreakActiveToday: Boolean = false,
    shieldsCount: Int = 3,
    activeTasksCount: Int = 0,
    totalTasksCount: Int = 0,
    unreadCount: Int = 0,
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val userInitial = remember(userName) {
        userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "K"
    }

    // Dynamic XP Level Calculation
    val xpPerLevel = 500L
    val levelNumber = (totalXp / xpPerLevel).toInt() + 1
    val currentLevelXp = totalXp % xpPerLevel
    val xpToNextLevel = xpPerLevel - currentLevelXp
    val levelProgress = (currentLevelXp.toFloat() / xpPerLevel.toFloat()).coerceIn(0.05f, 1f)

    val levelTitle = when (levelNumber) {
        1 -> "Novice"
        2 -> "Apprentice"
        3 -> "Scholar"
        4 -> "Master"
        else -> "Grandmaster"
    }

    val cardBg = if (isDark) Color(0xFF161922) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF282D3D) else Color(0xFFE5E7EB)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Profile Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Identity
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clean Teal Squircle Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitial,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = userName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Level $levelNumber · $levelTitle · $subtitleText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Search & Notifications Circular Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF222738) else Color(0xFFF3F4F6),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(17.dp),
                                tint = TextSecondary
                            )
                        }
                    }

                    com.kletaq.app.core.ui.NotificationBadgeIcon(
                        unreadCount = unreadCount,
                        onClick = onNotificationsClick,
                        modifier = Modifier.size(36.dp),
                        iconColor = TextSecondary,
                        containerColor = if (isDark) Color(0xFF222738) else Color(0xFFF3F4F6)
                    )
                }
            }

            // 2. XP & Level Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ $totalXp XP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                    Text(
                        text = "$xpToNextLevel XP to Level ${levelNumber + 1}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // Smooth Rounded Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF22283A) else Color(0xFFE5E7EB))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(levelProgress)
                            .height(9.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFB45309),
                                        Color(0xFFD97706),
                                        Color(0xFFF59E0B)
                                    )
                                )
                            )
                    )
                }
            }

            // 3. Three Stat Capsules in a Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak Capsule
                val isLit = isStreakActiveToday && streakDays > 0
                StatCapsule(
                    modifier = Modifier.weight(1f),
                    iconEmoji = "🔥",
                    valueText = "${streakDays}d",
                    labelText = if (isLit) "Streak active" else "Streak pending",
                    accentColor = Color(0xFFC2410C),
                    containerColor = if (isDark) Color(0xFF2C160B) else Color(0xFFFFF7ED),
                    borderColor = if (isDark) Color(0xFF9A3412) else Color(0xFFFED7AA)
                )

                // Shields Capsule
                StatCapsule(
                    modifier = Modifier.weight(1f),
                    iconEmoji = "🛡️",
                    valueText = "$shieldsCount",
                    labelText = "Shields",
                    accentColor = Color(0xFF1D4ED8),
                    containerColor = if (isDark) Color(0xFF0F1E3A) else Color(0xFFEFF6FF),
                    borderColor = if (isDark) Color(0xFF1E40AF) else Color(0xFFBFDBFE)
                )

                // Tasks Capsule
                val tasksDisplay = if (totalTasksCount > 0) "$activeTasksCount/$totalTasksCount" else "$activeTasksCount"
                StatCapsule(
                    modifier = Modifier.weight(1f),
                    iconEmoji = "📦",
                    valueText = tasksDisplay,
                    labelText = "Tasks",
                    accentColor = Color(0xFFB45309),
                    containerColor = if (isDark) Color(0xFF291E0A) else Color(0xFFFFFBEB),
                    borderColor = if (isDark) Color(0xFF92400E) else Color(0xFFFDE68A),
                    onClick = onTasksClick
                )
            }
        }
    }
}

@Composable
private fun StatCapsule(
    modifier: Modifier = Modifier,
    iconEmoji: String,
    valueText: String,
    labelText: String,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = iconEmoji, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = labelText,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
