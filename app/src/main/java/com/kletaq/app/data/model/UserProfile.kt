package com.kletaq.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val university: String = "VTU",
    val branch: String = "",
    val scheme: String = "",
    val semester: Int = 0,
    val totalSemesters: Int = 8,
    val educationLevel: String = "COLLEGE", // "COLLEGE" or "SCHOOL"
    val schoolBoard: String = "",
    val schoolGrade: String = "",
    val schoolStream: String = "",
    val hasCompletedOnboarding: Boolean = false,
    val currentSemester: Int? = null,
    val firstYearCycle: String = "", // "physics" or "chemistry"
    val backlogSubjects: List<String> = emptyList(),
    val calendarConfigured: Boolean = false,
    val studyDaysPerWeek: Int = 5,
    val preferredReminderTime: String = "",
    val studyWhy: String = "",
    @get:PropertyName("isStudyWhyPinned")
    val isStudyWhyPinned: Boolean = true,
    val studyWhyUpdatedAt: Long = 0L,
    val onboardingVersion: Int = 2,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    @get:PropertyName("isDemoAccount")
    val isDemoAccount: Boolean = false,
    val role: String = "STUDENT",
    @ServerTimestamp
    val acceptedAt: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null
) {
    @get:Exclude
    val isAdmin: Boolean get() = role.equals("ADMIN", ignoreCase = true) || role.equals("ADMINISTRATOR", ignoreCase = true)
}

data class UsernameDoc(
    val uid: String = ""
)
