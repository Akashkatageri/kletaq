package com.kletaq.app.core.timetable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object TimetableManager {

    private const val TAG = "TimetableManager"
    private const val TIMETABLE_DIR = "timetable"
    private const val TIMETABLE_FILE_NAME = "timetable.jpg"
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 78
    private const val MAX_FILE_SIZE_BYTES = 400 * 1024 // 400 KB target cap

    fun getTimetableFile(context: Context): File? {
        val file = File(File(context.filesDir, TIMETABLE_DIR), TIMETABLE_FILE_NAME)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun hasTimetable(context: Context): Boolean {
        return getTimetableFile(context) != null
    }

    fun deleteTimetable(context: Context): Boolean {
        val file = File(File(context.filesDir, TIMETABLE_DIR), TIMETABLE_FILE_NAME)
        return if (file.exists()) {
            file.delete()
        } else false
    }

    private fun isPdfFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                read == 4 && header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() &&
                        header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun compressBitmapToFile(bitmap: Bitmap, targetFile: File) {
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        // If file is still larger than target cap (400 KB), re-compress at 65% quality to keep size ultra-light
        if (targetFile.length() > MAX_FILE_SIZE_BYTES) {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 65, out)
            }
        }
    }

    suspend fun saveTimetable(context: Context, sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_tt_input_${System.currentTimeMillis()}.tmp")
        try {
            // 1. Copy source stream to temporary local cache file in a single pass to avoid stream exhaustion
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open file from selected source"))

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return@withContext Result.failure(Exception("Selected file is empty"))
            }

            // 2. Detect if file is a PDF (by MIME type or magic %PDF bytes)
            val mimeType = try { context.contentResolver.getType(sourceUri) } catch (_: Exception) { null }
            val isPdf = mimeType?.equals("application/pdf", ignoreCase = true) == true || isPdfFile(tempFile)

            val targetDir = File(context.filesDir, TIMETABLE_DIR)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, TIMETABLE_FILE_NAME)

            if (isPdf) {
                // PDF Ingestion: Render first page into high-res Bitmap
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)
                    if (renderer.pageCount == 0) {
                        return@withContext Result.failure(Exception("PDF has no pages"))
                    }

                    val page = renderer.openPage(0)
                    val origW = page.width
                    val origH = page.height

                    val scale = (MAX_DIMENSION.toFloat() / maxOf(origW, origH)).coerceIn(1.0f, 2.5f)
                    val targetW = (origW * scale).toInt().coerceAtLeast(1)
                    val targetH = (origH * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE) // Solid white background for clear timetable viewing
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    compressBitmapToFile(bitmap, targetFile)
                    bitmap.recycle()
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                }
            } else {
                // Image Ingestion: Check bounds from local file
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(tempFile.absolutePath, boundsOptions)

                val origWidth = boundsOptions.outWidth
                val origHeight = boundsOptions.outHeight
                if (origWidth <= 0 || origHeight <= 0) {
                    return@withContext Result.failure(Exception("Could not decode image format"))
                }

                var sampleSize = 1
                while (origWidth / sampleSize > MAX_DIMENSION * 1.5 || origHeight / sampleSize > MAX_DIMENSION * 1.5) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                var decodedBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions)
                    ?: return@withContext Result.failure(Exception("Failed to decode image data"))

                // Check EXIF orientation from local file path
                try {
                    val exif = ExifInterface(tempFile.absolutePath)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        val rotated = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                        if (rotated != decodedBitmap) {
                            decodedBitmap.recycle()
                            decodedBitmap = rotated
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "EXIF orientation check skipped", e)
                }

                // Downscale if still larger than MAX_DIMENSION
                val finalWidth = decodedBitmap.width
                val finalHeight = decodedBitmap.height
                val scale = (MAX_DIMENSION.toFloat() / maxOf(finalWidth, finalHeight)).coerceAtMost(1f)

                val scaledBitmap = if (scale < 1f) {
                    val newW = (finalWidth * scale).toInt().coerceAtLeast(1)
                    val newH = (finalHeight * scale).toInt().coerceAtLeast(1)
                    val resized = Bitmap.createScaledBitmap(decodedBitmap, newW, newH, true)
                    if (resized != decodedBitmap) {
                        decodedBitmap.recycle()
                    }
                    resized
                } else {
                    decodedBitmap
                }

                compressBitmapToFile(scaledBitmap, targetFile)
                scaledBitmap.recycle()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveTimetable error: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
