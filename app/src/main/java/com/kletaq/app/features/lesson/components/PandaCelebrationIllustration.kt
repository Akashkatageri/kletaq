package com.kletaq.app.features.lesson.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kletaq.app.R

@Composable
fun PandaCelebrationIllustration(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PandaCelebrationMotion")

    // Gentle floating/bounce animation for panda
    val bounceY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PandaBounceY"
    )

    // Sparkle rotation & pulse animation
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing)
        ),
        label = "SparkleRotation"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraPulse"
    )

    Box(
        modifier = modifier
            .width(150.dp)
            .height(135.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Glowing Backlight & Sparkle Stars Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f + bounceY)
            val w = size.width

            // Radial Glow Behind Panda
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF86EFAC).copy(alpha = 0.35f),
                        Color(0xFFFDE047).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (w * 0.48f) * auraPulse
                ),
                radius = (w * 0.48f) * auraPulse,
                center = center
            )

            // Sparkle Stars around Panda
            drawSparkleStar(this, Offset(w * 0.12f, size.height * 0.28f), size = 12f, color = Color(0xFFF59E0B), rotation = sparkleRotation)
            drawSparkleStar(this, Offset(w * 0.88f, size.height * 0.24f), size = 14f, color = Color(0xFF10B981), rotation = -sparkleRotation)
            drawSparkleStar(this, Offset(w * 0.20f, size.height * 0.74f), size = 11f, color = Color(0xFFA855F7), rotation = sparkleRotation * 0.7f)
            drawSparkleStar(this, Offset(w * 0.82f, size.height * 0.80f), size = 12f, color = Color(0xFFF59E0B), rotation = -sparkleRotation * 0.8f)
        }

        // 2. High-Resolution 3D Celebratory Panda Asset
        Image(
            painter = painterResource(id = R.drawable.panda_complete),
            contentDescription = "Panda Celebrating Lesson Completion",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .offset(y = bounceY.dp)
        )
    }
}

private fun drawSparkleStar(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    center: Offset,
    size: Float,
    color: Color,
    rotation: Float
) {
    scope.rotate(degrees = rotation, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - size)
            quadraticTo(center.x, center.y, center.x + size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + size)
            quadraticTo(center.x, center.y, center.x - size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - size)
            close()
        }
        drawPath(path, color)
    }
}
