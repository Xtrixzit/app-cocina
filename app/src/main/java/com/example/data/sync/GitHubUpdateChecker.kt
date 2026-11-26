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

sealed class GitHubUpdateCheckResult {
    data class Success(
        val config: DistributionConfig?,
        val normalizedRepo: String
    ) : GitHubUpdateCheckResult()

    data class RepoNotFound(val normalizedRepo: String) : GitHubUpdateCheckResult()
    data object InvalidRepoFormat : GitHubUpdateCheckResult()
    data class Failure(val errorMessage: String) : GitHubUpdateCheckResult()
}

/**
 * Checks GitHub projects (`owner/repo` or full GitHub URL) via:
 * 1) `releases/latest` (official GitHub Release + `.apk` asset),
 * 2) `tags` fallback,
 * 3) `default_branch` `app/build.gradle.kts` + `Recetario.apk` check so pushing code to GitHub works too.
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
     * Accepts "owner/repo", "https://github.com/owner/repo", "https://github.com/owner/repo/releases", etc.
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
        val repo = parts[1].removeSuffix(".git")
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
        return Regex(
            "(?:versionCode|build|code|\\+)\\s*[:=#(]?\\s*(\\d{1,7})",
            RegexOption.IGNORE_CASE
        ).find(combined)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun parseGradleVersionInfo(gradleText: String): Pair<Int?, String?> {
        val versionCode = Regex(
            """\bversionCode\s*(?:=|\s)\s*(\d{1,7})"""
        ).find(gradleText)?.groupValues?.getOrNull(1)?.toIntOrNull()

        val versionName = Regex(
            """\bversionName\s*(?:=|\s)\s*["']([^"']+)["']"""
        ).find(gradleText)?.groupValues?.getOrNull(1)?.trim()

        return versionCode to versionName
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
        currentVersionName: String,
        githubRepo: String = ""
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

        val safeApkUrl = apkUrl.takeIf { com.example.util.SecurityUtils.isSafeHttpsUrl(it) }.orEmpty()
        val safeHtmlUrl = htmlUrl.takeIf { com.example.util.SecurityUtils.isSafeHttpsUrl(it) }.orEmpty()
        return DistributionConfig(
            latestVersionCode = effectiveVersionCode,
            latestVersionName = displayVersion,
            minRequiredVersionCode = 0,
            updateUrl = safeApkUrl.ifBlank { safeHtmlUrl },
            updateNotes = bodyNotes.ifBlank { releaseName },
            requireLicense = false,
            supportContact = safeHtmlUrl,
            githubRepo = githubRepo
        )
    }

    suspend fun fetchLatestRelease(
        repoInput: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): Result<DistributionConfig?> {
        return when (val outcome = checkRepoUpdates(repoInput, currentVersionCode, currentVersionName)) {
            is GitHubUpdateCheckResult.Success -> Result.success(outcome.config)
            GitHubUpdateCheckResult.InvalidRepoFormat -> Result.success(null)
            is GitHubUpdateCheckResult.RepoNotFound -> Result.failure(
                IllegalStateException("404: ${outcome.normalizedRepo}")
            )
            is GitHubUpdateCheckResult.Failure -> Result.failure(IllegalStateException(outcome.errorMessage))
        }
    }

    suspend fun checkRepoUpdates(
        repoInput: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): GitHubUpdateCheckResult = withContext(Dispatchers.IO) {
        val slug = normalizeRepoSlug(repoInput)
        if (slug.isBlank()) {
            return@withContext GitHubUpdateCheckResult.InvalidRepoFormat
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
                    return@withContext checkRepoMetadataAndFallbacks(
                        slug = slug,
                        currentVersionCode = currentVersionCode,
                        currentVersionName = currentVersionName
                    )
                }
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext GitHubUpdateCheckResult.Failure("HTTP ${resp.code}")
                }
                val parsed = parseReleaseJson(
                    jsonBody = bodyStr,
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName,
                    githubRepo = slug
                )
                if (parsed == null || parsed.latestVersionCode <= currentVersionCode) {
                    val branchOutcome = checkRepoMetadataAndFallbacks(
                        slug = slug,
                        currentVersionCode = currentVersionCode,
                        currentVersionName = currentVersionName
                    )
                    if (branchOutcome is GitHubUpdateCheckResult.Success &&
                        branchOutcome.config != null &&
                        branchOutcome.config.latestVersionCode > (parsed?.latestVersionCode ?: currentVersionCode)
                    ) {
                        return@withContext branchOutcome
                    }
                }
                GitHubUpdateCheckResult.Success(parsed, slug)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed checking GitHub releases for $slug", e)
            GitHubUpdateCheckResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    private fun checkRepoMetadataAndFallbacks(
        slug: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): GitHubUpdateCheckResult {
        val repoReq = Request.Builder()
            .url("https://api.github.com/repos/$slug")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "RecetarioAndroid")
            .get()
            .build()

        val defaultBranch: String = client.newCall(repoReq).execute().use { resp ->
            if (resp.code == 404) {
                return GitHubUpdateCheckResult.RepoNotFound(slug)
            }
            if (!resp.isSuccessful) {
                return GitHubUpdateCheckResult.Failure("HTTP ${resp.code}")
            }
            val repoJson = JSONObject(resp.body?.string().orEmpty())
            repoJson.optString("default_branch", "main").ifBlank { "main" }
        }

        val tagCfg = fetchLatestTagFallback(
            slug = slug,
            defaultBranch = defaultBranch,
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName
        )
        if (tagCfg != null && tagCfg.latestVersionCode > currentVersionCode) {
            return GitHubUpdateCheckResult.Success(tagCfg, slug)
        }

        val branchCfg = fetchBranchGradleFallback(
            slug = slug,
            defaultBranch = defaultBranch,
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName
        )
        if (branchCfg != null && branchCfg.latestVersionCode > currentVersionCode) {
            return GitHubUpdateCheckResult.Success(branchCfg, slug)
        }

        return GitHubUpdateCheckResult.Success(tagCfg ?: branchCfg, slug)
    }

    private fun findBranchApkUrl(slug: String, defaultBranch: String): String? {
        return try {
            val apkReq = Request.Builder()
                .url("https://api.github.com/repos/$slug/contents/Recetario.apk?ref=$defaultBranch")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "RecetarioAndroid")
                .get()
                .build()
            client.newCall(apkReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val obj = JSONObject(resp.body?.string().orEmpty())
                val rawDownloadUrl = obj.optString("download_url", "").trim()
                val candidate = if (com.example.util.SecurityUtils.isSafeHttpsUrl(rawDownloadUrl)) {
                    rawDownloadUrl
                } else {
                    "https://github.com/$slug/raw/$defaultBranch/Recetario.apk"
                }
                candidate.takeIf { com.example.util.SecurityUtils.isSafeHttpsUrl(it) }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchLatestTagFallback(
        slug: String,
        defaultBranch: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): DistributionConfig? {
        return try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$slug/tags?per_page=1")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "RecetarioAndroid")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = org.json.JSONArray(resp.body?.string().orEmpty())
                if (arr.length() == 0) return null
                val tagName = arr.getJSONObject(0).optString("name", "").trim()
                if (tagName.isBlank()) return null
                val newer = isRemoteVersionNewer(
                    remoteTagOrName = tagName,
                    remoteTitle = tagName,
                    remoteNotes = "",
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                )
                val apkUrl = if (newer) findBranchApkUrl(slug, defaultBranch) else null
                DistributionConfig(
                    latestVersionCode = if (newer) currentVersionCode + 1 else currentVersionCode,
                    latestVersionName = tagName,
                    minRequiredVersionCode = 0,
                    updateUrl = apkUrl ?: "https://github.com/$slug/releases/tag/$tagName",
                    updateNotes = "GitHub $tagName",
                    requireLicense = false,
                    supportContact = "https://github.com/$slug/releases",
                    githubRepo = slug
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchBranchGradleFallback(
        slug: String,
        defaultBranch: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): DistributionConfig? {
        return try {
            val gradleReq = Request.Builder()
                .url("https://raw.githubusercontent.com/$slug/$defaultBranch/app/build.gradle.kts")
                .header("User-Agent", "RecetarioAndroid")
                .get()
                .build()
            val gradleText = client.newCall(gradleReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string().orEmpty()
            }
            val (remoteCode, remoteName) = parseGradleVersionInfo(gradleText)
            val codeIsNewer = remoteCode != null && remoteCode > currentVersionCode
            val nameIsNewer = !remoteName.isNullOrBlank() && isRemoteVersionNewer(
                remoteTagOrName = remoteName,
                remoteTitle = "",
                remoteNotes = "",
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )
            if (!codeIsNewer && !nameIsNewer) return null

            val effectiveCode = maxOf(remoteCode ?: 0, currentVersionCode + 1)
            val displayName = remoteName ?: "build $effectiveCode"
            val apkUrl = findBranchApkUrl(slug, defaultBranch)
            DistributionConfig(
                latestVersionCode = effectiveCode,
                latestVersionName = displayName,
                minRequiredVersionCode = 0,
                updateUrl = apkUrl ?: "https://github.com/$slug/releases",
                updateNotes = "GitHub ($defaultBranch): $displayName",
                requireLicense = false,
                supportContact = "https://github.com/$slug",
                githubRepo = slug
            )
        } catch (_: Exception) {
            null
        }
    }
}
