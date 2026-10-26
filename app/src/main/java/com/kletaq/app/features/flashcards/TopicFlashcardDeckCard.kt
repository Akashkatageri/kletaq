package com.kletaq.app.features.flashcards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.flashcards.TopicFlashcardRepository
import com.kletaq.app.data.model.TopicFlashcard
import com.kletaq.app.data.model.TopicFlashcardDeck
import kotlinx.coroutines.launch

@Composable
fun TopicFlashcardDeckCard(
    topicId: String,
    topicTitle: String,
    subjectCode: String,
    moduleIndex: Int,
    modifier: Modifier = Modifier,
    isStageHighlight: Boolean = false,
    onReviewRated: ((String, String) -> Unit)? = null // topicId, rating
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val deckState by TopicFlashcardRepository.observeDeck(topicId).collectAsState(initial = TopicFlashcardRepository.getCachedDeck(topicId))
    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember(deckState?.topicId) { mutableIntStateOf(0) }

    val cards = deckState?.cards.orEmpty()
    val currentCard = cards.getOrNull(currentIndex)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            if (isStageHighlight) 1.8.dp else 1.dp,
            if (isStageHighlight) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Section label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🗂️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TOPIC FLASHCARDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isStageHighlight) Color(0xFFD97706) else Color(0xFF6366F1),
                                letterSpacing = 0.6.sp
                            )
                            if (isStageHighlight) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "ACTIVE RECALL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (cards.isNotEmpty()) "Flip cards to test active exam recall" else "Generate 4–6 high-yield cards from notes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (cards.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isGenerating = true
                            generationError = null
                            coroutineScope.launch {
                                val result = TopicFlashcardRepository.generateDeck(
                                    context = context,
                                    subjectCode = subjectCode,
                                    moduleIndex = moduleIndex,
                                    topicId = topicId,
                                    topicTitle = topicTitle
                                )
                                isGenerating = false
                                if (result.isFailure) {
                                    generationError = result.exceptionOrNull()?.message
                                } else {
                                    currentIndex = 0
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Empty State, Loading State, or Interactive Flip Deck
            if (isGenerating) {
                // Loading Shimmer Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF6366F1),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extracting high-yield exam cards…",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Analyzing formulas, derivations & traps for $topicTitle",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (cards.isEmpty() || currentCard == null) {
                // Empty State: Prompt to generate
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Master derivations & key formulas faster by testing your recall with AI-generated cards grounded directly in your uploaded notes.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isGenerating = true
                                generationError = null
                                coroutineScope.launch {
                                    val result = TopicFlashcardRepository.generateDeck(
                                        context = context,
                                        subjectCode = subjectCode,
                                        moduleIndex = moduleIndex,
                                        topicId = topicId,
                                        topicTitle = topicTitle
                                    )
                                    isGenerating = false
                                    if (result.isFailure) {
                                        generationError = result.exceptionOrNull()?.message
                                    } else {
                                        currentIndex = 0
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "✨ Generate Flashcards",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        generationError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Could not generate: $err",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Interactive 3D Flip Card
                Interactive3DFlipCard(
                    card = currentCard,
                    cardIndex = currentIndex,
                    totalCards = cards.size,
                    onPrev = {
                        if (currentIndex > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentIndex--
                        }
                    },
                    onNext = {
                        if (currentIndex < cards.size - 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentIndex++
                        }
                    },
                    onRate = { rating ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReviewRated?.invoke(topicId, rating)
                        if (currentIndex < cards.size - 1) {
                            currentIndex++
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Interactive3DFlipCard(
    card: TopicFlashcard,
    cardIndex: Int,
    totalCards: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRate: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    var isFlipped by remember(card.id) { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "cardFlipAnimation"
    )

    // Top Card Pill & Category Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Badge
        val (categoryColor, categoryBg, categoryLabel) = when (card.category) {
            "FORMULA" -> Triple(Color(0xFF0284C7), Color(0xFFE0F2FE), "📐 FORMULA")
            "DERIVATION" -> Triple(Color(0xFFE11D48), Color(0xFFFFE4E6), "⚡ DERIVATION")
            "EXAM_TRAP" -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "⚠️ EXAM TRAP")
            else -> Triple(Color(0xFF7C3AED), Color(0xFFEDE9FE), "💡 CONCEPT")
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = categoryBg
        ) {
            Text(
                text = categoryLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = categoryColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "${cardIndex + 1} / $totalCards",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3D Flip Card Container
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.2.dp,
            if (isFlipped) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF6366F1).copy(alpha = 0.4f)
        ),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 170.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isFlipped = !isFlipped
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            if (rotation <= 90f) {
                // FRONT FACE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CHALLENGE / PROMPT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6366F1),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = card.front,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                        if (card.hint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Hint: ${card.hint}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "👆 Tap to reveal answer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            } else {
                // BACK FACE (Counter-rotated so text reads left-to-right)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationY = 180f },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ANSWER & DERIVATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981),
                                letterSpacing = 0.5.sp
                            )
                            if (card.pageCitation.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD1FAE5)
                                ) {
                                    Text(
                                        text = card.pageCitation,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = card.back,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "👆 Tap to flip back",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Navigation & Self-Assessment Controls
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrev,
            enabled = cardIndex > 0,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Card",
                tint = if (cardIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        // Self-Assessment Chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RecallRatingChip(label = "Hard 😓", color = Color(0xFFEF4444)) { onRate("HARD") }
            RecallRatingChip(label = "Good 👍", color = Color(0xFF6366F1)) { onRate("GOOD") }
            RecallRatingChip(label = "Easy ⚡", color = Color(0xFF10B981)) { onRate("EASY") }
        }

        IconButton(
            onClick = onNext,
            enabled = cardIndex < totalCards - 1,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Card",
                tint = if (cardIndex < totalCards - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun RecallRatingChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
