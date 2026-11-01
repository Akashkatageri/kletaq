package com.kletaq.app.features.memorypalace

import com.kletaq.app.features.memorypalace.model.DerivationWithSteps
import com.kletaq.app.features.memorypalace.model.MemoryPalace
import com.kletaq.app.features.memorypalace.model.PalaceDerivation
import com.kletaq.app.features.memorypalace.model.PalaceImage
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep
import com.kletaq.app.features.memorypalace.model.PalaceWithImagesAndLoci
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PalaceEntitySerializationTest {

    @Test
    fun testPalaceAndLociModels() {
        val palace = MemoryPalace(
            id = "palace_1",
            name = "Hostel Room 204",
            description = "Main study room",
            totalLociCount = 2
        )
        val image = PalaceImage(
            id = "img_1",
            palaceId = "palace_1",
            imageOrder = 1,
            photoPath = "/data/user/0/com.kletaq.app/files/palace_images/img_1.jpg"
        )
        val locus1 = PalaceLocus(
            id = "loc_1",
            imageId = "img_1",
            palaceId = "palace_1",
            orderIndex = 1,
            name = "Doorway",
            xPercent = 0.15f,
            yPercent = 0.35f
        )
        val locus2 = PalaceLocus(
            id = "loc_2",
            imageId = "img_1",
            palaceId = "palace_1",
            orderIndex = 2,
            name = "Study Lamp",
            xPercent = 0.60f,
            yPercent = 0.40f
        )

        val composite = PalaceWithImagesAndLoci(
            palace = palace,
            images = listOf(image),
            loci = listOf(locus1, locus2)
        )

        assertEquals("palace_1", composite.palace.id)
        assertEquals(2, composite.loci.size)
        assertEquals("Doorway", composite.loci[0].name)
        assertEquals(0.15f, composite.loci[0].xPercent, 0.001f)
        assertEquals(0.35f, composite.loci[0].yPercent, 0.001f)
        assertTrue(composite.palace.createdAt > 0)
    }

    @Test
    fun testDerivationAndStepsModels() {
        val derivation = PalaceDerivation(
            id = "deriv_1",
            palaceId = "palace_1",
            title = "Poisson Distribution",
            mode = "EXAM_SPRINT",
            examTimestamp = 1793580000000L,
            nextReviewTime = 1793520000000L,
            reviewStage = 0
        )
        val step1 = PalaceStep(
            id = "step_1",
            derivationId = "deriv_1",
            stepNumber = 1,
            locusOrderIndex = 1,
            stepTitle = "State Assumptions",
            formulaLatex = "P(x) = \\frac{\\lambda^x e^{-\\lambda}}{x!}",
            sensoryStory = "At the door, a glowing lambda is dancing.",
            examTrap = "Don't forget x factorial!"
        )
        val step2 = PalaceStep(
            id = "step_2",
            derivationId = "deriv_1",
            stepNumber = 2,
            locusOrderIndex = 2,
            stepTitle = "Apply Limits",
            formulaLatex = "\\lim_{n \\to \\infty}",
            sensoryStory = "At the lamp, infinity is burning.",
            examTrap = "Split exponent carefully."
        )

        val composite = DerivationWithSteps(
            derivation = derivation,
            steps = listOf(step1, step2)
        )

        assertEquals("Poisson Distribution", composite.derivation.title)
        assertEquals("EXAM_SPRINT", composite.derivation.mode)
        assertEquals(2, composite.steps.size)
        assertEquals("State Assumptions", composite.steps[0].stepTitle)
        assertEquals("Don't forget x factorial!", composite.steps[0].examTrap)
        assertEquals(2, composite.steps[1].locusOrderIndex)
    }

    @Test
    fun testDerivationAndStepsJsonSerialization_withSourcePages() {
        val originalDeriv = PalaceDerivation(
            id = "deriv_doc_1",
            palaceId = "palace_1",
            title = "Navier-Stokes Derivation",
            mode = "EXAM_SPRINT",
            sourceDocumentPaths = listOf("/data/notes/page_1.jpg", "/data/notes/page_2.jpg")
        )

        val originalStep = PalaceStep(
            id = "step_page_1",
            derivationId = "deriv_doc_1",
            stepNumber = 3,
            locusOrderIndex = 2,
            stepTitle = "Momentum Equation",
            formulaLatex = "\\rho \\frac{Du}{Dt} = -\\nabla p + \\mu \\nabla^2 u",
            sensoryStory = "At the bookshelf, fluid flows in neon currents.",
            examTrap = "Watch the material derivative notation.",
            pageIndex = 1
        )

        val derivJson = originalDeriv.toJson()
        val restoredDeriv = PalaceDerivation.fromJson(derivJson)

        assertEquals("deriv_doc_1", restoredDeriv.id)
        assertEquals("Navier-Stokes Derivation", restoredDeriv.title)
        assertEquals(2, restoredDeriv.sourceDocumentPaths.size)
        assertEquals("/data/notes/page_1.jpg", restoredDeriv.sourceDocumentPaths[0])
        assertEquals("/data/notes/page_2.jpg", restoredDeriv.sourceDocumentPaths[1])

        val stepJson = originalStep.toJson()
        val restoredStep = PalaceStep.fromJson(stepJson)

        assertEquals("step_page_1", restoredStep.id)
        assertEquals("Momentum Equation", restoredStep.stepTitle)
        assertEquals(1, restoredStep.pageIndex)
    }
}
