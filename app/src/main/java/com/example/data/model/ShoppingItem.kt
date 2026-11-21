package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class ShoppingItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Despensa",
    val quantity: Double = 0.0,
    val unit: String = "g",
    @get:PropertyName("isPurchased")
    @PropertyName("isPurchased")
    val isPurchased: Boolean = false,
    val recipeSource: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
