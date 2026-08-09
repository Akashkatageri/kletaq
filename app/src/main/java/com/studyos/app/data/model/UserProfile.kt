package com.studyos.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val university: String = "VTU",
    val branch: String = "",
    val scheme: String = "",
    val semester: Int = 0,
    val hasCompletedOnboarding: Boolean = false,
    val currentSemester: Int? = null,
    val firstYearCycle: String = "", // "physics" or "chemistry"
    val backlogSubjects: List<String> = emptyList(),
    val calendarConfigured: Boolean = false,
    val studyDaysPerWeek: Int = 5,
    val preferredReminderTime: String = "",
    val studyWhy: String = "",
    val isStudyWhyPinned: Boolean = true,
    val studyWhyUpdatedAt: Long = 0L,
    val onboardingVersion: Int = 2,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val isDemoAccount: Boolean = false,
    @ServerTimestamp
    val acceptedAt: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)

data class UsernameDoc(
    val uid: String = ""
)
