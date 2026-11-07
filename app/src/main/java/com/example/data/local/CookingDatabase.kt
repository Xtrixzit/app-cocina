package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.PantryDao
import com.example.data.local.dao.RecipeDao
import com.example.data.local.dao.ShoppingDao
import com.example.data.local.entity.PantryEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.ShoppingEntity

@Database(
    entities = [RecipeEntity::class, PantryEntity::class, ShoppingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CookingDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun pantryDao(): PantryDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: CookingDatabase? = null

        fun getDatabase(context: Context): CookingDatabase {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE != null) return INSTANCE!!

                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        CookingDatabase::class.java,
                        "cooking_pantry_database"
                    )
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()

                    // Force Room to open the SQLite file and verify checkIdentity(db) immediately
                    instance.openHelper.writableDatabase

                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    // Self-healing: if an old installation on the phone has a mismatched identity hash or corrupted schema,
                    // delete the stale database file and create a brand-new instance cleanly
                    try {
                        context.applicationContext.deleteDatabase("cooking_pantry_database")
                    } catch (_: Exception) {}

                    val freshInstance = Room.databaseBuilder(
                        context.applicationContext,
                        CookingDatabase::class.java,
                        "cooking_pantry_database"
                    )
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()

                    try {
                        freshInstance.openHelper.writableDatabase
                    } catch (_: Exception) {}

                    INSTANCE = freshInstance
                    freshInstance
                }
            }
        }
    }
}
