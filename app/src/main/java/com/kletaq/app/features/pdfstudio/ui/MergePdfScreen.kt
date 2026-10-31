package com.kletaq.app.features.pdfstudio.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.pdfstudio.data.PdfStudioHistoryRepository
import com.kletaq.app.features.pdfstudio.data.PdfToolType
import com.kletaq.app.features.pdfstudio.data.RecentPdfDocument
import com.kletaq.app.features.pdfstudio.engine.PdfStudioEngine
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

data class SelectedPdfItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int
)

@Composable
fun MergePdfScreen(
    onBackClick: () -> Unit,
    onNavigateToExamSprintWithFile: (filePath: String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedPdfs = remember { mutableStateListOf<SelectedPdfItem>() }
    var outputName by remember { mutableStateOf("Merged_Notes_${System.currentTimeMillis() % 10000}") }

    var isMerging by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var mergedFile by remember { mutableStateOf<File?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                val name = PdfStudioEngine.getFileName(context, uri)
                val size = PdfStudioEngine.getFileSize(context, uri)
                val pages = PdfStudioEngine.getPageCount(context, uri)
                selectedPdfs.add(SelectedPdfItem(uri, name, size, pages))
            }
            mergedFile = null
        }
    }

    val totalPages = selectedPdfs.sumOf { it.pageCount }
    val totalBytes = selectedPdfs.sumOf { it.sizeBytes }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Merge PDFs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Combine multiple question papers or notes in order",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Card (if merged)
            if (mergedFile != null) {
                val file = mergedFile!!
                item {
                    ConversionSuccessCard(
                        file = file,
                        pageCount = totalPages,
                        onOpen = {
                            try {
                                context.startActivity(PdfStudioEngine.createViewIntent(context, file))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            try {
                                context.startActivity(PdfStudioEngine.createShareIntent(context, file))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSaveToDownloads = {
                            val uri = PdfStudioEngine.saveToPublicDownloads(context, file)
                            if (uri != null) {
                                Toast.makeText(context, "Saved to Downloads/Kletaq_PDF/${file.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSendToExamSprint = {
                            onNavigateToExamSprintWithFile(file.absolutePath)
                        }
                    )
                }
            }

            // Output File Name
            item {
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Merged PDF Name") },
                    suffix = { Text(".pdf", color = Color(0xFF6366F1)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // List Header & Add Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PDF DOCUMENTS TO MERGE (${selectedPdfs.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (selectedPdfs.isNotEmpty()) {
                            Text(
                                text = "$totalPages total pages • ${formatBytes(totalBytes)}",
                                fontSize = 10.sp,
                                color = Color(0xFF6366F1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                            contentColor = Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add PDFs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (selectedPdfs.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pdfPickerLauncher.launch("application/pdf") },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MergeType,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select 2 or More PDFs to Merge",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "They will be merged into a single document in this exact sequence.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(selectedPdfs) { index, item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.pageCount} pages • ${formatBytes(item.sizeBytes)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Move Up
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val elem = selectedPdfs.removeAt(index)
                                        selectedPdfs.add(index - 1, elem)
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Move Down
                            IconButton(
                                onClick = {
                                    if (index < selectedPdfs.size - 1) {
                                        val elem = selectedPdfs.removeAt(index)
                                        selectedPdfs.add(index + 1, elem)
                                    }
                                },
                                enabled = index < selectedPdfs.size - 1,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (index < selectedPdfs.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete
                            IconButton(
                                onClick = { selectedPdfs.removeAt(index) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Merge Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (isMerging) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            color = Color(0xFF6366F1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = progressStatus.ifBlank { "Merging documents..." },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6366F1)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (selectedPdfs.size < 2) {
                                Toast.makeText(context, "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isMerging = true
                            progressStatus = "Starting merge..."

                            scope.launch {
                                val result = PdfStudioEngine.mergePdfs(
                                    context = context,
                                    pdfUris = selectedPdfs.map { it.uri },
                                    customFileName = outputName,
                                    onProgress = { curDoc, totDocs, curPage ->
                                        progressStatus = "Merging doc $curDoc of $totDocs ($curPage)..."
                                    }
                                )

                                isMerging = false
                                result.onSuccess { file ->
                                    mergedFile = file
                                    PdfStudioHistoryRepository.addRecentDocument(
                                        context,
                                        RecentPdfDocument(
                                            id = UUID.randomUUID().toString(),
                                            fileName = file.name,
                                            filePath = file.absolutePath,
                                            fileSizeBytes = file.length(),
                                            pageCount = totalPages,
                                            toolType = PdfToolType.MERGE_PDF
                                        )
                                    )
                                    Toast.makeText(context, "Merged ${selectedPdfs.size} PDFs successfully!", Toast.LENGTH_SHORT).show()
                                }.onFailure { error ->
                                    Toast.makeText(context, "Merge failed: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = selectedPdfs.size >= 2,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MergeType, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPdfs.size >= 2) "Merge ${selectedPdfs.size} PDFs ($totalPages Pages)" else "Select at least 2 PDFs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

