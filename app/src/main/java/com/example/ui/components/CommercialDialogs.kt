package com.example.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.model.DistributionConfig
import com.example.data.sync.AiReadableExportFormatter
import com.example.data.sync.DriveAuthStep
import com.example.data.sync.GoogleDriveSyncRepository
import com.example.settings.AppLanguage
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.settings.DriveSyncInfo
import com.example.ui.theme.BlueprintNavy
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.NotebookPaperBackground
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen
import com.example.util.LocaleHelper
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommercialSettingsDialog(
    isGuestMode: Boolean,
    localGuestCountForMigration: Int,
    currentUserName: String = "",
    currentUserEmail: String = "",
    driveSyncInfo: DriveSyncInfo = DriveSyncInfo(),
    driveSyncing: Boolean = false,
    driveSyncError: String? = null,
    driveActivationUrl: String? = null,
    driveNeedsPermissionPrompt: Boolean = false,
    distributionConfig: DistributionConfig? = null,
    onDismiss: () -> Unit,
    onMigrateGuestToCloud: () -> Unit,
    onSyncCloudNow: () -> Unit = {},
    onSwitchOrSignOutAccount: () -> Unit = {},
    onOpenChefChat: () -> Unit = {},
    onExportFullBackupJson: (onResult: (String) -> Unit) -> Unit,
    onImportFullBackupJson: (String, Boolean) -> Unit,
    onExportAiMarkdown: (onResult: (String) -> Unit) -> Unit = {},
    onExportAiCsv: (onResult: (String) -> Unit) -> Unit = {},
    onExportAiHtmlDoc: (onResult: (String) -> Unit) -> Unit = {},
    onSyncDriveWithToken: (String) -> Unit = {},
    onDriveAutoSyncChanged: (Boolean) -> Unit = {},
    onDriveSyncMessage: (String, Boolean) -> Unit = { _, _ -> },
    onActivateLicense: (String) -> Unit,
    onReplayTutorial: () -> Unit = {},
    onCheckUpdates: () -> Unit,
    onOpenAvailableUpdate: (DistributionConfig) -> Unit = {},
    onShowPrivacyPolicy: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val currentLanguage by AppSettingsPreferences.observeLanguage(context).collectAsStateWithLifecycle()
    val currentCurrency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val currentMargin by AppSettingsPreferences.observeMarginPercent(context).collectAsStateWithLifecycle()
    val savedLicense by AppSettingsPreferences.observeLicenseKey(context).collectAsStateWithLifecycle()
    val savedGithubRepo by AppSettingsPreferences.observeGithubRepo(context).collectAsStateWithLifecycle()

    var languageExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var marginText by remember(currentMargin) { mutableStateOf(currentMargin.toString()) }
    var licenseInput by remember(savedLicense) { mutableStateOf(savedLicense) }
    var githubRepoInput by remember(savedGithubRepo, distributionConfig?.githubRepo) {
        mutableStateOf(savedGithubRepo.ifBlank { distributionConfig?.githubRepo.orEmpty() })
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingExportText by remember { mutableStateOf<String?>(null) }

    val hasPendingUpdate = distributionConfig != null &&
        distributionConfig.latestVersionCode > BuildConfig.VERSION_CODE

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri != null && !json.isNullOrBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
    }

    val exportMdLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        val txt = pendingExportText
        pendingExportText = null
        if (uri != null && !txt.isNullOrBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(txt.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val txt = pendingExportText
        pendingExportText = null
        if (uri != null && !txt.isNullOrBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(txt.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
    }

    val exportHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val txt = pendingExportText
        pendingExportText = null
        if (uri != null && !txt.isNullOrBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(txt.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
    }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            GoogleDriveSyncRepository.extractTokenFromConsentResult(context, res.data)
                .onSuccess { token -> onSyncDriveWithToken(token) }
                .onFailure { err ->
                    onDriveSyncMessage(
                        context.getString(R.string.drive_ai_sync_error, err.localizedMessage ?: ""),
                        true
                    )
                }
        } else {
            onDriveSyncMessage(context.getString(R.string.drive_ai_sync_cancelled), false)
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onImportFullBackupJson(text, false)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun startDriveSyncFlow() {
        if (driveSyncing) return
        val host = activity ?: return
        coroutineScope.launch {
            when (val step = GoogleDriveSyncRepository.requestDriveAuthorization(host)) {
                is DriveAuthStep.TokenReady -> onSyncDriveWithToken(step.accessToken)
                is DriveAuthStep.NeedsConsent -> {
                    try {
                        driveConsentLauncher.launch(
                            IntentSenderRequest.Builder(step.pendingIntent.intentSender).build()
                        )
                    } catch (e: Exception) {
                        onDriveSyncMessage(
                            context.getString(R.string.drive_ai_sync_error, e.localizedMessage ?: ""),
                            true
                        )
                    }
                }
                is DriveAuthStep.Failed -> {
                    onDriveSyncMessage(
                        context.getString(R.string.drive_ai_sync_error, step.message),
                        true
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NotebookPaperBackground,
            tonalElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(14.dp))
                .sketchedBorder(color = CharcoalInk, strokeWidthDp = 2.2f, cornerRadiusDp = 14f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .notebookPaperBackground()
            ) {
                // Craft Notebook Header Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NotebookBackStamp(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_close_stamp")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontFamily = CaveatFontFamily,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueprintNavy
                        )
                    }
                    Surface(
                        color = PostItYellow,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.sketchedBorder(color = CharcoalInk.copy(alpha = 0.7f), strokeWidthDp = 1.2f)
                    ) {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = CharcoalInk.copy(alpha = 0.16f))

                // Notebook Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Profile & Account Status Post-It Card
                    PostItCard(
                        backgroundColor = PostItSky.copy(alpha = 0.80f),
                        rotationDegrees = -0.35f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_profile_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.settings_profile_section),
                                fontFamily = CaveatFontFamily,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueprintNavy,
                                modifier = Modifier.testTag("settings_profile_section")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGuestMode) Icons.Default.PersonOutline else Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = BlueprintNavy,
                                    modifier = Modifier.size(34.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentUserName.ifBlank {
                                            stringResource(
                                                if (isGuestMode) R.string.guest_display_name else R.string.account_cloud_active
                                            )
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalInk
                                    )
                                    if (!isGuestMode && currentUserEmail.isNotBlank()) {
                                        Text(
                                            text = currentUserEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk.copy(alpha = 0.78f)
                                        )
                                    }
                                    Text(
                                        text = stringResource(
                                            if (isGuestMode) R.string.settings_profile_guest_sub else R.string.settings_profile_cloud_sub
                                        ),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 14.sp,
                                        color = if (isGuestMode) CharcoalInk.copy(alpha = 0.8f) else StockGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!isGuestMode) {
                                val formattedDriveStamp = remember(driveSyncInfo.lastSyncMillis) {
                                    if (driveSyncInfo.lastSyncMillis > 0L) {
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                            .format(Date(driveSyncInfo.lastSyncMillis))
                                    } else {
                                        ""
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when {
                                            driveSyncInfo.lastSyncMillis > 0L -> stringResource(
                                                R.string.settings_profile_drive_linked,
                                                formattedDriveStamp
                                            )
                                            driveNeedsPermissionPrompt -> stringResource(
                                                R.string.settings_profile_drive_needs_perm
                                            )
                                            else -> stringResource(R.string.settings_profile_drive_pending)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            driveSyncInfo.lastSyncMillis > 0L -> StockGreen
                                            driveNeedsPermissionPrompt -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.settings_profile_drive_guest_note),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 14.sp,
                                    color = CharcoalInk.copy(alpha = 0.82f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isGuestMode) {
                                    OutlinedButton(
                                        onClick = onSyncCloudNow,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_sync_cloud_button")
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.account_sync_now), maxLines = 1)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onSwitchOrSignOutAccount()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_switch_account_button")
                                ) {
                                    Icon(
                                        imageVector = if (isGuestMode) Icons.Default.CloudUpload else Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(
                                            if (isGuestMode) R.string.account_connect_google else R.string.account_sign_out
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            if (!isGuestMode && localGuestCountForMigration > 0) {
                                Button(
                                    onClick = onMigrateGuestToCloud,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("migrate_guest_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.settings_migrate_guest, localGuestCountForMigration))
                                }
                            }
                        }
                    }

                    // 2. Google Drive AI-Readable Sync Post-It Card (moved from Kitchen Voice!)
                    PostItCard(
                        backgroundColor = PostItMint.copy(alpha = 0.82f),
                        rotationDegrees = 0.35f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drive_ai_sync_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (driveSyncInfo.lastSyncMillis > 0L) {
                                        Icons.Default.CloudDone
                                    } else {
                                        Icons.Default.CloudSync
                                    },
                                    contentDescription = stringResource(R.string.drive_ai_card_title),
                                    tint = StockGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.drive_ai_card_title),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueprintNavy
                                )
                            }

                            Text(
                                text = stringResource(R.string.drive_ai_card_desc),
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 15.sp,
                                color = CharcoalInk
                            )

                            val driveStatusText = if (driveSyncInfo.lastSyncMillis > 0L) {
                                val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    .format(Date(driveSyncInfo.lastSyncMillis))
                                stringResource(R.string.drive_ai_status_synced, stamp)
                            } else {
                                stringResource(R.string.drive_ai_status_never)
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sketchedBorder(color = CharcoalInk.copy(alpha = 0.3f), strokeWidthDp = 1f)
                            ) {
                                Text(
                                    text = driveStatusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (driveSyncInfo.lastSyncMillis > 0L) StockGreen else CharcoalInk.copy(alpha = 0.78f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            if (!driveSyncError.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = driveSyncError,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        if (!driveActivationUrl.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, driveActivationUrl.toUri()))
                                                    } catch (_: Exception) {
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("drive_enable_api_button")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(stringResource(R.string.drive_api_enable_console_btn), style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { startDriveSyncFlow() },
                                enabled = !driveSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StockGreen,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("drive_sync_now_button")
                            ) {
                                if (driveSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.drive_ai_syncing), fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.drive_ai_sync_now), fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.drive_ai_auto_sync),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalInk
                                    )
                                    Text(
                                        text = stringResource(R.string.drive_ai_auto_sync_sub),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 14.sp,
                                        color = CharcoalInk.copy(alpha = 0.8f)
                                    )
                                }
                                Switch(
                                    checked = driveSyncInfo.autoSyncEnabled,
                                    onCheckedChange = onDriveAutoSyncChanged,
                                    modifier = Modifier.testTag("drive_auto_sync_switch")
                                )
                            }

                            if (driveSyncInfo.docWebLink.isNotBlank() || driveSyncInfo.sheetWebLink.isNotBlank()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (driveSyncInfo.docWebLink.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, driveSyncInfo.docWebLink.toUri()))
                                                } catch (_: Exception) {
                                                }
                                            },
                                            modifier = Modifier.testTag("drive_open_doc_button")
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.drive_ai_open_doc), style = MaterialTheme.typography.labelMedium)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(driveSyncInfo.docWebLink))
                                                onDriveSyncMessage(context.getString(R.string.drive_ai_link_copied), false)
                                            },
                                            modifier = Modifier.testTag("drive_copy_link_button")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.drive_ai_copy_link), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    if (driveSyncInfo.sheetWebLink.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, driveSyncInfo.sheetWebLink.toUri()))
                                                } catch (_: Exception) {
                                                }
                                            },
                                            modifier = Modifier.testTag("drive_open_sheet_button")
                                        ) {
                                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.drive_ai_open_sheet), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sketchedBorder(color = StockGreen.copy(alpha = 0.65f), strokeWidthDp = 1.4f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.drive_free_section_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StockGreen
                                    )
                                    Text(
                                        text = stringResource(R.string.drive_free_section_desc),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 14.sp,
                                        color = CharcoalInk
                                    )

                                    Button(
                                        onClick = {
                                            onExportAiMarkdown { md ->
                                                pendingExportText = md
                                                exportMdLauncher.launch("recetario_inventario_ia.md")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BlueprintNavy,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("drive_free_save_button")
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.drive_free_save_btn), fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onExportAiHtmlDoc { html ->
                                                pendingExportText = html
                                                exportHtmlLauncher.launch("recetario_ia_google_docs.html")
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("drive_free_save_doc_button")
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.drive_free_save_doc_btn))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onExportAiMarkdown { md ->
                                                try {
                                                    val uri = AiReadableExportFormatter.writeShareableExportFile(
                                                        context = context,
                                                        fileName = "recetario_inventario_ia.md",
                                                        content = md
                                                    )
                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/markdown"
                                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.drive_ai_doc_name))
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                                } catch (_: Exception) {
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("drive_free_share_file_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.drive_free_share_file_btn))
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    onExportAiMarkdown { md ->
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.drive_ai_doc_name))
                                            putExtra(Intent.EXTRA_TEXT, md)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("drive_share_ai_text_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.drive_ai_share_text))
                            }
                        }
                    }

                    // 3. General Preferences & Pricing (Sketched Notebook Card)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sketchedBorder(color = CharcoalInk.copy(alpha = 0.72f), strokeWidthDp = 1.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = BlueprintNavy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_general_section),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueprintNavy
                                )
                            }

                            // Language Selector
                            ExposedDropdownMenuBox(
                                expanded = languageExpanded,
                                onExpandedChange = { languageExpanded = !languageExpanded }
                            ) {
                                OutlinedTextField(
                                    value = LocaleHelper.languageDisplayName(context, currentLanguage),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.settings_language)) },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .testTag("settings_language_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = languageExpanded,
                                    onDismissRequest = { languageExpanded = false }
                                ) {
                                    AppLanguage.entries.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(LocaleHelper.languageDisplayName(context, lang)) },
                                            onClick = {
                                                languageExpanded = false
                                                LocaleHelper.applyLanguage(activity, lang.code)
                                            }
                                        )
                                    }
                                }
                            }

                            // Currency Selector
                            ExposedDropdownMenuBox(
                                expanded = currencyExpanded,
                                onExpandedChange = { currencyExpanded = !currencyExpanded }
                            ) {
                                OutlinedTextField(
                                    value = currentCurrency.displayLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.settings_currency)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .testTag("settings_currency_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = currencyExpanded,
                                    onDismissRequest = { currencyExpanded = false }
                                ) {
                                    CurrencyOption.entries.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.displayLabel) },
                                            onClick = {
                                                AppSettingsPreferences.setCurrency(context, opt)
                                                currencyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Profit Margin %
                            OutlinedTextField(
                                value = marginText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() }
                                    marginText = filtered
                                    filtered.toIntOrNull()?.let { pct ->
                                        AppSettingsPreferences.setMarginPercent(context, pct)
                                    }
                                },
                                label = { Text(stringResource(R.string.settings_margin)) },
                                supportingText = { Text(stringResource(R.string.settings_margin_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_margin_input")
                            )
                        }
                    }

                    // 4. Features & Extra Capabilities Post-It Card (no v3 label)
                    PostItCard(
                        backgroundColor = PostItYellow.copy(alpha = 0.84f),
                        rotationDegrees = -0.25f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_features_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = BlueprintNavy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_features_section),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueprintNavy,
                                    modifier = Modifier.testTag("settings_features_section")
                                )
                            }

                            Text(
                                text = stringResource(R.string.settings_features_list),
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 15.sp,
                                color = CharcoalInk
                            )

                            Text(
                                text = stringResource(R.string.settings_extras_section),
                                fontFamily = CaveatFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueprintNavy
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PostItButton(
                                    text = stringResource(R.string.settings_extras_chef_btn),
                                    icon = Icons.Default.AutoAwesome,
                                    onClick = {
                                        onDismiss()
                                        onOpenChefChat()
                                    },
                                    backgroundColor = PostItPeach,
                                    fontSize = 17.sp,
                                    showTape = false,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_open_chef_button")
                                )
                            }

                            PostItButton(
                                text = stringResource(R.string.settings_replay_tutorial_btn),
                                icon = Icons.Default.School,
                                onClick = onReplayTutorial,
                                backgroundColor = PostItMint,
                                fontSize = 17.sp,
                                showTape = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_replay_tutorial_button")
                            )
                        }
                    }

                    // 5. Backups & Local Exports (Sketched Notebook Card)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sketchedBorder(color = CharcoalInk.copy(alpha = 0.72f), strokeWidthDp = 1.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_backup_section),
                                fontFamily = CaveatFontFamily,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueprintNavy
                            )

                            OutlinedButton(
                                onClick = { startDriveSyncFlow() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_sync_drive_button")
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_sync_drive_btn))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onExportAiMarkdown { md ->
                                            pendingExportText = md
                                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                            exportMdLauncher.launch("recetario_ia_$stamp.md")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_export_md_button")
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.settings_export_md_btn), maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onExportAiCsv { csv ->
                                            pendingExportText = csv
                                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                            exportCsvLauncher.launch("recetario_inventario_$stamp.csv")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_export_csv_button")
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.settings_export_csv_btn), maxLines = 1)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    onExportFullBackupJson { json ->
                                        pendingExportJson = json
                                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                        exportFileLauncher.launch("recetario_backup_$stamp.json")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_json_backup_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_export_backup_file))
                            }

                            OutlinedButton(
                                onClick = {
                                    importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("import_json_backup_button")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_import_backup_file))
                            }

                            OutlinedButton(
                                onClick = {
                                    onExportFullBackupJson { json ->
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_share_backup))
                            }
                        }
                    }

                    // 6. In-App Version Update Channel Post-It Card
                    PostItCard(
                        backgroundColor = PostItPeach.copy(alpha = 0.78f),
                        rotationDegrees = 0.25f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.settings_updates_section),
                                fontFamily = CaveatFontFamily,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueprintNavy,
                                modifier = Modifier.testTag("settings_updates_section")
                            )

                            val remoteLabel = distributionConfig?.latestVersionName?.ifBlank {
                                distributionConfig.latestVersionCode.toString()
                            }.orEmpty()
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasPendingUpdate) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (hasPendingUpdate) MaterialTheme.colorScheme.primary else StockGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (hasPendingUpdate) {
                                            stringResource(R.string.settings_updates_status_available, remoteLabel)
                                        } else {
                                            stringResource(R.string.settings_updates_status_ok, BuildConfig.VERSION_NAME)
                                        },
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 15.sp,
                                        color = CharcoalInk
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = githubRepoInput,
                                onValueChange = { raw ->
                                    githubRepoInput = raw
                                    AppSettingsPreferences.setGithubRepo(context, raw)
                                },
                                label = { Text(stringResource(R.string.settings_github_repo_label)) },
                                placeholder = { Text(stringResource(R.string.settings_github_repo_hint)) },
                                supportingText = { Text(stringResource(R.string.settings_github_repo_helper)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_github_repo_input")
                            )

                            val pendingUpdateConfig = distributionConfig?.takeIf {
                                it.latestVersionCode > BuildConfig.VERSION_CODE
                            }
                            if (pendingUpdateConfig != null) {
                                Button(
                                    onClick = { onOpenAvailableUpdate(pendingUpdateConfig) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_install_update_button")
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.settings_install_update_btn, remoteLabel))
                                }
                            }

                            OutlinedButton(
                                onClick = onCheckUpdates,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_check_updates_button")
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_check_updates))
                            }
                        }
                    }

                    // 7. Privacy Policy, License & Delete Account
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sketchedBorder(color = CharcoalInk.copy(alpha = 0.72f), strokeWidthDp = 1.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PostItButton(
                                text = stringResource(R.string.settings_privacy_policy),
                                icon = Icons.Default.Policy,
                                onClick = onShowPrivacyPolicy,
                                backgroundColor = PostItPink.copy(alpha = 0.85f),
                                fontSize = 18.sp,
                                showTape = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_privacy_policy_button")
                            )

                            Text(
                                text = stringResource(R.string.settings_license_section),
                                fontFamily = CaveatFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueprintNavy
                            )
                            if (savedLicense.isNotBlank()) {
                                Surface(
                                    color = StockGreen.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_license_active, savedLicense),
                                        color = StockGreen,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = licenseInput,
                                    onValueChange = { licenseInput = it },
                                    label = { Text(stringResource(R.string.settings_license_label)) },
                                    placeholder = { Text(stringResource(R.string.settings_license_placeholder)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (licenseInput.isNotBlank()) {
                                            onActivateLicense(licenseInput.trim())
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            HorizontalDivider(color = CharcoalInk.copy(alpha = 0.14f))

                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("delete_account_button")
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_delete_account))
                            }
                        }
                    }
                }

                HorizontalDivider(color = CharcoalInk.copy(alpha = 0.16f))

                // Footer Close Stamp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    PostItButton(
                        text = stringResource(R.string.action_close),
                        onClick = onDismiss,
                        backgroundColor = PostItYellow,
                        fontSize = 19.sp,
                        showTape = false,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("settings_close_button")
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_account_confirm_title),
                    fontFamily = CaveatFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_delete_account_confirm_body),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_delete_account_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.privacy_title),
                fontFamily = CaveatFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BlueprintNavy
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .sketchedBorder(color = CharcoalInk.copy(alpha = 0.45f), strokeWidthDp = 1.4f)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.privacy_body),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 16.sp,
                    color = CharcoalInk
                )
            }
        },
        confirmButton = {
            PostItButton(
                text = stringResource(R.string.action_understood),
                onClick = onDismiss,
                backgroundColor = PostItMint,
                fontSize = 18.sp,
                showTape = false
            )
        }
    )
}

@Composable
fun UpdateAvailableDialog(
    config: DistributionConfig,
    isMandatory: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val versionLabel = config.latestVersionName.ifBlank { config.latestVersionCode.toString() }
    val bodyText = if (isMandatory) {
        stringResource(R.string.update_dialog_required_body, versionLabel, config.updateNotes)
    } else {
        stringResource(R.string.update_dialog_body, versionLabel, config.updateNotes)
    }

    AlertDialog(
        onDismissRequest = { if (!isMandatory) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.update_dialog_title, versionLabel),
                fontFamily = CaveatFontFamily,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = BlueprintNavy
            )
        },
        text = {
            Text(
                text = bodyText,
                fontFamily = PatrickHandFontFamily,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            if (config.updateUrl.isNotBlank()) {
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, config.updateUrl.toUri()))
                        } catch (_: Exception) {
                        }
                    }
                ) {
                    Text(stringResource(R.string.update_dialog_action))
                }
            } else if (!isMandatory) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_understood))
                }
            }
        },
        dismissButton = {
            if (!isMandatory) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    )
}

@Composable
fun LicenseActivationDialog(
    onActivate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = {},
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.license_required_title),
                fontFamily = CaveatFontFamily,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = BlueprintNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.license_required_desc),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 16.sp
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.settings_license_label)) },
                    placeholder = { Text(stringResource(R.string.settings_license_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (code.isNotBlank()) onActivate(code.trim()) },
                enabled = code.isNotBlank()
            ) {
                Text(stringResource(R.string.settings_license_activate))
            }
        },
        dismissButton = {
            TextButton(onClick = onSignOut) {
                Text(stringResource(R.string.account_sign_out))
            }
        }
    )
}

@Composable
fun DrivePermissionPromptDialog(
    onGrantPermission: () -> Unit,
    onDismissLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissLater,
        containerColor = NotebookPaperBackground,
        modifier = Modifier
            .sketchedBorder(color = CharcoalInk, strokeWidthDp = 2f)
            .testTag("drive_permission_prompt_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = StockGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.drive_perm_dialog_title),
                    fontFamily = CaveatFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueprintNavy
                )
            }
        },
        text = {
            PostItCard(
                backgroundColor = PostItMint.copy(alpha = 0.72f),
                rotationDegrees = -0.3f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.drive_perm_dialog_body),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 16.sp,
                    color = CharcoalInk
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGrantPermission,
                colors = ButtonDefaults.buttonColors(containerColor = StockGreen),
                modifier = Modifier.testTag("drive_permission_grant_button")
            ) {
                Text(stringResource(R.string.drive_perm_grant_btn), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissLater,
                modifier = Modifier.testTag("drive_permission_later_button")
            ) {
                Text(stringResource(R.string.drive_perm_later_btn))
            }
        }
    )
}
