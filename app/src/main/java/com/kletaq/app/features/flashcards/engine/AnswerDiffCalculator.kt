package com.kletaq.app.features.flashcards.engine

data class DiffToken(
    val char: Char,
    val type: DiffType
)

enum class DiffType {
    MATCH,   // Correct character
    EXTRA,   // User typed this, but not in correct answer
    MISSING  // In correct answer, but user omitted it
}

data class AnswerDiffResult(
    val tokens: List<DiffToken>,
    val isExactMatch: Boolean,
    val accuracy: Float,
    val expectedText: String,
    val typedText: String
)

object AnswerDiffCalculator {

    fun computeDiff(typed: String, expected: String): AnswerDiffResult {
        val t = typed.trim()
        val e = expected.trim()

        if (t.equals(e, ignoreCase = true)) {
            return AnswerDiffResult(
                tokens = e.map { DiffToken(it, DiffType.MATCH) },
                isExactMatch = true,
                accuracy = 1.0f,
                expectedText = e,
                typedText = t
            )
        }

        // Longest Common Subsequence DP table
        val m = t.length
        val n = e.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0 until m) {
            for (j in 0 until n) {
                if (t[i].equals(e[j], ignoreCase = true)) {
                    dp[i + 1][j + 1] = dp[i][j] + 1
                } else {
                    dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        // Backtrack to create diff tokens
        val tokens = mutableListOf<DiffToken>()
        var i = m
        var j = n

        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && t[i - 1].equals(e[j - 1], ignoreCase = true) -> {
                    tokens.add(0, DiffToken(e[j - 1], DiffType.MATCH))
                    i--
                    j--
                }
                j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                    tokens.add(0, DiffToken(e[j - 1], DiffType.MISSING))
                    j--
                }
                i > 0 -> {
                    tokens.add(0, DiffToken(t[i - 1], DiffType.EXTRA))
                    i--
                }
            }
        }

        val matchCount = tokens.count { it.type == DiffType.MATCH }
        val maxLen = maxOf(1, maxOf(m, n))
        val accuracy = (matchCount.toFloat() / maxLen).coerceIn(0.0f, 1.0f)

        return AnswerDiffResult(
            tokens = tokens,
            isExactMatch = false,
            accuracy = accuracy,
            expectedText = e,
            typedText = t
        )
    }
}

