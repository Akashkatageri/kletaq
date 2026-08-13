package com.kletaq.app.features.focus.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kletaq.app.core.theme.PurpleAccent

@Composable
fun SessionCompleteDialog(
    minutesStudied: Int = 0,
    xpEarned: Int = 0,
    streakDays: Int = 0,
    isStreakMaintained: Boolean = true,
    onContinueClick: () -> Unit
) {
    Dialog(onDismissRequest = onContinueClick) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = if (minutesStudied >= 1) "🎉" else "⏱️", fontSize = 48.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (minutesStudied >= 1) "Session Complete" else "Session Ended",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$minutesStudied minutes studied",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (minutesStudied >= 1 && (xpEarned > 0 || isStreakMaintained)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Rewards Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (xpEarned > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PurpleAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "+$xpEarned XP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (isStreakMaintained) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFF9800).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔥", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Streak maintained",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onContinueClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
