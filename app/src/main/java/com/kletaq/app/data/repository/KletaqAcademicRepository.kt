package com.kletaq.app.data.repository

import com.kletaq.app.features.journey.components.Difficulty
import com.kletaq.app.features.journey.components.LessonCategory
import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.LessonStatus
import com.kletaq.app.features.journey.components.Prerequisite
import com.kletaq.app.features.journey.components.SemesterJourney
import com.kletaq.app.features.journey.components.SubjectJourney
import com.kletaq.app.features.journey.components.UnitJourney
import com.kletaq.app.domain.progression.LessonStatusEvaluator
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

/**
 * Single source of truth with flexible unlocking rules:
 * 1. Lesson 1 of EVERY unit is ALWAYS unlocked.
 * 2. Archived semesters are fully unlocked.
 * 3. Backlog subjects are fully unlocked.
 * 4. Advanced topics in a unit are locked based on prerequisites.
 */
object KletaqAcademicRepository {

    fun getOrGenerateTopicQuest(
        topicId: String,
        topicTitle: String,
        subjectName: String,
        semesterName: String
    ): com.kletaq.app.data.model.DynamicTopicQuest {
        return com.kletaq.app.domain.quest.AcademicQuestGenerator.generateQuestForTopic(
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

        val excludedCodes = setOf("1BNSS309", "1BCP308", "1BCSL307A")

        val evaluatedSemesters = visibleBaseSemesters.map { rawSem ->
            val semNum = rawSem.semesterNumber
            val isPriorSemester = semNum < effectiveUserSem
            val isLocked = semNum > effectiveUserSem && !completedSemesters.contains(semNum)

            val filteredSubjects = rawSem.subjects.filterNot { sub ->
                val idUp = sub.id.uppercase()
                val nameUp = sub.name.uppercase()
                excludedCodes.any { code -> idUp.contains(code) || nameUp.contains(code) }
            }

            val evaluatedSubjects = filteredSubjects.map { rawSubject ->
                val isBacklog = backlogSubjects.any {
                    it.equals(rawSubject.name, ignoreCase = true) ||
                    it.equals(rawSubject.id, ignoreCase = true) ||
                    rawSubject.name.lowercase().contains(it.lowercase())
                }

                val (evaluatedUnits, _) = LessonStatusEvaluator.evaluateSubjectUnits(
                    semesterId = rawSem.id,
                    subjectId = rawSubject.id,
                    units = rawSubject.units,
                    completedTopicKeys = completedTopicKeys,
                    isPriorSemester = isPriorSemester,
                    isBacklog = isBacklog,
                    isSemesterLocked = isLocked,
                    hasSetCurrentInSubject = false
                )

                val subjectCompletedCount = evaluatedUnits.sumOf { unit -> unit.lessons.count { it.status == LessonStatus.COMPLETED } }
                val subjectTotalCount = evaluatedUnits.sumOf { unit -> unit.lessons.size }

                val evaluatedSubject = rawSubject.copy(
                    completedCount = subjectCompletedCount,
                    totalCount = subjectTotalCount,
                    isBacklog = isBacklog,
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

    private val baseSemesters: List<SemesterJourney> by lazy { buildSemesters() }

    fun preloadAsync() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            baseSemesters
        }
    }

    suspend fun getSemestersForUserAsync(
        userSemesterNumber: Int = 1,
        completedSemesters: List<Int> = emptyList(),
        completedTopicKeys: Set<String> = emptySet(),
        backlogSubjects: List<String> = emptyList()
    ): List<SemesterJourney> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        getSemestersForUser(
            userSemesterNumber = userSemesterNumber,
            completedSemesters = completedSemesters,
            completedTopicKeys = completedTopicKeys,
            backlogSubjects = backlogSubjects
        )
    }

    fun getSemesters(): List<SemesterJourney> = baseSemesters

    private fun buildSemesters(): List<SemesterJourney> {
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
                id = "vtu-ise-s3",
                semesterNumber = 3,
                name = "Semester 3 • Information Science & Engineering",
                isArchived = true,
                progress = 0.75f,
                subjectCount = 9,
                subjects = listOf(
                    SubjectJourney(
                        id = "1BMATCS301",
                        name = "Probability, Distributions and Statistics (1BMATCS301)",
                        iconEmoji = "📊",
                        completedCount = 7,
                        totalCount = 30,
                        units = listOf(
                            UnitJourney(
                                id = "1BMATCS301_M1",
                                unitNumber = 1,
                                title = "Module 1 • Modular Arithmetic",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BMATCS301_M1_T1", 1, "Introduction to Congruences & Linear Congruences", "Congruences", "Hands-on build & master Introduction to Congruences & Linear Congruences", 40, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Master congruences & linear equations", "Solve modular arithmetic problems", "Apply modular theory to cryptosystems")),
                                    createLesson("1BMATCS301_M1_T2", 2, "The Remainder Theorem & Solving Polynomials", "Remainder Theorem", "Hands-on build & master The Remainder Theorem", 45, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Understand Chinese Remainder Theorem", "Solve polynomial congruences", "Verify numerical solutions")),
                                    createLesson("1BMATCS301_M1_T3", 3, "Linear Diophantine Equations", "Diophantine Eq", "Hands-on build & master Linear Diophantine Equations", 40, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Formulate Diophantine equations", "Compute Euclidean GCD algorithms", "Derive integral solutions")),
                                    createLesson("1BMATCS301_M1_T4", 4, "System of Linear Congruences", "System Congruences", "Hands-on build & master System of Linear Congruences", 45, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Solve simultaneous congruences", "Apply matrix reduction methods", "Compute inverse modular matrices")),
                                    createLesson("1BMATCS301_M1_T5", 5, "Euler's Theorem", "Euler's Theorem", "Hands-on build & master Euler's Theorem", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Compute Euler phi function φ(n)", "Apply Euler's theorem to powers", "Simplify large modular exponents")),
                                    createLesson("1BMATCS301_M1_T6", 6, "Wilson Theorem & Fermat's Little Theorem", "Wilson & Fermat", "Hands-on build & master Wilson & Fermat Theorems", 40, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Verify Fermat's Little Theorem", "Apply Wilson's primality test", "Solve primality check problems")),
                                    createLesson("1BMATCS301_M1_T7", 7, "RSA Cryptographic Algorithm (Applications)", "RSA Algorithm", "Hands-on build & master RSA Cryptographic Algorithm", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Generate RSA public and private keys", "Encrypt and decrypt secret messages", "Analyze RSA security & prime generation"))
                                )
                            ),
                            UnitJourney(
                                id = "1BMATCS301_M2",
                                unitNumber = 2,
                                title = "Module 2 • Statistics",
                                isExpanded = false,
                                lessons = listOf(
                                    createLesson("1BMATCS301_M2_T1", 1, "Principles of Least Squares", "Least Squares", "Hands-on build & master Principles of Least Squares", 35, Difficulty.MEDIUM, LessonStatus.AVAILABLE, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Derive normal equations for least squares", "Fit straight lines & curves", "Minimize sum of squared errors")),
                                    createLesson("1BMATCS301_M2_T2", 2, "Curve Fitting: y = a + bx & y = a + bx + cx²", "Curve Fitting", "Hands-on build & master Curve Fitting", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Fit linear and parabolic equations", "Solve system of 3 normal equations", "Compute goodness of fit")),
                                    createLesson("1BMATCS301_M2_T3", 3, "Curve Fitting: y = ax^b (Power Fit)", "Power Fit", "Hands-on build & master Power Fit", 40, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Apply log transformations to power curves", "Compute power fit coefficients", "Plot regression curves")),
                                    createLesson("1BMATCS301_M2_T4", 4, "Correlation & Coefficient of Correlation", "Correlation", "Hands-on build & master Correlation", 35, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Compute Karl Pearson correlation coefficient", "Interpret positive & negative correlation", "Evaluate scatter diagrams")),
                                    createLesson("1BMATCS301_M2_T5", 5, "Lines of Regression & Angle between them", "Regression Lines", "Hands-on build & master Lines of Regression", 40, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Derive regression lines of X on Y and Y on X", "Calculate angle between regression lines", "Estimate missing values")),
                                    createLesson("1BMATCS301_M2_T6", 6, "Rank Correlation", "Rank Correlation", "Hands-on build & master Rank Correlation", 35, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Compute Spearman's rank correlation", "Handle tied ranks in data", "Compare qualitative rankings"))
                                )
                            ),
                            UnitJourney(
                                id = "1BMATCS301_M3",
                                unitNumber = 3,
                                title = "Module 3 • Probability Distributions",
                                isExpanded = false,
                                lessons = listOf(
                                    createLesson("1BMATCS301_M3_T1", 1, "Review of Basic Probability Theory", "Basic Probability", "Hands-on build & master Basic Probability", 30, Difficulty.EASY, LessonStatus.AVAILABLE, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Review sample spaces & events", "Apply addition & multiplication rules", "Calculate conditional probabilities")),
                                    createLesson("1BMATCS301_M3_T2", 2, "Random Variables (Discrete & Continuous), PMF & PDF", "Random Variables", "Hands-on build & master Random Variables", 40, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Define discrete & continuous random variables", "Construct PMF & PDF tables", "Verify probability axioms")),
                                    createLesson("1BMATCS301_M3_T3", 3, "Mathematical Expectation: Mean & Variance", "Expectation & Variance", "Hands-on build & master Mathematical Expectation", 35, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Compute E(X) and E(X^2)", "Calculate variance and standard deviation", "Apply properties of expectation")),
                                    createLesson("1BMATCS301_M3_T4", 4, "Binomial Distribution (Derivations & Problems)", "Binomial Dist", "Hands-on build & master Binomial Distribution", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Derive mean & variance of Binomial distribution", "Solve real-world binomial problems", "Compute cumulative probabilities")),
                                    createLesson("1BMATCS301_M3_T5", 5, "Poisson Distribution (Derivations & Problems)", "Poisson Dist", "Hands-on build & master Poisson Distribution", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Derive Poisson distribution as limit of Binomial", "Calculate Poisson probabilities for rare events", "Determine mean & variance")),
                                    createLesson("1BMATCS301_M3_T6", 6, "Normal Distribution (Problems)", "Normal Dist", "Hands-on build & master Normal Distribution", 35, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Standardize normal variable to Z", "Use standard normal area tables", "Solve engineering application problems"))
                                )
                            ),
                            UnitJourney(
                                id = "1BMATCS301_M4",
                                unitNumber = 4,
                                title = "Module 4 • Joint Probability & Markov Chains",
                                isExpanded = false,
                                lessons = listOf(
                                    createLesson("1BMATCS301_M4_T1", 1, "Joint Distribution for Two Discrete RVs", "Joint Distribution", "Hands-on build & master Joint Distribution", 40, Difficulty.HARD, LessonStatus.AVAILABLE, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Construct joint PMF matrices", "Find marginal distributions of X and Y", "Check independence of random variables")),
                                    createLesson("1BMATCS301_M4_T2", 2, "Expectation, Covariance & Correlation", "Covariance", "Hands-on build & master Covariance & Correlation", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Compute joint expectation E(XY)", "Calculate covariance Cov(X,Y)", "Determine correlation coefficient ρ_XY")),
                                    createLesson("1BMATCS301_M4_T3", 3, "Stochastic Processes & Probability Vectors", "Stochastic Proc", "Hands-on build & master Stochastic Processes", 35, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Define stochastic process states", "Construct initial probability vectors", "Analyze state transitions")),
                                    createLesson("1BMATCS301_M4_T4", 4, "Stochastic Matrices & Regular Stochastic Matrices", "Stochastic Matrix", "Hands-on build & master Stochastic Matrices", 40, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Verify stochastic matrix row sums", "Test matrix regularity P^k > 0", "Compute higher matrix powers")),
                                    createLesson("1BMATCS301_M4_T5", 5, "Markov Chains & Higher Transition Probabilities", "Markov Chains", "Hands-on build & master Markov Chains", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Draw Markov chain state diagrams", "Compute n-step transition probabilities", "Solve transition matrix problems")),
                                    createLesson("1BMATCS301_M4_T6", 6, "Stationary Distribution of Regular Markov Chains", "Stationary Dist", "Hands-on build & master Stationary Distribution", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Solve steady-state vector equation V P = V", "Find long-term state probabilities", "Apply to PageRank & queueing models"))
                                )
                            ),
                            UnitJourney(
                                id = "1BMATCS301_M5",
                                unitNumber = 5,
                                title = "Module 5 • Statistical Inference",
                                isExpanded = false,
                                lessons = listOf(
                                    createLesson("1BMATCS301_M5_T1", 1, "Hypothesis Testing & Confidence Intervals", "Hypothesis Testing", "Hands-on build & master Hypothesis Testing", 40, Difficulty.HARD, LessonStatus.AVAILABLE, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Formulate Null (H0) and Alternative (H1) hypotheses", "Set significance level α and critical regions", "Construct confidence intervals for mean")),
                                    createLesson("1BMATCS301_M5_T2", 2, "Large Sample Tests", "Large Samples", "Hands-on build & master Large Sample Tests", 40, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Perform Z-tests for single mean & proportion", "Compare two sample means and proportions", "Make statistical decision conclusions")),
                                    createLesson("1BMATCS301_M5_T3", 3, "Small Sample Tests & Student's t-test", "Student's t-test", "Hands-on build & master Student's t-test", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Apply t-distribution for small sample n < 30", "Test significance of sample mean", "Perform paired t-test for dependent samples")),
                                    createLesson("1BMATCS301_M5_T4", 4, "Chi-Square Test", "Chi-Square Test", "Hands-on build & master Chi-Square Test", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Compute Chi-square goodness of fit χ²", "Test independence of attributes in contingency tables", "Calculate degrees of freedom")),
                                    createLesson("1BMATCS301_M5_T5", 5, "Central Limit Theorem (Applications)", "Central Limit Thm", "Hands-on build & master Central Limit Theorem", 30, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Understand CLT convergence to normal distribution", "Apply CLT to sample mean distributions", "Solve practical engineering sampling problems"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCS302",
                        name = "Object Oriented Programming with Java (1BCS302)",
                        iconEmoji = "☕",
                        completedCount = 8,
                        totalCount = 34,
                        units = listOf(
                            UnitJourney(
                                id = "1BCS302_M1",
                                unitNumber = 1,
                                title = "Module 1 • Java Basics",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BCS302_M1_T1", 1, "OOP Principles (Abstraction, Encapsulation, Inheritance, Polymorphism)", "OOP Principles", "Master OOP Principles", 30, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Understand 4 pillars of OOP", "Design clean class boundaries", "Apply modular software engineering")),
                                    createLesson("1BCS302_M1_T2", 2, "Lexical Issues (Identifiers, Literals, Comments, Separators, Keywords)", "Lexical Issues", "Master Lexical Issues", 25, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Identify valid Java identifiers", "Use literals and comments effectively", "Avoid reserved keyword conflicts")),
                                    createLesson("1BCS302_M1_T3", 3, "Primitive Data Types, Variables, Type Conversion & Casting", "Data Types", "Master Primitive Data Types", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Understand byte, short, int, long, float, double", "Perform automatic & explicit type casting", "Prevent numeric overflow errors")),
                                    createLesson("1BCS302_M1_T4", 4, "Arrays (Declaration, Creation, Usage)", "Arrays", "Master Java Arrays", 30, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Declare 1D & 2D arrays in Java", "Allocate memory with new operator", "Iterate over array elements")),
                                    createLesson("1BCS302_M1_T5", 5, "Operators (Arithmetic, Relational, Logical, Assignment, Ternary)", "Operators", "Master Java Operators", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Use arithmetic & bitwise operators", "Apply short-circuit logical operators", "Construct ternary conditional expressions")),
                                    createLesson("1BCS302_M1_T6", 6, "Operator Precedence & Using Parentheses", "Precedence", "Master Operator Precedence", 25, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Evaluate complex operator expressions", "Use parentheses to enforce evaluation order", "Avoid subtle precedence bugs")),
                                    createLesson("1BCS302_M1_T7", 7, "Selection Statements (if, switch) & Iteration Statements (while, do-while, for)", "Control Flow", "Master Selection & Iteration", 40, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Write nested if-else & switch blocks", "Implement while, do-while, and for loops", "Optimize conditional execution")),
                                    createLesson("1BCS302_M1_T8", 8, "Jump Statements (break, continue, return) & For-Each Loop", "Jump Statements", "Master Jump Statements", 30, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Use labeled break & continue statements", "Write elegant for-each enhanced loops", "Return values safely from methods"))
                                )
                            ),
                            UnitJourney(
                                id = "1BCS302_M2",
                                unitNumber = 2,
                                title = "Module 2 • Classes & Methods",
                                isExpanded = false,
                                lessons = listOf(
                                    createLesson("1BCS302_M2_T1", 1, "Class Fundamentals, Declaring Objects & Assigning References", "Class Fundamentals", "Master Class Fundamentals", 30, Difficulty.MEDIUM, LessonStatus.AVAILABLE, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Define classes and instance variables", "Instantiate object reference variables", "Analyze stack vs heap references")),
                                    createLesson("1BCS302_M2_T2", 2, "Introducing Methods & Constructors", "Methods & Constructors", "Master Methods & Constructors", 35, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Write parameterized methods", "Define default & overloaded constructors", "Initialize instance data properly")),
                                    createLesson("1BCS302_M2_T3", 3, "The 'this' Keyword & Garbage Collection", "'this' & GC", "Master 'this' & Garbage Collection", 30, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Use 'this' to resolve name hiding", "Understand automatic garbage collection", "Monitor object lifecycle & finalize")),
                                    createLesson("1BCS302_M2_T4", 4, "Method Overloading & Objects as Parameters", "Method Overloading", "Master Method Overloading", 35, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Overload methods by parameter types", "Pass objects as method arguments", "Design factory constructor patterns")),
                                    createLesson("1BCS302_M2_T5", 5, "Argument Passing (Call by Value) & Returning Objects", "Call by Value", "Master Argument Passing", 35, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Understand primitive value passing vs reference passing", "Return new objects from methods", "Prevent mutable reference leaks")),
                                    createLesson("1BCS302_M2_T6", 6, "Recursion in Java", "Recursion", "Master Recursion in Java", 30, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Write recursive methods with base cases", "Trace call stack execution", "Compare recursive vs iterative performance")),
                                    createLesson("1BCS302_M2_T7", 7, "Access Control (public, private, protected) & static keyword", "Access & Static", "Master Access Control & Static", 35, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Enforce encapsulation with access specifiers", "Use static variables, methods & blocks", "Understand class-level state")),
                                    createLesson("1BCS302_M2_T8", 8, "final keyword, Nested & Inner Classes", "final & Inner Classes", "Master final & Inner Classes", 30, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Use final with variables, methods & classes", "Create non-static inner classes", "Design static nested helper classes"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCOA303",
                        name = "Computer Organization and Architecture (1BCOA303)",
                        iconEmoji = "⚙️",
                        completedCount = 5,
                        totalCount = 25,
                        units = listOf(
                            UnitJourney(
                                id = "1BCOA303_M1",
                                unitNumber = 1,
                                title = "Module 1 • Boolean Algebra & Minimization",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BCOA303_M1_T1", 1, "Basic Elements & Properties of Boolean Algebra", "Boolean Algebra", "Master Boolean Algebra", 30, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Apply Boolean theorems & De Morgan's laws", "Simplify logic expressions", "Construct canonical SOP & POS forms")),
                                    createLesson("1BCOA303_M1_T2", 2, "Boolean Functions & Digital Logic Gates", "Logic Gates", "Master Logic Gates", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Analyze AND, OR, NOT, XOR, XNOR gates", "Draw logic circuit diagrams", "Calculate gate propagation delays")),
                                    createLesson("1BCOA303_M1_T3", 3, "The Map Method (K-Map) & 4-Variable K-Map", "K-Map", "Master K-Map Minimization", 45, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Group minterms in 2, 3, and 4-variable K-maps", "Form prime implicants & essential prime implicants", "Derive minimal SOP expressions")),
                                    createLesson("1BCOA303_M1_T4", 4, "Product of Sums Simplification & Don't-Care Conditions", "POS & Don't Care", "Master POS & Don't Care", 40, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Simplify functions in POS form", "Utilize don't-care conditions (X) for grouping", "Minimize logic complexity")),
                                    createLesson("1BCOA303_M1_T5", 5, "NAND and NOR Implementations (Universal Gates)", "Universal Gates", "Master Universal Gates", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Implement 2-level NAND-NAND networks", "Implement 2-level NOR-NOR networks", "Convert AND/OR logic to universal logic"))
                                )
                            ),
                            UnitJourney(
                                id = "1BCOA303_M2",
                                unitNumber = 2,
                                title = "Module 2 • Combinational & Sequential Logic",
                                isExpanded = false,
                                lessons = listOf(
                                    createLesson("1BCOA303_M2_T1", 1, "Combinational Circuits Analysis & Design Procedure", "Combinational Analysis", "Master Combinational Circuits", 35, Difficulty.HARD, LessonStatus.AVAILABLE, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Follow 5-step digital design procedure", "Analyze combinational logic circuits", "Derive truth tables from logic gates")),
                                    createLesson("1BCOA303_M2_T2", 2, "Binary Adder-Subtractor Circuits", "Adders & Subtractors", "Master Binary Adders", 40, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Design half adder & full adder circuits", "Build 4-bit parallel adder/subtractor with 2's complement", "Analyze carry look-ahead adders")),
                                    createLesson("1BCOA303_M2_T3", 3, "Decoders & Encoders", "Decoders & Encoders", "Master Decoders & Encoders", 35, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Design 2-to-4 & 3-to-8 line decoders", "Implement Boolean functions using decoders", "Build priority encoders")),
                                    createLesson("1BCOA303_M2_T4", 4, "Multiplexers (MUX)", "Multiplexers", "Master Multiplexers", 30, Difficulty.MEDIUM, LessonStatus.LOCKED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Design 4-to-1 & 8-to-1 multiplexers", "Implement logic functions using MUX", "Cascade smaller MUXes into larger ones")),
                                    createLesson("1BCOA303_M2_T5", 5, "Sequential Circuits & Storage Elements (Latches)", "Latches", "Master Latches", 35, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Compare combinational vs sequential circuits", "Design NOR and NAND SR latches", "Analyze Gated D latches")),
                                    createLesson("1BCOA303_M2_T6", 6, "Flip-Flops (SR, JK, D Types)", "Flip-Flops", "Master Flip-Flops", 45, Difficulty.HARD, LessonStatus.LOCKED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Analyze Master-Slave JK flip-flops", "Eliminate race-around conditions", "Derive characteristic equations & excitation tables"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BOS304",
                        name = "Operating Systems (1BOS304)",
                        iconEmoji = "💻",
                        completedCount = 6,
                        totalCount = 31,
                        units = listOf(
                            UnitJourney(
                                id = "1BOS304_M1",
                                unitNumber = 1,
                                title = "Module 1 • Introduction & System Structures",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BOS304_M1_T1", 1, "What OS do, Computer System Organization/Architecture", "OS Role", "Master OS Role & Organization", 30, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Understand OS user vs system view", "Analyze multiprocessor & clustered systems", "Trace interrupt-driven execution")),
                                    createLesson("1BOS304_M1_T2", 2, "Operating System Operations & OS Structures", "OS Structures", "Master OS Structures", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Understand dual-mode execution (User vs Kernel)", "Analyze timer interrupts & hardware protection", "Examine monolithic vs microkernel architectures")),
                                    createLesson("1BOS304_M1_T3", 3, "Process Management, Memory Management & Storage Management", "OS Managers", "Master OS Managers", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Understand process state transitions", "Manage main memory allocation", "Manage disk storage & file abstraction")),
                                    createLesson("1BOS304_M1_T4", 4, "OS Services & User-OS Interface", "OS Services", "Master OS Services", 30, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Examine CLI vs GUI user interfaces", "Study OS services for execution & I/O", "Analyze accounting & protection services")),
                                    createLesson("1BOS304_M1_T5", 5, "System Calls (Types) & System Programs", "System Calls", "Master System Calls", 40, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Trace system call invocation via API (POSIX/Win32)", "Categorize process, file & device system calls", "Pass parameters to system call traps")),
                                    createLesson("1BOS304_M1_T6", 6, "OS Design, Implementation & System Structures", "OS Design", "Master OS Design", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Analyze layered OS architecture", "Examine virtual machines & hypervisors", "Trace OS boot sequence (Bootstrap loader)"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCS305",
                        name = "Data Structures and Applications (1BCS305)",
                        iconEmoji = "🌳",
                        completedCount = 6,
                        totalCount = 29,
                        units = listOf(
                            UnitJourney(
                                id = "1BCS305_M1",
                                unitNumber = 1,
                                title = "Module 1 • Introduction & Arrays",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BCS305_M1_T1", 1, "Classifications of Data Structures (Primitive & Non-Primitive)", "DS Classification", "Master DS Classifications", 25, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Differentiate linear vs non-linear data structures", "Analyze static vs dynamic data structures", "Evaluate time & space complexity ADTs")),
                                    createLesson("1BCS305_M1_T2", 2, "Pointers & Dynamic Memory Allocation in C", "Pointers & Allocation", "Master Pointers & Memory", 35, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Use pointer arithmetic & dereferencing", "Allocate memory with malloc(), calloc(), realloc()", "Free dynamic memory & eliminate memory leaks")),
                                    createLesson("1BCS305_M1_T3", 3, "Arrays & Dynamic Allocation Arrays", "Arrays", "Master Arrays", 30, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Compute row-major & column-major address formulas", "Dynamically allocate 1D & 2D arrays in C", "Pass arrays to C functions")),
                                    createLesson("1BCS305_M1_T4", 4, "Structures, Unions & Polynomial Representation", "Structures & Unions", "Master Structures & Unions", 40, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Define C structs, typedefs, and unions", "Represent mathematical polynomials using structs", "Add two single-variable polynomials")),
                                    createLesson("1BCS305_M1_T5", 5, "Sparse Matrix Representation (Triplet Form)", "Sparse Matrix", "Master Sparse Matrix", 35, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Identify sparse matrices with majority zero entries", "Store sparse matrix in 3-tuple (row, col, value) form", "Save memory space in large matrix operations")),
                                    createLesson("1BCS305_M1_T6", 6, "Transposing a Sparse Matrix", "Transpose Matrix", "Master Sparse Transpose", 35, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Implement Ordinary Transpose algorithm O(cols * terms)", "Implement Fast Transpose algorithm O(cols + terms)", "Compare time & space efficiency"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCS306",
                        name = "Data Structures with C Lab (1BCS306)",
                        iconEmoji = "🛠️",
                        completedCount = 12,
                        totalCount = 12,
                        units = listOf(
                            UnitJourney(
                                id = "1BCS306_M1",
                                unitNumber = 1,
                                title = "Module 1 • Part A & B Experiments",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BCS306_M1_T1", 1, "Book Structure (Create/Display/Search/Issue/Return)", "Book Structure", "Master Book Structure Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Build C program for Library book records", "Search books by ID and Title", "Implement book issue & return routines")),
                                    createLesson("1BCS306_M1_T2", 2, "Stack Array (Push/Pop/Palindrome/Overflow/Underflow)", "Stack Array Lab", "Master Stack Array Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Implement stack push & pop in C", "Check string palindrome using stack", "Handle stack overflow & underflow")),
                                    createLesson("1BCS306_M1_T3", 3, "Printer Queue Simulation (Add/Process/Display/Overflow)", "Printer Queue Lab", "Master Printer Queue Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Simulate FIFO printer spooler queue", "Enqueue print jobs & dequeue processing", "Display active print job queue status")),
                                    createLesson("1BCS306_M1_T4", 4, "Singly Linked List (Front Insertion/Display/Search/Delete)", "SLL Lab", "Master Singly Linked List Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Create SLL with dynamic node allocation", "Insert & delete nodes at front/end", "Search & count elements in list")),
                                    createLesson("1BCS306_M1_T5", 5, "Binary Tree (Level-order Create, Pre/In/Post Traversals)", "Binary Tree Lab", "Master Binary Tree Lab", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Construct binary tree in C", "Implement recursive Preorder, Inorder, Postorder traversals", "Display tree nodes level-by-level")),
                                    createLesson("1BCS306_M1_T6", 6, "Graph (Adjacency Matrix, DFS/BFS Traversal)", "Graph Traversal Lab", "Master Graph Traversal Lab", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Represent graph with adjacency matrix", "Implement Depth First Search (DFS)", "Implement Breadth First Search (BFS)")),
                                    createLesson("1BCS306_M1_T7", 7, "Sparse Matrix Addition (3-tuple Representation)", "Sparse Add Lab", "Master Sparse Matrix Add Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Read two sparse matrices in triplet form", "Add non-zero terms into result triplet", "Print resulting sparse matrix")),
                                    createLesson("1BCS306_M1_T8", 8, "Infix to Postfix Conversion Tool", "Infix-Postfix Lab", "Master Infix to Postfix Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Parse infix expressions using stack", "Handle operator precedence & parentheses", "Generate clean postfix string")),
                                    createLesson("1BCS306_M1_T9", 9, "Circular Queue (Insert/Delete/Display/Overflow)", "Circular Queue Lab", "Master Circular Queue Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Implement circular queue with front & rear pointers", "Handle wrap-around index arithmetic", "Check full & empty conditions")),
                                    createLesson("1BCS306_M1_T10", 10, "Doubly Linked List (End/Front Insert/Delete, DEQue)", "DLL Lab", "Master Doubly Linked List Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Create DLL with prev & next pointers", "Implement double-ended queue (DEQue)", "Traverse DLL in forward & reverse directions")),
                                    createLesson("1BCS306_M1_T11", 11, "Binary Search Tree (Create, Traversals, Search)", "BST Lab", "Master BST Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Insert elements into Binary Search Tree", "Search target key in BST", "Perform in-order traversal to get sorted output")),
                                    createLesson("1BCS306_M1_T12", 12, "Hashing with Linear Probing (Employee Records)", "Hashing Lab", "Master Hashing Lab", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Build hash table with modulo division H(k) = k % m", "Resolve collisions using Linear Probing", "Store & retrieve employee records"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BEDA307",
                        name = "Exploratory Data Analysis Lab (1BEDA307)",
                        iconEmoji = "📊",
                        completedCount = 12,
                        totalCount = 12,
                        units = listOf(
                            UnitJourney(
                                id = "1BEDA307_M1",
                                unitNumber = 1,
                                title = "Module 1 • EDA Experiments",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BEDA307_M1_T1", 1, "Load CSV, Display Records, Data Types & Summary", "Load CSV", "Master CSV Loading & Summaries", 55, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Load dataset using Pandas read_csv()", "Inspect head(), tail(), info(), describe()", "Identify numerical & categorical columns")),
                                    createLesson("1BEDA307_M1_T2", 2, "Basic Statistical Analysis (Mean, Median, Mode, Std Dev)", "Stats Analysis", "Master Basic Statistical Analysis", 55, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Compute central tendency (mean, median, mode)", "Calculate dispersion (variance, std dev, IQR)", "Detect skewness & kurtosis")),
                                    createLesson("1BEDA307_M1_T3", 3, "Data Quality Assessment (Missing Values & Report)", "Data Quality", "Master Data Quality Assessment", 55, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Check null counts with isna().sum()", "Calculate missing percentage per column", "Generate data health summary report")),
                                    createLesson("1BEDA307_M1_T4", 4, "Data Cleaning (Handle Missing Values & Compare)", "Data Cleaning", "Master Data Cleaning", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Apply mean/median/mode imputation", "Drop missing rows vs fill strategies", "Compare pre & post cleaning distributions")),
                                    createLesson("1BEDA307_M1_T5", 5, "Detect and Remove Duplicate Records", "Remove Duplicates", "Master Removing Duplicates", 45, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Find exact & subset duplicate rows", "Use drop_duplicates(keep='first')", "Verify dataset row count integrity")),
                                    createLesson("1BEDA307_M1_T6", 6, "Data Filtering, Sorting & Selection", "Filtering & Sorting", "Master Data Filtering & Sorting", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Filter rows by multiple boolean conditions", "Sort DataFrame by single & multiple columns", "Select column subsets using loc & iloc")),
                                    createLesson("1BEDA307_M1_T7", 7, "Grouping and Aggregation (Category-wise Stats)", "GroupBy Stats", "Master Grouping & Aggregation", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Group records using groupby()", "Apply aggregate functions agg(['mean', 'sum'])", "Build pivot tables")),
                                    createLesson("1BEDA307_M1_T8", 8, "Exploratory Statistical Analysis (Patterns & Outliers)", "Outlier Detection", "Master Outlier Detection", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Detect outliers using Z-score & IQR methods", "Plot boxplots to visualize extreme values", "Cap or trim dataset outliers")),
                                    createLesson("1BEDA307_M1_T9", 9, "Bar Charts & Pie Charts (Categorical Data)", "Bar & Pie Charts", "Master Categorical Visualization", 55, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Plot Seaborn countplots & bar plots", "Generate Matplotlib pie charts", "Add titles, labels & legends")),
                                    createLesson("1BEDA307_M1_T10", 10, "Line Plots & Scatter Plots (Trends & Relationships)", "Line & Scatter Plots", "Master Trend & Relationship Plots", 55, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Plot time series line trends", "Create scatter plots with hue color encoding", "Add trendlines & regression fits")),
                                    createLesson("1BEDA307_M1_T11", 11, "Correlation Matrix & Heatmap Visualization", "Correlation Heatmap", "Master Correlation Heatmap", 55, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Compute pairwise correlation matrix df.corr()", "Plot annotated Seaborn heatmap", "Identify highly correlated feature pairs")),
                                    createLesson("1BEDA307_M1_T12", 12, "Comprehensive EDA Micro-Project (End-to-End)", "EDA Micro-Project", "Master End-to-End EDA Micro-Project", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Perform full EDA pipeline on raw dataset", "Clean, transform & visualize key insights", "Present executive summary report & conclusion"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCSL307A",
                        name = "Project Management with Git Lab (1BCSL307A)",
                        iconEmoji = "🎓",
                        completedCount = 12,
                        totalCount = 12,
                        units = listOf(
                            UnitJourney(
                                id = "1BCSL307A_M1",
                                unitNumber = 1,
                                title = "Module 1 • Git Operations",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BCSL307A_M1_T1", 1, "Init Repo, Add File & Commit", "Git Init & Commit", "Master Git Init & Commit", 45, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Initialize local Git repository with git init", "Stage untracked files with git add", "Commit changes with descriptive messages")),
                                    createLesson("1BCSL307A_M1_T2", 2, "Create Branch, Switch, & Merge", "Branch & Merge", "Master Branch & Merge", 45, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Create feature branches with git branch", "Switch branches using git checkout / switch", "Merge feature branch into main")),
                                    createLesson("1BCSL307A_M1_T3", 3, "Stash Changes, Switch Branch, & Apply Stash", "Git Stash", "Master Git Stash", 45, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Stash uncommitted changes with git stash", "Switch to hotfix branch safely", "Pop & reapply stashed work")),
                                    createLesson("1BCSL307A_M1_T4", 4, "Clone a Remote Repository", "Git Clone", "Master Git Clone", 45, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Clone remote GitHub repo with HTTPS/SSH", "Examine remote tracking branches", "Inspect git remote -v configuration")),
                                    createLesson("1BCSL307A_M1_T5", 5, "Fetch Latest Changes & Rebase Local Branch", "Git Rebase", "Master Git Rebase", 45, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Fetch remote updates with git fetch", "Rebase local commits onto updated main", "Maintain linear commit history")),
                                    createLesson("1BCSL307A_M1_T6", 6, "Merge with Custom Commit Message", "Git Merge Msg", "Master Custom Merge Commit", 45, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Perform non-fast-forward merge with --no-ff", "Provide detailed merge commit log", "Document feature integration")),
                                    createLesson("1BCSL307A_M1_T7", 7, "Create a Lightweight Git Tag (v1.0)", "Git Tagging", "Master Git Tags", 45, Difficulty.EASY, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Create lightweight & annotated tags", "List & inspect release tags", "Push tags to remote GitHub release")),
                                    createLesson("1BCSL307A_M1_T8", 8, "Cherry-pick a Range of Commits", "Cherry Pick", "Master Git Cherry Pick", 45, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Apply specific commits using git cherry-pick", "Cherry-pick range of commits A..B", "Resolve conflicts during cherry-picking")),
                                    createLesson("1BCSL307A_M1_T9", 9, "View Details of a Specific Commit", "Git Show", "Master Git Show", 45, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Inspect commit diffs with git show <hash>", "Examine modified files & line changes", "Identify commit author & timestamp")),
                                    createLesson("1BCSL307A_M1_T10", 10, "Display Last Five Commits (History)", "Git Log", "Master Git Log", 45, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Format clean history with git log -n 5 --oneline", "Graph branch relationships with --graph", "Filter commits by author & date")),
                                    createLesson("1BCSL307A_M1_T11", 11, "Undo Changes by a Specific Commit ID", "Git Revert", "Master Git Revert", 45, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Revert bad commits safely with git revert", "Preserve public commit history", "Handle revert conflict resolution")),
                                    createLesson("1BCSL307A_M1_T12", 12, "View Commits Between Two Dates", "Git Log Dates", "Master Git Log Date Filters", 45, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Filter commits using --since and --until", "Audit project progress by date range", "Export commit logs for reporting"))
                                )
                            )
                        )
                    ),
                    SubjectJourney(
                        id = "1BCP308",
                        name = "Community / Societal Project (1BCP308)",
                        iconEmoji = "👥",
                        completedCount = 6,
                        totalCount = 6,
                        units = listOf(
                            UnitJourney(
                                id = "1BCP308_M1",
                                unitNumber = 1,
                                title = "Module 1 • Project Lifecycle",
                                isExpanded = true,
                                lessons = listOf(
                                    createLesson("1BCP308_M1_T1", 1, "Problem Identification & Topic Selection (Community Needs)", "Problem ID", "Master Problem Identification", 60, Difficulty.MEDIUM, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Survey local community & societal pain points", "Identify target beneficiaries & stakeholders", "Select impactful project domain")),
                                    createLesson("1BCP308_M1_T2", 2, "Stakeholder Interaction, Survey & Data Collection", "Stakeholder Survey", "Master Stakeholder Surveys", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Design quantitative survey questionnaires", "Conduct field interviews & data gathering", "Analyze survey responses & statistics")),
                                    createLesson("1BCP308_M1_T3", 3, "Problem Statement Formulation & Feasibility Analysis", "Problem Statement", "Master Problem Statement & Feasibility", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Define clear problem statement", "Perform technical & economic feasibility analysis", "Identify project constraints & risks")),
                                    createLesson("1BCP308_M1_T4", 4, "Solution Design, Planning & Resource Mapping", "Solution Design", "Master Solution Design & Planning", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Design architecture & functional solution workflow", "Map team roles & resource allocation", "Create project timeline & milestones")),
                                    createLesson("1BCP308_M1_T5", 5, "Prototype/Model Development & Testing", "Prototype Build", "Master Prototype Development", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.CODING, listOf("Core Prerequisites"), listOf("Develop working working prototype / software solution", "Conduct field testing with target community users", "Iterate solution based on user feedback")),
                                    createLesson("1BCP308_M1_T6", 6, "Documentation, Presentation & Societal Impact Assessment", "Impact Report", "Master Impact Assessment & Presentation", 60, Difficulty.HARD, LessonStatus.COMPLETED, LessonCategory.THEORY, listOf("Core Prerequisites"), listOf("Evaluate qualitative & quantitative societal impact", "Compile comprehensive project documentation", "Present project outcomes to evaluation committee"))
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
        status: LessonStatus = LessonStatus.LOCKED,
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
