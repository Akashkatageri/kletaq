package com.kletaq.app.features.flashcards.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class WhiteboardPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun FlashcardWhiteboard(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<WhiteboardPath>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(Color(0xFF3B82F6)) } // Anki Blue default
    var strokeWidth by remember { mutableStateOf(6f) }

    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFFEF4444), // Red
        Color(0xFF10B981), // Green
        Color(0xFFF59E0B), // Yellow/Orange
        Color(0xFFFFFFFF), // White
        Color(0xFF1E293B)  // Dark Slate
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        // Drawing Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                paths.add(
                                    WhiteboardPath(
                                        points = currentPoints.toList(),
                                        color = selectedColor,
                                        strokeWidth = strokeWidth
                                    )
                                )
                                currentPoints.clear()
                            }
                        },
                        onDragCancel = {
                            currentPoints.clear()
                        }
                    )
                }
        ) {
            // Render completed strokes
            paths.forEach { stroke ->
                if (stroke.points.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke.points[0].x, stroke.points[0].y)
                        for (i in 1 until stroke.points.size) {
                            val p0 = stroke.points[i - 1]
                            val p1 = stroke.points[i]
                            quadraticTo(p0.x, p0.y, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                        }
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (stroke.points.size == 1) {
                    drawCircle(
                        color = stroke.color,
                        radius = stroke.strokeWidth / 2f,
                        center = stroke.points[0]
                    )
                }
            }

            // Render live stroke
            if (currentPoints.size > 1) {
                val livePath = Path().apply {
                    moveTo(currentPoints[0].x, currentPoints[0].y)
                    for (i in 1 until currentPoints.size) {
                        lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                }
                drawPath(
                    path = livePath,
                    color = selectedColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Floating Whiteboard Toolbar at top
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.92f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Palette
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { col ->
                        val isSelected = selectedColor == col
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = col }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                                )
                        )
                    }
                }

                // Action Controls: Undo, Clear, Close
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (paths.isNotEmpty()) {
                                paths.removeAt(paths.lastIndex)
                            }
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Stroke",
                            tint = if (paths.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { paths.clear() },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear All",
                            tint = if (paths.isNotEmpty()) Color(0xFFEF4444) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Whiteboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
