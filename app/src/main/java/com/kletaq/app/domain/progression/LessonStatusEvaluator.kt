package com.kletaq.app.domain.progression

import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.LessonStatus
import com.kletaq.app.features.journey.components.UnitJourney

/**
 * Single source of truth for computing [LessonStatus] across the application.
 *
 * Status derivation rules:
 * 1. COMPLETED: Prior passed semester (not backlog) OR topic key exists in [completedTopicKeys].
 * 2. LOCKED: Only a future, unavailable semester can be locked.
 * 3. CURRENT: First uncompleted lesson in the subject.
 * 4. AVAILABLE: Every other unfinished lesson is freely selectable.
 */
object LessonStatusEvaluator {

    fun evaluateSubjectUnits(
        semesterId: String,
        subjectId: String,
        units: List<UnitJourney>,
        completedTopicKeys: Set<String>,
        resetTopicKeys: Set<String> = emptySet(),
        isPriorSemester: Boolean,
        isBacklog: Boolean,
        isSemesterLocked: Boolean,
        hasSetCurrentInSubject: Boolean
    ): Pair<List<UnitJourney>, Boolean> {
        var setCurrentFlag = hasSetCurrentInSubject

        val evaluatedUnits = units.map { rawUnit ->
            val evaluatedLessons = rawUnit.lessons.map { rawLesson ->
                val scopedKey = "${semesterId}_${subjectId}_${rawLesson.id}"
                val isExplicitlyCompleted = completedTopicKeys.contains(scopedKey) || completedTopicKeys.contains(rawLesson.id)
                val isExplicitlyReset = resetTopicKeys.contains(scopedKey) || resetTopicKeys.contains(rawLesson.id)

                val status = when {
                    isPriorSemester && !isBacklog && !isExplicitlyReset -> LessonStatus.COMPLETED
                    isExplicitlyCompleted -> LessonStatus.COMPLETED
                    isSemesterLocked -> LessonStatus.LOCKED
                    !setCurrentFlag -> {
                        setCurrentFlag = true
                        LessonStatus.CURRENT
                    }
                    else -> LessonStatus.AVAILABLE
                }

                rawLesson.copy(status = status)
            }

            rawUnit.copy(lessons = evaluatedLessons)
        }

        return Pair(evaluatedUnits, setCurrentFlag)
    }

    fun evaluateLessonStatus(
        semesterId: String,
        subjectId: String,
        lessonId: String,
        completedTopicKeys: Set<String>,
        resetTopicKeys: Set<String> = emptySet(),
        isPriorSemester: Boolean,
        isBacklog: Boolean,
        isSemesterLocked: Boolean,
        isFirstUncompletedInSubject: Boolean,
        previousLessonCompletedOrAvailable: Boolean,
        lessonIndexInUnit: Int
    ): LessonStatus {
        val scopedKey = "${semesterId}_${subjectId}_${lessonId}"
        val isExplicitlyCompleted = completedTopicKeys.contains(scopedKey) || completedTopicKeys.contains(lessonId)
        val isExplicitlyReset = resetTopicKeys.contains(scopedKey) || resetTopicKeys.contains(lessonId)

        return when {
            isPriorSemester && !isBacklog && !isExplicitlyReset -> LessonStatus.COMPLETED
            isExplicitlyCompleted -> LessonStatus.COMPLETED
            isSemesterLocked -> LessonStatus.LOCKED
            isFirstUncompletedInSubject -> LessonStatus.CURRENT
            else -> LessonStatus.AVAILABLE
        }
    }
}
