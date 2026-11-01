package com.kletaq.app.features.memorypalace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep

/**
 * 🧩 Loci Puzzle View
 * Sequence-matching puzzle where students match shuffled derivation equations to the correct ordered room pins.
 */
@Composable
fun LociPuzzleView(
    loci: List<PalaceLocus>,
    steps: List<PalaceStep>,
    onPuzzleCompleted: () -> Unit
) {
    val sortedLoci = remember(loci) { loci.sortedBy { it.orderIndex } }
    val shuffledSteps = remember(steps) { steps.shuffled() }

    // Map: locusOrderIndex -> assigned PalaceStep
    val assignments = remember { mutableStateMapOf<Int, PalaceStep>() }
    var selectedStepToPlace by remember { mutableStateOf<PalaceStep?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    val allMatched = remember(assignments.size, isSubmitted) {
        if (!isSubmitted || assignments.size < steps.size) false
        else {
            assignments.all { (locusIndex, step) ->
                step.locusOrderIndex == locusIndex
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Loci Sequence Challenge",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select an equation below, then tap the correct Room Spot to place it in order:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Room Spots
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedLoci.take(steps.size).forEach { locus ->
                val assignedStep = assignments[locus.orderIndex]
                val isCorrect = isSubmitted && assignedStep?.locusOrderIndex == locus.orderIndex
                val isWrong = isSubmitted && assignedStep != null && assignedStep.locusOrderIndex != locus.orderIndex

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val selected = selectedStepToPlace
                            if (selected != null) {
                                assignments[locus.orderIndex] = selected
                                selectedStepToPlace = null
                                isSubmitted = false
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCorrect -> Color(0xFF10B981).copy(alpha = 0.2f)
                            isWrong -> Color(0xFFEF4444).copy(alpha = 0.2f)
                            assignedStep != null -> Color(0xFF2563EB).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    ),
                    border = when {
                        isCorrect -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981))
                        isWrong -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444))
                        selectedStepToPlace != null -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB))
                        else -> null
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    when {
                                        isCorrect -> Color(0xFF10B981)
                                        isWrong -> Color(0xFFEF4444)
                                        else -> Color(0xFF2563EB)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = locus.orderIndex.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = locus.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (assignedStep != null) {
                                Text(
                                    text = assignedStep.formulaLatex.ifBlank { assignedStep.stepTitle },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = if (isWrong) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "Tap to place equation here...",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Unplaced Shuffled Equation Cards
        val unplacedSteps = shuffledSteps.filter { step ->
            !assignments.values.contains(step)
        }

        if (unplacedSteps.isNotEmpty()) {
            Text(
                text = "Unplaced Equations (${unplacedSteps.size}):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                unplacedSteps.forEach { step ->
                    val isSelected = selectedStepToPlace?.id == step.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStepToPlace = if (isSelected) null else step
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                Color(0xFF2563EB).copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = step.stepTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (step.formulaLatex.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = step.formulaLatex,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    assignments.clear()
                    selectedStepToPlace = null
                    isSubmitted = false
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }

            Button(
                onClick = {
                    if (isSubmitted && allMatched) {
                        onPuzzleCompleted()
                    } else {
                        isSubmitted = true
                    }
                },
                enabled = assignments.size == steps.size,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSubmitted && allMatched) Color(0xFF10B981) else Color(0xFF2563EB)
                )
            ) {
                if (isSubmitted && allMatched) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sequence Mastered! Continue")
                } else {
                    Text("Check Order")
                }
            }
        }
    }
}

