package com.kletaq.app.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SystemDownloadProgress(
    val status: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progress: Float = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
) {
    val isRunning: Boolean = status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_PAUSED
    val isSuccessful: Boolean = status == DownloadManager.STATUS_SUCCESSFUL
    val isFailed: Boolean = status == DownloadManager.STATUS_FAILED
}

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
    private const val PRIMARY_VERSION_CDN =
        "https://trykletaq.cv/version.json"
    private const val BACKUP_VERSION_CDN =
        "https://kletaq.5122006.xyz/version.json"
    private const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/Akashkatageri/kletaq/releases/latest"
    private const val GITHUB_TAG_RELEASE_API =
        "https://api.github.com/repos/Akashkatageri/kletaq/releases/tags/Latest"

    private const val APK_FILE_NAME = "kletaq.apk"

    /**
     * Checks if a newer release exists on Cloudflare CDN or GitHub.
     * @param context Application or UI context
     * @param forceTreatAvailable If true, marks as available even if timestamps match (useful for manual reinstall/re-verification)
     */
    suspend fun checkForUpdate(
        context: Context,
        forceTreatAvailable: Boolean = false
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            // 1. Try Primary edge CDN (trykletaq.cv)
            var jsonString: String? = fetchJsonFromUrl(PRIMARY_VERSION_CDN)
            var isFromCdn = false

            // 1b. Fallback to Backup edge CDN (kletaq.5122006.xyz) if primary is unreachable or domain expired
            if (jsonString.isNullOrBlank()) {
                jsonString = fetchJsonFromUrl(BACKUP_VERSION_CDN)
            }

            if (!jsonString.isNullOrBlank()) {
                try {
                    val testObj = JSONObject(jsonString)
                    if (testObj.has("downloadUrl") || testObj.has("publishedAt")) {
                        isFromCdn = true
                    }
                } catch (_: Exception) {
                    jsonString = null
                }
            }

            // 2. Fall back to GitHub Releases API if CDN version is not yet deployed
            if (jsonString == null) {
                jsonString = fetchJsonFromUrl(GITHUB_LATEST_RELEASE_API)
                if (jsonString == null) {
                    jsonString = fetchJsonFromUrl(GITHUB_TAG_RELEASE_API)
                }
            }

            if (jsonString == null) {
                return@withContext Result.failure(Exception("Failed to reach update server."))
            }

            val releaseObj = JSONObject(jsonString)
            val tag = if (isFromCdn) releaseObj.optString("tag", "Latest") else releaseObj.optString("tag_name", "Latest")
            val title = if (isFromCdn) releaseObj.optString("title", "Kletaq Latest Benchmark Build") else releaseObj.optString("name", "Kletaq Latest Build")
            val body = if (isFromCdn) releaseObj.optString("releaseNotes", "Latest performance and feature updates.") else releaseObj.optString("body", "Latest performance and feature updates.")
            val publishedAt = if (isFromCdn) releaseObj.optString("publishedAt", "") else releaseObj.optString("published_at", "")

            var downloadUrl: String? = null
            var sizeBytes = 0L
            var assetUpdatedAt = publishedAt

            if (isFromCdn) {
                downloadUrl = releaseObj.optString("downloadUrl", "https://trykletaq.cv/kletaq.apk")
                sizeBytes = releaseObj.optLong("sizeBytes", 0L)
            } else {
                val assetsArray = releaseObj.optJSONArray("assets")
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
            }

            if (downloadUrl.isNullOrBlank()) {
                downloadUrl = "https://trykletaq.cv/kletaq.apk"
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
     * Opens the official website or direct download URL in the system web browser.
     * Delegates download to the browser to ensure full safety and avoid Play Protect dropper flags.
     */
    fun openUpdateInBrowser(context: Context, downloadUrl: String? = null) {
        val targetUrl = if (!downloadUrl.isNullOrBlank()) downloadUrl else "https://trykletaq.cv"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Enqueues an APK download using the Android OS System DownloadManager.
     * This avoids Play Protect dropper heuristics because the download and notification
     * are owned and vetted by the Android system provider (com.android.providers.downloads),
     * requiring ZERO dangerous permissions like REQUEST_INSTALL_PACKAGES.
     */
    fun startSystemDownload(
        context: Context,
        downloadUrl: String,
        versionTag: String
    ): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)
        val cleanTag = versionTag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "kletaq_${cleanTag}.apk"

        // Remove any old downloaded APK with the same name if exists in Downloads
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val existing = File(downloadDir, fileName)
            if (existing.exists()) existing.delete()
        } catch (_: Exception) {}

        val request = DownloadManager.Request(uri).apply {
            setTitle("Kletaq Update $versionTag")
            setDescription("Downloading latest Benchmark build...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        return downloadManager.enqueue(request)
    }

    /**
     * Queries the live progress of a running system download.
     */
    fun queryDownloadProgress(context: Context, downloadId: Long): SystemDownloadProgress {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        try {
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    val downloaded = if (bytesCol != -1) cursor.getLong(bytesCol) else 0L
                    val total = if (totalCol != -1) cursor.getLong(totalCol) else 0L
                    val status = if (statusCol != -1) cursor.getInt(statusCol) else DownloadManager.STATUS_FAILED

                    return SystemDownloadProgress(status, downloaded, total)
                }
            }
        } catch (_: Exception) {}
        return SystemDownloadProgress(DownloadManager.STATUS_FAILED, 0L, 0L)
    }

    /**
     * Opens the completed download file to initiate installation.
     * Uses DownloadManager's content URI or falls back to system Downloads view.
     */
    /**
     * Opens the completed download file to initiate installation.
     * Uses FileProvider with unknown sources permission check, or falls back to system DownloadManager URI.
     */
    fun openDownloadedApk(context: Context, downloadId: Long) {
        // 1. On Android 8.0+, verify unknown app installation permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return
                } catch (_: Exception) {}
            }
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var apkFile: File? = null

        // 2. Resolve actual file path on disk from DownloadManager query
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    if (uriIndex != -1) {
                        val localUriString = cursor.getString(uriIndex)
                        if (!localUriString.isNullOrBlank()) {
                            val localUri = Uri.parse(localUriString)
                            if (localUri.scheme == "file") {
                                val resolved = File(localUri.path ?: "")
                                if (resolved.exists()) apkFile = resolved
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Check standard Downloads folder directly for kletaq APK
        if (apkFile == null || !apkFile!!.exists()) {
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val candidate = downloadDir.listFiles()?.filter { it.name.startsWith("kletaq") && it.name.endsWith(".apk") }
                    ?.maxByOrNull { it.lastModified() }
                if (candidate != null && candidate.exists()) {
                    apkFile = candidate
                }
            } catch (_: Exception) {}
        }

        // 4. Preferred launch via FileProvider (Guarantees permission grant to PackageInstaller)
        if (apkFile != null && apkFile!!.exists()) {
            try {
                val authority = "${context.packageName}.fileprovider"
                val fileUri = FileProvider.getUriForFile(context, authority, apkFile!!)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                android.util.Log.w("AppUpdateManager", "FileProvider install failed, trying fallback: ${e.message}")
            }
        }

        // 5. Fallback: DownloadManager direct content URI
        try {
            val uri = downloadManager.getUriForDownloadedFile(downloadId)
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {}

        // 6. Secondary fallback: open system Downloads viewer or browser
        try {
            val fallbackIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            openUpdateInBrowser(context)
        }
    }

    /**
     * Cancels an in-progress download if the user dismisses.
     */
    fun cancelDownload(context: Context, downloadId: Long) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
        } catch (_: Exception) {}
    }

    /**
     * Purges temporary update APKs from the cache directory to prevent storage growth.
     */
    fun cleanUpdateCache(context: Context): Long {
        var freedBytes = 0L
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists()) {
                updatesDir.listFiles()?.forEach { file ->
                    freedBytes += file.length()
                    file.delete()
                }
            }
        } catch (_: Exception) {}
        return freedBytes
    }
}
