package com.example.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DistributionConfig
import com.example.settings.AppLanguage
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.StockGreen
import com.example.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommercialSettingsDialog(
    isGuestMode: Boolean,
    localGuestCountForMigration: Int,
    onDismiss: () -> Unit,
    onMigrateGuestToCloud: () -> Unit,
    onExportFullBackupJson: (onResult: (String) -> Unit) -> Unit,
    onImportFullBackupJson: (String, Boolean) -> Unit,
    onActivateLicense: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onShowPrivacyPolicy: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val currentLanguage by AppSettingsPreferences.observeLanguage(context).collectAsStateWithLifecycle()
    val currentCurrency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val currentMargin by AppSettingsPreferences.observeMarginPercent(context).collectAsStateWithLifecycle()
    val savedLicense by AppSettingsPreferences.observeLicenseKey(context).collectAsStateWithLifecycle()

    var languageExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var marginText by remember(currentMargin) { mutableStateOf(currentMargin.toString()) }
    var licenseInput by remember(savedLicense) { mutableStateOf(savedLicense) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                fontFamily = CaveatFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Language Selector
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

                // 2. Currency Selector
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

                // 3. Profit Margin %
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

                HorizontalDivider()

                // 4. Backup & Guest Migration
                Text(
                    text = stringResource(R.string.settings_backup_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

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

                HorizontalDivider()

                // 5. Private Distribution License
                Text(
                    text = stringResource(R.string.settings_license_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
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

                HorizontalDivider()

                // 6. Updates & Privacy & Account Deletion
                OutlinedButton(
                    onClick = onCheckUpdates,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_check_updates))
                }

                OutlinedButton(
                    onClick = onShowPrivacyPolicy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_privacy_policy))
                }

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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_account_confirm_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { Text(stringResource(R.string.settings_delete_account_confirm_body)) },
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
        title = {
            Text(
                text = stringResource(R.string.privacy_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.privacy_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_understood))
            }
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
        title = {
            Text(
                text = stringResource(R.string.update_dialog_title, versionLabel),
                fontWeight = FontWeight.Bold
            )
        },
        text = { Text(bodyText) },
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
        title = {
            Text(
                text = stringResource(R.string.license_required_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.license_required_desc))
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
