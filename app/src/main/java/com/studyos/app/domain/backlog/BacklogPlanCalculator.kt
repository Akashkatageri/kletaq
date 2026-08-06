package com.studyos.app.domain.backlog

import com.studyos.app.data.model.BacklogPlan
import com.studyos.app.features.journey.components.LessonStatus
import com.studyos.app.features.journey.components.SubjectJourney

data class BacklogNextTopicResult(
    val semesterId: String,
    val subjectId: String,
    val subjectName: String,
    val unitId: String,
    val unitTitle: String,
    val topicId: String,
    val topicTitle: String
)

data class BacklogPlanProgressResult(
    val completedCount: Int,
    val totalCount: Int,
    val progressFraction: Float
)

object BacklogPlanCalculator {

    fun isTopicCompleted(
        semesterId: String,
        subjectId: String,
        topicId: String,
        plan: BacklogPlan,
        completedTopicKeys: Set<String>
    ): Boolean {
        if (plan.completedTopicIds.contains(topicId)) return true
        if (completedTopicKeys.contains(topicId)) return true
        if (semesterId.isNotBlank() && subjectId.isNotBlank() && completedTopicKeys.contains("${semesterId}_${subjectId}_${topicId}")) return true
        return false
    }

    fun findNextUnfinishedTopic(
        plan: BacklogPlan,
        subject: SubjectJourney,
        semesterId: String = "",
        completedTopicKeys: Set<String> = emptySet()
    ): BacklogNextTopicResult? {
        val selectedUnits = if (plan.selectedUnitIds.isEmpty()) {
            subject.units
        } else {
            subject.units.filter { plan.selectedUnitIds.contains(it.id) }
        }

        for (unit in selectedUnits) {
            for (lesson in unit.lessons) {
                val completed = isTopicCompleted(semesterId, subject.id, lesson.id, plan, completedTopicKeys) || lesson.status == LessonStatus.COMPLETED
                if (!completed) {
                    return BacklogNextTopicResult(
                        semesterId = semesterId,
                        subjectId = subject.id,
                        subjectName = subject.name,
                        unitId = unit.id,
                        unitTitle = unit.title,
                        topicId = lesson.id,
                        topicTitle = lesson.title
                    )
                }
            }
        }
        return null
    }

    fun calculatePlanProgress(
        plan: BacklogPlan,
        subject: SubjectJourney,
        semesterId: String = "",
        completedTopicKeys: Set<String> = emptySet()
    ): BacklogPlanProgressResult {
        val selectedUnits = if (plan.selectedUnitIds.isEmpty()) {
            subject.units
        } else {
            subject.units.filter { plan.selectedUnitIds.contains(it.id) }
        }

        var totalCount = 0
        var completedCount = 0

        for (unit in selectedUnits) {
            for (lesson in unit.lessons) {
                totalCount++
                val completed = isTopicCompleted(semesterId, subject.id, lesson.id, plan, completedTopicKeys) || lesson.status == LessonStatus.COMPLETED
                if (completed) {
                    completedCount++
                }
            }
        }

        val fraction = if (totalCount > 0) {
            (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
        } else 0f

        return BacklogPlanProgressResult(
            completedCount = completedCount,
            totalCount = totalCount,
            progressFraction = fraction
        )
    }
}
