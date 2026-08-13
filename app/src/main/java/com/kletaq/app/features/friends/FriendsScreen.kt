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
    val requestStates by viewModel.requestStates.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    var selectedProfileForModal by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var quickFindQuery by remember { mutableStateOf("") }
    var leaderboardCategory by remember { mutableIntStateOf(0) } // 0 = XP, 1 = Streak
    var leaderboardScope by remember { mutableIntStateOf(0) } // 0 = Global, 1 = Friends

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // --- HEADER SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Friends",
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
                        onClick = { viewModel.loadGlobalLeaderboard(if (leaderboardCategory == 0) "xp" else "streak") },
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
                    viewModel.loadGlobalLeaderboard("streak")
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
                // --- LEADERBOARD TAB (STREAK IN DAYS) ---
                val activeList = if (leaderboardScope == 0) globalLeaderboard else friendsList.mapIndexed { index, item -> item.copy(rank = index + 1) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { leaderboardScope = 0 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (leaderboardScope == 0) PurpleAccent else CardSurface,
                                        contentColor = if (leaderboardScope == 0) Color.White else TextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Global", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { leaderboardScope = 1 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (leaderboardScope == 1) PurpleAccent else CardSurface,
                                        contentColor = if (leaderboardScope == 1) Color.White else TextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Friends Only", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (isLeaderboardLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PurpleAccent)
                            }
                        }
                    }

                    if (activeList.isEmpty()) {
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
                                        text = if (isLeaderboardLoading) "Loading leaderboard..." else "No leaderboard entries found.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Maintain a daily study streak to rank on the leaderboard!",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(activeList, key = { _, item -> item.uid.ifBlank { item.username } }) { index, entry ->
                            val rankDisplay = "#${index + 1}"
                            val streakLabel = "${entry.streak}-day streak"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = rankDisplay,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = TextPrimary,
                                            modifier = Modifier.width(36.dp)
                                        )
                                        Text(text = "🎓", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = entry.username,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PurpleAccent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "🔥 $streakLabel",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = PurpleAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
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
                    // 1. Friend requests section at the top, only when requests exist
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "🎓", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = request.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                Text(text = "🔥 ${request.streak}-day streak", fontSize = 11.sp, color = TextSecondary)
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

                    // 2. Search field: "Find students"
                    OutlinedTextField(
                        value = quickFindQuery,
                        onValueChange = {
                            quickFindQuery = it
                            viewModel.searchUsers(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Find students", fontSize = 13.sp) },
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

                    // 3. Search results (when query not blank) vs 4. Existing friends list (when query blank)
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
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = InkPaperBorder.HeavyShape,
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        border = InkPaperBorder.heavyBorder()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "👤", fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = user.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                    Text(text = "🔥 ${user.streak}-day streak", fontSize = 11.sp, color = TextSecondary)
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                                text = "Use the search bar above to find students.",
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
                                            .clickable { selectedProfileForModal = friend },
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "🎓", fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = friend.username,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                            }

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
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
