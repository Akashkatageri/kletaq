package com.kletaq.app.features.flashcards.engine

object ClozeParser {

    private val CLOZE_REGEX = Regex("""\{\{c(\d+)::([^:}]+)(?:::([^}]+))?\}\}""")

    fun hasValidCloze(text: String): Boolean {
        return CLOZE_REGEX.containsMatchIn(text)
    }

    fun extractClozeNumbers(text: String): List<Int> {
        return CLOZE_REGEX.findAll(text)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .distinct()
            .sorted()
            .toList()
    }

    fun renderQuestion(text: String, targetIndex: Int = 1): String {
        return CLOZE_REGEX.replace(text) { matchResult ->
            val index = matchResult.groupValues[1].toIntOrNull() ?: 1
            val answer = matchResult.groupValues[2]
            val hint = matchResult.groupValues[3].ifBlank { null }

            if (index == targetIndex) {
                if (hint != null) "[$hint]" else "[...]"
            } else {
                answer
            }
        }
    }

    fun renderAnswer(text: String, targetIndex: Int = 1): String {
        return CLOZE_REGEX.replace(text) { matchResult ->
            val index = matchResult.groupValues[1].toIntOrNull() ?: 1
            val answer = matchResult.groupValues[2]

            if (index == targetIndex) {
                "[$answer]"
            } else {
                answer
            }
        }
    }

    fun extractClozeAnswer(text: String, targetIndex: Int = 1): String {
        val match = CLOZE_REGEX.findAll(text).firstOrNull {
            it.groupValues[1].toIntOrNull() == targetIndex
        }
        return match?.groupValues?.getOrNull(2) ?: ""
    }
}

