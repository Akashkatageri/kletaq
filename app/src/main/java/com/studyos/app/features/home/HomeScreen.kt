package com.studyos.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyos.app.data.model.UserProfile
import com.studyos.app.data.repository.OvsiankinaRepository
import com.studyos.app.data.repository.TaskRepository
import com.studyos.app.features.home.components.CompactStatsRow
import com.studyos.app.features.home.components.ContinueLearningCard
import com.studyos.app.features.home.components.DailyTasksSection
import com.studyos.app.features.home.components.HeaderSection
import com.studyos.app.features.home.components.ReviewSessionSheet
import com.studyos.app.features.home.components.ReviewsCard
import com.studyos.app.features.journey.BacklogPlanViewModel
import com.studyos.app.domain.backlog.BacklogPlanCalculator

@Composable
fun HomeScreen(
    onNavigateToJourney: (semesterId: String, subjectId: String, unitId: String, topicId: String) -> Unit = { _, _, _, _ -> },
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    progressViewModel: ProgressViewModel = hiltViewModel(),
    backlogPlanViewModel: BacklogPlanViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val userStats by progressViewModel.userStats.collectAsState()
    val unfinishedPosition by OvsiankinaRepository.unfinishedPosition.collectAsState()
    val tasks by TaskRepository.tasks.collectAsState()
    val activeTasksCount = remember(tasks) { tasks.count { !it.isCompleted } }

    val dailyQueue by homeViewModel.dailyQueue.collectAsState()
    var showReviewSheet by remember { mutableStateOf(false) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.loadSpacedRepetitionData()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        userProfile = snapshot.toObject(UserProfile::class.java)
                    }
                }
        }

        // Show rationale dialog post-onboarding if permission not granted yet
        if (!com.studyos.app.core.ui.NotificationPermissionHelper.isPermissionGranted(context)) {
            kotlinx.coroutines.delay(1200)
            showPermissionDialog = true
        }
    }

    val displayName = userProfile?.username?.takeIf { it.isNotBlank() }
        ?: FirebaseAuth.getInstance().currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "Student"

    val currentSem = userProfile?.semester?.takeIf { it > 0 } ?: 2
    val subtitleText = "${userProfile?.branch?.ifBlank { "VTU CSE" }} • Semester $currentSem"
    val nextTopicName = dailyQueue.firstOrNull()?.topicName ?: "No pending reviews 🎉"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. High-Density Header Section (Option A: Linear Minimalist)
        item(key = "header") {
            HeaderSection(
                userName = displayName,
                subtitleText = subtitleText,
                onSearchClick = onNavigateToSearch,
                onNotificationsClick = onNavigateToNotifications,
                onSettingsClick = onNavigateToSettings
            )
        }

        // 2. Streak & XP Compact Row
        item(key = "stats_row") {
            CompactStatsRow(
                streakDays = userStats.studyStreak,
                xpTotal = userStats.totalXp.toInt(),
                activeTasksCount = activeTasksCount
            )
        }

        // 3. Continue Learning Card / Backlog Mission Card
        item(key = "continue_learning") {
            val activePlan by backlogPlanViewModel.activePlan.collectAsState()
            val isPlanLoading by backlogPlanViewModel.isLoading.collectAsState()

            val activePosition = unfinishedPosition
            val userSemester = userProfile?.semester?.takeIf { it > 0 }
                ?: userProfile?.currentSemester?.takeIf { it > 0 }
                ?: 1
            val completedTopicKeysSet = remember(userStats.completedTopicKeys) {
                userStats.completedTopicKeys.toSet()
            }
            val backlogSubjectsList = userProfile?.backlogSubjects ?: emptyList()

            val userSemesters = remember(userSemester, userStats.completedSemesters, completedTopicKeysSet, backlogSubjectsList) {
                com.studyos.app.data.repository.StudyOSAcademicRepository.getSemestersForUser(
                    userSemesterNumber = userSemester,
                    completedSemesters = userStats.completedSemesters,
                    completedTopicKeys = completedTopicKeysSet,
                    backlogSubjects = backlogSubjectsList
                )
            }

            val currentActivePlan = activePlan
            var hasBacklogCardRendered = false

            if (!isPlanLoading && currentActivePlan != null && currentActivePlan.isActive) {
                val matchedSubject = userSemesters.flatMap { it.subjects }.find { s ->
                    s.id == currentActivePlan.subjectId ||
                    s.name.equals(currentActivePlan.subjectName, ignoreCase = true) ||
                    s.name.replace("[Backlog] ", "").equals(currentActivePlan.subjectName, ignoreCase = true)
                }

                if (matchedSubject != null) {
                    val matchingSem = userSemesters.find { sem -> sem.subjects.any { it.id == matchedSubject.id } }
                    val semId = matchingSem?.id ?: ""
                    val nextBacklogTopic = BacklogPlanCalculator.findNextUnfinishedTopic(
                        plan = currentActivePlan,
                        subject = matchedSubject,
                        semesterId = semId,
                        completedTopicKeys = completedTopicKeysSet
                    )
                    val planProgress = BacklogPlanCalculator.calculatePlanProgress(
                        plan = currentActivePlan,
                        subject = matchedSubject,
                        semesterId = semId,
                        completedTopicKeys = completedTopicKeysSet
                    )

                    if (nextBacklogTopic != null) {
                        hasBacklogCardRendered = true
                        ContinueLearningCard(
                            headerTag = "BACKLOG MISSION",
                            subjectTitle = currentActivePlan.subjectName,
                            topicTitle = nextBacklogTopic.topicTitle,
                            progressPercentage = planProgress.progressFraction,
                            progressText = "${planProgress.completedCount}/${planProgress.totalCount} Topics",
                            buttonText = "Start session",
                            onContinueClick = {
                                onNavigateToJourney(
                                    nextBacklogTopic.semesterId,
                                    nextBacklogTopic.subjectId,
                                    nextBacklogTopic.unitId,
                                    nextBacklogTopic.topicId
                                )
                            }
                        )
                    }
                }
            }

            if (!hasBacklogCardRendered) {
                val dynamicNextTopic = remember(userSemesters, activePosition) {
                    if (activePosition != null && activePosition.topicTitle.isNotBlank()) {
                        activePosition
                    } else {
                        val currentSem = userSemesters.lastOrNull { !it.isLocked } ?: userSemesters.firstOrNull()
                        val activeSubj = currentSem?.subjects?.firstOrNull { s -> s.completedCount > 0 && s.completedCount < s.totalCount }
                            ?: currentSem?.subjects?.firstOrNull { s ->
                                s.units.any { u -> u.lessons.any { l -> l.status == com.studyos.app.features.journey.components.LessonStatus.CURRENT || l.status == com.studyos.app.features.journey.components.LessonStatus.AVAILABLE } }
                            }
                            ?: currentSem?.subjects?.firstOrNull()

                        val activeUnit = activeSubj?.units?.firstOrNull { u ->
                            u.lessons.any { l -> l.status == com.studyos.app.features.journey.components.LessonStatus.CURRENT || l.status == com.studyos.app.features.journey.components.LessonStatus.AVAILABLE }
                        } ?: activeSubj?.units?.firstOrNull()

                        val nextLesson = activeUnit?.lessons?.firstOrNull { l -> l.status == com.studyos.app.features.journey.components.LessonStatus.CURRENT || l.status == com.studyos.app.features.journey.components.LessonStatus.AVAILABLE }
                            ?: activeUnit?.lessons?.firstOrNull()

                        if (currentSem != null && activeSubj != null && activeUnit != null && nextLesson != null) {
                            com.studyos.app.domain.model.UnfinishedRoadmapPosition(
                                semesterId = currentSem.id,
                                semesterNumber = currentSem.semesterNumber,
                                subjectId = activeSubj.id,
                                subjectName = activeSubj.name,
                                unitId = activeUnit.id,
                                unitTitle = activeUnit.title,
                                topicId = nextLesson.id,
                                topicTitle = nextLesson.title,
                                completedCount = activeSubj.completedCount,
                                totalCount = activeSubj.totalCount
                            )
                        } else null
                    }
                }

                val displayTopic = dynamicNextTopic ?: activePosition

                if (displayTopic != null) {
                    val calcProgress = if (displayTopic.totalCount > 0) {
                        (displayTopic.completedCount.toFloat() / displayTopic.totalCount.toFloat()).coerceIn(0f, 1f)
                    } else 0.25f

                    ContinueLearningCard(
                        subjectTitle = displayTopic.subjectName,
                        topicTitle = displayTopic.topicTitle,
                        progressPercentage = calcProgress,
                        onContinueClick = {
                            onNavigateToJourney(
                                displayTopic.semesterId,
                                displayTopic.subjectId,
                                displayTopic.unitId,
                                displayTopic.topicId
                            )
                        }
                    )
                } else {
                    ContinueLearningCard(
                        subjectTitle = "Engineering Mathematics II",
                        topicTitle = "Partial Differentiation",
                        progressPercentage = 0.4f,
                        onContinueClick = {
                            onNavigateToJourney("vtu-cse-s2", "BMATE201", "BMATE201_M1", "pd_01")
                        }
                    )
                }
            }
        }

        // 4. Upcoming Tasks Section
        item(key = "upcoming_tasks") {
            DailyTasksSection(
                onAddTaskClick = onNavigateToCreateTask
            )
        }

        // 5. Spaced Repetition Section
        item(key = "spaced_repetition") {
            ReviewsCard(
                dueReviewsCount = dailyQueue.size,
                nextReviewTopic = nextTopicName,
                onStartReviewClick = {
                    showReviewSheet = true
                }
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showReviewSheet) {
        ReviewSessionSheet(
            dueRevisions = dailyQueue,
            onRecordReview = { topicId, rating ->
                homeViewModel.recordReview(topicId, rating)
            },
            onDismiss = {
                showReviewSheet = false
            }
        )
    }

    if (showPermissionDialog) {
        com.studyos.app.core.ui.NotificationPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onPermissionResult = { granted ->
                showPermissionDialog = false
            }
        )
    }
}
