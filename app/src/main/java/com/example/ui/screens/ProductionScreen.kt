package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
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
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockRed
import com.example.ui.theme.StockYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    viewModel: MainViewModel,
    recipes: List<Recipe>,
    pantryItems: List<PantryItem>,
    initialSelectedRecipe: Recipe? = null
) {
    val context = LocalContext.current

    var selectedRecipe by remember { mutableStateOf<Recipe?>(initialSelectedRecipe) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }

    // Calculation mode:
    // 0 = Por Porciones / Unidades deseadas
    // 1 = Por Ingrediente Clave (escala en base a cuánto ingrediente tienes)
    // 2 = Por Tandas / Lotes
    var calculationMode by remember { mutableIntStateOf(0) }

    // Target quantities
    var targetServings by remember { mutableIntStateOf(selectedRecipe?.servings ?: 4) }
    var selectedIngredientIndex by remember { mutableIntStateOf(0) }
    var ingredientAvailableAmountText by remember { mutableStateOf("") }
    var batchCount by remember { mutableIntStateOf(1) }

    // Dialog state
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
            // Header: Sketched notebook style
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Cálculo de Producción",
                    fontFamily = CaveatFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (selectedRecipe == null) {
                // --- RECIPE SELECTION CATALOG ---
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Selecciona una receta para calcular las cantidades de ingredientes según tu producción:",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Search row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar receta a producir...", fontFamily = PatrickHandFontFamily, fontSize = 16.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
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

                    // Category Filter Chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(categories) { cat ->
                            PostItFilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                text = cat,
                                selectedColor = PostItYellow
                            )
                        }
                    }

                    val filteredRecipes = recipes.filter { r ->
                        val matchesCat = selectedCategory == "Todas" || r.category.equals(selectedCategory, ignoreCase = true)
                        val matchesQuery = searchQuery.isBlank() || r.title.contains(searchQuery, ignoreCase = true)
                        matchesCat && matchesQuery
                    }

                    if (filteredRecipes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No se encontraron recetas",
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Crea recetas en el cuaderno para poder calcular su producción.",
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredRecipes, key = { it.id }) { recipe ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), strokeWidthDp = 1.6f)
                                        .clickable {
                                            selectedRecipe = recipe
                                            targetServings = recipe.servings
                                            batchCount = 1
                                            selectedIngredientIndex = 0
                                            ingredientAvailableAmountText = if (recipe.ingredients.isNotEmpty()) {
                                                recipe.ingredients[0].amount.toString()
                                            } else ""
                                        }
                                        .testTag("production_recipe_item_${recipe.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Photo / Icon
                                        if (recipe.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = recipe.imageUrl,
                                                contentDescription = recipe.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(60.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                                }
                                            }
                                        }

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
                                                text = "${recipe.category} • Base: ${recipe.servings} porciones • ${recipe.ingredients.size} ingredientes",
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        PostItButton(
                                            text = "Calcular",
                                            icon = Icons.Default.Calculate,
                                            onClick = {
                                                selectedRecipe = recipe
                                                targetServings = recipe.servings
                                                batchCount = 1
                                                selectedIngredientIndex = 0
                                                ingredientAvailableAmountText = if (recipe.ingredients.isNotEmpty()) {
                                                    recipe.ingredients[0].amount.toString()
                                                } else ""
                                            },
                                            backgroundColor = PostItMint,
                                            fontSize = 16.sp,
                                            rotationDegrees = -1f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- ACTIVE PRODUCTION CALCULATOR VIEW ---
                val recipe = selectedRecipe!!

                // Determine scale factor based on active calculation mode
                val scaleFactor: Double = when (calculationMode) {
                    0 -> {
                        // Por Porciones / Unidades
                        val base = recipe.servings.coerceAtLeast(1)
                        targetServings.toDouble() / base
                    }
                    1 -> {
                        // Por Ingrediente Clave
                        val ing = recipe.ingredients.getOrNull(selectedIngredientIndex)
                        val avail = ingredientAvailableAmountText.toDoubleOrNull() ?: 0.0
                        if (ing != null && ing.amount > 0.0 && avail > 0.0) {
                            avail / ing.amount
                        } else 1.0
                    }
                    2 -> {
                        // Por Tandas / Lotes
                        batchCount.toDouble().coerceAtLeast(1.0)
                    }
                    else -> 1.0
                }

                val calculatedTotalUnits = Math.round(recipe.servings * scaleFactor * 10.0) / 10.0

                // Calculate ingredient breakdown, shortages, and costs
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
                    val stock = pantryItem?.quantity ?: 0.0
                    val shortage = (required - stock).coerceAtLeast(0.0)

                    if (shortage > 0.001) {
                        missingIngredientsCount++
                    }

                    val effectiveUnitCost = when {
                        pantryItem != null && pantryItem.unitCost > 0.0 -> pantryItem.unitCost
                        ing.manualUnitCost > 0.0 -> ing.manualUnitCost
                        else -> 0.0
                    }

                    val lineCost = required * effectiveUnitCost
                    totalProductionCost += lineCost

                    ProductionRow(
                        ingredient = ing,
                        baseAmount = ing.amount,
                        requiredAmount = required,
                        pantryStock = stock,
                        shortage = shortage,
                        unitCost = effectiveUnitCost,
                        lineCost = lineCost
                    )
                }

                val costPerUnit = if (calculatedTotalUnits > 0) totalProductionCost / calculatedTotalUnits else 0.0
                val allInStock = missingIngredientsCount == 0

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selected Recipe Top Bar with photo and switch button
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
                                    if (recipe.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = recipe.imageUrl,
                                            contentDescription = recipe.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }

                                    Column {
                                        Text(
                                            text = recipe.title,
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Base: ${recipe.servings} porciones • Factor: ${"%.2f".format(scaleFactor)}x",
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                PostItButton(
                                    text = "Cambiar",
                                    icon = Icons.Default.Close,
                                    onClick = { selectedRecipe = null },
                                    backgroundColor = PostItPeach,
                                    fontSize = 16.sp,
                                    modifier = Modifier.testTag("production_change_recipe")
                                )
                            }
                        }
                    }

                    // --- PRODUCTION INPUT CONTROLS ---
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), strokeWidthDp = 1.8f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Meta de Producción",
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Mode selection chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = calculationMode == 0,
                                        onClick = { calculationMode = 0 },
                                        label = { Text("Por Porciones") }
                                    )
                                    FilterChip(
                                        selected = calculationMode == 1,
                                        onClick = { calculationMode = 1 },
                                        label = { Text("Por Ingrediente") }
                                    )
                                    FilterChip(
                                        selected = calculationMode == 2,
                                        onClick = { calculationMode = 2 },
                                        label = { Text("Por Tandas") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                when (calculationMode) {
                                    0 -> {
                                        // Mode 0: Por Porciones / Unidades
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Porciones a Producir:",
                                                    fontFamily = PatrickHandFontFamily,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "$targetServings porciones",
                                                    fontFamily = CaveatFontFamily,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Steppers
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
                                                    Icon(Icons.Default.Remove, contentDescription = "Menos 1")
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                FilledTonalIconButton(
                                                    onClick = { targetServings = (targetServings + 1).coerceAtMost(500) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Más 1")
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

                                        // Quick multiplier presets
                                        Text(
                                            text = "Multiplicadores rápidos:",
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
                                        // Mode 1: Por Ingrediente Clave
                                        var ingMenuExpanded by remember { mutableStateOf(false) }
                                        val currentIng = recipe.ingredients.getOrNull(selectedIngredientIndex)

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Ajusta la producción según la cantidad que tienes de un ingrediente:",
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
                                                    value = currentIng?.name ?: "Selecciona ingrediente",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Ingrediente clave de referencia") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ingMenuExpanded) },
                                                    modifier = Modifier
                                                        .menuAnchor()
                                                        .fillMaxWidth()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = ingMenuExpanded,
                                                    onDismissRequest = { ingMenuExpanded = false }
                                                ) {
                                                    recipe.ingredients.forEachIndexed { i, ing ->
                                                        DropdownMenuItem(
                                                            text = { Text("${ing.name} (Base: ${ing.amount} ${ing.unit})") },
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
                                                    label = { Text("Cantidad disponible (${currentIng?.unit ?: ""})") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )

                                                Text(
                                                    text = "Rinde ~${calculatedTotalUnits.toInt()} porciones",
                                                    fontFamily = CaveatFontFamily,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    2 -> {
                                        // Mode 2: Por Tandas / Lotes
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Tandas a Hornear/Cocinar:",
                                                    fontFamily = PatrickHandFontFamily,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "$batchCount tanda(s) = ${(recipe.servings * batchCount)} porciones",
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
                                                    Icon(Icons.Default.Remove, contentDescription = "Menos tandas")
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                FilledTonalIconButton(
                                                    onClick = { batchCount = (batchCount + 1).coerceAtMost(50) },
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Más tandas")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- PRODUCTION METRICS SUMMARY CARD ---
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
                                    Column {
                                        Text(
                                            text = "Costo Estimado de Producción",
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$${"%.2f".format(totalProductionCost)}",
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (costPerUnit > 0.0) {
                                            Text(
                                                text = "($${"%.2f".format(costPerUnit)} por porción)",
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Inventory Sufficiency Badge
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
                                                text = if (allInStock) "100% en Despensa" else "Faltan $missingIngredientsCount ingrediente(s)",
                                                fontFamily = PatrickHandFontFamily,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (allInStock) StockGreen else StockRed
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Production Action Buttons:
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!allInStock) {
                                        PostItButton(
                                            text = "Añadir Faltantes a Check list",
                                            icon = Icons.Default.ShoppingCart,
                                            onClick = {
                                                viewModel.addProductionShortageToShoppingList(
                                                    recipe = recipe,
                                                    scaleFactor = scaleFactor,
                                                    currentPantry = pantryItems,
                                                    targetUnitsLabel = "$calculatedTotalUnits porciones"
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
                                        text = "Registrar Producción",
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

                    // --- SECTION TITLE: DETALLE DE INGREDIENTES ESCALADOS ---
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ingredientes para esta Producción (${productionRows.size})",
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            // Share / Export icon button
                            IconButton(
                                onClick = {
                                    val textBuilder = StringBuilder()
                                    textBuilder.append("📋 HOJA DE PRODUCCIÓN\n")
                                    textBuilder.append("Receta: ${recipe.title}\n")
                                    textBuilder.append("Producción calculada: $calculatedTotalUnits porciones (Factor: ${"%.2f".format(scaleFactor)}x)\n\n")
                                    textBuilder.append("INGREDIENTES NECESARIOS:\n")
                                    productionRows.forEach { row ->
                                        val reqStr = if (row.requiredAmount % 1.0 == 0.0) row.requiredAmount.toInt().toString() else "%.2f".format(row.requiredAmount)
                                        val statusStr = if (row.shortage > 0.001) " (Falta comprar: ${if (row.shortage % 1.0 == 0.0) row.shortage.toInt().toString() else "%.2f".format(row.shortage)} ${row.ingredient.unit})" else " (En stock)"
                                        textBuilder.append("• ${row.ingredient.name}: $reqStr ${row.ingredient.unit}$statusStr\n")
                                    }
                                    if (totalProductionCost > 0.0) {
                                        textBuilder.append("\nCosto estimado de insumos: $${"%.2f".format(totalProductionCost)}")
                                    }

                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir hoja de producción"))
                                },
                                modifier = Modifier.testTag("production_share_sheet")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir hoja de producción", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // --- INGREDIENT ROWS ---
                    items(productionRows) { row ->
                        val reqFormatted = if (row.requiredAmount % 1.0 == 0.0) row.requiredAmount.toInt().toString() else "%.2f".format(row.requiredAmount)
                        val baseFormatted = if (row.baseAmount % 1.0 == 0.0) row.baseAmount.toInt().toString() else "%.1f".format(row.baseAmount)
                        val stockFormatted = if (row.pantryStock % 1.0 == 0.0) row.pantryStock.toInt().toString() else "%.1f".format(row.pantryStock)
                        val shortageFormatted = if (row.shortage % 1.0 == 0.0) row.shortage.toInt().toString() else "%.2f".format(row.shortage)
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
                                            text = "Base original: $baseFormatted ${row.ingredient.unit}",
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Required amount for production
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$reqFormatted ${row.ingredient.unit}",
                                            fontFamily = CaveatFontFamily,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (row.lineCost > 0.0) {
                                            Text(
                                                text = "Costo: $${"%.2f".format(row.lineCost)}",
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

                                // Inventory stock status for this ingredient
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
                                            text = "En despensa: $stockFormatted ${row.ingredient.unit}",
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
                                            text = if (hasEnough) "✓ Suficiente" else "⚠ Falta: $shortageFormatted ${row.ingredient.unit}",
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

                // Confirmation dialog for deducting stock after completing production
                if (showDeductConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeductConfirmDialog = false },
                        title = {
                            Text(
                                text = "¿Registrar Producción Realizada?",
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "Se descontarán automáticamente las cantidades calculadas de los ingredientes presentes en tu Despensa / Inventario para una producción de $calculatedTotalUnits porciones de ${recipe.title}.\n\n¿Deseas continuar?",
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
                                        currentPantry = pantryItems
                                    )
                                    showDeductConfirmDialog = false
                                },
                                modifier = Modifier.testTag("production_confirm_deduct")
                            ) {
                                Text("Sí, Descontar Stock")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeductConfirmDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}
