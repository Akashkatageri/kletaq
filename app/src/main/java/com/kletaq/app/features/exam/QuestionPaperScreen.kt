package com.kletaq.app.features.exam

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.PaperModule
import com.kletaq.app.data.model.PaperQuestionGroup
import com.kletaq.app.data.model.QuestionPaper
import com.kletaq.app.data.model.UniversityExamQuestion
import com.kletaq.app.data.repository.QuestionPaperRepository
import com.kletaq.app.data.repository.VtuPaperCatalog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPaperScreen(
    subjectCode: String,
    subjectName: String,
    semesterNumber: Int = 1,
    paper: QuestionPaper? = null,
    isLoading: Boolean = false,
    moduleThemes: List<String> = emptyList(),
    onBackClick: () -> Unit,
    onAwardXp: (amount: Long, source: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val paperInfo = remember(subjectCode, subjectName, semesterNumber) {
        VtuPaperCatalog.resolvePaper(subjectCode, subjectName, semesterNumber)
    }

    var currentPaper by remember(paper) { mutableStateOf(paper) }
    var currentLoading by remember(isLoading) { mutableStateOf(isLoading || paper == null) }

    LaunchedEffect(subjectCode, subjectName, semesterNumber) {
        if (paper == null) {
            currentLoading = true
            val repo = QuestionPaperRepository()
            repo.getQuestionPaperFlow(subjectCode, subjectName, semesterNumber, moduleThemes)
                .collect { result ->
                    result.onSuccess { p ->
                        currentPaper = p
                        currentLoading = false
                    }
                }
        }
    }

    var activeModuleIndex by remember { mutableIntStateOf(0) }
    var showAllSolutions by remember { mutableStateOf(true) }
    var isSimulatingExam by remember { mutableStateOf(false) }
    var examSecondsRemaining by remember { mutableLongStateOf(180 * 60L) } // 3 hours

    // Module choice selection: moduleNumber -> "A" (Q1) or "B" (Q2)
    val chosenQuestions = remember { mutableStateMapOf<Int, String>() }

    // Evaluated score map: questionId -> score
    val userScores = remember { mutableStateMapOf<String, Int>() }

    // Exam countdown timer
    LaunchedEffect(isSimulatingExam) {
        while (isSimulatingExam && examSecondsRemaining > 0) {
            delay(1000L)
            examSecondsRemaining--
        }
    }

    val activeModule = currentPaper?.modules?.getOrNull(activeModuleIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- 1. TOP APP BAR ---
            Surface(
                color = CardSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                            }
                            Column {
                                Text(
                                    text = "VTU Model Question Paper",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${paperInfo.vtuPaperCode} • ${paperInfo.scheme}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PurpleAccent
                                )
                            }
                        }

                        // Direct Official PDF Download Button
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paperInfo.officialPdfUrl)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Opening PDF...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Official PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Exam Meta Badges Row (Horizontally Scrollable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "⏱️ ${paperInfo.durationMinutes / 60} Hours (180 mins)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "🎯 ${paperInfo.maxMarks} Marks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Solved Rubrics", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            // --- 2. MODULE TABS (MODULE 1 TO MODULE 5) ---
            Surface(
                color = CardSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (0..4).forEach { idx ->
                        val isSelected = activeModuleIndex == idx
                        Surface(
                            onClick = { activeModuleIndex = idx },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent else Color.Transparent,
                            border = if (!isSelected) BorderStroke(1.dp, BorderColor) else null,
                            modifier = Modifier.width(68.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Mod ${idx + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. QUESTION PAPER CONTENT ---
            if (currentLoading && currentPaper == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurpleAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Synthesizing University Blueprint & Rubrics...", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else if (activeModule != null) {
                val moduleNum = activeModule.moduleNumber
                val selectedChoice = chosenQuestions[moduleNum] ?: "A"

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = if (isSimulatingExam) 120.dp else 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Module Header Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MODULE $moduleNum",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = PurpleAccent
                                    )
                                    val moduleMarks = activeModule.questionA.totalMarks.takeIf { it > 0 } ?: 20
                                    Text(
                                        text = "Answer Any ONE Full Question (${moduleMarks}M)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = activeModule.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Choice Selector Toggle (Q1 OR Q2)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val qANum = (moduleNum * 2) - 1
                                    val qBNum = moduleNum * 2

                                    ChoiceChip(
                                        label = "Question $qANum",
                                        isSelected = selectedChoice == "A",
                                        onClick = { chosenQuestions[moduleNum] = "A" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChoiceChip(
                                        label = "Question $qBNum (OR)",
                                        isSelected = selectedChoice == "B",
                                        onClick = { chosenQuestions[moduleNum] = "B" },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Render Selected Question Choice
                    val activeQuestionGroup = if (selectedChoice == "A") activeModule.questionA else activeModule.questionB

                    items(activeQuestionGroup.subQuestions, key = { it.id }) { question ->
                        QuestionSolutionCard(
                            question = question,
                            qNumber = activeQuestionGroup.questionNumber,
                            isExpandedByDefault = showAllSolutions,
                            currentScore = userScores[question.id] ?: 0,
                            onScoreChanged = { score ->
                                userScores[question.id] = score
                                val xpEarned = (score * 5).toLong()
                                onAwardXp(xpEarned, "Solved MQP Q${activeQuestionGroup.questionNumber}${question.subPart}")
                                Toast.makeText(context, "+$xpEarned XP for self-scoring! 🔥", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Bottom Safe Area Spacer ensuring last question never touches the navigation bar / gesture bar
                    item(key = "bottom_safe_area_spacer") {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }

        // --- 4. EXAM SIMULATION STICKY BAR ---
        if (isSimulatingExam) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = CardSurface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = PurpleAccent.copy(alpha = 0.2f), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val hours = examSecondsRemaining / 3600
                            val mins = (examSecondsRemaining % 3600) / 60
                            val secs = examSecondsRemaining % 60
                            Text(
                                text = String.format("%02d:%02d:%02d remaining", hours, mins, secs),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFFEAB308)
                            )
                            val totalScored = userScores.values.sum()
                            Text(text = "Self-Scored: $totalScored / 70 Marks", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Button(
                        onClick = {
                            isSimulatingExam = false
                            val totalScored = userScores.values.sum()
                            onAwardXp(150L, "Completed 3-Hour MQP Exam Simulation")
                            Toast.makeText(context, "Exam Complete! Final Score: $totalScored/70 (+150 XP) 🎉", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Finish Exam", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
private fun QuestionSolutionCard(
    question: UniversityExamQuestion,
    qNumber: Int,
    isExpandedByDefault: Boolean,
    currentScore: Int,
    onScoreChanged: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(isExpandedByDefault) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Question Header & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PurpleAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "$qNumber${question.subPart}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PurpleAccent)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${question.marks} Marks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEAB308)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(text = question.bloomLevel, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = TextSecondary)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(text = question.courseOutcome, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question Prompt
            Text(
                text = question.questionText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expand / Collapse Solved Rubric Button
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide Evaluator Solution" else "View Evaluator Solution & Marking Scheme",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Evaluator Rubric Steps
                    Text(
                        text = "📋 OFFICIAL EVALUATOR MARKING SCHEME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    question.rubricSteps.forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${step.stepNumber}. ${step.label}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "+${step.marks} Marks",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PurpleAccent
                                    )
                                }
                                if (step.content.isNotBlank()) {
                                    Text(
                                        text = step.content,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Derivation Checkpoints (if present)
                    if (question.checkpoints.isNotEmpty()) {
                        Text(
                            text = "📐 3-STAGE DERIVATION CHECKPOINTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF06B6D4),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        question.checkpoints.forEach { cp ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF06B6D4).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = cp.stageTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF06B6D4))
                                    Text(text = cp.answer, fontSize = 12.sp, color = TextPrimary, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }

                    // Self-Grade Score Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Self-Score: $currentScore / ${question.marks}M",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (0..question.marks step maxOf(1, question.marks / 3)).forEach { scoreVal ->
                                Surface(
                                    onClick = { onScoreChanged(scoreVal) },
                                    shape = CircleShape,
                                    color = if (currentScore == scoreVal) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "$scoreVal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentScore == scoreVal) Color.White else TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
