package com.kletaq.app.features.flashcards.ai

import android.util.Log
import com.kletaq.app.core.ai.UnifiedAiClient
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object FlashcardAiHelper {

    private const val TAG = "FlashcardAiHelper"

    suspend fun generateCardsForTopic(
        topicId: String,
        topicTitle: String,
        subjectId: String,
        subjectName: String,
        learnPoints: List<String> = emptyList(),
        examPrepSummary: String? = null
    ): List<Flashcard> = withContext(Dispatchers.IO) {
        val prompt = buildString {
            append("You are an expert academic tutor creating Anki flashcards for university students.\n")
            append("Course Subject: $subjectName\n")
            append("Topic: $topicTitle\n")
            if (learnPoints.isNotEmpty()) {
                append("Key Concepts:\n")
                learnPoints.forEach { append("- $it\n") }
            }
            if (!examPrepSummary.isNullOrBlank()) {
                append("Exam Summary:\n$examPrepSummary\n")
            }
            append("\nGenerate 4-6 high-yield flashcards testing core concepts. Include both BASIC and CLOZE deletion types.\n")
            append("For CLOZE cards, use Anki format: {{c1::hidden_keyword}} or {{c1::keyword::hint}}.\n")
            append("Respond ONLY with a valid JSON array of objects with keys: 'cardType' ('BASIC' or 'CLOZE'), 'front' (question for basic), 'back' (answer for basic), 'clozeText' (for cloze), 'hint'. No markdown formatting or extra text.\n")
        }

        try {
            val responseText = UnifiedAiClient.generateContent(
                systemInstruction = "You generate high-yield university exam flashcards in JSON format only.",
                userPrompt = prompt,
                temperature = 0.3f,
                maxTokens = 1500
            )

            val cleaned = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val jsonArray = JSONArray(cleaned)
            val generatedList = mutableListOf<Flashcard>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("cardType", "BASIC").uppercase()
                val type = if (typeStr == "CLOZE") FlashcardType.CLOZE else FlashcardType.BASIC

                generatedList.add(
                    Flashcard(
                        deckId = topicId,
                        deckName = topicTitle,
                        topicId = topicId,
                        topicTitle = topicTitle,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        cardType = type,
                        front = obj.optString("front"),
                        back = obj.optString("back"),
                        clozeText = obj.optString("clozeText"),
                        hint = obj.optString("hint"),
                        tags = listOf(subjectName.take(15).trim(), topicTitle.take(15).trim())
                    )
                )
            }

            if (generatedList.isNotEmpty()) {
                return@withContext generatedList
            }
        } catch (e: Exception) {
            Log.w(TAG, "AI generation failed, synthesizing from syllabus concepts: ${e.message}")
        }

        // Offline deterministic fallback derived strictly from real topic points
        val fallbackList = mutableListOf<Flashcard>()
        learnPoints.forEachIndexed { idx, point ->
            val words = point.split(" ").filter { it.isNotBlank() }
            if (words.size >= 4) {
                // Cloze card on key terms
                val targetWord = words.getOrNull(words.size / 2) ?: words.last()
                val clozeFormatted = point.replaceFirst(targetWord, "{{c1::$targetWord}}")
                fallbackList.add(
                    Flashcard(
                        deckId = topicId,
                        deckName = topicTitle,
                        topicId = topicId,
                        topicTitle = topicTitle,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        cardType = FlashcardType.CLOZE,
                        clozeText = clozeFormatted,
                        tags = listOf(topicTitle)
                    )
                )
            } else {
                fallbackList.add(
                    Flashcard(
                        deckId = topicId,
                        deckName = topicTitle,
                        topicId = topicId,
                        topicTitle = topicTitle,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        cardType = FlashcardType.BASIC,
                        front = "Explain: $topicTitle (${idx + 1})",
                        back = point,
                        tags = listOf(topicTitle)
                    )
                )
            }
        }

        if (!examPrepSummary.isNullOrBlank()) {
            fallbackList.add(
                Flashcard(
                    deckId = topicId,
                    deckName = topicTitle,
                    topicId = topicId,
                    topicTitle = topicTitle,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    cardType = FlashcardType.BASIC,
                    front = "Core Exam Summary: $topicTitle",
                    back = examPrepSummary,
                    tags = listOf("ExamPrep", topicTitle)
                )
            )
        }

        return@withContext fallbackList
    }
}
