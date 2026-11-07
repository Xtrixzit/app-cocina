package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Recipe
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.screens.CookingAssistantScreen
import com.example.ui.screens.PantryFormDialog
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.RecipeDetailScreen
import com.example.ui.screens.RecipeFormDialog
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.ShoppingListScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check before setContent, ViewModels or Firebase operations
        com.example.util.configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(false) }
            MyApplicationTheme(darkTheme = isDarkMode) {
                MainFeatureScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    viewModel: MainViewModel = viewModel(),
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recetas, 1: Despensa, 2: Compras, 3: Asistente
    var showAboutDialog by remember { mutableStateOf(false) }

    // Detail and Form dialog states
    val selectedRecipe by viewModel.selectedRecipe.collectAsState()
    val targetServings by viewModel.targetServings.collectAsState()
    var recipeFormTarget by remember { mutableStateOf<Recipe?>(null) }
    var showRecipeForm by remember { mutableStateOf(false) }
    var showPantryForm by remember { mutableStateOf(false) }

    // Collect user notifications
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Pantry state for cross-checking
    val pantryUiState by viewModel.pantryState.collectAsState()
    val pantryItems = when (val s = pantryUiState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    // Shopping items count for badge
    val shoppingUiState by viewModel.shoppingState.collectAsState()
    val pendingShoppingCount = when (val s = shoppingUiState) {
        is UiState.Success -> s.data.count { !it.isPurchased }
        else -> 0
    }

    // Recipes state for Assistant
    val recipesUiState by viewModel.recipesState.collectAsState()
    val recipesList = when (val s = recipesUiState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    // If viewing a recipe detail screen, display detail view with BackHandler
    if (selectedRecipe != null) {
        RecipeDetailScreen(
            recipe = selectedRecipe!!,
            pantryItems = pantryItems,
            targetServings = targetServings,
            onServingsChanged = { viewModel.setTargetServings(it) },
            onBack = { viewModel.selectRecipe(null) },
            onEdit = {
                recipeFormTarget = selectedRecipe
                showRecipeForm = true
            },
            onDelete = {
                viewModel.deleteRecipe(selectedRecipe!!.id)
            },
            onAddMissingToShoppingList = {
                viewModel.addMissingRecipeIngredientsToShoppingList(
                    selectedRecipe!!,
                    targetServings,
                    pantryItems
                )
            },
            onUpdateRecipe = { updated ->
                viewModel.saveRecipe(updated)
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        if (selectedTab == 0) {
                            Text(
                                "Mi Cuaderno de Recetas",
                                fontFamily = com.example.ui.theme.CaveatFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            val title = when (selectedTab) {
                                1 -> "Mi Despensa"
                                2 -> "Check List • Compras"
                                3 -> "Surprise Me • Asistente"
                                else -> "Recetario"
                            }
                            Text(
                                title,
                                fontFamily = com.example.ui.theme.CaveatFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.testTag("action_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.testTag("action_about")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Información", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )
            },
            bottomBar = {
                com.example.ui.components.NotebookBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    pendingShoppingCount = pendingShoppingCount
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        0 -> RecipesScreen(
                            viewModel = viewModel,
                            pantryItems = pantryItems,
                            onSelectRecipe = { recipe -> viewModel.selectRecipe(recipe) },
                            onAddNewRecipe = {
                                recipeFormTarget = null
                                showRecipeForm = true
                            },
                            onNavigateToCheckList = { selectedTab = 2 },
                            onNavigateToSurpriseMe = { selectedTab = 3 },
                            onShowHelp = { showAboutDialog = true }
                        )
                        1 -> PantryScreen(
                            viewModel = viewModel,
                            onAddNewItem = { showPantryForm = true }
                        )
                        2 -> ShoppingListScreen(
                            viewModel = viewModel,
                            pantryItems = pantryItems
                        )
                        3 -> CookingAssistantScreen(
                            viewModel = viewModel,
                            recipes = recipesList,
                            pantryItems = pantryItems,
                            onSelectRecipe = { recipe -> viewModel.selectRecipe(recipe) }
                        )
                    }
                }
            }
        }
    }

    // Recipe Create/Edit Dialog
    if (showRecipeForm) {
        RecipeFormDialog(
            initialRecipe = recipeFormTarget,
            onDismiss = { showRecipeForm = false },
            onSave = { recipe ->
                viewModel.saveRecipe(recipe)
            }
        )
    }

    // Pantry Add Dialog
    if (showPantryForm) {
        PantryFormDialog(
            initialItem = null,
            onDismiss = { showPantryForm = false },
            onSave = { item ->
                viewModel.savePantryItem(item)
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Recetario & Despensa", fontWeight = FontWeight.Bold) },
            text = {
                Text("Aplicación 100% privada y local.\nTus recetas, inventario de despensa y lista de compras se almacenan directamente en la memoria de tu dispositivo, sin necesidad de cuentas ni conexión externa.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}
