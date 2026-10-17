package com.kletaq.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for User Section Progress Tracking.
 */
class QuestRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

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
