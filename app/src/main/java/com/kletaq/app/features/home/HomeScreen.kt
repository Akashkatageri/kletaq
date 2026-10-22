package com.kletaq.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.data.repository.OvsiankinaRepository
import com.kletaq.app.data.repository.TaskRepository
import com.kletaq.app.features.home.components.CompactStatsRow
import com.kletaq.app.features.home.components.ContinueLearningCard
import com.kletaq.app.features.home.components.DailyTasksSection
import com.kletaq.app.features.home.components.HeaderSection
import com.kletaq.app.features.home.components.ReviewSessionSheet
import com.kletaq.app.features.home.components.ReviewsCard
import com.kletaq.app.features.home.components.StudyWhyCard
import com.kletaq.app.features.home.components.StudyWhyEditorSheet
import com.kletaq.app.features.progress.ProgressViewModel
import com.kletaq.app.features.journey.BacklogPlanViewModel
import com.kletaq.app.domain.backlog.BacklogPlanCalculator
import com.kletaq.app.data.repository.AcademicContentRepository
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onNavigateToJourney: (semesterId: String, subjectId: String, unitId: String, topicId: String) -> Unit = { _, _, _, _ -> },
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLesson: ((lessonNode: com.kletaq.app.features.journey.components.LessonNode, subjectName: String, semesterName: String, durationMinutes: Int, subId: String, semId: String) -> Unit)? = null,
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
    var showStudyWhyEditor by remember { mutableStateOf(false) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var listenerRegistration: ListenerRegistration? = null
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        userProfile = snapshot.toObject(UserProfile::class.java)
                    }
                }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.loadSpacedRepetitionData()

        // Show rationale dialog post-onboarding if permission not granted yet
        if (!com.kletaq.app.core.ui.NotificationPermissionHelper.isPermissionGranted(context)) {
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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

        // 1b. Always-Pinned Personal Study Goal / Reason Card ("Your Reason")
        val studyWhyText = userProfile?.studyWhy ?: ""

        item(key = "study_why") {
            StudyWhyCard(
                studyWhy = studyWhyText,
                onEditClick = { showStudyWhyEditor = true }
            )
        }

        // 2. Streak, Shields, XP & Tasks Compact Row
        item(key = "stats_row") {
            CompactStatsRow(
                streakDays = userStats.effectiveStreak,
                isStreakActiveToday = userStats.isStreakActiveToday,
                shieldsCount = userStats.shieldsRemaining,
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
            val resetTopicKeysSet = remember(userStats.resetTopicKeys) { userStats.resetTopicKeys.toSet() }
            val backlogSubjectsList = userProfile?.backlogSubjects ?: emptyList()

            val syllabusRevision by com.kletaq.app.core.syllabus.CustomSyllabusManager.revision.collectAsState()
            val customSubjectsList = remember(syllabusRevision) {
                com.kletaq.app.core.syllabus.CustomSyllabusManager.getImportedSubjects(context)
            }
            val userSemesters = remember(userSemester, userStats.completedSemesters, completedTopicKeysSet, resetTopicKeysSet, backlogSubjectsList, userProfile?.branch, userProfile?.university, userProfile?.totalSemesters, customSubjectsList, syllabusRevision) {
                com.kletaq.app.data.repository.KletaqAcademicRepository.getSemestersForUser(
                    userSemesterNumber = userSemester,
                    completedSemesters = userStats.completedSemesters,
                    completedTopicKeys = completedTopicKeysSet,
                    resetTopicKeys = resetTopicKeysSet,
                    backlogSubjects = backlogSubjectsList,
                    userBranch = userProfile?.branch.orEmpty(),
                    customSubjects = customSubjectsList,
                    userUniversity = userProfile?.university ?: "VTU",
                    totalSemesters = userProfile?.totalSemesters ?: 8
                )
            }

            // If activePosition references a deleted or nonexistent subject, purge it from Firestore
            LaunchedEffect(activePosition, userSemesters) {
                val pos = activePosition ?: return@LaunchedEffect
                if (userSemesters.isNotEmpty()) {
                    val subjectExists = userSemesters.any { sem ->
                        sem.subjects.any { s ->
                            s.id.equals(pos.subjectId, ignoreCase = true) ||
                            s.name.equals(pos.subjectName, ignoreCase = true) ||
                            s.name.replace("[Backlog] ", "").equals(pos.subjectName.replace("[Backlog] ", ""), ignoreCase = true)
                        }
                    }
                    if (!subjectExists) {
                        OvsiankinaRepository.clearPosition()
                    }
                }
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
                            subText = "${currentActivePlan.studyDaysPerWeek} study days this week",
                            buttonText = "Start ${currentActivePlan.sessionMinutes}-min session",
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
                    // A saved last position can be stale after the student completes that topic or deletes the custom subject.
                    // Only resume it if the subject and topic actually exist in userSemesters and are still unfinished.
                    val validSavedPosition = activePosition?.takeIf { position ->
                        val matchingSubject = userSemesters.flatMap { it.subjects }.find { s ->
                            s.id.equals(position.subjectId, ignoreCase = true) ||
                            s.name.equals(position.subjectName, ignoreCase = true) ||
                            s.name.replace("[Backlog] ", "").equals(position.subjectName.replace("[Backlog] ", ""), ignoreCase = true)
                        } ?: return@takeIf false

                        val topicExists = matchingSubject.units.any { u ->
                            u.lessons.any { l -> l.id.equals(position.topicId, ignoreCase = true) || l.title.equals(position.topicTitle, ignoreCase = true) }
                        }
                        if (!topicExists) return@takeIf false

                        val scopedKey = "${position.semesterId}_${position.subjectId}_${position.topicId}"
                        val isExplicitlyReset = resetTopicKeysSet.any {
                            it.equals(position.topicId, ignoreCase = true) ||
                            it.endsWith("_${position.topicId}", ignoreCase = true) ||
                            it.contains(position.topicId, ignoreCase = true)
                        }
                        isExplicitlyReset || (position.topicId !in completedTopicKeysSet && scopedKey !in completedTopicKeysSet)
                    }

                    if (validSavedPosition != null && validSavedPosition.topicTitle.isNotBlank()) {
                        validSavedPosition
                    } else {
                        val currentSem = userSemesters.lastOrNull { !it.isLocked } ?: userSemesters.firstOrNull()
                        val activeSubj = currentSem?.subjects?.firstOrNull { s -> s.completedCount > 0 && s.completedCount < s.totalCount }
                            ?: currentSem?.subjects?.firstOrNull { s ->
                                s.units.any { u -> u.lessons.any { l -> l.status == com.kletaq.app.features.journey.components.LessonStatus.CURRENT || l.status == com.kletaq.app.features.journey.components.LessonStatus.AVAILABLE } }
                            }
                            ?: currentSem?.subjects?.firstOrNull()

                        // Status labels are for the Journey UI. The Home resume card must use
                        // the persisted completion IDs as its source of truth.
                        val nextUnitAndLesson = activeSubj?.units
                            ?.asSequence()
                            ?.flatMap { unit ->
                                unit.lessons.asSequence().map { lesson -> unit to lesson }
                            }
                            ?.firstOrNull { (unit, lesson) ->
                                val scopedKey = "${currentSem?.id}_${activeSubj.id}_${lesson.id}"
                                val isExplicitlyReset = resetTopicKeysSet.any {
                                    it.equals(lesson.id, ignoreCase = true) ||
                                    it.endsWith("_${lesson.id}", ignoreCase = true) ||
                                    it.contains(lesson.id, ignoreCase = true)
                                }
                                isExplicitlyReset || (lesson.id !in completedTopicKeysSet && scopedKey !in completedTopicKeysSet)
                            }

                        val activeUnit = nextUnitAndLesson?.first
                        val nextLesson = nextUnitAndLesson?.second

                        if (currentSem != null && activeSubj != null && activeUnit != null && nextLesson != null) {
                            com.kletaq.app.domain.model.UnfinishedRoadmapPosition(
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

                val displayTopic = dynamicNextTopic

                if (displayTopic != null) {
                    val calcProgress = if (displayTopic.totalCount > 0) {
                        (displayTopic.completedCount.toFloat() / displayTopic.totalCount.toFloat()).coerceIn(0f, 1f)
                    } else 0.25f

                    val sanitizedSubjectTitle = displayTopic.subjectName
                        .replace("1BMATCS301", "BCS301")
                        .replace("1BCS302", "BCS306A")
                        .replace("1BCOA303", "BCS302")
                        .replace("1BOS304", "BCS303")
                        .replace("1BCS305", "BCS304")
                        .replace("1BCS306", "BCSL305")
                        .replace("Probability, Distributions and Statistics (BCS301)", "Mathematics for Computer Science (BCS301)")

                    ContinueLearningCard(
                        subjectTitle = sanitizedSubjectTitle,
                        topicTitle = displayTopic.topicTitle,
                        progressPercentage = calcProgress,
                        buttonText = if (displayTopic.completedCount == 0) "Start Lesson" else "Resume Lesson",
                        onContinueClick = {
                            onNavigateToJourney(
                                displayTopic.semesterId,
                                displayTopic.subjectId,
                                displayTopic.unitId,
                                displayTopic.topicId
                            )
                        }
                    )
                } else if (userProfile?.university?.equals("VTU", ignoreCase = true) == false) {
                    val currentSem = userSemesters.lastOrNull { !it.isLocked } ?: userSemesters.firstOrNull()
                    ContinueLearningCard(
                        headerTag = "GET STARTED",
                        subjectTitle = userProfile?.branch?.ifBlank { "Curriculum Roadmap" } ?: "Curriculum Roadmap",
                        topicTitle = "Import your syllabus to begin",
                        progressPercentage = 0f,
                        progressText = "No subjects",
                        buttonText = "Import Syllabus",
                        onContinueClick = {
                            onNavigateToJourney(currentSem?.id ?: "sem_1", "import", "", "")
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
                tasksList = tasks,
                onAddTaskClick = onNavigateToCalendar
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

        // 6. Class Timetable Section
        item(key = "class_timetable") {
            com.kletaq.app.features.home.components.ClassTimetableCard()
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showReviewSheet) {
        ReviewSessionSheet(
            dueRevisions = dailyQueue,
            onRecordReview = { topicId, rating ->
                homeViewModel.recordReview(topicId, rating)
            },
            onStartTopicReview = { revision ->
                val imported = com.kletaq.app.core.syllabus.CustomSyllabusManager.getImportedSubjects(context)
                val allSemesters = com.kletaq.app.data.repository.KletaqAcademicRepository.getSemestersForUser(
                    userSemesterNumber = userProfile?.currentSemester ?: 1,
                    customSubjects = imported
                )
                val destination = allSemesters
                    .firstNotNullOfOrNull { semester ->
                        semester.subjects.firstNotNullOfOrNull { subject ->
                            subject.units.firstNotNullOfOrNull { unit ->
                                unit.lessons.firstOrNull { it.id == revision.topicId }?.let {
                                    listOf(semester.id, subject.id, unit.id, it.id)
                                }
                            }
                        }
                    }
                if (destination != null) {
                    showReviewSheet = false
                    onNavigateToJourney(destination[0], destination[1], destination[2], destination[3])
                }
            },
            onFastPathReviewLesson = { revision ->
                val imported = com.kletaq.app.core.syllabus.CustomSyllabusManager.getImportedSubjects(context)
                val allSemesters = com.kletaq.app.data.repository.KletaqAcademicRepository.getSemestersForUser(
                    userSemesterNumber = userProfile?.currentSemester ?: 1,
                    customSubjects = imported
                )
                var foundLesson: com.kletaq.app.features.journey.components.LessonNode? = null
                var foundSubjectName = revision.subjectName
                var foundSemesterName = "Semester ${userProfile?.currentSemester ?: 1}"
                var foundSubId = revision.subjectId
                var foundSemId = ""

                for (sem in allSemesters) {
                    for (subj in sem.subjects) {
                        for (u in subj.units) {
                            val l = u.lessons.find { it.id == revision.topicId }
                            if (l != null) {
                                foundLesson = l
                                foundSubjectName = subj.name
                                foundSemesterName = sem.name
                                foundSubId = subj.id
                                foundSemId = sem.id
                                break
                            }
                        }
                        if (foundLesson != null) break
                    }
                    if (foundLesson != null) break
                }

                if (foundLesson != null && onNavigateToLesson != null) {
                    showReviewSheet = false
                    onNavigateToLesson(foundLesson, foundSubjectName, foundSemesterName, 20, foundSubId, foundSemId)
                }
            },
            onDismiss = {
                showReviewSheet = false
            }
        )
    }

    if (showStudyWhyEditor) {
        StudyWhyEditorSheet(
            initialWhy = userProfile?.studyWhy ?: "",
            onDismiss = { showStudyWhyEditor = false },
            onSave = { why ->
                homeViewModel.updateStudyWhy(why, true)
            },
            onDelete = {
                homeViewModel.deleteStudyWhy()
            }
        )
    }

    if (showPermissionDialog) {
        com.kletaq.app.core.ui.NotificationPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onPermissionResult = { granted ->
                showPermissionDialog = false
            }
        )
    }
}
