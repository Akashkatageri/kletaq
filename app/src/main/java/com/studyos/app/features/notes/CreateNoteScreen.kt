package com.studyos.app.features.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.data.repository.NoteRepository
import com.studyos.app.domain.model.NoteType
import com.studyos.app.domain.model.StudyNote

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateNoteScreen(
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Data Structures & Algorithms") }
    var topicTitle by remember { mutableStateOf("") }

    val subjects = listOf(
        "Data Structures & Algorithms",
        "Discrete Mathematics",
        "Applied Chemistry",
        "Operating Systems",
        "Computer Networks"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = null,
                tint = PurpleAccent,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Quick Note",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Form Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Note Title Input
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = { Text("Note Title *") },
                placeholder = { Text("e.g. AVL Tree Rotations Key Principle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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
                placeholder = { Text("Write formulas, definitions, doubts, or study ideas...") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            // 3. Subject Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SUBJECT *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Save Note Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
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
                                noteType = NoteType.GENERAL
                            )
                            NoteRepository.addNote(newNote)
                            onBackClick()
                        }
                    },
                    enabled = noteTitle.isNotBlank() || noteContent.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
