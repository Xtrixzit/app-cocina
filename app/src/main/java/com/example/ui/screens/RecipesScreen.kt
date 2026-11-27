package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.settings.AppSettingsPreferences
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.DashboardEmptyState
import com.example.ui.components.PostItButton
import com.example.ui.components.PostItFilterChip
import com.example.ui.components.RecipeCard
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.components.tutorialTarget
import com.example.ui.theme.BlueprintNavy
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.util.LocalizedLabels
import com.example.util.RecipeCategories
import com.example.util.SampleContentLocalizer
import com.example.util.UnitConverter

@Composable
fun RecipesScreen(
    viewModel: MainViewModel,
    pantryItems: List<PantryItem>,
    onSelectRecipe: (Recipe) -> Unit,
    onAddNewRecipe: () -> Unit,
    onNavigateToPantry: () -> Unit = {},
    onNavigateToCheckList: () -> Unit = {},
    onNavigateToProduction: () -> Unit = {},
    onNavigateToSurpriseMe: () -> Unit = {},
    onShowHelp: () -> Unit = {},
    onOpenChefChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val recipesUiState by viewModel.recipesState.collectAsState()
    val pantryUiState by viewModel.pantryState.collectAsState()
    val searchQuery by viewModel.recipeSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedRecipeCategory.collectAsState()
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val customSavedCategories by AppSettingsPreferences.observeCustomRecipeCategories(context).collectAsStateWithLifecycle()
    var showManageCategoriesDialog by remember { mutableStateOf(false) }

    val hasNoRecipesAtAll = (recipesUiState as? UiState.Success)?.data?.isEmpty() == true
    val isPantryResolvedEmpty = pantryItems.isEmpty() && pantryUiState is UiState.Success
    val showDashboardEmptyState = hasNoRecipesAtAll && isPantryResolvedEmpty

    val successRecipes = (recipesUiState as? UiState.Success)?.data.orEmpty()
    val categories = remember(customSavedCategories, successRecipes) {
        RecipeCategories.mergeFilterCategories(customSavedCategories, successRecipes)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            PostItButton(
                text = stringResource(R.string.recipes_fab_add),
                icon = Icons.Default.Add,
                onClick = onAddNewRecipe,
                backgroundColor = PostItPeach,
                rotationDegrees = -2f,
                modifier = Modifier
                    .tutorialTarget("add_recipe_fab")
                    .testTag("add_recipe_fab")
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .notebookPaperBackground()
                .imePadding()
        ) {
            if (showDashboardEmptyState) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .testTag("recipes_dashboard_empty_scroll")
                ) {
                    RecipesHeroSection(
                        onOpenChefChat = onOpenChefChat,
                        onNavigateToProduction = onNavigateToProduction,
                        onNavigateToCheckList = onNavigateToCheckList,
                        onNavigateToSurpriseMe = onNavigateToSurpriseMe,
                        onShowHelp = onShowHelp
                    )
                    DashboardEmptyState(
                        onAddFirstRecipe = onAddNewRecipe,
                        onGoToPantry = onNavigateToPantry,
                        scrollable = false
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recipes_lazy_list"),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "recipes_hero_header") {
                        RecipesHeroSection(
                            onOpenChefChat = onOpenChefChat,
                            onNavigateToProduction = onNavigateToProduction,
                            onNavigateToCheckList = onNavigateToCheckList,
                            onNavigateToSurpriseMe = onNavigateToSurpriseMe,
                            onShowHelp = onShowHelp
                        )
                    }

                    item(key = "recipes_search_bar") {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.recipeSearchQuery.value = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.recipes_search_hint),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 17.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.recipeSearchQuery.value = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.action_clear),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .sketchedBorder(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    strokeWidthDp = 1.2f
                                )
                                .testTag("recipe_search_field"),
                            singleLine = true
                        )
                    }

                    item(key = "recipes_category_chips") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .testTag("recipes_category_lazy_row")
                        ) {
                            item(key = "manage_categories_action") {
                                PostItFilterChip(
                                    selected = false,
                                    onClick = { showManageCategoriesDialog = true },
                                    text = stringResource(R.string.recipes_manage_categories_chip),
                                    icon = Icons.Default.Add,
                                    selectedColor = PostItSky,
                                    modifier = Modifier.testTag("recipes_manage_categories_btn")
                                )
                            }
                            items(categories) { cat ->
                                val color = when (cat) {
                                    "Todas" -> PostItYellow
                                    "Desayunos", "Pan" -> PostItPeach
                                    "Almuerzos", "Comidas" -> PostItMint
                                    "Cenas" -> PostItSky
                                    "Postres" -> PostItPink
                                    else -> PostItYellow
                                }
                                PostItFilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { viewModel.selectedRecipeCategory.value = cat },
                                    text = LocalizedLabels.recipeCategory(context, cat),
                                    selectedColor = color,
                                    modifier = Modifier.testTag("recipe_chip_$cat")
                                )
                            }
                        }
                    }

                    when (val state = recipesUiState) {
                        is UiState.Loading -> {
                            item(key = "recipes_loading") {
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
                            item(key = "recipes_error") {
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
                                                text = stringResource(R.string.recipes_sync_error),
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
                                                fontSize = 17.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            PostItButton(
                                                text = stringResource(R.string.action_retry_sync),
                                                onClick = { viewModel.retryCloudSync() },
                                                backgroundColor = PostItYellow,
                                                fontSize = 20.sp,
                                                modifier = Modifier.testTag("reset_database_button")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is UiState.Success -> {
                            val filtered = state.data.filter { recipe ->
                                val matchesCat = selectedCategory == "Todas" || recipe.category.equals(selectedCategory, ignoreCase = true)
                                val matchesSearch = searchQuery.isBlank() ||
                                    recipe.title.contains(searchQuery, ignoreCase = true) ||
                                    recipe.ingredients.any { it.name.contains(searchQuery, ignoreCase = true) }
                                matchesCat && matchesSearch
                            }

                            if (filtered.isEmpty()) {
                                item(key = "recipes_empty_filtered") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestaurantMenu,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (searchQuery.isNotBlank()) {
                                                stringResource(R.string.recipes_empty_search)
                                            } else {
                                                stringResource(R.string.recipes_empty_none)
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (searchQuery.isNotBlank()) {
                                                stringResource(R.string.recipes_empty_search_tip)
                                            } else {
                                                stringResource(R.string.recipes_empty_none_tip)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        if (searchQuery.isBlank() && state.data.isEmpty()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            PostItButton(
                                                text = stringResource(R.string.dashboard_empty_cta_recipe),
                                                icon = Icons.Default.Add,
                                                onClick = onAddNewRecipe,
                                                backgroundColor = PostItPeach,
                                                fontSize = 19.sp,
                                                modifier = Modifier.testTag("recipes_empty_add_recipe_cta")
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(filtered, key = { it.id }) { recipe ->
                                    var available = 0
                                    var totalRecipeCost = 0.0
                                    recipe.ingredients.forEach { ing ->
                                        val inPantry = pantryItems.find {
                                            SampleContentLocalizer.sameItemName(it.name, ing.name)
                                        }
                                        val inPantryQty = UnitConverter.availableInTargetUnit(inPantry, ing.unit)
                                        if (inPantryQty + 0.0001 >= ing.amount && ing.amount > 0) available++

                                        val pantryUnitCost = UnitConverter.unitCostInTargetUnit(inPantry, ing.unit)
                                        val unitCost = if (pantryUnitCost > 0.0) pantryUnitCost else ing.manualUnitCost
                                        totalRecipeCost += (ing.amount * unitCost)
                                    }
                                    val pct = if (recipe.ingredients.isEmpty()) 100 else ((available.toDouble() / recipe.ingredients.size) * 100).toInt()

                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        RecipeCard(
                                            recipe = recipe,
                                            feasibilityMatchPct = pct,
                                            estimatedCost = if (totalRecipeCost > 0.0) totalRecipeCost else null,
                                            currency = currency,
                                            onClick = { onSelectRecipe(recipe) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showManageCategoriesDialog) {
                ManageRecipeCategoriesDialog(
                    customCategories = customSavedCategories,
                    onAddCategory = { raw ->
                        AppSettingsPreferences.addCustomRecipeCategory(context, raw)
                    },
                    onRemoveCategory = { cat ->
                        AppSettingsPreferences.removeCustomRecipeCategory(context, cat)
                        if (selectedCategory.equals(cat, ignoreCase = true)) {
                            viewModel.selectedRecipeCategory.value = "Todas"
                        }
                    },
                    onDismiss = { showManageCategoriesDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageRecipeCategoriesDialog(
    customCategories: List<String>,
    onAddCategory: (String) -> String?,
    onRemoveCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newCategoryText by remember { mutableStateOf("") }

    fun submitNewCategory() {
        val added = onAddCategory(newCategoryText)
        if (added != null) {
            newCategoryText = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("manage_categories_dialog"),
        title = {
            Text(
                text = stringResource(R.string.manage_categories_title),
                fontFamily = CaveatFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BlueprintNavy
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.manage_categories_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        label = { Text(stringResource(R.string.recipe_form_cat_custom_label)) },
                        placeholder = { Text(stringResource(R.string.recipe_form_cat_custom_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitNewCategory() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manage_categories_input")
                    )
                    PostItButton(
                        text = stringResource(R.string.recipe_form_cat_add_btn),
                        icon = Icons.Default.Add,
                        onClick = { submitNewCategory() },
                        backgroundColor = PostItMint,
                        fontSize = 16.sp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("manage_categories_add_btn")
                    )
                }

                HorizontalDivider(color = CharcoalInk.copy(alpha = 0.14f))

                Text(
                    text = stringResource(R.string.manage_categories_custom_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = BlueprintNavy
                )

                if (customCategories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.manage_categories_empty_custom),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customCategories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PostItYellow.copy(alpha = 0.85f),
                                border = BorderStroke(1.2.dp, CharcoalInk.copy(alpha = 0.7f)),
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalInk
                                    )
                                    IconButton(
                                        onClick = { onRemoveCategory(cat) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("manage_category_delete_$cat")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.recipe_form_cat_remove_cd),
                                            tint = CharcoalInk,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.manage_categories_base_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecipeCategories.DEFAULT_CATEGORIES.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = LocalizedLabels.recipeCategory(context, cat),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("manage_categories_close_btn")
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun RecipesHeroSection(
    onOpenChefChat: () -> Unit,
    onNavigateToProduction: () -> Unit,
    onNavigateToCheckList: () -> Unit,
    onNavigateToSurpriseMe: () -> Unit,
    onShowHelp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.recipes_hero_word1),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.recipes_hero_word2),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2.4f)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recipes_hero_box),
                            fontFamily = CaveatFontFamily,
                            fontSize = 30.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onOpenChefChat,
                        modifier = Modifier.testTag("recipes_open_chef_chat")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.recipes_quick_chef_chat),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFFDF8))
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), strokeWidthDp = 1.6f)
                    .padding(3.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ref_tortilla_patatas),
                    contentDescription = stringResource(R.string.recipes_hero_photo_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(9.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PostItButton(
                text = stringResource(R.string.recipes_quick_production),
                onClick = onNavigateToProduction,
                backgroundColor = PostItYellow,
                rotationDegrees = -1.2f,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .testTag("postit_quick_production")
            )

            PostItButton(
                text = stringResource(R.string.recipes_quick_checklist),
                onClick = onNavigateToCheckList,
                backgroundColor = PostItSky,
                rotationDegrees = 1.0f,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                modifier = Modifier
                    .weight(1.0f)
                    .testTag("postit_quick_checklist")
            )

            PostItButton(
                text = stringResource(R.string.recipes_quick_cook),
                onClick = onNavigateToSurpriseMe,
                backgroundColor = PostItMint,
                rotationDegrees = -0.8f,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .testTag("postit_quick_surpriseme")
            )

            PostItButton(
                text = stringResource(R.string.recipes_quick_help),
                onClick = onShowHelp,
                backgroundColor = PostItPeach,
                rotationDegrees = 1.2f,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier.testTag("postit_quick_help")
            )
        }
    }
}
