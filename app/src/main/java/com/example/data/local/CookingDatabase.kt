package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.PantryDao
import com.example.data.local.dao.ProductionLogDao
import com.example.data.local.dao.RecipeDao
import com.example.data.local.dao.ShoppingDao
import com.example.data.local.entity.PantryEntity
import com.example.data.local.entity.ProductionLogEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.ShoppingEntity

@Database(
    entities = [
        RecipeEntity::class,
        PantryEntity::class,
        ShoppingEntity::class,
        ProductionLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class CookingDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun pantryDao(): PantryDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun productionLogDao(): ProductionLogDao

    companion object {
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `production_logs` (
                        `id` TEXT NOT NULL,
                        `recipeId` TEXT NOT NULL,
                        `recipeTitle` TEXT NOT NULL,
                        `targetServings` INTEGER NOT NULL,
                        `scaleFactor` REAL NOT NULL,
                        `totalCost` REAL NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `marginPercent` INTEGER NOT NULL,
                        `suggestedPrice` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

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
                        .addMigrations(MIGRATION_5_6)
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()

                    val db = instance.openHelper.writableDatabase
                    db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT count(*) FROM sqlite_master")).use { it.moveToFirst() }

                    INSTANCE = instance
                    instance
                } catch (_: Exception) {
                    try {
                        context.applicationContext.deleteDatabase("cooking_pantry_database")
                    } catch (_: Exception) {}

                    val freshInstance = Room.databaseBuilder(
                        context.applicationContext,
                        CookingDatabase::class.java,
                        "cooking_pantry_database"
                    )
                        .addMigrations(MIGRATION_5_6)
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()

                    try {
                        val freshDb = freshInstance.openHelper.writableDatabase
                        freshDb.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT count(*) FROM sqlite_master")).use { it.moveToFirst() }
                    } catch (_: Exception) {}

                    INSTANCE = freshInstance
                    freshInstance
                }
            }
        }

        fun resetDatabase(context: Context): CookingDatabase {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {}
                INSTANCE = null

                try {
                    context.applicationContext.deleteDatabase("cooking_pantry_database")
                } catch (_: Exception) {}

                val freshInstance = Room.databaseBuilder(
                    context.applicationContext,
                    CookingDatabase::class.java,
                    "cooking_pantry_database"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()

                try {
                    freshInstance.openHelper.writableDatabase
                } catch (_: Exception) {}

                INSTANCE = freshInstance
                return freshInstance
            }
        }
    }
}
