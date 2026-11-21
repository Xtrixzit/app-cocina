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
                            name = ingObj.optString("name", ""),
                            amount = ingObj.optDouble("amount", 0.0),
                            unit = ingObj.optString("unit", "g"),
                            category = ingObj.optString("category", "Despensa"),
                            manualUnitCost = ingObj.optDouble("manualUnitCost", 0.0)
                        )
                    )
                }
                val instructions = mutableListOf<String>()
                val instArr = obj.optJSONArray("instructions") ?: JSONArray()
                for (k in 0 until instArr.length()) {
                    instructions.add(instArr.optString(k, ""))
                }
                recipes.add(
                    Recipe(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", "Receta"),
                        category = obj.optString("category", "Almuerzos"),
                        servings = obj.optInt("servings", 4).coerceAtLeast(1),
                        prepTimeMinutes = obj.optInt("prepTimeMinutes", 0).coerceAtLeast(0),
                        cookTimeMinutes = obj.optInt("cookTimeMinutes", 0).coerceAtLeast(0),
                        difficulty = obj.optString("difficulty", "Fácil"),
                        ingredients = ingredients,
                        instructions = instructions.filter { it.isNotBlank() },
                        notes = obj.optString("notes", ""),
                        imageUrl = obj.optString("imageUrl", "")
                    )
                )
            }

            val pantry = mutableListOf<PantryItem>()
            val panArray = root.optJSONArray("pantry") ?: JSONArray()
            for (i in 0 until panArray.length()) {
                val obj = panArray.getJSONObject(i)
                pantry.add(
                    PantryItem(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        category = obj.optString("category", "Despensa"),
                        quantity = obj.optDouble("quantity", 0.0),
                        unit = obj.optString("unit", "unidades"),
                        minQuantity = obj.optDouble("minQuantity", 0.0),
                        cost = obj.optDouble("cost", 0.0),
                        costBaseQuantity = obj.optDouble("costBaseQuantity", 0.0),
                        expirationDate = obj.optString("expirationDate", "")
                    )
                )
            }

            val shopping = mutableListOf<ShoppingItem>()
            val shopArray = root.optJSONArray("shopping") ?: JSONArray()
            for (i in 0 until shopArray.length()) {
                val obj = shopArray.getJSONObject(i)
                shopping.add(
                    ShoppingItem(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        category = obj.optString("category", "Despensa"),
                        quantity = obj.optDouble("quantity", 1.0),
                        unit = obj.optString("unit", "unidades"),
                        isPurchased = obj.optBoolean("isPurchased", false),
                        recipeSource = obj.optString("recipeSource", "")
                    )
                )
            }

            val logs = mutableListOf<ProductionLog>()
            val logArray = root.optJSONArray("productionLogs") ?: JSONArray()
            for (i in 0 until logArray.length()) {
                val obj = logArray.getJSONObject(i)
                val millis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                logs.add(
                    ProductionLog(
                        id = obj.optString("id", ""),
                        recipeId = obj.optString("recipeId", ""),
                        recipeTitle = obj.optString("recipeTitle", ""),
                        targetServings = obj.optInt("targetServings", 1),
                        scaleFactor = obj.optDouble("scaleFactor", 1.0),
                        totalCost = obj.optDouble("totalCost", 0.0),
                        currencyCode = obj.optString("currencyCode", "USD"),
                        marginPercent = obj.optInt("marginPercent", 30),
                        suggestedPrice = obj.optDouble("suggestedPrice", 0.0),
                        createdAt = Timestamp(millis / 1000L, ((millis % 1000L) * 1_000_000L).toInt())
                    )
                )
            }

            FullBackupPayload(
                recipes = recipes,
                pantry = pantry,
                shopping = shopping,
                productionLogs = logs,
                currencyCode = if (root.has("currencyCode")) root.optString("currencyCode") else null,
                marginPercent = if (root.has("marginPercent")) root.optInt("marginPercent") else null
            )
        } catch (_: Exception) {
            null
        }
    }
}
