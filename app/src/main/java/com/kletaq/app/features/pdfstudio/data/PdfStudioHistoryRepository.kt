package com.kletaq.app.features.pdfstudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PdfStudioHistoryRepository {

    private const val PREFS_NAME = "pdf_studio_history_prefs"
    private const val KEY_RECENT_DOCS = "recent_pdf_documents"
    private const val MAX_RECENT_ITEMS = 30

    fun getRecentDocuments(context: Context): List<RecentPdfDocument> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECENT_DOCS, null) ?: return emptyList()

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<RecentPdfDocument>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val path = obj.getString("filePath")
                val file = File(path)
                // Only include if file still exists on disk
                if (file.exists()) {
                    list.add(
                        RecentPdfDocument(
                            id = obj.getString("id"),
                            fileName = obj.getString("fileName"),
                            filePath = path,
                            fileSizeBytes = obj.optLong("fileSizeBytes", file.length()),
                            pageCount = obj.optInt("pageCount", 1),
                            toolType = try {
                                PdfToolType.valueOf(obj.getString("toolType"))
                            } catch (_: Exception) {
                                PdfToolType.IMAGES_TO_PDF
                            },
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentDocument(context: Context, doc: RecentPdfDocument) {
        val current = getRecentDocuments(context).toMutableList()
        // Remove existing item with same path if present
        current.removeAll { it.filePath == doc.filePath }
        current.add(0, doc)

        val trimmed = if (current.size > MAX_RECENT_ITEMS) current.take(MAX_RECENT_ITEMS) else current

        val array = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("fileName", item.fileName)
                put("filePath", item.filePath)
                put("fileSizeBytes", item.fileSizeBytes)
                put("pageCount", item.pageCount)
                put("toolType", item.toolType.name)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT_DOCS, array.toString())
            .apply()
    }

    fun removeRecentDocument(context: Context, id: String) {
        val current = getRecentDocuments(context).filter { it.id != id }
        val array = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("fileName", item.fileName)
                put("filePath", item.filePath)
                put("fileSizeBytes", item.fileSizeBytes)
                put("pageCount", item.pageCount)
                put("toolType", item.toolType.name)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT_DOCS, array.toString())
            .apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RECENT_DOCS)
            .apply()
    }
}

