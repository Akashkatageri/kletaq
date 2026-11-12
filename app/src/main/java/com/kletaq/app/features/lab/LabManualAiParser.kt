package com.kletaq.app.features.lab

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LabManualAiParser {

    private const val TAG = "LabManualAiParser"

    data class ExtractedPdfProgram(
        val programNumber: Int,
        val title: String?,
        val fullCode: String,
        val sampleOutput: String,
        val kernels: List<LabKernel>,
        val drills: LabProgramDrills
    )

    private val SYSTEM_PROMPT = """
        You are an expert engineering laboratory practical curriculum deconstructor.
        Given an engineering lab manual PDF, document, or source code from a university engineering lab (C, C++, Java, or Python),
        deconstruct each program into an active recall study structure.

        RULES:
        1. Identify all distinct programs (Program 1, Program 2, etc.) in the manual.
        2. For each program, extract:
           - programNumber (Integer: 1, 2, ...)
           - title (e.g. "Program 1: Book Records Management")
           - shortTitle (max 26 characters)
           - language ("C", "C++", "Java", or "Python")
           - fullCode (Complete genuine compilable source code from the manual)
           - blueprintBlock (Structs, typedefs, includes, global variables)
           - coreFunctionBlock (The 20-30 line critical algorithmic core function)
           - guardBlock (Edge case checks, overflow/underflow, null checks)
           - driverBlock (main() and menu loop)
           - algorithmSteps (Array of 4 to 6 ordered logic steps)
           - vivaQuestions (Array of 3 to 5 objects with "question" and "answer")
           - crashTraps (Array of 3 to 4 common student mistakes)
           - inputOutputSample (Sample output or test cases from manual)

        OUTPUT FORMAT:
        Output ONLY a valid JSON object matching this structure:
        {
          "programs": [
            {
              "programNumber": 1,
              "title": "Program 1: Book Records Management",
              "shortTitle": "Prog 1: Book Records",
              "language": "C",
              "fullCode": "#include <stdio.h>...",
              "blueprintBlock": "typedef struct { ... } Book;",
              "coreFunctionBlock": "void issueBook(...) { ... }",
              "guardBlock": "if (books[i].id == id) ...",
              "driverBlock": "int main() { ... }",
              "algorithmSteps": ["Step 1", "Step 2", "Step 3", "Step 4"],
              "vivaQuestions": [{"question": "Q1", "answer": "A1"}],
              "crashTraps": ["Trap 1", "Trap 2"],
              "inputOutputSample": "Book 1\nID: 23\nTitle: ty\nAuthor: ui\nPrice: 900"
            }
          ]
        }
        Do NOT output markdown backticks or explanations.
    """.trimIndent()

    /**
     * Reads the uploaded PDF and extracts every "PROGRAM <N>" section with its exact C code,
     * Sample Output, 5 Kernels, and 4 Interactive Drills.
     * Uses instant deterministic on-device stream parsing first, falling back to ML Kit OCR and Gemini Multimodal.
     */
    suspend fun parseUploadedPdf(
        context: Context,
        pdfUri: Uri
    ): List<ExtractedPdfProgram> = withContext(Dispatchers.IO) {
        val pdfBytes = try {
            context.contentResolver.openInputStream(pdfUri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }

        // 1. Tier 1: Instant deterministic extraction from PDF text streams (100% offline, zero dependencies)
        if (pdfBytes != null && pdfBytes.isNotEmpty()) {
            val streamText = PdfStreamTextExtractor.extractText(pdfBytes)
            if (streamText.isNotBlank()) {
                val directPrograms = parseRawPdfText(streamText)
                if (directPrograms.isNotEmpty()) {
                    Log.i(TAG, "Extracted ${directPrograms.size} programs instantly via direct PDF stream extraction!")
                    return@withContext directPrograms
                }
            }
        }

        // 2. Tier 2: On-Device Native PDF Rendering + ML Kit OCR (for scanned image PDFs)
        val rawTextBuilder = StringBuilder()
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd != null) {
                pfd.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val totalPages = renderer.pageCount.coerceAtMost(30)
                        for (pageIdx in 0 until totalPages) {
                            renderer.openPage(pageIdx).use { page ->
                                val bitmap = Bitmap.createBitmap(
                                    page.width,
                                    page.height,
                                    Bitmap.Config.ARGB_8888
                                )
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val image = InputImage.fromBitmap(bitmap, 0)
                                try {
                                    val visionText = suspendCancellableCoroutine<Text> { cont ->
                                        recognizer.process(image)
                                            .addOnSuccessListener { cont.resume(it) }
                                            .addOnFailureListener { cont.resumeWithException(it) }
                                            .addOnCanceledListener { cont.cancel() }
                                    }
                                    if (!visionText.text.isNullOrBlank()) {
                                        rawTextBuilder.append(visionText.text).append("\n\n")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Page $pageIdx OCR error: ${e.message}")
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "On-device PDF extraction failed: ${e.message}")
        }

        val rawText = rawTextBuilder.toString()
        val extractedFromText = if (rawText.isNotBlank()) parseRawPdfText(rawText) else emptyList()
        if (extractedFromText.isNotEmpty()) {
            Log.i(TAG, "Extracted ${extractedFromText.size} programs directly via on-device OCR!")
            return@withContext extractedFromText
        }

        // 3. Tier 3: Multimodal Gemini
        if (pdfBytes != null && pdfBytes.isNotEmpty()) {
            val geminiResult = parseLabManualPdf(pdfBytes).getOrNull()
            if (!geminiResult.isNullOrEmpty()) {
                Log.i(TAG, "Extracted ${geminiResult.size} programs via Gemini Multimodal!")
                return@withContext geminiResult.map { prog ->
                    ExtractedPdfProgram(
                        programNumber = prog.programNumber,
                        title = prog.title,
                        fullCode = prog.fullCode,
                        sampleOutput = prog.inputOutputSample,
                        kernels = prog.kernels,
                        drills = prog.drills ?: LabProgramDrills()
                    )
                }
            }
        }

        emptyList()
    }

    /**
     * Parses raw extracted text from the lab manual PDF into structured programs.
     */
    fun parseRawPdfText(rawText: String): List<ExtractedPdfProgram> {
        val cleaned = rawText
            .replace("\r\n", "\n")
            .replace("$", "") // Strip math-mode artifacts if any

        // Split on "PROGRAM 1", "PROGRAM 2", "Experiment 1", "LAB PROGRAM 1", etc.
        val programHeaderRegex = Regex("""(?im)^\s*(?:PROGRAM|EXPERIMENT|LAB\s+PROGRAM)\s*[-:#]?\s*(\d+)\b(.*)$""")
        val matches = programHeaderRegex.findAll(cleaned).toList()

        if (matches.isEmpty()) return emptyList()

        val extractedList = mutableListOf<ExtractedPdfProgram>()

        for (idx in matches.indices) {
            val match = matches[idx]
            val progNum = match.groupValues[1].toIntOrNull() ?: (idx + 1)
            val inlineTitle = match.groupValues[2].trim().takeIf { it.isNotBlank() }

            val startOffset = match.range.last + 1
            val endOffset = if (idx + 1 < matches.size) matches[idx + 1].range.first else cleaned.length
            val sectionText = cleaned.substring(startOffset, endOffset).trim()

            // Split section into C Source Code vs Sample Output / Test Cases
            val sampleSplitRegex = Regex("""(?im)^\s*(?:Sample\s+o[pu]*tput|SAMPLE\s+OUTPUT|Output\s*:)\b""")
            val sampleMatch = sampleSplitRegex.find(sectionText)

            val codePart = if (sampleMatch != null) {
                sectionText.substring(0, sampleMatch.range.first).trim()
            } else {
                sectionText
            }

            val outputPart = if (sampleMatch != null) {
                val afterSample = sectionText.substring(sampleMatch.range.last + 1)
                // Stop before "Demonstration of Correctness" or next narrative section
                val demoSplit = Regex("""(?im)^\s*Demonstration\s+of\s+Correctness""").find(afterSample)
                if (demoSplit != null) afterSample.substring(0, demoSplit.range.first).trim()
                else afterSample.trim()
            } else {
                ""
            }

            // Extract from first #include (or typedef/struct/import/def) to end of main()
            val codeStartIdx = listOf(
                codePart.indexOf("#include"),
                codePart.indexOf("typedef struct"),
                codePart.indexOf("struct "),
                codePart.indexOf("import ")
            ).filter { it >= 0 }.minOrNull() ?: 0

            val cleanCode = codePart.substring(codeStartIdx)
                .replace(Regex("(?m)^_{3,}\\s*$"), "")
                .trim()

            if (cleanCode.isNotBlank()) {
                val inferredTitle = inlineTitle ?: inferTitleFromCode(progNum, cleanCode)
                val kernels = LabKernelExtractor.extractKernels("pdf_prog_$progNum", cleanCode)
                val drills = LabKernelExtractor.generateDrills(inferredTitle, cleanCode, outputPart)

                extractedList.add(
                    ExtractedPdfProgram(
                        programNumber = progNum,
                        title = inferredTitle,
                        fullCode = cleanCode,
                        sampleOutput = outputPart,
                        kernels = kernels,
                        drills = drills
                    )
                )
            }
        }

        return extractedList
    }

    private fun inferTitleFromCode(progNum: Int, code: String): String {
        return when {
            "typedef struct" in code && ("Book" in code || "book" in code) -> "Book Records (Struct & Dynamic Memory)"
            ("Term" in code || "sparse" in code) && "transpose" in code -> "Sparse Matrix Operations (3-Tuple Addition & Transpose)"
            "stack" in code || "top" in code -> "Array Stack Operations"
            "queue" in code -> "Circular Queue Operations"
            else -> "Lab Program $progNum"
        }
    }

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

        // 1. Direct deterministic stream extraction (instant & 100% offline)
        val streamText = PdfStreamTextExtractor.extractText(pdfBytes)
        if (streamText.isNotBlank()) {
            val extracted = parseRawPdfText(streamText)
            if (extracted.isNotEmpty()) {
                val programs = extracted.map { prog ->
                    LabProgramItem(
                        programNumber = prog.programNumber,
                        title = prog.title ?: "Program ${prog.programNumber}",
                        shortTitle = (prog.title ?: "Program ${prog.programNumber}").take(24),
                        language = "C",
                        fullCode = prog.fullCode,
                        blueprintBlock = prog.kernels.firstOrNull()?.codeSnippet ?: "",
                        coreFunctionBlock = prog.kernels.getOrNull(1)?.codeSnippet ?: "",
                        guardBlock = "",
                        driverBlock = prog.kernels.lastOrNull()?.codeSnippet ?: "",
                        algorithmSteps = listOf(
                            "Set up data structures and input values",
                            "Validate boundaries and error guards",
                            "Execute core program operations",
                            "Display results and verify state"
                        ),
                        vivaQuestions = listOf(
                            "What is the time complexity of this practical?" to "Optimal runtime proportional to input size.",
                            "Why is this data structure chosen?" to "Satisfies problem constraints with minimal overhead."
                        ),
                        crashTraps = listOf(
                            "Missing boundary check on invalid input",
                            "Uninitialized pointer or variable dereference"
                        ),
                        masteryStatus = ProgramMasteryStatus.UNTOUCHED,
                        kernels = prog.kernels,
                        drills = prog.drills,
                        inputOutputSample = prog.sampleOutput
                    )
                }
                Log.i(TAG, "parseLabManualPdf: extracted ${programs.size} programs offline via text streams!")
                return@withContext Result.success(programs)
            }
        }

        val promptContent = Content(
            role = "user",
            parts = listOf(
                InlineDataPart(pdfBytes, "application/pdf"),
                TextPart("Carefully analyze this complete engineering lab manual PDF. Extract every distinct lab program (Program 1, 2, 3...) and deconstruct each program into the active recall JSON structure.")
            )
        )

        var lastException: Exception? = null
        for (modelName in com.kletaq.app.core.ai.UnifiedAiClient.BUILT_IN_FALLBACK_MODELS) {
            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        maxOutputTokens = 8192
                        temperature = 0.1f
                    },
                    systemInstruction = content { text(SYSTEM_PROMPT) }
                )

                val response = model.generateContent(promptContent)
                val jsonText = response.text?.trim() ?: ""
                if (jsonText.isBlank()) {
                    throw Exception("Empty response from built-in AI ($modelName)")
                }

                val cleanJson = jsonText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val parsedList = parseJsonToLabPrograms(cleanJson)
                return@withContext if (parsedList.isNotEmpty()) {
                    Result.success(parsedList)
                } else {
                    Result.failure(Exception("Could not extract programs from this PDF. Please ensure the PDF contains program code or syllabus listings."))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lab manual PDF parse failed with $modelName: ${e.message}")
                lastException = e
            }
        }

        Log.e(TAG, "Failed to parse lab manual PDF: ${lastException?.message}", lastException)
        Result.failure(lastException ?: Exception("Firebase AI service unavailable. Please configure a custom API key in Settings -> AI Keys."))
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
            val prompt = "Deconstruct this engineering lab manual into active recall programs:\n\n$cleanText"
            val aiResult = com.kletaq.app.core.ai.UnifiedAiClient.generateContent(
                systemInstruction = SYSTEM_PROMPT,
                userPrompt = prompt,
                temperature = 0.1f,
                maxTokens = 8192
            )
            val jsonText = aiResult.getOrThrow().trim()

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
                val sampleOutput = obj.optString("inputOutputSample", "")

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

                val (extractedKernels, extractedDrills) = LabKernelExtractor.extractKernelsAndDrills(
                    fullCode = fullCode,
                    title = title,
                    language = language,
                    inputOutputSample = sampleOutput
                )

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
                        algorithmSteps = if (steps.isNotEmpty()) steps else listOf(
                            "Set up data structures and input values for $shortTitle",
                            "Validate input boundaries and error guards",
                            "Execute core $shortTitle operations",
                            "Output execution results and verify program state"
                        ),
                        vivaQuestions = if (viva.isNotEmpty()) viva else listOf(
                            "What is the time complexity of $shortTitle?" to "Proportional to input size with optimal asymptotic bounds.",
                            "What is the main advantage of using this approach in $shortTitle?" to "Ensures deterministic execution and prevents boundary memory corruption."
                        ),
                        crashTraps = if (traps.isNotEmpty()) traps else listOf(
                            "Missing boundary guard check for invalid inputs in $shortTitle",
                            "Uninitialized variable or pointer dereference"
                        ),
                        masteryStatus = ProgramMasteryStatus.UNTOUCHED,
                        kernels = extractedKernels,
                        drills = extractedDrills,
                        inputOutputSample = sampleOutput
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON to LabProgramItem list: ${e.message}", e)
        }
        return list
    }
}
