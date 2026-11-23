package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.settings.AppSettingsPreferences
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.PostItButton
import com.example.ui.components.PostItFilterChip
import com.example.ui.components.RecipeCard
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
import com.example.util.UnitConverter

@Composable
fun RecipesScreen(
    viewModel: MainViewModel,
    pantryItems: List<PantryItem>,
    onSelectRecipe: (Recipe) -> Unit,
    onAddNewRecipe: () -> Unit,
    onNavigateToCheckList: () -> Unit = {},
    onNavigateToProduction: () -> Unit = {},
    onNavigateToSurpriseMe: () -> Unit = {},
    onShowHelp: () -> Unit = {},
    onOpenChefChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val recipesUiState by viewModel.recipesState.collectAsState()
    val searchQuery by viewModel.recipeSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedRecipeCategory.collectAsState()
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()

    val categories = listOf("Todas", "Desayunos", "Almuerzos", "Cenas", "Postres", "Snacks", "Bebidas", "Sopas")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PostItButton(
                text = stringResource(R.string.recipes_fab_add),
                icon = Icons.Default.Add,
                onClick = onAddNewRecipe,
                backgroundColor = PostItPeach,
                rotationDegrees = -2f,
                modifier = Modifier.testTag("add_recipe_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .notebookPaperBackground()
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
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("postit_quick_production")
                    )

                    PostItButton(
                        text = stringResource(R.string.recipes_quick_checklist),
                        onClick = onNavigateToCheckList,
                        backgroundColor = PostItSky,
                        rotationDegrees = 1.0f,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1.0f)
                            .testTag("postit_quick_checklist")
                    )

                    PostItButton(
                        text = stringResource(R.string.recipes_quick_cook),
                        onClick = onNavigateToSurpriseMe,
                        backgroundColor = PostItMint,
                        rotationDegrees = -0.8f,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("postit_quick_surpriseme")
                    )

                    PostItButton(
                        text = stringResource(R.string.recipes_quick_help),
                        onClick = onShowHelp,
                        backgroundColor = PostItPeach,
                        rotationDegrees = 1.2f,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("postit_quick_help")
                    )
                }
            }

            // Search Bar
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
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.recipeSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), strokeWidthDp = 1.2f)
                    .testTag("recipe_search_field"),
                singleLine = true
            )

            // Category Filter Post-It Chips (canonical values stored, localized display label)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    val color = when (cat) {
                        "Todas" -> PostItYellow
                        "Desayunos" -> PostItPeach
                        "Almuerzos" -> PostItMint
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

            when (val state = recipesUiState) {
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
                is UiState.Success -> {
                    val filtered = state.data.filter { recipe ->
                        val matchesCat = selectedCategory == "Todas" || recipe.category.equals(selectedCategory, ignoreCase = true)
                        val matchesSearch = searchQuery.isBlank() ||
                            recipe.title.contains(searchQuery, ignoreCase = true) ||
                            recipe.ingredients.any { it.name.contains(searchQuery, ignoreCase = true) }
                        matchesCat && matchesSearch
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
                                color = MaterialTheme.colorScheme.onSurface
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

                            if (state.data.isEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                PostItButton(
                                    text = stringResource(R.string.recipes_seed_samples),
                                    onClick = { viewModel.seedSampleRecipes() },
                                    backgroundColor = PostItYellow,
                                    fontSize = 20.sp,
                                    modifier = Modifier.testTag("seed_sample_recipes_button")
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { recipe ->
                                var available = 0
                                var totalRecipeCost = 0.0
                                recipe.ingredients.forEach { ing ->
                                    val inPantry = pantryItems.find {
                                        it.name.trim().equals(ing.name.trim(), ignoreCase = true)
                                    }
                                    val inPantryQty = UnitConverter.availableInTargetUnit(inPantry, ing.unit)
                                    if (inPantryQty + 0.0001 >= ing.amount && ing.amount > 0) available++

                                    val pantryUnitCost = UnitConverter.unitCostInTargetUnit(inPantry, ing.unit)
                                    val unitCost = if (pantryUnitCost > 0.0) pantryUnitCost else ing.manualUnitCost
                                    totalRecipeCost += (ing.amount * unitCost)
                                }
                                val pct = if (recipe.ingredients.isEmpty()) 100 else ((available.toDouble() / recipe.ingredients.size) * 100).toInt()

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
}
