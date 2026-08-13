package com.kletaq.app.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.StreakOrange
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.XpAmber
import com.kletaq.app.core.theme.InkPaperBorder

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary

@Composable
fun CompactStatsRow(
    streakDays: Int = 0,
    shieldsCount: Int = 3,
    xpTotal: Int = 0,
    activeTasksCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Streak Paper Note Card
        PaperNoteCard(
            emoji = "🔥",
            textStr = "${streakDays}d",
            labelStr = "Streak",
            modifier = Modifier.weight(1f)
        )

        // Shields Paper Note Card
        PaperNoteCard(
            emoji = "🛡️",
            textStr = "$shieldsCount",
            labelStr = "Shields",
            modifier = Modifier.weight(1f)
        )

        // XP Paper Note Card
        PaperNoteCard(
            emoji = "⚡",
            textStr = "$xpTotal",
            labelStr = "XP",
            modifier = Modifier.weight(1f)
        )

        // Tasks Paper Note Card
        PaperNoteCard(
            emoji = "📋",
            textStr = "$activeTasksCount",
            labelStr = "Tasks",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaperNoteCard(
    emoji: String,
    textStr: String,
    labelStr: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = InkPaperBorder.MediumShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = InkPaperBorder.mediumBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = emoji, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = textStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = labelStr,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
