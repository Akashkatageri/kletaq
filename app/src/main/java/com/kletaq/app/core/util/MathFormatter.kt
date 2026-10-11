package com.kletaq.app.core.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PurpleAccent

object MathFormatter {

    private val GREEK_AND_SYMBOLS = linkedMapOf(
        "\\emptyset" to "∅",
        "\\cap" to " ∩ ",
        "\\cup" to " ∪ ",
        "\\subset" to " ⊂ ",
        "\\subseteq" to " ⊆ ",
        "\\in" to " ∈ ",
        "\\notin" to " ∉ ",
        "\\dots" to "…",
        "\\cdots" to "…",
        "\\ldots" to "…",
        "\\cdot" to " · ",
        "\\times" to " × ",
        "\\div" to " ÷ ",
        "\\pm" to " ± ",
        "\\mp" to " ∓ ",
        "\\leq" to " ≤ ",
        "\\le" to " ≤ ",
        "\\geq" to " ≥ ",
        "\\ge" to " ≥ ",
        "\\neq" to " ≠ ",
        "\\ne" to " ≠ ",
        "\\approx" to " ≈ ",
        "\\equiv" to " ≡ ",
        "\\to" to " → ",
        "\\infty" to "∞",
        "\\partial" to "∂",
        "\\nabla" to "∇",
        "\\sigma" to "σ",
        "\\mu" to "μ",
        "\\lambda" to "λ",
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\delta" to "δ",
        "\\theta" to "θ",
        "\\pi" to "π",
        "\\phi" to "φ",
        "\\omega" to "ω",
        "\\rho" to "ρ",
        "\\Delta" to "Δ",
        "\\Sigma" to "Σ",
        "\\Omega" to "Ω",
        "\\sqrt" to "√",
        "\\iint" to "∬",
        "\\int" to "∫",
        "\\sum" to "∑"
    )

    private val SUB_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        'i' to 'ᵢ', 'j' to 'ⱼ', 'k' to 'ₖ', 'n' to 'ₙ', 'm' to 'ₘ',
        'x' to 'ₓ', 'y' to 'ᵧ', 'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ',
        '+' to '₊', '-' to '₋'
    )

    private val SUP_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        'n' to 'ⁿ', 'i' to 'ⁱ', '+' to '⁺', '-' to '⁻', '(' to '⁽', ')' to '⁾'
    )

    /**
     * Converts raw LaTeX / mathematical notation into clean, high-readability text
     * with proper Unicode superscripts, subscripts, fractions, and Greek symbols.
     */
    fun formatMathToReadable(raw: String): String {
        if (raw.isBlank()) return ""

        var s = raw
            .replace("\\(", "").replace("\\)", "")
            .replace("\\[", "").replace("\\]", "")
            .replace("\\left", "").replace("\\right", "")

        // 1. Text wrappers: \text{...}, \mathrm{...}, \mathbf{...}
        s = Regex("""\\(?:text|mathrm|mathbf|mathit)\s*\{([^{}]+)\}""").replace(s) { it.groupValues[1] }

        // 2. Extract balanced fractions: \frac{a}{b} -> (a) / (b)
        s = replaceFractionsRecursively(s)

        // 3. Summation limits: \sum_{j=1}^n -> ∑[j=1…n]
        s = Regex("""\\sum_\{([^}]+)\}\^\{([^}]+)\}""").replace(s) { "∑[${it.groupValues[1]}…${it.groupValues[2]}] " }
        s = Regex("""\\sum_\{([^}]+)\}\^([0-9n])""").replace(s) { "∑[${it.groupValues[1]}…${it.groupValues[2]}] " }
        s = Regex("""\\sum_\{([^}]+)\}""").replace(s) { "∑[${it.groupValues[1]}] " }

        // 4. Integrals: \int_{-\infty}^{\infty} -> ∫[-∞ to ∞]
        s = Regex("""\\int_\{([^}]+)\}\^\{([^}]+)\}""").replace(s) { "∫[${it.groupValues[1]} to ${it.groupValues[2]}] " }
        s = Regex("""\\int_\{([^}]+)\}""").replace(s) { "∫[${it.groupValues[1]}] " }

        // 4.5 Handle modular arithmetic commands before \pm replaces
        s = s.replace("\\pmod", " (mod ")
        s = s.replace("\\mod", " mod ")
        s = s.replace("± od{", " (mod ")
        s = s.replace("± od", " (mod ")
        s = s.replace("±od", " (mod ")

        // 5. Replace Greek & Math symbols
        for ((tex, unicode) in GREEK_AND_SYMBOLS) {
            s = s.replace(tex, unicode)
        }

        // 6. Subscripts: _{...} or _char
        s = Regex("""_\{([^{}]+)\}""").replace(s) { m ->
            m.groupValues[1].map { SUB_MAP[it] ?: it }.joinToString("")
        }
        s = Regex("""_([0-9ijknmxy])""").replace(s) { m ->
            val c = m.groupValues[1][0]
            (SUB_MAP[c] ?: c).toString()
        }

        // 7. Superscripts: ^{...} or ^char
        s = Regex("""\^\{([^{}]+)\}""").replace(s) { m ->
            m.groupValues[1].map { SUP_MAP[it] ?: it }.joinToString("")
        }
        s = Regex("""\^([0-9ni+\-])""").replace(s) { m ->
            val c = m.groupValues[1][0]
            (SUP_MAP[c] ?: c).toString()
        }

        // 8. Strip math delimiters: $...$ or $$...$$
        s = Regex("""\$\$([^\$]+)\$\$""").replace(s) { "\n" + it.groupValues[1].trim() + "\n" }
        s = Regex("""\$([^\$]+)\$""").replace(s) { it.groupValues[1] }
        s = s.replace("$", "")

        // 9. Clean up spacing and leftover backslashes
        s = s.replace(Regex("""\\[,;! ]"""), " ")
        s = s.replace("\\", "")
        s = s.replace(Regex("""[ ]{2,}"""), " ")

        return s.trim()
    }

    private fun extractBraces(str: String, startIndex: Int): Pair<String, Int>? {
        if (startIndex >= str.length || str[startIndex] != '{') return null
        var depth = 0
        for (i in startIndex until str.length) {
            if (str[i] == '{') depth++
            else if (str[i] == '}') {
                depth--
                if (depth == 0) {
                    return Pair(str.substring(startIndex + 1, i), i)
                }
            }
        }
        return null
    }

    private fun replaceFractionsRecursively(str: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            if (str.startsWith("\\frac", i)) {
                var numStart = i + 5
                while (numStart < str.length && str[numStart] == ' ') numStart++
                val numResult = extractBraces(str, numStart)
                if (numResult != null) {
                    var denStart = numResult.second + 1
                    while (denStart < str.length && str[denStart] == ' ') denStart++
                    val denResult = extractBraces(str, denStart)
                    if (denResult != null) {
                        val numClean = replaceFractionsRecursively(numResult.first.trim())
                        val denClean = replaceFractionsRecursively(denResult.first.trim())
                        sb.append("($numClean) / ($denClean)")
                        i = denResult.second + 1
                        continue
                    }
                }
            }
            sb.append(str[i])
            i++
        }
        return sb.toString()
    }

    /**
     * Builds an AnnotatedString that highlights bold text, equation blocks,
     * and mathematical symbols.
     */
    fun formatToAnnotated(raw: String, primaryColor: Color = Color(0xFF6366F1)): AnnotatedString {
        val readable = formatMathToReadable(raw)
        return buildAnnotatedString {
            var i = 0
            while (i < readable.length) {
                if (readable.startsWith("**", i)) {
                    val end = readable.indexOf("**", i + 2)
                    if (end >= 0) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(readable.substring(i + 2, end))
                        pop()
                        i = end + 2
                        continue
                    }
                }
                append(readable[i])
                i++
            }
        }
    }
}

/**
 * A drop-in composable that renders mathematical text, exam model answers,
 * and academic concepts with high typographic polish and crisp readability.
 */
@Composable
fun MathFormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null
) {
    val cleanFormatted = remember(text) { MathFormatter.formatToAnnotated(text) }

    Text(
        text = cleanFormatted,
        modifier = modifier,
        style = style,
        color = color,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else 22.sp,
        fontWeight = fontWeight
    )
}
