package com.kletaq.app.features.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.util.HapticFeedbackHelper
import com.kletaq.app.data.model.NotificationModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val rawNotifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    var selectedForMuteItem by remember { mutableStateOf<NotificationModel?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    if (showClearAllConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = {
                Text(
                    text = "Clear all notifications?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all notifications.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirmDialog = false
                        viewModel.deleteAllNotifications()
                        HapticFeedbackHelper.vibrate(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear all", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showClearAllConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // --- 1. HEADER SECTION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (unreadCount > 0) PurpleAccent else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PurpleAccent.copy(alpha = 0.12f),
                            modifier = Modifier.clickable {
                                viewModel.markAllAsRead()
                                HapticFeedbackHelper.vibrate(context)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read all",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Read all",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                            }
                        }
                    }

                    if (rawNotifications.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            modifier = Modifier.clickable {
                                showClearAllConfirmDialog = true
                                HapticFeedbackHelper.vibrate(context)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear all",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Clear all",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

        // --- 2. EMPTY STATE VS NOTIFICATION FEED ---
        if (rawNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PurpleAccent.copy(alpha = 0.12f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🔔", fontSize = 36.sp)
                        }
                    }

                    Text(
                        text = "No notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "You're all caught up.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rawNotifications, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissVal ->
                            if (dismissVal == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteNotification(item.id)
                                HapticFeedbackHelper.vibrate(context)
                                true
                            } else if (dismissVal == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.markAsRead(item.id)
                                HapticFeedbackHelper.vibrate(context)
                                false
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val isDismissingToDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                            val color = if (isDismissingToDelete) Color(0xFFEF4444) else PurpleAccent

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (isDismissingToDelete) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Icon(
                                    imageVector = if (isDismissingToDelete) Icons.Default.Delete else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = color
                                )
                            }
                        }
                    ) {
                        NotificationCard(
                            item = item,
                            onClick = {
                                if (!item.read) {
                                    viewModel.markAsRead(item.id)
                                }
                                HapticFeedbackHelper.vibrate(context)
                            },
                            onLongClick = {
                                selectedForMuteItem = item
                                HapticFeedbackHelper.vibrate(context)
                            }
                        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationCard(
    item: NotificationModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val iconEmoji = when (item.type.lowercase()) {
        "streak" -> "🔥"
        "quest" -> "🎯"
        "xp" -> "⭐"
        "exam" -> "📝"
        "assignment" -> "📚"
        else -> "🔔"
    }

    val badgeColor = when (item.type.lowercase()) {
        "streak" -> Color(0xFFF97316)
        "quest" -> Color(0xFF10B981)
        "xp" -> Color(0xFFEAB308)
        "exam" -> Color(0xFFEF4444)
        "assignment" -> Color(0xFF8B5CF6)
        else -> PurpleAccent
    }

    val timeFormatted = item.createdAt?.let {
        SimpleDateFormat("MMM d, h:mm a", Locale.US).format(it)
    } ?: "Just now"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(
            width = if (!item.read) 1.5.dp else 1.dp,
            color = if (!item.read) PurpleAccent.copy(alpha = 0.4f) else TextSecondary.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!item.read) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = iconEmoji, fontSize = 22.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!item.read) FontWeight.ExtraBold else FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormatted,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )

                        if (!item.read) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PurpleAccent, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = if (!item.read) TextPrimary else TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
