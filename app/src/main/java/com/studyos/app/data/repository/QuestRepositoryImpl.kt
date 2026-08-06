package com.studyos.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.studyos.app.data.model.DynamicTopicQuest
import com.studyos.app.domain.quest.AcademicQuestGenerator
import kotlinx.coroutines.tasks.await

/**
 * Single Repository for Global Quest Caching and User Progress Tracking.
 *
 * Global Shared Storage:
 *   topicQuests/{scheme}/{semester}/{subject}/{topicId}
 *   -> Generated once, shared across thousands of users.
 *   -> Invalidation: 30-day expiration, scheme change, or admin refresh.
 *
 * User Progress Storage (Separated):
 *   users/{uid}/questProgress/{sectionId}
 */
class QuestRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getOrGenerateQuest(
        topicId: String,
        topicTitle: String,
        subjectName: String,
        semesterName: String,
        scheme: String = "2022_SCHEME",
        forceRefresh: Boolean = false
    ): DynamicTopicQuest {
        val sanitizedSubject = subjectName.replace("/", "_").lowercase()
        val sanitizedSem = semesterName.replace(" ", "_").lowercase()
        val cachePath = "topicQuests/$scheme/$sanitizedSem/$sanitizedSubject/$topicId"

        if (!forceRefresh) {
            try {
                val cachedDoc = db.document(cachePath).get().await()
                if (cachedDoc.exists()) {
                    val quest = cachedDoc.toObject(DynamicTopicQuest::class.java)
                    if (quest != null && System.currentTimeMillis() < quest.expiresAt) {
                        return quest
                    }
                }
            } catch (_: Exception) {
                // Fallthrough to generator on cache miss or error
            }
        }

        // Cache miss / expired / forceRefresh -> Run pipeline
        val generatedQuest = AcademicQuestGenerator.generateQuestForTopic(
            topicId = topicId,
            topicTitle = topicTitle,
            subjectName = subjectName,
            semesterName = semesterName,
            scheme = scheme
        )

        // Save to global shared Firestore cache for all users
        try {
            db.document(cachePath).set(generatedQuest).await()
        } catch (_: Exception) {}

        return generatedQuest
    }

    suspend fun markSectionProgress(
        uid: String,
        topicId: String,
        sectionId: String,
        isCompleted: Boolean,
        isSkipped: Boolean
    ) {
        val progressPath = "users/$uid/questProgress/$sectionId"
        val data = mapOf(
            "sectionId" to sectionId,
            "topicId" to topicId,
            "isCompleted" to isCompleted,
            "isSkipped" to isSkipped,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.document(progressPath).set(data).await()
        } catch (_: Exception) {}
    }
}
