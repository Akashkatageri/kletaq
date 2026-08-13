package com.kletaq.app.features.tasks.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kletaq.app.core.theme.InkPaperBorder
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.data.repository.NoteRepository
import com.kletaq.app.domain.model.NoteType
import com.kletaq.app.domain.model.StudyNote

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickNoteSheet(
    onDismiss: () -> Unit,
    onNoteSaved: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Data Structures & Algorithms") }
    var topicTitle by remember { mutableStateOf("") }
    var selectedNoteType by remember { mutableStateOf(NoteType.GENERAL) }

    val quickActions = listOf(
        Triple("📐 Add formula", NoteType.FORMULA, "Formula: "),
        Triple("📖 Add definition", NoteType.DEFINITION, "Definition: "),
        Triple("❓ Add doubt", NoteType.DOUBT, "Doubt / Question: "),
        Triple("💡 Add idea", NoteType.IDEA, "Idea / Insight: ")
    )

    val placeholders = listOf(
        "Write Maxwell's equations...",
        "Remember AVL tree rotations...",
        "Ask professor about recursion..."
    )
    val currentPlaceholder = remember { placeholders.shuffled().first() }

    val subjects = listOf(
        "Data Structures & Algorithms",
        "Discrete Mathematics",
        "Applied Chemistry",
        "Operating Systems",
        "Computer Networks"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Quick Study Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Capture formulas, definitions, doubts & study insights",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Actions Templates Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "QUICK ACTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickActions.forEach { (label, type, prefix) ->
                        val isSelected = selectedNoteType == type
                        Surface(
                            modifier = Modifier.clickable {
                                selectedNoteType = type
                                if (!noteContent.startsWith(prefix)) {
                                    noteContent = prefix + noteContent
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent else PurpleAccent.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, PurpleAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else PurpleAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 1. Note Title Input
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = { Text("Note Title *") },
                placeholder = { Text(currentPlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            // 2. Multiline Content Input
            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                label = { Text("Note Content *") },
                placeholder = { Text("Write key equations, explanations, or questions here...") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            // 3. Subject Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SUBJECT *",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { subject ->
                        val isSelected = subject == selectedSubject
                        Surface(
                            modifier = Modifier.clickable { selectedSubject = subject },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = subject,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 4. Optional Topic Input
            OutlinedTextField(
                value = topicTitle,
                onValueChange = { topicTitle = it },
                label = { Text("Optional Topic / Module") },
                placeholder = { Text("e.g. Electromagnetism or Unit 3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.MediumShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons (Discard / Save Note)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = InkPaperBorder.MediumShape
                ) {
                    Text("Discard", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (noteTitle.isNotBlank() || noteContent.isNotBlank()) {
                            val finalTitle = noteTitle.ifBlank {
                                noteContent.take(25) + if (noteContent.length > 25) "..." else ""
                            }
                            val newNote = StudyNote(
                                title = finalTitle.trim(),
                                content = noteContent.trim(),
                                subjectName = selectedSubject,
                                topicTitle = topicTitle.ifBlank { null },
                                noteType = selectedNoteType
                            )
                            NoteRepository.addNote(newNote)
                            onNoteSaved()
                            onDismiss()
                        }
                    },
                    enabled = noteTitle.isNotBlank() || noteContent.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
