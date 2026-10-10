package com.kletaq.app.data.model

data class CodeSnippet(
    val title: String = "",
    val language: String = "java",
    val code: String = "",
    val explanation: String = ""
)

data class FormulaItem(
    val name: String = "",
    val expression: String = "",
    val description: String = ""
)

data class ExamModelAnswer(
    val question: String = "",
    val marks: Int = 5,
    val stepByStepAnswer: String = "",
    val markingTip: String = ""
)

data class PyqItem(
    val year: String = "",
    val marks: Int = 5,
    val question: String = ""
)

data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = ""
)

data class TopicStudyPack(
    val topicId: String = "",
    val title: String = "",
    val subjectCode: String = "",
    val simpleConcept: String = "",
    val technicalConcept: String = "",
    val keyPoints: List<String> = emptyList(),
    val codeSnippets: List<CodeSnippet> = emptyList(),
    val formulas: List<FormulaItem> = emptyList(),
    val examModelAnswers: List<ExamModelAnswer> = emptyList(),
    val pyqs: List<PyqItem> = emptyList(),
    val quickQuiz: List<QuizQuestion> = emptyList(),
    val quickRecall: String = "",
    val isAiGenerated: Boolean = false
)
