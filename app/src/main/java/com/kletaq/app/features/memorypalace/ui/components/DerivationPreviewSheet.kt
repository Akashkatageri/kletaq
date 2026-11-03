package com.kletaq.app.features.memorypalace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep

/**
 * 🗺️ Derivation Preview & Refine Screen
 * Interactive review stage between AI decomposition and persisting to Palace storage.
 * Aligns directly with Phase 2 of the Memory Palace user flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DerivationPreviewSheet(
    topicTitle: String,
    initialSteps: List<PalaceStep>,
    loci: List<PalaceLocus>,
    onConfirmAndSave: (List<PalaceStep>) -> Unit,
    onBackToInputs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editableSteps = remember {
        mutableStateListOf<PalaceStep>().apply { addAll(initialSteps) }
    }
    val sortedLoci = remember(loci) { loci.sortedBy { it.orderIndex } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToInputs) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to inputs",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Review & Refine Sequence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = topicTitle.ifBlank { "Decomposed Derivation" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${editableSteps.size} Anchors",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBackToInputs,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Edit Inputs", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onConfirmAndSave(editableSteps.toList()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Anchor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Each step is anchored to an exact landmark in your room. Tap any step below to edit the title, formula, or sensory mental story.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            itemsIndexed(editableSteps, key = { _, step -> step.id }) { index, step ->
                val locus = sortedLoci.firstOrNull { it.orderIndex == step.locusOrderIndex }
                    ?: sortedLoci.getOrNull(index)

                PreviewStepCard(
                    step = step,
                    locus = locus,
                    stepNumber = index + 1,
                    totalSteps = editableSteps.size,
                    onStepUpdated = { updated ->
                        editableSteps[index] = updated
                    },
                    onRerollStory = {
                        val locusName = locus?.name ?: "room landmark"
                        val rerolledStory = generateAlternativeSensoryStory(step.stepTitle, locusName)
                        editableSteps[index] = step.copy(sensoryStory = rerolledStory)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PreviewStepCard(
    step: PalaceStep,
    locus: PalaceLocus?,
    stepNumber: Int,
    totalSteps: Int,
    onStepUpdated: (PalaceStep) -> Unit,
    onRerollStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember(step.stepTitle) { mutableStateOf(step.stepTitle) }
    var editFormula by remember(step.formulaLatex) { mutableStateOf(step.formulaLatex) }
    var editStory by remember(step.sensoryStory) { mutableStateOf(step.sensoryStory) }
    var editExamTrap by remember(step.examTrap) { mutableStateOf(step.examTrap) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            1.dp,
            if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Pin & Locus Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF2563EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$stepNumber",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2563EB).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Pin #${locus?.orderIndex ?: stepNumber}: ${locus?.name ?: "Room Spot"}",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (step.pageIndex >= 0) {
                        Text(
                            text = "Page ${step.pageIndex + 1}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                onStepUpdated(
                                    step.copy(
                                        stepTitle = editTitle.trim(),
                                        formulaLatex = editFormula.trim(),
                                        sensoryStory = editStory.trim(),
                                        examTrap = editExamTrap.trim()
                                    )
                                )
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Done" else "Edit",
                            tint = if (isEditing) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isEditing) {
                // Read-only presentation
                Text(
                    text = step.stepTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (step.formulaLatex.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = step.formulaLatex,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (step.sensoryStory.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1B4B).copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mnemonic Story:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5B4FC)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = step.sensoryStory,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE0E7FF),
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(
                                onClick = onRerollStory,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-roll story",
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (step.examTrap.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF451A03).copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = step.examTrap,
                            fontSize = 11.sp,
                            color = Color(0xFFFED7AA),
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                // Interactive Edit Fields
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Step Title", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    OutlinedTextField(
                        value = editFormula,
                        onValueChange = { editFormula = it },
                        label = { Text("Formula / Key Equation", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    OutlinedTextField(
                        value = editStory,
                        onValueChange = { editStory = it },
                        label = { Text("Sensory Story (Room Mnemonic)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    OutlinedTextField(
                        value = editExamTrap,
                        onValueChange = { editExamTrap = it },
                        label = { Text("Exam Trap Warning", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
            }
        }
    }
}

/**
 * Procedurally generates an alternative surreal sensory story anchored to a specific locus.
 */
private fun generateAlternativeSensoryStory(conceptTitle: String, locusName: String): String {
    val actions = listOf(
        "an incandescent lightning bolt strikes directly upon your $locusName, fusing the concept into a glowing sapphire crystal.",
        "an enormous mechanical clockwork owl lands at your $locusName, mechanically chanting the symbols in resonant baritone rhythm.",
        "a bubbling geyser of liquid neon erupts from your $locusName, spraying freezing mist that outlines the transformation.",
        "a massive golden dragon coils tightly around your $locusName, breathing warm golden vapor that etches the milestone."
    )
    val chosen = actions[Math.abs(conceptTitle.hashCode()) % actions.size]
    return "At your $locusName, $chosen"
}

