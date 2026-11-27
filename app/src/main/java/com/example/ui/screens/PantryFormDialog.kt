package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.ui.theme.StockGreen
import com.example.util.LocalizedLabels
import com.example.util.UnitConverter

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
    var unit by remember { mutableStateOf(initialItem?.unit?.ifBlank { "g" } ?: "g") }

    var quantityText by remember {
        mutableStateOf(
            if (initialItem != null) {
                UnitConverter.formatQuantity(initialItem.quantity)
            } else ""
        )
    }
    var minQuantityText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.minQuantity > 0.0) {
                UnitConverter.formatQuantity(initialItem.minQuantity)
            } else ""
        )
    }
    var expirationDate by remember { mutableStateOf(initialItem?.expirationDate ?: "") }

    // Pricing state: defaults to batch mode unless base quantity == 1
    var isPerUnitPriceMode by remember {
        mutableStateOf(initialItem != null && initialItem.cost > 0.0 && initialItem.costBaseQuantity == 1.0)
    }
    var priceInputUnit by remember {
        mutableStateOf(
            if (initialItem == null && (unit == "g" || unit == "mg")) {
                "kg"
            } else if (initialItem == null && unit == "ml") {
                "l"
            } else {
                unit
            }
        )
    }
    var costText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.cost > 0.0) {
                UnitConverter.formatQuantity(initialItem.cost, maxDecimals = 4)
            } else ""
        )
    }
    var costBaseQtyText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.costBaseQuantity > 0.0) {
                UnitConverter.formatQuantity(initialItem.costBaseQuantity)
            } else ""
        )
    }

    val categories = listOf("Despensa", "Verduras", "Frutas", "Carnes", "Lácteos", "Granos", "Especias", "Bebidas", "Congelados")
    val units = UnitConverter.allPantryUnits
    val compatiblePriceUnits = remember(unit) { UnitConverter.compatibleUnitsFor(unit) }
    val activePriceUnit = if (priceInputUnit in compatiblePriceUnits) priceInputUnit else unit

    val parsedCurrentQty = UnitConverter.parseNonNegativeDecimal(quantityText) ?: 0.0
    val parsedCost = UnitConverter.parseNonNegativeDecimal(costText) ?: 0.0
    val defaultBatchInPriceUnit = UnitConverter.convert(parsedCurrentQty, unit, activePriceUnit) ?: parsedCurrentQty
    val parsedBaseInPriceUnit = UnitConverter.parseNonNegativeDecimal(costBaseQtyText)
        ?: defaultBatchInPriceUnit.coerceAtLeast(0.0)

    val costBreakdown = UnitConverter.computePantryCostBreakdown(
        enteredPrice = parsedCost,
        enteredBaseQty = parsedBaseInPriceUnit,
        priceInputUnit = activePriceUnit,
        itemUnit = unit,
        isPerSingleUnitMode = isPerUnitPriceMode
    )

    val calculatedUnitCost = costBreakdown.unitCostInItemUnit
    val macroRef = UnitConverter.macroUnitCostReference(calculatedUnitCost, unit)
    val stockEquivalent = UnitConverter.humanEquivalent(parsedCurrentQty, unit)
    val totalStockValue = parsedCurrentQty * calculatedUnitCost
    val displayUnit = LocalizedLabels.unit(context, unit)
    val displayPriceUnit = LocalizedLabels.unit(context, activePriceUnit)

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
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
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
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.1f)
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
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .testTag("pantry_form_unit")
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { newUnit ->
                                DropdownMenuItem(
                                    text = { Text(LocalizedLabels.unit(context, newUnit)) },
                                    onClick = {
                                        val oldUnit = unit
                                        if (oldUnit != newUnit && UnitConverter.isSameOrCompatible(oldUnit, newUnit)) {
                                            UnitConverter.parseNonNegativeDecimal(quantityText)?.let { oldQty ->
                                                UnitConverter.convert(oldQty, oldUnit, newUnit)?.let { converted ->
                                                    quantityText = UnitConverter.formatQuantity(converted)
                                                }
                                            }
                                            UnitConverter.parseNonNegativeDecimal(minQuantityText)?.let { oldMin ->
                                                UnitConverter.convert(oldMin, oldUnit, newUnit)?.let { convertedMin ->
                                                    minQuantityText = UnitConverter.formatQuantity(convertedMin)
                                                }
                                            }
                                        }
                                        unit = newUnit
                                        if (!UnitConverter.isSameOrCompatible(priceInputUnit, newUnit)) {
                                            priceInputUnit = newUnit
                                        }
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (stockEquivalent != null) {
                    Text(
                        text = stringResource(
                            R.string.pantry_form_equiv_fmt,
                            UnitConverter.formatQuantity(stockEquivalent.first),
                            LocalizedLabels.unit(context, stockEquivalent.second)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Flexible Price & Unit Conversion Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pantry_form_price_mode_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !isPerUnitPriceMode,
                                onClick = { isPerUnitPriceMode = false },
                                label = { Text(stringResource(R.string.pantry_form_price_mode_batch)) },
                                modifier = Modifier.testTag("pantry_form_price_mode_batch")
                            )
                            FilterChip(
                                selected = isPerUnitPriceMode,
                                onClick = {
                                    isPerUnitPriceMode = true
                                    costBaseQtyText = "1"
                                },
                                label = { Text(stringResource(R.string.pantry_form_price_mode_unit)) },
                                modifier = Modifier.testTag("pantry_form_price_mode_unit")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = costText,
                                onValueChange = { costText = it },
                                label = { Text(stringResource(R.string.pantry_form_price_label, currency.symbol)) },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("pantry_form_cost"),
                                singleLine = true
                            )

                            if (compatiblePriceUnits.size > 1) {
                                var priceUnitExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = priceUnitExpanded,
                                    onExpandedChange = { priceUnitExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = displayPriceUnit,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.pantry_form_price_unit_label)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priceUnitExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                            .testTag("pantry_form_price_unit")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = priceUnitExpanded,
                                        onDismissRequest = { priceUnitExpanded = false }
                                    ) {
                                        compatiblePriceUnits.forEach { pUnit ->
                                            DropdownMenuItem(
                                                text = { Text(LocalizedLabels.unit(context, pUnit)) },
                                                onClick = {
                                                    val prevUnit = activePriceUnit
                                                    if (!isPerUnitPriceMode && prevUnit != pUnit) {
                                                        UnitConverter.parseNonNegativeDecimal(costBaseQtyText)?.let { oldBase ->
                                                            UnitConverter.convert(oldBase, prevUnit, pUnit)?.let { conv ->
                                                                costBaseQtyText = UnitConverter.formatQuantity(conv)
                                                            }
                                                        }
                                                    }
                                                    priceInputUnit = pUnit
                                                    priceUnitExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!isPerUnitPriceMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = costBaseQtyText,
                                    onValueChange = { costBaseQtyText = it },
                                    label = { Text(stringResource(R.string.pantry_form_base_qty_label, displayPriceUnit)) },
                                    placeholder = {
                                        Text(
                                            if (defaultBatchInPriceUnit > 0.0) UnitConverter.formatQuantity(defaultBatchInPriceUnit) else "1"
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pantry_form_cost_base"),
                                    singleLine = true
                                )
                            }

                            if (parsedCurrentQty > 0.0) {
                                TextButton(
                                    onClick = {
                                        costBaseQtyText = UnitConverter.formatQuantity(defaultBatchInPriceUnit)
                                    }
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.pantry_form_use_stock_qty,
                                            UnitConverter.formatQuantity(defaultBatchInPriceUnit),
                                            displayPriceUnit
                                        )
                                    )
                                }
                            }
                        }

                        if (calculatedUnitCost > 0.0) {
                            Column(
                                modifier = Modifier.testTag("pantry_form_unit_cost_preview"),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
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
                                if (macroRef != null) {
                                    Text(
                                        text = stringResource(
                                            R.string.pantry_form_macro_cost_fmt,
                                            AppSettingsPreferences.formatMoney(macroRef.first, currency),
                                            LocalizedLabels.unit(context, macroRef.second)
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StockGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (totalStockValue > 0.0) {
                                    Text(
                                        text = stringResource(
                                            R.string.pantry_form_total_value_preview,
                                            AppSettingsPreferences.formatMoney(totalStockValue, currency)
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = minQuantityText,
                    onValueChange = { minQuantityText = it },
                    label = { Text("${stringResource(R.string.pantry_form_min_qty_label)} ($displayUnit)") },
                    placeholder = { Text("0") },
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
                        val qty = UnitConverter.parseNonNegativeDecimal(quantityText) ?: 0.0
                        val minQty = UnitConverter.parseNonNegativeDecimal(minQuantityText) ?: 0.0
                        val finalCostVal = costBreakdown.costInItemUnit.coerceAtLeast(0.0)
                        val finalCostBaseVal = if (finalCostVal > 0.0) {
                            costBreakdown.baseQuantityInItemUnit.coerceAtLeast(0.0001)
                        } else 0.0
                        val item = (initialItem ?: PantryItem()).copy(
                            name = name.trim(),
                            category = category.trim(),
                            quantity = qty,
                            unit = unit.trim(),
                            minQuantity = minQty,
                            cost = finalCostVal,
                            costBaseQuantity = finalCostBaseVal,
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
