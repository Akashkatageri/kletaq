package com.kletaq.app.core.syllabus

import android.util.Log
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
                Given unstructured syllabus text from an engineering course, parse it into structured JSON.
                
                CRITICAL RULES:
                1. STRICT ZERO-HALLUCINATION: Extract ONLY the units and topic titles explicitly present in the input text. NEVER invent, synthesize, or hallucinate filler topics or fake modules that the user did not provide.
                2. OBEY USER CONSTRAINTS: If the user provided instructions (e.g. "do not generate random topics", "only include these topics", "only 1 module"), strictly obey those instructions above all else.
                3. RETAIN NATURAL STRUCTURE: If the input text only contains 1 unit, output ONLY 1 unit. Do NOT fabricate 5 units unless 5 units exist in the text or the user requested 5 modules.
                4. EXTRACT COURSE CODE & NAME: Extract Course Code (e.g. 21MAT11, 1BPHYS102, BCS301) and Course Name.
                5. DESCRIPTIVE TOPIC TITLES: Clean topic titles by stripping leading numbering prefixes (e.g. "01.", "1.1", "Topic 1:") unless the number is part of the concept. Do NOT output bare numbers (e.g. "01", "02") as topic titles if descriptive names are provided or can be identified.
                6. For each topic, assign an estimated study time between 15 and 35 minutes (default 20).
                7. Output MUST be ONLY valid JSON matching this exact structure:
                {
                  "code": "21MAT11",
                  "name": "Engineering Mathematics",
                  "units": [
                    {
                      "unitNumber": 1,
                      "title": "Calculus and Linear Algebra",
                      "topics": [
                        { "title": "Partial Derivatives", "estimatedMinutes": 25 }
                      ]
                    }
                  ]
                }
                Do NOT output markdown backticks, explanations, or any text other than the JSON object.
            """.trimIndent()

            val aiResult = com.kletaq.app.core.ai.UnifiedAiClient.generateContent(
                systemInstruction = systemPrompt,
                userPrompt = "Parse this engineering course syllabus:\n\n$cleanText",
                temperature = 0.1f,
                maxTokens = 4096
            )
            val jsonText = aiResult.getOrNull()?.trim() ?: ""

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
            Log.w(TAG, "Parsing failed, using offline deterministic parser: ${e.message}")
            val fallback = fallbackParseSyllabus(cleanText, targetSemester)
            return@withContext Result.success(fallback)
        }
    }

    internal fun parseJsonToImportedSubject(jsonStr: String, targetSemester: Int): ImportedSubject? {
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
                    var tTitle = tObj.optString("title", "").trim()
                    if (tTitle.isBlank()) {
                        tTitle = "Topic ${t + 1}"
                    } else {
                        // Strip leading numbering like "01. ", "01 - ", "Topic 1: ", "1) "
                        val cleaned = tTitle.replace(Regex("^(?:Topic\\s*)?\\d+\\s*[-.:–)]\\s*", RegexOption.IGNORE_CASE), "").trim()
                        if (cleaned.isNotBlank()) {
                            tTitle = cleaned
                        }
                    }
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
    internal fun fallbackParseSyllabus(text: String, targetSemester: Int): ImportedSubject {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        val codeRegex = Regex("[0-9]{0,2}[A-Z]{2,10}[0-9]{1,4}[A-Z]?", RegexOption.IGNORE_CASE)
        val extractedCode = codeRegex.find(text)?.value?.uppercase() ?: "COURSE101"

        val firstLine = lines.firstOrNull() ?: "Custom Engineering Subject"
        val subjectName = if (firstLine.length < 50 && !firstLine.startsWith("Module", ignoreCase = true)) {
            firstLine.replace(codeRegex, "").replace("-", "").trim()
        } else "Custom Engineering Subject"

        val moduleRegex = Regex("^(?:Module|Unit|Chapter)\\s*[-:]?\\s*([0-9IVXLCDM]+)\\s*[:–-]?\\s*(.*)$", RegexOption.IGNORE_CASE)
        val hasExplicitModules = lines.any { moduleRegex.containsMatchIn(it) }
        val units = mutableListOf<ImportedUnit>()
        var currentUnitNum = 1
        var currentUnitTitle = "Unit 1: Fundamentals"
        var currentTopics = mutableListOf<ImportedTopic>()
        var insideModule = !hasExplicitModules

        for (line in lines) {
            if (line.equals(firstLine, ignoreCase = true) ||
                line.equals(extractedCode, ignoreCase = true) ||
                line.equals(subjectName, ignoreCase = true) ||
                (subjectName.isNotBlank() && line.startsWith(subjectName, ignoreCase = true))
            ) {
                continue
            }
            val match = moduleRegex.find(line)
            if (match != null) {
                if (currentTopics.isNotEmpty()) {
                    units.add(ImportedUnit(currentUnitNum, currentUnitTitle, currentTopics))
                    currentTopics = mutableListOf()
                }
                insideModule = true
                currentUnitNum = match.groupValues[1].toIntOrNull() ?: (units.size + 1)
                currentUnitTitle = match.groupValues[2].ifBlank { "Unit $currentUnitNum" }
            } else if (insideModule && line.length in 3..90 && !line.startsWith("Page", ignoreCase = true) && !line.startsWith("Hours", ignoreCase = true)) {
                // Topic candidate
                val cleanTopic = line.replace(Regex("^(?:Topic\\s*)?\\d+\\s*[-.:–)]\\s*", RegexOption.IGNORE_CASE), "").replace(Regex("^[-•*]\\s*"), "").trim()
                if (cleanTopic.length > 2) {
                    currentTopics.add(ImportedTopic(title = cleanTopic, estimatedMinutes = 25))
                }
            }
        }

        if (currentTopics.isNotEmpty()) {
            units.add(ImportedUnit(currentUnitNum, currentUnitTitle, currentTopics))
        }

        if (units.isEmpty()) {
            val validTopics = lines.filter { line ->
                line.length in 3..90 && !line.startsWith("Page", ignoreCase = true) && !line.startsWith("Hours", ignoreCase = true)
            }.map { line ->
                val clean = line.replace(Regex("^(?:Topic\\s*)?\\d+[.:\\-–)]\\s*"), "").replace(Regex("^[-•*]\\s*"), "").trim()
                ImportedTopic(title = clean.ifBlank { line.take(45) }, estimatedMinutes = 20)
            }
            if (validTopics.isNotEmpty()) {
                units.add(ImportedUnit(1, "Unit 1: Fundamentals", validTopics))
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
