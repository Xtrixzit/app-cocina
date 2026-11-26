package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.CookingDatabase
import com.example.data.local.entity.PantryEntity
import com.example.data.local.entity.ProductionLogEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.ShoppingEntity
import com.example.data.model.DistributionConfig
import com.example.data.model.LicenseRecord
import com.example.data.model.PantryItem
import com.example.data.model.ProductionLog
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.ShoppingItem
import com.example.data.sync.FullBackupPayload
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.util.AppShortcutsUpdater
import com.example.util.ImageUtils
import com.example.util.LocaleHelper
import com.example.util.SampleContentLocalizer
import com.example.util.SecurityUtils
import com.example.util.UnitConverter
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class CookingRepository(
    private val firestore: FirebaseFirestore,
    private val appContext: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        firestore = createConfiguredFirestore(context.applicationContext),
        appContext = context.applicationContext
    )

    companion object {
        private const val TAG = "CookingRepository"
        const val GUEST_USER_ID = "local_guest"

        fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(databaseId)
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                instance.firestoreSettings = settings
            } catch (_: IllegalStateException) {
                // Settings were already applied on this Firestore instance
            }
            return instance
        }
    }

    val context: Context get() = appContext

    private val localDb by lazy { CookingDatabase.getDatabase(appContext) }
    private val recipeDao get() = localDb.recipeDao()
    private val pantryDao get() = localDb.pantryDao()
    private val shoppingDao get() = localDb.shoppingDao()
    private val productionLogDao get() = localDb.productionLogDao()

    private val bootstrapMutex = Mutex()
    private var bootstrappedKey: String? = null

    private fun loc(): Context = LocaleHelper.localizedContext(appContext)
    private fun defaultProductName(): String = loc().getString(R.string.default_product_name)
    private fun defaultRecipeName(): String = loc().getString(R.string.default_recipe_name)
    private fun untitledRecipeName(): String = loc().getString(R.string.untitled_recipe_name)

    private fun requireCloudUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: error("Debes iniciar sesión con tu cuenta de Google para almacenar o leer tus datos en la nube.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "La sesión de Google cambió; operación cancelada." }
        }
        return uid
    }

    private fun userProfileDoc(userId: String) =
        firestore.collection("users").document(userId)

    private fun recipesCol(userId: String) =
        userProfileDoc(userId).collection("recipes")

    private fun pantryCol(userId: String) =
        userProfileDoc(userId).collection("pantry")

    private fun shoppingCol(userId: String) =
        userProfileDoc(userId).collection("shopping_items")

    private fun productionLogsCol(userId: String) =
        userProfileDoc(userId).collection("production_logs")

    suspend fun clearSessionOnSignOut() = withContext(Dispatchers.IO) {
        bootstrappedKey = null
        AppShortcutsUpdater.clearShortcuts(appContext)
    }

    // --- SNAPSHOT FETCHERS (Google Cloud vs Local Guest) ---

    suspend fun getPantryItemsSnapshot(
        userId: String,
        isGuestMode: Boolean,
        source: Source = Source.DEFAULT
    ): List<PantryItem> = withContext(Dispatchers.IO) {
        if (isGuestMode) {
            return@withContext pantryDao.getAllPantryItemsList().map { it.toDomain() }
        }
        val uid = requireCloudUserId(userId)
        val snapshot = pantryCol(uid).get(source).await()
        snapshot.toObjects(
            PantryItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedBy { it.name.lowercase() }
    }

    suspend fun getRecipesSnapshot(
        userId: String,
        isGuestMode: Boolean,
        source: Source = Source.DEFAULT
    ): List<Recipe> = withContext(Dispatchers.IO) {
        if (isGuestMode) {
            return@withContext recipeDao.getAllRecipesList().map { it.toDomain() }
        }
        val uid = requireCloudUserId(userId)
        val snapshot = recipesCol(uid).get(source).await()
        snapshot.toObjects(
            Recipe::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }

    suspend fun getShoppingItemsSnapshot(
        userId: String,
        isGuestMode: Boolean,
        source: Source = Source.DEFAULT
    ): List<ShoppingItem> = withContext(Dispatchers.IO) {
        if (isGuestMode) {
            return@withContext shoppingDao.getAllShoppingItemsList().map { it.toDomain() }
        }
        val uid = requireCloudUserId(userId)
        val snapshot = shoppingCol(uid).get(source).await()
        snapshot.toObjects(
            ShoppingItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedWith(
            compareBy<ShoppingItem> { it.isPurchased }
                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
        )
    }

    /**
     * Initializes either:
     * - Guest Mode: seeds sample data ONLY in local Room if empty (NEVER touches Firestore/cloud), OR
     * - Google Mode: syncs `/users/{uid}` profile doc and seeds cloud samples ONLY if user's Firestore workspace is empty
     *   (never uploading guest local data to the cloud).
     */
    suspend fun ensureSessionBootstrapped(
        userId: String,
        isGuestMode: Boolean,
        forceRefresh: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetKey = if (isGuestMode) GUEST_USER_ID else userId
        bootstrapMutex.withLock {
            if (!forceRefresh && bootstrappedKey == targetKey) return@withLock Result.success(Unit)
            try {
                if (isGuestMode) {
                    val localPantry = pantryDao.getAllPantryItemsList()
                    AppShortcutsUpdater.notifyPantryUpdated(appContext, localPantry.map { it.toDomain() })
                    bootstrappedKey = GUEST_USER_ID
                    return@withLock Result.success(Unit)
                }

                val uid = requireCloudUserId(userId)
                val firebaseUser = auth.currentUser
                val profileExistedBefore = syncGoogleAccountProfile(
                    uid = uid,
                    email = firebaseUser?.email.orEmpty(),
                    displayName = firebaseUser?.displayName.orEmpty(),
                    photoUrl = firebaseUser?.photoUrl?.toString().orEmpty()
                )

                val cloudRecipes = recipesCol(uid).limit(1).get(Source.DEFAULT).await()
                val cloudPantry = pantryCol(uid).limit(1).get(Source.DEFAULT).await()

                if (profileExistedBefore || !cloudRecipes.isEmpty || !cloudPantry.isEmpty) {
                    AppSettingsPreferences.setHasLoggedInBefore(appContext, true)
                }

                bootstrappedKey = uid
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isGuestMode) {
                    handleFirestoreError(e, OperationType.WRITE, "users/$userId")
                } else {
                    Log.e(TAG, "Guest local bootstrap failed", e)
                }
                Result.failure(e)
            }
        }
    }

    private suspend fun syncGoogleAccountProfile(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String
    ): Boolean {
        val docRef = userProfileDoc(uid)
        val safeEmail = email.trim().take(200)
        val safeName = displayName.trim().take(200)
        val safePhoto = photoUrl.trim().take(1000)

        val exists = try {
            docRef.get(Source.DEFAULT).await().exists()
        } catch (_: Exception) {
            false
        }

        if (exists) {
            docRef.update(
                mapOf(
                    "email" to safeEmail,
                    "displayName" to safeName,
                    "photoUrl" to safePhoto,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "email" to safeEmail,
                    "displayName" to safeName,
                    "photoUrl" to safePhoto,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return exists
    }

    // --- RECIPES ---

    fun observeRecipes(userId: String, isGuestMode: Boolean): Flow<List<Recipe>> {
        if (isGuestMode) {
            return recipeDao.getAllRecipes().map { entities ->
                entities.map { it.toDomain() }
            }
        }
        requireCloudUserId(userId)
        return recipesCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Recipe::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun saveRecipe(
        userId: String,
        recipe: Recipe,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(recipe.id, "rec")
        val safeTitle = recipe.title.trim().ifBlank { untitledRecipeName() }.take(200)
        val safeIngredients = recipe.ingredients.take(150).map { ing ->
            ing.copy(
                name = ing.name.trim().take(100),
                amount = SecurityUtils.clampNonNegative(ing.amount),
                unit = ing.unit.trim().ifBlank { "g" }.take(30),
                category = ing.category.trim().ifBlank { "Despensa" }.take(50),
                manualUnitCost = SecurityUtils.clampNonNegative(ing.manualUnitCost)
            )
        }
        val sanitizedRecipe = recipe.copy(
            id = docId,
            title = safeTitle,
            category = recipe.category.trim().ifBlank { "Almuerzos" }.take(50),
            servings = recipe.servings.coerceIn(1, 1000),
            prepTimeMinutes = recipe.prepTimeMinutes.coerceIn(0, 1440),
            cookTimeMinutes = recipe.cookTimeMinutes.coerceIn(0, 1440),
            difficulty = recipe.difficulty.trim().ifBlank { "Fácil" }.take(30),
            ingredients = safeIngredients,
            instructions = recipe.instructions.take(150),
            notes = recipe.notes.take(5000),
            imageUrl = SecurityUtils.sanitizeImageUri(recipe.imageUrl)
        )
        if (isGuestMode) {
            return@withContext try {
                val toSave = sanitizedRecipe.copy(id = docId, userId = GUEST_USER_ID)
                recipeDao.insertOrUpdate(RecipeEntity.fromDomain(toSave))
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving recipe locally in guest mode", e)
                Result.failure(e)
            }
        }

        val isNew = recipe.id.isBlank()
        val path = "users/$userId/recipes/$docId"
        try {
            val uid = requireCloudUserId(userId)
            val docRef = recipesCol(uid).document(docId)
            val exists = if (isNew) false else docRef.get(Source.DEFAULT).await().exists()
            if (exists) {
                docRef.update(toRecipeUpdateMap(sanitizedRecipe)).await()
            } else {
                docRef.set(toRecipeCreateMap(uid, sanitizedRecipe)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(
        userId: String,
        recipeId: String,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.isValidDocId(recipeId)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid recipe ID"))
        }
        if (isGuestMode) {
            return@withContext try {
                recipeDao.deleteById(recipeId)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val path = "users/$userId/recipes/$recipeId"
        try {
            val uid = requireCloudUserId(userId)
            recipesCol(uid).document(recipeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- PANTRY ---

    fun observePantry(userId: String, isGuestMode: Boolean): Flow<List<PantryItem>> {
        if (isGuestMode) {
            return pantryDao.getAllPantryItems().map { entities ->
                val list = entities.map { it.toDomain() }
                withContext(Dispatchers.IO) {
                    AppShortcutsUpdater.notifyPantryUpdated(appContext, list)
                }
                list
            }
        }
        requireCloudUserId(userId)
        return pantryCol(userId)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(
                    PantryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name.lowercase() }
                withContext(Dispatchers.IO) {
                    AppShortcutsUpdater.notifyPantryUpdated(appContext, list)
                }
                list
            }
    }

    suspend fun savePantryItem(
        userId: String,
        item: PantryItem,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(item.id, "pan")
        if (isGuestMode) {
            return@withContext try {
                val safeName = item.name.trim().ifBlank { defaultProductName() }.take(100)
                val safeUnit = item.unit.trim().ifBlank { "unidades" }.take(30)
                val toSave = item.copy(
                    id = docId,
                    userId = GUEST_USER_ID,
                    name = safeName,
                    category = item.category.trim().ifBlank { "Despensa" }.take(50),
                    quantity = clampQuantity(item.quantity),
                    unit = safeUnit,
                    minQuantity = clampQuantity(item.minQuantity),
                    cost = SecurityUtils.clampNonNegative(item.cost),
                    costBaseQuantity = SecurityUtils.clampNonNegative(item.costBaseQuantity),
                    expirationDate = item.expirationDate.take(50)
                )
                pantryDao.insertOrUpdate(PantryEntity.fromDomain(toSave))
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val isNew = item.id.isBlank()
        val path = "users/$userId/pantry/$docId"
        try {
            val uid = requireCloudUserId(userId)
            val docRef = pantryCol(uid).document(docId)
            val exists = if (isNew) false else docRef.get(Source.DEFAULT).await().exists()
            if (exists) {
                docRef.update(toPantryUpdateMap(item)).await()
            } else {
                docRef.set(toPantryCreateMap(uid, item)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePantryStock(
        userId: String,
        itemId: String,
        newQuantity: Double,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.isValidDocId(itemId)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid pantry item ID"))
        }
        val clamped = clampQuantity(newQuantity)
        if (isGuestMode) {
            return@withContext try {
                pantryDao.updateQuantity(itemId, clamped)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val path = "users/$userId/pantry/$itemId"
        try {
            val uid = requireCloudUserId(userId)
            pantryCol(uid).document(itemId).update(
                mapOf(
                    "quantity" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePantryItem(
        userId: String,
        itemId: String,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.isValidDocId(itemId)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid pantry item ID"))
        }
        if (isGuestMode) {
            return@withContext try {
                pantryDao.deleteById(itemId)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val path = "users/$userId/pantry/$itemId"
        try {
            val uid = requireCloudUserId(userId)
            pantryCol(uid).document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- SHOPPING LIST ---

    fun observeShoppingList(userId: String, isGuestMode: Boolean): Flow<List<ShoppingItem>> {
        if (isGuestMode) {
            return shoppingDao.getAllShoppingItems().map { entities ->
                entities.map { it.toDomain() }
            }
        }
        requireCloudUserId(userId)
        return shoppingCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ShoppingItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareBy<ShoppingItem> { it.isPurchased }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
            }
    }

    suspend fun saveShoppingItem(
        userId: String,
        item: ShoppingItem,
        isGuestMode: Boolean,
        syncToPantry: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(item.id, "shop")
        if (isGuestMode) {
            return@withContext try {
                val safeName = item.name.trim().ifBlank { defaultProductName() }.take(100)
                val safeUnit = item.unit.trim().ifBlank { "unidades" }.take(30)
                val toSave = item.copy(
                    id = docId,
                    userId = GUEST_USER_ID,
                    name = safeName,
                    category = item.category.trim().ifBlank { "Despensa" }.take(50),
                    quantity = clampQuantity(item.quantity),
                    unit = safeUnit,
                    recipeSource = item.recipeSource.take(200)
                )
                shoppingDao.insertOrUpdate(ShoppingEntity.fromDomain(toSave))

                if (syncToPantry && toSave.name.isNotBlank()) {
                    val existingPantry = pantryDao.getAllPantryItemsList().map { it.toDomain() }
                    val match = existingPantry.find {
                        SampleContentLocalizer.sameItemName(it.name, toSave.name)
                    }
                    if (match != null) {
                        val qtyInPantryUnit = UnitConverter.convert(toSave.quantity, toSave.unit, match.unit) ?: toSave.quantity
                        updatePantryStock(GUEST_USER_ID, match.id, qtyInPantryUnit, isGuestMode = true).getOrThrow()
                    } else {
                        savePantryItem(
                            GUEST_USER_ID,
                            PantryItem(
                                name = toSave.name,
                                category = toSave.category.ifBlank { "Despensa" },
                                quantity = toSave.quantity,
                                unit = toSave.unit.ifBlank { "unidades" },
                                minQuantity = 0.0
                            ),
                            isGuestMode = true
                        ).getOrThrow()
                    }
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val isNew = item.id.isBlank()
        val path = "users/$userId/shopping_items/$docId"
        try {
            val uid = requireCloudUserId(userId)
            val docRef = shoppingCol(uid).document(docId)
            val exists = if (isNew) false else docRef.get(Source.DEFAULT).await().exists()
            if (exists) {
                docRef.update(toShoppingUpdateMap(item)).await()
            } else {
                docRef.set(toShoppingCreateMap(uid, item)).await()
            }

            if (syncToPantry && item.name.isNotBlank()) {
                val existingPantry = pantryCol(uid).get(Source.DEFAULT).await()
                    .toObjects(PantryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val match = existingPantry.find {
                    SampleContentLocalizer.sameItemName(it.name, item.name)
                }
                if (match != null) {
                    val qtyInPantryUnit = UnitConverter.convert(item.quantity, item.unit, match.unit) ?: item.quantity
                    updatePantryStock(uid, match.id, qtyInPantryUnit, isGuestMode = false).getOrThrow()
                } else {
                    savePantryItem(
                        uid,
                        PantryItem(
                            name = item.name.trim(),
                            category = item.category.ifBlank { "Despensa" },
                            quantity = item.quantity,
                            unit = item.unit.ifBlank { "unidades" },
                            minQuantity = 0.0
                        ),
                        isGuestMode = false
                    ).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleShoppingItemPurchased(
        userId: String,
        itemId: String,
        isPurchased: Boolean,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.isValidDocId(itemId)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid shopping item ID"))
        }
        if (isGuestMode) {
            return@withContext try {
                shoppingDao.updatePurchased(itemId, isPurchased)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val path = "users/$userId/shopping_items/$itemId"
        try {
            val uid = requireCloudUserId(userId)
            shoppingCol(uid).document(itemId).update(
                mapOf(
                    "isPurchased" to isPurchased,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteShoppingItem(
        userId: String,
        itemId: String,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.isValidDocId(itemId)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid shopping item ID"))
        }
        if (isGuestMode) {
            return@withContext try {
                shoppingDao.deleteById(itemId)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val path = "users/$userId/shopping_items/$itemId"
        try {
            val uid = requireCloudUserId(userId)
            shoppingCol(uid).document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateShoppingItemQuantity(
        userId: String,
        itemId: String,
        newQuantity: Double,
        isGuestMode: Boolean,
        itemName: String? = null,
        itemUnit: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.isValidDocId(itemId)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid shopping item ID"))
        }
        val clamped = clampQuantity(newQuantity)
        if (isGuestMode) {
            return@withContext try {
                shoppingDao.updateQuantity(itemId, clamped)
                if (itemName != null) {
                    val existingPantry = pantryDao.getAllPantryItemsList().map { it.toDomain() }
                    val match = existingPantry.find {
                        SampleContentLocalizer.sameItemName(it.name, itemName)
                    }
                    if (match != null) {
                        val qtyInPantryUnit = if (!itemUnit.isNullOrBlank()) {
                            UnitConverter.convert(clamped, itemUnit, match.unit) ?: clamped
                        } else clamped
                        updatePantryStock(GUEST_USER_ID, match.id, qtyInPantryUnit, isGuestMode = true).getOrThrow()
                    }
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val path = "users/$userId/shopping_items/$itemId"
        try {
            val uid = requireCloudUserId(userId)
            shoppingCol(uid).document(itemId).update(
                mapOf(
                    "quantity" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (itemName != null) {
                val existingPantry = pantryCol(uid).get(Source.DEFAULT).await()
                    .toObjects(PantryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val match = existingPantry.find {
                    SampleContentLocalizer.sameItemName(it.name, itemName)
                }
                if (match != null) {
                    val qtyInPantryUnit = if (!itemUnit.isNullOrBlank()) {
                        UnitConverter.convert(clamped, itemUnit, match.unit) ?: clamped
                    } else clamped
                    updatePantryStock(uid, match.id, qtyInPantryUnit, isGuestMode = false).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustShoppingItemQuantity(
        userId: String,
        item: ShoppingItem,
        delta: Double,
        isGuestMode: Boolean
    ): Result<Unit> {
        val safeDelta = if (delta.isFinite()) delta else 0.0
        val newQty = clampQuantity(item.quantity + safeDelta)
        return updateShoppingItemQuantity(userId, item.id, newQty, isGuestMode, item.name, item.unit)
    }

    suspend fun clearPurchasedShoppingItems(
        userId: String,
        items: List<ShoppingItem>,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (isGuestMode) {
            return@withContext try {
                shoppingDao.deletePurchased()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        try {
            val uid = requireCloudUserId(userId)
            for (item in items.filter { it.isPurchased && SecurityUtils.isValidDocId(it.id) }) {
                shoppingCol(uid).document(item.id).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/shopping_items")
            Result.failure(e)
        }
    }

    suspend fun transferPurchasedToPantry(
        userId: String,
        purchasedItems: List<ShoppingItem>,
        currentPantry: List<PantryItem>,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = if (isGuestMode) GUEST_USER_ID else requireCloudUserId(userId)
            for (purchased in purchasedItems) {
                val matchingPantry = currentPantry.find {
                    SampleContentLocalizer.sameItemName(it.name, purchased.name)
                }
                if (matchingPantry != null) {
                    val addedInPantryUnit = UnitConverter.convert(
                        amount = purchased.quantity,
                        fromUnit = purchased.unit,
                        toUnit = matchingPantry.unit
                    ) ?: purchased.quantity
                    val newQty = matchingPantry.quantity + addedInPantryUnit
                    updatePantryStock(uid, matchingPantry.id, newQty, isGuestMode).getOrThrow()
                } else {
                    savePantryItem(
                        uid,
                        PantryItem(
                            name = purchased.name,
                            category = purchased.category,
                            quantity = purchased.quantity,
                            unit = purchased.unit,
                            minQuantity = 0.0
                        ),
                        isGuestMode
                    ).getOrThrow()
                }
                if (SecurityUtils.isValidDocId(purchased.id)) {
                    deleteShoppingItem(uid, purchased.id, isGuestMode).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isGuestMode) {
                handleFirestoreError(e, OperationType.WRITE, "users/$userId/pantry")
            }
            Result.failure(e)
        }
    }

    suspend fun importInventory(
        userId: String,
        importedItems: List<PantryItem>,
        mergeMode: Boolean,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = if (isGuestMode) GUEST_USER_ID else requireCloudUserId(userId)
            val existing = getPantryItemsSnapshot(uid, isGuestMode)

            if (!mergeMode) {
                for (item in existing) {
                    if (SecurityUtils.isValidDocId(item.id)) {
                        deletePantryItem(uid, item.id, isGuestMode).getOrThrow()
                    }
                }
                for (imported in importedItems) {
                    savePantryItem(uid, imported.copy(id = ""), isGuestMode).getOrThrow()
                }
            } else {
                for (imported in importedItems) {
                    val match = existing.find {
                        SampleContentLocalizer.sameItemName(it.name, imported.name)
                    }
                    if (match != null) {
                        updatePantryStock(uid, match.id, imported.quantity, isGuestMode).getOrThrow()
                    } else {
                        savePantryItem(uid, imported.copy(id = ""), isGuestMode).getOrThrow()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isGuestMode) {
                handleFirestoreError(e, OperationType.WRITE, "users/$userId/pantry")
            }
            Result.failure(e)
        }
    }

    suspend fun addMissingRecipeIngredientsToShoppingList(
        userId: String,
        recipe: Recipe,
        targetServings: Int,
        currentPantry: List<PantryItem>,
        isGuestMode: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val uid = if (isGuestMode) GUEST_USER_ID else requireCloudUserId(userId)
            val scale = targetServings.toDouble() / recipe.servings.coerceAtLeast(1)
            var addedCount = 0

            for (ingredient in recipe.ingredients) {
                val neededAmount = Math.round(ingredient.amount * scale * 1000.0) / 1000.0
                val matching = currentPantry.find {
                    SampleContentLocalizer.sameItemName(it.name, ingredient.name)
                }
                val available = UnitConverter.availableInTargetUnit(matching, ingredient.unit)

                if (available + 0.0001 < neededAmount) {
                    val shortage = Math.round((neededAmount - available) * 1000.0) / 1000.0
                    val shoppingItem = ShoppingItem(
                        name = ingredient.name,
                        category = ingredient.category,
                        quantity = shortage,
                        unit = ingredient.unit,
                        isPurchased = false,
                        recipeSource = loc().getString(R.string.prod_short_servings_fmt, recipe.title, targetServings)
                    )
                    saveShoppingItem(uid, shoppingItem, isGuestMode, syncToPantry = false).getOrThrow()
                    addedCount++
                }
            }
            Result.success(addedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isGuestMode) {
                handleFirestoreError(e, OperationType.WRITE, "users/$userId/shopping_items")
            }
            Result.failure(e)
        }
    }

    suspend fun seedSampleRecipes(
        userId: String,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = if (isGuestMode) GUEST_USER_ID else requireCloudUserId(userId)
            val samples = SampleContentLocalizer.starterRecipes(appContext)

            for (recipe in samples) {
                saveRecipe(uid, recipe, isGuestMode).getOrThrow()
            }

            val pantrySamples = SampleContentLocalizer.starterPantryItems(appContext)

            for (p in pantrySamples) {
                savePantryItem(uid, p, isGuestMode).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isGuestMode) {
                handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            }
            Result.failure(e)
        }
    }

    // --- PRODUCTION LOGS (History & Profit Tracking) ---

    fun observeProductionLogs(userId: String, isGuestMode: Boolean): Flow<List<ProductionLog>> {
        if (isGuestMode) {
            return productionLogDao.getAllLogs().map { entities ->
                entities.map { it.toDomain() }
            }
        }
        requireCloudUserId(userId)
        return productionLogsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map(::snapshotToProductionLog)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun getProductionLogsSnapshot(
        userId: String,
        isGuestMode: Boolean,
        source: Source = Source.DEFAULT
    ): List<ProductionLog> = withContext(Dispatchers.IO) {
        if (isGuestMode) {
            return@withContext productionLogDao.getAllLogsList().map { it.toDomain() }
        }
        val uid = requireCloudUserId(userId)
        productionLogsCol(uid).get(source).await()
            .documents.map(::snapshotToProductionLog)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }

    suspend fun recordProductionLog(
        userId: String,
        log: ProductionLog,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(log.id, "log")
        val safeRecipeId = log.recipeId.trim().take(128).ifBlank { docId }
        val safeTitle = log.recipeTitle.trim().ifBlank { defaultRecipeName() }.take(200)
        val safeScale = SecurityUtils.clampPositive(log.scaleFactor, min = 0.01, max = 10_000.0, fallback = 1.0)
        val safeServings = log.targetServings.coerceIn(1, 100_000)
        val safeTotalCost = SecurityUtils.clampNonNegative(log.totalCost, SecurityUtils.MAX_PRODUCTION_CURRENCY_VALUE)
        val safeSuggestedPrice = SecurityUtils.clampNonNegative(log.suggestedPrice, SecurityUtils.MAX_PRODUCTION_CURRENCY_VALUE)
        val safeMargin = SecurityUtils.clampMarginPercent(log.marginPercent)
        val safeDeducted = log.deductedIngredientsCount.coerceIn(0, 500)
        val safeCurrency = log.currencyCode.trim().ifBlank { "USD" }.take(16)

        if (isGuestMode) {
            return@withContext try {
                val safeLog = log.copy(
                    id = docId,
                    userId = GUEST_USER_ID,
                    recipeId = safeRecipeId,
                    recipeTitle = safeTitle,
                    targetServings = safeServings,
                    scaleFactor = safeScale,
                    totalCost = safeTotalCost,
                    currencyCode = safeCurrency,
                    marginPercent = safeMargin,
                    suggestedPrice = safeSuggestedPrice,
                    deductedIngredientsCount = safeDeducted
                )
                productionLogDao.insertOrUpdate(ProductionLogEntity.fromDomain(safeLog))
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val path = "users/$userId/production_logs/$docId"
        try {
            val uid = requireCloudUserId(userId)
            val payload = mapOf(
                "userId" to uid,
                "recipeId" to safeRecipeId,
                "recipeTitle" to safeTitle,
                "servingsProduced" to safeServings,
                "scaleFactor" to safeScale,
                "totalCost" to safeTotalCost,
                "suggestedSellingPrice" to safeSuggestedPrice,
                "currencyCode" to safeCurrency,
                "deductedIngredientsCount" to safeDeducted,
                "createdAt" to FieldValue.serverTimestamp()
            )
            productionLogsCol(uid).document(docId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun snapshotToProductionLog(doc: DocumentSnapshot): ProductionLog {
        val servings = (doc.get("servingsProduced") as? Number)?.toInt()
            ?: (doc.get("targetServings") as? Number)?.toInt()
            ?: 1
        val totalCost = (doc.get("totalCost") as? Number)?.toDouble() ?: 0.0
        val suggestedPrice = (doc.get("suggestedSellingPrice") as? Number)?.toDouble()
            ?: (doc.get("suggestedPrice") as? Number)?.toDouble()
            ?: 0.0
        val computedMargin = (doc.get("marginPercent") as? Number)?.toInt()
            ?: if (totalCost > 0.0 && suggestedPrice >= totalCost) {
                Math.round(((suggestedPrice - totalCost) / totalCost) * 100.0).toInt().coerceIn(0, 500)
            } else {
                0
            }
        return ProductionLog(
            id = doc.id,
            userId = doc.getString("userId").orEmpty(),
            recipeId = doc.getString("recipeId").orEmpty(),
            recipeTitle = doc.getString("recipeTitle").orEmpty(),
            targetServings = servings.coerceAtLeast(1),
            scaleFactor = (doc.get("scaleFactor") as? Number)?.toDouble() ?: 1.0,
            totalCost = totalCost,
            currencyCode = doc.getString("currencyCode").orEmpty().ifBlank { "USD" },
            marginPercent = computedMargin,
            suggestedPrice = suggestedPrice,
            deductedIngredientsCount = (doc.get("deductedIngredientsCount") as? Number)?.toInt() ?: 0,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    // --- GUEST TO CLOUD MIGRATION & FULL BACKUP ---

    suspend fun getLocalGuestRecordCount(): Int = withContext(Dispatchers.IO) {
        try {
            recipeDao.getRecipeCount() +
                pantryDao.getAllPantryItemsList().size +
                shoppingDao.getAllShoppingItemsList().size +
                productionLogDao.getAllLogsList().size
        } catch (_: Exception) {
            0
        }
    }

    suspend fun migrateGuestDataToCloud(cloudUserId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val uid = requireCloudUserId(cloudUserId)
            var migrated = 0

            val localRecipes = recipeDao.getAllRecipesList().map { it.toDomain() }
            val cloudRecipes = getRecipesSnapshot(uid, isGuestMode = false)
            val cloudRecipeTitles = cloudRecipes.map { it.title.trim().lowercase() }.toSet()
            for (r in localRecipes) {
                if (r.title.trim().lowercase() !in cloudRecipeTitles) {
                    saveRecipe(uid, r.copy(id = ""), isGuestMode = false).getOrThrow()
                    migrated++
                }
            }

            val localPantry = pantryDao.getAllPantryItemsList().map { it.toDomain() }
            val cloudPantry = getPantryItemsSnapshot(uid, isGuestMode = false)
            val cloudPantryNames = cloudPantry.map { it.name.trim().lowercase() }.toSet()
            for (p in localPantry) {
                if (p.name.trim().lowercase() !in cloudPantryNames) {
                    savePantryItem(uid, p.copy(id = ""), isGuestMode = false).getOrThrow()
                    migrated++
                }
            }

            val localShopping = shoppingDao.getAllShoppingItemsList().map { it.toDomain() }
            val cloudShopping = getShoppingItemsSnapshot(uid, isGuestMode = false)
            val cloudShoppingNames = cloudShopping.map { it.name.trim().lowercase() }.toSet()
            for (s in localShopping) {
                if (s.name.trim().lowercase() !in cloudShoppingNames) {
                    saveShoppingItem(uid, s.copy(id = ""), isGuestMode = false, syncToPantry = false).getOrThrow()
                    migrated++
                }
            }

            val localLogs = productionLogDao.getAllLogsList().map { it.toDomain() }
            val cloudLogs = getProductionLogsSnapshot(uid, isGuestMode = false)
            val cloudLogIds = cloudLogs.map { it.id }.toSet()
            for (log in localLogs) {
                if (log.id !in cloudLogIds) {
                    recordProductionLog(uid, log, isGuestMode = false).getOrThrow()
                    migrated++
                }
            }

            Result.success(migrated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$cloudUserId")
            Result.failure(e)
        }
    }

    suspend fun exportFullBackup(
        userId: String,
        isGuestMode: Boolean
    ): Result<FullBackupPayload> = withContext(Dispatchers.IO) {
        try {
            val recipes = getRecipesSnapshot(userId, isGuestMode)
            val pantry = getPantryItemsSnapshot(userId, isGuestMode)
            val shopping = getShoppingItemsSnapshot(userId, isGuestMode)
            val logs = getProductionLogsSnapshot(userId, isGuestMode)
            Result.success(
                FullBackupPayload(
                    recipes = recipes,
                    pantry = pantry,
                    shopping = shopping,
                    productionLogs = logs,
                    currencyCode = AppSettingsPreferences.getCurrency(appContext).code,
                    marginPercent = AppSettingsPreferences.getMarginPercent(appContext)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFullBackup(
        userId: String,
        payload: FullBackupPayload,
        replaceExisting: Boolean,
        isGuestMode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = if (isGuestMode) GUEST_USER_ID else requireCloudUserId(userId)

            if (replaceExisting) {
                for (r in getRecipesSnapshot(uid, isGuestMode)) {
                    if (r.id.isNotBlank()) deleteRecipe(uid, r.id, isGuestMode).getOrThrow()
                }
                for (p in getPantryItemsSnapshot(uid, isGuestMode)) {
                    if (p.id.isNotBlank()) deletePantryItem(uid, p.id, isGuestMode).getOrThrow()
                }
                for (s in getShoppingItemsSnapshot(uid, isGuestMode)) {
                    if (s.id.isNotBlank()) deleteShoppingItem(uid, s.id, isGuestMode).getOrThrow()
                }
            }

            for (r in payload.recipes) {
                saveRecipe(uid, if (replaceExisting) r.copy(id = "") else r, isGuestMode).getOrThrow()
            }
            for (p in payload.pantry) {
                savePantryItem(uid, if (replaceExisting) p.copy(id = "") else p, isGuestMode).getOrThrow()
            }
            for (s in payload.shopping) {
                saveShoppingItem(uid, if (replaceExisting) s.copy(id = "") else s, isGuestMode, syncToPantry = false).getOrThrow()
            }
            for (log in payload.productionLogs) {
                recordProductionLog(uid, log, isGuestMode).getOrThrow()
            }

            payload.currencyCode?.takeIf { it.isNotBlank() }?.let { code ->
                AppSettingsPreferences.setCurrency(appContext, CurrencyOption.fromCode(code))
            }
            payload.marginPercent?.let { margin ->
                AppSettingsPreferences.setMarginPercent(appContext, margin)
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isGuestMode) {
                handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            }
            Result.failure(e)
        }
    }

    // --- COMMERCIAL DISTRIBUTION: UPDATE CHECK, LICENSE & ACCOUNT DELETION ---

    suspend fun fetchDistributionConfig(): DistributionConfig? = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext null
        try {
            val doc = firestore.collection("app_config").document("distribution").get(Source.DEFAULT).await()
            if (doc.exists()) {
                DistributionConfig(
                    latestVersionCode = (doc.get("latestVersionCode") as? Number)?.toInt() ?: 0,
                    latestVersionName = doc.getString("latestVersionName").orEmpty(),
                    minRequiredVersionCode = (doc.get("minRequiredVersionCode") as? Number)?.toInt() ?: 0,
                    updateUrl = (doc.getString("updateUrl").orEmpty().ifBlank {
                        doc.getString("downloadUrl").orEmpty()
                    }).takeIf { SecurityUtils.isSafeHttpsUrl(it) }.orEmpty(),
                    updateNotes = doc.getString("updateNotes").orEmpty().ifBlank {
                        doc.getString("releaseNotes").orEmpty()
                    },
                    requireLicense = doc.getBoolean("requireLicense") ?: false,
                    supportContact = doc.getString("supportContact").orEmpty(),
                    githubRepo = doc.getString("githubRepo").orEmpty()
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun verifyOrClaimLicense(userId: String, rawCode: String): Result<LicenseRecord> = withContext(Dispatchers.IO) {
        val cleanCode = rawCode.trim()
        if (!SecurityUtils.isValidDocId(cleanCode)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid license key format"))
        }
        try {
            val uid = requireCloudUserId(userId)
            val docRef = firestore.collection("licenses").document(cleanCode)
            // Strict Source.SERVER verification — offline cache cannot spoof or bypass license checks
            val serverSnap = docRef.get(Source.SERVER).await()
            if (!serverSnap.exists()) {
                return@withContext Result.failure(IllegalStateException("License not found"))
            }

            val userEmail = (auth.currentUser?.email ?: "").trim().take(200)
            val claimedRecord = firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    throw IllegalStateException("License not found")
                }
                val active = snap.getBoolean("active") ?: false
                if (!active) {
                    throw IllegalStateException("Inactive license")
                }
                val expiresAt = snap.getTimestamp("expiresAt")
                if (expiresAt != null && expiresAt.toDate().time <= System.currentTimeMillis()) {
                    throw IllegalStateException("License expired")
                }
                val assignedUid = snap.getString("assignedUid").orEmpty().ifBlank {
                    snap.getString("assignedUserId").orEmpty()
                }
                val assignedEmail = snap.getString("assignedEmail").orEmpty()
                val activatedAt = snap.getTimestamp("activatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val tier = snap.getString("tier").orEmpty().ifBlank { "pro" }
                val notes = snap.getString("notes").orEmpty()

                if (assignedUid.isNotBlank() && assignedUid != uid) {
                    throw IllegalStateException("License already claimed")
                }
                if (assignedUid.isBlank()) {
                    tx.update(
                        docRef,
                        mapOf(
                            "assignedUid" to uid,
                            "assignedEmail" to userEmail,
                            "activatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                LicenseRecord(
                    code = cleanCode,
                    active = true,
                    assignedUid = uid,
                    assignedEmail = assignedEmail.ifBlank { userEmail },
                    activatedAt = activatedAt,
                    tier = tier,
                    notes = notes
                )
            }.await()

            AppSettingsPreferences.setLicenseKey(appContext, cleanCode)
            Result.success(claimedRecord)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "licenses/$cleanCode")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountAndCloudData(userId: String, isGuestMode: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isGuestMode) {
                recipeDao.deleteAll()
                pantryDao.deleteAll()
                shoppingDao.deleteAll()
                productionLogDao.deleteAll()
                bootstrappedKey = null
                AppShortcutsUpdater.clearShortcuts(appContext)
                return@withContext Result.success(Unit)
            }

            val uid = requireCloudUserId(userId)
            val cols = listOf(recipesCol(uid), pantryCol(uid), shoppingCol(uid), productionLogsCol(uid))
            for (col in cols) {
                val docs = col.get(Source.DEFAULT).await()
                for (d in docs.documents) {
                    d.reference.delete().await()
                }
            }
            try {
                userProfileDoc(uid).delete().await()
            } catch (_: Exception) {
            }
            try {
                auth.currentUser?.delete()?.await()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                Log.w(TAG, "Recent login required for Firebase Auth delete; cloud Firestore data already deleted", e)
                auth.signOut()
            }
            bootstrappedKey = null
            AppShortcutsUpdater.clearShortcuts(appContext)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isGuestMode) {
                handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            }
            Result.failure(e)
        }
    }

    // --- PAYLOAD HELPERS (Strict 1:1 parity with firestore.rules) ---

    private fun sanitizeId(rawId: String, prefix: String): String {
        val cleaned = rawId.trim().replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
        return if (cleaned.isNotBlank() && cleaned.length <= 128) {
            cleaned
        } else {
            "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        }
    }

    private fun clampQuantity(value: Double): Double {
        val nonNeg = SecurityUtils.clampNonNegative(value)
        return Math.round(nonNeg * 10_000.0) / 10_000.0
    }

    private fun toIngredientsList(ingredients: List<RecipeIngredient>): List<Map<String, Any>> {
        return ingredients.take(150).map { ing ->
            mapOf(
                "name" to ing.name.trim().take(100),
                "amount" to SecurityUtils.clampNonNegative(ing.amount),
                "unit" to ing.unit.trim().ifBlank { "g" }.take(30),
                "category" to ing.category.trim().ifBlank { "Despensa" }.take(50),
                "manualUnitCost" to SecurityUtils.clampNonNegative(ing.manualUnitCost)
            )
        }
    }

    private fun toRecipeCreateMap(uid: String, recipe: Recipe): Map<String, Any> {
        val safeTitle = recipe.title.trim().ifBlank { untitledRecipeName() }.take(200)
        return mapOf(
            "userId" to uid,
            "title" to safeTitle,
            "category" to recipe.category.trim().take(50),
            "servings" to recipe.servings.coerceIn(1, 1000),
            "prepTimeMinutes" to recipe.prepTimeMinutes.coerceIn(0, 1440),
            "cookTimeMinutes" to recipe.cookTimeMinutes.coerceIn(0, 1440),
            "difficulty" to recipe.difficulty.trim().take(30),
            "ingredients" to toIngredientsList(recipe.ingredients),
            "instructions" to recipe.instructions.take(150),
            "notes" to recipe.notes.take(5000),
            "imageUrl" to SecurityUtils.sanitizeImageUri(recipe.imageUrl),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun toRecipeUpdateMap(recipe: Recipe): Map<String, Any> {
        val safeTitle = recipe.title.trim().ifBlank { untitledRecipeName() }.take(200)
        return mapOf(
            "title" to safeTitle,
            "category" to recipe.category.trim().take(50),
            "servings" to recipe.servings.coerceIn(1, 1000),
            "prepTimeMinutes" to recipe.prepTimeMinutes.coerceIn(0, 1440),
            "cookTimeMinutes" to recipe.cookTimeMinutes.coerceIn(0, 1440),
            "difficulty" to recipe.difficulty.trim().take(30),
            "ingredients" to toIngredientsList(recipe.ingredients),
            "instructions" to recipe.instructions.take(150),
            "notes" to recipe.notes.take(5000),
            "imageUrl" to SecurityUtils.sanitizeImageUri(recipe.imageUrl),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun toPantryCreateMap(uid: String, item: PantryItem): Map<String, Any> {
        val safeName = item.name.trim().ifBlank { defaultProductName() }.take(100)
        val safeUnit = item.unit.trim().ifBlank { "unidades" }.take(30)
        return mapOf(
            "userId" to uid,
            "name" to safeName,
            "category" to item.category.trim().ifBlank { "Despensa" }.take(50),
            "quantity" to clampQuantity(item.quantity),
            "unit" to safeUnit,
            "minQuantity" to clampQuantity(item.minQuantity),
            "cost" to SecurityUtils.clampNonNegative(item.cost),
            "costBaseQuantity" to SecurityUtils.clampNonNegative(item.costBaseQuantity),
            "expirationDate" to item.expirationDate.take(50),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun toPantryUpdateMap(item: PantryItem): Map<String, Any> {
        val safeName = item.name.trim().ifBlank { defaultProductName() }.take(100)
        val safeUnit = item.unit.trim().ifBlank { "unidades" }.take(30)
        return mapOf(
            "name" to safeName,
            "category" to item.category.trim().ifBlank { "Despensa" }.take(50),
            "quantity" to clampQuantity(item.quantity),
            "unit" to safeUnit,
            "minQuantity" to clampQuantity(item.minQuantity),
            "cost" to SecurityUtils.clampNonNegative(item.cost),
            "costBaseQuantity" to SecurityUtils.clampNonNegative(item.costBaseQuantity),
            "expirationDate" to item.expirationDate.take(50),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun toShoppingCreateMap(uid: String, item: ShoppingItem): Map<String, Any> {
        val safeName = item.name.trim().ifBlank { defaultProductName() }.take(100)
        val safeUnit = item.unit.trim().ifBlank { "unidades" }.take(30)
        return mapOf(
            "userId" to uid,
            "name" to safeName,
            "category" to item.category.trim().ifBlank { "Despensa" }.take(50),
            "quantity" to clampQuantity(item.quantity),
            "unit" to safeUnit,
            "isPurchased" to item.isPurchased,
            "recipeSource" to item.recipeSource.take(200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun toShoppingUpdateMap(item: ShoppingItem): Map<String, Any> {
        val safeName = item.name.trim().ifBlank { defaultProductName() }.take(100)
        val safeUnit = item.unit.trim().ifBlank { "unidades" }.take(30)
        return mapOf(
            "name" to safeName,
            "category" to item.category.trim().ifBlank { "Despensa" }.take(50),
            "quantity" to clampQuantity(item.quantity),
            "unit" to safeUnit,
            "isPurchased" to item.isPurchased,
            "recipeSource" to item.recipeSource.take(200),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
