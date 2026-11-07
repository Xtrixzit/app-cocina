package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RecipeIngredient(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String = "g",
    val category: String = "Despensa",
    val manualUnitCost: Double = 0.0 // Cost per unit (e.g. $0.005/g or $0.50/egg)
)

@IgnoreExtraProperties
data class Recipe(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Almuerzo",
    val servings: Int = 4,
    val prepTimeMinutes: Int = 15,
    val cookTimeMinutes: Int = 30,
    val difficulty: String = "Fácil",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    val notes: String = "",
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalTimeMinutes: Int
        get() = prepTimeMinutes + cookTimeMinutes
}
