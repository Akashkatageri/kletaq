package com.kletaq.app.features.profile

import com.kletaq.app.core.theme.BorderColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.kletaq.app.features.profile.components.TrophyHallBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.features.progress.ProgressViewModel
import com.kletaq.app.features.progress.components.GitHubContributionHeatmap
import com.kletaq.app.features.progress.components.WeeklyStudyChart
import com.kletaq.app.features.progress.components.XpHistoryGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    progressViewModel: ProgressViewModel = hiltViewModel(),
    onNavigateToCreateNote: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val userStats by progressViewModel.userStats.collectAsState()
    val achievements by progressViewModel.achievements.collectAsState()
    val consistencyScore by progressViewModel.consistencyScore.collectAsState()
    val heatmapTiles by progressViewModel.heatmapTiles.collectAsState()
    val weeklyBars by progressViewModel.weeklyBars.collectAsState()
    val xpPoints by progressViewModel.xpTrendPoints.collectAsState()

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var username by remember { mutableStateOf("Student") }
    var selectedBranch by remember { mutableStateOf("ISE") }
    var selectedScheme by remember { mutableStateOf("2025 Scheme") }
    var selectedSemester by remember { mutableStateOf("Semester 1") }
    var showTrophyHallSheet by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var listenerRegistration: ListenerRegistration? = null
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            listenerRegistration = db.collection("users").document(currentUser.uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val prof = snapshot.toObject(UserProfile::class.java)
                    if (prof != null) {
                        userProfile = prof
                        if (prof.username.isNotBlank()) username = prof.username
                        else if (!currentUser.displayName.isNullOrBlank()) username = currentUser.displayName!!
                        if (prof.branch.isNotBlank()) selectedBranch = prof.branch
                        if (prof.scheme.isNotBlank()) selectedScheme = prof.scheme
                        if (prof.semester > 0) selectedSemester = "Semester ${prof.semester}"
                    }
                }
            }
        }
        onDispose {
            listenerRegistration?.remove()
        }
    }

    val unlockedCount = remember(achievements) { achievements.count { it.unlocked } }
    val xpInCurrentLevel = userStats.currentLevelXp
    val xpNeededForNextLevel = userStats.nextLevelXp
    val xpPercent = (userStats.currentLevelXp.toFloat() / userStats.nextLevelXp.toFloat()).coerceIn(0f, 1f)

    val activeDays = heatmapTiles.count { it.intensityLevel > 0 }

    // Study Activity tab state
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("Heatmap", "Weekly", "XP Trend")

    if (showTrophyHallSheet) {
        TrophyHallBottomSheet(
            achievements = achievements,
            onDismiss = { showTrophyHallSheet = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ─── 1. HEADER ───
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = CardSurface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(34.dp)
                ) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ─── 2. PROFILE HERO CARD (COMPACT) ───
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.HeavyShape,
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = InkPaperBorder.heavyBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Compact Avatar with level badge overlay
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PurpleAccent.copy(alpha = 0.10f),
                            border = BorderStroke(1.5.dp, PurpleAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "👨‍💻", fontSize = 26.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PurpleAccent,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "Lvl ${userStats.currentLevel}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    // Content Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Username + Consistency badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PurpleAccent.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "🎯 $consistencyScore% · ${userStats.totalStudyHours.toInt()}h",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Academic Subtitle
                        Text(
                            text = "${userProfile?.university ?: "VTU"} · $selectedBranch · ${selectedSemester.replace("Semester ", "Sem ")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        // Compact XP Progress Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Level ${userStats.currentLevel} → ${userStats.currentLevel + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "$xpInCurrentLevel / $xpNeededForNextLevel XP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PurpleAccent
                                )
                            }

                            LinearProgressIndicator(
                                progress = { xpPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PurpleAccent,
                                trackColor = PurpleAccent.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }

        // ─── 3. QUICK STATS ROW ───
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoStatCard(emoji = "⚡", title = "TOTAL XP", value = "${userStats.totalXp}", accentColor = com.kletaq.app.core.theme.XpAmber, modifier = Modifier.weight(1f))
                BentoStatCard(emoji = "🔥", title = "STREAK", value = "${userStats.studyStreak}d", accentColor = com.kletaq.app.core.theme.StreakOrange, modifier = Modifier.weight(1f))
                BentoStatCard(emoji = "📖", title = "TOPICS", value = "${userStats.totalTopicsCompleted}", accentColor = com.kletaq.app.core.theme.PrimaryAccentColor, modifier = Modifier.weight(1f))
                BentoStatCard(emoji = "🏆", title = "BADGES", value = "$unlockedCount/${achievements.size}", accentColor = com.kletaq.app.core.theme.SuccessGreen, modifier = Modifier.weight(1f))
            }
        }

        // ─── 4. ACHIEVEMENTS SECTION ───
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 Achievements ($unlockedCount/${achievements.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Text(
                        text = "See all →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        modifier = Modifier.clickable { showTrophyHallSheet = true }
                    )
                }

                // 2×2 Achievement Grid
                val displayAchievements = achievements.take(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rowIdx in 0 until 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIdx in 0 until 2) {
                                val achIdx = rowIdx * 2 + colIdx
                                if (achIdx < displayAchievements.size) {
                                    val ach = displayAchievements[achIdx]
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (ach.unlocked) PurpleAccent.copy(alpha = 0.06f) else CardSurface
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (ach.unlocked) PurpleAccent.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = ach.iconEmoji, fontSize = 24.sp)
                                                Column {
                                                    Text(
                                                        text = ach.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (ach.unlocked) TextPrimary else TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (ach.unlocked) "✓ Unlocked" else "${ach.progress}/${ach.target}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (ach.unlocked) PurpleAccent else TextSecondary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            // Progress bar for locked achievements
                                            if (!ach.unlocked && ach.target > 0) {
                                                LinearProgressIndicator(
                                                    progress = { (ach.progress.toFloat() / ach.target).coerceIn(0f, 1f) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = PurpleAccent.copy(alpha = 0.6f),
                                                    trackColor = PurpleAccent.copy(alpha = 0.10f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── 5. STUDY ACTIVITY (TABBED) ───
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tab header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 Study Activity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // Tab selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        val isSelected = selectedTabIndex == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurpleAccent else PurpleAccent.copy(alpha = 0.08f),
                            border = if (isSelected) null else BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTabIndex = index }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                // Tab content — show the selected chart component
                AnimatedVisibility(
                    visible = selectedTabIndex == 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GitHubContributionHeatmap(
                        tiles = heatmapTiles,
                        totalStudyHours = userStats.totalStudyHours,
                        activeDays = activeDays
                    )
                }

                AnimatedVisibility(
                    visible = selectedTabIndex == 1,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    WeeklyStudyChart(bars = weeklyBars)
                }

                AnimatedVisibility(
                    visible = selectedTabIndex == 2,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    XpHistoryGraph(points = xpPoints)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun BentoStatCard(emoji: String, title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor.copy(alpha = 0.04f))
                .padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Text(text = title, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
