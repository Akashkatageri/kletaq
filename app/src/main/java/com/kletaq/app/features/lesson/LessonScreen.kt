package com.kletaq.app.features.lesson

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
            id = "${topicId}_sec_pyqs",
            title = "5-mark answer",
            type = QuestSectionType.IMPORTANT_DEFINITIONS,
            source = "Python Programming Syllabus",
            sourceType = SourceType.OFFICIAL_SYLLABUS,
            importance = 10,
            confidence = Confidence.HIGH,
            estimatedMinutes = 12,
            description = fiveMarkAnswer,
            prerequisiteId = "${topicId}_sec_concepts"
        ),
        QuestSection(
            id = "${topicId}_sec_practice",
            title = "Practice questions",
            type = QuestSectionType.PRACTICE_PROBLEMS,
            source = "Python Programming practice",
            sourceType = SourceType.OFFICIAL_SYLLABUS,
            importance = 8,
            confidence = Confidence.MEDIUM,
            estimatedMinutes = 14,
            description = practiceQuestions.joinToString(separator = "\n") { "• $it" },
            prerequisiteId = "${topicId}_sec_pyqs"
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
            prerequisiteId = "${topicId}_sec_practice"
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
    onBackClick: () -> Unit = {}
) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    val questRepository = remember { QuestRepositoryImpl(FirebaseFirestore.getInstance()) }
    val progressionRepository = remember { ProgressionRepositoryImpl(FirebaseFirestore.getInstance()) }
    val userRepository = remember { UserRepositoryImpl(FirebaseFirestore.getInstance()) }

    var flowState by remember { mutableStateOf(LessonFlowState.QUEST_OVERVIEW) }
    var activeSection by remember { mutableStateOf<QuestSection?>(null) }
    var sectionEstimatedMinutes by remember { mutableStateOf(20) }

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

    Column(Modifier.fillMaxSize()) {
    com.kletaq.app.features.chat.TopicChatEntry(lessonId, lessonTitle)
    Box(Modifier.weight(1f)) {
    when (flowState) {
        LessonFlowState.QUEST_OVERVIEW -> {
            DynamicQuestOverviewScreen(
                quest = currentQuest,
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
                    sectionEstimatedMinutes = section.estimatedMinutes
                    flowState = LessonFlowState.LESSON_CONTENT
                }
            )
        }

        LessonFlowState.LESSON_CONTENT -> {
            val section = activeSection ?: currentQuest.sections.first()
            PythonLessonContentScreen(
                section = section,
                onBackClick = { flowState = LessonFlowState.QUEST_OVERVIEW },
                onStartFocusTimer = { flowState = LessonFlowState.FOCUS_TIMER }
            )
        }

        LessonFlowState.FOCUS_TIMER -> {
            val section = activeSection ?: currentQuest.sections.first()

            QuestFocusTimerScreen(
                questionTitle = section.title,
                studyContent = section.description,
                initialMinutes = sectionEstimatedMinutes,
                onFinish = { isCompletedOnTime, _, isSkipped ->
                    if (currentUser != null && !isSkipped) {
                        val updatedSections = currentQuest.sections.map { s ->
                            if (s.id == section.id) s.copy(isCompleted = true) else s
                        }
                        val updatedQuest = currentQuest.copy(sections = updatedSections)
                        quest = updatedQuest

                        val isMastered = updatedQuest.isFullyMastered
                        flowState = if (isMastered) LessonFlowState.LESSON_CELEBRATION else LessonFlowState.QUEST_OVERVIEW

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
