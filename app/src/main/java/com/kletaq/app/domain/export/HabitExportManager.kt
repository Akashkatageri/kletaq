package com.kletaq.app.domain.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.kletaq.app.domain.model.HabitTaskItem
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object HabitExportManager {

    fun exportToCsv(context: Context, yearMonth: YearMonth, tasks: List<HabitTaskItem>) {
        try {
            val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val totalDays = yearMonth.lengthOfMonth()
            val sb = StringBuilder()

            // Header info
            sb.append("KLETAQ MONTHLY HABIT TRACKER - ").append(monthName.uppercase()).append(" ").append(yearMonth.year).append("\n\n")

            // Table headers
            sb.append("Task Name,Category,Completion Rate (%),Current Streak (Days),Longest Streak (Days)")
            for (day in 1..totalDays) {
                sb.append(",Day ").append(day)
            }
            sb.append("\n")

            // Rows
            tasks.forEach { item ->
                val titleEscaped = "\"" + item.title.replace("\"", "\"\"") + "\""
                val title = if (item.isDeleted) "~~$titleEscaped~~" else titleEscaped
                sb.append(title).append(",")
                    .append(item.categoryLabel).append(",")
                    .append(item.completionPercentage).append("% ,")
                    .append(item.currentStreak).append(",")
                    .append(item.longestStreak)

                val monthPrefix = String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)
                for (day in 1..totalDays) {
                    val dateIso = String.format("%s-%02d", monthPrefix, day)
                    if (item.completedDates.contains(dateIso)) {
                        sb.append(",✓")
                    } else {
                        sb.append(",-")
                    }
                }
                sb.append("\n")
            }

            val fileName = "HabitTracker_${yearMonth.year}_${yearMonth.monthValue}.csv"
            saveAndOpen(context, sb.toString().toByteArray(Charsets.UTF_8), fileName, "text/csv", "CSV Spreadsheet")

        } catch (e: Exception) {
            Log.e("HABIT_EXPORT", "Failed to export CSV", e)
        }
    }

    fun exportToPdf(context: Context, yearMonth: YearMonth, tasks: List<HabitTaskItem>) {
        try {
            val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val totalDays = yearMonth.lengthOfMonth()

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(1200, 850, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
            }

            // Background
            canvas.drawColor(Color.rgb(250, 250, 252))

            // Title Header
            paint.color = Color.rgb(40, 40, 50)
            paint.textSize = 28f
            paint.isFakeBoldText = true
            canvas.drawText("KLETAQ  |  MONTHLY HABIT TRACKER", 50f, 60f, paint)

            paint.color = Color.rgb(100, 100, 120)
            paint.textSize = 20f
            paint.isFakeBoldText = false
            canvas.drawText("${monthName.uppercase()} ${yearMonth.year}", 950f, 60f, paint)

            // Line separator
            paint.color = Color.rgb(200, 200, 210)
            paint.strokeWidth = 2f
            canvas.drawLine(50f, 80f, 1150f, 80f, paint)

            // Stats summary card
            paint.color = Color.rgb(238, 242, 255)
            canvas.drawRoundRect(50f, 95f, 1150f, 155f, 12f, 12f, paint)

            paint.color = Color.rgb(79, 70, 229)
            paint.textSize = 14f
            paint.isFakeBoldText = true
            val totalCompletions = tasks.sumOf { it.completedDaysCount }
            val avgPercentage = if (tasks.isNotEmpty()) tasks.map { it.completionPercentage }.average().toInt() else 0
            val topStreak = tasks.maxOfOrNull { it.currentStreak } ?: 0

            canvas.drawText("TOTAL HABITS: ${tasks.size}", 70f, 130f, paint)
            canvas.drawText("TOTAL CHECKMARKS: $totalCompletions", 320f, 130f, paint)
            canvas.drawText("AVG COMPLETION: $avgPercentage%", 650f, 130f, paint)
            canvas.drawText("TOP STREAK: 🔥 $topStreak DAYS", 920f, 130f, paint)

            // Grid Headers
            val startY = 200f
            val rowHeight = 32f
            val nameColWidth = 320f
            val dayColWidth = (1150f - 50f - nameColWidth) / totalDays

            // Table Header Background
            paint.color = Color.rgb(225, 231, 245)
            canvas.drawRect(50f, startY, 1150f, startY + rowHeight, paint)

            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("HABIT / TASK", 60f, startY + 20f, paint)

            for (day in 1..totalDays) {
                val xPos = 50f + nameColWidth + (day - 1) * dayColWidth
                canvas.drawText("$day", xPos + (dayColWidth / 4), startY + 20f, paint)
            }

            // Task Rows
            var currentY = startY + rowHeight
            paint.isFakeBoldText = false

            tasks.forEachIndexed { index, item ->
                if (currentY + rowHeight > 800f) return@forEachIndexed // page bound check

                // Alternate row fill
                if (index % 2 == 1) {
                    paint.color = Color.rgb(243, 244, 246)
                    canvas.drawRect(50f, currentY, 1150f, currentY + rowHeight, paint)
                }

                // Grid border lines
                paint.color = Color.rgb(226, 232, 240)
                paint.strokeWidth = 1f
                canvas.drawLine(50f, currentY + rowHeight, 1150f, currentY + rowHeight, paint)

                // Task Title
                paint.color = if (item.isDeleted) Color.rgb(150, 150, 150) else Color.rgb(15, 23, 42)
                paint.textSize = 11f
                val displayTitle = if (item.isDeleted) "[DELETED] ${item.title}" else "${item.categoryEmoji} ${item.title}"
                val truncatedTitle = if (displayTitle.length > 36) displayTitle.take(34) + ".." else displayTitle
                canvas.drawText(truncatedTitle, 60f, currentY + 20f, paint)

                // Days checkmarks
                val monthPrefix = String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)
                for (day in 1..totalDays) {
                    val dateIso = String.format("%s-%02d", monthPrefix, day)
                    val xPos = 50f + nameColWidth + (day - 1) * dayColWidth

                    if (item.completedDates.contains(dateIso)) {
                        paint.color = Color.rgb(16, 185, 129)
                        paint.textSize = 13f
                        paint.isFakeBoldText = true
                        canvas.drawText("✓", xPos + (dayColWidth / 4), currentY + 20f, paint)
                        paint.isFakeBoldText = false
                    } else {
                        paint.color = Color.rgb(203, 213, 225)
                        paint.textSize = 10f
                        canvas.drawText("•", xPos + (dayColWidth / 3), currentY + 18f, paint)
                    }
                }

                currentY += rowHeight
            }

            // Footer
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 10f
            canvas.drawText("Generated by Kletaq Academic & Habit Tracking OS  •  Page 1 of 1", 50f, 830f, paint)

            pdfDocument.finishPage(page)

            val stream = java.io.ByteArrayOutputStream()
            pdfDocument.writeTo(stream)
            pdfDocument.close()

            val fileName = "HabitTracker_${yearMonth.year}_${yearMonth.monthValue}.pdf"
            saveAndOpen(context, stream.toByteArray(), fileName, "application/pdf", "PDF Document")

        } catch (e: Exception) {
            Log.e("HABIT_EXPORT", "Failed to export PDF", e)
        }
    }

    private fun saveAndOpen(context: Context, fileData: ByteArray, fileName: String, mimeType: String, description: String) {
        try {
            var fileUri: Uri? = null

            // 1. Save to public Downloads directory via MediaStore (Android 10+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(fileData)
                    }
                    fileUri = uri
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    out.write(fileData)
                }
                fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
            }

            // Also keep a copy in app cache for file viewing fallback
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val cacheFile = File(cacheDir, fileName)
            FileOutputStream(cacheFile).use { out ->
                out.write(fileData)
            }

            if (fileUri == null) {
                fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            }

            // Toast feedback to confirm download completion to public Downloads
            android.widget.Toast.makeText(context, "✅ Downloaded to Downloads/$fileName", android.widget.Toast.LENGTH_LONG).show()

            // Open the downloaded file using ACTION_VIEW chooser
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "Open $description").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("HABIT_EXPORT", "Failed to save or open file: $fileName", e)
            android.widget.Toast.makeText(context, "Export error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
