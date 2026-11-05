package com.kletaq.app.features.lab

import androidx.compose.ui.graphics.Color

enum class ProgramMasteryStatus(val label: String, val badgeColor: Color, val emoji: String) {
    UNTOUCHED("Untouched", Color(0xFF6B7280), "⚪"),
    SKELETON_LEARNED("Skeleton Learned", Color(0xFF3B82F6), "🔵"),
    BLIND_VERIFIED("Blind Verified", Color(0xFFF59E0B), "🟡"),
    COLLEGE_EXECUTED("College Executed", Color(0xFF10B981), "🟢")
}

data class LabKernel(
    val id: String,
    val name: String,
    val codeSnippet: String,
    val examTrick: String
)

data class DryRunQuestion(
    val scenarioText: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class ParsonsPuzzle(
    val prompt: String,
    val scrambledLines: List<String>,
    val correctLines: List<String>,
    val explanation: String
)

data class ClozeDrill(
    val prompt: String,
    val codeTemplate: String,
    val blanks: List<String>,
    val chipOptions: List<String>
)

data class BugHuntDrill(
    val prompt: String,
    val codeLinesWithNumbers: List<Pair<Int, String>>,
    val buggyLineNumber: Int,
    val explanation: String
)

data class LabProgramDrills(
    val dryRun: DryRunQuestion? = null,
    val parsonsPuzzle: ParsonsPuzzle? = null,
    val clozeDrill: ClozeDrill? = null,
    val bugHunt: BugHuntDrill? = null
)

data class LabProgramItem(
    val programNumber: Int,
    val title: String,
    val shortTitle: String,
    val language: String = "C",
    val fullCode: String,
    val blueprintBlock: String = "",
    val coreFunctionBlock: String = "",
    val guardBlock: String = "",
    val driverBlock: String = "",
    val algorithmSteps: List<String> = emptyList(),
    val vivaQuestions: List<Pair<String, String>> = emptyList(),
    val crashTraps: List<String> = emptyList(),
    val masteryStatus: ProgramMasteryStatus = ProgramMasteryStatus.UNTOUCHED,
    val blindWriteAttempts: Int = 0,
    val lastPracticedEpoch: Long = 0L,
    val kernels: List<LabKernel> = emptyList(),
    val drills: LabProgramDrills? = null,
    val aim: String = "",
    val inputOutputSample: String = ""
)

data class ExamSimulationChits(
    val chit1: LabProgramItem,
    val chit2: LabProgramItem,
    val startedEpoch: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 180
)
