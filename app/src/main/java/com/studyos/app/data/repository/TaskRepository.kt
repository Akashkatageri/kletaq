package com.studyos.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.studyos.app.domain.model.StudyTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TaskRepository {

    private val _tasks = MutableStateFlow<List<StudyTask>>(emptyList())
    val tasks: StateFlow<List<StudyTask>> = _tasks.asStateFlow()
    private var listenerRegistration: ListenerRegistration? = null

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                listenToUserTasks(user.uid)
            } else {
                listenerRegistration?.remove()
                _tasks.value = emptyList()
            }
        }
    }

    fun listenToUserTasks(uid: String? = null) {
        val targetUid = uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        listenerRegistration?.remove()
        val db = FirebaseFirestore.getInstance()
        listenerRegistration = db.collection("users")
            .document(targetUid)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TASK_REPO", "Error listening to tasks", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val loadedTasks = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(StudyTask::class.java)
                    } catch (e: Exception) {
                        Log.e("TASK_REPO", "Failed to deserialize task: ${doc.id}", e)
                        null
                    }
                }.sortedByDescending { it.createdAt }
                _tasks.value = loadedTasks
            }
    }

    fun addTask(task: StudyTask) {
        _tasks.value = listOf(task) + _tasks.value.filterNot { it.id == task.id }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("tasks").document(task.id).set(task)
            .addOnFailureListener { e -> Log.e("TASK_REPO", "Error saving task to firestore", e) }
    }

    fun updateTask(task: StudyTask) {
        _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("tasks").document(task.id).set(task)
    }

    fun toggleTaskCompleted(taskId: String) {
        val updatedList = _tasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        _tasks.value = updatedList
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val task = updatedList.find { it.id == taskId } ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("tasks").document(taskId).set(task)
    }

    fun deleteTask(taskId: String) {
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("tasks").document(taskId).delete()
    }

    fun clearAllTasks() {
        _tasks.value = emptyList()
    }
}
