package com.kletaq.app.features.flashcards.ui

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.data.apkg.AnkiPackageImporter
import com.kletaq.app.features.flashcards.data.apkg.DelimitedTextImporter
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardDeck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDeckDialog(
    fileUri: Uri,
    fileName: String = "deck",
    onDismiss: () -> Unit,
    onImportCompleted: (deckId: String, cardCount: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allDecks by FlashcardRepository.decks.collectAsState()

    var isParsing by remember { mutableStateOf(true) }
    var isImporting by remember { mutableStateOf(false) }
    var detectedDeckName by remember { mutableStateOf(fileName.substringBeforeLast(".")) }
    var parsedCards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var mediaCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedDeckId by remember { mutableStateOf("__NEW__") }
    var deckDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(fileUri) {
        withContext(Dispatchers.IO) {
            try {
                val lowerName = fileName.lowercase()
                val isApkg = lowerName.endsWith(".apkg") || lowerName.endsWith(".colpkg") || isAnkiPackageStream(context, fileUri)
                val isDelimited = lowerName.endsWith(".csv") || lowerName.endsWith(".tsv") || lowerName.endsWith(".txt")

                if (isApkg) {
                    val result = AnkiPackageImporter.importApkg(context, fileUri)
                    if (result.success) {
                        detectedDeckName = result.deckName.ifBlank { fileName.substringBeforeLast(".") }
                        parsedCards = result.cards
                        mediaCount = result.mediaCount
                    } else {
                        errorMessage = result.errorMessage ?: "Failed to parse .apkg"
                    }
                } else if (isDelimited) {
                    val cards = DelimitedTextImporter.parseUri(context, fileUri, targetDeckName = detectedDeckName)
                    parsedCards = cards
                    if (cards.isEmpty()) {
                        errorMessage = "No valid cards found in text file"
                    }
                } else {
                    errorMessage = "Unsupported file type. Please select an Anki .apkg or .csv/.tsv deck."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error importing deck"
            } finally {
                isParsing = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PurpleAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = PurpleAccent)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Import Flashcard Deck",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = fileName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isParsing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PurpleAccent, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Reading and extracting deck...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Import Error", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Preview Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cards Detected:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${parsedCards.size} cards", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                            }

                            if (mediaCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Media Extracted:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$mediaCount files", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Destination Deck Option
                    ExposedDropdownMenuBox(
                        expanded = deckDropdownExpanded,
                        onExpandedChange = { deckDropdownExpanded = !deckDropdownExpanded }
                    ) {
                        val currentLabel = if (selectedDeckId == "__NEW__") "Create new deck: '$detectedDeckName'" else {
                            allDecks.find { it.id == selectedDeckId }?.name ?: "Select Deck"
                        }

                        OutlinedTextField(
                            value = currentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Destination Deck") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deckDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = deckDropdownExpanded,
                            onDismissRequest = { deckDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("+ Create new deck: '$detectedDeckName'") },
                                onClick = {
                                    selectedDeckId = "__NEW__"
                                    deckDropdownExpanded = false
                                }
                            )
                            allDecks.forEach { deckItem ->
                                DropdownMenuItem(
                                    text = { Text(deckItem.name) },
                                    onClick = {
                                        selectedDeckId = deckItem.id
                                        deckDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedDeckId == "__NEW__") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = detectedDeckName,
                            onValueChange = { detectedDeckName = it },
                            label = { Text("New Deck Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("Cancel")
                    }

                    if (!isParsing && errorMessage == null && parsedCards.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            enabled = !isImporting,
                            onClick = {
                                if (isImporting) return@Button
                                isImporting = true
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val targetDeckId = if (selectedDeckId == "__NEW__") {
                                            val newDeck = FlashcardRepository.createDeck(name = detectedDeckName.trim())
                                            newDeck.id
                                        } else {
                                            selectedDeckId
                                        }

                                        val targetDeck = allDecks.find { it.id == targetDeckId }
                                        val finalDeckName = targetDeck?.name ?: detectedDeckName

                                        val remappedCards = parsedCards.map { card ->
                                            card.copy(deckId = targetDeckId, deckName = finalDeckName)
                                        }
                                        FlashcardRepository.saveCardsBatch(remappedCards)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Imported ${remappedCards.size} cards! 🗂️", Toast.LENGTH_SHORT).show()
                                            onImportCompleted(targetDeckId, remappedCards.size)
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Importing...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import ${parsedCards.size} Cards", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isAnkiPackageStream(context: android.content.Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val header = ByteArray(4)
            val bytesRead = stream.read(header)
            // Check ZIP header: PK\u0003\u0004 (0x50, 0x4B, 0x03, 0x04)
            bytesRead == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
        } ?: false
    } catch (_: Exception) {
        false
    }
}

