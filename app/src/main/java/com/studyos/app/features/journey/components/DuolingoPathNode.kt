package com.studyos.app.features.journey.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color definitions (Preserved exact colors)
val SoftEmerald = Color(0xFF34D399)      // Soft emerald fill for completed node
val DarkEmeraldBorder = Color(0xFF059669) // Darker emerald 3D base pedestal
val SolidIndigo = Color(0xFF6366F1)       // Solid indigo for active node
val DeepIndigoSocket = Color(0xFF4338CA)  // Deep indigo 3D base pedestal

// Top-level layout parameters
private val OuterSocketSize = 88.dp
private val InnerNodeRadiusPx = 36.dp
private val SocketStrokeWidthPx = 4.dp
private val PedestalOffsetPx = 3.dp

@Composable
fun DuolingoPathNode(
    lesson: LessonNode,
    currentOffset: Dp,
    onNodeClick: (LessonNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isClickable = lesson.status != LessonStatus.LOCKED
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val topFaceTranslateY = if (isPressed && isClickable) 3.dp else 0.dp
    val topFaceScale = if (isPressed && isClickable) 0.97f else 1f

    // ── Theme & Status colors ────────────────────────────────────────────────
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = backgroundColor.red < 0.5f

    // 1. Layer 1: Outer Fixed Socket Ring Color
    val outerSocketRingColor = remember(lesson.status, isDark) {
        when (lesson.status) {
            LessonStatus.COMPLETED -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            LessonStatus.CURRENT -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            LessonStatus.AVAILABLE -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            LessonStatus.LOCKED -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        }
    }

    // 2. Layer 2: 3D Base Pedestal Color
    val basePedestalColor = remember(lesson.status, isDark) {
        when (lesson.status) {
            LessonStatus.COMPLETED -> DarkEmeraldBorder
            LessonStatus.CURRENT -> DeepIndigoSocket
            LessonStatus.AVAILABLE -> SolidIndigo.copy(alpha = 0.40f)
            LessonStatus.LOCKED -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
        }
    }

    // 3. Layer 3: Inner Top Face Solid Color
    val topFaceColor = remember(lesson.status, isDark) {
        when (lesson.status) {
            LessonStatus.COMPLETED -> SoftEmerald
            LessonStatus.CURRENT -> SolidIndigo
            LessonStatus.AVAILABLE -> if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
            LessonStatus.LOCKED -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        }
    }

    // 4. Icon Content Color
    val iconContentColor = remember(lesson.status) {
        when (lesson.status) {
            LessonStatus.COMPLETED -> Color.White
            LessonStatus.CURRENT -> Color.White
            LessonStatus.AVAILABLE -> SolidIndigo
            LessonStatus.LOCKED -> Color(0xFF94A3B8)
        }
    }

    val labelColor = remember(lesson.status) {
        when (lesson.status) {
            LessonStatus.CURRENT -> SolidIndigo
            LessonStatus.COMPLETED -> DarkEmeraldBorder
            else -> Color(0xFF64748B)
        }
    }

    val iconSize: Dp = remember(lesson.status) {
        when (lesson.status) {
            LessonStatus.COMPLETED -> 44.dp
            LessonStatus.CURRENT -> 42.dp
            LessonStatus.AVAILABLE -> 36.dp
            LessonStatus.LOCKED -> 30.dp
        }
    }

    val nodeIcon = remember(lesson.status, lesson.lessonNumber) {
        when (lesson.status) {
            LessonStatus.COMPLETED -> Icons.Default.Check
            LessonStatus.CURRENT -> Icons.Default.Star
            LessonStatus.LOCKED -> Icons.Default.Lock
            LessonStatus.AVAILABLE -> when (lesson.lessonNumber % 3) {
                0 -> Icons.Default.Headphones
                1 -> Icons.Default.Star
                else -> Icons.Default.FitnessCenter
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = currentOffset),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Floating "START" Speech Bubble Tooltip for ACTIVE Node ───────
                if (lesson.status == LessonStatus.CURRENT) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = (-2).dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            contentColor = SolidIndigo,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "START",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // ── COMBINED SINGLE DRAW LAYER (Outer Ring + 3D Pedestal + Top Face)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(OuterSocketSize)
                        .clickable(
                            enabled = isClickable,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onNodeClick(lesson) }
                        )
                        .drawWithCache {
                            val centerPt = Offset(size.width / 2f, size.height / 2f)
                            val outerRingRadius = (size.width - SocketStrokeWidthPx.toPx()) / 2f
                            val innerRadius = InnerNodeRadiusPx.toPx()
                            val pedestalY = centerPt.y + PedestalOffsetPx.toPx()
                            val topFaceY = centerPt.y + topFaceTranslateY.toPx()
                            val topFaceScaledRadius = innerRadius * topFaceScale

                            onDrawBehind {
                                // 1. Outer Ring Stroke
                                drawCircle(
                                    color = outerSocketRingColor,
                                    radius = outerRingRadius,
                                    center = centerPt,
                                    style = Stroke(width = SocketStrokeWidthPx.toPx())
                                )

                                // 2. 3D Pedestal Base
                                drawCircle(
                                    color = basePedestalColor,
                                    radius = innerRadius,
                                    center = Offset(centerPt.x, pedestalY)
                                )

                                // 3. Inner Top Face Circle
                                drawCircle(
                                    color = topFaceColor,
                                    radius = topFaceScaledRadius,
                                    center = Offset(centerPt.x, topFaceY)
                                )
                            }
                        }
                ) {
                    Icon(
                        imageVector = nodeIcon,
                        contentDescription = lesson.title,
                        tint = iconContentColor,
                        modifier = Modifier
                            .size(iconSize)
                            .offset(y = topFaceTranslateY)
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Topic Title Label
                Text(
                    text = lesson.shortTitle.ifBlank { lesson.title },
                    fontSize = 14.sp,
                    fontWeight = if (lesson.status == LessonStatus.CURRENT) FontWeight.ExtraBold else FontWeight.Bold,
                    color = labelColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 130.dp)
                )
            }
        }
    }
}
