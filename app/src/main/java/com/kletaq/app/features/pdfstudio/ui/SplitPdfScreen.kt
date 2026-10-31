package com.kletaq.app.features.pdfstudio.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.pdfstudio.data.PdfStudioHistoryRepository
import com.kletaq.app.features.pdfstudio.data.PdfToolType
import com.kletaq.app.features.pdfstudio.data.RecentPdfDocument
import com.kletaq.app.features.pdfstudio.engine.PdfStudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun SplitPdfScreen(
    onBackClick: () -> Unit,
    onNavigateToExamSprintWithFile: (filePath: String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf(0) }

    val selectedIndices = remember { mutableStateListOf<Int>() }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap>() }
    var rangeTextInput by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("") }

    var isExtracting by remember { mutableStateOf(false) }
    var progressCurrent by remember { mutableStateOf(0) }
    var progressTotal by remember { mutableStateOf(0) }
    var extractedFile by remember { mutableStateOf<File?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            selectedPdfName = PdfStudioEngine.getFileName(context, uri)
            val pages = PdfStudioEngine.getPageCount(context, uri)
            totalPages = pages
            selectedIndices.clear()
            pageThumbnails.clear()
            outputName = "Extracted_" + selectedPdfName.removeSuffix(".pdf")
            extractedFile = null

            // Asynchronously load page thumbnails
            scope.launch {
                withContext(Dispatchers.IO) {
                    for (i in 0 until pages) {
                        val thumb = PdfStudioEngine.renderPageThumbnail(context, uri, i, maxDimension = 240)
                        if (thumb != null) {
                            withContext(Dispatchers.Main) {
                                pageThumbnails[i] = thumb
                            }
                        }
                    }
                }
            }
        }
    }

    fun parseAndApplyRange(input: String) {
        val indices = mutableSetOf<Int>()
        val parts = input.split(",")
        for (rawPart in parts) {
            val part = rawPart.trim()
            if (part.contains("-")) {
                val sub = part.split("-")
                val start = sub.getOrNull(0)?.trim()?.toIntOrNull()
                val end = sub.getOrNull(1)?.trim()?.toIntOrNull()
                if (start != null && end != null) {
                    val s = minOf(start, end).coerceAtLeast(1)
                    val e = maxOf(start, end).coerceAtMost(totalPages)
                    for (p in s..e) {
                        indices.add(p - 1)
                    }
                }
            } else {
                val single = part.toIntOrNull()
                if (single != null && single in 1..totalPages) {
                    indices.add(single - 1)
                }
            }
        }
        selectedIndices.clear()
        selectedIndices.addAll(indices)
    }

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
                            text = "Split & Extract Pages",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Extract specific pages or assignment chapters",
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
            // Success Card (if extracted)
            if (extractedFile != null) {
                val file = extractedFile!!
                item {
                    ConversionSuccessCard(
                        file = file,
                        pageCount = selectedIndices.size,
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

            // PDF Picker
            item {
                if (selectedPdfUri == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pdfPickerLauncher.launch("application/pdf") },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select PDF Document",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose a PDF to view thumbnails and select pages to extract",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedPdfName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$totalPages total pages • ${selectedIndices.size} selected",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(text = "Change", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (selectedPdfUri != null) {
                // Quick Selection & Range Input
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PAGE SELECTION (${selectedIndices.size} / $totalPages)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Select All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedIndices.clear()
                                            for (i in 0 until totalPages) selectedIndices.add(i)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = "Clear",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedIndices.clear()
                                            rangeTextInput = ""
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Range text field
                        OutlinedTextField(
                            value = rangeTextInput,
                            onValueChange = {
                                rangeTextInput = it
                                parseAndApplyRange(it)
                            },
                            placeholder = { Text("e.g. 1, 3, 5-8", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                // Grid of Page Thumbnails (Rendered in chunks of 3 for smooth scrolling inside LazyColumn)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val allPages = (0 until totalPages).toList()
                        for (row in allPages.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (pageIndex in row) {
                                    val isSelected = selectedIndices.contains(pageIndex)
                                    val bitmap = pageThumbnails[pageIndex]

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.72f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (isSelected) {
                                                    selectedIndices.remove(pageIndex)
                                                } else {
                                                    selectedIndices.add(pageIndex)
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 0.5.dp,
                                            if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Page ${pageIndex + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(20.dp),
                                                        color = Color(0xFFF59E0B)
                                                    )
                                                }
                                            }

                                            // Top Page Number Badge
                                            Surface(
                                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                color = Color.Black.copy(alpha = 0.7f),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "${pageIndex + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Selection Checkmark Circle
                                            if (isSelected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFF59E0B),
                                                    modifier = Modifier
                                                        .padding(6.dp)
                                                        .size(22.dp)
                                                        .align(Alignment.TopEnd)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Empty fillers to preserve column widths if last row has < 3 items
                                for (i in row.size until 3) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Output File Name
                item {
                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
                        label = { Text("Extracted PDF Name") },
                        suffix = { Text(".pdf", color = Color(0xFFF59E0B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Action Extract Button
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isExtracting) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = if (progressTotal > 0) progressCurrent.toFloat() / progressTotal.toFloat() else 0f,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                        )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Extracting page $progressCurrent of $progressTotal...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val uri = selectedPdfUri ?: return@Button
                                if (selectedIndices.isEmpty()) {
                                    Toast.makeText(context, "Please select at least 1 page", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isExtracting = true
                                progressCurrent = 0
                                progressTotal = selectedIndices.size

                                scope.launch {
                                    val result = PdfStudioEngine.splitAndExtractPdf(
                                        context = context,
                                        pdfUri = uri,
                                        selectedPageIndices = selectedIndices.toList(),
                                        customFileName = outputName,
                                        onProgress = { cur, tot ->
                                            progressCurrent = cur
                                            progressTotal = tot
                                        }
                                    )

                                    isExtracting = false
                                    result.onSuccess { file ->
                                        extractedFile = file
                                        PdfStudioHistoryRepository.addRecentDocument(
                                            context,
                                            RecentPdfDocument(
                                                id = UUID.randomUUID().toString(),
                                                fileName = file.name,
                                                filePath = file.absolutePath,
                                                fileSizeBytes = file.length(),
                                                pageCount = selectedIndices.size,
                                                toolType = PdfToolType.SPLIT_PDF
                                            )
                                        )
                                        Toast.makeText(context, "Extracted ${selectedIndices.size} pages successfully!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { error ->
                                        Toast.makeText(context, "Extraction failed: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = selectedIndices.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CallSplit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedIndices.isNotEmpty()) "Extract ${selectedIndices.size} Pages" else "Select Pages to Extract",
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
}

