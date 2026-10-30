package com.kletaq.app.core.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

enum class UpdateUiState {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var uiState by remember { mutableStateOf(UpdateUiState.IDLE) }
    var activeDownloadId by remember { mutableLongStateOf(-1L) }
    var downloadProgress by remember { mutableStateOf<SystemDownloadProgress?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    val formattedSize = remember(updateInfo.sizeBytes) {
        if (updateInfo.sizeBytes > 0) {
            String.format(Locale.US, "%.1f MB", updateInfo.sizeBytes / (1024f * 1024f))
        } else "~32 MB"
    }

    // Polling effect for system DownloadManager progress
    LaunchedEffect(activeDownloadId, uiState) {
        if (uiState == UpdateUiState.DOWNLOADING && activeDownloadId > 0L) {
            while (isActive && uiState == UpdateUiState.DOWNLOADING) {
                val progress = AppUpdateManager.queryDownloadProgress(context, activeDownloadId)
                downloadProgress = progress
                if (progress.isSuccessful) {
                    uiState = UpdateUiState.COMPLETED
                    break
                } else if (progress.isFailed) {
                    uiState = UpdateUiState.ERROR
                    errorMessage = "Download was interrupted or cancelled."
                    break
                }
                delay(350)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (uiState != UpdateUiState.DOWNLOADING) {
                onDismiss()
            } else {
                // If user swipes away while downloading, let download run in background via system DownloadManager
                onDismiss()
            }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (uiState) {
                        UpdateUiState.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.15f)
                        UpdateUiState.ERROR -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        else -> PurpleAccent.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (uiState) {
                                UpdateUiState.COMPLETED -> Icons.Default.CheckCircle
                                UpdateUiState.ERROR -> Icons.Default.ErrorOutline
                                else -> Icons.Default.SystemUpdate
                            },
                            contentDescription = null,
                            tint = when (uiState) {
                                UpdateUiState.COMPLETED -> Color(0xFF10B981)
                                UpdateUiState.ERROR -> Color(0xFFEF4444)
                                else -> PurpleAccent
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = when (uiState) {
                            UpdateUiState.IDLE -> "Update Available! 🚀"
                            UpdateUiState.DOWNLOADING -> "Downloading Update... ⏳"
                            UpdateUiState.COMPLETED -> "Download Complete! 🎉"
                            UpdateUiState.ERROR -> "Download Failed ⚠️"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version ${updateInfo.tag} • $formattedSize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (uiState) {
                UpdateUiState.IDLE -> {
                    // Release Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Release Date",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = updateInfo.releaseDateFormatted.ifBlank { "Recently" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (updateInfo.installedDateFormatted.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Installed Version",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = updateInfo.installedDateFormatted,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (updateInfo.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "WHAT'S NEW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = updateInfo.releaseNotes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Action Buttons: Later & Download Update
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Later")
                        }

                        Button(
                            onClick = {
                                val id = AppUpdateManager.startSystemDownload(
                                    context = context,
                                    downloadUrl = updateInfo.apkDownloadUrl,
                                    versionTag = updateInfo.tag
                                )
                                activeDownloadId = id
                                uiState = UpdateUiState.DOWNLOADING
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                UpdateUiState.DOWNLOADING -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val cur = downloadProgress
                            val currentDownloadedMb = (cur?.bytesDownloaded ?: 0L) / (1024f * 1024f)
                            val currentTotalMb = (cur?.totalBytes ?: updateInfo.sizeBytes) / (1024f * 1024f)
                            val pct = ((cur?.progress ?: 0f) * 100).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentTotalMb > 0f) String.format(Locale.US, "%.1f MB / %.1f MB", currentDownloadedMb, currentTotalMb) else "Connecting...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$pct%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                            }

                            LinearProgressIndicator(
                                progress = { cur?.progress ?: 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = PurpleAccent,
                                trackColor = PurpleAccent.copy(alpha = 0.2f),
                            )

                            Text(
                                text = "🔒 Download managed securely by Android System DownloadManager.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (activeDownloadId > 0L) {
                                    AppUpdateManager.cancelDownload(context, activeDownloadId)
                                }
                                uiState = UpdateUiState.IDLE
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Run in Background", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                UpdateUiState.COMPLETED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.10f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Ready to Install",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "The update has been downloaded to your device. Tap below to install, or tap the system notification in your status bar.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done")
                        }

                        Button(
                            onClick = {
                                if (activeDownloadId > 0L) {
                                    AppUpdateManager.openDownloadedApk(context, activeDownloadId)
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Install Update 🚀", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                UpdateUiState.ERROR -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.10f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Download Error",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = errorMessage.ifBlank { "Could not complete update download." },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                AppUpdateManager.openUpdateInBrowser(context, updateInfo.apkDownloadUrl)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Browser")
                        }

                        Button(
                            onClick = {
                                val id = AppUpdateManager.startSystemDownload(
                                    context = context,
                                    downloadUrl = updateInfo.apkDownloadUrl,
                                    versionTag = updateInfo.tag
                                )
                                activeDownloadId = id
                                uiState = UpdateUiState.DOWNLOADING
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
