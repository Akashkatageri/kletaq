package com.kletaq.app.data.model

import java.util.UUID

/**
 * Represents a single high-yield, note-grounded flashcard generated for an engineering topic.
 */
data class TopicFlashcard(
    val id: String = UUID.randomUUID().toString(),
    val front: String = "",           // Challenge / Question / Formula derivation prompt
    val back: String = "",            // Answer / Worked derivation / Explanation
    val category: String = "CONCEPT", // "CONCEPT", "FORMULA", "EXAM_TRAP", "DERIVATION"
    val pageCitation: String = "",    // e.g. "Page 3"
    val hint: String = ""             // Subtle hint before flipping
)

/**
 * Represents the persistent deck of flashcards generated for a specific study topic.
 */
data class TopicFlashcardDeck(
    val topicId: String = "",
    val topicTitle: String = "",
    val subjectCode: String = "",
    val moduleIndex: Int = 1,
    val cards: List<TopicFlashcard> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)
