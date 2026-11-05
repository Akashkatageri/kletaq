package com.kletaq.app.features.flashcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.model.FlashcardDeck

enum class CustomStudyMode {
    REVIEW_AHEAD,
    REVIEW_FORGOTTEN,
    CRAM_ALL,
    STUDY_BY_TAG
}

@Composable
fun CustomStudyDialog(
    deck: FlashcardDeck,
    availableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onStartCustomStudy: (mode: CustomStudyMode, daysAhead: Int, tag: String?) -> Unit
) {
    var selectedMode by remember { mutableStateOf(CustomStudyMode.REVIEW_AHEAD) }
    var daysAhead by remember { mutableIntStateOf(3) }
    var selectedTag by remember { mutableStateOf(availableTags.firstOrNull() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "Custom Study: ${deck.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                CustomStudyOptionRow(
                    icon = Icons.Default.Alarm,
                    title = "Review Ahead",
                    subtitle = "Study cards due in the next few days early",
                    isSelected = selectedMode == CustomStudyMode.REVIEW_AHEAD,
                    onClick = { selectedMode = CustomStudyMode.REVIEW_AHEAD }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomStudyOptionRow(
                    icon = Icons.Default.Replay,
                    title = "Review Forgotten Cards",
                    subtitle = "Review cards with past lapses or failed recalls",
                    isSelected = selectedMode == CustomStudyMode.REVIEW_FORGOTTEN,
                    onClick = { selectedMode = CustomStudyMode.REVIEW_FORGOTTEN }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomStudyOptionRow(
                    icon = Icons.Default.Bolt,
                    title = "Cram Mode",
                    subtitle = "Review all cards in deck regardless of due date",
                    isSelected = selectedMode == CustomStudyMode.CRAM_ALL,
                    onClick = { selectedMode = CustomStudyMode.CRAM_ALL }
                )

                if (availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomStudyOptionRow(
                        icon = Icons.Default.LocalOffer,
                        title = "Study by Tag",
                        subtitle = "Target a specific topic or chapter tag",
                        isSelected = selectedMode == CustomStudyMode.STUDY_BY_TAG,
                        onClick = { selectedMode = CustomStudyMode.STUDY_BY_TAG }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedMode == CustomStudyMode.REVIEW_AHEAD) {
                    OutlinedTextField(
                        value = daysAhead.toString(),
                        onValueChange = { daysAhead = it.toIntOrNull()?.coerceIn(1, 30) ?: 1 },
                        label = { Text("Days Ahead") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (selectedMode == CustomStudyMode.STUDY_BY_TAG && availableTags.isNotEmpty()) {
                    OutlinedTextField(
                        value = selectedTag,
                        onValueChange = { selectedTag = it },
                        label = { Text("Tag to Study") },
                        placeholder = { Text("e.g. ${availableTags.first()}") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            onStartCustomStudy(selectedMode, daysAhead, selectedTag.ifBlank { null })
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Start Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomStudyOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) PurpleAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

