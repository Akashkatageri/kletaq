package com.kletaq.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.core.academic.ModelPaperAiSolver
import com.kletaq.app.data.model.QuestionPaper
import com.kletaq.app.data.model.VtuPaperInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for University Model Question Papers and Solved Answer Schemes.
 * Manages in-memory caching, Firestore cloud sync, and Gemini AI synthesis.
 */
@Singleton
class QuestionPaperRepository @Inject constructor() {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val memoryCache = mutableMapOf<String, QuestionPaper>()

    fun getQuestionPaperFlow(
        subjectCode: String,
        subjectName: String,
        semester: Int = 1,
        moduleThemes: List<String> = emptyList()
    ): Flow<Result<QuestionPaper>> = flow {
        val paperInfo = VtuPaperCatalog.resolvePaper(subjectCode, subjectName, semester)
        val cacheKey = paperInfo.vtuPaperCode.uppercase()

        // 1. In-Memory Cache (Instant 0ms)
        memoryCache[cacheKey]?.let {
            emit(Result.success(it))
            return@flow
        }

        // 2. Local Bundled / Deterministic Baseline emitted first for snappy UI
        val baseline = ModelPaperAiSolver.buildDeterministicPaper(paperInfo, moduleThemes)
        emit(Result.success(baseline))

        // 3. Cloud Firestore Cache
        try {
            val docRef = firestore.collection("model_papers").document(cacheKey)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val cachedJson = snapshot.getString("paperJson")
                if (!cachedJson.isNullOrBlank()) {
                    // Cached paper available
                    val parsed = parseCachedJson(cachedJson, paperInfo)
                    if (parsed != null) {
                        memoryCache[cacheKey] = parsed
                        emit(Result.success(parsed))
                        return@flow
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("QuestionPaperRepo", "Firestore cache lookup failed: ${e.message}")
        }

        // 4. Generate with Gemini AI
        try {
            val generated = ModelPaperAiSolver.generateOrSolvePaper(paperInfo, moduleThemes)
            memoryCache[cacheKey] = generated
            emit(Result.success(generated))

            // Save to Firestore asynchronously
            try {
                val data = mapOf(
                    "vtuCode" to paperInfo.vtuPaperCode,
                    "subjectTitle" to paperInfo.subjectTitle,
                    "semester" to paperInfo.semester,
                    "pdfUrl" to paperInfo.officialPdfUrl,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("model_papers").document(cacheKey)
                    .set(data, SetOptions.merge())
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e("QuestionPaperRepo", "Gemini generation failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    private fun parseCachedJson(json: String, paperInfo: VtuPaperInfo): QuestionPaper? {
        return null // Will be parsed via ModelPaperAiSolver when serialized
    }

    fun openOfficialPdf(context: Context, pdfUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(pdfUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("QuestionPaperRepo", "Could not launch PDF intent: ${e.message}")
        }
    }
}
