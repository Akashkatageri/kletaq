package com.kletaq.app.features.flashcards.model

import java.util.UUID

enum class FlashcardType {
    BASIC,
    CLOZE,
    REVERSIBLE
}

enum class CardQueueState {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING
}

enum class CardFlag(val colorHex: Long) {
    NONE(0x00000000),
    RED(0xFFEF4444),
    ORANGE(0xFFF97316),
    GREEN(0xFF10B981),
    BLUE(0xFF3B82F6)
}

enum class FlashcardReviewRating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4)
}

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val deckId: String = "",
    val deckName: String = "",
    val topicId: String = "",
    val topicTitle: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val cardType: FlashcardType = FlashcardType.BASIC,
    val front: String = "",
    val back: String = "",
    val clozeText: String = "",
    val hint: String = "",
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val tags: List<String> = emptyList(),
    val flagColor: CardFlag = CardFlag.NONE,
    val isSuspended: Boolean = false,
    val isMarked: Boolean = false,
    val queueState: CardQueueState = CardQueueState.NEW,
    val reps: Int = 0,
    val lapses: Int = 0,
    val easeFactor: Double = 2.50, // 250% default Anki ease
    val intervalDays: Int = 0,
    val dueTimestamp: Long = System.currentTimeMillis(),
    val learningStepIndex: Int = 0,
    val lastReviewedTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FlashcardDeck(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val subjectId: String? = null,
    val topicId: String? = null,
    val newCardsPerDay: Int = 20,
    val maxReviewsPerDay: Int = 200,
    val learningStepsMinutes: List<Int> = listOf(1, 10),
    val graduatingIntervalDays: Int = 1,
    val easyIntervalDays: Int = 4,
    val startingEase: Double = 2.5,
    val createdAt: Long = System.currentTimeMillis()
)

data class DeckStudyCounts(
    val newCount: Int = 0,
    val learningCount: Int = 0,
    val dueCount: Int = 0,
    val totalCount: Int = 0
)

data class FlashcardReviewSnapshot(
    val cardId: String,
    val previousState: Flashcard
)

