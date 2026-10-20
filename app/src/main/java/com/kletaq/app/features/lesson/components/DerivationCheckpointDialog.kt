package com.kletaq.app.features.lesson.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.data.model.DerivationCheckpoint
import com.kletaq.app.data.model.UniversityExamQuestion

@Composable
fun DerivationCheckpointDialog(
    question: UniversityExamQuestion,
    onDismiss: () -> Unit,
    onDerivationMastered: (marksEarned: Int) -> Unit = {}
) {
    val checkpoints = question.checkpoints
    if (checkpoints.isEmpty()) {
        onDismiss()
        return
    }

    var currentStageIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val masteredStages = remember { mutableStateListOf<Int>() }

    val currentCheckpoint = checkpoints[currentStageIndex]
    val totalStages = checkpoints.size
    val isLastStage = currentStageIndex == totalStages - 1
    val allCompleted = masteredStages.size == totalStages

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header: Stage progress & Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PurpleAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${question.marks}M DERIVATION CHUNKING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PurpleAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stage ${currentStageIndex + 1} of $totalStages",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = question.questionText.take(45) + if (question.questionText.length > 45) "..." else "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Linear Stage Progress Indicator
                LinearProgressIndicator(
                    progress = { (currentStageIndex + 1).toFloat() / totalStages },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PurpleAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // The Interactive 3D Flip Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped }
                ) {
                    if (rotation <= 90f) {
                        // FRONT SIDE: The Active Recall Prompt
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.5.dp, PurpleAccent.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = PurpleAccent,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${currentCheckpoint.stageIndex}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Text(
                                            text = currentCheckpoint.stageTitle.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleAccent
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = currentCheckpoint.prompt,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (currentCheckpoint.notePageCitation != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "From Page ${currentCheckpoint.notePageCitation} of your notes",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = PurpleAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tap card to reveal derivation step",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                }
                            }
                        }
                    } else {
                        // BACK SIDE: The Mathematical Step & Derivation Content
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E1E2E), // Dark high-contrast math slate
                            border = BorderStroke(1.5.dp, Color(0xFF89B4FA).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "EXPECTED DERIVATION STEP (+${currentCheckpoint.marks}M)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFA6E3A1)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF89B4FA).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Stage ${currentCheckpoint.stageIndex}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF89B4FA),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = currentCheckpoint.answer,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        color = Color(0xFFCDD6F4)
                                    )
                                }

                                Text(
                                    text = "Tap card to flip back to question",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6C7086),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Controls: Action & Rating Buttons
                if (allCompleted) {
                    Button(
                        onClick = {
                            onDerivationMastered(question.marks)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Derivation Mastered (+${question.marks * 5} XP)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isFlipped = false
                                if (currentStageIndex > 0) {
                                    currentStageIndex--
                                }
                            },
                            enabled = currentStageIndex > 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Previous", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (!masteredStages.contains(currentStageIndex)) {
                                    masteredStages.add(currentStageIndex)
                                }
                                isFlipped = false
                                if (currentStageIndex < totalStages - 1) {
                                    currentStageIndex++
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = if (isLastStage) "Finish & Master" else "Next Stage ➔",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
