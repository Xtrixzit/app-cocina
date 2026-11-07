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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.RecipeCard
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.components.stampClickable

@Composable
fun RecipesScreen(
    viewModel: MainViewModel,
    pantryItems: List<PantryItem>,
    onSelectRecipe: (Recipe) -> Unit,
    onAddNewRecipe: () -> Unit,
    onNavigateToCheckList: () -> Unit = {},
    onNavigateToProduction: () -> Unit = {},
    onNavigateToSurpriseMe: () -> Unit = {},
    onShowHelp: () -> Unit = {}
) {
    val recipesUiState by viewModel.recipesState.collectAsState()
    val searchQuery by viewModel.recipeSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedRecipeCategory.collectAsState()

    val categories = listOf("Todas", "Desayunos", "Almuerzos", "Cenas", "Postres", "Snacks", "Sopas")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            com.example.ui.components.PostItButton(
                text = "+ Nueva Receta",
                icon = Icons.Default.Add,
                onClick = onAddNewRecipe,
                backgroundColor = com.example.ui.theme.PostItPeach,
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
            // Reference Style Header: "Simply Cooking Recipes"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Simply",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cooking",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2.4f)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Recipes",
                        fontFamily = com.example.ui.theme.CaveatFontFamily,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Reference style Post-It Quick Action Buttons: [Producción] [Check list] [Surprise me] [Ayuda]
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.PostItButton(
                        text = "Producción",
                        onClick = onNavigateToProduction,
                        backgroundColor = com.example.ui.theme.PostItYellow,
                        rotationDegrees = -1.2f,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("postit_quick_production")
                    )

                    com.example.ui.components.PostItButton(
                        text = "Check list",
                        onClick = onNavigateToCheckList,
                        backgroundColor = com.example.ui.theme.PostItSky,
                        rotationDegrees = 1.0f,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1.0f)
                            .testTag("postit_quick_checklist")
                    )

                    com.example.ui.components.PostItButton(
                        text = "Surprise",
                        onClick = onNavigateToSurpriseMe,
                        backgroundColor = com.example.ui.theme.PostItMint,
                        rotationDegrees = -0.8f,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("postit_quick_surpriseme")
                    )

                    com.example.ui.components.PostItButton(
                        text = "Ayuda",
                        onClick = onShowHelp,
                        backgroundColor = com.example.ui.theme.PostItPeach,
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
                        "Buscar recetas, ingredientes...",
                        fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                        fontSize = 17.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.recipeSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurface)
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

            // Category Filter Post-It Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    val color = when (cat) {
                        "Todas" -> com.example.ui.theme.PostItYellow
                        "Desayunos" -> com.example.ui.theme.PostItPeach
                        "Almuerzos" -> com.example.ui.theme.PostItMint
                        "Cenas" -> com.example.ui.theme.PostItSky
                        "Postres" -> com.example.ui.theme.PostItPink
                        else -> com.example.ui.theme.PostItYellow
                    }
                    com.example.ui.components.PostItFilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.selectedRecipeCategory.value = cat },
                        text = cat,
                        selectedColor = color,
                        modifier = Modifier.testTag("recipe_chip_$cat")
                    )
                }
            }

            // Recipe List / States
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
                                    text = "Actualización de base de datos requerida",
                                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Se detectó una versión anterior de la base de datos local. Pulsa el botón para actualizarla y cargar tus recetas limpiamente.",
                                    fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                    fontSize = 17.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                com.example.ui.components.PostItButton(
                                    text = "Restablecer y Actualizar Base de Datos",
                                    onClick = { viewModel.resetDatabase() },
                                    backgroundColor = com.example.ui.theme.PostItYellow,
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
                                text = if (searchQuery.isNotBlank()) "No se encontraron recetas" else "Aún no tienes recetas guardadas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Prueba con otro término de búsqueda" else "Comienza agregando tu primera receta o carga las recetas de ejemplo para probar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (state.data.isEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                com.example.ui.components.PostItButton(
                                    text = "Cargar recetas de ejemplo",
                                    onClick = { viewModel.seedSampleRecipes() },
                                    backgroundColor = com.example.ui.theme.PostItYellow,
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
                                // Calculate feasibility for card
                                var available = 0
                                var totalRecipeCost = 0.0
                                recipe.ingredients.forEach { ing ->
                                    val inPantry = pantryItems.find { it.name.trim().equals(ing.name.trim(), ignoreCase = true) }
                                    val inPantryQty = inPantry?.quantity ?: 0.0
                                    if (inPantryQty >= ing.amount && ing.amount > 0) available++

                                    val unitCost = if (inPantry != null && inPantry.unitCost > 0.0) inPantry.unitCost else ing.manualUnitCost
                                    totalRecipeCost += (ing.amount * unitCost)
                                }
                                val pct = if (recipe.ingredients.isEmpty()) 100 else ((available.toDouble() / recipe.ingredients.size) * 100).toInt()

                                RecipeCard(
                                    recipe = recipe,
                                    feasibilityMatchPct = pct,
                                    estimatedCost = if (totalRecipeCost > 0.0) totalRecipeCost else null,
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
