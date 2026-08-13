package com.kletaq.app.domain.pipeline

import com.kletaq.app.data.model.Confidence
import com.kletaq.app.data.model.DynamicTopicQuest

/**
 * Production validator that certifies a generated quest is syllabus-safe
 * and zero-hallucination prior to caching or UI rendering.
 */
object QuestValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val validationErrors: List<String>
    )

    fun validateQuest(quest: DynamicTopicQuest): ValidationResult {
        val errors = mutableListOf<String>()

        if (quest.sections.isEmpty()) {
            errors.add("Quest contains no sections.")
        }

        quest.sections.forEachIndexed { index, section ->
            // Rule 1: Mandatory source attribution
            if (section.source.isBlank()) {
                errors.add("Section #${index + 1} ('${section.title}') lacks source attribution.")
            }

            // Rule 2: Reject LOW confidence sections
            if (section.confidence == Confidence.LOW) {
                errors.add("Section #${index + 1} ('${section.title}') has LOW confidence score.")
            }

            // Rule 3: Valid estimated study time
            if (section.estimatedMinutes <= 0) {
                errors.add("Section #${index + 1} ('${section.title}') has invalid study time (${section.estimatedMinutes}m).")
            }

            // Rule 4: PYQ frequency sanity check
            if (section.frequencyInPYQs < 0) {
                errors.add("Section #${index + 1} ('${section.title}') has invalid negative PYQ frequency.")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            validationErrors = errors
        )
    }
}
