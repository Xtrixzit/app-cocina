package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val servings: Int,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val difficulty: String,
    val ingredientsJson: String,
    val instructionsJson: String,
    val notes: String,
    val imageUrl: String = "",
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Recipe {
        return Recipe(
            id = id,
            title = title,
            category = category,
            servings = servings,
            prepTimeMinutes = prepTimeMinutes,
            cookTimeMinutes = cookTimeMinutes,
            difficulty = difficulty,
            ingredients = jsonToIngredients(ingredientsJson),
            instructions = jsonToInstructions(instructionsJson),
            notes = notes,
            imageUrl = imageUrl
        )
    }

    companion object {
        fun fromDomain(recipe: Recipe): RecipeEntity {
            return RecipeEntity(
                id = recipe.id,
                title = recipe.title,
                category = recipe.category,
                servings = recipe.servings,
                prepTimeMinutes = recipe.prepTimeMinutes,
                cookTimeMinutes = recipe.cookTimeMinutes,
                difficulty = recipe.difficulty,
                ingredientsJson = ingredientsToJson(recipe.ingredients),
                instructionsJson = instructionsToJson(recipe.instructions),
                notes = recipe.notes,
                imageUrl = recipe.imageUrl,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        fun ingredientsToJson(list: List<RecipeIngredient>): String {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("name", item.name)
                obj.put("amount", item.amount)
                obj.put("unit", item.unit)
                obj.put("category", item.category)
                obj.put("manualUnitCost", item.manualUnitCost)
                array.put(obj)
            }
            return array.toString()
        }

        fun jsonToIngredients(json: String): List<RecipeIngredient> {
            if (json.isBlank()) return emptyList()
            val list = mutableListOf<RecipeIngredient>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RecipeIngredient(
                            name = obj.optString("name", ""),
                            amount = obj.optDouble("amount", 0.0),
                            unit = obj.optString("unit", "g"),
                            category = obj.optString("category", "Despensa"),
                            manualUnitCost = obj.optDouble("manualUnitCost", 0.0)
                        )
                    )
                }
            } catch (_: Exception) {}
            return list
        }

        fun instructionsToJson(list: List<String>): String {
            val array = JSONArray()
            for (item in list) array.put(item)
            return array.toString()
        }

        fun jsonToInstructions(json: String): List<String> {
            if (json.isBlank()) return emptyList()
            val list = mutableListOf<String>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
            } catch (_: Exception) {}
            return list
        }
    }
}

@Entity(tableName = "pantry")
data class PantryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val minQuantity: Double,
    val cost: Double = 0.0,
    val costBaseQuantity: Double = 0.0,
    val expirationDate: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): PantryItem {
        return PantryItem(
            id = id,
            name = name,
            category = category,
            quantity = quantity,
            unit = unit,
            minQuantity = minQuantity,
            cost = cost,
            costBaseQuantity = costBaseQuantity,
            expirationDate = expirationDate
        )
    }

    companion object {
        fun fromDomain(item: PantryItem): PantryEntity {
            return PantryEntity(
                id = item.id,
                name = item.name,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit,
                minQuantity = item.minQuantity,
                cost = item.cost,
                costBaseQuantity = item.costBaseQuantity,
                expirationDate = item.expirationDate,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(tableName = "shopping")
data class ShoppingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val isPurchased: Boolean,
    val recipeSource: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): com.example.data.model.ShoppingItem {
        return com.example.data.model.ShoppingItem(
            id = id,
            name = name,
            category = category,
            quantity = quantity,
            unit = unit,
            isPurchased = isPurchased,
            recipeSource = recipeSource
        )
    }

    companion object {
        fun fromDomain(item: com.example.data.model.ShoppingItem): ShoppingEntity {
            return ShoppingEntity(
                id = item.id,
                name = item.name,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit,
                isPurchased = item.isPurchased,
                recipeSource = item.recipeSource,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
