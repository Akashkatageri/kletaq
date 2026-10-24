package com.kletaq.app.features.notes

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kletaq.app.core.notes.CloudflareR2Uploader
import com.kletaq.app.core.notes.CompressionResult
import com.kletaq.app.core.notes.NoteCompressionEngine
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.data.repository.NoteRepository
import com.kletaq.app.domain.model.NoteType
import com.kletaq.app.domain.model.StudyNote
import com.kletaq.app.features.lesson.components.HandwrittenNotesViewerDialog
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateNoteScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val noteId = remember { "note_${System.currentTimeMillis()}" }

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Data Structures & Algorithms") }
    var topicTitle by remember { mutableStateOf("") }

    // Attachment state
    val attachedCompressedFiles = remember { mutableStateListOf<File>() }
    var originalBytesCount by remember { mutableStateOf(0L) }
    var isCompressing by remember { mutableStateOf(false) }
    var compressionLabel by remember { mutableStateOf("") }
    var isSavingAndUploading by remember { mutableStateOf(false) }
    var uploadProgressText by remember { mutableStateOf("") }

    // Fullscreen page preview dialog
    var previewInitialPage by remember { mutableIntStateOf(-1) }

    val subjects = listOf(
        "Data Structures & Algorithms",
        "Discrete Mathematics",
        "Applied Chemistry",
        "Operating Systems",
        "Computer Networks"
    )

    // Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isCompressing = true
                compressionLabel = "Starting on-device compression..."
                val result = NoteCompressionEngine.compressNotes(
                    context = context,
                    uris = uris,
                    noteId = noteId,
                    onProgress = { current, total, label ->
                        compressionLabel = label
                    }
                )
                result.onSuccess { compResult ->
                    attachedCompressedFiles.addAll(compResult.compressedFiles)
                    originalBytesCount += compResult.originalTotalBytes
                }.onFailure { err ->
                    Toast.makeText(context, "Compression error: ${err.message}", Toast.LENGTH_SHORT).show()
                }
                isCompressing = false
            }
        }
    }

    // PDF Document Picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isCompressing = true
                compressionLabel = "Rasterizing & compressing PDF pages..."
                val result = NoteCompressionEngine.compressNotes(
                    context = context,
                    uris = listOf(uri),
                    noteId = noteId,
                    onProgress = { _, _, label ->
                        compressionLabel = label
                    }
                )
                result.onSuccess { compResult ->
                    attachedCompressedFiles.addAll(compResult.compressedFiles)
                    originalBytesCount += compResult.originalTotalBytes
                }.onFailure { err ->
                    Toast.makeText(context, "PDF error: ${err.message}", Toast.LENGTH_SHORT).show()
                }
                isCompressing = false
            }
        }
    }

    // Full-screen viewer for previewing attached pages before save
    if (previewInitialPage >= 0 && attachedCompressedFiles.isNotEmpty()) {
        HandwrittenNotesViewerDialog(
            pages = attachedCompressedFiles.map { it.absolutePath },
            initialPage = previewInitialPage,
            topicTitle = noteTitle.ifBlank { "Attached Note Pages" },
            onDismiss = { previewInitialPage = -1 }
        )
    }

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
                text = "Quick Note & Cloud Attachments",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Note Title Input
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = { Text("Note Title *") },
                placeholder = { Text("e.g. Unit 3 Formulas & Hand Derivations") },
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
                label = { Text("Note Content / Summary") },
                placeholder = { Text("Write formulas, summary, key exam insights...") },
                minLines = 4,
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
                placeholder = { Text("e.g. Module 3: Binary Trees") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    focusedLabelColor = PurpleAccent
                )
            )

            // 5. ATTACH HANDWRITTEN NOTES (CAMERA / GALLERY / PDF)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ATTACH NOTE SCANS (CLOUDFLARE R2) ☁️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (attachedCompressedFiles.isNotEmpty()) {
                        Text(
                            text = "${attachedCompressedFiles.size} Pages",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.6f)),
                        enabled = !isCompressing && !isSavingAndUploading
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = PurpleAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photos 📷", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                    }

                    OutlinedButton(
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.6f)),
                        enabled = !isCompressing && !isSavingAndUploading
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = PurpleAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add PDF 📄", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                    }
                }

                // Compression Loading Bar
                AnimatedVisibility(visible = isCompressing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = compressionLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PurpleAccent
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = PurpleAccent
                            )
                        }
                    }
                }

                // Compression Savings Badge
                if (attachedCompressedFiles.isNotEmpty()) {
                    val compressedBytes = attachedCompressedFiles.sumOf { it.length() }
                    val origMb = String.format(Locale.US, "%.1f MB", originalBytesCount / (1024f * 1024f))
                    val compKb = String.format(Locale.US, "%.0f KB", compressedBytes / 1024f)
                    val savings = if (originalBytesCount > 0) {
                        (((originalBytesCount - compressedBytes).toDouble() / originalTotal(originalBytesCount)) * 100).toInt().coerceIn(0, 99)
                    } else 95

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compressed: $origMb ➔ $compKb (${savings}% saved! 🚀)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    // Attached Pages Thumbnail Carousel
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        itemsIndexed(attachedCompressedFiles) { index, file ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(120.dp)
                                    .clickable { previewInitialPage = index },
                                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Page chip
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 6.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Text(
                                            text = "P.${index + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Delete Page button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clickable {
                                                attachedCompressedFiles.removeAt(index)
                                                file.delete()
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.padding(3.dp)
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

        // Bottom Save Note Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        val hasText = noteTitle.isNotBlank() || noteContent.isNotBlank()
                        val hasFiles = attachedCompressedFiles.isNotEmpty()
                        if (hasText || hasFiles) {
                            coroutineScope.launch {
                                isSavingAndUploading = true
                                uploadProgressText = "Uploading to Cloudflare R2..."

                                var finalUrls = emptyList<String>()
                                var originalBytes = originalBytesCount
                                var compressedBytes = 0L

                                if (attachedCompressedFiles.isNotEmpty()) {
                                    compressedBytes = attachedCompressedFiles.sumOf { it.length() }
                                    val uploadResult = CloudflareR2Uploader.uploadNotePages(
                                        context = context,
                                        noteId = noteId,
                                        compressedFiles = attachedCompressedFiles,
                                        onProgress = { uploaded, total, _ ->
                                            uploadProgressText = "Uploaded $uploaded of $total pages..."
                                        }
                                    )
                                    finalUrls = uploadResult.getOrElse {
                                        attachedCompressedFiles.map { it.absolutePath }
                                    }
                                }

                                val finalTitle = noteTitle.ifBlank {
                                    if (noteContent.isNotBlank()) {
                                        noteContent.take(25) + if (noteContent.length > 25) "..." else ""
                                    } else {
                                        "Handwritten Note (${attachedCompressedFiles.size} pages)"
                                    }
                                }

                                val newNote = StudyNote(
                                    id = noteId,
                                    title = finalTitle.trim(),
                                    content = noteContent.trim(),
                                    subjectName = selectedSubject,
                                    topicTitle = topicTitle.ifBlank { null },
                                    noteType = NoteType.GENERAL,
                                    attachmentUrls = finalUrls,
                                    pageCount = finalUrls.size,
                                    originalSizeBytes = originalBytes,
                                    compressedSizeBytes = compressedBytes
                                )

                                NoteRepository.addNote(newNote)
                                CloudflareR2Uploader.cleanStagingFiles(context, noteId)
                                isSavingAndUploading = false
                                onBackClick()
                            }
                        }
                    },
                    enabled = (!isCompressing && !isSavingAndUploading) &&
                            (noteTitle.isNotBlank() || noteContent.isNotBlank() || attachedCompressedFiles.isNotEmpty()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    )
                ) {
                    if (isSavingAndUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uploadProgressText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Text("Save Note", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private fun originalTotal(bytes: Long): Double = if (bytes > 0) bytes.toDouble() else 1.0
