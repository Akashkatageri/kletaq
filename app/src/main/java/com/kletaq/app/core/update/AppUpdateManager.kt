package com.kletaq.app.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppUpdateInfo(
    val tag: String,
    val title: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val publishedAt: String,
    val sizeBytes: Long,
    val isUpdateAvailable: Boolean,
    val releaseDateFormatted: String,
    val installedDateFormatted: String
)

object AppUpdateManager {
    private const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/Akashkatageri/kletaq/releases/latest"
    private const val GITHUB_TAG_RELEASE_API =
        "https://api.github.com/repos/Akashkatageri/kletaq/releases/tags/Latest"

    private const val APK_FILE_NAME = "kletaq.apk"

    /**
     * Checks if a newer release exists on GitHub.
     * @param context Application or UI context
     * @param forceTreatAvailable If true, marks as available even if timestamps match (useful for manual reinstall/re-verification)
     */
    suspend fun checkForUpdate(
        context: Context,
        forceTreatAvailable: Boolean = false
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            var jsonString: String? = fetchJsonFromUrl(GITHUB_LATEST_RELEASE_API)
            if (jsonString == null) {
                jsonString = fetchJsonFromUrl(GITHUB_TAG_RELEASE_API)
            }

            if (jsonString == null) {
                return@withContext Result.failure(Exception("Failed to reach GitHub release server."))
            }

            val releaseObj = JSONObject(jsonString)
            val tag = releaseObj.optString("tag_name", "Latest")
            val title = releaseObj.optString("name", "Kletaq Latest Build")
            val body = releaseObj.optString("body", "Latest performance and feature updates.")
            val publishedAt = releaseObj.optString("published_at", "")

            val assetsArray = releaseObj.optJSONArray("assets")
            var downloadUrl: String? = null
            var sizeBytes = 0L
            var assetUpdatedAt = publishedAt

            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.equals(APK_FILE_NAME, ignoreCase = true) || name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url")
                        sizeBytes = asset.optLong("size", 0L)
                        val updated = asset.optString("updated_at")
                        if (updated.isNotBlank()) {
                            assetUpdatedAt = updated
                        }
                        break
                    }
                }
            }

            if (downloadUrl.isNullOrBlank()) {
                downloadUrl = "https://github.com/Akashkatageri/kletaq/releases/download/Latest/$APK_FILE_NAME"
            }

            // Current app install/update timestamp
            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (e: Exception) {
                null
            }

            val installedTimeMillis = packageInfo?.lastUpdateTime ?: 0L

            var releaseEpochMillis = 0L
            var releaseDateFormatted = "Latest Build"
            if (assetUpdatedAt.isNotBlank()) {
                try {
                    val instant = Instant.parse(assetUpdatedAt)
                    releaseEpochMillis = instant.toEpochMilli()
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
                        .withZone(ZoneId.systemDefault())
                    releaseDateFormatted = formatter.format(instant)
                } catch (e: Exception) {
                    releaseDateFormatted = assetUpdatedAt
                }
            }

            val installedDateFormatted = if (installedTimeMillis > 0L) {
                val instant = Instant.ofEpochMilli(installedTimeMillis)
                DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
            } else {
                "Installed"
            }

            // An update is available if the release on GitHub was updated AFTER the current app was installed/updated on device
            // (allowing a 60 second buffer for CI clock variance)
            val isNewer = forceTreatAvailable || (releaseEpochMillis > 0L && releaseEpochMillis > (installedTimeMillis + 60_000L))

            val updateInfo = AppUpdateInfo(
                tag = tag,
                title = title,
                releaseNotes = body,
                apkDownloadUrl = downloadUrl,
                publishedAt = assetUpdatedAt,
                sizeBytes = sizeBytes,
                isUpdateAvailable = isNewer,
                releaseDateFormatted = releaseDateFormatted,
                installedDateFormatted = installedDateFormatted
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchJsonFromUrl(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.setRequestProperty("User-Agent", "Kletaq-Android-App")
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Downloads the APK file to cacheDir/updates/kletaq.apk with progress callbacks.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }

            val destinationFile = File(updatesDir, APK_FILE_NAME)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = downloadUrl
            var conn: HttpURLConnection? = null
            var redirects = 0

            // Follow HTTP redirects (GitHub Releases redirect 302 to objects.githubusercontent.com)
            while (redirects < 6) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "Kletaq-Android-App")

                val status = conn.responseCode
                if (status in 300..399) {
                    val location = conn.getHeaderField("Location")
                        ?: return@withContext Result.failure(Exception("Redirect with missing Location header"))
                    currentUrl = location
                    conn.disconnect()
                    redirects++
                } else if (status in 200..299) {
                    break
                } else {
                    return@withContext Result.failure(Exception("Server returned HTTP $status"))
                }
            }

            val finalConn = conn ?: return@withContext Result.failure(Exception("Failed to open connection"))
            val totalBytes = finalConn.contentLength.toLong().let { if (it <= 0L) 24_000_000L else it }

            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            finalConn.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }

            finalConn.disconnect()
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if the app has permission to install unknown apps (Android 8.0+).
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens the Unknown Apps permission screen for Kletaq.
     */
    fun openUnknownAppSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Starts the native system installer for the downloaded APK.
     */
    fun launchPackageInstaller(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}
