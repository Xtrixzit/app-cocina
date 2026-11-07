package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PantryEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.ShoppingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int
}

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry ORDER BY name ASC")
    fun getAllPantryItems(): Flow<List<PantryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: PantryEntity)

    @Query("UPDATE pantry SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM pantry WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping ORDER BY isPurchased ASC, createdAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: ShoppingEntity)

    @Query("UPDATE shopping SET isPurchased = :isPurchased, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePurchased(id: String, isPurchased: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE shopping SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM shopping WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping WHERE isPurchased = 1")
    suspend fun deletePurchased()
}
