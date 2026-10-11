package com.kletaq.app.core.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Completely client-side, offline manager for student-uploaded module notes.
 *
 * Capabilities:
 * - Native rasterization of user PDFs using Android's built-in [PdfRenderer].
 * - High-efficiency JPEG compression (75% quality, 1280px max dimension).
 * - Multi-photo ingestion for whiteboard / notebook photos.
 * - Local-only app sandbox storage (`context.filesDir/custom_notes/...`).
 * - Zero Firebase Firestore / Cloud Storage costs, keeping cloud charges at zero.
 */
object CustomNotesManager {

    private const val PREFS_NAME = "kletaq_custom_notes_registry"
    private const val MAX_DIMENSION = 1280

    private fun normalizeSubject(subjectCode: String): String {
        return subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    /**
     * Extracts module/unit index from a topic ID (e.g. "1BCS301_M3_T1" -> 3).
     */
    fun resolveModuleIndex(topicId: String): Int {
        val match = Regex("""(?:m|unit|module)[_-]?(\d+)""", RegexOption.IGNORE_CASE).find(topicId)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    private fun getModuleDir(context: Context, subjectCode: String, moduleIndex: Int): File {
        val cleanSubject = normalizeSubject(subjectCode)
        val dir = File(context.filesDir, "custom_notes/$cleanSubject/m$moduleIndex")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Retrieves all saved note page paths for a given subject and module.
     */
    fun getNotesForModule(context: Context, subjectCode: String, moduleIndex: Int): List<String> {
        val dir = getModuleDir(context, subjectCode, moduleIndex)
        val files = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
        } ?: return emptyList()

        return files.sortedBy { it.name }.map { it.absolutePath }
    }

    /**
     * Ingests a user PDF document from a content URI, rasterizes each page,
     * downsamples to high-readability dimensions, and saves compressed JPEGs locally.
     */
    suspend fun importFromPdf(
        context: Context,
        pdfUri: Uri,
        subjectCode: String,
        moduleIndex: Int
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(Exception("Cannot open PDF file descriptor"))

            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                return@withContext Result.failure(Exception("PDF has no pages"))
            }

            val dir = getModuleDir(context, subjectCode, moduleIndex)
            // Clear prior custom notes for this specific module
            dir.listFiles()?.forEach { it.delete() }

            val savedPaths = mutableListOf<String>()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val origW = page.width
                val origH = page.height

                val scale = (MAX_DIMENSION.toFloat() / maxOf(origW, origH)).coerceAtMost(2.0f)
                val targetW = (origW * scale).toInt().coerceAtLeast(1)
                val targetH = (origH * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE) // Solid white page background

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val fileName = "page_${String.format("%03d", i + 1)}.jpg"
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream)
                }
                bitmap.recycle()

                savedPaths.add(outFile.absolutePath)
            }

            Result.success(savedPaths)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Ingests a list of image URIs (from camera/gallery photo pickers), downsamples,
     * compresses, and stores them sequentially.
     */
    suspend fun importFromImages(
        context: Context,
        imageUris: List<Uri>,
        subjectCode: String,
        moduleIndex: Int
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                return@withContext Result.failure(Exception("No images provided"))
            }

            val dir = getModuleDir(context, subjectCode, moduleIndex)
            dir.listFiles()?.forEach { it.delete() }

            val savedPaths = mutableListOf<String>()

            imageUris.forEachIndexed { index, uri ->
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input, null, opts)

                    var inSampleSize = 1
                    val maxDim = maxOf(opts.outWidth, opts.outHeight)
                    while (maxDim / (inSampleSize * 2) >= MAX_DIMENSION) {
                        inSampleSize *= 2
                    }

                    // Decode sampled
                    context.contentResolver.openInputStream(uri)?.use { stream2 ->
                        val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                        val rawBmp = BitmapFactory.decodeStream(stream2, null, decodeOpts)
                        if (rawBmp != null) {
                            val fileName = "page_${String.format("%03d", index + 1)}.jpg"
                            val outFile = File(dir, fileName)
                            FileOutputStream(outFile).use { outStream ->
                                rawBmp.compress(Bitmap.CompressFormat.JPEG, 75, outStream)
                            }
                            rawBmp.recycle()
                            savedPaths.add(outFile.absolutePath)
                        }
                    }
                }
            }

            Result.success(savedPaths)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears all local custom notes for a given module.
     */
    fun deleteNotesForModule(context: Context, subjectCode: String, moduleIndex: Int): Boolean {
        val dir = getModuleDir(context, subjectCode, moduleIndex)
        var success = true
        dir.listFiles()?.forEach {
            if (!it.delete()) success = false
        }
        return success
    }
}
