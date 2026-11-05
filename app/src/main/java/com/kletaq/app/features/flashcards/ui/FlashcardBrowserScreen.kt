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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
import com.kletaq.app.features.flashcards.model.CardFlag
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import com.kletaq.app.features.flashcards.ui.components.CardInfoDialog

@Composable
fun FlashcardBrowserScreen(
    initialDeckId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var cardToEdit by remember { mutableStateOf<Flashcard?>(null) }
    var cardForInfo by remember { mutableStateOf<Flashcard?>(null) }

    val allCards by FlashcardRepository.cards.collectAsState()
    val selectedIds = remember { mutableStateListOf<String>() }
    var showBulkFlagMenu by remember { mutableStateOf(false) }

    val filteredCards = remember(allCards, searchQuery, selectedFilter, initialDeckId) {
        val now = System.currentTimeMillis()
        val tokens = searchQuery.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

        allCards.filter { card ->
            val matchInitialDeck = initialDeckId.isNullOrBlank() || card.deckId == initialDeckId || card.topicId == initialDeckId
            if (!matchInitialDeck) return@filter false

            // Preset Filter Pill match
            val matchPill = when (selectedFilter) {
                "DUE" -> (card.queueState == CardQueueState.REVIEW || card.queueState == CardQueueState.LEARNING) && card.dueTimestamp <= now && !card.isSuspended && !card.isBuried
                "NEW" -> card.queueState == CardQueueState.NEW && !card.isSuspended && !card.isBuried
                "LEARN" -> (card.queueState == CardQueueState.LEARNING || card.queueState == CardQueueState.RELEARNING) && !card.isSuspended && !card.isBuried
                "SUSPENDED" -> card.isSuspended
                "BURIED" -> card.isBuried
                "LEECHES" -> card.isLeech
                "MARKED" -> card.isMarked
                else -> true
            }
            if (!matchPill) return@filter false

            // Anki Query syntax matching
            if (tokens.isEmpty()) return@filter true

            tokens.all { token ->
                when {
                    token.startsWith("is:due", ignoreCase = true) -> {
                        (card.queueState == CardQueueState.REVIEW || card.queueState == CardQueueState.LEARNING) && card.dueTimestamp <= now && !card.isSuspended && !card.isBuried
                    }
                    token.startsWith("is:new", ignoreCase = true) -> card.queueState == CardQueueState.NEW
                    token.startsWith("is:learn", ignoreCase = true) || token.startsWith("is:learning", ignoreCase = true) -> card.queueState == CardQueueState.LEARNING || card.queueState == CardQueueState.RELEARNING
                    token.startsWith("is:review", ignoreCase = true) -> card.queueState == CardQueueState.REVIEW
                    token.startsWith("is:suspended", ignoreCase = true) -> card.isSuspended
                    token.startsWith("is:buried", ignoreCase = true) -> card.isBuried
                    token.startsWith("is:marked", ignoreCase = true) -> card.isMarked
                    token.startsWith("is:leech", ignoreCase = true) -> card.isLeech
                    token.startsWith("deck:", ignoreCase = true) -> {
                        val term = token.removePrefix("deck:")
                        card.deckName.contains(term, ignoreCase = true)
                    }
                    token.startsWith("tag:", ignoreCase = true) -> {
                        val term = token.removePrefix("tag:")
                        card.tags.any { it.contains(term, ignoreCase = true) }
                    }
                    token.startsWith("flag:", ignoreCase = true) -> {
                        val color = token.removePrefix("flag:")
                        card.flagColor.name.equals(color, ignoreCase = true)
                    }
                    else -> {
                        card.front.contains(token, ignoreCase = true) ||
                                card.back.contains(token, ignoreCase = true) ||
                                card.clozeText.contains(token, ignoreCase = true) ||
                                card.hint.contains(token, ignoreCase = true) ||
                                card.tags.any { it.contains(token, ignoreCase = true) } ||
                                card.topicTitle.contains(token, ignoreCase = true)
                    }
                }
            }
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

    if (cardForInfo != null) {
        CardInfoDialog(
            card = cardForInfo!!,
            onDismiss = { cardForInfo = null }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (selectedIds.isNotEmpty()) {
                        // Multi-Selection Action Toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedIds.clear() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Selection")
                                }
                                Text(
                                    text = "${selectedIds.size} selected",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (selectedIds.size == filteredCards.size) {
                                            selectedIds.clear()
                                        } else {
                                            selectedIds.clear()
                                            selectedIds.addAll(filteredCards.map { it.id })
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select All")
                                }

                                IconButton(
                                    onClick = {
                                        selectedIds.forEach { id ->
                                            FlashcardRepository.toggleSuspend(id)
                                        }
                                        Toast.makeText(context, "Suspension toggled for ${selectedIds.size} cards", Toast.LENGTH_SHORT).show()
                                        selectedIds.clear()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Toggle Suspend")
                                }

                                Box {
                                    IconButton(onClick = { showBulkFlagMenu = true }) {
                                        Icon(imageVector = Icons.Default.Flag, contentDescription = "Bulk Flag")
                                    }
                                    DropdownMenu(
                                        expanded = showBulkFlagMenu,
                                        onDismissRequest = { showBulkFlagMenu = false }
                                    ) {
                                        CardFlag.entries.forEach { flag ->
                                            DropdownMenuItem(
                                                text = { Text(flag.name) },
                                                onClick = {
                                                    selectedIds.forEach { id ->
                                                        FlashcardRepository.setFlag(id, flag)
                                                    }
                                                    Toast.makeText(context, "Flag set to ${flag.name}", Toast.LENGTH_SHORT).show()
                                                    showBulkFlagMenu = false
                                                    selectedIds.clear()
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        selectedIds.forEach { id ->
                                            FlashcardRepository.deleteCard(id)
                                        }
                                        Toast.makeText(context, "Deleted ${selectedIds.size} cards", Toast.LENGTH_SHORT).show()
                                        selectedIds.clear()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        // Normal Title Bar
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
                    }

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search or query (e.g. is:due, tag:math, flag:red)") },
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
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Pills Strip
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf(
                            "ALL" to "All",
                            "DUE" to "Due",
                            "NEW" to "New",
                            "LEARN" to "Learning",
                            "SUSPENDED" to "Suspended",
                            "BURIED" to "Buried",
                            "LEECHES" to "Leeches",
                            "MARKED" to "Marked"
                        )
                        items(filters) { (key, label) ->
                            val isSel = selectedFilter == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
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
                            text = "No cards matching criteria",
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
                    val isCardSelected = selectedIds.contains(card.id)
                    BrowserCardItem(
                        card = card,
                        isSelected = isCardSelected,
                        isSelectionActive = selectedIds.isNotEmpty(),
                        onToggleSelect = {
                            if (isCardSelected) selectedIds.remove(card.id) else selectedIds.add(card.id)
                        },
                        onEdit = { cardToEdit = card },
                        onInfo = { cardForInfo = card },
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
    isSelected: Boolean,
    isSelectionActive: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
    onToggleSuspend: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionActive) onToggleSelect() else onEdit()
            },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) PurpleAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionActive) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = PurpleAccent),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Topic & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.topicTitle.ifBlank { card.deckName.ifBlank { "General" } },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (card.isLeech) {
                            Text("🩸 Leech", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                        if (card.isBuried) {
                            Text("🪦 Buried", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (card.isSuspended) {
                            Text("⏸️ Suspended", fontSize = 10.sp, color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                        }
                        if (card.isMarked) {
                            Text("⭐", fontSize = 11.sp)
                        }
                        val queueColor = when (card.queueState) {
                            CardQueueState.NEW -> Color(0xFF3B82F6)
                            CardQueueState.LEARNING, CardQueueState.RELEARNING -> Color(0xFFF97316)
                            CardQueueState.REVIEW -> Color(0xFF10B981)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = queueColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = card.queueState.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = queueColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Front text
                val frontDisplay = if (card.cardType == FlashcardType.CLOZE) {
                    ClozeParser.renderQuestion(card.clozeText, 1)
                } else {
                    card.front
                }
                Text(
                    text = frontDisplay,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                // Back text
                val backDisplay = if (card.cardType == FlashcardType.CLOZE) {
                    ClozeParser.renderAnswer(card.clozeText, 1)
                } else {
                    card.back
                }
                if (backDisplay.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = backDisplay,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Metadata Footer: interval, reps, lapses
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Int: ${card.intervalDays}d", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Reps: ${card.reps}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Lapses: ${card.lapses}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (card.stability > 0.0) {
                        Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format("S: %.1fd", card.stability), fontSize = 10.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3-dots Menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit Card") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Card Info") },
                        onClick = {
                            menuExpanded = false
                            onInfo()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (card.isSuspended) "Unsuspend Card" else "Suspend Card") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (card.isSuspended) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleSuspend()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reset Progress") },
                        leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onReset()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Card", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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
