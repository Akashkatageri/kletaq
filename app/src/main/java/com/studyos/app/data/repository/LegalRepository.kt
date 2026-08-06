package com.studyos.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface LegalRepository {
    fun getTermsDocument(): String
    fun getPrivacyDocument(): String
}

@Singleton
class LegalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LegalRepository {

    override fun getTermsDocument(): String {
        return readAssetFile("terms.md")
    }

    override fun getPrivacyDocument(): String {
        return readAssetFile("privacy.md")
    }

    private fun readAssetFile(fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "# Document Error\nCould not load $fileName: ${e.localizedMessage}"
        }
    }
}
