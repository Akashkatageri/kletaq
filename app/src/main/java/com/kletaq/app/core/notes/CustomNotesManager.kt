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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manager for student-uploaded module notes with on-device compression and Cloudflare R2 online storage.
 *
 * Capabilities:
 * - Native rasterization of user PDFs using Android's built-in [PdfRenderer].
 * - High-efficiency JPEG compression (75% quality, 1280px max dimension).
 * - Multi-photo ingestion for whiteboard / notebook photos.
 * - Local-first app sandbox caching (`context.filesDir/custom_notes/...`).
 * - Cloudflare R2 online storage and Firestore manifest sync across devices.
 */
object CustomNotesManager {

    private const val PREFS_NAME = "kletaq_custom_notes_registry"
    private const val MAX_DIMENSION = 1080
    private const val JPEG_QUALITY = 60
    private const val MAX_PAGE_BYTES_THRESHOLD = 110 * 1024 // 110 KB cap per page

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
     * Returns local cached file paths if available, otherwise retrieves online Cloudflare R2 URLs.
     */
    fun getNotesForModule(context: Context, subjectCode: String, moduleIndex: Int): List<String> {
        val cleanSubject = normalizeSubject(subjectCode)
        val dir = getModuleDir(context, subjectCode, moduleIndex)
        val files = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
        }
        if (files != null && files.isNotEmpty()) {
            return files.sortedBy { it.name }.map { it.absolutePath }
        }

        // If local files are not on this device, return cached Cloudflare R2 URLs
        return CloudflareR2Uploader.getCachedCloudPages(context, cleanSubject, moduleIndex)
    }

    /**
     * Ingests a single user PDF document from a content URI.
     */
    suspend fun importFromPdf(
        context: Context,
        pdfUri: Uri,
        subjectCode: String,
        moduleIndex: Int,
        append: Boolean = false
    ): Result<List<String>> {
        return importFromPdfs(context, listOf(pdfUri), subjectCode, moduleIndex, append)
    }

    /**
     * Ingests one or more user PDF documents, rasterizes each page,
     * downsamples to high-readability dimensions, and saves compressed JPEGs locally.
     * When [append] is true, new pages continue numbering after existing pages.
     */
    suspend fun importFromPdfs(
        context: Context,
        pdfUris: List<Uri>,
        subjectCode: String,
        moduleIndex: Int,
        append: Boolean = false,
        onProgress: ((currentPdf: Int, totalPdfs: Int, currentPage: Int, totalPagesInPdf: Int) -> Unit)? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (pdfUris.isEmpty()) {
            return@withContext Result.failure(Exception("No PDF files selected"))
        }

        val dir = getModuleDir(context, subjectCode, moduleIndex)
        if (!append) {
            dir.listFiles()?.forEach { it.delete() }
        }

        // Find starting index based on existing note files
        val existingFiles = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
        } ?: emptyArray()
        var nextPageIndex = if (append) existingFiles.size + 1 else 1

        val newlySavedPaths = mutableListOf<String>()

        try {
            pdfUris.forEachIndexed { pdfIndex, uri ->
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        renderer = PdfRenderer(pfd)
                        val pageCount = renderer.pageCount
                        for (i in 0 until pageCount) {
                            onProgress?.invoke(pdfIndex + 1, pdfUris.size, i + 1, pageCount)
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

                            val fileName = "page_${String.format("%03d", nextPageIndex++)}.jpg"
                            val outFile = File(dir, fileName)
                            writeCompressedBitmap(bitmap, outFile)
                            bitmap.recycle()

                            newlySavedPaths.add(outFile.absolutePath)
                        }
                    }
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                }
            }

            val allLocalFiles = dir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
            }?.sortedBy { it.name } ?: emptyList()

            // Upload compressed pages online to Cloudflare R2 & sync manifest with Firestore
            val uploadResult = CloudflareR2Uploader.uploadModulePages(context, subjectCode, moduleIndex, allLocalFiles)
            val allPages = uploadResult.getOrElse { allLocalFiles.map { it.absolutePath } }

            ModuleNotesRepository.notifyNotesChanged(context, subjectCode, moduleIndex)
            Result.success(allPages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ingests a list of image URIs (from camera/gallery photo pickers), downsamples,
     * compresses, and stores them sequentially.
     * When [append] is true, new photos continue numbering after existing pages.
     */
    suspend fun importFromImages(
        context: Context,
        imageUris: List<Uri>,
        subjectCode: String,
        moduleIndex: Int,
        append: Boolean = false,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                return@withContext Result.failure(Exception("No images provided"))
            }

            val dir = getModuleDir(context, subjectCode, moduleIndex)
            if (!append) {
                dir.listFiles()?.forEach { it.delete() }
            }

            val existingFiles = dir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
            } ?: emptyArray()
            var nextPageIndex = if (append) existingFiles.size + 1 else 1

            imageUris.forEachIndexed { index, uri ->
                onProgress?.invoke(index + 1, imageUris.size)
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
                            val fileName = "page_${String.format("%03d", nextPageIndex++)}.jpg"
                            val outFile = File(dir, fileName)
                            writeCompressedBitmap(rawBmp, outFile)
                            rawBmp.recycle()
                        }
                    }
                }
            }

            val allLocalFiles = dir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
            }?.sortedBy { it.name } ?: emptyList()

            // Upload compressed pages online to Cloudflare R2 & sync manifest with Firestore
            val uploadResult = CloudflareR2Uploader.uploadModulePages(context, subjectCode, moduleIndex, allLocalFiles)
            val allPages = uploadResult.getOrElse { allLocalFiles.map { it.absolutePath } }

            ModuleNotesRepository.notifyNotesChanged(context, subjectCode, moduleIndex)
            Result.success(allPages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compresses a page bitmap to a lightweight, high-readability JPEG (~60-95 KB per page).
     * If the first pass at 60% quality exceeds the target threshold (110 KB), a second pass
     * at 50% quality is executed to guarantee aggressive compression.
     */
    private fun writeCompressedBitmap(bitmap: Bitmap, outFile: File) {
        FileOutputStream(outFile).use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outStream)
        }
        if (outFile.length() > MAX_PAGE_BYTES_THRESHOLD) {
            FileOutputStream(outFile).use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outStream)
            }
        }
    }

    /**
     * Clears all local custom notes for a given module and deletes cloud registry.
     */
    fun deleteNotesForModule(context: Context, subjectCode: String, moduleIndex: Int): Boolean {
        val dir = getModuleDir(context, subjectCode, moduleIndex)
        var success = true
        dir.listFiles()?.forEach {
            if (!it.delete()) success = false
        }
        CoroutineScope(Dispatchers.IO).launch {
            CloudflareR2Uploader.deleteModuleNotesFromCloud(context, subjectCode, moduleIndex)
        }
        ModuleNotesRepository.notifyNotesChanged(context, subjectCode, moduleIndex)
        return success
    }
}
