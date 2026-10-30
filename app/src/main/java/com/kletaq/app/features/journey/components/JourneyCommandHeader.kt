package com.kletaq.app.features.journey.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen

/**
 * Consolidated Hero Transit Command Deck.
 * Combines Semester Lines, Subject Route with dual progress gauge,
 * exam countdown, Model Paper launcher, and AI Import into one space-efficient card.
 */
@Composable
fun JourneyCommandHeader(
    semesters: List<SemesterJourney>,
    selectedSemesterId: String,
    onSemesterSelect: (String) -> Unit,
    currentSubjects: List<SubjectJourney>,
    activeSubject: SubjectJourney?,
    onSubjectSelect: (String) -> Unit,
    onOpenModelPaper: () -> Unit,
    onImportSyllabusClick: () -> Unit,
    onAddSubjectClick: () -> Unit,
    onShareSubjectClick: (SubjectJourney) -> Unit,
    onDeleteSubjectClick: (SubjectJourney) -> Unit,
    onToggleSubjectType: (SubjectType) -> Unit = {},
    userBranch: String = "CSE",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }

    val selectedSemester = semesters.find { it.id == selectedSemesterId }
        ?: semesters.lastOrNull { !it.isLocked }
        ?: semesters.firstOrNull()

    var subjectMenuExpanded by remember { mutableStateOf(false) }

    val daysUntilExam = remember(activeSubject?.id) {
        if (activeSubject != null) {
            com.kletaq.app.core.academic.ExamScheduleManager.getDaysUntilExam(context, activeSubject.id)
        } else null
    }

    val cleanBranch = userBranch.trim()
    val branchLabel = when {
        cleanBranch.isBlank() -> "CSE"
        cleanBranch.equals("Computer Science & Engineering", ignoreCase = true) || cleanBranch.equals("Computer Science", ignoreCase = true) -> "CSE"
        cleanBranch.equals("Information Science & Engineering", ignoreCase = true) || cleanBranch.equals("Information Science", ignoreCase = true) -> "ISE"
        cleanBranch.equals("Electronics & Communication", ignoreCase = true) || cleanBranch.equals("Electronics & Communication Engineering", ignoreCase = true) -> "ECE"
        cleanBranch.equals("Electrical & Electronics", ignoreCase = true) || cleanBranch.equals("Electrical & Electronics Engineering", ignoreCase = true) -> "EEE"
        cleanBranch.equals("Mechanical Engineering", ignoreCase = true) || cleanBranch.equals("Mechanical", ignoreCase = true) -> "ME"
        cleanBranch.equals("Civil Engineering", ignoreCase = true) || cleanBranch.equals("Civil", ignoreCase = true) -> "CIV"
        cleanBranch.equals("Artificial Intelligence & Machine Learning", ignoreCase = true) || cleanBranch.equals("AI & ML", ignoreCase = true) -> "AIML"
        cleanBranch.length <= 6 -> cleanBranch.uppercase()
        else -> cleanBranch.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── 1. SEMESTER LINE CHIPS ROW ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(semesters) { semester ->
                    val isSelected = semester.id == selectedSemesterId
                    val isLocked = semester.isLocked
                    val semCleared = semester.completedSubjectsCount >= semester.subjectCount && semester.subjectCount > 0

                    val chipLabel = when {
                        isLocked -> "Sem ${semester.semesterNumber} 🔒"
                        semCleared -> "Sem ${semester.semesterNumber} ✓"
                        else -> "Sem ${semester.semesterNumber}"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isSelected -> PurpleAccent
                            isLocked -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                            semCleared -> SuccessGreen.copy(alpha = 0.15f)
                            else -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color(0xFFF1F5F9)
                        },
                        contentColor = when {
                            isSelected -> Color.White
                            isLocked -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            semCleared -> SuccessGreen
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        border = when {
                            isSelected -> null
                            semCleared -> BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                            else -> BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        },
                        modifier = Modifier.clickable {
                            if (!isLocked) {
                                onSemesterSelect(semester.id)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Complete Semester ${semester.semesterNumber - 1} to unlock Semester ${semester.semesterNumber}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(
                            text = chipLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Branch Tag
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Text(
                    text = "$branchLabel Branch",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        // ── 2. HERO COURSE DECK (SUBJECT CARD + PROGRESS GAUGE) ──────────────
        if (activeSubject != null) {
            val totalStations = activeSubject.totalCount
            val completedStations = activeSubject.completedCount
            val progressFraction = if (totalStations > 0) completedStations.toFloat() / totalStations.toFloat() else 0f
            val percentInt = (progressFraction * 100).toInt()
            val cleanSubjectName = activeSubject.name.removePrefix("[Backlog]").removePrefix("[backlog]").trim()

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF11192A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)
                ),
                border = BorderStroke(1.2.dp, if (isDark) Color(0xFF1E2D4A) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Top Row: Circular Gauge + Subject Info + Dropdown Arrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // High-Precision Completion Circular Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(52.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(52.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                strokeWidth = 4.5.dp,
                                strokeCap = StrokeCap.Round
                            )
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.size(52.dp),
                                color = if (percentInt >= 100) SuccessGreen else PurpleAccent,
                                strokeWidth = 4.5.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Text(
                                text = "$percentInt%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Subject Title, Code & Progress Stats (Clickable to switch subject)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { subjectMenuExpanded = !subjectMenuExpanded }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeSubject.iconEmoji,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeSubject.isCustom) "CUSTOM" else "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp,
                                    color = if (activeSubject.isCustom) Color(0xFF06B6D4) else PurpleAccent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (activeSubject.subjectType == SubjectType.LAB) Color(0xFF10B981).copy(alpha = 0.2f) else PurpleAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (activeSubject.subjectType == SubjectType.LAB) "🧪 LAB" else "📖 THEORY",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeSubject.subjectType == SubjectType.LAB) Color(0xFF10B981) else PurpleAccent,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activeSubject.id.isNotBlank()) "· ${activeSubject.id}" else "",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cleanSubjectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (subjectMenuExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Subject",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "$completedStations of $totalStations Topics Completed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Finals Countdown Badge
                        if (daysUntilExam != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (daysUntilExam <= 5) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, if (daysUntilExam <= 5) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (daysUntilExam <= 5) "⚠️ Finals ${daysUntilExam}d" else "🎯 Finals ${daysUntilExam}d",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (daysUntilExam <= 5) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Dropdown Menu to switch subjects or manage custom subjects
                    DropdownMenu(
                        expanded = subjectMenuExpanded,
                        onDismissRequest = { subjectMenuExpanded = false }
                    ) {
                        currentSubjects.forEach { subj ->
                            val isSelected = subj.id == activeSubject.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = subj.iconEmoji, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = subj.name.removePrefix("[Backlog]").trim(),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${subj.completedCount}/${subj.totalCount} Topics",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    subjectMenuExpanded = false
                                    onSubjectSelect(subj.id)
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Action 1: Add Subject Manually
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Subject Manually", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                subjectMenuExpanded = false
                                onAddSubjectClick()
                            }
                        )

                        // Action 2: Import Syllabus with AI
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Import Syllabus with AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                subjectMenuExpanded = false
                                onImportSyllabusClick()
                            }
                        )

                        // Action 3: Share Active Subject Syllabus
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share Subject JSON", fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                subjectMenuExpanded = false
                                onShareSubjectClick(activeSubject)
                            }
                        )

                        // Action 3b: Switch Subject Type (Theory vs Lab)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (activeSubject.subjectType == SubjectType.LAB) "📖 Switch to Theory Mode" else "🧪 Switch to Practical / Lab Mode",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                subjectMenuExpanded = false
                                val newType = if (activeSubject.subjectType == SubjectType.LAB) SubjectType.THEORY else SubjectType.LAB
                                onToggleSubjectType(newType)
                            }
                        )

                        // Action 4: Delete Custom Subject (if custom)
                        if (activeSubject.isCustom) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delete Subject", fontSize = 12.sp, color = Color(0xFFEF4444))
                                    }
                                },
                                onClick = {
                                    subjectMenuExpanded = false
                                    onDeleteSubjectClick(activeSubject)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Action Pills Dock: [📄 Model Question Paper] [✨ AI Import]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Model Question Paper Action Pill (1-Tap Practice / PDF)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenModelPaper() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Model Paper",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 2. AI Syllabus Import Action Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onImportSyllabusClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Import",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
