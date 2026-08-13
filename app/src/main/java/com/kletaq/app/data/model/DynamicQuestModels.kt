package com.kletaq.app.data.model

/**
 * Subject classification for adaptive quest layout.
 */
enum class SubjectType {
    MATHEMATICS,
    PROGRAMMING,
    PHYSICS,
    CHEMISTRY,
    THEORY
}

/**
 * Quest section categories mapping to UI layout treatments.
 */
enum class QuestSectionType {
    CONCEPTS,
    FORMULAS,
    PROOFS,
    CODE_EXAMPLES,
    DERIVATIONS,
    COMMON_MISTAKES,
    VTU_PYQS,
    PRACTICE_PROBLEMS,
    IMPORTANT_DEFINITIONS
}

/**
 * Trusted academic source types prioritized for syllabus safety.
 */
enum class SourceType {
    OFFICIAL_SYLLABUS,
    VTU_PYQ,
    NPTEL,
    GEEKSFORGEEKS,
    KHAN_ACADEMY,
    WIKIPEDIA,
    YOUTUBE_LECTURE
}

/**
 * Confidence level computed strictly from source verification:
 *   - HIGH: Appears in Official Syllabus AND VTU PYQs
 *   - MEDIUM: Appears in Official Syllabus only
 *   - LOW: External source only (rejected or marked optional)
 */
enum class Confidence {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Real verified Previous-Year Question (PYQ) entry from VTU exam archives.
 */
data class PYQEntry(
    val question: String,
    val year: Int,
    val frequency: Int,
    val marks: Int = 8,
    val paperCode: String = "BMATE201"
)

/**
 * Official syllabus boundary defining allowed vs forbidden concepts.
 */
data class SyllabusChunk(
    val subjectId: String,
    val topicId: String,
    val topicTitle: String,
    val allowedConcepts: List<String>,
    val forbiddenConcepts: List<String>
)

/**
 * A single, syllabus-safe study section within a topic quest.
 *
 * @property importance Scale from 1 to 10 based on exam weightage
 * @property frequencyInPYQs Total occurrences in VTU past papers
 */
data class QuestSection(
    val id: String,
    val title: String,
    val type: QuestSectionType,
    val source: String,
    val sourceType: SourceType,
    val importance: Int = 5,
    val frequencyInPYQs: Int = 0,
    val confidence: Confidence = Confidence.MEDIUM,
    val estimatedMinutes: Int = 15,
    val description: String = "",
    val formulaOrSnippet: String? = null,
    val pyqEntries: List<PYQEntry> = emptyList(),
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val prerequisiteId: String? = null
)

/**
 * Production-ready Dynamic Topic Quest.
 * Shared globally across all users: topicQuests/{scheme}/{semester}/{subject}/{topicId}
 */
data class DynamicTopicQuest(
    val topicId: String,
    val title: String,
    val subjectName: String,
    val semesterName: String,
    val scheme: String = "2022_SCHEME",
    val subjectType: SubjectType,
    val estimatedStudyTime: Int = 90,
    val difficulty: String = "Medium",
    val sections: List<QuestSection> = emptyList(),
    val isApprovedByAdmin: Boolean = true,
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 Days Cache
    val iconEmoji: String = "📚",
    val sourcesSummary: String = ""
) {
    val completedCount: Int
        get() = sections.count { it.isCompleted }

    val totalCount: Int
        get() = sections.size

    val progressPercentage: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    val isFullyMastered: Boolean
        get() = totalCount > 0 && completedCount == totalCount
}
