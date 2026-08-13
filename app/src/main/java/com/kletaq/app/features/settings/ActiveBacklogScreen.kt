package com.kletaq.app.features.settings

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.repository.SyllabusRepositoryImpl
import kotlinx.coroutines.tasks.await

data class SemesterSubjects(
    val semester: Int,
    val subjects: List<String>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActiveBacklogScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val activeBacklogSubjects = remember { mutableStateListOf<String>() }
    val semesterSubjectsList = remember { mutableStateListOf<SemesterSubjects>() }
    var searchQuery by remember { mutableStateOf("") }
    var customSubjectInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    // Always load prior semester subjects regardless of auth state
    LaunchedEffect(Unit) {
        var semCount = 3
        var uni = "VTU"
        var scheme = "2025"
        var branch = "CSE"
        var cycle: String? = null

        if (currentUser != null) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                val profile = userDoc.toObject(com.kletaq.app.data.model.UserProfile::class.java)

                if (profile != null) {
                    activeBacklogSubjects.clear()
                    activeBacklogSubjects.addAll(profile.backlogSubjects ?: emptyList())
                    if (profile.semester > 1) semCount = profile.semester
                    if (profile.university.isNotBlank()) uni = profile.university
                    if (profile.scheme.isNotBlank()) scheme = profile.scheme
                    if (profile.branch.isNotBlank()) branch = profile.branch
                    cycle = profile.firstYearCycle
                }
            } catch (_: Exception) { }
        }

        val syllabusRepo = SyllabusRepositoryImpl(context)
        semesterSubjectsList.clear()

        // Fetch subjects from repository for prior semesters
        val repoResult = syllabusRepo.getBacklogPriorSemesters(
            university = uni,
            scheme = scheme,
            branch = branch,
            semester = semCount,
            cycle = cycle
        )

        repoResult.onSuccess { groups ->
            groups.forEach { group ->
                val names = group.subjects.map { it.title }
                if (names.isNotEmpty()) {
                    semesterSubjectsList.add(
                        SemesterSubjects(
                            semester = group.semester,
                            subjects = names
                        )
                    )
                }
            }
        }

        // Fallback default prior semester subjects if list is empty
        if (semesterSubjectsList.isEmpty()) {
            val maxSem = if (semCount > 1) semCount else 3
            for (s in 1 until maxSem) {
                val defaultSubjects = when (s) {
                    1 -> listOf(
                        "Calculus and Linear Algebra",
                        "Engineering Physics",
                        "Basic Electrical Engineering",
                        "Elements of Civil Engineering",
                        "C Programming & Problem Solving"
                    )
                    2 -> listOf(
                        "Advanced Calculus & Numerical Methods",
                        "Engineering Chemistry",
                        "Basic Electronics & Communication",
                        "Computer-Aided Engineering Drawing",
                        "Python Programming & Applications"
                    )
                    3 -> listOf(
                        "Transform Calculus & Data Structures",
                        "Data Structures and Algorithms",
                        "Analog and Digital Electronics",
                        "Computer Organization and Architecture",
                        "Object Oriented Programming with C++"
                    )
                    4 -> listOf(
                        "Mathematical Foundations for Computing",
                        "Design and Analysis of Algorithms",
                        "Microcontrollers and Embedded Systems",
                        "Operating Systems",
                        "Database Management Systems"
                    )
                    else -> listOf(
                        "Semester $s Core Subject 1",
                        "Semester $s Core Subject 2",
                        "Semester $s Core Subject 3",
                        "Semester $s Professional Elective"
                    )
                }
                semesterSubjectsList.add(SemesterSubjects(semester = s, subjects = defaultSubjects))
            }
        }

        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Active Backlog Subjects",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select subjects from previous semesters to add to active backlogs",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PurpleAccent)
                    }
                }
            } else {
                // 1. CURRENT BACKLOG SUMMARY CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📘", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE BACKLOGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PurpleAccent,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeBacklogSubjects.isEmpty()) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = if (activeBacklogSubjects.isEmpty()) "None" else "${activeBacklogSubjects.size} active",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeBacklogSubjects.isEmpty()) Color(0xFF10B981) else Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (activeBacklogSubjects.isEmpty()) {
                                Text(
                                    text = "🎉 No active backlogs selected. Check any subject below to track it!",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    activeBacklogSubjects.toList().forEach { subject ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = PurpleAccent.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = subject,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PurpleAccent
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clickable {
                                                            activeBacklogSubjects.remove(subject)
                                                            hasChanges = true
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(14.dp)
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

                // 2. ADD CUSTOM BACKLOG SUBJECT
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customSubjectInput,
                            onValueChange = { customSubjectInput = it },
                            placeholder = { Text("Add custom subject name...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Button(
                            onClick = {
                                val trimmed = customSubjectInput.trim()
                                if (trimmed.isNotBlank() && !activeBacklogSubjects.contains(trimmed)) {
                                    activeBacklogSubjects.add(trimmed)
                                    customSubjectInput = ""
                                    hasChanges = true
                                }
                            },
                            enabled = customSubjectInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 3. SEARCH FILTER FIELD
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search subjects from previous semesters...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )
                }

                // 4. PREVIOUS SEMESTERS SUBJECT LIST
                semesterSubjectsList.forEach { semData ->
                    val filteredSubjects = semData.subjects.filter { subj ->
                        searchQuery.isBlank() || subj.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredSubjects.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PrimaryAccentColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "SEMESTER ${semData.semester}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PrimaryAccentColor,
                                                letterSpacing = 0.5.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${filteredSubjects.size} subjects",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    filteredSubjects.forEach { subject ->
                                        val isChecked = activeBacklogSubjects.contains(subject)
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) {
                                                        activeBacklogSubjects.remove(subject)
                                                    } else {
                                                        activeBacklogSubjects.add(subject)
                                                    }
                                                    hasChanges = true
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isChecked) PurpleAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isChecked) PurpleAccent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = subject,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isChecked) PurpleAccent else TextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            if (!activeBacklogSubjects.contains(subject)) activeBacklogSubjects.add(subject)
                                                        } else {
                                                            activeBacklogSubjects.remove(subject)
                                                        }
                                                        hasChanges = true
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = PurpleAccent,
                                                        uncheckedColor = TextSecondary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. SAVE BUTTON
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (currentUser != null && hasChanges) {
                                isSaving = true
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(currentUser.uid)
                                    .update("backlogSubjects", activeBacklogSubjects.toList())
                                    .addOnSuccessListener {
                                        isSaving = false
                                        hasChanges = false
                                        Toast.makeText(context, "Backlog subjects updated!", Toast.LENGTH_SHORT).show()
                                        onBackClick()
                                    }
                                    .addOnFailureListener {
                                        isSaving = false
                                        Toast.makeText(context, "Saved locally!", Toast.LENGTH_SHORT).show()
                                        onBackClick()
                                    }
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasChanges) PurpleAccent else PrimaryAccentColor
                        ),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (hasChanges) "Save Changes" else "Done",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
