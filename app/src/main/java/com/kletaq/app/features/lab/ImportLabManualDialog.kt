package com.kletaq.app.features.lab

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import kotlinx.coroutines.launch

private enum class ImportTab(val title: String, val icon: String) {
    UPLOAD_PDF("Upload PDF", "📄"),
    PASTE_CODE("Paste Code", "📋")
}

@Composable
fun ImportLabManualDialog(
    subjectId: String,
    subjectName: String = "",
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ImportTab.UPLOAD_PDF) }

    // PDF state
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf<String?>(null) }
    var selectedPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Text state
    var manualText by remember { mutableStateOf("") }

    // Progress and Error states
    var isDeconstructing by remember { mutableStateOf(false) }
    var deconstructionPhase by remember { mutableStateOf("Analyzing with Gemini AI...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasCustom = remember(subjectId) {
        LabDefenseRepository.hasCustomLabPrograms(context, subjectId)
    }
    val existingPdfFile = remember(subjectId) {
        LabDefenseRepository.getCustomLabManualPdfFile(context, subjectId)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            try {
                // Query display name
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        selectedPdfName = cursor.getString(nameIndex)
                    }
                }
                if (selectedPdfName == null) {
                    selectedPdfName = uri.lastPathSegment ?: "lab_manual.pdf"
                }

                // Read bytes
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    selectedPdfBytes = bytes
                    errorMessage = null
                    Toast.makeText(context, "Selected ${selectedPdfName}! Ready to deconstruct 🚀", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Selected PDF file was empty or unreadable."
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load PDF: ${e.message}"
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isDeconstructing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📄 COLLEGE LAB MANUAL DECONSTRUCTOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (subjectName.isNotBlank()) subjectName else subjectId,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isDeconstructing) {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Upload your college's Lab Manual PDF or paste program code. Gemini AI generates Steps Order, Core Function Blackout, Crash-Traps, and Viva Q&As directly from your manual.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Banner if custom programs already active
                if (hasCustom) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✅", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Custom Lab Manual Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (existingPdfFile != null) {
                                    TextButton(
                                        onClick = { LabDefenseRepository.openLabManualPdf(context, subjectId) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("View PDF", fontSize = 11.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        LabDefenseRepository.clearCustomLabPrograms(context, subjectId)
                                        Toast.makeText(context, "Deleted custom manual. Restored standard syllabus! 🔄", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                        onDismiss()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Reset to Syllabus", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Tab Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImportTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PurpleAccent else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isDeconstructing) { selectedTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = tab.icon, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        ImportTab.UPLOAD_PDF -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (selectedPdfBytes != null) {
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(42.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = selectedPdfName ?: "lab_manual.pdf",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val sizeKb = (selectedPdfBytes!!.size / 1024)
                                            val sizeLabel = if (sizeKb > 1024) String.format("%.2f MB", sizeKb / 1024f) else "$sizeKb KB"
                                            Text(
                                                text = "File size: $sizeLabel • Ready for AI Deconstruction",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { pdfLauncher.launch("application/pdf") },
                                                enabled = !isDeconstructing,
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text("Choose Different PDF", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isDeconstructing) {
                                                pdfLauncher.launch("application/pdf")
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Select College Lab Manual PDF",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Choose your college's official PDF manual (from WhatsApp, Drive, or Downloads)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { pdfLauncher.launch("application/pdf") },
                                                enabled = !isDeconstructing,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Browse PDF Document 📄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ImportTab.PASTE_CODE -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Paste Source Code or Syllabus:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                manualText = clip
                                                Toast.makeText(context, "Pasted from clipboard! 📋", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Paste from Clipboard", fontSize = 11.sp)
                                    }
                                }

                                OutlinedTextField(
                                    value = manualText,
                                    onValueChange = { manualText = it },
                                    placeholder = {
                                        Text(
                                            text = "// Paste your college lab programs or syllabus text here...\n\n// Program 1: Implement Stack using Array...\n// #include <stdio.h>\n// ...",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary AI Deconstruct Action Button
                val canSubmit = when (selectedTab) {
                    ImportTab.UPLOAD_PDF -> selectedPdfBytes != null && !isDeconstructing
                    ImportTab.PASTE_CODE -> manualText.trim().length >= 50 && !isDeconstructing
                }

                Button(
                    onClick = {
                        isDeconstructing = true
                        errorMessage = null

                        scope.launch {
                            val result = when (selectedTab) {
                                ImportTab.UPLOAD_PDF -> {
                                    deconstructionPhase = "Reading PDF & deconstructing with Gemini AI..."
                                    val r = LabManualAiParser.parseLabManualPdf(selectedPdfBytes!!)
                                    if (r.isSuccess) {
                                        LabDefenseRepository.saveLabManualPdf(context, subjectId, selectedPdfBytes!!)
                                    }
                                    r
                                }
                                ImportTab.PASTE_CODE -> {
                                    deconstructionPhase = "Analyzing program code with Gemini AI..."
                                    LabManualAiParser.parseLabManualText(manualText)
                                }
                            }

                            isDeconstructing = false
                            if (result.isSuccess) {
                                val programs = result.getOrNull().orEmpty()
                                LabDefenseRepository.saveCustomLabPrograms(context, subjectId, programs)
                                Toast.makeText(context, "Successfully loaded ${programs.size} real lab programs! 🎉", Toast.LENGTH_LONG).show()
                                onSuccess()
                                onDismiss()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to generate lab programs."
                            }
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    if (isDeconstructing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deconstructionPhase,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedTab) {
                                ImportTab.UPLOAD_PDF -> "✨ Deconstruct PDF with AI"
                                ImportTab.PASTE_CODE -> "✨ Deconstruct Pasted Code with AI"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
