package com.kletaq.app.features.progress.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.features.progress.WeeklyChartBar

@Composable
fun WeeklyStudyChart(
    bars: List<WeeklyChartBar>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = remember(bars) { (bars.maxOfOrNull { it.minutes } ?: 60).coerceAtLeast(60) }
    val totalWeekMins = remember(bars) { bars.sumOf { it.minutes } }
    val avgMinutes = remember(bars) { if (bars.isNotEmpty()) totalWeekMins / bars.size else 0 }

    var selectedBar by remember { mutableStateOf<WeeklyChartBar?>(null) }

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
                    Text(text = "📈", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Weekly Focus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // Total badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleAccent.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = formatMinutesToHours(totalWeekMins) + " total",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Bars Container Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                // Bars Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    bars.forEach { bar ->
                        val targetFraction = (bar.minutes.toFloat() / maxMinutes).coerceIn(0.05f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = targetFraction,
                            animationSpec = tween(durationMillis = 700),
                            label = "BarHeight"
                        )
                        val isSelected = selectedBar == bar

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedBar = if (selectedBar == bar) null else bar
                                }
                        ) {
                            // Minute text above bar
                            if (bar.minutes > 0) {
                                Text(
                                    text = if (bar.minutes >= 60) "${bar.minutes / 60}h" else "${bar.minutes}m",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bar.isToday || isSelected) PurpleAccent else TextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Bar capsule inside background track capsule
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animatedFraction)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            brush = if (bar.isToday || isSelected) {
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        PurpleAccent,
                                                        PurpleAccent.copy(alpha = 0.75f)
                                                    )
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFFA5B4FC),
                                                        Color(0xFFA5B4FC).copy(alpha = 0.45f)
                                                    )
                                                )
                                            }
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Day label
                            Text(
                                text = bar.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = if (bar.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (bar.isToday) PurpleAccent else TextSecondary
                            )
                        }
                    }
                }
            }

            // Selected Bar Details Banner (on tap)
            AnimatedVisibility(
                visible = selectedBar != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedBar?.let { bar ->
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
                                text = "📅 ${bar.dayLabel} (${bar.dateString})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = formatMinutesToHours(bar.minutes),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent
                            )
                        }
                    }
                }
            }

            // Footer stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Avg: ${formatMinutesToHours(avgMinutes)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                // Legend indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PurpleAccent)
                    )
                    Text(text = "Today", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFA5B4FC))
                    )
                    Text(text = "Other", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun formatMinutesToHours(mins: Int): String {
    if (mins <= 0) return "0m"
    val hours = mins / 60
    val remMins = mins % 60
    return if (hours > 0) {
        if (remMins > 0) "${hours}h ${remMins}m" else "${hours}h"
    } else {
        "${remMins}m"
    }
}
