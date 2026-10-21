package com.kletaq.app.features.friends.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.data.model.ClassmateActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ClassmateActivityFeed(
    activities: List<ClassmateActivity>,
    onCheerClick: (ClassmateActivity) -> Unit,
    onStudentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) Color(0xFF1E1E24) else Color(0xFFF8F9FD)
    val cardBorder = if (isDark) Color(0xFF2E2E38) else Color(0xFFE5E7EB)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. Feed Header with Live Pulse ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Today's Classmate Activity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f)
            ) {
                Text(
                    text = "LIVE TODAY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // --- 2. Feed Body ---
        if (activities.isEmpty()) {
            EmptyActivityFeedState()
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(activities, key = { it.id }) { activity ->
                    ClassmateActivityCard(
                        activity = activity,
                        onCheer = { onCheerClick(activity) },
                        onClick = { onStudentClick(activity.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassmateActivityCard(
    activity: ClassmateActivity,
    onCheer: () -> Unit,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val itemBg = if (isDark) Color(0xFF262630) else Color.White
    val borderColor = if (isDark) Color(0xFF383846) else Color(0xFFE2E8F0)
    val haptic = LocalHapticFeedback.current

    var cheeredLocally by remember(activity.id) {
        mutableStateOf(activity.isCheeredByMe)
    }
    var cheerCountLocally by remember(activity.id, activity.cheerCount) {
        mutableStateOf(activity.cheerCount)
    }

    val iconData = when (activity.type.lowercase()) {
        "focus" -> Icons.Default.Timer to Color(0xFFF59E0B)
        "quiz" -> Icons.Default.ElectricBolt to Color(0xFF8B5CF6)
        "streak" -> Icons.Default.LocalFireDepartment to Color(0xFFEF4444)
        else -> Icons.Default.School to Color(0xFF3B82F6)
    }

    val relativeTime = remember(activity.timestamp) {
        formatRelativeTime(activity.timestamp)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = itemBg,
        modifier = Modifier
            .width(260.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Student info + relative time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconData.second.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = activity.userName.take(1).uppercase(Locale.US),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = iconData.second
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activity.userName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (activity.isCurrentUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "YOU",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF8B5CF6),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = relativeTime,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Study session title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = iconData.first,
                    contentDescription = null,
                    tint = iconData.second,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activity.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            // Footer: XP/Duration chip & High-Five cheer button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // XP or duration badge
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (activity.xpEarned > 0L) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "+${activity.xpEarned} XP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (activity.durationMinutes > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${activity.durationMinutes}m",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Interactive Cheer / High-Five Button
                if (!activity.isCurrentUser) {
                    Surface(
                        onClick = {
                            if (!cheeredLocally) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                cheeredLocally = true
                                cheerCountLocally++
                                onCheer()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (cheeredLocally) {
                            Color(0xFF8B5CF6).copy(alpha = 0.18f)
                        } else {
                            if (isDark) Color(0xFF333342) else Color(0xFFEEF2F6)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (cheeredLocally) "🙌" else "👏",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (cheeredLocally) "Cheered" else "Cheer",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cheeredLocally) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface
                            )
                            if (cheerCountLocally > 0) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$cheerCountLocally",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (cheeredLocally) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyActivityFeedState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "🌱 Be the first classmate to study today!",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Complete a 15m focus sprint or lesson to light up your class feed.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Today"
    val diffMillis = System.currentTimeMillis() - timestamp
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)

    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffHours < 24 -> "${diffHours}h ago"
        else -> SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
    }
}
