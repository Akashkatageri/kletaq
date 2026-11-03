package com.kletaq.app.features.flashcards.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType

@Composable
fun FlashcardBrowserScreen(
    initialDeckId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var cardToEdit by remember { mutableStateOf<Flashcard?>(null) }

    val allCards by FlashcardRepository.cards.collectAsState()

    val filteredCards = remember(allCards, searchQuery, selectedFilter, initialDeckId) {
        val now = System.currentTimeMillis()
        allCards.filter { card ->
            val matchDeck = initialDeckId.isNullOrBlank() || card.deckId == initialDeckId || card.topicId == initialDeckId
            val matchQuery = searchQuery.isBlank() ||
                card.front.contains(searchQuery, ignoreCase = true) ||
                card.back.contains(searchQuery, ignoreCase = true) ||
                card.clozeText.contains(searchQuery, ignoreCase = true) ||
                card.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                card.topicTitle.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "DUE" -> (card.queueState == CardQueueState.REVIEW || card.queueState == CardQueueState.LEARNING) && card.dueTimestamp <= now && !card.isSuspended
                "NEW" -> card.queueState == CardQueueState.NEW && !card.isSuspended
                "LEARN" -> (card.queueState == CardQueueState.LEARNING || card.queueState == CardQueueState.RELEARNING) && !card.isSuspended
                "SUSPENDED" -> card.isSuspended
                "MARKED" -> card.isMarked
                else -> true
            }

            matchDeck && matchQuery && matchFilter
        }
    }

    if (cardToEdit != null) {
        AddEditCardDialog(
            initialCard = cardToEdit,
            onDismiss = { cardToEdit = null },
            onSaveCard = { updated ->
                FlashcardRepository.saveCard(updated)
                Toast.makeText(context, "Card saved", Toast.LENGTH_SHORT).show()
                cardToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Card Browser (${filteredCards.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search questions, answers, tags...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PurpleAccent) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "DUE" to "Due",
                            "NEW" to "New",
                            "LEARN" to "Learning",
                            "MARKED" to "Marked ⭐",
                            "SUSPENDED" to "Suspended"
                        ).forEach { (key, label) ->
                            val isSel = selectedFilter == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { selectedFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredCards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No cards found",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = filteredCards,
                    key = { it.id }
                ) { card ->
                    BrowserCardItem(
                        card = card,
                        onEdit = { cardToEdit = card },
                        onToggleSuspend = {
                            FlashcardRepository.toggleSuspend(card.id)
                        },
                        onReset = {
                            FlashcardRepository.resetCardProgress(card.id)
                            Toast.makeText(context, "Card progress reset to New", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            FlashcardRepository.deleteCard(card.id)
                            Toast.makeText(context, "Card deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserCardItem(
    card: Flashcard,
    onEdit: () -> Unit,
    onToggleSuspend: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (card.isSuspended) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (card.queueState) {
                        CardQueueState.NEW -> Color(0xFF3B82F6)
                        CardQueueState.LEARNING, CardQueueState.RELEARNING -> Color(0xFFF97316)
                        CardQueueState.REVIEW -> Color(0xFF10B981)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.15f)) {
                        Text(
                            text = if (card.isSuspended) "SUSPENDED" else card.queueState.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (card.isSuspended) MaterialTheme.colorScheme.onSurfaceVariant else statusColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.topicTitle.ifBlank { card.deckName.ifBlank { "General" } },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val preview = if (card.cardType == FlashcardType.CLOZE) {
                    ClozeParser.renderQuestion(card.clozeText, 1)
                } else {
                    card.front
                }

                Text(
                    text = preview,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                Text(
                    text = "Interval: ${card.intervalDays}d • Ease: ${(card.easeFactor * 100).toInt()}% • Reps: ${card.reps}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (card.isSuspended) "Unsuspend Card" else "Suspend Card") },
                        onClick = {
                            menuExpanded = false
                            onToggleSuspend()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reset Progress") },
                        onClick = {
                            menuExpanded = false
                            onReset()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Card", color = Color(0xFFEF4444)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

