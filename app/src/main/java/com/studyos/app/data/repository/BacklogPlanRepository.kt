package com.studyos.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.studyos.app.data.model.BacklogPlan
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface BacklogPlanRepository {
    suspend fun getActivePlan(uid: String): Result<BacklogPlan?>
    fun observeActivePlan(uid: String): Flow<BacklogPlan?>
    suspend fun getPlanForSubject(uid: String, subjectId: String): Result<BacklogPlan?>
    suspend fun savePlan(uid: String, plan: BacklogPlan): Result<Unit>
    suspend fun deactivatePlan(uid: String, planId: String): Result<Unit>
    suspend fun deletePlan(uid: String, planId: String): Result<Unit>
    suspend fun markTopicCompletedInPlan(uid: String, planId: String, topicId: String): Result<Unit>
}

@Singleton
class BacklogPlanRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BacklogPlanRepository {

    override suspend fun getActivePlan(uid: String): Result<BacklogPlan?> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("backlogPlans")
                .whereEqualTo("isActive", true)
                .get()
                .await()
            val plan = snapshot.documents.firstOrNull()?.toObject(BacklogPlan::class.java)
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeActivePlan(uid: String): Flow<BacklogPlan?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users")
            .document(uid)
            .collection("backlogPlans")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val plan = snapshot?.documents?.firstOrNull()?.toObject(BacklogPlan::class.java)
                trySend(plan)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPlanForSubject(uid: String, subjectId: String): Result<BacklogPlan?> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("backlogPlans")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val plan = snapshot.documents.firstOrNull()?.toObject(BacklogPlan::class.java)
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePlan(uid: String, plan: BacklogPlan): Result<Unit> {
        return try {
            val userPlansRef = firestore.collection("users")
                .document(uid)
                .collection("backlogPlans")

            val activeDocsSnapshot = userPlansRef
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val batch = firestore.batch()

            // One-plan rule: Deactivate any currently active plan
            for (doc in activeDocsSnapshot.documents) {
                batch.update(doc.reference, "isActive", false)
            }

            val docRef = if (plan.id.isNotBlank()) {
                userPlansRef.document(plan.id)
            } else {
                userPlansRef.document()
            }

            val finalPlan = plan.copy(id = docRef.id, isActive = true)
            batch.set(docRef, finalPlan)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deactivatePlan(uid: String, planId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("backlogPlans")
                .document(planId)
                .update("isActive", false)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlan(uid: String, planId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("backlogPlans")
                .document(planId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markTopicCompletedInPlan(uid: String, planId: String, topicId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("backlogPlans")
                .document(planId)
                .update("completedTopicIds", FieldValue.arrayUnion(topicId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
