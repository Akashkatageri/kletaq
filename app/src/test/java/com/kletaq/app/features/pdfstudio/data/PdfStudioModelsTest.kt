package com.kletaq.app.features.pdfstudio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfStudioModelsTest {
    @Test
    fun `scan document tool type roundtrips cleanly by enum name`() {
        val serialized = PdfToolType.SCAN_DOCUMENT.name
        val deserialized = PdfToolType.valueOf(serialized)
        assertEquals(PdfToolType.SCAN_DOCUMENT, deserialized)
        assertTrue(deserialized.title.contains("Scan", ignoreCase = true))
        assertEquals(8, PdfToolType.values().size)
    }
}

