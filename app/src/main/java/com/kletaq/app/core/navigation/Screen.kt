package com.kletaq.app.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object GoogleSignIn : Screen("google_sign_in")
    object Legal : Screen("legal")
    object DocumentViewer : Screen("doc_viewer/{docType}") {
        fun createRoute(docType: String) = "doc_viewer/$docType"
    }

    // Onboarding Steps
    object UniversityOnboarding : Screen("university_onboarding")
    object BranchOnboarding : Screen("branch_onboarding")
    object SchemeOnboarding : Screen("scheme_onboarding")
    object SemesterOnboarding : Screen("semester_onboarding")
    object CycleOnboarding : Screen("cycle_onboarding")
    object BacklogOnboarding : Screen("backlog_onboarding")
    object UsernameOnboarding : Screen("username_onboarding")
    object ImportSyllabusOnboarding : Screen("import_syllabus_onboarding")
    object ConfigureCalendarOnboarding : Screen("configure_calendar_onboarding")
    object Welcome : Screen("welcome")

    // Main App Tabs
    object Home : Screen("home")
    object Journey : Screen("journey")
    object Tasks : Screen("tasks")
    object Focus : Screen("focus")
    object Profile : Screen("profile")
    object Friends : Screen("friends")
    object Lesson : Screen("lesson")
    object CreateTask : Screen("create_task")
    object CreateNote : Screen("create_note")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object ActiveBacklog : Screen("active_backlog")
    object StudyCalendar : Screen("study_calendar")
    object ArchivedSemesters : Screen("archived_semesters")
    object MonthlyHabitTracker : Screen("monthly_habit_tracker")
    object ArchivedHabitMonths : Screen("archived_habit_months")
}
