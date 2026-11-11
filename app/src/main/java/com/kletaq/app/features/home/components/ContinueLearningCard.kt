package com.kletaq.app.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.R
import com.kletaq.app.core.theme.TealMintBackground
import com.kletaq.app.core.theme.TealPrimary
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.isAppInDarkTheme

@Composable
fun ContinueLearningCard(
    subjectTitle: String = "",
    topicTitle: String = "",
    progressPercentage: Float = 0f,
    headerTag: String = "+ CURRENT QUEST",
    progressText: String? = null,
    subText: String? = null,
    buttonText: String = "Resume Lesson",
    isGamified: Boolean = false,
    xpReward: Int = 0,
    notesCount: Int = 0,
    flashcardsCount: Int = 0,
    onContinueClick: () -> Unit = {},
    onFocusClick: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme

    val cardBg = if (isDark) Color(0xFF161922) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF14B8A6) else TealPrimary

    val displayProgress = progressPercentage.coerceIn(0f, 1f)
    val displayPercentText = progressText ?: "${(displayProgress * 100).toInt()}% Done"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Tags Row: "+ CURRENT QUEST" (Mint) & "6% Done" (Purple)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF0F322E) else TealMintBackground,
                    border = BorderStroke(0.75.dp, if (isDark) Color(0xFF14B8A6) else Color(0xFFB7E4DC))
                ) {
                    Text(
                        text = headerTag,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF5EEAD4) else TealPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF281E48) else Color(0xFFEDE9FE),
                    border = BorderStroke(0.75.dp, if (isDark) Color(0xFF7C3AED) else Color(0xFFDDD6FE))
                ) {
                    Text(
                        text = displayPercentText,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9)
                    )
                }
            }

            // 2. Subject Title
            Text(
                text = subjectTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 3. Topic Title & 3D Isometric Book SVG
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = topicTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(10.dp))

                // The 3D Book SVG with sparkles and code icon
                Image(
                    painter = painterResource(id = R.drawable.ic_3d_book),
                    contentDescription = "Current Quest Book",
                    modifier = Modifier.size(72.dp)
                )
            }

            // 4. Slim Teal Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0xFF22283A) else Color(0xFFE5E7EB))
            ) {
                if (displayProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(displayProgress.coerceAtLeast(0.04f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDark) Color(0xFF2DD4BF) else TealPrimary)
                    )
                }
            }

            // 5. Reward & Resources Chips Row (only show chips with real non-zero data)
            val hasAnyChips = xpReward > 0 || notesCount > 0 || flashcardsCount > 0
            if (hasAnyChips) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // XP Reward Chip
                    if (xpReward > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF2B1D08) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "+$xpReward XP reward",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Notes Chip
                    if (notesCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF222736) else Color(0xFFF3F4F6)
                        ) {
                            Text(
                                text = "$notesCount ${if (notesCount == 1) "note" else "notes"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Flashcards Chip
                    if (flashcardsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF222736) else Color(0xFFF3F4F6)
                        ) {
                            Text(
                                text = "$flashcardsCount ${if (flashcardsCount == 1) "flashcard" else "flashcards"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 6. Action Buttons Row: Teal "Resume Lesson ➔" + Focus Timer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onContinueClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }

                // Focus Timer Square Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF22283A) else Color(0xFFF3F4F6),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF333B50) else Color(0xFFE5E7EB)),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = onFocusClick) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Start Focus Timer",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
