package com.kletaq.app.features.memorypalace

import com.kletaq.app.features.memorypalace.ai.MemoryPalaceAiHelper
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryPalaceAiHelperTest {

    @Test
    fun testParseAiStepJson_cleanJson() {
        val jsonPayload = """
            [
              {
                "stepNumber": 1,
                "stepTitle": "State Assumptions",
                "formulaLatex": "P(X = x) = \\binom{n}{x} p^x q^{n-x}",
                "sensoryStory": "At the doorway, an enormous glowing Sigma pours light on the floor.",
                "examTrap": "Don't forget q = 1 - p."
              },
              {
                "stepNumber": 2,
                "stepTitle": "Substitute Lambda",
                "formulaLatex": "\\lambda = np",
                "sensoryStory": "On the sofa, an electric blue lambda is buzzing loudly.",
                "examTrap": "Write n -> infinity clearly."
              }
            ]
        """.trimIndent()

        val loci = listOf(
            PalaceLocus(id = "l1", imageId = "i1", palaceId = "p1", orderIndex = 1, name = "Doorway", xPercent = 0.1f, yPercent = 0.2f),
            PalaceLocus(id = "l2", imageId = "i1", palaceId = "p1", orderIndex = 2, name = "Sofa", xPercent = 0.4f, yPercent = 0.5f)
        )

        val steps = MemoryPalaceAiHelper.parseStepsResponse(jsonPayload, "deriv_123", loci)
        assertEquals(2, steps.size)
        assertEquals(1, steps[0].stepNumber)
        assertEquals(1, steps[0].locusOrderIndex)
        assertEquals("State Assumptions", steps[0].stepTitle)
        assertEquals("deriv_123", steps[0].derivationId)
        assertTrue(steps[0].sensoryStory.contains("doorway", ignoreCase = true))

        assertEquals(2, steps[1].stepNumber)
        assertEquals(2, steps[1].locusOrderIndex)
        assertEquals("Substitute Lambda", steps[1].stepTitle)
    }

    @Test
    fun testParseAiStepJson_wrappedInMarkdownFence() {
        val markdownPayload = """
            Here is the memory palace derivation:
            ```json
            [
              {
                "stepNumber": 1,
                "stepTitle": "Initialization",
                "formulaLatex": "d[s] = 0, \\forall v \\neq s: d[v] = \\infty",
                "sensoryStory": "At your desk lamp, distance zero shines like a laser.",
                "examTrap": "Initialize all other nodes to infinity."
              }
            ]
            ```
            Hope this helps you ace the exam!
        """.trimIndent()

        val loci = listOf(
            PalaceLocus(id = "l1", imageId = "i1", palaceId = "p1", orderIndex = 1, name = "Desk Lamp", xPercent = 0.3f, yPercent = 0.4f)
        )

        val steps = MemoryPalaceAiHelper.parseStepsResponse(markdownPayload, "deriv_algo", loci)
        assertEquals(1, steps.size)
        assertEquals("Initialization", steps[0].stepTitle)
        assertEquals("deriv_algo", steps[0].derivationId)
    }
}

