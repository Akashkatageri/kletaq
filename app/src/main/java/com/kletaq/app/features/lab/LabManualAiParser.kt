package com.kletaq.app.features.lab

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object LabManualAiParser {

    private const val TAG = "LabManualAiParser"

    private val SYSTEM_PROMPT = """
        You are an expert engineering laboratory practical curriculum deconstructor.
        Given an engineering lab manual PDF, document, or source code from a university engineering lab (C, C++, Java, or Python),
        deconstruct each program into an active recall study structure.

        CRITICAL REQUIREMENT:
        You must dynamically extract and generate ALL four drills for every single program found in the manual:
        1. algorithmSteps: 4 to 6 ordered logic steps representing the program flow (used for interactive ordering drills).
        2. coreFunctionBlock: The critical 20-30 line algorithmic core function (the heart of the logic, NOT the menu/switch-case).
        3. crashTraps: 3 to 4 specific common student mistakes, segmentation fault traps, or logic gotchas for this exact program.
        4. vivaQuestions: 3 to 5 examiner Viva Voce questions & high-scoring answers specifically testing understanding of this program's algorithms, time complexity, and edge cases.

        RULES:
        1. Identify all distinct programs (Program 1, Program 2, etc.) in the manual.
        2. For each program, extract:
           - programNumber (Integer: 1, 2, ...)
           - title (e.g. "Program 1: Array Stack Operations")
           - shortTitle (max 26 characters, e.g. "Prog 1: Array Stack")
           - language ("C", "C++", "Java", or "Python")
           - fullCode (Complete compilable source code from the manual)
           - blueprintBlock (Structs, typedefs, includes, global variables)
           - coreFunctionBlock (The 20-30 line critical algorithmic core function)
           - guardBlock (Edge case checks, overflow/underflow, null checks)
           - driverBlock (main() and menu loop)
           - algorithmSteps (Array of 4 to 6 ordered logic steps for mental ordering drills)
           - vivaQuestions (Array of 3 to 5 objects with "question" and "answer" for external viva defense)
           - crashTraps (Array of 3 to 4 common student mistakes that cause Segmentation Faults or logic errors)

        OUTPUT FORMAT:
        Output ONLY a valid JSON object matching this exact structure:
        {
          "programs": [
            {
              "programNumber": 1,
              "title": "Program 1: Array Stack Operations",
              "shortTitle": "Prog 1: Array Stack",
              "language": "C",
              "fullCode": "#include <stdio.h>...",
              "blueprintBlock": "#define MAX 5\nint stack[MAX];",
              "coreFunctionBlock": "void push(int item) { ... }",
              "guardBlock": "if (top == MAX - 1) ...",
              "driverBlock": "int main() { ... }",
              "algorithmSteps": [
                "Check for overflow condition (top == MAX - 1)",
                "Increment top pointer",
                "Insert element at top index",
                "Display confirmation"
              ],
              "vivaQuestions": [
                { "question": "What is the time complexity of push?", "answer": "O(1) constant time." },
                { "question": "What causes stack overflow?", "answer": "Attempting to push an item when top == MAX - 1." }
              ],
              "crashTraps": [
                "Off-by-one error: checking top == MAX instead of MAX - 1",
                "Missing underflow guard on pop before decrementing top"
              ]
            }
          ]
        }
        Do NOT output markdown backticks, explanations, or any text other than the JSON object.
    """.trimIndent()

    /**
     * Parses an uploaded college lab manual PDF into fully deconstructed
     * [LabProgramItem] instances using Gemini Flash Lite with multimodal inline data.
     */
    suspend fun parseLabManualPdf(
        pdfBytes: ByteArray
    ): Result<List<LabProgramItem>> = withContext(Dispatchers.IO) {
        if (pdfBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Selected PDF file is empty."))
        }
        if (pdfBytes.size > 18 * 1024 * 1024) {
            return@withContext Result.failure(IllegalArgumentException("PDF exceeds 18MB. Please choose a smaller manual file or paste the code directly."))
        }

        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-lite",
                generationConfig = generationConfig {
                    maxOutputTokens = 8192
                    temperature = 0.1f
                },
                systemInstruction = content { text(SYSTEM_PROMPT) }
            )

            val promptContent = Content(
                role = "user",
                parts = listOf(
                    InlineDataPart(pdfBytes, "application/pdf"),
                    TextPart("Carefully analyze this complete engineering lab manual PDF. Extract every distinct lab program (Program 1, 2, 3...) and deconstruct each program into the active recall JSON structure.")
                )
            )

            val response = model.generateContent(promptContent)
            val jsonText = response.text?.trim() ?: ""

            val cleanJson = jsonText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedList = parseJsonToLabPrograms(cleanJson)
            if (parsedList.isNotEmpty()) {
                Result.success(parsedList)
            } else {
                Result.failure(Exception("Could not extract programs from this PDF. Please ensure the PDF contains program code or syllabus listings."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse lab manual PDF: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parses unstructured college lab manual text or code into fully deconstructed
     * [LabProgramItem] instances using Gemini.
     */
    suspend fun parseLabManualText(
        rawText: String
    ): Result<List<LabProgramItem>> = withContext(Dispatchers.IO) {
        val cleanText = rawText.trim()
        if (cleanText.length < 50) {
            return@withContext Result.failure(IllegalArgumentException("Lab text is too short. Please paste full program code or manual text."))
        }

        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-lite",
                generationConfig = generationConfig {
                    maxOutputTokens = 8192
                    temperature = 0.1f
                },
                systemInstruction = content { text(SYSTEM_PROMPT) }
            )

            val prompt = "Deconstruct this engineering lab manual into active recall programs:\n\n$cleanText"
            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim() ?: ""

            val cleanJson = jsonText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedList = parseJsonToLabPrograms(cleanJson)
            if (parsedList.isNotEmpty()) {
                Result.success(parsedList)
            } else {
                Result.failure(Exception("Could not extract valid programs from the provided text."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse lab manual text: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseJsonToLabPrograms(jsonString: String): List<LabProgramItem> {
        val list = mutableListOf<LabProgramItem>()
        try {
            val firstBrace = jsonString.indexOf('{')
            val lastBrace = jsonString.lastIndexOf('}')
            val cleanStr = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                jsonString.substring(firstBrace, lastBrace + 1)
            } else {
                jsonString
            }

            val root = JSONObject(cleanStr)
            val arr = root.optJSONArray("programs") ?: JSONArray()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pNum = obj.optInt("programNumber", i + 1)
                val title = obj.optString("title", "Program $pNum")
                val shortTitle = obj.optString("shortTitle", title.take(24))
                val language = obj.optString("language", "C")
                val fullCode = obj.optString("fullCode", "")
                val blueprint = obj.optString("blueprintBlock", "")
                val coreFunc = obj.optString("coreFunctionBlock", "")
                val guard = obj.optString("guardBlock", "")
                val driver = obj.optString("driverBlock", "")

                val stepsArr = obj.optJSONArray("algorithmSteps")
                val steps = mutableListOf<String>()
                if (stepsArr != null) {
                    for (s in 0 until stepsArr.length()) {
                        steps.add(stepsArr.getString(s))
                    }
                }

                val vivaArr = obj.optJSONArray("vivaQuestions")
                val viva = mutableListOf<Pair<String, String>>()
                if (vivaArr != null) {
                    for (v in 0 until vivaArr.length()) {
                        val vObj = vivaArr.getJSONObject(v)
                        val q = vObj.optString("question", "")
                        val a = vObj.optString("answer", "")
                        if (q.isNotBlank()) viva.add(q to a)
                    }
                }

                val trapsArr = obj.optJSONArray("crashTraps")
                val traps = mutableListOf<String>()
                if (trapsArr != null) {
                    for (t in 0 until trapsArr.length()) {
                        traps.add(trapsArr.getString(t))
                    }
                }

                list.add(
                    LabProgramItem(
                        programNumber = pNum,
                        title = title,
                        shortTitle = shortTitle,
                        language = language,
                        fullCode = fullCode,
                        blueprintBlock = blueprint,
                        coreFunctionBlock = coreFunc,
                        guardBlock = guard,
                        driverBlock = driver,
                        algorithmSteps = if (steps.isNotEmpty()) steps else listOf("Initialize variables and boundary pointers", "Check boundary guard conditions", "Execute algorithmic core logic", "Return status and print results"),
                        vivaQuestions = if (viva.isNotEmpty()) viva else listOf("What is the time complexity?" to "Depends on input size N.", "Why use this data structure?" to "Provides optimal runtime bounds."),
                        crashTraps = if (traps.isNotEmpty()) traps else listOf("Missing boundary check", "Uninitialized pointer access"),
                        masteryStatus = ProgramMasteryStatus.UNTOUCHED
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON to LabProgramItem list: ${e.message}", e)
        }
        return list
    }
}
