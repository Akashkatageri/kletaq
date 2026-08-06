package com.studyos.app.features.focus.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.PurpleAccent

enum class SessionMode(val label: String, val emoji: String, val defaultMinutes: Int) {
    POMODORO("Pomodoro", "🍅", 25),
    DEEP_FOCUS("Deep Focus", "⚡", 50),
    EXAM_PREP("Exam Prep", "🧠", 90),
    CUSTOM("Custom", "🎯", 30)
}

@Composable
fun SessionTypeSelector(
    selectedMode: SessionMode,
    customMinutes: Int = 30,
    onModeSelected: (SessionMode) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SessionMode.values().forEach { mode ->
            val isSelected = mode == selectedMode
            val displayMinutes = if (mode == SessionMode.CUSTOM) customMinutes else mode.defaultMinutes

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onModeSelected(mode)
                        if (mode == SessionMode.CUSTOM) {
                            onCustomClick()
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "${mode.emoji} ${mode.label}\n(${displayMinutes}m)",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
