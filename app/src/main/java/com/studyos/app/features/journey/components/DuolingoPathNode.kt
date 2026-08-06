package com.studyos.app.features.journey.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
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

@Composable
fun DuolingoPathNode(
    lesson: LessonNode,
    currentOffset: Dp,
    onNodeClick: (LessonNode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val isClickable = lesson.status != LessonStatus.LOCKED

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
    val basePedestalColor = remember(lesson.status) {
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

    // Node Sizing
    val outerSocketSize: Dp = 88.dp
    val socketRingPadding: Dp = 8.dp
    val innerNodeSize: Dp = 72.dp

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

                // ── LAYER 1: OUTER NEUTRAL SOCKET RING
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(4.dp, outerSocketRingColor),
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(outerSocketSize)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(socketRingPadding)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(innerNodeSize)
                                .then(
                                    if (isClickable) {
                                        Modifier.pointerInput(lesson.id) {
                                            detectTapGestures(
                                                onPress = {
                                                    isPressed = true
                                                    try {
                                                        awaitRelease()
                                                    } finally {
                                                        isPressed = false
                                                    }
                                                },
                                                onTap = { onNodeClick(lesson) }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            // ── LAYER 2: 3D BOTTOM PEDESTAL BASE
                            Surface(
                                shape = CircleShape,
                                color = basePedestalColor,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .size(innerNodeSize)
                                    .offset(y = 3.dp)
                            ) {}

                            Surface(
                                shape = CircleShape,
                                color = topFaceColor,
                                contentColor = iconContentColor,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .size(innerNodeSize)
                                    .scale(topFaceScale)
                                    .offset(y = topFaceTranslateY)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = nodeIcon,
                                        contentDescription = lesson.title,
                                        tint = iconContentColor,
                                        modifier = Modifier.size(iconSize)
                                    )
                                }
                            }
                        }
                    }
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
