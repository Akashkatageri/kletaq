package com.kletaq.app.core.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class CompressionResult(
    val compressedFiles: List<File>,
    val originalTotalBytes: Long,
    val compressedTotalBytes: Long
) {
    val savingsPercentage: Int
        get() = if (originalTotalBytes > 0) {
            (((originalTotalBytes - compressedTotalBytes).toDouble() / originalTotalBytes) * 100).toInt().coerceIn(0, 99)
        } else 0
}

object NoteCompressionEngine {

    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 80

    /**
     * Inspects a URI's file size via ContentResolver.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Inspects whether a URI is a PDF document based on MIME type or filename.
     */
    fun isPdf(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        if (type != null && type.contains("pdf", ignoreCase = true)) return true
        val path = uri.path ?: ""
        return path.endsWith(".pdf", ignoreCase = true)
    }

    /**
     * Compresses a mixed list of Image and PDF URIs into high-efficiency JPEG pages.
     */
    suspend fun compressNotes(
        context: Context,
        uris: List<Uri>,
        noteId: String,
        onProgress: ((current: Int, total: Int, label: String) -> Unit)? = null
    ): Result<CompressionResult> = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No files provided for compression."))
        }

        try {
            val stagingDir = File(context.cacheDir, "compressed_notes/$noteId").apply {
                if (!exists()) mkdirs()
            }
            // Clear any previous attempts
            stagingDir.listFiles()?.forEach { it.delete() }

            val compressedFiles = mutableListOf<File>()
            var originalTotalBytes = 0L
            var nextPageIndex = 1

            for ((fileIndex, uri) in uris.withIndex()) {
                val origSize = getFileSize(context, uri)
                originalTotalBytes += if (origSize > 0) origSize else 4_000_000L

                if (isPdf(context, uri)) {
                    // Rasterize & compress PDF pages
                    onProgress?.invoke(fileIndex + 1, uris.size, "Rasterizing PDF document...")
                    var pfd: ParcelFileDescriptor? = null
                    var renderer: PdfRenderer? = null
                    try {
                        pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        if (pfd != null) {
                            renderer = PdfRenderer(pfd)
                            val pageCount = renderer.pageCount
                            for (p in 0 until pageCount) {
                                onProgress?.invoke(
                                    fileIndex + 1,
                                    uris.size,
                                    "Rendering PDF page ${p + 1} of $pageCount..."
                                )
                                val page = renderer.openPage(p)
                                val origW = page.width
                                val origH = page.height

                                val scale = (MAX_DIMENSION.toFloat() / maxOf(origW, origH)).coerceAtMost(2.0f)
                                val targetW = (origW * scale).toInt().coerceAtLeast(1)
                                val targetH = (origH * scale).toInt().coerceAtLeast(1)

                                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE) // Solid white background for clean text

                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()

                                val outFile = File(stagingDir, "page_${String.format("%03d", nextPageIndex++)}.jpg")
                                FileOutputStream(outFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                                }
                                bitmap.recycle()
                                compressedFiles.add(outFile)
                            }
                        }
                    } finally {
                        try { renderer?.close() } catch (_: Exception) {}
                        try { pfd?.close() } catch (_: Exception) {}
                    }
                } else {
                    // Compress camera or gallery photo
                    onProgress?.invoke(
                        fileIndex + 1,
                        uris.size,
                        "Compressing image ${fileIndex + 1} of ${uris.size}..."
                    )
                    val sampledBitmap = decodeSampledBitmapFromUri(context, uri, MAX_DIMENSION)
                    if (sampledBitmap != null) {
                        val outFile = File(stagingDir, "page_${String.format("%03d", nextPageIndex++)}.jpg")
                        FileOutputStream(outFile).use { out ->
                            sampledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                        }
                        sampledBitmap.recycle()
                        compressedFiles.add(outFile)
                    }
                }
            }

            val compressedTotalBytes = compressedFiles.sumOf { it.length() }
            Result.success(
                CompressionResult(
                    compressedFiles = compressedFiles,
                    originalTotalBytes = originalTotalBytes,
                    compressedTotalBytes = compressedTotalBytes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            // 1. First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // 2. Calculate inSampleSize
            var inSampleSize = 1
            val maxEdge = maxOf(options.outWidth, options.outHeight)
            while (maxEdge / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // 3. Decode bitmap with inSampleSize set
            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bmp: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { stream ->
                bmp = BitmapFactory.decodeStream(stream, null, decodeOpts)
            }

            // 4. If still larger than maxDimension, scale down smoothly
            val originalBmp = bmp ?: return null
            val currentMax = maxOf(originalBmp.width, originalBmp.height)
            if (currentMax > maxDimension) {
                val ratio = maxDimension.toFloat() / currentMax.toFloat()
                val newW = (originalBmp.width * ratio).toInt().coerceAtLeast(1)
                val newH = (originalBmp.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(originalBmp, newW, newH, true)
                if (scaled != originalBmp) {
                    originalBmp.recycle()
                }
                scaled
            } else {
                originalBmp
            }
        } catch (_: Exception) {
            null
        }
    }
}
