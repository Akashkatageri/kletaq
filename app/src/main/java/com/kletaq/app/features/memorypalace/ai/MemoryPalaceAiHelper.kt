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

import com.kletaq.app.core.ai.SecureAiPreferences
import com.kletaq.app.core.ai.UnifiedAiClient
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 🧠 Memory Palace AI Helper
 * Decomposes study material, derivations, concepts, and algorithms into sequential steps anchored to physical loci,
 * generating vivid sensory stories and exam traps.
 */
object MemoryPalaceAiHelper {

    private const val TAG = "MemoryPalaceAi"

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

    internal fun fallbackParseSteps(
        text: String,
        derivationId: String,
        loci: List<PalaceLocus>,
        pageCount: Int = 1
    ): List<PalaceStep> {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 3 && !it.startsWith("Page", ignoreCase = true) }

        if (lines.isEmpty()) return emptyList()

        val steps = mutableListOf<PalaceStep>()
        val sortedLoci = loci.sortedBy { it.orderIndex }
        val totalExpected = (loci.size.takeIf { it > 0 } ?: 6)

        // Find candidate lines (formulas, steps, equations, numbered milestones)
        val candidateLines = lines.filter { line ->
            line.contains("Step", ignoreCase = true) ||
            line.contains("=") ||
            line.contains(":") ||
            line.contains("->") ||
            line.contains("≈") ||
            line.matches(Regex("""^(\d+[\.\)]|[a-zA-Z][\.\)]).*""")) ||
            line.contains(Regex("""\b(where|let|then|hence|therefore|integrate|differentiate|define|formula|law|theorem)\b""", RegexOption.IGNORE_CASE))
        }.ifEmpty { lines }

        val stepChunkSize = (candidateLines.size.toFloat() / totalExpected).coerceAtLeast(1.0f)

        for (i in 0 until totalExpected) {
            val fromIndex = (i * stepChunkSize).toInt().coerceIn(0, candidateLines.lastIndex)
            val toIndex = ((i + 1) * stepChunkSize).toInt().coerceIn(fromIndex + 1, candidateLines.size)
            val chunk = candidateLines.subList(fromIndex, toIndex)
            val formulaText = chunk.joinToString(" ").take(250)

            val locus = sortedLoci.getOrNull(i) ?: sortedLoci.lastOrNull()
            val locusName = locus?.name ?: "Landmark ${i + 1}"
            val locusOrder = locus?.orderIndex ?: (i + 1)
            val pageIdx = if (pageCount > 1) {
                ((i.toFloat() / totalExpected.toFloat()) * pageCount).toInt().coerceIn(0, pageCount - 1)
            } else 0

            val stepNum = i + 1
            val firstLine = chunk.firstOrNull()?.replace(Regex("""^\d+[\.\)]\s*"""), "")?.take(40) ?: "Milestone"
            steps.add(
                PalaceStep(
                    id = UUID.randomUUID().toString(),
                    derivationId = derivationId,
                    stepNumber = stepNum,
                    locusOrderIndex = locusOrder,
                    stepTitle = "Step $stepNum: $firstLine",
                    formulaLatex = formulaText,
                    sensoryStory = "Anchor this step at your $locusName: visualize the concepts physically operating on the surface of $locusName.",
                    examTrap = "Pay strict attention to algebraic signs, limits, and notation on exams.",
                    pageIndex = pageIdx
                )
            )
        }
        return steps
    }

    /**
     * Builds the specialized prompt for unbundling study topics into Method of Loci steps.
     */
    fun buildPrompt(topicTitle: String?, loci: List<PalaceLocus>): String {
        val sortedLoci = loci.sortedBy { it.orderIndex }
        val lociDescriptions = sortedLoci.joinToString("\n") {
            "- Spot #${it.orderIndex}: ${it.name}"
        }

        return """
You are a World Memory Champion using the Method of Loci (spatial memory technique).
Your task is to decompose the study material (which could be ANY topic: engineering theories, programming algorithms, science concepts, mathematical derivations, legal articles, historical timelines, medical anatomy, or multi-point answers) from the uploaded document into exact sequential steps that anchor to the student's personal physical room landmarks.

Target Topic: ${topicTitle ?: "Study Material in Document"}
The student's room has ${sortedLoci.size} ordered physical landmarks:
$lociDescriptions

INSTRUCTIONS:
1. Break down the material into EXACTLY ${sortedLoci.size} logical sequential steps or milestone points (one per room landmark).
2. For each step:
   - "stepNumber": integer starting from 1 to ${sortedLoci.size}.
   - "stepTitle": short descriptive title of this milestone or concept point (e.g. "Core Definition", "Key Formula", "Crucial Phase", "Algorithm Rule", "Key Principle").
   - "formulaLatex": the key statement, formula in LaTeX, code snippet, definition, rule, or primary sentence for this milestone. For non-mathematical topics, provide the clean core takeaway sentence or rule (no LaTeX markup needed if pure text).
   - "sensoryStory": an exaggerated, vivid, surreal sensory mnemonic story. It MUST explicitly feature and interact with that specific room landmark (e.g. "At your [landmark name], an enormous glowing golden object is sweating under the heat..."). Incorporate color, motion, sound, or absurd physical action to make it unforgettable under exam stress.
   - "examTrap": a critical warning pointing out the exact step, keyword, assumption, common misconception, or mistake where students lose marks on exams.

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

            // 1. Multimodal UnifiedAiClient (BYO-AI keys & Firebase AI with high-resolution document bitmaps)
            if (bitmaps.isNotEmpty()) {
                val userPrompt = "Analyze the attached document pages and decompose the study material into EXACTLY ${loci.size} sequential Method of Loci milestones anchored to the room landmarks. Output ONLY the JSON array."
                val aiResult = UnifiedAiClient.generateContent(
                    systemInstruction = prompt,
                    userPrompt = userPrompt,
                    temperature = 0.2f,
                    maxTokens = 4096,
                    images = bitmaps
                )
                val rawAiResponse = aiResult.getOrNull()
                if (!rawAiResponse.isNullOrBlank()) {
                    val steps = parseStepsResponse(rawAiResponse, derivationId, loci, pageCount = bitmaps.size)
                    if (steps.isNotEmpty()) {
                        return@withContext Result.success(DecomposedDerivationResult(steps, savedPaths))
                    }
                }
            }

            // Retain saved bitmaps and return an actionable error prompt for retry.
            Result.failure(Exception("AI could not extract legible topic steps from the document. Please ensure the pages are clear and well-lit, or retry when connected."))
        } catch (e: Exception) {
            Log.e(TAG, "decomposeDerivation error: ${e.message}", e)
            Result.failure(e)
        }
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
                            // Render at high resolution (up to 2048px) so equations remain sharp
                            val scale = Math.min(2048f / page.width, 2048f / page.height).coerceAtLeast(1.5f)
                            val width = (page.width * scale).toInt()
                            val height = (page.height * scale).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
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
                            val scaled = if (maxDim > 2048) {
                                val scale = 2048f / maxDim
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
}

