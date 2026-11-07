package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PantryItem
import com.example.data.sync.InventorySyncManager
import com.example.ui.theme.StockGreen

@Composable
fun ShareInventoryDialog(
    pantryItems: List<PantryItem>,
    onImportConfirmed: (List<PantryItem>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Compartir, 1: Recibir

    var inputText by remember { mutableStateOf("") }
    var mergeMode by remember { mutableStateOf(true) } // true: fusionar, false: reemplazar
    var copiedToClipboard by remember { mutableStateOf(false) }

    val parsedItems = remember(inputText) {
        if (inputText.isNotBlank()) InventorySyncManager.parseSyncToken(inputText) else null
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
                    text = "Sincronizar entre Dispositivos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Compartir") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Recibir / Actualizar") }
                    )
                }

                if (selectedTab == 0) {
                    // --- TAB COMPARTIR ---
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Envía tu inventario actual (${pantryItems.size} provisiones)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "El otro dispositivo puede escanear o pegar este código para actualizar su despensa sin necesitar cuentas de Google.",
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
                        Text("Compartir por WhatsApp / Mensajes")
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
                        Text(if (copiedToClipboard) "¡Código copiado al portapapeles!" else "Copiar código de sincronización")
                    }

                } else {
                    // --- TAB RECIBIR / ACTUALIZAR ---
                    Text(
                        text = "Pega aquí el código o mensaje recibido del otro dispositivo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Pega el código RECETARIO_SYNC_V1:... o texto aquí") },
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
                                Text("Pegar", fontSize = 11.sp)
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
                                    text = "¡Código válido! Contiene ${parsedItems.size} provisiones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StockGreen
                                )
                            }
                        }

                        // Merge Options
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = mergeMode, onClick = { mergeMode = true })
                                Text(
                                    text = "Fusionar / Actualizar existencias (Recomendado)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = !mergeMode, onClick = { mergeMode = false })
                                Text(
                                    text = "Reemplazar completamente mi despensa",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else if (inputText.isNotBlank()) {
                        Text(
                            text = "Código o formato no reconocido. Asegúrate de copiar el mensaje completo que comienza con RECETARIO_SYNC_V1:...",
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
                    Text("Actualizar Despensa")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
        dismissButton = {
            if (selectedTab == 1 && parsedItems != null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
