package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.ShoppingItem
import com.example.settings.AppSettingsPreferences
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.PantryItemCard
import com.example.ui.components.PostItButton
import com.example.ui.components.PostItFilterChip
import com.example.ui.components.ShareInventoryDialog
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockRed
import com.example.ui.theme.StockYellow
import com.example.util.LocalizedLabels

@Composable
fun PantryScreen(
    viewModel: MainViewModel,
    onAddNewItem: () -> Unit
) {
    val context = LocalContext.current
    val pantryUiState by viewModel.pantryState.collectAsState()
    val searchQuery by viewModel.pantrySearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedPantryCategory.collectAsState()
    val onlyLowStock by viewModel.filterOnlyLowStock.collectAsState()
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()

    var editingItem by remember { mutableStateOf<PantryItem?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

    val categories = listOf("Todas", "Despensa", "Verduras", "Frutas", "Carnes", "Lácteos", "Granos", "Especias", "Bebidas", "Congelados")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PostItButton(
                text = stringResource(R.string.pantry_fab_add),
                icon = Icons.Default.Add,
                onClick = onAddNewItem,
                backgroundColor = PostItPeach,
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
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.pantry_header),
                    fontFamily = CaveatFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

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
                            stringResource(R.string.pantry_search_hint),
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 17.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.pantrySearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear), tint = MaterialTheme.colorScheme.onSurface)
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

                PostItButton(
                    text = stringResource(R.string.pantry_sync_btn),
                    icon = Icons.Default.Sync,
                    onClick = { showShareDialog = true },
                    backgroundColor = PostItSky,
                    rotationDegrees = 1f,
                    fontSize = 18.sp,
                    modifier = Modifier.testTag("share_inventory_button")
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    PostItFilterChip(
                        selected = onlyLowStock,
                        onClick = { viewModel.filterOnlyLowStock.value = !onlyLowStock },
                        text = stringResource(R.string.pantry_filter_low),
                        selectedColor = PostItPeach,
                        modifier = Modifier.testTag("filter_low_stock")
                    )
                }

                items(categories) { cat ->
                    val color = when (cat) {
                        "Todas" -> PostItYellow
                        "Despensa", "Verduras" -> PostItMint
                        "Carnes" -> PostItPeach
                        "Lácteos" -> PostItSky
                        else -> PostItYellow
                    }
                    PostItFilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.selectedPantryCategory.value = cat },
                        text = LocalizedLabels.pantryCategory(context, cat),
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = MaterialTheme.colorScheme.error, strokeWidthDp = 2f)
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.pantry_load_error),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                PostItButton(
                                    text = stringResource(R.string.action_retry_sync),
                                    onClick = { viewModel.retryCloudSync() },
                                    backgroundColor = PostItYellow,
                                    fontSize = 18.sp,
                                    modifier = Modifier.testTag("reset_db_pantry")
                                )
                            }
                        }
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
                                val restockSource = stringResource(R.string.pantry_restock_source)
                                val totalInventoryValue = allItems.sumOf { it.quantity * it.unitCost }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        MetricPill(label = stringResource(R.string.pantry_metric_total), value = "${allItems.size}", color = MaterialTheme.colorScheme.primary)
                                        MetricPill(label = stringResource(R.string.pantry_metric_low), value = "${lowStockItems.size - outOfStockItems.size}", color = StockYellow)
                                        MetricPill(label = stringResource(R.string.pantry_metric_out), value = "${outOfStockItems.size}", color = StockRed)
                                        if (totalInventoryValue > 0.0) {
                                            MetricPill(
                                                label = stringResource(R.string.pantry_metric_value),
                                                value = AppSettingsPreferences.formatMoney(totalInventoryValue, currency),
                                                color = StockGreen
                                            )
                                        }
                                    }

                                    if (lowStockItems.isNotEmpty()) {
                                        PostItButton(
                                            text = stringResource(R.string.pantry_restock_btn, lowStockItems.size),
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
                                                            quantity = Math.round(needed * 1000.0) / 1000.0,
                                                            unit = item.unit,
                                                            isPurchased = false,
                                                            recipeSource = restockSource
                                                        )
                                                    )
                                                }
                                            },
                                            backgroundColor = PostItPeach,
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
                                text = when {
                                    onlyLowStock -> stringResource(R.string.pantry_empty_low_title)
                                    searchQuery.isNotBlank() -> stringResource(R.string.pantry_empty_search_title)
                                    else -> stringResource(R.string.pantry_empty_title)
                                },
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    onlyLowStock -> stringResource(R.string.pantry_empty_low_sub)
                                    searchQuery.isNotBlank() -> stringResource(R.string.pantry_empty_search_sub)
                                    else -> stringResource(R.string.pantry_empty_sub)
                                },
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PostItButton(
                                    text = stringResource(R.string.pantry_add_btn),
                                    onClick = onAddNewItem,
                                    backgroundColor = PostItYellow,
                                    fontSize = 18.sp,
                                    modifier = Modifier.testTag("empty_pantry_add_button")
                                )
                                PostItButton(
                                    text = stringResource(R.string.pantry_receive_btn),
                                    icon = Icons.Default.Sync,
                                    onClick = { showShareDialog = true },
                                    backgroundColor = PostItMint,
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
                                    currency = currency,
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
            onExportFullBackup = { cb -> viewModel.exportFullBackupJson(cb) },
            onImportFullBackup = { rawJson, replaceExisting ->
                viewModel.importFullBackupJson(rawJson, replaceExisting)
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color
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
