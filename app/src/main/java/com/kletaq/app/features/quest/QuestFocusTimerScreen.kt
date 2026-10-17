package com.kletaq.app.features.quest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import com.kletaq.app.features.focus.components.AmbientTimerView
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.features.lesson.components.HandwrittenNotesCard
import com.kletaq.app.features.lesson.components.HandwrittenNotesViewerDialog
import com.kletaq.app.features.quest.components.QuestCompletionDialog
import kotlinx.coroutines.delay

@Composable
fun QuestFocusTimerScreen(
    questionTitle: String,
    studyContent: String = "",
    notesPages: List<String> = emptyList(),
    isCustomUpload: Boolean = false,
    initialMinutes: Int = 30,
    onBackClick: (() -> Unit)? = null,
    onFinish: (isCompletedOnTime: Boolean, isExtendedTime: Boolean, isSkipped: Boolean) -> Unit
) {
    val totalSeconds = remember(initialMinutes) { initialMinutes * 60 }
    var remainingSeconds by remember(initialMinutes) { mutableIntStateOf(totalSeconds) }
    var isRunning by remember(initialMinutes) { mutableStateOf(true) }
    var isExtended by remember(initialMinutes) { mutableStateOf(false) }
    var isAmbientMode by remember { mutableStateOf(false) }

    var targetEndTimeMs by remember(initialMinutes) {
        mutableLongStateOf(System.currentTimeMillis() + (totalSeconds * 1000L))
    }

    var showCompletionPrompt by remember { mutableStateOf(false) }
    var showNotesViewer by remember { mutableStateOf(false) }
    var activeViewerPage by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen awake while studying, release when paused or disposed
    DisposableEffect(isRunning) {
        if (isRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = true
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = false
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = false
        }
    }

    // Lifecycle observer: immediately recalculate timer from wall-clock if screen wakes back up
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isRunning) {
                val diffMs = targetEndTimeMs - System.currentTimeMillis()
                val leftSecs = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                remainingSeconds = leftSecs
                if (leftSecs <= 0) {
                    isRunning = false
                    showCompletionPrompt = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Robust wall-clock timer loop: cannot drift or freeze
    LaunchedEffect(isRunning) {
        if (isRunning) {
            targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
            while (isRunning && remainingSeconds > 0) {
                delay(500L)
                val diffMs = targetEndTimeMs - System.currentTimeMillis()
                val leftSecs = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                remainingSeconds = leftSecs
                if (leftSecs <= 0) {
                    isRunning = false
                    showCompletionPrompt = true
                    break
                }
            }
        }
    }

    if (isAmbientMode) {
        // Battery-optimized OLED pitch black display with minimal brightness
        AmbientTimerView(
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds,
            isRunning = isRunning,
            topicName = questionTitle,
            sessionModeName = "Focus Quest",
            onTogglePlayPause = {
                if (isRunning) {
                    val diffMs = targetEndTimeMs - System.currentTimeMillis()
                    remainingSeconds = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                    isRunning = false
                } else {
                    targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                    isRunning = true
                }
            },
            onWake = {
                isAmbientMode = false
            }
        )
    } else {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val progress = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat() else 1f

        val animatedProgress by animateFloatAsState(targetValue = progress, label = "TimerProgress")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Current lesson title & optional back navigation & Battery Eco Mode button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PurpleAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "FOCUS MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurpleAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Battery Saver / Eco Mode Toggle Button
                    IconButton(
                        onClick = { isAmbientMode = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Battery Saver Eco Mode",
                            tint = PurpleAccent
                        )
                    }
                }

            Text(
                text = questionTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            // Parkinson's Law Timer status badge (2x Bonus XP)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (!isExtended) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (!isExtended) Color(0xFF10B981).copy(alpha = 0.4f) else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isExtended) "⚡ Parkinson's Timer: 2x XP if finished on time!" else "⏱ Standard XP (Time extended)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isExtended) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Compact timer with Play/Pause on left, Timer in center, and "+5 min" on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause toggle
                Surface(
                    onClick = {
                        if (isRunning) {
                            val diffMs = targetEndTimeMs - System.currentTimeMillis()
                            remainingSeconds = ((diffMs + 999L) / 1000L).coerceAtLeast(0L).toInt()
                            isRunning = false
                        } else {
                            targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                            isRunning = true
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Circular countdown timer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(136.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )

                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (!isExtended) PurpleAccent else Color(0xFFF59E0B),
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = if (isRunning) "FOCUSING..." else "PAUSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                // "+5m" / Need More Time button RIGHT NEXT TO THE TIMER
                Surface(
                    onClick = {
                        remainingSeconds += 5 * 60
                        targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                        isExtended = true
                        isRunning = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = PurpleAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.2.dp, PurpleAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.size(width = 56.dp, height = 52.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "+5m",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleAccent
                        )
                        Text(
                            text = "More",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurpleAccent
                        )
                    }
                }
            }

            // Prominently display uploaded/module handwritten notes while timer runs
            if (notesPages.isNotEmpty()) {
                HandwrittenNotesCard(
                    pages = notesPages,
                    onOpenPage = { page ->
                        activeViewerPage = page
                        showNotesViewer = true
                    },
                    isCustomUpload = isCustomUpload
                )
            }

            // 3. Bottom CTA: MARK COMPLETE Button
            Button(
                onClick = {
                    isRunning = false
                    showCompletionPrompt = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 28.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!isExtended) "MARK COMPLETE (2X XP ⚡)" else "MARK COMPLETE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        if (showNotesViewer && notesPages.isNotEmpty()) {
            HandwrittenNotesViewerDialog(
                pages = notesPages,
                initialPage = activeViewerPage,
                topicTitle = questionTitle,
                onDismiss = { showNotesViewer = false }
            )
        }

        if (showCompletionPrompt) {
            QuestCompletionDialog(
                questionTitle = questionTitle,
                isParkinsonBonus = !isExtended,
                onCompleted = {
                    showCompletionPrompt = false
                    onFinish(!isExtended, isExtended, false)
                },
                onDismiss = {
                    showCompletionPrompt = false
                    targetEndTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)
                    isRunning = true
                },
                onSkip = {
                    showCompletionPrompt = false
                    onFinish(false, false, true)
                }
            )
        }
    }
    }
}
