package com.kletaq.app.features.lesson.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.data.model.UniversityExamQuestion

@Composable
fun ExamQuestionsSection(
    questions: List<UniversityExamQuestion>,
    onDerivationMastered: (marks: Int) -> Unit = {},
    onScoreSubmitted: (score: Int, maxMarks: Int) -> Unit = { _, _ -> }
) {
    if (questions.isEmpty()) return

    var activeCheckpointQuestion by remember { mutableStateOf<UniversityExamQuestion?>(null) }
    var activePaperCheckQuestion by remember { mutableStateOf<UniversityExamQuestion?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "UNIVERSITY EXAM MODEL QUESTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurpleAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "7/8-Mark Derivations & Numerical Solvers",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Questions List
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                questions.forEachIndexed { index, question ->
                    ExamQuestionCard(
                        question = question,
                        index = index + 1,
                        onOpenCheckpoints = { activeCheckpointQuestion = question },
                        onOpenPaperSelfCheck = { activePaperCheckQuestion = question }
                    )
                }
            }
        }
    }

    activeCheckpointQuestion?.let { q ->
        DerivationCheckpointDialog(
            question = q,
            onDismiss = { activeCheckpointQuestion = null },
            onDerivationMastered = onDerivationMastered
        )
    }

    activePaperCheckQuestion?.let { q ->
        PaperSelfCheckDialog(
            question = q,
            onDismiss = { activePaperCheckQuestion = null },
            onScoreSubmitted = onScoreSubmitted
        )
    }
}

@Composable
private fun ExamQuestionCard(
    question: UniversityExamQuestion,
    index: Int,
    onOpenCheckpoints: () -> Unit,
    onOpenPaperSelfCheck: () -> Unit
) {
    var isRubricExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Badges row: Marks, Bloom's, CO, Guaranteed PYQ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PurpleAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Q.$index(${question.subPart.uppercase()}) • ${question.marks} MARKS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${question.bloomLevel} ${question.courseOutcome}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (question.isGuaranteedPyq) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "⭐ High-Yield PYQ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The Question statement
            Text(
                text = question.questionText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // If it's a Numerical Problem, render the 3-Step Solver Card
            question.numericalSolver?.let { solver ->
                NumericalSolverCard(solver = solver)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (question.checkpoints.isNotEmpty()) {
                    Button(
                        onClick = onOpenCheckpoints,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "3-Stage Checkpoints", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onOpenPaperSelfCheck,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Paper Self-Check", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Collapsible Evaluator Marking Rubric view
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isRubricExpanded = !isRubricExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evaluator Marking Rubric (${question.rubricSteps.size} steps)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isRubricExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isRubricExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    question.rubricSteps.forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = step.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                    Text(
                                        text = "+${step.marks}M",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SuccessGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = step.content,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (question.evaluatorTip.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "💡", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = question.evaluatorTip,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericalSolverCard(solver: com.kletaq.app.data.model.NumericalSolver) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E2E), // High-contrast math slate
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = null,
                    tint = Color(0xFF89B4FA),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "NUMERICAL STEP-BY-STEP SOLVER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF89B4FA)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step 1: Given Data
            if (solver.givenData.isNotEmpty()) {
                Text(
                    text = "1. Given Data (with SI Units):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA6ADC8)
                )
                solver.givenData.forEach { data ->
                    Text(
                        text = "• $data",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFCDD6F4)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Step 2: Formula
            if (solver.formula.isNotBlank()) {
                Text(
                    text = "2. Formula:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA6ADC8)
                )
                Text(
                    text = solver.formula,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA6E3A1)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Step 3: Calculation & Final Answer
            if (solver.finalAnswer.isNotBlank()) {
                Text(
                    text = "3. Final Answer with Units:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA6ADC8)
                )
                Text(
                    text = solver.finalAnswer,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF9E2AF)
                )
            }

            if (solver.trapWarning.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "⚠️ Trap: ${solver.trapWarning}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF38BA8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
