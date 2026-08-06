package com.studyos.app.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────────────────────
// GameButton Shape Variants
// ──────────────────────────────────────────────────────────────────────────────

enum class GameButtonShape {
    CIRCLE,
    ROUNDED_RECT,
    PILL
}

// ──────────────────────────────────────────────────────────────────────────────
// GameButton Style Hierarchy
// ──────────────────────────────────────────────────────────────────────────────

enum class GameButtonStyle {
    PRIMARY,
    SECONDARY,
    ICON_ONLY,
    FAB,
    NAV
}

// ──────────────────────────────────────────────────────────────────────────────
// Soft & Satisfying Duolingo Quest-Node Press Animation GameButton
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Reusable Duolingo Quest-Node 3-Layer Socket Button:
 *
 *  1. Outer Ring (Socket):
 *      - 100% FIXED in place (never moves or scales).
 *
 *  2. Pedestal Base:
 *      - 100% FIXED inside socket.
 *      - Pedestal height compresses by 70% (from 4dp to 1.2dp) on press.
 *
 *  3. Inner Button:
 *      - Floats 4 dp above bottom in normal state.
 *      - Moves DOWN by 3 dp in pressed state.
 *      - Soft and satisfying spring/ease-out transition (Duolingo quest node feel).
 *
 *  - Colors, proportions, shadows, and border thickness remain untouched.
 */
@Composable
fun GameButton(
    text: String = "",
    icon: ImageVector? = null,
    onClick: () -> Unit,
    style: GameButtonStyle = GameButtonStyle.PRIMARY,
    shape: GameButtonShape = GameButtonShape.CIRCLE,
    floatingLabel: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 72.dp,
    ringWidth: Dp = 3.2.dp,
    iconSize: Dp = 26.dp,
    fontSize: TextUnit = 14.sp
) {
    val colors = rememberGameButtonColors(style)

    var isPressed by remember { mutableStateOf(false) }

    // ── Refined Soft & Satisfying Press Animations (Inner Circle ONLY) ──────
    val topLayerScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "topLayerScale"
    )

    // Inner circle moves DOWN by 3 dp when pressed
    val topLayerTranslateY by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing),
        label = "topLayerTranslateY"
    )

    // Pedestal bottom depth reduces by 70% from 4dp to 1.2dp on press
    val pedestalDepth by animateDpAsState(
        targetValue = if (isPressed && enabled) 1.2.dp else 4.dp,
        animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing),
        label = "pedestalDepth"
    )

    val disabledAlpha = if (enabled) 1f else 0.4f

    val composableShape: Shape = when (shape) {
        GameButtonShape.CIRCLE -> CircleShape
        GameButtonShape.ROUNDED_RECT -> RoundedCornerShape(18.dp)
        GameButtonShape.PILL -> RoundedCornerShape(50)
    }

    val tapModifier = if (enabled) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                    onClick()
                }
            )
        }
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // ── 1. Floating Label (ALWAYS FIXED) ──────────────────────────────────
        if (floatingLabel != null) {
            FloatingLabelWithPointer(
                text = floatingLabel,
                backgroundColor = colors.containerColor,
                contentColor = colors.contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // ── 2. LAYER 1: OUTER RING SOCKET (100% FIXED, never moves or scales) ──
        Surface(
            shape = composableShape,
            color = colors.ringColor.copy(alpha = disabledAlpha),
            shadowElevation = 0.dp,
            modifier = when (shape) {
                GameButtonShape.CIRCLE -> Modifier.size(width = buttonSize, height = buttonSize + 4.dp)
                GameButtonShape.ROUNDED_RECT -> Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minWidth = 140.dp, minHeight = 48.dp)
                GameButtonShape.PILL -> Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minWidth = 120.dp, minHeight = 44.dp)
            }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(ringWidth)
            ) {
                val innerSize = buttonSize - ringWidth * 2

                // Socket Interior Housing
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = when (shape) {
                        GameButtonShape.CIRCLE -> Modifier.size(width = innerSize, height = innerSize + 4.dp)
                        GameButtonShape.ROUNDED_RECT -> Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minWidth = 132.dp, minHeight = 42.dp)
                        GameButtonShape.PILL -> Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minWidth = 112.dp, minHeight = 38.dp)
                    }
                ) {
                    // ── 3. LAYER 2: PEDESTAL BASE (100% FIXED, bottom depth reduces by 70%) ──
                    Surface(
                        shape = composableShape,
                        color = colors.baseColor.copy(alpha = disabledAlpha),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pedestalDepth)
                            .align(Alignment.BottomCenter)
                    ) {}

                    // ── 4. LAYER 3: INNER INTERACTIVE BUTTON (Floats 4dp, moves DOWN 3dp on press) ──
                    Surface(
                        shape = composableShape,
                        color = colors.containerColor.copy(alpha = disabledAlpha),
                        contentColor = colors.contentColor.copy(alpha = disabledAlpha),
                        border = BorderStroke(1.75.dp, Color(0xFF1A1A1A)),
                        shadowElevation = 0.dp,
                        modifier = (when (shape) {
                            GameButtonShape.CIRCLE -> Modifier.size(innerSize)
                            GameButtonShape.ROUNDED_RECT -> Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 38.dp)
                            GameButtonShape.PILL -> Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 34.dp)
                        })
                            .align(Alignment.TopCenter)
                            .scale(topLayerScale)
                            .offset(y = topLayerTranslateY)
                            .then(tapModifier)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                // Icon + Text side-by-side
                                icon != null && text.isNotBlank() && shape != GameButtonShape.CIRCLE -> {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = text,
                                            modifier = Modifier.size(iconSize)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = text,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = fontSize,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Icon only
                                icon != null -> {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = text.ifBlank { "Button" },
                                        modifier = Modifier.size(iconSize)
                                    )
                                }
                                // Text only
                                text.isNotBlank() -> {
                                    Text(
                                        text = text,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = fontSize,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Floating Label with Pointer Triangle (ALWAYS FIXED)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloatingLabelWithPointer(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = backgroundColor,
            contentColor = contentColor,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Canvas(modifier = Modifier.size(width = 10.dp, height = 6.dp)) {
            val pointerPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2, size.height)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(pointerPath, color = backgroundColor)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Theme-Adaptive Colors (Outer Ring, Base Pedestal, Main Button Cap)
// ──────────────────────────────────────────────────────────────────────────────

data class GameButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val ringColor: Color,
    val baseColor: Color
)

@Composable
fun rememberGameButtonColors(style: GameButtonStyle): GameButtonColors {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val background = MaterialTheme.colorScheme.background
    val isDark = background.red < 0.5f

    // Outer Ring: Solid black border matching app theme
    val socketRingColor = Color(0xFF1A1A1A)

    return remember(style, primary, onPrimary, surface, onSurface, surfaceVariant, onSurfaceVariant, outline, background, isDark, socketRingColor) {
        when (style) {
            GameButtonStyle.PRIMARY -> GameButtonColors(
                containerColor = primary,
                contentColor = onPrimary,
                ringColor = socketRingColor,
                baseColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFF4C1D95)
            )
            GameButtonStyle.SECONDARY -> GameButtonColors(
                containerColor = surfaceVariant,
                contentColor = onSurface,
                ringColor = socketRingColor,
                baseColor = outline
            )
            GameButtonStyle.ICON_ONLY -> GameButtonColors(
                containerColor = surface,
                contentColor = onSurface,
                ringColor = socketRingColor,
                baseColor = outline.copy(alpha = 0.8f)
            )
            GameButtonStyle.FAB -> GameButtonColors(
                containerColor = primary,
                contentColor = onPrimary,
                ringColor = socketRingColor,
                baseColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFF4C1D95)
            )
            GameButtonStyle.NAV -> GameButtonColors(
                containerColor = surfaceVariant,
                contentColor = onSurfaceVariant,
                ringColor = socketRingColor,
                baseColor = outline
            )
        }
    }
}
