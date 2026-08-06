package com.studyos.app.features.focus.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.PurpleAccent

data class AmbientSoundItem(
    val id: String?,
    val name: String,
    val emoji: String,
    val description: String = ""
)

@Composable
fun AmbientSoundsSelector(
    selectedSoundId: String?,
    onSoundSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sounds = listOf(
        AmbientSoundItem("rain", "Rain", "🌧️"),
        AmbientSoundItem("cafe", "Café", "☕"),
        AmbientSoundItem("forest", "Forest", "🌲"),
        AmbientSoundItem("whitenoise", "White Noise", "🌊")
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Background Sound",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (selectedSoundId != null) {
                Text(
                    text = "Mute",
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = PurpleAccent,
                    modifier = Modifier.clickable { onSoundSelect(null) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sounds) { sound ->
                val isSelected = sound.id == selectedSoundId

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)) else null,
                    modifier = Modifier.clickable {
                        if (isSelected) onSoundSelect(null) else onSoundSelect(sound.id)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = sound.emoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sound.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
