package com.kletaq.app.features.lesson.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.sound.SoundEffectType
import com.kletaq.app.core.sound.SoundEffectsManager
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.data.model.QuizQuestion

/**
 * Addictive, Duolingo-style 30-Second Active Recall Micro-Quiz Card.
 * Gives instant dopamine hits with crisp haptics, success chime, and immediate +15 XP rewards.
 * Works seamlessly across all 200+ topics with zero per-topic manual authoring.
 */
@Composable
fun QuickQuizSection(
    quiz: List<QuizQuestion>,
    topicTitle: String,
    onAwardXp: (amount: Long, source: String) -> Unit,
    modifier: Modifier = Modifier,
    isFromUploadedNotes: Boolean = true
) {
    if (quiz.isEmpty()) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val soundManager = remember(context) {
        SoundEffectsManager(context).apply { initialize() }
    }
    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    var currentQuestionIndex by remember(quiz) { mutableIntStateOf(0) }
    var selectedOptionIndex by remember(quiz, currentQuestionIndex) { mutableStateOf<Int?>(null) }
    var hasAnsweredCurrent by remember(quiz, currentQuestionIndex) { mutableStateOf(false) }
    var completedQuestionsCount by remember(quiz) { mutableIntStateOf(0) }
    var totalXpEarnedInQuiz by remember(quiz) { mutableLongStateOf(0L) }

    val currentQuestion = quiz.getOrNull(currentQuestionIndex) ?: return
    val isQuizFinished = completedQuestionsCount >= quiz.size

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.2.dp,
            if (isQuizFinished) Color(0xFF10B981).copy(alpha = 0.4f) else PurpleAccent.copy(alpha = 0.25f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Badge + Step Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isFromUploadedNotes) "ACTIVE RECALL • FROM YOUR NOTES 📝" else "30-SEC ACTIVE RECALL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isQuizFinished) "Notes Quiz Mastered! 🎉" else "Notes Micro-Quiz • +15 XP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Question Counter Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (isQuizFinished) "DONE" else "${currentQuestionIndex + 1} of ${quiz.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isQuizFinished) {
                // Question Text
                Text(
                    text = currentQuestion.question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Options List (Duolingo-style tappable cards)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currentQuestion.options.forEachIndexed { optIndex, optionText ->
                        val isSelected = selectedOptionIndex == optIndex
                        val isCorrect = optIndex == currentQuestion.correctIndex

                        val backgroundColor by animateColorAsState(
                            targetValue = when {
                                hasAnsweredCurrent && isCorrect -> Color(0xFF10B981).copy(alpha = 0.15f)
                                hasAnsweredCurrent && isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                isSelected -> PurpleAccent.copy(alpha = 0.10f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            animationSpec = tween(250),
                            label = "optionBg"
                        )

                        val borderColor by animateColorAsState(
                            targetValue = when {
                                hasAnsweredCurrent && isCorrect -> Color(0xFF10B981)
                                hasAnsweredCurrent && isSelected && !isCorrect -> Color(0xFFEF4444)
                                isSelected -> PurpleAccent
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                            },
                            animationSpec = tween(250),
                            label = "optionBorder"
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = backgroundColor,
                            border = BorderStroke(if (hasAnsweredCurrent && (isCorrect || isSelected)) 1.8.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !hasAnsweredCurrent) {
                                    selectedOptionIndex = optIndex
                                    hasAnsweredCurrent = true

                                    if (optIndex == currentQuestion.correctIndex) {
                                        // Tactile reward: success chime + punchy haptic + instant XP
                                        soundManager.playSound(SoundEffectType.XP_GAIN)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        totalXpEarnedInQuiz += 15L
                                        onAwardXp(15L, "Quick Quiz: $topicTitle")
                                    } else {
                                        // Educational feedback: light haptic vibration buzz
                                        soundManager.playSound(SoundEffectType.BUTTON_CLICK)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Index badge / status icon
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                hasAnsweredCurrent && isCorrect -> Color(0xFF10B981)
                                                hasAnsweredCurrent && isSelected && !isCorrect -> Color(0xFFEF4444)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasAnsweredCurrent && isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else if (hasAnsweredCurrent && isSelected && !isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = ('A' + optIndex).toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = optionText,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || (hasAnsweredCurrent && isCorrect)) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Post-Answer Feedback & Explanation
                AnimatedVisibility(
                    visible = hasAnsweredCurrent,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    val isCorrect = selectedOptionIndex == currentQuestion.correctIndex
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.12f) else PurpleAccent.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, if (isCorrect) Color(0xFF10B981).copy(alpha = 0.35f) else PurpleAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = if (isCorrect) Color(0xFF10B981) else PurpleAccent,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isCorrect) "Awesome! +15 XP Earned 🔥" else "Concept Note:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrect) Color(0xFF10B981) else PurpleAccent
                                    )
                                    if (currentQuestion.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentQuestion.explanation,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Next / Continue Button
                        Button(
                            onClick = {
                                soundManager.playSound(SoundEffectType.BUTTON_CLICK)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                completedQuestionsCount++
                                if (currentQuestionIndex + 1 < quiz.size) {
                                    currentQuestionIndex++
                                    selectedOptionIndex = null
                                    hasAnsweredCurrent = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isCorrect) Color(0xFF10B981) else PurpleAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = if (currentQuestionIndex + 1 < quiz.size) "Next Question" else "Finish Micro-Quiz",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Completed State: High dopamine celebration card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🎉", fontSize = 42.sp)
                    Text(
                        text = "Topic Recall Mastered!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = if (totalXpEarnedInQuiz > 0) "+$totalXpEarnedInQuiz XP added to your daily progress!" else "Great revision check completed!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
