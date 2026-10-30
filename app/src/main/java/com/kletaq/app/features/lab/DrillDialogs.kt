package com.kletaq.app.features.lab

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import kotlinx.coroutines.delay

/**
 * Drill 1: Algorithm Step Order Rebuilder.
 * Presents shuffled algorithmic steps that the student must order correctly.
 */
@Composable
fun DrillStepOrderDialog(
    program: LabProgramItem,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val originalSteps = remember(program) { program.algorithmSteps }
    var currentOrder by remember(program) { mutableStateOf(originalSteps.shuffled()) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isVerified by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🧩 DRILL 1: ALGORITHM ORDER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3B82F6),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = program.shortTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Tap any two steps to swap them into the correct logical order:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Shuffled Steps List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(currentOrder) { index, stepText ->
                        val isSelected = selectedIndex == index
                        val isCorrectPos = isVerified && index < originalSteps.size && originalSteps[index] == stepText

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isCorrectPos -> Color(0xFF10B981).copy(alpha = 0.2f)
                                isSelected -> Color(0xFF3B82F6).copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    isCorrectPos -> Color(0xFF10B981)
                                    isSelected -> Color(0xFF3B82F6)
                                    else -> Color.Transparent
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedIndex == null) {
                                        selectedIndex = index
                                    } else {
                                        val first = selectedIndex!!
                                        if (first != index) {
                                            val mutable = currentOrder.toMutableList()
                                            val temp = mutable[first]
                                            mutable[first] = mutable[index]
                                            mutable[index] = temp
                                            currentOrder = mutable
                                        }
                                        selectedIndex = null
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCorrectPos) Color(0xFF10B981) else Color(0xFF3B82F6),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stepText,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Verify Button
                Button(
                    onClick = {
                        isVerified = true
                        val matches = currentOrder == originalSteps
                        if (matches) {
                            Toast.makeText(context, "Perfect Step Order! +20 XP 🎉", Toast.LENGTH_SHORT).show()
                            onSuccess()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Order not quite right. Check highlighted steps!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Step Order", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Drill 2: The Core Function Blackout.
 * Displays the blueprint boilerplate and driver, while blanking out the critical 20-30 line algorithmic core.
 */
@Composable
fun DrillCoreFunctionDialog(
    program: LabProgramItem,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var showSolution by remember { mutableStateOf(false) }
    var enteredCode by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ DRILL 2: CORE FUNCTION BLACKOUT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = program.shortTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "The boilerplate is provided. Reconstruct the critical algorithmic logic:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Blueprint block (Given)
                    item {
                        Text(text = "1. Blueprint & Globals (Provided):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E2E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = program.blueprintBlock,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF93C5FD),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Core Function Slot (Blackout)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "2. Core Logic Function (Your Turn):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            TextButton(onClick = { showSolution = !showSolution }) {
                                Text(if (showSolution) "Hide Solution" else "Peek Solution 👁️", fontSize = 11.sp)
                            }
                        }

                        if (showSolution) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = program.coreFunctionBlock,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6EE7B7),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = enteredCode,
                                onValueChange = { enteredCode = it },
                                placeholder = {
                                    Text(
                                        text = "// Type the core logic function here (or practice on paper and tap verified)...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp, max = 220.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Guard conditions reminder
                    item {
                        Text(text = "3. Guard Conditions to Remember:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = program.guardBlock,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                // Completion Button
                Button(
                    onClick = {
                        Toast.makeText(context, "Core Function Mastered! +30 XP ⚡", Toast.LENGTH_SHORT).show()
                        onSuccess()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Core Function Mastered", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Drill 3: Pen & Paper Blind Write Timer + Crash-Trap Audit Checklist.
 */
@Composable
fun DrillPenAndPaperDialog(
    program: LabProgramItem,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isTimerRunning by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(15 * 60) } // 15 minutes
    var isAuditMode by remember { mutableStateOf(false) }

    val checkedTraps = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isTimerRunning && !isAuditMode) Color(0xFF0A0F1D) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✍️ DRILL 3: PEN & PAPER BLIND WRITE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = program.shortTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTimerRunning && !isAuditMode) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isTimerRunning && !isAuditMode) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isAuditMode) {
                    // Timer & Screen-down Mode
                    Spacer(modifier = Modifier.weight(0.3f))

                    val minutes = secondsRemaining / 60
                    val seconds = secondsRemaining % 60
                    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                    Text(
                        text = timeFormatted,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Take a real pen and paper.\nWrite ${program.shortTitle} from memory without looking.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = if (isTimerRunning) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.weight(0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isTimerRunning) {
                            Button(
                                onClick = { isTimerRunning = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start 15-Min Timer", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { isTimerRunning = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Pause", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                isTimerRunning = false
                                isAuditMode = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Icon(imageVector = Icons.Default.Checklist, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Done! Audit Paper", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Audit Mode: Crash-Trap Checklist & Full Code Review
                    Text(
                        text = "Crash-Trap Self-Audit Checklist",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Check your written paper against common examiner crash traps:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(program.crashTraps) { idx, trap ->
                            val isChecked = checkedTraps[idx] == true
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkedTraps[idx] = !isChecked }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checkedTraps[idx] = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trap,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Full Master Code Reference:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E1E2E),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = program.fullCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "100% BLIND VERIFIED! +50 XP 🏆", Toast.LENGTH_SHORT).show()
                            onSuccess()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(imageVector = Icons.Default.Verified, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm 100% Blind Verified", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Examiner Viva Voce Defense Dialog.
 */
@Composable
fun VivaVoceDefenseDialog(
    program: LabProgramItem,
    onDismiss: () -> Unit
) {
    var activeIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val total = program.vivaQuestions.size
    val currentQ = program.vivaQuestions.getOrNull(activeIndex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(420.dp),
            border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗣️ EXAMINER VIVA DEFENSE (${activeIndex + 1}/$total)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurpleAccent
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Flashcard Box
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isFlipped) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.5.dp, if (isFlipped) Color(0xFF10B981) else Color(0xFF6366F1).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { isFlipped = !isFlipped }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isFlipped) "EXAMINER ANSWER:" else "QUESTION:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isFlipped) Color(0xFF34D399) else PurpleAccent
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap to ${if (isFlipped) "view question" else "reveal answer"}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Controls
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
                        Text("← Prev")
                    }

                    Button(
                        onClick = {
                            if (activeIndex < total - 1) {
                                activeIndex++
                                isFlipped = false
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text(if (activeIndex < total - 1) "Next →" else "Done")
                    }
                }
            }
        }
    }
}

/**
 * End-of-Semester Practical Exam Simulator Dialog.
 * Randomly draws 2 chits, starts 3-hour timer, and shows official evaluation rubric.
 */
@Composable
fun FinalExamSimulatorDialog(
    programs: List<LabProgramItem>,
    onDismiss: () -> Unit
) {
    var chits by remember {
        mutableStateOf<Pair<LabProgramItem, LabProgramItem>?>(null)
    }

    LaunchedEffect(Unit) {
        if (programs.size >= 2) {
            val shuffled = programs.shuffled()
            chits = Pair(shuffled[0], shuffled[1])
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎓 UNIVERSITY PRACTICAL EXAM SIMULATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Random 2-Program Chit Draw",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Drawn Chits Cards
                if (chits != null) {
                    val (c1, c2) = chits!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "🎟️ CHIT 1 (PART A)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB91C1C))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = c1.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 3)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "🎟️ CHIT 2 (PART B)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB91C1C))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = c2.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 3)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Official Evaluation Rubric (100 Marks)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        RubricItem(title = "Part A Writeup (Booklet)", marks = "20 Marks", desc = "Writing Program 1 from memory with correct data structures and logic.")
                    }
                    item {
                        RubricItem(title = "Part A Terminal Execution", marks = "20 Marks", desc = "Compiling, testing edge cases, and demonstrating successful run.")
                    }
                    item {
                        RubricItem(title = "Part B Writeup (Booklet)", marks = "20 Marks", desc = "Writing Program 2 from memory without looking.")
                    }
                    item {
                        RubricItem(title = "Part B Terminal Execution", marks = "20 Marks", desc = "Compiling and executing Program 2 with examiner inputs.")
                    }
                    item {
                        RubricItem(title = "Viva Voce Defense", marks = "20 Marks", desc = "Defending time/space complexity, data structure choices, and pointers.")
                    }
                }

                // Action
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Ready for Exam Hall", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RubricItem(title: String, marks: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = marks, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
        }
    }
}

