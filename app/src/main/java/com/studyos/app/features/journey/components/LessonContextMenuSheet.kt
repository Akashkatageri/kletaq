package com.studyos.app.features.journey.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyos.app.core.theme.PurpleAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonContextMenuSheet(
    lesson: LessonNode,
    onDismiss: () -> Unit,
    onMarkComplete: (LessonNode) -> Unit,
    onAddToRevision: (LessonNode) -> Unit,
    onBookmark: (LessonNode) -> Unit,
    onViewNotes: (LessonNode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ListItem(
                headlineContent = { Text("Mark complete") },
                leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981)) },
                modifier = Modifier.clickable {
                    onMarkComplete(lesson)
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Add to revision") },
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null, tint = PurpleAccent) },
                modifier = Modifier.clickable {
                    onAddToRevision(lesson)
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Bookmark") },
                leadingContent = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFFF9800)) },
                modifier = Modifier.clickable {
                    onBookmark(lesson)
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("View notes") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.clickable {
                    onViewNotes(lesson)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
