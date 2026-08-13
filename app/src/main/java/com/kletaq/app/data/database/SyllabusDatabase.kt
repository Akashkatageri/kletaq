package com.kletaq.app.data.database

import com.kletaq.app.data.model.SyllabusChunk

/**
 * Official Syllabus Database for VTU Engineering Curricula.
 * Serves as the authoritative scope boundary to prevent AI hallucinations.
 */
object SyllabusDatabase {

    private val syllabusMap: Map<String, SyllabusChunk> = mapOf(
        // 📐 Mathematics II: Partial Differentiation
        "pd_01" to SyllabusChunk(
            subjectId = "BMATE201",
            topicId = "pd_01",
            topicTitle = "Partial Differentiation",
            allowedConcepts = listOf(
                "Partial derivatives",
                "Chain rule",
                "Jacobian matrix",
                "Euler's theorem for homogeneous functions",
                "Total derivatives",
                "Taylor's series for two variables"
            ),
            forbiddenConcepts = listOf(
                "Green's theorem",
                "Tensor calculus",
                "Vector optimization",
                "Stokes theorem",
                "Differential forms"
            )
        ),
        "dsa_recursion_memo" to SyllabusChunk(
            subjectId = "BPOPS103",
            topicId = "dsa_recursion_memo",
            topicTitle = "Recursion and Memoization",
            allowedConcepts = listOf(
                "Recursive call stack",
                "Base case vs recursive case",
                "Memoization table / lookup dictionary",
                "Time & space complexity of recursion",
                "Tree traversal via recursion",
                "Fibonacci & Factorial recursion"
            ),
            forbiddenConcepts = listOf(
                "Red-Black Tree rotations",
                "B-Tree disk IO optimization",
                "NP-complete reductions",
                "Graph max flow min cut"
            )
        ),
        "phys_wo" to SyllabusChunk(
            subjectId = "BPHYS102",
            topicId = "phys_wo",
            topicTitle = "Wave Optics",
            allowedConcepts = listOf(
                "Interference in thin films",
                "Wedge shaped film path difference",
                "Newton's rings setup & diameter calculation",
                "Diffraction at single slit",
                "Diffraction grating resolving power",
                "Optical path difference"
            ),
            forbiddenConcepts = listOf(
                "Quantum electrodynamics",
                "Nonlinear Kerr effect",
                "Cherenkov radiation",
                "General relativity gravitational lensing"
            )
        ),
        "chem_ec" to SyllabusChunk(
            subjectId = "BCHME102",
            topicId = "chem_ec",
            topicTitle = "Electrochemistry and Corrosion",
            allowedConcepts = listOf(
                "Nernst equation for single electrode potential",
                "Reference electrodes (Calomel & Ag/AgCl)",
                "Electrochemical series & EMF",
                "Corrosion types (Galvanic, Pitting, Stress)",
                "Pilling-Bedworth ratio",
                "Cathodic protection (Sacrificial anode)"
            ),
            forbiddenConcepts = listOf(
                "Nuclear magnetic resonance spectroscopy",
                "X-ray photoelectron spectroscopy",
                "Supercritical fluid chromatography"
            )
        )
    )

    fun getSyllabusChunk(topicId: String, topicTitle: String, subjectName: String): SyllabusChunk {
        return syllabusMap[topicId] ?: SyllabusChunk(
            subjectId = "VTU_GENERIC",
            topicId = topicId,
            topicTitle = topicTitle,
            allowedConcepts = listOf(
                "Core definitions & principles of $topicTitle",
                "Key governing equations for $topicTitle",
                "VTU standard exam patterns",
                "Common student mistakes & misconceptions",
                "Step-by-step problem solving methods"
            ),
            forbiddenConcepts = listOf(
                "Post-graduate research topics",
                "Out-of-syllabus advanced theorems",
                "Non-VTU curriculum extensions"
            )
        )
    }
}
