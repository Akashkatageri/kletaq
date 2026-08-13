package com.kletaq.app.features.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.AchievementModel

private val RARE_COLOR = Color(0xFF3B82F6)
private val EPIC_COLOR = Color(0xFFA855F7)
private val LEGENDARY_COLOR = Color(0xFFF59E0B)
private val ULTRA_RARE_COLOR = Color(0xFFEF4444)
private val COMMON_COLOR = Color(0xFF10B981)

private fun getTierColor(tierStr: String): Color = when (tierStr.uppercase()) {
    "RARE" -> RARE_COLOR
    "EPIC" -> EPIC_COLOR
    "LEGENDARY" -> LEGENDARY_COLOR
    "ULTRA_RARE" -> ULTRA_RARE_COLOR
    else -> COMMON_COLOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrophyHallBottomSheet(
    achievements: List<AchievementModel>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val unlockedCount = remember(achievements) { achievements.count { it.unlocked } }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All", "Streaks", "Focus", "Academics", "Progression", "Productivity") }

    val filteredAchievements = remember(achievements, selectedCategory) {
        if (selectedCategory == "All") achievements
        else achievements.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏆 Trophy Hall",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$unlockedCount of ${achievements.size} Unlocked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .background(PurpleAccent.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${((unlockedCount.toFloat() / achievements.size.coerceAtLeast(1)) * 100).toInt()}% Done",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurpleAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories, key = { it }) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) PurpleAccent else PurpleAccent.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Achievement List (Optimized flat layout)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredAchievements,
                    key = { it.id },
                    contentType = { "achievement" }
                ) { ach ->
                    AchievementListItem(achievement = ach)
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun AchievementListItem(achievement: AchievementModel) {
    val progressFraction = (achievement.progress.toFloat() / achievement.target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val tierColor = getTierColor(achievement.tier)
    val borderColor = if (achievement.unlocked) PurpleAccent.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.3f)
    val iconBgColor = if (achievement.unlocked) PurpleAccent.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBgColor, shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.iconEmoji.ifBlank { "🏆" },
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Line 1: Title (left) & Unlocked/Lock Status (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.unlocked) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (achievement.unlocked) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD1FAE5), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "UNLOCKED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${achievement.progress}/${achievement.target}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Line 2: Tier Badge Pill + Description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(tierColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = achievement.tier.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tierColor
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Line 3: Progress Bar if locked
                if (!achievement.unlocked) {
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = PurpleAccent,
                        trackColor = PurpleAccent.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }
}
