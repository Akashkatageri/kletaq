package com.studyos.app.core.util

object UsernameValidator {

    private val allowedPattern = Regex("^[a-zA-Z0-9_]+$")
    private val consecutiveUnderscoresPattern = Regex("_{2,}")

    fun isValidUsername(username: String): Boolean {
        if (username.length < 3 || username.length > 20) return false
        if (!allowedPattern.matches(username)) return false
        if (username.startsWith("_") || username.endsWith("_")) return false
        if (consecutiveUnderscoresPattern.containsMatchIn(username)) return false
        if (containsBlockedWord(username)) return false
        return true
    }

    fun getValidationError(username: String): String? {
        if (username.length < 3) return "Username is too short."
        if (username.length > 20) return "Username must be 20 characters or less."
        if (!allowedPattern.matches(username)) return "Only letters, numbers, and underscores are allowed."
        if (username.startsWith("_") || username.endsWith("_")) return "Only letters, numbers, and underscores are allowed."
        if (consecutiveUnderscoresPattern.containsMatchIn(username)) return "Only letters, numbers, and underscores are allowed."
        if (containsBlockedWord(username)) return "Username contains inappropriate language."
        return null
    }

    fun normalizeUsername(username: String): String {
        return username
            .lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace(".", "")
            .replace("0", "o")
            .replace("1", "i")
            .replace("3", "e")
            .replace("4", "a")
            .replace("5", "s")
            .replace("7", "t")
    }

    fun containsBlockedWord(username: String): Boolean {
        val normalized = normalizeUsername(username)
        for (word in BlockedWords.words) {
            val normalizedBlockedWord = normalizeUsername(word)
            if (normalized.contains(normalizedBlockedWord)) {
                return true
            }
        }
        return false
    }
}
