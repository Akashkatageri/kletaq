package com.kletaq.app.features.journey.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen

val TrackCyan = Color(0xFF06B6D4)
val TrackViolet = Color(0xFF6366F1)
val RailGlowDark = Color(0xFF818CF8).copy(alpha = 0.35f)

/**
 * A single Metro Station stop along the vertical rail line.
 * ALL nodes are 100% unlocked and freely accessible.
 */
@Composable
fun MetroRoadmapNode(
    lesson: LessonNode,
    stationIndex: Int,
    totalStationsInModule: Int,
    isFirstInModule: Boolean,
    isLastInModule: Boolean,
    onNodeClick: (LessonNode) -> Unit,
    modifier: Modifier = Modifier,
    retentionPercentage: Int? = null,
    isPriority: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }

    // Pulsing halo animation for the CURRENT / Next Stop station
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val isCurrent = lesson.status == LessonStatus.CURRENT
    val isCompleted = lesson.status == LessonStatus.COMPLETED

    // Track line colors
    val trackBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = listOf(
                TrackCyan,
                TrackViolet
            )
        )
    }
    val inactiveTrackColor = if (isDark) Color(0xFF26334D) else Color(0xFFCBD5E1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 1. LEFT RAIL & STATION PIP ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(84.dp)
                .drawBehind {
                    val trackCenterX = size.width * 0.52f
                    val pipCenterY = size.height * 0.5f
                    val trackWidthPx = 4.dp.toPx()

                    // Top rail segment (connecting from previous topic)
                    if (!isFirstInModule) {
                        drawLine(
                            brush = if (isCompleted || isCurrent) trackBrush else Brush.verticalGradient(listOf(inactiveTrackColor, inactiveTrackColor)),
                            start = Offset(trackCenterX, 0f),
                            end = Offset(trackCenterX, pipCenterY),
                            strokeWidth = trackWidthPx,
                            cap = StrokeCap.Round
                        )
                    }

                    // Bottom rail segment (connecting to next topic)
                    if (!isLastInModule) {
                        drawLine(
                            brush = if (isCompleted) trackBrush else Brush.verticalGradient(listOf(inactiveTrackColor, inactiveTrackColor)),
                            start = Offset(trackCenterX, pipCenterY),
                            end = Offset(trackCenterX, size.height),
                            strokeWidth = trackWidthPx,
                            cap = StrokeCap.Round
                        )
                    }

                    // Circuit callout stem leading to the right topic card
                    val stemWidthPx = 2.dp.toPx()
                    val stemColor = if (isCurrent) TrackViolet else if (isCompleted) TrackCyan else inactiveTrackColor
                    drawLine(
                        color = stemColor,
                        start = Offset(trackCenterX, pipCenterY),
                        end = Offset(size.width, pipCenterY),
                        strokeWidth = stemWidthPx,
                        cap = StrokeCap.Round
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Pulse Beacon for CURRENT / Next Stop
            if (isCurrent) {
                Surface(
                    shape = CircleShape,
                    color = TrackViolet.copy(alpha = pulseAlpha),
                    modifier = Modifier
                        .size(32.dp * pulseScale)
                        .offset(x = 1.dp)
                ) {}
            }

            // Station Pip Disc
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 1.dp)
                    .drawBehind {
                        // Dynamic Ebbinghaus Memory Decay Ring around Completed Stations
                        val retention = retentionPercentage ?: if (isCompleted) 100 else null
                        if (isCompleted && retention != null) {
                            val arcColor = when {
                                retention >= 80 -> Color(0xFF10B981) // 🟢 Solid Green (> 80% Retention)
                                retention >= 60 -> Color(0xFFF59E0B) // 🟡 Amber (60% - 80% Retention)
                                else -> Color(0xFFEF4444)            // 🔴 Red (< 60% Memory decaying)
                            }
                            val sweep = 360f * (retention.coerceIn(5, 100) / 100f)
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
            ) {
                when {
                    isCompleted -> {
                        Surface(
                            shape = CircleShape,
                            color = SuccessGreen,
                            contentColor = Color.White,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    isCurrent -> {
                        Surface(
                            shape = CircleShape,
                            color = TrackViolet,
                            border = BorderStroke(2.dp, Color.White),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Next Stop",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        // Available and 100% Unlocked Station Pip
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF0F172A) else Color.White,
                            border = BorderStroke(2.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }
                    }
                }

                // Priority Star Badge (Exam Sprint Mode)
                if (isPriority) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "⭐", fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // ── 2. FROSTED GLASS STATION TOPIC CARD ──────────────────────────────────
        val cardBorderColor = when {
            isCurrent -> TrackViolet.copy(alpha = 0.85f)
            isCompleted -> SuccessGreen.copy(alpha = 0.45f)
            else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        }

        val cardBackgroundColor = when {
            isCurrent -> if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.70f) else Color(0xFFEEF2FF).copy(alpha = 0.90f)
            isCompleted -> if (isDark) Color(0xFF062017).copy(alpha = 0.55f) else Color(0xFFECFDF5).copy(alpha = 0.85f)
            else -> if (isDark) Color(0xFF131B2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.90f)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
            border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, cardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 0.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNodeClick(lesson)
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Topic Title
                Text(
                    text = lesson.shortTitle.ifBlank { lesson.title },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Action / Status Pill
                when {
                    isCurrent -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TrackViolet,
                            contentColor = Color.White
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "UP NEXT · +80 XP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "▶",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    isCompleted -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreen.copy(alpha = 0.15f),
                            contentColor = SuccessGreen,
                            border = BorderStroke(0.8.dp, SuccessGreen.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Mastered ✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    else -> {
                        // Available Action Hint
                        Text(
                            text = "Study →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

