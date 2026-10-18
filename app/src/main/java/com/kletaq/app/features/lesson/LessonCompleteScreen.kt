package com.kletaq.app.features.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.core.config.AdaptiveEngineConfig
import com.kletaq.app.core.sound.SoundEffectType
import com.kletaq.app.core.sound.SoundEffectsManager
import com.kletaq.app.core.utils.SpacedRepetitionEngine
import com.kletaq.app.data.model.TopicDifficulty
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import com.kletaq.app.features.lesson.components.CelebrationConfetti
import com.kletaq.app.features.lesson.components.PandaCelebrationIllustration
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LessonCompleteScreen(
    xpEarnedAmount: Int = 80,
    topicId: String = "",
    lessonTitle: String = "Recursion & Memoization",
    showFeedbackOnContinue: Boolean = true,
    onContinueClick: () -> Unit = {},
    onReviewClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    var userStats by remember { mutableStateOf(UserStats()) }

    var isVisible by remember { mutableStateOf(false) }
    var targetXp by remember { mutableIntStateOf(0) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Inline Spaced Repetition Retention State
    var selectedDifficulty by remember { mutableStateOf(TopicDifficulty.MEDIUM) }
    var selectedConfidence by remember { mutableIntStateOf(4) }
    val config = remember { AdaptiveEngineConfig() }

    val initialIntervalDays = remember(selectedDifficulty, selectedConfidence) {
        SpacedRepetitionEngine.computeInitialIntervalDays(selectedDifficulty, selectedConfidence, config)
    }

    val nextReviewDateFormatted = remember(initialIntervalDays) {
        val futureDate = Date(System.currentTimeMillis() + (initialIntervalDays * 86400000L))
        SimpleDateFormat("EEE, MMM d", Locale.US).format(futureDate)
    }

    // Sound Manager lifecycle
    val soundManager = remember(context) {
        SoundEffectsManager(context).apply { initialize() }
    }
    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    // Dynamic Panda Motivational Message Pool
    val pandaMessages = remember {
        listOf(
            "You're smarter than yesterday! 🐼",
            "One step closer to semester mastery! 🌟",
            "Consistency today, Success tomorrow! 🔥",
            "Great work, scholar! The panda is proud! 📚",
            "Knowledge locked in! Keep the momentum! ⚡"
        )
    }
    val randomMessage = remember { pandaMessages.random() }

    // Count-up animation for XP
    val animatedXp by animateIntAsState(
        targetValue = targetXp,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "XpCountUp"
    )

    // Animated Level Progress Bar
    val targetProgress = remember(userStats) {
        val totalNeeded = userStats.nextLevelXp.toFloat().coerceAtLeast(1f)
        (userStats.currentLevelXp.toFloat() / totalNeeded).coerceIn(0.05f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (isVisible) targetProgress else 0.05f,
        animationSpec = tween(durationMillis = 1300, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "LevelProgressBar"
    )

    LaunchedEffect(Unit) {
        isVisible = true
        delay(150)
        soundManager.playSound(SoundEffectType.XP_GAIN)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(150)
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

    fun handleClaimXp() {
        soundManager.playSound(SoundEffectType.BUTTON_CLICK)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Automatically persist the Spaced Repetition MemoryCard to Firestore
        if (currentUser != null) {
            val effectiveTopicId = topicId.ifBlank {
                lessonTitle.lowercase().replace("/", "_").replace(" ", "_")
            }
            val memoryCard = SpacedRepetitionEngine.createInitialMemoryCard(
                topicId = effectiveTopicId,
                topicName = lessonTitle,
                difficulty = selectedDifficulty,
                confidence = selectedConfidence,
                config = config
            )

            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(currentUser.uid)
                    .collection("memory_cards").document(effectiveTopicId)
                    .set(memoryCard, SetOptions.merge())
            } catch (e: Exception) {
                android.util.Log.e("LessonCompleteScreen", "Failed to save memory card: ${e.message}")
            }
        }

        onContinueClick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) Color(0xFF0F1117) else Color(0xFFF8FAFC)
            )
    ) {
        // Fullscreen Celebratory Confetti Burst
        CelebrationConfetti(
            particleCount = 70,
            durationMillis = 2800
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── 1. 3D Panda Celebration Mascot (Compact) ────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { -30 })
            ) {
                PandaCelebrationIllustration(
                    modifier = Modifier
                        .width(140.dp)
                        .height(120.dp)
                )
            }

            // ── 2. Header & Dynamic Motivational Subtitle (Smaller) ──────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 100))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "🎉 Lesson Complete!",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = lessonTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Text(
                        text = randomMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── 3. XP Earned & Level Progression Hero Card (Compact Bento) ───
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(700, delayMillis = 180)) + slideInVertically(initialOffsetY = { 25 })
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF171A23) else Color(0xFFFFFFFF)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.4f) else Color(0xFFDDD6FE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Row: XP Earned Badge & Current Level Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // XP Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE),
                                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(text = "⚡", fontSize = 13.sp)
                                    Text(
                                        text = "+$animatedXp XP",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)
                                    )
                                }
                            }

                            // Current Level Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(text = "⭐", fontSize = 13.sp)
                                    Text(
                                        text = "Level ${userStats.currentLevel}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        // Compact Level Progress Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Level Progress",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${userStats.currentLevelXp} / ${userStats.nextLevelXp} XP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF8B5CF6),
                                trackColor = if (isDark) Color(0xFF282C3A) else Color(0xFFE2E8F0),
                                strokeCap = StrokeCap.Round
                            )

                            val xpRemaining = (userStats.nextLevelXp - userStats.currentLevelXp).coerceAtLeast(0L)
                            Text(
                                text = if (xpRemaining > 0) "$xpRemaining XP to Level ${userStats.currentLevel + 1}! 🚀" else "Ready to level up! 🌟",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // ── 4. Two Compact Stat Cards (Streak & Shields Bento) ────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 240)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Streak (Fire / Emerald)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF13231B) else Color(0xFFF0FDF4)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFF86EFAC)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🔥", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "${userStats.effectiveStreak} Days",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A)
                                )
                                Text(
                                    text = "Streak active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Card 2: Shields Protected
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF162032) else Color(0xFFEFF6FF)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFF93C5FD)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🛡️", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "${userStats.shieldsRemaining} Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    text = "Shields kept",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── 5. Inline Spaced Repetition Retention Card (Visible Star Rating) ─
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(850, delayMillis = 300))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF171A23) else Color(0xFFFFFFFF)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🧠", fontSize = 15.sp)
                            Text(
                                text = "How did this lesson feel?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Difficulty Pill Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(TopicDifficulty.EASY, "🟢 Easy", "7d"),
                                Triple(TopicDifficulty.MEDIUM, "🟡 Good", "3d"),
                                Triple(TopicDifficulty.HARD, "🔴 Hard", "1d")
                            ).forEach { (diff, label, interval) ->
                                val isSelected = selectedDifficulty == diff
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedDifficulty = diff
                                            soundManager.playSound(SoundEffectType.BUTTON_CLICK)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)
                                    } else {
                                        Color.Transparent
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF3B82F6) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Review in $interval",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // ── Enhanced & Highly Visible 5-Star Confidence Rating ──
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF222634) else Color(0xFFFFFBEB),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFFF59E0B).copy(alpha = 0.35f) else Color(0xFFFDE68A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Confidence Level:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val confidenceLabel = when (selectedConfidence) {
                                        1 -> "Needs Work 🔴"
                                        2 -> "Fair 🟠"
                                        3 -> "Good 🟡"
                                        4 -> "Strong 🟢"
                                        else -> "Mastered! 🌟"
                                    }
                                    Text(
                                        text = confidenceLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selectedConfidence >= 4) Color(0xFF16A34A) else Color(0xFFD97706)
                                    )
                                }

                                // Prominent 30.dp Stars with clear touch feedback
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (star in 1..5) {
                                        val isStarFilled = star <= selectedConfidence
                                        Icon(
                                            imageVector = if (isStarFilled) Icons.Filled.Star else Icons.Outlined.Star,
                                            contentDescription = "Rate $star stars",
                                            tint = if (isStarFilled) Color(0xFFFFB800) else Color(0xFF94A3B8).copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable {
                                                    selectedConfidence = star
                                                    soundManager.playSound(SoundEffectType.BUTTON_CLICK)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Next Review Preview Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E2230) else Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📅 Next review scheduled for $nextReviewDateFormatted",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── 6. Action Buttons: CLAIM XP & Review Lesson ──────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(900, delayMillis = 350))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // High-contrast, tactile CLAIM XP Button (as requested by user)
                    Button(
                        onClick = { handleClaimXp() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF84CC16), // Duolingo Lime / Energetic Accent
                            contentColor = Color(0xFF0F172A)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 5.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "CLAIM XP ⚡",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Secondary: Review Lesson Notes
                    TextButton(
                        onClick = onReviewClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Review Lesson Notes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
