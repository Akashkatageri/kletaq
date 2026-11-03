package com.kletaq.app.core.ai

import com.kletaq.app.core.notes.NotesTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentIngestionRoutingTest {

    @Test
    fun testParseDocumentResultJson_cleanJson() {
        val jsonPayload = """
            {
              "title": "Calculus Notes",
              "pages": [
                {
                  "page_number": 1,
                  "extracted_text": "Definition of derivative:\nf'(x) = \\lim_{h \\to 0} \\frac{f(x+h) - f(x)}{h}",
                  "formulas": ["f'(x) = \\lim_{h \\to 0} \\frac{f(x+h) - f(x)}{h}"],
                  "handwritten_notes": "Important for SEE Exam"
                }
              ]
            }
        """.trimIndent()

        val result = UnifiedAiClient.parseDocumentResultJson(jsonPayload)
        assertNotNull(result)
        assertEquals("Calculus Notes", result?.title)
        assertEquals(1, result?.pages?.size)
        val p1 = result?.pages?.get(0)
        assertEquals(1, p1?.pageNumber)
        assertTrue(p1?.extractedText?.contains("\\lim") == true)
        assertEquals(1, p1?.formulas?.size)
        assertEquals("Important for SEE Exam", p1?.handwrittenNotes)
    }

    @Test
    fun testParseDocumentResultJson_wrappedInMarkdownBackticks() {
        val markdownWrapped = """
            Here is the parsed document:
            ```json
            {
              "title": "Signals and Systems",
              "pages": [
                {
                  "page_number": 1,
                  "extracted_text": "Fourier Transform: X(\\omega) = \\int_{-\\infty}^{\\infty} x(t) e^{-j\\omega t} dt",
                  "formulas": ["X(\\omega) = \\int_{-\\infty}^{\\infty} x(t) e^{-j\\omega t} dt"],
                  "handwritten_notes": ""
                }
              ]
            }
            ```
        """.trimIndent()

        val result = UnifiedAiClient.parseDocumentResultJson(markdownWrapped)
        assertNotNull(result)
        assertEquals("Signals and Systems", result?.title)
        assertEquals(1, result?.pages?.size)
        assertTrue(result?.rawCombinedText?.contains("Fourier Transform") == true)
    }

    @Test
    fun testParseDocumentResultJson_malformedJsonFallback() {
        val proseOnly = """
            This is plain text with no valid json object.
            Line 1: Maxwell's equations.
            Line 2: div D = rho.
        """.trimIndent()

        val result = UnifiedAiClient.parseDocumentResultJson(proseOnly)
        assertNotNull(result)
        // Fallback should preserve raw text rather than dropping content
        assertTrue(result?.rawCombinedText?.contains("Maxwell's equations") == true)
    }

    @Test
    fun testIsGarbledOrMathText_detection() {
        // 1. Clean English typed notes -> Should NOT be flagged as garbled math
        val cleanProse = """
            A computer network is a set of computers sharing resources located on or provided by network nodes.
            Computers use common communication protocols over digital interconnections to communicate with each other.
            These interconnections are made up of telecommunication network technologies based on physically wired,
            optical, and wireless radio frequency methods that may be arranged in a variety of network topologies.
        """.trimIndent()
        assertFalse(NotesTextExtractor.isGarbledOrMathText(cleanProse, 48))

        // 2. Fragmented OCR with gibberish symbols -> Flagged as garbled
        val garbledOcr = "|| / ~ _ # © § \n | .. x ] {"
        assertTrue(NotesTextExtractor.isGarbledOrMathText(garbledOcr, 4))

        // 3. Mathematical derivations / formulas with LaTeX or math symbols -> Flagged for AI vision routing
        val mathText = "Evaluate the integral: \\int_0^\\infty e^{-st} dt + \\sum_{i=1}^n \\frac{a_i}{b_i} = \\lambda"
        assertTrue(NotesTextExtractor.isGarbledOrMathText(mathText, 10))

        // 4. Very low word count (< 4 words) from handwritten diagram or empty scan -> Flagged
        val lowWordCount = "Fig 1.2"
        assertTrue(NotesTextExtractor.isGarbledOrMathText(lowWordCount, 2))
    }
}

