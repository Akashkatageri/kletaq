package com.studyos.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studyos.app.core.utils.SpacedRepetitionEngine
import com.studyos.app.data.model.LearningDifficulty
import com.studyos.app.data.model.Revision
import com.studyos.app.data.model.ReviewRating
import com.studyos.app.data.model.ReviewStats
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
}

@Singleton
class SpacedRepetitionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SpacedRepetitionRepository {

    private companion object {
        private const val TAG_DEBUG = "SpacedRepetitionDebug"
        private const val TAG_ERROR = "SpacedRepetitionError"
    }

    override suspend fun getRevisions(uid: String): Result<List<Revision>> {
        return try {
            Log.d(TAG_DEBUG, "Fetching revisions for users/$uid/revisions...")
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("revisions")
                .get()
                .await()

            val revisions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Revision::class.java)
            }
            val sanitized = SpacedRepetitionEngine.sanitizeRevisions(revisions)
            Log.d(TAG_DEBUG, "Successfully fetched ${sanitized.size} revisions for users/$uid")
            Result.success(sanitized)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to fetch revisions for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun saveRevision(uid: String, revision: Revision): Result<Unit> {
        return try {
            Log.d(TAG_DEBUG, "Saving revision for topic ${revision.topicId} under users/$uid/revisions...")
            firestore.collection("users")
                .document(uid)
                .collection("revisions")
                .document(revision.topicId)
                .set(revision, SetOptions.merge())
                .await()
            Log.d(TAG_DEBUG, "Successfully saved revision for topic ${revision.topicId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to save revision for topic ${revision.topicId}", e)
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
            Log.e(TAG_ERROR, "Failed to create initial revision for topic $topicId", e)
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
            Log.d(TAG_DEBUG, "Recorded review rating '${rating.value}' for topic $topicId. Next review: ${updated.nextReview}")
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to record review for topic $topicId", e)
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
            Log.d(TAG_DEBUG, "Daily review queue size for users/$uid: ${queue.size}")
            Result.success(queue)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to get daily review queue for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun getReviewStats(uid: String): Result<ReviewStats> {
        return try {
            val revisions = getRevisions(uid).getOrThrow()
            val stats = SpacedRepetitionEngine.getReviewStats(revisions)
            Log.d(TAG_DEBUG, "Computed review stats for users/$uid: $stats")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG_ERROR, "Failed to get review stats for $uid", e)
            Result.failure(e)
        }
    }
}
