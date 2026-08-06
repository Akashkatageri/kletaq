package com.studyos.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.studyos.app.domain.model.NoteType
import com.studyos.app.domain.model.StudyNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NoteRepository {

    private val initialNotes = listOf(
        StudyNote(
            id = "note_1",
            title = "AVL Tree Rotations Shortcut",
            content = "Balance factor difference must be in {-1, 0, +1}. Left rotation brings right child to root when right subtree is heavier.",
            subjectId = "sub_dsa",
            subjectName = "Data Structures & Algorithms",
            topicId = "topic_avl",
            topicTitle = "AVL Trees & Rotations",
            noteType = NoteType.DEFINITION,
            isPinned = true,
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        StudyNote(
            id = "note_2",
            title = "AVL Insertion Formula & Cases",
            content = "LL Case -> Single Right Rotation. RR Case -> Single Left Rotation. LR Case -> Left-Right Double Rotation. RL Case -> Right-Left Double Rotation.",
            subjectId = "sub_dsa",
            subjectName = "Data Structures & Algorithms",
            topicId = "topic_avl",
            topicTitle = "AVL Trees & Rotations",
            noteType = NoteType.FORMULA,
            isPinned = false,
            createdAt = System.currentTimeMillis() - 86400000L
        ),
        StudyNote(
            id = "note_3",
            title = "Java Interfaces & Default Methods",
            content = "Interfaces in Java 8+ can have default and static methods. Abstract class vs Interface: abstract class can have instance state fields.",
            subjectId = "sub_prog",
            subjectName = "Programming in Java",
            topicId = "topic_oop",
            topicTitle = "Object-Oriented Programming",
            noteType = NoteType.DEFINITION,
            isPinned = false,
            createdAt = System.currentTimeMillis() - 3600000L * 5
        )
    )

    private val _notes = MutableStateFlow<List<StudyNote>>(initialNotes)
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
