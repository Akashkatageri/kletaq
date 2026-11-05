package com.kletaq.app.features.flashcards.data.apkg

import android.content.Context
import android.net.Uri
import com.kletaq.app.features.flashcards.engine.ClozeParser
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

object DelimitedTextImporter {

    fun parseStream(
        reader: BufferedReader,
        targetDeckId: String = "default",
        targetDeckName: String = "Imported Deck"
    ): List<Flashcard> {
        val cards = mutableListOf<Flashcard>()
        var lineIndex = 0

        reader.forEachLine { rawLine ->
            lineIndex++
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) return@forEachLine

            // Determine delimiter: tab or comma
            val delimiter = if (line.contains("\t")) "\t" else ","
            val tokens = splitCsvLine(line, delimiter)

            if (tokens.isNotEmpty()) {
                val front = tokens[0].trim()
                val back = tokens.getOrNull(1)?.trim().orEmpty()
                val tags = tokens.getOrNull(2)?.split(";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

                // Check if line is a header
                if (lineIndex == 1 && front.equals("front", ignoreCase = true) && back.equals("back", ignoreCase = true)) {
                    return@forEachLine
                }

                if (front.isNotBlank()) {
                    val isCloze = ClozeParser.hasValidCloze(front)
                    val card = Flashcard(
                        id = UUID.randomUUID().toString(),
                        deckId = targetDeckId,
                        deckName = targetDeckName,
                        cardType = if (isCloze) FlashcardType.CLOZE else FlashcardType.BASIC,
                        front = front,
                        back = back,
                        clozeText = if (isCloze) front else "",
                        tags = tags
                    )
                    cards.add(card)
                }
            }
        }

        return cards
    }

    fun parseUri(
        context: Context,
        uri: Uri,
        targetDeckId: String = "default",
        targetDeckName: String = "Imported Deck"
    ): List<Flashcard> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    parseStream(reader, targetDeckId, targetDeckName)
                }
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        if (delimiter == "\t") {
            return line.split("\t")
        }

        // CSV parsing with quote support
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}

