package com.kletaq.app.features.flashcards.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import com.kletaq.app.features.flashcards.ui.components.AudioRecorderChip
import com.kletaq.app.features.flashcards.ui.components.MultiImagePickerField
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardDialog(
    initialCard: Flashcard? = null,
    defaultTopicId: String = "",
    defaultTopicTitle: String = "",
    defaultSubjectId: String = "",
    defaultSubjectName: String = "",
    defaultDeckId: String? = null,
    onDismiss: () -> Unit,
    onSaveCard: (Flashcard) -> Unit
) {
    val allDecks by FlashcardRepository.decks.collectAsState()

    var cardType by remember { mutableStateOf(initialCard?.cardType ?: FlashcardType.BASIC) }
    var frontText by remember { mutableStateOf(initialCard?.front.orEmpty()) }
    var backText by remember { mutableStateOf(initialCard?.back.orEmpty()) }
    var typedExpectedAnswer by remember { mutableStateOf(initialCard?.typedAnswer.orEmpty()) }
    var frontImages by remember {
        mutableStateOf(initialCard?.allFrontImages.orEmpty())
    }
    var backImages by remember {
        mutableStateOf(initialCard?.allBackImages.orEmpty())
    }
    var audioPath by remember { mutableStateOf(initialCard?.audioPath) }
    var clozeField by remember {
        mutableStateOf(TextFieldValue(initialCard?.clozeText.orEmpty()))
    }
    var hintText by remember { mutableStateOf(initialCard?.hint.orEmpty()) }
    var tagsInput by remember { mutableStateOf(initialCard?.tags?.joinToString(", ").orEmpty()) }

    // Deck selection
    var selectedDeckId by remember {
        mutableStateOf(
            defaultDeckId
                ?: initialCard?.deckId
                ?: allDecks.firstOrNull { it.id == defaultTopicId }?.id
                ?: allDecks.firstOrNull()?.id
                ?: "default"
        )
    }
    var deckDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 36.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PurpleAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Style,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (initialCard != null) "Edit Flashcard" else "New Flashcard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (defaultTopicTitle.isNotBlank()) {
                                Text(
                                    text = "Node: $defaultTopicTitle",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Target Deck Selector
                if (allDecks.isNotEmpty()) {
                    val currentDeck = allDecks.find { it.id == selectedDeckId } ?: allDecks.first()
                    ExposedDropdownMenuBox(
                        expanded = deckDropdownExpanded,
                        onExpandedChange = { deckDropdownExpanded = !deckDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDeck.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Deck") },
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
                }

                // Card Type Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        FlashcardType.BASIC to "Basic",
                        FlashcardType.REVERSIBLE to "Reverse",
                        FlashcardType.CLOZE to "Cloze",
                        FlashcardType.TYPE_IN to "Type-In"
                    ).forEach { (type, label) ->
                        val isSelected = cardType == type
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { cardType = type }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Voice Note Audio Recorder Chip
                AudioRecorderChip(
                    audioPath = audioPath,
                    onAudioChanged = { audioPath = it },
                    isEditable = true
                )

                if (cardType == FlashcardType.CLOZE) {
                    // Cloze Editor
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cloze Text (Select word to cloze)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick Cloze Inserter Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PurpleAccent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable {
                                    val sel = clozeField.selection
                                    val current = clozeField.text
                                    val nextNum = (ClozeParser.extractClozeNumbers(current).maxOrNull() ?: 0) + 1
                                    if (sel.start != sel.end) {
                                        val selectedText = current.substring(sel.start, sel.end)
                                        val replacement = "{{c${nextNum}::$selectedText}}"
                                        val newText = current.replaceRange(sel.start, sel.end, replacement)
                                        clozeField = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(sel.start + replacement.length)
                                        )
                                    } else {
                                        val insert = "{{c${nextNum}::answer}}"
                                        val newText = current.substring(0, sel.start) + insert + current.substring(sel.start)
                                        clozeField = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(sel.start + insert.length)
                                        )
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ [{{c1::}}]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = clozeField,
                            onValueChange = { clozeField = it },
                            placeholder = { Text("e.g. Mitochondria is the {{c1::powerhouse}} of the cell.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(14.dp)
                        )

                        MultiImagePickerField(
                            label = "Cloze Diagrams / Images",
                            imagePaths = frontImages,
                            onImagesChanged = { frontImages = it }
                        )
                    }
                } else {
                    // Front Text
                    OutlinedTextField(
                        value = frontText,
                        onValueChange = { frontText = it },
                        label = { Text(if (cardType == FlashcardType.REVERSIBLE) "Front (Side 1)" else "Question / Front") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp)
                    )

                    MultiImagePickerField(
                        label = "Front Images",
                        imagePaths = frontImages,
                        onImagesChanged = { frontImages = it }
                    )

                    // Back Text
                    OutlinedTextField(
                        value = backText,
                        onValueChange = { backText = it },
                        label = { Text(if (cardType == FlashcardType.REVERSIBLE) "Back (Side 2)" else "Answer / Back") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp)
                    )

                    MultiImagePickerField(
                        label = "Back Images",
                        imagePaths = backImages,
                        onImagesChanged = { backImages = it }
                    )

                    // Type-In Answer Field
                    if (cardType == FlashcardType.TYPE_IN) {
                        OutlinedTextField(
                            value = typedExpectedAnswer,
                            onValueChange = { typedExpectedAnswer = it },
                            label = { Text("Exact Expected Answer to Type") },
                            placeholder = { Text("e.g. Photosynthesis") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // Hint & Tags
                OutlinedTextField(
                    value = hintText,
                    onValueChange = { hintText = it },
                    label = { Text("Optional Hint") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Preview Box for Cloze
                if (cardType == FlashcardType.CLOZE && clozeField.text.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Preview:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                            Text("Q: ${ClozeParser.renderQuestion(clozeField.text, 1)}", fontSize = 13.sp)
                            Text("A: ${ClozeParser.renderAnswer(clozeField.text, 1)}", fontSize = 13.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    val canSave = when (cardType) {
                        FlashcardType.CLOZE -> clozeField.text.isNotBlank()
                        FlashcardType.TYPE_IN -> frontText.isNotBlank() && (typedExpectedAnswer.isNotBlank() || backText.isNotBlank())
                        else -> (frontText.isNotBlank() || frontImages.isNotEmpty()) && (backText.isNotBlank() || backImages.isNotEmpty())
                    }

                    Button(
                        onClick = {
                            val targetDeck = allDecks.find { it.id == selectedDeckId }
                            val deckTitle = targetDeck?.name ?: defaultTopicTitle.ifBlank { "Default" }
                            val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val noteId = initialCard?.noteId?.ifBlank { null } ?: UUID.randomUUID().toString()

                            val baseCard = (initialCard ?: Flashcard()).copy(
                                noteId = noteId,
                                deckId = selectedDeckId,
                                deckName = deckTitle,
                                topicId = defaultTopicId.ifBlank { initialCard?.topicId.orEmpty() },
                                topicTitle = defaultTopicTitle.ifBlank { initialCard?.topicTitle.orEmpty() },
                                subjectId = defaultSubjectId.ifBlank { initialCard?.subjectId.orEmpty() },
                                subjectName = defaultSubjectName.ifBlank { initialCard?.subjectName.orEmpty() },
                                cardType = cardType,
                                front = frontText.trim(),
                                back = backText.trim(),
                                clozeText = clozeField.text.trim(),
                                hint = hintText.trim(),
                                frontImagePath = frontImages.firstOrNull(),
                                backImagePath = backImages.firstOrNull(),
                                frontImagePaths = frontImages,
                                backImagePaths = backImages,
                                audioPath = audioPath,
                                typedAnswer = if (cardType == FlashcardType.TYPE_IN) typedExpectedAnswer.trim() else "",
                                tags = parsedTags
                            )

                            when (cardType) {
                                FlashcardType.CLOZE -> {
                                    val siblings = ClozeParser.generateSiblingCards(noteId, clozeField.text.trim(), baseCard)
                                    FlashcardRepository.saveCardsBatch(siblings)
                                    onSaveCard(siblings.first())
                                }
                                FlashcardType.REVERSIBLE -> {
                                    val pair = ClozeParser.generateReversibleCards(noteId, frontText.trim(), backText.trim(), baseCard)
                                    FlashcardRepository.saveCardsBatch(pair)
                                    onSaveCard(pair.first())
                                }
                                else -> {
                                    onSaveCard(baseCard)
                                }
                            }
                            onDismiss()
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Card", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
