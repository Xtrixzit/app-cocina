package com.example.data.repository

import android.content.Context
import com.example.data.local.CookingDatabase
import com.example.data.local.entity.PantryEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.ShoppingEntity
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.ShoppingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class CookingRepository(
    private val database: CookingDatabase
) {
    constructor(context: Context) : this(CookingDatabase.getDatabase(context))

    private val recipeDao = database.recipeDao()
    private val pantryDao = database.pantryDao()
    private val shoppingDao = database.shoppingDao()

    // --- RECIPES ---

    fun observeRecipes(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipes()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun saveRecipe(recipe: Recipe) = withContext(Dispatchers.IO) {
        val docId = if (recipe.id.isNotBlank()) recipe.id else "rec_${UUID.randomUUID().toString().take(12)}"
        val entity = RecipeEntity.fromDomain(recipe.copy(id = docId))
        recipeDao.insertOrUpdate(entity)
    }

    suspend fun deleteRecipe(recipeId: String) = withContext(Dispatchers.IO) {
        recipeDao.deleteById(recipeId)
    }

    // --- PANTRY ---

    fun observePantry(): Flow<List<PantryItem>> {
        return pantryDao.getAllPantryItems()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun savePantryItem(item: PantryItem) = withContext(Dispatchers.IO) {
        val docId = if (item.id.isNotBlank()) item.id else "pan_${UUID.randomUUID().toString().take(12)}"
        val entity = PantryEntity.fromDomain(item.copy(id = docId))
        pantryDao.insertOrUpdate(entity)
    }

    suspend fun updatePantryStock(itemId: String, newQuantity: Double, createdAt: Any? = null) = withContext(Dispatchers.IO) {
        val clamped = if (newQuantity < 0) 0.0 else (Math.round(newQuantity * 100.0) / 100.0)
        pantryDao.updateQuantity(itemId, clamped)
    }

    suspend fun deletePantryItem(itemId: String) = withContext(Dispatchers.IO) {
        pantryDao.deleteById(itemId)
    }

    // --- SHOPPING LIST ---

    fun observeShoppingList(): Flow<List<ShoppingItem>> {
        return shoppingDao.getAllShoppingItems()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun saveShoppingItem(item: ShoppingItem, syncToPantry: Boolean = true) = withContext(Dispatchers.IO) {
        val docId = if (item.id.isNotBlank()) item.id else "shop_${UUID.randomUUID().toString().take(12)}"
        val entity = ShoppingEntity.fromDomain(item.copy(id = docId))
        shoppingDao.insertOrUpdate(entity)

        // Automatically store/update in the pantry inventory as well
        if (syncToPantry && item.name.isNotBlank()) {
            try {
                val existingPantry = pantryDao.getAllPantryItems().first()
                val match = existingPantry.find { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
                if (match != null) {
                    pantryDao.updateQuantity(match.id, item.quantity)
                } else {
                    val newPantryItem = PantryEntity(
                        id = "pan_${UUID.randomUUID().toString().take(12)}",
                        name = item.name.trim(),
                        category = item.category.ifBlank { "Despensa" },
                        quantity = item.quantity,
                        unit = item.unit.ifBlank { "unidades" },
                        minQuantity = 0.0,
                        cost = 0.0,
                        costBaseQuantity = 0.0,
                        expirationDate = "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    pantryDao.insertOrUpdate(newPantryItem)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun toggleShoppingItemPurchased(itemId: String, isPurchased: Boolean) = withContext(Dispatchers.IO) {
        shoppingDao.updatePurchased(itemId, isPurchased)
    }

    suspend fun deleteShoppingItem(itemId: String) = withContext(Dispatchers.IO) {
        shoppingDao.deleteById(itemId)
    }

    suspend fun updateShoppingItemQuantity(itemId: String, newQuantity: Double, itemName: String? = null) = withContext(Dispatchers.IO) {
        val clamped = if (newQuantity < 0) 0.0 else (Math.round(newQuantity * 100.0) / 100.0)
        shoppingDao.updateQuantity(itemId, clamped)

        if (itemName != null) {
            try {
                val existingPantry = pantryDao.getAllPantryItems().first()
                val match = existingPantry.find { it.name.trim().equals(itemName.trim(), ignoreCase = true) }
                if (match != null) {
                    pantryDao.updateQuantity(match.id, clamped)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun adjustShoppingItemQuantity(item: ShoppingItem, delta: Double) = withContext(Dispatchers.IO) {
        val newQty = (item.quantity + delta).coerceAtLeast(0.0)
        val clamped = Math.round(newQty * 100.0) / 100.0
        shoppingDao.updateQuantity(item.id, clamped)

        try {
            val existingPantry = pantryDao.getAllPantryItems().first()
            val match = existingPantry.find { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
            if (match != null) {
                pantryDao.updateQuantity(match.id, clamped)
            }
        } catch (_: Exception) {}
    }

    suspend fun clearPurchasedShoppingItems(items: List<ShoppingItem>) = withContext(Dispatchers.IO) {
        shoppingDao.deletePurchased()
    }

    /**
     * Transfers purchased items to the pantry inventory.
     * Updates quantities if already existing, otherwise inserts a new pantry item.
     */
    suspend fun transferPurchasedToPantry(
        purchasedItems: List<ShoppingItem>,
        currentPantry: List<PantryItem>
    ) = withContext(Dispatchers.IO) {
        for (purchased in purchasedItems) {
            val matchingPantry = currentPantry.find {
                it.name.trim().equals(purchased.name.trim(), ignoreCase = true)
            }
            if (matchingPantry != null) {
                val newQty = matchingPantry.quantity + purchased.quantity
                updatePantryStock(matchingPantry.id, newQty)
            } else {
                val newItem = PantryItem(
                    name = purchased.name,
                    category = purchased.category,
                    quantity = purchased.quantity,
                    unit = purchased.unit,
                    minQuantity = 0.0
                )
                savePantryItem(newItem)
            }
            deleteShoppingItem(purchased.id)
        }
    }

    /**
     * Imports inventory items from another device.
     * If mergeMode is true, updates existing item stocks and adds new ones.
     * If mergeMode is false, completely replaces current pantry with the imported one.
     */
    suspend fun importInventory(
        importedItems: List<PantryItem>,
        mergeMode: Boolean
    ) = withContext(Dispatchers.IO) {
        if (!mergeMode) {
            val existing = pantryDao.getAllPantryItems().first()
            for (item in existing) {
                pantryDao.deleteById(item.id)
            }
            for (imported in importedItems) {
                savePantryItem(imported)
            }
        } else {
            val existing = pantryDao.getAllPantryItems().first().map { it.toDomain() }
            for (imported in importedItems) {
                val match = existing.find { it.name.trim().equals(imported.name.trim(), ignoreCase = true) }
                if (match != null) {
                    pantryDao.updateQuantity(match.id, imported.quantity)
                } else {
                    savePantryItem(imported)
                }
            }
        }
    }

    /**
     * Add missing recipe ingredients for a target serving size directly to the shopping list.
     */
    suspend fun addMissingRecipeIngredientsToShoppingList(
        recipe: Recipe,
        targetServings: Int,
        currentPantry: List<PantryItem>
    ): Int = withContext(Dispatchers.IO) {
        val scale = targetServings.toDouble() / recipe.servings.coerceAtLeast(1)
        var addedCount = 0

        for (ingredient in recipe.ingredients) {
            val neededAmount = Math.round(ingredient.amount * scale * 100.0) / 100.0
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
                    recipeSource = "${recipe.title} ($targetServings porc.)"
                )
                saveShoppingItem(shoppingItem)
                addedCount++
            }
        }
        addedCount
    }

    suspend fun seedSampleRecipes() = withContext(Dispatchers.IO) {
        val samples = listOf(
            Recipe(
                title = "Tortilla de Patatas Tradicional",
                category = "Almuerzos",
                servings = 4,
                prepTimeMinutes = 15,
                cookTimeMinutes = 20,
                difficulty = "Fácil",
                ingredients = listOf(
                    RecipeIngredient(name = "Patatas", amount = 600.0, unit = "g", category = "Verduras", manualUnitCost = 0.002),
                    RecipeIngredient(name = "Huevos", amount = 6.0, unit = "unidades", category = "Lácteos", manualUnitCost = 0.30),
                    RecipeIngredient(name = "Cebolla", amount = 1.0, unit = "unidades", category = "Verduras", manualUnitCost = 0.45),
                    RecipeIngredient(name = "Aceite de oliva", amount = 150.0, unit = "ml", category = "Despensa", manualUnitCost = 0.008),
                    RecipeIngredient(name = "Sal", amount = 5.0, unit = "g", category = "Especias", manualUnitCost = 0.001)
                ),
                instructions = listOf(
                    "Pelar y cortar las patatas y la cebolla en láminas finas.",
                    "Pocharlas en la sartén a fuego medio-bajo con abundante aceite de oliva durante 15 minutos hasta que estén tiernas.",
                    "Batir los huevos en un bol grande y añadir una pizca de sal.",
                    "Escurrir las patatas y cebolla e incorporarlas al bol de huevos batidos. Dejar reposar 5 minutos para que absorban.",
                    "En una sartén caliente con unas gotas de aceite, verter la mezcla y cuajar 2-3 minutos por cada lado dando la vuelta con un plato."
                ),
                notes = "Para que quede jugosa, no cocinar en exceso el huevo al dar la vuelta."
            ),
            Recipe(
                title = "Pasta Cremosa con Pollo y Champiñones",
                category = "Cenas",
                servings = 2,
                prepTimeMinutes = 10,
                cookTimeMinutes = 15,
                difficulty = "Fácil",
                ingredients = listOf(
                    RecipeIngredient(name = "Pasta (Penne o Fettuccine)", amount = 200.0, unit = "g", category = "Granos", manualUnitCost = 0.003),
                    RecipeIngredient(name = "Pechuga de pollo", amount = 250.0, unit = "g", category = "Carnes", manualUnitCost = 0.009),
                    RecipeIngredient(name = "Champiñones", amount = 150.0, unit = "g", category = "Verduras", manualUnitCost = 0.008),
                    RecipeIngredient(name = "Crema de leche / Nata", amount = 200.0, unit = "ml", category = "Lácteos", manualUnitCost = 0.007),
                    RecipeIngredient(name = "Ajo", amount = 2.0, unit = "dientes", category = "Verduras", manualUnitCost = 0.15),
                    RecipeIngredient(name = "Queso Parmesano", amount = 40.0, unit = "g", category = "Lácteos", manualUnitCost = 0.025)
                ),
                instructions = listOf(
                    "Cocer la pasta en abundante agua con sal según las instrucciones del paquete.",
                    "En una sartén, saltear el pollo cortado en tiras con el ajo picado hasta dorar.",
                    "Añadir los champiñones laminados y cocinar 4 minutos más.",
                    "Verter la crema de leche y cocinar a fuego lento hasta espesar ligeramente.",
                    "Mezclar con la pasta escurrida y coronar con queso parmesano rallado."
                ),
                notes = "Reserva media taza del agua de cocción de la pasta para aligerar la salsa si queda muy densa."
            ),
            Recipe(
                title = "Panqueques Esponjosos de Avena y Plátano",
                category = "Desayunos",
                servings = 2,
                prepTimeMinutes = 8,
                cookTimeMinutes = 7,
                difficulty = "Fácil",
                ingredients = listOf(
                    RecipeIngredient(name = "Avena en copos", amount = 100.0, unit = "g", category = "Granos", manualUnitCost = 0.002),
                    RecipeIngredient(name = "Plátano maduro", amount = 2.0, unit = "unidades", category = "Frutas", manualUnitCost = 0.35),
                    RecipeIngredient(name = "Huevos", amount = 2.0, unit = "unidades", category = "Lácteos", manualUnitCost = 0.30),
                    RecipeIngredient(name = "Leche", amount = 60.0, unit = "ml", category = "Lácteos", manualUnitCost = 0.0015),
                    RecipeIngredient(name = "Canela en polvo", amount = 2.0, unit = "g", category = "Especias", manualUnitCost = 0.02),
                    RecipeIngredient(name = "Polvo de hornear", amount = 5.0, unit = "g", category = "Despensa", manualUnitCost = 0.01)
                ),
                instructions = listOf(
                    "Triturar en licuadora los copos de avena, plátanos, huevos, leche, canela y polvo de hornear hasta obtener una mezcla homogénea.",
                    "Calentar una sartén antiadherente a fuego medio y engrasar ligeramente.",
                    "Verter porciones de masa y cocinar 2 minutos hasta que aparezcan burbujas en la superficie.",
                    "Dar la vuelta con espátula y cocinar 1 minuto más. Servir con miel o frutas frescas."
                ),
                notes = "Usa plátanos con motas marrones para que aporten dulzor natural sin necesidad de añadir azúcar."
            )
        )

        for (recipe in samples) {
            saveRecipe(recipe)
        }

        // Also add some starter pantry items with purchase costs
        val pantrySamples = listOf(
            PantryItem(name = "Huevos", category = "Lácteos", quantity = 12.0, unit = "unidades", minQuantity = 4.0, cost = 3.60, costBaseQuantity = 12.0),
            PantryItem(name = "Aceite de oliva", category = "Despensa", quantity = 750.0, unit = "ml", minQuantity = 200.0, cost = 6.00, costBaseQuantity = 750.0),
            PantryItem(name = "Sal", category = "Especias", quantity = 1000.0, unit = "g", minQuantity = 200.0, cost = 1.00, costBaseQuantity = 1000.0),
            PantryItem(name = "Patatas", category = "Verduras", quantity = 1000.0, unit = "g", minQuantity = 500.0, cost = 2.00, costBaseQuantity = 1000.0),
            PantryItem(name = "Pasta (Penne o Fettuccine)", category = "Granos", quantity = 500.0, unit = "g", minQuantity = 200.0, cost = 1.50, costBaseQuantity = 500.0)
        )

        for (p in pantrySamples) {
            savePantryItem(p)
        }
    }
}
