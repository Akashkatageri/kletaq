package com.kletaq.app.features.friends

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
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel()
) {
    var activeSubTab by remember { mutableIntStateOf(0) }

    val friendsList by viewModel.friends.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val globalLeaderboard by viewModel.globalLeaderboard.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLeaderboardLoading by viewModel.isLeaderboardLoading.collectAsState()
    val activeCategory by viewModel.leaderboardCategory.collectAsState()
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
    var leaderboardScope by remember { mutableIntStateOf(0) } // 0 = Global, 1 = Friends

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- HEADER SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Community",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.loadGlobalLeaderboard(activeCategory) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        }
                    }

                    com.kletaq.app.core.ui.NotificationBadgeIcon(
                        unreadCount = unreadNotificationCount,
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(44.dp),
                        iconColor = TextSecondary,
                        containerColor = CardSurface
                    )

                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // --- SUB TAB CONTROLS (2 Tabs: Leaderboard, Friends) ---
            TabRow(
                selectedTabIndex = activeSubTab.coerceIn(0, 1),
                containerColor = CardSurface,
                contentColor = PurpleAccent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = {
                        activeSubTab = 0
                        viewModel.loadGlobalLeaderboard(activeCategory)
                    },
                    modifier = Modifier.requiredWidthIn(min = 20.dp)
                ) {
                    Text(
                        text = "Leaderboard",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeSubTab == 0) PurpleAccent else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    modifier = Modifier.requiredWidthIn(min = 20.dp)
                ) {
                    val friendsTabText = if (friendRequests.isNotEmpty()) {
                        "Friends (${friendsList.size}) • ${friendRequests.size} request${if (friendRequests.size > 1) "s" else ""}"
                    } else {
                        "Friends (${friendsList.size})"
                    }
                    Text(
                        text = friendsTabText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeSubTab == 1) PurpleAccent else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            when (activeSubTab) {
                0 -> {
                    // --- LEADERBOARD TAB ---
                    val sortedFriends = if (activeCategory == "xp") {
                        friendsList.sortedByDescending { it.totalXp }
                    } else {
                        friendsList.sortedByDescending { it.streak }
                    }

                    val activeList = if (leaderboardScope == 0) {
                        globalLeaderboard
                    } else {
                        sortedFriends.mapIndexed { index, item -> item.copy(rank = index + 1) }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Controls Header: Scope Switcher + Category Filters
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Scope: Global vs. Friends
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            onClick = { leaderboardScope = 0 },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (leaderboardScope == 0) PurpleAccent else CardSurface,
                                            border = BorderStroke(1.dp, if (leaderboardScope == 0) PurpleAccent else BorderColor)
                                        ) {
                                            Text(
                                                text = "🌍 Global",
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
                                                text = "👥 Friends Only",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (leaderboardScope == 1) Color.White else TextPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    // Category Filter: Streak vs XP
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                            )
                                        }

                                        Surface(
                                            onClick = { viewModel.setLeaderboardCategory("xp") },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (activeCategory == "xp") PurpleAccent.copy(alpha = 0.15f) else CardSurface,
                                            border = BorderStroke(1.dp, if (activeCategory == "xp") PurpleAccent else BorderColor)
                                        ) {
                                            Text(
                                                text = "⚡ XP",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeCategory == "xp") PurpleAccent else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                if (isLeaderboardLoading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PurpleAccent)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Updating ranking...", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }

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
                                            text = if (leaderboardScope == 1) "No friends found." else "No leaderboard entries yet.",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (leaderboardScope == 1) "Connect with classmates in the Friends tab to see your ranking!" else "Study daily to be the first student ranked on the leaderboard!",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else if (activeList.isNotEmpty()) {
                            // 2. TOP 3 PODIUM (IF 3 OR MORE USERS)
                            if (activeList.size >= 3) {
                                item {
                                    LeaderboardPodium(
                                        first = activeList[0],
                                        second = activeList[1],
                                        third = activeList[2],
                                        activeCategory = activeCategory,
                                        currentUserId = viewModel.currentUserId,
                                        onUserClick = { entry -> viewModel.openUserProfile(entry.uid, entry) }
                                    )
                                }

                                // Header for remaining ranks
                                if (activeList.size > 3) {
                                    item {
                                        Text(
                                            text = "RANKS 4+",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextSecondary,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                }

                                // 3. RANKS 4 AND ONWARDS
                                itemsIndexed(activeList.drop(3), key = { _, item -> item.uid.ifBlank { item.username } }) { index, entry ->
                                    val realRank = index + 4
                                    LeaderboardRowCard(
                                        entry = entry,
                                        rank = realRank,
                                        activeCategory = activeCategory,
                                        isCurrentUser = entry.uid == viewModel.currentUserId,
                                        onClick = { viewModel.openUserProfile(entry.uid, entry) }
                                    )
                                }
                            } else {
                                // Less than 3 users - render all as clean rows
                                itemsIndexed(activeList, key = { _, item -> item.uid.ifBlank { item.username } }) { index, entry ->
                                    LeaderboardRowCard(
                                        entry = entry,
                                        rank = index + 1,
                                        activeCategory = activeCategory,
                                        isCurrentUser = entry.uid == viewModel.currentUserId,
                                        onClick = { viewModel.openUserProfile(entry.uid, entry) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // --- FRIENDS TAB ---
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Friend requests section at top (if pending requests exist)
                        if (friendRequests.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = InkPaperBorder.HeavyShape,
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                border = InkPaperBorder.heavyBorder(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Friend Requests (${friendRequests.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )

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

                        // 2. Search field: "Find students"
                        OutlinedTextField(
                            value = quickFindQuery,
                            onValueChange = {
                                quickFindQuery = it
                                viewModel.searchUsers(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Find students by name or email...", fontSize = 13.sp) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PurpleAccent) },
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PurpleAccent)
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

                        // 3. Search results (when query not blank) vs 4. Friends list (when query blank)
                        if (quickFindQuery.isNotBlank()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                                                    text = "No student found for '$quickFindQuery'",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary
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
                                                        Text(text = "🔥 ${user.streak}-day streak • Tap to view profile", fontSize = 11.sp, color = TextSecondary)
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
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "No friends added yet.",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Use the search bar above to find students by name or email.",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                } else {
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
                                                    Text(text = "🎓", fontSize = 22.sp)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = friend.username,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "Tap to view profile & all progress",
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
                                                            text = "🔥 ${friend.streak}-day streak",
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
private fun LeaderboardPodium(
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry,
    activeCategory: String,
    currentUserId: String,
    onUserClick: (LeaderboardEntry) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Silver)
        PodiumColumn(
            modifier = Modifier.weight(1f),
            entry = second,
            rank = 2,
            rankBadge = "🥈 #2",
            accentColor = Color(0xFF64748B),
            containerColor = CardSurface,
            height = 160.dp,
            activeCategory = activeCategory,
            isCurrentUser = second.uid == currentUserId,
            onClick = { onUserClick(second) }
        )

        // 1st Place (Gold Champion) - Elevated
        PodiumColumn(
            modifier = Modifier.weight(1.12f),
            entry = first,
            rank = 1,
            rankBadge = "🥇 #1",
            accentColor = Color(0xFFF59E0B),
            containerColor = Color(0xFFFEF3C7).copy(alpha = 0.15f),
            height = 190.dp,
            activeCategory = activeCategory,
            isCurrentUser = first.uid == currentUserId,
            crown = true,
            onClick = { onUserClick(first) }
        )

        // 3rd Place (Bronze)
        PodiumColumn(
            modifier = Modifier.weight(1f),
            entry = third,
            rank = 3,
            rankBadge = "🥉 #3",
            accentColor = Color(0xFFB45309),
            containerColor = CardSurface,
            height = 145.dp,
            activeCategory = activeCategory,
            isCurrentUser = third.uid == currentUserId,
            onClick = { onUserClick(third) }
        )
    }
}

@Composable
private fun PodiumColumn(
    modifier: Modifier = Modifier,
    entry: LeaderboardEntry,
    rank: Int,
    rankBadge: String,
    accentColor: Color,
    containerColor: Color,
    height: androidx.compose.ui.unit.Dp,
    activeCategory: String,
    isCurrentUser: Boolean,
    crown: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(height)
            .clickable(onClick = onClick),
        shape = InkPaperBorder.MediumShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            if (isCurrentUser) 2.dp else 1.5.dp,
            if (isCurrentUser) PurpleAccent else accentColor.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (rank == 1) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Crown or Rank Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (crown) {
                    Text(text = "👑", fontSize = 16.sp)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = rankBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Avatar with Level Tag
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(if (rank == 1) 40.dp else 34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🎓", fontSize = if (rank == 1) 20.sp else 16.sp)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor
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

            // Username
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = entry.username,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCurrentUser) {
                    Text(
                        text = "(You)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                }
            }

            // Metric Chip
            val statText = if (activeCategory == "xp") "⚡ ${entry.totalXp}" else "🔥 ${entry.streak}d"
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    entry: LeaderboardEntry,
    rank: Int,
    activeCategory: String,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = InkPaperBorder.MediumShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = if (isCurrentUser) BorderStroke(2.dp, PurpleAccent) else InkPaperBorder.heavyBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank label
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.width(36.dp)
                )

                // Avatar with Level Badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        shape = CircleShape,
                        color = PurpleAccent.copy(alpha = 0.10f),
                        modifier = Modifier.size(36.dp)
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
                        text = "Tap to view all progress",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val statLabel = if (activeCategory == "xp") "⚡ ${entry.totalXp} XP" else "🔥 ${entry.streak}d"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleAccent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statLabel,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = PurpleAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Profile",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
