package com.kletaq.app.features.flashcards.engine

import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType

object ClozeParser {

    private val CLOZE_REGEX = Regex("""\{\{c(\d+)::(.*?)\}\}""")

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
            val body = matchResult.groupValues[2]
            val parts = body.split("::", limit = 2)
            val answer = parts[0]
            val hint = if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else null

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
            val body = matchResult.groupValues[2]
            val parts = body.split("::", limit = 2)
            val answer = parts[0]

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
        } ?: return ""
        val body = match.groupValues[2]
        return body.split("::", limit = 2).firstOrNull() ?: ""
    }

    fun extractClozeHint(text: String, targetIndex: Int = 1): String? {
        val match = CLOZE_REGEX.findAll(text).firstOrNull {
            it.groupValues[1].toIntOrNull() == targetIndex
        } ?: return null
        val body = match.groupValues[2]
        val parts = body.split("::", limit = 2)
        return if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else null
    }

    fun generateSiblingCards(
        noteId: String,
        clozeText: String,
        baseCard: Flashcard
    ): List<Flashcard> {
        val indices = extractClozeNumbers(clozeText)
        if (indices.isEmpty()) {
            return listOf(baseCard.copy(noteId = noteId))
        }

        return indices.map { index ->
            val q = renderQuestion(clozeText, index)
            val a = renderAnswer(clozeText, index)
            val hint = extractClozeHint(clozeText, index)
            val answerOnly = extractClozeAnswer(clozeText, index)
            baseCard.copy(
                id = if (indices.size == 1 && baseCard.id.isNotBlank()) baseCard.id else "${noteId}_c$index",
                noteId = noteId,
                cardType = FlashcardType.CLOZE,
                front = q,
                back = a,
                clozeText = clozeText,
                hint = hint ?: baseCard.hint,
                typedAnswer = answerOnly
            )
        }
    }

    fun generateReversibleCards(
        noteId: String,
        front: String,
        back: String,
        baseCard: Flashcard
    ): List<Flashcard> {
        val forwardCard = baseCard.copy(
            id = "${noteId}_fwd",
            noteId = noteId,
            cardType = FlashcardType.REVERSIBLE,
            front = front,
            back = back
        )
        val reverseCard = baseCard.copy(
            id = "${noteId}_rev",
            noteId = noteId,
            cardType = FlashcardType.REVERSIBLE,
            front = back,
            back = front,
            frontImagePaths = baseCard.backImagePaths,
            backImagePaths = baseCard.frontImagePaths
        )
        return listOf(forwardCard, reverseCard)
    }
}
