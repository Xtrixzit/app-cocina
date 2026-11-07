package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PantryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Despensa",
    val quantity: Double = 0.0,
    val unit: String = "g",
    val minQuantity: Double = 0.0,
    val cost: Double = 0.0, // Purchase price paid (e.g. $2.50)
    val costBaseQuantity: Double = 0.0, // The quantity the cost applies to (e.g. 1000g). If 0, uses quantity.
    val expirationDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isOutOfStock: Boolean
        get() = quantity <= 0.001

    val isLowStock: Boolean
        get() = !isOutOfStock && minQuantity > 0.0 && quantity <= minQuantity

    /**
     * Calculated unit price (cost per 1 unit of measurement, e.g. per gram, per unit, per ml)
     */
    val unitCost: Double
        get() {
            if (cost <= 0.0) return 0.0
            val base = if (costBaseQuantity > 0.0) costBaseQuantity else if (quantity > 0.0) quantity else 1.0
            return cost / base
        }
}
