package com.kletaq.app.features.lab

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.features.journey.components.SubjectJourney
import com.kletaq.app.features.journey.components.SubjectType
import java.util.Calendar

@Composable
fun LabDefenseView(
    subject: SubjectJourney,
    onToggleSubjectType: (SubjectType) -> Unit,
    onAttachPdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showImportManualDialog by remember { mutableStateOf(false) }

    // Load Lab Programs
    val programs = remember(subject.id, refreshKey) {
        LabDefenseRepository.getLabProgramsForSubject(context, subject)
    }

    val hasCustomPrograms = remember(subject.id, refreshKey) {
        LabDefenseRepository.hasCustomLabPrograms(context, subject.id)
    }

    // Lab Day State
    var scheduledDay by remember(subject.id, refreshKey) {
        mutableStateOf(LabDefenseRepository.getLabDay(context, subject.id))
    }
    var showDayPickerMenu by remember { mutableStateOf(false) }

    // Dialog States
    var activeProgramForDrill1 by remember { mutableStateOf<LabProgramItem?>(null) }
    var activeProgramForDrill2 by remember { mutableStateOf<LabProgramItem?>(null) }
    var activeProgramForDrill3 by remember { mutableStateOf<LabProgramItem?>(null) }
    var activeProgramForViva by remember { mutableStateOf<LabProgramItem?>(null) }
    var showFinalExamSimulator by remember { mutableStateOf(false) }

    // Compute Readiness & Pass Probability
    val (readinessRatio, passProb) = remember(programs) {
        LabDefenseRepository.calculateReadiness(programs)
    }
    val verifiedCount = remember(programs) {
        programs.count { it.masteryStatus == ProgramMasteryStatus.BLIND_VERIFIED || it.masteryStatus == ProgramMasteryStatus.COLLEGE_EXECUTED }
    }
    val readinessPercent = (readinessRatio * 100).toInt()
    val passProbPercent = (passProb * 100).toInt()

    // Days until next lab calculation
    val daysUntilLab = remember(scheduledDay) {
        if (scheduledDay == null) null
        else {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            var diff = scheduledDay!! - today
            if (diff < 0) diff += 7
            diff
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. Compact Lab Defense Hub Card ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🧪", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LAB DEFENSE ENGINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6366F1),
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${programs.size} Programs",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    // Mode switch chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable {
                            onToggleSubjectType(SubjectType.THEORY)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🧪 Lab", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(11.dp), tint = PurpleAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weekly Lab Slot Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.10f),
                    border = BorderStroke(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDayPickerMenu = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val dayName = when (scheduledDay) {
                                    Calendar.MONDAY -> "Every Monday"
                                    Calendar.TUESDAY -> "Every Tuesday"
                                    Calendar.WEDNESDAY -> "Every Wednesday"
                                    Calendar.THURSDAY -> "Every Thursday"
                                    Calendar.FRIDAY -> "Every Friday"
                                    Calendar.SATURDAY -> "Every Saturday"
                                    Calendar.SUNDAY -> "Every Sunday"
                                    else -> "Tap to set weekly lab class"
                                }
                                Text(
                                    text = if (scheduledDay != null) "Lab Slot: $dayName" else dayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        daysUntilLab == 0 -> "🚨 Lab is TODAY • Execute and sign off"
                                        daysUntilLab == 1 -> "⚠️ Lab is TOMORROW • Do 1 Blind Write"
                                        daysUntilLab != null -> "$daysUntilLab days until next weekly test"
                                        else -> "Anchoring avoids semester-end panic"
                                    },
                                    fontSize = 10.sp,
                                    color = Color(0xFF3B82F6),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(15.dp))
                    }

                    // Dropdown menu for day picker
                    DropdownMenu(
                        expanded = showDayPickerMenu,
                        onDismissRequest = { showDayPickerMenu = false }
                    ) {
                        val days = listOf(
                            "Monday" to Calendar.MONDAY,
                            "Tuesday" to Calendar.TUESDAY,
                            "Wednesday" to Calendar.WEDNESDAY,
                            "Thursday" to Calendar.THURSDAY,
                            "Friday" to Calendar.FRIDAY,
                            "Saturday" to Calendar.SATURDAY
                        )
                        days.forEach { (name, calDay) ->
                            DropdownMenuItem(
                                text = { Text("Every $name", fontSize = 12.sp) },
                                onClick = {
                                    LabDefenseRepository.setLabDay(context, subject.id, calDay)
                                    scheduledDay = calDay
                                    showDayPickerMenu = false
                                    Toast.makeText(context, "Lab set to Every $name! 📅", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Readiness & Pass Probability (Compact)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Pass Probability: ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$passProbPercent%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (passProbPercent >= 70) SuccessGreen else Color(0xFFEF4444)
                                )
                            }
                            Text(
                                text = "$verifiedCount of ${programs.size} Verified (Exam: 2 random chits)",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (readinessPercent >= 70) SuccessGreen else PurpleAccent
                        ) {
                            Text(
                                text = "$readinessPercent% Ready",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Exam Sim & Manual PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showFinalExamSimulator = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🎲 Exam Sim (2 Chits)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { showImportManualDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (hasCustomPrograms) Color(0xFF10B981) else Color(0xFF6366F1).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (hasCustomPrograms) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (hasCustomPrograms) Color(0xFF10B981) else Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (hasCustomPrograms) "📄 Manual (Custom)" else "📄 Lab Manual PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasCustomPrograms) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- 2. Program Section Header ---
        Text(
            text = "LAB SYLLABUS PROGRAMS (${programs.size})",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )

        // Program Cards
        programs.forEach { program ->
            ProgramCard(
                program = program,
                onDrill1 = { activeProgramForDrill1 = program },
                onDrill2 = { activeProgramForDrill2 = program },
                onDrill3 = { activeProgramForDrill3 = program },
                onViva = { activeProgramForViva = program },
                onToggleCollegeExecuted = {
                    val newStatus = if (program.masteryStatus == ProgramMasteryStatus.COLLEGE_EXECUTED) {
                        ProgramMasteryStatus.BLIND_VERIFIED
                    } else {
                        ProgramMasteryStatus.COLLEGE_EXECUTED
                    }
                    LabDefenseRepository.setProgramMastery(context, subject.id, program.programNumber, newStatus)
                    refreshKey++
                    val msg = if (newStatus == ProgramMasteryStatus.COLLEGE_EXECUTED) "Signed off by faculty! 🟢" else "Reverted to verified"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // --- Drill Dialogs ---
    if (activeProgramForDrill1 != null) {
        DrillStepOrderDialog(
            program = activeProgramForDrill1!!,
            onDismiss = { activeProgramForDrill1 = null },
            onSuccess = {
                LabDefenseRepository.setProgramMastery(context, subject.id, activeProgramForDrill1!!.programNumber, ProgramMasteryStatus.SKELETON_LEARNED)
                refreshKey++
            }
        )
    }

    if (activeProgramForDrill2 != null) {
        DrillCoreFunctionDialog(
            program = activeProgramForDrill2!!,
            onDismiss = { activeProgramForDrill2 = null },
            onSuccess = {
                LabDefenseRepository.setProgramMastery(context, subject.id, activeProgramForDrill2!!.programNumber, ProgramMasteryStatus.SKELETON_LEARNED)
                refreshKey++
            }
        )
    }

    if (activeProgramForDrill3 != null) {
        DrillPenAndPaperDialog(
            program = activeProgramForDrill3!!,
            onDismiss = { activeProgramForDrill3 = null },
            onSuccess = {
                LabDefenseRepository.setProgramMastery(context, subject.id, activeProgramForDrill3!!.programNumber, ProgramMasteryStatus.BLIND_VERIFIED)
                refreshKey++
            }
        )
    }

    if (activeProgramForViva != null) {
        VivaVoceDefenseDialog(
            program = activeProgramForViva!!,
            onDismiss = { activeProgramForViva = null }
        )
    }

    if (showFinalExamSimulator) {
        FinalExamSimulatorDialog(
            programs = programs,
            onDismiss = { showFinalExamSimulator = false }
        )
    }

    if (showImportManualDialog) {
        ImportLabManualDialog(
            subjectId = subject.id,
            onDismiss = { showImportManualDialog = false },
            onSuccess = {
                refreshKey++
                showImportManualDialog = false
            }
        )
    }
}

@Composable
private fun ProgramCard(
    program: LabProgramItem,
    onDrill1: () -> Unit,
    onDrill2: () -> Unit,
    onDrill3: () -> Unit,
    onViva: () -> Unit,
    onToggleCollegeExecuted: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Number, Title, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = program.masteryStatus.badgeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${program.programNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = program.masteryStatus.badgeColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = program.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = program.masteryStatus.badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, program.masteryStatus.badgeColor.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onToggleCollegeExecuted() }
                ) {
                    Text(
                        text = "${program.masteryStatus.emoji} ${program.masteryStatus.label}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = program.masteryStatus.badgeColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Distinctive Drill Actions Row (Ultra-compact, zero-overflow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Drill 1: Steps
                ActionPill(
                    icon = "🧩",
                    label = "Steps",
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = onDrill1
                )

                // Drill 2: Core
                ActionPill(
                    icon = "⚡",
                    label = "Core",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = onDrill2
                )

                // Drill 3: Write
                ActionPill(
                    icon = "✍️",
                    label = "Write",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = onDrill3
                )

                // Viva
                ActionPill(
                    icon = "🗣️",
                    label = "Viva",
                    color = PurpleAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onViva
                )
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.30f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}
