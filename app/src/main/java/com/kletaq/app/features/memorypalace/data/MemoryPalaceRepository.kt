package com.kletaq.app.features.memorypalace.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kletaq.app.core.di.KletaqApplication
import com.kletaq.app.features.memorypalace.model.DerivationWithSteps
import com.kletaq.app.features.memorypalace.model.MemoryPalace
import com.kletaq.app.features.memorypalace.model.PalaceDerivation
import com.kletaq.app.features.memorypalace.model.PalaceImage
import com.kletaq.app.features.memorypalace.model.PalaceLocus
import com.kletaq.app.features.memorypalace.model.PalaceStep
import com.kletaq.app.features.memorypalace.model.PalaceWithImagesAndLoci
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File

/**
 * 📦 Offline-First Repository for Memory Palaces and Derivations.
 * Manages palaces, loci pins, derivations, steps, and spaced repetition scheduling.
 */
object MemoryPalaceRepository {

    private const val TAG = "MemoryPalaceRepository"
    private const val PREFS_NAME = "kletaq_memory_palace_store"
    private const val KEY_PALACES_JSON = "palaces_json"
    private const val KEY_IMAGES_JSON = "images_json"
    private const val KEY_LOCI_JSON = "loci_json"
    private const val KEY_DERIVATIONS_JSON = "derivations_json"
    private const val KEY_STEPS_JSON = "steps_json"

    private val _palaces = MutableStateFlow<List<MemoryPalace>>(emptyList())
    val palaces: StateFlow<List<MemoryPalace>> = _palaces.asStateFlow()

    private val _derivations = MutableStateFlow<List<PalaceDerivation>>(emptyList())
    val derivations: StateFlow<List<PalaceDerivation>> = _derivations.asStateFlow()

    private val _images = mutableListOf<PalaceImage>()
    private val _loci = mutableListOf<PalaceLocus>()
    private val _steps = mutableListOf<PalaceStep>()

    init {
        loadFromStorage()
    }

    private fun getPrefs(): SharedPreferences? {
        return try {
            val ctx = KletaqApplication.appContext
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null // In pure unit tests without Android Context
        }
    }

    @Synchronized
    fun loadFromStorage() {
        val prefs = getPrefs() ?: return
        try {
            // 1. Palaces
            val palacesStr = prefs.getString(KEY_PALACES_JSON, null)
            if (!palacesStr.isNullOrBlank()) {
                val array = JSONArray(palacesStr)
                val list = mutableListOf<MemoryPalace>()
                for (i in 0 until array.length()) {
                    list.add(MemoryPalace.fromJson(array.getJSONObject(i)))
                }
                _palaces.value = list
            }

            // 2. Images
            val imagesStr = prefs.getString(KEY_IMAGES_JSON, null)
            _images.clear()
            if (!imagesStr.isNullOrBlank()) {
                val array = JSONArray(imagesStr)
                for (i in 0 until array.length()) {
                    _images.add(PalaceImage.fromJson(array.getJSONObject(i)))
                }
            }

            // 3. Loci
            val lociStr = prefs.getString(KEY_LOCI_JSON, null)
            _loci.clear()
            if (!lociStr.isNullOrBlank()) {
                val array = JSONArray(lociStr)
                for (i in 0 until array.length()) {
                    _loci.add(PalaceLocus.fromJson(array.getJSONObject(i)))
                }
            }

            // 4. Derivations
            val derivStr = prefs.getString(KEY_DERIVATIONS_JSON, null)
            if (!derivStr.isNullOrBlank()) {
                val array = JSONArray(derivStr)
                val list = mutableListOf<PalaceDerivation>()
                for (i in 0 until array.length()) {
                    list.add(PalaceDerivation.fromJson(array.getJSONObject(i)))
                }
                _derivations.value = list
            }

            // 5. Steps
            val stepsStr = prefs.getString(KEY_STEPS_JSON, null)
            _steps.clear()
            if (!stepsStr.isNullOrBlank()) {
                val array = JSONArray(stepsStr)
                for (i in 0 until array.length()) {
                    _steps.add(PalaceStep.fromJson(array.getJSONObject(i)))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading memory palace store: ${e.message}", e)
        }
    }

    @Synchronized
    private fun persistAll() {
        val prefs = getPrefs() ?: return
        try {
            val palacesArr = JSONArray()
            _palaces.value.forEach { palacesArr.put(it.toJson()) }

            val imagesArr = JSONArray()
            _images.forEach { imagesArr.put(it.toJson()) }

            val lociArr = JSONArray()
            _loci.forEach { lociArr.put(it.toJson()) }

            val derivArr = JSONArray()
            _derivations.value.forEach { derivArr.put(it.toJson()) }

            val stepsArr = JSONArray()
            _steps.forEach { stepsArr.put(it.toJson()) }

            prefs.edit()
                .putString(KEY_PALACES_JSON, palacesArr.toString())
                .putString(KEY_IMAGES_JSON, imagesArr.toString())
                .putString(KEY_LOCI_JSON, lociArr.toString())
                .putString(KEY_DERIVATIONS_JSON, derivArr.toString())
                .putString(KEY_STEPS_JSON, stepsArr.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting memory palace store: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------------------
    // Palace CRUD
    // -------------------------------------------------------------------------

    @Synchronized
    fun savePalaceWithPins(
        palace: MemoryPalace,
        images: List<PalaceImage>,
        loci: List<PalaceLocus>
    ) {
        val currentPalaces = _palaces.value.filter { it.id != palace.id }.toMutableList()
        currentPalaces.add(0, palace)
        _palaces.value = currentPalaces

        // Remove old images/loci for this palace
        _images.removeAll { it.palaceId == palace.id }
        _images.addAll(images)

        _loci.removeAll { it.palaceId == palace.id }
        _loci.addAll(loci)

        persistAll()
    }

    @Synchronized
    fun getPalaceWithLoci(palaceId: String): PalaceWithImagesAndLoci? {
        val palace = _palaces.value.firstOrNull { it.id == palaceId } ?: return null
        val images = _images.filter { it.palaceId == palaceId }.sortedBy { it.imageOrder }
        val loci = _loci.filter { it.palaceId == palaceId }.sortedBy { it.orderIndex }
        return PalaceWithImagesAndLoci(palace, images, loci)
    }

    @Synchronized
    fun deletePalace(palaceId: String) {
        val palaceImages = _images.filter { it.palaceId == palaceId }
        // Clean up image files from storage
        palaceImages.forEach { img ->
            try {
                val f = File(img.photoPath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete file ${img.photoPath}: ${e.message}")
            }
        }

        _palaces.value = _palaces.value.filter { it.id != palaceId }
        _images.removeAll { it.palaceId == palaceId }
        _loci.removeAll { it.palaceId == palaceId }

        // Cascade delete derivations
        val associatedDerivs = _derivations.value.filter { it.palaceId == palaceId }
        associatedDerivs.forEach { d ->
            d.sourceDocumentPaths.forEach { path ->
                try {
                    val f = File(path)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete source document $path: ${e.message}")
                }
            }
            _steps.removeAll { it.derivationId == d.id }
        }
        _derivations.value = _derivations.value.filter { it.palaceId != palaceId }

        persistAll()
    }

    // -------------------------------------------------------------------------
    // Derivation CRUD
    // -------------------------------------------------------------------------

    @Synchronized
    fun saveDerivationWithSteps(
        derivation: PalaceDerivation,
        steps: List<PalaceStep>
    ) {
        val current = _derivations.value.filter { it.id != derivation.id }.toMutableList()
        current.add(0, derivation)
        _derivations.value = current

        _steps.removeAll { it.derivationId == derivation.id }
        _steps.addAll(steps)

        persistAll()
    }

    @Synchronized
    fun getDerivationWithSteps(derivationId: String): DerivationWithSteps? {
        val deriv = _derivations.value.firstOrNull { it.id == derivationId } ?: return null
        val steps = _steps.filter { it.derivationId == derivationId }.sortedBy { it.stepNumber }
        return DerivationWithSteps(deriv, steps)
    }

    @Synchronized
    fun deleteDerivation(derivationId: String) {
        val deriv = _derivations.value.firstOrNull { it.id == derivationId }
        deriv?.sourceDocumentPaths?.forEach { path ->
            try {
                val f = File(path)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete source document $path: ${e.message}")
            }
        }
        _derivations.value = _derivations.value.filter { it.id != derivationId }
        _steps.removeAll { it.derivationId == derivationId }
        persistAll()
    }

    @Synchronized
    fun getDueDerivations(): List<PalaceDerivation> {
        val now = System.currentTimeMillis()
        return _derivations.value
            .filter { it.nextReviewTime <= now && it.reviewStage < 4 }
            .sortedBy { it.nextReviewTime }
    }

    // -------------------------------------------------------------------------
    // Spaced Repetition Engine
    // -------------------------------------------------------------------------

    /**
     * Calculates the next review timestamp and progression stage based on recall feedback.
     */
    /**
     * Recall rating for SM-2 Spaced Repetition review progression.
     */
    enum class RecallRating {
        HARD,
        GOOD,
        EASY
    }

    /**
     * Calculates the next review timestamp and progression stage based on 3-tier recall feedback.
     */
    fun calculateNextReview(
        mode: String,
        currentStage: Int,
        examTimestamp: Long?,
        rating: RecallRating
    ): Pair<Long, Int> {
        val now = System.currentTimeMillis()

        if (rating == RecallRating.HARD) {
            // Struggled recall -> stay or step back, trigger fast retry
            return if (mode == "EXAM_SPRINT") {
                val retry = now + 15 * 60 * 1000L // 15 mins
                Pair(retry, currentStage)
            } else {
                val regressedStage = maxOf(0, currentStage - 1)
                val retry = now + 4 * 3600 * 1000L // 4 hours
                Pair(retry, regressedStage)
            }
        }

        val easyMultiplier = if (rating == RecallRating.EASY) 1.5f else 1.0f

        // Clean recall progression
        if (mode == "EXAM_SPRINT") {
            return when (currentStage) {
                0 -> {
                    // Pass 1 -> Pass 2 (+30 mins, or +45 mins on EASY)
                    val interval = (30 * 60 * 1000L * easyMultiplier).toLong()
                    Pair(now + interval, 1)
                }
                1 -> {
                    // Pass 2 -> Pass 3 (Evening / before sleep: approx +3-4 hours or relative to exam)
                    val baseHours = if (examTimestamp != null && examTimestamp > now + 12 * 3600 * 1000L) 4L else 3L
                    val interval = ((baseHours * 3600 * 1000L) * easyMultiplier).toLong()
                    Pair(now + interval, 2)
                }
                2 -> {
                    // Pass 3 -> Final morning-of exam review (2 hours before exam)
                    val next = if (examTimestamp != null && examTimestamp > now) {
                        val twoHoursBefore = examTimestamp - 2 * 3600 * 1000L
                        if (twoHoursBefore > now) twoHoursBefore else now + 60 * 60 * 1000L
                    } else {
                        val interval = (6 * 3600 * 1000L * easyMultiplier).toLong()
                        now + interval
                    }
                    Pair(next, 3)
                }
                else -> {
                    // Mastered
                    Pair(Long.MAX_VALUE, 4)
                }
            }
        } else {
            // Semester Mastery standard intervals (scaled by easy multiplier)
            return when (currentStage) {
                0 -> Pair(now + (24 * 3600 * 1000L * easyMultiplier).toLong(), 1) // Day 1
                1 -> Pair(now + (3 * 24 * 3600 * 1000L * easyMultiplier).toLong(), 2) // Day 3
                2 -> Pair(now + (7 * 24 * 3600 * 1000L * easyMultiplier).toLong(), 3) // Day 7
                3 -> Pair(now + (14 * 24 * 3600 * 1000L * easyMultiplier).toLong(), 4) // Day 14
                else -> Pair(now + (30 * 24 * 3600 * 1000L * easyMultiplier).toLong(), 5) // Day 30
            }
        }
    }

    /**
     * Backward-compatible overload for existing Boolean callers.
     */
    fun calculateNextReview(
        mode: String,
        currentStage: Int,
        examTimestamp: Long?,
        rememberedCleanly: Boolean
    ): Pair<Long, Int> {
        val rating = if (rememberedCleanly) RecallRating.GOOD else RecallRating.HARD
        return calculateNextReview(mode, currentStage, examTimestamp, rating)
    }

    @Synchronized
    fun recordStepReview(derivationId: String, rating: RecallRating): PalaceDerivation? {
        val deriv = _derivations.value.firstOrNull { it.id == derivationId } ?: return null
        val (nextTime, nextStage) = calculateNextReview(
            mode = deriv.mode,
            currentStage = deriv.reviewStage,
            examTimestamp = deriv.examTimestamp,
            rating = rating
        )

        val updated = deriv.copy(
            nextReviewTime = nextTime,
            reviewStage = nextStage
        )

        val list = _derivations.value.toMutableList()
        val index = list.indexOfFirst { it.id == derivationId }
        if (index != -1) {
            list[index] = updated
            _derivations.value = list
            persistAll()
        }

        return updated
    }

    @Synchronized
    fun recordStepReview(derivationId: String, rememberedCleanly: Boolean): PalaceDerivation? {
        return recordStepReview(derivationId, if (rememberedCleanly) RecallRating.GOOD else RecallRating.HARD)
    }
}

