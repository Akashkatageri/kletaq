package com.studyos.app.features.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.studyos.app.core.theme.InkPaperBorder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.data.repository.StudyOSAcademicRepository
import com.studyos.app.data.repository.TaskRepository
import com.studyos.app.domain.model.StudyTask
import com.studyos.app.features.tasks.components.TaskDetailsSheet

data class SearchFriend(val id: String, val name: String, val handle: String, val status: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onNavigateToSubject: (subjectId: String) -> Unit = {},
    onNavigateToTopic: (semId: String, subjectId: String, unitId: String, topicId: String) -> Unit = { _, _, _, _ -> },
    onNavigateToFriends: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val recentSearches = remember { mutableStateListOf("AVL Trees", "Recursion", "Physics", "Java") }

    val popularTopics = listOf(
        "🔥 Dynamic Programming",
        "🔥 Operating Systems",
        "🔥 Chemistry Formulas"
    )

    val tasks by TaskRepository.tasks.collectAsState()
    var selectedTaskForDetails by remember { mutableStateOf<StudyTask?>(null) }

    // Friends Data
    val friends = remember {
        listOf<SearchFriend>()
    }

    // Comprehensive Academic Data Search
    val semesters = remember { StudyOSAcademicRepository.getSemesters() }

    // 1. Subject Results
    val matchedSubjects = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            semesters.flatMap { sem ->
                sem.subjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }.distinctBy { it.id }
        }
    }

    // 2. Topic Results
    val matchedTopics = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            semesters.flatMap { sem ->
                sem.subjects.flatMap { sub ->
                    sub.units.flatMap { unit ->
                        unit.lessons.filter { lesson ->
                            lesson.title.contains(searchQuery, ignoreCase = true) ||
                            lesson.description.contains(searchQuery, ignoreCase = true)
                        }.map { lesson ->
                            Triple(sem.id, sub.id, unit.id to lesson)
                        }
                    }
                }
            }
        }
    }

    // 3. Task Results
    val matchedTasks = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            tasks.filter { task ->
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.category.label.contains(searchQuery, ignoreCase = true) ||
                task.subjectName?.contains(searchQuery, ignoreCase = true) == true ||
                task.topicTitle?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    // 4. Friend Results
    val matchedFriends = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            friends.filter { friend ->
                friend.name.contains(searchQuery, ignoreCase = true) ||
                friend.handle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val hasResults = matchedSubjects.isNotEmpty() || matchedTopics.isNotEmpty() || matchedTasks.isNotEmpty() || matchedFriends.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- TOP FIXED SEARCH BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search subjects, topics, tasks, friends...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = PurpleAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            )
        }

        // --- SEARCH CONTENT BODY ---
        Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isBlank()) {
                // --- STATE A: EMPTY QUERY (Recent & Popular Topics) ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Recent Searches
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Recent searches",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                if (recentSearches.isNotEmpty()) {
                                    Text(
                                        text = "Clear",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable { recentSearches.clear() }
                                    )
                                }
                            }

                            if (recentSearches.isEmpty()) {
                                Text(
                                    text = "No recent searches.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recentSearches.forEach { term ->
                                        Surface(
                                            modifier = Modifier.clickable {
                                                searchQuery = term
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• $term",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Popular Topics
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Popular topics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                popularTopics.forEach { topic ->
                                    val cleanTopic = topic.replace("🔥 ", "")
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { searchQuery = cleanTopic },
                                        shape = InkPaperBorder.HeavyShape,
                                        border = InkPaperBorder.heavyBorder(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = topic,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!hasResults) {
                // --- STATE C: NO RESULTS FOUND ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PurpleAccent.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🔍 No results found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try searching for subjects, topics, tasks, or friends.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // --- STATE B: ACTIVE SEARCH RESULTS (Categorized) ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Subjects Section
                    if (matchedSubjects.isNotEmpty()) {
                        item {
                            SearchCategoryHeader(title = "📚 Subjects (${matchedSubjects.size})")
                        }
                        items(matchedSubjects, key = { "sub_${it.id}" }) { subject ->
                            SearchResultCard(
                                title = subject.name,
                                subtitle = "${subject.completedCount}/${subject.totalCount} completed • ${subject.units.size} Modules",
                                iconEmoji = subject.iconEmoji,
                                onClick = {
                                    if (!recentSearches.contains(searchQuery)) recentSearches.add(0, searchQuery)
                                    onNavigateToSubject(subject.id)
                                }
                            )
                        }
                    }

                    // 2. Topics Section
                    if (matchedTopics.isNotEmpty()) {
                        item {
                            SearchCategoryHeader(title = "📖 Topics (${matchedTopics.size})")
                        }
                        items(matchedTopics, key = { "top_${it.third.second.id}" }) { (semId, subId, unitAndLesson) ->
                            val (unitId, lesson) = unitAndLesson
                            SearchResultCard(
                                title = lesson.title,
                                subtitle = "${lesson.category.label} • ${lesson.durationMinutes} min",
                                iconEmoji = "📖",
                                onClick = {
                                    if (!recentSearches.contains(searchQuery)) recentSearches.add(0, searchQuery)
                                    onNavigateToTopic(semId, subId, unitId, lesson.id)
                                }
                            )
                        }
                    }

                    // 3. Tasks Section
                    if (matchedTasks.isNotEmpty()) {
                        item {
                            SearchCategoryHeader(title = "✅ Tasks (${matchedTasks.size})")
                        }
                        items(matchedTasks, key = { "task_${it.id}" }) { task ->
                            SearchResultCard(
                                title = task.title,
                                subtitle = "${task.category.emoji} ${task.category.label} • Priority: ${task.priority.label}",
                                iconEmoji = if (task.isCompleted) "☑" else "📋",
                                onClick = {
                                    if (!recentSearches.contains(searchQuery)) recentSearches.add(0, searchQuery)
                                    selectedTaskForDetails = task
                                }
                            )
                        }
                    }

                    // 4. Friends Section
                    if (matchedFriends.isNotEmpty()) {
                        item {
                            SearchCategoryHeader(title = "👥 Friends (${matchedFriends.size})")
                        }
                        items(matchedFriends, key = { "friend_${it.id}" }) { friend ->
                            SearchResultCard(
                                title = friend.name,
                                subtitle = "${friend.handle} • ${friend.status}",
                                iconEmoji = "👤",
                                onClick = {
                                    if (!recentSearches.contains(searchQuery)) recentSearches.add(0, searchQuery)
                                    onNavigateToFriends()
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Task Details Sheet when clicking a task result
    selectedTaskForDetails?.let { task ->
        TaskDetailsSheet(
            task = task,
            onDismiss = { selectedTaskForDetails = null }
        )
    }
}

@Composable
private fun SearchCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = PurpleAccent,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SearchResultCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = InkPaperBorder.HeavyShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = InkPaperBorder.heavyBorder()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = PurpleAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = iconEmoji, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
