package com.kletaq.app.features.flashcards.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

object FlashcardMediaManager {
    private const val TAG = "FlashcardMediaManager"
    const val MEDIA_DIR_NAME = "flashcard_media"

    fun getMediaDirectory(context: Context): File {
        val dir = File(context.filesDir, MEDIA_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveImageFromUri(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val mediaDir = getMediaDirectory(context)
            val fileName = "card_img_${UUID.randomUUID()}.jpg"
            val destFile = File(mediaDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            fileName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image from uri: $sourceUri", e)
            null
        }
    }

    suspend fun saveMultipleImages(context: Context, uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        for (uri in uris) {
            val name = saveImageFromUri(context, uri)
            if (name != null) results.add(name)
        }
        results
    }

    suspend fun saveAudioFile(context: Context, sourceFile: File): String? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        try {
            val mediaDir = getMediaDirectory(context)
            val fileName = "card_audio_${UUID.randomUUID()}.m4a"
            val destFile = File(mediaDir, fileName)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save audio file", e)
            null
        }
    }

    fun resolveFile(context: Context, filenameOrPath: String?): File? {
        if (filenameOrPath.isNullOrBlank()) return null
        val directFile = File(filenameOrPath)
        if (directFile.isAbsolute && directFile.exists()) {
            return directFile
        }
        val cleanName = filenameOrPath.substringAfterLast("/")
        val sandboxFile = File(getMediaDirectory(context), cleanName)
        return if (sandboxFile.exists()) sandboxFile else directFile
    }

    fun resolveUri(context: Context, filenameOrPath: String?): Uri? {
        val f = resolveFile(context, filenameOrPath) ?: return null
        return if (f.exists()) Uri.fromFile(f) else null
    }

    fun normalizeFilename(pathOrName: String?): String? {
        if (pathOrName.isNullOrBlank()) return null
        return pathOrName.substringAfterLast("/").substringAfterLast("\\")
    }

    suspend fun deleteImageFile(context: Context, filenameOrPath: String?) = withContext(Dispatchers.IO) {
        if (filenameOrPath.isNullOrBlank()) return@withContext
        try {
            val f = resolveFile(context, filenameOrPath)
            if (f != null && f.exists()) {
                f.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete flashcard media: $filenameOrPath", e)
        }
    }
}

