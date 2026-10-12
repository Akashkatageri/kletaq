package com.kletaq.app.core.timetable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object TimetableManager {

    private const val TIMETABLE_DIR = "timetable"
    private const val TIMETABLE_FILE_NAME = "timetable.jpg"
    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 82

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

    suspend fun saveTimetable(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Determine image bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return@withContext false

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext false

            // 2. Compute sample size to keep memory usage low
            var sampleSize = 1
            while (origWidth / sampleSize > MAX_DIMENSION * 1.5 || origHeight / sampleSize > MAX_DIMENSION * 1.5) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var decodedBitmap = contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return@withContext false

            // 3. Check EXIF orientation
            try {
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
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
                }
            } catch (_: Exception) {}

            // 4. Downscale if still larger than MAX_DIMENSION
            val finalWidth = decodedBitmap.width
            val finalHeight = decodedBitmap.height
            val scale = (MAX_DIMENSION.toFloat() / maxOf(finalWidth, finalHeight)).coerceAtMost(1f)

            val scaledBitmap = if (scale < 1f) {
                val newW = (finalWidth * scale).toInt()
                val newH = (finalHeight * scale).toInt()
                val resized = Bitmap.createScaledBitmap(decodedBitmap, newW, newH, true)
                if (resized != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                resized
            } else {
                decodedBitmap
            }

            // 5. Compress and write to filesDir/timetable/timetable.jpg
            val targetDir = File(context.filesDir, TIMETABLE_DIR)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, TIMETABLE_FILE_NAME)
            FileOutputStream(targetFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            scaledBitmap.recycle()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
