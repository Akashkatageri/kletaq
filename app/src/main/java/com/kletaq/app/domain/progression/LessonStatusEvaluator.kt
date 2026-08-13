package com.kletaq.app.domain.progression

import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.LessonStatus
import com.kletaq.app.features.journey.components.UnitJourney

/**
 * Single source of truth for computing [LessonStatus] across the application.
 *
 * Status derivation rules:
 * 1. COMPLETED: Prior passed semester (not backlog) OR topic key exists in [completedTopicKeys].
 * 2. LOCKED: Semester is locked OR previous lesson in the progression path was not completed/available.
 * 3. CURRENT: First uncompleted lesson in the subject with prerequisites/previous lessons satisfied.
 * 4. AVAILABLE: Subsequent uncompleted lessons with prerequisites satisfied after a CURRENT lesson has been assigned.
 */
object LessonStatusEvaluator {

    fun evaluateSubjectUnits(
        semesterId: String,
        subjectId: String,
        units: List<UnitJourney>,
        completedTopicKeys: Set<String>,
        isPriorSemester: Boolean,
        isBacklog: Boolean,
        isSemesterLocked: Boolean,
        hasSetCurrentInSubject: Boolean
    ): Pair<List<UnitJourney>, Boolean> {
        var setCurrentFlag = hasSetCurrentInSubject

        val evaluatedUnits = units.map { rawUnit ->
            var previousLessonCompletedOrAvailable = true

            val evaluatedLessons = rawUnit.lessons.mapIndexed { index, rawLesson ->
                val scopedKey = "${semesterId}_${subjectId}_${rawLesson.id}"
                val isExplicitlyCompleted = completedTopicKeys.contains(scopedKey) || completedTopicKeys.contains(rawLesson.id)

                val status = when {
                    isPriorSemester && !isBacklog -> LessonStatus.COMPLETED
                    isExplicitlyCompleted -> LessonStatus.COMPLETED
                    isSemesterLocked -> LessonStatus.LOCKED
                    !setCurrentFlag -> {
                        setCurrentFlag = true
                        LessonStatus.CURRENT
                    }
                    index == 0 || previousLessonCompletedOrAvailable -> LessonStatus.AVAILABLE
                    else -> LessonStatus.LOCKED
                }

                if (status != LessonStatus.COMPLETED) {
                    previousLessonCompletedOrAvailable = false
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
        isPriorSemester: Boolean,
        isBacklog: Boolean,
        isSemesterLocked: Boolean,
        isFirstUncompletedInSubject: Boolean,
        previousLessonCompletedOrAvailable: Boolean,
        lessonIndexInUnit: Int
    ): LessonStatus {
        val scopedKey = "${semesterId}_${subjectId}_${lessonId}"
        val isExplicitlyCompleted = completedTopicKeys.contains(scopedKey) || completedTopicKeys.contains(lessonId)

        return when {
            isPriorSemester && !isBacklog -> LessonStatus.COMPLETED
            isExplicitlyCompleted -> LessonStatus.COMPLETED
            isSemesterLocked -> LessonStatus.LOCKED
            isFirstUncompletedInSubject -> LessonStatus.CURRENT
            lessonIndexInUnit == 0 || previousLessonCompletedOrAvailable -> LessonStatus.AVAILABLE
            else -> LessonStatus.LOCKED
        }
    }
}
