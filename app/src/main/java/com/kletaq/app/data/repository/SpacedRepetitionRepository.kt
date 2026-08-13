package com.kletaq.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.core.config.AdaptiveEngineConfig
import com.kletaq.app.core.utils.SpacedRepetitionEngine
import com.kletaq.app.data.model.LearningDifficulty
import com.kletaq.app.data.model.MemoryCard
import com.kletaq.app.data.model.Revision
import com.kletaq.app.data.model.ReviewRating
import com.kletaq.app.data.model.ReviewStats
import com.kletaq.app.data.model.TopicDifficulty
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface SpacedRepetitionRepository {
    suspend fun getRevisions(uid: String): Result<List<Revision>>
    suspend fun saveRevision(uid: String, revision: Revision): Result<Unit>
    suspend fun createRevisionForTopic(
        uid: String,
        topicId: String,
        subjectId: String,
        subjectName: String,
        topicName: String,
        difficulty: LearningDifficulty = LearningDifficulty.MEDIUM
    ): Result<Revision>
    suspend fun recordReview(uid: String, topicId: String, rating: ReviewRating): Result<Revision>
    suspend fun getDailyReviewQueue(uid: String, isExamMode: Boolean = false): Result<List<Revision>>
    suspend fun getReviewStats(uid: String): Result<ReviewStats>

    // ── MemoryCard Persistence Engine ──────────────────────────────────────────────
    suspend fun getMemoryCards(uid: String): Result<List<MemoryCard>>
    suspend fun getMemoryCardForTopic(uid: String, topicId: String): Result<MemoryCard?>
    suspend fun saveMemoryCard(uid: String, card: MemoryCard): Result<Unit>
    suspend fun recordMemoryCardReview(uid: String, topicId: String, rating: ReviewRating): Result<MemoryCard>
}

@Singleton
class SpacedRepetitionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SpacedRepetitionRepository {

    private companion object {
        private const val TAG_DEBUG = "SpacedRepetitionDebug"
        private const val TAG_ERROR = "SpacedRepetitionError"
    }

    override suspend fun getMemoryCards(uid: String): Result<List<MemoryCard>> {
        if (uid.isBlank()) return Result.success(emptyList())
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("memory_cards")
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MemoryCard::class.java)
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to get memory cards for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun getMemoryCardForTopic(uid: String, topicId: String): Result<MemoryCard?> {
        if (uid.isBlank() || topicId.isBlank()) return Result.success(null)
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("memory_cards")
                .document(topicId)
                .get()
                .await()

            if (snapshot.exists()) {
                Result.success(snapshot.toObject(MemoryCard::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveMemoryCard(uid: String, card: MemoryCard): Result<Unit> {
        if (uid.isBlank() || card.topicId.isBlank()) return Result.failure(IllegalArgumentException("Invalid params"))
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("memory_cards")
                .document(card.topicId)
                .set(card, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to save MemoryCard for ${card.topicId}", e)
            Result.failure(e)
        }
    }

    override suspend fun recordMemoryCardReview(
        uid: String,
        topicId: String,
        rating: ReviewRating
    ): Result<MemoryCard> {
        if (uid.isBlank() || topicId.isBlank()) return Result.failure(IllegalArgumentException("Invalid params"))
        return try {
            val cardRef = firestore.collection("users")
                .document(uid)
                .collection("memory_cards")
                .document(topicId)

            val updatedCard = firestore.runTransaction { tx ->
                val snap = tx.get(cardRef)
                val existing = if (snap.exists()) snap.toObject(MemoryCard::class.java) ?: MemoryCard(topicId = topicId) else MemoryCard(topicId = topicId)
                val updated = SpacedRepetitionEngine.updateMemoryCardReview(existing, rating)
                tx.set(cardRef, updated, SetOptions.merge())
                updated
            }.await()

            Result.success(updatedCard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Legacy Revision Implementations ───────────────────────────────────────────

    override suspend fun getRevisions(uid: String): Result<List<Revision>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("revisions")
                .get()
                .await()

            val revisions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Revision::class.java)
            }
            val sanitized = SpacedRepetitionEngine.sanitizeRevisions(revisions)
            Result.success(sanitized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveRevision(uid: String, revision: Revision): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("revisions")
                .document(revision.topicId)
                .set(revision, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createRevisionForTopic(
        uid: String,
        topicId: String,
        subjectId: String,
        subjectName: String,
        topicName: String,
        difficulty: LearningDifficulty
    ): Result<Revision> {
        return try {
            val initial = SpacedRepetitionEngine.createInitialRevision(
                topicId = topicId,
                subjectId = subjectId,
                subjectName = subjectName,
                topicName = topicName,
                difficulty = difficulty
            )
            saveRevision(uid, initial).getOrThrow()
            Result.success(initial)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordReview(
        uid: String,
        topicId: String,
        rating: ReviewRating
    ): Result<Revision> {
        return try {
            val revisions = getRevisions(uid).getOrThrow()
            val existing = revisions.find { it.topicId == topicId }
                ?: return Result.failure(NoSuchElementException("Revision not found for topic $topicId"))

            val updated = SpacedRepetitionEngine.updateRevisionScheduling(existing, rating)
            saveRevision(uid, updated).getOrThrow()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDailyReviewQueue(
        uid: String,
        isExamMode: Boolean
    ): Result<List<Revision>> {
        return try {
            val revisions = getRevisions(uid).getOrThrow()
            val queue = SpacedRepetitionEngine.getDailyReviewQueue(revisions, isExamMode)
            Result.success(queue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewStats(uid: String): Result<ReviewStats> {
        return try {
            val revisions = getRevisions(uid).getOrThrow()
            val stats = SpacedRepetitionEngine.getReviewStats(revisions)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
