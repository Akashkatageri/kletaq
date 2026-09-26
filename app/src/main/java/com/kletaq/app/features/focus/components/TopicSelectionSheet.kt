package com.kletaq.app.features.focus.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.data.repository.KletaqAcademicRepository
import com.kletaq.app.features.journey.components.LessonStatus
import com.kletaq.app.features.journey.components.SemesterJourney
import com.kletaq.app.features.journey.components.SubjectJourney
import kotlinx.coroutines.tasks.await

import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSelectionSheet(
    onDismiss: () -> Unit,
    onTopicSelected: (topicName: String, subjectName: String, semesterName: String) -> Unit,
    userBranch: String = "",
    userSemester: Int = 1,
    completedSemesters: List<Int> = emptyList(),
    completedTopicKeys: Set<String> = emptySet(),
    backlogSubjects: List<String> = emptyList()
) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    var loadedBranch by remember(userBranch) { mutableStateOf(userBranch) }
    var loadedSemester by remember(userSemester) { mutableIntStateOf(userSemester) }

    LaunchedEffect(currentUser, userBranch, userSemester) {
        if (userBranch.isNotBlank()) loadedBranch = userBranch
        if (userSemester > 0) loadedSemester = userSemester

        if ((loadedBranch.isBlank() || loadedSemester <= 0) && currentUser != null) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get().await()
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    if (loadedBranch.isBlank() && profile.branch.isNotBlank()) {
                        loadedBranch = profile.branch
                    }
                    val sem = profile.semester.takeIf { it > 0 } ?: profile.currentSemester.takeIf { it != null && it > 0 } ?: 1
                    if (loadedSemester <= 0) {
                        loadedSemester = sem
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val effectiveSemester = if (loadedSemester <= 0) 1 else loadedSemester
    val effectiveBranch = loadedBranch

    val semesters by produceState<List<SemesterJourney>>(
        initialValue = emptyList(),
        effectiveBranch,
        effectiveSemester,
        completedSemesters,
        completedTopicKeys,
        backlogSubjects
    ) {
        value = withContext(Dispatchers.Default) {
            KletaqAcademicRepository.getSemestersForUser(
                userSemesterNumber = effectiveSemester,
                completedSemesters = completedSemesters,
                completedTopicKeys = completedTopicKeys,
                backlogSubjects = backlogSubjects,
                userBranch = effectiveBranch
            )
        }
    }
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSemester by remember { mutableStateOf<SemesterJourney?>(null) }
    var selectedSubject by remember { mutableStateOf<SubjectJourney?>(null) }
    var semesterMenuExpanded by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var topicMenuExpanded by remember { mutableStateOf(false) }
    val topics = selectedSubject?.units?.flatMap { it.lessons }.orEmpty()

    LaunchedEffect(semesters, effectiveSemester) {
        if (selectedSemester == null && semesters.isNotEmpty()) {
            selectedSemester = semesters.find { it.semesterNumber == effectiveSemester } ?: semesters.lastOrNull()
        }
    }

    LaunchedEffect(Unit) {
        try {
            sheetState.expand()
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📚", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Link Focus Session to a Topic",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose a semester, subject, then the topic you want to study.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FocusDropdown(
                    label = "Semester",
                    value = selectedSemester?.name ?: "Select semester",
                    enabled = semesters.isNotEmpty(),
                    expanded = semesterMenuExpanded,
                    onExpandedChange = { semesterMenuExpanded = it },
                    items = semesters,
                    itemText = { it.name },
                    onItemSelected = { semester ->
                        selectedSemester = semester
                        selectedSubject = null
                        semesterMenuExpanded = false
                    }
                )

                FocusDropdown(
                    label = "Subject",
                    value = selectedSubject?.name ?: "Select subject",
                    enabled = selectedSemester != null,
                    expanded = subjectMenuExpanded,
                    onExpandedChange = { subjectMenuExpanded = it },
                    items = selectedSemester?.subjects.orEmpty(),
                    itemText = { it.name },
                    onItemSelected = { subject ->
                        selectedSubject = subject
                        subjectMenuExpanded = false
                    }
                )

                FocusDropdown(
                    label = "Topic",
                    value = "Select topic",
                    enabled = selectedSubject != null,
                    expanded = topicMenuExpanded,
                    onExpandedChange = { topicMenuExpanded = it },
                    items = topics,
                    itemText = { it.title },
                    trailing = { lesson ->
                        if (lesson.status == LessonStatus.COMPLETED) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onItemSelected = { lesson ->
                        val subject = selectedSubject ?: return@FocusDropdown
                        val semester = selectedSemester ?: return@FocusDropdown
                        onTopicSelected(lesson.title, subject.name, semester.name)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FocusDropdown(
    label: String,
    value: String,
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<T>,
    itemText: (T) -> String,
    onItemSelected: (T) -> Unit,
    trailing: @Composable (T) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) onExpandedChange(it) }
        ) {
            TextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemText(item)) },
                        trailingIcon = { trailing(item) },
                        onClick = { onItemSelected(item) }
                    )
                }
            }
        }
    }
}
