package com.kletaq.app.features.flashcards.model

import java.util.UUID

enum class FlashcardType {
    BASIC,
    CLOZE,
    REVERSIBLE,
    TYPE_IN
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
    BLUE(0xFF3B82F6),
    PINK(0xFFEC4899),
    TURQUOISE(0xFF14B8A6),
    PURPLE(0xFF8B5CF6)
}

enum class FlashcardReviewRating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4)
}

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String = UUID.randomUUID().toString(),
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
    val frontImagePaths: List<String> = emptyList(),
    val backImagePaths: List<String> = emptyList(),
    val audioPath: String? = null,
    val typedAnswer: String = "",
    val tags: List<String> = emptyList(),
    val flagColor: CardFlag = CardFlag.NONE,
    val isSuspended: Boolean = false,
    val isMarked: Boolean = false,
    val isBuried: Boolean = false,
    val buriedUntilTimestamp: Long = 0L,
    val isLeech: Boolean = false,
    val queueState: CardQueueState = CardQueueState.NEW,
    val reps: Int = 0,
    val lapses: Int = 0,
    val easeFactor: Double = 2.50, // 250% default Anki ease
    val intervalDays: Int = 0,
    val dueTimestamp: Long = System.currentTimeMillis(),
    val learningStepIndex: Int = 0,
    val lastReviewedTimestamp: Long = 0L,
    // FSRS parameters
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val allFrontImages: List<String>
        get() = frontImagePaths.ifEmpty { listOfNotNull(frontImagePath) }

    val allBackImages: List<String>
        get() = backImagePaths.ifEmpty { listOfNotNull(backImagePath) }
}

data class FlashcardDeck(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val subjectId: String? = null,
    val topicId: String? = null,
    val parentDeckId: String? = null,
    val presetId: String = "default",
    val newCardsPerDay: Int = 20,
    val maxReviewsPerDay: Int = 200,
    val learningStepsMinutes: List<Int> = listOf(1, 10),
    val graduatingIntervalDays: Int = 1,
    val easyIntervalDays: Int = 4,
    val startingEase: Double = 2.5,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isSubdeck: Boolean get() = name.contains("::") || parentDeckId != null
    val displayName: String get() = if (name.contains("::")) name.substringAfterLast("::") else name
}

data class DeckPreset(
    val id: String = "default",
    val name: String = "Default",
    val newCardsPerDay: Int = 20,
    val maxReviewsPerDay: Int = 200,
    val learningStepsMinutes: List<Int> = listOf(1, 10),
    val relearningStepsMinutes: List<Int> = listOf(10),
    val useFsrs: Boolean = true,
    val desiredRetention: Double = 0.90,
    val startingEase: Double = 2.50,
    val easyBonus: Double = 1.30,
    val intervalModifier: Double = 1.00,
    val maximumIntervalDays: Int = 36500,
    val leechThreshold: Int = 8,
    val leechAction: String = "TAG_ONLY", // "TAG_ONLY" or "SUSPEND"
    val autoBurySiblings: Boolean = true
)

data class ReviewLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val rating: FlashcardReviewRating,
    val reviewTimestamp: Long = System.currentTimeMillis(),
    val timeSpentMs: Long = 0L,
    val intervalDays: Int = 0,
    val lastIntervalDays: Int = 0,
    val reviewType: String = "REVIEW" // "NEW", "LEARN", "REVIEW", "RELEARN", "CRAM"
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

