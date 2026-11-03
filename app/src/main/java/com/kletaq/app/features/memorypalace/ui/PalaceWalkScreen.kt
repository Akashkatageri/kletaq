package com.kletaq.app.features.memorypalace.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.memorypalace.data.MemoryPalaceRepository
import com.kletaq.app.features.memorypalace.model.DerivationWithSteps
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep
import com.kletaq.app.features.memorypalace.model.PalaceWithImagesAndLoci
import com.kletaq.app.features.memorypalace.ui.components.CameraPanRoomViewer
import com.kletaq.app.features.memorypalace.ui.components.LociPuzzleView
import com.kletaq.app.features.memorypalace.ui.components.MnemonicCueCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🚶 Palace Walkthrough & Recall Screen
 * Provides Mental Walk & Reveal practice and Loci Sequence Challenge.
 */
@Composable
fun PalaceWalkScreen(
    derivationId: String,
    onSessionComplete: () -> Unit,
    onBack: () -> Unit
) {
    var derivationWithSteps by remember { mutableStateOf<DerivationWithSteps?>(null) }
    var palaceWithLoci by remember { mutableStateOf<PalaceWithImagesAndLoci?>(null) }

    // Load data
    LaunchedEffect(derivationId) {
        val dws = MemoryPalaceRepository.getDerivationWithSteps(derivationId)
        derivationWithSteps = dws
        if (dws != null) {
            palaceWithLoci = MemoryPalaceRepository.getPalaceWithLoci(dws.derivation.palaceId)
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Mental Walk, 1: Puzzle

    // Active Walk State
    val reviewQueue = remember { mutableStateListOf<PalaceStep>() }
    var currentQueueIndex by remember { mutableIntStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    var sessionFinished by remember { mutableStateOf(false) }

    // Initialize review queue
    LaunchedEffect(derivationWithSteps) {
        val steps = derivationWithSteps?.steps
        if (steps != null && reviewQueue.isEmpty()) {
            reviewQueue.addAll(steps.sortedBy { it.stepNumber })
        }
    }

    val dws = derivationWithSteps
    val pwl = palaceWithLoci
    val sourcePaths = dws?.derivation?.sourceDocumentPaths ?: emptyList()

    var isFullscreenNotesOpen by remember { mutableStateOf(false) }
    var activePageIndex by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        text = dws?.derivation?.title ?: "Memory Palace Walk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Mental Walk & Reveal") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Sequence Puzzle") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (dws == null || pwl == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading palace topic...", color = Color.Gray)
                }
            } else if (selectedTabIndex == 1) {
                // Mode B: Sequence Puzzle
                LociPuzzleView(
                    loci = pwl.loci,
                    steps = dws.steps,
                    onPuzzleCompleted = {
                        selectedTabIndex = 0
                    }
                )
            } else if (sessionFinished) {
                // Completed Session Card
                WalkCompletedCard(
                    derivation = dws.derivation,
                    onFinish = onSessionComplete
                )
            } else if (reviewQueue.isNotEmpty() && currentQueueIndex < reviewQueue.size) {
                // Mode A: Mental Walk & Reveal
                val currentStep = reviewQueue[currentQueueIndex]
                val currentLocus = pwl.loci.firstOrNull { it.orderIndex == currentStep.locusOrderIndex }
                    ?: pwl.loci.firstOrNull()

                val currentImage = pwl.images.firstOrNull { it.id == currentLocus?.imageId }
                    ?: pwl.images.firstOrNull()

                val sourcePaths = dws.derivation.sourceDocumentPaths
                var viewerMode by remember { mutableStateOf(if (sourcePaths.isNotEmpty()) "NOTES" else "ROOM") }

                LaunchedEffect(currentStep.id) {
                    if (sourcePaths.isNotEmpty()) {
                        activePageIndex = currentStep.pageIndex.coerceIn(0, (sourcePaths.size - 1).coerceAtLeast(0))
                    }
                }

                val bitmap = remember(currentImage?.photoPath) {
                    currentImage?.photoPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                    }
                }
                val imageAspectRatio = remember(bitmap) {
                    if (bitmap != null && bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat() else (4f / 3f)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2563EB)
                        ) {
                            Text(
                                text = "Locus ${currentStep.locusOrderIndex} of ${pwl.loci.size}: ${currentLocus?.name ?: "Room"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = "Step ${currentStep.stepNumber} of ${dws.steps.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mode Switcher: Room Locus vs Original Notes
                    if (sourcePaths.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (viewerMode == "ROOM") Color(0xFF2563EB) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewerMode = "ROOM" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = if (viewerMode == "ROOM") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Room Locus",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewerMode == "ROOM") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (viewerMode == "NOTES") Color(0xFF2563EB) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewerMode = "NOTES" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (viewerMode == "NOTES") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Original Notes (P${activePageIndex + 1})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewerMode == "NOTES") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Main Viewer: Room Photo OR Original Notes Page
                    if (viewerMode == "NOTES" && sourcePaths.isNotEmpty()) {
                        val currentNotePath = sourcePaths.getOrNull(activePageIndex)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            if (currentNotePath != null) {
                                ZoomableNoteImageViewer(
                                    imagePath = currentNotePath,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (sourcePaths.size > 1) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (activePageIndex > 0) activePageIndex-- },
                                                enabled = activePageIndex > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Prev Page",
                                                    tint = if (activePageIndex > 0) Color.White else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = "Page ${activePageIndex + 1} of ${sourcePaths.size}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(
                                                onClick = { if (activePageIndex < sourcePaths.size - 1) activePageIndex++ },
                                                enabled = activePageIndex < sourcePaths.size - 1,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Next Page",
                                                    tint = if (activePageIndex < sourcePaths.size - 1) Color.White else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Page 1 of 1 • Pinch to zoom",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { isFullscreenNotesOpen = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Room Photo with Animated Zoom & Pan to Active Locus Pin
                        CameraPanRoomViewer(
                            bitmap = bitmap,
                            activeLocus = currentLocus,
                            allLoci = pwl.loci,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Pre-reveal Mnemonic Mental Theater Cue Card
                    if (!isRevealed) {
                        MnemonicCueCard(
                            locus = currentLocus,
                            step = currentStep,
                            onReveal = { isRevealed = true }
                        )
                    } else {
                        // Revealed Details: Formula / Point + Sensory Story + Exam Trap
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Milestone Title & Formula
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = currentStep.stepTitle,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentStep.formulaLatex.ifBlank { "Formula line" },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (sourcePaths.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                viewerMode = "NOTES"
                                                activePageIndex = currentStep.pageIndex.coerceIn(0, sourcePaths.size - 1)
                                                isFullscreenNotesOpen = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "View Step in Original Notes (Page ${currentStep.pageIndex + 1})",
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Sensory Mnemonic Story
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Sensory Mnemonic Story:",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentStep.sensoryStory,
                                        fontSize = 13.sp,
                                        color = Color(0xFFE0E7FF),
                                        lineHeight = 19.sp
                                    )
                                }
                            }

                            // VTU Exam Trap
                            if (currentStep.examTrap.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF451A03),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = Color(0xFFF97316),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Exam Trap: ${currentStep.examTrap}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFED7AA),
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }

                            // 3-Tier Recall Rating Row (Phase 3 Alignment: Hard / Good / Easy)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Rate Your Spatial Recall:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 🔴 HARD
                                OutlinedButton(
                                    onClick = {
                                        reviewQueue.add(currentStep)
                                        MemoryPalaceRepository.recordStepReview(
                                            derivationId,
                                            MemoryPalaceRepository.RecallRating.HARD
                                        )
                                        isRevealed = false
                                        currentQueueIndex++
                                        if (currentQueueIndex >= reviewQueue.size) {
                                            sessionFinished = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Hard 🔴", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Retry", fontSize = 10.sp, color = Color(0xFFEF4444))
                                    }
                                }

                                // 🟡 GOOD
                                Button(
                                    onClick = {
                                        MemoryPalaceRepository.recordStepReview(
                                            derivationId,
                                            MemoryPalaceRepository.RecallRating.GOOD
                                        )
                                        isRevealed = false
                                        currentQueueIndex++
                                        if (currentQueueIndex >= reviewQueue.size) {
                                            sessionFinished = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Good 🟡", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                        Text("Advance", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.8f))
                                    }
                                }

                                // 🟢 EASY
                                Button(
                                    onClick = {
                                        MemoryPalaceRepository.recordStepReview(
                                            derivationId,
                                            MemoryPalaceRepository.RecallRating.EASY
                                        )
                                        isRevealed = false
                                        currentQueueIndex++
                                        if (currentQueueIndex >= reviewQueue.size) {
                                            sessionFinished = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Easy 🟢", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text("+1.5x", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

        // Fullscreen Notes Page Viewer (Zero Bottom Sheets, Pure Edge-to-Edge)
        if (isFullscreenNotesOpen && sourcePaths.isNotEmpty()) {
            val currentNotePath = sourcePaths.getOrNull(activePageIndex)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (currentNotePath != null) {
                    ZoomableNoteImageViewer(
                        imagePath = currentNotePath,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isFullscreenNotesOpen = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Fullscreen",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${dws?.derivation?.title ?: "Derivation"} • Page ${activePageIndex + 1} of ${sourcePaths.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (sourcePaths.size > 1) {
                        IconButton(
                            onClick = { if (activePageIndex > 0) activePageIndex-- },
                            enabled = activePageIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Prev Page",
                                tint = if (activePageIndex > 0) Color.White else Color.DarkGray
                            )
                        }
                        IconButton(
                            onClick = { if (activePageIndex < sourcePaths.size - 1) activePageIndex++ },
                            enabled = activePageIndex < sourcePaths.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Page",
                                tint = if (activePageIndex < sourcePaths.size - 1) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkCompletedCard(
    derivation: com.kletaq.app.features.memorypalace.model.PalaceDerivation,
    onFinish: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM h:mm a", Locale.getDefault()) }
    val formattedNextTime = remember(derivation.nextReviewTime) {
        if (derivation.nextReviewTime == Long.MAX_VALUE) "Mastered 🎉"
        else dateFormat.format(Date(derivation.nextReviewTime))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFF10B981)
                    )
                }

                Text(
                    text = "Palace Walk Completed!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "All topic steps were reviewed and anchored to your physical loci.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2563EB).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next Spaced Review Due:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedNextTime,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 🔍 Pinch-To-Zoom and Pan Image Viewer for Original Notes & Derivations.
 * Zero bottom sheets, full gesture control, double-tap zoom to 2.2x.
 */
@Composable
fun ZoomableNoteImageViewer(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    var scale by remember(imagePath) { mutableFloatStateOf(1f) }
    var offsetX by remember(imagePath) { mutableFloatStateOf(0f) }
    var offsetY by remember(imagePath) { mutableFloatStateOf(0f) }

    val bitmap = remember(imagePath) {
        val f = File(imagePath)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(imagePath) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.2f
                        }
                    }
                )
            }
            .pointerInput(imagePath) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.8f, 4.5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Original Notes Page",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Text("Notes page not available", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
