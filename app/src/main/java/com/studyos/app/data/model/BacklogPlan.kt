package com.studyos.app.data.model

import com.google.firebase.firestore.PropertyName

data class BacklogPlan(
    val id: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val semester: Int = 0,
    val studyDaysPerWeek: Int = 3, // 3 or 5
    val sessionMinutes: Int = 30,  // 30, 45, or 60
    val selectedUnitIds: List<String> = emptyList(),
    val completedTopicIds: List<String> = emptyList(),
    @get:PropertyName("isActive") val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
