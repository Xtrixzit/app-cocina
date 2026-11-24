package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ShoppingItem
import com.example.ui.theme.CaveatFontFamily
import com.example.util.LocalizedLabels
import com.example.util.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemEditDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(item.name) }
    var quantityText by remember {
        mutableStateOf(UnitConverter.formatQuantity(item.quantity))
    }
    var unit by remember { mutableStateOf(if (item.unit.isNotBlank()) item.unit else "unidades") }
    var unitExpanded by remember { mutableStateOf(false) }

    val commonUnits = UnitConverter.allShoppingUnits

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.shopping_edit_title),
                fontFamily = CaveatFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.shopping_edit_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_shopping_name"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(stringResource(R.string.shopping_qty_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("edit_shopping_qty"),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = LocalizedLabels.unit(context, unit),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.recipe_form_unit_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .testTag("edit_shopping_unit")
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            commonUnits.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(LocalizedLabels.unit(context, u)) },
                                    onClick = {
                                        val currentQty = UnitConverter.parseDecimal(quantityText)
                                        val converted = currentQty?.let { UnitConverter.convert(it, unit, u) }
                                        if (converted != null) {
                                            quantityText = UnitConverter.formatQuantity(converted)
                                        }
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                val parsedPreviewQty = UnitConverter.parseDecimal(quantityText) ?: 0.0
                UnitConverter.humanEquivalent(parsedPreviewQty, unit)?.let { (eqVal, eqUnit) ->
                    Text(
                        text = stringResource(
                            R.string.pantry_form_equiv_fmt,
                            UnitConverter.formatQuantity(eqVal),
                            LocalizedLabels.unit(context, eqUnit)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedQty = UnitConverter.parseDecimal(quantityText) ?: 0.0
                        onSave(
                            item.copy(
                                name = name.trim(),
                                quantity = parsedQty.coerceAtLeast(0.0),
                                unit = unit.trim()
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("edit_shopping_save_button")
            ) {
                Text(stringResource(R.string.action_save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
