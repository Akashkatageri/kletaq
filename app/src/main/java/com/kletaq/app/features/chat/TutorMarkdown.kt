package com.kletaq.app.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun TutorMarkdown(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.replace("\r\n", "\n").lines()
    var inCode = false
    val code = StringBuilder()
    SelectionContainer {
        Column(modifier.fillMaxWidth()) {
            lines.forEach { raw ->
                val line = raw.trimEnd()
                if (line.trimStart().startsWith("```")) {
                    if (inCode) {
                        Text(
                            text = code.toString().trimEnd(),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                        code.clear()
                    }
                    inCode = !inCode
                } else if (inCode) {
                    code.appendLine(line)
                } else {
                    MarkdownLine(line)
                }
            }
            if (code.isNotEmpty()) {
                Text(code.toString().trimEnd(), Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            }
        }
    }
}

@Composable
private fun MarkdownLine(line: String) {
    val trimmed = line.trim()
    when {
        trimmed.isEmpty() -> Spacer(Modifier.height(6.dp))
        trimmed == "---" -> Spacer(Modifier.height(8.dp))
        trimmed.startsWith("### ") -> Text(inlineMarkdown(trimmed.removePrefix("### ")), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
        trimmed.startsWith("## ") -> Text(inlineMarkdown(trimmed.removePrefix("## ")), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        trimmed.startsWith("# ") -> Text(inlineMarkdown(trimmed.removePrefix("# ")), style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Row(Modifier.padding(vertical = 2.dp)) {
            Text("•", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(inlineMarkdown(trimmed.drop(2)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        NumberedListRegex.matches(trimmed) -> {
            val marker = trimmed.substringBefore(' ') + " "
            Row(Modifier.padding(vertical = 2.dp)) {
                Text(marker, fontWeight = FontWeight.SemiBold)
                Text(inlineMarkdown(trimmed.removePrefix(marker)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
        else -> Text(inlineMarkdown(trimmed), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 2.dp))
    }
}

private val NumberedListRegex = Regex("""^\d+[.)]\s+.*""")
private val FractionsRegex = Regex("""\\frac\s*\{([^{}]+)\}\s*\{([^{}]+)\}""")
private val SqrtRegex = Regex("""\\sqrt\s*\{([^{}]+)\}""")
private val TextCommandRegex = Regex("""\\(?:text|mathrm|mathbf)\s*\{([^{}]+)\}""")
private val CaretBraceRegex = Regex("""\^\{([^{}]+)\}""")
private val UnderscoreBraceRegex = Regex("""_\{([^{}]+)\}""")
private val CaretCharRegex = Regex("""\^([0-9+\-=()n])""")
private val UnderscoreCharRegex = Regex("""_([0-9+\-=()])""")
private val MathDelimRegex = Regex("""\$([^$]+)\$""")
private val SpacingRegex = Regex("""\\[,;! ]""")

private val MathSymbols = linkedMapOf(
    "\\partial" to "∂", "\\nabla" to "∇", "\\infty" to "∞",
    "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
    "\\theta" to "θ", "\\lambda" to "λ", "\\mu" to "μ", "\\pi" to "π",
    "\\rho" to "ρ", "\\sigma" to "σ", "\\phi" to "φ", "\\omega" to "ω",
    "\\Delta" to "Δ", "\\Sigma" to "Σ", "\\Omega" to "Ω",
    "\\times" to "×", "\\cdot" to "·", "\\div" to "÷", "\\pm" to "±",
    "\\leq" to "≤", "\\le" to "≤", "\\geq" to "≥", "\\ge" to "≥",
    "\\neq" to "≠", "\\approx" to "≈", "\\equiv" to "≡", "\\to" to "→",
    "\\sum" to "∑", "\\int" to "∫", "\\lim" to "lim", "\\in" to "∈"
)

private fun inlineMarkdown(rawSource: String): AnnotatedString = buildAnnotatedString {
    val source = mathToUnicode(rawSource)
    var index = 0
    while (index < source.length) {
        when {
            source.startsWith("**", index) -> {
                val end = source.indexOf("**", index + 2)
                if (end >= 0) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(source.substring(index + 2, end)); pop(); index = end + 2
                } else { append("**"); index += 2 }
            }
            source[index] == '`' -> {
                val end = source.indexOf('`', index + 1)
                if (end >= 0) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = androidx.compose.ui.graphics.Color.Black.copy(alpha = .08f)))
                    append(source.substring(index + 1, end)); pop(); index = end + 1
                } else { append('`'); index++ }
            }
            else -> { append(source[index]); index++ }
        }
    }
}

/** Makes the common TeX emitted by AI readable without exposing raw commands to students. */
internal fun mathToUnicode(input: String): String {
    var value = input
        .replace("\\(", "").replace("\\)", "")
        .replace("\\[", "").replace("\\]", "")
        .replace("\\left", "").replace("\\right", "")
        .replace("\\$", "$")

    repeat(4) { value = FractionsRegex.replace(value) { "(${it.groupValues[1]})⁄(${it.groupValues[2]})" } }
    value = SqrtRegex.replace(value) { "√(${it.groupValues[1]})" }
    value = TextCommandRegex.replace(value) { it.groupValues[1] }

    MathSymbols.forEach { (latex, symbol) -> value = value.replace(latex, symbol) }

    value = CaretBraceRegex.replace(value) { toScript(it.groupValues[1], superscript = true) }
    value = UnderscoreBraceRegex.replace(value) { toScript(it.groupValues[1], superscript = false) }
    value = CaretCharRegex.replace(value) { toScript(it.groupValues[1], superscript = true) }
    value = UnderscoreCharRegex.replace(value) { toScript(it.groupValues[1], superscript = false) }
    value = MathDelimRegex.replace(value) { it.groupValues[1] }
    return value.replace("$$", "").replace(SpacingRegex, " ").trim()
}

private fun toScript(text: String, superscript: Boolean): String {
    val normal = "0123456789+-=()n"
    val script = if (superscript) "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁿ" else "₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎n"
    val converted = text.map { character -> normal.indexOf(character).takeIf { it >= 0 }?.let(script::get) ?: character }.joinToString("")
    return if (converted == text && text.length > 1) (if (superscript) "^($text)" else "_($text)") else converted
}
