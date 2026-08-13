package com.kletaq.app.features.lesson.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.config.AdaptiveEngineConfig
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.utils.SpacedRepetitionEngine
import com.kletaq.app.data.model.MemoryCard
import com.kletaq.app.data.model.TopicDifficulty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCompletionFeedbackSheet(
    topicTitle: String = "Recursion & Memoization",
    config: AdaptiveEngineConfig = remember { AdaptiveEngineConfig() },
    onDismiss: () -> Unit,
    onSubmitFeedback: (TopicDifficulty, Int, MemoryCard) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedDifficulty by remember { mutableStateOf(TopicDifficulty.MEDIUM) }
    var selectedConfidence by remember { mutableIntStateOf(3) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Data-driven calculation of predicted review schedule & retention
    val initialIntervalDays = remember(selectedDifficulty, selectedConfidence) {
        SpacedRepetitionEngine.computeInitialIntervalDays(selectedDifficulty, selectedConfidence, config)
    }

    val predictedReviewDateStr = remember(initialIntervalDays) {
        val futureDate = Date(System.currentTimeMillis() + (initialIntervalDays * 86400000L))
        SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.US).format(futureDate)
    }

    val estimatedRetention = remember(initialIntervalDays) {
        SpacedRepetitionEngine.estimateMemoryRetentionPercentage(0, initialIntervalDays)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✓ Lesson Complete!",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = topicTitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // ── 1. Difficulty Question ─────────────────────────────────────────
            Text(
                text = "How difficult was this topic?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    TopicDifficulty.EASY to "😊 Easy",
                    TopicDifficulty.MEDIUM to "😐 Medium",
                    TopicDifficulty.HARD to "😵 Hard"
                ).forEach { (diff, label) ->
                    val isSelected = selectedDifficulty == diff
                    val unselectedBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedDifficulty = diff },
                        color = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else unselectedBg,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PurpleAccent) else null
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 2. Confidence Rating (1..5 Stars) ─────────────────────────────
            Text(
                text = "How confident are you for an exam tomorrow?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { starIndex ->
                    val isFilled = starIndex <= selectedConfidence
                    Icon(
                        imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = "$starIndex Stars",
                        tint = if (isFilled) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { selectedConfidence = starIndex }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. Real-Time Feedback Card (Predicted Review Date & Retention %) ───
            val cardBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            val cardBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "📅 First review scheduled:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (initialIntervalDays == 0) "Today" else predictedReviewDateStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "🧠 Estimated retention:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$estimatedRetention%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    val card = SpacedRepetitionEngine.createInitialMemoryCard(
                        topicId = topicTitle.lowercase().replace(" ", "_"),
                        topicName = topicTitle,
                        difficulty = selectedDifficulty,
                        confidence = selectedConfidence,
                        config = config
                    )
                    onSubmitFeedback(selectedDifficulty, selectedConfidence, card)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save Memory Card & Continue",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
