package com.kletaq.app.features.flashcards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.flashcards.model.ReviewLogEntry
import java.util.Calendar

@Composable
fun ReviewHeatmapView(
    reviewLogs: List<ReviewLogEntry>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val (matrix, stats) = remember(reviewLogs) {
        computeHeatmapData(reviewLogs)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Review Activity Heatmap",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last 52 weeks (365 days)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stats.totalReviews}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent
                        )
                        Text(
                            text = "Reviews",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stats.daysStudied}d",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Active Days",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 52-Week Matrix (Scrollable horizontally)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 52 week columns
                for (weekIndex in 0 until 52) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (dayIndex in 0 until 7) {
                            val count = matrix.getOrNull(weekIndex)?.getOrNull(dayIndex) ?: 0
                            val cellColor = getIntensityColor(count)
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0, 3, 10, 20, 35).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getIntensityColor(level))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class HeatmapStats(
    val totalReviews: Int,
    val daysStudied: Int
)

private fun computeHeatmapData(logs: List<ReviewLogEntry>): Pair<List<List<Int>>, HeatmapStats> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val todayMillis = calendar.timeInMillis
    val oneDayMillis = 86_400_000L

    // Map of day offset from today (0 = today, 1 = yesterday, etc.) -> count
    val dailyCounts = mutableMapOf<Int, Int>()
    var totalReviews = 0

    for (log in logs) {
        val diffDays = ((todayMillis - log.reviewTimestamp) / oneDayMillis).toInt()
        if (diffDays in 0..364) {
            dailyCounts[diffDays] = (dailyCounts[diffDays] ?: 0) + 1
            totalReviews++
        }
    }

    val daysStudied = dailyCounts.keys.size

    // Build 52 weeks x 7 days grid (0 = 51 weeks ago, 51 = this week)
    val grid = MutableList(52) { MutableList(7) { 0 } }

    for (week in 0 until 52) {
        for (day in 0 until 7) {
            val daysAgo = (51 - week) * 7 + (6 - day)
            grid[week][day] = dailyCounts[daysAgo] ?: 0
        }
    }

    return Pair(grid, HeatmapStats(totalReviews = totalReviews, daysStudied = daysStudied))
}

@Composable
private fun getIntensityColor(count: Int): Color {
    val isDark = MaterialTheme.colorScheme.surface.hashCode() < 0 // Basic check or use theme
    return when {
        count == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        count in 1..4 -> Color(0xFF86EFAC)
        count in 5..14 -> Color(0xFF22C55E)
        count in 15..29 -> Color(0xFF16A34A)
        else -> Color(0xFF15803D)
    }
}

