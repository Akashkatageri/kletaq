package com.kletaq.app.features.journey.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.syllabus.CustomSyllabusManager
import com.kletaq.app.core.syllabus.ImportedSubject
import com.kletaq.app.core.syllabus.SyllabusAiImporter
import com.kletaq.app.core.theme.PurpleAccent
import kotlinx.coroutines.launch

@Composable
fun ImportSyllabusDialog(
    currentSemester: Int,
    onDismiss: () -> Unit,
    onSubjectImported: (ImportedSubject) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var syllabusText by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableIntStateOf(currentSemester) }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isParsing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI SYLLABUS IMPORTER ⚡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Zero Maintenance Course Setup",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (!isParsing) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Paste your university course syllabus below. Gemini Flash Lite extracts modules, units, and high-yield topics to create an instant playable node roadmap.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Semester Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Assign to:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    (1..8).forEach { sem ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedSemester == sem) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (selectedSemester == sem) PurpleAccent else Color.Transparent),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            TextButton(
                                onClick = { selectedSemester = sem },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "S$sem",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedSemester == sem) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text Input Area
                OutlinedTextField(
                    value = syllabusText,
                    onValueChange = {
                        syllabusText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = {
                        Text(
                            text = "Paste course syllabus text here...\n\nExample:\n1BPHYS102 - Quantum Physics & Applications\nModule 1: Quantum Mechanics\n• Schrödinger wave equation in 1D box\n• Heisenberg uncertainty principle\n• Physical significance of wave function\n\nModule 2: Superconductivity\n• Type I & Type II superconductors\n• Josephson Junction & SQUID...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    enabled = !isParsing
                )

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (syllabusText.trim().length < 30) {
                            errorMessage = "Please paste at least 1 module or topic list to parse."
                            return@Button
                        }
                        isParsing = true
                        errorMessage = null

                        scope.launch {
                            val result = SyllabusAiImporter.importSyllabus(
                                rawText = syllabusText,
                                targetSemester = selectedSemester
                            )

                            isParsing = false
                            result.onSuccess { imported ->
                                CustomSyllabusManager.saveSubject(context, imported)
                                onSubjectImported(imported)
                                onDismiss()
                            }.onFailure { err ->
                                errorMessage = "Parsing failed: ${err.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isParsing && syllabusText.isNotBlank()
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Parsing with Gemini Flash Lite...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generate Interactive Roadmap 🚀",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
