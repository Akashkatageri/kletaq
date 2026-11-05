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
    object SchoolOnboarding : Screen("school_onboarding")
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
    object AiSettings : Screen("ai_settings")
    object Notifications : Screen("notifications")
    object ActiveBacklog : Screen("active_backlog")
    object StudyCalendar : Screen("study_calendar")
    object ArchivedSemesters : Screen("archived_semesters")
    object MonthlyHabitTracker : Screen("monthly_habit_tracker")
    object ArchivedHabitMonths : Screen("archived_habit_months")
    object Calendar : Screen("calendar")
    object ExamSprintHub : Screen("exam_sprint_hub")
    object CreateExamSprint : Screen("create_exam_sprint")
    object ExamSprintDetail : Screen("exam_sprint_detail/{sprintId}") {
        fun createRoute(sprintId: String) = "exam_sprint_detail/$sprintId"
    }
    object ExamSprintMock : Screen("exam_sprint_mock/{sprintId}?count={count}") {
        fun createRoute(sprintId: String, count: Int = 5) = "exam_sprint_mock/$sprintId?count=$count"
    }

    // PDF Studio Routes
    object PdfStudioHub : Screen("pdf_studio_hub")
    object ImagesToPdf : Screen("pdf_studio_images_to_pdf")
    object CompressPdf : Screen("pdf_studio_compress")
    object MergePdf : Screen("pdf_studio_merge")
    object SplitPdf : Screen("pdf_studio_split")
    object PdfToImages : Screen("pdf_studio_pdf_to_images")
    object InvertPdf : Screen("pdf_studio_invert")
    object CompressPhoto : Screen("pdf_studio_compress_photo")

    // Memory Palace Routes
    object MemoryPalaceHub : Screen("memory_palace_hub")
    object MemoryPalaceCreate : Screen("memory_palace_create")
    object MemoryPalaceDerivationCreate : Screen("memory_palace_derivation_create?palaceId={palaceId}") {
        fun createRoute(palaceId: String? = null) = if (palaceId != null) "memory_palace_derivation_create?palaceId=$palaceId" else "memory_palace_derivation_create"
    }
    object MemoryPalaceWalk : Screen("memory_palace_walk/{derivationId}") {
        fun createRoute(derivationId: String) = "memory_palace_walk/$derivationId"
    }

    // Flashcards (Anki) Routes
    object FlashcardsHub : Screen("flashcards_hub")
    object FlashcardDeckOverview : Screen("flashcards_deck_overview/{deckId}") {
        fun createRoute(deckId: String) = "flashcards_deck_overview/$deckId"
    }
    object FlashcardsStudy : Screen("flashcards_study?deckId={deckId}&topicId={topicId}&filter={filter}") {
        fun createRoute(deckId: String? = null, topicId: String? = null, filter: String? = null): String {
            val params = mutableListOf<String>()
            if (deckId != null) params.add("deckId=$deckId")
            if (topicId != null) params.add("topicId=$topicId")
            if (filter != null) params.add("filter=$filter")
            return if (params.isEmpty()) "flashcards_study" else "flashcards_study?" + params.joinToString("&")
        }
    }
    object FlashcardBrowser : Screen("flashcards_browser?deckId={deckId}") {
        fun createRoute(deckId: String? = null) = if (deckId != null) "flashcards_browser?deckId=$deckId" else "flashcards_browser"
    }
    object FlashcardStats : Screen("flashcards_stats")
}
