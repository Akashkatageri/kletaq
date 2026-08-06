package com.studyos.app.features.lesson.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun PandaCelebrationIllustration(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PandaBounce")
    
    // Gentle floating/bounce animation for panda
    val bounceY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BounceY"
    )

    // Sparkle rotation animation
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing)
        ),
        label = "SparkleRotation"
    )

    Box(
        modifier = modifier
            .width(260.dp)
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f + bounceY)
            val w = size.width
            val h = size.height

            // 1. Soft Glowing Ambient Backlight (Emerald Green & Gold)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF86EFAC).copy(alpha = 0.25f),
                        Color(0xFF34D399).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = center
            )

            // 2. Background Bamboo Stalks (Left & Right)
            drawBambooStalk(this, Offset(w * 0.18f, h * 0.25f), height = 140f, angle = -10f)
            drawBambooStalk(this, Offset(w * 0.82f, h * 0.20f), height = 160f, angle = 12f)

            // 3. Floating Sparkles & Confetti Stars
            drawSparkle(this, Offset(w * 0.15f, h * 0.22f), size = 18f, color = Color(0xFFFBBF24), rotation = sparkleRotation)
            drawSparkle(this, Offset(w * 0.85f, h * 0.32f), size = 16f, color = Color(0xFFFBBF24), rotation = -sparkleRotation)
            drawSparkle(this, Offset(w * 0.28f, h * 0.15f), size = 14f, color = Color(0xFFA855F7), rotation = sparkleRotation * 0.8f)

            // 4. Confetti Streamers
            drawConfettiStreamers(this, center, w, h)

            // 5. PANDA HEAD & BODY
            val pandaCenter = Offset(center.x, center.y + 10f)

            // Ears (Left & Right)
            drawCircle(Color(0xFF1E293B), radius = 28f, center = Offset(pandaCenter.x - 55f, pandaCenter.y - 65f))
            drawCircle(Color(0xFF0F172A), radius = 24f, center = Offset(pandaCenter.x - 55f, pandaCenter.y - 65f))

            drawCircle(Color(0xFF1E293B), radius = 28f, center = Offset(pandaCenter.x + 55f, pandaCenter.y - 65f))
            drawCircle(Color(0xFF0F172A), radius = 24f, center = Offset(pandaCenter.x + 55f, pandaCenter.y - 65f))

            // Body (White rounded oval)
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(pandaCenter.x - 65f, pandaCenter.y - 10f),
                size = Size(130f, 100f),
                cornerRadius = CornerRadius(50f, 50f)
            )

            // Paws/Arms (Left Waving, Right holding Bamboo)
            // Left Arm Waving Up!
            drawCircle(Color(0xFF1E293B), radius = 20f, center = Offset(pandaCenter.x - 68f, pandaCenter.y - 25f))
            // Left Pink Paw Pad
            drawCircle(Color(0xFFF472B6), radius = 8f, center = Offset(pandaCenter.x - 70f, pandaCenter.y - 28f))

            // Right Arm Holding Bamboo
            drawCircle(Color(0xFF1E293B), radius = 20f, center = Offset(pandaCenter.x + 45f, pandaCenter.y + 15f))

            // Held Bamboo Stalk in Right Paw
            drawRoundRect(
                color = Color(0xFF4ADE80),
                topLeft = Offset(pandaCenter.x + 32f, pandaCenter.y - 30f),
                size = Size(12f, 65f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Bamboo Leaves on Held Stalk
            drawCircle(Color(0xFF22C55E), radius = 7f, center = Offset(pandaCenter.x + 46f, pandaCenter.y - 35f))
            drawCircle(Color(0xFF22C55E), radius = 7f, center = Offset(pandaCenter.x + 24f, pandaCenter.y - 20f))

            // Head (Bright White Circle)
            drawCircle(Color(0xFFFFFFFF), radius = 68f, center = Offset(pandaCenter.x, pandaCenter.y - 25f))

            // Eye Patches (Dark Oval Slanted)
            rotate(degrees = 15f, pivot = Offset(pandaCenter.x - 30f, pandaCenter.y - 30f)) {
                drawOval(Color(0xFF1E293B), topLeft = Offset(pandaCenter.x - 44f, pandaCenter.y - 42f), size = Size(30f, 24f))
            }
            rotate(degrees = -15f, pivot = Offset(pandaCenter.x + 30f, pandaCenter.y - 30f)) {
                drawOval(Color(0xFF1E293B), topLeft = Offset(pandaCenter.x + 14f, pandaCenter.y - 42f), size = Size(30f, 24f))
            }

            // Happy Eye Arcs (Smiling eyes ^ ^)
            val happyEyePathLeft = Path().apply {
                moveTo(pandaCenter.x - 38f, pandaCenter.y - 28f)
                quadraticTo(pandaCenter.x - 30f, pandaCenter.y - 36f, pandaCenter.x - 22f, pandaCenter.y - 28f)
            }
            drawPath(happyEyePathLeft, Color.White, style = Stroke(width = 3.5f))

            val happyEyePathRight = Path().apply {
                moveTo(pandaCenter.x + 22f, pandaCenter.y - 28f)
                quadraticTo(pandaCenter.x + 30f, pandaCenter.y - 36f, pandaCenter.x + 38f, pandaCenter.y - 28f)
            }
            drawPath(happyEyePathRight, Color.White, style = Stroke(width = 3.5f))

            // Cheeks (Rosy Pink Blushes)
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.65f), radius = 10f, center = Offset(pandaCenter.x - 42f, pandaCenter.y - 12f))
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.65f), radius = 10f, center = Offset(pandaCenter.x + 42f, pandaCenter.y - 12f))

            // Cute Nose (Rounded Triangle)
            val nosePath = Path().apply {
                moveTo(pandaCenter.x - 7f, pandaCenter.y - 18f)
                lineTo(pandaCenter.x + 7f, pandaCenter.y - 18f)
                quadraticTo(pandaCenter.x, pandaCenter.y - 10f, pandaCenter.x - 7f, pandaCenter.y - 18f)
            }
            drawPath(nosePath, Color(0xFF0F172A))

            // Open Happy Mouth (Smiling U with pink tongue)
            val mouthPath = Path().apply {
                moveTo(pandaCenter.x - 14f, pandaCenter.y - 10f)
                quadraticTo(pandaCenter.x, pandaCenter.y + 12f, pandaCenter.x + 14f, pandaCenter.y - 10f)
            }
            drawPath(mouthPath, Color(0xFF0F172A), style = Stroke(width = 3.5f))
            drawCircle(Color(0xFFFB7185), radius = 6f, center = Offset(pandaCenter.x, pandaCenter.y + 1f))
        }
    }
}

private fun drawBambooStalk(scope: DrawScope, origin: Offset, height: Float, angle: Float) {
    scope.rotate(degrees = angle, pivot = origin) {
        scope.drawRoundRect(
            color = Color(0xFF166534).copy(alpha = 0.5f),
            topLeft = origin,
            size = Size(10f, height),
            cornerRadius = CornerRadius(5f, 5f)
        )
        // Joints
        scope.drawLine(
            color = Color(0xFF22C55E).copy(alpha = 0.7f),
            start = Offset(origin.x - 2f, origin.y + height * 0.35f),
            end = Offset(origin.x + 12f, origin.y + height * 0.35f),
            strokeWidth = 3f
        )
        scope.drawLine(
            color = Color(0xFF22C55E).copy(alpha = 0.7f),
            start = Offset(origin.x - 2f, origin.y + height * 0.7f),
            end = Offset(origin.x + 12f, origin.y + height * 0.7f),
            strokeWidth = 3f
        )
    }
}

private fun drawSparkle(scope: DrawScope, center: Offset, size: Float, color: Color, rotation: Float) {
    scope.rotate(degrees = rotation, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - size)
            quadraticTo(center.x, center.y, center.x + size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + size)
            quadraticTo(center.x, center.y, center.x - size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - size)
        }
        scope.drawPath(path, color)
    }
}

private fun drawConfettiStreamers(scope: DrawScope, center: Offset, w: Float, h: Float) {
    val confettiColors = listOf(
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF59E0B), // Amber
        Color(0xFFA855F7), // Purple
        Color(0xFF10B981)  // Emerald
    )

    val positions = listOf(
        Offset(w * 0.30f, h * 0.08f),
        Offset(w * 0.45f, h * 0.05f),
        Offset(w * 0.65f, h * 0.08f),
        Offset(w * 0.20f, h * 0.35f),
        Offset(w * 0.80f, h * 0.38f),
        Offset(w * 0.12f, h * 0.18f),
        Offset(w * 0.88f, h * 0.18f)
    )

    positions.forEachIndexed { index, pos ->
        val color = confettiColors[index % confettiColors.size]
        scope.rotate(degrees = (index * 47) % 360f, pivot = pos) {
            if (index % 2 == 0) {
                scope.drawRoundRect(
                    color = color,
                    topLeft = pos,
                    size = Size(10f, 18f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            } else {
                scope.drawCircle(color, radius = 5f, center = pos)
            }
        }
    }
}
