package com.kletaq.app.features.journey.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.data.model.FormulaItem

@Composable
fun FormulaDiagramBlitzDialog(
    subjectName: String,
    formulas: List<FormulaItem>,
    onDismiss: () -> Unit,
    onFormulaMastered: (FormulaItem) -> Unit = {}
) {
    if (formulas.isEmpty()) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Formulas Found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Formulas will appear here as you upload notes and explore the syllabus.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss) {
                        Text("Got It")
                    }
                }
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentFormula = formulas.getOrElse(currentIndex) { formulas.first() }
    val masteredIndices = remember { mutableStateListOf<Int>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RAPID FORMULA BLITZ ⚡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$subjectName • ${currentIndex + 1} of ${formulas.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / formulas.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFFF59E0B),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Active Formula Card
                AnimatedContent(
                    targetState = currentFormula,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f),
                    label = "FormulaBlitzCard"
                ) { formula ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PurpleAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "CORE EQUATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PurpleAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = formula.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Expression Container
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = formula.expression,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF38BDF8),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                if (formula.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = formula.description,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // Mastered Badge
                            val isMastered = masteredIndices.contains(currentIndex)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isMastered) SuccessGreen.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isMastered) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Formula Mastered! (+5 XP)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Formula"
                        )
                    }

                    Button(
                        onClick = {
                            if (!masteredIndices.contains(currentIndex)) {
                                masteredIndices.add(currentIndex)
                                onFormulaMastered(currentFormula)
                            }
                            if (currentIndex < formulas.size - 1) {
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentIndex < formulas.size - 1) "Master & Next ➔" else "Finish Blitz 🎉",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    IconButton(
                        onClick = { if (currentIndex < formulas.size - 1) currentIndex++ },
                        enabled = currentIndex < formulas.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Formula"
                        )
                    }
                }
            }
        }
    }
}
