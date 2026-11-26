package com.example.data.sync

import com.example.data.model.PantryItem
import com.example.data.model.ProductionLog
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.ShoppingItem
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject

data class FullBackupPayload(
    val recipes: List<Recipe>,
    val pantry: List<PantryItem>,
    val shopping: List<ShoppingItem>,
    val productionLogs: List<ProductionLog>,
    val currencyCode: String? = null,
    val marginPercent: Int? = null
)

object FullBackupManager {

    const val BACKUP_PREFIX = "RECETARIO_BACKUP_V2:"

    fun exportToJson(
        recipes: List<Recipe>,
        pantry: List<PantryItem>,
        shopping: List<ShoppingItem>,
        productionLogs: List<ProductionLog>,
        currencyCode: String,
        marginPercent: Int
    ): String {
        val root = JSONObject().apply {
            put("format", "recetario_backup_v2")
            put("exportedAtMillis", System.currentTimeMillis())
            put("currencyCode", currencyCode)
            put("marginPercent", marginPercent)

            put("recipes", JSONArray().apply {
                recipes.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("category", r.category)
                        put("servings", r.servings)
                        put("prepTimeMinutes", r.prepTimeMinutes)
                        put("cookTimeMinutes", r.cookTimeMinutes)
                        put("difficulty", r.difficulty)
                        put("notes", r.notes)
                        put("imageUrl", r.imageUrl)
                        put("ingredients", JSONArray().apply {
                            r.ingredients.forEach { ing ->
                                put(JSONObject().apply {
                                    put("name", ing.name)
                                    put("amount", ing.amount)
                                    put("unit", ing.unit)
                                    put("category", ing.category)
                                    put("manualUnitCost", ing.manualUnitCost)
                                })
                            }
                        })
                        put("instructions", JSONArray().apply {
                            r.instructions.forEach { put(it) }
                        })
                    })
                }
            })

            put("pantry", JSONArray().apply {
                pantry.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("category", p.category)
                        put("quantity", p.quantity)
                        put("unit", p.unit)
                        put("minQuantity", p.minQuantity)
                        put("cost", p.cost)
                        put("costBaseQuantity", p.costBaseQuantity)
                        put("expirationDate", p.expirationDate)
                    })
                }
            })

            put("shopping", JSONArray().apply {
                shopping.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("category", s.category)
                        put("quantity", s.quantity)
                        put("unit", s.unit)
                        put("isPurchased", s.isPurchased)
                        put("recipeSource", s.recipeSource)
                    })
                }
            })

            put("productionLogs", JSONArray().apply {
                productionLogs.forEach { log ->
                    put(JSONObject().apply {
                        put("id", log.id)
                        put("recipeId", log.recipeId)
                        put("recipeTitle", log.recipeTitle)
                        put("targetServings", log.targetServings)
                        put("scaleFactor", log.scaleFactor)
                        put("totalCost", log.totalCost)
                        put("currencyCode", log.currencyCode)
                        put("marginPercent", log.marginPercent)
                        put("suggestedPrice", log.suggestedPrice)
                        put("createdAtMillis", log.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun parseFromJson(rawText: String): FullBackupPayload? {
        val cleaned = rawText.trim().let {
            if (it.startsWith(BACKUP_PREFIX)) it.removePrefix(BACKUP_PREFIX).trim() else it
        }
        if (!cleaned.startsWith("{")) return null

        return try {
            val root = JSONObject(cleaned)
            if (!root.has("recipes") && !root.has("pantry")) return null

            val recipes = mutableListOf<Recipe>()
            val recArray = root.optJSONArray("recipes") ?: JSONArray()
            for (i in 0 until recArray.length()) {
                val obj = recArray.getJSONObject(i)
                val ingredients = mutableListOf<RecipeIngredient>()
                val ingArr = obj.optJSONArray("ingredients") ?: JSONArray()
                for (j in 0 until ingArr.length()) {
                    val ingObj = ingArr.getJSONObject(j)
                    ingredients.add(
                        RecipeIngredient(
                            name = ingObj.optString("name", "").trim().take(100),
                            amount = com.example.util.SecurityUtils.clampNonNegative(ingObj.optDouble("amount", 0.0)),
                            unit = ingObj.optString("unit", "g").trim().ifBlank { "g" }.take(30),
                            category = ingObj.optString("category", "Despensa").trim().ifBlank { "Despensa" }.take(50),
                            manualUnitCost = com.example.util.SecurityUtils.clampNonNegative(ingObj.optDouble("manualUnitCost", 0.0))
                        )
                    )
                }
                val instructions = mutableListOf<String>()
                val instArr = obj.optJSONArray("instructions") ?: JSONArray()
                for (k in 0 until instArr.length()) {
                    instructions.add(instArr.optString(k, "").trim().take(1000))
                }
                recipes.add(
                    Recipe(
                        id = obj.optString("id", "").trim().take(128),
                        title = obj.optString("title", "Receta").trim().ifBlank { "Receta" }.take(200),
                        category = obj.optString("category", "Almuerzos").trim().ifBlank { "Almuerzos" }.take(50),
                        servings = obj.optInt("servings", 4).coerceIn(1, 1000),
                        prepTimeMinutes = obj.optInt("prepTimeMinutes", 0).coerceIn(0, 1440),
                        cookTimeMinutes = obj.optInt("cookTimeMinutes", 0).coerceIn(0, 1440),
                        difficulty = obj.optString("difficulty", "Fácil").trim().ifBlank { "Fácil" }.take(30),
                        ingredients = ingredients.take(150),
                        instructions = instructions.filter { it.isNotBlank() }.take(150),
                        notes = obj.optString("notes", "").take(5000),
                        imageUrl = com.example.util.SecurityUtils.sanitizeImageUri(obj.optString("imageUrl", ""))
                    )
                )
            }

            val pantry = mutableListOf<PantryItem>()
            val panArray = root.optJSONArray("pantry") ?: JSONArray()
            for (i in 0 until panArray.length()) {
                val obj = panArray.getJSONObject(i)
                pantry.add(
                    PantryItem(
                        id = obj.optString("id", "").trim().take(128),
                        name = obj.optString("name", "").trim().take(100),
                        category = obj.optString("category", "Despensa").trim().ifBlank { "Despensa" }.take(50),
                        quantity = com.example.util.SecurityUtils.clampNonNegative(obj.optDouble("quantity", 0.0)),
                        unit = obj.optString("unit", "unidades").trim().ifBlank { "unidades" }.take(30),
                        minQuantity = com.example.util.SecurityUtils.clampNonNegative(obj.optDouble("minQuantity", 0.0)),
                        cost = com.example.util.SecurityUtils.clampNonNegative(obj.optDouble("cost", 0.0)),
                        costBaseQuantity = com.example.util.SecurityUtils.clampNonNegative(obj.optDouble("costBaseQuantity", 0.0)),
                        expirationDate = obj.optString("expirationDate", "").trim().take(50)
                    )
                )
            }

            val shopping = mutableListOf<ShoppingItem>()
            val shopArray = root.optJSONArray("shopping") ?: JSONArray()
            for (i in 0 until shopArray.length()) {
                val obj = shopArray.getJSONObject(i)
                shopping.add(
                    ShoppingItem(
                        id = obj.optString("id", "").trim().take(128),
                        name = obj.optString("name", "").trim().take(100),
                        category = obj.optString("category", "Despensa").trim().ifBlank { "Despensa" }.take(50),
                        quantity = com.example.util.SecurityUtils.clampNonNegative(obj.optDouble("quantity", 1.0)),
                        unit = obj.optString("unit", "unidades").trim().ifBlank { "unidades" }.take(30),
                        isPurchased = obj.optBoolean("isPurchased", false),
                        recipeSource = obj.optString("recipeSource", "").trim().take(200)
                    )
                )
            }

            val logs = mutableListOf<ProductionLog>()
            val logArray = root.optJSONArray("productionLogs") ?: JSONArray()
            for (i in 0 until logArray.length()) {
                val obj = logArray.getJSONObject(i)
                val millis = obj.optLong("createdAtMillis", System.currentTimeMillis()).coerceAtLeast(0L)
                logs.add(
                    ProductionLog(
                        id = obj.optString("id", "").trim().take(128),
                        recipeId = obj.optString("recipeId", "").trim().take(128),
                        recipeTitle = obj.optString("recipeTitle", "").trim().take(200),
                        targetServings = obj.optInt("targetServings", 1).coerceIn(1, 100_000),
                        scaleFactor = com.example.util.SecurityUtils.clampPositive(obj.optDouble("scaleFactor", 1.0), min = 0.01, max = 10_000.0, fallback = 1.0),
                        totalCost = com.example.util.SecurityUtils.clampNonNegative(obj.optDouble("totalCost", 0.0), com.example.util.SecurityUtils.MAX_PRODUCTION_CURRENCY_VALUE),
                        currencyCode = obj.optString("currencyCode", "USD").trim().ifBlank { "USD" }.take(16),
                        marginPercent = com.example.util.SecurityUtils.clampMarginPercent(obj.optInt("marginPercent", 0)),
                        suggestedPrice = com.example.util.SecurityUtils.clampNonNegative(obj.optDouble("suggestedPrice", 0.0), com.example.util.SecurityUtils.MAX_PRODUCTION_CURRENCY_VALUE),
                        createdAt = Timestamp(millis / 1000L, ((millis % 1000L) * 1_000_000L).toInt())
                    )
                )
            }

            FullBackupPayload(
                recipes = recipes,
                pantry = pantry,
                shopping = shopping,
                productionLogs = logs,
                currencyCode = if (root.has("currencyCode")) root.optString("currencyCode").trim().take(16) else null,
                marginPercent = if (root.has("marginPercent")) com.example.util.SecurityUtils.clampMarginPercent(root.optInt("marginPercent")) else null
            )
        } catch (_: Exception) {
            null
        }
    }
}
