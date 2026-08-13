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
import androidx.compose.ui.text.style.TextOverflow

// Warm Orange Border & Supportive Backlog Colors
val BacklogOrangeBorder = Color(0xFFF59E0B)  // Warm Amber / Orange
val BacklogRedDot = Color(0xFFEF4444)        // Subtle Red Dot Indicator

@Composable
fun SubjectDropdownSelector(
    subjects: List<SubjectJourney>,
    selectedSubjectId: String,
    onSubjectSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.firstOrNull() ?: return
    val cleanName = currentSubject.name.removePrefix("[Backlog]").removePrefix("[backlog]").trim()

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
            border = BorderStroke(1.2.dp, PrimaryAccentColor.copy(alpha = 0.40f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
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
                    modifier = Modifier.weight(1f)
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
                        Text(
                            text = "CURRENT SUBJECT (TAP TO SWITCH)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccentColor
                        )
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
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Subject",
                        tint = PrimaryAccentColor,
                        modifier = Modifier.size(24.dp)
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
                        }
                    },
                    onClick = {
                        onSubjectSelect(subject.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SubjectSelectorRow(
    subjects: List<SubjectJourney>,
    selectedSubjectId: String,
    onSubjectSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(subjects) { subject ->
            val isSelected = subject.id == selectedSubjectId
            val isBacklog = subject.isBacklog || subject.name.contains("[Backlog]", ignoreCase = true)
            val cleanName = subject.name.removePrefix("[Backlog]").removePrefix("[backlog]").trim()

            // 1. Surface colors: Supportive & non-punitive
            val surfaceColor = when {
                isSelected && isBacklog -> BacklogOrangeBorder
                isSelected -> PrimaryAccentColor
                isBacklog -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }

            val contentColor = when {
                isSelected -> Color.White
                else -> MaterialTheme.colorScheme.onSurface
            }

            // 2. Orange Border stroke for Backlogs (never harsh bright red)
            val borderStroke = when {
                isBacklog && !isSelected -> BorderStroke(1.5.dp, BacklogOrangeBorder)
                else -> null
            }

            Surface(
                shape = CircleShape,
                color = surfaceColor,
                contentColor = contentColor,
                border = borderStroke ?: if (!isSelected) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null,
                modifier = Modifier.clickable { onSubjectSelect(subject.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBacklog) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else BacklogRedDot)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(text = subject.iconEmoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = cleanName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )

                    if (subject.completedCount > 0 && subject.totalCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${subject.completedCount}/${subject.totalCount})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
