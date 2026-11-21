package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.sync.FullBackupManager
import com.example.data.sync.InventorySyncManager
import com.example.ui.theme.StockGreen

@Composable
fun ShareInventoryDialog(
    pantryItems: List<PantryItem>,
    onImportConfirmed: (List<PantryItem>, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onExportFullBackup: (((String) -> Unit) -> Unit)? = null,
    onImportFullBackup: ((String, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var inputText by remember { mutableStateOf("") }
    var mergeMode by remember { mutableStateOf(true) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    val parsedItems = remember(inputText) {
        if (inputText.isNotBlank()) InventorySyncManager.parseSyncToken(inputText) else null
    }
    val parsedBackup = remember(inputText) {
        if (inputText.isNotBlank() && parsedItems == null) FullBackupManager.parseFromJson(inputText) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.sync_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.sync_tab_share)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.sync_tab_import)) }
                    )
                }

                if (selectedTab == 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.sync_pantry_header, pantryItems.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.sync_pantry_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            InventorySyncManager.shareInventory(context, pantryItems)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_whatsapp_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sync_share_pantry_btn))
                    }

                    OutlinedButton(
                        onClick = {
                            val token = InventorySyncManager.createSyncToken(pantryItems)
                            clipboardManager.setText(AnnotatedString(token))
                            copiedToClipboard = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("copy_token_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (copiedToClipboard) {
                                stringResource(R.string.sync_copied_btn)
                            } else {
                                stringResource(R.string.sync_copy_token_btn)
                            }
                        )
                    }

                    if (onExportFullBackup != null) {
                        val chooserTitle = stringResource(R.string.settings_share_backup)
                        OutlinedButton(
                            onClick = {
                                onExportFullBackup { json ->
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_share_backup))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.sync_import_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(stringResource(R.string.sync_import_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_token_field"),
                        maxLines = 4,
                        trailingIcon = {
                            FilledTonalButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        inputText = clip
                                    }
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(stringResource(R.string.action_paste), fontSize = 11.sp)
                            }
                        }
                    )

                    if (parsedItems != null) {
                        Surface(
                            color = StockGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = StockGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.sync_valid_pantry, parsedItems.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StockGreen
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = mergeMode, onClick = { mergeMode = true })
                                Text(
                                    text = stringResource(R.string.sync_merge_opt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = !mergeMode, onClick = { mergeMode = false })
                                Text(
                                    text = stringResource(R.string.sync_replace_opt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else if (parsedBackup != null) {
                        Surface(
                            color = StockGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = StockGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        R.string.sync_valid_full,
                                        parsedBackup.recipes.size,
                                        parsedBackup.pantry.size,
                                        parsedBackup.shopping.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StockGreen
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = mergeMode, onClick = { mergeMode = true })
                                Text(
                                    text = stringResource(R.string.sync_merge_opt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = !mergeMode, onClick = { mergeMode = false })
                                Text(
                                    text = stringResource(R.string.sync_replace_opt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else if (inputText.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.sync_invalid_token),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1 && parsedItems != null) {
                Button(
                    onClick = {
                        onImportConfirmed(parsedItems, mergeMode)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("apply_import_button")
                ) {
                    Text(stringResource(R.string.sync_apply_btn))
                }
            } else if (selectedTab == 1 && parsedBackup != null && onImportFullBackup != null) {
                Button(
                    onClick = {
                        onImportFullBackup(inputText, !mergeMode)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("apply_import_button")
                ) {
                    Text(stringResource(R.string.sync_apply_btn))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }
        },
        dismissButton = {
            if (selectedTab == 1 && (parsedItems != null || (parsedBackup != null && onImportFullBackup != null))) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
