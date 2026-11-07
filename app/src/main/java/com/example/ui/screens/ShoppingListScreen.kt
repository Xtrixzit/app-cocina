package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PantryItem
import com.example.data.model.ShoppingItem
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.ShoppingItemCard
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.StockGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: MainViewModel,
    pantryItems: List<PantryItem>
) {
    val shoppingUiState by viewModel.shoppingState.collectAsState()

    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemUnit by remember { mutableStateOf("unidades") }
    var markAsAvailable by remember { mutableStateOf(true) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Todos, 1 = Disponibles, 2 = Por Comprar
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    val commonUnits = listOf("unidades", "kg", "g", "l", "ml", "paquetes", "latas", "piezas", "docenas")

    val onAddItem = {
        if (newItemName.isNotBlank()) {
            val qty = newItemQty.toDoubleOrNull() ?: 1.0
            viewModel.saveShoppingItem(
                ShoppingItem(
                    name = newItemName.trim(),
                    quantity = qty.coerceAtLeast(0.0),
                    unit = newItemUnit.trim(),
                    isPurchased = markAsAvailable
                )
            )
            newItemName = ""
            newItemQty = "1"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .notebookPaperBackground()
        ) {
            // Sketched Box Header matching reference image: "[Check list]"
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Check list • Inventario",
                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Quick Add Input Card with explicit available quantity controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Product Name field
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        placeholder = {
                            Text(
                                "Nombre del producto...",
                                fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                fontSize = 17.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_add_shopping_name"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // Available Quantity + Unit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemQty,
                            onValueChange = { newItemQty = it },
                            label = { Text("Cant. disponible") },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { onAddItem() }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_shopping_qty"),
                            singleLine = true
                        )

                        ExposedDropdownMenuBox(
                            expanded = unitMenuExpanded,
                            onExpandedChange = { unitMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = newItemUnit,
                                onValueChange = { newItemUnit = it },
                                label = { Text("Unidad") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .testTag("quick_add_shopping_unit"),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false }
                            ) {
                                commonUnits.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            newItemUnit = u
                                            unitMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        com.example.ui.components.PostItButton(
                            text = "+ Añadir",
                            onClick = onAddItem,
                            backgroundColor = com.example.ui.theme.PostItPeach,
                            rotationDegrees = -1.5f,
                            fontSize = 18.sp,
                            modifier = Modifier.testTag("quick_add_shopping_button")
                        )
                    }

                    // Available in inventory toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = markAsAvailable,
                            onCheckedChange = { markAsAvailable = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Disponible en inventario (se sincroniza con la Despensa)",
                            fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when (val state = shoppingUiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val allItems = state.data
                    val purchasedItems = allItems.filter { it.isPurchased }
                    val pendingItems = allItems.filter { !it.isPurchased }

                    // Post-It Tabs: "Todos", "Disponibles", "Por Comprar"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.PostItFilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = "Todos (${allItems.size})",
                            selectedColor = com.example.ui.theme.PostItSky
                        )
                        com.example.ui.components.PostItFilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = "Disponibles (${purchasedItems.size})",
                            selectedColor = com.example.ui.theme.PostItMint
                        )
                        com.example.ui.components.PostItFilterChip(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = "Por Comprar (${pendingItems.size})",
                            selectedColor = com.example.ui.theme.PostItYellow
                        )
                    }

                    val activeList = when (selectedTab) {
                        1 -> purchasedItems
                        2 -> pendingItems
                        else -> allItems
                    }

                    if (activeList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Default.ShoppingCart else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 2) "No hay artículos por comprar" else if (selectedTab == 1) "No hay artículos disponibles en este filtro" else "¡Tu check list de inventario está vacía!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Añade un producto y su cantidad disponible arriba para gestionar tu inventario.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (purchasedItems.isNotEmpty() && selectedTab != 2) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${allItems.size} producto(s) en seguimiento",
                                            fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        com.example.ui.components.PostItButton(
                                            text = "Limpiar verificados",
                                            icon = Icons.Default.DeleteSweep,
                                            onClick = { viewModel.clearPurchasedItems(purchasedItems) },
                                            backgroundColor = com.example.ui.theme.PostItPink,
                                            fontSize = 16.sp,
                                            rotationDegrees = 1f,
                                            modifier = Modifier.testTag("clear_purchased_button")
                                        )
                                    }
                                }
                            }

                            items(activeList, key = { it.id }) { item ->
                                val matchingPantry = pantryItems.find {
                                    it.name.trim().equals(item.name.trim(), ignoreCase = true)
                                }
                                ShoppingItemCard(
                                    item = item,
                                    pantryItem = matchingPantry,
                                    onTogglePurchased = { isChecked ->
                                        viewModel.toggleShoppingItem(item.id, isChecked)
                                    },
                                    onAdjustQuantity = { delta ->
                                        viewModel.adjustShoppingItemQuantity(item, delta)
                                    },
                                    onEdit = { editingItem = item },
                                    onDelete = { viewModel.deleteShoppingItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        ShoppingItemEditDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                viewModel.saveShoppingItem(updated)
            }
        )
    }
}
