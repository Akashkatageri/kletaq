package com.kletaq.app.core.notes

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

import com.google.firebase.firestore.FirebaseFirestore

object CloudflareR2Uploader {

    private const val TAG = "CloudflareR2Uploader"

    // Cloudflare Custom Domain for Notes R2 bucket
    const val R2_PUBLIC_BASE_URL = "https://notes.5122006.xyz"

    // Cloudflare Worker API upload gateway (100k requests/day free tier)
    // Falls back to direct R2 public bucket PUT or local sandbox if offline
    var workerApiUrl = "https://kletaq-notes-api.d-katageri360.workers.dev"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Uploads compressed note pages to Cloudflare R2 bucket.
     * If network is unreachable or worker is not yet configured, gracefully copies to
     * offline app storage and returns persistent offline paths so the user NEVER loses notes!
     */
    suspend fun uploadNotePages(
        context: Context,
        noteId: String,
        compressedFiles: List<File>,
        onProgress: ((uploadedCount: Int, totalCount: Int, percent: Float) -> Unit)? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: "anonymous_user"

        val uploadedUrls = mutableListOf<String>()

        // 1. Ensure a permanent local backup exists in app sandbox first (offline-first architecture)
        val localPermanentDir = File(context.filesDir, "study_notes/$noteId").apply {
            if (!exists()) mkdirs()
        }

        try {
            compressedFiles.forEachIndexed { index, file ->
                val fileName = file.name
                val localFile = File(localPermanentDir, fileName)
                file.copyTo(localFile, overwrite = true)

                // Try uploading to Cloudflare R2 Worker
                val remoteKey = "users/$uid/notes/$noteId/$fileName"
                var finalUrl: String = localFile.absolutePath

                try {
                    val uploadUrl = "$workerApiUrl/upload/$remoteKey"
                    val requestBody = localFile.asRequestBody("image/jpeg".toMediaType())
                    val request = Request.Builder()
                        .url(uploadUrl)
                        .put(requestBody)
                        .addHeader("X-Kletaq-UID", uid)
                        .addHeader("X-Kletaq-Note-ID", noteId)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        finalUrl = "$R2_PUBLIC_BASE_URL/$remoteKey"
                        Log.i(TAG, "Uploaded page to R2: $finalUrl")
                    } else {
                        Log.w(TAG, "R2 Worker upload returned ${response.code}. Falling back to local offline storage.")
                    }
                } catch (netEx: Exception) {
                    Log.w(TAG, "Network unavailable for R2 upload: ${netEx.message}. Retaining local offline copy.")
                }

                uploadedUrls.add(finalUrl)
                onProgress?.invoke(
                    index + 1,
                    compressedFiles.size,
                    ((index + 1).toFloat() / compressedFiles.size.toFloat())
                )
            }

            Result.success(uploadedUrls)
        } catch (e: Exception) {
            Log.e(TAG, "Error in note upload pipeline: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads compressed module/topic note pages in learning nodes to Cloudflare R2
     * and syncs the metadata with Firestore.
     * Remote key: users/{uid}/modules/{cleanSubject}/m{moduleIndex}/{fileName}
     * Public URL: https://notes.5122006.xyz/users/{uid}/modules/{cleanSubject}/m{moduleIndex}/{fileName}
     */
    suspend fun uploadModulePages(
        context: Context,
        subjectCode: String,
        moduleIndex: Int,
        pageFiles: List<File>,
        onProgress: ((uploadedCount: Int, totalCount: Int, percent: Float) -> Unit)? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanSubject = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: "anonymous_user"

        val uploadedUrls = mutableListOf<String>()

        try {
            pageFiles.forEachIndexed { index, file ->
                val fileName = file.name
                val remoteKey = "users/$uid/modules/$cleanSubject/m$moduleIndex/$fileName"
                var finalUrl = file.absolutePath

                try {
                    val uploadUrl = "$workerApiUrl/upload/$remoteKey"
                    val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                    val request = Request.Builder()
                        .url(uploadUrl)
                        .put(requestBody)
                        .addHeader("X-Kletaq-UID", uid)
                        .addHeader("X-Kletaq-Subject", cleanSubject)
                        .addHeader("X-Kletaq-Module", moduleIndex.toString())
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        finalUrl = "$R2_PUBLIC_BASE_URL/$remoteKey"
                        Log.i(TAG, "Uploaded module page to R2: $finalUrl")
                    } else {
                        Log.w(TAG, "R2 Worker upload returned ${response.code}. Falling back to local file.")
                    }
                } catch (netEx: Exception) {
                    Log.w(TAG, "Network unavailable for module R2 upload: ${netEx.message}. Retaining local copy.")
                }

                uploadedUrls.add(finalUrl)
                onProgress?.invoke(
                    index + 1,
                    pageFiles.size,
                    ((index + 1).toFloat() / pageFiles.size.toFloat())
                )
            }

            // Sync with Firestore so this module's notes are saved online across devices
            if (currentUser != null) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val docData = mapOf(
                        "subjectCode" to subjectCode,
                        "cleanSubject" to cleanSubject,
                        "moduleIndex" to moduleIndex,
                        "pageUrls" to uploadedUrls,
                        "pageCount" to uploadedUrls.size,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("users")
                        .document(currentUser.uid)
                        .collection("module_notes")
                        .document("${cleanSubject}_m$moduleIndex")
                        .set(docData)
                } catch (fsEx: Exception) {
                    Log.w(TAG, "Failed to sync module notes manifest to Firestore: ${fsEx.message}")
                }
            }

            // Save in fast local cloud cache
            saveCachedCloudPages(context, cleanSubject, moduleIndex, uploadedUrls)

            Result.success(uploadedUrls)
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadModulePages: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes module notes from Firestore and cache.
     */
    suspend fun deleteModuleNotesFromCloud(
        context: Context,
        subjectCode: String,
        moduleIndex: Int
    ) = withContext(Dispatchers.IO) {
        val cleanSubject = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(currentUser.uid)
                    .collection("module_notes")
                    .document("${cleanSubject}_m$moduleIndex")
                    .delete()
            } catch (_: Exception) {}
        }
        clearCachedCloudPages(context, cleanSubject, moduleIndex)
    }

    fun saveCachedCloudPages(context: Context, cleanSubject: String, moduleIndex: Int, urls: List<String>) {
        val prefs = context.getSharedPreferences("kletaq_module_notes_cloud", Context.MODE_PRIVATE)
        prefs.edit().putString("${cleanSubject}_m$moduleIndex", urls.joinToString("|||")).apply()
    }

    fun getCachedCloudPages(context: Context, cleanSubject: String, moduleIndex: Int): List<String> {
        val prefs = context.getSharedPreferences("kletaq_module_notes_cloud", Context.MODE_PRIVATE)
        val raw = prefs.getString("${cleanSubject}_m$moduleIndex", null) ?: return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    fun clearCachedCloudPages(context: Context, cleanSubject: String, moduleIndex: Int) {
        val prefs = context.getSharedPreferences("kletaq_module_notes_cloud", Context.MODE_PRIVATE)
        prefs.edit().remove("${cleanSubject}_m$moduleIndex").apply()
    }

    /**
     * Hydrates and listens to user module notes from Firestore across all subjects/modules.
     */
    fun listenToCloudModuleNotes(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(currentUser.uid)
            .collection("module_notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                for (doc in snapshot.documents) {
                    val cleanSubject = doc.getString("cleanSubject") ?: continue
                    val moduleIndex = doc.getLong("moduleIndex")?.toInt() ?: continue
                    @Suppress("UNCHECKED_CAST")
                    val pageUrls = doc.get("pageUrls") as? List<String> ?: emptyList()
                    if (pageUrls.isNotEmpty()) {
                        saveCachedCloudPages(context, cleanSubject, moduleIndex, pageUrls)
                        ModuleNotesRepository.notifyNotesChanged(context, cleanSubject, moduleIndex)
                    }
                }
            }
    }

    /**
     * Purges staging temporary files from cache.
     */
    fun cleanStagingFiles(context: Context, noteId: String) {
        try {
            val stagingDir = File(context.cacheDir, "compressed_notes/$noteId")
            if (stagingDir.exists()) {
                stagingDir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }
}
