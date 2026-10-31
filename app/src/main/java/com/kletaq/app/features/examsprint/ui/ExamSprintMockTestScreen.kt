package com.kletaq.app.features.examsprint.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.WarningAmber
import com.kletaq.app.features.examsprint.data.ExamSprintRepository
import com.kletaq.app.features.examsprint.model.QuestionMastery
import com.kletaq.app.features.examsprint.model.SprintQuestion
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSprintMockTestScreen(
    sprintId: String,
    questionCount: Int = 5,
    onBackClick: () -> Unit
) {
    val sprint = remember(sprintId) { ExamSprintRepository.getSprint(sprintId) }
    val sampledQuestions = remember(sprintId, questionCount) {
        ExamSprintRepository.sampleMockQuestions(sprintId, questionCount)
    }

    if (sprint == null || sampledQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No questions available for mock test", color = TextPrimary)
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var secondsRemaining by remember { mutableLongStateOf(60 * 60L) } // 60 minutes
    var isTimerRunning by remember { mutableStateOf(true) }

    // Scratchpad notes per question
    val scratchpads = remember { mutableStateMapOf<String, String>() }

    // Evaluation mode after submission
    var isEvaluating by remember { mutableStateOf(false) }
    val questionScores = remember { mutableStateMapOf<String, Int>() }
    var isSubmittedAndScored by remember { mutableStateOf(false) }

    // Countdown Timer Loop
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    val currentQ = sampledQuestions[currentIndex.coerceIn(0, sampledQuestions.size - 1)]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Mock", tint = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEvaluating) "Self-Evaluation & Grading" else "Internal Mock Simulation",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${sampledQuestions.size} Questions • 60 Mins",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Countdown Timer Chip
                val mins = secondsRemaining / 60
                val secs = secondsRemaining % 60
                val timerColor = when {
                    secondsRemaining < 300 -> Color(0xFFEF4444) // < 5 mins red
                    secondsRemaining < 900 -> WarningAmber
                    else -> PrimaryAccentColor
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = timerColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, timerColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor
                        )
                    }
                }
            }

            if (!isEvaluating) {
                // Test In-Progress View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Progress Indicator Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${currentIndex + 1} of ${sampledQuestions.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${currentQ.marks} Marks",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryAccentColor
                        )
                    }

                    // Main Question Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currentQ.questionText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    // Scratchpad / Answer notes
                    Text(
                        text = "Scratchpad / Quick Points (Write full answers on your paper):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = scratchpads[currentQ.id] ?: "",
                        onValueChange = { scratchpads[currentQ.id] = it },
                        placeholder = { Text("Jot down formulas, outline points, or key diagrams...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Navigation & Submit Pager
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentIndex > 0) currentIndex-- },
                            enabled = currentIndex > 0,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }

                        if (currentIndex < sampledQuestions.size - 1) {
                            Button(
                                onClick = { currentIndex++ },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentColor)
                            ) {
                                Text("Next", color = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    isTimerRunning = false
                                    isEvaluating = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Submit & Grade", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Post-Submission Self-Evaluation & Grading View
                val totalMaxMarks = sampledQuestions.sumOf { it.marks }
                val totalEarned = sampledQuestions.sumOf { questionScores[it.id] ?: (it.marks * 8 / 10) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Score Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
                                Text(
                                    text = "Mock Completed! Grade Your Answers:",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Total Score: $totalEarned / $totalMaxMarks Marks (${(totalEarned * 100 / totalMaxMarks)}%)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                                Text(
                                    text = "Review your points against the examiner rubrics below and adjust marks.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Questions & Rubrics List for Self-Grading
                    itemsIndexed(sampledQuestions) { idx, q ->
                        val currentScore = questionScores[q.id] ?: (q.marks * 8 / 10)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Q${idx + 1}: ${q.marks} Marks",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryAccentColor
                                    )
                                    Text(
                                        text = "Awarded: $currentScore / ${q.marks}M",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }

                                Text(
                                    text = q.questionText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                // Rubric checkpoints
                                if (q.markingRubric.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryAccentColor.copy(alpha = 0.08f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Scoring Checkpoints:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryAccentColor)
                                            q.markingRubric.forEach { r ->
                                                Text("• $r", fontSize = 10.sp, color = TextPrimary)
                                            }
                                        }
                                    }
                                }

                                // Model Answer
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Model Answer Reference:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(q.effectiveAnswer, fontSize = 11.sp, color = TextPrimary, lineHeight = 15.sp)
                                    }
                                }

                                // Mark slider
                                Text("Adjust Score for this Question:", fontSize = 11.sp, color = TextSecondary)
                                Slider(
                                    value = currentScore.toFloat(),
                                    onValueChange = { questionScores[q.id] = it.toInt() },
                                    valueRange = 0f..q.marks.toFloat(),
                                    steps = q.marks - 1,
                                    colors = SliderDefaults.colors(thumbColor = PrimaryAccentColor, activeTrackColor = PrimaryAccentColor)
                                )
                            }
                        }
                    }

                    // Confirm and Save Score
                    item {
                        Button(
                            onClick = {
                                ExamSprintRepository.recordMockTestResult(sprintId, totalEarned, totalMaxMarks)
                                // Mark questions that scored >= 80% as MASTERED
                                sampledQuestions.forEach { q ->
                                    val score = questionScores[q.id] ?: (q.marks * 8 / 10)
                                    if (score >= (q.marks * 8 / 10)) {
                                        ExamSprintRepository.updateQuestionMastery(sprintId, q.id, QuestionMastery.MASTERED)
                                    } else {
                                        ExamSprintRepository.updateQuestionMastery(sprintId, q.id, QuestionMastery.NEEDS_REVIEW)
                                    }
                                }
                                onBackClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Score & Award XP", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

