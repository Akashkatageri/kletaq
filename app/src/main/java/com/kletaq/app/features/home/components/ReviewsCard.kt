package com.kletaq.app.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.R
import com.kletaq.app.core.theme.TealMintBackground
import com.kletaq.app.core.theme.TealMintBorder
import com.kletaq.app.core.theme.TealPrimary
import com.kletaq.app.core.theme.StreakOrangeAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.isAppInDarkTheme

@Composable
fun ReviewsCard(
    dueReviewsCount: Int = 3,
    nextReviewTopic: String = "Recursion & Backtracking",
    onStartReviewClick: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme
    val hasDue = dueReviewsCount > 0

    val cardBg = if (isDark) Color(0xFF161922) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF282D3D) else Color(0xFFE5E7EB)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // Header: Brain Icon, Title, Due Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF0F322E) else TealMintBackground,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_3d_brain),
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "SPACED REPETITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TealPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                if (hasDue) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2C160B) else Color(0xFFFFF0E6),
                        border = BorderStroke(0.75.dp, if (isDark) Color(0xFF9A3412) else Color(0xFFFFD8BF))
                    ) {
                        Text(
                            text = "$dueReviewsCount Due",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StreakOrangeAccent
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF0F322E) else Color(0xFFECFDF5),
                        border = BorderStroke(0.75.dp, if (isDark) Color(0xFF14B8A6) else Color(0xFFA7F3D0))
                    ) {
                        Text(
                            text = "All Caught Up 🎉",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            // Next Review Topic Info
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (hasDue) "Next review topic:" else "Status:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = if (hasDue) nextReviewTopic else "Memory retention is optimal! No flashcards due.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mystery Review Card Boxes (shown if there are pending reviews)
            if (hasDue) {
                val boxCount = minOf(dueReviewsCount, 4).coerceAtLeast(3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(boxCount) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF0F322E) else TealMintBackground,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF14B8A6) else TealMintBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TealPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Primary Teal Action Button
            Button(
                onClick = onStartReviewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
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
                        text = if (hasDue) "Start Spaced Review ($dueReviewsCount topics)" else "Review Library",
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
        }
    }
}
