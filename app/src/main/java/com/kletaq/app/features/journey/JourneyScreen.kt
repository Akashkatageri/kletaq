package com.kletaq.app.features.journey

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.data.repository.OvsiankinaRepository
import com.kletaq.app.data.repository.ProgressionRepositoryImpl
import com.kletaq.app.data.repository.SpacedRepetitionRepositoryImpl
import com.kletaq.app.data.repository.KletaqAcademicRepository
import com.kletaq.app.data.repository.UserRepositoryImpl
import com.kletaq.app.domain.progression.TopicDifficulty
import com.kletaq.app.features.journey.components.DuolingoPathNode
import com.kletaq.app.features.journey.components.LessonBottomSheet
import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.LessonStatus
import com.kletaq.app.features.journey.components.SemesterHeader
import com.kletaq.app.features.journey.components.SemesterJourney
import com.kletaq.app.features.journey.components.SubjectJourney
import com.kletaq.app.features.journey.components.SubjectDropdownSelector
import com.kletaq.app.features.journey.components.UnitSectionHeader
import com.kletaq.app.features.progress.ProgressViewModel
import com.kletaq.app.features.journey.components.BacklogPlanCard
import com.kletaq.app.features.journey.components.CreateBacklogPlanBottomSheet
import com.kletaq.app.features.journey.BacklogPlanViewModel
import kotlinx.coroutines.launch

@Composable
fun JourneyScreen(
    userSemesterNumber: Int = 1,
    targetSemesterId: String? = null,
    targetSubjectId: String? = null,
    targetUnitId: String? = null,
    targetTopicId: String? = null,
    onNavigateToLesson: (LessonNode, String, String) -> Unit,
    onNavigateToFocus: (LessonNode, String, String) -> Unit = { _, _, _ -> },
    progressViewModel: ProgressViewModel = hiltViewModel(),
    backlogPlanViewModel: BacklogPlanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vmUserStats by progressViewModel.userStats.collectAsState()
    var optimisticUserStats by remember(vmUserStats) { mutableStateOf(vmUserStats) }
    LaunchedEffect(vmUserStats) {
        optimisticUserStats = vmUserStats
    }
    val userStats = optimisticUserStats

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
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

    val activeSemesterNumber = userProfile?.semester?.takeIf { it > 0 }
        ?: userProfile?.currentSemester?.takeIf { it > 0 }
        ?: userSemesterNumber

    val completedTopicKeysSet = remember(userStats.completedTopicKeys) {
        userStats.completedTopicKeys.toSet()
    }
    val backlogSubjectsList = if (activeSemesterNumber <= 1) emptyList() else (userProfile?.backlogSubjects ?: emptyList())

    val semesters by produceState<List<SemesterJourney>>(
        initialValue = emptyList(),
        activeSemesterNumber,
        userStats.completedSemesters,
        completedTopicKeysSet,
        backlogSubjectsList
    ) {
        value = withContext(Dispatchers.Default) {
            KletaqAcademicRepository.getSemestersForUser(
                userSemesterNumber = activeSemesterNumber,
                completedSemesters = userStats.completedSemesters,
                completedTopicKeys = completedTopicKeysSet,
                backlogSubjects = backlogSubjectsList
            )
        }
    }

    val latestUnlockedSemesterId = remember(semesters) {
        semesters.lastOrNull { !it.isLocked }?.id ?: semesters.firstOrNull()?.id ?: ""
    }

    var selectedSemesterId by remember(targetSemesterId, latestUnlockedSemesterId) {
        mutableStateOf(targetSemesterId ?: latestUnlockedSemesterId)
    }

    LaunchedEffect(targetSemesterId, latestUnlockedSemesterId) {
        if (!targetSemesterId.isNullOrBlank()) {
            selectedSemesterId = targetSemesterId
        } else if (semesters.none { it.id == selectedSemesterId }) {
            selectedSemesterId = latestUnlockedSemesterId
        }
    }

    val currentSemester = semesters.find { it.id == selectedSemesterId }
        ?: semesters.lastOrNull { !it.isLocked }
        ?: semesters.firstOrNull()

    if (semesters.isEmpty() || currentSemester == null) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val unfinishedPosition by OvsiankinaRepository.unfinishedPosition.collectAsState()

    val defaultSubjectId = remember(currentSemester, unfinishedPosition, targetSubjectId) {
        if (!targetSubjectId.isNullOrBlank() && currentSemester.subjects.any { it.id == targetSubjectId }) {
            targetSubjectId
        } else if (unfinishedPosition != null && currentSemester.subjects.any { it.id == unfinishedPosition?.subjectId }) {
            unfinishedPosition!!.subjectId
        } else {
            currentSemester.subjects.firstOrNull { s -> s.completedCount > 0 && s.completedCount < s.totalCount }?.id
                ?: currentSemester.subjects.firstOrNull { s ->
                    s.units.any { u -> u.lessons.any { l -> l.status == LessonStatus.CURRENT || l.status == LessonStatus.AVAILABLE } }
                }?.id
                ?: currentSemester.subjects.firstOrNull()?.id
                ?: ""
        }
    }

    var selectedSubjectId by remember {
        mutableStateOf(defaultSubjectId)
    }

    LaunchedEffect(defaultSubjectId) {
        if (selectedSubjectId.isEmpty() || currentSemester.subjects.none { it.id == selectedSubjectId }) {
            selectedSubjectId = defaultSubjectId
        }
    }

    val currentSubjects = currentSemester.subjects

    if (currentSubjects.isEmpty()) {
        Text("No subjects available", modifier = Modifier.padding(16.dp))
        return
    }

    val activeSubject = currentSubjects.find { it.id == selectedSubjectId }
        ?: currentSubjects.firstOrNull()

    var selectedLessonForSheet by remember { mutableStateOf<LessonNode?>(null) }
    val offsets = listOf((-36).dp, 36.dp, 0.dp, (-36).dp, 36.dp)

    val activePlan by backlogPlanViewModel.activePlan.collectAsState()
    var showCreatePlanSheet by remember { mutableStateOf(false) }

    val isSelectedSubjectBacklog = remember(activeSubject, backlogSubjectsList) {
        activeSubject?.isBacklog == true || backlogSubjectsList.any { backlogItem ->
            val subjName = activeSubject?.name?.replace("[Backlog] ", "") ?: ""
            backlogItem.equals(activeSubject?.id, ignoreCase = true) ||
            backlogItem.equals(activeSubject?.name, ignoreCase = true) ||
            backlogItem.equals(subjName, ignoreCase = true) ||
            (subjName.isNotBlank() && subjName.lowercase().contains(backlogItem.lowercase()))
        }
    }

    val activeUnitId = remember(activeSubject) {
        activeSubject?.units?.firstOrNull { unit ->
            unit.lessons.any { it.status == LessonStatus.CURRENT || it.status == LessonStatus.AVAILABLE }
        }?.id ?: activeSubject?.units?.firstOrNull()?.id
    }

    val expandedUnitIds = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 6.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Level 1: Semester selection header
        item(key = "semester_header") {
            val userBranch = userProfile?.branch?.takeIf { it.isNotBlank() } ?: "CSE"
            SemesterHeader(
                semesters = semesters,
                selectedSemesterId = selectedSemesterId,
                onSemesterSelect = { semId ->
                    selectedSemesterId = semId
                    val newSem = semesters.find { it.id == semId }
                    selectedSubjectId = newSem?.subjects?.firstOrNull()?.id ?: ""
                },
                userBranch = userBranch
            )
        }

        // Level 2: Subject selection dropdown
        if (currentSubjects.isNotEmpty()) {
            item(key = "subject_selector") {
                SubjectDropdownSelector(
                    subjects = currentSubjects,
                    selectedSubjectId = activeSubject?.id ?: "",
                    onSubjectSelect = { selectedSubjectId = it }
                )
            }
        }

        if (activeSemesterNumber > 1 && isSelectedSubjectBacklog && activeSubject != null) {
            item(key = "backlog_plan_card_${activeSubject.id}") {
                val matchingPlan = activePlan?.takeIf {
                    it.isActive && (it.subjectId == activeSubject.id ||
                    it.subjectName.equals(activeSubject.name, ignoreCase = true) ||
                    it.subjectName.equals(activeSubject.name.replace("[Backlog] ", ""), ignoreCase = true))
                }
                BacklogPlanCard(
                    subjectName = activeSubject.name.replace("[Backlog] ", ""),
                    activePlan = matchingPlan,
                    onCreatePlanClick = { showCreatePlanSheet = true },
                    onEndPlanClick = {
                        matchingPlan?.let { plan ->
                            backlogPlanViewModel.deactivatePlan(plan.id)
                        }
                    }
                )
            }
        }

        item(key = "spacer_top") {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Level 3 & Level 4: Units and Topic Nodes dynamically rendered with Lazy Recycling
        activeSubject?.units?.forEach { unit ->
            val isTargetUnit = targetUnitId == unit.id || unit.id == activeUnitId
            val isExpanded = expandedUnitIds[unit.id] ?: (unit.isExpanded || isTargetUnit)

            item(key = "unit_header_${unit.id}") {
                UnitSectionHeader(
                    unit = unit.copy(isExpanded = isExpanded),
                    onToggleExpand = { expandedUnitIds[unit.id] = !isExpanded }
                )
            }

            if (isExpanded) {
                items(
                    count = unit.lessons.size,
                    key = { index -> "lesson_${unit.id}_${unit.lessons[index].id}" },
                    contentType = { "lesson_node" }
                ) { unitLessonIndex ->
                    val lesson = unit.lessons[unitLessonIndex]
                    val currentOffset = if (lesson.status == LessonStatus.CURRENT) {
                        0.dp
                    } else {
                        offsets[unitLessonIndex % offsets.size]
                    }

                    DuolingoPathNode(
                        lesson = lesson,
                        currentOffset = currentOffset,
                        onNodeClick = { node ->
                            selectedLessonForSheet = node

                            activeSubject?.let { subject ->
                                OvsiankinaRepository.updateRoadmapPosition(
                                    semesterId = currentSemester.id,
                                    semesterNumber = currentSemester.semesterNumber,
                                    subjectId = subject.id,
                                    subjectName = subject.name,
                                    unitId = unit.id,
                                    unitTitle = unit.title,
                                    topicId = node.id,
                                    topicTitle = node.title,
                                    completedCount = subject.completedCount,
                                    totalCount = subject.totalCount
                                )
                            }
                        }
                    )
                }
            }

            item(key = "unit_spacer_${unit.id}") {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    var lessonForFeedback by remember { mutableStateOf<LessonNode?>(null) }
    var lessonForCelebration by remember { mutableStateOf<LessonNode?>(null) }

    lessonForFeedback?.let { lessonToMark ->
        com.kletaq.app.features.lesson.components.LessonCompletionFeedbackSheet(
            topicTitle = lessonToMark.title,
            onDismiss = {
                lessonForFeedback = null
                lessonForCelebration = lessonToMark
            },
            onSubmitFeedback = { difficulty, confidence, memoryCard ->
                lessonForFeedback = null
                lessonForCelebration = lessonToMark
                val scopedKey = "${currentSemester.id}_${activeSubject?.id}_${lessonToMark.id}"
                val earnedXp = 80L

                // Instant Optimistic Local UI Update
                val updatedKeys = (userStats.completedTopicKeys + lessonToMark.id + scopedKey).distinct()
                val newTotalXp = userStats.totalXp + earnedXp
                val newLevel = com.kletaq.app.domain.progression.ProgressionCalculator.calculateLevel(newTotalXp)
                optimisticUserStats = userStats.copy(
                    completedTopicKeys = updatedKeys,
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    totalTopicsCompleted = userStats.totalTopicsCompleted + 1
                )

                // Background Firestore Synchronization
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val progressionRepo = ProgressionRepositoryImpl(db)
                            val userRepo = UserRepositoryImpl(db)
                            val spacedRepo = SpacedRepetitionRepositoryImpl(db)

                            spacedRepo.saveMemoryCard(currentUser.uid, memoryCard.copy(
                                topicId = lessonToMark.id,
                                subjectId = activeSubject?.id ?: "",
                                subjectName = activeSubject?.name ?: "",
                                topicName = lessonToMark.title,
                                difficulty = difficulty.value,
                                confidence = confidence
                            ))

                            val progDifficulty = TopicDifficulty.valueOf(difficulty.name)
                            progressionRepo.completeTopic(currentUser.uid, scopedKey, progDifficulty)
                            progressionRepo.completeTopic(currentUser.uid, lessonToMark.id, progDifficulty)
                            userRepo.markTopicCompleted(currentUser.uid, scopedKey, 80)
                            userRepo.markTopicCompleted(currentUser.uid, lessonToMark.id, 80)

                            activePlan?.let { plan ->
                                if (plan.isActive) {
                                    backlogPlanViewModel.markTopicCompletedInPlan(plan.id, lessonToMark.id)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        )
    }

    lessonForCelebration?.let { lessonCompleted ->
        com.kletaq.app.features.lesson.LessonCompleteScreen(
            xpEarnedAmount = 80,
            lessonTitle = lessonCompleted.title,
            showFeedbackOnContinue = false,
            onContinueClick = { lessonForCelebration = null },
            onReviewClick = {
                lessonForCelebration = null
                onNavigateToLesson(
                    lessonCompleted,
                    activeSubject?.name ?: "Engineering Mathematics II",
                    currentSemester.name.ifEmpty { "Semester ${currentSemester.semesterNumber}" }
                )
            }
        )
    }

    selectedLessonForSheet?.let { lesson ->
        LessonBottomSheet(
            lesson = lesson,
            onDismiss = { selectedLessonForSheet = null },
            onStartLesson = { lessonToStart ->
                selectedLessonForSheet = null
                onNavigateToLesson(
                    lessonToStart,
                    activeSubject?.name ?: "Engineering Mathematics II",
                    currentSemester.name.ifEmpty { "Semester ${currentSemester.semesterNumber}" }
                )
            },
            onStartFocus = { lessonToFocus ->
                selectedLessonForSheet = null
                onNavigateToFocus(
                    lessonToFocus,
                    activeSubject?.name ?: "Computer Science",
                    "Semester ${currentSemester.semesterNumber}"
                )
            },
            onToggleRevision = { },
            onToggleBookmark = { },
            onMarkComplete = { lessonToMark ->
                selectedLessonForSheet = null
                lessonForFeedback = lessonToMark
            },
            onResetNode = { lessonToReset ->
                selectedLessonForSheet = null
                val scopedKey = "${currentSemester.id}_${activeSubject?.id}_${lessonToReset.id}"
                val xpDeducted = 80L

                // Instant Optimistic Local UI Update
                val updatedKeys = userStats.completedTopicKeys.filterNot { it == lessonToReset.id || it == scopedKey }
                val newTotalXp = (userStats.totalXp - xpDeducted).coerceAtLeast(0L)
                val newLevel = com.kletaq.app.domain.progression.ProgressionCalculator.calculateLevel(newTotalXp)
                optimisticUserStats = userStats.copy(
                    completedTopicKeys = updatedKeys,
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    totalTopicsCompleted = (userStats.totalTopicsCompleted - 1).coerceAtLeast(0)
                )

                // Background Firestore Synchronization
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    scope.launch {
                        val db = FirebaseFirestore.getInstance()
                        val progressionRepo = ProgressionRepositoryImpl(db)
                        progressionRepo.resetTopic(currentUser.uid, scopedKey, TopicDifficulty.MEDIUM)
                        progressionRepo.resetTopic(currentUser.uid, lessonToReset.id, TopicDifficulty.MEDIUM)
                    }
                }
            }
        )
    }

    if (showCreatePlanSheet && activeSubject != null) {
        CreateBacklogPlanBottomSheet(
            subject = activeSubject,
            onDismiss = { showCreatePlanSheet = false },
            onSavePlan = { days, mins, unitIds ->
                showCreatePlanSheet = false
                backlogPlanViewModel.savePlan(
                    subjectId = activeSubject.id,
                    subjectName = activeSubject.name.replace("[Backlog] ", ""),
                    semester = currentSemester.semesterNumber,
                    studyDaysPerWeek = days,
                    sessionMinutes = mins,
                    selectedUnitIds = unitIds
                )
            }
        )
    }
}
