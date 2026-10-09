package com.kletaq.app.features.focus.components

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent

/**
 * Ultra-low-power OLED / AMOLED Ambient Sleep Display for Focus Timer.
 * - Pitch black (#000000) turns off 100% of OLED subpixels.
 * - Dynamically dims screen brightness to minimum (0.01f).
 * - Subtle anti-burn-in micro-offset shifts every minute.
 * - Tap anywhere to wake back to full timer controls.
 */
@Composable
fun AmbientTimerView(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    topicName: String?,
    sessionModeName: String,
    onTogglePlayPause: () -> Unit,
    onWake: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Dim screen brightness to minimum for maximal battery conservation
    DisposableEffect(Unit) {
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = 0.01f
            win.attributes = lp
        }

        onDispose {
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = originalBrightness
                win.attributes = lp
            }
        }
    }

    // Intercept back button to wake rather than exit screen immediately
    BackHandler {
        onWake()
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    // Anti-burn-in drift: every minute shift pixels slightly
    val driftStep = (remainingSeconds / 60) % 5
    val driftX = when (driftStep) {
        0 -> 0.dp
        1 -> 4.dp
        2 -> (-4).dp
        3 -> 2.dp
        else -> (-2).dp
    }
    val driftY = when (driftStep) {
        0 -> 0.dp
        1 -> (-4).dp
        2 -> 4.dp
        3 -> (-2).dp
        else -> 2.dp
    }

    val progress = if (totalSeconds > 0) {
        ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // True OLED #000000
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onWake()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .offset(x = driftX, y = driftY)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mode & Status pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isRunning) Color(0xFF10B981) else Color(0xFFF59E0B),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "FOCUSING • $sessionModeName" else "PAUSED",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Topic name if set
            if (!topicName.isNullOrBlank()) {
                Text(
                    text = topicName,
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Big Minimalist Ambient Digital Timer
            Text(
                text = timeFormatted,
                fontSize = 76.sp,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = FontFamily.Monospace,
                color = if (isRunning) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                letterSpacing = (-2).sp
            )

            // Progress percentage
            Text(
                text = "${(progress * 100).toInt()}% complete",
                color = Color(0xFF475569),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Play/Pause tap button inside Ambient mode
            Surface(
                shape = CircleShape,
                color = Color(0xFF18181B),
                modifier = Modifier
                    .size(54.dp)
                    .clickable {
                        onTogglePlayPause()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Play",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Subtle Hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OLED Sleep Saver • Tap anywhere to wake",
                    color = Color(0xFF334155),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
