package com.studyos.app.features.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.studyos.app.core.theme.CardSurface
import com.studyos.app.core.theme.PrimaryAccentColor
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.core.theme.SuccessGreen
import com.studyos.app.core.theme.TextPrimary
import com.studyos.app.core.theme.TextSecondary
import com.studyos.app.data.repository.SyllabusRepositoryImpl
import kotlinx.coroutines.tasks.await

data class ArchivedSemesterData(
    val semesterNumber: Int,
    val subjects: List<String> = emptyList(),
    val topicsCompleted: Int = 0,
    val totalTopics: Int = 0,
    val xpEarned: Long = 0L
)

@Composable
fun ArchivedSemestersScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val archivedSemesters = remember { mutableStateListOf<ArchivedSemesterData>() }
    var isLoading by remember { mutableStateOf(true) }
    val expandedSemesters = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(Unit) {
        var currentSem = 3
        var uni = "VTU"
        var scheme = "2025"
        var branch = "CSE"
        var cycle: String? = null

        if (currentUser != null) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                val profile = userDoc.toObject(com.studyos.app.data.model.UserProfile::class.java)

                if (profile != null) {
                    currentSem = if (profile.semester > 1) profile.semester else 3
                    if (profile.university.isNotBlank()) uni = profile.university
                    if (profile.scheme.isNotBlank()) scheme = profile.scheme
                    if (profile.branch.isNotBlank()) branch = profile.branch
                    cycle = profile.firstYearCycle
                }
            } catch (_: Exception) { }
        }

        if (currentSem <= 1) {
            currentSem = 3
        }

        val syllabusRepo = SyllabusRepositoryImpl(context)
        val priorGroupsResult = syllabusRepo.getBacklogPriorSemesters(
            university = uni,
            scheme = scheme,
            branch = branch,
            semester = currentSem,
            cycle = cycle
        )

        val priorSubjectsMap = mutableMapOf<Int, List<String>>()
        priorGroupsResult.onSuccess { groups ->
            groups.forEach { group ->
                priorSubjectsMap[group.semester] = group.subjects.map { it.title }
            }
        }

        val archiveList = mutableListOf<ArchivedSemesterData>()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        for (sem in 1 until currentSem) {
            var subjects = priorSubjectsMap[sem] ?: emptyList()

            if (subjects.isEmpty()) {
                subjects = when (sem) {
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
                    else -> listOf(
                        "Semester $sem Core Subject 1",
                        "Semester $sem Core Subject 2",
                        "Semester $sem Core Subject 3",
                        "Semester $sem Professional Elective"
                    )
                }
            }

            // Strictly read stored semester XP from semesterStats/{uid}_sem{sem}
            // Do NOT grant or calculate XP from imported subjects or completion status alone!
            var storedSemesterXp = 0L
            var storedTopicsCompleted = 0

            if (currentUser != null) {
                try {
                    val semStatDoc = db.collection("semesterStats").document("${currentUser.uid}_sem${sem}").get().await()
                    if (semStatDoc.exists()) {
                        storedSemesterXp = semStatDoc.getLong("archivedXp") ?: 0L
                        storedTopicsCompleted = semStatDoc.getLong("archivedTopics")?.toInt() ?: 0
                    }
                } catch (_: Exception) { }
            }

            val semTopicsCount = subjects.size * 5

            archiveList.add(
                ArchivedSemesterData(
                    semesterNumber = sem,
                    subjects = subjects,
                    topicsCompleted = storedTopicsCompleted,
                    totalTopics = semTopicsCount,
                    xpEarned = storedSemesterXp
                )
            )

            expandedSemesters[sem] = true
        }

        archivedSemesters.clear()
        archivedSemesters.addAll(archiveList)
        isLoading = false
    }

    val overallTotalXp = archivedSemesters.sumOf { it.xpEarned }
    val overallTotalSubjects = archivedSemesters.sumOf { it.subjects.size }

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
                            text = "Archived Semesters",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "View past completed semesters & earned XP",
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
                // OVERALL STATS SUMMARY CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PurpleAccent.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${archivedSemesters.size}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PurpleAccent
                                )
                                Text(
                                    text = "Semesters",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$overallTotalXp",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen
                                )
                                Text(
                                    text = "Earned XP",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$overallTotalSubjects",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryAccentColor
                                )
                                Text(
                                    text = "Subjects",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // ARCHIVED SEMESTER CARDS
                items(archivedSemesters) { semData ->
                    val isExpanded = expandedSemesters[semData.semesterNumber] ?: false

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSemesters[semData.semesterNumber] = !isExpanded
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PurpleAccent.copy(alpha = 0.12f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = null,
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Semester ${semData.semesterNumber}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${semData.subjects.size} subjects • ${semData.xpEarned} XP earned",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            if (isExpanded) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "SUBJECTS IN SEMESTER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextSecondary,
                                            letterSpacing = 0.5.sp
                                        )

                                        semData.subjects.forEachIndexed { index, subject ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = SuccessGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = subject,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = SuccessGreen.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "Archived",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SuccessGreen,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
