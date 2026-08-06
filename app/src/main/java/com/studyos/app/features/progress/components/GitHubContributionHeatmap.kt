package com.studyos.app.features.progress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.BorderColor
import com.studyos.app.core.theme.CardSurface
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.core.theme.TextPrimary
import com.studyos.app.core.theme.TextSecondary
import com.studyos.app.features.progress.HeatmapDayTile

@Composable
fun GitHubContributionHeatmap(
    tiles: List<HeatmapDayTile>,
    totalStudyHours: Double,
    activeDays: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📊", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Study Activity Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "$activeDays active days",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    dayLabels.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.height(14.dp)
                        )
                    }
                }

                // 12 Weeks Columns
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    for (weekIdx in 0 until weeks) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            for (dayIdx in 0 until 7) {
                                val tileIdx = weekIdx * 7 + dayIdx
                                val tile = tiles.getOrNull(tileIdx)
                                val color = getHeatmapColor(tile?.intensityLevel ?: 0)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .background(color, RoundedCornerShape(3.dp))
                                        .border(
                                            width = 0.5.dp,
                                            color = BorderColor.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                            }
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
                    text = "Total ${String.format("%.1f", totalStudyHours)} hours logged",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Less", fontSize = 9.sp, color = TextSecondary)
                    listOf(0, 1, 2, 3, 4).forEach { lvl ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(getHeatmapColor(lvl), RoundedCornerShape(2.dp))
                        )
                    }
                    Text(text = "More", fontSize = 9.sp, color = TextSecondary)
                }
            }
        }
    }
}

private fun getHeatmapColor(level: Int): Color {
    return when (level) {
        0 -> Color(0xFF334155).copy(alpha = 0.4f) // Empty Slate
        1 -> Color(0xFF86EFAC) // Light Green
        2 -> Color(0xFF22C55E) // Medium Green
        3 -> Color(0xFF16A34A) // Dark Green
        4 -> Color(0xFF6366F1) // Electric Indigo High
        else -> Color(0xFF334155).copy(alpha = 0.4f)
    }
}
