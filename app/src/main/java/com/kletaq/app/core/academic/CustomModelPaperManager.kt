package com.kletaq.app.core.academic

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 📄 CustomModelPaperManager
 *
 * Enables students from any university (VTU, autonomous, or private universities)
 * and custom subjects to attach, view, replace, and delete their own model question papers (PDF or images) offline.
 */
object CustomModelPaperManager {
    private const val TAG = "CustomModelPaper"
    private const val MODEL_PAPERS_DIR = "custom_model_papers"

    private fun normalizeSubject(subjectCode: String): String {
        return subjectCode.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")
    }

    private fun getPapersDir(context: Context): File {
        val dir = File(context.filesDir, MODEL_PAPERS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getModelPaperFile(context: Context, subjectCode: String): File? {
        val clean = normalizeSubject(subjectCode)
        val dir = getPapersDir(context)
        val pdf = File(dir, "$clean.pdf")
        if (pdf.exists() && pdf.length() > 0) return pdf
        val jpg = File(dir, "$clean.jpg")
        if (jpg.exists() && jpg.length() > 0) return jpg
        val png = File(dir, "$clean.png")
        if (png.exists() && png.length() > 0) return png
        return null
    }

    fun hasModelPaper(context: Context, subjectCode: String): Boolean {
        return getModelPaperFile(context, subjectCode) != null
    }

    suspend fun loadModelPaperBitmaps(context: Context, subjectCode: String): List<Bitmap> = withContext(Dispatchers.IO) {
        val file = getModelPaperFile(context, subjectCode) ?: return@withContext emptyList()
        val bitmaps = mutableListOf<Bitmap>()
        val isPdf = file.name.endsWith(".pdf", ignoreCase = true)

        if (isPdf) {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                for (i in 0 until count) {
                    val page = renderer.openPage(i)
                    val origW = page.width
                    val origH = page.height
                    // Render high resolution (up to 2048px dimension) for crystal clear formula & diagram reading
                    val scale = (2048f / maxOf(origW, origH)).coerceIn(1.0f, 2.5f)
                    val targetW = (origW * scale).toInt().coerceAtLeast(1)
                    val targetH = (origH * scale).toInt().coerceAtLeast(1)

                    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bmp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rendering PDF pages for $subjectCode", e)
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        } else {
            try {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    bitmaps.add(bmp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding image paper for $subjectCode", e)
            }
        }
        bitmaps
    }

    suspend fun saveModelPaper(context: Context, subjectCode: String, sourceUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val clean = normalizeSubject(subjectCode)
            val dir = getPapersDir(context)
            val mimeType = context.contentResolver.getType(sourceUri).orEmpty()
            val extension = when {
                mimeType.contains("pdf", ignoreCase = true) -> "pdf"
                mimeType.contains("png", ignoreCase = true) -> "png"
                else -> {
                    val path = sourceUri.toString().lowercase()
                    if (path.endsWith(".pdf")) "pdf"
                    else if (path.endsWith(".png")) "png"
                    else "jpg"
                }
            }

            // Remove previous versions
            File(dir, "$clean.pdf").delete()
            File(dir, "$clean.jpg").delete()
            File(dir, "$clean.png").delete()

            val targetFile = File(dir, "$clean.$extension")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not read file data"))

            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save model paper for $subjectCode", e)
            Result.failure(e)
        }
    }

    fun deleteModelPaper(context: Context, subjectCode: String): Boolean {
        val clean = normalizeSubject(subjectCode)
        val dir = getPapersDir(context)
        var deleted = false
        listOf("pdf", "jpg", "png").forEach { ext ->
            val f = File(dir, "$clean.$ext")
            if (f.exists() && f.delete()) {
                deleted = true
            }
        }
        return deleted
    }

    fun openModelPaper(context: Context, subjectCode: String) {
        val file = getModelPaperFile(context, subjectCode)
        if (file == null) {
            Toast.makeText(context, "No attached model paper found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = when {
                file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                else -> "image/*"
            }
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Grant read permission to potential receiver apps
            val resInfoList = context.packageManager.queryIntentActivities(viewIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val pkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(viewIntent, "Open Model Question Paper").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(TAG, "No app found to open file", e)
            Toast.makeText(context, "No application found to view this file. Please install a PDF or image viewer.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening model paper: ${e.message}", e)
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
