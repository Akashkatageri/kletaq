package com.kletaq.app.features.examsprint.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kletaq.app.core.di.KletaqApplication
import com.kletaq.app.features.examsprint.model.ExamSprint
import com.kletaq.app.features.examsprint.model.QuestionMastery
import com.kletaq.app.features.examsprint.model.SprintQuestion
import com.kletaq.app.widgets.data.WidgetDataHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 📦 Offline-First Repository for Exam Sprints.
 * Stores sprints and questions with 0ms local SharedPreferences persistence.
 */
object ExamSprintRepository {

    private const val TAG = "ExamSprintRepository"
    private const val PREFS_NAME = "kletaq_exam_sprints_store"
    private const val KEY_SPRINTS_JSON = "sprints_json"

    private val _sprints = MutableStateFlow<List<ExamSprint>>(emptyList())
    val sprints: StateFlow<List<ExamSprint>> = _sprints.asStateFlow()

    init {
        loadFromStorage()
    }

    private fun getPrefs(): SharedPreferences {
        val ctx = KletaqApplication.appContext
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    private fun loadFromStorage() {
        try {
            val jsonStr = getPrefs().getString(KEY_SPRINTS_JSON, null)
            if (jsonStr.isNullOrBlank()) {
                _sprints.value = emptyList()
                return
            }

            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ExamSprint>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseSprint(obj))
            }
            _sprints.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sprints from storage", e)
            _sprints.value = emptyList()
        }
    }

    @Synchronized
    private fun saveToStorage(list: List<ExamSprint>) {
        _sprints.value = list
        try {
            val jsonArray = JSONArray()
            for (s in list) {
                jsonArray.put(serializeSprint(s))
            }
            getPrefs().edit().putString(KEY_SPRINTS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sprints to storage", e)
        }
    }

    fun getAllSprints(): List<ExamSprint> = _sprints.value

    fun getSprint(sprintId: String): ExamSprint? {
        return _sprints.value.firstOrNull { it.id == sprintId }
    }

    fun saveSprint(sprint: ExamSprint) {
        val current = _sprints.value.toMutableList()
        val index = current.indexOfFirst { it.id == sprint.id }
        if (index >= 0) {
            current[index] = sprint
        } else {
            current.add(0, sprint)
        }
        saveToStorage(current)
    }

    fun deleteSprint(sprintId: String) {
        val current = _sprints.value.toMutableList()
        current.removeAll { it.id == sprintId }
        saveToStorage(current)
    }

    fun updateQuestionMastery(sprintId: String, questionId: String, status: QuestionMastery) {
        val sprint = getSprint(sprintId) ?: return
        val updatedQuestions = sprint.questions.map { q ->
            if (q.id == questionId) {
                q.copy(
                    masteryStatus = status,
                    lastPracticedAt = System.currentTimeMillis()
                )
            } else q
        }
        val updatedSprint = sprint.copy(questions = updatedQuestions)
        saveSprint(updatedSprint)
    }

    fun updateQuestionCustomAnswer(sprintId: String, questionId: String, newAnswer: String) {
        val sprint = getSprint(sprintId) ?: return
        val updatedQuestions = sprint.questions.map { q ->
            if (q.id == questionId) {
                q.copy(
                    studentAnswer = newAnswer,
                    isUserAnswerCustomized = true
                )
            } else q
        }
        val updatedSprint = sprint.copy(questions = updatedQuestions)
        saveSprint(updatedSprint)
    }

    /**
     * Samples target count questions (typically 5 to 10) from the sprint pool.
     * Uses smart weighting: prioritizes UNATTEMPTED and NEEDS_REVIEW questions,
     * while maintaining stochastic variety to mirror the teacher's real test.
     */
    fun sampleMockQuestions(sprintId: String, requestedCount: Int? = null): List<SprintQuestion> {
        val sprint = getSprint(sprintId) ?: return emptyList()
        if (sprint.questions.isEmpty()) return emptyList()

        val count = (requestedCount ?: sprint.targetTestQuestionsCount).coerceIn(1, sprint.questions.size)

        val unattempted = sprint.questions.filter { it.masteryStatus == QuestionMastery.UNATTEMPTED }.shuffled()
        val needsReview = sprint.questions.filter { it.masteryStatus == QuestionMastery.NEEDS_REVIEW }.shuffled()
        val mastered = sprint.questions.filter { it.masteryStatus == QuestionMastery.MASTERED }.shuffled()

        val pool = (unattempted + needsReview + mastered)
        return pool.take(count).shuffled()
    }

    fun recordMockTestResult(sprintId: String, score: Int, maxMarks: Int) {
        val sprint = getSprint(sprintId) ?: return
        val newCount = sprint.mockTestsTaken + 1
        val newBest = maxOf(sprint.bestScore, score)
        saveSprint(sprint.copy(mockTestsTaken = newCount, bestScore = newBest))

        // Trigger widget refresh
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetDataHelper.refreshWidgets(KletaqApplication.appContext)
            } catch (_: Exception) {}
        }
    }

    // --- JSON Serialization ---

    private fun serializeSprint(sprint: ExamSprint): JSONObject {
        val obj = JSONObject()
        obj.put("id", sprint.id)
        obj.put("title", sprint.title)
        obj.put("subjectCode", sprint.subjectCode)
        obj.put("subjectName", sprint.subjectName)
        obj.put("examDateMillis", sprint.examDateMillis)
        obj.put("targetTestQuestionsCount", sprint.targetTestQuestionsCount)
        obj.put("targetTestDurationMinutes", sprint.targetTestDurationMinutes)
        obj.put("mockTestsTaken", sprint.mockTestsTaken)
        obj.put("bestScore", sprint.bestScore)
        obj.put("createdAt", sprint.createdAt)

        val qArray = JSONArray()
        for (q in sprint.questions) {
            val qObj = JSONObject()
            qObj.put("id", q.id)
            qObj.put("questionNumber", q.questionNumber)
            qObj.put("questionText", q.questionText)
            qObj.put("marks", q.marks)
            qObj.put("topicOrModule", q.topicOrModule)
            if (q.studentAnswer != null) qObj.put("studentAnswer", q.studentAnswer)
            qObj.put("aiModelAnswer", q.aiModelAnswer)
            qObj.put("isUserAnswerCustomized", q.isUserAnswerCustomized)
            qObj.put("quickRecall", q.quickRecall)

            val rubricArray = JSONArray()
            q.markingRubric.forEach { rubricArray.put(it) }
            qObj.put("markingRubric", rubricArray)

            qObj.put("masteryStatus", q.masteryStatus.name)
            if (q.lastPracticedAt != null) qObj.put("lastPracticedAt", q.lastPracticedAt)
            qArray.put(qObj)
        }
        obj.put("questions", qArray)
        return obj
    }

    private fun parseSprint(obj: JSONObject): ExamSprint {
        val qArray = obj.optJSONArray("questions") ?: JSONArray()
        val questions = mutableListOf<SprintQuestion>()
        for (i in 0 until qArray.length()) {
            val qObj = qArray.getJSONObject(i)
            val rubricList = mutableListOf<String>()
            val rubricArr = qObj.optJSONArray("markingRubric")
            if (rubricArr != null) {
                for (j in 0 until rubricArr.length()) {
                    rubricList.add(rubricArr.getString(j))
                }
            }

            questions.add(
                SprintQuestion(
                    id = qObj.optString("id", "q_$i"),
                    questionNumber = qObj.optInt("questionNumber", i + 1),
                    questionText = qObj.optString("questionText", ""),
                    marks = qObj.optInt("marks", 10),
                    topicOrModule = qObj.optString("topicOrModule", ""),
                    studentAnswer = if (qObj.has("studentAnswer")) qObj.getString("studentAnswer") else null,
                    aiModelAnswer = qObj.optString("aiModelAnswer", ""),
                    isUserAnswerCustomized = qObj.optBoolean("isUserAnswerCustomized", false),
                    quickRecall = qObj.optString("quickRecall", ""),
                    markingRubric = rubricList,
                    masteryStatus = try {
                        QuestionMastery.valueOf(qObj.optString("masteryStatus", QuestionMastery.UNATTEMPTED.name))
                    } catch (_: Exception) {
                        QuestionMastery.UNATTEMPTED
                    },
                    lastPracticedAt = if (qObj.has("lastPracticedAt")) qObj.getLong("lastPracticedAt") else null
                )
            )
        }

        return ExamSprint(
            id = obj.getString("id"),
            title = obj.optString("title", "Exam Sprint"),
            subjectCode = obj.optString("subjectCode", ""),
            subjectName = obj.optString("subjectName", ""),
            examDateMillis = obj.optLong("examDateMillis", System.currentTimeMillis() + 86400000L * 2),
            targetTestQuestionsCount = obj.optInt("targetTestQuestionsCount", 5),
            targetTestDurationMinutes = obj.optInt("targetTestDurationMinutes", 60),
            questions = questions,
            mockTestsTaken = obj.optInt("mockTestsTaken", 0),
            bestScore = obj.optInt("bestScore", 0),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

