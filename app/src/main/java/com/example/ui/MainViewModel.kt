package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.ShoppingItem
import com.example.data.repository.CookingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RecipeFeasibility(
    val recipe: Recipe,
    val matchPercentage: Int, // 0 - 100
    val availableCount: Int,
    val totalCount: Int,
    val missingIngredients: List<Pair<String, Double>> // Name and missing amount
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CookingRepository(application.applicationContext)

    init {
        viewModelScope.launch {
            try {
                val current = repository.observeRecipes().first()
                if (current.isEmpty()) {
                    repository.seedSampleRecipes()
                }
            } catch (_: Exception) {}
        }
    }

    // User notification messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Data streams
    val recipesState: StateFlow<UiState<List<Recipe>>> = repository.observeRecipes()
        .map<List<Recipe>, UiState<List<Recipe>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar recetas")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val pantryState: StateFlow<UiState<List<PantryItem>>> = repository.observePantry()
        .map<List<PantryItem>, UiState<List<PantryItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar despensa")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val shoppingState: StateFlow<UiState<List<ShoppingItem>>> = repository.observeShoppingList()
        .map<List<ShoppingItem>, UiState<List<ShoppingItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar lista de compras")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Search & Filter states
    val recipeSearchQuery = MutableStateFlow("")
    val selectedRecipeCategory = MutableStateFlow("Todas")

    val pantrySearchQuery = MutableStateFlow("")
    val selectedPantryCategory = MutableStateFlow("Todas")
    val filterOnlyLowStock = MutableStateFlow(false)

    // Selected recipe for detail/scaling
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    // Servings override for currently viewed recipe
    val targetServings = MutableStateFlow(4)

    fun selectRecipe(recipe: Recipe?) {
        _selectedRecipe.value = recipe
        if (recipe != null) {
            targetServings.value = recipe.servings
        }
    }

    fun setTargetServings(servings: Int) {
        targetServings.value = servings.coerceIn(1, 50)
    }

    // --- RECIPE ACTIONS ---

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                repository.saveRecipe(recipe)
                _userMessage.emit("Receta guardada exitosamente")
                if (_selectedRecipe.value?.id == recipe.id) {
                    _selectedRecipe.value = recipe
                }
            } catch (e: Exception) {
                _userMessage.emit("Error al guardar receta: ${e.localizedMessage}")
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipeId)
                if (_selectedRecipe.value?.id == recipeId) {
                    _selectedRecipe.value = null
                }
                _userMessage.emit("Receta eliminada")
            } catch (e: Exception) {
                _userMessage.emit("Error al eliminar: ${e.localizedMessage}")
            }
        }
    }

    fun seedSampleRecipes() {
        viewModelScope.launch {
            try {
                repository.seedSampleRecipes()
                _userMessage.emit("Recetas de ejemplo agregadas con éxito")
            } catch (e: Exception) {
                _userMessage.emit("Error al cargar ejemplos: ${e.localizedMessage}")
            }
        }
    }

    // --- PANTRY ACTIONS ---

    fun savePantryItem(item: PantryItem) {
        viewModelScope.launch {
            try {
                repository.savePantryItem(item)
                _userMessage.emit("Provisión guardada en despensa")
            } catch (e: Exception) {
                _userMessage.emit("Error al guardar en despensa: ${e.localizedMessage}")
            }
        }
    }

    fun adjustPantryStock(item: PantryItem, delta: Double) {
        viewModelScope.launch {
            try {
                val newQty = (item.quantity + delta).coerceAtLeast(0.0)
                repository.updatePantryStock(item.id, newQty, item.createdAt)
            } catch (e: Exception) {
                _userMessage.emit("Error al actualizar stock: ${e.localizedMessage}")
            }
        }
    }

    fun deletePantryItem(itemId: String) {
        viewModelScope.launch {
            try {
                repository.deletePantryItem(itemId)
                _userMessage.emit("Elemento eliminado de la despensa")
            } catch (e: Exception) {
                _userMessage.emit("Error al eliminar de despensa: ${e.localizedMessage}")
            }
        }
    }

    fun importInventory(importedItems: List<PantryItem>, mergeMode: Boolean) {
        viewModelScope.launch {
            try {
                repository.importInventory(importedItems, mergeMode)
                _userMessage.emit("¡Inventario actualizado con ${importedItems.size} provisiones!")
            } catch (e: Exception) {
                _userMessage.emit("Error al importar: ${e.localizedMessage}")
            }
        }
    }

    // --- SHOPPING LIST ACTIONS ---

    fun saveShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            try {
                repository.saveShoppingItem(item)
                _userMessage.emit("Guardado en Check list e Inventario")
            } catch (e: Exception) {
                _userMessage.emit("Error al guardar: ${e.localizedMessage}")
            }
        }
    }

    fun toggleShoppingItem(itemId: String, isPurchased: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleShoppingItemPurchased(itemId, isPurchased)
            } catch (e: Exception) {
                _userMessage.emit("Error al actualizar compra: ${e.localizedMessage}")
            }
        }
    }

    fun deleteShoppingItem(itemId: String) {
        viewModelScope.launch {
            try {
                repository.deleteShoppingItem(itemId)
            } catch (e: Exception) {
                _userMessage.emit("Error al eliminar de lista: ${e.localizedMessage}")
            }
        }
    }

    fun adjustShoppingItemQuantity(item: ShoppingItem, delta: Double) {
        viewModelScope.launch {
            try {
                repository.adjustShoppingItemQuantity(item, delta)
            } catch (e: Exception) {
                _userMessage.emit("Error al actualizar cantidad: ${e.localizedMessage}")
            }
        }
    }

    fun updateShoppingItemQuantity(itemId: String, newQuantity: Double, itemName: String? = null) {
        viewModelScope.launch {
            try {
                repository.updateShoppingItemQuantity(itemId, newQuantity, itemName)
            } catch (e: Exception) {
                _userMessage.emit("Error al actualizar cantidad: ${e.localizedMessage}")
            }
        }
    }

    fun clearPurchasedItems(items: List<ShoppingItem>) {
        viewModelScope.launch {
            try {
                repository.clearPurchasedShoppingItems(items)
                _userMessage.emit("Comprados eliminados de la lista")
            } catch (e: Exception) {
                _userMessage.emit("Error: ${e.localizedMessage}")
            }
        }
    }

    fun transferPurchasedToPantry(purchased: List<ShoppingItem>, currentPantry: List<PantryItem>) {
        viewModelScope.launch {
            try {
                repository.transferPurchasedToPantry(purchased, currentPantry)
                _userMessage.emit("¡Provisiones añadidas a tu despensa!")
            } catch (e: Exception) {
                _userMessage.emit("Error al transferir a despensa: ${e.localizedMessage}")
            }
        }
    }

    fun addMissingRecipeIngredientsToShoppingList(
        recipe: Recipe,
        targetServings: Int,
        currentPantry: List<PantryItem>
    ) {
        viewModelScope.launch {
            try {
                val added = repository.addMissingRecipeIngredientsToShoppingList(
                    recipe,
                    targetServings,
                    currentPantry
                )
                if (added > 0) {
                    _userMessage.emit("Se añadieron $added ingrediente(s) faltante(s) a tu lista de compras")
                } else {
                    _userMessage.emit("¡Tienes todos los ingredientes en tu despensa!")
                }
            } catch (e: Exception) {
                _userMessage.emit("Error al agregar a compras: ${e.localizedMessage}")
            }
        }
    }

    fun deductPantryStockForProduction(
        recipe: Recipe,
        scaleFactor: Double,
        currentPantry: List<PantryItem>
    ) {
        viewModelScope.launch {
            try {
                var deductedCount = 0
                for (ing in recipe.ingredients) {
                    val needed = ing.amount * scaleFactor
                    val match = currentPantry.find { it.name.trim().equals(ing.name.trim(), ignoreCase = true) }
                    if (match != null) {
                        val newQty = (match.quantity - needed).coerceAtLeast(0.0)
                        repository.updatePantryStock(match.id, newQty)
                        deductedCount++
                    }
                }
                _userMessage.emit("¡Producción registrada! Se actualizaron $deductedCount ingredientes en la despensa.")
            } catch (e: Exception) {
                _userMessage.emit("Error al descontar ingredientes: ${e.localizedMessage}")
            }
        }
    }

    fun addProductionShortageToShoppingList(
        recipe: Recipe,
        scaleFactor: Double,
        currentPantry: List<PantryItem>,
        targetUnitsLabel: String
    ) {
        viewModelScope.launch {
            try {
                var addedCount = 0
                for (ingredient in recipe.ingredients) {
                    val neededAmount = Math.round(ingredient.amount * scaleFactor * 100.0) / 100.0
                    val matching = currentPantry.find {
                        it.name.trim().equals(ingredient.name.trim(), ignoreCase = true)
                    }
                    val available = matching?.quantity ?: 0.0

                    if (available < neededAmount) {
                        val shortage = Math.round((neededAmount - available) * 100.0) / 100.0
                        val shoppingItem = ShoppingItem(
                            name = ingredient.name,
                            category = ingredient.category,
                            quantity = shortage,
                            unit = ingredient.unit,
                            isPurchased = false,
                            recipeSource = "Producción: ${recipe.title} ($targetUnitsLabel)"
                        )
                        repository.saveShoppingItem(shoppingItem, syncToPantry = false)
                        addedCount++
                    }
                }
                if (addedCount > 0) {
                    _userMessage.emit("Se añadieron $addedCount ingredientes faltantes a la Check list de compras")
                } else {
                    _userMessage.emit("¡Cuentas con suficiente stock para toda la producción!")
                }
            } catch (e: Exception) {
                _userMessage.emit("Error al agregar a compras: ${e.localizedMessage}")
            }
        }
    }

    // Helper: calculate feasibility for recipes based on current pantry
    fun calculateRecipeFeasibilities(
        recipes: List<Recipe>,
        pantry: List<PantryItem>
    ): List<RecipeFeasibility> {
        return recipes.map { recipe ->
            var available = 0
            val missing = mutableListOf<Pair<String, Double>>()

            for (ing in recipe.ingredients) {
                val inPantry = pantry.find {
                    it.name.trim().equals(ing.name.trim(), ignoreCase = true)
                }
                val hasQty = inPantry?.quantity ?: 0.0
                if (hasQty >= ing.amount && ing.amount > 0) {
                    available++
                } else {
                    val shortage = (ing.amount - hasQty).coerceAtLeast(0.0)
                    missing.add(Pair(ing.name, shortage))
                }
            }

            val total = recipe.ingredients.size
            val pct = if (total == 0) 100 else ((available.toDouble() / total) * 100).toInt()

            RecipeFeasibility(
                recipe = recipe,
                matchPercentage = pct,
                availableCount = available,
                totalCount = total,
                missingIngredients = missing
            )
        }.sortedByDescending { it.matchPercentage }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            try {
                repository.resetDatabase()
                _userMessage.emit("Base de datos restablecida correctamente.")
            } catch (e: Exception) {
                _userMessage.emit("Error al restablecer base de datos: ${e.localizedMessage}")
            }
        }
    }
}
