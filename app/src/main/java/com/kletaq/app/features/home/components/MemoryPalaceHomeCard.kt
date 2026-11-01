package com.kletaq.app.features.home.components

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.WarningAmber
import com.kletaq.app.features.memorypalace.data.MemoryPalaceRepository

private val PalacePurple = Color(0xFF8B5CF6)

@Composable
fun MemoryPalaceHomeCard(
    onOpenPalace: () -> Unit
) {
    val palaces by MemoryPalaceRepository.palaces.collectAsState()
    val derivations by MemoryPalaceRepository.derivations.collectAsState()

    val dueDerivations = remember(derivations) {
        MemoryPalaceRepository.getDueDerivations()
    }
    val hasDue = dueDerivations.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPalace() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(
            1.dp,
            if (hasDue) WarningAmber.copy(alpha = 0.5f) else PalacePurple.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Tag + Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = (if (hasDue) WarningAmber else PalacePurple).copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (hasDue) WarningAmber else PalacePurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "MEMORY PALACE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasDue) WarningAmber else PalacePurple,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Patrick Jane's Method of Loci",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (hasDue) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarningAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${dueDerivations.size} DUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Body text
            val headline = if (hasDue) {
                val firstDue = dueDerivations.first()
                "Review ready: ${firstDue.title}"
            } else if (palaces.isNotEmpty()) {
                "${palaces.size} Palaces • ${derivations.size} Derivations mapped"
            } else {
                "Master complex math proofs & code derivations"
            }

            val subtitle = if (hasDue) {
                "Walk your room mentally to lock in sequential derivation steps."
            } else if (palaces.isNotEmpty()) {
                "Anchor formulas to objects in your room for instant exam recall."
            } else {
                "Snap 1-3 room photos, drop pins on objects, and anchor LaTeX steps."
            }

            Text(
                text = headline,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            // Bottom Action
            Button(
                onClick = onOpenPalace,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasDue) WarningAmber else PalacePurple
                )
            ) {
                Text(
                    text = if (hasDue) "Start Recall Walk" else if (palaces.isNotEmpty()) "Open Memory Palaces" else "Build Memory Palace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }
    }
}

