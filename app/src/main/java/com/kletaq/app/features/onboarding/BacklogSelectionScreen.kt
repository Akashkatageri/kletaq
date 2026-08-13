package com.kletaq.app.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

import androidx.activity.compose.BackHandler

@Composable
fun BacklogSelectionScreen(
    viewModel: OnboardingViewModel,
    onBacklogsCompleted: () -> Unit,
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    BackHandler { onBackClick() }

    val subjects by viewModel.backlogSubjects.collectAsState()
    val searchQuery by viewModel.backlogSearchQuery.collectAsState()
    val backlogState by viewModel.backlogState.collectAsState()
    val backlogChoice by viewModel.backlogChoice.collectAsState()

    // Track collapse/expand state for Academic Years
    val yearExpandedState = remember { mutableStateMapOf<String, Boolean>() }

    val selectedSemester by viewModel.selectedSemester.collectAsState()

    LaunchedEffect(selectedSemester) {
        if (selectedSemester <= 1) {
            viewModel.clearAllBacklogs()
            onBacklogsCompleted()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.selectedSemester.value > 1) {
            viewModel.ensureBacklogSubjectsLoaded()
        }
    }

    LaunchedEffect(backlogState) {
        if (backlogState is StepUiState.Success) {
            onBacklogsCompleted()
        }
    }

    val filteredSubjects = subjects.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }

    val selectedCount = subjects.count { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        // Top Navigation Bar (Back & Switch Account)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Back",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(8.dp)
            )

            Text(
                text = "Switch Account",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier
                    .clickable {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        onSignOut()
                    }
                    .padding(8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "📌", fontSize = 22.sp)
                    }
                }

                Text(
                    text = "Do you have any backlog subjects?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Select if you have previous semester subjects to clear.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two Choice Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isNoSelected = backlogChoice == BacklogChoice.NO_BACKLOGS
                val noBorderColor by animateColorAsState(
                    targetValue = if (isNoSelected) TextPrimary else BorderColor,
                    label = "noBorder"
                )

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .clickable { viewModel.selectBacklogChoice(BacklogChoice.NO_BACKLOGS) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(if (isNoSelected) 2.dp else 1.dp, noBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✨", fontSize = 18.sp)
                            if (isNoSelected) {
                                Text(text = "✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        Column {
                            Text("No Backlogs", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("Passed all subjects cleanly", fontSize = 9.sp, color = TextSecondary)
                        }
                    }
                }

                val isHasSelected = backlogChoice == BacklogChoice.HAS_BACKLOGS
                val hasBorderColor by animateColorAsState(
                    targetValue = if (isHasSelected) TextPrimary else BorderColor,
                    label = "hasBorder"
                )

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .clickable { viewModel.selectBacklogChoice(BacklogChoice.HAS_BACKLOGS) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(if (isHasSelected) 2.dp else 1.dp, hasBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📌", fontSize = 18.sp)
                            if (isHasSelected) {
                                Text(text = "✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        Column {
                            Text("Yes, I have backlogs", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("Select subjects to clear", fontSize = 9.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Backlog Subject Selector
            AnimatedVisibility(
                visible = backlogChoice == BacklogChoice.HAS_BACKLOGS,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Bar with "Change Choice"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select backlog subjects:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val loadedSemesters = subjects.map { it.semesterNumber }.distinct().sorted()
                            if (loadedSemesters.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.padding(top = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Loaded ${loadedSemesters.size} semesters (${loadedSemesters.joinToString(", ") { "Sem $it" }})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { viewModel.selectBacklogChoice(BacklogChoice.NONE) }) {
                            Text("Change Choice", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Search Box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateBacklogSearch(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search backlog subjects...", fontSize = 12.sp, color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    if (filteredSubjects.isEmpty()) {
                        val currentBranch by viewModel.selectedBranch.collectAsState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unable to load syllabus for ${currentBranch.uppercase()}.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Subjects Grouped by Academic Year & Semester
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val groupedByYear = filteredSubjects.groupBy { it.yearName }
                            val defaultExpanded = groupedByYear.size <= 1

                            groupedByYear.forEach { (yearName, yearItems) ->
                                val isExpanded = yearExpandedState[yearName] ?: defaultExpanded

                                item(key = "year_$yearName") {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { yearExpandedState[yearName] = !isExpanded },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = yearName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = TextPrimary,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = TextPrimary.copy(alpha = 0.1f)
                                                ) {
                                                    Text(
                                                        text = "${yearItems.size} subjects",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (isExpanded) {
                                    val groupedBySem = yearItems.groupBy { it.semesterNumber }
                                    groupedBySem.forEach { (semNum, semItems) ->
                                        item(key = "sem_$semNum") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = TextPrimary.copy(alpha = 0.08f)
                                                ) {
                                                    Text(
                                                        text = "Semester $semNum",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = TextPrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${semItems.size} subjects",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        items(semItems, key = { it.id }) { item ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.toggleBacklogSubject(item.id) },
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, BorderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Checkbox(
                                                            checked = item.isSelected,
                                                            onCheckedChange = { viewModel.toggleBacklogSubject(item.id) },
                                                            colors = CheckboxDefaults.colors(
                                                                checkedColor = TextPrimary,
                                                                uncheckedColor = TextSecondary
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = item.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (backlogChoice == BacklogChoice.HAS_BACKLOGS && selectedCount > 0) {
                    OutlinedButton(
                        onClick = {
                            viewModel.selectBacklogChoice(BacklogChoice.NO_BACKLOGS)
                            viewModel.submitBacklogs()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Clear selection and skip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.submitBacklogs() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    enabled = (backlogChoice == BacklogChoice.NO_BACKLOGS || (backlogChoice == BacklogChoice.HAS_BACKLOGS && selectedCount > 0)) && backlogState !is StepUiState.Loading,
                    elevation = null
                ) {
                    if (backlogState is StepUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        val label = when {
                            backlogChoice == BacklogChoice.NO_BACKLOGS -> "Continue (All Clear)"
                            selectedCount > 0 -> "Continue ($selectedCount selected)"
                            else -> "Continue"
                        }
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
