package com.kletaq.app.core.academic

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max

/**
 * Manages fixed university exam dates per subject and applies dynamic SM-2 interval tapering.
 *
 * Prevents standard exponential intervals from scheduling critical review sessions after
 * the student's final university exam has already passed.
 */
object ExamScheduleManager {

    private const val PREFS_NAME = "kletaq_exam_schedule_prefs"
    private const val KEY_PREFIX_EXAM = "exam_epoch_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getExamDate(context: Context, subjectId: String): Long? {
        val normalizedId = subjectId.trim().uppercase()
        val epoch = getPrefs(context).getLong("$KEY_PREFIX_EXAM$normalizedId", -1L)
        return if (epoch > 0L) epoch else null
    }

    fun getExamEpoch(context: Context, subjectId: String): Long? = getExamDate(context, subjectId)

    fun setExamDate(context: Context, subjectId: String, epochMillis: Long) {
        val normalizedId = subjectId.trim().uppercase()
        getPrefs(context).edit().putLong("$KEY_PREFIX_EXAM$normalizedId", epochMillis).apply()
    }

    fun clearExamDate(context: Context, subjectId: String) {
        val normalizedId = subjectId.trim().uppercase()
        getPrefs(context).edit().remove("$KEY_PREFIX_EXAM$normalizedId").apply()
    }

    fun getDaysUntilExam(context: Context, subjectId: String, nowTimestamp: Long = System.currentTimeMillis()): Int? {
        val examEpoch = getExamDate(context, subjectId) ?: return null
        val diffDays = ((examEpoch - nowTimestamp) / (1000 * 60 * 60 * 24)).toInt()
        return diffDays
    }

    /**
     * Pre-Exam Intake Cap: 5 days before the exam, automatically freeze new lesson intake
     * (newCardsPerDay = 0) for that course so the student enters pure revision mode without
     * last-minute cramming overload.
     */
    fun isPreExamIntakeCapped(context: Context, subjectId: String, nowTimestamp: Long = System.currentTimeMillis()): Boolean {
        val daysUntilExam = getDaysUntilExam(context, subjectId, nowTimestamp) ?: return false
        return daysUntilExam in 0..5
    }

    /**
     * Tapers SM-2 calculated intervals when an exam date is within 30 days, guaranteeing
     * a final consolidation pass 24–48 hours before the exam morning.
     */
    fun calculateTaperedInterval(
        sm2CalculatedDays: Int,
        examEpoch: Long,
        nowTimestamp: Long = System.currentTimeMillis()
    ): Int {
        val daysUntilExam = ((examEpoch - nowTimestamp) / (1000 * 60 * 60 * 24)).toInt()
        if (daysUntilExam <= 0) return 1
        val finalReviewLeadDays = 2 // 48 hours prior
        val maxAllowedInterval = max(1, daysUntilExam - finalReviewLeadDays)
        return sm2CalculatedDays.coerceAtMost(maxAllowedInterval)
    }
}
