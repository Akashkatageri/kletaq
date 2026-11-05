package com.kletaq.app.features.flashcards.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

enum class DeckShareFormat {
    ANKI_APKG,  // Standard Anki package (.apkg) with media bundled
    ANKI_TSV,   // Anki Desktop / AnkiDroid compatible TSV (.txt)
    KLETAQ_JSON // Full Kletaq deck package (.json)
}

object FlashcardShareManager {
    private const val TAG = "FlashcardShareManager"

    suspend fun shareDeck(
        context: Context,
        deckTitle: String,
        cards: List<Flashcard>,
        format: DeckShareFormat
    ) = withContext(Dispatchers.IO) {
        if (cards.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No cards to share in this deck!", Toast.LENGTH_SHORT).show()
            }
            return@withContext
        }

        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedTitle = deckTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "deck" }
            val (file, mimeType) = when (format) {
                DeckShareFormat.ANKI_APKG -> {
                    val apkgFile = com.kletaq.app.features.flashcards.data.apkg.AnkiPackageExporter.exportToApkg(context, deckTitle, cards)
                        ?: throw IllegalStateException("Failed to generate .apkg package")
                    Pair(apkgFile, "application/apkg")
                }
                DeckShareFormat.ANKI_TSV -> {
                    val exportFile = File(exportDir, "${sanitizedTitle}_anki.txt")
                    val content = buildAnkiTsvContent(cards)
                    exportFile.writeText(content)
                    Pair(exportFile, "text/tab-separated-values")
                }
                DeckShareFormat.KLETAQ_JSON -> {
                    val exportFile = File(exportDir, "${sanitizedTitle}_kletaq.json")
                    val array = JSONArray()
                    for (c in cards) {
                        array.put(serializeCardJson(c))
                    }
                    exportFile.writeText(array.toString(2))
                    Pair(exportFile, "application/json")
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$deckTitle Flashcard Deck")
                putExtra(Intent.EXTRA_TEXT, "Here is the '$deckTitle' flashcard deck (${cards.size} cards) from Kletaq!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Deck via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share deck", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to export deck: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildAnkiTsvContent(cards: List<Flashcard>): String {
        val sb = StringBuilder()
        sb.append("#separator:tab\n")
        sb.append("#html:true\n")
        sb.append("#tags column:3\n")

        for (c in cards) {
            val tags = c.tags.joinToString(" ")
            if (c.cardType == FlashcardType.CLOZE) {
                var clozeText = c.clozeText.replace("\n", "<br>").replace("\t", " ")
                for (img in c.allFrontImages) {
                    val filename = File(img).name
                    clozeText += "<br><img src=\"$filename\">"
                }
                if (!c.audioPath.isNullOrBlank()) {
                    val audioName = File(c.audioPath).name
                    clozeText += "<br>[sound:$audioName]"
                }
                val hint = c.hint.replace("\n", "<br>").replace("\t", " ")
                sb.append("$clozeText\t$hint\t$tags\n")
            } else {
                var front = c.front.replace("\n", "<br>").replace("\t", " ")
                for (img in c.allFrontImages) {
                    val filename = File(img).name
                    front += "<br><img src=\"$filename\">"
                }
                if (!c.audioPath.isNullOrBlank()) {
                    val audioName = File(c.audioPath).name
                    front += "<br>[sound:$audioName]"
                }
                var back = c.back.replace("\n", "<br>").replace("\t", " ")
                for (img in c.allBackImages) {
                    val filename = File(img).name
                    back += "<br><img src=\"$filename\">"
                }
                sb.append("$front\t$back\t$tags\n")
            }
        }
        return sb.toString()
    }

    private fun serializeCardJson(c: Flashcard): JSONObject {
        return JSONObject().apply {
            put("id", c.id)
            put("deckId", c.deckId)
            put("deckName", c.deckName)
            put("topicId", c.topicId)
            put("topicTitle", c.topicTitle)
            put("subjectId", c.subjectId)
            put("subjectName", c.subjectName)
            put("cardType", c.cardType.name)
            put("front", c.front)
            put("back", c.back)
            put("clozeText", c.clozeText)
            put("hint", c.hint)
            put("frontImagePath", c.frontImagePath ?: "")
            put("backImagePath", c.backImagePath ?: "")
            put("frontImagePaths", JSONArray(c.frontImagePaths))
            put("backImagePaths", JSONArray(c.backImagePaths))
            put("audioPath", c.audioPath ?: "")
            put("typedAnswer", c.typedAnswer)
            put("tags", JSONArray(c.tags))
            put("createdAt", c.createdAt)
        }
    }
}

