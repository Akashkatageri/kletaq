package com.kletaq.app.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.Revision
import com.kletaq.app.data.model.ReviewRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionSheet(
    dueRevisions: List<Revision>,
    onRecordReview: (topicId: String, rating: ReviewRating) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentIndex by remember { mutableIntStateOf(0) }
    var completedCount by remember { mutableIntStateOf(0) }

    val currentRevision = dueRevisions.getOrNull(currentIndex)
    val isFinished = currentRevision == null || dueRevisions.isEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🧠", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPACED REPETITION REVIEW",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = PurpleAccent,
                        letterSpacing = 0.5.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            if (!isFinished && currentRevision != null) {
                // Progress Counter
                Text(
                    text = "Review ${currentIndex + 1} of ${dueRevisions.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                // Revision Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E163B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2F216E))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurpleAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = currentRevision.subjectName.ifBlank { "Subject" },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        }

                        Text(
                            text = currentRevision.topicName.ifBlank { "Study Topic" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF374151)
                            ) {
                                Text(
                                    text = "Difficulty: ${currentRevision.learningDifficulty.uppercase()}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Reps: ${currentRevision.repetitions}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = "How well did you recall this topic?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                // Rating Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingButton(
                        modifier = Modifier.weight(1f),
                        emoji = "🔴",
                        label = "Forgot",
                        color = Color(0xFFEF4444),
                        onClick = {
                            onRecordReview(currentRevision.topicId, ReviewRating.FORGOT)
                            completedCount++
                            currentIndex++
                        }
                    )
                    RatingButton(
                        modifier = Modifier.weight(1f),
                        emoji = "🟠",
                        label = "Hard",
                        color = Color(0xFFF59E0B),
                        onClick = {
                            onRecordReview(currentRevision.topicId, ReviewRating.HARD)
                            completedCount++
                            currentIndex++
                        }
                    )
                    RatingButton(
                        modifier = Modifier.weight(1f),
                        emoji = "🟢",
                        label = "Good",
                        color = Color(0xFF10B981),
                        onClick = {
                            onRecordReview(currentRevision.topicId, ReviewRating.GOOD)
                            completedCount++
                            currentIndex++
                        }
                    )
                    RatingButton(
                        modifier = Modifier.weight(1f),
                        emoji = "⚡",
                        label = "Easy",
                        color = Color(0xFF8B5CF6),
                        onClick = {
                            onRecordReview(currentRevision.topicId, ReviewRating.EASY)
                            completedCount++
                            currentIndex++
                        }
                    )
                }
            } else {
                // Completion Celebration State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = "Review Session Completed!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = if (completedCount > 0)
                            "Great job! You completed $completedCount reviews and earned +${completedCount * 15} XP."
                        else
                            "No reviews due right now! All your topics are up to date.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
