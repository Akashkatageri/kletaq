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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kletaq.app.core.theme.PurpleAccent
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
import com.kletaq.app.data.model.TopicStudyPack
import com.kletaq.app.data.repository.AcademicContentRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.syllabus.CustomSyllabusManager
import com.kletaq.app.features.journey.components.ImportSyllabusDialog
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.OutlinedButton
import com.kletaq.app.features.journey.components.AddSubjectDialog
import com.kletaq.app.features.journey.components.AddModuleDialog
import com.kletaq.app.features.journey.components.AddTopicNodeDialog
import com.kletaq.app.features.journey.components.UnitJourney
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.style.TextAlign
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

@Composable
fun JourneyScreen(
    userSemesterNumber: Int = 1,
    targetSemesterId: String? = null,
    targetSubjectId: String? = null,
    targetUnitId: String? = null,
    targetTopicId: String? = null,
    onNavigateToLesson: (LessonNode, String, String, Int, String, String) -> Unit,
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
    val resetTopicKeysSet = remember(userStats.resetTopicKeys) { userStats.resetTopicKeys.toSet() }
    val backlogSubjectsList = if (activeSemesterNumber <= 1) emptyList() else (userProfile?.backlogSubjects ?: emptyList())

    var showImportSyllabusDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddModuleDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var selectedUnitForNewTopic by remember { mutableStateOf<UnitJourney?>(null) }
    val customSyllabiRevision by CustomSyllabusManager.revision.collectAsState()
    var subjectToDelete by remember { mutableStateOf<SubjectJourney?>(null) }

    val semesters by produceState<List<SemesterJourney>>(
        initialValue = emptyList(),
        activeSemesterNumber,
        userStats.completedSemesters,
        completedTopicKeysSet,
        resetTopicKeysSet,
        backlogSubjectsList,
        userProfile?.branch,
        userProfile?.university,
        userProfile?.totalSemesters,
        customSyllabiRevision
    ) {
        val imported = CustomSyllabusManager.getImportedSubjects(context)
        value = withContext(Dispatchers.Default) {
            KletaqAcademicRepository.getSemestersForUser(
                userSemesterNumber = activeSemesterNumber,
                completedSemesters = userStats.completedSemesters,
                completedTopicKeys = completedTopicKeysSet,
                resetTopicKeys = resetTopicKeysSet,
                backlogSubjects = backlogSubjectsList,
                userBranch = userProfile?.branch.orEmpty(),
                customSubjects = imported,
                userUniversity = userProfile?.university ?: "VTU",
                totalSemesters = userProfile?.totalSemesters ?: 8
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

    LaunchedEffect(targetSubjectId) {
        if (targetSubjectId == "import") {
            showImportSyllabusDialog = true
        } else if (!targetSubjectId.isNullOrBlank() && currentSemester.subjects.any { it.id == targetSubjectId }) {
            selectedSubjectId = targetSubjectId
        }
    }

    LaunchedEffect(defaultSubjectId) {
        if (selectedSubjectId.isEmpty() || currentSemester.subjects.none { it.id == selectedSubjectId }) {
            selectedSubjectId = defaultSubjectId
        }
    }

    val currentSubjects = currentSemester.subjects

    val activeSubject = currentSubjects.find { it.id == selectedSubjectId }
        ?: currentSubjects.firstOrNull()

    var selectedLessonForSheet by remember { mutableStateOf<LessonNode?>(null) }
    var openedTargetTopicId by remember(targetTopicId) { mutableStateOf<String?>(null) }

    // Review navigation targets a specific topic; open its existing lesson sheet once the
    // semester and subject data have loaded instead of only scrolling to the subject.
    LaunchedEffect(targetTopicId, activeSubject?.id) {
        val topicId = targetTopicId ?: return@LaunchedEffect
        if (openedTargetTopicId == topicId) return@LaunchedEffect

        val targetLesson = activeSubject
            ?.units
            ?.asSequence()
            ?.flatMap { it.lessons.asSequence() }
            ?.firstOrNull { it.id == topicId }

        if (targetLesson != null) {
            selectedLessonForSheet = targetLesson
            openedTargetTopicId = topicId
        }
    }
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

    val activeUnitId = remember(activeSubject, unfinishedPosition) {
        val lastStudiedUnitId = unfinishedPosition?.takeIf { it.subjectId == activeSubject?.id }?.unitId
        if (!lastStudiedUnitId.isNullOrBlank() && activeSubject?.units?.any { it.id == lastStudiedUnitId } == true) {
            lastStudiedUnitId
        } else {
            activeSubject?.units?.firstOrNull { unit ->
                unit.lessons.any { it.status == LessonStatus.CURRENT }
            }?.id ?: activeSubject?.units?.firstOrNull { unit ->
                unit.lessons.any { it.status == LessonStatus.AVAILABLE }
            }?.id ?: activeSubject?.units?.lastOrNull { unit ->
                unit.lessons.any { it.status == LessonStatus.COMPLETED }
            }?.id ?: activeSubject?.units?.firstOrNull()?.id
        }
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

        // Level 2: Subject selection dropdown OR Empty State
        if (currentSubjects.isNotEmpty()) {
            item(key = "subject_selector") {
                SubjectDropdownSelector(
                    subjects = currentSubjects,
                    selectedSubjectId = activeSubject?.id ?: "",
                    onSubjectSelect = { selectedSubjectId = it },
                    onAddSubjectClick = { showAddSubjectDialog = true },
                    onImportSyllabusClick = { showImportSyllabusDialog = true },
                    onDeleteCustomSubjectClick = { subject -> subjectToDelete = subject }
                )
            }
        } else {
            item(key = "empty_syllabus_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "✨", fontSize = 26.sp)
                            }
                        }

                        Text(
                            text = "Semester ${currentSemester.semesterNumber} Roadmap Ready",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Import your syllabus using AI to automatically generate modules, topic nodes, and question banks.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { showAddSubjectDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleAccent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Subject Manually",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showImportSyllabusDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, PurpleAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "✨ Import Syllabus with AI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        }
                    }
                }
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
            val isExpanded = expandedUnitIds[unit.id] ?: isTargetUnit

            item(key = "unit_header_${unit.id}") {
                UnitSectionHeader(
                    unit = unit.copy(isExpanded = isExpanded),
                    onToggleExpand = { expandedUnitIds[unit.id] = !isExpanded }
                )
            }

            if (isExpanded) {
                if (unit.lessons.isEmpty()) {
                    item(key = "empty_unit_${unit.id}") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No topics in ${unit.title} yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = {
                                        selectedUnitForNewTopic = unit
                                        showAddTopicDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PurpleAccent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Topic Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
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

                    if (activeSubject.isCustom) {
                        item(key = "add_node_${unit.id}") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.2.dp, PurpleAccent.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .clickable {
                                        selectedUnitForNewTopic = unit
                                        showAddTopicDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Topic Node",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "unit_spacer_${unit.id}") {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (activeSubject != null && activeSubject.isCustom) {
            item(key = "add_module_${activeSubject.id}") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.2.dp, BorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .clickable { showAddModuleDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleAccent.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add Module ${activeSubject.units.size + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Extend this course with a new syllabus unit",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    var lessonForCelebration by remember { mutableStateOf<LessonNode?>(null) }

    lessonForCelebration?.let { lessonCompleted ->
        com.kletaq.app.features.lesson.LessonCompleteScreen(
            xpEarnedAmount = 80,
            topicId = lessonCompleted.id,
            lessonTitle = lessonCompleted.title,
            showFeedbackOnContinue = false,
            onContinueClick = { lessonForCelebration = null },
            onReviewClick = {
                lessonForCelebration = null
                onNavigateToLesson(
                    lessonCompleted,
                    activeSubject?.name ?: "Engineering Mathematics II",
                    currentSemester.name.ifEmpty { "Semester ${currentSemester.semesterNumber}" },
                    20,
                    activeSubject?.id ?: "",
                    currentSemester.id
                )
            }
        )
    }

    selectedLessonForSheet?.let { rawLesson ->
        val lesson = activeSubject?.units
            ?.asSequence()
            ?.flatMap { it.lessons.asSequence() }
            ?.firstOrNull { it.id == rawLesson.id }
            ?: rawLesson

        val isExplicitlyReset = userStats.resetTopicKeys.any {
            it.equals(lesson.id, ignoreCase = true) ||
            it.endsWith("_${lesson.id}", ignoreCase = true) ||
            it.contains(lesson.id, ignoreCase = true)
        }
        val isNodeActuallyCompleted = !isExplicitlyReset && (
            lesson.status == LessonStatus.COMPLETED ||
            userStats.completedTopicKeys.any {
                it.equals(lesson.id, ignoreCase = true) ||
                it.endsWith("_${lesson.id}", ignoreCase = true) ||
                it.contains(lesson.id, ignoreCase = true)
            }
        )

        LessonBottomSheet(
            lesson = lesson,
            isCompleted = isNodeActuallyCompleted,
            onDismiss = {
                selectedLessonForSheet = null
                openedTargetTopicId = null
            },
            onStartLesson = { lessonToStart, durationMinutes ->
                selectedLessonForSheet = null
                openedTargetTopicId = null
                onNavigateToLesson(
                    lessonToStart,
                    activeSubject?.name ?: "Engineering Mathematics II",
                    currentSemester.name.ifEmpty { "Semester ${currentSemester.semesterNumber}" },
                    durationMinutes,
                    activeSubject?.id ?: "",
                    currentSemester.id
                )
            },
            onStartFocus = { lessonToFocus ->
                selectedLessonForSheet = null
                openedTargetTopicId = null
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
                openedTargetTopicId = null
                val scopedKey = "${currentSemester.id}_${activeSubject?.id}_${lessonToMark.id}"
                val earnedXp = 80L

                // 1. Instant Optimistic Local UI Update
                val updatedResetKeys = userStats.resetTopicKeys.filterNot { key ->
                    key.equals(lessonToMark.id, ignoreCase = true) ||
                    key.equals(scopedKey, ignoreCase = true) ||
                    key.endsWith("_${lessonToMark.id}", ignoreCase = true) ||
                    key.contains(lessonToMark.id, ignoreCase = true)
                }
                val updatedKeys = (userStats.completedTopicKeys + lessonToMark.id + scopedKey).distinct()
                val newTotalXp = userStats.totalXp + earnedXp
                val newLevel = com.kletaq.app.domain.progression.ProgressionCalculator.calculateLevel(newTotalXp)
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val updatedDailyMins = userStats.dailyStudyMinutes.toMutableMap()
                updatedDailyMins[todayKey] = (updatedDailyMins[todayKey] ?: 0) + 25

                optimisticUserStats = userStats.copy(
                    completedTopicKeys = updatedKeys,
                    resetTopicKeys = updatedResetKeys,
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    totalTopicsCompleted = userStats.totalTopicsCompleted + 1,
                    totalStudyMinutes = userStats.totalStudyMinutes + 25,
                    totalFocusMinutes = userStats.totalFocusMinutes + 25,
                    dailyStudyMinutes = updatedDailyMins
                )

                // 2. Background Firestore Synchronization
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val progressionRepo = ProgressionRepositoryImpl(db)

                            progressionRepo.completeTopic(
                                uid = currentUser.uid,
                                topicId = lessonToMark.id,
                                difficulty = TopicDifficulty.EASY,
                                relatedTopicIds = listOf(scopedKey)
                            )

                            activePlan?.let { plan ->
                                if (plan.isActive && plan.subjectId == activeSubject?.id) {
                                    backlogPlanViewModel.markTopicCompletedInPlan(plan.id, lessonToMark.id)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("JourneyScreen", "Error completing topic: ${e.message}")
                        }
                    }
                }

                // 3. Directly open the celebratory complete screen with 3D Panda, confetti, and sound!
                lessonForCelebration = lessonToMark
            },
            onResetNode = { lessonToReset ->
                selectedLessonForSheet = null
                openedTargetTopicId = null
                val scopedKey = "${currentSemester.id}_${activeSubject?.id}_${lessonToReset.id}"
                val cleanSubId = activeSubject?.id?.trim() ?: ""
                val storedScopedKeys = userStats.completedTopicKeys.filter { key ->
                    key.endsWith("_${lessonToReset.id}", ignoreCase = true) ||
                    key.contains(lessonToReset.id, ignoreCase = true) ||
                    (cleanSubId.isNotBlank() && key.contains(cleanSubId, ignoreCase = true) && key.contains(lessonToReset.id, ignoreCase = true))
                }
                val allResetKeys = (listOf(lessonToReset.id, scopedKey) + storedScopedKeys).distinct()
                val xpDeducted = 80L

                // Instant Optimistic Local UI Update: strictly remove from completedTopicKeys and add to resetTopicKeys
                val updatedKeys = userStats.completedTopicKeys.filterNot { key ->
                    allResetKeys.any { resetKey ->
                        key.equals(resetKey, ignoreCase = true) ||
                        key.endsWith("_$resetKey", ignoreCase = true) ||
                        resetKey.endsWith("_$key", ignoreCase = true) ||
                        key.contains(lessonToReset.id, ignoreCase = true)
                    }
                }
                val updatedResetKeys = (userStats.resetTopicKeys + allResetKeys).distinct()
                val newTotalXp = (userStats.totalXp - xpDeducted).coerceAtLeast(0L)
                val newLevel = com.kletaq.app.domain.progression.ProgressionCalculator.calculateLevel(newTotalXp)

                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val updatedDailyMins = userStats.dailyStudyMinutes.toMutableMap()
                val todayMins = ((updatedDailyMins[todayKey] ?: 0) - 25).coerceAtLeast(0)
                if (todayMins > 0) {
                    updatedDailyMins[todayKey] = todayMins
                } else {
                    updatedDailyMins.remove(todayKey)
                }

                optimisticUserStats = userStats.copy(
                    completedTopicKeys = updatedKeys,
                    resetTopicKeys = updatedResetKeys,
                    totalXp = newTotalXp,
                    currentLevel = newLevel,
                    totalTopicsCompleted = (userStats.totalTopicsCompleted - 1).coerceAtLeast(0),
                    totalStudyMinutes = (userStats.totalStudyMinutes - 25).coerceAtLeast(0),
                    totalFocusMinutes = (userStats.totalFocusMinutes - 25).coerceAtLeast(0),
                    dailyStudyMinutes = updatedDailyMins
                )

                // Background Firestore Synchronization
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val progressionRepo = ProgressionRepositoryImpl(db)
                            val spacedRepo = SpacedRepetitionRepositoryImpl(db)
                            spacedRepo.deleteMemoryCard(currentUser.uid, lessonToReset.id)
                            val resetResult = progressionRepo.resetTopic(
                                uid = currentUser.uid,
                                topicId = lessonToReset.id,
                                difficulty = TopicDifficulty.EASY,
                                relatedTopicIds = allResetKeys.filterNot { it == lessonToReset.id }
                            )
                            activePlan?.takeIf { it.subjectId == activeSubject?.id }?.let { plan ->
                                backlogPlanViewModel.unmarkTopicCompletedInPlan(plan.id, lessonToReset.id)
                            }
                            resetResult.onSuccess { canonicalStats ->
                                optimisticUserStats = canonicalStats.copy(
                                    resetTopicKeys = (canonicalStats.resetTopicKeys + updatedResetKeys).distinct(),
                                    completedTopicKeys = canonicalStats.completedTopicKeys.filterNot { key ->
                                        allResetKeys.any { resetKey ->
                                            key.equals(resetKey, ignoreCase = true) || key.contains(lessonToReset.id, ignoreCase = true)
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("JourneyScreen", "Error resetting topic: ${e.message}")
                        }
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

    if (showImportSyllabusDialog) {
        ImportSyllabusDialog(
            currentSemester = currentSemester.semesterNumber,
            totalSemesters = userProfile?.totalSemesters ?: 8,
            onDismiss = { showImportSyllabusDialog = false },
            onSubjectImported = { imported ->
                selectedSubjectId = imported.code.ifBlank { imported.name.replace(" ", "_").uppercase() }
                val targetSem = semesters.find { it.semesterNumber == imported.semesterNumber }
                if (targetSem != null) {
                    selectedSemesterId = targetSem.id
                }
                showImportSyllabusDialog = false
            }
        )
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            initialSemester = currentSemester.semesterNumber,
            totalSemesters = userProfile?.totalSemesters ?: 8,
            onDismiss = { showAddSubjectDialog = false },
            onSubjectCreated = { created ->
                selectedSubjectId = created.code
                val targetSem = semesters.find { it.semesterNumber == created.semesterNumber }
                if (targetSem != null) {
                    selectedSemesterId = targetSem.id
                }
                showAddSubjectDialog = false
            }
        )
    }

    if (showAddModuleDialog && activeSubject != null) {
        AddModuleDialog(
            subjectName = activeSubject.name,
            subjectCode = activeSubject.id,
            nextModuleNumber = activeSubject.units.size + 1,
            onDismiss = { showAddModuleDialog = false },
            onModuleCreated = { created ->
                expandedUnitIds["${activeSubject.id}_M${created.unitNumber}"] = true
                showAddModuleDialog = false
            }
        )
    }

    if (showAddTopicDialog && activeSubject != null && selectedUnitForNewTopic != null) {
        val targetUnit = selectedUnitForNewTopic!!
        AddTopicNodeDialog(
            subjectCode = activeSubject.id,
            unitNumber = targetUnit.unitNumber,
            unitTitle = targetUnit.title,
            onDismiss = {
                showAddTopicDialog = false
                selectedUnitForNewTopic = null
            },
            onTopicCreated = {
                showAddTopicDialog = false
                selectedUnitForNewTopic = null
            }
        )
    }

    if (subjectToDelete != null) {
        val target = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete Custom Roadmap?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.name}\"? This custom subject roadmap and all its units will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = target.id
                        val name = target.name
                        CustomSyllabusManager.deleteSubject(context, code, name)
                        OvsiankinaRepository.unfinishedPosition.value?.let { pos ->
                            if (pos.subjectId.equals(code, ignoreCase = true) || pos.subjectName.equals(name, ignoreCase = true)) {
                                OvsiankinaRepository.clearPosition()
                            }
                        }
                        Toast.makeText(context, "\"$name\" deleted", Toast.LENGTH_SHORT).show()
                        subjectToDelete = null
                        if (selectedSubjectId == code || selectedSubjectId == name) {
                            selectedSubjectId = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { subjectToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
