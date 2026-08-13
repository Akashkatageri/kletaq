package com.kletaq.app.features.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.features.journey.components.Difficulty
import com.kletaq.app.features.journey.components.LessonCategory

data class StudioTopic(
    val id: String,
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val topicType: String, // "Theory", "Lab", "Project"
    val resources: String = "",
    val isPublished: Boolean = true
)

data class StudioLesson(
    val id: String,
    val title: String,
    val content: String,
    val question: String = "",
    val answer: String = "",
    val explanation: String = "",
    val estimatedMinutes: Int = 30
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentStudioScreen(
    onBackClick: () -> Unit = {},
    onPreviewQuest: (topicId: String) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("📚 Content", "🤖 AI Drafts", "⚙️ Settings")

    // State for CRUD topics & lessons
    val topics = remember {
        mutableStateListOf(
            StudioTopic("1BMATCS301_M1_T1", "Introduction to Congruences & Linear Congruences", "Master congruences & linear equations", 40, "Medium", "Theory", "https://vtu.ac.in/syllabus"),
            StudioTopic("1BMATCS301_M1_T2", "The Remainder Theorem & Solving Polynomials", "Understand Chinese Remainder Theorem", 45, "Hard", "Theory", "https://vtu.ac.in/syllabus"),
            StudioTopic("1BCS302_M1_T1", "OOP Principles & Java Syntax Basics", "Understand 4 pillars of OOP", 30, "Easy", "Theory", "https://docs.oracle.com/en/java/"),
            StudioTopic("1BCS306_M1_T1", "Data Structures with C Lab Experiments", "Hands-on C programming lab", 55, "Hard", "Lab", "https://vtu.ac.in/lab-manual")
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var editingTopic by remember { mutableStateOf<StudioTopic?>(null) }
    var editingLesson by remember { mutableStateOf<StudioLesson?>(null) }
    var isAddingTopic by remember { mutableStateOf(false) }

    // Minimal Content Settings state
    var xpPerLesson by remember { mutableStateOf("50") }
    var dailyXpCap by remember { mutableStateOf("500") }
    var defaultLessonDuration by remember { mutableStateOf("30") }
    var publishDraftsAuto by remember { mutableStateOf(false) }
    var maintenanceMode by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KLETAQ CONTENT STUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Manage & Publish Learning Content",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PurpleAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "ADMIN MODE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector Navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PurpleAccent
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == index) PurpleAccent else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body Area
            when (selectedTabIndex) {
                0 -> {
                    // 📚 CONTENT MANAGEMENT TAB
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search topics or codes...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleAccent,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    editingTopic = StudioTopic(
                                        id = "TOPIC_${System.currentTimeMillis() % 10000}",
                                        title = "",
                                        description = "",
                                        estimatedMinutes = 30,
                                        difficulty = "Medium",
                                        topicType = "Theory"
                                    )
                                    isAddingTopic = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Topic", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val filteredTopics = topics.filter {
                            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredTopics, key = { it.id }) { topic ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = topic.id,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PurpleAccent
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (topic.isPublished) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = if (topic.isPublished) "PUBLISHED" else "DRAFT",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (topic.isPublished) Color(0xFF2E7D32) else Color.Gray,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = topic.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )

                                        if (topic.description.isNotBlank()) {
                                            Text(
                                                text = topic.description,
                                                fontSize = 12.sp,
                                                color = TextSecondary,
                                                maxLines = 2
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⏱️ ${topic.estimatedMinutes}m  •  🎯 ${topic.difficulty}  •  🏷️ ${topic.topicType}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { onPreviewQuest(topic.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = PurpleAccent, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        editingTopic = topic
                                                        isAddingTopic = false
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        editingLesson = StudioLesson(
                                                            id = "LESSON_${topic.id}",
                                                            title = topic.title,
                                                            content = "Detailed lesson notes for ${topic.title}",
                                                            question = "What is the core principle of ${topic.title}?",
                                                            answer = "The primary formulation.",
                                                            explanation = "Step-by-step breakdown.",
                                                            estimatedMinutes = topic.estimatedMinutes
                                                        )
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.MenuBook, contentDescription = "Edit Lesson", tint = PurpleAccent, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = { topics.removeIf { it.id == topic.id } },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // 🤖 AI DRAFTS TAB
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PurpleAccent.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleAccent)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI Lesson Draft Generator",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Generate structured topic notes, practice questions, and estimated durations from VTU syllabus topics instantly.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        topics.add(
                                            0,
                                            StudioTopic(
                                                id = "AI_GEN_${System.currentTimeMillis() % 1000}",
                                                title = "AI Draft: Advanced Modular Arithmetic & Cryptography",
                                                description = "Generated draft covering modular inverses & public key basics.",
                                                estimatedMinutes = 35,
                                                difficulty = "Hard",
                                                topicType = "Theory",
                                                isPublished = false
                                            )
                                        )
                                        selectedTabIndex = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("✨ Generate New Draft", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "Pending AI Drafts for Review",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        val draftTopics = topics.filter { !it.isPublished }
                        if (draftTopics.isEmpty()) {
                            Text("No pending drafts for review. Click 'Generate New Draft' above.", fontSize = 12.sp, color = TextSecondary)
                        } else {
                            draftTopics.forEach { draft ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(draft.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(draft.description, fontSize = 11.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = { topics.removeIf { it.id == draft.id } },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Reject", fontSize = 11.sp, color = Color(0xFFD32F2F))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    val idx = topics.indexOfFirst { it.id == draft.id }
                                                    if (idx != -1) {
                                                        topics[idx] = draft.copy(isPublished = true)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Approve & Publish", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ⚙️ CONTENT SETTINGS TAB
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Lesson & XP Parameters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = xpPerLesson,
                                    onValueChange = { xpPerLesson = it },
                                    label = { Text("Base XP per Lesson") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = dailyXpCap,
                                    onValueChange = { dailyXpCap = it },
                                    label = { Text("Daily XP Cap Limit") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = defaultLessonDuration,
                                    onValueChange = { defaultLessonDuration = it },
                                    label = { Text("Default Lesson Duration (Minutes)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("System Controls", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Publish Drafts Automatically", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("Publish AI-generated lessons without manual review step", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = publishDraftsAuto,
                                        onCheckedChange = { publishDraftsAuto = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleAccent)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Maintenance Mode", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFD32F2F))
                                        Text("Lock content updates for students during syllabus maintenance", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = maintenanceMode,
                                        onCheckedChange = { maintenanceMode = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD32F2F))
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onBackClick() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ✍️ TOPIC EDITOR DIALOG
        editingTopic?.let { topic ->
            TopicEditorDialog(
                topic = topic,
                onDismiss = { editingTopic = null },
                onSave = { updated ->
                    val idx = topics.indexOfFirst { it.id == updated.id }
                    if (idx != -1) {
                        topics[idx] = updated
                    } else {
                        topics.add(0, updated)
                    }
                    editingTopic = null
                }
            )
        }

        // 📝 LESSON EDITOR DIALOG
        editingLesson?.let { lesson ->
            LessonEditorDialog(
                lesson = lesson,
                onDismiss = { editingLesson = null },
                onSave = { updated ->
                    editingLesson = null
                }
            )
        }
    }
}

@Composable
fun TopicEditorDialog(
    topic: StudioTopic,
    onDismiss: () -> Unit,
    onSave: (StudioTopic) -> Unit
) {
    var title by remember { mutableStateOf(topic.title) }
    var description by remember { mutableStateOf(topic.description) }
    var estMin by remember { mutableStateOf(topic.estimatedMinutes.toString()) }
    var difficulty by remember { mutableStateOf(topic.difficulty) }
    var topicType by remember { mutableStateOf(topic.topicType) }
    var resources by remember { mutableStateOf(topic.resources) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (topic.title.isBlank()) "Create New Topic" else "Edit Topic Details", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Topic Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estMin,
                        onValueChange = { estMin = it },
                        label = { Text("Est Min") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        label = { Text("Difficulty") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = topicType,
                    onValueChange = { topicType = it },
                    label = { Text("Topic Type (Theory/Lab/Project)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = resources,
                    onValueChange = { resources = it },
                    label = { Text("Resource Link / Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        topic.copy(
                            title = title.ifBlank { "Untitled Topic" },
                            description = description,
                            estimatedMinutes = estMin.toIntOrNull() ?: 30,
                            difficulty = difficulty,
                            topicType = topicType,
                            resources = resources
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Save Topic")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LessonEditorDialog(
    lesson: StudioLesson,
    onDismiss: () -> Unit,
    onSave: (StudioLesson) -> Unit
) {
    var title by remember { mutableStateOf(lesson.title) }
    var content by remember { mutableStateOf(lesson.content) }
    var question by remember { mutableStateOf(lesson.question) }
    var answer by remember { mutableStateOf(lesson.answer) }
    var explanation by remember { mutableStateOf(lesson.explanation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lesson Content Editor", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Lesson Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content Notes / Text Body") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Practice Question") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Answer Key") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        lesson.copy(
                            title = title,
                            content = content,
                            question = question,
                            answer = answer,
                            explanation = explanation
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Save Lesson")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
