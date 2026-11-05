package com.kletaq.app.features.lab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabProgramStudioDialog(
    program: LabProgramItem,
    subjectId: String,
    onDismiss: () -> Unit,
    onStatusChanged: (ProgramMasteryStatus) -> Unit,
    onProgramUpdated: ((LabProgramItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Code & Blueprint, 1: Daily Sprint (4 Drills), 2: Viva Voce
    var currentProgram by remember(program) { mutableStateOf(program) }
    var currentMastery by remember(program) { mutableStateOf(program.masteryStatus) }
    var showMasteryMenu by remember { mutableStateOf(false) }

    val hasPdf = remember(subjectId) {
        LabDefenseRepository.getCustomLabManualPdfFile(context, subjectId) != null
    }
    var isSyncingPdf by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    isSyncingPdf = true
                    Toast.makeText(context, "Syncing program from uploaded PDF...", Toast.LENGTH_SHORT).show()
                    val updated = LabDefenseRepository.attachAndSyncLabManualPdf(
                        context = context,
                        subjectId = subjectId,
                        pdfUri = uri
                    )
                    val matching = updated.find { it.programNumber == currentProgram.programNumber }
                    if (matching != null) {
                        currentProgram = matching
                        onProgramUpdated?.invoke(matching)
                        Toast.makeText(context, "Successfully updated from manual! 🚀", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Lab manual attached and synced! 📄", Toast.LENGTH_SHORT).show()
                        onProgramUpdated?.invoke(currentProgram)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to parse PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSyncingPdf = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.95f),
            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = currentProgram.language.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PROGRAM ${currentProgram.programNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = currentProgram.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // PDF Button (Open if attached, else Attach)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasPdf) SuccessGreen.copy(alpha = 0.15f) else PurpleAccent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (hasPdf) SuccessGreen else PurpleAccent),
                            modifier = Modifier.clickable {
                                if (hasPdf) {
                                    LabDefenseRepository.openLabManualPdf(context, subjectId)
                                } else {
                                    pdfLauncher.launch("application/pdf")
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasPdf) Icons.Default.PictureAsPdf else Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (hasPdf) SuccessGreen else PurpleAccent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncingPdf) "Syncing..." else if (hasPdf) "PDF Manual" else "Attach PDF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasPdf) SuccessGreen else PurpleAccent
                                )
                            }
                        }

                        if (hasPdf) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { pdfLauncher.launch("application/pdf") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Sync New PDF",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("💻 Code & Blueprint", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("⚡ 5-Min Sprint", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("🗣️ Viva (${currentProgram.vivaQuestions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> CodeAndBlueprintTab(program = currentProgram)
                    1 -> DailyLabSprintTab(
                        program = currentProgram,
                        onSprintCompleted = {
                            currentMastery = ProgramMasteryStatus.BLIND_VERIFIED
                            onStatusChanged(ProgramMasteryStatus.BLIND_VERIFIED)
                        }
                    )
                    2 -> VivaDefenseTab(program = currentProgram)
                }
            }
        }
    }
}

@Composable
private fun CodeAndBlueprintTab(program: LabProgramItem) {
    val context = LocalContext.current
    var isFullCodeExpanded by remember { mutableStateOf(true) }
    val expandedKernels = remember { mutableStateMapOf<String, Boolean>() }

    if (program.fullCode.isBlank()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No Code Attached Yet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Attach your college's Lab Manual PDF to extract genuine program code, 5-Kernel blueprints, and interactive sprint mini-games.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Copy Code & Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FULL PROGRAM IMPLEMENTATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("${program.shortTitle} Code", program.fullCode))
                    Toast.makeText(context, "Full Program Code copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Code", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Full Code Monospace Box with Line Numbers
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E2E),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                val lines = remember(program.fullCode) { program.fullCode.lines() }
                val displayLines = if (isFullCodeExpanded) lines else lines.take(15)

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column {
                        displayLines.forEachIndexed { idx, line ->
                            Row {
                                Text(
                                    text = String.format("%2d ", idx + 1),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280),
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = line,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }
                    }
                }

                if (lines.size > 15) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { isFullCodeExpanded = !isFullCodeExpanded },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isFullCodeExpanded) "▲ Collapse Full Code" else "▼ Show Full ${lines.size} Lines",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                    }
                }
            }
        }

        // Pre-Lab Blueprint: 5 Logic Kernels (Panic Mode)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "PRE-LAB BLUEPRINT (5 LOGIC KERNELS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Master these 5 blocks so you never have to memorize a giant wall of text.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The 5 Kernel Collapsible Chips
                program.kernels.forEach { kernel ->
                    val isExpanded = expandedKernels[kernel.id] ?: false

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.8.dp, Color.Gray.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { expandedKernels[kernel.id] = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = kernel.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Gray
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Exam Trick Box
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(text = "💡", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = kernel.examTrick,
                                            fontSize = 11.sp,
                                            color = Color(0xFFB45309),
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Exact Code Snippet
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E1E2E),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = kernel.codeSnippet,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF6EE7B7),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DailyLabSprintTab(
    program: LabProgramItem,
    onSprintCompleted: () -> Unit
) {
    val drills = program.drills
    var currentDrillIndex by remember { mutableIntStateOf(0) }
    val completedDrills = remember { mutableStateMapOf<Int, Boolean>() }

    if (drills == null || program.fullCode.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Attach your college Lab Manual PDF to unlock interactive Trace, Parson's Puzzle, Cloze, and Bug Hunt drills generated directly from your program code.",
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step Stepper Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val drillTitles = listOf("1. Trace", "2. Parson's", "3. Cloze", "4. Bug Hunt")
            drillTitles.forEachIndexed { index, name ->
                val isCurrent = currentDrillIndex == index
                val isDone = completedDrills[index] == true

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isDone -> SuccessGreen.copy(alpha = 0.15f)
                        isCurrent -> Color(0xFF6366F1).copy(alpha = 0.20f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isDone -> SuccessGreen
                            isCurrent -> Color(0xFF6366F1)
                            else -> Color.Transparent
                        }
                    ),
                    modifier = Modifier.clickable { currentDrillIndex = index }
                ) {
                    Text(
                        text = if (isDone) "✓ $name" else name,
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isDone -> SuccessGreen
                            isCurrent -> Color(0xFF6366F1)
                            else -> Color.Gray
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Active Drill Component
        when (currentDrillIndex) {
            0 -> drills.dryRun?.let { dryRun ->
                DryRunGameView(
                    drill = dryRun,
                    onSuccess = {
                        completedDrills[0] = true
                        if (currentDrillIndex < 3) currentDrillIndex = 1
                    }
                )
            }
            1 -> drills.parsonsPuzzle?.let { puzzle ->
                ParsonsPuzzleGameView(
                    drill = puzzle,
                    onSuccess = {
                        completedDrills[1] = true
                        if (currentDrillIndex < 3) currentDrillIndex = 2
                    }
                )
            }
            2 -> drills.clozeDrill?.let { cloze ->
                ClozeDrillGameView(
                    drill = cloze,
                    onSuccess = {
                        completedDrills[2] = true
                        if (currentDrillIndex < 3) currentDrillIndex = 3
                    }
                )
            }
            3 -> drills.bugHunt?.let { bugHunt ->
                BugHuntGameView(
                    drill = bugHunt,
                    onSuccess = {
                        completedDrills[3] = true
                    }
                )
            }
        }

        // Check if all 4 completed
        if (completedDrills.size >= 4) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SuccessGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🏆", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SPRINT COMPLETE! +100 XP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                    Text(
                        text = "Program 1 logic is now verified from memory via SM-2.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSprintCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mark Mastered in Syllabus")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DryRunGameView(
    drill: DryRunQuestion,
    onSuccess: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "DRILL 1: VISUAL DRY-RUN TRACE",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3B82F6),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Scenario Memory State
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E1E2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = drill.scenarioText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF93C5FD),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = drill.question,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Options
            drill.options.forEachIndexed { idx, opt ->
                val isSelected = selectedOption == idx
                val isCorrect = idx == drill.correctOptionIndex

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isSubmitted && isCorrect -> SuccessGreen.copy(alpha = 0.20f)
                        isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.20f)
                        isSelected -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isSubmitted && isCorrect -> SuccessGreen
                            isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                            isSelected -> Color(0xFF3B82F6)
                            else -> Color.Transparent
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { if (!isSubmitted) selectedOption = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A'.code + idx).toChar()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = opt,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isSubmitted) {
                Button(
                    onClick = {
                        isSubmitted = true
                        if (selectedOption == drill.correctOptionIndex) {
                            onSuccess()
                        }
                    },
                    enabled = selectedOption != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Check Answer", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = drill.explanation,
                    fontSize = 11.sp,
                    color = Color(0xFF10B981),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ParsonsPuzzleGameView(
    drill: ParsonsPuzzle,
    onSuccess: () -> Unit
) {
    var currentLines by remember(drill) { mutableStateOf(drill.scrambledLines) }
    var selectedLineIndex by remember { mutableStateOf<Int?>(null) }
    var isVerified by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "DRILL 2: PARSON'S PUZZLE (TAP TO ORDER LINES)",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF8B5CF6),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = drill.prompt,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            currentLines.forEachIndexed { idx, line ->
                val isSelected = selectedLineIndex == idx
                val isCorrectPos = isVerified && idx < drill.correctLines.size && drill.correctLines[idx] == line

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isCorrectPos -> SuccessGreen.copy(alpha = 0.20f)
                        isSelected -> Color(0xFF8B5CF6).copy(alpha = 0.25f)
                        else -> Color(0xFF1E1E2E)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isCorrectPos -> SuccessGreen
                            isSelected -> Color(0xFF8B5CF6)
                            else -> Color.Transparent
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            if (selectedLineIndex == null) {
                                selectedLineIndex = idx
                            } else {
                                val first = selectedLineIndex!!
                                if (first != idx) {
                                    val mut = currentLines.toMutableList()
                                    val temp = mut[first]
                                    mut[first] = mut[idx]
                                    mut[idx] = temp
                                    currentLines = mut
                                }
                                selectedLineIndex = null
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF93C5FD)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    isVerified = true
                    if (currentLines == drill.correctLines) {
                        onSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text(if (currentLines == drill.correctLines) "✓ Correct Order!" else "Verify Line Sequence", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ClozeDrillGameView(
    drill: ClozeDrill,
    onSuccess: () -> Unit
) {
    val filledBlanks = remember(drill) { mutableStateListOf<String>() }
    var isSubmitted by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "DRILL 3: TOKEN-CHIP CLOZE",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF59E0B),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = drill.prompt,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Code Template with Blanks
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1E2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = drill.codeTemplate,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFFCD34D),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Currently Selected Tokens
            Text(
                text = "Your Placed Chips: ${filledBlanks.joinToString("  •  ").ifBlank { "(None placed yet)" }}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chip Tray
            Text(text = "Tap to Add / Remove:", fontSize = 10.sp, color = Color.Gray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                drill.chipOptions.forEach { chip ->
                    val isUsed = filledBlanks.contains(chip)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isUsed) Color.Gray.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isUsed) Color.Gray else Color(0xFFF59E0B)),
                        modifier = Modifier.clickable {
                            if (isUsed) {
                                filledBlanks.remove(chip)
                            } else if (filledBlanks.size < drill.blanks.size) {
                                filledBlanks.add(chip)
                            }
                        }
                    ) {
                        Text(
                            text = chip,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUsed) Color.Gray else Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    isSubmitted = true
                    if (filledBlanks.toList() == drill.blanks) {
                        onSuccess()
                    }
                },
                enabled = filledBlanks.size == drill.blanks.size,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text(if (filledBlanks.toList() == drill.blanks) "✓ Correct Tokens!" else "Verify Tokens", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BugHuntGameView(
    drill: BugHuntDrill,
    onSuccess: () -> Unit
) {
    var selectedLine by remember { mutableStateOf<Int?>(null) }
    var isRevealed by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "DRILL 4: BUG HUNT (SPOT THE EXAM MISTAKE)",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFEF4444),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = drill.prompt,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            drill.codeLinesWithNumbers.forEach { (lineNum, text) ->
                val isSelected = selectedLine == lineNum
                val isBuggy = lineNum == drill.buggyLineNumber

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isRevealed && isBuggy -> Color(0xFFEF4444).copy(alpha = 0.25f)
                        isSelected -> Color(0xFF6366F1).copy(alpha = 0.20f)
                        else -> Color(0xFF1E1E2E)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isRevealed && isBuggy -> Color(0xFFEF4444)
                            isSelected -> Color(0xFF6366F1)
                            else -> Color.Transparent
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable {
                            selectedLine = lineNum
                            isRevealed = true
                            if (isBuggy) {
                                onSuccess()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%2d:", lineNum),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (isRevealed && isBuggy) Color(0xFFFCA5A5) else Color(0xFF93C5FD)
                        )
                    }
                }
            }

            if (isRevealed) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedLine == drill.buggyLineNumber) SuccessGreen.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (selectedLine == drill.buggyLineNumber) SuccessGreen else Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (selectedLine == drill.buggyLineNumber) "✓ Bug Caught! Line ${drill.buggyLineNumber}" else "❌ Bug was on Line ${drill.buggyLineNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedLine == drill.buggyLineNumber) SuccessGreen else Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = drill.explanation,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VivaDefenseTab(program: LabProgramItem) {
    var activeIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val viva = program.vivaQuestions
    if (viva.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No viva questions loaded for this program.", color = Color.Gray)
        }
        return
    }

    val currentQ = viva.getOrNull(activeIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EXAMINER VIVA VOCE DEFENSE (${activeIndex + 1} of ${viva.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PurpleAccent,
            letterSpacing = 0.5.sp
        )

        // Flashcard Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isFlipped) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.5.dp, if (isFlipped) SuccessGreen else PurpleAccent.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .clickable { isFlipped = !isFlipped }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFlipped) "EXAMINER MODEL ANSWER:" else "VIVA QUESTION:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isFlipped) SuccessGreen else PurpleAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isFlipped) currentQ?.second.orEmpty() else currentQ?.first.orEmpty(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = if (isFlipped) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Tap to ${if (isFlipped) "view question" else "reveal answer"}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Stepper Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    if (activeIndex > 0) {
                        activeIndex--
                        isFlipped = false
                    }
                },
                enabled = activeIndex > 0
            ) {
                Text("← Previous")
            }

            Button(
                onClick = {
                    if (activeIndex < viva.size - 1) {
                        activeIndex++
                        isFlipped = false
                    }
                },
                enabled = activeIndex < viva.size - 1,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Next →")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

