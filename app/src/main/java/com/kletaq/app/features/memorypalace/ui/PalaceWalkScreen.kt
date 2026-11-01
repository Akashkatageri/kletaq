package com.kletaq.app.features.memorypalace.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.kletaq.app.features.memorypalace.ui.components.LociPuzzleView
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
                    Text("Loading palace derivation...", color = Color.Gray)
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

                val bitmap = remember(currentImage?.photoPath) {
                    currentImage?.photoPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                    }
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

                    // Room Photo Box with Glowing Active Pin
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        val containerWidth = constraints.maxWidth.toFloat()
                        val containerHeight = constraints.maxHeight.toFloat()

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Active Room Locus",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Room Photo", color = Color.Gray)
                            }
                        }

                        // Active Glowing Pin
                        if (currentLocus != null) {
                            val pinX = (currentLocus.xPercent * containerWidth) - 18.dp.value
                            val pinY = (currentLocus.yPercent * containerHeight) - 18.dp.value

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(pinX.toInt(), pinY.toInt()) }
                                    .size(36.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .border(3.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentLocus.orderIndex.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Mental Prompt Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Stand at your ${currentLocus?.name ?: "Spot"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Close your eyes or visualize what happens here. What is the mathematical transformation or equation line?",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Reveal Button or Revealed Content
                    if (!isRevealed) {
                        Button(
                            onClick = { isRevealed = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reveal Formula & Mental Story", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Revealed Details: Formula + Patrick Jane Story + Exam Trap
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
                                }
                            }

                            // Patrick Jane Story
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Patrick Jane Sensory Story:",
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

                            // Feedback Buttons
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Re-queue step for retry in this session
                                        reviewQueue.add(currentStep)
                                        MemoryPalaceRepository.recordStepReview(derivationId, rememberedCleanly = false)

                                        isRevealed = false
                                        currentQueueIndex++
                                        if (currentQueueIndex >= reviewQueue.size) {
                                            sessionFinished = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Struggled", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        MemoryPalaceRepository.recordStepReview(derivationId, rememberedCleanly = true)
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
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remembered!", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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
                    text = "All derivation steps were reviewed and anchored to your physical loci.",
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
