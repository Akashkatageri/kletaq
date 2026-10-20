package com.kletaq.app.core.syllabus

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * ⚡ AI Syllabus Importer
 *
 * Uses Gemini Flash Lite to instantly transform unstructured syllabus documents
 * (pasted text, scheme outlines, course descriptions) into a validated
 * 5-Module engineering journey tree.
 */
object SyllabusAiImporter {

    private const val TAG = "SyllabusAiImporter"

    suspend fun importSyllabus(
        rawText: String,
        targetSemester: Int = 1
    ): Result<ImportedSubject> = withContext(Dispatchers.IO) {
        val cleanText = rawText.trim()
        if (cleanText.length < 30) {
            return@withContext Result.failure(IllegalArgumentException("Syllabus text too short. Please paste course topics or modules."))
        }

        try {
            val systemPrompt = """
                You are a university engineering curriculum parser (specialized in VTU, autonomous, and STEM schemes).
                Given unstructured syllabus text from an engineering course, parse it into a structured 5-module JSON tree.
                
                RULES:
                1. Extract Course Code (e.g. 1BPHYS102, 21MAT31, BCS301) and Course Name.
                2. Divide the course into 5 distinct modules/units (or retain the units present in text).
                3. For each unit, extract 3 to 7 atomic, high-yield topic lessons (e.g. "Schrödinger 1D Box", "Hall Effect", "Josephson Junction").
                4. For each topic, assign an estimated study time between 15 and 35 minutes.
                5. Output MUST be ONLY valid JSON matching this exact structure:
                {
                  "code": "1BPHYS102",
                  "name": "Quantum Physics and Applications",
                  "units": [
                    {
                      "unitNumber": 1,
                      "title": "Quantum Mechanics Principles",
                      "topics": [
                        { "title": "Schrödinger Equation in 1D Box", "estimatedMinutes": 35 },
                        { "title": "Heisenberg Uncertainty Principle", "estimatedMinutes": 25 }
                      ]
                    }
                  ]
                }
                Do NOT output markdown backticks, explanations, or any text other than the JSON object.
            """.trimIndent()

            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-lite",
                generationConfig = generationConfig {
                    maxOutputTokens = 2048
                    temperature = 0.1f
                },
                systemInstruction = content { text(systemPrompt) }
            )

            val prompt = "Parse this engineering course syllabus:\n\n$cleanText"
            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim() ?: ""

            // Strip possible ```json ``` markdown code blocks
            val cleanJson = jsonText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = parseJsonToImportedSubject(cleanJson, targetSemester)
            if (parsed != null) {
                return@withContext Result.success(parsed)
            } else {
                // If model JSON parsing had minor syntax discrepancy, use deterministic fallback parser
                val fallback = fallbackParseSyllabus(cleanText, targetSemester)
                return@withContext Result.success(fallback)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini parsing failed, using offline deterministic parser: ${e.message}")
            val fallback = fallbackParseSyllabus(cleanText, targetSemester)
            return@withContext Result.success(fallback)
        }
    }

    private fun parseJsonToImportedSubject(jsonStr: String, targetSemester: Int): ImportedSubject? {
        return try {
            val obj = JSONObject(jsonStr)
            val code = obj.optString("code", "").ifBlank { "COURSE101" }
            val name = obj.optString("name", "").ifBlank { "Imported Course" }

            val unitsList = mutableListOf<ImportedUnit>()
            val unitsArr = obj.optJSONArray("units") ?: return null

            for (u in 0 until unitsArr.length()) {
                val uObj = unitsArr.getJSONObject(u)
                val uNum = uObj.optInt("unitNumber", u + 1)
                val uTitle = uObj.optString("title", "Unit $uNum")

                val topicsList = mutableListOf<ImportedTopic>()
                val topicsArr = uObj.optJSONArray("topics") ?: continue

                for (t in 0 until topicsArr.length()) {
                    val tObj = topicsArr.getJSONObject(t)
                    val tTitle = tObj.optString("title", "Topic ${t + 1}")
                    val mins = tObj.optInt("estimatedMinutes", 20)
                    topicsList.add(ImportedTopic(title = tTitle, estimatedMinutes = mins))
                }

                if (topicsList.isNotEmpty()) {
                    unitsList.add(ImportedUnit(uNum, uTitle, topicsList))
                }
            }

            if (unitsList.isNotEmpty()) {
                ImportedSubject(
                    code = code,
                    name = name,
                    semesterNumber = targetSemester,
                    units = unitsList
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}")
            null
        }
    }

    /**
     * Deterministic regex fallback parser when offline or in no-network environments.
     */
    private fun fallbackParseSyllabus(text: String, targetSemester: Int): ImportedSubject {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        val codeRegex = Regex("[0-9]?[A-Z]{2,10}[0-9]{3}[A-Z]?", RegexOption.IGNORE_CASE)
        val extractedCode = codeRegex.find(text)?.value?.uppercase() ?: "COURSE101"

        val firstLine = lines.firstOrNull() ?: "Custom Engineering Subject"
        val subjectName = if (firstLine.length < 50 && !firstLine.startsWith("Module", ignoreCase = true)) {
            firstLine.replace(codeRegex, "").replace("-", "").trim()
        } else "Custom Engineering Subject"

        val moduleRegex = Regex("^(?:Module|Unit|Chapter)\\s*[-:]?\\s*([0-9IVXLCDM]+)\\s*[:–-]?\\s*(.*)$", RegexOption.IGNORE_CASE)
        val units = mutableListOf<ImportedUnit>()
        var currentUnitNum = 1
        var currentUnitTitle = "Unit 1: Fundamentals"
        var currentTopics = mutableListOf<ImportedTopic>()

        for (line in lines) {
            val match = moduleRegex.find(line)
            if (match != null) {
                if (currentTopics.isNotEmpty()) {
                    units.add(ImportedUnit(currentUnitNum, currentUnitTitle, currentTopics))
                    currentTopics = mutableListOf()
                }
                currentUnitNum++
                currentUnitTitle = match.groupValues[2].ifBlank { "Unit $currentUnitNum" }
            } else if (line.length in 5..90 && !line.startsWith("Page", ignoreCase = true) && !line.startsWith("Hours", ignoreCase = true)) {
                // Topic candidate
                val cleanTopic = line.replace(Regex("^[0-9]+[.)-]\\s*"), "").replace(Regex("^[-•*]\\s*"), "").trim()
                if (cleanTopic.length > 3) {
                    currentTopics.add(ImportedTopic(title = cleanTopic, estimatedMinutes = 25))
                }
            }
        }

        if (currentTopics.isNotEmpty()) {
            units.add(ImportedUnit(currentUnitNum, currentUnitTitle, currentTopics))
        }

        if (units.isEmpty()) {
            // Generate 5 baseline units from topic list
            val chunked = lines.chunked(maxOf(1, lines.size / 5))
            chunked.forEachIndexed { idx, chunk ->
                val topics = chunk.take(6).map {
                    ImportedTopic(title = it.take(45), estimatedMinutes = 20)
                }
                units.add(ImportedUnit(idx + 1, "Unit ${idx + 1}", topics))
            }
        }

        return ImportedSubject(
            code = extractedCode,
            name = subjectName.ifBlank { "Imported Engineering Subject" },
            semesterNumber = targetSemester,
            units = units
        )
    }
}
