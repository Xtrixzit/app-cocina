package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ProductionLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recipeId: String = "",
    val recipeTitle: String = "",
    val targetServings: Int = 1,
    val scaleFactor: Double = 1.0,
    val totalCost: Double = 0.0,
    val currencyCode: String = "USD",
    val marginPercent: Int = 0,
    val suggestedPrice: Double = 0.0,
    val deductedIngredientsCount: Int = 0,
    val createdAt: Timestamp? = null
) {
    val estimatedProfit: Double
        get() = (suggestedPrice - totalCost).coerceAtLeast(0.0)
}

@Keep
@IgnoreExtraProperties
data class DistributionConfig(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val minRequiredVersionCode: Int = 0,
    val updateUrl: String = "",
    val updateNotes: String = "",
    val requireLicense: Boolean = false,
    val supportContact: String = "",
    val githubRepo: String = ""
)

@Keep
@IgnoreExtraProperties
data class LicenseRecord(
    @DocumentId val code: String = "",
    val active: Boolean = false,
    val assignedUid: String = "",
    val assignedEmail: String = "",
    val activatedAt: Timestamp? = null,
    val tier: String = "pro",
    val notes: String = ""
)
