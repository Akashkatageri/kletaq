package com.kletaq.app.features.memorypalace.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.core.theme.WarningAmber
import com.kletaq.app.features.memorypalace.data.MemoryPalaceRepository
import com.kletaq.app.features.memorypalace.model.MemoryPalace
import com.kletaq.app.features.memorypalace.model.PalaceDerivation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalaceHubScreen(
    onBackClick: () -> Unit,
    onCreatePalaceClick: () -> Unit,
    onCreateDerivationClick: (palaceId: String?) -> Unit,
    onStartWalkClick: (derivationId: String) -> Unit
) {
    val palaces by MemoryPalaceRepository.palaces.collectAsState()
    val derivations by MemoryPalaceRepository.derivations.collectAsState()

    var palaceToDelete by remember { mutableStateOf<MemoryPalace?>(null) }
    var derivationToDelete by remember { mutableStateOf<PalaceDerivation?>(null) }

    val dueDerivations = remember(derivations) {
        MemoryPalaceRepository.getDueDerivations()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (palaces.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onCreateDerivationClick(palaces.firstOrNull()?.id) },
                    containerColor = PrimaryAccentColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Anchor Derivation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Memory Palace",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Method of Loci • Spatial Recall",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onCreatePalaceClick) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = "New Palace Room",
                        tint = PrimaryAccentColor
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Intro Card
                item(key = "intro_banner") {
                    IntroBannerCard()
                }

                // Section 1: Due for Walk & Recall (Urgency Queue)
                if (dueDerivations.isNotEmpty()) {
                    item(key = "due_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "🔥 Ready for Recall Walk",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = WarningAmber.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = dueDerivations.size.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(dueDerivations, key = { "due_${it.id}" }) { deriv ->
                        val palace = palaces.firstOrNull { it.id == deriv.palaceId }
                        DueDerivationCard(
                            derivation = deriv,
                            palaceName = palace?.name ?: "Memory Palace",
                            onStartWalk = { onStartWalkClick(deriv.id) }
                        )
                    }
                }

                // Section 2: Your Memory Palaces
                item(key = "palaces_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Palaces (${palaces.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        TextButton(onClick = onCreatePalaceClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryAccentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Room",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryAccentColor
                            )
                        }
                    }
                }

                if (palaces.isEmpty()) {
                    item(key = "empty_palaces") {
                        EmptyPalacesCard(onCreatePalace = onCreatePalaceClick)
                    }
                } else {
                    items(palaces, key = { "palace_${it.id}" }) { palace ->
                        val palaceData = remember(palace.id) {
                            MemoryPalaceRepository.getPalaceWithLoci(palace.id)
                        }
                        val derivationCount = remember(derivations, palace.id) {
                            derivations.count { it.palaceId == palace.id }
                        }

                        PalaceItemCard(
                            palace = palace,
                            lociCount = palaceData?.loci?.size ?: 0,
                            derivationCount = derivationCount,
                            firstImagePath = palaceData?.images?.firstOrNull()?.photoPath,
                            onAddDerivation = { onCreateDerivationClick(palace.id) },
                            onDelete = { palaceToDelete = palace }
                        )
                    }
                }

                // Section 3: All Anchored Derivations
                item(key = "derivations_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Anchored Derivations (${derivations.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (palaces.isNotEmpty()) {
                            TextButton(onClick = { onCreateDerivationClick(palaces.firstOrNull()?.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryAccentColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Anchor",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryAccentColor
                                )
                            }
                        }
                    }
                }

                if (derivations.isEmpty()) {
                    item(key = "empty_derivations") {
                        EmptyDerivationsCard(
                            hasPalaces = palaces.isNotEmpty(),
                            onCreatePalace = onCreatePalaceClick,
                            onAddDerivation = { onCreateDerivationClick(palaces.firstOrNull()?.id) }
                        )
                    }
                } else {
                    items(derivations, key = { "deriv_${it.id}" }) { deriv ->
                        val palace = palaces.firstOrNull { it.id == deriv.palaceId }
                        DerivationItemCard(
                            derivation = deriv,
                            palaceName = palace?.name ?: "Memory Palace",
                            onStartWalk = { onStartWalkClick(deriv.id) },
                            onDelete = { derivationToDelete = deriv }
                        )
                    }
                }
            }
        }
    }

    // Delete Palace Confirmation Dialog
    palaceToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { palaceToDelete = null },
            title = { Text("Delete Palace Room?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${p.name}'? All pinned loci and associated derivations will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        MemoryPalaceRepository.deletePalace(p.id)
                        palaceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { palaceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Derivation Confirmation Dialog
    derivationToDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { derivationToDelete = null },
            title = { Text("Delete Derivation?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${d.title}' from your memory palace?") },
            confirmButton = {
                Button(
                    onClick = {
                        MemoryPalaceRepository.deleteDerivation(d.id)
                        derivationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { derivationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Subcomponents
// -------------------------------------------------------------------------

@Composable
private fun IntroBannerCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryAccentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PrimaryAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Mind Palace (Method of Loci)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Spatial Mnemonics for Total Recall",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Map complex concepts, multi-step answers, formulas, or key lists to physical objects in your familiar room. Walk your palace mentally during exams for effortless sequential recall.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun DueDerivationCard(
    derivation: PalaceDerivation,
    palaceName: String,
    onStartWalk: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.5.dp, WarningAmber.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (derivation.mode == "EXAM_SPRINT") WarningAmber.copy(alpha = 0.15f) else PrimaryAccentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (derivation.mode == "EXAM_SPRINT") "TOMORROW'S EXAM" else "SEMESTER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (derivation.mode == "EXAM_SPRINT") WarningAmber else PrimaryAccentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Pass ${derivation.reviewStage + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = derivation.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = palaceName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartWalk,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Walk & Recall",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PalaceItemCard(
    palace: MemoryPalace,
    lociCount: Int,
    derivationCount: Int,
    firstImagePath: String?,
    onAddDerivation: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(firstImagePath) {
        firstImagePath?.let { path ->
            val f = File(path)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Room Thumbnail or Placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = palace.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = palace.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryAccentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$lociCount Loci Pins",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryAccentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "• $derivationCount anchored",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Room",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onAddDerivation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, PrimaryAccentColor.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PrimaryAccentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Anchor Derivation Here",
                    color = PrimaryAccentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DerivationItemCard(
    derivation: PalaceDerivation,
    palaceName: String,
    onStartWalk: () -> Unit,
    onDelete: () -> Unit
) {
    val isDue = derivation.nextReviewTime <= System.currentTimeMillis()
    val formattedNext = remember(derivation.nextReviewTime) {
        if (derivation.nextReviewTime == Long.MAX_VALUE) {
            "Mastered 🎓"
        } else if (isDue) {
            "Due Now"
        } else {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            "Next: " + sdf.format(Date(derivation.nextReviewTime))
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, if (isDue) WarningAmber.copy(alpha = 0.6f) else BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = derivation.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Derivation",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = palaceName,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = formattedNext,
                    fontSize = 11.sp,
                    fontWeight = if (isDue) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDue) WarningAmber else TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onStartWalk,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDue) WarningAmber else PrimaryAccentColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Walk Palace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun EmptyPalacesCard(onCreatePalace: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MeetingRoom,
                contentDescription = null,
                tint = PrimaryAccentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No Memory Palaces Yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Snap a wide photo of your bedroom, hostel desk, or living room and tap objects to set up 4 to 12 recall loci.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreatePalace,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentColor)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Room Palace", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyDerivationsCard(
    hasPalaces: Boolean,
    onCreatePalace: () -> Unit,
    onAddDerivation: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Derivations Anchored",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Upload photos, documents, or notes for any subject or topic. The AI unbundles key points into vivid, memorable loci scenes.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (hasPalaces) {
                Button(
                    onClick = onAddDerivation,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentColor)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Anchor a Derivation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onCreatePalace,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create Room First", fontSize = 13.sp)
                }
            }
        }
    }
}

