package com.kletaq.app.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.kletaq.app.data.model.Confidence
import com.kletaq.app.data.model.DynamicTopicQuest
import com.kletaq.app.domain.quest.AcademicQuestGenerator

@Composable
fun AdminQuestReviewScreen(
    questId: String = "pd_01",
    questTitle: String = "Partial Differentiation",
    subjectName: String = "Engineering Mathematics II",
    semesterName: String = "Semester 2",
    onBackClick: () -> Unit = {},
    onApprove: (quest: DynamicTopicQuest) -> Unit = {},
    onReject: () -> Unit = {}
) {
    var quest by remember(questId) {
        mutableStateOf(
            AcademicQuestGenerator.generateQuestForTopic(
                topicId = questId,
                topicTitle = questTitle,
                subjectName = subjectName,
                semesterName = semesterName
            )
        )
    }

    val scrollState = rememberScrollState()

    val highConfidenceCount = quest.sections.count { it.confidence == Confidence.HIGH }
    val mediumConfidenceCount = quest.sections.count { it.confidence == Confidence.MEDIUM }
    val lowConfidenceCount = quest.sections.count { it.confidence == Confidence.LOW }

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
            // Top Bar Navigation
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
                        text = "ADMIN REVIEW DASHBOARD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quality Stats Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🛡️ Zero-Hallucination & Quality Certification",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF10B981).copy(alpha = 0.12f)) {
                                Text(
                                    text = "🟢 HIGH: $highConfidenceCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF59E0B).copy(alpha = 0.12f)) {
                                Text(
                                    text = "🟡 MEDIUM: $mediumConfidenceCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEF4444).copy(alpha = 0.12f)) {
                                Text(
                                    text = "🔴 LOW: $lowConfidenceCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Subject: ${quest.subjectName} (${quest.subjectType.name})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = quest.sourcesSummary,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Sections for Review:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Sections List
                quest.sections.forEachIndexed { idx, section ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${section.title}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (section.confidence) {
                                        Confidence.HIGH -> Color(0xFF10B981).copy(alpha = 0.15f)
                                        Confidence.MEDIUM -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        Confidence.LOW -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = section.confidence.name,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (section.confidence) {
                                            Confidence.HIGH -> Color(0xFF10B981)
                                            Confidence.MEDIUM -> Color(0xFFF59E0B)
                                            Confidence.LOW -> Color(0xFFEF4444)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Source: ${section.source} (${section.sourceType.name})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Importance: ${section.importance}/10 | PYQ Asked: ${section.frequencyInPYQs}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PurpleAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Approve / Reject / Regenerate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        quest = AcademicQuestGenerator.generateQuestForTopic(
                            topicId = questId,
                            topicTitle = questTitle,
                            subjectName = subjectName,
                            semesterName = semesterName
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onApprove(quest) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
