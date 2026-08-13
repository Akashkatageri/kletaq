package com.kletaq.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kletaq.app.core.theme.PurpleAccent

@Composable
fun NotificationBadgeIcon(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = PurpleAccent,
    containerColor: Color = PurpleAccent.copy(alpha = 0.12f)
) {
    val contentDesc = if (unreadCount > 0) {
        "Notifications, unread notifications available"
    } else {
        "Notifications"
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = contentDesc,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(8.dp)
                    .background(Color(0xFFEF4444), CircleShape)
            )
        }
    }
}
