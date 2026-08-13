package com.kletaq.app.features.progress.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.features.progress.HeatmapDayTile

@Composable
fun GitHubContributionHeatmap(
    tiles: List<HeatmapDayTile>,
    totalStudyHours: Double,
    activeDays: Int,
    modifier: Modifier = Modifier
) {
    var selectedTile by remember { mutableStateOf<HeatmapDayTile?>(null) }

    val calculatedAvgMins = remember(totalStudyHours, activeDays) {
        if (activeDays > 0) ((totalStudyHours * 60) / activeDays).toInt() else 0
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📊", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Study Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // Active days badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleAccent.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = "$activeDays days active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Summary stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeatmapMiniStat(
                    label = "Total Hours",
                    value = String.format("%.1fh", totalStudyHours),
                    modifier = Modifier.weight(1f)
                )
                HeatmapMiniStat(
                    label = "Active Days",
                    value = "$activeDays",
                    modifier = Modifier.weight(1f)
                )
                HeatmapMiniStat(
                    label = "Avg / Day",
                    value = "${calculatedAvgMins}m",
                    modifier = Modifier.weight(1f)
                )
            }

            // 7-row x 12-week Grid
            val weeks = 12
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Day of week labels column
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    dayLabels.forEach { label ->
                        Box(
                            modifier = Modifier.height(13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 12 Weeks Columns
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    for (weekIdx in 0 until weeks) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            for (dayIdx in 0 until 7) {
                                val tileIdx = weekIdx * 7 + dayIdx
                                val tile = tiles.getOrNull(tileIdx)
                                val color = getHeatmapColor(tile?.intensityLevel ?: 0)
                                val isSelected = selectedTile == tile && tile != null

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(13.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (isSelected) PurpleAccent else color
                                        )
                                        .clickable(enabled = tile != null) {
                                            selectedTile = if (selectedTile == tile) null else tile
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Tile Inspector Banner (if tapped)
            AnimatedVisibility(
                visible = selectedTile != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedTile?.let { tile ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PurpleAccent.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 ${tile.dateString}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (tile.minutes > 0) "${tile.minutes} mins logged" else "No study logged",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent
                            )
                        }
                    }
                }
            }

            // Legend Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap any tile for details",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(text = "Less", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    listOf(0, 1, 2, 3, 4).forEach { lvl ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(getHeatmapColor(lvl))
                        )
                    }
                    Text(text = "More", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun HeatmapMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PurpleAccent.copy(alpha = 0.06f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PurpleAccent
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun getHeatmapColor(level: Int): Color {
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    return when (level) {
        0 -> emptyColor
        1 -> Color(0xFFA5B4FC)  // Soft Indigo
        2 -> Color(0xFF818CF8)  // Medium Indigo
        3 -> Color(0xFF6366F1)  // Vibrant Indigo (PurpleAccent)
        4 -> Color(0xFF4F46E5)  // Deep High Indigo
        else -> emptyColor
    }
}
