package com.kletaq.app.features.flashcards.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.model.Flashcard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CardInfoDialog(
    card: Flashcard,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

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
                    text = "Card Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                InfoRow("Card Type", card.cardType.name)
                InfoRow("Queue State", card.queueState.name)
                InfoRow("Deck", card.deckName.ifBlank { "Default" })
                InfoRow("Total Reviews", card.reps.toString())
                InfoRow("Lapses", card.lapses.toString())
                InfoRow("Interval", "${card.intervalDays} days")
                InfoRow("Ease Factor", "${(card.easeFactor * 100).toInt()}%")

                if (card.stability > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FSRS Cognitive Metrics",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    InfoRow("Stability (S)", String.format(Locale.US, "%.2f days", card.stability))
                    InfoRow("Difficulty (D)", String.format(Locale.US, "%.2f / 10.0", card.difficulty))
                    InfoRow("Scheduled", "${card.scheduledDays} days")
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                InfoRow("Due Date", dateFormat.format(Date(card.dueTimestamp)))
                InfoRow("Created", dateFormat.format(Date(card.createdAt)))
                InfoRow("Last Reviewed", if (card.lastReviewedTimestamp > 0L) dateFormat.format(Date(card.lastReviewedTimestamp)) else "Never")
                InfoRow("Leech", if (card.isLeech) "Yes" else "No")
                InfoRow("Suspended", if (card.isSuspended) "Yes" else "No")
                InfoRow("Buried", if (card.isBuried) "Yes" else "No")

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

