package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.ui.MainViewModel
import com.example.ui.RecipeFeasibility
import com.example.ui.components.CookingTimerDialog
import com.example.ui.components.PostItButton
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

@Composable
fun CookingAssistantScreen(
    viewModel: MainViewModel,
    recipes: List<Recipe>,
    pantryItems: List<PantryItem>,
    onSelectRecipe: (Recipe) -> Unit
) {
    var activeTimerMinutes by remember { mutableStateOf<Int?>(null) }

    val feasibilities = remember(recipes, pantryItems) {
        viewModel.calculateRecipeFeasibilities(recipes, pantryItems)
    }

    val readyToCook = feasibilities.filter { it.matchPercentage == 100 }
    val almostReady = feasibilities.filter { it.matchPercentage in 50..99 }
    val needSupplies = feasibilities.filter { it.matchPercentage < 50 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .notebookPaperBackground(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sketched Box Header matching reference image: "[Surprise me]"
            item {
                Box(
                    modifier = Modifier
                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Surprise me",
                        fontFamily = CaveatFontFamily,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Hero card: Análisis de despensa vs recetas
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .sketchedBorder(color = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¿Qué puedo cocinar hoy?",
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Analizamos tu despensa contra todas tus recetas para recomendarte qué puedes preparar ahora mismo sin salir a comprar.",
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Quick Timer Section with Post-It buttons
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.sketchedBorder(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Temporizador de Cocina",
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val timerColors = listOf(
                                PostItYellow,
                                PostItPeach,
                                PostItMint,
                                PostItSky,
                                PostItPink,
                                PostItYellow
                            )
                            listOf(3, 5, 10, 15, 20, 30).forEachIndexed { idx, mins ->
                                PostItButton(
                                    text = "${mins}m",
                                    onClick = { activeTimerMinutes = mins },
                                    backgroundColor = timerColors[idx % timerColors.size],
                                    fontSize = 17.sp,
                                    rotationDegrees = if (idx % 2 == 0) -1f else 1f,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Category 1: 100% Ready to cook
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StockGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Listas para cocinar (${readyToCook.size})",
                        fontFamily = CaveatFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (readyToCook.isEmpty()) {
                item {
                    Text(
                        text = "No tienes recetas completas al 100% con tu despensa actual. Revisa las opciones casi listas a continuación.",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(readyToCook) { item ->
                    FeasibilityRecipeCard(
                        feasibility = item,
                        onClick = { onSelectRecipe(item.recipe) },
                        onAddMissingToShopping = {}
                    )
                }
            }

            // Category 2: Almost ready (50% - 99%)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StockYellow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Casi listas (${almostReady.size})",
                        fontFamily = CaveatFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (almostReady.isEmpty()) {
                item {
                    Text(
                        text = "No hay recetas en esta categoría.",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(almostReady) { item ->
                    FeasibilityRecipeCard(
                        feasibility = item,
                        onClick = { onSelectRecipe(item.recipe) },
                        onAddMissingToShopping = {
                            viewModel.addMissingRecipeIngredientsToShoppingList(
                                item.recipe,
                                item.recipe.servings,
                                pantryItems
                            )
                        }
                    )
                }
            }

            // Category 3: Need more supplies (< 50%)
            if (needSupplies.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Requieren más ingredientes (${needSupplies.size})",
                        fontFamily = CaveatFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(needSupplies) { item ->
                    FeasibilityRecipeCard(
                        feasibility = item,
                        onClick = { onSelectRecipe(item.recipe) },
                        onAddMissingToShopping = {
                            viewModel.addMissingRecipeIngredientsToShoppingList(
                                item.recipe,
                                item.recipe.servings,
                                pantryItems
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (activeTimerMinutes != null) {
        CookingTimerDialog(
            initialMinutes = activeTimerMinutes ?: 5,
            timerTitle = "Temporizador (${activeTimerMinutes} min)",
            onDismiss = { activeTimerMinutes = null }
        )
    }
}

@Composable
private fun FeasibilityRecipeCard(
    feasibility: RecipeFeasibility,
    onClick: () -> Unit,
    onAddMissingToShopping: () -> Unit
) {
    val recipe = feasibility.recipe
    val pct = feasibility.matchPercentage
    val barColor = when {
        pct >= 100 -> StockGreen
        pct >= 50 -> StockYellow
        else -> StockRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("assistant_recipe_${recipe.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.title,
                    fontFamily = CaveatFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = barColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$pct% despensa",
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = pct / 100f,
                animationSpec = tween(700),
                label = "feasibilityProgress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${feasibility.availableCount} de ${feasibility.totalCount} ingredientes listos • ${recipe.totalTimeMinutes} min",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (feasibility.missingIngredients.isNotEmpty()) {
                    PostItButton(
                        text = "Comprar (${feasibility.missingIngredients.size})",
                        icon = Icons.Default.ShoppingCart,
                        onClick = onAddMissingToShopping,
                        backgroundColor = PostItMint,
                        fontSize = 17.sp,
                        rotationDegrees = -1f,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("buy_missing_${recipe.id}")
                    )
                }
            }

            if (feasibility.missingIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Te falta: ${feasibility.missingIngredients.joinToString(", ") { it.first }}",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
