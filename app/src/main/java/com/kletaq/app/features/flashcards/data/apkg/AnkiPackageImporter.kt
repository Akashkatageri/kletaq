package com.kletaq.app.features.flashcards.data.apkg

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.kletaq.app.features.flashcards.data.FlashcardMediaManager
import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class AnkiImportResult(
    val deckName: String,
    val cards: List<Flashcard>,
    val mediaCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
)

object AnkiPackageImporter {

    private val IMG_REGEX = Regex("""<img[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)

    fun importApkg(
        context: Context,
        sourceUri: Uri,
        targetDeckId: String? = null
    ): AnkiImportResult {
        val tempDir = File(context.cacheDir, "apkg_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            // 1. Unzip .apkg into temp directory
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                zipStream.copyTo(fos)
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            } ?: return AnkiImportResult("", emptyList(), 0, false, "Could not open file input stream")

            // 2. Process media map
            val mediaMapFile = File(tempDir, "media")
            var importedMediaCount = 0
            val mediaDir = FlashcardMediaManager.getMediaDirectory(context)

            if (mediaMapFile.exists()) {
                try {
                    val mediaJsonStr = mediaMapFile.readText()
                    val mediaJson = JSONObject(mediaJsonStr)
                    val keys = mediaJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val originalFilename = mediaJson.getString(key)
                        val sourceMediaFile = File(tempDir, key)
                        if (sourceMediaFile.exists()) {
                            val destMediaFile = File(mediaDir, originalFilename)
                            sourceMediaFile.copyTo(destMediaFile, overwrite = true)
                            importedMediaCount++
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Open SQLite database (collection.anki21 preferred, or collection.anki2 with non-zero size)
            val dbFile = sequenceOf("collection.anki21", "collection.anki2")
                .map { File(tempDir, it) }
                .firstOrNull { it.exists() && it.length() > 0L }
                ?: return AnkiImportResult("", emptyList(), importedMediaCount, false, "No valid Anki database found in package")

            val db = try {
                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            } catch (e: Exception) {
                return AnkiImportResult("", emptyList(), importedMediaCount, false, "Failed to open Anki database: ${e.message}")
            }

            var extractedDeckName = "Imported Deck"
            try {
                val cursor = db.rawQuery("SELECT decks FROM col LIMIT 1", null)
                if (cursor.moveToFirst()) {
                    val decksJsonStr = cursor.getString(0)
                    val decksJson = JSONObject(decksJsonStr)
                    val keys = decksJson.keys()
                    while (keys.hasNext()) {
                        val dKey = keys.next()
                        val dObj = decksJson.getJSONObject(dKey)
                        val dName = dObj.optString("name")
                        if (dName.isNotBlank() && dName != "Default") {
                            extractedDeckName = dName
                            break
                        }
                    }
                }
                cursor.close()
            } catch (_: Exception) {
                // Fallback for newer Anki schemas with separate decks table
                try {
                    val deckCursor = db.rawQuery("SELECT name FROM decks WHERE name != 'Default' LIMIT 1", null)
                    if (deckCursor.moveToFirst()) {
                        val dName = deckCursor.getString(0)
                        if (!dName.isNullOrBlank()) extractedDeckName = dName
                    }
                    deckCursor.close()
                } catch (_: Exception) {}
            }

            val finalDeckId = targetDeckId ?: UUID.randomUUID().toString()
            val importedCards = mutableListOf<Flashcard>()

            // 4. Query notes
            val noteCursor = try {
                db.rawQuery("SELECT id, flds, tags FROM notes", null)
            } catch (e: Exception) {
                db.close()
                return AnkiImportResult("", emptyList(), importedMediaCount, false, "Failed querying Anki notes: ${e.message}")
            }
            val idIndex = noteCursor.getColumnIndex("id")
            val fldsIndex = noteCursor.getColumnIndex("flds")
            val tagsIndex = noteCursor.getColumnIndex("tags")

            while (noteCursor.moveToNext()) {
                val noteId = if (idIndex >= 0) noteCursor.getLong(idIndex).toString() else UUID.randomUUID().toString()
                val flds = if (fldsIndex >= 0) noteCursor.getString(fldsIndex).orEmpty() else ""
                val tagsStr = if (tagsIndex >= 0) noteCursor.getString(tagsIndex).orEmpty() else ""

                val fields = flds.split("\u001f")
                if (fields.isNotEmpty()) {
                    val rawFront = cleanHtml(fields[0])
                    val rawBack = if (fields.size > 1) cleanHtml(fields[1]) else ""

                    // Extract embedded image filenames
                    val frontImages = extractImageFilenames(fields[0])
                    val backImages = if (fields.size > 1) extractImageFilenames(fields[1]) else emptyList()

                    val tagsList = tagsStr.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    val isCloze = ClozeParser.hasValidCloze(rawFront)

                    val card = Flashcard(
                        id = UUID.randomUUID().toString(),
                        noteId = noteId,
                        deckId = finalDeckId,
                        deckName = extractedDeckName,
                        cardType = if (isCloze) FlashcardType.CLOZE else FlashcardType.BASIC,
                        front = rawFront,
                        back = rawBack,
                        clozeText = if (isCloze) rawFront else "",
                        frontImagePaths = frontImages,
                        backImagePaths = backImages,
                        tags = tagsList
                    )
                    importedCards.add(card)
                }
            }
            noteCursor.close()
            db.close()

            return AnkiImportResult(
                deckName = extractedDeckName,
                cards = importedCards,
                mediaCount = importedMediaCount,
                success = true
            )
        } catch (e: Exception) {
            return AnkiImportResult("", emptyList(), 0, false, "Import failed: ${e.message}")
        } finally {
            try {
                tempDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    private fun extractImageFilenames(html: String): List<String> {
        return IMG_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(IMG_REGEX, "")
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<div[^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</div>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<p[^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]*>"""), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}

