package com.kletaq.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * MemoryCard represents a single topic's adaptive spaced repetition memory state.
 * Each topic owns exactly ONE MemoryCard stored at users/{uid}/memory_cards/{topicId}.
 */
data class MemoryCard(
    val topicId: String = "",
    val subjectId: String = "",
    val moduleId: String = "",
    val lessonId: String = "",

    val subjectName: String = "",
    val topicName: String = "",

    val difficulty: String = TopicDifficulty.MEDIUM.value,
    val confidence: Int = 3, // 1..5

    val reviewCount: Int = 0,
    val lastReview: Long = 0L,
    val nextReview: Long = 0L,
    val currentInterval: Int = 1, // in days

    val easeFactor: Double = 2.5,
    val lapses: Int = 0,
    val mastery: Float = 0.0f, // 0.0 .. 1.0

    val memoryHealth: String = MemoryHealth.FRESH.value,
    val importanceScore: Double = 1.0,

    val studyTimeMinutes: Int = 0,
    val completionDate: Long = System.currentTimeMillis(),
    val lastRating: String = ReviewRating.GOOD.value,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
