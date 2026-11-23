package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.PostItButton
import com.example.ui.components.PostItFilterChip
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockRed
import com.example.util.ImageUtils
import com.example.util.LocalizedLabels
import com.example.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    viewModel: MainViewModel,
    recipes: List<Recipe>,
    pantryItems: List<PantryItem>,
    initialSelectedRecipe: Recipe? = null
) {
    val context = LocalContext.current
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val marginPercent by AppSettingsPreferences.observeMarginPercent(context).collectAsStateWithLifecycle()
    val logsUiState by viewModel.productionLogsState.collectAsState()

    var selectedRecipe by remember { mutableStateOf<Recipe?>(initialSelectedRecipe) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }

    var calculationMode by remember { mutableIntStateOf(0) }
    var targetServings by remember { mutableIntStateOf(selectedRecipe?.servings ?: 4) }
    var selectedIngredientIndex by remember { mutableIntStateOf(0) }
    var ingredientAvailableAmountText by remember { mutableStateOf("") }
    var batchCount by remember { mutableIntStateOf(1) }

    var showDeductConfirmDialog by remember { mutableStateOf(false) }

    val categories = listOf("Todas", "Desayunos", "Almuerzos", "Cenas", "Postres", "Snacks", "Bebidas", "Sopas")

    BackHandler(enabled = selectedRecipe != null) {
        selectedRecipe = null
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
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.prod_header),
                    fontFamily = CaveatFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (selectedRecipe == null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.prod_select_prompt),
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.prod_search_hint), fontFamily = PatrickHandFontFamily, fontSize = 16.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), strokeWidthDp = 1.2f)
                                .testTag("production_search_recipe")
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(categories) { cat ->
                            PostItFilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                text = LocalizedLabels.recipeCategory(context, cat),
                                selectedColor = PostItYellow
                            )
                        }
                    }

                    val filteredRecipes = recipes.filter { r ->
                        val matchesCat = selectedCategory == "Todas" || r.category.equals(selectedCategory, ignoreCase = true)
                        val matchesQuery = searchQuery.isBlank() || r.title.contains(searchQuery, ignoreCase = true)
                        matchesCat && matchesQuery
                    }

                    val recentLogs = when (val s = logsUiState) {
                        is UiState.Success -> s.data
                        else -> emptyList()
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredRecipes.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.prod_no_recipes),
                                        fontFamily = CaveatFontFamily,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.prod_no_recipes_tip),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(filteredRecipes, key = { it.id }) { recipe ->
                                val selectThisRecipe = {
                                    selectedRecipe = recipe
                                    targetServings = recipe.servings
                                    batchCount = 1
                                    selectedIngredientIndex = 0
                                    ingredientAvailableAmountText = if (recipe.ingredients.isNotEmpty()) {
                                        recipe.ingredients[0].amount.toString()
                                    } else ""
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), strokeWidthDp = 1.6f)
                                        .clickable(onClick = selectThisRecipe)
                                        .testTag("production_recipe_item_${recipe.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val listPhotoUri = recipe.imageUrl.ifBlank {
                                            ImageUtils.defaultUriForCategory(recipe.category)
                                        }
                                        AsyncImage(
                                            model = ImageUtils.toImageModel(listPhotoUri),
                                            contentDescription = recipe.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = recipe.title,
                                                fontFamily = CaveatFontFamily,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.prod_recipe_meta,
                                                    LocalizedLabels.recipeCategory(context, recipe.category),
                                                    recipe.servings,
                                                    recipe.ingredients.size
                                                ),
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        PostItButton(
                                            text = stringResource(R.string.action_calculate),
                                            icon = Icons.Default.Calculate,
                                            onClick = selectThisRecipe,
                                            backgroundColor = PostItMint,
                                            fontSize = 16.sp,
                                            rotationDegrees = -1f
                                        )
                                    }
                                }
                            }
                        }

                        // Production History Section
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(R.string.prod_history_title, recentLogs.size),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        if (recentLogs.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.prod_history_empty),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(recentLogs.take(15), key = { it.id.ifBlank { "${it.recipeTitle}_${it.createdAt}" } }) { log ->
                                val logCurrency = CurrencyOption.fromCode(log.currencyCode)
                                val totalCostFmt = AppSettingsPreferences.formatMoney(log.totalCost, logCurrency)
                                val suggestedFmt = AppSettingsPreferences.formatMoney(log.suggestedPrice, logCurrency)
                                val dateStr = log.createdAt?.toDate()?.let { d ->
                                    SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(d)
                                } ?: ""

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), strokeWidthDp = 1.2f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.recipeTitle,
                                                fontFamily = CaveatFontFamily,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.prod_history_row,
                                                    log.targetServings,
                                                    totalCostFmt,
                                                    suggestedFmt
                                                ),
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (dateStr.isNotBlank()) {
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val recipe = selectedRecipe!!

                val scaleFactor: Double = when (calculationMode) {
                    0 -> {
                        val base = recipe.servings.coerceAtLeast(1)
                        targetServings.toDouble() / base
                    }
                    1 -> {
                        val ing = recipe.ingredients.getOrNull(selectedIngredientIndex)
                        val avail = ingredientAvailableAmountText.toDoubleOrNull() ?: 0.0
                        if (ing != null && ing.amount > 0.0 && avail > 0.0) {
                            avail / ing.amount
                        } else 1.0
                    }
                    2 -> batchCount.toDouble().coerceAtLeast(1.0)
                    else -> 1.0
                }

                val calculatedTotalUnits = Math.round(recipe.servings * scaleFactor * 10.0) / 10.0

                data class ProductionRow(
                    val ingredient: RecipeIngredient,
                    val baseAmount: Double,
                    val requiredAmount: Double,
                    val pantryStock: Double,
                    val shortage: Double,
                    val unitCost: Double,
                    val lineCost: Double
                )

                var totalProductionCost = 0.0
                var missingIngredientsCount = 0

                val productionRows = recipe.ingredients.map { ing ->
                    val required = Math.round(ing.amount * scaleFactor * 100.0) / 100.0
                    val pantryItem = pantryItems.find { it.name.trim().equals(ing.name.trim(), ignoreCase = true) }
                    val stockConverted = UnitConverter.availableInTargetUnit(pantryItem, ing.unit)
                    val shortage = (required - stockConverted).coerceAtLeast(0.0)

                    if (shortage > 0.001) {
                        missingIngredientsCount++
                    }

                    val pantryUnitCost = UnitConverter.unitCostInTargetUnit(pantryItem, ing.unit)
                    val effectiveUnitCost = when {
                        pantryUnitCost > 0.0 -> pantryUnitCost
                        ing.manualUnitCost > 0.0 -> ing.manualUnitCost
                        else -> 0.0
                    }

                    val lineCost = required * effectiveUnitCost
                    totalProductionCost += lineCost

                    ProductionRow(
                        ingredient = ing,
                        baseAmount = ing.amount,
                        requiredAmount = required,
                        pantryStock = stockConverted,
                        shortage = shortage,
                        unitCost = effectiveUnitCost,
                        lineCost = lineCost
                    )
                }

                val costPerUnit = if (calculatedTotalUnits > 0) totalProductionCost / calculatedTotalUnits else 0.0
                val suggestedSalePrice = AppSettingsPreferences.suggestedSellingPrice(totalProductionCost, marginPercent)
                val estimatedProfit = AppSettingsPreferences.estimatedProfit(totalProductionCost, marginPercent)
                val allInStock = missingIngredientsCount == 0

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), strokeWidthDp = 1.6f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val headerPhotoUri = recipe.imageUrl.ifBlank {
                                        ImageUtils.defaultUriForCategory(recipe.category)
                                    }
                                    AsyncImage(
                                        model = ImageUtils.toImageModel(headerPhotoUri),
                                        contentDescription = recipe.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = recipe.title,
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.prod_base_factor,
                                                recipe.servings,
                                                String.format(Locale.US, "%.2f", scaleFactor)
                                            ),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                PostItButton(
                                    text = stringResource(R.string.action_change),
                                    icon = Icons.Default.Close,
                                    onClick = { selectedRecipe = null },
                                    backgroundColor = PostItPeach,
                                    fontSize = 16.sp,
                                    modifier = Modifier.testTag("production_change_recipe")
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), strokeWidthDp = 1.8f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = stringResource(R.string.prod_goal_title),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = calculationMode == 0,
                                        onClick = { calculationMode = 0 },
                                        label = { Text(stringResource(R.string.prod_mode_servings)) }
                                    )
                                    FilterChip(
                                        selected = calculationMode == 1,
                                        onClick = { calculationMode = 1 },
                                        label = { Text(stringResource(R.string.prod_mode_ingredient)) }
                                    )
                                    FilterChip(
                                        selected = calculationMode == 2,
                                        onClick = { calculationMode = 2 },
                                        label = { Text(stringResource(R.string.prod_mode_batches)) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                when (calculationMode) {
                                    0 -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.prod_servings_to_make),
                                                    fontFamily = PatrickHandFontFamily,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = stringResource(R.string.prod_servings_count, targetServings),
                                                    fontFamily = CaveatFontFamily,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                FilledTonalIconButton(
                                                    onClick = { targetServings = (targetServings - 5).coerceAtLeast(1) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("-5", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                FilledTonalIconButton(
                                                    onClick = { targetServings = (targetServings - 1).coerceAtLeast(1) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.action_less))
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                FilledTonalIconButton(
                                                    onClick = { targetServings = (targetServings + 1).coerceAtMost(500) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_more))
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                FilledTonalIconButton(
                                                    onClick = { targetServings = (targetServings + 5).coerceAtMost(500) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("+5", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = stringResource(R.string.prod_quick_multipliers),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            val multipliers = listOf(2, 3, 5, 10, 20, 50, 100)
                                            items(multipliers) { m ->
                                                val units = recipe.servings * m
                                                PostItFilterChip(
                                                    selected = targetServings == units,
                                                    onClick = { targetServings = units },
                                                    text = "${m}x ($units)",
                                                    selectedColor = PostItYellow
                                                )
                                            }
                                        }
                                    }
                                    1 -> {
                                        var ingMenuExpanded by remember { mutableStateOf(false) }
                                        val currentIng = recipe.ingredients.getOrNull(selectedIngredientIndex)
                                        val currentIngUnitLabel = currentIng?.let { LocalizedLabels.unit(context, it.unit) } ?: ""

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = stringResource(R.string.prod_key_ing_tip),
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            ExposedDropdownMenuBox(
                                                expanded = ingMenuExpanded,
                                                onExpandedChange = { ingMenuExpanded = it },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = currentIng?.name ?: stringResource(R.string.prod_key_ing_select),
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text(stringResource(R.string.prod_key_ing_label)) },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ingMenuExpanded) },
                                                    modifier = Modifier
                                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                                        .fillMaxWidth()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = ingMenuExpanded,
                                                    onDismissRequest = { ingMenuExpanded = false }
                                                ) {
                                                    recipe.ingredients.forEachIndexed { i, ing ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text("${ing.name} (${formatProdQty(ing.amount)} ${LocalizedLabels.unit(context, ing.unit)})")
                                                            },
                                                            onClick = {
                                                                selectedIngredientIndex = i
                                                                ingredientAvailableAmountText = ing.amount.toString()
                                                                ingMenuExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = ingredientAvailableAmountText,
                                                    onValueChange = { ingredientAvailableAmountText = it },
                                                    label = { Text(stringResource(R.string.prod_key_ing_avail, currentIngUnitLabel)) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )

                                                Text(
                                                    text = stringResource(R.string.prod_yields_approx, calculatedTotalUnits.toInt()),
                                                    fontFamily = CaveatFontFamily,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    2 -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.prod_batches_title),
                                                    fontFamily = PatrickHandFontFamily,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.prod_batches_summary,
                                                        batchCount,
                                                        recipe.servings * batchCount
                                                    ),
                                                    fontFamily = CaveatFontFamily,
                                                    fontSize = 26.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                FilledTonalIconButton(
                                                    onClick = { batchCount = (batchCount - 1).coerceAtLeast(1) },
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.action_less))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                FilledTonalIconButton(
                                                    onClick = { batchCount = (batchCount + 1).coerceAtMost(50) },
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_more))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.prod_est_cost_title),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = AppSettingsPreferences.formatMoney(totalProductionCost, currency),
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (costPerUnit > 0.0) {
                                            Text(
                                                text = stringResource(
                                                    R.string.prod_cost_per_portion,
                                                    AppSettingsPreferences.formatMoney(costPerUnit, currency)
                                                ),
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (totalProductionCost > 0.0) {
                                            Text(
                                                text = stringResource(
                                                    R.string.prod_suggested_price_line,
                                                    marginPercent,
                                                    AppSettingsPreferences.formatMoney(suggestedSalePrice, currency),
                                                    AppSettingsPreferences.formatMoney(estimatedProfit, currency)
                                                ),
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StockGreen
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = (if (allInStock) StockGreen else StockRed).copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (allInStock) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (allInStock) StockGreen else StockRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (allInStock) {
                                                    stringResource(R.string.prod_stock_full)
                                                } else {
                                                    stringResource(R.string.prod_stock_missing, missingIngredientsCount)
                                                },
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (allInStock) StockGreen else StockRed
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val targetUnitsLabel = stringResource(R.string.prod_servings_count, calculatedTotalUnits.toInt())
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!allInStock) {
                                        PostItButton(
                                            text = stringResource(R.string.prod_add_shortage),
                                            icon = Icons.Default.ShoppingCart,
                                            onClick = {
                                                viewModel.addProductionShortageToShoppingList(
                                                    recipe = recipe,
                                                    scaleFactor = scaleFactor,
                                                    currentPantry = pantryItems,
                                                    targetUnitsLabel = targetUnitsLabel
                                                )
                                            },
                                            backgroundColor = PostItPeach,
                                            fontSize = 16.sp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("production_add_shortage_to_shopping")
                                        )
                                    }

                                    PostItButton(
                                        text = stringResource(R.string.prod_register_btn),
                                        icon = Icons.Default.PrecisionManufacturing,
                                        onClick = { showDeductConfirmDialog = true },
                                        backgroundColor = PostItMint,
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("production_register_button")
                                    )
                                }
                            }
                        }
                    }

                    item {
                        val shareTitle = stringResource(R.string.prod_share_title)
                        val shareRecipe = stringResource(R.string.prod_share_recipe, recipe.title)
                        val shareCalc = stringResource(
                            R.string.prod_share_calc,
                            calculatedTotalUnits.toString(),
                            String.format(Locale.US, "%.2f", scaleFactor)
                        )
                        val shareNeeded = stringResource(R.string.prod_share_needed)
                        val shareOk = stringResource(R.string.prod_share_ok)
                        val shareChooser = stringResource(R.string.prod_share_chooser)
                        val shareCostLine = stringResource(
                            R.string.prod_share_cost,
                            AppSettingsPreferences.formatMoney(totalProductionCost, currency)
                        )
                        val sharePriceLine = stringResource(
                            R.string.prod_share_price,
                            marginPercent,
                            AppSettingsPreferences.formatMoney(suggestedSalePrice, currency)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.prod_ingredients_header, productionRows.size),
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            IconButton(
                                onClick = {
                                    val textBuilder = StringBuilder()
                                    textBuilder.append("$shareTitle\n")
                                    textBuilder.append("$shareRecipe\n")
                                    textBuilder.append("$shareCalc\n\n")
                                    textBuilder.append("$shareNeeded\n")
                                    productionRows.forEach { row ->
                                        val reqStr = formatProdQty(row.requiredAmount)
                                        val unitLabel = LocalizedLabels.unit(context, row.ingredient.unit)
                                        val statusStr = if (row.shortage > 0.001) {
                                            context.getString(R.string.prod_share_buy, formatProdQty(row.shortage), unitLabel)
                                        } else {
                                            shareOk
                                        }
                                        textBuilder.append("• ${row.ingredient.name}: $reqStr $unitLabel$statusStr\n")
                                    }
                                    if (totalProductionCost > 0.0) {
                                        textBuilder.append("\n$shareCostLine\n$sharePriceLine")
                                    }

                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, shareChooser))
                                },
                                modifier = Modifier.testTag("production_share_sheet")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = shareChooser, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    items(productionRows) { row ->
                        val reqFormatted = formatProdQty(row.requiredAmount)
                        val baseFormatted = formatProdQty(row.baseAmount)
                        val stockFormatted = formatProdQty(row.pantryStock)
                        val shortageFormatted = formatProdQty(row.shortage)
                        val unitLabel = LocalizedLabels.unit(context, row.ingredient.unit)
                        val hasEnough = row.shortage <= 0.001

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(
                                    color = if (hasEnough) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else StockRed.copy(alpha = 0.7f),
                                    strokeWidthDp = if (hasEnough) 1.2f else 1.8f
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.ingredient.name,
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = stringResource(R.string.prod_ing_base, baseFormatted, unitLabel),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$reqFormatted $unitLabel",
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (row.lineCost > 0.0) {
                                            Text(
                                                text = stringResource(
                                                    R.string.prod_ing_cost,
                                                    AppSettingsPreferences.formatMoney(row.lineCost, currency)
                                                ),
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.prod_ing_in_pantry, "$stockFormatted $unitLabel"),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = (if (hasEnough) StockGreen else StockRed).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (hasEnough) {
                                                stringResource(R.string.prod_ing_ok)
                                            } else {
                                                stringResource(R.string.prod_ing_short, shortageFormatted, unitLabel)
                                            },
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasEnough) StockGreen else StockRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDeductConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeductConfirmDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.prod_confirm_title),
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.prod_confirm_body, calculatedTotalUnits.toString(), recipe.title),
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 16.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deductPantryStockForProduction(
                                        recipe = recipe,
                                        scaleFactor = scaleFactor,
                                        currentPantry = pantryItems,
                                        targetServings = calculatedTotalUnits.toInt().coerceAtLeast(1)
                                    )
                                    showDeductConfirmDialog = false
                                },
                                modifier = Modifier.testTag("production_confirm_deduct")
                            ) {
                                Text(stringResource(R.string.prod_confirm_btn))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeductConfirmDialog = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatProdQty(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
