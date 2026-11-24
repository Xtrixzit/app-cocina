package com.example.data.sync

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.ProductionLog
import com.example.data.model.Recipe
import com.example.data.model.ShoppingItem
import com.example.settings.AppSettingsPreferences
import com.example.settings.DriveSyncInfo
import com.example.util.LocaleHelper
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed interface DriveAuthStep {
    data class TokenReady(val accessToken: String) : DriveAuthStep
    data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAuthStep
    data class Failed(val message: String) : DriveAuthStep
}

class DriveApiDisabledException(
    val activationUrl: String,
    message: String
) : IllegalStateException(message)

/**
 * Links to the user's Google Drive using Google Identity Services [AuthorizationClient]
 * with least-privilege `drive.file` scope, creating and updating AI-readable Google Docs,
 * Google Sheets, and Markdown files inside the user's "Recetario" Drive folder.
 */
object GoogleDriveSyncRepository {

    private const val TAG = "GoogleDriveSync"
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    private const val FOLDER_NAME = "Recetario"
    private const val MIME_FOLDER = "application/vnd.google-apps.folder"
    private const val MIME_GOOGLE_DOC = "application/vnd.google-apps.document"
    private const val MIME_GOOGLE_SHEET = "application/vnd.google-apps.spreadsheet"
    private const val MIME_MARKDOWN = "text/markdown"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    private fun buildAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .build()
    }

    suspend fun requestDriveAuthorization(activity: Activity): DriveAuthStep {
        return try {
            val result = Identity.getAuthorizationClient(activity)
                .authorize(buildAuthorizationRequest())
                .await()
            val resolution = result.pendingIntent
            val token = result.accessToken
            when {
                result.hasResolution() && resolution != null -> DriveAuthStep.NeedsConsent(resolution)
                !token.isNullOrBlank() -> DriveAuthStep.TokenReady(token)
                else -> DriveAuthStep.Failed(
                    LocaleHelper.localizedContext(activity).getString(R.string.drive_ai_sync_cancelled)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Drive authorization failed", e)
            DriveAuthStep.Failed(e.localizedMessage ?: "Authorization error")
        }
    }

    fun extractTokenFromConsentResult(context: Context, intentData: Intent?): Result<String> {
        return try {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(intentData)
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(
                    IllegalStateException(
                        LocaleHelper.localizedContext(context).getString(R.string.drive_ai_sync_cancelled)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trySilentAccessToken(context: Context): String? {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(buildAuthorizationRequest())
                .await()
            if (!result.hasResolution()) {
                result.accessToken?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun syncReadableFilesToDrive(
        context: Context,
        accessToken: String,
        recipes: List<Recipe>,
        pantry: List<PantryItem>,
        shopping: List<ShoppingItem>,
        productionLogs: List<ProductionLog>,
        currencyCode: String,
        marginPercent: Int,
        linkedEmail: String = ""
    ): Result<DriveSyncInfo> = withContext(Dispatchers.IO) {
        try {
            val loc = LocaleHelper.localizedContext(context)
            val existing = AppSettingsPreferences.getDriveSyncInfo(context)
            val now = System.currentTimeMillis()

            val folder = ensureRecetarioFolder(loc, accessToken, existing.folderId)

            val docTitle = loc.getString(R.string.drive_ai_doc_name)
            val docHtml = AiReadableExportFormatter.exportToGoogleDocHtml(
                context = context,
                recipes = recipes,
                pantry = pantry,
                shopping = shopping,
                productionLogs = productionLogs,
                currencyCode = currencyCode,
                marginPercent = marginPercent,
                timestampMillis = now
            )
            val docUploaded = upsertMultipartFile(
                loc = loc,
                accessToken = accessToken,
                existingFileId = existing.docFileId,
                folderId = folder.id,
                fileName = docTitle,
                targetDriveMimeType = MIME_GOOGLE_DOC,
                appKindTag = "ai_doc",
                content = docHtml,
                contentMimeType = "text/html; charset=UTF-8"
            )

            val sheetTitle = loc.getString(R.string.drive_ai_sheet_name)
            val csvText = AiReadableExportFormatter.exportToCsv(
                context = context,
                recipes = recipes,
                pantry = pantry,
                shopping = shopping
            )
            val sheetUploaded = upsertMultipartFile(
                loc = loc,
                accessToken = accessToken,
                existingFileId = existing.sheetFileId,
                folderId = folder.id,
                fileName = sheetTitle,
                targetDriveMimeType = MIME_GOOGLE_SHEET,
                appKindTag = "ai_sheet",
                content = csvText,
                contentMimeType = "text/csv; charset=UTF-8"
            )

            val mdFileName = loc.getString(R.string.drive_ai_md_filename)
            val mdText = AiReadableExportFormatter.exportToMarkdown(
                context = context,
                recipes = recipes,
                pantry = pantry,
                shopping = shopping,
                productionLogs = productionLogs,
                currencyCode = currencyCode,
                marginPercent = marginPercent,
                timestampMillis = now
            )
            val mdUploaded = upsertMultipartFile(
                loc = loc,
                accessToken = accessToken,
                existingFileId = existing.markdownFileId,
                folderId = folder.id,
                fileName = mdFileName,
                targetDriveMimeType = MIME_MARKDOWN,
                appKindTag = "ai_markdown",
                content = mdText,
                contentMimeType = "text/markdown; charset=UTF-8"
            )

            val updatedInfo = DriveSyncInfo(
                autoSyncEnabled = existing.autoSyncEnabled,
                lastSyncMillis = now,
                folderId = folder.id,
                docFileId = docUploaded.id,
                docWebLink = docUploaded.webViewLink,
                sheetFileId = sheetUploaded.id,
                sheetWebLink = sheetUploaded.webViewLink,
                markdownFileId = mdUploaded.id,
                markdownWebLink = mdUploaded.webViewLink,
                linkedEmail = linkedEmail.ifBlank { existing.linkedEmail }
            )
            AppSettingsPreferences.saveDriveSyncInfo(context, updatedInfo)
            Result.success(updatedInfo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing readable files to Google Drive", e)
            Result.failure(e)
        }
    }

    private data class DriveFileMeta(
        val id: String,
        val webViewLink: String
    )

    internal fun buildFriendlyDriveHttpError(
        loc: Context,
        httpCode: Int,
        bodyStr: String
    ): IllegalStateException {
        val lower = bodyStr.lowercase()
        val isServiceDisabled = lower.contains("service_disabled") ||
            lower.contains("accessnotconfigured") ||
            lower.contains("has not been used in project") ||
            lower.contains("drive.googleapis.com")
        if (isServiceDisabled) {
            val urlMatch = Regex("https://console\\.developers\\.google\\.com[^\\s\"']+").find(bodyStr)?.value
            val projectId = Regex("project[=\\s]+(\\d{6,20})").find(bodyStr)?.groupValues?.getOrNull(1)
                ?: "1034453362537"
            val activationUrl = urlMatch
                ?: "https://console.developers.google.com/apis/api/drive.googleapis.com/overview?project=$projectId"
            return DriveApiDisabledException(
                activationUrl = activationUrl,
                message = loc.getString(R.string.drive_api_disabled_friendly, projectId)
            )
        }
        if (httpCode == 401 || lower.contains("insufficientpermissions") || lower.contains("unauthenticated")) {
            return IllegalStateException(loc.getString(R.string.drive_perm_expired_friendly))
        }
        val shortJsonMsg = try {
            JSONObject(bodyStr).optJSONObject("error")?.optString("message").orEmpty()
        } catch (_: Exception) {
            ""
        }
        val cleanDetail = shortJsonMsg.ifBlank { "HTTP $httpCode" }
        return IllegalStateException(cleanDetail)
    }

    private fun ensureRecetarioFolder(loc: Context, accessToken: String, cachedFolderId: String): DriveFileMeta {
        if (cachedFolderId.isNotBlank()) {
            verifyExistingFile(accessToken, cachedFolderId)?.let { return it }
        }
        val query = "name='$FOLDER_NAME' and mimeType='$MIME_FOLDER' and trashed=false"
        findFirstFileByQuery(accessToken, query)?.let { return it }

        val metadata = JSONObject().apply {
            put("name", FOLDER_NAME)
            put("mimeType", MIME_FOLDER)
            put("appProperties", JSONObject().put("recetario_kind", "root_folder"))
        }
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id,webViewLink")
            .header("Authorization", "Bearer $accessToken")
            .post(metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw buildFriendlyDriveHttpError(loc, resp.code, bodyStr)
            }
            val json = JSONObject(bodyStr)
            return DriveFileMeta(
                id = json.getString("id"),
                webViewLink = json.optString("webViewLink", "")
            )
        }
    }

    private fun verifyExistingFile(accessToken: String, fileId: String): DriveFileMeta? {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?fields=id,webViewLink,trashed")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        return httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val json = JSONObject(resp.body?.string().orEmpty())
            if (json.optBoolean("trashed", false)) return@use null
            DriveFileMeta(
                id = json.getString("id"),
                webViewLink = json.optString("webViewLink", "")
            )
        }
    }

    private fun findFirstFileByQuery(accessToken: String, query: String): DriveFileMeta? {
        val encodedQ = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQ&spaces=drive&fields=files(id,webViewLink)&pageSize=1"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        return httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val files = JSONObject(resp.body?.string().orEmpty()).optJSONArray("files") ?: return@use null
            if (files.length() == 0) return@use null
            val first = files.getJSONObject(0)
            DriveFileMeta(
                id = first.getString("id"),
                webViewLink = first.optString("webViewLink", "")
            )
        }
    }

    private fun upsertMultipartFile(
        loc: Context,
        accessToken: String,
        existingFileId: String,
        folderId: String,
        fileName: String,
        targetDriveMimeType: String,
        appKindTag: String,
        content: String,
        contentMimeType: String
    ): DriveFileMeta {
        val resolvedExisting = when {
            existingFileId.isNotBlank() -> verifyExistingFile(accessToken, existingFileId)
            else -> null
        } ?: findFirstFileByQuery(
            accessToken,
            "'$folderId' in parents and appProperties has { key='recetario_kind' and value='$appKindTag' } and trashed=false"
        )

        val isUpdate = resolvedExisting != null
        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("appProperties", JSONObject().put("recetario_kind", appKindTag))
            if (!isUpdate) {
                put("mimeType", targetDriveMimeType)
                put("parents", JSONArray().put(folderId))
            }
        }

        val multipartBody = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(content.toRequestBody(contentMimeType.toMediaType()))
            .build()

        val url = if (isUpdate) {
            "https://www.googleapis.com/upload/drive/v3/files/${resolvedExisting!!.id}?uploadType=multipart&fields=id,webViewLink"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,webViewLink"
        }

        val builder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")

        val request = if (isUpdate) {
            builder.patch(multipartBody).build()
        } else {
            builder.post(multipartBody).build()
        }

        httpClient.newCall(request).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw buildFriendlyDriveHttpError(loc, resp.code, bodyStr)
            }
            val json = JSONObject(bodyStr)
            return DriveFileMeta(
                id = json.getString("id"),
                webViewLink = json.optString("webViewLink", "")
            )
        }
    }
}
