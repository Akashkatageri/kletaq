package com.kletaq.app.features.flashcards.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FlashcardMediaManager {
    private const val TAG = "FlashcardMediaManager"
    private const val MEDIA_DIR_NAME = "flashcard_media"

    private fun getMediaDirectory(context: Context): File {
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

            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image from uri: $sourceUri", e)
            null
        }
    }

    suspend fun deleteImageFile(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete flashcard image: $path", e)
        }
    }
}

