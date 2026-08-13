package com.kletaq.app.domain.pipeline

import com.kletaq.app.data.model.Confidence
import com.kletaq.app.data.model.SourceType

/**
 * Evaluates syllabus & PYQ verification to assign Confidence levels:
 *   - HIGH 🟢: Appears in Syllabus AND in VTU PYQs
 *   - MEDIUM 🟡: Appears in Syllabus only
 *   - LOW 🔴: External source only
 */
object ConfidenceScorer {

    fun calculateConfidence(
        isInSyllabus: Boolean,
        hasPYQOccurrence: Boolean,
        sourceType: SourceType
    ): Confidence {
        return when {
            isInSyllabus && hasPYQOccurrence -> Confidence.HIGH
            isInSyllabus -> Confidence.MEDIUM
            sourceType == SourceType.OFFICIAL_SYLLABUS || sourceType == SourceType.VTU_PYQ -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
    }
}
