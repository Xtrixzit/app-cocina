package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PantryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryFormDialog(
    initialItem: PantryItem? = null,
    onDismiss: () -> Unit,
    onSave: (PantryItem) -> Unit
) {
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

    // Live preview of unit cost
    val parsedCost = costText.toDoubleOrNull() ?: 0.0
    val parsedCurrentQty = quantityText.toDoubleOrNull() ?: 0.0
    val parsedCostBase = costBaseQtyText.toDoubleOrNull() ?: if (parsedCurrentQty > 0.0) parsedCurrentQty else 1.0
    val calculatedUnitCost = if (parsedCost > 0.0 && parsedCostBase > 0.0) parsedCost / parsedCostBase else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Editar Provisión y Costo" else "Añadir a la Despensa",
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
                    label = { Text("Nombre del producto/ingrediente *") },
                    placeholder = { Text("Ej. Arroz redondo, Aceite de oliva") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pantry_form_name"),
                    singleLine = true
                )

                // Category dropdown
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity and Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Cantidad actual") },
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
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // --- COST / PRICING SECTION ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Precio de compra ($)") },
                        placeholder = { Text("Ej. 3.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("pantry_form_cost"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = costBaseQtyText,
                        onValueChange = { costBaseQtyText = it },
                        label = { Text("Por cantidad ($unit)") },
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
                        text = "🏷 Costo unitario: $${"%.4f".format(calculatedUnitCost)} por $unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Min Quantity threshold for alerts
                OutlinedTextField(
                    value = minQuantityText,
                    onValueChange = { minQuantityText = it },
                    label = { Text("Stock mínimo para alerta (opcional)") },
                    placeholder = { Text("Ej. 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pantry_form_min_qty"),
                    singleLine = true
                )

                // Expiration date
                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { expirationDate = it },
                    label = { Text("Fecha de vencimiento (opcional)") },
                    placeholder = { Text("AAAA-MM-DD") },
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
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
