package com.kletaq.app.features.lesson.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.data.model.UniversityExamQuestion

@Composable
fun PaperSelfCheckDialog(
    question: UniversityExamQuestion,
    onDismiss: () -> Unit,
    onScoreSubmitted: (score: Int, maxMarks: Int) -> Unit
) {
    val rubricSteps = question.rubricSteps
    val checkedStepIndices = remember { mutableStateListOf<Int>() }

    val currentScore = remember(checkedStepIndices.size) {
        checkedStepIndices.sumOf { idx -> rubricSteps.getOrNull(idx)?.marks ?: 0 }
    }
    val maxMarks = question.marks

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PAPER SELF-EVALUATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent
                            )
                            Text(
                                text = "Score: $currentScore / $maxMarks Marks",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentScore >= maxMarks * 0.7f) SuccessGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Compare what you wrote on paper against the university evaluator marking rubric below. Tick each step you correctly wrote:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Rubric Step Checkboxes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rubricSteps.forEachIndexed { index, step ->
                        val isChecked = checkedStepIndices.contains(index)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isChecked) PurpleAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.5.dp,
                                if (isChecked) PurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!checkedStepIndices.contains(index)) checkedStepIndices.add(index)
                                        } else {
                                            checkedStepIndices.remove(index)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = PurpleAccent)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = step.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isChecked) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "+${step.marks}M",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isChecked) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = step.content,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    if (question.evaluatorTip.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "💡", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = question.evaluatorTip,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Score Button
                Button(
                    onClick = {
                        onScoreSubmitted(currentScore, maxMarks)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Score ($currentScore / $maxMarks Marks)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
