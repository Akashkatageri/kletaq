package com.kletaq.app.features.memorypalace.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.memorypalace.model.PalaceLocus

/**
 * 🎥 Camera Pan & Zoom Room Viewer
 * Implements Phase 3 (Walk Mode): smoothly pans and zooms the camera across room landmarks
 * as the student progresses from pin to pin along their memory journey.
 */
@Composable
fun CameraPanRoomViewer(
    bitmap: Bitmap?,
    activeLocus: PalaceLocus?,
    allLoci: List<PalaceLocus> = emptyList(),
    modifier: Modifier = Modifier,
    initialFocusOnPin: Boolean = true
) {
    var isPinFocused by remember { mutableStateOf(initialFocusOnPin) }

    // Target zoom scale and pivot
    val targetScale = if (isPinFocused) 2.2f else 1.0f
    val targetPivotX = if (isPinFocused && activeLocus != null) activeLocus.xPercent.coerceIn(0.1f, 0.9f) else 0.5f
    val targetPivotY = if (isPinFocused && activeLocus != null) activeLocus.yPercent.coerceIn(0.1f, 0.9f) else 0.5f

    val animScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "cameraScale"
    )

    val animPivotX by animateFloatAsState(
        targetValue = targetPivotX,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "cameraPivotX"
    )

    val animPivotY by animateFloatAsState(
        targetValue = targetPivotY,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "cameraPivotY"
    )

    // Pulsing halo animation for the active pin
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val imageAspectRatio = remember(bitmap) {
        if (bitmap != null && bitmap.height > 0) {
            bitmap.width.toFloat() / bitmap.height.toFloat()
        } else {
            4f / 3f
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (bitmap != null) Modifier.aspectRatio(imageAspectRatio) else Modifier.height(240.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val pinRadiusPx = with(LocalDensity.current) { 18.dp.toPx() }
        val smallPinRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }

        // Camera layer: Room image + Pin markers transform synchronously
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animScale
                    scaleY = animScale
                    transformOrigin = TransformOrigin(animPivotX, animPivotY)
                }
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Memory Palace Room Photo",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Room Photo Available", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Inactive Pins (subtle background markers)
            allLoci.forEach { locus ->
                if (locus.orderIndex != activeLocus?.orderIndex) {
                    val px = (locus.xPercent * containerWidth) - smallPinRadiusPx
                    val py = (locus.yPercent * containerHeight) - smallPinRadiusPx
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(px.toInt(), py.toInt()) }
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${locus.orderIndex}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Active Glowing Pin with animated pulse
            if (activeLocus != null) {
                val pinX = (activeLocus.xPercent * containerWidth) - pinRadiusPx
                val pinY = (activeLocus.yPercent * containerHeight) - pinRadiusPx

                // Outer pulsing halo
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pinX - (18.dp.toPx() * (pulseScale - 1f))).toInt(),
                                (pinY - (18.dp.toPx() * (pulseScale - 1f))).toInt()
                            )
                        }
                        .size((36 * pulseScale).dp)
                        .background(Color(0xFFEF4444).copy(alpha = pulseAlpha), CircleShape)
                )

                // Core Pin Badge
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pinX.toInt(), pinY.toInt()) }
                        .size(36.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${activeLocus.orderIndex}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Camera Mode Toggle Pill (Top-Right Floating Overlay)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clickable { isPinFocused = !isPinFocused }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPinFocused) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isPinFocused) "Full Room" else "Focus Pin",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
