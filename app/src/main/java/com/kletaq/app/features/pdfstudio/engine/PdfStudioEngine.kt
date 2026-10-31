package com.kletaq.app.features.pdfstudio.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.kletaq.app.features.pdfstudio.data.CompressionLevel
import com.kletaq.app.features.pdfstudio.data.PageMarginOption
import com.kletaq.app.features.pdfstudio.data.PageOrientationOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

data class PdfCompressionResult(
    val outputFile: File,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val pageCount: Int
) {
    val savingsPercentage: Int
        get() = if (originalSizeBytes > 0) {
            (((originalSizeBytes - compressedSizeBytes).toDouble() / originalSizeBytes) * 100).toInt().coerceIn(0, 99)
        } else 0
}

object PdfStudioEngine {

    // Standard A4 dimensions in typographic points (72 points = 1 inch)
    private const val A4_WIDTH_PTS = 595
    private const val A4_HEIGHT_PTS = 842

    fun getOutputDirectory(context: Context): File {
        val dir = File(context.filesDir, "pdf_studio").apply {
            if (!exists()) mkdirs()
        }
        return dir
    }

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

    fun getFileName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment ?: "document.pdf"
        } catch (_: Exception) {
            "document.pdf"
        }
    }

    fun getPageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (_: Exception) {
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Renders a lightweight thumbnail of a specific page for UI grid previews.
     */
    fun renderPageThumbnail(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        maxDimension: Int = 360
    ): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (pageIndex !in 0 until renderer.pageCount) return null
            page = renderer.openPage(pageIndex)

            val origW = page.width
            val origH = page.height
            val scale = (maxDimension.toFloat() / max(origW, origH)).coerceAtMost(1.5f)
            val targetW = (origW * scale).toInt().coerceAtLeast(1)
            val targetH = (origH * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * 1. Images to PDF:
     * Converts a list of photo Uris to a clean A4 multi-page PDF document.
     */
    suspend fun convertImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        customFileName: String? = null,
        marginOption: PageMarginOption = PageMarginOption.STANDARD,
        orientationOption: PageOrientationOption = PageOrientationOption.AUTO,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No images selected."))
        }

        val pdfDocument = PdfDocument()
        val margin = marginOption.marginPoints

        try {
            for ((index, uri) in imageUris.withIndex()) {
                onProgress?.invoke(index + 1, imageUris.size)

                val bitmap = decodeSampledBitmapWithOrientation(context, uri, maxDimension = 1800)
                if (bitmap != null) {
                    val isLandscape = when (orientationOption) {
                        PageOrientationOption.LANDSCAPE -> true
                        PageOrientationOption.PORTRAIT -> false
                        PageOrientationOption.AUTO -> bitmap.width > bitmap.height
                    }

                    val pageWidth = if (isLandscape) A4_HEIGHT_PTS else A4_WIDTH_PTS
                    val pageHeight = if (isLandscape) A4_WIDTH_PTS else A4_HEIGHT_PTS

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    // Clean white background
                    canvas.drawColor(Color.WHITE)

                    val availableW = (pageWidth - (margin * 2)).toFloat().coerceAtLeast(1f)
                    val availableH = (pageHeight - (margin * 2)).toFloat().coerceAtLeast(1f)

                    val scale = min(availableW / bitmap.width.toFloat(), availableH / bitmap.height.toFloat())
                    val drawnW = bitmap.width.toFloat() * scale
                    val drawnH = bitmap.height.toFloat() * scale

                    val left = margin.toFloat() + (availableW - drawnW) / 2f
                    val top = margin.toFloat() + (availableH - drawnH) / 2f

                    val destRect = RectF(left, top, left + drawnW, top + drawnH)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(bitmap, null, destRect, paint)

                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }
            }

            val sanitizedName = (customFileName?.trim()?.takeIf { it.isNotBlank() } ?: "Document_${System.currentTimeMillis()}")
                .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
            val outFile = File(getOutputDirectory(context), sanitizedName)
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
    }

    /**
     * 2. Compress PDF:
     * Rasterizes pages, applies smart resolution scaling and JPEG compression,
     * and compiles back into a lightweight PDF.
     */
    suspend fun compressPdf(
        context: Context,
        pdfUri: Uri,
        compressionLevel: CompressionLevel,
        customFileName: String? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<PdfCompressionResult> = withContext(Dispatchers.IO) {
        val originalSize = getFileSize(context, pdfUri)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(IllegalStateException("Cannot open PDF file descriptor."))
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            if (pageCount == 0) {
                return@withContext Result.failure(IllegalStateException("The selected PDF document contains 0 pages."))
            }

            for (p in 0 until pageCount) {
                onProgress?.invoke(p + 1, pageCount)

                val page = renderer.openPage(p)
                val origW = page.width
                val origH = page.height

                val scale = (compressionLevel.maxDimension.toFloat() / max(origW, origH)).coerceAtMost(1.5f)
                val targetW = (origW * scale).toInt().coerceAtLeast(1)
                val targetH = (origH * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Compress page bitmap via JPEG compression stream
                val jpegBytes = ByteArrayOutputStream().use { byteStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, compressionLevel.jpegQuality, byteStream)
                    byteStream.toByteArray()
                }
                bitmap.recycle()

                val compressedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

                // Page dimensions in target PDF (keep original proportions)
                val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, p + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val docCanvas = docPage.canvas

                docCanvas.drawColor(Color.WHITE)
                val destRect = RectF(0f, 0f, origW.toFloat(), origH.toFloat())
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                docCanvas.drawBitmap(compressedBitmap, null, destRect, paint)

                pdfDocument.finishPage(docPage)
                compressedBitmap.recycle()
            }

            val sanitizedName = (customFileName?.trim()?.takeIf { it.isNotBlank() } ?: "Compressed_${System.currentTimeMillis()}.pdf")
                .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
            val outFile = File(getOutputDirectory(context), sanitizedName)
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Result.success(
                PdfCompressionResult(
                    outputFile = outFile,
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = outFile.length(),
                    pageCount = pageCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * 3. Merge PDFs:
     * Combines multiple source PDFs in the exact specified order into a single unified document.
     */
    suspend fun mergePdfs(
        context: Context,
        pdfUris: List<Uri>,
        customFileName: String? = null,
        onProgress: ((currentDoc: Int, totalDocs: Int, currentDocPages: String) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (pdfUris.size < 2) {
            return@withContext Result.failure(IllegalArgumentException("Please select at least 2 PDFs to merge."))
        }

        val pdfDocument = PdfDocument()
        var overallPageIndex = 1

        try {
            for ((docIndex, uri) in pdfUris.withIndex()) {
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null

                try {
                    pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        renderer = PdfRenderer(pfd)
                        val pageCount = renderer.pageCount
                        for (p in 0 until pageCount) {
                            onProgress?.invoke(docIndex + 1, pdfUris.size, "Page ${p + 1}/$pageCount")

                            val page = renderer.openPage(p)
                            val origW = page.width
                            val origH = page.height

                            val scale = (1440f / max(origW, origH)).coerceAtMost(2.0f)
                            val targetW = (origW * scale).toInt().coerceAtLeast(1)
                            val targetH = (origH * scale).toInt().coerceAtLeast(1)

                            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()

                            val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, overallPageIndex++).create()
                            val docPage = pdfDocument.startPage(pageInfo)
                            val docCanvas = docPage.canvas

                            val destRect = RectF(0f, 0f, origW.toFloat(), origH.toFloat())
                            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                            docCanvas.drawBitmap(bitmap, null, destRect, paint)

                            pdfDocument.finishPage(docPage)
                            bitmap.recycle()
                        }
                    }
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                }
            }

            val sanitizedName = (customFileName?.trim()?.takeIf { it.isNotBlank() } ?: "Merged_${System.currentTimeMillis()}.pdf")
                .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
            val outFile = File(getOutputDirectory(context), sanitizedName)
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
    }

    /**
     * 4. Split & Extract Pages:
     * Extracts only the selected page indices from a source PDF.
     */
    suspend fun splitAndExtractPdf(
        context: Context,
        pdfUri: Uri,
        selectedPageIndices: List<Int>, // 0-based indices
        customFileName: String? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (selectedPageIndices.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please select at least 1 page to extract."))
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(IllegalStateException("Cannot open PDF file descriptor."))
            renderer = PdfRenderer(pfd)
            val totalSourcePages = renderer.pageCount

            val validIndices = selectedPageIndices.filter { it in 0 until totalSourcePages }.sorted()
            if (validIndices.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No valid pages selected."))
            }

            for ((outIndex, p) in validIndices.withIndex()) {
                onProgress?.invoke(outIndex + 1, validIndices.size)

                val page = renderer.openPage(p)
                val origW = page.width
                val origH = page.height

                val scale = (1500f / max(origW, origH)).coerceAtMost(2.0f)
                val targetW = (origW * scale).toInt().coerceAtLeast(1)
                val targetH = (origH * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, outIndex + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val docCanvas = docPage.canvas

                val destRect = RectF(0f, 0f, origW.toFloat(), origH.toFloat())
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                docCanvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(docPage)
                bitmap.recycle()
            }

            val sanitizedName = (customFileName?.trim()?.takeIf { it.isNotBlank() } ?: "Extracted_${System.currentTimeMillis()}.pdf")
                .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
            val outFile = File(getOutputDirectory(context), sanitizedName)
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * 5. PDF to Images:
     * Renders each page into a separate high-resolution JPEG image file.
     */
    suspend fun convertPdfToImages(
        context: Context,
        pdfUri: Uri,
        folderName: String? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val generatedFiles = mutableListOf<File>()

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(IllegalStateException("Cannot open PDF file descriptor."))
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            val targetDir = File(getOutputDirectory(context), folderName ?: "images_${System.currentTimeMillis()}").apply {
                if (!exists()) mkdirs()
            }

            for (p in 0 until pageCount) {
                onProgress?.invoke(p + 1, pageCount)

                val page = renderer.openPage(p)
                val origW = page.width
                val origH = page.height

                val scale = (1600f / max(origW, origH)).coerceAtMost(2.0f)
                val targetW = (origW * scale).toInt().coerceAtLeast(1)
                val targetH = (origH * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val imgFile = File(targetDir, "Page_${String.format("%03d", p + 1)}.jpg")
                FileOutputStream(imgFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
                generatedFiles.add(imgFile)
            }

            Result.success(generatedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * 6. Dark Mode / Invert PDF:
     * Inverts RGB colors of each page, turning white background into dark mode for comfortable reading.
     */
    suspend fun invertPdfColors(
        context: Context,
        pdfUri: Uri,
        customFileName: String? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        val invertMatrix = ColorMatrix(floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        ))
        val filter = ColorMatrixColorFilter(invertMatrix)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = filter
        }

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(IllegalStateException("Cannot open PDF file descriptor."))
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            for (p in 0 until pageCount) {
                onProgress?.invoke(p + 1, pageCount)

                val page = renderer.openPage(p)
                val origW = page.width
                val origH = page.height

                val scale = (1440f / max(origW, origH)).coerceAtMost(2.0f)
                val targetW = (origW * scale).toInt().coerceAtLeast(1)
                val targetH = (origH * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, p + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val docCanvas = docPage.canvas

                val destRect = RectF(0f, 0f, origW.toFloat(), origH.toFloat())
                docCanvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(docPage)
                bitmap.recycle()
            }

            val sanitizedName = (customFileName?.trim()?.takeIf { it.isNotBlank() } ?: "Inverted_${System.currentTimeMillis()}.pdf")
                .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
            val outFile = File(getOutputDirectory(context), sanitizedName)
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Copies a file to the public Downloads folder using MediaStore (Android 10+)
     * or standard filesystem, ensuring the user can easily see it in their file manager.
     */
    fun saveToPublicDownloads(context: Context, file: File, mimeType: String = "application/pdf"): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Kletaq_PDF")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    uri
                } else null
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kletaq_PDF")
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, file.name)
                file.copyTo(targetFile, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates an Intent to view the PDF in any external PDF reader.
     */
    fun createViewIntent(context: Context, file: File, mimeType: String = "application/pdf"): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an Intent to share the PDF via WhatsApp, Telegram, Gmail, AirDrop, etc.
     */
    fun createShareIntent(context: Context, file: File, mimeType: String = "application/pdf"): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share ${file.name}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Decodes a bitmap from Uri with automatic downsampling and EXIF orientation rotation.
     */
    private fun decodeSampledBitmapWithOrientation(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        return try {
            // 1. Check EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            context.contentResolver.openInputStream(uri)?.use { input ->
                try {
                    val exif = ExifInterface(input)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } catch (_: Exception) {}
            }

            // 2. Decode bounds
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            }

            // 3. Compute sample size
            var inSampleSize = 1
            val maxEdge = max(boundsOpts.outWidth, boundsOpts.outHeight)
            while (maxEdge / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // 4. Decode bitmap
            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bmp: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { input ->
                bmp = BitmapFactory.decodeStream(input, null, decodeOpts)
            }
            val originalBmp = bmp ?: return null

            // 5. Handle rotation
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val rotated = if (!matrix.isIdentity) {
                val rotatedBmp = Bitmap.createBitmap(originalBmp, 0, 0, originalBmp.width, originalBmp.height, matrix, true)
                if (rotatedBmp != originalBmp) {
                    originalBmp.recycle()
                }
                rotatedBmp
            } else {
                originalBmp
            }

            // 6. Final scale check if still larger than maxDimension
            val curMax = max(rotated.width, rotated.height)
            if (curMax > maxDimension) {
                val ratio = maxDimension.toFloat() / curMax.toFloat()
                val targetW = (rotated.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (rotated.height * ratio).toInt().coerceAtLeast(1)
                val finalScaled = Bitmap.createScaledBitmap(rotated, targetW, targetH, true)
                if (finalScaled != rotated) {
                    rotated.recycle()
                }
                finalScaled
            } else {
                rotated
            }
        } catch (_: Exception) {
            null
        }
    }
}

