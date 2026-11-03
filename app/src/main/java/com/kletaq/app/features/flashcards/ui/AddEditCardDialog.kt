package com.kletaq.app.features.flashcards.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardMediaManager
import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import kotlinx.coroutines.launch

@Composable
fun AddEditCardDialog(
    initialCard: Flashcard? = null,
    defaultTopicId: String = "",
    defaultTopicTitle: String = "",
    defaultSubjectId: String = "",
    defaultSubjectName: String = "",
    onDismiss: () -> Unit,
    onSaveCard: (Flashcard) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cardType by remember { mutableStateOf(initialCard?.cardType ?: FlashcardType.BASIC) }
    var frontText by remember { mutableStateOf(initialCard?.front.orEmpty()) }
    var backText by remember { mutableStateOf(initialCard?.back.orEmpty()) }
    var frontImagePath by remember { mutableStateOf(initialCard?.frontImagePath) }
    var backImagePath by remember { mutableStateOf(initialCard?.backImagePath) }
    var clozeField by remember {
        mutableStateOf(TextFieldValue(initialCard?.clozeText.orEmpty()))
    }
    var hintText by remember { mutableStateOf(initialCard?.hint.orEmpty()) }
    var tagsInput by remember { mutableStateOf(initialCard?.tags?.joinToString(", ").orEmpty()) }
    var showPreview by remember { mutableStateOf(false) }

    val frontImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FlashcardMediaManager.saveImageFromUri(context, it)
                if (path != null) frontImagePath = path
            }
        }
    }

    val backImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = FlashcardMediaManager.saveImageFromUri(context, it)
                if (path != null) backImagePath = path
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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

                // Card Type Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(FlashcardType.BASIC, FlashcardType.CLOZE).forEach { type ->
                        val isSelected = cardType == type
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { cardType = type }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (type == FlashcardType.BASIC) "Basic (Front / Back)" else "Cloze Deletion {{c1::}}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (cardType == FlashcardType.BASIC) {
                    // Front
                    OutlinedTextField(
                        value = frontText,
                        onValueChange = { frontText = it },
                        label = { Text("Front (Question / Prompt)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp)
                    )

                    ImageAttachmentField(
                        label = "+ Add Front Image / Diagram",
                        imagePath = frontImagePath,
                        onPickImage = { frontImageLauncher.launch("image/*") },
                        onRemoveImage = { frontImagePath = null }
                    )

                    // Back
                    OutlinedTextField(
                        value = backText,
                        onValueChange = { backText = it },
                        label = { Text("Back (Answer / Explanation)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp)
                    )

                    ImageAttachmentField(
                        label = "+ Add Back Image / Diagram",
                        imagePath = backImagePath,
                        onPickImage = { backImageLauncher.launch("image/*") },
                        onRemoveImage = { backImagePath = null }
                    )
                } else {
                    // Cloze Editor
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cloze Text (Highlight word to cloze)",
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

                        ImageAttachmentField(
                            label = "+ Add Cloze Diagram / Image",
                            imagePath = frontImagePath,
                            onPickImage = { frontImageLauncher.launch("image/*") },
                            onRemoveImage = { frontImagePath = null }
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

                // Preview Box
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

                Spacer(modifier = Modifier.weight(1f, fill = false))

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

                    val canSave = if (cardType == FlashcardType.BASIC) {
                        (frontText.isNotBlank() || frontImagePath != null) && (backText.isNotBlank() || backImagePath != null)
                    } else {
                        clozeField.text.isNotBlank()
                    }
                    Button(
                        onClick = {
                            val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val newCard = (initialCard ?: Flashcard()).copy(
                                deckId = defaultTopicId.ifBlank { initialCard?.deckId.orEmpty() },
                                deckName = defaultTopicTitle.ifBlank { initialCard?.deckName.orEmpty() },
                                topicId = defaultTopicId.ifBlank { initialCard?.topicId.orEmpty() },
                                topicTitle = defaultTopicTitle.ifBlank { initialCard?.topicTitle.orEmpty() },
                                subjectId = defaultSubjectId.ifBlank { initialCard?.subjectId.orEmpty() },
                                subjectName = defaultSubjectName.ifBlank { initialCard?.subjectName.orEmpty() },
                                cardType = cardType,
                                front = frontText.trim(),
                                back = backText.trim(),
                                clozeText = clozeField.text.trim(),
                                hint = hintText.trim(),
                                frontImagePath = frontImagePath,
                                backImagePath = backImagePath,
                                tags = parsedTags
                            )
                            onSaveCard(newCard)
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

@Composable
private fun ImageAttachmentField(
    label: String,
    imagePath: String?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit
) {
    if (imagePath != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clickable { onRemoveImage() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPickImage() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

