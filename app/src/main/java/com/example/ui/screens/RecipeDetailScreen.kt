package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.ui.components.CookingTimerDialog
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockRed
import com.example.ui.theme.StockYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    pantryItems: List<PantryItem>,
    targetServings: Int,
    onServingsChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddMissingToShoppingList: () -> Unit,
    onUpdateRecipe: (Recipe) -> Unit = {}
) {
    BackHandler { onBack() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var activeTimerLabel by remember { mutableStateOf("") }
    var editingCostIngredientIndex by remember { mutableStateOf<Int?>(null) }
    var manualCostInputText by remember { mutableStateOf("") }

    val completedSteps = remember { mutableStateListOf<Int>() }

    // Multiplier for proportional ingredient scaling
    val scaleFactor = targetServings.toDouble() / recipe.servings.coerceAtLeast(1)

    // Check shortage and compute ingredient costs
    var missingIngredientsCount = 0
    var totalProductionCost = 0.0

    data class IngredientCostDetail(
        val ingredient: com.example.data.model.RecipeIngredient,
        val scaledAmount: Double,
        val unitCost: Double,
        val lineTotal: Double,
        val sourceLabel: String,
        val isFromPantry: Boolean
    )

    val costBreakdown = recipe.ingredients.map { ing ->
        val needed = ing.amount * scaleFactor
        val inStockItem = pantryItems.find { it.name.trim().equals(ing.name.trim(), ignoreCase = true) }
        val inStock = inStockItem?.quantity ?: 0.0
        if (inStock < needed) {
            missingIngredientsCount++
        }

        val effectiveUnitCost = when {
            inStockItem != null && inStockItem.unitCost > 0.0 -> inStockItem.unitCost
            ing.manualUnitCost > 0.0 -> ing.manualUnitCost
            else -> 0.0
        }
        val lineCost = needed * effectiveUnitCost
        totalProductionCost += lineCost

        val sourceLabel = when {
            inStockItem != null && inStockItem.unitCost > 0.0 -> "Despensa"
            ing.manualUnitCost > 0.0 -> "Manual"
            else -> "Sin costo"
        }

        IngredientCostDetail(
            ingredient = ing,
            scaledAmount = needed,
            unitCost = effectiveUnitCost,
            lineTotal = lineCost,
            sourceLabel = sourceLabel,
            isFromPantry = inStockItem != null && inStockItem.unitCost > 0.0
        )
    }

    val costPerServing = if (targetServings > 0) totalProductionCost / targetServings else 0.0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        recipe.title,
                        maxLines = 1,
                        fontFamily = com.example.ui.theme.CaveatFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        com.example.ui.components.NotebookBackStamp(
                            onClick = onBack,
                            modifier = Modifier.testTag("recipe_detail_back")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Main",
                            fontFamily = com.example.ui.theme.CaveatFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("recipe_detail_edit")) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("recipe_detail_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .notebookPaperBackground()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Final Product Photo Banner (if present)
            if (recipe.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), strokeWidthDp = 1.8f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = "Foto de ${recipe.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            // Category & Difficulty Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = recipe.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Dificultad: ${recipe.difficulty}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Prep & Cook Times with Direct Timer Trigger Buttons
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prep Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Preparación", fontFamily = com.example.ui.theme.CaveatFontFamily, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${recipe.prepTimeMinutes} min", fontFamily = com.example.ui.theme.CaveatFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        if (recipe.prepTimeMinutes > 0) {
                            com.example.ui.components.PostItButton(
                                text = "Temporizador",
                                icon = Icons.Default.Timer,
                                onClick = {
                                    activeTimerMinutes = recipe.prepTimeMinutes
                                    activeTimerLabel = "Prep: ${recipe.title}"
                                },
                                backgroundColor = com.example.ui.theme.PostItYellow,
                                fontSize = 17.sp,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.padding(top = 4.dp).testTag("timer_prep_button")
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                    )

                    // Cook Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cocción", fontFamily = com.example.ui.theme.CaveatFontFamily, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${recipe.cookTimeMinutes} min", fontFamily = com.example.ui.theme.CaveatFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        if (recipe.cookTimeMinutes > 0) {
                            com.example.ui.components.PostItButton(
                                text = "Temporizador",
                                icon = Icons.Default.Timer,
                                onClick = {
                                    activeTimerMinutes = recipe.cookTimeMinutes
                                    activeTimerLabel = "Cocción: ${recipe.title}"
                                },
                                backgroundColor = com.example.ui.theme.PostItMint,
                                fontSize = 17.sp,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.padding(top = 4.dp).testTag("timer_cook_button")
                            )
                        }
                    }
                }
            }

            // --- CALCULADORA DE PORCIONES (DYNAMIC SERVING SCALER) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Calculadora de Porciones",
                                fontFamily = com.example.ui.theme.CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Base de la receta: ${recipe.servings} porciones",
                                fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Stepper (- / +)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { onServingsChanged(targetServings - 1) },
                                enabled = targetServings > 1,
                                modifier = Modifier.testTag("servings_decrease")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos porciones")
                            }

                            AnimatedContent(
                                targetState = targetServings,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                                    } else {
                                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                    }
                                },
                                label = "servingsCounter"
                            ) { servings ->
                                Text(
                                    text = "$servings",
                                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp)
                                        .testTag("target_servings_value")
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onServingsChanged(targetServings + 1) },
                                modifier = Modifier.testTag("servings_increase")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más porciones")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Multiplier Post-It Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Base (${recipe.servings})" to recipe.servings,
                            "Doble (${recipe.servings * 2})" to (recipe.servings * 2),
                            "Triple (${recipe.servings * 3})" to (recipe.servings * 3),
                            "Mitad (${(recipe.servings / 2).coerceAtLeast(1)})" to (recipe.servings / 2).coerceAtLeast(1)
                        ).forEach { (label, value) ->
                            com.example.ui.components.PostItFilterChip(
                                selected = targetServings == value,
                                onClick = { onServingsChanged(value) },
                                text = label,
                                selectedColor = com.example.ui.theme.PostItPeach
                            )
                        }
                    }
                }
            }

            // --- TICKET DE COSTO DE PRODUCCIÓN (PRODUCTION COST TICKET) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Presupuesto y Costos",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cálculo para $targetServings porciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Big Total Display
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Costo Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${"%.2f".format(totalProductionCost)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "($${"%.2f".format(costPerServing)} / porción)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "Desglose por ingrediente (toca uno para asignar/editar costo):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    costBreakdown.forEachIndexed { index, itemCost ->
                        val formattedAmount = if (itemCost.scaledAmount % 1.0 == 0.0) itemCost.scaledAmount.toInt().toString() else "%.1f".format(itemCost.scaledAmount)
                        val formattedLine = "%.2f".format(itemCost.lineTotal)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    editingCostIngredientIndex = index
                                    manualCostInputText = if (itemCost.ingredient.manualUnitCost > 0.0) itemCost.ingredient.manualUnitCost.toString() else ""
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${itemCost.ingredient.name} ($formattedAmount ${itemCost.ingredient.unit})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Origen: ${itemCost.sourceLabel} • ${if (itemCost.unitCost > 0) "$${if (itemCost.unitCost < 0.01) "%.4f".format(itemCost.unitCost) else "%.2f".format(itemCost.unitCost)}/${itemCost.ingredient.unit}" else "Sin costo"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (itemCost.isFromPantry) com.example.ui.theme.StockGreen else if (itemCost.unitCost > 0.0) MaterialTheme.colorScheme.secondary else com.example.ui.theme.StockYellow,
                                    fontSize = 11.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (itemCost.lineTotal > 0.0) "$$formattedLine" else "Pendiente",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (itemCost.lineTotal > 0.0) MaterialTheme.colorScheme.onSurface else com.example.ui.theme.StockYellow
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar costo",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // --- INGREDIENTES LIST WITH PANTRY AVAILABILITY CHECK ---
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ingredientes calculados ($targetServings porc.)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (missingIngredientsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StockYellow.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StockYellow, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Faltan $missingIngredientsCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StockYellow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StockGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StockGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "¡Todo en despensa!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StockGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                recipe.ingredients.forEach { ingredient ->
                    val scaledAmount = ingredient.amount * scaleFactor
                    val formattedScaled = if (scaledAmount % 1.0 == 0.0) {
                        scaledAmount.toInt().toString()
                    } else {
                        "%.1f".format(scaledAmount)
                    }

                    val inStockItem = pantryItems.find {
                        it.name.trim().equals(ingredient.name.trim(), ignoreCase = true)
                    }
                    val inStockQty = inStockItem?.quantity ?: 0.0
                    val isSufficient = inStockQty >= scaledAmount

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), strokeWidthDp = 1.2f)
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
                                    text = ingredient.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val stockText = if (inStockItem != null) {
                                    val formattedInStock = if (inStockQty % 1.0 == 0.0) inStockQty.toInt().toString() else "%.1f".format(inStockQty)
                                    if (isSufficient) {
                                        "✓ Tienes $formattedInStock ${inStockItem.unit} en despensa"
                                    } else {
                                        val shortage = scaledAmount - inStockQty
                                        val formattedShortage = if (shortage % 1.0 == 0.0) shortage.toInt().toString() else "%.1f".format(shortage)
                                        "⚠ Falta $formattedShortage ${ingredient.unit} (Tienes $formattedInStock)"
                                    }
                                } else {
                                    "✗ No está en tu despensa"
                                }

                                Text(
                                    text = stockText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSufficient) StockGreen else StockRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$formattedScaled ${ingredient.unit}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Button to add shortages to shopping list
                com.example.ui.components.PostItButton(
                    text = if (missingIngredientsCount > 0)
                        "Agregar faltantes a Lista de Compras ($missingIngredientsCount)"
                    else
                        "Verificar y agregar ingredientes a Compras",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onAddMissingToShoppingList,
                    backgroundColor = com.example.ui.theme.PostItMint,
                    rotationDegrees = -0.8f,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("add_missing_to_shopping_button")
                )
            }

            // --- INSTRUCTIONS STEP BY STEP ---
            if (recipe.instructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pasos de preparación (toca para marcar)",
                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                recipe.instructions.forEachIndexed { index, step ->
                    val isChecked = completedSteps.contains(index)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), strokeWidthDp = 1.2f)
                            .clickable {
                                if (isChecked) completedSteps.remove(index) else completedSteps.add(index)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) completedSteps.add(index) else completedSteps.remove(index)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Paso ${index + 1}",
                                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step,
                                    fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                    fontSize = 17.sp,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Chef notes Post-It
            if (recipe.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                com.example.ui.components.PostItCard(
                    backgroundColor = com.example.ui.theme.PostItYellow,
                    rotationDegrees = 1f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Consejos del Chef",
                        fontFamily = com.example.ui.theme.CaveatFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) androidx.compose.ui.graphics.Color(0xFFFDE047) else androidx.compose.ui.graphics.Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recipe.notes,
                        fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                        fontSize = 17.sp,
                        color = if (isDark) androidx.compose.ui.graphics.Color(0xFFF4F4F5) else androidx.compose.ui.graphics.Color(0xFF18181B)
                    )
                }
            }

            // Bottom Page Navigation matching reference image ("Main" & Red Stamp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    com.example.ui.components.NotebookBackStamp(onClick = onBack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Main",
                        fontFamily = com.example.ui.theme.CaveatFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Recetario",
                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Active Timer Dialog
    if (activeTimerMinutes != null) {
        CookingTimerDialog(
            initialMinutes = activeTimerMinutes ?: 5,
            timerTitle = activeTimerLabel,
            onDismiss = { activeTimerMinutes = null }
        )
    }

    // Confirm Delete Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar receta?") },
            text = { Text("Esta acción eliminará '${recipe.title}' de forma permanente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_recipe")
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Quick Edit Manual Cost Dialog
    if (editingCostIngredientIndex != null) {
        val idx = editingCostIngredientIndex!!
        val targetIngredient = recipe.ingredients.getOrNull(idx)
        if (targetIngredient != null) {
            AlertDialog(
                onDismissRequest = { editingCostIngredientIndex = null },
                title = { Text("Costo para ${targetIngredient.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ingresa el costo unitario por ${targetIngredient.unit} para calcular el costo de la receta cuando no esté en despensa:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = manualCostInputText,
                            onValueChange = { manualCostInputText = it },
                            label = { Text("Costo ($ / ${targetIngredient.unit})") },
                            placeholder = { Text("Ej. 0.002") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newCost = manualCostInputText.toDoubleOrNull() ?: 0.0
                            val updatedIngredients = recipe.ingredients.toMutableList()
                            updatedIngredients[idx] = targetIngredient.copy(manualUnitCost = newCost)
                            val updatedRecipe = recipe.copy(ingredients = updatedIngredients)
                            onUpdateRecipe(updatedRecipe)
                            editingCostIngredientIndex = null
                        }
                    ) {
                        Text("Guardar Costo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCostIngredientIndex = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
