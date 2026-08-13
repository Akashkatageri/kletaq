package com.kletaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.domain.model.UnfinishedRoadmapPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OvsiankinaRepository {

    private val _unfinishedPosition = MutableStateFlow<UnfinishedRoadmapPosition?>(null)
    val unfinishedPosition: StateFlow<UnfinishedRoadmapPosition?> = _unfinishedPosition.asStateFlow()
    private var listenerRegistration: ListenerRegistration? = null

    init {
        listenToRoadmapPosition()
    }

    fun listenToRoadmapPosition() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        listenerRegistration?.remove()
        val db = FirebaseFirestore.getInstance()
        listenerRegistration = db.collection("users")
            .document(currentUser.uid)
            .collection("roadmapPosition")
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                try {
                    val pos = snapshot.toObject(UnfinishedRoadmapPosition::class.java)
                    if (pos != null) {
                        _unfinishedPosition.value = pos
                    }
                } catch (_: Exception) {}
            }
    }

    fun updateRoadmapPosition(
        semesterId: String,
        semesterNumber: Int,
        subjectId: String,
        subjectName: String,
        unitId: String,
        unitTitle: String,
        topicId: String,
        topicTitle: String,
        completedCount: Int,
        totalCount: Int
    ) {
        val updated = UnfinishedRoadmapPosition(
            semesterId = semesterId,
            semesterNumber = semesterNumber,
            subjectId = subjectId,
            subjectName = subjectName,
            unitId = unitId,
            unitTitle = unitTitle,
            topicId = topicId,
            topicTitle = topicTitle,
            completedCount = completedCount,
            totalCount = totalCount,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        _unfinishedPosition.value = updated

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(currentUser.uid)
            .collection("roadmapPosition")
            .document("current")
            .set(updated)
    }

    fun clearPosition() {
        _unfinishedPosition.value = null
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(currentUser.uid)
            .collection("roadmapPosition")
            .document("current")
            .delete()
    }
}
