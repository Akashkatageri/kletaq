package com.kletaq.app.features.lesson.components

import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.kletaq.app.core.theme.PurpleAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberBitmapFromAsset(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(assetPath) {
        withContext(Dispatchers.IO) {
            try {
                if (assetPath.startsWith("http://") || assetPath.startsWith("https://")) {
                    val request = coil.request.ImageRequest.Builder(context)
                        .data(assetPath)
                        .allowHardware(false)
                        .build()
                    val result = (coil.Coil.imageLoader(context).execute(request) as? coil.request.SuccessResult)?.drawable
                    (result as? android.graphics.drawable.BitmapDrawable)?.bitmap?.let {
                        bitmap = it.asImageBitmap()
                    }
                } else {
                    val cleanPath = assetPath.removePrefix("file://")
                    val file = java.io.File(cleanPath)
                    if (file.exists() && file.isFile) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        bmp?.let { bitmap = it.asImageBitmap() }
                    } else {
                        context.assets.open(assetPath).use { inputStream ->
                            val bmp = BitmapFactory.decodeStream(inputStream)
                            bmp?.let { bitmap = it.asImageBitmap() }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
    return bitmap
}

@Composable
fun EditTopicPageRangeDialog(
    topicTitle: String,
    totalPages: Int,
    initialStart: Int,
    initialEnd: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var start by remember { mutableIntStateOf(initialStart.coerceIn(1, totalPages)) }
    var end by remember { mutableIntStateOf(initialEnd.coerceIn(start, totalPages)) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Set Topic Notes Pages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Which pages in your module notes cover:\n$topicTitle?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Page", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (start > 1) { start--; if (end < start) end = start } },
                                enabled = start > 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Dec", modifier = Modifier.size(14.dp))
                            }
                            Text("$start", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp))
                            IconButton(
                                onClick = { if (start < totalPages) { start++; if (end < start) end = start } },
                                enabled = start < totalPages,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Inc", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Text("to", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("End Page", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (end > start) end-- },
                                enabled = end > start,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Dec", modifier = Modifier.size(14.dp))
                            }
                            Text("$end", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp))
                            IconButton(
                                onClick = { if (end < totalPages) end++ },
                                enabled = end < totalPages,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Inc", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(start, end) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text("Save Pages")
                    }
                }
            }
        }
    }
}

@Composable
fun HandwrittenNotesCard(
    pages: List<String>,
    onOpenPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onUploadNotes: (() -> Unit)? = null,
    onDeleteNotes: (() -> Unit)? = null,
    isCustomUpload: Boolean = false,
    scopedRange: IntRange? = null,
    topicTitle: String = "",
    onUpdateManualRange: ((startPage: Int, endPage: Int) -> Unit)? = null
) {
    var showEditRangeDialog by remember { mutableStateOf(false) }

    if (pages.isEmpty()) {
        if (onUploadNotes != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.35f)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PurpleAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Attach Module Notes (PDF / Photo)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload PDF notes or camera photos. Compressed on-device and stored securely in the Cloud.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onUploadNotes,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PurpleAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent)
                    ) {
                        Text(text = "Upload Notes (PDF / Images) 📄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    val scopedStart = (scopedRange?.first ?: 1).coerceIn(1, pages.size.coerceAtLeast(1))
    val scopedEnd = (scopedRange?.last ?: pages.size).coerceIn(scopedStart, pages.size.coerceAtLeast(1))
    val isScoped = scopedRange != null && pages.size > 1
    val scopedIndices = (scopedStart - 1)..(scopedEnd - 1)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PurpleAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isCustomUpload) "My Module Notes" else "Classroom Handwritten Notes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCustomUpload) {
                            Text(
                                text = "☁️ Synced to Cloud",
                                fontSize = 10.sp,
                                color = PurpleAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PurpleAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isScoped) "Topic: P.$scopedStart - P.$scopedEnd" else "${pages.size} Pages",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isScoped && onUpdateManualRange != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showEditRangeDialog = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Topic Pages",
                                tint = PurpleAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isScoped) "Jumped to pages for this topic. Swipe in full screen anytime to view all ${pages.size} pages."
                       else "Scanned classroom lecture notes & worked derivations. Tap any page to zoom in.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // If scoped, display target topic pages first
                val displayIndices = if (isScoped) scopedIndices.toList() else pages.indices.toList()

                itemsIndexed(displayIndices) { _, pageIndex ->
                    val pagePath = pages[pageIndex]
                    val bitmap = rememberBitmapFromAsset(pagePath)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(if (isScoped) 1.5.dp else 1.dp, if (isScoped) PurpleAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .width(110.dp)
                            .height(155.dp)
                            .clickable { onOpenPage(pageIndex) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Page ${pageIndex + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val isOnline = pagePath.startsWith("http://") || pagePath.startsWith("https://")
                                val existsLocally = !isOnline && java.io.File(pagePath.removePrefix("file://")).exists()
                                if (!isOnline && !existsLocally) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Sync pending",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = PurpleAccent
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(topStart = 8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = "P.${pageIndex + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // If scoped and there are more module pages, show trailing browse card
                if (isScoped && pages.size > scopedIndices.count()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .width(110.dp)
                                .height(155.dp)
                                .clickable { onOpenPage(0) }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "+${pages.size - scopedIndices.count()} more",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Browse all ${pages.size} pages",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenPage(scopedStart - 1) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PurpleAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScoped) "View Notes (P.$scopedStart-$scopedEnd)" else "View Fullscreen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onUploadNotes != null) {
                    TextButton(
                        onClick = onUploadNotes,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isCustomUpload) "+ Add / Manage" else "Upload Custom", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PurpleAccent)
                    }
                }

                if (onDeleteNotes != null && isCustomUpload) {
                    TextButton(
                        onClick = onDeleteNotes,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }

    if (showEditRangeDialog) {
        EditTopicPageRangeDialog(
            topicTitle = topicTitle,
            totalPages = pages.size,
            initialStart = scopedStart,
            initialEnd = scopedEnd,
            onDismiss = { showEditRangeDialog = false },
            onSave = { s, e ->
                onUpdateManualRange?.invoke(s, e)
                showEditRangeDialog = false
            }
        )
    }
}

@Composable
fun HandwrittenNotesViewerDialog(
    pages: List<String>,
    initialPage: Int,
    topicTitle: String,
    scopedRange: IntRange? = null,
    onDismiss: () -> Unit
) {
    var currentPageIndex by remember(initialPage) { mutableIntStateOf(initialPage.coerceIn(0, pages.size - 1)) }
    val currentPath = pages[currentPageIndex]
    val bitmap = rememberBitmapFromAsset(currentPath)

    var pageRotations by remember { mutableStateOf(mapOf<Int, Float>()) }
    val currentRotation = pageRotations[currentPageIndex] ?: 0f

    var scale by remember(currentPageIndex) { mutableFloatStateOf(1f) }
    var offsetX by remember(currentPageIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(currentPageIndex) { mutableFloatStateOf(0f) }

    val onRotateClockwise = {
        val nextRot = (currentRotation + 90f) % 360f
        pageRotations = pageRotations + (currentPageIndex to nextRot)
        offsetX = 0f
        offsetY = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)
            if (android.os.Build.VERSION.SDK_INT >= 30 && window != null) {
                window.attributes = window.attributes.apply { setFitInsetsTypes(0) }
            }
            window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            onDispose { }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0B0F19)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                Surface(
                    color = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HANDWRITTEN NOTES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent,
                                letterSpacing = 0.5.sp
                            )
                            val scopeSubtitle = if (scopedRange != null) {
                                "Page ${currentPageIndex + 1} of ${pages.size} • Topic P.${scopedRange.first}-${scopedRange.last} • $topicTitle"
                            } else {
                                "Page ${currentPageIndex + 1} of ${pages.size} • $topicTitle"
                            }
                            Text(
                                text = scopeSubtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }

                        if (scale > 1.05f || currentRotation != 0f) {
                            TextButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    pageRotations = pageRotations - currentPageIndex
                                }
                            ) {
                                Text("Reset", color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onRotateClockwise,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate 90° Clockwise",
                                tint = if (currentRotation != 0f) PurpleAccent else Color.White
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .pointerInput(currentPageIndex) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.6f, 5f)
                                if (scale > 1.05f || currentRotation != 0f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .pointerInput(currentPageIndex) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Handwritten page ${currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                    rotationZ = currentRotation
                                )
                        )
                    } else {
                        val isOnline = currentPath.startsWith("http://") || currentPath.startsWith("https://")
                        val existsLocally = !isOnline && java.io.File(currentPath.removePrefix("file://")).exists()
                        if (!isOnline && !existsLocally) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "This note page was uploaded from another device\nand is pending cloud sync.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            CircularProgressIndicator(color = PurpleAccent)
                        }
                    }
                }

                Surface(
                    color = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Previous Page",
                                tint = if (currentPageIndex > 0) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${currentPageIndex + 1} / ${pages.size}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentRotation != 0f) PurpleAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, if (currentRotation != 0f) PurpleAccent else Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier.clickable { onRotateClockwise() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RotateRight,
                                        contentDescription = "Rotate",
                                        tint = if (currentRotation != 0f) PurpleAccent else Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentRotation != 0f) "${currentRotation.toInt()}°" else "Rotate",
                                        color = if (currentRotation != 0f) PurpleAccent else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { if (currentPageIndex < pages.size - 1) currentPageIndex++ },
                            enabled = currentPageIndex < pages.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Next Page",
                                tint = if (currentPageIndex < pages.size - 1) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
