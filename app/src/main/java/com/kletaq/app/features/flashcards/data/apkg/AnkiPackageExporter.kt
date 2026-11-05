package com.kletaq.app.features.flashcards.data.apkg

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kletaq.app.features.flashcards.data.FlashcardMediaManager
import com.kletaq.app.features.flashcards.model.Flashcard
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object AnkiPackageExporter {

    fun exportToApkg(
        context: Context,
        deckName: String,
        cards: List<Flashcard>
    ): File? {
        val tempDir = File(context.cacheDir, "apkg_export_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val dbFile = File(tempDir, "collection.anki2")
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)

            // 1. Create Anki schema
            db.execSQL("""
                CREATE TABLE col (
                    id INTEGER PRIMARY KEY,
                    crt INTEGER,
                    mod INTEGER,
                    scm INTEGER,
                    ver INTEGER,
                    dty INTEGER,
                    usn INTEGER,
                    ls INTEGER,
                    conf TEXT,
                    models TEXT,
                    decks TEXT,
                    dconf TEXT,
                    tags TEXT
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE notes (
                    id INTEGER PRIMARY KEY,
                    guid TEXT,
                    mid INTEGER,
                    mod INTEGER,
                    usn INTEGER,
                    tags TEXT,
                    flds TEXT,
                    sfld TEXT,
                    csum INTEGER,
                    flags INTEGER,
                    data TEXT
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE cards (
                    id INTEGER PRIMARY KEY,
                    nid INTEGER,
                    did INTEGER,
                    ord INTEGER,
                    mod INTEGER,
                    usn INTEGER,
                    type INTEGER,
                    queue INTEGER,
                    due INTEGER,
                    ivl INTEGER,
                    factor INTEGER,
                    reps INTEGER,
                    lapses INTEGER,
                    left INTEGER,
                    odue INTEGER,
                    odid INTEGER,
                    flags INTEGER,
                    data TEXT
                )
            """.trimIndent())

            // 2. Populate col table with deck info
            val nowSeconds = System.currentTimeMillis() / 1000L
            val deckId = 1L
            val decksJson = JSONObject().apply {
                put("1", JSONObject().apply {
                    put("id", deckId)
                    put("name", deckName)
                })
            }

            db.execSQL(
                "INSERT INTO col (id, crt, mod, scm, ver, dty, usn, ls, conf, models, decks, dconf, tags) " +
                        "VALUES (1, ?, ?, ?, 11, 0, 0, 0, '{}', '{}', ?, '{}', '{}')",
                arrayOf<Any>(nowSeconds, nowSeconds, nowSeconds, decksJson.toString())
            )

            // 3. Populate notes & cards
            val mediaMap = JSONObject()
            var mediaCounter = 0
            val mediaDir = FlashcardMediaManager.getMediaDirectory(context)

            var noteIdSeq = 1000L
            for (card in cards) {
                noteIdSeq++
                var frontHtml = card.front
                var backHtml = card.back

                // Append images
                for (img in card.allFrontImages) {
                    frontHtml += "<br><img src=\"$img\">"
                    val sourceFile = File(mediaDir, img)
                    if (sourceFile.exists()) {
                        val mediaKey = mediaCounter.toString()
                        mediaMap.put(mediaKey, img)
                        sourceFile.copyTo(File(tempDir, mediaKey), overwrite = true)
                        mediaCounter++
                    }
                }

                for (img in card.allBackImages) {
                    backHtml += "<br><img src=\"$img\">"
                    val sourceFile = File(mediaDir, img)
                    if (sourceFile.exists()) {
                        val mediaKey = mediaCounter.toString()
                        mediaMap.put(mediaKey, img)
                        sourceFile.copyTo(File(tempDir, mediaKey), overwrite = true)
                        mediaCounter++
                    }
                }

                val flds = "$frontHtml\u001f$backHtml"
                val tagsStr = if (card.tags.isNotEmpty()) " " + card.tags.joinToString(" ") + " " else ""

                db.execSQL(
                    "INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data) " +
                            "VALUES (?, ?, 1, ?, 0, ?, ?, ?, 0, 0, '')",
                    arrayOf<Any>(noteIdSeq, card.id, nowSeconds, tagsStr, flds, frontHtml)
                )

                db.execSQL(
                    "INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data) " +
                            "VALUES (?, ?, 1, 0, ?, 0, 0, 0, ?, ?, ?, ?, ?, 0, 0, 0, 0, '')",
                    arrayOf<Any>(noteIdSeq + 100000L, noteIdSeq, nowSeconds, card.dueTimestamp / 1000L, card.intervalDays, (card.easeFactor * 1000).toInt(), card.reps, card.lapses)
                )
            }

            db.close()

            // 4. Write media map JSON
            File(tempDir, "media").writeText(mediaMap.toString())

            // 5. ZIP everything into .apkg
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanDeckName = deckName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val outputApkgFile = File(exportsDir, "${cleanDeckName}.apkg")

            ZipOutputStream(FileOutputStream(outputApkgFile)).use { zos ->
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        zos.putNextEntry(ZipEntry(file.name))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }

            return outputApkgFile
        } catch (_: Exception) {
            return null
        } finally {
            try {
                tempDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }
}

