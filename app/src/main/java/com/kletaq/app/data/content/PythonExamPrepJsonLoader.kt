package com.kletaq.app.data.content

import com.kletaq.app.core.di.KletaqApplication
import com.kletaq.app.features.journey.components.ExamPrep
import org.json.JSONObject

/** Loads topic content from assets; the academic repository only assembles syllabus nodes. */
object PythonExamPrepJsonLoader {
    private const val AssetPath = "academic/subjects/python/1BPLC105B.json"

    private val lessons: Map<String, ExamPrep> by lazy {
        val root = JSONObject(
            KletaqApplication.appContext.assets.open(AssetPath)
                .bufferedReader()
                .use { it.readText() }
        )
        val lessonJson = root.getJSONObject("lessons")
        buildMap {
            val keys = lessonJson.keys()
            while (keys.hasNext()) {
                val topicId = keys.next()
                val lesson = lessonJson.getJSONObject(topicId)
                val questions = lesson.getJSONArray("practiceQuestions")
                put(
                    topicId,
                    ExamPrep(
                        learnSummary = lesson.getString("concept"),
                        fiveMarkAnswer = lesson.getString("fiveMarkAnswer"),
                        practiceQuestions = List(questions.length()) { questions.getString(it) },
                        recallPrompt = lesson.getString("quickRecall")
                    )
                )
            }
        }
    }

    fun forTopic(topicId: String): ExamPrep? = lessons[topicId]
}
