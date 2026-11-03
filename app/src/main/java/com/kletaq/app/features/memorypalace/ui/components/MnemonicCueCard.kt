package com.kletaq.app.features.memorypalace.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep

/**
 * 🃏 Mnemonic Cue Card
 * Pre-reveal mental theater card: prompts the student to stand at their room landmark,
 * retrieve the surreal visual cue from spatial memory, then reveal the underlying notes and equations.
 */
@Composable
fun MnemonicCueCard(
    locus: PalaceLocus?,
    step: PalaceStep,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locusName = locus?.name ?: "Room Landmark"
    val emoji = getLocusEmoji(locusName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B4B).copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Anchor Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF4338CA), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Anchor: $locusName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Step ${step.stepNumber} • Spatial Retrieval Cue",
                        fontSize = 11.sp,
                        color = Color(0xFFA5B4FC)
                    )
                }
            }

            // Sensory Mental Prompt
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Mental Imagery Cue:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Stand at your $locusName. Visualize what bizarre, exaggerated event is happening here. What is the mathematical transformation or milestone?",
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 18.sp
                    )
                }
            }

            // Big Action Button: Reveal Formula & Mental Story
            Button(
                onClick = onReveal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reveal Formula & Mental Story", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

private fun getLocusEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.contains("bed") -> "🛏️"
        lower.contains("desk") || lower.contains("table") -> "🪑"
        lower.contains("lamp") || lower.contains("light") -> "💡"
        lower.contains("door") -> "🚪"
        lower.contains("window") -> "🪟"
        lower.contains("sofa") || lower.contains("couch") -> "🛋️"
        lower.contains("shelf") || lower.contains("book") -> "📚"
        lower.contains("board") -> "📋"
        lower.contains("fan") -> "🌀"
        lower.contains("mirror") -> "🪞"
        lower.contains("clock") -> "⏰"
        lower.contains("chair") -> "🪑"
        lower.contains("tv") || lower.contains("screen") || lower.contains("monitor") -> "🖥️"
        else -> "📍"
    }
}

