package com.kletaq.app.domain.model

enum class NoteType(val label: String, val emoji: String) {
    FORMULA("Formula", "📐"),
    DEFINITION("Definition", "📖"),
    DOUBT("Doubt", "❓"),
    IDEA("Idea", "💡"),
    GENERAL("General Note", "📝")
}

data class StudyNote(
    val id: String = System.currentTimeMillis().toString(),
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val subjectId: String? = null,
    val subjectName: String? = "Data Structures & Algorithms",
    val topicId: String? = null,
    val topicTitle: String? = null,
    val noteType: NoteType = NoteType.GENERAL,
    val isPinned: Boolean = false,
    val attachmentUrls: List<String> = emptyList(),
    val pageCount: Int = 0,
    val originalSizeBytes: Long = 0L,
    val compressedSizeBytes: Long = 0L
) {
    val hasAttachments: Boolean get() = attachmentUrls.isNotEmpty() || pageCount > 0
}
