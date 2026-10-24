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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
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
fun HandwrittenNotesCard(
    pages: List<String>,
    onOpenPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onUploadNotes: (() -> Unit)? = null,
    onDeleteNotes: (() -> Unit)? = null,
    isCustomUpload: Boolean = false
) {
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
                        text = "Upload PDF notes or camera photos. Compressed on-device and stored online in Cloudflare R2.",
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
                                text = "☁️ Synced to Cloudflare R2",
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
                            text = "${pages.size} Pages",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Scanned classroom lecture notes & worked derivations. Tap any page to zoom in.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(pages) { index, pagePath ->
                    val bitmap = rememberBitmapFromAsset(pagePath)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .width(110.dp)
                            .height(155.dp)
                            .clickable { onOpenPage(index) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
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

                            Surface(
                                shape = RoundedCornerShape(topStart = 8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = "P.${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenPage(0) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PurpleAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "View Fullscreen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
}

@Composable
fun HandwrittenNotesViewerDialog(
    pages: List<String>,
    initialPage: Int,
    topicTitle: String,
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
                            Text(
                                text = "Page ${currentPageIndex + 1} of ${pages.size} • $topicTitle",
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
                        CircularProgressIndicator(color = PurpleAccent)
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
