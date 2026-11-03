package com.kletaq.app.features.memorypalace.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.memorypalace.ai.MemoryPalaceAiHelper
import com.kletaq.app.features.memorypalace.data.MemoryPalaceRepository
import com.kletaq.app.features.memorypalace.model.MemoryPalace
import com.kletaq.app.features.memorypalace.model.PalaceDerivation
import com.kletaq.app.features.memorypalace.model.PalaceStep
import com.kletaq.app.features.memorypalace.ui.components.DerivationPreviewSheet
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

/**
 * 📜 Create Derivation Screen
 * Upload notes/PDF, select a target Memory Palace, configure exam timing, and let AI decompose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDerivationScreen(
    initialPalaceId: String? = null,
    onDerivationCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val palaces by MemoryPalaceRepository.palaces.collectAsState()
    var selectedPalaceId by remember {
        mutableStateOf(initialPalaceId ?: palaces.firstOrNull()?.id ?: "")
    }
    var isPalaceDropdownExpanded by remember { mutableStateOf(false) }

    var derivationTitle by remember { mutableStateOf("") }
    val pickedUris = remember { mutableStateListOf<Uri>() }

    // Mode: "EXAM_SPRINT" vs "SEMESTER"
    var selectedMode by remember { mutableStateOf("EXAM_SPRINT") }

    // Exam date/time (default: tomorrow at 9:00 AM)
    var examCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        )
    }

    var isGenerating by remember { mutableStateOf(false) }
    var generationProgressText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val combined = (pickedUris + uris).take(4)
            pickedUris.clear()
            pickedUris.addAll(combined)
            if (uris.size > 4) {
                Toast.makeText(context, "Capped to 4 pages for optimal vision precision", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val activePalaceComposite = remember(selectedPalaceId, palaces) {
        if (selectedPalaceId.isNotBlank()) {
            MemoryPalaceRepository.getPalaceWithLoci(selectedPalaceId)
        } else null
    }

    var pendingPreviewSteps by remember { mutableStateOf<List<PalaceStep>?>(null) }
    var pendingSourcePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingDerivationId by remember { mutableStateOf("") }

    if (pendingPreviewSteps != null) {
        val composite = activePalaceComposite
        DerivationPreviewSheet(
            topicTitle = derivationTitle.trim().ifBlank { "Derivation Sequence" },
            initialSteps = pendingPreviewSteps!!,
            loci = composite?.loci ?: emptyList(),
            onConfirmAndSave = { finalSteps ->
                val resolvedTitle = derivationTitle.trim().ifBlank { "Derivation #1" }
                val (initialNextReview, initialStage) = MemoryPalaceRepository.calculateNextReview(
                    mode = selectedMode,
                    currentStage = 0,
                    examTimestamp = if (selectedMode == "EXAM_SPRINT") examCalendar.timeInMillis else null,
                    rememberedCleanly = true
                )

                val derivation = PalaceDerivation(
                    id = pendingDerivationId,
                    palaceId = selectedPalaceId,
                    title = resolvedTitle,
                    mode = selectedMode,
                    examTimestamp = if (selectedMode == "EXAM_SPRINT") examCalendar.timeInMillis else null,
                    nextReviewTime = initialNextReview,
                    reviewStage = initialStage,
                    sourceDocumentPaths = pendingSourcePaths
                )

                MemoryPalaceRepository.saveDerivationWithSteps(derivation, finalSteps)
                Toast.makeText(context, "Anchored ${finalSteps.size} steps to your Palace!", Toast.LENGTH_SHORT).show()
                onDerivationCreated(pendingDerivationId)
            },
            onBackToInputs = {
                pendingPreviewSteps = null
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Anchor Topic / Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Palace Dropdown
            Text(
                text = "Target Memory Palace",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (palaces.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "No Memory Palaces found. Please create a palace with room photos first!",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp
                    )
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = isPalaceDropdownExpanded,
                    onExpandedChange = { isPalaceDropdownExpanded = !isPalaceDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentPalaceName = palaces.firstOrNull { it.id == selectedPalaceId }?.name
                        ?: "Select a Palace"

                    OutlinedTextField(
                        value = currentPalaceName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPalaceDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isPalaceDropdownExpanded,
                        onDismissRequest = { isPalaceDropdownExpanded = false }
                    ) {
                        palaces.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        Text("${p.totalLociCount} pinned loci", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    selectedPalaceId = p.id
                                    isPalaceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Topic Title
            OutlinedTextField(
                value = derivationTitle,
                onValueChange = { derivationTitle = it },
                label = { Text("Topic / Derivation Title") },
                placeholder = { Text("e.g. Poisson Derivation, Process Scheduling, French Revolution") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Document Upload Section (Max 4 pages)
            Text(
                text = "Study Notes, Pages or Photos (Up to 4)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2563EB).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = Color(0xFF2563EB)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (pickedUris.isEmpty()) "Select PDF or Photos" else "${pickedUris.size} document page(s) attached",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Handwritten notes, textbook photos, slides, or PDF",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Attached Files Chips
            if (pickedUris.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pickedUris.forEachIndexed { index, uri ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Page ${index + 1}: ${uri.lastPathSegment ?: "document"}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { pickedUris.remove(uri) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mode Selector
            Text(
                text = "Spaced Repetition Schedule",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = "EXAM_SPRINT" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == "EXAM_SPRINT")
                            Color(0xFF2563EB).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = if (selectedMode == "EXAM_SPRINT")
                        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB))
                    else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Tomorrow's Exam ⚡",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedMode == "EXAM_SPRINT") Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3 rapid passes: 30m, night before, and 2h before exam",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = "SEMESTER" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == "SEMESTER")
                            Color(0xFF10B981).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = if (selectedMode == "SEMESTER")
                        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981))
                    else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Semester Mastery 📚",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedMode == "SEMESTER") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Long-term curve: Day 1, Day 3, Day 7, Day 14, Day 30",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Generate Button
            Button(
                onClick = {
                    if (selectedPalaceId.isBlank()) {
                        Toast.makeText(context, "Please select a Memory Palace", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val composite = activePalaceComposite
                    if (composite == null || composite.loci.isEmpty()) {
                        Toast.makeText(context, "Selected palace has no loci pins", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (pickedUris.isEmpty()) {
                        Toast.makeText(context, "Please attach at least 1 notes photo or PDF", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isGenerating = true
                    generationProgressText = "Decomposing into ${composite.loci.size} loci steps with vivid spatial mnemonics..."

                    coroutineScope.launch {
                        val derivationId = UUID.randomUUID().toString()
                        val result = MemoryPalaceAiHelper.decomposeDerivation(
                            context = context,
                            uris = pickedUris,
                            topicTitle = derivationTitle.trim().takeIf { it.isNotBlank() },
                            loci = composite.loci,
                            derivationId = derivationId
                        )

                        isGenerating = false
                        result.onSuccess { decompResult ->
                            pendingDerivationId = derivationId
                            pendingSourcePaths = decompResult.sourceDocumentPaths
                            pendingPreviewSteps = decompResult.steps
                        }.onFailure { err ->
                            Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isGenerating && selectedPalaceId.isNotBlank() && pickedUris.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(generationProgressText, fontSize = 13.sp)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decompose & Anchor into Palace", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

