package com.kletaq.app.data.repository

import android.util.Log
import com.kletaq.app.core.di.KletaqApplication
import com.kletaq.app.data.model.CodeSnippet
import com.kletaq.app.data.model.ExamModelAnswer
import com.kletaq.app.data.model.FormulaItem
import com.kletaq.app.data.model.PyqItem
import com.kletaq.app.data.model.QuizQuestion
import com.kletaq.app.data.model.TopicStudyPack
import com.kletaq.app.features.journey.components.ExamPrep
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * Universal Academic Content Repository.
 * Resolves verified syllabus study packs from local bundled assets and cache.
 * Eliminates empty placeholder lessons across all subjects.
 */
object AcademicContentRepository {
    private const val TAG = "AcademicContentRepo"

    // In-memory cache of loaded subject packs: subjectCode -> (topicId -> TopicStudyPack)
    private val subjectPacksCache = mutableMapOf<String, Map<String, TopicStudyPack>>()

    fun getStudyPack(
        topicId: String,
        topicTitle: String,
        subjectCode: String = "",
        subjectName: String = ""
    ): TopicStudyPack {
        val resolvedCode = resolveSubjectCode(topicId, subjectCode, subjectName)

        val subjectMap = getOrLoadSubjectMap(resolvedCode)
        val pack = subjectMap[topicId]
        if (pack != null) {
            return pack
        }

        // Try lookup by case-insensitive key
        val caseInsensitive = subjectMap.entries.firstOrNull {
            it.key.equals(topicId, ignoreCase = true)
        }?.value
        if (caseInsensitive != null) return caseInsensitive

        // Try match by module and topic index (e.g., m1-t1 or m1_t1)
        val moduleTopicRegex = Regex("m(\\d+)[_-]t(\\d+)", RegexOption.IGNORE_CASE)
        val targetMatch = moduleTopicRegex.find(topicId)
        if (targetMatch != null) {
            val (mNum, tNum) = targetMatch.destructured
            val matchedByMt = subjectMap.entries.firstOrNull {
                val keyMatch = moduleTopicRegex.find(it.key)
                if (keyMatch != null) {
                    val (km, kt) = keyMatch.destructured
                    km == mNum && kt == tNum
                } else false
            }?.value
            if (matchedByMt != null) return matchedByMt
        }

        // Try fuzzy match by topicId substring or title similarity
        val fuzzyMatch = subjectMap.entries.firstOrNull {
            topicId.contains(it.key, ignoreCase = true) ||
            it.key.contains(topicId, ignoreCase = true) ||
            (topicTitle.isNotBlank() && it.value.title.isNotBlank() && (
                it.value.title.contains(topicTitle, ignoreCase = true) ||
                topicTitle.contains(it.value.title, ignoreCase = true)
            ))
        }?.value

        if (fuzzyMatch != null) {
            return fuzzyMatch
        }

        // Return structured baseline topic pack based on syllabus topic
        return generateBaselinePack(topicId, topicTitle, resolvedCode, subjectName)
    }

    private fun resolveSubjectCode(topicId: String, subjectCode: String, subjectName: String): String {
        val codeRegex = Regex("[0-9]?[A-Z]{2,4}[0-9]{3}[A-Z]?")
        if (subjectCode.isNotBlank()) {
            val clean = subjectCode.replace("(", "").replace(")", "").trim().uppercase()
            val match = codeRegex.find(clean)
            if (match != null) return match.value
        }
        val topicMatch = codeRegex.find(topicId.uppercase())
        if (topicMatch != null) {
            return topicMatch.value
        }
        val nameMatch = codeRegex.find(subjectName.uppercase())
        if (nameMatch != null) {
            return nameMatch.value
        }
        return when {
            subjectName.contains("python", ignoreCase = true) -> "1BPLC105B"
            subjectName.contains("java", ignoreCase = true) -> "1BCS302"
            subjectName.contains("data structure", ignoreCase = true) && subjectName.contains("lab", ignoreCase = true) -> "1BCSL306"
            subjectName.contains("data structure", ignoreCase = true) -> "1BCS305"
            subjectName.contains("git", ignoreCase = true) -> "1BCSL307A"
            subjectName.contains("operating", ignoreCase = true) -> "1BCS304"
            subjectName.contains("digital", ignoreCase = true) -> "1BCS303"
            subjectName.contains("probability", ignoreCase = true) || subjectName.contains("math", ignoreCase = true) -> "1BCS301"
            else -> "GENERAL"
        }
    }

    @Synchronized
    private fun getOrLoadSubjectMap(subjectCode: String): Map<String, TopicStudyPack> {
        val cached = subjectPacksCache[subjectCode]
        if (cached != null) return cached

        val possibleAssetPaths = if (subjectCode.equals("1BPLC105B", ignoreCase = true)) {
            listOf(
                "academic/subjects/1BPLC105B.json",
                "academic/subjects/python/1BPLC105B.json"
            )
        } else {
            listOf(
                "academic/subjects/$subjectCode.json",
                "academic/subjects/${subjectCode.lowercase()}.json"
            )
        }

        for (path in possibleAssetPaths) {
            try {
                val stream: InputStream = KletaqApplication.appContext.assets.open(path)
                val jsonString = stream.bufferedReader().use { it.readText() }
                val parsed = parseSubjectJson(jsonString, subjectCode)
                if (parsed.isNotEmpty()) {
                    subjectPacksCache[subjectCode] = parsed
                    return parsed
                }
            } catch (_: Exception) {
                // Try next path
            }
        }

        return emptyMap()
    }

    private fun parseSubjectJson(jsonString: String, defaultSubjectCode: String): Map<String, TopicStudyPack> {
        val resultMap = mutableMapOf<String, TopicStudyPack>()
        try {
            val root = JSONObject(jsonString)
            val subjectId = root.optString("subjectId", defaultSubjectCode)
            val lessonsObj = root.optJSONObject("lessons") ?: return emptyMap()

            val keys = lessonsObj.keys()
            while (keys.hasNext()) {
                val topicId = keys.next()
                val obj = lessonsObj.getJSONObject(topicId)

                // Support both new rich TopicStudyPack schema and legacy Python schema
                val simpleConcept = obj.optString("simpleConcept", "").ifEmpty {
                    obj.optString("concept", "")
                }
                val technicalConcept = obj.optString("technicalConcept", "").ifEmpty {
                    obj.optString("concept", "")
                }

                val keyPointsList = mutableListOf<String>()
                if (obj.has("keyPoints")) {
                    val arr = obj.optJSONArray("keyPoints")
                    if (arr != null) {
                        for (i in 0 until arr.length()) keyPointsList.add(arr.getString(i))
                    }
                }

                val snippetsList = mutableListOf<CodeSnippet>()
                if (obj.has("codeSnippets")) {
                    val arr = obj.optJSONArray("codeSnippets")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val snipObj = arr.getJSONObject(i)
                            snippetsList.add(
                                CodeSnippet(
                                    title = snipObj.optString("title", "Code Example"),
                                    language = snipObj.optString("language", "java"),
                                    code = snipObj.optString("code", ""),
                                    explanation = snipObj.optString("explanation", "")
                                )
                            )
                        }
                    }
                }

                val formulasList = mutableListOf<FormulaItem>()
                if (obj.has("formulas")) {
                    val arr = obj.optJSONArray("formulas")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val fObj = arr.getJSONObject(i)
                            formulasList.add(
                                FormulaItem(
                                    name = fObj.optString("name", ""),
                                    expression = fObj.optString("expression", ""),
                                    description = fObj.optString("description", "")
                                )
                            )
                        }
                    }
                }

                val modelAnswersList = mutableListOf<ExamModelAnswer>()
                if (obj.has("examModelAnswers")) {
                    val arr = obj.optJSONArray("examModelAnswers")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val ansObj = arr.getJSONObject(i)
                            modelAnswersList.add(
                                ExamModelAnswer(
                                    question = ansObj.optString("question", ""),
                                    marks = ansObj.optInt("marks", 5),
                                    stepByStepAnswer = ansObj.optString("stepByStepAnswer", ansObj.optString("answer", "")),
                                    markingTip = ansObj.optString("markingTip", "")
                                )
                            )
                        }
                    }
                } else if (obj.has("fiveMarkAnswer")) {
                    val fiveMark = obj.optString("fiveMarkAnswer")
                    val legacyQuestions = obj.optJSONArray("practiceQuestions")
                    val primaryQ = if (legacyQuestions != null && legacyQuestions.length() > 0) legacyQuestions.getString(0) else "VTU Exam Model Question"
                    modelAnswersList.add(
                        ExamModelAnswer(
                            question = primaryQ,
                            marks = 5,
                            stepByStepAnswer = fiveMark,
                            markingTip = "Step-by-step points with examples are required for full marks."
                        )
                    )
                }

                val pyqList = mutableListOf<PyqItem>()
                if (obj.has("pyqs")) {
                    val arr = obj.optJSONArray("pyqs")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val pObj = arr.getJSONObject(i)
                            pyqList.add(
                                PyqItem(
                                    year = pObj.optString("year", "VTU Exam"),
                                    marks = pObj.optInt("marks", 5),
                                    question = pObj.optString("question", "")
                                )
                            )
                        }
                    }
                }

                val quizList = mutableListOf<QuizQuestion>()
                if (obj.has("quickQuiz")) {
                    val arr = obj.optJSONArray("quickQuiz")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val qObj = arr.getJSONObject(i)
                            val optsArr = qObj.optJSONArray("options")
                            val options = mutableListOf<String>()
                            if (optsArr != null) {
                                for (j in 0 until optsArr.length()) options.add(optsArr.getString(j))
                            }
                            quizList.add(
                                QuizQuestion(
                                    question = qObj.optString("question", ""),
                                    options = options,
                                    correctIndex = qObj.optInt("correctIndex", 0),
                                    explanation = qObj.optString("explanation", "")
                                )
                            )
                        }
                    }
                }

                val quickRecall = obj.optString("quickRecall", "")

                resultMap[topicId] = TopicStudyPack(
                    topicId = topicId,
                    title = obj.optString("title", topicId),
                    subjectCode = subjectId,
                    simpleConcept = simpleConcept,
                    technicalConcept = technicalConcept,
                    keyPoints = keyPointsList,
                    codeSnippets = snippetsList,
                    formulas = formulasList,
                    examModelAnswers = modelAnswersList,
                    pyqs = pyqList,
                    quickQuiz = quizList,
                    quickRecall = quickRecall,
                    isAiGenerated = obj.optBoolean("isAiGenerated", false)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subject JSON: ${e.message}")
        }
        return resultMap
    }

    private fun generateBaselinePack(
        topicId: String,
        topicTitle: String,
        subjectCode: String,
        subjectName: String
    ): TopicStudyPack {
        return TopicStudyPack(
            topicId = topicId,
            title = topicTitle,
            subjectCode = subjectCode,
            simpleConcept = "In $subjectName, $topicTitle covers foundational principles essential for understanding engineering workflows and exam problem solving.",
            technicalConcept = "This topic explores the core syllabus definitions, operational mechanics, and theoretical models of $topicTitle in accordance with the official scheme guidelines.",
            keyPoints = listOf(
                "Understand the primary definitions and purpose of $topicTitle",
                "Trace the step-by-step algorithms, equations, or syntax rules",
                "Recognize common edge cases and practical implementations in $subjectName"
            ),
            examModelAnswers = listOf(
                ExamModelAnswer(
                    question = "Explain the fundamental principles and applications of $topicTitle.",
                    marks = 5,
                    stepByStepAnswer = "1. Definition & Core Purpose:\nExplain what $topicTitle accomplishes in $subjectName.\n\n2. Key Architectural/Operational Steps:\nBreak down the primary workflow, syntax, or equations.\n\n3. Engineering Applications:\nCite real-world applications and exam problem contexts.",
                    markingTip = "Provide clear definitions, point-wise explanation, and illustrative diagram/example."
                )
            ),
            quickRecall = "$topicTitle: Core definition ➔ step-by-step workflow ➔ exam example.",
            isAiGenerated = true
        )
    }
}

/**
 * Adapter to convert TopicStudyPack to ExamPrep for backwards compatibility with
 * existing widgets and sheets.
 */
fun TopicStudyPack.toExamPrep(): ExamPrep {
    return ExamPrep(
        learnSummary = simpleConcept.ifEmpty { technicalConcept },
        fiveMarkAnswer = examModelAnswers.firstOrNull()?.stepByStepAnswer ?: keyPoints.joinToString("\n• "),
        practiceQuestions = examModelAnswers.map { it.question }.ifEmpty { pyqs.map { it.question } },
        recallPrompt = quickRecall.ifEmpty { keyPoints.joinToString(" • ") }
    )
}
