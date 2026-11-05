package com.kletaq.app.features.lab

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * Pure-Kotlin, offline-first deterministic PDF text extractor.
 * Extracts text streams from PDF objects (FlateDecode) without external dependencies,
 * Google Play services, or cloud API tokens.
 */
object PdfStreamTextExtractor {

    fun extractText(pdfBytes: ByteArray): String {
        if (pdfBytes.isEmpty()) return ""
        val sb = StringBuilder()
        val text = String(pdfBytes, Charsets.ISO_8859_1)
        val streamRegex = Regex("""stream[\r\n]+([\s\S]*?)[\r\n]+endstream""")
        val matches = streamRegex.findAll(text)

        for (m in matches) {
            val rawStream = m.groupValues[1].toByteArray(Charsets.ISO_8859_1)
            val decompressed = try {
                val inflater = Inflater()
                inflater.setInput(rawStream)
                val outputStream = ByteArrayOutputStream(rawStream.size * 2)
                val buffer = ByteArray(4096)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count <= 0 && inflater.needsInput()) break
                    outputStream.write(buffer, 0, count)
                }
                inflater.end()
                outputStream.toString(Charsets.UTF_8.name())
            } catch (_: Exception) {
                ""
            }

            if (decompressed.isNotBlank()) {
                // Extract text from BT (Begin Text) ... ET (End Text) blocks
                val btRegex = Regex("""BT([\s\S]*?)ET""")
                for (btMatch in btRegex.findAll(decompressed)) {
                    val block = btMatch.groupValues[1]

                    // Tj matches: (string) Tj
                    val tjRegex = Regex("""\(((?:\\\(|\\\)|[^()])*)\)\s*Tj""")
                    for (tj in tjRegex.findAll(block)) {
                        val str = unescapePdfString(tj.groupValues[1])
                        sb.append(str).append(" ")
                    }

                    // TJ matches: [ (str) num (str) ] TJ
                    val tjArrayRegex = Regex("""\[([\s\S]*?)\]\s*TJ""")
                    for (tja in tjArrayRegex.findAll(block)) {
                        val inner = tja.groupValues[1]
                        val parts = Regex("""\(((?:\\\(|\\\)|[^()])*)\)""").findAll(inner)
                        for (p in parts) {
                            sb.append(unescapePdfString(p.groupValues[1]))
                        }
                        sb.append(" ")
                    }

                    // T* or newline operators
                    if (block.contains("T*") || block.contains("TD") || block.contains("Td") || block.contains("'")) {
                        sb.append("\n")
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun unescapePdfString(s: String): String {
        return s.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}

