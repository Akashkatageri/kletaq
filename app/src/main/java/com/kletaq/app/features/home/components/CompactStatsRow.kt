package com.kletaq.app.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

@Composable
fun CompactStatsRow(
    streakDays: Int = 0,
    isStreakActiveToday: Boolean = false,
    shieldsCount: Int = 3,
    xpTotal: Int = 0,
    activeTasksCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isLit = isStreakActiveToday && streakDays > 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Streak Capsule
        StatCapsule(
            modifier = Modifier.weight(1f),
            iconEmoji = if (isLit) "🔥" else "⏳",
            valueText = "${streakDays}d",
            labelText = if (isLit) "Streak" else "Pending",
            accentColor = if (isLit) Color(0xFFEA580C) else Color(0xFFF59E0B),
            containerColor = if (isDark) {
                if (isLit) Color(0xFF2E1505) else Color(0xFF221708)
            } else {
                if (isLit) Color(0xFFFFF7ED) else Color(0xFFFFFBEB)
            },
            borderColor = if (isDark) {
                if (isLit) Color(0xFF9A3412) else Color(0xFF78350F)
            } else {
                if (isLit) Color(0xFFFFEDD5) else Color(0xFFFEF3C7)
            }
        )

        // Shields Capsule
        StatCapsule(
            modifier = Modifier.weight(1f),
            iconEmoji = "🛡️",
            valueText = "$shieldsCount",
            labelText = "Shields",
            accentColor = Color(0xFF2563EB),
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFEFF6FF),
            borderColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
        )

        // XP Capsule
        val formattedXp = if (xpTotal >= 10000) {
            "${xpTotal / 1000}k"
        } else {
            "$xpTotal"
        }

        StatCapsule(
            modifier = Modifier.weight(1f),
            iconEmoji = "⚡",
            valueText = formattedXp,
            labelText = "XP",
            accentColor = Color(0xFF7C3AED),
            containerColor = if (isDark) Color(0xFF19102E) else Color(0xFFF5F3FF),
            borderColor = if (isDark) Color(0xFF4C1D95) else Color(0xFFEDE9FE)
        )

        // Tasks Capsule
        StatCapsule(
            modifier = Modifier.weight(1f),
            iconEmoji = "📋",
            valueText = "$activeTasksCount",
            labelText = "Tasks",
            accentColor = Color(0xFF059669),
            containerColor = if (isDark) Color(0xFF06231C) else Color(0xFFECFDF5),
            borderColor = if (isDark) Color(0xFF065F46) else Color(0xFFD1FAE5)
        )
    }
}

@Composable
private fun StatCapsule(
    modifier: Modifier = Modifier,
    iconEmoji: String,
    valueText: String,
    labelText: String,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = iconEmoji,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = valueText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = labelText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
