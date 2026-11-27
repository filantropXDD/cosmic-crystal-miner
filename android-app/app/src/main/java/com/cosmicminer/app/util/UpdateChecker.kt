package com.cosmicminer.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionTag: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {
    const val CURRENT_VERSION = "1.2.1"
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/filantropXDD/cosmic-crystal-miner/releases/latest"
    private const val RELEASES_PAGE =
        "https://github.com/filantropXDD/cosmic-crystal-miner/releases/latest"

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(LATEST_RELEASE_API)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "CosmicCrystalMiner/$CURRENT_VERSION")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("Не удалось проверить обновления (${connection.responseCode})")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").trim()
            if (tagName.isEmpty()) {
                return@withContext UpdateCheckResult.UpToDate
            }

            if (!isNewerVersion(remoteTag = tagName, currentVersion = CURRENT_VERSION)) {
                return@withContext UpdateCheckResult.UpToDate
            }

            val htmlUrl = json.optString("html_url", RELEASES_PAGE)
            val notes = json.optString("body", "").trim()
            var apkUrl = htmlUrl

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (downloadUrl.isNotEmpty()) {
                            apkUrl = downloadUrl
                            break
                        }
                    }
                }
            }

            UpdateCheckResult.UpdateAvailable(
                AppUpdateInfo(
                    versionTag = tagName,
                    releaseNotes = notes,
                    apkDownloadUrl = apkUrl,
                    releasePageUrl = htmlUrl
                )
            )
        } catch (_: Throwable) {
            UpdateCheckResult.Error("Нет подключения к сети")
        } finally {
            connection?.disconnect()
        }
    }

    internal fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = parseVersionParts(remoteTag)
        val localParts = parseVersionParts(currentVersion)
        val count = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until count) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun parseVersionParts(raw: String): List<Int> {
        return raw.removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .split(".")
            .map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
    }
}
