package com.kletaq.app.features.quest

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.kletaq.app.data.model.LessonQuest
import com.kletaq.app.data.model.QuestSubtopic
import com.kletaq.app.features.home.components.ProgressBar
import com.kletaq.app.features.quest.components.QuestTimeEstimateSheet

@Composable
fun QuestOverviewScreen(
    quest: LessonQuest,
    onBackClick: () -> Unit = {},
    onStartSubtopicFocus: (subtopic: QuestSubtopic, estimatedMinutes: Int) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()
    var selectedSubtopicForSheet by remember { mutableStateOf<QuestSubtopic?>(null) }

    val nextSubtopicToResume = remember(quest) {
        quest.subtopics.firstOrNull { !it.isCompleted } ?: quest.subtopics.firstOrNull()
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
            // 1. Top Bar Navigation
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

                Column {
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. Topic Header & Progress Card
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
                            Text(
                                text = quest.topicTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress: ${quest.completedCount}/${quest.totalCount} completed",
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
                    }
                }

                // 3. YOUR NEXT STEP Priority Card (Atomic Habits & Low Friction)
                if (nextSubtopicToResume != null && !quest.isFullyMastered) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.5.dp, PurpleAccent)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PurpleAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "🎯 YOUR NEXT STEP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PurpleAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "~${nextSubtopicToResume.estimatedMinutes} minutes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = nextSubtopicToResume.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Priority reasons
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = "• High exam importance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                                Text(text = "• Appeared in VTU PYQs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "• Not completed yet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PurpleAccent)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { selectedSubtopicForSheet = nextSubtopicToResume },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PurpleAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "START LEARNING ⚡",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // 4. Questions / Subtopics List Header
                Text(
                    text = "Progress: ${quest.completedCount}/${quest.totalCount} Sections",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Questions to master:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // 5. Interactive Subtopic Item List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quest.subtopics.forEachIndexed { index, subtopic ->
                        val isUnlocked = subtopic.prerequisiteId == null ||
                                quest.subtopics.find { it.id == subtopic.prerequisiteId }?.isCompleted == true ||
                                index == 0

                        val containerBg = when {
                            subtopic.isCompleted -> Color(0xFF10B981).copy(alpha = 0.12f)
                            isUnlocked -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }

                        val borderColor = when {
                            subtopic.isCompleted -> Color(0xFF10B981).copy(alpha = 0.4f)
                            isUnlocked -> PurpleAccent.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isUnlocked) {
                                    selectedSubtopicForSheet = subtopic
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = containerBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 1.dp else 0.dp),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                            subtopic.isCompleted -> Color(0xFF10B981)
                                            isUnlocked -> PurpleAccent.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (subtopic.isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Completed",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else if (!isUnlocked) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Locked",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PurpleAccent
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = subtopic.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (subtopic.isCompleted) FontWeight.SemiBold else FontWeight.Bold,
                                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (!isUnlocked) {
                                            Text(
                                                text = "Requires previous question",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (isUnlocked && !subtopic.isCompleted) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PurpleAccent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "START ⚡",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PurpleAccent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedSubtopicForSheet?.let { subtopic ->
            QuestTimeEstimateSheet(
                subtopicTitle = subtopic.title,
                onDismiss = { selectedSubtopicForSheet = null },
                onTimeSelected = { minutes ->
                    val target = subtopic
                    selectedSubtopicForSheet = null
                    onStartSubtopicFocus(target, minutes)
                }
            )
        }
    }
}
