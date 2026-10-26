package com.kletaq.app.features.journey.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.SuccessGreen

@Composable
fun MetroModuleHeader(
    unit: UnitJourney,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onShareUnitClick: (() -> Unit)? = null
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }

    val completedCount = remember(unit.lessons) {
        unit.lessons.count { it.status == LessonStatus.COMPLETED }
    }
    val totalCount = unit.lessons.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val isAllCleared = completedCount >= totalCount && totalCount > 0

    val bannerBackground = if (isDark) Color(0xFF131C31).copy(alpha = 0.85f) else Color(0xFFF1F5F9).copy(alpha = 0.90f)
    val bannerBorder = if (isAllCleared) SuccessGreen.copy(alpha = 0.40f) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = bannerBackground,
        border = BorderStroke(1.2.dp, bannerBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Module Badge e.g. "MODULE 01"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAllCleared) SuccessGreen else PurpleAccent,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "MODULE 0${unit.unitNumber}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val formattedModuleTitle = remember(unit.unitNumber, unit.title) {
                        val raw = unit.title.trim()
                        if (raw.startsWith("Module", ignoreCase = true) || raw.startsWith("Unit", ignoreCase = true)) {
                            raw
                        } else {
                            "Module ${unit.unitNumber}: $raw"
                        }
                    }

                    Text(
                        text = formattedModuleTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onShareUnitClick != null) {
                        IconButton(
                            onClick = onShareUnitClick,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Module",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (unit.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (unit.isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar & Cleared Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = if (isAllCleared) SuccessGreen else PurpleAccent,
                    trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (isAllCleared) "All Completed ✓" else "$completedCount/$totalCount Completed",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAllCleared) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
