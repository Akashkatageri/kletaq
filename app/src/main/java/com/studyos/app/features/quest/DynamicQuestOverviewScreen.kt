package com.studyos.app.features.quest

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.data.model.Confidence
import com.studyos.app.data.model.DynamicTopicQuest
import com.studyos.app.data.model.QuestSection
import com.studyos.app.data.model.QuestSectionType
import com.studyos.app.data.model.SubjectType
import com.studyos.app.features.home.components.ProgressBar
import com.studyos.app.features.quest.components.QuestTimeEstimateSheet

@Composable
fun DynamicQuestOverviewScreen(
    quest: DynamicTopicQuest,
    onBackClick: () -> Unit = {},
    onSkipItem: (section: QuestSection) -> Unit = {},
    onStartSubtopicFocus: (section: QuestSection, estimatedMinutes: Int) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()
    var selectedSectionForSheet by remember { mutableStateOf<QuestSection?>(null) }

    val nextUncompletedSection = remember(quest) {
        quest.sections.firstOrNull { !it.isCompleted && !it.isSkipped }
    }

    val categoryBadgeColor = when (quest.subjectType) {
        SubjectType.MATHEMATICS -> Color(0xFFF59E0B)
        SubjectType.PROGRAMMING -> Color(0xFF6366F1)
        SubjectType.PHYSICS -> Color(0xFF06B6D4)
        SubjectType.CHEMISTRY -> Color(0xFF10B981)
        SubjectType.THEORY -> Color(0xFFEC4899)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // 1. Top Navigation Bar
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
                        text = quest.semesterName.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                    Text(
                        text = quest.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryBadgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = quest.subjectType.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = categoryBadgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. Topic Header & Syllabus Compliance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = quest.iconEmoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = quest.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Difficulty: ${quest.difficulty}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "~${quest.estimatedStudyTime} min",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mastery: ${quest.completedCount}/${quest.totalCount} sections",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(quest.progressPercentage * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }

                        ProgressBar(
                            progress = quest.progressPercentage,
                            height = 8.dp,
                            progressColor = Color(0xFF10B981),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )

                        // 🔒 Zero Hallucination Guarantee & Sources
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🛡️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Syllabus-Safe Guarantee",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = quest.sourcesSummary.ifEmpty { "Sourced strictly from VTU Syllabus & Exam Archives." },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Quick Continue Button
                if (nextUncompletedSection != null && !quest.isFullyMastered) {
                    Button(
                        onClick = { selectedSectionForSheet = nextUncompletedSection },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Continue: ${nextUncompletedSection.title}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 4. Section Header
                Text(
                    text = "Exam-Focused Study Sections:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // 5. Syllabus-Safe Section List with Confidence Badges & PYQ Analytics
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quest.sections.forEachIndexed { index, section ->
                        val isUnlocked = section.prerequisiteId == null ||
                                quest.sections.find { it.id == section.prerequisiteId }?.isCompleted == true ||
                                quest.sections.find { it.id == section.prerequisiteId }?.isSkipped == true ||
                                index == 0

                        val confidenceColor = when (section.confidence) {
                            Confidence.HIGH -> Color(0xFF10B981)   // 🟢 High Confidence
                            Confidence.MEDIUM -> Color(0xFFF59E0B) // 🟡 Medium Confidence
                            Confidence.LOW -> Color(0xFFEF4444)    // 🔴 Low Confidence
                        }

                        val confidenceLabel = when (section.confidence) {
                            Confidence.HIGH -> "🟢 HIGH CONFIDENCE"
                            Confidence.MEDIUM -> "🟡 MEDIUM CONFIDENCE"
                            Confidence.LOW -> "🔴 LOW CONFIDENCE"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isUnlocked) {
                                    selectedSectionForSheet = section
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    section.isCompleted -> Color(0xFF10B981).copy(alpha = 0.12f)
                                    section.isSkipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    isUnlocked -> MaterialTheme.colorScheme.surface
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    section.isCompleted -> Color(0xFF10B981).copy(alpha = 0.4f)
                                    isUnlocked -> confidenceColor.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Title Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when {
                                                section.isCompleted -> Color(0xFF10B981)
                                                section.isSkipped -> MaterialTheme.colorScheme.surfaceVariant
                                                isUnlocked -> confidenceColor.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (section.isCompleted) {
                                                    Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                                                } else if (!isUnlocked) {
                                                    Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                } else {
                                                    Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = confidenceColor)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = section.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (section.isCompleted) FontWeight.SemiBold else FontWeight.Bold,
                                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Confidence Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = confidenceColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = confidenceLabel,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = confidenceColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Badges Row: Source, Importance, PYQ Asked
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(
                                            text = section.sourceType.name.replace("_", " "),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.12f)) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${section.importance}/10", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B))
                                        }
                                    }

                                    if (section.frequencyInPYQs > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981).copy(alpha = 0.12f)) {
                                            Text(
                                                text = "Asked ${section.frequencyInPYQs}x in PYQs",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "~${section.estimatedMinutes}m",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Description
                                if (section.description.isNotEmpty()) {
                                    Text(
                                        text = section.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }

                                // PYQ Question Entries List
                                if (section.pyqEntries.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        section.pyqEntries.forEach { pyq ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("📜", fontSize = 12.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "[VTU ${pyq.year}] ${pyq.question}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Full Source Line
                                Text(
                                    text = "📖 Source: ${section.source}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )

                                // Skip Section Button
                                if (isUnlocked && !section.isCompleted && !section.isSkipped) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { onSkipItem(section) },
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("I know this", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedSectionForSheet?.let { section ->
            QuestTimeEstimateSheet(
                subtopicTitle = section.title,
                onDismiss = { selectedSectionForSheet = null },
                onTimeSelected = { minutes ->
                    val target = section
                    selectedSectionForSheet = null
                    onStartSubtopicFocus(target, minutes)
                }
            )
        }
    }
}
