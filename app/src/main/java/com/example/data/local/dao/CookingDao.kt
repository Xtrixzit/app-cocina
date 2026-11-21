package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PantryEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.ShoppingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    suspend fun getAllRecipesList(): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int
}

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry ORDER BY name ASC")
    fun getAllPantryItems(): Flow<List<PantryEntity>>

    @Query("SELECT * FROM pantry ORDER BY name ASC")
    suspend fun getAllPantryItemsList(): List<PantryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: PantryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PantryEntity>)

    @Query("UPDATE pantry SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM pantry WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pantry")
    suspend fun deleteAll()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping ORDER BY isPurchased ASC, createdAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingEntity>>

    @Query("SELECT * FROM shopping ORDER BY isPurchased ASC, createdAt DESC")
    suspend fun getAllShoppingItemsList(): List<ShoppingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: ShoppingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingEntity>)

    @Query("UPDATE shopping SET isPurchased = :isPurchased, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePurchased(id: String, isPurchased: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE shopping SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM shopping WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping WHERE isPurchased = 1")
    suspend fun deletePurchased()

    @Query("DELETE FROM shopping")
    suspend fun deleteAll()
}

@Dao
interface ProductionLogDao {
    @Query("SELECT * FROM production_logs ORDER BY createdAt DESC LIMIT 100")
    fun getAllLogs(): Flow<List<com.example.data.local.entity.ProductionLogEntity>>

    @Query("SELECT * FROM production_logs ORDER BY createdAt DESC")
    suspend fun getAllLogsList(): List<com.example.data.local.entity.ProductionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: com.example.data.local.entity.ProductionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<com.example.data.local.entity.ProductionLogEntity>)

    @Query("DELETE FROM production_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM production_logs")
    suspend fun deleteAll()
}

