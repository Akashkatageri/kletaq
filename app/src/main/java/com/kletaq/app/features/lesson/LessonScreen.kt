package com.kletaq.app.features.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.data.repository.AcademicContentRepository
import com.kletaq.app.data.repository.ProgressionRepositoryImpl
import com.kletaq.app.data.repository.QuestRepositoryImpl
import com.kletaq.app.domain.progression.TopicDifficulty
import com.kletaq.app.features.journey.components.ExamPrep
import com.kletaq.app.features.quest.QuestFocusTimerScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class LessonFlowState {
    LESSON_STUDY,
    FOCUS_TIMER,
    LESSON_CELEBRATION
}

/**
 * Universal Lesson Screen.
 * Hosts the rich, continuous-scroll LessonStudyScreen with direct access to
 * verified concepts, code snippets, model exam answers, quizzes, and integrated focus timer.
 */
@Composable
fun LessonScreen(
    lessonId: String = "pd_01",
    lessonTitle: String = "Partial Differentiation",
    subjectName: String = "Engineering Mathematics II",
    subjectId: String = "",
    semesterName: String = "Semester 2",
    semesterId: String = "",
    scheme: String = "2022_SCHEME",
    examPrep: ExamPrep? = null,
    isReviewMode: Boolean = false,
    customDurationMinutes: Int = 20,
    onBackClick: () -> Unit = {}
) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    val progressionRepository = remember { ProgressionRepositoryImpl(FirebaseFirestore.getInstance()) }
    val questRepository = remember { QuestRepositoryImpl(FirebaseFirestore.getInstance()) }

    var flowState by remember { mutableStateOf(LessonFlowState.LESSON_STUDY) }
    var focusTimerMinutes by remember(customDurationMinutes) { mutableIntStateOf(customDurationMinutes) }
    var lastCompletedOnTime by remember { mutableStateOf(true) }

    // Resolve comprehensive study pack from bundled assets or structured generator
    val studyPack = remember(lessonId, lessonTitle, subjectName) {
        AcademicContentRepository.getStudyPack(
            topicId = lessonId,
            topicTitle = lessonTitle,
            subjectName = subjectName
        )
    }

    fun completeLesson(completedOnTime: Boolean = true) {
        lastCompletedOnTime = completedOnTime
        if (currentUser != null) {
            val uid = currentUser.uid
            val effectiveSubjectId = subjectId.ifBlank { studyPack.subjectCode }
            val relatedKeys = mutableListOf<String>()
            if (semesterId.isNotBlank() && effectiveSubjectId.isNotBlank()) {
                relatedKeys.add("${semesterId}_${effectiveSubjectId}_${lessonId}")
            }
            if (effectiveSubjectId.isNotBlank()) {
                relatedKeys.add("${effectiveSubjectId}_${lessonId}")
            }

            // Application-level coroutine scope to guarantee completion write is NOT cancelled on back pop
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Mark topic fully completed in Firestore
                    progressionRepository.completeTopic(
                        uid = uid,
                        topicId = lessonId,
                        difficulty = TopicDifficulty.MEDIUM,
                        relatedTopicIds = relatedKeys
                    )

                    // Also mark section progress for backward compatibility with quest tracking
                    questRepository.markSectionProgress(
                        uid = uid,
                        topicId = lessonId,
                        sectionId = "${lessonId}_sec_concepts",
                        isCompleted = true,
                        isSkipped = false
                    )
                } catch (e: Exception) {
                    android.util.Log.e("LessonScreen", "Error persisting topic completion: ${e.message}", e)
                }
            }
        }
        flowState = LessonFlowState.LESSON_CELEBRATION
    }

    BackHandler(enabled = flowState != LessonFlowState.LESSON_STUDY) {
        flowState = LessonFlowState.LESSON_STUDY
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (flowState) {
            LessonFlowState.LESSON_STUDY -> {
                LessonStudyScreen(
                    studyPack = studyPack,
                    subjectName = subjectName,
                    semesterName = semesterName,
                    initialEstimatedMinutes = focusTimerMinutes,
                    isCompleted = isReviewMode,
                    onBackClick = onBackClick,
                    onStartFocusTimer = { chosenMinutes ->
                        focusTimerMinutes = chosenMinutes
                        flowState = LessonFlowState.FOCUS_TIMER
                    },
                    onMarkCompleted = {
                        completeLesson(completedOnTime = true)
                    }
                )
            }

            LessonFlowState.FOCUS_TIMER -> {
                QuestFocusTimerScreen(
                    questionTitle = studyPack.title,
                    studyContent = studyPack.technicalConcept.ifBlank { studyPack.simpleConcept },
                    initialMinutes = focusTimerMinutes,
                    onBackClick = { flowState = LessonFlowState.LESSON_STUDY },
                    onFinish = { isCompletedOnTime, _, isSkipped ->
                        if (!isSkipped) {
                            completeLesson(completedOnTime = isCompletedOnTime)
                        } else {
                            flowState = LessonFlowState.LESSON_STUDY
                        }
                    }
                )
            }

            LessonFlowState.LESSON_CELEBRATION -> {
                LessonCompleteScreen(
                    xpEarnedAmount = if (lastCompletedOnTime) 80 else 50,
                    topicId = lessonId,
                    lessonTitle = lessonTitle,
                    onContinueClick = onBackClick,
                    onReviewClick = {
                        flowState = LessonFlowState.LESSON_STUDY
                    }
                )
            }
        }
    }
}
