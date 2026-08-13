package com.kletaq.app.features.focus.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomDurationSheet(
    initialMinutes: Int = 30,
    onDismiss: () -> Unit,
    onDurationSelected: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }
    val presets = listOf(15, 25, 35, 45, 60, 90, 120, 180)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        try {
            sheetState.expand()
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Choose Focus Duration ⏱️",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Select a preset or customize between 5 to 180 minutes",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Display current selected duration
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PurpleAccent.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedMinutes",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                    Text(
                        text = " minutes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleAccent,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Preset Pills
            Text(
                text = "PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presets.forEach { preset ->
                    val isSelected = selectedMinutes == preset
                    Surface(
                        modifier = Modifier.clickable { selectedMinutes = preset },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isSelected) BorderStroke(1.dp, PurpleAccent) else null
                    ) {
                        Text(
                            text = "${preset} min",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Slider for Custom Range (5 min - 180 min)
            Text(
                text = "CUSTOM SLIDER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleAccent
            )

            Slider(
                value = selectedMinutes.toFloat(),
                onValueChange = { selectedMinutes = it.toInt() },
                valueRange = 5f..180f,
                steps = 34,
                colors = SliderDefaults.colors(
                    thumbColor = PurpleAccent,
                    activeTrackColor = PurpleAccent,
                    inactiveTrackColor = PurpleAccent.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    onDurationSelected(selectedMinutes)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleAccent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Apply Duration ($selectedMinutes min)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
