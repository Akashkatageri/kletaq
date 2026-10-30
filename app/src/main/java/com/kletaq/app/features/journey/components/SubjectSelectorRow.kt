package com.kletaq.app.features.journey.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.WarningAmber

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow

// Warm Orange Border & Supportive Backlog Colors
val BacklogOrangeBorder = Color(0xFFF59E0B)  // Warm Amber / Orange
val BacklogRedDot = Color(0xFFEF4444)        // Subtle Red Dot Indicator

@Composable
fun SubjectDropdownSelector(
    subjects: List<SubjectJourney>,
    selectedSubjectId: String,
    onSubjectSelect: (String) -> Unit,
    onAddSubjectClick: (() -> Unit)? = null,
    onImportSyllabusClick: (() -> Unit)? = null,
    onDeleteCustomSubjectClick: ((SubjectJourney) -> Unit)? = null,
    onShareCustomSubjectClick: ((SubjectJourney) -> Unit)? = null,
    onShareSubjectClick: ((SubjectJourney) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember(selectedSubjectId) { mutableStateOf(false) }
    val currentSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.firstOrNull() ?: return
    val cleanName = currentSubject.name.removePrefix("[Backlog]").removePrefix("[backlog]").trim()
    val context = androidx.compose.ui.platform.LocalContext.current
    val daysUntilExam = com.kletaq.app.core.academic.ExamScheduleManager.getDaysUntilExam(context, currentSubject.id)

    val effectiveShareClick = onShareSubjectClick ?: onShareCustomSubjectClick

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
            border = BorderStroke(1.2.dp, PrimaryAccentColor.copy(alpha = 0.40f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryAccentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = currentSubject.iconEmoji, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentSubject.isCustom) "CUSTOM ROADMAP (TAP TO SWITCH)" else "CURRENT SUBJECT (TAP TO SWITCH)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSubject.isCustom) Color(0xFF6366F1) else PrimaryAccentColor
                            )
                            if (daysUntilExam != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (daysUntilExam <= 5) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (daysUntilExam <= 5) "⚠️ Exam in ${daysUntilExam}d" else "🎯 Exam in ${daysUntilExam}d",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (daysUntilExam <= 5) Color(0xFFEF4444) else Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = cleanName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentSubject.completedCount > 0 && currentSubject.totalCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryAccentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${currentSubject.completedCount}/${currentSubject.totalCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Direct Share Button on Subject Card
                    if (effectiveShareClick != null) {
                        IconButton(
                            onClick = { effectiveShareClick(currentSubject) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Subject Roadmap",
                                tint = PrimaryAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Subject",
                        tint = PrimaryAccentColor,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { expanded = !expanded }
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            subjects.forEach { subject ->
                val isSelected = subject.id == selectedSubjectId
                val isBacklog = subject.isBacklog || subject.name.contains("[Backlog]", ignoreCase = true)
                val subjCleanName = subject.name.removePrefix("[Backlog]").removePrefix("[backlog]").trim()

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = subject.iconEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isBacklog) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(BacklogRedDot)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }

                                    Text(
                                        text = subjCleanName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryAccentColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (subject.isCustom) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF6366F1).copy(alpha = 0.12f),
                                            modifier = Modifier.padding(start = 6.dp)
                                        ) {
                                            Text(
                                                text = "CUSTOM",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6366F1),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (subject.totalCount > 0) {
                                    Text(
                                        text = "${subject.completedCount}/${subject.totalCount} topics completed",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = PrimaryAccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Share button for any subject / roadmap
                            if (effectiveShareClick != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        expanded = false
                                        effectiveShareClick(subject)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Roadmap",
                                        tint = if (subject.isCustom) Color(0xFF6366F1) else PrimaryAccentColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // Delete button exclusively for custom syllabus / roadmaps
                            if (subject.isCustom && onDeleteCustomSubjectClick != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        expanded = false
                                        onDeleteCustomSubjectClick(subject)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Custom Roadmap",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onSubjectSelect(subject.id)
                        expanded = false
                    }
                )
            }

            if (onAddSubjectClick != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = PrimaryAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Add Subject Manually ➕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccentColor
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onAddSubjectClick()
                    }
                )
            }

            if (onImportSyllabusClick != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Import Syllabus with AI ⚡",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccentColor
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onImportSyllabusClick()
                    }
                )
            }
        }
    }
}

