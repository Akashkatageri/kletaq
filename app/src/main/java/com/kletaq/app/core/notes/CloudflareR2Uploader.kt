package com.kletaq.app.core.notes

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

        val idToken = try {
            currentUser?.getIdToken(false)?.await()?.token
        } catch (_: Exception) {
            null
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
                    val requestBuilder = Request.Builder()
                        .url(uploadUrl)
                        .put(requestBody)
                        .addHeader("X-Kletaq-UID", uid)
                        .addHeader("X-Kletaq-Note-ID", noteId)

                    if (!idToken.isNullOrBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer $idToken")
                    }
                    val request = requestBuilder.build()

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

        val idToken = try {
            currentUser?.getIdToken(false)?.await()?.token
        } catch (_: Exception) {
            null
        }

        try {
            pageFiles.forEachIndexed { index, file ->
                val fileName = file.name
                // Format matches Cloudflare Worker anti-abuse regex: users/{uid}/notes/{noteId}/page_XXX.jpg
                val remoteKey = "users/$uid/notes/${cleanSubject}_m$moduleIndex/$fileName"
                var finalUrl = file.absolutePath

                try {
                    val uploadUrl = "$workerApiUrl/upload/$remoteKey"
                    val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                    val requestBuilder = Request.Builder()
                        .url(uploadUrl)
                        .put(requestBody)
                        .addHeader("X-Kletaq-UID", uid)
                        .addHeader("X-Kletaq-Subject", cleanSubject)
                        .addHeader("X-Kletaq-Module", moduleIndex.toString())

                    if (!idToken.isNullOrBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer $idToken")
                    }
                    val request = requestBuilder.build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        finalUrl = "$R2_PUBLIC_BASE_URL/$remoteKey"
                        Log.i(TAG, "Uploaded module page to R2: $finalUrl")
                    } else {
                        Log.w(TAG, "R2 Worker upload returned ${response.code}. Retaining local copy.")
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
            // ONLY sync remote URLs (starting with http/https) so other devices never receive local sandbox paths!
            val remoteUrlsOnly = uploadedUrls.filter { it.startsWith("http://") || it.startsWith("https://") }
            if (currentUser != null && remoteUrlsOnly.isNotEmpty()) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val docData = mapOf(
                        "subjectCode" to subjectCode,
                        "cleanSubject" to cleanSubject,
                        "moduleIndex" to moduleIndex,
                        "pageUrls" to remoteUrlsOnly,
                        "pageCount" to remoteUrlsOnly.size,
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
     * Deletes module notes from Cloudflare R2 bucket, Firestore, and local cache.
     */
    suspend fun deleteModuleNotesFromCloud(
        context: Context,
        subjectCode: String,
        moduleIndex: Int
    ) = withContext(Dispatchers.IO) {
        val cleanSubject = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        // 1. Retrieve page URLs before clearing
        val cachedUrls = getCachedCloudPages(context, cleanSubject, moduleIndex)

        // 2. Delete physical objects from Cloudflare R2
        if (currentUser != null && cachedUrls.isNotEmpty()) {
            val idToken = try {
                currentUser.getIdToken(false).await()?.token
            } catch (_: Exception) {
                null
            }

            for (url in cachedUrls) {
                try {
                    val key = when {
                        url.startsWith(R2_PUBLIC_BASE_URL) -> url.removePrefix("$R2_PUBLIC_BASE_URL/").trimStart('/')
                        url.contains("/users/") -> "users/" + url.substringAfter("/users/")
                        else -> null
                    }

                    if (key != null) {
                        val deleteUrl = "$workerApiUrl/upload/$key"
                        val reqBuilder = Request.Builder()
                            .url(deleteUrl)
                            .delete()
                            .addHeader("X-Kletaq-UID", uid ?: "")

                        if (!idToken.isNullOrBlank()) {
                            reqBuilder.addHeader("Authorization", "Bearer $idToken")
                        }

                        val response = httpClient.newCall(reqBuilder.build()).execute()
                        response.close()
                        Log.i(TAG, "Deleted R2 object: $key (status ${response.code})")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed deleting R2 object for $url: ${e.message}")
                }
            }
        }

        // 3. Delete from Firestore
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

        // 4. Clear local cache
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

                // Handle deletions across devices
                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        val doc = change.document
                        val cleanSubject = doc.getString("cleanSubject") ?: continue
                        val moduleIndex = doc.getLong("moduleIndex")?.toInt() ?: continue
                        val dir = CustomNotesManager.getModuleDir(context, cleanSubject, moduleIndex)
                        dir.listFiles()?.forEach { it.delete() }
                        clearCachedCloudPages(context, cleanSubject, moduleIndex)
                        ModuleNotesRepository.notifyNotesChanged(context, cleanSubject, moduleIndex)
                    }
                }

                // Handle additions and updates
                for (doc in snapshot.documents) {
                    val cleanSubject = doc.getString("cleanSubject") ?: continue
                    val moduleIndex = doc.getLong("moduleIndex")?.toInt() ?: continue
                    val updatedAt = doc.getLong("updatedAt") ?: 0L
                    @Suppress("UNCHECKED_CAST")
                    val rawUrls = doc.get("pageUrls") as? List<String> ?: emptyList()
                    val validUrls = rawUrls.filter { url ->
                        url.startsWith("http://") || url.startsWith("https://")
                    }
                    if (validUrls.isNotEmpty()) {
                        saveCachedCloudPages(context, cleanSubject, moduleIndex, validUrls)
                        downloadCloudPagesLocally(context, cleanSubject, moduleIndex, validUrls, updatedAt)
                        ModuleNotesRepository.notifyNotesChanged(context, cleanSubject, moduleIndex)
                    }
                }
            }
    }

    /**
     * Background downloads remote Cloudflare R2 note pages to the local module sandbox
     * so that on-device MLKit OCR, search, and image display work instantly offline.
     * Automatically invalidates stale local files when an update from another device arrives.
     */
    fun downloadCloudPagesLocally(
        context: Context,
        cleanSubject: String,
        moduleIndex: Int,
        urls: List<String>,
        updatedAt: Long = 0L
    ) {
        val remoteUrls = urls.filter { it.startsWith("http://") || it.startsWith("https://") }
        if (remoteUrls.isEmpty()) return

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val dir = CustomNotesManager.getModuleDir(context, cleanSubject, moduleIndex)
            val prefs = context.getSharedPreferences("kletaq_module_notes_cloud", Context.MODE_PRIVATE)
            val lastSyncedTime = prefs.getLong("${cleanSubject}_m${moduleIndex}_synced_time", 0L)

            val existingFiles = dir.listFiles { f ->
                f.isFile && (f.name.endsWith(".jpg") || f.name.endsWith(".png"))
            } ?: emptyArray()

            val isNewerVersion = (updatedAt > 0L && updatedAt > lastSyncedTime)
            val countMismatch = existingFiles.size != remoteUrls.size
            val needsFullRefresh = isNewerVersion || countMismatch

            if (needsFullRefresh && existingFiles.isNotEmpty()) {
                dir.listFiles()?.forEach { it.delete() }
            }

            var hasNewDownloads = false
            remoteUrls.forEachIndexed { index, url ->
                val fileName = "page_${String.format("%03d", index + 1)}.jpg"
                val targetFile = File(dir, fileName)
                if (!targetFile.exists() || targetFile.length() == 0L || needsFullRefresh) {
                    try {
                        val req = Request.Builder().url(url).build()
                        val res = httpClient.newCall(req).execute()
                        if (res.isSuccessful) {
                            res.body?.byteStream()?.use { input ->
                                java.io.FileOutputStream(targetFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            hasNewDownloads = true
                        }
                        res.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed downloading cloud page $url: ${e.message}")
                    }
                }
            }

            if (updatedAt > 0L) {
                prefs.edit().putLong("${cleanSubject}_m${moduleIndex}_synced_time", updatedAt).apply()
            }

            if (hasNewDownloads || needsFullRefresh) {
                ModuleNotesRepository.notifyNotesChanged(context, cleanSubject, moduleIndex)
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
