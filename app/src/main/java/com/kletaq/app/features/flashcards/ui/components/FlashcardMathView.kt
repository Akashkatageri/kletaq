package com.kletaq.app.features.flashcards.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.kletaq.app.features.chat.mathToUnicode

@Composable
fun FlashcardMathView(
    rawText: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val renderedText = remember(rawText) {
        if (containsMathSyntax(rawText)) {
            mathToUnicode(rawText)
        } else {
            rawText
        }
    }

    val isMathHeavy = remember(rawText) { containsMathSyntax(rawText) }

    Text(
        text = renderedText,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        lineHeight = lineHeight,
        fontFamily = if (isMathHeavy) FontFamily.Default else FontFamily.Default,
        modifier = modifier.fillMaxWidth()
    )
}

private fun containsMathSyntax(text: String): Boolean {
    return text.contains("$") ||
            text.contains("\\(") ||
            text.contains("\\[") ||
            text.contains("\\frac") ||
            text.contains("\\sqrt") ||
            text.contains("\\partial") ||
            text.contains("\\sum") ||
            text.contains("\\int")
}

