package com.kletaq.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.domain.model.StudyTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TaskRepository {

    private var appContext: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private fun triggerWidgetSync() {
        val ctx = appContext ?: return
        val completedCount = _tasks.value.count { it.isDoneToday }
        com.kletaq.app.widgets.data.WidgetDataHelper.updateCompletedTasks(ctx, completedCount)
    }

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
                triggerWidgetSync()
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
                val todayIso = java.time.LocalDate.now().toString()
                val loadedTasks = snapshot.documents.mapNotNull { doc ->
                    try {
                        val task = doc.toObject(StudyTask::class.java) ?: return@mapNotNull null
                        val isDoneToday = task.completedDates.contains(todayIso) || task.isCompleted
                        task.copy(isCompleted = isDoneToday)
                    } catch (e: Exception) {
                        Log.e("TASK_REPO", "Failed to deserialize task: ${doc.id}", e)
                        null
                    }
                }.filterNot { it.isDeleted }.sortedByDescending { it.createdAt }
                _tasks.value = loadedTasks
                triggerWidgetSync()
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
        val todayIso = java.time.LocalDate.now().toString()
        toggleTaskCompletedForDate(taskId, todayIso)
    }

    fun toggleTaskCompletedForDate(taskId: String, dateIso: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val taskRef = db.collection("users").document(currentUser.uid).collection("tasks").document(taskId)

        val targetTask = _tasks.value.find { it.id == taskId } ?: return
        val currentDates = targetTask.completedDates.toMutableList()
        val newCompletedState: Boolean
        if (currentDates.contains(dateIso)) {
            currentDates.remove(dateIso)
            newCompletedState = false
        } else {
            currentDates.add(dateIso)
            newCompletedState = true
        }

        val todayIso = java.time.LocalDate.now().toString()
        val updatedIsCompleted = if (dateIso == todayIso) newCompletedState else currentDates.contains(todayIso)

        val updatedTask = targetTask.copy(
            completedDates = currentDates,
            isCompleted = updatedIsCompleted
        )

        // Optimistically update local StateFlow instantly
        _tasks.value = _tasks.value.map { if (it.id == taskId) updatedTask else it }
        triggerWidgetSync()

        // Persist to Firestore asynchronously
        taskRef.set(updatedTask).addOnFailureListener { e ->
            Log.e("TASK_REPO", "Error updating task completion for date $dateIso", e)
        }
    }

    fun deleteTask(taskId: String) {
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "isDeleted" to true,
            "deletedAt" to now
        )
        db.collection("users").document(currentUser.uid).collection("tasks").document(taskId).update(updates)
            .addOnFailureListener {
                // If update fails (e.g. document missing), fallback to delete
                db.collection("users").document(currentUser.uid).collection("tasks").document(taskId).delete()
            }
    }

    fun clearAllTasks() {
        _tasks.value = emptyList()
    }
}
