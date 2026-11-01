package com.kletaq.app.features.memorypalace.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * 🧠 Memory Palace AI Helper
 * Decomposes math derivations and algorithms into sequential steps anchored to physical loci,
 * generating vivid Patrick Jane sensory stories and VTU exam traps.
 */
object MemoryPalaceAiHelper {

    private const val TAG = "MemoryPalaceAi"
    private const val DEFAULT_MODEL = "gemini-2.0-flash"

    /**
     * Parse raw AI output (which may be wrapped in markdown ```json codeblocks or prose)
     * into a strongly-typed list of PalaceStep entities bound to loci.
     */
    fun parseStepsResponse(
        rawResponse: String,
        derivationId: String,
        loci: List<PalaceLocus>
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

                result.add(
                    PalaceStep(
                        id = UUID.randomUUID().toString(),
                        derivationId = derivationId,
                        stepNumber = stepNum,
                        locusOrderIndex = locusIndex,
                        stepTitle = stepTitle,
                        formulaLatex = formulaLatex,
                        sensoryStory = sensoryStory,
                        examTrap = examTrap
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI JSON response: ${e.message}", e)
            // Fallback deterministic line-by-line parsing if JSON parse fails
            return fallbackParseSteps(rawResponse, derivationId, loci)
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
        loci: List<PalaceLocus>
    ): List<PalaceStep> {
        val lines = text.lines().filter { it.isNotBlank() }
        val steps = mutableListOf<PalaceStep>()
        val sortedLoci = loci.sortedBy { it.orderIndex }

        var currentStepNum = 1
        for (line in lines) {
            if (line.contains("Step", ignoreCase = true) || line.contains("=", ignoreCase = true)) {
                val locusIndex = if (sortedLoci.isNotEmpty()) {
                    val locus = sortedLoci.getOrNull(steps.size) ?: sortedLoci.last()
                    locus.orderIndex
                } else {
                    currentStepNum
                }

                steps.add(
                    PalaceStep(
                        id = UUID.randomUUID().toString(),
                        derivationId = derivationId,
                        stepNumber = currentStepNum,
                        locusOrderIndex = locusIndex,
                        stepTitle = "Milestone $currentStepNum",
                        formulaLatex = line.trim(),
                        sensoryStory = "Visualize this transformation anchored to your locus.",
                        examTrap = "Pay strict attention to algebraic signs and variable limits."
                    )
                )
                currentStepNum++
                if (steps.size >= (loci.size.takeIf { it > 0 } ?: 6)) break
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
You are a World Memory Champion using the Method of Loci (Patrick Jane / Mentalist technique) and a Senior VTU Engineering Professor.
Your task is to decompose the mathematical derivation or programming algorithm from the uploaded document into exact sequential steps that anchor to the student's personal physical room landmarks.

Target Topic: ${topicTitle ?: "Derivation / Algorithm in Document"}
The student's room has ${sortedLoci.size} ordered physical landmarks:
$lociDescriptions

INSTRUCTIONS:
1. Break down the proof/derivation into EXACTLY ${sortedLoci.size} logical sequential steps (one step per room landmark).
2. For each step:
   - "stepNumber": integer starting from 1 to ${sortedLoci.size}.
   - "stepTitle": short descriptive title of this mathematical or code milestone (e.g. "Substitute Taylor Series", "Apply Limit as n -> infinity").
   - "formulaLatex": clean mathematical formula formatted with LaTeX (e.g. "P(X = x) = \\lim_{n \\to \\infty} \\binom{n}{x} p^x (1-p)^{n-x}") or formatted code line.
   - "sensoryStory": an exaggerated, vivid, surreal sensory mnemonic story in the style of Patrick Jane from The Mentalist. It MUST explicitly feature and interact with that specific room landmark (e.g. "At your [landmark name], an enormous glowing golden lambda is sweating under the heat..."). Incorporate color, motion, sound, or absurd physical action to make it unforgettable under exam stress.
   - "examTrap": a critical warning pointing out the exact step, index, or assumption where students lose marks on university exams.

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
    ): Result<List<PalaceStep>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(topicTitle, loci)
            SecureAiPreferences.init(context)
            val keyEntry = SecureAiPreferences.getNextKey()
            val apiKey = keyEntry?.apiKey
            val model = keyEntry?.model ?: DEFAULT_MODEL

            // If we have an API key, invoke Gemini vision
            if (!apiKey.isNullOrBlank()) {
                val bitmaps = loadBitmaps(context, uris, maxPages = 4)
                val response = callGeminiVision(apiKey, model, bitmaps, prompt)
                if (response != null) {
                    val steps = parseStepsResponse(response, derivationId, loci)
                    if (steps.isNotEmpty()) {
                        return@withContext Result.success(steps)
                    }
                }
            }

            // Fallback: On-device ML Kit OCR
            val extractedText = extractTextWithMlKit(context, uris)
            val fallbackSteps = fallbackParseSteps(extractedText, derivationId, loci)
            if (fallbackSteps.isNotEmpty()) {
                return@withContext Result.success(fallbackSteps)
            }

            Result.failure(Exception("Could not extract derivation steps from the provided documents."))
        } catch (e: Exception) {
            Log.e(TAG, "decomposeDerivation error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun extractTextWithMlKit(context: Context, uris: List<Uri>): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = java.lang.StringBuilder()

        for (uri in uris.take(4)) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val visionText = recognizer.process(inputImage).await()
                sb.append(visionText.text).append("\n")
            } catch (e: Exception) {
                Log.w(TAG, "ML Kit failed for uri $uri: ${e.message}")
            }
        }
        return sb.toString()
    }

    private fun loadBitmaps(context: Context, uris: List<Uri>, maxPages: Int): List<Bitmap> {
        val result = mutableListOf<Bitmap>()
        for (uri in uris.take(maxPages)) {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                if (stream != null) {
                    val original = BitmapFactory.decodeStream(stream)
                    stream.close()
                    if (original != null) {
                        // Downscale to max 1024px to prevent OOM
                        val maxDim = Math.max(original.width, original.height)
                        val scaled = if (maxDim > 1024) {
                            val scale = 1024f / maxDim
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
        return result
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

