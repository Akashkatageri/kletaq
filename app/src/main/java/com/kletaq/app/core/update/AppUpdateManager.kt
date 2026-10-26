package com.kletaq.app.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
    private const val CLOUDFLARE_VERSION_CDN =
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
            // 1. Try Cloudflare Pages edge CDN manifest first (zero rate limits, fast, campus Wi-Fi safe)
            var jsonString: String? = fetchJsonFromUrl(CLOUDFLARE_VERSION_CDN)
            var isFromCdn = false

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
                downloadUrl = releaseObj.optString("downloadUrl", "https://kletaq.5122006.xyz/kletaq.apk")
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
                downloadUrl = "https://kletaq.5122006.xyz/kletaq.apk"
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
        val targetUrl = if (!downloadUrl.isNullOrBlank()) downloadUrl else "https://kletaq.5122006.xyz"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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
