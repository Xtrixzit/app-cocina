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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PantryItem
import com.example.settings.AppSettingsPreferences
import com.example.util.LocalizedLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryFormDialog(
    initialItem: PantryItem? = null,
    onDismiss: () -> Unit,
    onSave: (PantryItem) -> Unit
) {
    val context = LocalContext.current
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val isEdit = initialItem != null

    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "Despensa") }
    var quantityText by remember {
        mutableStateOf(
            if (initialItem != null) {
                if (initialItem.quantity % 1.0 == 0.0) initialItem.quantity.toInt().toString() else initialItem.quantity.toString()
            } else "500"
        )
    }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "g") }
    var minQuantityText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.minQuantity > 0.0) {
                if (initialItem.minQuantity % 1.0 == 0.0) initialItem.minQuantity.toInt().toString() else initialItem.minQuantity.toString()
            } else "100"
        )
    }
    var expirationDate by remember { mutableStateOf(initialItem?.expirationDate ?: "") }

    var costText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.cost > 0.0) {
                if (initialItem.cost % 1.0 == 0.0) initialItem.cost.toInt().toString() else initialItem.cost.toString()
            } else ""
        )
    }
    var costBaseQtyText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.costBaseQuantity > 0.0) {
                if (initialItem.costBaseQuantity % 1.0 == 0.0) initialItem.costBaseQuantity.toInt().toString() else initialItem.costBaseQuantity.toString()
            } else ""
        )
    }

    val categories = listOf("Despensa", "Verduras", "Frutas", "Carnes", "Lácteos", "Granos", "Especias", "Bebidas", "Congelados")
    val units = listOf("g", "kg", "ml", "l", "unidades", "taza", "cda", "cdta", "latas", "paquetes")

    val parsedCost = costText.toDoubleOrNull() ?: 0.0
    val parsedCurrentQty = quantityText.toDoubleOrNull() ?: 0.0
    val parsedCostBase = costBaseQtyText.toDoubleOrNull() ?: if (parsedCurrentQty > 0.0) parsedCurrentQty else 1.0
    val calculatedUnitCost = if (parsedCost > 0.0 && parsedCostBase > 0.0) parsedCost / parsedCostBase else 0.0
    val displayUnit = LocalizedLabels.unit(context, unit)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) stringResource(R.string.pantry_form_edit_title) else stringResource(R.string.pantry_form_new_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
                    label = { Text(stringResource(R.string.pantry_form_name_label)) },
                    placeholder = { Text(stringResource(R.string.pantry_form_name_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pantry_form_name"),
                    singleLine = true
                )

                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = LocalizedLabels.pantryCategory(context, category),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recipe_form_cat_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(LocalizedLabels.pantryCategory(context, cat)) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(stringResource(R.string.pantry_form_qty_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("pantry_form_qty"),
                        singleLine = true
                    )

                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = displayUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.recipe_form_unit_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(LocalizedLabels.unit(context, u)) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text(stringResource(R.string.pantry_form_price_label, currency.symbol)) },
                        placeholder = { Text("3.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("pantry_form_cost"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = costBaseQtyText,
                        onValueChange = { costBaseQtyText = it },
                        label = { Text(stringResource(R.string.pantry_form_base_qty_label, displayUnit)) },
                        placeholder = { Text(if (quantityText.isNotBlank()) quantityText else "1000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pantry_form_cost_base"),
                        singleLine = true
                    )
                }

                if (calculatedUnitCost > 0.0) {
                    Text(
                        text = stringResource(
                            R.string.pantry_form_unit_cost_preview,
                            AppSettingsPreferences.formatMoney(calculatedUnitCost, currency, forceFourDecimalsIfTiny = true),
                            displayUnit
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = minQuantityText,
                    onValueChange = { minQuantityText = it },
                    label = { Text(stringResource(R.string.pantry_form_min_qty_label)) },
                    placeholder = { Text("100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pantry_form_min_qty"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { expirationDate = it },
                    label = { Text(stringResource(R.string.pantry_form_exp_label)) },
                    placeholder = { Text(stringResource(R.string.pantry_form_exp_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val qty = quantityText.toDoubleOrNull() ?: 0.0
                        val minQty = minQuantityText.toDoubleOrNull() ?: 0.0
                        val costVal = costText.toDoubleOrNull() ?: 0.0
                        val costBaseVal = costBaseQtyText.toDoubleOrNull() ?: qty
                        val item = (initialItem ?: PantryItem()).copy(
                            name = name.trim(),
                            category = category.trim(),
                            quantity = qty.coerceAtLeast(0.0),
                            unit = unit.trim(),
                            minQuantity = minQty.coerceAtLeast(0.0),
                            cost = costVal.coerceAtLeast(0.0),
                            costBaseQuantity = costBaseVal.coerceAtLeast(0.0),
                            expirationDate = expirationDate.trim()
                        )
                        onSave(item)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("pantry_form_save")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
