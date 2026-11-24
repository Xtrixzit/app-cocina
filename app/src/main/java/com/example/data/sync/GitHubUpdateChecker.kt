package com.example.data.sync

import android.util.Log
import com.example.data.model.DistributionConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Checks public GitHub Releases (`GET /repos/{owner}/{repo}/releases/latest`) so publishing a new
 * GitHub release/APK automatically notifies users inside the app to update cleanly via browser.
 */
object GitHubUpdateChecker {

    private const val TAG = "GitHubUpdateChecker"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Accepts "owner/repo", "https://github.com/owner/repo", or "github.com/owner/repo".
     */
    fun normalizeRepoSlug(rawInput: String): String {
        var cleaned = rawInput.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
        if (cleaned.startsWith("github.com/", ignoreCase = true)) {
            cleaned = cleaned.substringAfter("github.com/", "")
        }
        cleaned = cleaned.removeSuffix(".git").trim('/')
        val parts = cleaned.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return ""
        val owner = parts[0]
        val repo = parts[1]
        val validSegment = "^[A-Za-z0-9_.-]{1,100}$".toRegex()
        return if (validSegment.matches(owner) && validSegment.matches(repo)) "$owner/$repo" else ""
    }

    fun parseVersionSegments(rawVersion: String): List<Int> {
        val core = rawVersion.trim()
            .removePrefix("refs/tags/")
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("+")
            .substringBefore("-")
        return Regex("\\d+")
            .findAll(core)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()
            .ifEmpty { listOf(0) }
    }

    fun extractBuildCodeHint(vararg texts: String): Int? {
        val combined = texts.joinToString(" ")
        val explicit = Regex(
            "(?:versionCode|build|code|\\+)\\s*[:=#(]?\\s*(\\d{1,7})",
            RegexOption.IGNORE_CASE
        ).find(combined)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return explicit
    }

    fun isRemoteVersionNewer(
        remoteTagOrName: String,
        remoteTitle: String,
        remoteNotes: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): Boolean {
        val codeHint = extractBuildCodeHint(remoteTagOrName, remoteTitle, remoteNotes)
        if (codeHint != null && codeHint > currentVersionCode) {
            return true
        }
        val remoteParts = parseVersionSegments(remoteTagOrName.ifBlank { remoteTitle })
        val localParts = parseVersionSegments(currentVersionName)
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun parseReleaseJson(
        jsonBody: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): DistributionConfig? {
        val json = JSONObject(jsonBody)
        if (json.optBoolean("draft", false)) return null

        val tagName = json.optString("tag_name", "").trim()
        val releaseName = json.optString("name", "").trim()
        val bodyNotes = json.optString("body", "").trim()
        val htmlUrl = json.optString("html_url", "").trim()

        var apkUrl = ""
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "").trim()
                if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    apkUrl = downloadUrl
                    break
                }
            }
        }

        val displayVersion = tagName.ifBlank { releaseName }.ifBlank { currentVersionName }
        val isNewer = isRemoteVersionNewer(
            remoteTagOrName = tagName,
            remoteTitle = releaseName,
            remoteNotes = bodyNotes,
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName
        )
        val codeHint = extractBuildCodeHint(tagName, releaseName, bodyNotes)
        val effectiveVersionCode = when {
            codeHint != null -> codeHint
            isNewer -> currentVersionCode + 1
            else -> currentVersionCode
        }

        return DistributionConfig(
            latestVersionCode = effectiveVersionCode,
            latestVersionName = displayVersion,
            minRequiredVersionCode = 0,
            updateUrl = apkUrl.ifBlank { htmlUrl },
            updateNotes = bodyNotes.ifBlank { releaseName },
            requireLicense = false,
            supportContact = htmlUrl
        )
    }

    suspend fun fetchLatestRelease(
        repoInput: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): Result<DistributionConfig?> = withContext(Dispatchers.IO) {
        val slug = normalizeRepoSlug(repoInput)
        if (slug.isBlank()) {
            return@withContext Result.success(null)
        }
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$slug/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "RecetarioAndroid")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) {
                    return@withContext fetchLatestTagFallback(
                        slug = slug,
                        currentVersionCode = currentVersionCode,
                        currentVersionName = currentVersionName
                    )
                }
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("GitHub API (${resp.code})")
                    )
                }
                Result.success(
                    parseReleaseJson(
                        jsonBody = bodyStr,
                        currentVersionCode = currentVersionCode,
                        currentVersionName = currentVersionName
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed checking GitHub releases for $slug", e)
            Result.failure(e)
        }
    }

    private fun fetchLatestTagFallback(
        slug: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): Result<DistributionConfig?> {
        return try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$slug/tags?per_page=1")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "RecetarioAndroid")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Result.success(null)
                val arr = org.json.JSONArray(resp.body?.string().orEmpty())
                if (arr.length() == 0) return Result.success(null)
                val tagName = arr.getJSONObject(0).optString("name", "").trim()
                if (tagName.isBlank()) return Result.success(null)
                val newer = isRemoteVersionNewer(
                    remoteTagOrName = tagName,
                    remoteTitle = tagName,
                    remoteNotes = "",
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                )
                Result.success(
                    DistributionConfig(
                        latestVersionCode = if (newer) currentVersionCode + 1 else currentVersionCode,
                        latestVersionName = tagName,
                        minRequiredVersionCode = 0,
                        updateUrl = "https://github.com/$slug/releases",
                        updateNotes = "GitHub tag $tagName",
                        requireLicense = false,
                        supportContact = "https://github.com/$slug",
                        githubRepo = slug
                    )
                )
            }
        } catch (_: Exception) {
            Result.success(null)
        }
    }
}
