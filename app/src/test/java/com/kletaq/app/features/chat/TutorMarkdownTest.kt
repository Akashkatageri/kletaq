package com.kletaq.app.features.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorMarkdownTest {

    @Test
    fun mathToUnicode_doesNotThrowPatternSyntaxException() {
        val input = "Consider \\frac{a + b}{c} and \\sqrt{x^2 + y^2} with \\partial f / \\partial x."
        val result = mathToUnicode(input)
        assertTrue(result.contains("∂"))
        assertTrue(result.contains("√"))
        assertTrue(result.contains("⁄"))
    }

    @Test
    fun mathToUnicode_handlesFractionsAndScripts() {
        val input = "\\frac{1}{2} + x^{2} + y_{1} = z^n"
        val result = mathToUnicode(input)
        assertTrue(result.contains("(1)⁄(2)"))
        assertTrue(result.contains("x²"))
        assertTrue(result.contains("y₁"))
    }
}
