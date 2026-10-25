package com.kletaq.app.core.notes

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PageOcrRecord(
    val pageIndex: Int,          // 1-based page index (e.g. 1, 2, 3...)
    val fileName: String,        // e.g. "page_001.jpg"
    val fileFingerprint: String, // "${file.length()}_${file.lastModified()}"
    val text: String,            // Raw extracted OCR text
    val wordCount: Int,
    val isReadable: Boolean      // true if text contains readable words (>= 3 words)
)

data class ModuleOcrManifest(
    val subjectCode: String,
    val moduleIndex: Int,
    val lastScanTimestamp: Long,
    val pages: List<PageOcrRecord>
)

/**
 * On-device, incremental OCR engine for student-attached module notes.
 * Extracts text page-by-page, caches results by file fingerprint, and runs 100% off-thread.
 */
object NotesTextExtractor {

    private const val TAG = "NotesTextExtractor"
    private const val MANIFEST_FILE_NAME = "notes_manifest.json"

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private fun getModuleDir(context: Context, subjectCode: String, moduleIndex: Int): File {
        val cleanSubject = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        return File(context.filesDir, "custom_notes/$cleanSubject/m$moduleIndex")
    }

    /**
     * Loads the cached OCR manifest if available.
     */
    fun loadCachedManifest(context: Context, subjectCode: String, moduleIndex: Int): ModuleOcrManifest? {
        val dir = getModuleDir(context, subjectCode, moduleIndex)
        val manifestFile = File(dir, MANIFEST_FILE_NAME)
        if (!manifestFile.exists()) return null

        return try {
            val jsonStr = manifestFile.readText()
            val root = JSONObject(jsonStr)
            val sub = root.optString("subjectCode", subjectCode)
            val mod = root.optInt("moduleIndex", moduleIndex)
            val timestamp = root.optLong("lastScanTimestamp", 0L)
            val pagesArray = root.optJSONArray("pages") ?: JSONArray()

            val pageRecords = mutableListOf<PageOcrRecord>()
            for (i in 0 until pagesArray.length()) {
                val pageObj = pagesArray.getJSONObject(i)
                pageRecords.add(
                    PageOcrRecord(
                        pageIndex = pageObj.optInt("pageIndex", i + 1),
                        fileName = pageObj.optString("fileName", ""),
                        fileFingerprint = pageObj.optString("fileFingerprint", ""),
                        text = pageObj.optString("text", ""),
                        wordCount = pageObj.optInt("wordCount", 0),
                        isReadable = pageObj.optBoolean("isReadable", false)
                    )
                )
            }
            ModuleOcrManifest(
                subjectCode = sub,
                moduleIndex = mod,
                lastScanTimestamp = timestamp,
                pages = pageRecords
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading OCR manifest: ${e.message}")
            null
        }
    }

    private fun saveManifest(dir: File, manifest: ModuleOcrManifest) {
        try {
            val root = JSONObject()
            root.put("subjectCode", manifest.subjectCode)
            root.put("moduleIndex", manifest.moduleIndex)
            root.put("lastScanTimestamp", manifest.lastScanTimestamp)

            val pagesArray = JSONArray()
            manifest.pages.forEach { page ->
                val pObj = JSONObject()
                pObj.put("pageIndex", page.pageIndex)
                pObj.put("fileName", page.fileName)
                pObj.put("fileFingerprint", page.fileFingerprint)
                pObj.put("text", page.text)
                pObj.put("wordCount", page.wordCount)
                pObj.put("isReadable", page.isReadable)
                pagesArray.put(pObj)
            }
            root.put("pages", pagesArray)

            val manifestFile = File(dir, MANIFEST_FILE_NAME)
            manifestFile.writeText(root.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving OCR manifest: ${e.message}")
        }
    }

    /**
     * Performs incremental OCR across all note page images for the given subject and module.
     * Reuses cached records for unchanged files and processes only new/modified pages.
     */
    suspend fun extractTextForModule(
        context: Context,
        subjectCode: String,
        moduleIndex: Int,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ModuleOcrManifest = withContext(Dispatchers.Default) {
        val dir = getModuleDir(context, subjectCode, moduleIndex)
        if (!dir.exists()) {
            return@withContext ModuleOcrManifest(subjectCode, moduleIndex, System.currentTimeMillis(), emptyList())
        }

        val noteFiles = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
        }?.sortedBy { it.name } ?: emptyList()

        if (noteFiles.isEmpty()) {
            val emptyManifest = ModuleOcrManifest(subjectCode, moduleIndex, System.currentTimeMillis(), emptyList())
            saveManifest(dir, emptyManifest)
            return@withContext emptyManifest
        }

        val cachedManifest = loadCachedManifest(context, subjectCode, moduleIndex)
        val cachedMap = cachedManifest?.pages?.associateBy { it.fileName } ?: emptyMap()

        val updatedPages = mutableListOf<PageOcrRecord>()
        var recognizer: com.google.mlkit.vision.text.TextRecognizer? = null

        try {
            noteFiles.forEachIndexed { index, file ->
                onProgress?.invoke(index + 1, noteFiles.size)
                val currentFingerprint = "${file.length()}_${file.lastModified()}"
                val cached = cachedMap[file.name]

                if (cached != null && cached.fileFingerprint == currentFingerprint) {
                    // Cache hit: file has not changed
                    updatedPages.add(cached.copy(pageIndex = index + 1))
                } else {
                    // Cache miss: process image via ML Kit OCR off the main thread
                    if (recognizer == null) {
                        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    }

                    var rawText = ""
                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            val inputImage = InputImage.fromBitmap(bitmap, 0)
                            val visionText = recognizer?.process(inputImage)?.awaitResult()
                            rawText = visionText?.text ?: ""
                            bitmap.recycle()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "OCR failed for ${file.name}: ${e.message}")
                    }

                    val words = rawText.split(Regex("\\s+")).filter { it.isNotBlank() }
                    val wordCount = words.size
                    val isReadable = wordCount >= 3

                    updatedPages.add(
                        PageOcrRecord(
                            pageIndex = index + 1,
                            fileName = file.name,
                            fileFingerprint = currentFingerprint,
                            text = rawText.trim(),
                            wordCount = wordCount,
                            isReadable = isReadable
                        )
                    )
                }
            }
        } finally {
            recognizer?.close()
        }

        val resultManifest = ModuleOcrManifest(
            subjectCode = subjectCode,
            moduleIndex = moduleIndex,
            lastScanTimestamp = System.currentTimeMillis(),
            pages = updatedPages
        )

        saveManifest(dir, resultManifest)
        resultManifest
    }

    /**
     * Resolves the 1-based page range (e.g. 5..6) covering a topic within the module notes.
     * Uses a 4-tier fail-safe hierarchy:
     * Tier 1: User's manual override from UserSettingsRepository (if set).
     * Tier 2: Small notes safe mode (if total pages <= 4, shows all pages).
     * Tier 3: OCR keyword search in ModuleOcrManifest.
     * Tier 4: Proportional syllabus index partitioning.
     */
    fun resolveTopicPageRange(
        topicId: String,
        topicTitle: String,
        topicIndex: Int,
        totalTopicsInModule: Int,
        totalPages: Int,
        manifest: ModuleOcrManifest? = null
    ): IntRange {
        if (totalPages <= 0) return 1..1
        if (totalPages <= 4) return 1..totalPages

        // Tier 1: Manual student override
        val manual = com.kletaq.app.data.repository.UserSettingsRepository.getManualTopicPageRange(topicId)
        if (manual != null) {
            val s = manual.first.coerceIn(1, totalPages)
            val e = manual.last.coerceIn(s, totalPages)
            return s..e
        }

        // Tier 3: OCR keyword search
        if (manifest != null && manifest.pages.isNotEmpty()) {
            val stopWords = setOf("and", "the", "for", "with", "from", "using", "into", "introduction", "theory", "basic", "basics", "overview")
            val keywords = topicTitle.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 && !stopWords.contains(it) }

            if (keywords.isNotEmpty()) {
                val matchingPageIndices = manifest.pages.filter { page ->
                    if (!page.isReadable) false
                    else {
                        val pageText = page.text.lowercase()
                        keywords.count { kw -> pageText.contains(kw) } >= 1
                    }
                }.map { it.pageIndex }

                if (matchingPageIndices.isNotEmpty()) {
                    val firstMatch = matchingPageIndices.first()
                    // Clamp to max 2-3 consecutive pages starting at first match
                    val lastMatch = (firstMatch + 1).coerceAtMost(totalPages)
                    return firstMatch..lastMatch
                }
            }
        }

        // Tier 4: Proportional mathematical fallback
        val numTopics = totalTopicsInModule.coerceAtLeast(1)
        val pagesPerTopic = (totalPages.toFloat() / numTopics.toFloat())
        val startPage = ((topicIndex * pagesPerTopic).toInt() + 1).coerceIn(1, totalPages)
        val endPage = (((topicIndex + 1) * pagesPerTopic).toInt()).coerceIn(startPage, totalPages).coerceAtMost(startPage + 2)
        return startPage..endPage
    }
}

