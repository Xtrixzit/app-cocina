package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.components.PostItButton
import com.example.ui.components.PostItFilterChip
import com.example.ui.components.ShoppingItemCard
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.util.LocalizedLabels
import com.example.util.SampleContentLocalizer
import com.example.util.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: MainViewModel,
    pantryItems: List<PantryItem>
) {
    val context = LocalContext.current
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val shoppingUiState by viewModel.shoppingState.collectAsState()

    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("") }
    var newItemUnit by remember { mutableStateOf("unidades") }
    var markAsAvailable by remember { mutableStateOf(true) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    val commonUnits = UnitConverter.allShoppingUnits

    val onAddItem = {
        if (newItemName.isNotBlank()) {
            val qty = UnitConverter.parseNonNegativeDecimal(newItemQty) ?: 0.0
            viewModel.saveShoppingItem(
                ShoppingItem(
                    name = newItemName.trim(),
                    quantity = qty,
                    unit = newItemUnit.trim(),
                    isPurchased = markAsAvailable
                )
            )
            newItemName = ""
            newItemQty = ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .notebookPaperBackground()
                .imePadding()
                .testTag("shopping_lazy_list"),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "shopping_header_quick_add") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_header),
                            fontFamily = CaveatFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
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
                            OutlinedTextField(
                                value = newItemName,
                                onValueChange = { newItemName = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.shopping_name_hint),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 17.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_add_shopping_name"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newItemQty,
                                    onValueChange = { newItemQty = it },
                                    label = { Text(stringResource(R.string.shopping_qty_label)) },
                                    placeholder = { Text("0") },
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
                                        value = LocalizedLabels.unit(context, newItemUnit),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.recipe_form_unit_label)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                            .testTag("quick_add_shopping_unit"),
                                        singleLine = true
                                    )
                                    ExposedDropdownMenu(
                                        expanded = unitMenuExpanded,
                                        onDismissRequest = { unitMenuExpanded = false }
                                    ) {
                                        commonUnits.forEach { u ->
                                            DropdownMenuItem(
                                                text = { Text(LocalizedLabels.unit(context, u)) },
                                                onClick = {
                                                    newItemUnit = u
                                                    unitMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                PostItButton(
                                    text = stringResource(R.string.action_add_short),
                                    onClick = onAddItem,
                                    backgroundColor = PostItPeach,
                                    rotationDegrees = -1.5f,
                                    fontSize = 18.sp,
                                    modifier = Modifier.testTag("quick_add_shopping_button")
                                )
                            }

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
                                    text = stringResource(R.string.shopping_available_toggle),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            when (val state = shoppingUiState) {
                is UiState.Loading -> {
                    item(key = "shopping_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item(key = "shopping_error") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sketchedBorder(color = MaterialTheme.colorScheme.error, strokeWidthDp = 2f)
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.shopping_load_error),
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
                                        modifier = Modifier.testTag("reset_db_shopping")
                                    )
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val allItems = state.data
                    val purchasedItems = allItems.filter { it.isPurchased }
                    val pendingItems = allItems.filter { !it.isPurchased }

                    item(key = "shopping_filter_tabs") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PostItFilterChip(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = stringResource(R.string.shopping_tab_all, allItems.size),
                                selectedColor = PostItSky
                            )
                            PostItFilterChip(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = stringResource(R.string.shopping_tab_avail, purchasedItems.size),
                                selectedColor = PostItMint
                            )
                            PostItFilterChip(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = stringResource(R.string.shopping_tab_buy, pendingItems.size),
                                selectedColor = PostItYellow
                            )
                        }
                    }

                    val activeList = when (selectedTab) {
                        1 -> purchasedItems
                        2 -> pendingItems
                        else -> allItems
                    }

                    if (activeList.isEmpty()) {
                        item(key = "shopping_empty_state") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
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
                                    text = when (selectedTab) {
                                        2 -> stringResource(R.string.shopping_empty_buy)
                                        1 -> stringResource(R.string.shopping_empty_avail)
                                        else -> stringResource(R.string.shopping_empty_all)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.shopping_empty_tip),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val totalEstCost = allItems.sumOf { item ->
                            val match = pantryItems.find { SampleContentLocalizer.sameItemName(it.name, item.name) }
                            val rate = UnitConverter.unitCostInTargetUnit(match, item.unit)
                            if (rate > 0.0) rate * item.quantity else 0.0
                        }
                        val pendingEstCost = pendingItems.sumOf { item ->
                            val match = pantryItems.find { SampleContentLocalizer.sameItemName(it.name, item.name) }
                            val rate = UnitConverter.unitCostInTargetUnit(match, item.unit)
                            if (rate > 0.0) rate * item.quantity else 0.0
                        }
                        if (totalEstCost > 0.0) {
                            item(key = "shopping_est_total") {
                                Text(
                                    text = stringResource(
                                        R.string.shopping_est_total_label,
                                        AppSettingsPreferences.formatMoney(totalEstCost, currency),
                                        AppSettingsPreferences.formatMoney(pendingEstCost, currency)
                                    ),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .testTag("shopping_est_total_summary")
                                )
                            }
                        }

                        if (purchasedItems.isNotEmpty() && selectedTab != 2) {
                            item(key = "shopping_clear_bar") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.shopping_tracking_count, allItems.size),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    PostItButton(
                                        text = stringResource(R.string.shopping_clear_checked),
                                        icon = Icons.Default.DeleteSweep,
                                        onClick = { viewModel.clearPurchasedItems(purchasedItems) },
                                        backgroundColor = PostItPink,
                                        fontSize = 16.sp,
                                        rotationDegrees = 1f,
                                        modifier = Modifier.testTag("clear_purchased_button")
                                    )
                                }
                            }
                        }

                        items(activeList, key = { it.id }) { item ->
                            val matchingPantry = pantryItems.find {
                                SampleContentLocalizer.sameItemName(it.name, item.name)
                            }
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ShoppingItemCard(
                                    item = item,
                                    pantryItem = matchingPantry,
                                    currency = currency,
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
