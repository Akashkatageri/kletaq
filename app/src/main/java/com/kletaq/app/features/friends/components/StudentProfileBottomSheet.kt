package com.kletaq.app.features.friends.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.FriendshipStatus
import com.kletaq.app.data.model.UserPublicProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileBottomSheet(
    profile: UserPublicProfile,
    isLoading: Boolean = false,
    isSendingRequest: Boolean = false,
    onDismiss: () -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onRejectFriendRequest: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optional progress bar while fetching deep data
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = PurpleAccent,
                    trackColor = PurpleAccent.copy(alpha = 0.15f)
                )
            }

            // 1. HERO PROFILE HEADER
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.HeavyShape,
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = InkPaperBorder.heavyBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar with Level Badge Overlay
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleAccent.copy(alpha = 0.12f),
                            border = BorderStroke(2.dp, PurpleAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (profile.photoUrl.isNotBlank()) "🎓" else "👨‍🎓",
                                    fontSize = 34.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurpleAccent,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "Lvl ${profile.stats.currentLevel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Username
                    Text(
                        text = profile.username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Academic Subtitle & University
                    val academicText = buildString {
                        if (profile.branch.isNotBlank()) {
                            append(profile.branch)
                        } else {
                            append("Engineering")
                        }
                        if (profile.semester > 0) {
                            append(" · Sem ${profile.semester}")
                        }
                        append(" · ${profile.university}")
                    }

                    Text(
                        text = academicText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    // Leaderboard Rank Pill if applicable
                    if (profile.rank > 0) {
                        val rankColor = when (profile.rank) {
                            1 -> Color(0xFFD97706) // Gold
                            2 -> Color(0xFF64748B) // Silver
                            3 -> Color(0xFFB45309) // Bronze
                            else -> PurpleAccent
                        }
                        val rankIcon = when (profile.rank) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "🏆"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = rankColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, rankColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$rankIcon Rank #${profile.rank} on Leaderboard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = rankColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. RELATIONSHIP CTA / ACTION BUTTON
                    when (profile.friendshipStatus) {
                        FriendshipStatus.SELF -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = "👤 Your Profile",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        FriendshipStatus.FRIENDS -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Friends with ${profile.username}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        FriendshipStatus.REQUEST_SENT -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PurpleAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "⏳", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Friend Request Pending",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                }
                            }
                        }

                        FriendshipStatus.REQUEST_RECEIVED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onAcceptFriendRequest(profile.uid) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onRejectFriendRequest(profile.uid) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Decline", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        FriendshipStatus.NOT_FRIENDS -> {
                            Button(
                                onClick = { onSendFriendRequest(profile.uid) },
                                enabled = !isSendingRequest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PurpleAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isSendingRequest) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sending Request...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Send Friend Request",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. ALL PROGRESS & STATS TITLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 ALL STUDY PROGRESS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurpleAccent,
                    letterSpacing = 1.sp
                )
            }

            // 4. STATS 2x2 GRID
            val stats = profile.stats
            val hours = stats.totalStudyMinutes / 60
            val minutes = stats.totalStudyMinutes % 60
            val timeString = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Streak
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "🔥",
                    title = "${stats.studyStreak} Days",
                    label = "Study Streak",
                    subtext = "Longest: ${stats.longestStreak}d"
                )

                // Stat 2: Total XP
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "⚡",
                    title = "${stats.totalXp} XP",
                    label = "Total Points",
                    subtext = "Lvl ${stats.currentLevel}"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 3: Study Time
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "⏱",
                    title = timeString,
                    label = "Total Study Time",
                    subtext = "${stats.studySessions} sessions"
                )

                // Stat 4: Topics Completed
                StatTile(
                    modifier = Modifier.weight(1f),
                    emoji = "📚",
                    title = "${stats.totalTopicsCompleted}",
                    label = "Topics Mastered",
                    subtext = "${stats.totalModulesCompleted} modules"
                )
            }

            // 5. WHY I STUDY / MOTIVATION (IF AVAILABLE)
            if (profile.studyWhy.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.MediumShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 STUDY GOAL & WHY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "\"${profile.studyWhy}\"",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Close button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Text(text = "Close", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    label: String,
    subtext: String
) {
    Card(
        modifier = modifier,
        shape = InkPaperBorder.MediumShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = InkPaperBorder.heavyBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 20.sp)
                Text(
                    text = subtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
