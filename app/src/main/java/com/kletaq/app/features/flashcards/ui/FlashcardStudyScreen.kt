package com.kletaq.app.features.flashcards.ui

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.engine.AnkiScheduler
import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.CardFlag
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import com.kletaq.app.features.flashcards.model.FlashcardType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FlashcardStudyScreen(
    deckId: String? = null,
    topicId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = speech
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }

    val studyQueue = remember { mutableStateListOf<Flashcard>() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFlagMenu by remember { mutableStateOf(false) }
    var sessionReviewsCount by remember { mutableIntStateOf(0) }

    fun refreshQueue() {
        val cards = FlashcardRepository.getStudyQueue(deckId, topicId)
        studyQueue.clear()
        studyQueue.addAll(cards)
        currentIndex = 0
        isAnswerRevealed = false
        showHint = false
    }

    LaunchedEffect(deckId, topicId) {
        refreshQueue()
    }

    val currentCard = studyQueue.getOrNull(currentIndex)
    val isDeckFinished = currentCard == null && studyQueue.isNotEmpty() || (studyQueue.isEmpty() && sessionReviewsCount > 0)

    val counts = remember(studyQueue.size, currentIndex) {
        val remaining = studyQueue.drop(currentIndex)
        val newC = remaining.count { it.queueState == CardQueueState.NEW }
        val learnC = remaining.count { it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING }
        val dueC = remaining.count { it.queueState == CardQueueState.REVIEW }
        Triple(newC, learnC, dueC)
    }

    if (showEditDialog && currentCard != null) {
        AddEditCardDialog(
            initialCard = currentCard,
            onDismiss = { showEditDialog = false },
            onSaveCard = { updated ->
                FlashcardRepository.saveCard(updated)
                studyQueue[currentIndex] = updated
                Toast.makeText(context, "Card updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showInfoDialog && currentCard != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Card Info (Anki)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
                    Text("Queue: ${currentCard.queueState.name}", fontSize = 13.sp)
                    Text("Interval: ${currentCard.intervalDays} days", fontSize = 13.sp)
                    Text("Ease Factor: ${(currentCard.easeFactor * 100).toInt()}%", fontSize = 13.sp)
                    Text("Reviews (Reps): ${currentCard.reps}", fontSize = 13.sp)
                    Text("Lapses: ${currentCard.lapses}", fontSize = 13.sp)
                    Text("Next Due: ${sdf.format(Date(currentCard.dueTimestamp))}", fontSize = 13.sp)
                    if (currentCard.tags.isNotEmpty()) {
                        Text("Tags: ${currentCard.tags.joinToString(", ")}", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    // Anki Badge Trio [New] [Learn] [Due]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgePill(count = counts.first, color = Color(0xFF3B82F6))
                        BadgePill(count = counts.second, color = Color(0xFFF97316))
                        BadgePill(count = counts.third, color = Color(0xFF10B981))
                    }

                    // Action Icons: Undo, Flag, Star, Info, Edit
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val undone = FlashcardRepository.undoLastReview()
                                if (undone != null) {
                                    Toast.makeText(context, "Undid last review! ↩️", Toast.LENGTH_SHORT).show()
                                    refreshQueue()
                                }
                            },
                            enabled = FlashcardRepository.canUndo()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (FlashcardRepository.canUndo()) PurpleAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        Box {
                            IconButton(onClick = { showFlagMenu = true }) {
                                val flagColor = when (currentCard?.flagColor ?: CardFlag.NONE) {
                                    CardFlag.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                                    CardFlag.RED -> Color(0xFFEF4444)
                                    CardFlag.ORANGE -> Color(0xFFF97316)
                                    CardFlag.GREEN -> Color(0xFF10B981)
                                    CardFlag.BLUE -> Color(0xFF3B82F6)
                                }
                                Icon(imageVector = Icons.Default.Flag, contentDescription = "Flag", tint = flagColor)
                            }

                            DropdownMenu(expanded = showFlagMenu, onDismissRequest = { showFlagMenu = false }) {
                                CardFlag.entries.forEach { flag ->
                                    DropdownMenuItem(
                                        text = { Text(flag.name) },
                                        onClick = {
                                            showFlagMenu = false
                                            currentCard?.let {
                                                FlashcardRepository.setFlag(it.id, flag)
                                                studyQueue[currentIndex] = it.copy(flagColor = flag)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = {
                            currentCard?.let {
                                FlashcardRepository.toggleMark(it.id)
                                studyQueue[currentIndex] = it.copy(isMarked = !it.isMarked)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Mark",
                                tint = if (currentCard?.isMarked == true) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Info")
                        }

                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isDeckFinished || currentCard == null) {
                // Deck Finished Celebration
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎉", fontSize = 40.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Congratulations!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You've finished this deck for now! Check back later for your next spaced repetition review.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Return to Hub", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Active Study Card View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable {
                                if (!isAnswerRevealed) {
                                    isAnswerRevealed = true
                                }
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Node Topic Pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PurpleAccent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = currentCard.topicTitle.ifBlank { currentCard.deckName.ifBlank { "Flashcard" } },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // TTS Speaker Button
                                    IconButton(
                                        onClick = {
                                            val textToSpeak = if (isAnswerRevealed) {
                                                if (currentCard.cardType == FlashcardType.CLOZE) {
                                                    ClozeParser.renderAnswer(currentCard.clozeText, 1)
                                                } else {
                                                    "${currentCard.front}. Answer: ${currentCard.back}"
                                                }
                                            } else {
                                                if (currentCard.cardType == FlashcardType.CLOZE) {
                                                    ClozeParser.renderQuestion(currentCard.clozeText, 1)
                                                } else {
                                                    currentCard.front
                                                }
                                            }
                                            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Read aloud", tint = PurpleAccent)
                                    }
                                }

                                // Question Content
                                val questionText = if (currentCard.cardType == FlashcardType.CLOZE) {
                                    ClozeParser.renderQuestion(currentCard.clozeText, 1)
                                } else {
                                    currentCard.front
                                }

                                Text(
                                    text = questionText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 28.sp
                                )

                                // Optional Hint
                                if (currentCard.hint.isNotBlank()) {
                                    if (showHint) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "💡 Hint: ${currentCard.hint}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Show Hint",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleAccent,
                                            modifier = Modifier.clickable { showHint = true }
                                        )
                                    }
                                }

                                // Answer Content (when revealed)
                                if (isAnswerRevealed) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Answer:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981)
                                            )

                                            val answerText = if (currentCard.cardType == FlashcardType.CLOZE) {
                                                ClozeParser.renderAnswer(currentCard.clozeText, 1)
                                            } else {
                                                currentCard.back
                                            }

                                            Text(
                                                text = answerText,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 24.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isAnswerRevealed) {
                                Text(
                                    text = "Tap card to flip",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Panel
                    if (!isAnswerRevealed) {
                        Button(
                            onClick = { isAnswerRevealed = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Show Answer", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // 4 Anki Rating Buttons: [Again] [Hard] [Good] [Easy]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnkiRatingButton(
                                label = "Again",
                                interval = AnkiScheduler.getEstimatedIntervalLabel(currentCard, FlashcardReviewRating.AGAIN),
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    FlashcardRepository.recordReview(currentCard.id, FlashcardReviewRating.AGAIN)
                                    sessionReviewsCount++
                                    isAnswerRevealed = false
                                    showHint = false
                                    currentIndex++
                                }
                            )

                            AnkiRatingButton(
                                label = "Hard",
                                interval = AnkiScheduler.getEstimatedIntervalLabel(currentCard, FlashcardReviewRating.HARD),
                                color = Color(0xFFF97316),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    FlashcardRepository.recordReview(currentCard.id, FlashcardReviewRating.HARD)
                                    sessionReviewsCount++
                                    isAnswerRevealed = false
                                    showHint = false
                                    currentIndex++
                                }
                            )

                            AnkiRatingButton(
                                label = "Good",
                                interval = AnkiScheduler.getEstimatedIntervalLabel(currentCard, FlashcardReviewRating.GOOD),
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    FlashcardRepository.recordReview(currentCard.id, FlashcardReviewRating.GOOD)
                                    sessionReviewsCount++
                                    isAnswerRevealed = false
                                    showHint = false
                                    currentIndex++
                                }
                            )

                            AnkiRatingButton(
                                label = "Easy",
                                interval = AnkiScheduler.getEstimatedIntervalLabel(currentCard, FlashcardReviewRating.EASY),
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    FlashcardRepository.recordReview(currentCard.id, FlashcardReviewRating.EASY)
                                    sessionReviewsCount++
                                    isAnswerRevealed = false
                                    showHint = false
                                    currentIndex++
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnkiRatingButton(
    label: String,
    interval: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
            .height(54.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = interval, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun BadgePill(count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

