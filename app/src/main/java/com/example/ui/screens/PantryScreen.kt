package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PantryItem
import com.example.data.model.ShoppingItem
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.PantryItemCard
import com.example.ui.components.ShareInventoryDialog
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.components.stampClickable
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockRed
import com.example.ui.theme.StockYellow

@Composable
fun PantryScreen(
    viewModel: MainViewModel,
    onAddNewItem: () -> Unit
) {
    val pantryUiState by viewModel.pantryState.collectAsState()
    val searchQuery by viewModel.pantrySearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedPantryCategory.collectAsState()
    val onlyLowStock by viewModel.filterOnlyLowStock.collectAsState()

    var editingItem by remember { mutableStateOf<PantryItem?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

    val categories = listOf("Todas", "Despensa", "Verduras", "Frutas", "Carnes", "Lácteos", "Granos", "Especias", "Bebidas", "Congelados")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            com.example.ui.components.PostItButton(
                text = "+ Añadir Provisión",
                icon = Icons.Default.Add,
                onClick = onAddNewItem,
                backgroundColor = com.example.ui.theme.PostItPeach,
                rotationDegrees = -2f,
                modifier = Modifier.testTag("add_pantry_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .notebookPaperBackground()
        ) {
            // Sketched Box Header matching notebook style
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Inventory & Provisions",
                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            // Search Bar & Share Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.pantrySearchQuery.value = it },
                    placeholder = {
                        Text(
                            "Buscar provisión...",
                            fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                            fontSize = 17.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.pantrySearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), strokeWidthDp = 1.2f)
                        .testTag("pantry_search_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                com.example.ui.components.PostItButton(
                    text = "Sincronizar",
                    icon = Icons.Default.Sync,
                    onClick = { showShareDialog = true },
                    backgroundColor = com.example.ui.theme.PostItSky,
                    rotationDegrees = 1f,
                    fontSize = 18.sp,
                    modifier = Modifier.testTag("share_inventory_button")
                )
            }

            // Post-It Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Low stock toggle chip
                item {
                    com.example.ui.components.PostItFilterChip(
                        selected = onlyLowStock,
                        onClick = { viewModel.filterOnlyLowStock.value = !onlyLowStock },
                        text = "⚠ Stock Bajo",
                        selectedColor = com.example.ui.theme.PostItPeach,
                        modifier = Modifier.testTag("filter_low_stock")
                    )
                }

                items(categories) { cat ->
                    val color = when (cat) {
                        "Todas" -> com.example.ui.theme.PostItYellow
                        "Despensa" -> com.example.ui.theme.PostItMint
                        "Verduras" -> com.example.ui.theme.PostItMint
                        "Carnes" -> com.example.ui.theme.PostItPeach
                        "Lácteos" -> com.example.ui.theme.PostItSky
                        else -> com.example.ui.theme.PostItYellow
                    }
                    com.example.ui.components.PostItFilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.selectedPantryCategory.value = cat },
                        text = cat,
                        selectedColor = color
                    )
                }
            }

            when (val state = pantryUiState) {
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
                    val lowStockItems = allItems.filter { it.isLowStock || it.isOutOfStock }
                    val outOfStockItems = allItems.filter { it.isOutOfStock }

                    val filtered = allItems.filter { item ->
                        val matchesLow = !onlyLowStock || item.isLowStock || item.isOutOfStock
                        val matchesCat = selectedCategory == "Todas" || item.category.equals(selectedCategory, ignoreCase = true)
                        val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
                        matchesLow && matchesCat && matchesSearch
                    }

                    // Modern Stock Banner with Sharing and Restock triggers
                    if (allItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        MetricPill(label = "Total", value = "${allItems.size}", color = MaterialTheme.colorScheme.primary)
                                        MetricPill(label = "Bajo", value = "${lowStockItems.size - outOfStockItems.size}", color = StockYellow)
                                        MetricPill(label = "Agotado", value = "${outOfStockItems.size}", color = StockRed)
                                    }

                                    if (lowStockItems.isNotEmpty()) {
                                        com.example.ui.components.PostItButton(
                                            text = "Reponer (${lowStockItems.size})",
                                            icon = Icons.Default.ShoppingCart,
                                            onClick = {
                                                for (item in lowStockItems) {
                                                    val needed = if (item.minQuantity > item.quantity) {
                                                        item.minQuantity - item.quantity
                                                    } else if (item.quantity <= 0.0) {
                                                        if (item.minQuantity > 0.0) item.minQuantity else 1.0
                                                    } else 1.0

                                                    viewModel.saveShoppingItem(
                                                        ShoppingItem(
                                                            name = item.name,
                                                            category = item.category,
                                                            quantity = Math.round(needed * 100.0) / 100.0,
                                                            unit = item.unit,
                                                            isPurchased = false,
                                                            recipeSource = "Reposición de Despensa"
                                                        )
                                                    )
                                                }
                                            },
                                            backgroundColor = com.example.ui.theme.PostItPeach,
                                            fontSize = 18.sp,
                                            rotationDegrees = -1f,
                                            modifier = Modifier.testTag("restock_shopping_button")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (onlyLowStock) "¡Excelente! No tienes provisiones con stock bajo" else if (searchQuery.isNotBlank()) "No se encontraron provisiones" else "Tu despensa está vacía",
                                fontFamily = com.example.ui.theme.CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (onlyLowStock) "Todos tus ingredientes superan el stock mínimo configurado." else if (searchQuery.isNotBlank()) "Intenta buscar con otro nombre." else "Registra tus víveres o sincroniza el inventario desde otro teléfono.",
                                fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                com.example.ui.components.PostItButton(
                                    text = "Añadir producto",
                                    onClick = onAddNewItem,
                                    backgroundColor = com.example.ui.theme.PostItYellow,
                                    fontSize = 18.sp,
                                    modifier = Modifier.testTag("empty_pantry_add_button")
                                )
                                com.example.ui.components.PostItButton(
                                    text = "Recibir de otro teléfono",
                                    icon = Icons.Default.Sync,
                                    onClick = { showShareDialog = true },
                                    backgroundColor = com.example.ui.theme.PostItMint,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                PantryItemCard(
                                    item = item,
                                    onAdjustStock = { delta ->
                                        viewModel.adjustPantryStock(item, delta)
                                    },
                                    onEdit = { editingItem = item },
                                    onDelete = { viewModel.deletePantryItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        PantryFormDialog(
            initialItem = editingItem,
            onDismiss = { editingItem = null },
            onSave = { updated -> viewModel.savePantryItem(updated) }
        )
    }

    if (showShareDialog) {
        val currentPantry = when (val s = pantryUiState) {
            is UiState.Success -> s.data
            else -> emptyList()
        }
        ShareInventoryDialog(
            pantryItems = currentPantry,
            onImportConfirmed = { imported, mergeMode ->
                viewModel.importInventory(imported, mergeMode)
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
