package com.kletaq.app.features.friends

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.LeaderboardEntry
import com.kletaq.app.features.friends.components.StudentProfileBottomSheet

data class LeagueInfo(
    val name: String,
    val icon: String,
    val primaryColor: Color,
    val nextLeagueName: String
)

fun getLeagueInfo(leagueName: String): LeagueInfo {
    return when (leagueName.lowercase().trim()) {
        "silver" -> LeagueInfo(
            name = "Silver League",
            icon = "🥈",
            primaryColor = Color(0xFF94A3B8),
            nextLeagueName = "Gold League"
        )
        "gold" -> LeagueInfo(
            name = "Gold League",
            icon = "🥇",
            primaryColor = Color(0xFFF59E0B),
            nextLeagueName = "Sapphire League"
        )
        "sapphire" -> LeagueInfo(
            name = "Sapphire League",
            icon = "💎",
            primaryColor = Color(0xFF3B82F6),
            nextLeagueName = "Diamond League"
        )
        "diamond" -> LeagueInfo(
            name = "Diamond League",
            icon = "👑",
            primaryColor = Color(0xFFA855F7),
            nextLeagueName = "Champions Tier"
        )
        else -> LeagueInfo(
            name = "Bronze League",
            icon = "🥉",
            primaryColor = Color(0xFFCD7F32),
            nextLeagueName = "Silver League"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) }

    val friendsList by viewModel.friends.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val globalLeaderboard by viewModel.globalLeaderboard.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLeaderboardLoading by viewModel.isLeaderboardLoading.collectAsState()
    val activeCategory by viewModel.leaderboardCategory.collectAsState()
    val weeklyCountdown by viewModel.weeklyCountdown.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val requestStates by viewModel.requestStates.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val selectedUserProfile by viewModel.selectedUserProfile.collectAsState()
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    var quickFindQuery by remember { mutableStateOf("") }
    var leaderboardScope by remember { mutableIntStateOf(0) } // 0 = League/Global, 1 = Friends

    // Determine user's active league
    val currentUserEntry = globalLeaderboard.find { it.uid == viewModel.currentUserId }
    val userLeague = currentUserEntry?.league ?: "Bronze"
    val leagueInfo = remember(userLeague) { getLeagueInfo(userLeague) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 1. HEADER SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Community",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Leagues & Classmates",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        border = InkPaperBorder.heavyBorder(),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.loadGlobalLeaderboard(activeCategory) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    com.kletaq.app.core.ui.NotificationBadgeIcon(
                        unreadCount = unreadNotificationCount,
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(40.dp),
                        iconColor = TextSecondary,
                        containerColor = CardSurface
                    )

                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        border = InkPaperBorder.heavyBorder(),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // --- 2. CAPSULE NAVIGATION TABS (DUOLINGO STYLE) ---
            CapsuleSubTabBar(
                selectedTab = activeSubTab,
                onTabSelected = { tab ->
                    activeSubTab = tab
                    if (tab == 0) viewModel.loadGlobalLeaderboard(activeCategory)
                },
                friendsCount = friendsList.size,
                requestsCount = friendRequests.size
            )

            // --- 3. SUB TAB CONTENT ---
            when (activeSubTab) {
                0 -> {
                    // --- WEEKLY LEAGUE / LEADERBOARD TAB ---
                    val sortedFriends = when (activeCategory) {
                        "weekly" -> friendsList.sortedByDescending { it.weeklyXp }
                        "xp" -> friendsList.sortedByDescending { it.totalXp }
                        else -> friendsList.sortedByDescending { it.streak }
                    }

                    val activeList = if (leaderboardScope == 0) {
                        globalLeaderboard
                    } else {
                        sortedFriends.mapIndexed { index, item -> item.copy(rank = index + 1) }
                    }

                    val currentUserRank = activeList.indexOfFirst { it.uid == viewModel.currentUserId }.let {
                        if (it >= 0) it + 1 else 0
                    }
                    val cutoffRank10Xp = if (activeList.size >= 10) activeList[9].weeklyXp else 0L

                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = if (currentUserRank > 0) 72.dp else 12.dp)
                        ) {
                            // Duolingo League Crest Banner
                            item {
                                DuolingoLeagueCrestHeader(
                                    leagueInfo = leagueInfo,
                                    countdownText = weeklyCountdown
                                )
                            }

                            // Scope & Category Filter Controls
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Scope: Global League vs. Friends
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                onClick = { leaderboardScope = 0 },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (leaderboardScope == 0) PurpleAccent else CardSurface,
                                                border = BorderStroke(1.dp, if (leaderboardScope == 0) PurpleAccent else BorderColor)
                                            ) {
                                                Text(
                                                    text = "🌍 League",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (leaderboardScope == 0) Color.White else TextPrimary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }

                                            Surface(
                                                onClick = { leaderboardScope = 1 },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (leaderboardScope == 1) PurpleAccent else CardSurface,
                                                border = BorderStroke(1.dp, if (leaderboardScope == 1) PurpleAccent else BorderColor)
                                            ) {
                                                Text(
                                                    text = "👥 Friends",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (leaderboardScope == 1) Color.White else TextPrimary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        // Category Filter Chips: Weekly XP vs Streak vs Total XP
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Surface(
                                                onClick = { viewModel.setLeaderboardCategory("weekly") },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (activeCategory == "weekly") PurpleAccent.copy(alpha = 0.15f) else CardSurface,
                                                border = BorderStroke(1.dp, if (activeCategory == "weekly") PurpleAccent else BorderColor)
                                            ) {
                                                Text(
                                                    text = "⚡ Weekly",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (activeCategory == "weekly") PurpleAccent else TextSecondary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }

                                            Surface(
                                                onClick = { viewModel.setLeaderboardCategory("streak") },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (activeCategory == "streak") PurpleAccent.copy(alpha = 0.15f) else CardSurface,
                                                border = BorderStroke(1.dp, if (activeCategory == "streak") PurpleAccent else BorderColor)
                                            ) {
                                                Text(
                                                    text = "🔥 Streak",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (activeCategory == "streak") PurpleAccent else TextSecondary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }

                                            Surface(
                                                onClick = { viewModel.setLeaderboardCategory("xp") },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (activeCategory == "xp") PurpleAccent.copy(alpha = 0.15f) else CardSurface,
                                                border = BorderStroke(1.dp, if (activeCategory == "xp") PurpleAccent else BorderColor)
                                            ) {
                                                Text(
                                                    text = "🌟 Total",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (activeCategory == "xp") PurpleAccent else TextSecondary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (isLeaderboardLoading) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PurpleAccent)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Updating league rankings...", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }

                            // Empty State
                            if (activeList.isEmpty() && !isLeaderboardLoading) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = InkPaperBorder.HeavyShape,
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        border = InkPaperBorder.heavyBorder(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (leaderboardScope == 1) "No friends ranked yet." else "League is waiting for you!",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (leaderboardScope == 1) "Invite classmates in the Friends tab to compete on the leaderboard!" else "Complete a focus session or study topic to earn Weekly XP and claim your league spot!",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                // --- PURE DUOLINGO UNIFIED LEADERBOARD LIST (NO 3D PODIUM) ---
                                val demotionThreshold = (activeList.size - 5).coerceAtLeast(11)

                                itemsIndexed(activeList, key = { _, item -> item.uid.ifBlank { item.username } }) { index, entry ->
                                    val realRank = index + 1

                                    DuolingoLeaderboardRowCard(
                                        entry = entry,
                                        rank = realRank,
                                        activeCategory = activeCategory,
                                        isCurrentUser = entry.uid == viewModel.currentUserId,
                                        onClick = { viewModel.openUserProfile(entry.uid, entry) }
                                    )

                                    // Promotion Zone Divider after Rank 10
                                    if (realRank == 10 && activeList.size > 10) {
                                        PromotionZoneDivider(nextLeagueName = leagueInfo.nextLeagueName)
                                    }

                                    // Demotion Zone Divider before bottom ranks (if large league)
                                    if (realRank == demotionThreshold && activeList.size >= 20) {
                                        DemotionZoneDivider()
                                    }
                                }
                            }
                        }

                        // --- STICKY FLOATING "MY RANK" BAR ---
                        if (currentUserRank > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                StickyUserRankBar(
                                    currentUserEntry = currentUserEntry,
                                    currentUserRank = currentUserRank,
                                    activeCategory = activeCategory,
                                    cutoffRank10Xp = cutoffRank10Xp,
                                    onClick = {
                                        currentUserEntry?.let { entry ->
                                            viewModel.openUserProfile(entry.uid, entry)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // --- FRIENDS & CLASSMATES TAB ---
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Friend Invite & Share Card
                        item {
                            FriendInviteCard(
                                username = currentUsername,
                                onCopySuccess = {
                                    Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // 2. Friend Requests Section (if pending)
                        if (friendRequests.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = InkPaperBorder.HeavyShape,
                                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                                    border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Friend Requests (${friendRequests.size})",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextPrimary
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Action required",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        friendRequests.forEach { request ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.openUserProfile(request.uid, request) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(text = "🎓", fontSize = 20.sp)
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(text = request.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                            Text(text = "🔥 ${request.streak}-day streak • Tap to view profile", fontSize = 11.sp, color = TextSecondary)
                                                        }
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        IconButton(
                                                            onClick = { viewModel.acceptFriendRequest(request.uid) },
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF10B981))
                                                        }

                                                        IconButton(
                                                            onClick = { viewModel.rejectFriendRequest(request.uid) },
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Decline", tint = Color(0xFFEF4444))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Search Field: "Find students"
                        item {
                            OutlinedTextField(
                                value = quickFindQuery,
                                onValueChange = {
                                    quickFindQuery = it
                                    viewModel.searchUsers(it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Find students by name or username...", fontSize = 13.sp) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PurpleAccent) },
                                trailingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PurpleAccent)
                                    } else if (quickFindQuery.isNotBlank()) {
                                        IconButton(onClick = {
                                            quickFindQuery = ""
                                            viewModel.searchUsers("")
                                        }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleAccent,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = CardSurface,
                                    unfocusedContainerColor = CardSurface
                                ),
                                singleLine = true
                            )
                        }

                        // 4. Search Results OR Classmates Roster
                        if (quickFindQuery.isNotBlank()) {
                            if (searchResults.isEmpty() && !isSearching) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = InkPaperBorder.HeavyShape,
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        border = InkPaperBorder.heavyBorder()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "No students found for '$quickFindQuery'",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Try searching for their exact username or share your invite link above.",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(searchResults, key = { it.uid }) { user ->
                                    val reqState = requestStates[user.uid] ?: RequestState.Idle
                                    val isSending = reqState is RequestState.Sending
                                    val isSent = reqState is RequestState.Sent
                                    val isError = reqState is RequestState.Error

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openUserProfile(user.uid, user) },
                                        shape = InkPaperBorder.HeavyShape,
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        border = InkPaperBorder.heavyBorder()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(text = "👤", fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = user.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                    Text(text = "🔥 ${user.streak}-day streak • L${user.currentLevel}", fontSize = 11.sp, color = TextSecondary)
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.sendFriendRequest(user.uid) },
                                                enabled = !isSending && !isSent,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = when {
                                                        isSent -> Color(0xFF10B981)
                                                        isError -> Color(0xFFEF4444)
                                                        else -> PurpleAccent
                                                    }
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSending) {
                                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(text = "Sending...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Icon(
                                                            imageVector = if (isSent) Icons.Default.Check else Icons.Default.PersonAdd,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = when {
                                                                isSent -> "Sent"
                                                                isError -> "Retry"
                                                                else -> "Add"
                                                            },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Classmates list
                            if (friendsList.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = InkPaperBorder.HeavyShape,
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        border = InkPaperBorder.heavyBorder(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "No classmates connected yet.",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Use the invite card above to send your link to friends, or search their name to connect and study together!",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        text = "MY CLASSMATES (${friendsList.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                }

                                items(friendsList, key = { it.uid }) { friend ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openUserProfile(friend.uid, friend) },
                                        shape = InkPaperBorder.HeavyShape,
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        border = InkPaperBorder.heavyBorder(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(contentAlignment = Alignment.BottomEnd) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = PurpleAccent.copy(alpha = 0.12f),
                                                        modifier = Modifier.size(38.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(text = "🎓", fontSize = 18.sp)
                                                        }
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = PurpleAccent
                                                    ) {
                                                        Text(
                                                            text = "L${friend.currentLevel}",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {
                                                    Text(
                                                        text = friend.username,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "⚡ ${friend.weeklyXp} weekly XP • Tap for profile",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = PurpleAccent.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "🔥 ${friend.streak}d",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PurpleAccent,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = TextSecondary.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- STUDENT PROFILE BOTTOM SHEET ---
        selectedUserProfile?.let { profile ->
            StudentProfileBottomSheet(
                profile = profile,
                isLoading = isLoadingProfile,
                isSendingRequest = requestStates[profile.uid] is RequestState.Sending,
                onDismiss = { viewModel.closeUserProfile() },
                onSendFriendRequest = { targetUid -> viewModel.sendFriendRequest(targetUid) },
                onAcceptFriendRequest = { fromUid -> viewModel.acceptFriendRequest(fromUid) },
                onRejectFriendRequest = { fromUid -> viewModel.rejectFriendRequest(fromUid) }
            )
        }

        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CapsuleSubTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    friendsCount: Int,
    requestsCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = InkPaperBorder.heavyBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(0) },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedTab == 0) PurpleAccent else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 Weekly League",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (selectedTab == 0) Color.White else TextSecondary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(1) },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedTab == 1) PurpleAccent else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 Classmates",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (selectedTab == 1) Color.White else TextSecondary
                    )
                    if (requestsCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = requestsCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuolingoLeagueCrestHeader(
    leagueInfo: LeagueInfo,
    countdownText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.5.dp, leagueInfo.primaryColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            leagueInfo.primaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = leagueInfo.primaryColor.copy(alpha = 0.2f),
                        border = BorderStroke(2.dp, leagueInfo.primaryColor),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = leagueInfo.icon, fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = leagueInfo.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (leagueInfo.name.contains("Diamond", ignoreCase = true)) {
                                "Top 10 stay in Champions Tier"
                            } else {
                                "Top 10 advance to ${leagueInfo.nextLeagueName}"
                            },
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PurpleAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⏳ $countdownText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuolingoLeaderboardRowCard(
    entry: LeaderboardEntry,
    rank: Int,
    activeCategory: String,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    val isTop3 = rank in 1..3
    val medalColor = when (rank) {
        1 -> Color(0xFFF59E0B) // Gold
        2 -> Color(0xFF94A3B8) // Silver
        3 -> Color(0xFFD97706) // Bronze
        else -> TextSecondary
    }

    val medalIcon = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> ""
    }

    val cardBorder = when {
        isCurrentUser -> BorderStroke(2.dp, PurpleAccent)
        rank == 1 -> BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
        rank == 2 -> BorderStroke(1.5.dp, Color(0xFF94A3B8).copy(alpha = 0.6f))
        rank == 3 -> BorderStroke(1.5.dp, Color(0xFFD97706).copy(alpha = 0.5f))
        else -> InkPaperBorder.heavyBorder()
    }

    val cardBg = when {
        isCurrentUser -> CardSurface
        rank == 1 -> Color(0xFFFEF3C7).copy(alpha = 0.14f)
        rank == 2 -> Color(0xFFF1F5F9).copy(alpha = 0.10f)
        rank == 3 -> Color(0xFFFEF3C7).copy(alpha = 0.06f)
        else -> CardSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = InkPaperBorder.MediumShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTop3) 2.5.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (isTop3) 12.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank label or Medal Pill
                if (isTop3) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = medalColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, medalColor.copy(alpha = 0.4f)),
                        modifier = Modifier.width(42.dp)
                    ) {
                        Text(
                            text = "$medalIcon #$rank",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = medalColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (rank <= 10) Color(0xFF10B981) else TextSecondary,
                        modifier = Modifier.width(42.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar with Level Tag
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        shape = CircleShape,
                        color = if (rank == 1) Color(0xFFF59E0B).copy(alpha = 0.15f) else PurpleAccent.copy(alpha = 0.10f),
                        border = if (isTop3) BorderStroke(1.5.dp, medalColor.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.size(if (isTop3) 40.dp else 36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (rank == 1) "👑" else "🎓",
                                fontSize = if (rank == 1) 20.sp else 18.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isTop3) medalColor else PurpleAccent
                    ) {
                        Text(
                            text = "L${entry.currentLevel}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PurpleAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "You",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PurpleAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (rank <= 10) "🔥 Promotion Zone • Tap to view" else "Tap to view profile",
                        fontSize = 10.sp,
                        color = if (rank <= 10) Color(0xFF10B981) else TextSecondary
                    )
                }
            }

            // Metric Chip
            val statLabel = when (activeCategory) {
                "weekly" -> "⚡ ${entry.weeklyXp} XP"
                "xp" -> "⚡ ${entry.totalXp} XP"
                else -> "🔥 ${entry.streak}d"
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isTop3) medalColor.copy(alpha = 0.14f) else PurpleAccent.copy(alpha = 0.12f),
                border = if (isTop3) BorderStroke(1.dp, medalColor.copy(alpha = 0.3f)) else null
            ) {
                Text(
                    text = statLabel,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = if (isTop3) medalColor else PurpleAccent,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun PromotionZoneDivider(nextLeagueName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF10B981).copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "PROMOTION ZONE (Ranks 1–10 advance)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun DemotionZoneDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEF4444).copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "RELEGATION ZONE (Bottom ranks demote)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun StickyUserRankBar(
    currentUserEntry: LeaderboardEntry?,
    currentUserRank: Int,
    activeCategory: String,
    cutoffRank10Xp: Long,
    onClick: () -> Unit
) {
    if (currentUserRank <= 0 || currentUserEntry == null) return

    val inPromotionZone = currentUserRank in 1..10
    val statusColor = if (inPromotionZone) Color(0xFF10B981) else PurpleAccent
    val statusText = if (inPromotionZone) {
        "⬆️ In Promotion Zone!"
    } else {
        val diff = (cutoffRank10Xp - currentUserEntry.weeklyXp + 1).coerceAtLeast(1L)
        "⚡ $diff XP to reach Top 10"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = BorderStroke(1.5.dp, PurpleAccent),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.width(38.dp)
                ) {
                    Text(
                        text = "#$currentUserRank",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = PurpleAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Your Ranking",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PurpleAccent
                        ) {
                            Text(
                                text = "L${currentUserEntry.currentLevel}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            val statLabel = when (activeCategory) {
                "weekly" -> "⚡ ${currentUserEntry.weeklyXp} XP"
                "xp" -> "⚡ ${currentUserEntry.totalXp} XP"
                else -> "🔥 ${currentUserEntry.streak}d"
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PurpleAccent.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statLabel,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = PurpleAccent,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun FriendInviteCard(
    username: String,
    onCopySuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val inviteUrl = "https://kletaq.5122006.xyz/u/$username"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = InkPaperBorder.HeavyShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = InkPaperBorder.heavyBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🤝", fontSize = 20.sp)
                Column {
                    Text(
                        text = "Invite Classmates",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Study together, climb the weekly league, and keep streaks alive!",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = inviteUrl,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(inviteUrl))
                            onCopySuccess()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy link",
                            tint = PurpleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my study league on Kletaq! Study together and climb the weekly leaderboard: $inviteUrl"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Invite Classmates")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share Invite", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(inviteUrl))
                        onCopySuccess()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Copy Link", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }
}
