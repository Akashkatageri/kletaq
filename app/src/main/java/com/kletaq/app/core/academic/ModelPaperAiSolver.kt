package com.kletaq.app.core.academic

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.kletaq.app.data.model.DerivationCheckpoint
import com.kletaq.app.data.model.PaperModule
import com.kletaq.app.data.model.PaperQuestionGroup
import com.kletaq.app.data.model.QuestionPaper
import com.kletaq.app.data.model.RubricStep
import com.kletaq.app.data.model.UniversityExamQuestion
import com.kletaq.app.data.model.VtuPaperInfo
import com.kletaq.app.data.repository.VtuSubjectQuestionBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ModelPaperAiSolver {

    private const val TAG = "ModelPaperAiSolver"

    suspend fun generateOrSolvePaper(
        paperInfo: VtuPaperInfo,
        moduleThemes: List<String> = emptyList(),
        forceAiGeneration: Boolean = false
    ): QuestionPaper = withContext(Dispatchers.IO) {
        // 1. Serve verified curated paper immediately if available and AI regeneration isn't forced
        if (!forceAiGeneration) {
            VtuSubjectQuestionBank.getCuratedPaper(paperInfo, moduleThemes)?.let { curated ->
                return@withContext curated
            }
        }

        try {
            val themesPrompt = if (moduleThemes.isNotEmpty()) {
                moduleThemes.mapIndexed { idx, t -> "Module ${idx + 1}: $t" }.joinToString("\n")
            } else {
                "Official VTU (${paperInfo.scheme} Scheme) 5-Module curriculum for ${paperInfo.subjectTitle} (${paperInfo.vtuPaperCode})"
            }

            val systemPrompt = """
                You are a senior VTU university examination board paper setter and chief evaluator.
                Given an engineering course title, VTU subject code, and module topics, generate an authentic 100-Mark, 5-Module VTU Model Question Paper with step-by-step evaluator marking rubrics.

                BLUEPRINT RULES (Official VTU 100-Mark SEE Pattern):
                1. Exactly 5 Modules (Modules 1 to 5).
                2. Each module has TWO internal-choice main questions (Module 1: Q1 OR Q2, Module 2: Q3 OR Q4, ..., Module 5: Q9 OR Q10).
                3. Each main question MUST total 20 Marks:
                   - Standard split: 3 sub-questions ("a" [6 Marks], "b" [7 Marks], "c" [7 Marks]) = 20 Marks, OR
                   - Heavy numerical/design split: 2 sub-questions ("a" [10 Marks], "b" [10 Marks]) = 20 Marks.
                4. Every question must be a specific, rigorous university exam problem (with concrete numbers, matrices, circuits, or algorithms—NEVER generic placeholder text).
                5. For each sub-question, provide:
                   - "subPart": "a", "b", or "c"
                   - "questionText": Complete university exam question with given values/conditions
                   - "marks": 6, 7, 8, or 10 (summing to 20 per main question)
                   - "bloomLevel": "L2" or "L3"
                   - "rubric": Array of 3 evaluator marking steps whose marks sum to the sub-question's marks
                   - "finalAnswer": Exact numerical result with SI units or final derived equation

                OUTPUT FORMAT:
                Output ONLY a single valid JSON object matching this exact schema:
                {
                  "modules": [
                    {
                      "moduleNumber": 1,
                      "title": "Module 1 Title",
                      "questionA": {
                        "questionNumber": 1,
                        "subQuestions": [
                          {
                            "subPart": "a",
                            "questionText": "...",
                            "marks": 6,
                            "bloomLevel": "L2",
                            "rubric": [
                              { "step": 1, "label": "Governing Principle / Formula", "marks": 2, "content": "..." },
                              { "step": 2, "label": "Step-by-Step Derivation / Working", "marks": 3, "content": "..." },
                              { "step": 3, "label": "Final Answer & Units", "marks": 1, "content": "..." }
                            ],
                            "finalAnswer": "..."
                          }
                        ]
                      },
                      "questionB": {
                        "questionNumber": 2,
                        "subQuestions": []
                      }
                    }
                  ]
                }
            """.trimIndent()

            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-lite",
                generationConfig = generationConfig {
                    maxOutputTokens = 8192
                    temperature = 0.15f
                    responseMimeType = "application/json"
                },
                systemInstruction = content { text(systemPrompt) }
            )

            val prompt = "Generate a complete 100-mark VTU model question paper with evaluator rubrics for:\n" +
                "Course: ${paperInfo.subjectTitle} (${paperInfo.vtuPaperCode})\n" +
                "Scheme: ${paperInfo.scheme} | Semester: ${paperInfo.semester}\n" +
                "Modules:\n$themesPrompt"

            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim() ?: ""

            val cleanJson = jsonText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedPaper = parseModelPaperJson(cleanJson, paperInfo)
            if (parsedPaper != null && parsedPaper.modules.size == 5) {
                return@withContext parsedPaper
            }

            Log.w(TAG, "Gemini JSON parsing incomplete, falling back to deterministic paper")
            return@withContext buildDeterministicPaper(paperInfo, moduleThemes)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini paper generation error: ${e.message}, falling back to deterministic paper")
            return@withContext buildDeterministicPaper(paperInfo, moduleThemes)
        }
    }

    private fun parseModelPaperJson(jsonStr: String, paperInfo: VtuPaperInfo): QuestionPaper? {
        return try {
            val root = JSONObject(jsonStr)
            val modulesArr = root.optJSONArray("modules") ?: return null
            val moduleList = mutableListOf<PaperModule>()
            val codePrefix = paperInfo.vtuPaperCode.lowercase().trim()

            for (m in 0 until modulesArr.length()) {
                val mObj = modulesArr.getJSONObject(m)
                val mNum = mObj.optInt("moduleNumber", m + 1)
                val mTitle = mObj.optString("title", "Module $mNum")

                val qAObj = mObj.optJSONObject("questionA")
                val qBObj = mObj.optJSONObject("questionB")

                val qA = parseQuestionGroup(qAObj, (mNum * 2) - 1, mNum, codePrefix)
                val qB = parseQuestionGroup(qBObj, mNum * 2, mNum, codePrefix)

                moduleList.add(
                    PaperModule(
                        moduleNumber = mNum,
                        title = mTitle,
                        questionA = qA,
                        questionB = qB
                    )
                )
            }

            QuestionPaper(
                id = "paper_${paperInfo.vtuPaperCode}",
                subjectCode = paperInfo.vtuPaperCode,
                subjectName = paperInfo.subjectTitle,
                vtuCode = paperInfo.vtuPaperCode,
                officialPdfUrl = paperInfo.officialPdfUrl,
                semester = paperInfo.semester,
                scheme = paperInfo.scheme,
                maxMarks = paperInfo.maxMarks,
                durationMinutes = paperInfo.durationMinutes,
                modules = moduleList,
                isAiSolved = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing paper JSON: ${e.message}", e)
            null
        }
    }

    private fun parseQuestionGroup(
        obj: JSONObject?,
        defaultNum: Int,
        moduleNum: Int,
        codePrefix: String
    ): PaperQuestionGroup {
        if (obj == null) {
            return buildFallbackQuestionGroup(defaultNum, moduleNum, codePrefix)
        }

        val qNum = obj.optInt("questionNumber", defaultNum)
        val subArr = obj.optJSONArray("subQuestions") ?: JSONArray()
        val subList = mutableListOf<UniversityExamQuestion>()

        for (s in 0 until subArr.length()) {
            val sObj = subArr.getJSONObject(s)
            val defaultSubPart = ('a' + s).toString()
            val subPart = sObj.optString("subPart", defaultSubPart)
            val qText = sObj.optString("questionText", "Explain core principles and applications.")
            val marks = sObj.optInt("marks", 7)
            val bloom = sObj.optString("bloomLevel", if (marks >= 7) "L3" else "L2")

            // Support both "rubric" and "evaluatorRubric" in case the model uses either key
            val rubricArr = sObj.optJSONArray("rubric")
                ?: sObj.optJSONArray("evaluatorRubric")
                ?: JSONArray()

            val rubricSteps = mutableListOf<RubricStep>()
            for (r in 0 until rubricArr.length()) {
                val rObj = rubricArr.getJSONObject(r)
                rubricSteps.add(
                    RubricStep(
                        stepNumber = rObj.optInt("step", r + 1),
                        label = rObj.optString("label", "Step ${r + 1}"),
                        marks = rObj.optInt("marks", 2),
                        content = rObj.optString("content", rObj.optString("explanation", ""))
                    )
                )
            }

            val finalAns = sObj.optString("finalAnswer", rubricSteps.lastOrNull()?.content ?: "Final verified expression.")

            val checkpoints = if (marks >= 6) {
                listOf(
                    DerivationCheckpoint(
                        stageIndex = 1,
                        stageTitle = "Stage 1: Setup & Governing Statements",
                        prompt = "State formal definitions and initial assumptions:",
                        answer = rubricSteps.firstOrNull()?.content ?: "State formal principles and draw labeled diagrams.",
                        marks = 2
                    ),
                    DerivationCheckpoint(
                        stageIndex = 2,
                        stageTitle = "Stage 2: Core Derivation / Logic",
                        prompt = "Derive governing equations and intermediate substitutions:",
                        answer = rubricSteps.getOrNull(1)?.content ?: "Apply standard theorems and simplify.",
                        marks = maxOf(2, marks - 4)
                    ),
                    DerivationCheckpoint(
                        stageIndex = 3,
                        stageTitle = "Stage 3: Final Expressions & Boundary Limits",
                        prompt = "What is the final formulation?",
                        answer = finalAns,
                        marks = 2
                    )
                )
            } else emptyList()

            subList.add(
                UniversityExamQuestion(
                    id = "${codePrefix}_q${qNum}_$subPart",
                    subPart = subPart,
                    questionText = qText,
                    marks = marks,
                    bloomLevel = bloom,
                    courseOutcome = "CO$moduleNum",
                    isGuaranteedPyq = marks >= 7,
                    rubricSteps = rubricSteps.ifEmpty {
                        listOf(
                            RubricStep(1, "Conceptual Statement & Diagram", maxOf(1, marks / 3), "Define core principles and illustrate schematic diagram."),
                            RubricStep(2, "Derivation / Algorithm Steps", maxOf(2, marks / 2), "Detailed step-by-step mathematical working or code implementation."),
                            RubricStep(3, "Final Formulation & Units", maxOf(1, marks - (marks / 3) - (marks / 2)), finalAns)
                        )
                    },
                    checkpoints = checkpoints,
                    evaluatorTip = "Ensure diagrams are clearly labeled and intermediate steps state governing assumptions."
                )
            )
        }

        return PaperQuestionGroup(
            questionNumber = qNum,
            subQuestions = subList.ifEmpty { buildFallbackQuestionGroup(defaultNum, moduleNum, codePrefix).subQuestions }
        )
    }

    private fun buildFallbackQuestionGroup(qNum: Int, moduleNum: Int, codePrefix: String): PaperQuestionGroup {
        return PaperQuestionGroup(
            questionNumber = qNum,
            subQuestions = listOf(
                UniversityExamQuestion(
                    id = "${codePrefix}_q${qNum}_a",
                    subPart = "a",
                    questionText = "State and derive the core governing equations and operating principles for Module $moduleNum.",
                    marks = 6,
                    bloomLevel = "L2",
                    courseOutcome = "CO$moduleNum",
                    isGuaranteedPyq = true,
                    rubricSteps = listOf(
                        RubricStep(1, "Statement & Diagram", 2, "Define governing statement and provide neatly labeled diagrams."),
                        RubricStep(2, "Mathematical Working", 2, "Step-by-step derivation and intermediate substitution."),
                        RubricStep(3, "Final Expression", 2, "Final formula with SI units and application context.")
                    )
                ),
                UniversityExamQuestion(
                    id = "${codePrefix}_q${qNum}_b",
                    subPart = "b",
                    questionText = "Explain the step-by-step algorithmic procedure and functional architecture for Module $moduleNum.",
                    marks = 7,
                    bloomLevel = "L2",
                    courseOutcome = "CO$moduleNum",
                    isGuaranteedPyq = true,
                    rubricSteps = listOf(
                        RubricStep(1, "Architecture Diagram", 2, "Draw labeled block diagram or flowchart."),
                        RubricStep(2, "Step-by-Step Execution", 3, "Trace control flow and operational stages."),
                        RubricStep(3, "Complexity & Applications", 2, "Analyze performance and practical constraints.")
                    )
                ),
                UniversityExamQuestion(
                    id = "${codePrefix}_q${qNum}_c",
                    subPart = "c",
                    questionText = "Solve the applied engineering numerical or design problem for Module $moduleNum.",
                    marks = 7,
                    bloomLevel = "L3",
                    courseOutcome = "CO$moduleNum",
                    isGuaranteedPyq = true,
                    rubricSteps = listOf(
                        RubricStep(1, "Given Data & Formulas", 2, "List given boundary conditions and governing formulas."),
                        RubricStep(2, "Substitution Steps", 3, "Compute intermediate values with clear working."),
                        RubricStep(3, "Final Solution & Units", 2, "Final magnitude with explicit units.")
                    )
                )
            )
        )
    }

    fun buildDeterministicPaper(
        paperInfo: VtuPaperInfo,
        moduleThemes: List<String> = emptyList()
    ): QuestionPaper {
        return VtuSubjectQuestionBank.getCuratedPaper(paperInfo, moduleThemes)
            ?: VtuSubjectQuestionBank.buildDynamicPaperFallback(paperInfo, moduleThemes)
    }
}
