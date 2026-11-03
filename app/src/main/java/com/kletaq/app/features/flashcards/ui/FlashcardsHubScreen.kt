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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.Flashcard

@Composable
fun FlashcardsHubScreen(
    onBack: () -> Unit,
    onNavigateToStudy: (deckId: String?, topicId: String?) -> Unit,
    onNavigateToBrowser: (deckId: String?) -> Unit,
    onNavigateToStats: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allCards by FlashcardRepository.cards.collectAsState()
    var showAddCardDialog by remember { mutableStateOf(false) }

    val globalCounts = remember(allCards) {
        FlashcardRepository.getGlobalCounts()
    }

    // Group cards by deck/topic
    val groupedDecks = remember(allCards) {
        allCards.groupBy { if (it.topicTitle.isNotBlank()) it.topicTitle else it.deckName.ifBlank { "General Deck" } }
            .map { (name, cards) ->
                val now = System.currentTimeMillis()
                val newC = cards.count { it.queueState == CardQueueState.NEW }
                val learnC = cards.count { it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING }
                val dueC = cards.count {
                    (it.queueState == CardQueueState.REVIEW || it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) && it.dueTimestamp <= now
                }
                DeckSummaryItem(
                    name = name,
                    topicId = cards.firstOrNull()?.topicId,
                    deckId = cards.firstOrNull()?.deckId,
                    subjectName = cards.firstOrNull()?.subjectName.orEmpty(),
                    newCount = newC,
                    learningCount = learnC,
                    dueCount = dueC,
                    totalCount = cards.size
                )
            }
    }

    if (showAddCardDialog) {
        AddEditCardDialog(
            onDismiss = { showAddCardDialog = false },
            onSaveCard = { card ->
                FlashcardRepository.saveCard(card)
                Toast.makeText(context, "Flashcard created! 🗂️", Toast.LENGTH_SHORT).show()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Flashcards",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Anki Spaced Repetition",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onNavigateToBrowser(null) }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Browser")
                        }
                        IconButton(onClick = onNavigateToStats) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = "Stats")
                        }
                        IconButton(onClick = { showAddCardDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Card", tint = PurpleAccent)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "spacer_top") {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Hero Global Due Deck Card
            item(key = "hero_global_deck") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, PurpleAccent.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ready to Study",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Consolidated spaced repetition queue",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = PurpleAccent.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Style, contentDescription = null, tint = PurpleAccent)
                                }
                            }
                        }

                        // Anki Counter Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HubStatBox(label = "New", count = globalCounts.newCount, color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                            HubStatBox(label = "Learning", count = globalCounts.learningCount, color = Color(0xFFF97316), modifier = Modifier.weight(1f))
                            HubStatBox(label = "Due", count = globalCounts.dueCount, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                            HubStatBox(label = "Total", count = globalCounts.totalCount, color = PurpleAccent, modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = { onNavigateToStudy(null, null) },
                            enabled = globalCounts.totalCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (globalCounts.dueCount > 0) "Study All Due Cards (${globalCounts.dueCount})" else if (globalCounts.totalCount > 0) "Review All (${globalCounts.totalCount})" else "Add Flashcards to Begin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Section: Topic & Subject Decks
            item(key = "section_header_decks") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Topic & Journey Decks (${groupedDecks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Export / Import JSON & CSV
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val json = FlashcardRepository.exportToJson()
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Decks exported to Clipboard (JSON)! 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = "Export JSON", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    val count = FlashcardRepository.importFromJson(clip)
                                    if (count > 0) {
                                        Toast.makeText(context, "Imported $count flashcards! ✨", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No valid cards found in clipboard JSON", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Clipboard is empty! Copy JSON first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Import JSON", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (groupedDecks.isEmpty()) {
                item(key = "empty_decks") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🗂️", fontSize = 32.sp)
                            Text("No Flashcards Yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Create flashcards with the '+' button above or toggle Anki Mode in the Journey Tab to generate cards directly from topics.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = groupedDecks,
                    key = { it.name }
                ) { deck ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToStudy(deck.deckId, deck.topicId) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deck.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (deck.subjectName.isNotBlank()) {
                                    Text(
                                        text = deck.subjectName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DeckCountBadge(deck.newCount, Color(0xFF3B82F6))
                                DeckCountBadge(deck.learningCount, Color(0xFFF97316))
                                DeckCountBadge(deck.dueCount, Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class DeckSummaryItem(
    val name: String,
    val topicId: String?,
    val deckId: String?,
    val subjectName: String,
    val newCount: Int,
    val learningCount: Int,
    val dueCount: Int,
    val totalCount: Int
)

@Composable
private fun HubStatBox(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun DeckCountBadge(count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

