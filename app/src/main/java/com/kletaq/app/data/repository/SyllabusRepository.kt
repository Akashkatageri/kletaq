package com.kletaq.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SyllabusSubjectModel(
    val id: String,
    val code: String,
    val title: String,
    val credits: Int,
    val semesterNumber: Int = 1
)

data class BacklogSemesterGroup(
    val semester: Int,
    val subjects: List<SyllabusSubjectModel>
)

interface SyllabusRepository {
    suspend fun importSyllabus(branch: String, semester: Int): Result<String>

    fun getSemesterSubjects(
        university: String = "VTU",
        scheme: String = "2025",
        branch: String,
        semester: Int,
        cycle: String? = null
    ): Result<List<SyllabusSubjectModel>>

    fun getBacklogSubjects(
        university: String = "VTU",
        scheme: String = "2025",
        branch: String,
        semester: Int,
        cycle: String? = null
    ): Result<List<SyllabusSubjectModel>>

    fun getBacklogPriorSemesters(
        university: String = "VTU",
        scheme: String = "2025",
        branch: String,
        semester: Int,
        cycle: String? = null
    ): Result<List<BacklogSemesterGroup>>

    fun getAllSubjects(
        university: String = "VTU",
        scheme: String = "2025",
        branch: String
    ): Result<List<SyllabusSubjectModel>>

    // Retained for backward compatibility
    fun getSubjectsFromAssets(
        university: String,
        scheme: String,
        branch: String,
        semester: Int,
        cycle: String?
    ): List<SyllabusSubjectModel>
}

@Singleton
class SyllabusRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SyllabusRepository {

    override suspend fun importSyllabus(branch: String, semester: Int): Result<String> {
        return try {
            val path = "syllabus/${branch.lowercase()}/semester${semester}.json"
            val content = try {
                context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                """[{"branch": "$branch", "semester": $semester, "status": "imported"}]"""
            }
            delay(800L) // Smooth progress animation delay
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSemesterSubjects(
        university: String,
        scheme: String,
        branch: String,
        semester: Int,
        cycle: String?
    ): Result<List<SyllabusSubjectModel>> {
        val cleanBranch = branch.lowercase().replace(" ", "").replace("&", "").replace("_", "").trim()
        val cleanScheme = scheme.replace(" Scheme", "").lowercase().trim()
        val cleanUni = university.lowercase().trim()

        val primaryPath = "syllabus/$cleanUni/$cleanScheme/$cleanBranch/semester${semester}.json"
        val aliasPath = "syllabus/$cleanUni/$cleanScheme/$cleanBranch/sem${semester}.json"

        val possiblePaths = listOf(primaryPath, aliasPath)

        for (path in possiblePaths) {
            try {
                val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
                val parsed = parseSubjectsFromJson(jsonString, semester, cycle)
                if (parsed.isNotEmpty()) {
                    return Result.success(parsed)
                }
            } catch (_: Exception) {
                // Try alias path
            }
        }

        val missingPath = "assets/syllabus/$cleanUni/$cleanScheme/$cleanBranch/semester${semester}.json"
        android.util.Log.e("BacklogDebug", "Missing semester $semester JSON file: $missingPath")
        return Result.failure(Exception("Unable to load syllabus for ${branch.uppercase()} semester $semester ($missingPath)."))
    }

    override fun getBacklogPriorSemesters(
        university: String,
        scheme: String,
        branch: String,
        semester: Int,
        cycle: String?
    ): Result<List<BacklogSemesterGroup>> {
        if (semester <= 1) return Result.success(emptyList())

        val resultList = mutableListOf<BacklogSemesterGroup>()
        val loadedSemesters = mutableListOf<Int>()
        val loadedJsonPaths = mutableListOf<String>()

        val cleanBranch = branch.lowercase().replace(" ", "").replace("&", "").replace("_", "").trim()
        val cleanScheme = scheme.replace(" Scheme", "").lowercase().trim()
        val cleanUni = university.lowercase().trim()

        for (sem in 1 until semester) {
            val semResult = getSemesterSubjects(university, scheme, branch, sem, cycle)
            semResult.onSuccess { subjects ->
                if (subjects.isNotEmpty()) {
                    loadedSemesters.add(sem)
                    val loadedPath = "assets/syllabus/$cleanUni/$cleanScheme/$cleanBranch/semester${sem}.json"
                    loadedJsonPaths.add(loadedPath)
                    resultList.add(
                        BacklogSemesterGroup(
                            semester = sem,
                            subjects = subjects.map { it.copy(semesterNumber = sem) }
                        )
                    )
                }
            }.onFailure { err ->
                val missingPath = "assets/syllabus/$cleanUni/$cleanScheme/$cleanBranch/semester${sem}.json"
                android.util.Log.e("BacklogDebug", "Missing semester $sem JSON file: $missingPath ($err)")
            }
        }

        android.util.Log.d("BacklogDebug", "selectedSemester = $semester")
        android.util.Log.d("BacklogDebug", "selectedBranch = $cleanBranch")
        android.util.Log.d("BacklogDebug", "loadedSemesters = $loadedSemesters")
        android.util.Log.d("BacklogDebug", "loadedJsonPaths =")
        loadedJsonPaths.forEach { path ->
            android.util.Log.d("BacklogDebug", "- $path")
        }

        return if (resultList.isNotEmpty()) {
            Result.success(resultList)
        } else {
            Result.failure(Exception("Unable to load syllabus for ${branch.uppercase()}."))
        }
    }

    override fun getBacklogSubjects(
        university: String,
        scheme: String,
        branch: String,
        semester: Int,
        cycle: String?
    ): Result<List<SyllabusSubjectModel>> {
        val groupsResult = getBacklogPriorSemesters(university, scheme, branch, semester, cycle)
        return groupsResult.map { groups -> groups.flatMap { it.subjects } }
    }

    override fun getAllSubjects(
        university: String,
        scheme: String,
        branch: String
    ): Result<List<SyllabusSubjectModel>> {
        val allSubjectsList = mutableListOf<SyllabusSubjectModel>()

        for (sem in 1..8) {
            val semResult = getSemesterSubjects(university, scheme, branch, sem, null)
            semResult.onSuccess { subjects ->
                allSubjectsList.addAll(subjects.map { it.copy(semesterNumber = sem) })
            }
        }

        return if (allSubjectsList.isNotEmpty()) {
            Result.success(allSubjectsList)
        } else {
            Result.failure(Exception("Unable to load syllabus for ${branch.uppercase()}."))
        }
    }

    override fun getSubjectsFromAssets(
        university: String,
        scheme: String,
        branch: String,
        semester: Int,
        cycle: String?
    ): List<SyllabusSubjectModel> {
        return getSemesterSubjects(university, scheme, branch, semester, cycle).getOrElse { emptyList() }
    }

    private fun parseSubjectsFromJson(jsonString: String, semester: Int, cycle: String?): List<SyllabusSubjectModel> {
        val list = mutableListOf<SyllabusSubjectModel>()
        try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                // Official Kletaq Web Format: Top-level JSON Array of Subject Objects
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", "sub_${semester}_$i")
                    val code = obj.optString("code", "")
                    val title = if (obj.has("name")) obj.getString("name") else obj.optString("title", "Subject ${i + 1}")
                    val credits = obj.optInt("credits", 4)
                    list.add(SyllabusSubjectModel(id = id, code = code, title = title, credits = credits, semesterNumber = semester))
                }
            } else if (trimmed.startsWith("{")) {
                // Alternative JSON Object Format
                val root = JSONObject(trimmed)
                val cleanCycle = cycle?.lowercase()?.trim() ?: ""

                val targetArrayKey = when {
                    (semester == 1 || semester == 2) && cleanCycle == "physics" && root.has("physics_cycle") -> "physics_cycle"
                    (semester == 1 || semester == 2) && cleanCycle == "chemistry" && root.has("chemistry_cycle") -> "chemistry_cycle"
                    root.has("subjects") -> "subjects"
                    else -> null
                }

                if (targetArrayKey != null) {
                    val array = root.getJSONArray(targetArrayKey)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.optString("id", "sub_${semester}_$i")
                        val code = obj.optString("code", "")
                        val title = if (obj.has("name")) obj.getString("name") else obj.optString("title", "Subject ${i + 1}")
                        val credits = obj.optInt("credits", 4)
                        list.add(SyllabusSubjectModel(id = id, code = code, title = title, credits = credits, semesterNumber = semester))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyllabusRepository", "Error parsing json for sem $semester: ${e.message}")
        }
        return list
    }
}
