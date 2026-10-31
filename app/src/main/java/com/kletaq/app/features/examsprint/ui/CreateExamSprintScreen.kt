package com.kletaq.app.features.examsprint.ui

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.ai.SecureAiPreferences
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.WarningAmber
import com.kletaq.app.features.examsprint.ai.ExamSprintAiHelper
import com.kletaq.app.features.examsprint.data.ExamSprintRepository
import com.kletaq.app.features.examsprint.model.ExamSprint
import com.kletaq.app.features.examsprint.model.UploadedDocumentRole
import com.kletaq.app.features.examsprint.model.UploadedExamDocument
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * ⚡ CreateExamSprintScreen:
 * Setup flow to create a targeted exam preparation sprint.
 * Supports:
 * 1. Pasted text (Questions only, or Questions + Answers)
 * 2. Multi-document upload (Question Paper PDF + Answers/Solutions PDF + Reference Notes)
 * 3. Official curriculum syllabus module topics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamSprintScreen(
    initialPdfPath: String? = null,
    onBackClick: () -> Unit,
    onSprintCreated: (sprintId: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var subjectName by remember { mutableStateOf("Operating Systems") }
    var subjectCode by remember { mutableStateOf("1BCS304") }
    var sprintTitle by remember { mutableStateOf("IA-1 Question Bank") }

    // Exam Date selection: 0 = Today, 1 = Tomorrow, 2 = In 2 Days, 3 = In 3 Days, 5 = In 5 Days
    var daysUntilExam by remember { mutableIntStateOf(2) }

    // Test question count: 5 vs 8 vs 10
    var targetTestQuestionCount by remember { mutableIntStateOf(5) }

    // Input mode tab: 0 = Paste text, 1 = Upload PDF/Image, 2 = Syllabus topics
    var selectedTab by remember { mutableIntStateOf(0) }

    // Tab 0: Paste text
    var pastedText by remember { mutableStateOf("") }

    // Tab 1: Uploaded documents list (supports question sheet + answer sheet)
    val uploadedDocuments = remember { mutableStateListOf<UploadedExamDocument>() }

    // Helper to query file name and size from content Uri
    fun queryDocInfo(uri: Uri): Pair<String, Long> {
        var name = "Document_${System.currentTimeMillis() % 1000}.pdf"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}
        return name to size
    }

    // Launchers for Question Doc, Answers Doc, and Generic Doc
    val pickQuestionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryDocInfo(uri)
            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            // Replace existing QUESTIONS role doc or add new
            uploadedDocuments.removeAll { it.role == UploadedDocumentRole.QUESTIONS }
            uploadedDocuments.add(
                0,
                UploadedExamDocument(
                    name = name,
                    uri = uri,
                    sizeBytes = size,
                    mimeType = mime,
                    role = UploadedDocumentRole.QUESTIONS
                )
            )
            Toast.makeText(context, "Added Questions Paper: $name", Toast.LENGTH_SHORT).show()
        }
    }

    val pickAnswersLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryDocInfo(uri)
            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            uploadedDocuments.removeAll { it.role == UploadedDocumentRole.ANSWERS }
            uploadedDocuments.add(
                UploadedExamDocument(
                    name = name,
                    uri = uri,
                    sizeBytes = size,
                    mimeType = mime,
                    role = UploadedDocumentRole.ANSWERS
                )
            )
            Toast.makeText(context, "Added Solutions Sheet: $name", Toast.LENGTH_SHORT).show()
        }
    }

    val pickGenericLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryDocInfo(uri)
            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            uploadedDocuments.add(
                UploadedExamDocument(
                    name = name,
                    uri = uri,
                    sizeBytes = size,
                    mimeType = mime,
                    role = UploadedDocumentRole.REFERENCE
                )
            )
            Toast.makeText(context, "Added Document: $name", Toast.LENGTH_SHORT).show()
        }
    }

    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEachIndexed { index, uri ->
                val (name, size) = queryDocInfo(uri)
                val mime = context.contentResolver.getType(uri) ?: "application/pdf"
                val role = when {
                    index == 0 && uploadedDocuments.none { it.role == UploadedDocumentRole.QUESTIONS } -> UploadedDocumentRole.QUESTIONS
                    index == 1 && uploadedDocuments.none { it.role == UploadedDocumentRole.ANSWERS } -> UploadedDocumentRole.ANSWERS
                    else -> UploadedDocumentRole.REFERENCE
                }
                uploadedDocuments.add(
                    UploadedExamDocument(
                        name = name,
                        uri = uri,
                        sizeBytes = size,
                        mimeType = mime,
                        role = role
                    )
                )
            }
            Toast.makeText(context, "Added ${uris.size} document(s)", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-populate initial PDF if navigated from PDF Studio
    LaunchedEffect(initialPdfPath) {
        if (!initialPdfPath.isNullOrBlank()) {
            val file = File(initialPdfPath)
            if (file.exists()) {
                val uri = Uri.fromFile(file)
                uploadedDocuments.clear()
                uploadedDocuments.add(
                    UploadedExamDocument(
                        name = file.name,
                        uri = uri,
                        sizeBytes = file.length(),
                        mimeType = "application/pdf",
                        role = UploadedDocumentRole.QUESTIONS
                    )
                )
                selectedTab = 1
            }
        }
    }

    // Loading state
    var isProcessing by remember { mutableStateOf(false) }
    var processingStatus by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "New Exam Sprint",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Set up your targeted question pool & drill",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Subject & Sprint Name
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Course & Sprint Target",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = subjectName,
                                onValueChange = { subjectName = it },
                                label = { Text("Subject Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = subjectCode,
                                onValueChange = { subjectCode = it },
                                label = { Text("Code", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = sprintTitle,
                            onValueChange = { sprintTitle = it },
                            label = { Text("Sprint Title (e.g. IA-1 Assignment)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 2: Exam Timeline & Target Sampling
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Exam Timeline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Days Remaining Chips - Horizontal Scrollable to prevent ANY overflow
                        val daysScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(daysScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "Today",
                                1 to "Tomorrow",
                                2 to "In 2 Days",
                                3 to "In 3 Days",
                                5 to "In 5 Days"
                            ).forEach { (d, label) ->
                                val isSelected = daysUntilExam == d
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { daysUntilExam = d },
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                                        selectedLabelColor = WarningAmber
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "How many questions will be in the test?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Teacher said they will pick this many questions from the pool",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )

                        // Question Count Chips - Horizontal Scrollable
                        val countScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(countScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 8, 10, 15).forEach { count ->
                                val isSelected = targetTestQuestionCount == count
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { targetTestQuestionCount = count },
                                    label = { Text("$count Questions", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryAccentColor.copy(alpha = 0.2f),
                                        selectedLabelColor = PrimaryAccentColor
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 3: Question & Answer Source Input
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Assignment / Question Pool Source",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Text("Paste Text", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Text("Upload PDFs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Text("Syllabus", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            )
                        }

                        when (selectedTab) {
                            0 -> {
                                // Paste Text Mode
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = pastedText,
                                        onValueChange = { pastedText = it },
                                        placeholder = {
                                            Text(
                                                "Paste your assignment questions here...\n\nExample:\n1. Explain dual-mode operation in OS.\n2. Describe process state transition diagram.\n3. What is dining philosophers problem?\n\n(If you have answers too, paste them below each question!)",
                                                fontSize = 12.sp,
                                                color = TextSecondary.copy(alpha = 0.6f)
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "💡 Tip: If you already have solutions written down, paste them as 'Q1: ... Ans: ...'. We will preserve your answers and extract examiner scoring rubrics!",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Multi-Document Upload Mode (Questions File + Answers File)
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val hasQuestionsDoc = uploadedDocuments.any { it.role == UploadedDocumentRole.QUESTIONS }
                                    val hasAnswersDoc = uploadedDocuments.any { it.role == UploadedDocumentRole.ANSWERS }

                                    if (!SecureAiPreferences.hasCustomKeys()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = PrimaryAccentColor.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, PrimaryAccentColor.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = PrimaryAccentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Want 100% Precise Math & Handwriting Vision?",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "Connect a free Gemini key in Settings -> AI Keys for instant multimodal math extraction.",
                                                        fontSize = 10.sp,
                                                        color = TextSecondary,
                                                        lineHeight = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Slot 1: Questions Document Picker
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasQuestionsDoc) SuccessGreen.copy(alpha = 0.08f) else PrimaryAccentColor.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, if (hasQuestionsDoc) SuccessGreen.copy(alpha = 0.3f) else PrimaryAccentColor.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { pickQuestionsLauncher.launch("*/*") }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (hasQuestionsDoc) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = if (hasQuestionsDoc) SuccessGreen else PrimaryAccentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (hasQuestionsDoc) "Questions Document Selected" else "Upload Questions PDF / Photos",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (hasQuestionsDoc) "Tap to replace question paper" else "Assignment sheet, question bank, or exam photos",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (hasQuestionsDoc) SuccessGreen.copy(alpha = 0.2f) else PrimaryAccentColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Required",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (hasQuestionsDoc) SuccessGreen else PrimaryAccentColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Slot 2: Answer Scheme / Solved Notes Document Picker
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasAnswersDoc) SuccessGreen.copy(alpha = 0.08f) else WarningAmber.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, if (hasAnswersDoc) SuccessGreen.copy(alpha = 0.3f) else WarningAmber.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { pickAnswersLauncher.launch("*/*") }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (hasAnswersDoc) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                                contentDescription = null,
                                                tint = if (hasAnswersDoc) SuccessGreen else WarningAmber,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (hasAnswersDoc) "Solutions Document Selected" else "+ Add Answer Scheme / Notes PDF",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (hasAnswersDoc) "Tap to replace solutions document" else "Matches solutions to question paper automatically",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (hasAnswersDoc) SuccessGreen.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Optional",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (hasAnswersDoc) SuccessGreen else WarningAmber,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Attached Documents List (with remove and role tags)
                                    if (uploadedDocuments.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "ATTACHED FILES (${uploadedDocuments.size}):",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp,
                                                color = TextSecondary
                                            )

                                            uploadedDocuments.forEachIndexed { idx, doc ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = CardSurface,
                                                    border = BorderStroke(0.5.dp, BorderColor),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (doc.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.Description,
                                                            contentDescription = null,
                                                            tint = when (doc.role) {
                                                                UploadedDocumentRole.QUESTIONS -> PrimaryAccentColor
                                                                UploadedDocumentRole.ANSWERS -> WarningAmber
                                                                UploadedDocumentRole.REFERENCE -> TextSecondary
                                                            },
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = doc.name,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = when (doc.role) {
                                                                        UploadedDocumentRole.QUESTIONS -> PrimaryAccentColor.copy(alpha = 0.15f)
                                                                        UploadedDocumentRole.ANSWERS -> WarningAmber.copy(alpha = 0.15f)
                                                                        UploadedDocumentRole.REFERENCE -> MaterialTheme.colorScheme.surfaceVariant
                                                                    },
                                                                    modifier = Modifier.clickable {
                                                                        val nextRole = when (doc.role) {
                                                                            UploadedDocumentRole.QUESTIONS -> UploadedDocumentRole.ANSWERS
                                                                            UploadedDocumentRole.ANSWERS -> UploadedDocumentRole.REFERENCE
                                                                            UploadedDocumentRole.REFERENCE -> UploadedDocumentRole.QUESTIONS
                                                                        }
                                                                        uploadedDocuments[idx] = doc.copy(role = nextRole)
                                                                    }
                                                                ) {
                                                                    Text(
                                                                        text = when (doc.role) {
                                                                            UploadedDocumentRole.QUESTIONS -> "Questions Sheet ⟳"
                                                                            UploadedDocumentRole.ANSWERS -> "Solutions Sheet ⟳"
                                                                            UploadedDocumentRole.REFERENCE -> "Reference Notes ⟳"
                                                                        },
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = when (doc.role) {
                                                                            UploadedDocumentRole.QUESTIONS -> PrimaryAccentColor
                                                                            UploadedDocumentRole.ANSWERS -> WarningAmber
                                                                            UploadedDocumentRole.REFERENCE -> TextSecondary
                                                                        },
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                                if (doc.sizeBytes > 0) {
                                                                    Text(
                                                                        text = "• ${formatBytes(doc.sizeBytes)}",
                                                                        fontSize = 9.sp,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = { uploadedDocuments.removeAt(idx) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.DeleteOutline,
                                                                contentDescription = "Remove file",
                                                                tint = Color(0xFFEF4444),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { pickGenericLauncher.launch("*/*") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.heightIn(min = 32.dp).weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("+ Add File", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }

                                                OutlinedButton(
                                                    onClick = { pickMultipleLauncher.launch("*/*") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.heightIn(min = 32.dp).weight(1f)
                                                ) {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pick Multiple", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Syllabus Modules Mode
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Official Syllabus Topics (Verified Assets)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Pulls verified exam model answers, PYQs, and scoring tips directly from the official curriculum tree.",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SuccessGreen.copy(alpha = 0.12f),
                                        border = BorderStroke(0.5.dp, SuccessGreen.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Will automatically populate Modules 1 & 2 exam bank for $subjectCode",
                                                fontSize = 11.sp,
                                                color = TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Processing Indicator
                if (isProcessing) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = WarningAmber,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = processingStatus.ifBlank { "Processing study materials..." },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WarningAmber,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        isProcessing = true
                        processingStatus = "Analyzing questions & generating study answers..."

                        coroutineScope.launch {
                            try {
                                val examDateMillis = System.currentTimeMillis() + (daysUntilExam * 86400000L)
                                val sprintId = UUID.randomUUID().toString()

                                val questions = when (selectedTab) {
                                    0 -> {
                                        // Pasted text
                                        val text = pastedText.trim()
                                        if (text.isBlank()) {
                                            isProcessing = false
                                            Toast.makeText(context, "Please paste questions first", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        ExamSprintAiHelper.parsePastedQuestionsAndAnswers(text, subjectName, subjectCode)
                                    }
                                    1 -> {
                                        // Uploaded files (multi-document support)
                                        if (uploadedDocuments.isEmpty()) {
                                            isProcessing = false
                                            Toast.makeText(context, "Please upload at least 1 document", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        processingStatus = "Starting extraction from ${uploadedDocuments.size} document(s)..."
                                        ExamSprintAiHelper.parseUploadedDocuments(
                                            context = context,
                                            documents = uploadedDocuments,
                                            subjectName = subjectName,
                                            subjectCode = subjectCode,
                                            onProgress = { status ->
                                                processingStatus = status
                                            }
                                        )
                                    }
                                    else -> {
                                        // Syllabus
                                        processingStatus = "Extracting verified curriculum study bank..."
                                        val sampleTopics = listOf(
                                            "vtu-ise-s3-1bcs304-2025-m1-t1",
                                            "vtu-ise-s3-1bcs304-2025-m1-t2",
                                            "vtu-ise-s3-1bcs304-2025-m2-t1"
                                        )
                                        ExamSprintAiHelper.extractFromSyllabus(subjectCode, subjectName, sampleTopics)
                                    }
                                }

                                if (questions.isEmpty()) {
                                    isProcessing = false
                                    Toast.makeText(context, "Could not extract questions. Please check document content.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }

                                val newSprint = ExamSprint(
                                    id = sprintId,
                                    title = sprintTitle.ifBlank { "$subjectName Sprint" },
                                    subjectCode = subjectCode,
                                    subjectName = subjectName,
                                    examDateMillis = examDateMillis,
                                    targetTestQuestionsCount = targetTestQuestionCount,
                                    targetTestDurationMinutes = 60,
                                    questions = questions,
                                    mockTestsTaken = 0,
                                    bestScore = 0
                                )

                                ExamSprintRepository.saveSprint(newSprint)
                                isProcessing = false
                                onSprintCreated(sprintId)
                            } catch (e: Exception) {
                                isProcessing = false
                                Toast.makeText(context, "Error setting up sprint: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProcessing) "Generating Exam Sprint..." else "Generate Exam Sprint",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
