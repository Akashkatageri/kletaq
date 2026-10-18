package com.kletaq.app.features.lesson.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val width: Float,
    val height: Float,
    val color: Color
)

@Composable
fun CelebrationConfetti(
    modifier: Modifier = Modifier,
    particleCount: Int = 65,
    durationMillis: Int = 3000
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
        )
    }

    val palette = remember {
        listOf(
            Color(0xFFF59E0B), // Gold
            Color(0xFF10B981), // Emerald
            Color(0xFF8B5CF6), // Purple
            Color(0xFF3B82F6), // Blue
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4), // Cyan
            Color(0xFFF97316)  // Orange
        )
    }

    val particles = remember {
        List(particleCount) {
            val randomColor = palette[it % palette.size]
            ConfettiParticle(
                initialXRatio = Random.nextFloat(),
                initialYRatio = Random.nextFloat() * 0.35f - 0.1f, // Spawn mostly near top
                velocityX = (Random.nextFloat() - 0.5f) * 280f,
                velocityY = Random.nextFloat() * 600f + 350f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                width = Random.nextFloat() * 8f + 8f,
                height = Random.nextFloat() * 12f + 10f,
                color = randomColor
            )
        }
    }

    val t = progress.value
    if (t < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val alpha = (1f - (t - 0.65f) / 0.35f).coerceIn(0f, 1f)

            particles.forEach { particle ->
                val currentX = (particle.initialXRatio * w) + (particle.velocityX * t)
                val currentY = (particle.initialYRatio * h) + (particle.velocityY * t) + (0.5f * 980f * t * t)
                val currentRot = particle.rotation + (particle.rotationSpeed * t)

                if (currentY in 0f..h && currentX in 0f..w) {
                    rotate(degrees = currentRot, pivot = Offset(currentX, currentY)) {
                        drawRect(
                            color = particle.color.copy(alpha = alpha),
                            topLeft = Offset(currentX - particle.width / 2f, currentY - particle.height / 2f),
                            size = Size(particle.width, particle.height)
                        )
                    }
                }
            }
        }
    }
}
