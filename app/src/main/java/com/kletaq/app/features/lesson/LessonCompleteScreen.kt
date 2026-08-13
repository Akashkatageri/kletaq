package com.kletaq.app.features.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.features.lesson.components.PandaCelebrationIllustration
import kotlinx.coroutines.delay

@Composable
fun LessonCompleteScreen(
    xpEarnedAmount: Int = 80,
    lessonTitle: String = "Recursion & Memoization",
    showFeedbackOnContinue: Boolean = true,
    onContinueClick: () -> Unit = {},
    onReviewClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    var userStats by remember { mutableStateOf(UserStats()) }

    var isVisible by remember { mutableStateOf(false) }
    var targetXp by remember { mutableIntStateOf(0) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Random Panda Motivational Message Pool
    val pandaMessages = remember {
        listOf(
            "You're smarter than yesterday! 🐼",
            "One lesson closer to mastery! 🌟",
            "Keep the streak alive! 🔥",
            "Great work, scholar! 📚",
            "The panda is proud of you! 🐼"
        )
    }
    val randomMessage = remember { pandaMessages.random() }

    // Count-up animation for XP
    val animatedXp by animateIntAsState(
        targetValue = targetXp,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "XpCountUp"
    )

    LaunchedEffect(Unit) {
        isVisible = true
        delay(200)
        targetXp = xpEarnedAmount
    }

    // Live Snapshot Listener for UserStats
    DisposableEffect(currentUser) {
        var listenerRegistration: ListenerRegistration? = null
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("stats").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val stats = snapshot.toUserStatsSafe()
                        if (stats != null) {
                            userStats = stats
                        }
                    }
                }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Celebratory Panda Illustration with entrance animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { -40 })
            ) {
                PandaCelebrationIllustration(
                    modifier = Modifier
                        .width(200.dp)
                        .height(170.dp)
                )
            }

            // 2. Main Title & Dynamic Panda Subtitle
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(700, delayMillis = 150))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎉 Lesson Complete!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = randomMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Three Duolingo-style Stat Cards Row (Light/Dark Mode Adaptive)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300)) + slideInVertically(initialOffsetY = { 60 })
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: XP Earned (Purple Accent)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF131127) else Color(0xFFF3E8FF)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "XP Earned",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)
                            )

                            Text(text = "⚡", fontSize = 22.sp)

                            Text(
                                text = "+$animatedXp",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF9333EA)
                            )
                        }
                    }

                    // Card 2: Current Streak (Emerald/Orange)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0D2818) else Color(0xFFDCFCE7)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Streak",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                            )

                            Text(text = "🔥", fontSize = 22.sp)

                            Text(
                                text = "${userStats.studyStreak} d",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }

                    // Card 3: Current Level (Amber Gold)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF291E09) else Color(0xFFFEF3C7)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Level",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                            )

                            Text(text = "⭐", fontSize = 22.sp)

                            Text(
                                text = "Lvl ${userStats.currentLevel}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD97706),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 4. Motivational Banner Pill
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(900, delayMillis = 400))
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "💖", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Consistency today, ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Success tomorrow!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }

            var showFeedbackSheet by remember { mutableStateOf(false) }

            if (showFeedbackSheet) {
                com.kletaq.app.features.lesson.components.LessonCompletionFeedbackSheet(
                    topicTitle = lessonTitle,
                    onDismiss = {
                        showFeedbackSheet = false
                        onContinueClick()
                    },
                    onSubmitFeedback = { difficulty, confidence, card ->
                        if (currentUser != null) {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(currentUser.uid)
                                .collection("memory_cards").document(card.topicId)
                                .set(card, com.google.firebase.firestore.SetOptions.merge())
                        }
                        showFeedbackSheet = false
                        onContinueClick()
                    }
                )
            }

            // 5. Action Buttons (Primary CONTINUE JOURNEY & Secondary Review Lesson)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 500))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (showFeedbackOnContinue) {
                                showFeedbackSheet = true
                            } else {
                                onContinueClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF84CC16),
                            contentColor = Color(0xFF0F172A)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "CONTINUE JOURNEY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    TextButton(onClick = onReviewClick) {
                        Text(
                            text = "Review Lesson",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
