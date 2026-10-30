package com.kletaq.app.features.lab

import androidx.compose.ui.graphics.Color

enum class ProgramMasteryStatus(val label: String, val badgeColor: Color, val emoji: String) {
    UNTOUCHED("Untouched", Color(0xFF6B7280), "⚪"),
    SKELETON_LEARNED("Skeleton Learned", Color(0xFF3B82F6), "🔵"),
    BLIND_VERIFIED("Blind Verified", Color(0xFFF59E0B), "🟡"),
    COLLEGE_EXECUTED("College Executed", Color(0xFF10B981), "🟢")
}

data class LabProgramItem(
    val programNumber: Int,
    val title: String,
    val shortTitle: String,
    val language: String = "C",
    val fullCode: String,
    val blueprintBlock: String,
    val coreFunctionBlock: String,
    val guardBlock: String,
    val driverBlock: String,
    val algorithmSteps: List<String>,
    val vivaQuestions: List<Pair<String, String>>,
    val crashTraps: List<String>,
    val masteryStatus: ProgramMasteryStatus = ProgramMasteryStatus.UNTOUCHED,
    val blindWriteAttempts: Int = 0,
    val lastPracticedEpoch: Long = 0L
)

data class ExamSimulationChits(
    val chit1: LabProgramItem,
    val chit2: LabProgramItem,
    val startedEpoch: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 180
)

