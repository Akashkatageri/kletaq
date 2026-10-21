package com.kletaq.app.data.model

/**
 * Data models for University Model Question Papers and Previous Year Papers (PYQs).
 * Tailored to VTU, autonomous engineering colleges, and STEM curriculums.
 *
 * Models a complete university paper structure:
 * - 5 Modules with standard internal choice (Question 1 OR Question 2, Question 3 OR Question 4...)
 * - 14 to 20 Marks per module (e.g. 7+7, 8+6, or 10+10 splits)
 * - Evaluator marking rubrics and step-by-step derivation checkpoints
 */

data class VtuPaperInfo(
    val vtuPaperCode: String,
    val officialPdfUrl: String,
    val subjectTitle: String,
    val semester: Int = 1,
    val scheme: String = "2025 Scheme",
    val maxMarks: Int = 100,
    val durationMinutes: Int = 180,
    val isAvailableOnline: Boolean = true
)

data class PaperQuestionGroup(
    val questionNumber: Int,                              // e.g. 1, 2, 3...
    val subQuestions: List<UniversityExamQuestion> = emptyList() // e.g. 1a (7M), 1b (7M)
) {
    val totalMarks: Int
        get() = subQuestions.sumOf { it.marks }
}

data class PaperModule(
    val moduleNumber: Int,                                // 1, 2, 3, 4, 5
    val title: String = "Module $moduleNumber",
    val questionA: PaperQuestionGroup,                    // e.g. Question 1
    val questionB: PaperQuestionGroup                     // e.g. Question 2 (OR choice)
)

data class QuestionPaper(
    val id: String = "",
    val subjectCode: String = "",
    val subjectName: String = "",
    val vtuCode: String = "",
    val officialPdfUrl: String = "",
    val semester: Int = 1,
    val scheme: String = "2025 Scheme",
    val maxMarks: Int = 100,
    val durationMinutes: Int = 180,
    val modules: List<PaperModule> = emptyList(),
    val isAiSolved: Boolean = true,
    val sourceDescription: String = "VTU Official Model Question Paper with AI Evaluator Solutions"
)
