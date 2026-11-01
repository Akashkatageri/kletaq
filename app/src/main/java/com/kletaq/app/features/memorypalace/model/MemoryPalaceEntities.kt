package com.kletaq.app.features.memorypalace.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 🏛️ Memory Palace Entity
 * Represents a physical environment (e.g. "My Hostel Room", "Home Living Room")
 */
data class MemoryPalace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val totalLociCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description ?: "")
        put("totalLociCount", totalLociCount)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): MemoryPalace = MemoryPalace(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name", "Untitled Palace"),
            description = json.optString("description").takeIf { it.isNotBlank() },
            totalLociCount = json.optInt("totalLociCount", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

/**
 * 📸 Palace Image
 * A wide room or area photograph inside a Palace
 */
data class PalaceImage(
    val id: String = UUID.randomUUID().toString(),
    val palaceId: String,
    val imageOrder: Int = 1,
    val photoPath: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("palaceId", palaceId)
        put("imageOrder", imageOrder)
        put("photoPath", photoPath)
    }

    companion object {
        fun fromJson(json: JSONObject): PalaceImage = PalaceImage(
            id = json.optString("id", UUID.randomUUID().toString()),
            palaceId = json.optString("palaceId", ""),
            imageOrder = json.optInt("imageOrder", 1),
            photoPath = json.optString("photoPath", "")
        )
    }
}

/**
 * 📍 Palace Locus
 * A pinned landmark/object in a specific Palace Image with relative coordinates
 */
data class PalaceLocus(
    val id: String = UUID.randomUUID().toString(),
    val imageId: String,
    val palaceId: String,
    val orderIndex: Int, // Global sequential index in palace (1, 2, 3...)
    val name: String,
    val xPercent: Float, // 0.0f to 1.0f relative to image width
    val yPercent: Float  // 0.0f to 1.0f relative to image height
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("imageId", imageId)
        put("palaceId", palaceId)
        put("orderIndex", orderIndex)
        put("name", name)
        put("xPercent", xPercent.toDouble())
        put("yPercent", yPercent.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): PalaceLocus {
            val order = json.optInt("orderIndex", 1)
            return PalaceLocus(
                id = json.optString("id", UUID.randomUUID().toString()),
                imageId = json.optString("imageId", ""),
                palaceId = json.optString("palaceId", ""),
                orderIndex = order,
                name = json.optString("name", "Locus $order"),
                xPercent = json.optDouble("xPercent", 0.5).toFloat(),
                yPercent = json.optDouble("yPercent", 0.5).toFloat()
            )
        }
    }
}

/**
 * 📜 Palace Derivation
 * A mathematical derivation or algorithm mapped to a Memory Palace
 */
data class PalaceDerivation(
    val id: String = UUID.randomUUID().toString(),
    val palaceId: String,
    val title: String,
    val subjectId: String? = null,
    val mode: String = "EXAM_SPRINT", // "EXAM_SPRINT" or "SEMESTER"
    val examTimestamp: Long? = null,
    val nextReviewTime: Long = System.currentTimeMillis(),
    val reviewStage: Int = 0, // 0: Pass 1, 1: Pass 2, 2: Pass 3, 3+: Mastered
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("palaceId", palaceId)
        put("title", title)
        put("subjectId", subjectId ?: "")
        put("mode", mode)
        put("examTimestamp", examTimestamp ?: -1L)
        put("nextReviewTime", nextReviewTime)
        put("reviewStage", reviewStage)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): PalaceDerivation = PalaceDerivation(
            id = json.optString("id", UUID.randomUUID().toString()),
            palaceId = json.optString("palaceId", ""),
            title = json.optString("title", "Untitled Derivation"),
            subjectId = json.optString("subjectId").takeIf { it.isNotBlank() },
            mode = json.optString("mode", "EXAM_SPRINT"),
            examTimestamp = json.optLong("examTimestamp", -1L).takeIf { it > 0L },
            nextReviewTime = json.optLong("nextReviewTime", System.currentTimeMillis()),
            reviewStage = json.optInt("reviewStage", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

/**
 * ⚡ Palace Step
 * A single transformation/step of a derivation anchored to a Locus
 */
data class PalaceStep(
    val id: String = UUID.randomUUID().toString(),
    val derivationId: String,
    val stepNumber: Int,
    val locusOrderIndex: Int,
    val stepTitle: String,
    val formulaLatex: String,
    val sensoryStory: String,
    val examTrap: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("derivationId", derivationId)
        put("stepNumber", stepNumber)
        put("locusOrderIndex", locusOrderIndex)
        put("stepTitle", stepTitle)
        put("formulaLatex", formulaLatex)
        put("sensoryStory", sensoryStory)
        put("examTrap", examTrap)
    }

    companion object {
        fun fromJson(json: JSONObject): PalaceStep {
            val stepNum = json.optInt("stepNumber", 1)
            return PalaceStep(
                id = json.optString("id", UUID.randomUUID().toString()),
                derivationId = json.optString("derivationId", ""),
                stepNumber = stepNum,
                locusOrderIndex = json.optInt("locusOrderIndex", 1),
                stepTitle = json.optString("stepTitle", "Step $stepNum"),
                formulaLatex = json.optString("formulaLatex", ""),
                sensoryStory = json.optString("sensoryStory", ""),
                examTrap = json.optString("examTrap", "")
            )
        }
    }
}

/**
 * Composite view of a palace with its photos and loci
 */
data class PalaceWithImagesAndLoci(
    val palace: MemoryPalace,
    val images: List<PalaceImage>,
    val loci: List<PalaceLocus>
)

/**
 * Composite view of a derivation with all ordered steps
 */
data class DerivationWithSteps(
    val derivation: PalaceDerivation,
    val steps: List<PalaceStep>
)

