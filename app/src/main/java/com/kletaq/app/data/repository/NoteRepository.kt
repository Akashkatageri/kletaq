package com.kletaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kletaq.app.domain.model.NoteType
import com.kletaq.app.domain.model.StudyNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NoteRepository {

    private val _notes = MutableStateFlow<List<StudyNote>>(emptyList())
    val notes: StateFlow<List<StudyNote>> = _notes.asStateFlow()
    private var listenerRegistration: ListenerRegistration? = null

    init {
        listenToUserNotes()
    }

    fun listenToUserNotes() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        listenerRegistration?.remove()
        val db = FirebaseFirestore.getInstance()
        listenerRegistration = db.collection("users")
            .document(currentUser.uid)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val loadedNotes = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(StudyNote::class.java)
                    } catch (_: Exception) { null }
                }.sortedByDescending { it.createdAt }
                if (loadedNotes.isNotEmpty()) {
                    _notes.value = loadedNotes
                }
            }
    }

    fun addNote(note: StudyNote) {
        _notes.value = listOf(note) + _notes.value.filterNot { it.id == note.id }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("notes").document(note.id).set(note)
    }

    fun updateNote(updatedNote: StudyNote) {
        _notes.value = _notes.value.map {
            if (it.id == updatedNote.id) updatedNote.copy(updatedAt = System.currentTimeMillis()) else it
        }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("notes").document(updatedNote.id).set(updatedNote)
    }

    fun deleteNote(noteId: String) {
        _notes.value = _notes.value.filterNot { it.id == noteId }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("notes").document(noteId).delete()
    }

    fun togglePin(noteId: String) {
        val updatedList = _notes.value.map {
            if (it.id == noteId) it.copy(isPinned = !it.isPinned) else it
        }
        _notes.value = updatedList
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val updatedNote = updatedList.find { it.id == noteId } ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).collection("notes").document(noteId).set(updatedNote)
    }

    fun getNotesForTopic(topicTitle: String): List<StudyNote> {
        return _notes.value.filter { note ->
            note.topicTitle?.contains(topicTitle, ignoreCase = true) == true ||
            note.title.contains(topicTitle, ignoreCase = true) ||
            note.content.contains(topicTitle, ignoreCase = true)
        }
    }
}
