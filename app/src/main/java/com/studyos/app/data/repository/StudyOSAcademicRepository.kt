package com.studyos.app.data.repository

import com.studyos.app.features.journey.components.Difficulty
import com.studyos.app.features.journey.components.LessonCategory
import com.studyos.app.features.journey.components.LessonNode
import com.studyos.app.features.journey.components.LessonStatus
import com.studyos.app.features.journey.components.Prerequisite
import com.studyos.app.features.journey.components.SemesterJourney
import com.studyos.app.features.journey.components.SubjectJourney
import com.studyos.app.features.journey.components.UnitJourney

/**
 * Single source of truth with flexible unlocking rules:
 * 1. Lesson 1 of EVERY unit is ALWAYS unlocked.
 * 2. Archived semesters are fully unlocked.
 * 3. Backlog subjects are fully unlocked.
 * 4. Advanced topics in a unit are locked based on prerequisites.
 */
object StudyOSAcademicRepository {

    fun getOrGenerateTopicQuest(
        topicId: String,
        topicTitle: String,
        subjectName: String,
        semesterName: String
    ): com.studyos.app.data.model.DynamicTopicQuest {
        return com.studyos.app.domain.quest.AcademicQuestGenerator.generateQuestForTopic(
            topicId = topicId,
            topicTitle = topicTitle,
            subjectName = subjectName,
            semesterName = semesterName
        )
    }

    fun getSemestersForUser(
        userSemesterNumber: Int = 1,
        completedSemesters: List<Int> = emptyList(),
        completedTopicKeys: Set<String> = emptySet(),
        backlogSubjects: List<String> = emptyList()
    ): List<SemesterJourney> {
        val baseSemesters = getSemesters()
        val effectiveUserSem = if (userSemesterNumber <= 0) 1 else userSemesterNumber

        // Rule: Show ONLY unlocked semesters (effectiveUserSem or completed). Hide future locked semesters completely.
        val visibleBaseSemesters = baseSemesters.filter { rawSem ->
            rawSem.semesterNumber <= effectiveUserSem || completedSemesters.contains(rawSem.semesterNumber)
        }

        // Collect all backlog subjects from prior semesters for Rule 4
        val priorSemesterBacklogSubjects = mutableListOf<SubjectJourney>()

        val evaluatedSemesters = visibleBaseSemesters.map { rawSem ->
            val semNum = rawSem.semesterNumber
            val isPriorSemester = semNum < effectiveUserSem
            val isLocked = semNum > effectiveUserSem && !completedSemesters.contains(semNum)

            val evaluatedSubjects = rawSem.subjects.map { rawSubject ->
                val isBacklog = backlogSubjects.any {
                    it.equals(rawSubject.name, ignoreCase = true) ||
                    it.equals(rawSubject.id, ignoreCase = true) ||
                    rawSubject.name.lowercase().contains(it.lowercase())
                }

                val evaluatedUnits = rawSubject.units.map { rawUnit ->
                    var previousLessonCompletedOrAvailable = true

                    val evaluatedLessons = rawUnit.lessons.mapIndexed { index, rawLesson ->
                        val scopedKey = "${rawSem.id}_${rawSubject.id}_${rawLesson.id}"
                        val isExplicitlyCompleted = completedTopicKeys.contains(scopedKey) || completedTopicKeys.contains(rawLesson.id)

                        // Rule 2 & 3: Previous semester subjects passed (NOT in backlog) are 100% completed
                        val status = when {
                            isPriorSemester && !isBacklog -> LessonStatus.COMPLETED
                            isExplicitlyCompleted -> LessonStatus.COMPLETED
                            isLocked -> LessonStatus.LOCKED
                            index == 0 || previousLessonCompletedOrAvailable -> LessonStatus.AVAILABLE
                            else -> LessonStatus.LOCKED
                        }

                        if (status != LessonStatus.COMPLETED) {
                            previousLessonCompletedOrAvailable = false
                        }

                        rawLesson.copy(status = status)
                    }

                    rawUnit.copy(lessons = evaluatedLessons)
                }

                val subjectCompletedCount = evaluatedUnits.sumOf { unit -> unit.lessons.count { it.status == LessonStatus.COMPLETED } }
                val subjectTotalCount = evaluatedUnits.sumOf { unit -> unit.lessons.size }

                val evaluatedSubject = rawSubject.copy(
                    completedCount = subjectCompletedCount,
                    totalCount = subjectTotalCount,
                    units = evaluatedUnits
                )

                if (isPriorSemester && isBacklog) {
                    priorSemesterBacklogSubjects.add(
                        evaluatedSubject.copy(
                            name = if (evaluatedSubject.name.startsWith("[Backlog]")) evaluatedSubject.name else "[Backlog] ${evaluatedSubject.name}"
                        )
                    )
                }

                evaluatedSubject
            }

            val completedSubjectsCount = evaluatedSubjects.count { it.completedCount >= it.totalCount }
            val semCompletedLessons = evaluatedSubjects.sumOf { it.completedCount }
            val semTotalLessons = evaluatedSubjects.sumOf { it.totalCount }
            val semProgress = if (semTotalLessons > 0) semCompletedLessons.toFloat() / semTotalLessons.toFloat() else 0f

            rawSem.copy(
                isArchived = isPriorSemester,
                isLocked = isLocked,
                progress = semProgress,
                completedSubjectsCount = completedSubjectsCount,
                subjectCount = evaluatedSubjects.size,
                subjects = evaluatedSubjects
            )
        }

        // Rule 4: Backlog subjects must appear at the top of the current semester journey
        if (priorSemesterBacklogSubjects.isNotEmpty()) {
            return evaluatedSemesters.map { sem ->
                if (sem.semesterNumber == effectiveUserSem) {
                    val combinedSubjects = (priorSemesterBacklogSubjects + sem.subjects).distinctBy { it.id }
                    val newCompletedSubjects = combinedSubjects.count { it.completedCount >= it.totalCount }
                    val semCompletedLessons = combinedSubjects.sumOf { it.completedCount }
                    val semTotalLessons = combinedSubjects.sumOf { it.totalCount }
                    val semProgress = if (semTotalLessons > 0) semCompletedLessons.toFloat() / semTotalLessons.toFloat() else 0f

                    sem.copy(
                        subjects = combinedSubjects,
                        subjectCount = combinedSubjects.size,
                        completedSubjectsCount = newCompletedSubjects,
                        progress = semProgress
                    )
                } else {
                    sem
                }
            }
        }

        return evaluatedSemesters
    }

    fun getSemesters(): List<SemesterJourney> {
        return listOf(
            SemesterJourney(
                id = "vtu-cse-s1",
                semesterNumber = 1,
                name = "Semester 1 • Computer Science & Engineering",
                isArchived = true,
                progress = 1f,
                subjectCount = 5,
                subjects = listOf(
                    SubjectJourney(
                        id = "1BMATS101",
                        name = "Calculus and Linear Algebra",
                        iconEmoji = "📐",
                        completedCount = 37,
                        totalCount = 37,
                        units = listOf(
                            UnitJourney(
                                id = "1BMATS101_M1",
                                unitNumber = 1,
                                title = "Module 1 • Calculus",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS101_M1_T1",
                                    num = 1,
                                    title = "Partial differentiation",
                                    shortTitle = "Partial differentiation",
                                    desc = "Hands-on build & master Partial differentiation",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Partial differentiation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M1_T2",
                                    num = 2,
                                    title = "Total derivative",
                                    shortTitle = "Total derivative",
                                    desc = "Hands-on build & master Total derivative",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Total derivative solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M1_T3",
                                    num = 3,
                                    title = "Differentiation of composite functions",
                                    shortTitle = "Differentiation of composite functions",
                                    desc = "Hands-on build & master Differentiation of composite functions",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Differentiation of composite functions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M1_T4",
                                    num = 4,
                                    title = "Jacobian",
                                    shortTitle = "Jacobian",
                                    desc = "Hands-on build & master Jacobian",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Jacobian solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M1_T5",
                                    num = 5,
                                    title = "Statement of Taylor's and Maclaurin's series expansion for two variables",
                                    shortTitle = "Statement of Taylor's and Maclaurin's series expansion for two variables",
                                    desc = "Hands-on build & master Statement of Taylor's and Maclaurin's series expansion for two variables",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Statement of Taylor's and Maclaurin's series expansion for two variables solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M1_T6",
                                    num = 6,
                                    title = "Maxima and minima for the function of two variables",
                                    shortTitle = "Maxima and minima for the function of two variables",
                                    desc = "Hands-on build & master Maxima and minima for the function of two variables",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Maxima and minima for the function of two variables solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS101_M2",
                                unitNumber = 2,
                                title = "Module 2 • Vector Calculus",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS101_M2_T1",
                                    num = 1,
                                    title = "Scalar and vector fields",
                                    shortTitle = "Scalar and vector fields",
                                    desc = "Hands-on build & master Scalar and vector fields",
                                    estMin = 20,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T2",
                                    num = 2,
                                    title = "Gradient and directional derivatives",
                                    shortTitle = "Gradient and directional derivatives",
                                    desc = "Hands-on build & master Gradient and directional derivatives",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Gradient and directional derivatives solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T3",
                                    num = 3,
                                    title = "Divergence and curl-physical interpretation",
                                    shortTitle = "Divergence and curl-physical interpretation",
                                    desc = "Hands-on build & master Divergence and curl-physical interpretation",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Divergence and curl-physical interpretation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T4",
                                    num = 4,
                                    title = "Solenoidal vector fields, irrotational vector fields and scalar potential",
                                    shortTitle = "Solenoidal vector fields",
                                    desc = "Hands-on build & master Solenoidal vector fields",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T5",
                                    num = 5,
                                    title = "Introduction to polar coordinates and polar curves",
                                    shortTitle = "Introduction to polar coordinates and polar curves",
                                    desc = "Hands-on build & master Introduction to polar coordinates and polar curves",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Introduction to polar coordinates and polar curves solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T6",
                                    num = 6,
                                    title = "Curvilinear coordinates: Scale factors and base vectors",
                                    shortTitle = "Curvilinear coordinates",
                                    desc = "Hands-on build & master Curvilinear coordinates",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T7",
                                    num = 7,
                                    title = "Cylindrical polar coordinates",
                                    shortTitle = "Cylindrical polar coordinates",
                                    desc = "Hands-on build & master Cylindrical polar coordinates",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Cylindrical polar coordinates solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T8",
                                    num = 8,
                                    title = "Spherical polar coordinates",
                                    shortTitle = "Spherical polar coordinates",
                                    desc = "Hands-on build & master Spherical polar coordinates",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Spherical polar coordinates solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M2_T9",
                                    num = 9,
                                    title = "Transformation between cartesian and curvilinear systems, orthogonality",
                                    shortTitle = "Transformation between cartesian and curvilinear systems",
                                    desc = "Hands-on build & master Transformation between cartesian and curvilinear systems",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Transformation between cartesian and curvilinear systems solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS101_M3",
                                unitNumber = 3,
                                title = "Module 3 • System of Linear Equations",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS101_M3_T1",
                                    num = 1,
                                    title = "Elementary row transformation of a matrix and Echelon form",
                                    shortTitle = "Elementary row transformation of a matrix and Echelon form",
                                    desc = "Hands-on build & master Elementary row transformation of a matrix and Echelon form",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Elementary row transformation of a matrix and Echelon form solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M3_T2",
                                    num = 2,
                                    title = "Rank of a matrix",
                                    shortTitle = "Rank of a matrix",
                                    desc = "Hands-on build & master Rank of a matrix",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Rank of a matrix solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M3_T3",
                                    num = 3,
                                    title = "Consistency and solution of system of linear equations",
                                    shortTitle = "Consistency and solution of system of linear equations",
                                    desc = "Hands-on build & master Consistency and solution of system of linear equations",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Consistency and solution of system of linear equations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M3_T4",
                                    num = 4,
                                    title = "Gauss elimination method",
                                    shortTitle = "Gauss elimination method",
                                    desc = "Hands-on build & master Gauss elimination method",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Gauss elimination method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M3_T5",
                                    num = 5,
                                    title = "Gauss Jordan method",
                                    shortTitle = "Gauss Jordan method",
                                    desc = "Hands-on build & master Gauss Jordan method",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Gauss Jordan method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M3_T6",
                                    num = 6,
                                    title = "Applications: Traffic flow",
                                    shortTitle = "Applications",
                                    desc = "Hands-on build & master Applications",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Applications solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M3_T7",
                                    num = 7,
                                    title = "Eigenvalues and Eigenvectors",
                                    shortTitle = "Eigenvalues and Eigenvectors",
                                    desc = "Hands-on build & master Eigenvalues and Eigenvectors",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS101_M3_T8",
                                    num = 8,
                                    title = "Diagonalization of the matrix and modal matrix",
                                    shortTitle = "Diagonalization of the matrix and modal matrix",
                                    desc = "Hands-on build & master Diagonalization of the matrix and modal matrix",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Diagonalization of the matrix and modal matrix solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS101_M4",
                                unitNumber = 4,
                                title = "Module 4 • Vector Space",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS101_M4_T1",
                                    num = 1,
                                    title = "Vector spaces: definition and examples",
                                    shortTitle = "Vector spaces",
                                    desc = "Hands-on build & master Vector spaces",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS101_M4_T2",
                                    num = 2,
                                    title = "Subspace: definition and examples",
                                    shortTitle = "Subspace",
                                    desc = "Hands-on build & master Subspace",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Subspace solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M4_T3",
                                    num = 3,
                                    title = "Linear Combinations and linear span",
                                    shortTitle = "Linear Combinations and linear span",
                                    desc = "Hands-on build & master Linear Combinations and linear span",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Linear Combinations and linear span solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M4_T4",
                                    num = 4,
                                    title = "Linearly independent and dependent sets",
                                    shortTitle = "Linearly independent and dependent sets",
                                    desc = "Hands-on build & master Linearly independent and dependent sets",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Linearly independent and dependent sets solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M4_T5",
                                    num = 5,
                                    title = "Basis and dimension",
                                    shortTitle = "Basis and dimension",
                                    desc = "Hands-on build & master Basis and dimension",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Basis and dimension solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M4_T6",
                                    num = 6,
                                    title = "Row space and column space of a matrix",
                                    shortTitle = "Row space and column space of a matrix",
                                    desc = "Hands-on build & master Row space and column space of a matrix",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Row space and column space of a matrix solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M4_T7",
                                    num = 7,
                                    title = "Coordinates vector",
                                    shortTitle = "Coordinates vector",
                                    desc = "Hands-on build & master Coordinates vector",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS101_M4_T8",
                                    num = 8,
                                    title = "Inner products and orthogonality",
                                    shortTitle = "Inner products and orthogonality",
                                    desc = "Hands-on build & master Inner products and orthogonality",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Inner products and orthogonality solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS101_M5",
                                unitNumber = 5,
                                title = "Module 5 • Linear Transformation",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS101_M5_T1",
                                    num = 1,
                                    title = "Definition and examples of linear transformations",
                                    shortTitle = "Definition and examples of linear transformations",
                                    desc = "Hands-on build & master Definition and examples of linear transformations",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Definition and examples of linear transformations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M5_T2",
                                    num = 2,
                                    title = "Algebra of linear transformations",
                                    shortTitle = "Algebra of linear transformations",
                                    desc = "Hands-on build & master Algebra of linear transformations",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Algebra of linear transformations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M5_T3",
                                    num = 3,
                                    title = "Matrix of a linear transformation",
                                    shortTitle = "Matrix of a linear transformation",
                                    desc = "Hands-on build & master Matrix of a linear transformation",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Matrix of a linear transformation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M5_T4",
                                    num = 4,
                                    title = "Singular, non-singular linear transformations and invertible linear transformations",
                                    shortTitle = "Singular",
                                    desc = "Hands-on build & master Singular",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Singular solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M5_T5",
                                    num = 5,
                                    title = "Rank and nullity of linear transformations",
                                    shortTitle = "Rank and nullity of linear transformations",
                                    desc = "Hands-on build & master Rank and nullity of linear transformations",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Rank and nullity of linear transformations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS101_M5_T6",
                                    num = 6,
                                    title = "Rank-Nullity theorem",
                                    shortTitle = "Rank-Nullity theorem",
                                    desc = "Hands-on build & master Rank-Nullity theorem",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Rank-Nullity theorem solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCHEC102",
                        name = "Applied Chemistry for Computer Science Stream",
                        iconEmoji = "📚",
                        completedCount = 39,
                        totalCount = 39,
                        units = listOf(
                            UnitJourney(
                                id = "1BCHEC102_M1",
                                unitNumber = 1,
                                title = "Module 1 • Functional Materials for Mem...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCHEC102_M1_T1",
                                    num = 1,
                                    title = "Organic Semiconductors: p-type Pentacene & n-type Perfluoropentacene",
                                    shortTitle = "Organic Semiconductors",
                                    desc = "Hands-on build & master Organic Semiconductors",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Organic Semiconductors solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T2",
                                    num = 2,
                                    title = "Differences between Organic and Inorganic Memory Devices",
                                    shortTitle = "Differences between Organic and Inorganic Memory Devices",
                                    desc = "Hands-on build & master Differences between Organic and Inorganic Memory Devices",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Differences between Organic and Inorganic Memory Devices solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T3",
                                    num = 3,
                                    title = "Pentacene Semiconductor Chip: Construction, Working and Advantages",
                                    shortTitle = "Pentacene Semiconductor Chip",
                                    desc = "Hands-on build & master Pentacene Semiconductor Chip",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T4",
                                    num = 4,
                                    title = "Resistive RAM (ReRAM): Synthesis of TiO2-RAM Nanomaterial by Sol-Gel Method",
                                    shortTitle = "Resistive RAM (ReRAM)",
                                    desc = "Hands-on build & master Resistive RAM (ReRAM)",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Resistive RAM (ReRAM) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T5",
                                    num = 5,
                                    title = "Properties and Applications of ReRAM Materials",
                                    shortTitle = "Properties and Applications of ReRAM Materials",
                                    desc = "Hands-on build & master Properties and Applications of ReRAM Materials",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T6",
                                    num = 6,
                                    title = "Liquid Crystals (LCs): Classification, Properties and Applications",
                                    shortTitle = "Liquid Crystals (LCs)",
                                    desc = "Hands-on build & master Liquid Crystals (LCs)",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T7",
                                    num = 7,
                                    title = "Light Emitting Diodes (LEDs): Construction, Working and Applications",
                                    shortTitle = "Light Emitting Diodes (LEDs)",
                                    desc = "Hands-on build & master Light Emitting Diodes (LEDs)",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Light Emitting Diodes (LEDs) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T8",
                                    num = 8,
                                    title = "OLEDs and AMOLEDs: Construction, Working Principle and Applications",
                                    shortTitle = "OLEDs and AMOLEDs",
                                    desc = "Hands-on build & master OLEDs and AMOLEDs",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M1_T9",
                                    num = 9,
                                    title = "Quantum Light Emitting Diodes (QLEDs): Construction, Working and Applications",
                                    shortTitle = "Quantum Light Emitting Diodes (QLEDs)",
                                    desc = "Hands-on build & master Quantum Light Emitting Diodes (QLEDs)",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Quantum Light Emitting Diodes (QLEDs) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCHEC102_M2",
                                unitNumber = 2,
                                title = "Module 2 • Quantum Materials and Polyme...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCHEC102_M2_T1",
                                    num = 1,
                                    title = "Quantum Dots: Quantum Confinement Effect, Surface-to-Volume Ratio & Band Gap",
                                    shortTitle = "Quantum Dots",
                                    desc = "Hands-on build & master Quantum Dots",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Quantum Dots solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M2_T2",
                                    num = 2,
                                    title = "Synthesis of Cd-Se Quantum Dots by Wet Chemical Method & Applications",
                                    shortTitle = "Synthesis of Cd-Se Quantum Dots by Wet Chemical Method",
                                    desc = "Hands-on build & master Synthesis of Cd-Se Quantum Dots by Wet Chemical Method",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Synthesis of Cd-Se Quantum Dots by Wet Chemical Method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M2_T3",
                                    num = 3,
                                    title = "Quantum Dot Sensitized Solar Cells (QDSSCs): Construction, Working and Applications",
                                    shortTitle = "Quantum Dot Sensitized Solar Cells (QDSSCs)",
                                    desc = "Hands-on build & master Quantum Dot Sensitized Solar Cells (QDSSCs)",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Quantum Dot Sensitized Solar Cells (QDSSCs) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M2_T4",
                                    num = 4,
                                    title = "Polymer Molecular Weight: Number Average, Weight Average & Numerical Problems",
                                    shortTitle = "Polymer Molecular Weight",
                                    desc = "Hands-on build & master Polymer Molecular Weight",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Polymer Molecular Weight solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M2_T5",
                                    num = 5,
                                    title = "Structure-Properties Relationship of Polymers (Crystallinity, Strength, Elasticity, Chemical Resistivity)",
                                    shortTitle = "Structure-Properties Relationship of Polymers (Crystallinity",
                                    desc = "Hands-on build & master Structure-Properties Relationship of Polymers (Crystallinity",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M2_T6",
                                    num = 6,
                                    title = "Nylon-6,6: Synthesis, Properties and Advantages in 3D Printing Applications",
                                    shortTitle = "Nylon-6",
                                    desc = "Hands-on build & master Nylon-6",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M2_T7",
                                    num = 7,
                                    title = "CPVC and PMMA: Synthesis, Properties and Device Applications",
                                    shortTitle = "CPVC and PMMA",
                                    desc = "Hands-on build & master CPVC and PMMA",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M2_T8",
                                    num = 8,
                                    title = "Conducting Polymers: Polyaniline Synthesis, Conduction Mechanism and Applications",
                                    shortTitle = "Conducting Polymers",
                                    desc = "Hands-on build & master Conducting Polymers",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Conducting Polymers solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCHEC102_M3",
                                unitNumber = 3,
                                title = "Module 3 • Sustainable Energy Systems",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCHEC102_M3_T1",
                                    num = 1,
                                    title = "Nernst Equation Overview & Concentration Cell Construction, Working and Numerical Problems",
                                    shortTitle = "Nernst Equation Overview",
                                    desc = "Hands-on build & master Nernst Equation Overview",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Nernst Equation Overview solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M3_T2",
                                    num = 2,
                                    title = "Batteries Classification & Li-Ion Battery Construction, Working and Applications",
                                    shortTitle = "Batteries Classification",
                                    desc = "Hands-on build & master Batteries Classification",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M3_T3",
                                    num = 3,
                                    title = "Sodium-Ion Battery: Construction and Working for EV Applications",
                                    shortTitle = "Sodium-Ion Battery",
                                    desc = "Hands-on build & master Sodium-Ion Battery",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Sodium-Ion Battery solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M3_T4",
                                    num = 4,
                                    title = "Ultra-Small Asymmetric Supercapacitor for IoT/Wearable Devices",
                                    shortTitle = "Ultra-Small Asymmetric Supercapacitor for IoT/Wearable Devices",
                                    desc = "Hands-on build & master Ultra-Small Asymmetric Supercapacitor for IoT/Wearable Devices",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Ultra-Small Asymmetric Supercapacitor for IoT/Wearable Devices solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M3_T5",
                                    num = 5,
                                    title = "Fuel Cells vs Batteries & Solid-Oxide Fuel Cells (SOFCs)",
                                    shortTitle = "Fuel Cells vs Batteries",
                                    desc = "Hands-on build & master Fuel Cells vs Batteries",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Fuel Cells vs Batteries solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M3_T6",
                                    num = 6,
                                    title = "Solar Photovoltaic Cell (PV Cell): Construction, Working, Applications and Limitations",
                                    shortTitle = "Solar Photovoltaic Cell (PV Cell)",
                                    desc = "Hands-on build & master Solar Photovoltaic Cell (PV Cell)",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Solar Photovoltaic Cell (PV Cell) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M3_T7",
                                    num = 7,
                                    title = "Green Hydrogen Production by Photocatalytic Water Splitting using TiO2 Catalyst",
                                    shortTitle = "Green Hydrogen Production by Photocatalytic Water Splitting using TiO2 Catalyst",
                                    desc = "Hands-on build & master Green Hydrogen Production by Photocatalytic Water Splitting using TiO2 Catalyst",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Green Hydrogen Production by Photocatalytic Water Splitting using TiO2 Catalyst solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCHEC102_M4",
                                unitNumber = 4,
                                title = "Module 4 • Sensors and Corrosion Scienc...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCHEC102_M4_T1",
                                    num = 1,
                                    title = "Sensors Terminology: Transducer, Actuators and Sensors",
                                    shortTitle = "Sensors Terminology",
                                    desc = "Hands-on build & master Sensors Terminology",
                                    estMin = 20,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Sensors Terminology solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M4_T2",
                                    num = 2,
                                    title = "Conductometric Sensor: Estimation of Acid Mixture",
                                    shortTitle = "Conductometric Sensor",
                                    desc = "Hands-on build & master Conductometric Sensor",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Conductometric Sensor solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M4_T3",
                                    num = 3,
                                    title = "Colorimetric Sensor: Estimation of Copper in PCB",
                                    shortTitle = "Colorimetric Sensor",
                                    desc = "Hands-on build & master Colorimetric Sensor",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Construct connected graph networks", "Find Eulerian & Hamiltonian paths", "Apply 4-color graph coloring algorithms")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M4_T4",
                                    num = 4,
                                    title = "Electrochemical Gas Sensors: Detection of NOx and SOx in Air",
                                    shortTitle = "Electrochemical Gas Sensors",
                                    desc = "Hands-on build & master Electrochemical Gas Sensors",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Electrochemical Gas Sensors solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M4_T5",
                                    num = 5,
                                    title = "Biosensor: Glucose Detection in Biofluids",
                                    shortTitle = "Biosensor",
                                    desc = "Hands-on build & master Biosensor",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Biosensor solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M4_T6",
                                    num = 6,
                                    title = "Electrochemical Theory of Corrosion & Types (Differential Metal, Waterline, Pitting)",
                                    shortTitle = "Electrochemical Theory of Corrosion",
                                    desc = "Hands-on build & master Electrochemical Theory of Corrosion",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Electrochemical Theory of Corrosion solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M4_T7",
                                    num = 7,
                                    title = "Corrosion Control: Galvanization, Anodization and Vapour Corrosion Inhibitors for PCBs",
                                    shortTitle = "Corrosion Control",
                                    desc = "Hands-on build & master Corrosion Control",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Corrosion Control solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M4_T8",
                                    num = 8,
                                    title = "Corrosion Penetration Rate (CPR): Definition and Numerical Problems",
                                    shortTitle = "Corrosion Penetration Rate (CPR)",
                                    desc = "Hands-on build & master Corrosion Penetration Rate (CPR)",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Corrosion Penetration Rate (CPR) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCHEC102_M5",
                                unitNumber = 5,
                                title = "Module 5 • Green Materials and E-Waste...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCHEC102_M5_T1",
                                    num = 1,
                                    title = "Green Solvents for Server Heat Management",
                                    shortTitle = "Green Solvents for Server Heat Management",
                                    desc = "Hands-on build & master Green Solvents for Server Heat Management",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Green Solvents for Server Heat Management solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M5_T2",
                                    num = 2,
                                    title = "Glycerol Trioleate Ester: Synthesis, Properties and Uses in IT Infrastructure",
                                    shortTitle = "Glycerol Trioleate Ester",
                                    desc = "Hands-on build & master Glycerol Trioleate Ester",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M5_T3",
                                    num = 3,
                                    title = "Green Synthesis of ZnO Nanoparticles for RFID and IONT System Applications",
                                    shortTitle = "Green Synthesis of ZnO Nanoparticles for RFID and IONT System Applications",
                                    desc = "Hands-on build & master Green Synthesis of ZnO Nanoparticles for RFID and IONT System Applications",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Green Synthesis of ZnO Nanoparticles for RFID and IONT System Applications solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M5_T4",
                                    num = 4,
                                    title = "Biomaterials: Synthesis, Properties and Touch Screen Uses of PLA and PEG",
                                    shortTitle = "Biomaterials",
                                    desc = "Hands-on build & master Biomaterials",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M5_T5",
                                    num = 5,
                                    title = "Alginate Hydrogel: Synthesis, Properties and Uses in Brain-Computer Interfaces (BCIs)",
                                    shortTitle = "Alginate Hydrogel",
                                    desc = "Hands-on build & master Alginate Hydrogel",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M5_T6",
                                    num = 6,
                                    title = "E-Waste: Sources, Composition and Environmental/Health Effects",
                                    shortTitle = "E-Waste",
                                    desc = "Hands-on build & master E-Waste",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on E-Waste solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCHEC102_M5_T7",
                                    num = 7,
                                    title = "Artificial Intelligence in E-Waste Management & Gold Extraction by Bioleaching",
                                    shortTitle = "Artificial Intelligence in E-Waste Management",
                                    desc = "Hands-on build & master Artificial Intelligence in E-Waste Management",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Artificial Intelligence in E-Waste Management solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BAIA103",
                        name = "Introduction to AI and Applications",
                        iconEmoji = "📚",
                        completedCount = 35,
                        totalCount = 35,
                        units = listOf(
                            UnitJourney(
                                id = "1BAIA103_M1",
                                unitNumber = 1,
                                title = "Module 1 • Introduction to Artificial I...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BAIA103_M1_T1",
                                    num = 1,
                                    title = "Introduction to Artificial Intelligence: How AI Works, History, Advantages & Disadvantages",
                                    shortTitle = "Introduction to Artificial Intelligence",
                                    desc = "Hands-on build & master Introduction to Artificial Intelligence",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Introduction to Artificial Intelligence solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M1_T2",
                                    num = 2,
                                    title = "Types of AI: Weak AI, Strong AI, Reactive Machines, Limited Memory, Theory of Mind, Self-Awareness",
                                    shortTitle = "Types of AI",
                                    desc = "Hands-on build & master Types of AI",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Types of AI solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M1_T3",
                                    num = 3,
                                    title = "AI vs Augmented Intelligence vs Cognitive Computing vs Machine Learning vs Deep Learning",
                                    shortTitle = "AI vs Augmented Intelligence vs Cognitive Computing vs Machine Learning vs Deep Learning",
                                    desc = "Hands-on build & master AI vs Augmented Intelligence vs Cognitive Computing vs Machine Learning vs Deep Learning",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on AI vs Augmented Intelligence vs Cognitive Computing vs Machine Learning vs Deep Learning solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M1_T4",
                                    num = 4,
                                    title = "Machine Intelligence: Defining Intelligence, Components & Human vs Machine Intelligence",
                                    shortTitle = "Machine Intelligence",
                                    desc = "Hands-on build & master Machine Intelligence",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Machine Intelligence solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M1_T5",
                                    num = 5,
                                    title = "Agent and Environment & Search Concepts",
                                    shortTitle = "Agent and Environment",
                                    desc = "Hands-on build & master Agent and Environment",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Agent and Environment solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M1_T6",
                                    num = 6,
                                    title = "Uninformed Search Algorithms",
                                    shortTitle = "Uninformed Search Algorithms",
                                    desc = "Hands-on build & master Uninformed Search Algorithms",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Uninformed Search Algorithms solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M1_T7",
                                    num = 7,
                                    title = "Informed Search Algorithms: Pure Heuristic Search & Best-First Search (Greedy Search)",
                                    shortTitle = "Informed Search Algorithms",
                                    desc = "Hands-on build & master Informed Search Algorithms",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Informed Search Algorithms solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M1_T8",
                                    num = 8,
                                    title = "Knowledge Representation: Introduction, Knowledge-Based Agent & Types of Knowledge",
                                    shortTitle = "Knowledge Representation",
                                    desc = "Hands-on build & master Knowledge Representation",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Knowledge Representation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BAIA103_M2",
                                unitNumber = 2,
                                title = "Module 2 • Introduction to Prompt Engin...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BAIA103_M2_T1",
                                    num = 1,
                                    title = "Introduction to Prompt Engineering: Evolution, Types of Prompts & Working Mechanism",
                                    shortTitle = "Introduction to Prompt Engineering",
                                    desc = "Hands-on build & master Introduction to Prompt Engineering",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Introduction to Prompt Engineering solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M2_T2",
                                    num = 2,
                                    title = "Role of Prompt Engineering in Communication, Advantages & Future of LLM Communication",
                                    shortTitle = "Role of Prompt Engineering in Communication",
                                    desc = "Hands-on build & master Role of Prompt Engineering in Communication",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Role of Prompt Engineering in Communication solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M2_T3",
                                    num = 3,
                                    title = "Prompt Engineering Techniques for ChatGPT: Instructions Prompt Technique",
                                    shortTitle = "Prompt Engineering Techniques for ChatGPT",
                                    desc = "Hands-on build & master Prompt Engineering Techniques for ChatGPT",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Prompt Engineering Techniques for ChatGPT solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M2_T4",
                                    num = 4,
                                    title = "Zero-Shot, One-Shot, and Few-Shot Prompting",
                                    shortTitle = "Zero-Shot",
                                    desc = "Hands-on build & master Zero-Shot",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Zero-Shot solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M2_T5",
                                    num = 5,
                                    title = "Self-Consistency Prompting Technique",
                                    shortTitle = "Self-Consistency Prompting Technique",
                                    desc = "Hands-on build & master Self-Consistency Prompting Technique",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Self-Consistency Prompting Technique solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M2_T6",
                                    num = 6,
                                    title = "Prompts for Creative Thinking: Unlocking Imagination and Innovation",
                                    shortTitle = "Prompts for Creative Thinking",
                                    desc = "Hands-on build & master Prompts for Creative Thinking",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Prompts for Creative Thinking solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M2_T7",
                                    num = 7,
                                    title = "Prompts for Effective Writing: Igniting the Writing Process",
                                    shortTitle = "Prompts for Effective Writing",
                                    desc = "Hands-on build & master Prompts for Effective Writing",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Prompts for Effective Writing solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BAIA103_M3",
                                unitNumber = 3,
                                title = "Module 3 • Machine Learning Techniques...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BAIA103_M3_T1",
                                    num = 1,
                                    title = "Machine Learning Techniques in AI & Machine Learning Model Overview",
                                    shortTitle = "Machine Learning Techniques in AI",
                                    desc = "Hands-on build & master Machine Learning Techniques in AI",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Machine Learning Techniques in AI solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M3_T2",
                                    num = 2,
                                    title = "Regression Analysis in Machine Learning",
                                    shortTitle = "Regression Analysis in Machine Learning",
                                    desc = "Hands-on build & master Regression Analysis in Machine Learning",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Regression Analysis in Machine Learning solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M3_T3",
                                    num = 3,
                                    title = "Classification Techniques Overview",
                                    shortTitle = "Classification Techniques Overview",
                                    desc = "Hands-on build & master Classification Techniques Overview",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BAIA103_M3_T4",
                                    num = 4,
                                    title = "Clustering Techniques Overview",
                                    shortTitle = "Clustering Techniques Overview",
                                    desc = "Hands-on build & master Clustering Techniques Overview",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Clustering Techniques Overview solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M3_T5",
                                    num = 5,
                                    title = "Naïve Bayes Classification",
                                    shortTitle = "Naïve Bayes Classification",
                                    desc = "Hands-on build & master Naïve Bayes Classification",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BAIA103_M3_T6",
                                    num = 6,
                                    title = "Neural Networks Fundamentals",
                                    shortTitle = "Neural Networks Fundamentals",
                                    desc = "Hands-on build & master Neural Networks Fundamentals",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Neural Networks Fundamentals solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M3_T7",
                                    num = 7,
                                    title = "Support Vector Machine (SVM)",
                                    shortTitle = "Support Vector Machine (SVM)",
                                    desc = "Hands-on build & master Support Vector Machine (SVM)",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BAIA103_M4",
                                unitNumber = 4,
                                title = "Module 4 • Trends in AI",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BAIA103_M4_T1",
                                    num = 1,
                                    title = "AI and Ethical Concerns",
                                    shortTitle = "AI and Ethical Concerns",
                                    desc = "Hands-on build & master AI and Ethical Concerns",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on AI and Ethical Concerns solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M4_T2",
                                    num = 2,
                                    title = "AI as a Service (AIaaS)",
                                    shortTitle = "AI as a Service (AIaaS)",
                                    desc = "Hands-on build & master AI as a Service (AIaaS)",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on AI as a Service (AIaaS) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M4_T3",
                                    num = 3,
                                    title = "Recent Trends in AI",
                                    shortTitle = "Recent Trends in AI",
                                    desc = "Hands-on build & master Recent Trends in AI",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Recent Trends in AI solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M4_T4",
                                    num = 4,
                                    title = "Expert Systems",
                                    shortTitle = "Expert Systems",
                                    desc = "Hands-on build & master Expert Systems",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Expert Systems solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M4_T5",
                                    num = 5,
                                    title = "Internet of Things (IoT)",
                                    shortTitle = "Internet of Things (IoT)",
                                    desc = "Hands-on build & master Internet of Things (IoT)",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Internet of Things (IoT) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M4_T6",
                                    num = 6,
                                    title = "Artificial Intelligence of Things (AIoT)",
                                    shortTitle = "Artificial Intelligence of Things (AIoT)",
                                    desc = "Hands-on build & master Artificial Intelligence of Things (AIoT)",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Artificial Intelligence of Things (AIoT) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BAIA103_M5",
                                unitNumber = 5,
                                title = "Module 5 • Robotics and Industrial Appl...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BAIA103_M5_T1",
                                    num = 1,
                                    title = "Robotics: An Application of AI & Drones Using AI",
                                    shortTitle = "Robotics",
                                    desc = "Hands-on build & master Robotics",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Robotics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M5_T2",
                                    num = 2,
                                    title = "No Code AI and Low Code AI",
                                    shortTitle = "No Code AI and Low Code AI",
                                    desc = "Hands-on build & master No Code AI and Low Code AI",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on No Code AI and Low Code AI solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M5_T3",
                                    num = 3,
                                    title = "Application of AI in Healthcare",
                                    shortTitle = "Application of AI in Healthcare",
                                    desc = "Hands-on build & master Application of AI in Healthcare",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Application of AI in Healthcare solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M5_T4",
                                    num = 4,
                                    title = "Application of AI in Finance",
                                    shortTitle = "Application of AI in Finance",
                                    desc = "Hands-on build & master Application of AI in Finance",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Application of AI in Finance solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M5_T5",
                                    num = 5,
                                    title = "Application of AI in Retail and Agriculture",
                                    shortTitle = "Application of AI in Retail and Agriculture",
                                    desc = "Hands-on build & master Application of AI in Retail and Agriculture",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Application of AI in Retail and Agriculture solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M5_T6",
                                    num = 6,
                                    title = "Application of AI in Education and Transportation",
                                    shortTitle = "Application of AI in Education and Transportation",
                                    desc = "Hands-on build & master Application of AI in Education and Transportation",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Application of AI in Education and Transportation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BAIA103_M5_T7",
                                    num = 7,
                                    title = "AI in Experimentation and Multi-Disciplinary Research",
                                    shortTitle = "AI in Experimentation and Multi-Disciplinary Research",
                                    desc = "Hands-on build & master AI in Experimentation and Multi-Disciplinary Research",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.LAB,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BECE105",
                        name = "Fundamentals of Electronics and Communication Engineering",
                        iconEmoji = "📚",
                        completedCount = 36,
                        totalCount = 36,
                        units = listOf(
                            UnitJourney(
                                id = "1BECE105_M1",
                                unitNumber = 1,
                                title = "Module 1 • Diodes and Their Application...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BECE105_M1_T1",
                                    num = 1,
                                    title = "Diodes Introduction: Characteristics and Parameters",
                                    shortTitle = "Diodes Introduction",
                                    desc = "Hands-on build & master Diodes Introduction",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Diodes Introduction solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M1_T2",
                                    num = 2,
                                    title = "Diode Approximation and DC Load Line Analysis",
                                    shortTitle = "Diode Approximation and DC Load Line Analysis",
                                    desc = "Hands-on build & master Diode Approximation and DC Load Line Analysis",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Diode Approximation and DC Load Line Analysis solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M1_T3",
                                    num = 3,
                                    title = "Half Wave Rectifier",
                                    shortTitle = "Half Wave Rectifier",
                                    desc = "Hands-on build & master Half Wave Rectifier",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Half Wave Rectifier solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M1_T4",
                                    num = 4,
                                    title = "Full Wave Bridge Rectifier",
                                    shortTitle = "Full Wave Bridge Rectifier",
                                    desc = "Hands-on build & master Full Wave Bridge Rectifier",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Full Wave Bridge Rectifier solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M1_T5",
                                    num = 5,
                                    title = "Capacitor Filter Circuit (Qualitative Approach)",
                                    shortTitle = "Capacitor Filter Circuit (Qualitative Approach)",
                                    desc = "Hands-on build & master Capacitor Filter Circuit (Qualitative Approach)",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Capacitor Filter Circuit (Qualitative Approach) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M1_T6",
                                    num = 6,
                                    title = "Zener Diode and Voltage Regulation",
                                    shortTitle = "Zener Diode and Voltage Regulation",
                                    desc = "Hands-on build & master Zener Diode and Voltage Regulation",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Zener Diode and Voltage Regulation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M1_T7",
                                    num = 7,
                                    title = "Diode Logic Circuits",
                                    shortTitle = "Diode Logic Circuits",
                                    desc = "Hands-on build & master Diode Logic Circuits",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BECE105_M2",
                                unitNumber = 2,
                                title = "Module 2 • Bipolar Junction Transistors...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BECE105_M2_T1",
                                    num = 1,
                                    title = "BJT Introduction, Voltages, Currents, Amplification and Switching",
                                    shortTitle = "BJT Introduction",
                                    desc = "Hands-on build & master BJT Introduction",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on BJT Introduction solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M2_T2",
                                    num = 2,
                                    title = "Common Base and Common Emitter Characteristics",
                                    shortTitle = "Common Base and Common Emitter Characteristics",
                                    desc = "Hands-on build & master Common Base and Common Emitter Characteristics",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Common Base and Common Emitter Characteristics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M2_T3",
                                    num = 3,
                                    title = "BJT Biasing: Fixed Biasing and Voltage Divider",
                                    shortTitle = "BJT Biasing",
                                    desc = "Hands-on build & master BJT Biasing",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Formulate divide-and-conquer recurrences", "Solve Master Theorem cases 1-3", "Measure recursive call stack depth")
                                ),
                                createLesson(
                                    id = "1BECE105_M2_T4",
                                    num = 4,
                                    title = "DC Load Line and Bias Point",
                                    shortTitle = "DC Load Line and Bias Point",
                                    desc = "Hands-on build & master DC Load Line and Bias Point",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on DC Load Line and Bias Point solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M2_T5",
                                    num = 5,
                                    title = "Junction Field Effect Transistor (N-Channel JFET) & Characteristics",
                                    shortTitle = "Junction Field Effect Transistor (N-Channel JFET)",
                                    desc = "Hands-on build & master Junction Field Effect Transistor (N-Channel JFET)",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Junction Field Effect Transistor (N-Channel JFET) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M2_T6",
                                    num = 6,
                                    title = "MOSFETS: Enhancement MOSFETS",
                                    shortTitle = "MOSFETS",
                                    desc = "Hands-on build & master MOSFETS",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on MOSFETS solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M2_T7",
                                    num = 7,
                                    title = "Case Study: MOSFET as a Switch",
                                    shortTitle = "Case Study",
                                    desc = "Hands-on build & master Case Study",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Case Study solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BECE105_M3",
                                unitNumber = 3,
                                title = "Module 3 • Operational Amplifiers and A...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BECE105_M3_T1",
                                    num = 1,
                                    title = "Operational Amplifiers Introduction, Block Diagram & Schematic Symbol",
                                    shortTitle = "Operational Amplifiers Introduction",
                                    desc = "Hands-on build & master Operational Amplifiers Introduction",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Operational Amplifiers Introduction solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M3_T2",
                                    num = 2,
                                    title = "Op-Amp Parameters (Gain, Resistance, CMRR, Slew Rate, Bandwidth, Offset/Bias)",
                                    shortTitle = "Op-Amp Parameters (Gain",
                                    desc = "Hands-on build & master Op-Amp Parameters (Gain",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Op-Amp Parameters (Gain solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M3_T3",
                                    num = 3,
                                    title = "Ideal Op-Amp & Equivalent Circuit",
                                    shortTitle = "Ideal Op-Amp",
                                    desc = "Hands-on build & master Ideal Op-Amp",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Ideal Op-Amp solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M3_T4",
                                    num = 4,
                                    title = "Open Loop Op-Amp Configurations & Differential Amplifier",
                                    shortTitle = "Open Loop Op-Amp Configurations",
                                    desc = "Hands-on build & master Open Loop Op-Amp Configurations",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Open Loop Op-Amp Configurations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M3_T5",
                                    num = 5,
                                    title = "Inverting and Non-Inverting Amplifier Configurations",
                                    shortTitle = "Inverting and Non-Inverting Amplifier Configurations",
                                    desc = "Hands-on build & master Inverting and Non-Inverting Amplifier Configurations",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Inverting and Non-Inverting Amplifier Configurations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M3_T6",
                                    num = 6,
                                    title = "Differential Configuration & Voltage Follower",
                                    shortTitle = "Differential Configuration",
                                    desc = "Hands-on build & master Differential Configuration",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Differential Configuration solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M3_T7",
                                    num = 7,
                                    title = "Integrator and Differentiator Circuits",
                                    shortTitle = "Integrator and Differentiator Circuits",
                                    desc = "Hands-on build & master Integrator and Differentiator Circuits",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Integrator and Differentiator Circuits solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BECE105_M4",
                                unitNumber = 4,
                                title = "Module 4 • Fundamentals of Communicatio...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BECE105_M4_T1",
                                    num = 1,
                                    title = "Elements of an Electrical Communication System",
                                    shortTitle = "Elements of an Electrical Communication System",
                                    desc = "Hands-on build & master Elements of an Electrical Communication System",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Elements of an Electrical Communication System solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M4_T2",
                                    num = 2,
                                    title = "Communication Channels: Wireline, Fiber Optic, Wireless Electromagnetic",
                                    shortTitle = "Communication Channels",
                                    desc = "Hands-on build & master Communication Channels",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Communication Channels solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M4_T3",
                                    num = 3,
                                    title = "Analog Modulation Types: Amplitude, Frequency & Phase Modulation Waveforms",
                                    shortTitle = "Analog Modulation Types",
                                    desc = "Hands-on build & master Analog Modulation Types",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Analog Modulation Types solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M4_T4",
                                    num = 4,
                                    title = "AM Radio Broadcasting Applications",
                                    shortTitle = "AM Radio Broadcasting Applications",
                                    desc = "Hands-on build & master AM Radio Broadcasting Applications",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on AM Radio Broadcasting Applications solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M4_T5",
                                    num = 5,
                                    title = "Superheterodyne FM Receiver",
                                    shortTitle = "Superheterodyne FM Receiver",
                                    desc = "Hands-on build & master Superheterodyne FM Receiver",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Superheterodyne FM Receiver solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M4_T6",
                                    num = 6,
                                    title = "Mobile Wireless Telephone Systems",
                                    shortTitle = "Mobile Wireless Telephone Systems",
                                    desc = "Hands-on build & master Mobile Wireless Telephone Systems",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Mobile Wireless Telephone Systems solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M4_T7",
                                    num = 7,
                                    title = "Case Study: Analog to Digital Conversion Using PCM",
                                    shortTitle = "Case Study",
                                    desc = "Hands-on build & master Case Study",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Case Study solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BECE105_M5",
                                unitNumber = 5,
                                title = "Module 5 • Digital Systems",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BECE105_M5_T1",
                                    num = 1,
                                    title = "Digital Systems & Numbering Systems (Binary, Octal, Decimal, Hexadecimal)",
                                    shortTitle = "Digital Systems",
                                    desc = "Hands-on build & master Digital Systems",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Digital Systems solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M5_T2",
                                    num = 2,
                                    title = "Number Base Conversion",
                                    shortTitle = "Number Base Conversion",
                                    desc = "Hands-on build & master Number Base Conversion",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Number Base Conversion solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M5_T3",
                                    num = 3,
                                    title = "1's and 2's Complement Operations & Signed Binary Arithmetic",
                                    shortTitle = "1's and 2's Complement Operations",
                                    desc = "Hands-on build & master 1's and 2's Complement Operations",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on 1's and 2's Complement Operations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M5_T4",
                                    num = 4,
                                    title = "Binary Logic & Theorems/Properties of Boolean Algebra",
                                    shortTitle = "Binary Logic",
                                    desc = "Hands-on build & master Binary Logic",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BECE105_M5_T5",
                                    num = 5,
                                    title = "Boolean Functions, Canonical and Standard Forms",
                                    shortTitle = "Boolean Functions",
                                    desc = "Hands-on build & master Boolean Functions",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Boolean Functions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M5_T6",
                                    num = 6,
                                    title = "Digital Logic Gates & NAND/NOR Universal Gates",
                                    shortTitle = "Digital Logic Gates",
                                    desc = "Hands-on build & master Digital Logic Gates",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "1BECE105_M5_T7",
                                    num = 7,
                                    title = "Binary Adders: Half Adder and Full Adder",
                                    shortTitle = "Binary Adders",
                                    desc = "Hands-on build & master Binary Adders",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Binary Adders solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BECE105_M5_T8",
                                    num = 8,
                                    title = "Case Study: 4-Bit Adder Simulation",
                                    shortTitle = "Case Study",
                                    desc = "Hands-on build & master Case Study",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Case Study solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BPLC105B",
                        name = "Python Programming",
                        iconEmoji = "☕",
                        completedCount = 31,
                        totalCount = 31,
                        units = listOf(
                            UnitJourney(
                                id = "1BPLC105B_M1",
                                unitNumber = 1,
                                title = "Module 1 • The Way of the Program",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPLC105B_M1_T1",
                                    num = 1,
                                    title = "The Way of the Program: Python Language, Programs, and Debugging (Syntax, Runtime & Semantic Errors)",
                                    shortTitle = "The Way of the Program",
                                    desc = "Hands-on build & master The Way of the Program",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on The Way of the Program solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M1_T2",
                                    num = 2,
                                    title = "Values, Data Types, Variables, Variable Names, Keywords, and Statements",
                                    shortTitle = "Values",
                                    desc = "Hands-on build & master Values",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Values solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M1_T3",
                                    num = 3,
                                    title = "Evaluating Expressions, Operators/Operands, Type Converters, Order of Operations & Modulus Operator",
                                    shortTitle = "Evaluating Expressions",
                                    desc = "Hands-on build & master Evaluating Expressions",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Evaluating Expressions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M1_T4",
                                    num = 4,
                                    title = "Operations on Strings, Input, and Composition",
                                    shortTitle = "Operations on Strings",
                                    desc = "Hands-on build & master Operations on Strings",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Operations on Strings solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M1_T5",
                                    num = 5,
                                    title = "Iteration: Variable Updating, for Loop, while Statement & Collatz 3n+1 Sequence",
                                    shortTitle = "Iteration",
                                    desc = "Hands-on build & master Iteration",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Iteration solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M1_T6",
                                    num = 6,
                                    title = "Tables, Two-Dimensional Tables, Break, Continue, Paired Data & Nested Loops for Nested Data",
                                    shortTitle = "Tables",
                                    desc = "Hands-on build & master Tables",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Tables solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M1_T7",
                                    num = 7,
                                    title = "Functions with Arguments and Return Values",
                                    shortTitle = "Functions with Arguments and Return Values",
                                    desc = "Hands-on build & master Functions with Arguments and Return Values",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Functions with Arguments and Return Values solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPLC105B_M2",
                                unitNumber = 2,
                                title = "Module 2 • Strings",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPLC105B_M2_T1",
                                    num = 1,
                                    title = "Strings: Traversal, for Loop, Slices, Comparisons, Immutability & 'in'/'not in' Operators",
                                    shortTitle = "Strings",
                                    desc = "Hands-on build & master Strings",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Strings solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M2_T2",
                                    num = 2,
                                    title = "String Methods: find, split, string cleanup, and format method",
                                    shortTitle = "String Methods",
                                    desc = "Hands-on build & master String Methods",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on String Methods solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M2_T3",
                                    num = 3,
                                    title = "Tuples: Data Grouping, Tuple Assignment, Return Values & Composability of Data Structures",
                                    shortTitle = "Tuples",
                                    desc = "Hands-on build & master Tuples",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Tuples solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M2_T4",
                                    num = 4,
                                    title = "Lists: Values, Element Access, Length, Membership, Operations, Slices & Mutability/Deletion",
                                    shortTitle = "Lists",
                                    desc = "Hands-on build & master Lists",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M2_T5",
                                    num = 5,
                                    title = "Objects, References, Aliasing, and Cloning Lists",
                                    shortTitle = "Objects",
                                    desc = "Hands-on build & master Objects",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M2_T6",
                                    num = 6,
                                    title = "Lists and Loops, Parameters, List Methods, Pure Functions vs Modifiers & List Producing Functions",
                                    shortTitle = "Lists and Loops",
                                    desc = "Hands-on build & master Lists and Loops",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Lists and Loops solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M2_T7",
                                    num = 7,
                                    title = "Strings and Lists, list & range, Nested Lists, and Matrices",
                                    shortTitle = "Strings and Lists",
                                    desc = "Hands-on build & master Strings and Lists",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Strings and Lists solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPLC105B_M3",
                                unitNumber = 3,
                                title = "Module 3 • Dictionaries",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPLC105B_M3_T1",
                                    num = 1,
                                    title = "Dictionaries: Operations, Methods, Aliasing, and Copying",
                                    shortTitle = "Dictionaries",
                                    desc = "Hands-on build & master Dictionaries",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Dictionaries solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M3_T2",
                                    num = 2,
                                    title = "NumPy Basics: Shape, Slicing, and Masking",
                                    shortTitle = "NumPy Basics",
                                    desc = "Hands-on build & master NumPy Basics",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on NumPy Basics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M3_T3",
                                    num = 3,
                                    title = "NumPy Advanced: Broadcasting and dtype",
                                    shortTitle = "NumPy Advanced",
                                    desc = "Hands-on build & master NumPy Advanced",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on NumPy Advanced solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M3_T4",
                                    num = 4,
                                    title = "Files: Writing Files, Reading Line-at-a-Time, Converting File to List of Lines & Full File Reading",
                                    shortTitle = "Files",
                                    desc = "Hands-on build & master Files",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Files solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M3_T5",
                                    num = 5,
                                    title = "Working with Binary Files and Directory Operations",
                                    shortTitle = "Working with Binary Files and Directory Operations",
                                    desc = "Hands-on build & master Working with Binary Files and Directory Operations",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Working with Binary Files and Directory Operations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M3_T6",
                                    num = 6,
                                    title = "Fetching Web Content in Python",
                                    shortTitle = "Fetching Web Content in Python",
                                    desc = "Hands-on build & master Fetching Web Content in Python",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Download a web page", "Parse HTML structure & elements", "Extract dynamic links & text data")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPLC105B_M4",
                                unitNumber = 4,
                                title = "Module 4 • Modules",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPLC105B_M4_T1",
                                    num = 1,
                                    title = "Standard Modules: random, time, and math modules",
                                    shortTitle = "Standard Modules",
                                    desc = "Hands-on build & master Standard Modules",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Standard Modules solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M4_T2",
                                    num = 2,
                                    title = "Creating Custom Modules, Namespaces, Scope & Lookup Rules",
                                    shortTitle = "Creating Custom Modules",
                                    desc = "Hands-on build & master Creating Custom Modules",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Creating Custom Modules solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M4_T3",
                                    num = 3,
                                    title = "Attributes, Dot Operator, and Three Import Statement Variants",
                                    shortTitle = "Attributes",
                                    desc = "Hands-on build & master Attributes",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Attributes solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M4_T4",
                                    num = 4,
                                    title = "Mutable vs Immutable Types and Aliasing Revisited",
                                    shortTitle = "Mutable vs Immutable Types and Aliasing Revisited",
                                    desc = "Hands-on build & master Mutable vs Immutable Types and Aliasing Revisited",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Mutable vs Immutable Types and Aliasing Revisited solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M4_T5",
                                    num = 5,
                                    title = "OOP Basics: Classes, Objects, Attributes, and Adding Methods to Classes",
                                    shortTitle = "OOP Basics",
                                    desc = "Hands-on build & master OOP Basics",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M4_T6",
                                    num = 6,
                                    title = "Instances as Arguments/Parameters, Converting Instances to Strings & Instances as Return Values",
                                    shortTitle = "Instances as Arguments/Parameters",
                                    desc = "Hands-on build & master Instances as Arguments/Parameters",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Instances as Arguments/Parameters solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPLC105B_M5",
                                unitNumber = 5,
                                title = "Module 5 • Object-Oriented Programming...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPLC105B_M5_T1",
                                    num = 1,
                                    title = "OOP Advanced: Object Mutability, Sameness, and Copying",
                                    shortTitle = "OOP Advanced",
                                    desc = "Hands-on build & master OOP Advanced",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M5_T2",
                                    num = 2,
                                    title = "Inheritance: Pure Functions, Modifiers, and Generalization",
                                    shortTitle = "Inheritance",
                                    desc = "Hands-on build & master Inheritance",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Inheritance solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M5_T3",
                                    num = 3,
                                    title = "Operator Overloading and Polymorphism",
                                    shortTitle = "Operator Overloading and Polymorphism",
                                    desc = "Hands-on build & master Operator Overloading and Polymorphism",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Operator Overloading and Polymorphism solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M5_T4",
                                    num = 4,
                                    title = "Exceptions: Catching Exceptions",
                                    shortTitle = "Exceptions",
                                    desc = "Hands-on build & master Exceptions",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Exceptions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPLC105B_M5_T5",
                                    num = 5,
                                    title = "Exceptions: Raising Custom Exceptions",
                                    shortTitle = "Exceptions",
                                    desc = "Hands-on build & master Exceptions",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Exceptions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    )
                )
            ),
            SemesterJourney(
                id = "vtu-cse-s2",
                semesterNumber = 2,
                name = "Semester 2 • Computer Science & Engineering",
                isArchived = true,
                progress = 0.9f,
                subjectCount = 6,
                subjects = listOf(
                    SubjectJourney(
                        id = "1BMATS201",
                        name = "Numerical Methods: CSE Stream",
                        iconEmoji = "📐",
                        completedCount = 32,
                        totalCount = 32,
                        units = listOf(
                            UnitJourney(
                                id = "1BMATS201_M1",
                                unitNumber = 1,
                                title = "Module 1 • Introduction to Numerical Me...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS201_M1_T1",
                                    num = 1,
                                    title = "Errors and their computation: Round off error and Truncation error",
                                    shortTitle = "Errors and their computation",
                                    desc = "Hands-on build & master Errors and their computation",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Errors and their computation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M1_T2",
                                    num = 2,
                                    title = "Absolute error, Relative error and Percentage error",
                                    shortTitle = "Absolute error",
                                    desc = "Hands-on build & master Absolute error",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Absolute error solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M1_T3",
                                    num = 3,
                                    title = "Solution of algebraic and transcendental equations: Bisection method",
                                    shortTitle = "Solution of algebraic and transcendental equations",
                                    desc = "Hands-on build & master Solution of algebraic and transcendental equations",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement Bisection & Newton-Raphson solvers", "Calculate root iteration error bounds", "Plot algebraic convergence curves")
                                ),
                                createLesson(
                                    id = "1BMATS201_M1_T4",
                                    num = 4,
                                    title = "Regula-Falsi method",
                                    shortTitle = "Regula-Falsi method",
                                    desc = "Hands-on build & master Regula-Falsi method",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Regula-Falsi method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M1_T5",
                                    num = 5,
                                    title = "Secant method",
                                    shortTitle = "Secant method",
                                    desc = "Hands-on build & master Secant method",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Secant method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M1_T6",
                                    num = 6,
                                    title = "Newton-Raphson method",
                                    shortTitle = "Newton-Raphson method",
                                    desc = "Hands-on build & master Newton-Raphson method",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement Bisection & Newton-Raphson solvers", "Calculate root iteration error bounds", "Plot algebraic convergence curves")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS201_M2",
                                unitNumber = 2,
                                title = "Module 2 • Numerical solutions for syst...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS201_M2_T1",
                                    num = 1,
                                    title = "Norms: Vector norms and Matrix norms - L1, L2 and L_infinity",
                                    shortTitle = "Norms",
                                    desc = "Hands-on build & master Norms",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS201_M2_T2",
                                    num = 2,
                                    title = "Ill conditioned linear system and condition number",
                                    shortTitle = "Ill conditioned linear system and condition number",
                                    desc = "Hands-on build & master Ill conditioned linear system and condition number",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Ill conditioned linear system and condition number solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M2_T3",
                                    num = 3,
                                    title = "Solution of system of linear equations: Gauss Seidel method",
                                    shortTitle = "Solution of system of linear equations",
                                    desc = "Hands-on build & master Solution of system of linear equations",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Solution of system of linear equations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M2_T4",
                                    num = 4,
                                    title = "LU-decomposition method",
                                    shortTitle = "LU-decomposition method",
                                    desc = "Hands-on build & master LU-decomposition method",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on LU-decomposition method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M2_T5",
                                    num = 5,
                                    title = "Eigenvalues and Eigen vectors: Rayleigh power method",
                                    shortTitle = "Eigenvalues and Eigen vectors",
                                    desc = "Hands-on build & master Eigenvalues and Eigen vectors",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                ),
                                createLesson(
                                    id = "1BMATS201_M2_T6",
                                    num = 6,
                                    title = "Jacobi's method",
                                    shortTitle = "Jacobi's method",
                                    desc = "Hands-on build & master Jacobi's method",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Jacobi's method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS201_M3",
                                unitNumber = 3,
                                title = "Module 3 • Interpolation",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS201_M3_T1",
                                    num = 1,
                                    title = "Finite differences and interpolation concepts",
                                    shortTitle = "Finite differences and interpolation concepts",
                                    desc = "Hands-on build & master Finite differences and interpolation concepts",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Finite differences and interpolation concepts solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M3_T2",
                                    num = 2,
                                    title = "Newton Gregory forward difference formula",
                                    shortTitle = "Newton Gregory forward difference formula",
                                    desc = "Hands-on build & master Newton Gregory forward difference formula",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement Bisection & Newton-Raphson solvers", "Calculate root iteration error bounds", "Plot algebraic convergence curves")
                                ),
                                createLesson(
                                    id = "1BMATS201_M3_T3",
                                    num = 3,
                                    title = "Newton Gregory backward difference formula",
                                    shortTitle = "Newton Gregory backward difference formula",
                                    desc = "Hands-on build & master Newton Gregory backward difference formula",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement Bisection & Newton-Raphson solvers", "Calculate root iteration error bounds", "Plot algebraic convergence curves")
                                ),
                                createLesson(
                                    id = "1BMATS201_M3_T4",
                                    num = 4,
                                    title = "Newton's divided difference formula",
                                    shortTitle = "Newton's divided difference formula",
                                    desc = "Hands-on build & master Newton's divided difference formula",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Formulate divide-and-conquer recurrences", "Solve Master Theorem cases 1-3", "Measure recursive call stack depth")
                                ),
                                createLesson(
                                    id = "1BMATS201_M3_T5",
                                    num = 5,
                                    title = "Lagrange interpolation formula",
                                    shortTitle = "Lagrange interpolation formula",
                                    desc = "Hands-on build & master Lagrange interpolation formula",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Lagrange interpolation formula solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M3_T6",
                                    num = 6,
                                    title = "Piecewise interpolation: Linear and quadratic",
                                    shortTitle = "Piecewise interpolation",
                                    desc = "Hands-on build & master Piecewise interpolation",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Piecewise interpolation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS201_M4",
                                unitNumber = 4,
                                title = "Module 4 • Numerical Methods - 1",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS201_M4_T1",
                                    num = 1,
                                    title = "Linear and Bernoulli's differential equations",
                                    shortTitle = "Linear and Bernoulli's differential equations",
                                    desc = "Hands-on build & master Linear and Bernoulli's differential equations",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Linear and Bernoulli's differential equations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M4_T2",
                                    num = 2,
                                    title = "Exact and reducible to exact differential equations with integrating factors using 1/N(My-Nx)",
                                    shortTitle = "Exact and reducible to exact differential equations with integrating factors using 1/N(My-Nx)",
                                    desc = "Hands-on build & master Exact and reducible to exact differential equations with integrating factors using 1/N(My-Nx)",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Exact and reducible to exact differential equations with integrating factors using 1/N(My-Nx) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M4_T3",
                                    num = 3,
                                    title = "Exact and reducible to exact differential equations with integrating factors using -1/M(My-Nx)",
                                    shortTitle = "Exact and reducible to exact differential equations with integrating factors using -1/M(My-Nx)",
                                    desc = "Hands-on build & master Exact and reducible to exact differential equations with integrating factors using -1/M(My-Nx)",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Exact and reducible to exact differential equations with integrating factors using -1/M(My-Nx) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M4_T4",
                                    num = 4,
                                    title = "Homogeneous and non-homogeneous Differential equations of higher order with constant coefficients",
                                    shortTitle = "Homogeneous and non-homogeneous Differential equations of higher order with constant coefficients",
                                    desc = "Hands-on build & master Homogeneous and non-homogeneous Differential equations of higher order with constant coefficients",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Homogeneous and non-homogeneous Differential equations of higher order with constant coefficients solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M4_T5",
                                    num = 5,
                                    title = "Inverse differential operators for e^(ax)",
                                    shortTitle = "Inverse differential operators for e^(ax)",
                                    desc = "Hands-on build & master Inverse differential operators for e^(ax)",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Inverse differential operators for e^(ax) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M4_T6",
                                    num = 6,
                                    title = "Inverse differential operators for sin(ax+b) and cos(ax+b)",
                                    shortTitle = "Inverse differential operators for sin(ax+b) and cos(ax+b)",
                                    desc = "Hands-on build & master Inverse differential operators for sin(ax+b) and cos(ax+b)",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Inverse differential operators for sin(ax+b) and cos(ax+b) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M4_T7",
                                    num = 7,
                                    title = "Inverse differential operators for x^n",
                                    shortTitle = "Inverse differential operators for x^n",
                                    desc = "Hands-on build & master Inverse differential operators for x^n",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Inverse differential operators for x^n solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BMATS201_M5",
                                unitNumber = 5,
                                title = "Module 5 • Numerical Integration and Nu...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BMATS201_M5_T1",
                                    num = 1,
                                    title = "Numerical integration: Trapezoidal rule",
                                    shortTitle = "Numerical integration",
                                    desc = "Hands-on build & master Numerical integration",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Numerical integration solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M5_T2",
                                    num = 2,
                                    title = "Simpson's 1/3rd rule and Simpson's 3/8th rule",
                                    shortTitle = "Simpson's 1/3rd rule and Simpson's 3/8th rule",
                                    desc = "Hands-on build & master Simpson's 1/3rd rule and Simpson's 3/8th rule",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Simpson's 1/3rd rule and Simpson's 3/8th rule solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M5_T3",
                                    num = 3,
                                    title = "Weddle's rule",
                                    shortTitle = "Weddle's rule",
                                    desc = "Hands-on build & master Weddle's rule",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Weddle's rule solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M5_T4",
                                    num = 4,
                                    title = "Numerical solution of ODEs: Taylor's series method",
                                    shortTitle = "Numerical solution of ODEs",
                                    desc = "Hands-on build & master Numerical solution of ODEs",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Numerical solution of ODEs solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M5_T5",
                                    num = 5,
                                    title = "Modified Euler's method",
                                    shortTitle = "Modified Euler's method",
                                    desc = "Hands-on build & master Modified Euler's method",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Construct connected graph networks", "Find Eulerian & Hamiltonian paths", "Apply 4-color graph coloring algorithms")
                                ),
                                createLesson(
                                    id = "1BMATS201_M5_T6",
                                    num = 6,
                                    title = "Runge-Kutta method of fourth order",
                                    shortTitle = "Runge-Kutta method of fourth order",
                                    desc = "Hands-on build & master Runge-Kutta method of fourth order",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Runge-Kutta method of fourth order solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BMATS201_M5_T7",
                                    num = 7,
                                    title = "Milne's predictor-corrector method",
                                    shortTitle = "Milne's predictor-corrector method",
                                    desc = "Hands-on build & master Milne's predictor-corrector method",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Milne's predictor-corrector method solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BPHYS102",
                        name = "Quantum Physics and Applications",
                        iconEmoji = "⚡",
                        completedCount = 35,
                        totalCount = 35,
                        units = listOf(
                            UnitJourney(
                                id = "1BPHYS102_M1",
                                unitNumber = 1,
                                title = "Module 1 • Quantum Mechanics",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPHYS102_M1_T1",
                                    num = 1,
                                    title = "de Broglie Hypothesis & Heisenberg's Uncertainty Principle with Spectral Line Broadening Application",
                                    shortTitle = "de Broglie Hypothesis",
                                    desc = "Hands-on build & master de Broglie Hypothesis",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M1_T2",
                                    num = 2,
                                    title = "Principle of Complementarity, Wave Function & Time-Independent Schrödinger Equation Derivation",
                                    shortTitle = "Principle of Complementarity",
                                    desc = "Hands-on build & master Principle of Complementarity",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M1_T3",
                                    num = 3,
                                    title = "Physical Significance of Wave Function, Born Interpretation & Expectation Values",
                                    shortTitle = "Physical Significance of Wave Function",
                                    desc = "Hands-on build & master Physical Significance of Wave Function",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Physical Significance of Wave Function solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M1_T4",
                                    num = 4,
                                    title = "Eigen Functions, Eigen Values & Particle Inside 1D Infinite Potential Well",
                                    shortTitle = "Eigen Functions",
                                    desc = "Hands-on build & master Eigen Functions",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Eigen Functions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M1_T5",
                                    num = 5,
                                    title = "Role of Higher Dimensions (Qualitative) & Waveforms and Probabilities",
                                    shortTitle = "Role of Higher Dimensions (Qualitative)",
                                    desc = "Hands-on build & master Role of Higher Dimensions (Qualitative)",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Role of Higher Dimensions (Qualitative) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M1_T6",
                                    num = 6,
                                    title = "Particle Inside a Finite Potential Well and Quantum Tunneling",
                                    shortTitle = "Particle Inside a Finite Potential Well and Quantum Tunneling",
                                    desc = "Hands-on build & master Particle Inside a Finite Potential Well and Quantum Tunneling",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Particle Inside a Finite Potential Well and Quantum Tunneling solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M1_T7",
                                    num = 7,
                                    title = "Numerical Problems on Quantum Mechanics",
                                    shortTitle = "Numerical Problems on Quantum Mechanics",
                                    desc = "Hands-on build & master Numerical Problems on Quantum Mechanics",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Numerical Problems on Quantum Mechanics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPHYS102_M2",
                                unitNumber = 2,
                                title = "Module 2 • Electrical Properties of Met...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPHYS102_M2_T1",
                                    num = 1,
                                    title = "Failures of Classical Free Electron Theory, Electron Scattering & Matthiessen's Rule",
                                    shortTitle = "Failures of Classical Free Electron Theory",
                                    desc = "Hands-on build & master Failures of Classical Free Electron Theory",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M2_T2",
                                    num = 2,
                                    title = "Assumptions of Quantum Free Electron Theory & Density of States",
                                    shortTitle = "Assumptions of Quantum Free Electron Theory",
                                    desc = "Hands-on build & master Assumptions of Quantum Free Electron Theory",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Assumptions of Quantum Free Electron Theory solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M2_T3",
                                    num = 3,
                                    title = "Fermi Dirac Statistics, Fermi Energy & Fermi Factor Variation with Temperature/Energy",
                                    shortTitle = "Fermi Dirac Statistics",
                                    desc = "Hands-on build & master Fermi Dirac Statistics",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Fermi Dirac Statistics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M2_T4",
                                    num = 4,
                                    title = "Carrier Concentration in Conductors, Electrical Conductivity & Success of Quantum Free Electron Theory",
                                    shortTitle = "Carrier Concentration in Conductors",
                                    desc = "Hands-on build & master Carrier Concentration in Conductors",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Carrier Concentration in Conductors solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M2_T5",
                                    num = 5,
                                    title = "Derivation of Electron Concentration in Intrinsic Semiconductors & Extrinsic Semiconductor Concentrations",
                                    shortTitle = "Derivation of Electron Concentration in Intrinsic Semiconductors",
                                    desc = "Hands-on build & master Derivation of Electron Concentration in Intrinsic Semiconductors",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Derivation of Electron Concentration in Intrinsic Semiconductors solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M2_T6",
                                    num = 6,
                                    title = "Fermi Level for Intrinsic (Derivation) and Extrinsic Semiconductors & Hall Effect",
                                    shortTitle = "Fermi Level for Intrinsic (Derivation) and Extrinsic Semiconductors",
                                    desc = "Hands-on build & master Fermi Level for Intrinsic (Derivation) and Extrinsic Semiconductors",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Fermi Level for Intrinsic (Derivation) and Extrinsic Semiconductors solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M2_T7",
                                    num = 7,
                                    title = "Numerical Problems on Metals and Semiconductors",
                                    shortTitle = "Numerical Problems on Metals and Semiconductors",
                                    desc = "Hands-on build & master Numerical Problems on Metals and Semiconductors",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Numerical Problems on Metals and Semiconductors solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPHYS102_M3",
                                unitNumber = 3,
                                title = "Module 3 • Superconductivity",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPHYS102_M3_T1",
                                    num = 1,
                                    title = "Zero Resistance State, Persistent Current, Meissner Effect, Critical Temperature & Critical Field",
                                    shortTitle = "Zero Resistance State",
                                    desc = "Hands-on build & master Zero Resistance State",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Zero Resistance State solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M3_T2",
                                    num = 2,
                                    title = "Silsbee Effect: Derivation of Critical Current Expression for Cylindrical Wire via Ampere's Law",
                                    shortTitle = "Silsbee Effect",
                                    desc = "Hands-on build & master Silsbee Effect",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Silsbee Effect solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M3_T3",
                                    num = 3,
                                    title = "Cooper Pairs Formation via Phonons, Two-Fluid Model & BCS Theory Phase Coherent State",
                                    shortTitle = "Cooper Pairs Formation via Phonons",
                                    desc = "Hands-on build & master Cooper Pairs Formation via Phonons",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Cooper Pairs Formation via Phonons solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M3_T4",
                                    num = 4,
                                    title = "Limitations of BCS Theory & Low vs High Electron-Phonon Coupling Systems",
                                    shortTitle = "Limitations of BCS Theory",
                                    desc = "Hands-on build & master Limitations of BCS Theory",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Limitations of BCS Theory solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M3_T5",
                                    num = 5,
                                    title = "Type-I & Type-II Superconductors, Vortices Formation, Upper Critical Field & Andreev Reflection",
                                    shortTitle = "Type-I",
                                    desc = "Hands-on build & master Type-I",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Type-I solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M3_T6",
                                    num = 6,
                                    title = "Josephson Junction, Flux Quantization, DC SQUID & AC SQUID",
                                    shortTitle = "Josephson Junction",
                                    desc = "Hands-on build & master Josephson Junction",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Josephson Junction solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M3_T7",
                                    num = 7,
                                    title = "Numerical Problems on Superconductivity",
                                    shortTitle = "Numerical Problems on Superconductivity",
                                    desc = "Hands-on build & master Numerical Problems on Superconductivity",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Numerical Problems on Superconductivity solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPHYS102_M4",
                                unitNumber = 4,
                                title = "Module 4 • Photonics",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPHYS102_M4_T1",
                                    num = 1,
                                    title = "Radiation-Matter Interaction: Einstein's A & B Coefficients and Energy Density Expression Derivation",
                                    shortTitle = "Radiation-Matter Interaction",
                                    desc = "Hands-on build & master Radiation-Matter Interaction",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Radiation-Matter Interaction solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M4_T2",
                                    num = 2,
                                    title = "Prerequisites for Lasing Action, Types of LASER & Semiconductor Diode LASER",
                                    shortTitle = "Prerequisites for Lasing Action",
                                    desc = "Hands-on build & master Prerequisites for Lasing Action",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Prerequisites for Lasing Action solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M4_T3",
                                    num = 3,
                                    title = "Attenuators for Single Photon Sources & Optical Modulators (Pockel's and Kerr Effects)",
                                    shortTitle = "Attenuators for Single Photon Sources",
                                    desc = "Hands-on build & master Attenuators for Single Photon Sources",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Attenuators for Single Photon Sources solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M4_T4",
                                    num = 4,
                                    title = "Photodetectors: Single Photon Avalanche Diode & Superconducting Nanowire Single Photon Detector",
                                    shortTitle = "Photodetectors",
                                    desc = "Hands-on build & master Photodetectors",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Photodetectors solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M4_T5",
                                    num = 5,
                                    title = "Optical Fiber: Numerical Aperture Derivation, V-Number, Modes & Losses",
                                    shortTitle = "Optical Fiber",
                                    desc = "Hands-on build & master Optical Fiber",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Optical Fiber solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M4_T6",
                                    num = 6,
                                    title = "Mach-Zehnder Interferometer",
                                    shortTitle = "Mach-Zehnder Interferometer",
                                    desc = "Hands-on build & master Mach-Zehnder Interferometer",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Mach-Zehnder Interferometer solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M4_T7",
                                    num = 7,
                                    title = "Numerical Problems on Photonics",
                                    shortTitle = "Numerical Problems on Photonics",
                                    desc = "Hands-on build & master Numerical Problems on Photonics",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Numerical Problems on Photonics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPHYS102_M5",
                                unitNumber = 5,
                                title = "Module 5 • Quantum Computing",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPHYS102_M5_T1",
                                    num = 1,
                                    title = "Moore's Law VLSI Limitations, Classical vs Quantum Computation, Bits, Qubits, Bloch Sphere & Dirac Notation",
                                    shortTitle = "Moore's Law VLSI Limitations",
                                    desc = "Hands-on build & master Moore's Law VLSI Limitations",
                                    estMin = 35,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M5_T2",
                                    num = 2,
                                    title = "Types of Qubits: Superconducting Qubits, Harmonic Oscillator, Anharmonicity Need & Charge Qubit",
                                    shortTitle = "Types of Qubits",
                                    desc = "Hands-on build & master Types of Qubits",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Types of Qubits solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M5_T3",
                                    num = 3,
                                    title = "Quantum Operators and Operations in Matrix Form",
                                    shortTitle = "Quantum Operators and Operations in Matrix Form",
                                    desc = "Hands-on build & master Quantum Operators and Operations in Matrix Form",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Quantum Operators and Operations in Matrix Form solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M5_T4",
                                    num = 4,
                                    title = "Single Qubit Gates: Pauli Gates, Phase Gates (S, T) & Hadamard Gate",
                                    shortTitle = "Single Qubit Gates",
                                    desc = "Hands-on build & master Single Qubit Gates",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Single Qubit Gates solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M5_T5",
                                    num = 5,
                                    title = "Two-Qubit Gates (CNOT), Entanglement & Bell States",
                                    shortTitle = "Two-Qubit Gates (CNOT)",
                                    desc = "Hands-on build & master Two-Qubit Gates (CNOT)",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Two-Qubit Gates (CNOT) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M5_T6",
                                    num = 6,
                                    title = "Predicting Outputs of Single & Two-Qubit Gate Combinations",
                                    shortTitle = "Predicting Outputs of Single",
                                    desc = "Hands-on build & master Predicting Outputs of Single",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Predicting Outputs of Single solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPHYS102_M5_T7",
                                    num = 7,
                                    title = "Numerical Problems on Quantum Computing",
                                    shortTitle = "Numerical Problems on Quantum Computing",
                                    desc = "Hands-on build & master Numerical Problems on Quantum Computing",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Numerical Problems on Quantum Computing solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCEDS203",
                        name = "Computer-Aided Engineering Drawing for CS Stream",
                        iconEmoji = "📚",
                        completedCount = 10,
                        totalCount = 10,
                        units = listOf(
                            UnitJourney(
                                id = "1BCEDS203_M1",
                                unitNumber = 1,
                                title = "Module 1 • Introduction to Computer Aid...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCEDS203_M1_T1",
                                    num = 1,
                                    title = "Drafting Software Basics",
                                    shortTitle = "Drafting Software Basics",
                                    desc = "Hands-on build & master Drafting Software Basics",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Drafting Software Basics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCEDS203_M1_T2",
                                    num = 2,
                                    title = "Orthographic Projections Principles",
                                    shortTitle = "Orthographic Projections Principles",
                                    desc = "Hands-on build & master Orthographic Projections Principles",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Construct connected graph networks", "Find Eulerian & Hamiltonian paths", "Apply 4-color graph coloring algorithms")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCEDS203_M2",
                                unitNumber = 2,
                                title = "Module 2 • Projections of Points and Li...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCEDS203_M2_T1",
                                    num = 1,
                                    title = "Projections of Points in Four Quadrants",
                                    shortTitle = "Projections of Points in Four Quadrants",
                                    desc = "Hands-on build & master Projections of Points in Four Quadrants",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Projections of Points in Four Quadrants solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCEDS203_M2_T2",
                                    num = 2,
                                    title = "Projections of Lines",
                                    shortTitle = "Projections of Lines",
                                    desc = "Hands-on build & master Projections of Lines",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Projections of Lines solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCEDS203_M3",
                                unitNumber = 3,
                                title = "Module 3 • Projections of Plane Surface...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCEDS203_M3_T1",
                                    num = 1,
                                    title = "Projections of Triangular and Square Planes",
                                    shortTitle = "Projections of Triangular and Square Planes",
                                    desc = "Hands-on build & master Projections of Triangular and Square Planes",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Projections of Triangular and Square Planes solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCEDS203_M3_T2",
                                    num = 2,
                                    title = "Projections of Pentagonal and Hexagonal Planes",
                                    shortTitle = "Projections of Pentagonal and Hexagonal Planes",
                                    desc = "Hands-on build & master Projections of Pentagonal and Hexagonal Planes",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Projections of Pentagonal and Hexagonal Planes solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCEDS203_M4",
                                unitNumber = 4,
                                title = "Module 4 • Projections of Solids",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCEDS203_M4_T1",
                                    num = 1,
                                    title = "Projections of Prisms and Pyramids",
                                    shortTitle = "Projections of Prisms and Pyramids",
                                    desc = "Hands-on build & master Projections of Prisms and Pyramids",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Projections of Prisms and Pyramids solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCEDS203_M4_T2",
                                    num = 2,
                                    title = "Projections of Cylinders and Cones",
                                    shortTitle = "Projections of Cylinders and Cones",
                                    desc = "Hands-on build & master Projections of Cylinders and Cones",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Projections of Cylinders and Cones solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BCEDS203_M5",
                                unitNumber = 5,
                                title = "Module 5 • Isometric Projections",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BCEDS203_M5_T1",
                                    num = 1,
                                    title = "Isometric Scale",
                                    shortTitle = "Isometric Scale",
                                    desc = "Hands-on build & master Isometric Scale",
                                    estMin = 20,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Isometric Scale solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BCEDS203_M5_T2",
                                    num = 2,
                                    title = "Isometric Projections of Simple Solids",
                                    shortTitle = "Isometric Projections of Simple Solids",
                                    desc = "Hands-on build & master Isometric Projections of Simple Solids",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Isometric Projections of Simple Solids solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BESC204D",
                        name = "Intro to Mechanical Engineering",
                        iconEmoji = "📚",
                        completedCount = 12,
                        totalCount = 12,
                        units = listOf(
                            UnitJourney(
                                id = "1BESC204D_M1",
                                unitNumber = 1,
                                title = "Module 1 • Machine Tools and Metal Cutt...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BESC204D_M1_T1",
                                    num = 1,
                                    title = "Basic Machine Tools Classification",
                                    shortTitle = "Basic Machine Tools Classification",
                                    desc = "Hands-on build & master Basic Machine Tools Classification",
                                    estMin = 20,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BESC204D_M1_T2",
                                    num = 2,
                                    title = "Primary and Secondary Motions",
                                    shortTitle = "Primary and Secondary Motions",
                                    desc = "Hands-on build & master Primary and Secondary Motions",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Primary and Secondary Motions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BESC204D_M1_T3",
                                    num = 3,
                                    title = "Single Point Cutting Tool Geometry",
                                    shortTitle = "Single Point Cutting Tool Geometry",
                                    desc = "Hands-on build & master Single Point Cutting Tool Geometry",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Single Point Cutting Tool Geometry solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BESC204D_M2",
                                unitNumber = 2,
                                title = "Module 2 • Machining Operations",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BESC204D_M2_T1",
                                    num = 1,
                                    title = "Lathe Machine and Operations",
                                    shortTitle = "Lathe Machine and Operations",
                                    desc = "Hands-on build & master Lathe Machine and Operations",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Lathe Machine and Operations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BESC204D_M2_T2",
                                    num = 2,
                                    title = "Drilling Machine and Operations",
                                    shortTitle = "Drilling Machine and Operations",
                                    desc = "Hands-on build & master Drilling Machine and Operations",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Drilling Machine and Operations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BESC204D_M2_T3",
                                    num = 3,
                                    title = "Milling Machine Operations",
                                    shortTitle = "Milling Machine Operations",
                                    desc = "Hands-on build & master Milling Machine Operations",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Milling Machine Operations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BESC204D_M3",
                                unitNumber = 3,
                                title = "Module 3 • Advanced Machining and Broac...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BESC204D_M3_T1",
                                    num = 1,
                                    title = "Shaping and Planning Machines",
                                    shortTitle = "Shaping and Planning Machines",
                                    desc = "Hands-on build & master Shaping and Planning Machines",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Shaping and Planning Machines solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BESC204D_M3_T2",
                                    num = 2,
                                    title = "Broaching Machine Principles",
                                    shortTitle = "Broaching Machine Principles",
                                    desc = "Hands-on build & master Broaching Machine Principles",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BESC204D_M4",
                                unitNumber = 4,
                                title = "Module 4 • Thermodynamics and IC Engine...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BESC204D_M4_T1",
                                    num = 1,
                                    title = "Fundamentals of Thermodynamics",
                                    shortTitle = "Fundamentals of Thermodynamics",
                                    desc = "Hands-on build & master Fundamentals of Thermodynamics",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Fundamentals of Thermodynamics solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BESC204D_M4_T2",
                                    num = 2,
                                    title = "Working Principles of IC Engines",
                                    shortTitle = "Working Principles of IC Engines",
                                    desc = "Hands-on build & master Working Principles of IC Engines",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BESC204D_M5",
                                unitNumber = 5,
                                title = "Module 5 • Emerging Technologies in Man...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BESC204D_M5_T1",
                                    num = 1,
                                    title = "Introduction to CNC Machines",
                                    shortTitle = "Introduction to CNC Machines",
                                    desc = "Hands-on build & master Introduction to CNC Machines",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Introduction to CNC Machines solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BESC204D_M5_T2",
                                    num = 2,
                                    title = "Basics of Robotics in Manufacturing",
                                    shortTitle = "Basics of Robotics in Manufacturing",
                                    desc = "Hands-on build & master Basics of Robotics in Manufacturing",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Basics of Robotics in Manufacturing solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BEIT205",
                        name = "Programming in C",
                        iconEmoji = "☕",
                        completedCount = 34,
                        totalCount = 34,
                        units = listOf(
                            UnitJourney(
                                id = "1BEIT205_M1",
                                unitNumber = 1,
                                title = "Module 1 • Introduction to Computing",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BEIT205_M1_T1",
                                    num = 1,
                                    title = "Introduction to Computing: Computer Languages, Creating and Running Programs, System Development",
                                    shortTitle = "Introduction to Computing",
                                    desc = "Hands-on build & master Introduction to Computing",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Introduction to Computing solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M1_T2",
                                    num = 2,
                                    title = "Overview of C: History, Features, Structured Programming, Compilers vs. Interpreters",
                                    shortTitle = "Overview of C",
                                    desc = "Hands-on build & master Overview of C",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Overview of C solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M1_T3",
                                    num = 3,
                                    title = "Form of a C Program, Library, Linking, Separate Compilation, Compiling a C Program, C's Memory Map",
                                    shortTitle = "Form of a C Program",
                                    desc = "Hands-on build & master Form of a C Program",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Form of a C Program solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M1_T4",
                                    num = 4,
                                    title = "Basic Data Types, Modifying Basic Types, Identifier Names, Variables",
                                    shortTitle = "Basic Data Types",
                                    desc = "Hands-on build & master Basic Data Types",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Basic Data Types solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M1_T5",
                                    num = 5,
                                    title = "The Four C Scopes, Type Qualifiers, Storage Class Specifiers",
                                    shortTitle = "The Four C Scopes",
                                    desc = "Hands-on build & master The Four C Scopes",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BEIT205_M1_T6",
                                    num = 6,
                                    title = "Variable Initializations and Constants",
                                    shortTitle = "Variable Initializations and Constants",
                                    desc = "Hands-on build & master Variable Initializations and Constants",
                                    estMin = 20,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Variable Initializations and Constants solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M1_T7",
                                    num = 7,
                                    title = "Operators and Expressions",
                                    shortTitle = "Operators and Expressions",
                                    desc = "Hands-on build & master Operators and Expressions",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Operators and Expressions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BEIT205_M2",
                                unitNumber = 2,
                                title = "Module 2 • Console I/O and Statements",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BEIT205_M2_T1",
                                    num = 1,
                                    title = "Console I/O: Reading and Writing Characters & Strings",
                                    shortTitle = "Console I/O",
                                    desc = "Hands-on build & master Console I/O",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Console I/O solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M2_T2",
                                    num = 2,
                                    title = "Formatted Console I/O: printf() and scanf()",
                                    shortTitle = "Formatted Console I/O",
                                    desc = "Hands-on build & master Formatted Console I/O",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Formatted Console I/O solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M2_T3",
                                    num = 3,
                                    title = "True and False in C",
                                    shortTitle = "True and False in C",
                                    desc = "Hands-on build & master True and False in C",
                                    estMin = 15,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on True and False in C solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M2_T4",
                                    num = 4,
                                    title = "Selection Statements (if, switch)",
                                    shortTitle = "Selection Statements (if",
                                    desc = "Hands-on build & master Selection Statements (if",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Selection Statements (if solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M2_T5",
                                    num = 5,
                                    title = "Iteration Statements (for, while, do-while)",
                                    shortTitle = "Iteration Statements (for",
                                    desc = "Hands-on build & master Iteration Statements (for",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Iteration Statements (for solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M2_T6",
                                    num = 6,
                                    title = "Jump Statements (break, continue, goto, return)",
                                    shortTitle = "Jump Statements (break",
                                    desc = "Hands-on build & master Jump Statements (break",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Jump Statements (break solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M2_T7",
                                    num = 7,
                                    title = "Expression Statements and Block Statements",
                                    shortTitle = "Expression Statements and Block Statements",
                                    desc = "Hands-on build & master Expression Statements and Block Statements",
                                    estMin = 20,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Expression Statements and Block Statements solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BEIT205_M3",
                                unitNumber = 3,
                                title = "Module 3 • Arrays",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BEIT205_M3_T1",
                                    num = 1,
                                    title = "Single-Dimension Arrays & Array Initialization",
                                    shortTitle = "Single-Dimension Arrays",
                                    desc = "Hands-on build & master Single-Dimension Arrays",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Single-Dimension Arrays solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M3_T2",
                                    num = 2,
                                    title = "Generating a Pointer to an Array & Passing Single-Dimension Arrays to Functions",
                                    shortTitle = "Generating a Pointer to an Array",
                                    desc = "Hands-on build & master Generating a Pointer to an Array",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Generating a Pointer to an Array solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M3_T3",
                                    num = 3,
                                    title = "Strings and String Manipulation Concepts",
                                    shortTitle = "Strings and String Manipulation Concepts",
                                    desc = "Hands-on build & master Strings and String Manipulation Concepts",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "1BEIT205_M3_T4",
                                    num = 4,
                                    title = "Two-Dimensional Arrays, Multidimensional Arrays, and Variable-Length Arrays",
                                    shortTitle = "Two-Dimensional Arrays",
                                    desc = "Hands-on build & master Two-Dimensional Arrays",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Two-Dimensional Arrays solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M3_T5",
                                    num = 5,
                                    title = "Introduction to Pointers: Pointer Variables & Pointer Operators",
                                    shortTitle = "Introduction to Pointers",
                                    desc = "Hands-on build & master Introduction to Pointers",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Introduction to Pointers solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M3_T6",
                                    num = 6,
                                    title = "Pointer Expressions and Pointers and Arrays",
                                    shortTitle = "Pointer Expressions and Pointers and Arrays",
                                    desc = "Hands-on build & master Pointer Expressions and Pointers and Arrays",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Pointer Expressions and Pointers and Arrays solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M3_T7",
                                    num = 7,
                                    title = "Multiple Indirection & Initializing Pointers",
                                    shortTitle = "Multiple Indirection",
                                    desc = "Hands-on build & master Multiple Indirection",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BEIT205_M4",
                                unitNumber = 4,
                                title = "Module 4 • Functions and Advanced Point...",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BEIT205_M4_T1",
                                    num = 1,
                                    title = "General Form of a Function, Scope of a Function, and Function Arguments",
                                    shortTitle = "General Form of a Function",
                                    desc = "Hands-on build & master General Form of a Function",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on General Form of a Function solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M4_T2",
                                    num = 2,
                                    title = "argc and argv - Arguments to main() & What Does main() Return?",
                                    shortTitle = "argc and argv - Arguments to main()",
                                    desc = "Hands-on build & master argc and argv - Arguments to main()",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on argc and argv - Arguments to main() solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M4_T3",
                                    num = 3,
                                    title = "The return Statement and Function Prototypes",
                                    shortTitle = "The return Statement and Function Prototypes",
                                    desc = "Hands-on build & master The return Statement and Function Prototypes",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on The return Statement and Function Prototypes solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M4_T4",
                                    num = 4,
                                    title = "Recursion",
                                    shortTitle = "Recursion",
                                    desc = "Hands-on build & master Recursion",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Recursion solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M4_T5",
                                    num = 5,
                                    title = "Declaring Variable Length Parameter Declarations & The inline Keyword",
                                    shortTitle = "Declaring Variable Length Parameter Declarations",
                                    desc = "Hands-on build & master Declaring Variable Length Parameter Declarations",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Declaring Variable Length Parameter Declarations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M4_T6",
                                    num = 6,
                                    title = "Pointers to Functions",
                                    shortTitle = "Pointers to Functions",
                                    desc = "Hands-on build & master Pointers to Functions",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Pointers to Functions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M4_T7",
                                    num = 7,
                                    title = "C's Dynamic Allocation Functions (malloc, calloc, realloc, free)",
                                    shortTitle = "C's Dynamic Allocation Functions (malloc",
                                    desc = "Hands-on build & master C's Dynamic Allocation Functions (malloc",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on C's Dynamic Allocation Functions (malloc solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BEIT205_M5",
                                unitNumber = 5,
                                title = "Module 5 • Structures",
                                isExpanded = false,
                                lessons = listOf(
                                createLesson(
                                    id = "1BEIT205_M5_T1",
                                    num = 1,
                                    title = "Structures: Basics & Arrays of Structures",
                                    shortTitle = "Structures",
                                    desc = "Hands-on build & master Structures",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Structures solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M5_T2",
                                    num = 2,
                                    title = "Passing Structures to Functions & Structure Pointers",
                                    shortTitle = "Passing Structures to Functions",
                                    desc = "Hands-on build & master Passing Structures to Functions",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Passing Structures to Functions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M5_T3",
                                    num = 3,
                                    title = "Arrays and Structures within Structures",
                                    shortTitle = "Arrays and Structures within Structures",
                                    desc = "Hands-on build & master Arrays and Structures within Structures",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Arrays and Structures within Structures solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M5_T4",
                                    num = 4,
                                    title = "Unions and Bit-Fields",
                                    shortTitle = "Unions and Bit-Fields",
                                    desc = "Hands-on build & master Unions and Bit-Fields",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Unions and Bit-Fields solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M5_T5",
                                    num = 5,
                                    title = "Enumerations",
                                    shortTitle = "Enumerations",
                                    desc = "Hands-on build & master Enumerations",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Enumerations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BEIT205_M5_T6",
                                    num = 6,
                                    title = "Using sizeof to Ensure Portability & typedef Keyword",
                                    shortTitle = "Using sizeof to Ensure Portability",
                                    desc = "Hands-on build & master Using sizeof to Ensure Portability",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Using sizeof to Ensure Portability solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BPOPL107",
                        name = "C Programming Lab",
                        iconEmoji = "☕",
                        completedCount = 14,
                        totalCount = 14,
                        units = listOf(
                            UnitJourney(
                                id = "1BPOPL107_M1",
                                unitNumber = 1,
                                title = "Module 1 • PART-A",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPOPL107_M1_T1",
                                    num = 1,
                                    title = "Straight-Line Distance Calculation Between Two Coordinates on a 2D Plane",
                                    shortTitle = "Straight-Line Distance Calculation Between Two Coordinates on a 2D Plane",
                                    desc = "Hands-on build & master Straight-Line Distance Calculation Between Two Coordinates on a 2D Plane",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Straight-Line Distance Calculation Between Two Coordinates on a 2D Plane solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M1_T2",
                                    num = 2,
                                    title = "Student Marks Evaluation and Grade Assignment Using Efficient Control Structures",
                                    shortTitle = "Student Marks Evaluation and Grade Assignment Using Efficient Control Structures",
                                    desc = "Hands-on build & master Student Marks Evaluation and Grade Assignment Using Efficient Control Structures",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Student Marks Evaluation and Grade Assignment Using Efficient Control Structures solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M1_T3",
                                    num = 3,
                                    title = "Stored KYC Records Matching and Unique Identification Verification",
                                    shortTitle = "Stored KYC Records Matching and Unique Identification Verification",
                                    desc = "Hands-on build & master Stored KYC Records Matching and Unique Identification Verification",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Stored KYC Records Matching and Unique Identification Verification solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M1_T4",
                                    num = 4,
                                    title = "Quadratic Equation Roots Calculation and Nature Classification",
                                    shortTitle = "Quadratic Equation Roots Calculation and Nature Classification",
                                    desc = "Hands-on build & master Quadratic Equation Roots Calculation and Nature Classification",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M1_T5",
                                    num = 5,
                                    title = "Approximation of sin(x) Using Series Expansion Method for Robotic Arm Rotation",
                                    shortTitle = "Approximation of sin(x) Using Series Expansion Method for Robotic Arm Rotation",
                                    desc = "Hands-on build & master Approximation of sin(x) Using Series Expansion Method for Robotic Arm Rotation",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Approximation of sin(x) Using Series Expansion Method for Robotic Arm Rotation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M1_T6",
                                    num = 6,
                                    title = "Course Description Keyword Search Using String Functions",
                                    shortTitle = "Course Description Keyword Search Using String Functions",
                                    desc = "Hands-on build & master Course Description Keyword Search Using String Functions",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M1_T7",
                                    num = 7,
                                    title = "Three-Subject Marks Pass/Fail Verification and Average Calculation Function",
                                    shortTitle = "Three-Subject Marks Pass/Fail Verification and Average Calculation Function",
                                    desc = "Hands-on build & master Three-Subject Marks Pass/Fail Verification and Average Calculation Function",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Three-Subject Marks Pass/Fail Verification and Average Calculation Function solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M1_T8",
                                    num = 8,
                                    title = "Swapping Account Balances in an ATM System Using Pointer Functions",
                                    shortTitle = "Swapping Account Balances in an ATM System Using Pointer Functions",
                                    desc = "Hands-on build & master Swapping Account Balances in an ATM System Using Pointer Functions",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Swapping Account Balances in an ATM System Using Pointer Functions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "1BPOPL107_M2",
                                unitNumber = 2,
                                title = "Module 2 • PART-B",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "1BPOPL107_M2_T1",
                                    num = 1,
                                    title = "Digital Bookshelf Searching System Using Unique Book IDs in Sorted Arrays",
                                    shortTitle = "Digital Bookshelf Searching System Using Unique Book IDs in Sorted Arrays",
                                    desc = "Hands-on build & master Digital Bookshelf Searching System Using Unique Book IDs in Sorted Arrays",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Digital Bookshelf Searching System Using Unique Book IDs in Sorted Arrays solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M2_T2",
                                    num = 2,
                                    title = "100-Meter Race Scores Sorting in Descending Order for Result Sheet Generation",
                                    shortTitle = "100-Meter Race Scores Sorting in Descending Order for Result Sheet Generation",
                                    desc = "Hands-on build & master 100-Meter Race Scores Sorting in Descending Order for Result Sheet Generation",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on 100-Meter Race Scores Sorting in Descending Order for Result Sheet Generation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M2_T3",
                                    num = 3,
                                    title = "Warehouse Product Units and Unit Revenue Matrix Dataset Combination for Branch Total Revenue",
                                    shortTitle = "Warehouse Product Units and Unit Revenue Matrix Dataset Combination for Branch Total Revenue",
                                    desc = "Hands-on build & master Warehouse Product Units and Unit Revenue Matrix Dataset Combination for Branch Total Revenue",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Warehouse Product Units and Unit Revenue Matrix Dataset Combination for Branch Total Revenue solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M2_T4",
                                    num = 4,
                                    title = "Mobile Contact Manager Name Concatenation and Length Validation Without Built-in String Functions",
                                    shortTitle = "Mobile Contact Manager Name Concatenation and Length Validation Without Built-in String Functions",
                                    desc = "Hands-on build & master Mobile Contact Manager Name Concatenation and Length Validation Without Built-in String Functions",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Mobile Contact Manager Name Concatenation and Length Validation Without Built-in String Functions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M2_T5",
                                    num = 5,
                                    title = "Currency Exchange Simulation and Actual Update Implementation Using Call by Value and Call by Reference",
                                    shortTitle = "Currency Exchange Simulation and Actual Update Implementation Using Call by Value and Call by Reference",
                                    desc = "Hands-on build & master Currency Exchange Simulation and Actual Update Implementation Using Call by Value and Call by Reference",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Currency Exchange Simulation and Actual Update Implementation Using Call by Value and Call by Reference solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "1BPOPL107_M2_T6",
                                    num = 6,
                                    title = "Library Book Catalog System Using Custom Structures (Title, Author, Year)",
                                    shortTitle = "Library Book Catalog System Using Custom Structures (Title",
                                    desc = "Hands-on build & master Library Book Catalog System Using Custom Structures (Title",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Library Book Catalog System Using Custom Structures (Title solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    )
                )
            ),
            SemesterJourney(
                id = "vtu-cse-s3",
                semesterNumber = 3,
                name = "Semester 3 • Computer Science & Engineering",
                isArchived = true,
                progress = 0.75f,
                subjectCount = 9,
                subjects = listOf(
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcs301-2025",
                        name = "Probability, Distributions and Statistics (1BCS301)",
                        iconEmoji = "📚",
                        completedCount = 4,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs301-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Probability",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs301-2025-m1-t1",
                                    num = 1,
                                    title = "Joint Probability & Mathematical Expectation",
                                    shortTitle = "Joint Probability",
                                    desc = "Hands-on build & master Joint Probability",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Joint Probability solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs301-2025-m1-t2",
                                    num = 2,
                                    title = "Discrete & Continuous Distributions",
                                    shortTitle = "Discrete",
                                    desc = "Hands-on build & master Discrete",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Discrete solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs301-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Statistical Inference",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs301-2025-m2-t1",
                                    num = 1,
                                    title = "Estimation Theory & Central Limit Theorem",
                                    shortTitle = "Estimation Theory",
                                    desc = "Hands-on build & master Estimation Theory",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Estimation Theory solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs301-2025-m2-t2",
                                    num = 2,
                                    title = "Hypothesis Testing (t-test, Chi-Square)",
                                    shortTitle = "Hypothesis Testing (t-test",
                                    desc = "Hands-on build & master Hypothesis Testing (t-test",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.QUIZ,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Hypothesis Testing (t-test solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcs302-2025",
                        name = "Object Oriented Programming with Java (1BCS302)",
                        iconEmoji = "☕",
                        completedCount = 4,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs302-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Java Basics",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs302-2025-m1-t1",
                                    num = 1,
                                    title = "Classes, Objects, Constructors & Methods",
                                    shortTitle = "Classes",
                                    desc = "Hands-on build & master Classes",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Design object-oriented class structures", "Instantiate custom objects & constructors", "Manage JVM heap memory & references")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs302-2025-m1-t2",
                                    num = 2,
                                    title = "Inheritance, Polymorphism & Abstraction",
                                    shortTitle = "Inheritance",
                                    desc = "Hands-on build & master Inheritance",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Inheritance solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs302-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Exceptions",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs302-2025-m2-t1",
                                    num = 1,
                                    title = "Exception Handling (Try-Catch-Finally)",
                                    shortTitle = "Exception Handling (Try-Catch-Finally)",
                                    desc = "Hands-on build & master Exception Handling (Try-Catch-Finally)",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Exception Handling (Try-Catch-Finally) solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs302-2025-m2-t2",
                                    num = 2,
                                    title = "Multi-threading & Package Management",
                                    shortTitle = "Multi-threading",
                                    desc = "Hands-on build & master Multi-threading",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Multi-threading solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcs303-2025",
                        name = "Digital Design and Computer Organization (1BCS303)",
                        iconEmoji = "⚙️",
                        completedCount = 4,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs303-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Combinational Logic",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs303-2025-m1-t1",
                                    num = 1,
                                    title = "K-Maps & Quine-McCluskey Simplification",
                                    shortTitle = "K-Maps",
                                    desc = "Hands-on build & master K-Maps",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on K-Maps solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs303-2025-m1-t2",
                                    num = 2,
                                    title = "Multiplexers, Decoders & Adders",
                                    shortTitle = "Multiplexers",
                                    desc = "Hands-on build & master Multiplexers",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs303-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • CPU Structure",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs303-2025-m2-t1",
                                    num = 1,
                                    title = "Instruction Cycles & Addressing Modes",
                                    shortTitle = "Instruction Cycles",
                                    desc = "Hands-on build & master Instruction Cycles",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Configure 8051 internal RAM banks", "Program SFR control registers", "Execute direct memory instructions")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs303-2025-m2-t2",
                                    num = 2,
                                    title = "Cache Memory Design & Direct Memory Access",
                                    shortTitle = "Cache Memory Design",
                                    desc = "Hands-on build & master Cache Memory Design",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Cache Memory Design solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcs304-2025",
                        name = "Operating Systems (1BCS304)",
                        iconEmoji = "📚",
                        completedCount = 4,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs304-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Process Management",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs304-2025-m1-t1",
                                    num = 1,
                                    title = "Scheduling Algorithms (FCFS, Round Robin, SRTF)",
                                    shortTitle = "Scheduling Algorithms (FCFS",
                                    desc = "Hands-on build & master Scheduling Algorithms (FCFS",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Scheduling Algorithms (FCFS solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs304-2025-m1-t2",
                                    num = 2,
                                    title = "Process Synchronization & Semaphores",
                                    shortTitle = "Process Synchronization",
                                    desc = "Hands-on build & master Process Synchronization",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Process Synchronization solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs304-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Memory",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs304-2025-m2-t1",
                                    num = 1,
                                    title = "Paging, Segmentation & Page Replacement",
                                    shortTitle = "Paging",
                                    desc = "Hands-on build & master Paging",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Paging solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs304-2025-m2-t2",
                                    num = 2,
                                    title = "Disk Scheduling (SSTF, SCAN, LOOK)",
                                    shortTitle = "Disk Scheduling (SSTF",
                                    desc = "Hands-on build & master Disk Scheduling (SSTF",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Disk Scheduling (SSTF solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcs305-2025",
                        name = "Data Structures and Applications (1BCS305)",
                        iconEmoji = "🌳",
                        completedCount = 4,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs305-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Linear Data Structures",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs305-2025-m1-t1",
                                    num = 1,
                                    title = "Stacks, Queues & Circular Queues",
                                    shortTitle = "Stacks",
                                    desc = "Hands-on build & master Stacks",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Stacks solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs305-2025-m1-t2",
                                    num = 2,
                                    title = "Singly & Doubly Linked Lists",
                                    shortTitle = "Singly",
                                    desc = "Hands-on build & master Singly",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Singly solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s3-1bcs305-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Non-Linear Structures",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcs305-2025-m2-t1",
                                    num = 1,
                                    title = "Binary Search Trees & Heap structures",
                                    shortTitle = "Binary Search Trees",
                                    desc = "Hands-on build & master Binary Search Trees",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Binary Search Trees solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s3-1bcs305-2025-m2-t2",
                                    num = 2,
                                    title = "Graph Traversal (BFS & DFS)",
                                    shortTitle = "Graph Traversal (BFS",
                                    desc = "Hands-on build & master Graph Traversal (BFS",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Construct connected graph networks", "Find Eulerian & Hamiltonian paths", "Apply 4-color graph coloring algorithms")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcsl306-2025",
                        name = "Data Structures Laboratory (1BCSL306)",
                        iconEmoji = "🌳",
                        completedCount = 1,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcsl306-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Stack",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcsl306-2025-m1-t1",
                                    num = 1,
                                    title = "Infix to Postfix & Evaluation",
                                    shortTitle = "Infix to Postfix",
                                    desc = "Hands-on build & master Infix to Postfix",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Infix to Postfix solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcsl307a-2025",
                        name = "Project Management with Git (1BCSL307A)",
                        iconEmoji = "🎓",
                        completedCount = 1,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcsl307a-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Git",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcsl307a-2025-m1-t1",
                                    num = 1,
                                    title = "Branching, Merging & Conflict Resolution",
                                    shortTitle = "Branching",
                                    desc = "Hands-on build & master Branching",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Branching solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bcp308-2025",
                        name = "Community / Societal Project (1BCP308)",
                        iconEmoji = "🎓",
                        completedCount = 1,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bcp308-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Community Problem Solving",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bcp308-2025-m1-t1",
                                    num = 1,
                                    title = "Problem Identification & Proposal",
                                    shortTitle = "Problem Identification",
                                    desc = "Hands-on build & master Problem Identification",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s3-1bnss309-2025",
                        name = "National Service Scheme NSS (1BNSS309)",
                        iconEmoji = "📚",
                        completedCount = 1,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s3-1bnss309-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Social Responsibility",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s3-1bnss309-2025-m1-t1",
                                    num = 1,
                                    title = "NSS Orientation & Community Initiatives",
                                    shortTitle = "NSS Orientation",
                                    desc = "Hands-on build & master NSS Orientation",
                                    estMin = 20,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on NSS Orientation solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    )
                )
            ),
            SemesterJourney(
                id = "vtu-cse-s4",
                semesterNumber = 4,
                name = "Semester 4 • Computer Science & Engineering",
                isArchived = false,
                progress = 0.4f,
                subjectCount = 8,
                subjects = listOf(
                    SubjectJourney(
                        id = "vtu-cse-s4-1bcs401-2025",
                        name = "Discrete Mathematics and Graph Theory (1BCS401)",
                        iconEmoji = "📐",
                        completedCount = 2,
                        totalCount = 3,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bcs401-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Mathematical Logic",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcs401-2025-m1-t1",
                                    num = 1,
                                    title = "Propositional Logic & Quantifiers",
                                    shortTitle = "Propositional Logic",
                                    desc = "Hands-on build & master Propositional Logic",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                ),
                                createLesson(
                                    id = "vtu-cse-s4-1bcs401-2025-m1-t2",
                                    num = 2,
                                    title = "Relation Properties & Principle of Inclusion-Exclusion",
                                    shortTitle = "Relation Properties",
                                    desc = "Hands-on build & master Relation Properties",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.COMPLETED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s4-1bcs401-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Graph Theory",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcs401-2025-m2-t1",
                                    num = 1,
                                    title = "Euler, Hamiltonian Graphs & Colorings",
                                    shortTitle = "Euler",
                                    desc = "Hands-on build & master Euler",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.CURRENT,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Construct connected graph networks", "Find Eulerian & Hamiltonian paths", "Apply 4-color graph coloring algorithms")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s4-1bce402-2025",
                        name = "Microcontrollers (1BCE402)",
                        iconEmoji = "⚙️",
                        completedCount = 0,
                        totalCount = 3,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bce402-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • 8051 Architecture",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bce402-2025-m1-t1",
                                    num = 1,
                                    title = "Register Banks & Addressing Modes",
                                    shortTitle = "Register Banks",
                                    desc = "Hands-on build & master Register Banks",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Configure 8051 internal RAM banks", "Program SFR control registers", "Execute direct memory instructions")
                                ),
                                createLesson(
                                    id = "vtu-cse-s4-1bce402-2025-m1-t2",
                                    num = 2,
                                    title = "8051 Instruction Set & Assembly coding",
                                    shortTitle = "8051 Instruction Set",
                                    desc = "Hands-on build & master 8051 Instruction Set",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Configure 8051 internal RAM banks", "Program SFR control registers", "Execute direct memory instructions")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s4-1bce402-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Peripheral Interfacing",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bce402-2025-m2-t1",
                                    num = 1,
                                    title = "Interfacing with LCD & Stepper Motors",
                                    shortTitle = "Interfacing with LCD",
                                    desc = "Hands-on build & master Interfacing with LCD",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.LAB,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Interface 16x2 LCD display pins", "Control stepper motor step sequences", "Program hardware timer interrupts")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s4-1bcs403-2025",
                        name = "Computer Networks (1BCS403)",
                        iconEmoji = "🌐",
                        completedCount = 0,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bcs403-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Network Layer Addressing",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcs403-2025-m1-t1",
                                    num = 1,
                                    title = "IPv4 / IPv6 Subnetting & CIDR",
                                    shortTitle = "IPv4 / IPv6 Subnetting",
                                    desc = "Hands-on build & master IPv4 / IPv6 Subnetting",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "vtu-cse-s4-1bcs403-2025-m1-t2",
                                    num = 2,
                                    title = "Dijkstra & Distance Vector Routing",
                                    shortTitle = "Dijkstra",
                                    desc = "Hands-on build & master Dijkstra",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s4-1bcs403-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Transport",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcs403-2025-m2-t1",
                                    num = 1,
                                    title = "TCP 3-Way Handshake & Congestion Control",
                                    shortTitle = "TCP 3-Way Handshake",
                                    desc = "Hands-on build & master TCP 3-Way Handshake",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on TCP 3-Way Handshake solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s4-1bcs403-2025-m2-t2",
                                    num = 2,
                                    title = "Application Protocols (DNS, HTTP, SMTP)",
                                    shortTitle = "Application Protocols (DNS",
                                    desc = "Hands-on build & master Application Protocols (DNS",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Download a web page", "Parse HTML structure & elements", "Extract dynamic links & text data")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s4-1bcs404-2025",
                        name = "Design and Analysis of Algorithms (1BCS404)",
                        iconEmoji = "🌳",
                        completedCount = 0,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bcs404-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Divide",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcs404-2025-m1-t1",
                                    num = 1,
                                    title = "Recurrence Relations & Master Theorem",
                                    shortTitle = "Recurrence Relations",
                                    desc = "Hands-on build & master Recurrence Relations",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Formulate divide-and-conquer recurrences", "Solve Master Theorem cases 1-3", "Measure recursive call stack depth")
                                ),
                                createLesson(
                                    id = "vtu-cse-s4-1bcs404-2025-m1-t2",
                                    num = 2,
                                    title = "Kruskal, Prim's MST & Dijkstra's Algorithms",
                                    shortTitle = "Kruskal",
                                    desc = "Hands-on build & master Kruskal",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s4-1bcs404-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Dynamic Programming",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcs404-2025-m2-t1",
                                    num = 1,
                                    title = "0/1 Knapsack & Floyd-Warshall Algorithms",
                                    shortTitle = "0/1 Knapsack",
                                    desc = "Hands-on build & master 0/1 Knapsack",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build 0/1 Knapsack DP memoization matrix", "Run Floyd-Warshall all-pairs shortest path", "Optimize space complexity to O(W)")
                                ),
                                createLesson(
                                    id = "vtu-cse-s4-1bcs404-2025-m2-t2",
                                    num = 2,
                                    title = "N-Queens & State Space Trees",
                                    shortTitle = "N-Queens",
                                    desc = "Hands-on build & master N-Queens",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on N-Queens solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s4-1bcsl405-2025",
                        name = "Algorithms Laboratory (1BCSL405)",
                        iconEmoji = "🌳",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bcsl405-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Algorithm Programming Exerci...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcsl405-2025-m1-t1",
                                    num = 1,
                                    title = "MST & Shortest Path implementations",
                                    shortTitle = "MST",
                                    desc = "Hands-on build & master MST",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on MST solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s4-1bxxl406-2025",
                        name = "Ability Enhancement Course Laboratory (1BXXL406)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bxxl406-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Technical",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bxxl406-2025-m1-t1",
                                    num = 1,
                                    title = "Technical Presentations & Reports",
                                    shortTitle = "Technical Presentations",
                                    desc = "Hands-on build & master Technical Presentations",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Technical Presentations solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s4-1bcs407-2025",
                        name = "Biology for Computer Engineers (1BCS407)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 2,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bcs407-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Biomolecules",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bcs407-2025-m1-t1",
                                    num = 1,
                                    title = "DNA/RNA Data Storage Principles",
                                    shortTitle = "DNA/RNA Data Storage Principles",
                                    desc = "Hands-on build & master DNA/RNA Data Storage Principles",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "vtu-cse-s4-1bcs407-2025-m1-t2",
                                    num = 2,
                                    title = "Artificial Neural Nets vs Biological Neurons",
                                    shortTitle = "Artificial Neural Nets vs Biological Neurons",
                                    desc = "Hands-on build & master Artificial Neural Nets vs Biological Neurons",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s4-1bep408-2025",
                        name = "Environmental Science Project (1BEP408)",
                        iconEmoji = "🎓",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s4-1bep408-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Ecological Balance",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s4-1bep408-2025-m1-t1",
                                    num = 1,
                                    title = "Sustainable Systems Design & Planning",
                                    shortTitle = "Sustainable Systems Design",
                                    desc = "Hands-on build & master Sustainable Systems Design",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Sustainable Systems Design solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    )
                )
            ),
            SemesterJourney(
                id = "vtu-cse-s5",
                semesterNumber = 5,
                name = "Semester 5 • Computer Science & Engineering",
                isArchived = false,
                progress = 0f,
                subjectCount = 8,
                subjects = listOf(
                    SubjectJourney(
                        id = "vtu-cse-s5-1bcs501-2025",
                        name = "Software Engineering and Project Management (1BCS501)",
                        iconEmoji = "🚀",
                        completedCount = 0,
                        totalCount = 3,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1bcs501-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Agile Processes",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bcs501-2025-m1-t1",
                                    num = 1,
                                    title = "Agile Scrum Methodology & Team Roles",
                                    shortTitle = "Agile Scrum Methodology",
                                    desc = "Hands-on build & master Agile Scrum Methodology",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Agile Scrum Methodology solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1bcs501-2025-m1-t2",
                                    num = 2,
                                    title = "SRS & Requirements Modeling",
                                    shortTitle = "SRS",
                                    desc = "Hands-on build & master SRS",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on SRS solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s5-1bcs501-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Testing",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bcs501-2025-m2-t1",
                                    num = 1,
                                    title = "White-Box, Black-Box & Boundary Analysis",
                                    shortTitle = "White-Box",
                                    desc = "Hands-on build & master White-Box",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on White-Box solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s5-1bcm502-2025",
                        name = "Database Management Systems (1BCM502)",
                        iconEmoji = "🗄️",
                        completedCount = 0,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1bcm502-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • ER Models",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bcm502-2025-m1-t1",
                                    num = 1,
                                    title = "ER Diagrams & Relational Mapping",
                                    shortTitle = "ER Diagrams",
                                    desc = "Hands-on build & master ER Diagrams",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on ER Diagrams solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1bcm502-2025-m1-t2",
                                    num = 2,
                                    title = "Complex SQL Queries & Subqueries",
                                    shortTitle = "Complex SQL Queries",
                                    desc = "Hands-on build & master Complex SQL Queries",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Complex SQL Queries solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s5-1bcm502-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Normalization",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bcm502-2025-m2-t1",
                                    num = 1,
                                    title = "1NF, 2NF, 3NF & BCNF Normalization",
                                    shortTitle = "1NF",
                                    desc = "Hands-on build & master 1NF",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on 1NF solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1bcm502-2025-m2-t2",
                                    num = 2,
                                    title = "ACID Properties & Concurrency Control",
                                    shortTitle = "ACID Properties",
                                    desc = "Hands-on build & master ACID Properties",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s5-1bcs503-2025",
                        name = "Theory of Computation (1BCS503)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1bcs503-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Finite Automata",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bcs503-2025-m1-t1",
                                    num = 1,
                                    title = "DFA & NFA Design & Equivalences",
                                    shortTitle = "DFA",
                                    desc = "Hands-on build & master DFA",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on DFA solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1bcs503-2025-m1-t2",
                                    num = 2,
                                    title = "Regular Expressions & Pumping Lemma",
                                    shortTitle = "Regular Expressions",
                                    desc = "Hands-on build & master Regular Expressions",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Regular Expressions solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s5-1bcs503-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Pushdown Automata",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bcs503-2025-m2-t1",
                                    num = 1,
                                    title = "CFG, PDA & Context-Free Languages",
                                    shortTitle = "CFG",
                                    desc = "Hands-on build & master CFG",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on CFG solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1bcs503-2025-m2-t2",
                                    num = 2,
                                    title = "Turing Machines & Decidability Limits",
                                    shortTitle = "Turing Machines",
                                    desc = "Hands-on build & master Turing Machines",
                                    estMin = 50,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Turing Machines solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s5-1bce504-2025",
                        name = "Machine Learning (1BCE504)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1bce504-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Supervised ML Models",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bce504-2025-m1-t1",
                                    num = 1,
                                    title = "Linear, Logistic Regression & Decision Trees",
                                    shortTitle = "Linear",
                                    desc = "Hands-on build & master Linear",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Linear solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1bce504-2025-m1-t2",
                                    num = 2,
                                    title = "Naive Bayes & Support Vector Machines (SVM)",
                                    shortTitle = "Naive Bayes",
                                    desc = "Hands-on build & master Naive Bayes",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build weighted graph routing tables", "Execute Dijkstra shortest path finder", "Handle link-state packet updates")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s5-1bce504-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Unsupervised Learning",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bce504-2025-m2-t1",
                                    num = 1,
                                    title = "K-Means Clustering & PCA Dimensions",
                                    shortTitle = "K-Means Clustering",
                                    desc = "Hands-on build & master K-Means Clustering",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on K-Means Clustering solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1bce504-2025-m2-t2",
                                    num = 2,
                                    title = "Perceptron & Backpropagation Networks",
                                    shortTitle = "Perceptron",
                                    desc = "Hands-on build & master Perceptron",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s5-1bxx505-2025",
                        name = "Professional Elective Course-I (1BXX505)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1bxx505-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Advanced Subject Elective",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bxx505-2025-m1-t1",
                                    num = 1,
                                    title = "Foundational Elective Concepts",
                                    shortTitle = "Foundational Elective Concepts",
                                    desc = "Hands-on build & master Foundational Elective Concepts",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Foundational Elective Concepts solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s5-1brm506-2025",
                        name = "Research Methodology and IPR (1BRM506)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 2,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1brm506-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Research Methodology",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1brm506-2025-m1-t1",
                                    num = 1,
                                    title = "Literature Review & Problem Formulation",
                                    shortTitle = "Literature Review",
                                    desc = "Hands-on build & master Literature Review",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Literature Review solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s5-1brm506-2025-m1-t2",
                                    num = 2,
                                    title = "IPR, Copyrights & Filing Patents",
                                    shortTitle = "IPR",
                                    desc = "Hands-on build & master IPR",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s5-1bcel507-2025",
                        name = "Machine Learning Laboratory (1BCEL507)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1bcel507-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Machine Learning Lab Exercis...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bcel507-2025-m1-t1",
                                    num = 1,
                                    title = "Implementing Supervised & Unsupervised Models in Python",
                                    shortTitle = "Implementing Supervised",
                                    desc = "Hands-on build & master Implementing Supervised",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Implementing Supervised solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s5-1bce508-2025",
                        name = "Hackathon-Based Project (1BCE508)",
                        iconEmoji = "🎓",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s5-1bce508-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Product Development Lifecycl...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s5-1bce508-2025-m1-t1",
                                    num = 1,
                                    title = "MVP Scoping & Development",
                                    shortTitle = "MVP Scoping",
                                    desc = "Hands-on build & master MVP Scoping",
                                    estMin = 40,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on MVP Scoping solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    )
                )
            ),
            SemesterJourney(
                id = "vtu-cse-s6",
                semesterNumber = 6,
                name = "Semester 6 • Computer Science & Engineering",
                isArchived = false,
                progress = 0f,
                subjectCount = 8,
                subjects = listOf(
                    SubjectJourney(
                        id = "vtu-cse-s6-1bcs601-2025",
                        name = "Advanced Java Programming (1BCS601)",
                        iconEmoji = "☕",
                        completedCount = 0,
                        totalCount = 3,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bcs601-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Collections",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bcs601-2025-m1-t1",
                                    num = 1,
                                    title = "Java Collection Framework & Generics",
                                    shortTitle = "Java Collection Framework",
                                    desc = "Hands-on build & master Java Collection Framework",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.CODING,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Java Collection Framework solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s6-1bcs601-2025-m1-t2",
                                    num = 2,
                                    title = "Thread Synchronization & Executor Services",
                                    shortTitle = "Thread Synchronization",
                                    desc = "Hands-on build & master Thread Synchronization",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Thread Synchronization solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s6-1bcs601-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Web Frameworks",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bcs601-2025-m2-t1",
                                    num = 1,
                                    title = "JDBC Database Integration & Servlets",
                                    shortTitle = "JDBC Database Integration",
                                    desc = "Hands-on build & master JDBC Database Integration",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on JDBC Database Integration solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s6-1bcs602-2025",
                        name = "Cryptography and Network Security (1BCS602)",
                        iconEmoji = "🌐",
                        completedCount = 0,
                        totalCount = 3,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bcs602-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Symmetric",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bcs602-2025-m1-t1",
                                    num = 1,
                                    title = "DES, AES & Stream Ciphers",
                                    shortTitle = "DES",
                                    desc = "Hands-on build & master DES",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "vtu-cse-s6-1bcs602-2025-m1-t2",
                                    num = 2,
                                    title = "RSA & Diffie-Hellman Key Exchange",
                                    shortTitle = "RSA",
                                    desc = "Hands-on build & master RSA",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on RSA solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s6-1bcs602-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Web Security Protocols",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bcs602-2025-m2-t1",
                                    num = 1,
                                    title = "IPsec, SSL/TLS, Firewalls & IDS",
                                    shortTitle = "IPsec",
                                    desc = "Hands-on build & master IPsec",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s6-1bce603-2025",
                        name = "Advanced Computer Architecture (1BCE603)",
                        iconEmoji = "⚙️",
                        completedCount = 0,
                        totalCount = 2,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bce603-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Instruction-Level Parallelis...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bce603-2025-m1-t1",
                                    num = 1,
                                    title = "Pipelining, Hazards & Branch Prediction",
                                    shortTitle = "Pipelining",
                                    desc = "Hands-on build & master Pipelining",
                                    estMin = 35,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s6-1bce603-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Multiprocessors",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bce603-2025-m2-t1",
                                    num = 1,
                                    title = "Shared Memory Systems & MESI Protocol",
                                    shortTitle = "Shared Memory Systems",
                                    desc = "Hands-on build & master Shared Memory Systems",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Shared Memory Systems solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s6-1bcs604-2025",
                        name = "Internet of Things (1BCS604)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 2,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bcs604-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • IoT Architecture",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bcs604-2025-m1-t1",
                                    num = 1,
                                    title = "Arduino/Raspberry Pi Hardware Interfacing",
                                    shortTitle = "Arduino/Raspberry Pi Hardware Interfacing",
                                    desc = "Hands-on build & master Arduino/Raspberry Pi Hardware Interfacing",
                                    estMin = 30,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.LAB,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Interface 16x2 LCD display pins", "Control stepper motor step sequences", "Program hardware timer interrupts")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s6-1bcs604-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Protocols",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bcs604-2025-m2-t1",
                                    num = 1,
                                    title = "MQTT, CoAP, Node-RED & Cloud Storage",
                                    shortTitle = "MQTT",
                                    desc = "Hands-on build & master MQTT",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on MQTT solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s6-1bxx605-2025",
                        name = "Professional Elective Courses-II (1BXX605)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bxx605-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Advanced Elective Subject",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bxx605-2025-m1-t1",
                                    num = 1,
                                    title = "Advanced Elective Theory Applications",
                                    shortTitle = "Advanced Elective Theory Applications",
                                    desc = "Hands-on build & master Advanced Elective Theory Applications",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Advanced Elective Theory Applications solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s6-1bcsl606-2025",
                        name = "IoT Laboratory (1BCSL606)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bcsl606-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • IoT Lab Practical Sessions",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bcsl606-2025-m1-t1",
                                    num = 1,
                                    title = "Interfacing ESP32, MQTT Broker & LEDs",
                                    shortTitle = "Interfacing ESP32",
                                    desc = "Hands-on build & master Interfacing ESP32",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.LAB,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Interface 16x2 LCD display pins", "Control stepper motor step sequences", "Program hardware timer interrupts")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s6-1bxxl607-2025",
                        name = "Ability Enhancement Course Laboratory (1BXXL607)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bxxl607-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Placement",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bxxl607-2025-m1-t1",
                                    num = 1,
                                    title = "Technical Interview Practice & Mock GDs",
                                    shortTitle = "Technical Interview Practice",
                                    desc = "Hands-on build & master Technical Interview Practice",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Technical Interview Practice solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s6-1bce608-2025",
                        name = "Capstone Project - Phase I (1BCE608)",
                        iconEmoji = "🎓",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s6-1bce608-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Literature Review",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s6-1bce608-2025-m1-t1",
                                    num = 1,
                                    title = "Problem Statement & Proposed System Design",
                                    shortTitle = "Problem Statement",
                                    desc = "Hands-on build & master Problem Statement",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Build logical truth tables", "Validate boolean proposition trees", "Simplify quantifier equations")
                                )
                                )
                            )
                        )
                    )
                )
            ),
            SemesterJourney(
                id = "vtu-cse-s7",
                semesterNumber = 7,
                name = "Semester 7 • Computer Science & Engineering",
                isArchived = false,
                progress = 0f,
                subjectCount = 5,
                subjects = listOf(
                    SubjectJourney(
                        id = "vtu-cse-s7-1bis701-2025",
                        name = "High Performance Computing (1BIS701)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 4,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s7-1bis701-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Multiprocessing",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s7-1bis701-2025-m1-t1",
                                    num = 1,
                                    title = "Symmetric Multiprocessing & Thread Safety",
                                    shortTitle = "Symmetric Multiprocessing",
                                    desc = "Hands-on build & master Symmetric Multiprocessing",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                ),
                                createLesson(
                                    id = "vtu-cse-s7-1bis701-2025-m1-t2",
                                    num = 2,
                                    title = "Parallel Programming loops in OpenMP",
                                    shortTitle = "Parallel Programming loops in OpenMP",
                                    desc = "Hands-on build & master Parallel Programming loops in OpenMP",
                                    estMin = 45,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Parallel Programming loops in OpenMP solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            ),
                            UnitJourney(
                                id = "vtu-cse-s7-1bis701-2025-m2",
                                unitNumber = 2,
                                title = "Module 2 • Message Passing (MPI)",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s7-1bis701-2025-m2-t1",
                                    num = 1,
                                    title = "MPI Communication & Broadcasts",
                                    shortTitle = "MPI Communication",
                                    desc = "Hands-on build & master MPI Communication",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on MPI Communication solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                ),
                                createLesson(
                                    id = "vtu-cse-s7-1bis701-2025-m2-t2",
                                    num = 2,
                                    title = "CUDA Memory & Block Dimensions",
                                    shortTitle = "CUDA Memory",
                                    desc = "Hands-on build & master CUDA Memory",
                                    estMin = 50,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.LOCKED,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on CUDA Memory solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s7-1bxx702-2025",
                        name = "Professional Elective Course-III (1BXX702)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s7-1bxx702-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Advanced Track Elective",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s7-1bxx702-2025-m1-t1",
                                    num = 1,
                                    title = "Elective Deep-Dive & Architectures",
                                    shortTitle = "Elective Deep-Dive",
                                    desc = "Hands-on build & master Elective Deep-Dive",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Elective Deep-Dive solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s7-1bxx703-2025",
                        name = "Professional Elective Course-IV (1BXX703)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s7-1bxx703-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Specialization Track Electiv...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s7-1bxx703-2025-m1-t1",
                                    num = 1,
                                    title = "Specialized Theoretical Frameworks",
                                    shortTitle = "Specialized Theoretical Frameworks",
                                    desc = "Hands-on build & master Specialized Theoretical Frameworks",
                                    estMin = 35,
                                    diff = Difficulty.MEDIUM,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Specialized Theoretical Frameworks solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s7-1bxx704-2025",
                        name = "Open Elective Course-I (1BXX704)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s7-1bxx704-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Cross-department Open Course",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s7-1bxx704-2025-m1-t1",
                                    num = 1,
                                    title = "Interdisciplinary Concepts & Studies",
                                    shortTitle = "Interdisciplinary Concepts",
                                    desc = "Hands-on build & master Interdisciplinary Concepts",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s7-1bce705-2025",
                        name = "Capstone Project - Phase-II (1BCE705)",
                        iconEmoji = "🎓",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s7-1bce705-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • System Implementation",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s7-1bce705-2025-m1-t1",
                                    num = 1,
                                    title = "Full Coding, Integration & Performance Testing",
                                    shortTitle = "Full Coding",
                                    desc = "Hands-on build & master Full Coding",
                                    estMin = 55,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.QUIZ,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Full Coding solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    )
                )
            ),
            SemesterJourney(
                id = "vtu-cse-s8",
                semesterNumber = 8,
                name = "Semester 8 • Computer Science & Engineering",
                isArchived = false,
                progress = 0f,
                subjectCount = 3,
                subjects = listOf(
                    SubjectJourney(
                        id = "vtu-cse-s8-1bxx801-2025",
                        name = "Professional Elective-V (1BXX801)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s8-1bxx801-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Advanced Research Elective",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s8-1bxx801-2025-m1-t1",
                                    num = 1,
                                    title = "Emerging Technologies Research Papers",
                                    shortTitle = "Emerging Technologies Research Papers",
                                    desc = "Hands-on build & master Emerging Technologies Research Papers",
                                    estMin = 40,
                                    diff = Difficulty.HARD,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Emerging Technologies Research Papers solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s8-1bxx802-2025",
                        name = "Open Elective-II (1BXX802)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s8-1bxx802-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Multidisciplinary Open Elect...",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s8-1bxx802-2025-m1-t1",
                                    num = 1,
                                    title = "Global Engineering & Market Trends",
                                    shortTitle = "Global Engineering",
                                    desc = "Hands-on build & master Global Engineering",
                                    estMin = 30,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Implement hands-on Global Engineering solver", "Build working test bench & cases", "Optimize execution performance & memory")
                                )
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "vtu-cse-s8-1bxx803-2025",
                        name = "Internship (1BXX803)",
                        iconEmoji = "📚",
                        completedCount = 0,
                        totalCount = 1,
                        units = listOf(
                            UnitJourney(
                                id = "vtu-cse-s8-1bxx803-2025-m1",
                                unitNumber = 1,
                                title = "Module 1 • Industry Practice",
                                isExpanded = true,
                                lessons = listOf(
                                createLesson(
                                    id = "vtu-cse-s8-1bxx803-2025-m1-t1",
                                    num = 1,
                                    title = "Internship Weekly Journal & Case Studies",
                                    shortTitle = "Internship Weekly Journal",
                                    desc = "Hands-on build & master Internship Weekly Journal",
                                    estMin = 25,
                                    diff = Difficulty.EASY,
                                    status = LessonStatus.AVAILABLE,
                                    category = LessonCategory.THEORY,
                                    reqs = listOf("Core Prerequisites"),
                                    points = listOf("Calculate CIDR subnet masks", "Allocate IPv4 / IPv6 host addresses", "Configure network gateway routes")
                                )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private fun createLesson(
        id: String,
        num: Int,
        title: String,
        shortTitle: String,
        desc: String,
        estMin: Int,
        diff: Difficulty,
        status: LessonStatus,
        category: LessonCategory,
        reqs: List<String>,
        points: List<String>
    ): LessonNode {
        return LessonNode(
            id = id,
            lessonNumber = num,
            title = title,
            shortTitle = shortTitle,
            description = desc,
            status = status,
            category = category,
            durationMinutes = estMin,
            xpReward = estMin * 2,
            difficulty = diff,
            prerequisites = reqs.map { Prerequisite(it, true) },
            learnPoints = points
        )
    }
}
