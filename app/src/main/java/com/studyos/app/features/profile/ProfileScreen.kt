package com.studyos.app.features.profile

import com.studyos.app.core.theme.BorderColor
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyos.app.core.theme.CardSurface
import com.studyos.app.core.theme.InkPaperBorder
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.core.theme.TextPrimary
import com.studyos.app.core.theme.TextSecondary
import com.studyos.app.data.model.UserProfile
import com.studyos.app.features.progress.ProgressViewModel
import com.studyos.app.features.progress.components.GitHubContributionHeatmap
import com.studyos.app.features.progress.components.WeeklyStudyChart
import com.studyos.app.features.progress.components.XpHistoryGraph

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

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid).addSnapshotListener { snapshot, _ ->
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
    }

    val unlockedCount = remember(achievements) { achievements.count { it.unlocked } }
    val xpInCurrentLevel = userStats.currentLevelXp
    val xpNeededForNextLevel = userStats.nextLevelXp
    val xpPercent = (userStats.currentLevelXp.toFloat() / userStats.nextLevelXp.toFloat()).coerceIn(0f, 1f)

    val activeDays = heatmapTiles.count { it.intensityLevel > 0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- 1. TOP HEADER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile & Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = CardSurface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // --- 2. PLAYER CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = InkPaperBorder.HeavyShape,
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = InkPaperBorder.heavyBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = PurpleAccent.copy(alpha = 0.12f),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "👨‍💻", fontSize = 32.sp)
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = PurpleAccent
                            ) {
                                Text(
                                    text = "Lvl ${userStats.currentLevel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Consistency Score: $consistencyScore% • ${userStats.totalStudyHours.toInt()}h Total",
                                fontSize = 11.sp,
                                color = PurpleAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // XP Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LEVEL XP PROGRESS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "$xpInCurrentLevel / $xpNeededForNextLevel XP (${(xpPercent * 100).toInt()}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent
                            )
                        }

                        LinearProgressIndicator(
                            progress = { xpPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = PurpleAccent,
                            trackColor = PurpleAccent.copy(alpha = 0.12f)
                        )
                    }

                    // Academic Info Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AcademicInfoPill(label = "UNI", value = userProfile?.university ?: "VTU", modifier = Modifier.weight(1f))
                        AcademicInfoPill(label = "BRANCH", value = selectedBranch, modifier = Modifier.weight(1f))
                        AcademicInfoPill(label = "SEM", value = selectedSemester.replace("Semester ", "Sem "), modifier = Modifier.weight(1f))
                        AcademicInfoPill(label = "SCHEME", value = selectedScheme, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // --- 3. BENTO STAT CARDS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoStatCard(emoji = "⚡", title = "TOTAL XP", value = "${userStats.totalXp}", accentColor = com.studyos.app.core.theme.XpAmber, modifier = Modifier.weight(1f))
                BentoStatCard(emoji = "🔥", title = "STREAK", value = "${userStats.studyStreak}d", accentColor = com.studyos.app.core.theme.StreakOrange, modifier = Modifier.weight(1f))
                BentoStatCard(emoji = "📖", title = "TOPICS", value = "${userStats.totalTopicsCompleted}", accentColor = com.studyos.app.core.theme.PrimaryAccentColor, modifier = Modifier.weight(1f))
                BentoStatCard(emoji = "🏆", title = "BADGES", value = "$unlockedCount/${achievements.size}", accentColor = com.studyos.app.core.theme.SuccessGreen, modifier = Modifier.weight(1f))
            }
        }

        // --- 4. GITHUB CONTRIBUTION HEATMAP ---
        item {
            GitHubContributionHeatmap(
                tiles = heatmapTiles,
                totalStudyHours = userStats.totalStudyHours,
                activeDays = activeDays
            )
        }

        // --- 5. WEEKLY FOCUS CHART ---
        item {
            WeeklyStudyChart(bars = weeklyBars)
        }

        // --- 6. XP TREND GRAPH ---
        item {
            XpHistoryGraph(points = xpPoints)
        }

        // --- 7. ACHIEVEMENTS SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 ACHIEVEMENTS ($unlockedCount/${achievements.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurpleAccent
                    )

                    Text(
                        text = "See all →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        modifier = Modifier.clickable { showTrophyHallSheet = true }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    achievements.take(3).forEach { ach ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, if (ach.unlocked) PurpleAccent.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = ach.iconEmoji, fontSize = 24.sp)
                                Text(
                                    text = ach.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ach.unlocked) TextPrimary else TextSecondary
                                )
                                Text(
                                    text = if (ach.unlocked) "Unlocked" else "${ach.progress}/${ach.target}",
                                    fontSize = 10.sp,
                                    color = if (ach.unlocked) PurpleAccent else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AcademicInfoPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary)
            Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
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
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Text(text = title, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
    }
}
