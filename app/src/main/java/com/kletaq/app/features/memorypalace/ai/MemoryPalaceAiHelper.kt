package com.kletaq.app.features.memorypalace.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kletaq.app.core.ai.SecureAiPreferences
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * 🧠 Memory Palace AI Helper
 * Decomposes study material, derivations, concepts, and algorithms into sequential steps anchored to physical loci,
 * generating vivid sensory stories and exam traps.
 */
object MemoryPalaceAiHelper {

    private const val TAG = "MemoryPalaceAi"
    private const val DEFAULT_MODEL = "gemini-3.5-flash"

    data class DecomposedDerivationResult(
        val steps: List<PalaceStep>,
        val sourceDocumentPaths: List<String>
    )

    /**
     * Parse raw AI output (which may be wrapped in markdown ```json codeblocks or prose)
     * into a strongly-typed list of PalaceStep entities bound to loci.
     */
    fun parseStepsResponse(
        rawResponse: String,
        derivationId: String,
        loci: List<PalaceLocus>,
        pageCount: Int = 1
    ): List<PalaceStep> {
        val cleaned = extractJsonArrayString(rawResponse)
        val result = mutableListOf<PalaceStep>()

        try {
            val jsonArray = JSONArray(cleaned)
            val sortedLoci = loci.sortedBy { it.orderIndex }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val stepNum = obj.optInt("stepNumber", i + 1)
                val stepTitle = obj.optString("stepTitle", "Step $stepNum")
                val formulaLatex = obj.optString("formulaLatex", "")
                val sensoryStory = obj.optString("sensoryStory", "")
                val examTrap = obj.optString("examTrap", "")

                // Map to corresponding locus or wrap around
                val locusIndex = if (sortedLoci.isNotEmpty()) {
                    val locus = sortedLoci.getOrNull(i) ?: sortedLoci.last()
                    locus.orderIndex
                } else {
                    i + 1
                }

                val pageIdx = if (pageCount > 1) {
                    ((i.toFloat() / jsonArray.length().toFloat()) * pageCount).toInt().coerceIn(0, pageCount - 1)
                } else 0

                result.add(
                    PalaceStep(
                        id = UUID.randomUUID().toString(),
                        derivationId = derivationId,
                        stepNumber = stepNum,
                        locusOrderIndex = locusIndex,
                        stepTitle = stepTitle,
                        formulaLatex = formulaLatex,
                        sensoryStory = sensoryStory,
                        examTrap = examTrap,
                        pageIndex = pageIdx
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI JSON response: ${e.message}", e)
            // Fallback deterministic line-by-line parsing if JSON parse fails
            return fallbackParseSteps(rawResponse, derivationId, loci, pageCount)
        }

        return result
    }

    private fun extractJsonArrayString(text: String): String {
        val trimmed = text.trim()
        val jsonStart = trimmed.indexOf('[')
        val jsonEnd = trimmed.lastIndexOf(']')
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1)
        }
        return "[]"
    }

    private fun fallbackParseSteps(
        text: String,
        derivationId: String,
        loci: List<PalaceLocus>,
        pageCount: Int = 1
    ): List<PalaceStep> {
        val lines = text.lines().filter { it.isNotBlank() }
        val steps = mutableListOf<PalaceStep>()
        val sortedLoci = loci.sortedBy { it.orderIndex }
        val totalExpected = (loci.size.takeIf { it > 0 } ?: 6)

        var currentStepNum = 1
        for (line in lines) {
            if (line.contains("Step", ignoreCase = true) || line.contains("=", ignoreCase = true)) {
                val locusIndex = if (sortedLoci.isNotEmpty()) {
                    val locus = sortedLoci.getOrNull(steps.size) ?: sortedLoci.last()
                    locus.orderIndex
                } else {
                    currentStepNum
                }

                val pageIdx = if (pageCount > 1) {
                    ((steps.size.toFloat() / totalExpected.toFloat()) * pageCount).toInt().coerceIn(0, pageCount - 1)
                } else 0

                steps.add(
                    PalaceStep(
                        id = UUID.randomUUID().toString(),
                        derivationId = derivationId,
                        stepNumber = currentStepNum,
                        locusOrderIndex = locusIndex,
                        stepTitle = "Milestone $currentStepNum",
                        formulaLatex = line.trim(),
                        sensoryStory = "Visualize this transformation anchored to your locus.",
                        examTrap = "Pay strict attention to algebraic signs and variable limits.",
                        pageIndex = pageIdx
                    )
                )
                currentStepNum++
                if (steps.size >= totalExpected) break
            }
        }
        return steps
    }

    /**
     * Builds the specialized prompt for unbundling derivations into Method of Loci steps.
     */
    fun buildPrompt(topicTitle: String?, loci: List<PalaceLocus>): String {
        val sortedLoci = loci.sortedBy { it.orderIndex }
        val lociDescriptions = sortedLoci.joinToString("\n") {
            "- Spot #${it.orderIndex}: ${it.name}"
        }

        return """
You are a World Memory Champion using the Method of Loci (spatial memory technique).
Your task is to decompose the study material (which could be mathematical derivations, programming algorithms, science concepts, legal articles, historical timelines, medical anatomy, or multi-point answers) from the uploaded document into exact sequential steps that anchor to the student's personal physical room landmarks.

Target Topic: ${topicTitle ?: "Study Material in Document"}
The student's room has ${sortedLoci.size} ordered physical landmarks:
$lociDescriptions

INSTRUCTIONS:
1. Break down the material into EXACTLY ${sortedLoci.size} logical sequential steps or milestone points (one per room landmark).
2. For each step:
   - "stepNumber": integer starting from 1 to ${sortedLoci.size}.
   - "stepTitle": short descriptive title of this milestone or concept point (e.g. "Initial Postulate", "Key Formula", "Core Principle", "Crucial Phase").
   - "formulaLatex": clean mathematical formula formatted with LaTeX, formatted code snippet, definition, or primary key sentence.
   - "sensoryStory": an exaggerated, vivid, surreal sensory mnemonic story. It MUST explicitly feature and interact with that specific room landmark (e.g. "At your [landmark name], an enormous glowing golden object is sweating under the heat..."). Incorporate color, motion, sound, or absurd physical action to make it unforgettable under exam stress.
   - "examTrap": a critical warning pointing out the exact step, keyword, assumption, or mistake where students lose marks on exams.

OUTPUT REQUIREMENT:
Return ONLY a valid JSON array of objects with the keys above. Do not include markdown preamble outside the JSON array.
        """.trimIndent()
    }

    /**
     * End-to-end derivation unbundler from image/PDF URIs.
     */
    suspend fun decomposeDerivation(
        context: Context,
        uris: List<Uri>,
        topicTitle: String?,
        loci: List<PalaceLocus>,
        derivationId: String
    ): Result<DecomposedDerivationResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(topicTitle, loci)
            val bitmaps = loadDocumentBitmaps(context, uris, maxPages = 4)
            val savedPaths = if (bitmaps.isNotEmpty()) {
                saveDocumentBitmaps(context, derivationId, bitmaps)
            } else {
                emptyList()
            }

            SecureAiPreferences.init(context)
            val keyEntry = SecureAiPreferences.getNextKey()
            val apiKey = keyEntry?.apiKey

            val modelsToTry = if (!keyEntry?.model.isNullOrBlank()) {
                listOf(keyEntry!!.model, "gemini-3.5-flash", "gemini-2.5-pro", "gemini-2.5-flash").distinct()
            } else {
                listOf("gemini-3.5-flash", "gemini-2.5-pro", "gemini-2.5-flash")
            }

            // If we have an API key and bitmaps, invoke Gemini vision with cascade fallback
            if (!apiKey.isNullOrBlank() && bitmaps.isNotEmpty()) {
                for (targetModel in modelsToTry) {
                    val response = callGeminiVision(apiKey, targetModel, bitmaps, prompt)
                    if (response != null) {
                        val steps = parseStepsResponse(response, derivationId, loci, pageCount = bitmaps.size)
                        if (steps.isNotEmpty()) {
                            return@withContext Result.success(DecomposedDerivationResult(steps, savedPaths))
                        }
                    }
                }
            }

            // Fallback: On-device ML Kit OCR from the rendered bitmaps
            val extractedText = extractTextWithMlKit(bitmaps)
            val fallbackSteps = fallbackParseSteps(extractedText, derivationId, loci, pageCount = bitmaps.size.coerceAtLeast(1))
            if (fallbackSteps.isNotEmpty()) {
                return@withContext Result.success(DecomposedDerivationResult(fallbackSteps, savedPaths))
            }

            Result.failure(Exception("Could not extract derivation steps from the provided documents."))
        } catch (e: Exception) {
            Log.e(TAG, "decomposeDerivation error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun extractTextWithMlKit(bitmaps: List<Bitmap>): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = java.lang.StringBuilder()

        for (bitmap in bitmaps) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.process(inputImage).await()
                sb.append(visionText.text).append("\n")
            } catch (e: Exception) {
                Log.w(TAG, "ML Kit failed for bitmap: ${e.message}")
            }
        }
        return sb.toString()
    }

    /**
     * Renders both PDF and image URIs into high-resolution bitmaps.
     * Uses native Android PdfRenderer for PDFs so multi-page math notes render clearly.
     */
    fun loadDocumentBitmaps(context: Context, uris: List<Uri>, maxPages: Int = 4): List<Bitmap> {
        val result = mutableListOf<Bitmap>()
        for (uri in uris) {
            if (result.size >= maxPages) break
            val mimeType = context.contentResolver.getType(uri)
            val isPdf = mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)

            if (isPdf) {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        val renderer = PdfRenderer(pfd)
                        val pagesToTake = Math.min(renderer.pageCount, maxPages - result.size)
                        for (i in 0 until pagesToTake) {
                            val page = renderer.openPage(i)
                            // Render at high resolution (up to 1400px) so equations remain sharp
                            val scale = Math.min(1400f / page.width, 1400f / page.height).coerceAtLeast(1.5f)
                            val width = (page.width * scale).toInt()
                            val height = (page.height * scale).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            result.add(bitmap)
                        }
                        renderer.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to render PDF pages from $uri: ${e.message}", e)
                }
            } else {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) {
                            val maxDim = Math.max(original.width, original.height)
                            val scaled = if (maxDim > 1400) {
                                val scale = 1400f / maxDim
                                Bitmap.createScaledBitmap(
                                    original,
                                    (original.width * scale).toInt(),
                                    (original.height * scale).toInt(),
                                    true
                                )
                            } else {
                                original
                            }
                            result.add(scaled)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load bitmap from uri $uri: ${e.message}")
                }
            }
        }
        return result
    }

    /**
     * Saves rendered page bitmaps to internal app storage so students can inspect the exact pages
     * anytime in Palace Walk mode.
     */
    fun saveDocumentBitmaps(context: Context, derivationId: String, bitmaps: List<Bitmap>): List<String> {
        val paths = mutableListOf<String>()
        try {
            val dir = File(context.filesDir, "palace_notes/$derivationId").apply { mkdirs() }
            bitmaps.forEachIndexed { index, bitmap ->
                val file = File(dir, "page_${index + 1}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                paths.add(file.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save document pages to disk: ${e.message}", e)
        }
        return paths
    }

    private fun callGeminiVision(apiKey: String, model: String, bitmaps: List<Bitmap>, promptText: String): String? {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 45000

            val parts = JSONArray()
            parts.put(JSONObject().put("text", promptText))

            for (bitmap in bitmaps) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                parts.put(
                    JSONObject().put(
                        "inline_data",
                        JSONObject().put("mime_type", "image/jpeg").put("data", base64)
                    )
                )
            }

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            }

            conn.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val respObj = JSONObject(responseText)
                val candidates = respObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val resParts = content?.optJSONArray("parts")
                    if (resParts != null && resParts.length() > 0) {
                        return resParts.getJSONObject(0).optString("text")
                    }
                }
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Gemini API HTTP ${conn.responseCode}: $errText")
            }
        } catch (e: Exception) {
            Log.e(TAG, "callGeminiVision exception: ${e.message}", e)
        }
        return null
    }
}

