package com.kletaq.app.features.lab

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kletaq.app.data.repository.AcademicContentRepository
import com.kletaq.app.features.journey.components.SubjectJourney
import com.kletaq.app.features.journey.components.SubjectType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

object LabDefenseRepository {

    private const val PREFS_NAME = "kletaq_lab_defense_prefs"
    private const val KEY_IS_LAB_PREFIX = "is_lab_"
    private const val KEY_LAB_DAY_PREFIX = "lab_day_"
    private const val KEY_MASTERY_PREFIX = "mastery_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Determines whether a subject is a Lab / Practical subject.
     * Evaluates explicit user overrides first, then auto-detection heuristics.
     */
    fun isSubjectLab(context: Context, subjectId: String, subjectName: String): Boolean {
        val prefs = getPrefs(context)
        val cleanId = subjectId.trim()
        val overrideKey = KEY_IS_LAB_PREFIX + cleanId
        if (prefs.contains(overrideKey)) {
            return prefs.getBoolean(overrideKey, false)
        }

        // Auto-detection heuristics
        val combined = "$subjectId $subjectName".lowercase()
        return combined.contains("lab") ||
               combined.contains("laboratory") ||
               combined.contains("practical") ||
               cleanId.matches(Regex(".*\\d+.*l.*", RegexOption.IGNORE_CASE))
    }

    /**
     * Sets or overrides whether a subject is treated as a Lab subject.
     */
    fun setSubjectLab(context: Context, subjectId: String, isLab: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_IS_LAB_PREFIX + subjectId.trim(), isLab).apply()
    }

    /**
     * Retrieves the scheduled day of the week for this lab (e.g., Calendar.THURSDAY).
     * Defaults to null (unset).
     */
    fun getLabDay(context: Context, subjectId: String): Int? {
        val prefs = getPrefs(context)
        val day = prefs.getInt(KEY_LAB_DAY_PREFIX + subjectId.trim(), -1)
        return if (day in Calendar.SUNDAY..Calendar.SATURDAY) day else null
    }

    /**
     * Sets the scheduled day of the week for this lab.
     */
    fun setLabDay(context: Context, subjectId: String, dayOfWeek: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(KEY_LAB_DAY_PREFIX + subjectId.trim(), dayOfWeek).apply()
    }

    /**
     * Retrieves the mastery status of a specific lab program.
     */
    fun getProgramMastery(context: Context, subjectId: String, programNumber: Int): ProgramMasteryStatus {
        val prefs = getPrefs(context)
        val key = "${KEY_MASTERY_PREFIX}${subjectId.trim()}_$programNumber"
        val name = prefs.getString(key, null) ?: return ProgramMasteryStatus.UNTOUCHED
        return try {
            ProgramMasteryStatus.valueOf(name)
        } catch (_: Exception) {
            ProgramMasteryStatus.UNTOUCHED
        }
    }

    /**
     * Updates the mastery status of a specific lab program.
     */
    fun setProgramMastery(context: Context, subjectId: String, programNumber: Int, status: ProgramMasteryStatus) {
        val prefs = getPrefs(context)
        val key = "${KEY_MASTERY_PREFIX}${subjectId.trim()}_$programNumber"
        prefs.edit().putString(key, status.name).apply()
    }

    /**
     * Calculates the Lab External Pass Probability:
     * Readiness = (Verified Programs / Total Programs) ^ 2
     */
    fun calculateReadiness(programs: List<LabProgramItem>): Pair<Float, Float> {
        if (programs.isEmpty()) return Pair(0f, 0f)
        val verifiedCount = programs.count { it.masteryStatus == ProgramMasteryStatus.BLIND_VERIFIED || it.masteryStatus == ProgramMasteryStatus.COLLEGE_EXECUTED }
        val completionRatio = verifiedCount.toFloat() / programs.size.toFloat()
        // Squaring reflects the probability of drawing 2 programs and knowing BOTH
        val passProbability = completionRatio * completionRatio
        return Pair(completionRatio, passProbability)
    }

    private fun getCustomProgramsFile(context: Context, subjectId: String): File {
        val clean = subjectId.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val dir = File(context.filesDir, "custom_lab_programs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$clean.json")
    }

    fun hasCustomLabPrograms(context: Context, subjectId: String): Boolean {
        val programs = getCustomLabPrograms(context, subjectId)
        return !programs.isNullOrEmpty()
    }

    fun saveCustomLabPrograms(context: Context, subjectId: String, programs: List<LabProgramItem>) {
        try {
            val file = getCustomProgramsFile(context, subjectId)
            val root = JSONObject()
            val arr = JSONArray()
            for (p in programs) {
                val obj = JSONObject()
                obj.put("programNumber", p.programNumber)
                obj.put("title", p.title)
                obj.put("shortTitle", p.shortTitle)
                obj.put("language", p.language)
                obj.put("fullCode", p.fullCode)
                obj.put("blueprintBlock", p.blueprintBlock)
                obj.put("coreFunctionBlock", p.coreFunctionBlock)
                obj.put("guardBlock", p.guardBlock)
                obj.put("driverBlock", p.driverBlock)

                val stepsArr = JSONArray()
                p.algorithmSteps.forEach { stepsArr.put(it) }
                obj.put("algorithmSteps", stepsArr)

                val vivaArr = JSONArray()
                p.vivaQuestions.forEach { (q, a) ->
                    val vObj = JSONObject()
                    vObj.put("question", q)
                    vObj.put("answer", a)
                    vivaArr.put(vObj)
                }
                obj.put("vivaQuestions", vivaArr)

                val trapsArr = JSONArray()
                p.crashTraps.forEach { trapsArr.put(it) }
                obj.put("crashTraps", trapsArr)

                arr.put(obj)
            }
            root.put("programs", arr)
            file.writeText(root.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("LabDefenseRepository", "Error saving custom lab programs: ${e.message}")
        }
    }

    fun getCustomLabPrograms(context: Context, subjectId: String): List<LabProgramItem>? {
        val file = getCustomProgramsFile(context, subjectId)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val json = file.readText(Charsets.UTF_8)
            // Immediately purge legacy placeholder/dummy data
            if (json.contains("Initialize environment and variables") ||
                json.contains("Practical Program 1: Algorithm & Implementation") ||
                json.contains("Execute the primary logic computation") ||
                json.contains("Practical Program 1: Algorit")
            ) {
                file.delete()
                val pdfFile = getCustomLabManualPdfFile(context, subjectId)
                if (pdfFile != null && pdfFile.exists()) pdfFile.delete()
                return null
            }

            val list = mutableListOf<LabProgramItem>()
            val root = JSONObject(json)
            val arr = root.optJSONArray("programs") ?: return null
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pNum = obj.optInt("programNumber", i + 1)
                val title = obj.optString("title", "Program $pNum")
                val shortTitle = obj.optString("shortTitle", title.take(24))
                val language = obj.optString("language", "C")
                val fullCode = obj.optString("fullCode", "")
                val blueprint = obj.optString("blueprintBlock", "")
                val coreFunc = obj.optString("coreFunctionBlock", "")
                val guard = obj.optString("guardBlock", "")
                val driver = obj.optString("driverBlock", "")

                val stepsArr = obj.optJSONArray("algorithmSteps")
                val steps = mutableListOf<String>()
                if (stepsArr != null) {
                    for (s in 0 until stepsArr.length()) steps.add(stepsArr.getString(s))
                }

                val vivaArr = obj.optJSONArray("vivaQuestions")
                val viva = mutableListOf<Pair<String, String>>()
                if (vivaArr != null) {
                    for (v in 0 until vivaArr.length()) {
                        val vObj = vivaArr.getJSONObject(v)
                        viva.add(vObj.optString("question") to vObj.optString("answer"))
                    }
                }

                val trapsArr = obj.optJSONArray("crashTraps")
                val traps = mutableListOf<String>()
                if (trapsArr != null) {
                    for (t in 0 until trapsArr.length()) traps.add(trapsArr.getString(t))
                }

                list.add(
                    LabProgramItem(
                        programNumber = pNum,
                        title = title,
                        shortTitle = shortTitle,
                        language = language,
                        fullCode = fullCode,
                        blueprintBlock = blueprint,
                        coreFunctionBlock = coreFunc,
                        guardBlock = guard,
                        driverBlock = driver,
                        algorithmSteps = steps,
                        vivaQuestions = viva,
                        crashTraps = traps,
                        masteryStatus = ProgramMasteryStatus.UNTOUCHED
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    fun getCustomLabManualPdfFile(context: Context, subjectId: String): File? {
        val dir = File(context.filesDir, "lab_manual_pdfs")
        val clean = subjectId.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val file = File(dir, "$clean.pdf")
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun saveLabManualPdf(context: Context, subjectId: String, pdfBytes: ByteArray) {
        try {
            val dir = File(context.filesDir, "lab_manual_pdfs")
            if (!dir.exists()) dir.mkdirs()
            val clean = subjectId.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
            File(dir, "$clean.pdf").writeBytes(pdfBytes)
        } catch (e: Exception) {
            Log.e("LabDefenseRepository", "Failed to save lab manual PDF: ${e.message}")
        }
    }

    fun openLabManualPdf(context: Context, subjectId: String) {
        val file = getCustomLabManualPdfFile(context, subjectId)
        if (file == null) {
            Toast.makeText(context, "No saved PDF manual found.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resInfoList = context.packageManager.queryIntentActivities(viewIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearCustomLabPrograms(context: Context, subjectId: String) {
        val file = getCustomProgramsFile(context, subjectId)
        if (file.exists()) file.delete()
        val pdfFile = getCustomLabManualPdfFile(context, subjectId)
        if (pdfFile != null && pdfFile.exists()) pdfFile.delete()
    }

    /**
     * Loads the genuine syllabus programs for the given lab subject.
     * Extracts rich practicals from AcademicContentRepository (assets/academic/subjects/{id}.json)
     * or from the subject's structured unit lessons.
     */
    fun loadSyllabusLabPrograms(context: Context, subject: SubjectJourney): List<LabProgramItem> {
        // 1. Try loading rich TopicStudyPack from AcademicContentRepository
        val studyPacks = AcademicContentRepository.getAllStudyPacksForSubject(context, subject.id, subject.name)
        if (studyPacks.isNotEmpty()) {
            val programs = mutableListOf<LabProgramItem>()
            val seenTitles = mutableSetOf<String>()
            var progIndex = 1

            for ((_, pack) in studyPacks) {
                val cleanTitle = pack.title.trim()
                if (cleanTitle.isBlank() || seenTitles.contains(cleanTitle)) continue
                seenTitles.add(cleanTitle)

                val shortTitle = cleanTitle.split("(").first().trim().take(24)
                val codeSnippet = pack.codeSnippets.firstOrNull()
                val lang = codeSnippet?.language?.ifBlank { "C" } ?: if (subject.name.contains("git", ignoreCase = true)) "bash" else if (subject.name.contains("python", ignoreCase = true)) "python" else "C"
                val code = codeSnippet?.code ?: ""

                // Algorithm Steps from keyPoints or stepByStepAnswer
                val steps = if (pack.keyPoints.size >= 3) {
                    pack.keyPoints.map { pt ->
                        pt.replace(Regex("^\\s*[-•*\\d.]+\\s*"), "").trim()
                    }.filter { it.isNotBlank() }
                } else if (pack.examModelAnswers.isNotEmpty() && pack.examModelAnswers[0].stepByStepAnswer.isNotBlank()) {
                    pack.examModelAnswers[0].stepByStepAnswer
                        .split("\n")
                        .map { it.replace(Regex("^\\s*[-•*\\d.]+\\s*"), "").trim() }
                        .filter { it.isNotBlank() && !it.startsWith("```") && it.length > 5 }
                        .take(5)
                } else {
                    emptyList()
                }

                // Viva Questions from examModelAnswers and quickQuiz
                val vivaList = mutableListOf<Pair<String, String>>()
                pack.quickQuiz.forEach { q ->
                    if (q.question.isNotBlank()) {
                        val answer = if (q.explanation.isNotBlank()) q.explanation else q.options.getOrNull(q.correctIndex).orEmpty()
                        vivaList.add(q.question to answer)
                    }
                }
                pack.examModelAnswers.forEach { ema ->
                    if (ema.question.isNotBlank()) {
                        val ans = if (ema.markingTip.isNotBlank()) ema.markingTip else ema.stepByStepAnswer.take(120)
                        vivaList.add(ema.question to ans)
                    }
                }

                // Crash Traps from technicalConcept or specific common pitfalls
                val traps = mutableListOf<String>()
                if (pack.technicalConcept.contains("pointer", ignoreCase = true) || pack.technicalConcept.contains("malloc", ignoreCase = true)) {
                    traps.add("Uninitialized pointer dereference causing SIGSEGV (Segmentation Fault)")
                    traps.add("Memory leak from missing free() deallocation on dynamically allocated nodes")
                }
                if (pack.technicalConcept.contains("overflow", ignoreCase = true) || pack.technicalConcept.contains("stack", ignoreCase = true)) {
                    traps.add("Off-by-one boundary check: testing top == MAX instead of top == MAX - 1")
                    traps.add("Popping from empty stack without underflow guard check")
                }
                if (pack.technicalConcept.contains("queue", ignoreCase = true)) {
                    traps.add("Circular index wrap-around formula failure: forgetting `(rear + 1) % MAX`")
                    traps.add("Queue empty condition confusion when front == -1 or front > rear")
                }
                if (pack.technicalConcept.contains("tree", ignoreCase = true)) {
                    traps.add("Dereferencing NULL left or right pointer in leaf nodes during recursion")
                    traps.add("Infinite recursion stack overflow from missing base case `if (root == NULL) return`")
                }
                if (pack.technicalConcept.contains("git", ignoreCase = true) || lang.equals("bash", ignoreCase = true)) {
                    traps.add("Rebasing shared public commits creating divergent history conflicts")
                    traps.add("Detached HEAD state when checking out specific commit hash directly without branch")
                }
                if (traps.isEmpty()) {
                    traps.add("Missing boundary guard check for null/empty input")
                    traps.add("Off-by-one index bound iteration in array access")
                }

                val finalSteps = if (steps.size in 3..6) steps else listOf(
                    "Define data structure and initialize pointers",
                    "Read input elements and validate boundaries",
                    "Execute core algorithm operations",
                    "Verify result state and release allocated resources"
                )

                programs.add(
                    LabProgramItem(
                        programNumber = progIndex++,
                        title = cleanTitle,
                        shortTitle = shortTitle,
                        language = lang,
                        fullCode = code,
                        blueprintBlock = extractBlueprintBlock(code, lang),
                        coreFunctionBlock = extractCoreFunctionBlock(code, lang),
                        guardBlock = extractGuardBlock(code, lang),
                        driverBlock = extractDriverBlock(code, lang),
                        algorithmSteps = finalSteps,
                        vivaQuestions = if (vivaList.isNotEmpty()) vivaList else listOf(
                            "What is the time complexity of this practical?" to "Optimal runtime proportional to input size N.",
                            "Why is this data structure selected for this problem?" to "Provides required operational constraints with minimal space overhead."
                        ),
                        crashTraps = traps,
                        masteryStatus = ProgramMasteryStatus.UNTOUCHED
                    )
                )
            }
            if (programs.isNotEmpty()) return programs
        }

        // 2. Fallback: Parse directly from subject.units
        val allLessons = subject.units.flatMap { it.lessons }
        if (allLessons.isNotEmpty()) {
            return allLessons.mapIndexed { idx, lesson ->
                val pNum = idx + 1
                val lang = if (subject.name.contains("git", ignoreCase = true)) "bash" else if (subject.name.contains("python", ignoreCase = true)) "python" else "C"
                val steps = if (lesson.learnPoints.size >= 3) {
                    lesson.learnPoints.take(5)
                } else {
                    listOf(
                        "Set up preconditions and input data",
                        "Validate boundary conditions and parameters",
                        "Execute primary logic routine",
                        "Output results and release resources"
                    )
                }
                val viva = mutableListOf<Pair<String, String>>()
                lesson.examPrep?.practiceQuestions?.forEach { q ->
                    viva.add(q to (lesson.examPrep.fiveMarkAnswer.take(100).ifBlank { "Core concept from syllabus." }))
                }
                if (viva.isEmpty()) {
                    viva.add("What is the main objective of this lab program?" to lesson.description.ifBlank { lesson.title })
                }
                LabProgramItem(
                    programNumber = pNum,
                    title = lesson.title,
                    shortTitle = lesson.shortTitle.ifBlank { lesson.title.split("(").first().trim().take(24) },
                    language = lang,
                    fullCode = "// Implementation for ${lesson.title}\n// Refer to course syllabus manual",
                    blueprintBlock = "// Data structures and includes for ${lesson.title}",
                    coreFunctionBlock = "// Core algorithmic function for ${lesson.title}",
                    guardBlock = "// Boundary guard checks",
                    driverBlock = "// Driver routine / main()",
                    algorithmSteps = steps,
                    vivaQuestions = viva,
                    crashTraps = listOf("Missing boundary check on invalid inputs", "Off-by-one condition in loop traversal"),
                    masteryStatus = ProgramMasteryStatus.UNTOUCHED
                )
            }
        }

        return emptyList()
    }

    private fun extractBlueprintBlock(code: String, lang: String): String {
        if (code.isBlank()) return "// Blueprint specifications"
        val lines = code.lines()
        val blueprintLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.startsWith("typedef") || trimmed.startsWith("struct") ||
                trimmed.startsWith("import") || trimmed.startsWith("from") || trimmed.startsWith("class") ||
                trimmed.startsWith("//") || trimmed.isBlank() || trimmed.endsWith(";")) {
                if (trimmed.contains("main(") || trimmed.contains("void ") || (trimmed.contains("int ") && trimmed.endsWith("{"))) {
                    break
                }
                blueprintLines.add(line)
            } else {
                break
            }
        }
        return if (blueprintLines.isNotEmpty()) blueprintLines.joinToString("\n").trim() else code.take(200)
    }

    private fun extractCoreFunctionBlock(code: String, lang: String): String {
        if (code.isBlank()) return "// Core algorithmic routine"
        val mainIdx = code.indexOf("int main")
        if (mainIdx != -1) {
            val beforeMain = code.substring(0, mainIdx).trim()
            val lastFuncStart = listOf("void ", "int ", "bool ", "struct ", "def ").map { beforeMain.lastIndexOf(it) }.filter { it != -1 }.maxOrNull()
            if (lastFuncStart != null && lastFuncStart < beforeMain.length) {
                return beforeMain.substring(lastFuncStart).trim()
            }
        }
        return code.lines().take(25).joinToString("\n")
    }

    private fun extractGuardBlock(code: String, lang: String): String {
        val guardLine = code.lines().firstOrNull {
            it.contains("if (") && (it.contains("NULL") || it.contains("== -1") || it.contains("MAX") || it.contains("empty") || it.contains("full") || it.contains("overflow") || it.contains("underflow"))
        }
        return guardLine?.trim() ?: "if (ptr == NULL || n <= 0) return; // Boundary guard"
    }

    private fun extractDriverBlock(code: String, lang: String): String {
        val mainIdx = code.indexOf("int main")
        if (mainIdx != -1) {
            return code.substring(mainIdx).trim()
        }
        return "// Driver execution loop"
    }

    /**
     * Loads the lab programs for a subject. If the user has attached/parsed a custom lab manual,
     * returns those custom programs. Otherwise, loads the genuine syllabus practicals.
     */
    fun getLabProgramsForSubject(context: Context, subject: SubjectJourney): List<LabProgramItem> {
        val customList = getCustomLabPrograms(context, subject.id)
        val baseList = if (customList != null && customList.isNotEmpty()) {
            customList
        } else {
            loadSyllabusLabPrograms(context, subject)
        }

        // Apply saved mastery statuses
        return baseList.map { program ->
            val savedMastery = getProgramMastery(context, subject.id, program.programNumber)
            program.copy(masteryStatus = savedMastery)
        }
    }
}
