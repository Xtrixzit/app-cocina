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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.settings.AppSettingsPreferences
import com.example.ui.components.CookingTimerDialog
import com.example.ui.components.NotebookBackStamp
import com.example.ui.components.PostItButton
import com.example.ui.components.PostItCard
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
import com.example.ui.theme.StockYellow
import com.example.util.ImageUtils
import com.example.util.LocalizedLabels
import com.example.util.UnitConverter
import java.util.Locale

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

    val context = LocalContext.current
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val marginPercent by AppSettingsPreferences.observeMarginPercent(context).collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var activeTimerLabel by remember { mutableStateOf("") }
    var editingCostIngredientIndex by remember { mutableStateOf<Int?>(null) }
    var manualCostInputText by remember { mutableStateOf("") }

    val completedSteps = remember { mutableStateListOf<Int>() }

    val scaleFactor = targetServings.toDouble() / recipe.servings.coerceAtLeast(1)

    var missingIngredientsCount = 0
    var totalProductionCost = 0.0

    data class IngredientCostDetail(
        val ingredient: RecipeIngredient,
        val scaledAmount: Double,
        val unitCost: Double,
        val lineTotal: Double,
        val sourceLabel: String,
        val isFromPantry: Boolean
    )

    val labelPantry = stringResource(R.string.recipe_detail_origin_pantry)
    val labelManual = stringResource(R.string.recipe_detail_origin_manual)
    val labelNoCost = stringResource(R.string.recipe_detail_origin_none)

    val costBreakdown = recipe.ingredients.map { ing ->
        val needed = ing.amount * scaleFactor
        val inStockItem = pantryItems.find { it.name.trim().equals(ing.name.trim(), ignoreCase = true) }
        val inStockConverted = UnitConverter.availableInTargetUnit(inStockItem, ing.unit)
        if (inStockConverted + 0.0001 < needed) {
            missingIngredientsCount++
        }

        val pantryUnitCost = UnitConverter.unitCostInTargetUnit(inStockItem, ing.unit)
        val effectiveUnitCost = when {
            pantryUnitCost > 0.0 -> pantryUnitCost
            ing.manualUnitCost > 0.0 -> ing.manualUnitCost
            else -> 0.0
        }
        val lineCost = needed * effectiveUnitCost
        totalProductionCost += lineCost

        val sourceLabel = when {
            pantryUnitCost > 0.0 -> labelPantry
            ing.manualUnitCost > 0.0 -> labelManual
            else -> labelNoCost
        }

        IngredientCostDetail(
            ingredient = ing,
            scaledAmount = needed,
            unitCost = effectiveUnitCost,
            lineTotal = lineCost,
            sourceLabel = sourceLabel,
            isFromPantry = pantryUnitCost > 0.0
        )
    }

    val costPerServing = if (targetServings > 0) totalProductionCost / targetServings else 0.0
    val suggestedTotalSale = AppSettingsPreferences.suggestedSellingPrice(totalProductionCost, marginPercent)
    val suggestedPerServingSale = if (targetServings > 0) suggestedTotalSale / targetServings else 0.0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        recipe.title,
                        maxLines = 1,
                        fontFamily = CaveatFontFamily,
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
                        NotebookBackStamp(
                            onClick = onBack,
                            modifier = Modifier.testTag("recipe_detail_back")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.action_back_main),
                            fontFamily = CaveatFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("recipe_detail_edit")) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("recipe_detail_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
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
            val effectivePhotoUri = recipe.imageUrl.ifBlank {
                ImageUtils.defaultUriForCategory(recipe.category)
            }
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
                    model = ImageUtils.toImageModel(effectivePhotoUri),
                    contentDescription = stringResource(R.string.recipe_detail_photo_cd, recipe.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            }

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
                        text = LocalizedLabels.recipeCategory(context, recipe.category),
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
                        text = stringResource(
                            R.string.recipe_detail_difficulty,
                            LocalizedLabels.difficulty(context, recipe.difficulty)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.recipe_detail_prep),
                            fontFamily = CaveatFontFamily,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.recipe_card_minutes, recipe.prepTimeMinutes),
                            fontFamily = CaveatFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (recipe.prepTimeMinutes > 0) {
                            val prepTimerTitle = stringResource(R.string.recipe_detail_timer_prep_label, recipe.title)
                            PostItButton(
                                text = stringResource(R.string.recipe_detail_timer),
                                icon = Icons.Default.Timer,
                                onClick = {
                                    activeTimerMinutes = recipe.prepTimeMinutes
                                    activeTimerLabel = prepTimerTitle
                                },
                                backgroundColor = PostItYellow,
                                fontSize = 17.sp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .testTag("timer_prep_button")
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.recipe_detail_cook),
                            fontFamily = CaveatFontFamily,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.recipe_card_minutes, recipe.cookTimeMinutes),
                            fontFamily = CaveatFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (recipe.cookTimeMinutes > 0) {
                            val cookTimerTitle = stringResource(R.string.recipe_detail_timer_cook_label, recipe.title)
                            PostItButton(
                                text = stringResource(R.string.recipe_detail_timer),
                                icon = Icons.Default.Timer,
                                onClick = {
                                    activeTimerMinutes = recipe.cookTimeMinutes
                                    activeTimerLabel = cookTimerTitle
                                },
                                backgroundColor = PostItMint,
                                fontSize = 17.sp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .testTag("timer_cook_button")
                            )
                        }
                    }
                }
            }

            // Servings Scaler
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
                                text = stringResource(R.string.recipe_detail_servings_calc),
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.recipe_detail_base_servings, recipe.servings),
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { onServingsChanged(targetServings - 1) },
                                enabled = targetServings > 1,
                                modifier = Modifier.testTag("servings_decrease")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.recipe_detail_less_servings))
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
                                    fontFamily = CaveatFontFamily,
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
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.recipe_detail_more_servings))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            stringResource(R.string.recipe_detail_preset_base, recipe.servings) to recipe.servings,
                            stringResource(R.string.recipe_detail_preset_double, recipe.servings * 2) to (recipe.servings * 2),
                            stringResource(R.string.recipe_detail_preset_triple, recipe.servings * 3) to (recipe.servings * 3),
                            stringResource(R.string.recipe_detail_preset_half, (recipe.servings / 2).coerceAtLeast(1)) to (recipe.servings / 2).coerceAtLeast(1)
                        ).forEach { (label, value) ->
                            PostItFilterChip(
                                selected = targetServings == value,
                                onClick = { onServingsChanged(value) },
                                text = label,
                                selectedColor = PostItPeach
                            )
                        }
                    }
                }
            }

            // Production Cost Ticket
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
                                text = stringResource(R.string.recipe_detail_cost_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.recipe_detail_cost_sub, targetServings),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.recipe_detail_cost_total),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AppSettingsPreferences.formatMoney(totalProductionCost, currency),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    R.string.recipe_detail_cost_per_serving,
                                    AppSettingsPreferences.formatMoney(costPerServing, currency)
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (totalProductionCost > 0.0) {
                                Text(
                                    text = stringResource(
                                        R.string.recipe_detail_suggested_sale,
                                        marginPercent,
                                        AppSettingsPreferences.formatMoney(suggestedTotalSale, currency),
                                        AppSettingsPreferences.formatMoney(suggestedPerServingSale, currency)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StockGreen
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = stringResource(R.string.recipe_detail_cost_breakdown_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    costBreakdown.forEachIndexed { index, itemCost ->
                        val formattedAmount = formatQty(itemCost.scaledAmount)
                        val displayUnit = LocalizedLabels.unit(context, itemCost.ingredient.unit)
                        val rateText = if (itemCost.unitCost > 0.0) {
                            "${AppSettingsPreferences.formatMoney(itemCost.unitCost, currency, forceFourDecimalsIfTiny = true)}/$displayUnit"
                        } else {
                            labelNoCost
                        }

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
                                    text = "${itemCost.ingredient.name} ($formattedAmount $displayUnit)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.recipe_detail_origin_line,
                                        itemCost.sourceLabel,
                                        rateText
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (itemCost.isFromPantry) StockGreen else if (itemCost.unitCost > 0.0) MaterialTheme.colorScheme.secondary else StockYellow,
                                    fontSize = 11.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (itemCost.lineTotal > 0.0) {
                                        AppSettingsPreferences.formatMoney(itemCost.lineTotal, currency)
                                    } else {
                                        stringResource(R.string.recipe_detail_pending)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (itemCost.lineTotal > 0.0) MaterialTheme.colorScheme.onSurface else StockYellow
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.recipe_detail_edit_cost_cd),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Ingredients list with unit-aware pantry check
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recipe_detail_ingredients_calc, targetServings),
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
                                    text = stringResource(R.string.recipe_detail_missing_count, missingIngredientsCount),
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
                                    text = stringResource(R.string.recipe_detail_all_in_pantry),
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
                    val formattedScaled = formatQty(scaledAmount)
                    val displayRecipeUnit = LocalizedLabels.unit(context, ingredient.unit)

                    val inStockItem = pantryItems.find {
                        it.name.trim().equals(ingredient.name.trim(), ignoreCase = true)
                    }
                    val convertedStock = UnitConverter.availableInTargetUnit(inStockItem, ingredient.unit)
                    val isSufficient = convertedStock + 0.0001 >= scaledAmount

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
                                    val formattedNativeStock = formatQty(inStockItem.quantity)
                                    val nativeUnit = LocalizedLabels.unit(context, inStockItem.unit)
                                    if (isSufficient) {
                                        stringResource(R.string.recipe_detail_stock_ok, formattedNativeStock, nativeUnit)
                                    } else {
                                        val shortage = (scaledAmount - convertedStock).coerceAtLeast(0.0)
                                        val formattedShortage = formatQty(shortage)
                                        stringResource(
                                            R.string.recipe_detail_stock_short,
                                            formattedShortage,
                                            displayRecipeUnit,
                                            "$formattedNativeStock $nativeUnit"
                                        )
                                    }
                                } else {
                                    stringResource(R.string.recipe_detail_stock_missing)
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
                                    text = "$formattedScaled $displayRecipeUnit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                PostItButton(
                    text = if (missingIngredientsCount > 0) {
                        stringResource(R.string.recipe_detail_add_missing, missingIngredientsCount)
                    } else {
                        stringResource(R.string.recipe_detail_check_shopping)
                    },
                    icon = Icons.Default.ShoppingCart,
                    onClick = onAddMissingToShoppingList,
                    backgroundColor = PostItMint,
                    rotationDegrees = -0.8f,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("add_missing_to_shopping_button")
                )
            }

            if (recipe.instructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.recipe_detail_steps_title),
                    fontFamily = CaveatFontFamily,
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
                                    text = stringResource(R.string.recipe_detail_step_num, index + 1),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step,
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 17.sp,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (recipe.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                PostItCard(
                    backgroundColor = PostItYellow,
                    rotationDegrees = 1f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.recipe_detail_chef_notes),
                        fontFamily = CaveatFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recipe.notes,
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 17.sp,
                        color = Color(0xFF18181B)
                    )
                }
            }

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
                    NotebookBackStamp(onClick = onBack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_back_main),
                        fontFamily = CaveatFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = CaveatFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (activeTimerMinutes != null) {
        CookingTimerDialog(
            initialMinutes = activeTimerMinutes ?: 5,
            timerTitle = activeTimerLabel,
            onDismiss = { activeTimerMinutes = null }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.recipe_detail_delete_title)) },
            text = { Text(stringResource(R.string.recipe_detail_delete_body, recipe.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_recipe")
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (editingCostIngredientIndex != null) {
        val idx = editingCostIngredientIndex!!
        val targetIngredient = recipe.ingredients.getOrNull(idx)
        if (targetIngredient != null) {
            val unitLabel = LocalizedLabels.unit(context, targetIngredient.unit)
            AlertDialog(
                onDismissRequest = { editingCostIngredientIndex = null },
                title = {
                    Text(
                        stringResource(R.string.recipe_detail_manual_cost_title, targetIngredient.name),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.recipe_detail_manual_cost_desc, unitLabel),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = manualCostInputText,
                            onValueChange = { manualCostInputText = it },
                            label = { Text(stringResource(R.string.recipe_detail_manual_cost_label, currency.symbol, unitLabel)) },
                            placeholder = { Text(stringResource(R.string.recipe_detail_manual_cost_hint)) },
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
                            onUpdateRecipe(recipe.copy(ingredients = updatedIngredients))
                            editingCostIngredientIndex = null
                        }
                    ) {
                        Text(stringResource(R.string.recipe_detail_manual_cost_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCostIngredientIndex = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

private fun formatQty(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
