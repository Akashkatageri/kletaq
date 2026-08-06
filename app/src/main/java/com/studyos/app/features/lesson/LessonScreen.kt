package com.studyos.app.features.lesson

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyos.app.data.model.DynamicTopicQuest
import com.studyos.app.data.model.QuestSection
import com.studyos.app.data.repository.ProgressionRepositoryImpl
import com.studyos.app.data.repository.QuestRepositoryImpl
import com.studyos.app.data.repository.UserRepositoryImpl
import com.studyos.app.domain.progression.TopicDifficulty
import com.studyos.app.features.quest.DynamicQuestOverviewScreen
import com.studyos.app.features.quest.QuestFocusTimerScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class LessonFlowState {
    QUEST_OVERVIEW,
    FOCUS_TIMER,
    LESSON_CELEBRATION
}

@Composable
fun LessonScreen(
    lessonId: String = "pd_01",
    lessonTitle: String = "Partial Differentiation",
    subjectName: String = "Engineering Mathematics II",
    semesterName: String = "Semester 2",
    scheme: String = "2022_SCHEME",
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

    var quest by remember(lessonId, lessonTitle, subjectName) {
        mutableStateOf<DynamicTopicQuest?>(null)
    }

    // Load Quest from Global Shared Cache & User Progress from users/{uid}/questProgress/{sectionId}
    LaunchedEffect(currentUser, lessonId) {
        val loadedQuest = questRepository.getOrGenerateQuest(
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

                if (quest?.isFullyMastered == true) {
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
                }
            )
        }

        LessonFlowState.FOCUS_TIMER -> {
            val section = activeSection ?: currentQuest.sections.first()

            QuestFocusTimerScreen(
                questionTitle = section.title,
                initialMinutes = sectionEstimatedMinutes,
                onFinish = { isCompletedOnTime, _, isSkipped ->
                    if (currentUser != null && !isSkipped) {
                        scope.launch {
                            questRepository.markSectionProgress(
                                uid = currentUser.uid,
                                topicId = lessonId,
                                sectionId = section.id,
                                isCompleted = true,
                                isSkipped = false
                            )

                            progressionRepository.completeSubtopic(
                                uid = currentUser.uid,
                                subtopicId = section.id,
                                topicId = lessonId,
                                isWithinEstimatedTime = isCompletedOnTime
                            )

                            val updatedSections = currentQuest.sections.map { s ->
                                if (s.id == section.id) s.copy(isCompleted = true) else s
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
                            } else {
                                flowState = LessonFlowState.QUEST_OVERVIEW
                            }
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
                lessonTitle = lessonTitle,
                onContinueClick = onBackClick,
                onReviewClick = {
                    flowState = LessonFlowState.QUEST_OVERVIEW
                }
            )
        }
    }
}
