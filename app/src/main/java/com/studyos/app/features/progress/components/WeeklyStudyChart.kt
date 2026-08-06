package com.studyos.app.features.progress.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.studyos.app.features.progress.WeeklyChartBar

@Composable
fun WeeklyStudyChart(
    bars: List<WeeklyChartBar>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = (bars.maxOfOrNull { it.minutes } ?: 60).coerceAtLeast(60)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📈", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Weekly Focus Minutes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                val totalWeekMins = bars.sumOf { it.minutes }
                Text(
                    text = "${totalWeekMins}m this week",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
            }

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val targetFraction = (bar.minutes.toFloat() / maxMinutes).coerceIn(0.05f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(durationMillis = 600),
                        label = "BarHeight"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Minute text
                        if (bar.minutes > 0) {
                            Text(
                                text = "${bar.minutes}m",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bar.isToday) PurpleAccent else TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Bar Capsule
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(animatedFraction)
                                .background(
                                    color = if (bar.isToday) PurpleAccent else Color(0xFF38BDF8),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day label
                        Text(
                            text = bar.dayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (bar.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (bar.isToday) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
