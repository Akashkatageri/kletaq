package com.kletaq.app.features.examsprint.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamSprintScreen(
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

    // Test question count: 5 vs 10
    var targetTestQuestionCount by remember { mutableIntStateOf(5) }

    // Input mode tab: 0 = Paste text, 1 = Upload PDF/Image, 2 = Syllabus topics
    var selectedTab by remember { mutableIntStateOf(0) }

    // Tab 1: Paste text
    var pastedText by remember { mutableStateOf("") }
    var hasExistingAnswers by remember { mutableStateOf(false) }

    // Tab 2: Uploaded file
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var uploadedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var uploadedFileMime by remember { mutableStateOf<String>("application/pdf") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bytes = stream?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    uploadedFileBytes = bytes
                    uploadedFileName = uri.lastPathSegment ?: "Assignment_Document.pdf"
                    uploadedFileMime = context.contentResolver.getType(uri) ?: "application/pdf"
                    Toast.makeText(context, "File loaded: ${bytes.size / 1024} KB", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Loading state
    var isProcessing by remember { mutableStateOf(false) }
    var processingStatus by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "New Exam Sprint",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Set up your targeted question pool & drill",
                        fontSize = 11.sp,
                        color = TextSecondary
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
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = subjectName,
                                onValueChange = { subjectName = it },
                                label = { Text("Subject Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = subjectCode,
                                onValueChange = { subjectCode = it },
                                label = { Text("Code") },
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
                            color = TextPrimary
                        )

                        // Days Remaining Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                                        selectedLabelColor = WarningAmber
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "How many questions will be in the test?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Teacher said they will pick this many questions from the assignment",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(5, 8, 10).forEach { count ->
                                val isSelected = targetTestQuestionCount == count
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { targetTestQuestionCount = count },
                                    label = { Text("$count Questions", fontWeight = FontWeight.SemiBold) },
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
                            color = TextPrimary
                        )

                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Paste Text", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Scan / PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Syllabus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
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
                                                "Paste your 20-30 assignment questions here...\n\nExample:\n1. Explain dual-mode operation in OS.\n2. Describe process state transition diagram.\n3. What is dining philosophers problem?\n\n(If you have answers too, paste them below each question!)",
                                                fontSize = 12.sp,
                                                color = TextSecondary.copy(alpha = 0.6f)
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "💡 Tip: If you already have answers written down, paste them as 'Q1: ... Ans: ...'. We will preserve your answers and extract examiner scoring rubrics!",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Upload Document Mode
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, PrimaryAccentColor.copy(alpha = 0.4f)),
                                        color = PrimaryAccentColor.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                filePickerLauncher.launch("*/*")
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (uploadedFileName != null) Icons.Default.Description else Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = PrimaryAccentColor,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = uploadedFileName ?: "Tap to select assignment PDF or photo",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Supports question sheet PDFs, solved assignment docs, or camera photos",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
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
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Pulls verified exam model answers, PYQs, and scoring tips directly from the official curriculum tree.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
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
                                                color = TextPrimary
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
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = processingStatus,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
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
                                        // Uploaded file
                                        val bytes = uploadedFileBytes
                                        if (bytes == null || bytes.isEmpty()) {
                                            isProcessing = false
                                            Toast.makeText(context, "Please choose a file first", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        processingStatus = "Multimodal AI extracting questions & answers from document..."
                                        val result = ExamSprintAiHelper.parseUploadedAssignment(bytes, uploadedFileMime, subjectName)
                                        result.getOrNull() ?: emptyList()
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
                                    Toast.makeText(context, "Could not extract questions. Please check input.", Toast.LENGTH_SHORT).show()
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
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

