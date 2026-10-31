package com.kletaq.app.features.examsprint.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kletaq.app.core.ai.UnifiedAiClient
import com.kletaq.app.data.repository.AcademicContentRepository
import com.kletaq.app.features.examsprint.model.QuestionMastery
import com.kletaq.app.features.examsprint.model.SprintQuestion
import com.kletaq.app.features.examsprint.model.UploadedDocumentRole
import com.kletaq.app.features.examsprint.model.UploadedExamDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 🧠 AI & Syllabus Extractor for Exam Sprints.
 * Zero-Hardcoding compliant: pulls from verified syllabus assets, user-uploaded PDFs/images via ML Kit OCR,
 * and intelligent question/answer pairing via UnifiedAiClient.
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

            GUIDELINES:
            1. If an answer was already provided in the student's text, extract it verbatim into "studentAnswer".
            2. If NO answer was provided in the student's text, synthesize an accurate university-grade model answer into "aiModelAnswer", and leave "studentAnswer" as null.
            3. Provide "quickRecall": 1 short punchy mental model sentence (e.g. "Remember: Mutex = binary semaphore with ownership; Semaphore = integer signaling counter").
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
        val fallback = parseTextDeterministically(rawText)
        if (fallback.isNotEmpty()) return@withContext fallback
        return@withContext fallbackChunkIntoQuestions(rawText)
    }

    /**
     * Extracts text directly from an uploaded document (PDF, Image, or Text file)
     * using on-device Google ML Kit OCR and Android PdfRenderer.
     * Works 100% offline with zero cloud dependency.
     */
    suspend fun extractTextFromUri(
        context: Context,
        uri: Uri,
        mimeType: String,
        docName: String = "",
        onProgress: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val type = mimeType.lowercase()
        val nameLower = docName.lowercase()
        val uriLower = uri.toString().lowercase()

        val isPdf = type == "application/pdf" ||
                uriLower.endsWith(".pdf") ||
                nameLower.endsWith(".pdf") ||
                uriLower.contains(".pdf")

        val isImage = type.startsWith("image/") ||
                nameLower.matches(Regex(".*\\.(jpe?g|png|webp|bmp|gif)$")) ||
                uriLower.matches(Regex(".*\\.(jpe?g|png|webp|bmp|gif)$"))

        Log.d(TAG, "extractTextFromUri: name=$docName, mime=$mimeType, isPdf=$isPdf, isImage=$isImage, uri=$uri")

        if (isPdf) {
            val sb = StringBuilder()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    renderer = PdfRenderer(pfd)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val maxPages = minOf(renderer.pageCount, 15)
                    Log.d(TAG, "PdfRenderer opened $docName (${renderer.pageCount} total pages, scanning up to $maxPages)")

                    for (i in 0 until maxPages) {
                        onProgress?.invoke("Scanning page ${i + 1} of $maxPages in ${docName.ifBlank { "document" }}...")
                        val page = renderer.openPage(i)
                        val targetWidth = 1500
                        val targetHeight = ((page.height.toFloat() / page.width.toFloat()) * targetWidth).toInt().coerceAtLeast(200)
                        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        
                        // CRITICAL: Fill with pure WHITE background!
                        // Android PdfRenderer draws over existing bitmap pixels; if not pre-filled with white,
                        // black text over a transparent background renders invisible to ML Kit OCR!
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        try {
                            val visionText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
                            if (visionText.text.isNotBlank()) {
                                sb.append("\n--- Page ${i + 1} ---\n")
                                sb.append(visionText.text)
                                Log.d(TAG, "Page ${i + 1} OCR extracted ${visionText.text.length} chars")
                            } else {
                                Log.w(TAG, "Page ${i + 1} OCR returned no text")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "OCR failed on page ${i + 1}: ${e.message}")
                        } finally {
                            bitmap.recycle()
                        }
                    }
                } else {
                    Log.e(TAG, "openFileDescriptor returned null for $uri")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to render/extract PDF: ${e.message}", e)
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
            val text = sb.toString().trim()
            if (text.isNotBlank()) return@withContext text
        }

        if (isImage) {
            try {
                onProgress?.invoke("Processing image OCR for ${docName.ifBlank { "photo" }}...")
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val visionText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
                    bitmap.recycle()
                    if (visionText.text.isNotBlank()) {
                        Log.d(TAG, "Image OCR extracted ${visionText.text.length} chars")
                        return@withContext visionText.text.trim()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract image text: ${e.message}", e)
            }
        }

        // Fallback: try reading as plain text stream (for text/markdown files, skipping binary PDF headers)
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            if (text.isNotBlank() && !text.startsWith("%PDF")) {
                return@withContext text.trim()
            }
        } catch (_: Exception) {}

        return@withContext ""
    }

    /**
     * Parses multiple uploaded assignment documents:
     * Supports dedicated Questions PDF, Answers / Solved Scheme PDF, and reference notes.
     */
    suspend fun parseUploadedDocuments(
        context: Context,
        documents: List<UploadedExamDocument>,
        subjectName: String = "",
        subjectCode: String = "",
        onProgress: ((String) -> Unit)? = null
    ): List<SprintQuestion> = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) return@withContext emptyList()

        val questionDocs = documents.filter { it.role == UploadedDocumentRole.QUESTIONS }
        val answerDocs = documents.filter { it.role == UploadedDocumentRole.ANSWERS }
        val referenceDocs = documents.filter { it.role == UploadedDocumentRole.REFERENCE }

        val activeQuestionDocs = if (questionDocs.isNotEmpty()) questionDocs else documents

        // Extract text from question documents
        val questionsTextList = mutableListOf<String>()
        for ((idx, doc) in activeQuestionDocs.withIndex()) {
            onProgress?.invoke("Extracting question document (${idx + 1}/${activeQuestionDocs.size}): ${doc.name}...")
            val txt = extractTextFromUri(context, doc.uri, doc.mimeType, doc.name, onProgress)
            if (txt.isNotBlank()) questionsTextList.add("### Question Document: ${doc.name}\n$txt")
        }
        val questionsText = questionsTextList.joinToString("\n\n")

        // Extract text from answers documents
        val answersTextList = mutableListOf<String>()
        for ((idx, doc) in answerDocs.withIndex()) {
            onProgress?.invoke("Extracting solutions sheet (${idx + 1}/${answerDocs.size}): ${doc.name}...")
            val txt = extractTextFromUri(context, doc.uri, doc.mimeType, doc.name, onProgress)
            if (txt.isNotBlank()) answersTextList.add("### Solution Document: ${doc.name}\n$txt")
        }
        // Also extract from any reference documents
        for ((idx, doc) in referenceDocs.withIndex()) {
            onProgress?.invoke("Extracting reference notes (${idx + 1}/${referenceDocs.size}): ${doc.name}...")
            val txt = extractTextFromUri(context, doc.uri, doc.mimeType, doc.name, onProgress)
            if (txt.isNotBlank()) answersTextList.add("### Reference Notes: ${doc.name}\n$txt")
        }
        val answersText = answersTextList.joinToString("\n\n")

        val hasSeparateAnswers = answersText.isNotBlank()

        val systemPrompt = """
            You are an expert university examiner and assignment evaluator for "$subjectName ($subjectCode)".
            Your task is to parse the student's exam documents and extract every distinct question, along with answers and scoring rubrics.

            ${if (hasSeparateAnswers) """
            The student has uploaded:
            1. A QUESTIONS document
            2. A SOLUTIONS / ANSWER SCHEME document
            Match each question from Document 1 with its corresponding solution from Document 2.
            If a question has no solution in the solutions document, provide an accurate model answer and marking rubric.
            """ else """
            Extract every distinct question and any written answer/solution from the provided text.
            For any question without an answer, provide an accurate model answer and marking rubric.
            """}

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
            Do NOT output markdown backticks or commentary.
        """.trimIndent()

        val userPrompt = if (hasSeparateAnswers) {
            "QUESTIONS DOCUMENT:\n$questionsText\n\nSOLUTIONS DOCUMENT:\n$answersText"
        } else {
            "DOCUMENT CONTENT:\n$questionsText"
        }

        onProgress?.invoke("Synthesizing questions & marking rubrics with AI...")

        try {
            val aiResponse = UnifiedAiClient.generateContent(
                systemInstruction = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.2f
            )
            val jsonText = aiResponse.getOrNull()?.trim() ?: ""
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
            Log.w(TAG, "UnifiedAiClient failed on document text: ${e.message}")
        }

        // Deterministic fallback (guaranteed offline)
        val combinedForRegex = if (questionsText.isNotBlank()) questionsText else answersText
        val deterministic = parseTextDeterministically(combinedForRegex)
        if (deterministic.isNotEmpty()) return@withContext deterministic

        // Fallback for unformatted text (split paragraphs into questions)
        return@withContext fallbackChunkIntoQuestions(combinedForRegex)
    }

    /**
     * Backward-compatible wrapper for single byte array uploads.
     */
    suspend fun parseUploadedAssignment(
        fileBytes: ByteArray,
        mimeType: String,
        subjectName: String = ""
    ): Result<List<SprintQuestion>> = withContext(Dispatchers.IO) {
        if (fileBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("File is empty."))
        }

        val text = try {
            val isImage = mimeType.lowercase().startsWith("image/")
            if (isImage) {
                val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                if (bitmap != null) {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val visionText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
                    bitmap.recycle()
                    visionText.text
                } else String(fileBytes)
            } else {
                String(fileBytes)
            }
        } catch (e: Exception) {
            String(fileBytes)
        }

        val questions = parsePastedQuestionsAndAnswers(text, subjectName)
        if (questions.isNotEmpty()) {
            Result.success(questions)
        } else {
            val fallback = fallbackChunkIntoQuestions(text)
            if (fallback.isNotEmpty()) Result.success(fallback)
            else Result.failure(Exception("Could not extract questions from document."))
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
                val qNum = item.optInt("questionNumber", i + 1)
                val qText = item.optString("questionText", "")
                if (qText.isBlank()) continue

                val marks = item.optInt("marks", 10)
                val studentAns = item.optString("studentAnswer", null).takeIf { !it.isNullOrBlank() && it != "null" }
                val aiModelAns = item.optString("aiModelAnswer", "Comprehensive answer outlined in course syllabus.")
                val quickRecall = item.optString("quickRecall", "")

                val rubricList = mutableListOf<String>()
                val rubricArr = item.optJSONArray("markingRubric")
                if (rubricArr != null) {
                    for (r in 0 until rubricArr.length()) {
                        rubricList.add(rubricArr.getString(r))
                    }
                } else {
                    rubricList.add("Definition & governing law")
                    rubricList.add("Detailed analytical diagram or steps")
                    rubricList.add("Final working and conclusion")
                }

                list.add(
                    SprintQuestion(
                        id = UUID.randomUUID().toString(),
                        questionNumber = qNum,
                        questionText = qText,
                        marks = marks,
                        studentAnswer = studentAns,
                        aiModelAnswer = aiModelAns,
                        isUserAnswerCustomized = studentAns != null,
                        quickRecall = quickRecall,
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
        var currentMarks = 10
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
                        marks = currentMarks,
                        studentAnswer = aStr,
                        aiModelAnswer = aStr ?: "Comprehensive answer outlined in course syllabus and lecture notes.",
                        isUserAnswerCustomized = aStr != null,
                        quickRecall = "Key concept: review core definition and primary diagram.",
                        markingRubric = listOf(
                            "[2M] Statement & Core Principle",
                            "[3M] Diagram or Core Architecture",
                            "[5M] Complete Explanation and Steps"
                        ),
                        masteryStatus = QuestionMastery.UNATTEMPTED
                    )
                )
                currentQText = StringBuilder()
                currentAnsText = StringBuilder()
                currentMarks = 10
                isCapturingAnswer = false
            }
        }

        // Matches: 1., 1), (1), 1.a, 1 a), Q1., Q.1:, Question 1:, Module 1 - Q1
        val questionHeaderRegex = Regex(
            "^(?:Q(?:uestion)?\\.?\\s*\\d+|\\(?\\d+[\\.\\)]\\s*(?:[a-d][\\.\\)]\\s*)?|\\bQ\\d+[:\\.\\)])\\s*(.*)",
            RegexOption.IGNORE_CASE
        )
        val answerHeaderRegex = Regex("^(?:A(?:nswer)?|Ans|Solution)[:\\.)\\s]+(.*)", RegexOption.IGNORE_CASE)
        val marksRegex = Regex("\\[?(\\d+)\\s*(?:Marks?|M)\\]?", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("--- Page")) continue

            val qMatch = questionHeaderRegex.find(trimmed)
            val aMatch = answerHeaderRegex.find(trimmed)

            if (qMatch != null) {
                flushCurrent()
                val rest = qMatch.groupValues[1]
                val mMatch = marksRegex.find(rest)
                if (mMatch != null) {
                    currentMarks = mMatch.groupValues[1].toIntOrNull() ?: 10
                }
                currentQText.append(rest)
                isCapturingAnswer = false
            } else if (aMatch != null) {
                isCapturingAnswer = true
                currentAnsText.append(aMatch.groupValues[1]).append("\n")
            } else {
                if (isCapturingAnswer) {
                    currentAnsText.append(trimmed).append("\n")
                } else {
                    if (currentQText.isEmpty()) {
                        // Check if line starts with question-like verb (Explain, Describe, What, Define, etc.)
                        val isQuestionVerb = trimmed.matches(
                            Regex("^(?:Explain|Describe|What is|What are|Define|Differentiate|Distinguish|Compare|Discuss|Derive|Calculate|State|Illustrate|Write a|Write short|Show that)\\b.*", RegexOption.IGNORE_CASE)
                        )
                        if (isQuestionVerb) {
                            flushCurrent()
                            currentQText.append(trimmed)
                        }
                    } else {
                        val mMatch = marksRegex.find(trimmed)
                        if (mMatch != null) {
                            currentMarks = mMatch.groupValues[1].toIntOrNull() ?: currentMarks
                        }
                        currentQText.append(" ").append(trimmed)
                    }
                }
            }
        }
        flushCurrent()

        return list
    }

    /**
     * Fallback chunker for unstructured text/notes. Ensures the user is NEVER blocked with an empty list.
     */
    private fun fallbackChunkIntoQuestions(text: String): List<SprintQuestion> {
        val clean = text.lines()
            .filterNot { it.trim().startsWith("--- Page") }
            .joinToString("\n")

        // First try splitting by double newline
        var chunks = clean.split(Regex("\n{2,}"))
            .map { it.trim() }
            .filter { it.length > 20 }

        // If double-newline didn't give multiple chunks, split by question marks or bullet lines
        if (chunks.size < 2) {
            val questionMarkSplits = clean.split(Regex("(?<=\\?)\\s+"))
                .map { it.trim() }
                .filter { it.length > 15 }
            if (questionMarkSplits.size >= 2) {
                chunks = questionMarkSplits
            } else {
                val lineSplits = clean.lines()
                    .map { it.trim() }
                    .filter { it.length > 25 }
                if (lineSplits.isNotEmpty()) {
                    chunks = lineSplits
                }
            }
        }

        if (chunks.isEmpty()) {
            if (clean.isNotBlank()) {
                return listOf(
                    SprintQuestion(
                        id = UUID.randomUUID().toString(),
                        questionNumber = 1,
                        questionText = clean.take(150),
                        marks = 10,
                        studentAnswer = clean,
                        aiModelAnswer = clean,
                        isUserAnswerCustomized = true,
                        quickRecall = "Core concept from uploaded notes.",
                        markingRubric = listOf("[2M] Definition", "[4M] Working", "[4M] Conclusion"),
                        masteryStatus = QuestionMastery.UNATTEMPTED
                    )
                )
            }
            return emptyList()
        }

        return chunks.take(15).mapIndexed { idx, p ->
            val firstLine = p.lines().firstOrNull()?.take(140) ?: "Question ${idx + 1}"
            SprintQuestion(
                id = UUID.randomUUID().toString(),
                questionNumber = idx + 1,
                questionText = firstLine,
                marks = 10,
                studentAnswer = p,
                aiModelAnswer = p,
                isUserAnswerCustomized = true,
                quickRecall = "Key concept from uploaded document.",
                markingRubric = listOf("[2M] Core Concept", "[4M] Details & Working", "[4M] Key Points"),
                masteryStatus = QuestionMastery.UNATTEMPTED
            )
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }
            addOnFailureListener { exception ->
                if (cont.isActive) cont.resumeWithException(exception)
            }
        }
}
