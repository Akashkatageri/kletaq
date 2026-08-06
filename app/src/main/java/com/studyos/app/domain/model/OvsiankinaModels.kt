package com.studyos.app.domain.model

data class UnfinishedRoadmapPosition(
    val semesterId: String = "vtu-cse-s4",
    val semesterNumber: Int = 4,
    val subjectId: String = "1BCS401",
    val subjectName: String = "Discrete Mathematics",
    val unitId: String = "1BCS401_M1",
    val unitTitle: String = "Unit 1 • Relation Properties",
    val topicId: String = "1BCS401_M1_T2",
    val topicTitle: String = "Relation Properties",
    val completedCount: Int = 2,
    val totalCount: Int = 3,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    val completedText: String
        get() = "$completedCount/$totalCount completed"
}
