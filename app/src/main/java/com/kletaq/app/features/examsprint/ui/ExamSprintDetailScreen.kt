package com.kletaq.app.features.examsprint.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.kletaq.app.features.examsprint.model.ExamSprint
import com.kletaq.app.features.examsprint.model.QuestionMastery
import com.kletaq.app.features.examsprint.model.SprintQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSprintDetailScreen(
    sprintId: String,
    onBackClick: () -> Unit,
    onStartMockTest: (sprintId: String, count: Int) -> Unit,
    onStartFocusSprint: (subjectCode: String, subjectName: String) -> Unit
) {
    val sprints by ExamSprintRepository.sprints.collectAsState()
    val sprint = sprints.firstOrNull { it.id == sprintId }

    if (sprint == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sprint not found", color = TextPrimary)
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var editingQuestion by remember { mutableStateOf<SprintQuestion?>(null) }

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
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sprint.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (sprint.subjectCode.isNotBlank()) "${sprint.subjectCode} • ${sprint.subjectName}" else sprint.subjectName,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Focus sprint button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryAccentColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, PrimaryAccentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        onStartFocusSprint(sprint.subjectCode, sprint.subjectName)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryAccentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("25m Focus", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryAccentColor)
                    }
                }
            }

            // Sprint Progress Banner
            SprintProgressBanner(sprint = sprint)

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Questions (${sprint.totalQuestions})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Mock Exam", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Flash Drill", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Question Bank with Layered Answer Deconstruction
                    QuestionBankTab(
                        sprint = sprint,
                        onEditQuestionAnswer = { editingQuestion = it },
                        onUpdateMastery = { qId, status ->
                            ExamSprintRepository.updateQuestionMastery(sprint.id, qId, status)
                        }
                    )
                }
                1 -> {
                    // Mock Internal Simulator Tab
                    MockExamSimulatorTab(
                        sprint = sprint,
                        onStartMock = { count -> onStartMockTest(sprint.id, count) }
                    )
                }
                2 -> {
                    // Active Recall Flashcard Drill Tab
                    FlashDrillTab(
                        sprint = sprint,
                        onUpdateMastery = { qId, status ->
                            ExamSprintRepository.updateQuestionMastery(sprint.id, qId, status)
                        }
                    )
                }
            }
        }
    }

    // Edit Custom Notes / Answer Dialog
    if (editingQuestion != null) {
        val q = editingQuestion!!
        var answerDraft by remember { mutableStateOf(q.effectiveAnswer) }

        AlertDialog(
            onDismissRequest = { editingQuestion = null },
            title = {
                Text(
                    text = "Edit Answer (Q${q.questionNumber})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = q.questionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = answerDraft,
                        onValueChange = { answerDraft = it },
                        label = { Text("Your Notes / Answer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        text = "Your classroom notes will be saved and prioritized over the generated answer.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ExamSprintRepository.updateQuestionCustomAnswer(sprint.id, q.id, answerDraft.trim())
                        editingQuestion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("Save Notes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingQuestion = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SprintProgressBanner(sprint: ExamSprint) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val days = sprint.daysRemaining
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarningAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (days == 0) "🔥 TODAY" else if (days == 1) "⚡ TOMORROW" else "🎯 IN $days DAYS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${sprint.masteredQuestionsCount}/${sprint.totalQuestions} Mastered",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "${sprint.masteryPercentage.toInt()}% Exam Ready",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (sprint.masteryPercentage >= 80f) SuccessGreen else WarningAmber
                )
            }

            LinearProgressIndicator(
                progress = { sprint.masteryPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (sprint.masteryPercentage >= 80f) SuccessGreen else WarningAmber,
                trackColor = BorderColor.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun QuestionBankTab(
    sprint: ExamSprint,
    onEditQuestionAnswer: (SprintQuestion) -> Unit,
    onUpdateMastery: (questionId: String, QuestionMastery) -> Unit
) {
    var filterMastery by remember { mutableStateOf<QuestionMastery?>(null) }

    val filteredQuestions = remember(sprint.questions, filterMastery) {
        if (filterMastery == null) sprint.questions
        else sprint.questions.filter { it.masteryStatus == filterMastery }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterMastery == null,
                onClick = { filterMastery = null },
                label = { Text("All (${sprint.totalQuestions})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterMastery == QuestionMastery.MASTERED,
                onClick = { filterMastery = QuestionMastery.MASTERED },
                label = { Text("Mastered (${sprint.masteredQuestionsCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuccessGreen.copy(alpha = 0.2f), selectedLabelColor = SuccessGreen)
            )
            FilterChip(
                selected = filterMastery == QuestionMastery.NEEDS_REVIEW,
                onClick = { filterMastery = QuestionMastery.NEEDS_REVIEW },
                label = { Text("Review (${sprint.needsReviewCount})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WarningAmber.copy(alpha = 0.2f), selectedLabelColor = WarningAmber)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredQuestions, key = { it.id }) { q ->
                QuestionStudyCard(
                    question = q,
                    onEdit = { onEditQuestionAnswer(q) },
                    onToggleMastery = { status -> onUpdateMastery(q.id, status) }
                )
            }
        }
    }
}

@Composable
private fun QuestionStudyCard(
    question: SprintQuestion,
    onEdit: () -> Unit,
    onToggleMastery: (QuestionMastery) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isAnswerHiddenForSelfTest by remember { mutableStateOf(false) }

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
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryAccentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Q${question.questionNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "${question.marks}M",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (question.isUserAnswerCustomized) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SuccessGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Class Notes",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Mastery Status Badge
                val (masteryLabel, masteryColor) = when (question.masteryStatus) {
                    QuestionMastery.MASTERED -> "Mastered" to SuccessGreen
                    QuestionMastery.NEEDS_REVIEW -> "Needs Review" to WarningAmber
                    QuestionMastery.UNATTEMPTED -> "Unattempted" to TextSecondary
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = masteryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = masteryLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = masteryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Question Text
            Text(
                text = question.questionText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // Layer 1: 10-Second Mental Model Hook
            if (question.quickRecall.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmber.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, WarningAmber.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = question.quickRecall,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Expandable Deconstruction Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Layer 2: Examiner Scoring Anchors (Rubric)
                    if (question.markingRubric.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryAccentColor.copy(alpha = 0.07f),
                            border = BorderStroke(0.5.dp, PrimaryAccentColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🎯 Examiner Scoring Rubric (Must-Write Points):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccentColor
                                )
                                question.markingRubric.forEach { r ->
                                    Text(
                                        text = "• $r",
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Layer 3: Full Answer with "Self-Test Cover" Mode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (question.studentAnswer != null) "Full Answer (Your Notes):" else "Full Model Answer:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Cover / Hide answer toggle
                                TextButton(
                                    onClick = { isAnswerHiddenForSelfTest = !isAnswerHiddenForSelfTest }
                                ) {
                                    Icon(
                                        imageVector = if (isAnswerHiddenForSelfTest) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAnswerHiddenForSelfTest) "Reveal Answer" else "Hide (Self-Test)",
                                        fontSize = 10.sp
                                    )
                                }

                                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Notes", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        if (isAnswerHiddenForSelfTest) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAnswerHiddenForSelfTest = false }
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextSecondary)
                                    Text("Answer is hidden for active recall practice", fontSize = 11.sp, color = TextSecondary)
                                    Text("Tap to reveal & verify your memory", fontSize = 10.sp, color = PrimaryAccentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = question.effectiveAnswer,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Mastery Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onToggleMastery(QuestionMastery.NEEDS_REVIEW) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Needs Practice", fontSize = 11.sp, color = WarningAmber)
                        }

                        Button(
                            onClick = { onToggleMastery(QuestionMastery.MASTERED) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Mastered", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // Expand / Collapse Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Show Less" else "Study Full Solution & Rubric",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryAccentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = PrimaryAccentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MockExamSimulatorTab(
    sprint: ExamSprint,
    onStartMock: (questionCount: Int) -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(sprint.targetTestQuestionsCount) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryAccentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryAccentColor, modifier = Modifier.size(28.dp))
                    }
                }

                Text(
                    text = "Predictive Internal Mock Exam",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Teacher said they will give an assignment of ${sprint.totalQuestions} questions and only pick $selectedCount questions for the test?\n\nThis simulator draws exactly $selectedCount random questions from your pool, puts you under a 60-minute exam timer, and lets you self-evaluate against the examiner rubrics!",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Text(
                    text = "Choose Sample Size for this Mock:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(5, 8, 10).forEach { c ->
                        val isSelected = selectedCount == c
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCount = c },
                            label = { Text("$c Questions", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryAccentColor.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryAccentColor
                            )
                        )
                    }
                }

                Button(
                    onClick = { onStartMock(selectedCount) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start Timed Mock Exam ($selectedCount Qs)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Mock Stats Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${sprint.mockTestsTaken}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Mocks Completed", fontSize = 11.sp, color = TextSecondary)
                }
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderColor))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${sprint.bestScore}/50", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    Text(text = "Best Score", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FlashDrillTab(
    sprint: ExamSprint,
    onUpdateMastery: (questionId: String, QuestionMastery) -> Unit
) {
    if (sprint.questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No questions in sprint", color = TextSecondary)
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val currentQ = sprint.questions[currentIndex.coerceIn(0, sprint.questions.size - 1)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Counter
        Text(
            text = "Flashcard ${currentIndex + 1} of ${sprint.questions.size}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        // Flashcard Box
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFlipped) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else CardSurface
            ),
            border = BorderStroke(1.dp, if (isFlipped) PrimaryAccentColor.copy(alpha = 0.4f) else BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { isFlipped = !isFlipped }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isFlipped) {
                    // Front of Card: Question
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarningAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Question ${currentQ.questionNumber} • ${currentQ.marks} Marks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = currentQ.questionText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "💡 Tap card to reveal answer & scoring rubric",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    // Back of Card: Quick recall & Answer
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scoring Rubric & Model Answer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccentColor
                            )
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        if (currentQ.quickRecall.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WarningAmber.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚡ Hook: ${currentQ.quickRecall}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CardSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = currentQ.effectiveAnswer,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onUpdateMastery(currentQ.id, QuestionMastery.NEEDS_REVIEW)
                    isFlipped = false
                    if (currentIndex < sprint.questions.size - 1) currentIndex++
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Still Learning", color = WarningAmber, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onUpdateMastery(currentQ.id, QuestionMastery.MASTERED)
                    isFlipped = false
                    if (currentIndex < sprint.questions.size - 1) currentIndex++
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mastered!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

