package com.kletaq.app.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

@Composable
fun StudyWhyCard(
    studyWhy: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (studyWhy.isBlank()) {
        // Empty state: Small non-intrusive action under the header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PurpleAccent.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.18f)),
            modifier = modifier
                .fillMaxWidth()
                .clickable { onEditClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add your reason for studying",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
            }
        }
    } else {
        // Calm personal note card
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = InkPaperBorder.HeavyShape,
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = InkPaperBorder.heavyBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned Note",
                            tint = PurpleAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Your Goal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit reason",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = studyWhy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
