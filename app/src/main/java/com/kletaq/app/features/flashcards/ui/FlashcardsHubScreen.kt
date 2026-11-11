package com.kletaq.app.features.flashcards.ui

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.FlashcardDeck
import com.kletaq.app.features.flashcards.share.DeckShareFormat
import com.kletaq.app.features.flashcards.share.FlashcardShareManager
import kotlinx.coroutines.launch

@Composable
fun FlashcardsHubScreen(
    onBack: () -> Unit,
    onNavigateToStudy: (deckId: String?, topicId: String?) -> Unit,
    onNavigateToBrowser: (deckId: String?) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToDeckOverview: (deckId: String) -> Unit = { deckId -> onNavigateToStudy(deckId, null) }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val allCards by FlashcardRepository.cards.collectAsState()
    val allDecks by FlashcardRepository.decks.collectAsState()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var parentDeckForSubdeck by remember { mutableStateOf<FlashcardDeck?>(null) }
    var deckForOptions by remember { mutableStateOf<FlashcardDeck?>(null) }
    var deckToRename by remember { mutableStateOf<FlashcardDeck?>(null) }
    var showShareCollectionMenu by remember { mutableStateOf(false) }
    var importDialogUri by remember { mutableStateOf<Uri?>(null) }
    var importDialogFileName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importDialogFileName = queryFileName(context, uri)
            importDialogUri = uri
        }
    }

    val globalCounts = remember(allCards) {
        FlashcardRepository.getGlobalCounts()
    }

    // Combine custom decks with any legacy topic groupings
    val displayDecks = remember(allCards, allDecks) {
        val now = System.currentTimeMillis()
        val customDeckMap = allDecks.map { deck ->
            val cards = allCards.filter { it.deckId == deck.id }
            val newC = cards.count { it.queueState == CardQueueState.NEW && !it.isSuspended && !it.isBuried }
            val learnC = cards.count { (it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) && !it.isSuspended && !it.isBuried }
            val dueC = cards.count {
                (it.queueState == CardQueueState.REVIEW || it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) &&
                        it.dueTimestamp <= now && !it.isSuspended && !it.isBuried
            }
            DeckSummaryItem(
                name = deck.name,
                displayName = deck.displayName,
                isSubdeck = deck.isSubdeck,
                deck = deck,
                topicId = deck.topicId,
                deckId = deck.id,
                subjectName = deck.description,
                newCount = newC,
                learningCount = learnC,
                dueCount = dueC,
                totalCount = cards.size
            )
        }.toMutableList()

        // Include any cards that don't have a matching custom deck yet
        val customDeckIds = allDecks.map { it.id }.toSet()
        val unassignedCards = allCards.filter { it.deckId !in customDeckIds }
        val topicGroups = unassignedCards.groupBy { if (it.topicTitle.isNotBlank()) it.topicTitle else it.deckName.ifBlank { "General Deck" } }

        for ((name, cards) in topicGroups) {
            val trimmedName = name.trim()
            val existingDeckIndex = customDeckMap.indexOfFirst { it.name.trim().equals(trimmedName, ignoreCase = true) }
            val newC = cards.count { it.queueState == CardQueueState.NEW && !it.isSuspended && !it.isBuried }
            val learnC = cards.count { (it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) && !it.isSuspended && !it.isBuried }
            val dueC = cards.count {
                (it.queueState == CardQueueState.REVIEW || it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) &&
                        it.dueTimestamp <= now && !it.isSuspended && !it.isBuried
            }
            if (existingDeckIndex >= 0) {
                val existing = customDeckMap[existingDeckIndex]
                customDeckMap[existingDeckIndex] = existing.copy(
                    newCount = existing.newCount + newC,
                    learningCount = existing.learningCount + learnC,
                    dueCount = existing.dueCount + dueC,
                    totalCount = existing.totalCount + cards.size
                )
            } else {
                val firstCard = cards.firstOrNull()
                customDeckMap.add(
                    DeckSummaryItem(
                        name = name,
                        displayName = if (name.contains("::")) name.substringAfterLast("::") else name,
                        isSubdeck = name.contains("::"),
                        deck = FlashcardDeck(id = firstCard?.deckId ?: firstCard?.topicId ?: name, name = name),
                        topicId = firstCard?.topicId,
                        deckId = firstCard?.deckId,
                        subjectName = firstCard?.subjectName.orEmpty(),
                        newCount = newC,
                        learningCount = learnC,
                        dueCount = dueC,
                        totalCount = cards.size
                    )
                )
            }
        }

        customDeckMap.sortedBy { it.name }
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

    if (showCreateDeckDialog) {
        CreateDeckDialog(
            parentDeck = parentDeckForSubdeck,
            onDismiss = {
                showCreateDeckDialog = false
                parentDeckForSubdeck = null
            },
            onDeckCreated = {
                Toast.makeText(context, "Deck created: ${it.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (deckForOptions != null) {
        DeckOptionsDialog(
            deck = deckForOptions!!,
            onDismiss = { deckForOptions = null },
            onSaved = {
                Toast.makeText(context, "Deck options updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (deckToRename != null) {
        RenameDeckDialog(
            deck = deckToRename!!,
            onDismiss = { deckToRename = null },
            onRenamed = {
                Toast.makeText(context, "Deck renamed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (importDialogUri != null) {
        ImportDeckDialog(
            fileUri = importDialogUri!!,
            fileName = importDialogFileName,
            onDismiss = { importDialogUri = null },
            onImportCompleted = { _, _ ->
                importDialogUri = null
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
                                text = "AnkiDroid Parity Spaced Repetition",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showCreateDeckDialog = true }) {
                            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Create Deck", tint = PurpleAccent)
                        }
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

            // Section: Decks
            item(key = "section_header_decks") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Decks (${displayDecks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Export / Import JSON & CSV & Share
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box {
                            IconButton(
                                onClick = { showShareCollectionMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Decks", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                            }

                            DropdownMenu(
                                expanded = showShareCollectionMenu,
                                onDismissRequest = { showShareCollectionMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share Decks as Anki Package (.apkg)") },
                                    onClick = {
                                        showShareCollectionMenu = false
                                        scope.launch {
                                            FlashcardShareManager.shareDeck(context, "Kletaq_Flashcards", allCards, DeckShareFormat.ANKI_APKG)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Decks as Anki (.tsv / text)") },
                                    onClick = {
                                        showShareCollectionMenu = false
                                        scope.launch {
                                            FlashcardShareManager.shareDeck(context, "Kletaq_Flashcards", allCards, DeckShareFormat.ANKI_TSV)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Decks as Kletaq (.json)") },
                                    onClick = {
                                        showShareCollectionMenu = false
                                        scope.launch {
                                            FlashcardShareManager.shareDeck(context, "Kletaq_Flashcards", allCards, DeckShareFormat.KLETAQ_JSON)
                                        }
                                    }
                                )
                            }
                        }

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

                        IconButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Import .apkg / .csv Deck", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (displayDecks.isEmpty()) {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🗂️", fontSize = 36.sp)
                            Text("No Decks Created Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Create custom decks with '::' subdeck hierarchies, or add your first cards to start studying.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showCreateDeckDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create First Deck", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import .apkg", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = displayDecks,
                    key = { index, deckItem ->
                        "${deckItem.deckId ?: deckItem.topicId ?: deckItem.name}_$index"
                    }
                ) { _, deckItem ->
                    var itemMenuExpanded by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = if (deckItem.isSubdeck) 16.dp else 0.dp)
                            .clickable {
                                if (deckItem.deckId != null) {
                                    onNavigateToDeckOverview(deckItem.deckId)
                                } else {
                                    onNavigateToStudy(null, deckItem.topicId)
                                }
                            },
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
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (deckItem.isSubdeck) PurpleAccent.copy(alpha = 0.7f) else PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = deckItem.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    if (deckItem.isSubdeck) {
                                        Text(
                                            text = deckItem.name.substringBeforeLast("::"),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (deckItem.subjectName.isNotBlank()) {
                                        Text(
                                            text = deckItem.subjectName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DeckCountBadge(deckItem.newCount, Color(0xFF3B82F6))
                                DeckCountBadge(deckItem.learningCount, Color(0xFFF97316))
                                DeckCountBadge(deckItem.dueCount, Color(0xFF10B981))

                                Box {
                                    IconButton(
                                        onClick = { itemMenuExpanded = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(16.dp))
                                    }

                                    DropdownMenu(
                                        expanded = itemMenuExpanded,
                                        onDismissRequest = { itemMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Study Deck") },
                                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                onNavigateToStudy(deckItem.deckId, deckItem.topicId)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Browse Cards") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                onNavigateToBrowser(deckItem.deckId)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Create Subdeck") },
                                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                parentDeckForSubdeck = deckItem.deck
                                                showCreateDeckDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Deck Options") },
                                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                deckForOptions = deckItem.deck
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export Deck (.apkg)") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                scope.launch {
                                                    val deckCards = allCards.filter { it.deckId == deckItem.deck.id }
                                                    FlashcardShareManager.shareDeck(context, deckItem.displayName, deckCards, DeckShareFormat.ANKI_APKG)
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename Deck") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                deckToRename = deckItem.deck
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Deck", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                FlashcardRepository.deleteDeck(deckItem.deck.id)
                                                Toast.makeText(context, "Deck deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
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

@Composable
private fun RenameDeckDialog(
    deck: FlashcardDeck,
    onDismiss: () -> Unit,
    onRenamed: () -> Unit
) {
    var newName by remember { mutableStateOf(deck.name) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Rename Deck", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Deck Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                FlashcardRepository.renameDeck(deck.id, newName.trim())
                                onRenamed()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Rename")
                    }
                }
            }
        }
    }
}

private data class DeckSummaryItem(
    val name: String,
    val displayName: String,
    val isSubdeck: Boolean,
    val deck: FlashcardDeck,
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

private fun queryFileName(context: android.content.Context, uri: Uri): String {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Exception) {}
    }
    return uri.lastPathSegment ?: "deck.apkg"
}
