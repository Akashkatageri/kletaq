package com.kletaq.app.features.flashcards.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.share.DeckShareFormat
import com.kletaq.app.features.flashcards.share.FlashcardShareManager
import com.kletaq.app.features.journey.components.LessonNode
import kotlinx.coroutines.launch

@Composable
fun AnkiNodeStudyDialog(
    node: LessonNode,
    subjectId: String,
    subjectName: String,
    onDismiss: () -> Unit,
    onStudyNode: () -> Unit,
    onBrowseNode: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }

    val allCards by FlashcardRepository.cards.collectAsState()
    val counts = remember(allCards, node.id) {
        FlashcardRepository.getCountsForTopic(node.id)
    }

    if (showAddCardDialog) {
        AddEditCardDialog(
            defaultTopicId = node.id,
            defaultTopicTitle = node.title,
            defaultSubjectId = subjectId,
            defaultSubjectName = subjectName,
            onDismiss = { showAddCardDialog = false },
            onSaveCard = { card ->
                FlashcardRepository.saveCard(card)
                Toast.makeText(context, "Flashcard added to ${node.title}! 🗂️", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Style,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Anki Flashcard Deck • $subjectName",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Anki Metrics Counter Strip (New, Learning, Due, Total)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCounterPill(
                        label = "New",
                        count = counts.newCount,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCounterPill(
                        label = "Learning",
                        count = counts.learningCount,
                        color = Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCounterPill(
                        label = "Due",
                        count = counts.dueCount,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCounterPill(
                        label = "Total",
                        count = counts.totalCount,
                        color = PurpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Primary CTA: Study Deck
                Button(
                    onClick = {
                        onDismiss()
                        onStudyNode()
                    },
                    enabled = counts.totalCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (counts.dueCount > 0) "Study Due Cards (${counts.dueCount})" else if (counts.totalCount > 0) "Review All Cards (${counts.totalCount})" else "No Cards in this Deck",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action 2: Add Card to this Topic Node
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddCardDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Add Flashcard", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Create a Basic or Cloze card anchored to this node", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Action 3: Browse Topic Cards
                if (counts.totalCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onBrowseNode()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Browse Node Cards", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Search, edit, or manage cards in this node", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Action 4: Share Deck
                    Box {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showShareMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Share Node Deck", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Export to Anki (.tsv) or Kletaq format via WhatsApp, Quick Share, Drive", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share as Anki Deck (.tsv / text)") },
                                onClick = {
                                    showShareMenu = false
                                    scope.launch {
                                        val topicCards = FlashcardRepository.getCardsForTopic(node.id)
                                        FlashcardShareManager.shareDeck(context, node.title, topicCards, DeckShareFormat.ANKI_TSV)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as Kletaq Deck (.json)") },
                                onClick = {
                                    showShareMenu = false
                                    scope.launch {
                                        val topicCards = FlashcardRepository.getCardsForTopic(node.id)
                                        FlashcardShareManager.shareDeck(context, node.title, topicCards, DeckShareFormat.KLETAQ_JSON)
                                    }
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
private fun MetricCounterPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f)),
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

