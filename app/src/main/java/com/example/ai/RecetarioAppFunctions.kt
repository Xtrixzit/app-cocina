package com.example.ai

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.example.R
import com.example.auth.SessionPreferences
import com.example.data.local.CookingDatabase
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.ShoppingItem
import com.example.data.repository.CookingRepository
import com.example.settings.AppSettingsPreferences
import com.example.util.LocaleHelper
import com.example.util.LocalizedLabels
import com.example.util.UnitConverter
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer

/** Pantry product stock information. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PantryStockItemInfo(
    /** Ingredient or item name. */
    val name: String,
    /** Item category. */
    val category: String,
    /** Available quantity in stock. */
    val quantity: Double,
    /** Measurement unit (e.g., g, kg, ml, l, units). */
    val unit: String,
    /** Minimum quantity threshold before restock alert. */
    val minQuantity: Double,
    /** Whether stock is low relative to minimum threshold. */
    val isLowStock: Boolean,
    /** Whether item is out of stock (quantity <= 0). */
    val isOutOfStock: Boolean,
    /** Estimated unit cost per measurement unit. */
    val estimatedUnitCost: Double
)

/** Pantry inventory query result for voice assistants without opening the app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PantrySummaryResult(
    /** Total items matched by this query. */
    val totalItemsCount: Int,
    /** Count of low-stock items. */
    val lowStockCount: Int,
    /** Count of out-of-stock items. */
    val outOfStockCount: Int,
    /** Natural-language summary ready for TTS / assistant response. */
    val summaryText: String,
    /** Detailed list of matched pantry items. */
    val items: List<PantryStockItemInfo>
)

/** Recipe production feasibility and shortage evaluation result. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class RecipeProductionResult(
    /** Official recipe title evaluated. */
    val recipeTitle: String,
    /** Target servings evaluated. */
    val targetServings: Int,
    /** Whether current pantry covers 100% of required ingredients. */
    val canProduceAll: Boolean,
    /** Total estimated ingredient cost for those servings. */
    val maxEstimatedCost: Double,
    /** Natural-language summary ready for assistant playback. */
    val summaryText: String,
    /** List of missing ingredients with quantities and units. */
    val missingIngredients: List<String>
)

/** Shopping list item details. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ShoppingItemInfo(
    /** Product name. */
    val name: String,
    /** Pantry or store category. */
    val category: String,
    /** Quantity to buy. */
    val quantity: Double,
    /** Measurement unit. */
    val unit: String,
    /** Whether already marked as purchased. */
    val isPurchased: Boolean,
    /** Source recipe or batch label if applicable. */
    val recipeSource: String
)

/** Shopping list query result for background voice assistants. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ShoppingSummaryResult(
    /** Count of returned shopping items. */
    val totalCount: Int,
    /** Total pending items remaining to purchase. */
    val pendingCount: Int,
    /** Natural-language summary for voice assistant playback. */
    val summaryText: String,
    /** Detailed list of shopping items. */
    val items: List<ShoppingItemInfo>
)

/** Recipe readiness summary based on current pantry inventory. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AvailableRecipeInfo(
    /** Recipe title. */
    val title: String,
    /** Recipe category. */
    val category: String,
    /** Base servings count. */
    val servings: Int,
    /** Total preparation + cooking time in minutes. */
    val totalTimeMinutes: Int,
    /** Percentage of available ingredients in pantry (0-100). */
    val matchPercentage: Int,
    /** Missing ingredients for base servings. */
    val missingIngredients: List<String>
)

/** Available or nearly ready recipes query result. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AvailableRecipesResult(
    /** Total recipes evaluated. */
    val totalRecipesCount: Int,
    /** Count of recipes 100% ready to cook right now. */
    val readyNowCount: Int,
    /** Natural-language summary for assistant playback. */
    val summaryText: String,
    /** Recipes ordered from highest to lowest ingredient readiness. */
    val recipes: List<AvailableRecipeInfo>
)

/**
 * Background data provider via Android AppFunctions.
 * Allows any compatible voice assistant or system AI agent on Android to query pantry,
 * low stock alerts, shopping list, and recipe feasibility without opening the app.
 */
class RecetarioAppFunctions {

    private fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private sealed interface ActiveSource {
        data class GoogleCloud(val user: FirebaseUser) : ActiveSource
        data object LocalGuest : ActiveSource
    }

    private fun resolveActiveSource(appFunctionContext: AppFunctionContext): ActiveSource {
        val appContext = appFunctionContext.context.applicationContext
        val loc = LocaleHelper.localizedContext(appContext)
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        val user = Firebase.auth.currentUser
        val isGuest = SessionPreferences.isGuestMode(appContext)
        return when {
            !isGuest && user != null -> ActiveSource.GoogleCloud(user)
            isGuest && user == null -> ActiveSource.LocalGuest
            user != null -> ActiveSource.GoogleCloud(user)
            else -> throw AppFunctionElementNotFoundException(
                loc.getString(R.string.af_auth_required)
            )
        }
    }

    private suspend fun loadPantryItemsForSource(
        appFunctionContext: AppFunctionContext,
        source: ActiveSource
    ): List<PantryItem> = when (source) {
        is ActiveSource.GoogleCloud -> loadCloudPantryItems(appFunctionContext, source.user.uid)
        is ActiveSource.LocalGuest -> withContext(Dispatchers.IO) {
            CookingDatabase.getDatabase(appFunctionContext.context.applicationContext)
                .pantryDao()
                .getAllPantryItemsList()
                .map { it.toDomain() }
        }
    }

    private suspend fun loadRecipesForSource(
        appFunctionContext: AppFunctionContext,
        source: ActiveSource
    ): List<Recipe> = when (source) {
        is ActiveSource.GoogleCloud -> loadCloudRecipes(appFunctionContext, source.user.uid)
        is ActiveSource.LocalGuest -> withContext(Dispatchers.IO) {
            CookingDatabase.getDatabase(appFunctionContext.context.applicationContext)
                .recipeDao()
                .getAllRecipesList()
                .map { it.toDomain() }
        }
    }

    private suspend fun loadShoppingItemsForSource(
        appFunctionContext: AppFunctionContext,
        source: ActiveSource
    ): List<ShoppingItem> = when (source) {
        is ActiveSource.GoogleCloud -> loadCloudShoppingItems(appFunctionContext, source.user.uid)
        is ActiveSource.LocalGuest -> withContext(Dispatchers.IO) {
            CookingDatabase.getDatabase(appFunctionContext.context.applicationContext)
                .shoppingDao()
                .getAllShoppingItemsList()
                .map { it.toDomain() }
        }
    }

    private suspend fun loadCloudPantryItems(
        appFunctionContext: AppFunctionContext,
        uid: String
    ): List<PantryItem> = withContext(Dispatchers.IO) {
        val appContext = appFunctionContext.context.applicationContext
        val firestore = CookingRepository.createConfiguredFirestore(appContext)
        val col = firestore.collection("users").document(uid).collection("pantry")

        val serverList = withTimeoutOrNull(4000L) {
            try {
                col.get(Source.SERVER).await()
                    .toObjects(PantryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (_: Exception) {
                null
            }
        }
        if (serverList != null) return@withContext serverList

        col.get(Source.CACHE).await()
            .toObjects(PantryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    private suspend fun loadCloudRecipes(
        appFunctionContext: AppFunctionContext,
        uid: String
    ): List<Recipe> = withContext(Dispatchers.IO) {
        val appContext = appFunctionContext.context.applicationContext
        val firestore = CookingRepository.createConfiguredFirestore(appContext)
        val col = firestore.collection("users").document(uid).collection("recipes")

        val serverList = withTimeoutOrNull(4000L) {
            try {
                col.get(Source.SERVER).await()
                    .toObjects(Recipe::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (_: Exception) {
                null
            }
        }
        if (serverList != null) return@withContext serverList

        col.get(Source.CACHE).await()
            .toObjects(Recipe::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    private suspend fun loadCloudShoppingItems(
        appFunctionContext: AppFunctionContext,
        uid: String
    ): List<ShoppingItem> = withContext(Dispatchers.IO) {
        val appContext = appFunctionContext.context.applicationContext
        val firestore = CookingRepository.createConfiguredFirestore(appContext)
        val col = firestore.collection("users").document(uid).collection("shopping_items")

        val serverList = withTimeoutOrNull(4000L) {
            try {
                col.get(Source.SERVER).await()
                    .toObjects(ShoppingItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (_: Exception) {
                null
            }
        }
        if (serverList != null) return@withContext serverList

        col.get(Source.CACHE).await()
            .toObjects(ShoppingItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    private fun PantryItem.toInfo() = PantryStockItemInfo(
        name = name,
        category = category,
        quantity = quantity,
        unit = unit,
        minQuantity = minQuantity,
        isLowStock = isLowStock,
        isOutOfStock = isOutOfStock,
        estimatedUnitCost = unitCost
    )

    private fun ShoppingItem.toInfo() = ShoppingItemInfo(
        name = name,
        category = category,
        quantity = quantity,
        unit = unit,
        isPurchased = isPurchased,
        recipeSource = recipeSource
    )

    private fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else "%.2f".format(qty)
    }

    private fun sourceLabel(loc: android.content.Context, source: ActiveSource): String = when (source) {
        is ActiveSource.GoogleCloud -> source.user.email ?: loc.getString(R.string.af_src_google_default)
        is ActiveSource.LocalGuest -> loc.getString(R.string.af_src_guest)
    }

    /**
     * Queries current pantry inventory without opening the app.
     *
     * @param appFunctionContext AppFunctions execution context.
     * @param nombreIngrediente Optional ingredient name to search; when blank returns whole pantry.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun consultarStockDespensa(
        appFunctionContext: AppFunctionContext,
        nombreIngrediente: String? = null
    ): PantrySummaryResult = withContext(Dispatchers.IO) {
        val loc = LocaleHelper.localizedContext(appFunctionContext.context)
        val source = resolveActiveSource(appFunctionContext)
        val allItems = loadPantryItemsForSource(appFunctionContext, source)
        val rawQuery = nombreIngrediente.orEmpty().trim()
        val query = normalizeText(rawQuery)
        val matched = if (query.isBlank()) {
            allItems
        } else {
            allItems.filter {
                val itemNorm = normalizeText(it.name)
                val catNorm = normalizeText(it.category)
                itemNorm.contains(query) || query.contains(itemNorm) || catNorm.contains(query)
            }
        }

        val sLabel = sourceLabel(loc, source)
        if (query.isNotBlank() && matched.isEmpty()) {
            throw AppFunctionElementNotFoundException(
                loc.getString(R.string.af_pantry_not_found, rawQuery, sLabel, allItems.size)
            )
        }

        val lowCount = matched.count { it.isLowStock }
        val outCount = matched.count { it.isOutOfStock }
        val summary = if (matched.isEmpty()) {
            loc.getString(R.string.af_pantry_empty, sLabel)
        } else if (query.isNotBlank()) {
            matched.joinToString(". ") { item ->
                val uDisp = LocalizedLabels.unit(loc, item.unit)
                val stateTag = when {
                    item.isOutOfStock -> loc.getString(R.string.af_pantry_out_tag)
                    item.isLowStock -> loc.getString(R.string.af_pantry_low_tag, formatQuantity(item.minQuantity), uDisp)
                    else -> ""
                }
                "${item.name}: ${formatQuantity(item.quantity)} $uDisp$stateTag"
            }
        } else {
            val preview = matched.take(12).joinToString(", ") {
                "${it.name}: ${formatQuantity(it.quantity)} ${LocalizedLabels.unit(loc, it.unit)}"
            }
            val extra = if (matched.size > 12) loc.getString(R.string.af_more_items, matched.size - 12) else ""
            loc.getString(R.string.af_pantry_summary, matched.size, sLabel, lowCount, outCount, preview, extra)
        }

        PantrySummaryResult(
            totalItemsCount = matched.size,
            lowStockCount = lowCount,
            outOfStockCount = outCount,
            summaryText = summary,
            items = matched.map { it.toInfo() }
        )
    }

    /**
     * Lists pantry items that are out of stock or below minimum restock threshold.
     *
     * @param appFunctionContext AppFunctions execution context.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listarStockBajoDespensa(
        appFunctionContext: AppFunctionContext
    ): PantrySummaryResult = withContext(Dispatchers.IO) {
        val loc = LocaleHelper.localizedContext(appFunctionContext.context)
        val source = resolveActiveSource(appFunctionContext)
        val allItems = loadPantryItemsForSource(appFunctionContext, source)
        val alerts = allItems.filter { it.isOutOfStock || it.isLowStock }
        val lowCount = alerts.count { it.isLowStock }
        val outCount = alerts.count { it.isOutOfStock }
        val sLabel = sourceLabel(loc, source)

        val summary = if (alerts.isEmpty()) {
            loc.getString(R.string.af_low_all_ok, allItems.size, sLabel)
        } else {
            val details = alerts.joinToString("; ") {
                val uDisp = LocalizedLabels.unit(loc, it.unit)
                "${it.name}: ${formatQuantity(it.quantity)} $uDisp (min ${formatQuantity(it.minQuantity)} $uDisp)"
            }
            loc.getString(R.string.af_low_summary, alerts.size, sLabel, outCount, lowCount, details)
        }

        PantrySummaryResult(
            totalItemsCount = alerts.size,
            lowStockCount = lowCount,
            outOfStockCount = outCount,
            summaryText = summary,
            items = alerts.map { it.toInfo() }
        )
    }

    /**
     * Evaluates recipe production feasibility against pantry stock (unit-aware) and estimates cost.
     *
     * @param appFunctionContext AppFunctions execution context.
     * @param nombreReceta Partial or full recipe title.
     * @param porcionesDeseadas Target servings (0 uses recipe base servings).
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun verificarProduccionReceta(
        appFunctionContext: AppFunctionContext,
        nombreReceta: String,
        porcionesDeseadas: Int = 0
    ): RecipeProductionResult = withContext(Dispatchers.IO) {
        val loc = LocaleHelper.localizedContext(appFunctionContext.context)
        if (nombreReceta.isBlank()) {
            throw AppFunctionInvalidArgumentException(loc.getString(R.string.af_recipe_empty_arg))
        }
        val source = resolveActiveSource(appFunctionContext)
        val recipes = loadRecipesForSource(appFunctionContext, source)
        val query = normalizeText(nombreReceta)
        val recipe = recipes.firstOrNull {
            val titleNorm = normalizeText(it.title)
            titleNorm.contains(query) || query.contains(titleNorm)
        } ?: throw AppFunctionElementNotFoundException(loc.getString(R.string.af_recipe_not_found, nombreReceta))

        val pantry = loadPantryItemsForSource(appFunctionContext, source)
        val target = if (porcionesDeseadas > 0) porcionesDeseadas else recipe.servings.coerceAtLeast(1)
        val scale = target.toDouble() / recipe.servings.coerceAtLeast(1)

        var totalCost = 0.0
        val missingList = mutableListOf<String>()
        for (ing in recipe.ingredients) {
            val needed = ing.amount * scale
            val pantryMatch = pantry.firstOrNull {
                normalizeText(it.name) == normalizeText(ing.name)
            }
            val stockInRecipeUnit = UnitConverter.availableInTargetUnit(pantryMatch, ing.unit)
            if (stockInRecipeUnit + 0.001 < needed) {
                val shortage = (needed - stockInRecipeUnit).coerceAtLeast(0.0)
                val uDisp = LocalizedLabels.unit(loc, ing.unit)
                missingList.add(loc.getString(R.string.af_missing_item_fmt, ing.name, formatQuantity(shortage), uDisp))
            }
            val pantryConvertedUnitCost = UnitConverter.unitCostInTargetUnit(pantryMatch, ing.unit)
            val unitCost = when {
                pantryConvertedUnitCost > 0.0 -> pantryConvertedUnitCost
                ing.manualUnitCost > 0.0 -> ing.manualUnitCost
                else -> 0.0
            }
            totalCost += needed * unitCost
        }

        val canProduce = missingList.isEmpty()
        val sLabel = sourceLabel(loc, source)
        val currency = AppSettingsPreferences.getCurrency(appFunctionContext.context)
        val costText = AppSettingsPreferences.formatMoney(totalCost, currency)
        val summary = if (canProduce) {
            loc.getString(R.string.af_prod_can_produce, target, recipe.title, sLabel, costText)
        } else {
            loc.getString(R.string.af_prod_cannot_produce, target, recipe.title, missingList.joinToString(", "), costText)
        }

        RecipeProductionResult(
            recipeTitle = recipe.title,
            targetServings = target,
            canProduceAll = canProduce,
            maxEstimatedCost = totalCost,
            summaryText = summary,
            missingIngredients = missingList
        )
    }

    /**
     * Queries the user's shopping list (pending or all) without opening the app.
     *
     * @param appFunctionContext AppFunctions execution context.
     * @param soloPendientes When true (default), returns only unpurchased items.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun consultarListaCompras(
        appFunctionContext: AppFunctionContext,
        soloPendientes: Boolean = true
    ): ShoppingSummaryResult = withContext(Dispatchers.IO) {
        val loc = LocaleHelper.localizedContext(appFunctionContext.context)
        val source = resolveActiveSource(appFunctionContext)
        val allShopping = loadShoppingItemsForSource(appFunctionContext, source)
        val pendingCount = allShopping.count { !it.isPurchased }
        val filtered = if (soloPendientes) allShopping.filter { !it.isPurchased } else allShopping
        val sLabel = sourceLabel(loc, source)
        val purchasedPrefix = loc.getString(R.string.af_shop_purchased_prefix)

        val summary = when {
            allShopping.isEmpty() -> loc.getString(R.string.af_shop_empty, sLabel)
            soloPendientes && pendingCount == 0 -> loc.getString(R.string.af_shop_none_pending, sLabel, allShopping.size)
            else -> {
                val preview = filtered.take(15).joinToString(", ") {
                    val donePrefix = if (it.isPurchased) purchasedPrefix else ""
                    val uDisp = LocalizedLabels.unit(loc, it.unit)
                    "$donePrefix${it.name}: ${formatQuantity(it.quantity)} $uDisp"
                }
                val extra = if (filtered.size > 15) loc.getString(R.string.af_more_items, filtered.size - 15) else ""
                if (soloPendientes) {
                    loc.getString(R.string.af_shop_summary_pending, sLabel, pendingCount, preview, extra)
                } else {
                    loc.getString(R.string.af_shop_summary_all, sLabel, filtered.size, pendingCount, preview, extra)
                }
            }
        }

        ShoppingSummaryResult(
            totalCount = filtered.size,
            pendingCount = pendingCount,
            summaryText = summary,
            items = filtered.map { it.toInfo() }
        )
    }

    /**
     * Evaluates which saved recipes can be cooked right now using current pantry inventory.
     *
     * @param appFunctionContext AppFunctions execution context.
     * @param categoria Optional recipe category filter.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listarRecetasDisponibles(
        appFunctionContext: AppFunctionContext,
        categoria: String? = null
    ): AvailableRecipesResult = withContext(Dispatchers.IO) {
        val loc = LocaleHelper.localizedContext(appFunctionContext.context)
        val source = resolveActiveSource(appFunctionContext)
        val allRecipes = loadRecipesForSource(appFunctionContext, source)
        val pantry = loadPantryItemsForSource(appFunctionContext, source)

        val catQuery = normalizeText(categoria.orEmpty())
        val scopedRecipes = if (catQuery.isBlank() || catQuery == "todas" || catQuery == "all" || catQuery == "toutes") {
            allRecipes
        } else {
            allRecipes.filter {
                val canonicalNorm = normalizeText(it.category)
                val localizedNorm = normalizeText(LocalizedLabels.recipeCategory(loc, it.category))
                canonicalNorm.contains(catQuery) || localizedNorm.contains(catQuery)
            }
        }

        val evaluated = scopedRecipes.map { recipe ->
            var available = 0
            val missing = mutableListOf<String>()
            for (ing in recipe.ingredients) {
                val match = pantry.firstOrNull { normalizeText(it.name) == normalizeText(ing.name) }
                val hasQty = UnitConverter.availableInTargetUnit(match, ing.unit)
                if (hasQty + 0.0001 >= ing.amount) {
                    available++
                } else {
                    val shortage = (ing.amount - hasQty).coerceAtLeast(0.0)
                    val uDisp = LocalizedLabels.unit(loc, ing.unit)
                    missing.add(loc.getString(R.string.af_missing_item_fmt, ing.name, formatQuantity(shortage), uDisp))
                }
            }
            val total = recipe.ingredients.size
            val pct = if (total == 0) 100 else ((available.toDouble() / total) * 100).toInt()
            AvailableRecipeInfo(
                title = recipe.title,
                category = LocalizedLabels.recipeCategory(loc, recipe.category),
                servings = recipe.servings,
                totalTimeMinutes = recipe.totalTimeMinutes,
                matchPercentage = pct,
                missingIngredients = missing
            )
        }.sortedByDescending { it.matchPercentage }

        val readyNow = evaluated.filter { it.matchPercentage == 100 }
        val almostReady = evaluated.filter { it.matchPercentage in 50..99 }

        val summary = when {
            evaluated.isEmpty() -> if (catQuery.isBlank()) {
                loc.getString(R.string.af_avail_none)
            } else {
                loc.getString(R.string.af_avail_none_cat, categoria.orEmpty())
            }
            readyNow.isNotEmpty() -> {
                val readyNames = readyNow.take(8).joinToString(", ") {
                    "${it.title} (${loc.getString(R.string.recipe_card_servings, it.servings)})"
                }
                val almostText = if (almostReady.isNotEmpty()) {
                    val almostList = almostReady.take(4).joinToString(", ") { "${it.title} (${it.matchPercentage}%)" }
                    loc.getString(R.string.af_avail_almost_note, almostList)
                } else ""
                loc.getString(R.string.af_avail_ready, readyNow.size, readyNames, almostText)
            }
            almostReady.isNotEmpty() -> {
                val almostText = almostReady.take(5).joinToString("; ") {
                    "${it.title} (${it.matchPercentage}%: ${it.missingIngredients.take(2).joinToString(", ")})"
                }
                loc.getString(R.string.af_avail_almost, almostText)
            }
            else -> loc.getString(R.string.af_avail_low, evaluated.size)
        }

        AvailableRecipesResult(
            totalRecipesCount = evaluated.size,
            readyNowCount = readyNow.size,
            summaryText = summary,
            recipes = evaluated
        )
    }
}
