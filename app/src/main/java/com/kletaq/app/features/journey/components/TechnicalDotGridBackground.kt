package com.kletaq.app.features.journey.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders an ultra-premium technical dot-matrix blueprint background with
 * subtle ambient radial glows behind the active rail and command deck.
 */
@Composable
fun TechnicalDotGridBackground(
    modifier: Modifier = Modifier,
    dotSpacing: Dp = 24.dp,
    dotRadius: Dp = 1.2.dp,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }

    val baseBackgroundColor = if (isDark) Color(0xFF090D16) else Color(0xFFF8FAFC)
    val dotColor = if (isDark) Color.White.copy(alpha = 0.055f) else Color(0xFF0F172A).copy(alpha = 0.045f)
    
    // Ambient Glow Colors
    val ambientPrimaryGlow = if (isDark) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF818CF8).copy(alpha = 0.08f)
    val ambientCyanGlow = if (isDark) Color(0xFF06B6D4).copy(alpha = 0.08f) else Color(0xFF06B6D4).copy(alpha = 0.05f)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Base Background Fill
            drawRect(color = baseBackgroundColor)

            // 2. Ambient Top-Left Violet Radial Glow (Behind the Command Deck & Active Rail)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ambientPrimaryGlow, Color.Transparent),
                    center = Offset(canvasWidth * 0.25f, canvasHeight * 0.18f),
                    radius = canvasWidth * 0.85f
                ),
                radius = canvasWidth * 0.85f,
                center = Offset(canvasWidth * 0.25f, canvasHeight * 0.18f)
            )

            // 3. Ambient Mid-Right Cyan Radial Glow (Behind lower roadmap stations)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ambientCyanGlow, Color.Transparent),
                    center = Offset(canvasWidth * 0.80f, canvasHeight * 0.55f),
                    radius = canvasWidth * 0.70f
                ),
                radius = canvasWidth * 0.70f,
                center = Offset(canvasWidth * 0.80f, canvasHeight * 0.55f)
            )

            // 4. Precision Technical Dot Grid
            val spacingPx = dotSpacing.toPx()
            val dotRadiusPx = dotRadius.toPx()

            val numCols = (canvasWidth / spacingPx).toInt() + 1
            val numRows = (canvasHeight / spacingPx).toInt() + 1

            for (col in 0..numCols) {
                val x = col * spacingPx
                for (row in 0..numRows) {
                    val y = row * spacingPx
                    drawCircle(
                        color = dotColor,
                        radius = dotRadiusPx,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Layer the actual content above the technical canvas
        content()
    }
}
