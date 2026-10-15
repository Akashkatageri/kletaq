package com.kletaq.app.features.friends

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.HorizontalDivider
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
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.LeaderboardEntry
import com.kletaq.app.features.friends.components.StudentProfileBottomSheet

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
    var leaderboardScope by remember { mutableIntStateOf(0) } // 0 = Global League, 1 = Friends Only

    val currentUserEntry = globalLeaderboard.find { it.uid == viewModel.currentUserId }
    val userLeague = currentUserEntry?.league?.ifBlank { "Bronze" } ?: "Bronze"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // --- 1. TOP APP BAR (SEAMLESS & BORDERLESS) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.loadGlobalLeaderboard("weekly") },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    com.kletaq.app.core.ui.NotificationBadgeIcon(
                        unreadCount = unreadNotificationCount,
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(38.dp),
                        iconColor = TextSecondary,
                        containerColor = CardSurface
                    )

                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // --- 2. CAPSULE TABS (DUOLINGO SUB-TAB SWITCHER) ---
            DuolingoCapsuleTabs(
                selectedTab = activeSubTab,
                onTabSelected = { tab ->
                    activeSubTab = tab
                    if (tab == 0) viewModel.loadGlobalLeaderboard("weekly")
                },
                requestsCount = friendRequests.size
            )

            Spacer(modifier = Modifier.height(6.dp))

            // --- 3. TAB CONTENT ---
            when (activeSubTab) {
                0 -> {
                    // --- DUOLINGO WEEKLY LEAGUE ---
                    val sortedFriends = friendsList.sortedByDescending { it.weeklyXp }
                    val activeList = if (leaderboardScope == 0) {
                        globalLeaderboard
                    } else {
                        sortedFriends.mapIndexed { index, item -> item.copy(rank = index + 1) }
                    }

                    val currentUserRank = activeList.indexOfFirst { it.uid == viewModel.currentUserId }.let {
                        if (it >= 0) it + 1 else 0
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Header with League Gems & Title
                        item {
                            DuolingoLeagueHeader(
                                currentLeague = userLeague,
                                countdownText = weeklyCountdown,
                                leaderboardScope = leaderboardScope,
                                onScopeChanged = { leaderboardScope = it }
                            )
                        }

                        if (isLeaderboardLoading && activeList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PurpleAccent, strokeWidth = 2.5.dp)
                                }
                            }
                        } else if (activeList.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (leaderboardScope == 1) "No friends ranked yet" else "League is waiting for you!",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (leaderboardScope == 1) "Invite classmates to compete together!" else "Complete a study session to claim your rank!",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            // --- EXACT DUOLINGO LEADERBOARD ROWS ---
                            itemsIndexed(activeList, key = { _, item -> item.uid.ifBlank { item.username } }) { index, entry ->
                                val realRank = index + 1

                                DuolingoLeaderboardRow(
                                    entry = entry,
                                    rank = realRank,
                                    isCurrentUser = entry.uid == viewModel.currentUserId,
                                    onClick = { viewModel.openUserProfile(entry.uid, entry) }
                                )

                                // DUOLINGO PROMOTION ZONE DIVIDER (Between Rank 10 and 11)
                                if (realRank == 10 && activeList.size > 10) {
                                    DuolingoPromotionDivider(currentLeague = userLeague)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // --- CLASSMATES & FRIENDS TAB ---
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // 1. Friend Invite Card
                        item {
                            FriendInviteCard(
                                username = currentUsername,
                                onCopySuccess = {
                                    Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // 2. Pending Friend Requests
                        if (friendRequests.isNotEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = CardSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Friend Requests (${friendRequests.size})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimary
                                        )

                                        friendRequests.forEach { request ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = PurpleAccent.copy(alpha = 0.12f),
                                                        modifier = Modifier.size(38.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(text = "🎓", fontSize = 18.sp)
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(text = request.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                        Text(text = "🔥 ${request.streak}-day streak", fontSize = 11.sp, color = Color(0xFFF97316))
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    IconButton(
                                                        onClick = { viewModel.acceptFriendRequest(request.uid) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF10B981))
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.rejectFriendRequest(request.uid) },
                                                        modifier = Modifier.size(32.dp)
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

                        // 3. Search Bar
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
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PurpleAccent)
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
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = CardSurface,
                                    unfocusedContainerColor = CardSurface
                                ),
                                singleLine = true
                            )
                        }

                        // 4. Search Results or Classmates Roster
                        if (quickFindQuery.isNotBlank()) {
                            if (searchResults.isEmpty() && !isSearching) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "No students found for '$quickFindQuery'", fontSize = 13.sp, color = TextSecondary)
                                    }
                                }
                            } else {
                                items(searchResults, key = { it.uid }) { user ->
                                    val reqState = requestStates[user.uid] ?: RequestState.Idle
                                    val isSending = reqState is RequestState.Sending
                                    val isSent = reqState is RequestState.Sent
                                    val isError = reqState is RequestState.Error

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openUserProfile(user.uid, user) }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = PurpleAccent.copy(alpha = 0.12f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(text = "👤", fontSize = 18.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = user.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                Text(text = "🔥 ${user.streak}-day streak • L${user.currentLevel}", fontSize = 11.sp, color = Color(0xFFF97316))
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
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            if (isSending) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                                            } else {
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
                        } else {
                            if (friendsList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No classmates added yet. Use the invite link above!",
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            } else {
                                items(friendsList, key = { it.uid }) { friend ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openUserProfile(friend.uid, friend) }
                                            .padding(horizontal = 8.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = PurpleAccent.copy(alpha = 0.12f),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(text = "🎓", fontSize = 18.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(text = friend.username, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                                Text(text = "🔥 ${friend.streak}-day streak", fontSize = 11.sp, color = Color(0xFFF97316))
                                            }
                                        }

                                        Text(
                                            text = "${friend.weeklyXp} XP",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
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

// --- DUOLINGO SUB TAB CONTROLS ---
@Composable
private fun DuolingoCapsuleTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    requestsCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(0) },
                shape = RoundedCornerShape(11.dp),
                color = if (selectedTab == 0) PurpleAccent else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
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
                shape = RoundedCornerShape(11.dp),
                color = if (selectedTab == 1) PurpleAccent else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
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
                            modifier = Modifier.size(17.dp)
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

// --- EXACT DUOLINGO LEAGUE HEADER (GEMS, TITLE, COUNTDOWN) ---
@Composable
private fun DuolingoLeagueHeader(
    currentLeague: String,
    countdownText: String,
    leaderboardScope: Int,
    onScopeChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. League Gems Progression Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("🥉", "🥈", "🥇", "💎", "👑").forEachIndexed { idx, gem ->
                val isCurrent = when (currentLeague.lowercase().trim()) {
                    "bronze" -> idx == 0
                    "silver" -> idx == 1
                    "gold" -> idx == 2
                    "sapphire" -> idx == 3
                    "diamond" -> idx == 4
                    else -> idx == 0
                }
                Surface(
                    shape = CircleShape,
                    color = if (isCurrent) PurpleAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(if (isCurrent) 40.dp else 32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = gem, fontSize = if (isCurrent) 22.sp else 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. League Title
        Text(
            text = "$currentLeague League",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(3.dp))

        // 3. Subtitle
        Text(
            text = "Top 10 advance to the next league",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(3.dp))

        // 4. Yellow Countdown
        Text(
            text = countdownText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEAB308)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Minimal Scope Switcher (Global vs Friends)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardSurface
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                Surface(
                    onClick = { onScopeChanged(0) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (leaderboardScope == 0) PurpleAccent else Color.Transparent
                ) {
                    Text(
                        text = "🌍 League",
                        fontSize = 11.sp,
                        fontWeight = if (leaderboardScope == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (leaderboardScope == 0) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    onClick = { onScopeChanged(1) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (leaderboardScope == 1) PurpleAccent else Color.Transparent
                ) {
                    Text(
                        text = "👥 Friends",
                        fontSize = 11.sp,
                        fontWeight = if (leaderboardScope == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (leaderboardScope == 1) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Hairline Divider
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    }
}

// --- EXACT DUOLINGO LEADERBOARD ROW (PURE SEAMLESS LIST) ---
@Composable
private fun DuolingoLeaderboardRow(
    entry: LeaderboardEntry,
    rank: Int,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    val highlightColor = Color(0xFF06B6D4) // Duolingo cyan / mint highlight
    val rowBackground = if (isCurrentUser) {
        highlightColor.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = if (isCurrentUser) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp),
        color = rowBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCurrentUser) 12.dp else 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Duolingo Rank Badge
            DuolingoRankBadge(rank = rank, isCurrentUser = isCurrentUser)

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Avatar
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = CircleShape,
                    color = if (isCurrentUser) highlightColor.copy(alpha = 0.2f) else PurpleAccent.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (entry.photoUrl.isNotBlank()) "🎓" else "👤",
                            fontSize = 20.sp
                        )
                    }
                }
                if (isCurrentUser) {
                    Text(
                        text = "🏆",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 3. User Name & Streak Flame
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) highlightColor else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🔥 ${entry.streak} days",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF97316)
                )
            }

            // 4. XP Amount
            Text(
                text = "${entry.weeklyXp} XP",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentUser) highlightColor else TextSecondary
            )
        }
    }
}

// --- DUOLINGO RANK BADGE (1 IN GOLD, 2 IN SILVER, 3 IN BRONZE, PLAIN NUMBERS 4+) ---
@Composable
private fun DuolingoRankBadge(rank: Int, isCurrentUser: Boolean) {
    Box(
        modifier = Modifier.width(30.dp),
        contentAlignment = Alignment.Center
    ) {
        when (rank) {
            1 -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFACC15),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "1",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
            2 -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "2",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
            3 -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFDBA74),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "3",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF7C2D12)
                        )
                    }
                }
            }
            else -> {
                Text(
                    text = "$rank",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) Color(0xFF06B6D4) else TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- DUOLINGO PROMOTION ZONE DIVIDER ---
@Composable
private fun DuolingoPromotionDivider(currentLeague: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Promotion Zone
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "PROMOTION ZONE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF8B5CF6),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(13.dp)
            )
        }

        // Current League
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = Color(0xFF06B6D4),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentLeague.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF06B6D4),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = Color(0xFF06B6D4),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

// --- FRIEND INVITE CARD ---
@Composable
private fun FriendInviteCard(
    username: String,
    onCopySuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val inviteUrl = "https://kletaq.5122006.xyz/u/$username"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🤝", fontSize = 22.sp)
                Column {
                    Text(
                        text = "Invite Classmates",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Compete together on the weekly league and keep streaks alive!",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        modifier = Modifier.size(28.dp)
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
                                "Join my study league on Kletaq! Compete on the weekly leaderboard: $inviteUrl"
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
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Copy Link", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }
}
