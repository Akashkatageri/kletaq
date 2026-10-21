package com.kletaq.app.core.syllabus

import android.content.Context
import android.util.Log
import com.kletaq.app.features.journey.components.Difficulty
import com.kletaq.app.features.journey.components.LessonCategory
import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.LessonStatus
import com.kletaq.app.features.journey.components.SubjectJourney
import com.kletaq.app.features.journey.components.UnitJourney
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.flow.asStateFlow

data class ImportedTopic(
    val title: String,
    val estimatedMinutes: Int = 20
)

data class ImportedUnit(
    val unitNumber: Int,
    val title: String,
    val topics: List<ImportedTopic>
)

data class ImportedSubject(
    val code: String,
    val name: String,
    val semesterNumber: Int = 1,
    val units: List<ImportedUnit>,
    val iconEmoji: String = "📚"
) {
    fun toSubjectJourney(completedTopicKeys: Set<String> = emptySet()): SubjectJourney {
        val subjectId = code.ifBlank { name.replace(" ", "_").uppercase() }
        var totalLessons = 0
        var completedLessons = 0

        val unitJourneys = units.mapIndexed { uIdx, unit ->
            val unitNum = unit.unitNumber.takeIf { it > 0 } ?: (uIdx + 1)
            val unitId = "${subjectId}_M$unitNum"

            val lessonNodes = unit.topics.mapIndexed { tIdx, topic ->
                totalLessons++
                val topicId = "${unitId}_T${tIdx + 1}"
                val isCompleted = completedTopicKeys.contains(topicId)
                if (isCompleted) completedLessons++

                val status = when {
                    isCompleted -> LessonStatus.COMPLETED
                    tIdx == 0 -> LessonStatus.AVAILABLE // First lesson of each unit is unlocked
                    else -> LessonStatus.AVAILABLE
                }

                LessonNode(
                    id = topicId,
                    lessonNumber = tIdx + 1,
                    title = topic.title,
                    shortTitle = topic.title.take(28),
                    description = "In $name, ${topic.title} covers essential engineering principles and exam problem patterns.",
                    status = status,
                    category = LessonCategory.THEORY,
                    durationMinutes = topic.estimatedMinutes,
                    xpReward = 50,
                    difficulty = Difficulty.MEDIUM
                )
            }

            UnitJourney(
                id = unitId,
                unitNumber = unitNum,
                title = unit.title.ifBlank { "Unit $unitNum" },
                isExpanded = uIdx == 0,
                lessons = lessonNodes
            )
        }

        return SubjectJourney(
            id = subjectId,
            name = name,
            iconEmoji = iconEmoji,
            completedCount = completedLessons,
            totalCount = totalLessons,
            isBacklog = false,
            units = unitJourneys,
            isCustom = true
        )
    }
}

/**
 * Manages locally imported custom syllabi generated via AI Syllabus Importer.
 */
object CustomSyllabusManager {

    private const val TAG = "CustomSyllabus"
    private const val FILE_NAME = "custom_syllabi.json"

    private val cachedSubjects = mutableListOf<ImportedSubject>()
    private var isLoaded = false

    private val _revision = kotlinx.coroutines.flow.MutableStateFlow(0)
    val revision: kotlinx.coroutines.flow.StateFlow<Int> = _revision.asStateFlow()

    @Synchronized
    fun getImportedSubjects(context: Context): List<ImportedSubject> {
        if (!isLoaded) {
            loadFromFile(context)
        }
        return cachedSubjects.toList()
    }

    @Synchronized
    fun saveSubject(context: Context, subject: ImportedSubject) {
        if (!isLoaded) {
            loadFromFile(context)
        }
        // Replace existing subject with same code or name
        cachedSubjects.removeAll { it.code.equals(subject.code, ignoreCase = true) || it.name.equals(subject.name, ignoreCase = true) }
        cachedSubjects.add(subject)
        saveToFile(context)
        _revision.value += 1
    }

    @Synchronized
    fun deleteSubject(context: Context, subjectCode: String, subjectName: String = ""): Boolean {
        if (!isLoaded) {
            loadFromFile(context)
        }
        val removed = cachedSubjects.removeAll {
            (subjectCode.isNotBlank() && it.code.equals(subjectCode, ignoreCase = true)) ||
            (subjectName.isNotBlank() && it.name.equals(subjectName, ignoreCase = true)) ||
            (subjectCode.isNotBlank() && it.name.contains(subjectCode, ignoreCase = true)) ||
            (subjectName.isNotBlank() && it.code.contains(subjectName, ignoreCase = true))
        }
        if (removed) {
            saveToFile(context)
            Log.d(TAG, "Deleted custom subject: code=$subjectCode name=$subjectName. Remaining: ${cachedSubjects.size}")
            _revision.value += 1
        }
        return removed
    }

    @Synchronized
    fun addSubject(
        context: Context,
        code: String,
        name: String,
        semesterNumber: Int,
        iconEmoji: String = "📚",
        initialModuleTitle: String = "Module 1"
    ): ImportedSubject {
        if (!isLoaded) {
            loadFromFile(context)
        }
        val cleanCode = code.trim().ifBlank { name.trim().replace(" ", "_").uppercase() }
        val cleanName = name.trim().ifBlank { cleanCode }
        val initialUnit = ImportedUnit(
            unitNumber = 1,
            title = initialModuleTitle.trim().ifBlank { "Module 1" },
            topics = emptyList()
        )
        val newSubj = ImportedSubject(
            code = cleanCode,
            name = cleanName,
            semesterNumber = semesterNumber,
            units = listOf(initialUnit),
            iconEmoji = iconEmoji
        )
        cachedSubjects.removeAll { it.code.equals(cleanCode, ignoreCase = true) || it.name.equals(cleanName, ignoreCase = true) }
        cachedSubjects.add(newSubj)
        saveToFile(context)
        _revision.value += 1
        return newSubj
    }

    @Synchronized
    fun addUnit(
        context: Context,
        subjectCode: String,
        unitTitle: String
    ): ImportedUnit? {
        if (!isLoaded) {
            loadFromFile(context)
        }
        val subjIdx = cachedSubjects.indexOfFirst {
            it.code.equals(subjectCode, ignoreCase = true) || it.name.equals(subjectCode, ignoreCase = true)
        }
        if (subjIdx < 0) return null

        val currentSubj = cachedSubjects[subjIdx]
        val nextUnitNum = (currentSubj.units.maxOfOrNull { it.unitNumber } ?: 0) + 1
        val newUnit = ImportedUnit(
            unitNumber = nextUnitNum,
            title = unitTitle.trim().ifBlank { "Module $nextUnitNum" },
            topics = emptyList()
        )
        val updatedUnits = currentSubj.units + newUnit
        cachedSubjects[subjIdx] = currentSubj.copy(units = updatedUnits)
        saveToFile(context)
        _revision.value += 1
        return newUnit
    }

    @Synchronized
    fun addTopic(
        context: Context,
        subjectCode: String,
        unitNumber: Int,
        topicTitle: String,
        estimatedMinutes: Int = 25
    ): ImportedTopic? {
        if (!isLoaded) {
            loadFromFile(context)
        }
        val subjIdx = cachedSubjects.indexOfFirst {
            it.code.equals(subjectCode, ignoreCase = true) || it.name.equals(subjectCode, ignoreCase = true)
        }
        if (subjIdx < 0) return null

        val currentSubj = cachedSubjects[subjIdx]
        val unitIdx = currentSubj.units.indexOfFirst { it.unitNumber == unitNumber }
        if (unitIdx < 0) return null

        val currentUnit = currentSubj.units[unitIdx]
        val newTopic = ImportedTopic(
            title = topicTitle.trim().ifBlank { "Topic ${currentUnit.topics.size + 1}" },
            estimatedMinutes = estimatedMinutes.coerceIn(5, 120)
        )
        val updatedTopics = currentUnit.topics + newTopic
        val updatedUnits = currentSubj.units.toMutableList()
        updatedUnits[unitIdx] = currentUnit.copy(topics = updatedTopics)
        cachedSubjects[subjIdx] = currentSubj.copy(units = updatedUnits)
        saveToFile(context)
        _revision.value += 1
        return newTopic
    }

    @Synchronized
    fun deleteUnit(
        context: Context,
        subjectCode: String,
        unitNumber: Int
    ): Boolean {
        if (!isLoaded) {
            loadFromFile(context)
        }
        val subjIdx = cachedSubjects.indexOfFirst {
            it.code.equals(subjectCode, ignoreCase = true) || it.name.equals(subjectCode, ignoreCase = true)
        }
        if (subjIdx < 0) return false

        val currentSubj = cachedSubjects[subjIdx]
        val updatedUnits = currentSubj.units.filterNot { it.unitNumber == unitNumber }
        cachedSubjects[subjIdx] = currentSubj.copy(units = updatedUnits)
        saveToFile(context)
        _revision.value += 1
        return true
    }

    @Synchronized
    fun deleteTopic(
        context: Context,
        subjectCode: String,
        unitNumber: Int,
        topicTitle: String
    ): Boolean {
        if (!isLoaded) {
            loadFromFile(context)
        }
        val subjIdx = cachedSubjects.indexOfFirst {
            it.code.equals(subjectCode, ignoreCase = true) || it.name.equals(subjectCode, ignoreCase = true)
        }
        if (subjIdx < 0) return false

        val currentSubj = cachedSubjects[subjIdx]
        val unitIdx = currentSubj.units.indexOfFirst { it.unitNumber == unitNumber }
        if (unitIdx < 0) return false

        val currentUnit = currentSubj.units[unitIdx]
        val updatedTopics = currentUnit.topics.filterNot { it.title.equals(topicTitle, ignoreCase = true) }
        val updatedUnits = currentSubj.units.toMutableList()
        updatedUnits[unitIdx] = currentUnit.copy(topics = updatedTopics)
        cachedSubjects[subjIdx] = currentSubj.copy(units = updatedUnits)
        saveToFile(context)
        _revision.value += 1
        return true
    }

    private fun loadFromFile(context: Context) {
        cachedSubjects.clear()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                isLoaded = true
                return
            }
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val code = obj.optString("code", "")
                val name = obj.optString("name", "")
                val semester = obj.optInt("semester", 1)
                val iconEmoji = obj.optString("iconEmoji", "📚").ifBlank { "📚" }

                val unitsList = mutableListOf<ImportedUnit>()
                val unitsArr = obj.optJSONArray("units") ?: JSONArray()
                for (u in 0 until unitsArr.length()) {
                    val uObj = unitsArr.getJSONObject(u)
                    val uNum = uObj.optInt("unitNumber", u + 1)
                    val uTitle = uObj.optString("title", "Unit $uNum")

                    val topicsList = mutableListOf<ImportedTopic>()
                    val topicsArr = uObj.optJSONArray("topics") ?: JSONArray()
                    for (t in 0 until topicsArr.length()) {
                        val tObj = topicsArr.getJSONObject(t)
                        topicsList.add(
                            ImportedTopic(
                                title = tObj.optString("title", "Topic ${t + 1}"),
                                estimatedMinutes = tObj.optInt("estimatedMinutes", 20)
                            )
                        )
                    }
                    unitsList.add(ImportedUnit(uNum, uTitle, topicsList))
                }
                cachedSubjects.add(ImportedSubject(code, name, semester, unitsList, iconEmoji))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom syllabi: ${e.message}")
        }
        isLoaded = true
        _revision.value += 1
    }

    private fun saveToFile(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val array = JSONArray()
            for (subject in cachedSubjects) {
                val obj = JSONObject()
                obj.put("code", subject.code)
                obj.put("name", subject.name)
                obj.put("semester", subject.semesterNumber)
                obj.put("iconEmoji", subject.iconEmoji)

                val unitsArr = JSONArray()
                for (unit in subject.units) {
                    val uObj = JSONObject()
                    uObj.put("unitNumber", unit.unitNumber)
                    uObj.put("title", unit.title)

                    val topicsArr = JSONArray()
                    for (topic in unit.topics) {
                        val tObj = JSONObject()
                        tObj.put("title", topic.title)
                        tObj.put("estimatedMinutes", topic.estimatedMinutes)
                        topicsArr.put(tObj)
                    }
                    uObj.put("topics", topicsArr)
                    unitsArr.put(uObj)
                }
                obj.put("units", unitsArr)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom syllabi: ${e.message}")
        }
    }
}
