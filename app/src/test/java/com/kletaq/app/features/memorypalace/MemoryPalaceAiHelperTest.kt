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

    @Test
    fun testFallbackParseSteps_unstructuredOcrTextWithoutEqualsOrStepKeyword() {
        val rawOcrText = """
            Module 3: Maxwell Equations and Electromagnetic Fields
            1. Gauss Law for Electricity describes total electric flux through a closed surface
            2. Gauss Law for Magnetism establishes absence of magnetic monopoles
            3. Faraday Law of Induction shows time-varying magnetic field induces electric field
            4. Ampere Maxwell Law demonstrates magnetic fields produced by electric currents and displacement
            Conclusion and boundary condition limits
        """.trimIndent()

        val loci = listOf(
            PalaceLocus(id = "l1", imageId = "i1", palaceId = "p1", orderIndex = 1, name = "Door", xPercent = 0.1f, yPercent = 0.2f),
            PalaceLocus(id = "l2", imageId = "i1", palaceId = "p1", orderIndex = 2, name = "Bed", xPercent = 0.3f, yPercent = 0.5f),
            PalaceLocus(id = "l3", imageId = "i1", palaceId = "p1", orderIndex = 3, name = "Desk", xPercent = 0.6f, yPercent = 0.7f),
            PalaceLocus(id = "l4", imageId = "i1", palaceId = "p1", orderIndex = 4, name = "Window", xPercent = 0.8f, yPercent = 0.9f)
        )

        val steps = MemoryPalaceAiHelper.fallbackParseSteps(rawOcrText, "deriv_maxwell", loci)
        assertEquals(4, steps.size)
        assertEquals(1, steps[0].locusOrderIndex)
        assertEquals(4, steps[3].locusOrderIndex)
        assertTrue(steps[0].formulaLatex.isNotBlank())
        assertTrue(steps[0].sensoryStory.contains("Door", ignoreCase = true))
        assertTrue(steps[1].sensoryStory.contains("Bed", ignoreCase = true))
    }
}

