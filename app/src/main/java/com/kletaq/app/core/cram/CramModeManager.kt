package com.kletaq.app.core.cram

import android.content.Context
import android.content.SharedPreferences
import com.kletaq.app.data.model.TopicStudyPack
import com.kletaq.app.features.journey.components.LessonNode
import com.kletaq.app.features.journey.components.UnitJourney
import com.kletaq.app.widgets.data.WidgetDataHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CramState(
    val isActive: Boolean = false,
    val targetSubjectCode: String = "",
    val targetSubjectName: String = "",
    val examTimestampMillis: Long = 0L
) {
    val hoursRemaining: Int
        get() {
            if (!isActive || examTimestampMillis <= 0L) return 0
            val diff = examTimestampMillis - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60)).coerceAtLeast(0).toInt()
        }
}

/**
 * 🚨 48-Hour Exam Cram Mode Manager
 *
 * Optimizes engineering exam preparation by:
 * 1. Module OR-Choice Selection: In VTU / autonomous semester exams, each module has an OR choice.
 *    Cram Mode picks the single highest-yield topic per module to eliminate redundant studying.
 * 2. 80/20 Filter: Highlights guaranteed 7/8M derivations & 4/5/6M numericals.
 * 3. 3× High-Stakes Bonus XP during cram focus sessions.
 * 4. Panda Glance Widget synchronization: Locks companion into `panda_revision`.
 */
object CramModeManager {

    private const val PREFS_NAME = "kletaq_cram_mode_prefs"
    private const val KEY_ACTIVE = "cram_is_active"
    private const val KEY_SUBJECT_CODE = "cram_target_subject_code"
    private const val KEY_SUBJECT_NAME = "cram_target_subject_name"
    private const val KEY_EXAM_TIMESTAMP = "cram_exam_timestamp"

    private val _cramState = MutableStateFlow(CramState())
    val cramState: StateFlow<CramState> = _cramState.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        val subjectCode = prefs.getString(KEY_SUBJECT_CODE, "") ?: ""
        val subjectName = prefs.getString(KEY_SUBJECT_NAME, "") ?: ""
        val timestamp = prefs.getLong(KEY_EXAM_TIMESTAMP, 0L)

        // Check if cram window expired
        if (active && timestamp > 0L && System.currentTimeMillis() > timestamp) {
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
            _cramState.value = CramState(isActive = false)
            WidgetDataHelper.setManualMood(context, null)
        } else {
            _cramState.value = CramState(
                isActive = active,
                targetSubjectCode = subjectCode,
                targetSubjectName = subjectName,
                examTimestampMillis = timestamp
            )
            if (active) {
                WidgetDataHelper.setManualMood(context, "exam_revision")
            }
        }
        isInitialized = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun activateCramMode(
        context: Context,
        subjectCode: String,
        subjectName: String,
        durationHours: Int = 48
    ) {
        val targetTimestamp = System.currentTimeMillis() + (durationHours * 3600 * 1000L)
        getPrefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_SUBJECT_CODE, subjectCode)
            .putString(KEY_SUBJECT_NAME, subjectName)
            .putLong(KEY_EXAM_TIMESTAMP, targetTimestamp)
            .apply()

        _cramState.value = CramState(
            isActive = true,
            targetSubjectCode = subjectCode,
            targetSubjectName = subjectName,
            examTimestampMillis = targetTimestamp
        )

        // Lock Panda widget into panda_revision ("Revision time! 📝")
        WidgetDataHelper.setManualMood(context, "exam_revision")
    }

    fun deactivateCramMode(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_ACTIVE, false)
            .apply()

        _cramState.value = CramState(isActive = false)

        // Restore dynamic Panda widget moods
        WidgetDataHelper.setManualMood(context, null)
    }

    fun getXpMultiplier(): Int {
        return if (_cramState.value.isActive) 3 else 1
    }

    /**
     * Module OR-Choice Selection:
     * In an engineering exam module with 2 questions (e.g. Q1 or Q2), this picks
     * the single topic with the highest marks or guaranteed past paper recurrence.
     */
    fun getOrChoicePickForUnit(
        unit: UnitJourney,
        studyPacksMap: Map<String, TopicStudyPack> = emptyMap()
    ): String? {
        if (unit.lessons.isEmpty()) return null

        // 1. Look for topic with highest marks in examQuestions
        var bestLesson: LessonNode? = null
        var maxMarks = -1
        var hasGuaranteedPyq = false

        for (lesson in unit.lessons) {
            val pack = studyPacksMap[lesson.id]
            if (pack != null && pack.examQuestions.isNotEmpty()) {
                val guaranteed = pack.examQuestions.any { it.isGuaranteedPyq }
                val highestMark = pack.examQuestions.maxOfOrNull { it.marks } ?: 0

                if (guaranteed && !hasGuaranteedPyq) {
                    hasGuaranteedPyq = true
                    bestLesson = lesson
                    maxMarks = highestMark
                } else if (guaranteed == hasGuaranteedPyq && highestMark > maxMarks) {
                    bestLesson = lesson
                    maxMarks = highestMark
                }
            }
        }

        // 2. Fallback: topic with formulas / derivations, or first available lesson
        return bestLesson?.id ?: unit.lessons.firstOrNull()?.id
    }

    fun isOrChoicePick(
        unit: UnitJourney,
        topicId: String,
        studyPacksMap: Map<String, TopicStudyPack> = emptyMap()
    ): Boolean {
        if (!_cramState.value.isActive) return false
        val pickId = getOrChoicePickForUnit(unit, studyPacksMap)
        return pickId != null && pickId == topicId
    }
}
