package com.kletaq.app.features.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.data.model.DynamicTopicQuest
import com.kletaq.app.data.model.Confidence
import com.kletaq.app.data.model.QuestSection
import com.kletaq.app.data.model.QuestSectionType
import com.kletaq.app.data.model.SourceType
import com.kletaq.app.data.model.SubjectType
import com.kletaq.app.data.repository.ProgressionRepositoryImpl
import com.kletaq.app.data.repository.QuestRepositoryImpl
import com.kletaq.app.data.repository.UserRepositoryImpl
import com.kletaq.app.domain.progression.TopicDifficulty
import com.kletaq.app.features.quest.DynamicQuestOverviewScreen
import com.kletaq.app.features.quest.QuestFocusTimerScreen
import com.kletaq.app.features.journey.components.ExamPrep
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class LessonFlowState {
    QUEST_OVERVIEW,
    LESSON_CONTENT,
    FOCUS_TIMER,
    LESSON_CELEBRATION
}

/**
 * Python nodes supply their approved exam-prep content directly. This avoids
 * showing an unrelated, previously cached Firestore quest when Start Learning
 * is opened from the Chemistry-cycle Python syllabus.
 */
private fun ExamPrep.toPythonExamPrepQuest(
    topicId: String,
    topicTitle: String,
    subjectName: String,
    semesterName: String,
    scheme: String
): DynamicTopicQuest = DynamicTopicQuest(
    topicId = topicId,
    title = topicTitle,
    subjectName = subjectName,
    semesterName = semesterName,
    scheme = scheme,
    subjectType = SubjectType.PROGRAMMING,
    estimatedStudyTime = 45,
    difficulty = "Medium",
    sourcesSummary = "Python Programming syllabus exam-prep path",
    sections = listOf(
        QuestSection(
            id = "${topicId}_sec_concepts",
            title = "Learn",
            type = QuestSectionType.CONCEPTS,
            source = "Python Programming Syllabus",
            sourceType = SourceType.OFFICIAL_SYLLABUS,
            importance = 9,
            confidence = Confidence.HIGH,
            estimatedMinutes = 12,
            description = learnSummary
        ),
        QuestSection(
            id = "${topicId}_sec_qa",
            title = "Questions & Answers",
            type = QuestSectionType.PRACTICE_PROBLEMS,
            source = "Python Programming Syllabus",
            sourceType = SourceType.OFFICIAL_SYLLABUS,
            importance = 10,
            confidence = Confidence.HIGH,
            estimatedMinutes = 20,
            description = buildString {
                if (practiceQuestions.isNotEmpty()) {
                    append("PRACTICE QUESTIONS:\n")
                    practiceQuestions.forEach { append("• $it\n") }
                    append("\n")
                }
                if (fiveMarkAnswer.isNotBlank()) {
                    append("MODEL ANSWER & KEY POINTS:\n")
                    append(fiveMarkAnswer)
                }
            }.trim(),
            prerequisiteId = "${topicId}_sec_concepts"
        ),
        QuestSection(
            id = "${topicId}_sec_revision",
            title = "Quick revision",
            type = QuestSectionType.COMMON_MISTAKES,
            source = "Python Programming syllabus",
            sourceType = SourceType.OFFICIAL_SYLLABUS,
            importance = 7,
            confidence = Confidence.HIGH,
            estimatedMinutes = 7,
            description = recallPrompt,
            prerequisiteId = "${topicId}_sec_qa"
        )
    )
)

@Composable
fun LessonScreen(
    lessonId: String = "pd_01",
    lessonTitle: String = "Partial Differentiation",
    subjectName: String = "Engineering Mathematics II",
    semesterName: String = "Semester 2",
    scheme: String = "2022_SCHEME",
    examPrep: ExamPrep? = null,
    isReviewMode: Boolean = false,
    customDurationMinutes: Int = 20,
    onBackClick: () -> Unit = {}
) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    val questRepository = remember { QuestRepositoryImpl(FirebaseFirestore.getInstance()) }
    val progressionRepository = remember { ProgressionRepositoryImpl(FirebaseFirestore.getInstance()) }
    val userRepository = remember { UserRepositoryImpl(FirebaseFirestore.getInstance()) }

    var flowState by remember { mutableStateOf(LessonFlowState.QUEST_OVERVIEW) }
    var activeSection by remember { mutableStateOf<QuestSection?>(null) }
    var sectionEstimatedMinutes by remember(customDurationMinutes) { mutableIntStateOf(customDurationMinutes) }
    var sectionCompletionFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sectionCompletionFeedback) {
        if (sectionCompletionFeedback != null) {
            delay(4000L)
            sectionCompletionFeedback = null
        }
    }

    var quest by remember(lessonId, lessonTitle, subjectName, examPrep) {
        mutableStateOf<DynamicTopicQuest?>(null)
    }

    // Load Quest from Global Shared Cache & User Progress from users/{uid}/questProgress/{sectionId}
    LaunchedEffect(currentUser, lessonId, examPrep) {
        val loadedQuest = examPrep?.toPythonExamPrepQuest(
            topicId = lessonId,
            topicTitle = lessonTitle,
            subjectName = subjectName,
            semesterName = semesterName,
            scheme = scheme
        ) ?: questRepository.getOrGenerateQuest(
            topicId = lessonId,
            topicTitle = lessonTitle,
            subjectName = subjectName,
            semesterName = semesterName,
            scheme = scheme
        )

        if (currentUser != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("users")
                    .document(currentUser.uid)
                    .collection("questProgress")
                    .whereEqualTo("topicId", lessonId)
                    .get()
                    .await()

                val completedIds = mutableSetOf<String>()
                val skippedIds = mutableSetOf<String>()

                snapshot.documents.forEach { doc ->
                    val secId = doc.getString("sectionId")
                    if (secId != null) {
                        if (doc.getBoolean("isSkipped") == true) {
                            skippedIds.add(secId)
                        } else if (doc.getBoolean("isCompleted") == true) {
                            completedIds.add(secId)
                        }
                    }
                }

                val updatedSections = loadedQuest.sections.map { section ->
                    when {
                        completedIds.contains(section.id) -> section.copy(isCompleted = true)
                        skippedIds.contains(section.id) -> section.copy(isSkipped = true)
                        else -> section
                    }
                }

                quest = loadedQuest.copy(sections = updatedSections)

                // A completed topic is intentionally reopened for revision. It must show
                // its content, not immediately send the student back to the celebration.
                if (!isReviewMode && quest?.isFullyMastered == true) {
                    flowState = LessonFlowState.LESSON_CELEBRATION
                }
            } catch (_: Exception) {
                quest = loadedQuest
            }
        } else {
            quest = loadedQuest
        }
    }

    val currentQuest = quest ?: return

    BackHandler(enabled = flowState != LessonFlowState.QUEST_OVERVIEW) {
        flowState = LessonFlowState.QUEST_OVERVIEW
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (flowState != LessonFlowState.LESSON_CELEBRATION) {
            com.kletaq.app.features.chat.TopicChatEntry(
                topicId = lessonId,
                title = lessonTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (sectionCompletionFeedback != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sectionCompletionFeedback ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        IconButton(
                            onClick = { sectionCompletionFeedback = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (flowState) {
                LessonFlowState.QUEST_OVERVIEW -> {
                    DynamicQuestOverviewScreen(
                        quest = currentQuest,
                        defaultDurationMinutes = customDurationMinutes,
                        onBackClick = onBackClick,
                        onSkipItem = { section ->
                            if (currentUser != null) {
                                scope.launch {
                                    questRepository.markSectionProgress(
                                        uid = currentUser.uid,
                                        topicId = lessonId,
                                        sectionId = section.id,
                                        isCompleted = false,
                                        isSkipped = true
                                    )

                                    val updatedSections = currentQuest.sections.map { s ->
                                        if (s.id == section.id) s.copy(isSkipped = true) else s
                                    }
                                    val updatedQuest = currentQuest.copy(sections = updatedSections)
                                    quest = updatedQuest

                                    if (updatedQuest.isFullyMastered) {
                                        userRepository.markTopicCompleted(currentUser.uid, lessonId)
                                        progressionRepository.completeTopic(
                                            uid = currentUser.uid,
                                            topicId = lessonId,
                                            difficulty = TopicDifficulty.MEDIUM
                                        )
                                        flowState = LessonFlowState.LESSON_CELEBRATION
                                    }
                                }
                            }
                        },
                        onStartSubtopicFocus = { section, minutes ->
                            activeSection = section
                            sectionEstimatedMinutes = minutes
                            flowState = LessonFlowState.FOCUS_TIMER
                        },
                        onOpenLessonSection = { section ->
                            activeSection = section
                            sectionEstimatedMinutes = customDurationMinutes
                            flowState = LessonFlowState.LESSON_CONTENT
                        }
                    )
                }

                LessonFlowState.LESSON_CONTENT -> {
                    val section = activeSection ?: currentQuest.sections.first()
                    PythonLessonContentScreen(
                        section = section,
                        initialEstimatedMinutes = section.estimatedMinutes.takeIf { it > 0 } ?: 20,
                        onBackClick = { flowState = LessonFlowState.QUEST_OVERVIEW },
                        onStartFocusTimer = { chosenMinutes ->
                            sectionEstimatedMinutes = chosenMinutes
                            flowState = LessonFlowState.FOCUS_TIMER
                        }
                    )
                }

                LessonFlowState.FOCUS_TIMER -> {
                    val section = activeSection ?: currentQuest.sections.first()

                    QuestFocusTimerScreen(
                        questionTitle = section.title,
                        studyContent = section.description,
                        initialMinutes = sectionEstimatedMinutes,
                        onBackClick = { flowState = LessonFlowState.QUEST_OVERVIEW },
                        onFinish = { isCompletedOnTime, _, isSkipped ->
                    if (currentUser != null && !isSkipped) {
                        val updatedSections = currentQuest.sections.map { s ->
                            if (s.id == section.id) s.copy(isCompleted = true) else s
                        }
                        val updatedQuest = currentQuest.copy(sections = updatedSections)
                        quest = updatedQuest

                        val isMastered = updatedQuest.isFullyMastered
                        flowState = if (isMastered) LessonFlowState.LESSON_CELEBRATION else LessonFlowState.QUEST_OVERVIEW
                        if (!isMastered) {
                            sectionCompletionFeedback = if (isCompletedOnTime) {
                                "⚡ Section Complete! +40 XP • Streak Extended 🔥"
                            } else {
                                "✅ Section Complete! +20 XP • Streak Extended 🔥"
                            }
                        }

                        val uid = currentUser.uid
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                questRepository.markSectionProgress(
                                    uid = uid,
                                    topicId = lessonId,
                                    sectionId = section.id,
                                    isCompleted = true,
                                    isSkipped = false
                                )

                                progressionRepository.completeSubtopic(
                                    uid = uid,
                                    subtopicId = section.id,
                                    topicId = lessonId,
                                    isWithinEstimatedTime = isCompletedOnTime
                                )

                                if (isMastered) {
                                    userRepository.markTopicCompleted(uid, lessonId)
                                    progressionRepository.completeTopic(
                                        uid = uid,
                                        topicId = lessonId,
                                        difficulty = TopicDifficulty.MEDIUM
                                    )
                                }
                            } catch (_: Exception) {}
                        }
                    } else {
                        flowState = LessonFlowState.QUEST_OVERVIEW
                    }
                }
            )
        }

        LessonFlowState.LESSON_CELEBRATION -> {
            LessonCompleteScreen(
                xpEarnedAmount = 80,
                topicId = lessonId,
                lessonTitle = lessonTitle,
                onContinueClick = onBackClick,
                onReviewClick = {
                    flowState = LessonFlowState.QUEST_OVERVIEW
                }
            )
        }
    }
    }
    }
}
