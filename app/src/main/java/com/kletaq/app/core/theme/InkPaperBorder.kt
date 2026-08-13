package com.kletaq.app.core.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object InkPaperBorder {
    // Heavy (1.5dp, 16dp radius): Main cards, lesson cards, profile cards, achievement cards, bottom sheets
    val HeavyStrokeWidth = 1.5.dp
    val HeavyRadius = 16.dp
    val HeavyShape = RoundedCornerShape(HeavyRadius)

    // Medium (1.0dp, 12dp radius): Search bars, input fields, task rows, settings items
    val MediumStrokeWidth = 1.0.dp
    val MediumRadius = 12.dp
    val MediumShape = RoundedCornerShape(MediumRadius)

    // Light (0.75dp): Separators, chips, badges, progress containers
    val LightStrokeWidth = 0.75.dp

    // Dialogs (20dp radius)
    val DialogRadius = 20.dp
    val DialogShape = RoundedCornerShape(DialogRadius)

    val ColorLight = Color(0xFF27272A)
    val ColorDark = Color(0xFFE4E4E7)

    @Composable
    fun heavyBorder(): BorderStroke = BorderStroke(HeavyStrokeWidth, BorderColor)

    @Composable
    fun mediumBorder(): BorderStroke = BorderStroke(MediumStrokeWidth, BorderColor)

    @Composable
    fun lightBorder(): BorderStroke = BorderStroke(LightStrokeWidth, BorderColor)
}
