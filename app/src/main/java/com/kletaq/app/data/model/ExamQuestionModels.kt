package com.kletaq.app.data.model

import org.json.JSONObject

/**
 * High-yield subjective exam models tailored directly to university engineering exams
 * (VTU, autonomous colleges, and STEM engineering curriculums).
 *
 * Supports the 3 core question types found in actual exam papers:
 * 1. 7 & 8-Mark Core Mathematical Derivations (broken into 3-stage chunked checkpoints)
 * 2. 7 & 8-Mark Diagrams, Working Principles & M-H / Circuit Comparisons (with evaluator rubrics)
 * 3. 4, 5 & 6-Mark Applied Numericals (with given data, formulas, steps, and trap warnings)
 */

data class RubricStep(
    val stepNumber: Int = 1,
    val label: String = "",           // e.g. "Definition / Statement", "Labeled Diagram", "Derivation"
    val marks: Int = 1,               // Marks allocated for this step by the evaluator
    val content: String = "",          // Detailed mathematical / conceptual breakdown
    val notePageCitation: Int? = null  // Reference to [Page X] in student's notes
)

data class DerivationCheckpoint(
    val stageIndex: Int = 1,          // 1 = Setup, 2 = Intermediate Substitution, 3 = Final Result
    val stageTitle: String = "",      // e.g. "Stage 1: Setup & Governing Equations"
    val prompt: String = "",          // Active recall question for this specific step
    val answer: String = "",          // Expected mathematical step / derivation snippet
    val marks: Int = 3,               // Marks associated with completing this stage
    val notePageCitation: Int? = null // Reference to [Page X] in student's notes
)

data class NumericalSolver(
    val givenData: List<String> = emptyList(),       // e.g. ["n₁ = 1.48 (Core index)", "n₂ = 1.46 (Cladding index)"]
    val formula: String = "",                        // e.g. "NA = √(n₁² - n₂²), θₐ = sin⁻¹(NA)"
    val calculationSteps: String = "",               // Step-by-Step substitution
    val finalAnswer: String = "",                    // Final value with explicit SI units
    val trapWarning: String = ""                     // Common pitfall, e.g. "Set calculator to DEG, not RAD!"
)

data class UniversityExamQuestion(
    val id: String = "",
    val subPart: String = "a",                       // "a", "b", or "c"
    val questionText: String = "",
    val marks: Int = 8,                              // 4, 5, 6, 7, 8, or 10 marks
    val bloomLevel: String = "L2",                   // e.g. "L2", "L3"
    val courseOutcome: String = "CO1",               // e.g. "CO1", "CO2"
    val isGuaranteedPyq: Boolean = false,            // True if repeated in recent past papers
    val pyqYears: List<String> = emptyList(),        // e.g. ["Jan 2024", "Jan 2025"]
    val rubricSteps: List<RubricStep> = emptyList(), // Evaluator mark breakdown
    val checkpoints: List<DerivationCheckpoint> = emptyList(), // 3-stage active recall chunks
    val numericalSolver: NumericalSolver? = null,    // If numerical problem
    val evaluatorTip: String = ""                    // What students usually forget that costs marks
)

/**
 * Converts a legacy ExamModelAnswer into a fully structured UniversityExamQuestion
 * with evaluator rubric steps and 3-stage derivation checkpoints for active recall.
 */
fun ExamModelAnswer.toUniversityExamQuestion(index: Int = 0): UniversityExamQuestion {
    val cleanAnswer = stepByStepAnswer.trim()
    val lines = cleanAnswer.split(Regex("\n(?=\\d+\\.|🔹|•)"))
    val steps = if (lines.size > 1) {
        lines.mapIndexed { idx, line ->
            val lineTrimmed = line.trim()
            val headerMatch = Regex("""^(?:\d+\.\s*|🔹\s*|•\s*)([^:\n]+):?\s*(.*)$""", RegexOption.DOT_MATCHES_ALL).find(lineTrimmed)
            val label = headerMatch?.groupValues?.get(1)?.trim() ?: "Step ${idx + 1}"
            val body = headerMatch?.groupValues?.get(2)?.trim() ?: lineTrimmed
            RubricStep(
                stepNumber = idx + 1,
                label = label,
                marks = maxOf(1, marks / lines.size),
                content = body.ifBlank { lineTrimmed }
            )
        }
    } else {
        listOf(
            RubricStep(
                stepNumber = 1,
                label = "Complete Model Answer & Steps",
                marks = marks,
                content = cleanAnswer
            )
        )
    }

    val checkpoints = if (marks >= 7) {
        listOf(
            DerivationCheckpoint(
                stageIndex = 1,
                stageTitle = "Stage 1: Setup, Assumptions & Governing Statement",
                prompt = "What are the core starting assumptions and definitions for:\n$question?",
                answer = steps.firstOrNull()?.content ?: "State formal principle and draw initial labeled diagram.",
                marks = 2
            ),
            DerivationCheckpoint(
                stageIndex = 2,
                stageTitle = "Stage 2: Core Derivation / Working Logic",
                prompt = "What is the primary intermediate substitution or operational step?",
                answer = steps.getOrNull(1)?.content ?: "Apply governing equation and simplify intermediate terms.",
                marks = maxOf(2, marks - 4)
            ),
            DerivationCheckpoint(
                stageIndex = 3,
                stageTitle = "Stage 3: Final Expression & Applications",
                prompt = "What is the terminal formula and its direct engineering application?",
                answer = steps.lastOrNull()?.content ?: "Final simplified formula with proper SI units and conditions.",
                marks = 2
            )
        )
    } else emptyList()

    return UniversityExamQuestion(
        id = "eq_$index",
        subPart = if (index % 2 == 0) "a" else "b",
        questionText = question,
        marks = marks,
        bloomLevel = if (marks >= 8) "L3" else "L2",
        courseOutcome = "CO1",
        isGuaranteedPyq = marks >= 7,
        rubricSteps = steps,
        checkpoints = checkpoints,
        evaluatorTip = markingTip.ifBlank { "Ensure diagrams are labeled with arrows and final values have explicit SI units." }
    )
}
