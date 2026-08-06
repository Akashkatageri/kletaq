package com.studyos.app.domain.pipeline

import com.studyos.app.data.model.SyllabusChunk

/**
 * Filter that enforces strict official syllabus boundaries.
 * Rejects any out-of-syllabus concepts or forbidden topics.
 */
object SyllabusFilter {

    data class FilterResult(
        val allowedConcepts: List<String>,
        val rejectedConcepts: List<String>,
        val isSyllabusCompliant: Boolean
    )

    fun filterConcepts(
        rawConcepts: List<String>,
        syllabusChunk: SyllabusChunk
    ): FilterResult {
        val allowed = mutableListOf<String>()
        val rejected = mutableListOf<String>()

        for (concept in rawConcepts) {
            val isForbidden = syllabusChunk.forbiddenConcepts.any { forbidden ->
                concept.contains(forbidden, ignoreCase = true) || forbidden.contains(concept, ignoreCase = true)
            }

            if (isForbidden) {
                rejected.add(concept)
            } else {
                allowed.add(concept)
            }
        }

        return FilterResult(
            allowedConcepts = allowed,
            rejectedConcepts = rejected,
            isSyllabusCompliant = rejected.isEmpty()
        )
    }
}
