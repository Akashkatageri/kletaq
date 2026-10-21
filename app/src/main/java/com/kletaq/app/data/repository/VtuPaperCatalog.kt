package com.kletaq.app.data.repository

import com.kletaq.app.data.model.VtuPaperInfo

/**
 * 🏛️ Official VTU Model Question Paper Catalog
 *
 * Provides a Semantic Dual-Key Resolver mapping both Course Codes and Subject Titles
 * to verified, official university PDF endpoints hosted at https://vtu.ac.in/pdf/QP/
 *
 * Bridges the gap between:
 * - Autonomous college schemes (e.g. 1BCS302 = Java, 1BCS303 = DDCO, 1BCS304 = OS)
 * - Standard VTU Affiliated 2022/2025 schemes (e.g. BCS302 = DDCO, BCS303 = OS, BCS306A = Java)
 */
object VtuPaperCatalog {

    private const val VTU_BASE_URL = "https://vtu.ac.in/pdf/QP"

    fun resolvePaper(
        subjectCode: String,
        subjectName: String,
        semester: Int = 1
    ): VtuPaperInfo {
        val sName = subjectName.lowercase().trim()
        val sCode = subjectCode.uppercase().trim().replace("(", "").replace(")", "")

        return when {
            // ==========================================
            // --- SEMESTER 1 (Physics & Chem Cycles) ---
            // ==========================================
            sName.contains("calculus") || sCode.contains("MATS101") || sCode.contains("MATC101") || sCode == "22MATS11" || sCode == "22MATC11" ->
                VtuPaperInfo(
                    vtuPaperCode = "1BMATC101",
                    officialPdfUrl = "$VTU_BASE_URL/1BMATC101.pdf",
                    subjectTitle = "Calculus and Linear Algebra",
                    semester = 1,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("chemistry") || sCode.contains("CHEC102") || sCode.contains("CHEM102") || sCode == "22CHEM12" || sCode == "22CHEC12" ->
                VtuPaperInfo(
                    vtuPaperCode = "1BCHEC102",
                    officialPdfUrl = "$VTU_BASE_URL/1BCHEC102.pdf",
                    subjectTitle = "Applied Chemistry for CSE Stream",
                    semester = 1,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("python") || sCode.contains("PLC105") || sCode.contains("POP13") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BESC104B",
                    officialPdfUrl = "$VTU_BASE_URL/1BESC104B.pdf",
                    subjectTitle = "Python Programming",
                    semester = 1,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("electronics") || sCode.contains("ECE105") || sCode.contains("ETC15") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BECE105",
                    officialPdfUrl = "$VTU_BASE_URL/1BECE105.pdf",
                    subjectTitle = "Basic Electronics and Communication",
                    semester = 1,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("electrical") || sCode.contains("BEE105") || sCode.contains("ESC142") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BBEE105",
                    officialPdfUrl = "$VTU_BASE_URL/1BBEE105.pdf",
                    subjectTitle = "Basic Electrical Engineering",
                    semester = 1,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("artificial intelligence") || sCode.contains("AIA103") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BAIA103",
                    officialPdfUrl = "$VTU_BASE_URL/1BCEDx103.pdf", // Baseline fallback link
                    subjectTitle = "Introduction to AI and Applications",
                    semester = 1,
                    scheme = "2025 Scheme"
                )

            sName.contains("english") || sCode.contains("ENG106") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BENG106",
                    officialPdfUrl = "$VTU_BASE_URL/1BENG106.pdf",
                    subjectTitle = "Professional Writing Skills in English",
                    semester = 1,
                    scheme = "2025 Scheme",
                    maxMarks = 50,
                    durationMinutes = 120
                )

            sName.contains("constitution") || sCode.contains("ICO107") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BICO107",
                    officialPdfUrl = "$VTU_BASE_URL/1BICO107.pdf",
                    subjectTitle = "Indian Constitution",
                    semester = 1,
                    scheme = "2025 Scheme",
                    maxMarks = 50,
                    durationMinutes = 120
                )

            // ==========================================
            // --- SEMESTER 2 (Physics & Chem Cycles) ---
            // ==========================================
            sName.contains("numerical") || sCode.contains("MATS201") || sCode.contains("MATC201") || sCode == "22MATS21" ->
                VtuPaperInfo(
                    vtuPaperCode = "1BMATC201",
                    officialPdfUrl = "$VTU_BASE_URL/1BMATC201.pdf",
                    subjectTitle = "Numerical Methods: CSE Stream",
                    semester = 2,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("physics") || sCode.contains("PHYS102") || sCode.contains("PHEC102") || sCode.contains("PHEC202") || sCode == "22PHYS12" ->
                VtuPaperInfo(
                    vtuPaperCode = "1BPHYS102",
                    officialPdfUrl = "$VTU_BASE_URL/1BPHYS102.pdf",
                    subjectTitle = "Quantum Physics and Applications",
                    semester = 2,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("drawing") || sName.contains("caed") || sCode.contains("CED") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BCEDx103",
                    officialPdfUrl = "$VTU_BASE_URL/1BCEDx103.pdf",
                    subjectTitle = "Computer-Aided Engineering Drawing",
                    semester = 2,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("programming in c") || sName.contains("c programming") || sCode.contains("EIT205") || sCode.contains("POPL107") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BEIT105",
                    officialPdfUrl = "$VTU_BASE_URL/1BEIT105.pdf",
                    subjectTitle = "Programming in C",
                    semester = 2,
                    scheme = "2025/2022 Scheme"
                )

            sName.contains("mechanical") || sCode.contains("ESC204") ->
                VtuPaperInfo(
                    vtuPaperCode = "1BESC104B",
                    officialPdfUrl = "$VTU_BASE_URL/1BESC104B.pdf",
                    subjectTitle = "Introduction to Mechanical Engineering",
                    semester = 2,
                    scheme = "2025/2022 Scheme"
                )

            // ==========================================
            // --- SEMESTER 3 (Core Engineering) -------
            // ==========================================
            sName.contains("probability") || sName.contains("statistics") || sCode.contains("1BCS301") || sCode == "BCS301" || sCode.contains("BMAT301") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS301",
                    officialPdfUrl = "$VTU_BASE_URL/BCS301.pdf",
                    subjectTitle = "Mathematics for Computer Science (Probability & Stats)",
                    semester = 3,
                    scheme = "2022 Scheme"
                )

            sName.contains("java") || sCode.contains("1BCS302") || sCode.contains("BCS306A") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS306A",
                    officialPdfUrl = "$VTU_BASE_URL/BCS306A.pdf",
                    subjectTitle = "Object Oriented Programming with Java",
                    semester = 3,
                    scheme = "2022 Scheme"
                )

            sName.contains("digital design") || sName.contains("computer organization") || sCode.contains("1BCS303") || sCode == "BCS302" || sCode.contains("COA303") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS302",
                    officialPdfUrl = "$VTU_BASE_URL/BCS302.pdf",
                    subjectTitle = "Digital Design and Computer Organization (DDCO)",
                    semester = 3,
                    scheme = "2022 Scheme"
                )

            sName.contains("operating system") || sCode.contains("1BCS304") || sCode == "BCS303" || sCode.contains("BOS304") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS303",
                    officialPdfUrl = "$VTU_BASE_URL/BCS303.pdf",
                    subjectTitle = "Operating Systems",
                    semester = 3,
                    scheme = "2022 Scheme"
                )

            sName.contains("data structure") || sCode.contains("1BCS305") || sCode.contains("1BCSL306") || sCode == "BCS304" ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS304",
                    officialPdfUrl = "$VTU_BASE_URL/BCS304.pdf",
                    subjectTitle = "Data Structures and Applications",
                    semester = 3,
                    scheme = "2022 Scheme"
                )

            // ==========================================
            // --- SEMESTER 4 (Core Engineering) -------
            // ==========================================
            sName.contains("algorithms") || sName.contains("ada") || sCode.contains("BCS401") || sCode.contains("1BCS404") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS401",
                    officialPdfUrl = "$VTU_BASE_URL/BCS401.pdf",
                    subjectTitle = "Analysis and Design of Algorithms",
                    semester = 4,
                    scheme = "2022 Scheme"
                )

            sName.contains("microcontroller") || sName.contains("embedded") || sCode.contains("BCS402") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS402",
                    officialPdfUrl = "$VTU_BASE_URL/BCS402.pdf",
                    subjectTitle = "Microcontrollers and Embedded Systems",
                    semester = 4,
                    scheme = "2022 Scheme"
                )

            sName.contains("database") || sName.contains("dbms") || sCode.contains("BCS403") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS403",
                    officialPdfUrl = "$VTU_BASE_URL/BCS403.pdf",
                    subjectTitle = "Database Management Systems",
                    semester = 4,
                    scheme = "2022 Scheme"
                )

            sName.contains("discrete") || sCode.contains("BCS404") || sCode.contains("1BCS401") ->
                VtuPaperInfo(
                    vtuPaperCode = "BCS404",
                    officialPdfUrl = "$VTU_BASE_URL/BCS404.pdf",
                    subjectTitle = "Discrete Mathematical Structures",
                    semester = 4,
                    scheme = "2022 Scheme"
                )

            sName.contains("biology") || sCode.contains("BBIO406") ->
                VtuPaperInfo(
                    vtuPaperCode = "BBIO406",
                    officialPdfUrl = "$VTU_BASE_URL/BBIO406.pdf",
                    subjectTitle = "Biology for Engineers",
                    semester = 4,
                    scheme = "2022 Scheme",
                    maxMarks = 50,
                    durationMinutes = 120
                )

            // Fallback for any other custom or unlisted code
            else -> {
                val cleanFallback = sCode.removePrefix("1").removePrefix("21")
                VtuPaperInfo(
                    vtuPaperCode = cleanFallback,
                    officialPdfUrl = "$VTU_BASE_URL/$cleanFallback.pdf",
                    subjectTitle = subjectName.ifBlank { "University Course" },
                    semester = semester,
                    scheme = "VTU University Scheme"
                )
            }
        }
    }
}
