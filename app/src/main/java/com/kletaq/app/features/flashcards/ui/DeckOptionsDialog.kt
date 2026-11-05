package com.kletaq.app.features.flashcards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.data.FlashcardRepository
import com.kletaq.app.features.flashcards.model.DeckPreset
import com.kletaq.app.features.flashcards.model.FlashcardDeck

@Composable
fun DeckOptionsDialog(
    deck: FlashcardDeck,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val initialPreset = remember(deck.presetId) {
        FlashcardRepository.getPreset(deck.presetId)
    }

    var newCardsPerDay by remember { mutableIntStateOf(initialPreset.newCardsPerDay) }
    var maxReviewsPerDay by remember { mutableIntStateOf(initialPreset.maxReviewsPerDay) }
    var useFsrs by remember { mutableStateOf(initialPreset.useFsrs) }
    var desiredRetention by remember { mutableDoubleStateOf(initialPreset.desiredRetention) }
    var leechThreshold by remember { mutableIntStateOf(initialPreset.leechThreshold) }
    var suspendOnLeech by remember { mutableStateOf(initialPreset.leechAction.equals("SUSPEND", ignoreCase = true)) }
    var autoBurySiblings by remember { mutableStateOf(initialPreset.autoBurySiblings) }

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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Deck Options: ${deck.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Limits Section
                Text(
                    text = "Daily Limits",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newCardsPerDay.toString(),
                        onValueChange = { newCardsPerDay = it.toIntOrNull()?.coerceAtLeast(0) ?: 0 },
                        label = { Text("New Cards/Day") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxReviewsPerDay.toString(),
                        onValueChange = { maxReviewsPerDay = it.toIntOrNull()?.coerceAtLeast(0) ?: 0 },
                        label = { Text("Max Reviews/Day") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Algorithm Section
                Text(
                    text = "Spaced Repetition Engine",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (useFsrs) "FSRS v4.5 / v5 (Recommended)" else "Classic SM-2 Algorithm",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (useFsrs) "Modern cognitive model with Stability & Difficulty" else "Standard Anki SM-2 ease factor scheduler",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = useFsrs,
                        onCheckedChange = { useFsrs = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleAccent)
                    )
                }

                if (useFsrs) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Target Retention: ${(desiredRetention * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = desiredRetention.toFloat(),
                        onValueChange = { desiredRetention = it.toDouble() },
                        valueRange = 0.70f..0.98f,
                        steps = 27
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Sibling Burying
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Bury Sibling Cards",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hides related cloze and reverse cards until tomorrow",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = autoBurySiblings,
                        onCheckedChange = { autoBurySiblings = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleAccent)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Leech Management
                Text(
                    text = "Leech Management",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = leechThreshold.toString(),
                        onValueChange = { leechThreshold = it.toIntOrNull()?.coerceAtLeast(1) ?: 8 },
                        label = { Text("Leech Threshold (Lapses)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(160.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (suspendOnLeech) "Suspend Card" else "Tag Only",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = suspendOnLeech,
                            onCheckedChange = { suspendOnLeech = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PurpleAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            val updatedPreset = initialPreset.copy(
                                newCardsPerDay = newCardsPerDay,
                                maxReviewsPerDay = maxReviewsPerDay,
                                useFsrs = useFsrs,
                                desiredRetention = desiredRetention,
                                autoBurySiblings = autoBurySiblings,
                                leechThreshold = leechThreshold,
                                leechAction = if (suspendOnLeech) "SUSPEND" else "TAG_ONLY"
                            )
                            FlashcardRepository.savePreset(updatedPreset)
                            onSaved()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Options", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

