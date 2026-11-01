package com.kletaq.app.features.memorypalace.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.memorypalace.data.MemoryPalaceRepository
import com.kletaq.app.features.memorypalace.model.MemoryPalace
import com.kletaq.app.features.memorypalace.model.PalaceImage
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * 📍 Interactive Pin-Drop Palace Creation Screen
 * Allows students to snap room photos and tap objects to drop sequential numbered pins (Loci).
 */
@Composable
fun PalacePinDropScreen(
    onPalaceCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var palaceName by remember { mutableStateOf("") }
    var palaceDesc by remember { mutableStateOf("") }

    // List of room photos (local file path or uri)
    val roomBitmaps = remember { mutableStateListOf<Pair<String, Bitmap>>() }
    var selectedImageIndex by remember { mutableStateOf(0) }

    // List of placed pins
    val lociPins = remember { mutableStateListOf<PalaceLocus>() }

    // Dialog state for naming a dropped pin
    var pendingTapOffset by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var pendingLocusName by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadAndScaleBitmap(context, uri)
            if (bitmap != null) {
                val imageId = UUID.randomUUID().toString()
                roomBitmaps.add(Pair(imageId, bitmap))
                selectedImageIndex = roomBitmaps.size - 1
            } else {
                Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Create Memory Palace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Palace Name Field
            OutlinedTextField(
                value = palaceName,
                onValueChange = { palaceName = it },
                label = { Text("Palace Name (e.g. My Bedroom, Hostel 204)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Room Photo Section
            if (roomBitmaps.isEmpty()) {
                // Empty photo card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Add Room Photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Snap or Select Room Photo",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Take a wide shot of your room to drop loci pins",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Image Carousel if multiple images
                if (roomBitmaps.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(roomBitmaps) { idx, item ->
                            val isSelected = idx == selectedImageIndex
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedImageIndex = idx }
                            ) {
                                Image(
                                    bitmap = item.second.asImageBitmap(),
                                    contentDescription = "Room $idx",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Interactive Pin-Drop Canvas
                val currentImageId = roomBitmaps[selectedImageIndex].first
                val currentBitmap = roomBitmaps[selectedImageIndex].second
                val imageAspectRatio = remember(currentBitmap) {
                    if (currentBitmap.height > 0) currentBitmap.width.toFloat() / currentBitmap.height.toFloat() else (4f / 3f)
                }

                Text(
                    text = "Tap any object below to drop a numbered pin:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageAspectRatio)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()
                    val pinRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedImageIndex) {
                                detectTapGestures { offset ->
                                    val xPct = (offset.x / containerWidth).coerceIn(0f, 1f)
                                    val yPct = (offset.y / containerHeight).coerceIn(0f, 1f)
                                    pendingTapOffset = Pair(xPct, yPct)
                                    pendingLocusName = "Spot #${lociPins.size + 1}"
                                }
                            }
                    ) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Interactive Room",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Render pins placed on this image
                        val currentPins = lociPins.filter { it.imageId == currentImageId }
                        currentPins.forEach { pin ->
                            val pinX = (pin.xPercent * containerWidth) - pinRadiusPx
                            val pinY = (pin.yPercent * containerHeight) - pinRadiusPx

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(pinX.toInt(), pinY.toInt()) }
                                    .size(32.dp)
                                    .background(Color(0xFF2563EB), CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pin.orderIndex.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Add Another Photo Button
                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Another Room Photo / Angle")
                }
            }

            // Pinned Loci List
            if (lociPins.isNotEmpty()) {
                Text(
                    text = "Pinned Loci in Order (${lociPins.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                lociPins.sortedBy { it.orderIndex }.forEach { pin ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF2563EB), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pin.orderIndex.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = pin.name,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    lociPins.remove(pin)
                                    // Re-index remaining pins
                                    val reindexed = lociPins.mapIndexed { idx, p ->
                                        p.copy(orderIndex = idx + 1)
                                    }
                                    lociPins.clear()
                                    lociPins.addAll(reindexed)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Pin",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save Palace Button
            Button(
                onClick = {
                    if (palaceName.isBlank()) {
                        Toast.makeText(context, "Please enter a Palace name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (lociPins.size < 2) {
                        Toast.makeText(context, "Please drop at least 2 loci pins on your room", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Save images to disk
                    val palaceId = UUID.randomUUID().toString()
                    val imagesDir = File(context.filesDir, "palace_images").apply { mkdirs() }
                    val savedImages = mutableListOf<PalaceImage>()

                    roomBitmaps.forEachIndexed { idx, pair ->
                        val file = File(imagesDir, "${pair.first}.jpg")
                        FileOutputStream(file).use { out ->
                            pair.second.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        }
                        savedImages.add(
                            PalaceImage(
                                id = pair.first,
                                palaceId = palaceId,
                                imageOrder = idx + 1,
                                photoPath = file.absolutePath
                            )
                        )
                    }

                    // Remap loci palaceId
                    val remappedLoci = lociPins.map { it.copy(palaceId = palaceId) }

                    val palace = MemoryPalace(
                        id = palaceId,
                        name = palaceName.trim(),
                        description = palaceDesc.trim().takeIf { it.isNotBlank() },
                        totalLociCount = remappedLoci.size
                    )

                    MemoryPalaceRepository.savePalaceWithPins(palace, savedImages, remappedLoci)
                    Toast.makeText(context, "Palace created with ${remappedLoci.size} loci!", Toast.LENGTH_SHORT).show()
                    onPalaceCreated(palaceId)
                },
                enabled = palaceName.isNotBlank() && lociPins.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Memory Palace", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Name Pin Dialog
    if (pendingTapOffset != null) {
        val nextPinNumber = lociPins.size + 1
        AlertDialog(
            onDismissRequest = { pendingTapOffset = null },
            title = { Text("Label Spot #$nextPinNumber") },
            text = {
                Column {
                    Text(
                        "What is this landmark or object?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pendingLocusName,
                        onValueChange = { pendingLocusName = it },
                        placeholder = { Text("e.g. Desk Lamp, Sofa, Door") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val offset = pendingTapOffset
                        if (offset != null && roomBitmaps.isNotEmpty()) {
                            val currentImageId = roomBitmaps[selectedImageIndex].first
                            lociPins.add(
                                PalaceLocus(
                                    id = UUID.randomUUID().toString(),
                                    imageId = currentImageId,
                                    palaceId = "",
                                    orderIndex = nextPinNumber,
                                    name = pendingLocusName.trim().ifBlank { "Spot #$nextPinNumber" },
                                    xPercent = offset.first,
                                    yPercent = offset.second
                                )
                            )
                        }
                        pendingTapOffset = null
                    }
                ) {
                    Text("Add Pin", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTapOffset = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun loadAndScaleBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val stream: InputStream? = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(stream)
        stream?.close()
        if (original != null) {
            val maxDim = Math.max(original.width, original.height)
            if (maxDim > 1080) {
                val scale = 1080f / maxDim
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                )
            } else {
                original
            }
        } else null
    } catch (e: Exception) {
        null
    }
}

