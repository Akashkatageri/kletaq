package com.kletaq.app.features.examsprint.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.kletaq.app.core.ai.UnifiedAiClient
import com.kletaq.app.data.repository.AcademicContentRepository
import com.kletaq.app.features.examsprint.model.QuestionMastery
import com.kletaq.app.features.examsprint.model.SprintQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 🧠 AI & Syllabus Extractor for Exam Sprints.
 * Zero-Hardcoding compliant: pulls from verified syllabus assets or user inputs via UnifiedAiClient.
 */
object ExamSprintAiHelper {

    private const val TAG = "ExamSprintAiHelper"

    /**
     * Parses raw user-pasted text into structured SprintQuestion objects.
     * Automatically differentiates between Questions-Only and Question-plus-Answer pairs.
     */
    suspend fun parsePastedQuestionsAndAnswers(
        rawText: String,
        subjectName: String = "",
        subjectCode: String = ""
    ): List<SprintQuestion> = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) return@withContext emptyList()

        val prompt = """
            You are a university engineering exam evaluator.
            Given this raw pasted assignment text from a student for the course "$subjectName ($subjectCode)",
            extract each distinct question and any provided student answer.

            For each item:
            1. If an answer was already provided in the student's text, extract it verbatim into "studentAnswer".
            2. If no answer was provided, generate a high-scoring university model answer in "aiModelAnswer" (with bullet points, formulas, ASCII diagram or code where relevant).
            3. Provide "quickRecall": 1 to 2 sentence core mental model.
            4. Provide "markingRubric": Array of 3 to 4 scoring anchors (e.g. ["[2M] Governing definition & principle", "[3M] Architecture/Diagram", "[5M] Mathematical derivation or working"]).
            5. Provide "marks": 5 or 10.

            OUTPUT FORMAT:
            Output ONLY a valid JSON object matching this schema:
            {
              "items": [
                {
                  "questionNumber": 1,
                  "questionText": "...",
                  "marks": 10,
                  "studentAnswer": "..." (or null if none was in the text),
                  "aiModelAnswer": "...",
                  "quickRecall": "...",
                  "markingRubric": ["...", "..."]
                }
              ]
            }
            Do NOT output markdown backticks or explanations.
        """.trimIndent()

        try {
            val response = UnifiedAiClient.generateContent(
                systemInstruction = prompt,
                userPrompt = "Raw assignment text:\n$rawText",
                temperature = 0.2f
            )

            val jsonText = response.getOrNull()?.trim() ?: ""
            val cleanJson = jsonText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            if (cleanJson.isNotBlank() && cleanJson.startsWith("{")) {
                val parsed = parseQuestionsJson(cleanJson)
                if (parsed.isNotEmpty()) return@withContext parsed
            }
        } catch (e: Exception) {
            Log.w(TAG, "UnifiedAiClient parsing failed, falling back to deterministic parser: ${e.message}")
        }

        // Offline deterministic fallback parser (never blocks user)
        return@withContext parseTextDeterministically(rawText)
    }

    /**
     * Parses an uploaded assignment PDF or camera image using multimodal Gemini.
     */
    suspend fun parseUploadedAssignment(
        fileBytes: ByteArray,
        mimeType: String,
        subjectName: String = ""
    ): Result<List<SprintQuestion>> = withContext(Dispatchers.IO) {
        if (fileBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("File is empty."))
        }

        val systemPrompt = """
            You are an expert university examiner.
            Read this assignment / question sheet / solved notes document carefully for course "$subjectName".
            Extract every single question and any written answer/solution.
            
            OUTPUT FORMAT:
            Output ONLY a JSON object:
            {
              "items": [
                {
                  "questionNumber": 1,
                  "questionText": "...",
                  "marks": 10,
                  "studentAnswer": "..." (or null if unwritten),
                  "aiModelAnswer": "...",
                  "quickRecall": "...",
                  "markingRubric": ["...", "..."]
                }
              ]
            }
            Do NOT output backticks or commentary.
        """.trimIndent()

        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-lite",
                generationConfig = generationConfig {
                    maxOutputTokens = 8192
                    temperature = 0.1f
                },
                systemInstruction = content { text(systemPrompt) }
            )

            val promptContent = Content(
                role = "user",
                parts = listOf(
                    InlineDataPart(fileBytes, mimeType),
                    TextPart("Extract all questions and answers from this assignment document.")
                )
            )

            val response = model.generateContent(promptContent)
            val jsonText = (response.text ?: "").trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val questions = parseQuestionsJson(jsonText)
            if (questions.isNotEmpty()) {
                return@withContext Result.success(questions)
            } else {
                return@withContext Result.failure(Exception("Could not extract questions from document."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Multimodal parsing failed: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Extracts questions from verified syllabus assets (Rule 7 Zero-Hardcoding compliant)
     * for selected syllabus modules or topics.
     */
    fun extractFromSyllabus(
        subjectCode: String,
        subjectName: String,
        selectedTopicIds: List<String>
    ): List<SprintQuestion> {
        val result = mutableListOf<SprintQuestion>()
        var questionCounter = 1

        for (topicId in selectedTopicIds) {
            val studyPack = AcademicContentRepository.getStudyPack(
                topicId = topicId,
                topicTitle = "",
                subjectCode = subjectCode,
                subjectName = subjectName
            )

            // Extract exam model answers
            studyPack.examModelAnswers.forEach { modelAns ->
                result.add(
                    SprintQuestion(
                        id = UUID.randomUUID().toString(),
                        questionNumber = questionCounter++,
                        questionText = modelAns.question,
                        marks = modelAns.marks,
                        topicOrModule = studyPack.title,
                        aiModelAnswer = modelAns.stepByStepAnswer,
                        quickRecall = studyPack.quickRecall.ifBlank { studyPack.simpleConcept },
                        markingRubric = if (modelAns.markingTip.isNotBlank()) {
                            listOf(modelAns.markingTip)
                        } else {
                            listOf("Definition & core concept", "Derivation or detailed diagram", "Final working & units")
                        },
                        masteryStatus = QuestionMastery.UNATTEMPTED
                    )
                )
            }

            // Extract PYQs
            studyPack.pyqs.forEach { pyq ->
                result.add(
                    SprintQuestion(
                        id = UUID.randomUUID().toString(),
                        questionNumber = questionCounter++,
                        questionText = pyq.question,
                        marks = pyq.marks,
                        topicOrModule = studyPack.title,
                        aiModelAnswer = studyPack.technicalConcept,
                        quickRecall = studyPack.quickRecall.ifBlank { studyPack.simpleConcept },
                        markingRubric = listOf("Core principle & definition", "Step-by-step logic", "Final outcome"),
                        masteryStatus = QuestionMastery.UNATTEMPTED
                    )
                )
            }
        }

        return result
    }

    /**
     * Helper to parse JSON output into SprintQuestion instances.
     */
    private fun parseQuestionsJson(jsonText: String): List<SprintQuestion> {
        val list = mutableListOf<SprintQuestion>()
        try {
            val root = JSONObject(jsonText)
            val items = root.optJSONArray("items") ?: return emptyList()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val rubricArr = item.optJSONArray("markingRubric")
                val rubricList = mutableListOf<String>()
                if (rubricArr != null) {
                    for (j in 0 until rubricArr.length()) {
                        rubricList.add(rubricArr.getString(j))
                    }
                }

                val studentAns = if (item.has("studentAnswer") && !item.isNull("studentAnswer")) {
                    item.getString("studentAnswer").takeIf { it.isNotBlank() }
                } else null

                list.add(
                    SprintQuestion(
                        id = UUID.randomUUID().toString(),
                        questionNumber = item.optInt("questionNumber", i + 1),
                        questionText = item.optString("questionText", ""),
                        marks = item.optInt("marks", 10),
                        topicOrModule = item.optString("topicOrModule", ""),
                        studentAnswer = studentAns,
                        aiModelAnswer = item.optString("aiModelAnswer", ""),
                        isUserAnswerCustomized = studentAns != null,
                        quickRecall = item.optString("quickRecall", ""),
                        markingRubric = rubricList,
                        masteryStatus = QuestionMastery.UNATTEMPTED
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing questions JSON", e)
        }
        return list
    }

    /**
     * Deterministic regex parser when offline. Splits on standard numbering (1., 2., Q1:, etc.).
     */
    private fun parseTextDeterministically(text: String): List<SprintQuestion> {
        val lines = text.lines()
        val list = mutableListOf<SprintQuestion>()
        var currentQNumber = 1
        var currentQText = StringBuilder()
        var currentAnsText = StringBuilder()
        var isCapturingAnswer = false

        fun flushCurrent() {
            if (currentQText.isNotBlank()) {
                val qStr = currentQText.toString().trim()
                val aStr = currentAnsText.toString().trim().takeIf { it.isNotBlank() }
                list.add(
                    SprintQuestion(
                        id = UUID.randomUUID().toString(),
                        questionNumber = currentQNumber++,
                        questionText = qStr,
                        marks = 10,
                        studentAnswer = aStr,
                        aiModelAnswer = aStr ?: "Answer to be synthesized or reviewed with classroom notes.",
                        isUserAnswerCustomized = aStr != null,
                        quickRecall = "Key concept: review core definition and primary formula/diagram.",
                        markingRubric = listOf(
                            "[2M] Statement & Principle",
                            "[3M] Diagram or Core Architecture",
                            "[5M] Complete Explanation and Steps"
                        ),
                        masteryStatus = QuestionMastery.UNATTEMPTED
                    )
                )
                currentQText = StringBuilder()
                currentAnsText = StringBuilder()
                isCapturingAnswer = false
            }
        }

        val questionHeaderRegex = Regex("^(?:Q(?:uestion)?\\s*\\d+|\\d+)[:\\.)\\s]+(.*)", RegexOption.IGNORE_CASE)
        val answerHeaderRegex = Regex("^(?:A(?:nswer)?|Ans)[:\\.)\\s]+(.*)", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val qMatch = questionHeaderRegex.find(trimmed)
            val aMatch = answerHeaderRegex.find(trimmed)

            if (qMatch != null) {
                flushCurrent()
                currentQText.append(qMatch.groupValues[1])
                isCapturingAnswer = false
            } else if (aMatch != null) {
                isCapturingAnswer = true
                currentAnsText.append(aMatch.groupValues[1]).append("\n")
            } else {
                if (isCapturingAnswer) {
                    currentAnsText.append(trimmed).append("\n")
                } else {
                    if (currentQText.isEmpty()) {
                        currentQText.append(trimmed)
                    } else {
                        currentQText.append(" ").append(trimmed)
                    }
                }
            }
        }
        flushCurrent()

        return list
    }
}

