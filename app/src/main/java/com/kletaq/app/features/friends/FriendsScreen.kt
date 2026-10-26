package com.kletaq.app.features.friends

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.google.firebase.auth.FirebaseAuth
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
import androidx.compose.runtime.saveable.rememberSaveable
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.data.model.LeaderboardEntry
import com.kletaq.app.features.friends.components.ClassmateActivityFeed
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
    val classmateActivities by viewModel.classmateActivities.collectAsState()
    val promotedLeague by viewModel.promotedLeague.collectAsState()
    val currentUserLeague by viewModel.currentUserLeague.collectAsState()
    var showPromotionBanner by rememberSaveable { mutableStateOf(true) }
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
    val userLeague = remember(currentUserLeague, currentUserEntry?.league) {
        val fromVm = currentUserLeague.trim()
        val fromEntry = currentUserEntry?.league?.trim()
        when {
            fromVm.isNotBlank() && !fromVm.equals("bronze", ignoreCase = true) -> fromVm
            !fromEntry.isNullOrBlank() && !fromEntry.equals("bronze", ignoreCase = true) -> fromEntry
            fromVm.isNotBlank() -> fromVm
            !fromEntry.isNullOrBlank() -> fromEntry
            else -> "Bronze"
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }

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
                        color = if (isDark) Color(0xFF1C1C1E) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
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
                        containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF1C1C1E) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
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
                    val currentAuthUser = FirebaseAuth.getInstance().currentUser
                    val effectiveCurrentUserEntry = (currentUserEntry ?: currentAuthUser?.let {
                        LeaderboardEntry(
                            uid = it.uid,
                            username = currentUsername,
                            photoUrl = it.photoUrl?.toString() ?: "",
                            currentLevel = 1,
                            totalXp = 0L,
                            weeklyXp = 0L,
                            monthlyXp = 0L,
                            streak = 0,
                            league = userLeague
                        )
                    })?.copy(league = userLeague)

                    // Overlay userLeague on current user in global leaderboard
                    val patchedGlobal = globalLeaderboard.map { entry ->
                        if (entry.uid == viewModel.currentUserId) {
                            entry.copy(league = userLeague)
                        } else {
                            entry
                        }
                    }.let { list ->
                        if (list.none { it.uid == viewModel.currentUserId } && effectiveCurrentUserEntry != null) {
                            list + effectiveCurrentUserEntry
                        } else {
                            list
                        }
                    }

                    val sortedFriends = (friendsList + listOfNotNull(effectiveCurrentUserEntry)).distinctBy { it.uid }
                        .sortedByDescending { it.weeklyXp }

                    val activeList = when (leaderboardScope) {
                        0 -> {
                            // 🏆 League scope: filter users in the user's current league tier
                            val inLeague = patchedGlobal.filter { it.league.equals(userLeague, ignoreCase = true) }
                            if (inLeague.isNotEmpty()) {
                                inLeague.mapIndexed { index, item -> item.copy(rank = index + 1) }
                            } else {
                                patchedGlobal.mapIndexed { index, item -> item.copy(rank = index + 1) }
                            }
                        }
                        1 -> sortedFriends.mapIndexed { index, item -> item.copy(rank = index + 1) }
                        else -> patchedGlobal.mapIndexed { index, item -> item.copy(rank = index + 1) } // 🌐 Global scope: all students across the entire app
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

                        // League Promotion Celebration Banner
                        if (userLeague != "Bronze" && showPromotionBanner) {
                            item {
                                DuolingoLeaguePromotionBanner(
                                    league = userLeague,
                                    onDismiss = {
                                        showPromotionBanner = false
                                        viewModel.dismissPromotion()
                                    }
                                )
                            }
                        }

                        // Today's Classmate Activity Feed (Visible in Classmates scope)
                        if (leaderboardScope == 1) {
                            item {
                                ClassmateActivityFeed(
                                    activities = classmateActivities,
                                    onCheerClick = { viewModel.cheerActivity(it) },
                                    onStudentClick = { uid -> viewModel.openUserProfile(uid) },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
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
                                        text = when (leaderboardScope) {
                                            0 -> "League is waiting for you!"
                                            1 -> "No friends ranked yet"
                                            else -> "Global leaderboard is waiting for you!"
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = when (leaderboardScope) {
                                            0 -> "Complete a study session to claim your rank!"
                                            1 -> "Invite classmates to compete together!"
                                            else -> "Complete a study session to claim your rank!"
                                        },
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

                        // 1b. Today's Classmate Activity Feed
                        item {
                            ClassmateActivityFeed(
                                activities = classmateActivities,
                                onCheerClick = { viewModel.cheerActivity(it) },
                                onStudentClick = { uid -> viewModel.openUserProfile(uid) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // 2. Pending Friend Requests
                        if (friendRequests.isNotEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF1C1C1E) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
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
                                    unfocusedBorderColor = if (isDark) Color(0xFF2E2E38) else Color(0xFFE2E8F0),
                                    focusedContainerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
                                    unfocusedContainerColor = if (isDark) Color(0xFF1C1C1E) else Color.White
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
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1C1C1E) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E2E38) else Color(0xFFE2E8F0))
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

// --- EXACT DUOLINGO LEAGUE HEADER (GEMS, TITLE, COUNTDOWN, 3 SCOPES) ---
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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

        // 2. League Tournament Title
        Text(
            text = "$currentLeague Tournament",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(3.dp))

        // 3. Subtitle
        Text(
            text = "Top 10 reach the Semifinals",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Yellow Countdown
        Text(
            text = countdownText.replace(" left", ""),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEAB308)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. 3 Scope Switchers: [ 🏆 League | 👥 Friends | 🌐 Global ]
        val surfaceColor = MaterialTheme.colorScheme.surface
        val isDark = surfaceColor.luminance() < 0.5f

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1C1C1E) else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E2E38) else Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(
                    0 to "🏆 League",
                    1 to "👥 Friends",
                    2 to "🌐 Global"
                ).forEach { (scopeIndex, label) ->
                    val isSelected = leaderboardScope == scopeIndex
                    Surface(
                        onClick = { onScopeChanged(scopeIndex) },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) PurpleAccent else Color.Transparent
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
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
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f
    val highlightColor = if (isDark) Color(0xFF06B6D4) else PurpleAccent
    val rowBackground = if (isCurrentUser) {
        if (isDark) Color(0xFF0F363C) else Color(0xFFF3F0FF)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = if (isCurrentUser) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp),
        color = rowBackground,
        border = if (isCurrentUser) androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF06B6D4).copy(alpha = 0.4f) else PurpleAccent.copy(alpha = 0.35f)) else null
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

            // 2. Avatar with cute accessory badge
            val accessory = when {
                isCurrentUser -> "🏆"
                rank == 1 -> "👑"
                rank == 2 -> "🌟"
                rank == 3 -> "🔥"
                rank % 5 == 0 -> "🍿"
                rank % 4 == 0 -> "🐱"
                else -> null
            }

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
                if (accessory != null) {
                    Text(
                        text = accessory,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 3. User Name & Streak Flame
            val streakText = when {
                entry.streak >= 730 -> "🔥 ${entry.streak / 365}+ years"
                entry.streak >= 365 -> "🔥 1+ year"
                entry.streak > 0 -> "🔥 ${entry.streak} days"
                else -> "🔥 0 days"
            }

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
                    text = streakText,
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

// --- DUOLINGO RANK BADGE (1 IN GOLD RIBBON, 2 IN SILVER RIBBON, 3 IN BRONZE RIBBON, PLAIN NUMBERS 4+) ---
@Composable
private fun DuolingoRankBadge(rank: Int, isCurrentUser: Boolean) {
    Box(
        modifier = Modifier.width(30.dp),
        contentAlignment = Alignment.Center
    ) {
        when (rank) {
            1 -> DuolingoRibbonMedal(
                rank = 1,
                ribbonColor = Color(0xFFFACC15),
                textColor = Color(0xFF854D0E)
            )
            2 -> DuolingoRibbonMedal(
                rank = 2,
                ribbonColor = Color(0xFF94A3B8),
                textColor = Color(0xFF334155)
            )
            3 -> DuolingoRibbonMedal(
                rank = 3,
                ribbonColor = Color(0xFFFB923C),
                textColor = Color(0xFF7C2D12)
            )
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

// --- DUOLINGO SCALLOPED RIBBON MEDAL (CANVAS DRAWN WITH RIBBON TAILS) ---
@Composable
private fun DuolingoRibbonMedal(
    rank: Int,
    ribbonColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier.size(width = 24.dp, height = 28.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val circleRadius = w / 2f

            // 1. Draw ribbon tails at the bottom
            val tailPath = Path().apply {
                // Left ribbon tail
                moveTo(w * 0.2f, circleRadius * 1.4f)
                lineTo(w * 0.1f, h)
                lineTo(w * 0.35f, h - 3.dp.toPx())
                lineTo(w * 0.45f, circleRadius * 1.4f)
                close()

                // Right ribbon tail
                moveTo(w * 0.55f, circleRadius * 1.4f)
                lineTo(w * 0.65f, h - 3.dp.toPx())
                lineTo(w * 0.9f, h)
                lineTo(w * 0.8f, circleRadius * 1.4f)
                close()
            }
            drawPath(tailPath, color = ribbonColor)

            // 2. Draw circular medal disc
            drawCircle(
                color = ribbonColor,
                radius = circleRadius,
                center = Offset(w / 2f, circleRadius)
            )

            // 3. Draw inner white circle
            drawCircle(
                color = Color.White,
                radius = circleRadius * 0.68f,
                center = Offset(w / 2f, circleRadius)
            )
        }

        // 4. Rank number centered in the circle
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- DUOLINGO LEAGUE PROMOTION CELEBRATION BANNER ---
@Composable
private fun DuolingoLeaguePromotionBanner(
    league: String,
    onDismiss: () -> Unit
) {
    val gem = when (league.lowercase().trim()) {
        "silver" -> "🥈"
        "gold" -> "🥇"
        "sapphire" -> "💎"
        "diamond" -> "👑"
        else -> "🥉"
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.5f) else Color(0xFFF5F3FF),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, PurpleAccent.copy(alpha = if (isDark) 0.4f else 0.25f), RoundedCornerShape(16.dp))
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
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = gem, fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PROMOTED TO ${league.uppercase()}! 🎉",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PurpleAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "You've unlocked the $league Tournament! Compete with students to reach Diamond.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
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
            Text(text = "▲", fontSize = 10.sp, color = Color(0xFFA855F7))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SEMIFINALS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFA855F7),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "▲", fontSize = 10.sp, color = Color(0xFFA855F7))
        }

        // Current League
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "▼", fontSize = 10.sp, color = Color(0xFF06B6D4))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${currentLeague.uppercase()} LEAGUE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF06B6D4),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "▼", fontSize = 10.sp, color = Color(0xFF06B6D4))
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

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1C1C1E) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
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
                color = if (isDark) Color(0xFF262629) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF383846) else Color(0xFFE2E8F0))
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
                    color = if (isDark) Color(0xFF262629) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF383846) else Color(0xFFE2E8F0)),
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
