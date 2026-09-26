package com.kletaq.app.features.focus.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.home.components.ProgressBar

@Composable
fun CircularTimerHero(
    timeFormatted: String = "25:00",
    progress: Float = 0.0f,
    xpEarned: Int = 0,
    streakDays: Int = 0,
    isStreakActiveToday: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val primaryAccent = MaterialTheme.colorScheme.primary

        val timerSize = if (compact) 124.dp else 190.dp
        val ringSize = if (compact) 116.dp else 180.dp
        val strokeWidth = if (compact) 8.dp else 11.dp
        val timerTextSize = if (compact) 25.sp else 38.sp
        val progressWidth = if (compact) 116.dp else 180.dp

        Box(
            modifier = Modifier.size(timerSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val strokeWidthPx = strokeWidth.toPx()

                // Background Ring Track
                drawArc(
                    color = primaryAccent.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Active Progress Sweep Arc
                drawArc(
                    color = primaryAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // Center Countdown Time Typography (Hero Element)
            Text(
                text = timeFormatted,
                fontSize = timerTextSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 4.dp else 10.dp))

        // Progress percentage bar (████████░░ 70%)
        Box(modifier = Modifier.width(progressWidth)) {
            ProgressBar(
                progress = progress,
                height = 6.dp,
                progressColor = PurpleAccent,
                backgroundColor = PurpleAccent.copy(alpha = 0.15f)
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))

        Text(
            text = "${(progress * 100).toInt()}% complete",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

        // 🔥 7-day streak   ⚡ +80 XP Badges Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isLit = isStreakActiveToday && streakDays > 0
            val streakBgColor = if (isLit) Color(0xFFFF9800).copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
            val streakContentColor = if (isLit) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = streakBgColor,
                contentColor = streakContentColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isLit) "🔥" else "🩶", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (streakDays > 0) "$streakDays-day streak" else "0-day streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PurpleAccent.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "⚡ +$xpEarned XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
