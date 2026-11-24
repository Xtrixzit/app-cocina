package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.ChefChatMessage
import com.example.ai.ChefChatRepository
import com.example.ai.ChefChatRole
import com.example.ai.ChefChatStreamEvent
import com.example.ai.PhotoAspectOption
import com.example.ai.RecipePhotoGenState
import com.example.ai.RecipePhotoGenerator
import com.example.data.model.DistributionConfig
import com.example.data.model.PantryItem
import com.example.data.model.ProductionLog
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.ShoppingItem
import com.example.data.repository.CookingRepository
import com.example.BuildConfig
import com.example.data.sync.AiReadableExportFormatter
import com.example.data.sync.DriveApiDisabledException
import com.example.data.sync.FullBackupManager
import com.example.data.sync.GitHubUpdateChecker
import com.example.data.sync.GoogleDriveSyncRepository
import com.example.settings.AppSettingsPreferences
import com.example.settings.DriveSyncInfo
import com.example.util.LocaleHelper
import com.example.util.UnitConverter
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecipeFeasibility(
    val recipe: Recipe,
    val matchPercentage: Int, // 0 - 100
    val availableCount: Int,
    val totalCount: Int,
    val missingIngredients: List<Pair<String, Double>> // Name and missing amount
)

class MainViewModel(
    private val repository: CookingRepository,
    private val currentUserId: String,
    val currentUserEmail: String = "",
    val currentUserName: String = "",
    val isGuestMode: Boolean = false
) : ViewModel() {

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private fun str(resId: Int, vararg args: Any): String =
        LocaleHelper.localizedContext(repository.context).getString(resId, *args)

    // User notification messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Commercial controls & guest migration states
    private val _distributionConfig = MutableStateFlow<DistributionConfig?>(null)
    val distributionConfig: StateFlow<DistributionConfig?> = _distributionConfig.asStateFlow()

    private val _licenseValidated = MutableStateFlow(true)
    val licenseValidated: StateFlow<Boolean> = _licenseValidated.asStateFlow()

    private val _localGuestCountForMigration = MutableStateFlow(0)
    val localGuestCountForMigration: StateFlow<Int> = _localGuestCountForMigration.asStateFlow()

    private val _recipePhotoGenState = MutableStateFlow<RecipePhotoGenState>(RecipePhotoGenState.Idle)
    val recipePhotoGenState: StateFlow<RecipePhotoGenState> = _recipePhotoGenState.asStateFlow()

    private val _chefChatMessages = MutableStateFlow<List<ChefChatMessage>>(emptyList())
    val chefChatMessages: StateFlow<List<ChefChatMessage>> = _chefChatMessages.asStateFlow()

    private val _chefChatRole = MutableStateFlow(ChefChatRole.GENERAL)
    val chefChatRole: StateFlow<ChefChatRole> = _chefChatRole.asStateFlow()

    private val _chefChatSending = MutableStateFlow(false)
    val chefChatSending: StateFlow<Boolean> = _chefChatSending.asStateFlow()

    private val _driveSyncing = MutableStateFlow(false)
    val driveSyncing: StateFlow<Boolean> = _driveSyncing.asStateFlow()

    private val _driveSyncError = MutableStateFlow<String?>(null)
    val driveSyncError: StateFlow<String?> = _driveSyncError.asStateFlow()

    private val _driveActivationUrl = MutableStateFlow<String?>(null)
    val driveActivationUrl: StateFlow<String?> = _driveActivationUrl.asStateFlow()

    val driveSyncInfo: StateFlow<DriveSyncInfo> =
        AppSettingsPreferences.observeDriveSyncInfo(repository.context)

    private var autoDriveSyncJob: Job? = null

    fun generateRecipePhoto(
        title: String,
        category: String,
        ingredients: List<RecipeIngredient>,
        aspectOption: PhotoAspectOption = PhotoAspectOption.RATIO_4_3
    ) {
        if (_recipePhotoGenState.value is RecipePhotoGenState.Generating) return
        viewModelScope.launch {
            _recipePhotoGenState.value = RecipePhotoGenState.Generating
            val res = RecipePhotoGenerator.generateRealisticRecipePhoto(
                context = repository.context,
                title = title,
                category = category,
                ingredients = ingredients,
                aspectOption = aspectOption
            )
            _recipePhotoGenState.value = res.fold(
                onSuccess = { RecipePhotoGenState.Success(it) },
                onFailure = { RecipePhotoGenState.Error(it.localizedMessage ?: str(R.string.recipe_photo_ai_failed)) }
            )
        }
    }

    fun enhanceRecipePhoto(
        sourceImageUrl: String,
        title: String,
        category: String,
        aspectOption: PhotoAspectOption = PhotoAspectOption.RATIO_4_3
    ) {
        if (_recipePhotoGenState.value is RecipePhotoGenState.Generating) return
        viewModelScope.launch {
            _recipePhotoGenState.value = RecipePhotoGenState.Generating
            val res = RecipePhotoGenerator.enhanceRealPhotoWithAi(
                context = repository.context,
                sourceImageUrl = sourceImageUrl,
                title = title,
                category = category,
                aspectOption = aspectOption
            )
            _recipePhotoGenState.value = res.fold(
                onSuccess = { RecipePhotoGenState.Success(it) },
                onFailure = { RecipePhotoGenState.Error(it.localizedMessage ?: str(R.string.recipe_photo_ai_failed)) }
            )
        }
    }

    fun resetRecipePhotoGenState() {
        _recipePhotoGenState.value = RecipePhotoGenState.Idle
    }

    fun setChefChatRole(role: ChefChatRole) {
        _chefChatRole.value = role
    }

    fun clearChefChat() {
        _chefChatMessages.value = emptyList()
    }

    fun sendChefChatMessage(
        prompt: String,
        attachedImageUri: String = ""
    ) {
        if (_chefChatSending.value) return
        val trimmed = prompt.trim()
        if (trimmed.isBlank() && attachedImageUri.isBlank()) return

        val currentHistory = _chefChatMessages.value
        val userMsgId = "u_${System.nanoTime()}"
        val modelMsgId = "m_${System.nanoTime()}"

        val userMessage = ChefChatMessage(
            id = userMsgId,
            role = "user",
            text = trimmed.ifBlank { str(R.string.chef_chat_user_photo_badge) },
            attachedImageUri = attachedImageUri
        )
        val placeholderModelMessage = ChefChatMessage(
            id = modelMsgId,
            role = "model",
            text = ""
        )

        _chefChatMessages.value = currentHistory + userMessage + placeholderModelMessage
        _chefChatSending.value = true

        val currentRecipes = (recipesState.value as? UiState.Success)?.data ?: emptyList()
        val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
        val activeRole = _chefChatRole.value

        viewModelScope.launch {
            try {
                ChefChatRepository.sendChatMessageStream(
                    context = repository.context,
                    role = activeRole,
                    historyMessages = currentHistory,
                    userPrompt = trimmed,
                    attachedImageUri = attachedImageUri,
                    recipes = currentRecipes,
                    pantry = currentPantry
                ).collect { event ->
                    when (event) {
                        is ChefChatStreamEvent.Chunk -> {
                            _chefChatMessages.value = _chefChatMessages.value.map { msg ->
                                if (msg.id == modelMsgId) msg.copy(text = event.accumulatedText) else msg
                            }
                        }
                        is ChefChatStreamEvent.Completed -> {
                            _chefChatMessages.value = _chefChatMessages.value.map { msg ->
                                if (msg.id == modelMsgId) msg.copy(text = event.finalText, isError = false) else msg
                            }
                        }
                        is ChefChatStreamEvent.Failed -> {
                            _chefChatMessages.value = _chefChatMessages.value.map { msg ->
                                if (msg.id == modelMsgId) msg.copy(text = event.errorMessage, isError = true) else msg
                            }
                        }
                    }
                }
            } finally {
                _chefChatSending.value = false
            }
        }
    }

    init {
        viewModelScope.launch {
            val result = repository.ensureSessionBootstrapped(currentUserId, isGuestMode)
            result.onFailure { err ->
                val detail = err.localizedMessage ?: "..."
                val msg = if (isGuestMode) {
                    str(R.string.vm_bootstrap_err_guest, detail)
                } else {
                    str(R.string.vm_bootstrap_err_cloud, detail)
                }
                _userMessage.emit(msg)
            }

            if (!isGuestMode) {
                val existingDrive = AppSettingsPreferences.getDriveSyncInfo(repository.context)
                if (
                    existingDrive.linkedEmail.isNotBlank() &&
                    currentUserEmail.isNotBlank() &&
                    !existingDrive.linkedEmail.equals(currentUserEmail, ignoreCase = true)
                ) {
                    AppSettingsPreferences.saveDriveSyncInfo(
                        repository.context,
                        DriveSyncInfo(autoSyncEnabled = true, linkedEmail = currentUserEmail)
                    )
                } else if (!existingDrive.autoSyncEnabled && existingDrive.lastSyncMillis == 0L) {
                    AppSettingsPreferences.setDriveAutoSyncEnabled(repository.context, true)
                }

                _localGuestCountForMigration.value = repository.getLocalGuestRecordCount()
                val cfg = repository.fetchDistributionConfig()
                _distributionConfig.value = cfg
                if (cfg?.requireLicense == true) {
                    val savedKey = AppSettingsPreferences.getLicenseKey(repository.context)
                    if (savedKey.isBlank()) {
                        _licenseValidated.value = false
                    } else {
                        val claimRes = repository.verifyOrClaimLicense(currentUserId, savedKey)
                        _licenseValidated.value = claimRes.isSuccess
                    }
                }
            }

            // Silently check configured GitHub Releases in background for instant version awareness
            val configuredRepo = resolveEffectiveGithubRepo()
            if (configuredRepo.isNotBlank()) {
                val ghConfig = GitHubUpdateChecker.fetchLatestRelease(
                    repoInput = configuredRepo,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    currentVersionName = BuildConfig.VERSION_NAME
                ).getOrNull()
                if (ghConfig != null) {
                    val current = _distributionConfig.value
                    if (current == null || ghConfig.latestVersionCode >= current.latestVersionCode) {
                        _distributionConfig.value = ghConfig.copy(
                            minRequiredVersionCode = current?.minRequiredVersionCode ?: 0,
                            requireLicense = current?.requireLicense ?: false
                        )
                    }
                }
            }
        }
    }

    // Data streams with two-tier Flow error unwrapping
    val recipesState: StateFlow<UiState<List<Recipe>>> = repository.observeRecipes(currentUserId, isGuestMode)
        .map<List<Recipe>, UiState<List<Recipe>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing recipes", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: str(R.string.recipes_sync_error)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val pantryState: StateFlow<UiState<List<PantryItem>>> = repository.observePantry(currentUserId, isGuestMode)
        .map<List<PantryItem>, UiState<List<PantryItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing pantry", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: str(R.string.pantry_load_error)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val shoppingState: StateFlow<UiState<List<ShoppingItem>>> = repository.observeShoppingList(currentUserId, isGuestMode)
        .map<List<ShoppingItem>, UiState<List<ShoppingItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing shopping list", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: str(R.string.shopping_load_error)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val productionLogsState: StateFlow<UiState<List<ProductionLog>>> = repository.observeProductionLogs(currentUserId, isGuestMode)
        .map<List<ProductionLog>, UiState<List<ProductionLog>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing production logs", firestoreError ?: error)
            emit(UiState.Success(emptyList()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Search & Filter states (canonical Spanish keys kept stable)
    val recipeSearchQuery = MutableStateFlow("")
    val selectedRecipeCategory = MutableStateFlow("Todas")

    val pantrySearchQuery = MutableStateFlow("")
    val selectedPantryCategory = MutableStateFlow("Todas")
    val filterOnlyLowStock = MutableStateFlow(false)

    // Selected recipe for detail/scaling
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    // Servings override for currently viewed recipe
    val targetServings = MutableStateFlow(4)

    fun selectRecipe(recipe: Recipe?) {
        _selectedRecipe.value = recipe
        if (recipe != null) {
            targetServings.value = recipe.servings
        }
    }

    fun setTargetServings(servings: Int) {
        targetServings.value = servings.coerceIn(1, 500)
    }

    // --- AUTH / LIFECYCLE ---

    suspend fun prepareSignOut() {
        autoDriveSyncJob?.cancel()
        withContext(NonCancellable) {
            AppSettingsPreferences.clearDriveSyncInfo(repository.context)
            repository.clearSessionOnSignOut()
        }
    }

    // --- RECIPE ACTIONS ---

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.saveRecipe(currentUserId, recipe, isGuestMode)
                .onSuccess {
                    val msg = if (isGuestMode) {
                        str(R.string.vm_saved_recipe_guest)
                    } else {
                        str(R.string.vm_saved_recipe_cloud)
                    }
                    _userMessage.emit(msg)
                    if (_selectedRecipe.value?.id == recipe.id) {
                        _selectedRecipe.value = recipe
                    }
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_save_recipe, e.localizedMessage ?: ""))
                }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            repository.deleteRecipe(currentUserId, recipeId, isGuestMode)
                .onSuccess {
                    if (_selectedRecipe.value?.id == recipeId) {
                        _selectedRecipe.value = null
                    }
                    _userMessage.emit(str(R.string.vm_deleted_recipe))
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_delete, e.localizedMessage ?: ""))
                }
        }
    }

    fun seedSampleRecipes() {
        viewModelScope.launch {
            repository.seedSampleRecipes(currentUserId, isGuestMode)
                .onSuccess {
                    val msg = if (isGuestMode) {
                        str(R.string.vm_samples_guest)
                    } else {
                        str(R.string.vm_samples_cloud)
                    }
                    _userMessage.emit(msg)
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_samples, e.localizedMessage ?: ""))
                }
        }
    }

    // --- PANTRY ACTIONS ---

    fun savePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.savePantryItem(currentUserId, item, isGuestMode)
                .onSuccess {
                    val msg = if (isGuestMode) {
                        str(R.string.vm_saved_pantry_guest)
                    } else {
                        str(R.string.vm_saved_pantry_cloud)
                    }
                    _userMessage.emit(msg)
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_save_pantry, e.localizedMessage ?: ""))
                }
        }
    }

    fun adjustPantryStock(item: PantryItem, delta: Double) {
        viewModelScope.launch {
            val newQty = (item.quantity + delta).coerceAtLeast(0.0)
            repository.updatePantryStock(currentUserId, item.id, newQty, isGuestMode)
                .onSuccess { scheduleAutoDriveSync() }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_update_stock, e.localizedMessage ?: ""))
                }
        }
    }

    fun deletePantryItem(itemId: String) {
        viewModelScope.launch {
            repository.deletePantryItem(currentUserId, itemId, isGuestMode)
                .onSuccess {
                    _userMessage.emit(str(R.string.vm_deleted_pantry))
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_delete_pantry, e.localizedMessage ?: ""))
                }
        }
    }

    fun importInventory(importedItems: List<PantryItem>, mergeMode: Boolean) {
        viewModelScope.launch {
            repository.importInventory(currentUserId, importedItems, mergeMode, isGuestMode)
                .onSuccess {
                    _userMessage.emit(str(R.string.vm_import_success, importedItems.size))
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_import, e.localizedMessage ?: ""))
                }
        }
    }

    // --- SHOPPING LIST ACTIONS ---

    fun saveShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.saveShoppingItem(currentUserId, item, isGuestMode)
                .onSuccess {
                    _userMessage.emit(str(R.string.vm_saved_shopping))
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_save, e.localizedMessage ?: ""))
                }
        }
    }

    fun toggleShoppingItem(itemId: String, isPurchased: Boolean) {
        viewModelScope.launch {
            repository.toggleShoppingItemPurchased(currentUserId, itemId, isPurchased, isGuestMode)
                .onSuccess { scheduleAutoDriveSync() }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_update_shop, e.localizedMessage ?: ""))
                }
        }
    }

    fun deleteShoppingItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteShoppingItem(currentUserId, itemId, isGuestMode)
                .onSuccess { scheduleAutoDriveSync() }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_delete_shop, e.localizedMessage ?: ""))
                }
        }
    }

    fun adjustShoppingItemQuantity(item: ShoppingItem, delta: Double) {
        viewModelScope.launch {
            repository.adjustShoppingItemQuantity(currentUserId, item, delta, isGuestMode)
                .onSuccess { scheduleAutoDriveSync() }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_update_qty, e.localizedMessage ?: ""))
                }
        }
    }

    fun updateShoppingItemQuantity(itemId: String, newQuantity: Double, itemName: String? = null) {
        viewModelScope.launch {
            repository.updateShoppingItemQuantity(currentUserId, itemId, newQuantity, isGuestMode, itemName)
                .onSuccess { scheduleAutoDriveSync() }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_update_qty, e.localizedMessage ?: ""))
                }
        }
    }

    fun clearPurchasedItems(items: List<ShoppingItem>) {
        viewModelScope.launch {
            repository.clearPurchasedShoppingItems(currentUserId, items, isGuestMode)
                .onSuccess {
                    _userMessage.emit(str(R.string.vm_cleared_purchased))
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_save, e.localizedMessage ?: ""))
                }
        }
    }

    fun transferPurchasedToPantry(purchased: List<ShoppingItem>, currentPantry: List<PantryItem>) {
        viewModelScope.launch {
            repository.transferPurchasedToPantry(currentUserId, purchased, currentPantry, isGuestMode)
                .onSuccess {
                    _userMessage.emit(str(R.string.vm_transferred_pantry))
                    scheduleAutoDriveSync()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_transfer, e.localizedMessage ?: ""))
                }
        }
    }

    fun addMissingRecipeIngredientsToShoppingList(
        recipe: Recipe,
        targetServings: Int,
        currentPantry: List<PantryItem>
    ) {
        viewModelScope.launch {
            repository.addMissingRecipeIngredientsToShoppingList(
                currentUserId,
                recipe,
                targetServings,
                currentPantry,
                isGuestMode
            ).onSuccess { added ->
                if (added > 0) {
                    _userMessage.emit(str(R.string.vm_added_missing_shop, added))
                } else {
                    _userMessage.emit(str(R.string.vm_all_ingredients_ok))
                }
            }.onFailure { e ->
                _userMessage.emit(str(R.string.vm_err_add_shop, e.localizedMessage ?: ""))
            }
        }
    }

    fun deductPantryStockForProduction(
        recipe: Recipe,
        scaleFactor: Double,
        currentPantry: List<PantryItem>,
        targetServings: Int = (recipe.servings * scaleFactor).toInt().coerceAtLeast(1)
    ) {
        viewModelScope.launch {
            try {
                var deductedCount = 0
                var batchTotalCost = 0.0

                for (ing in recipe.ingredients) {
                    val needed = ing.amount * scaleFactor
                    val match = currentPantry.find { it.name.trim().equals(ing.name.trim(), ignoreCase = true) }

                    val pantryUnitCost = UnitConverter.unitCostInTargetUnit(match, ing.unit)
                    val unitCost = if (pantryUnitCost > 0.0) pantryUnitCost else ing.manualUnitCost
                    batchTotalCost += needed * unitCost

                    if (match != null) {
                        val toDeductInPantryUnit = UnitConverter.deductAmountInPantryUnit(needed, ing.unit, match.unit) ?: needed
                        val newQty = (match.quantity - toDeductInPantryUnit).coerceAtLeast(0.0)
                        repository.updatePantryStock(currentUserId, match.id, newQty, isGuestMode).getOrThrow()
                        deductedCount++
                    }
                }

                val currency = AppSettingsPreferences.getCurrency(repository.context)
                val marginPercent = AppSettingsPreferences.getMarginPercent(repository.context)
                val suggestedPrice = AppSettingsPreferences.suggestedSellingPrice(batchTotalCost, marginPercent)

                repository.recordProductionLog(
                    userId = currentUserId,
                    log = ProductionLog(
                        recipeId = recipe.id,
                        recipeTitle = recipe.title,
                        targetServings = targetServings,
                        scaleFactor = scaleFactor,
                        totalCost = batchTotalCost,
                        currencyCode = currency.code,
                        marginPercent = marginPercent,
                        suggestedPrice = suggestedPrice,
                        deductedIngredientsCount = deductedCount
                    ),
                    isGuestMode = isGuestMode
                ).getOrThrow()

                _userMessage.emit(str(R.string.vm_production_recorded, deductedCount))
                scheduleAutoDriveSync()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _userMessage.emit(str(R.string.vm_err_deduct, e.localizedMessage ?: ""))
            }
        }
    }

    fun addProductionShortageToShoppingList(
        recipe: Recipe,
        scaleFactor: Double,
        currentPantry: List<PantryItem>,
        targetUnitsLabel: String
    ) {
        viewModelScope.launch {
            try {
                var addedCount = 0
                for (ingredient in recipe.ingredients) {
                    val neededAmount = Math.round(ingredient.amount * scaleFactor * 1000.0) / 1000.0
                    val matching = currentPantry.find {
                        it.name.trim().equals(ingredient.name.trim(), ignoreCase = true)
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
                            recipeSource = "${recipe.title} ($targetUnitsLabel)"
                        )
                        repository.saveShoppingItem(currentUserId, shoppingItem, isGuestMode, syncToPantry = false).getOrThrow()
                        addedCount++
                    }
                }
                if (addedCount > 0) {
                    _userMessage.emit(str(R.string.vm_added_shortage_shop, addedCount))
                } else {
                    _userMessage.emit(str(R.string.vm_sufficient_stock))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _userMessage.emit(str(R.string.vm_err_add_shop, e.localizedMessage ?: ""))
            }
        }
    }

    // Helper: calculate feasibility for recipes based on current pantry (unit-aware)
    fun calculateRecipeFeasibilities(
        recipes: List<Recipe>,
        pantry: List<PantryItem>
    ): List<RecipeFeasibility> {
        return recipes.map { recipe ->
            var available = 0
            val missing = mutableListOf<Pair<String, Double>>()

            for (ing in recipe.ingredients) {
                val inPantry = pantry.find {
                    it.name.trim().equals(ing.name.trim(), ignoreCase = true)
                }
                val hasQty = UnitConverter.availableInTargetUnit(inPantry, ing.unit)
                if (hasQty + 0.0001 >= ing.amount) {
                    available++
                } else {
                    val shortage = (ing.amount - hasQty).coerceAtLeast(0.0)
                    missing.add(Pair(ing.name, shortage))
                }
            }

            val total = recipe.ingredients.size
            val pct = if (total == 0) 100 else ((available.toDouble() / total) * 100).toInt()

            RecipeFeasibility(
                recipe = recipe,
                matchPercentage = pct,
                availableCount = available,
                totalCount = total,
                missingIngredients = missing
            )
        }.sortedByDescending { it.matchPercentage }
    }

    // --- COMMERCIAL: MIGRATION, FULL BACKUP, LICENSE, UPDATES, ACCOUNT DELETION ---

    fun migrateGuestDataToCloud() {
        if (isGuestMode) return
        viewModelScope.launch {
            repository.migrateGuestDataToCloud(currentUserId)
                .onSuccess { count ->
                    _localGuestCountForMigration.value = 0
                    _userMessage.emit(str(R.string.vm_migrated_guest, count))
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_import, e.localizedMessage ?: ""))
                }
        }
    }

    fun exportFullBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.exportFullBackup(currentUserId, isGuestMode)
                .onSuccess { payload ->
                    val json = FullBackupManager.exportToJson(
                        recipes = payload.recipes,
                        pantry = payload.pantry,
                        shopping = payload.shopping,
                        productionLogs = payload.productionLogs,
                        currencyCode = payload.currencyCode ?: "USD",
                        marginPercent = payload.marginPercent ?: 30
                    )
                    onResult(json)
                    _userMessage.emit(str(R.string.vm_backup_exported, payload.recipes.size, payload.pantry.size))
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_import, e.localizedMessage ?: ""))
                }
        }
    }

    fun exportAiReadableMarkdown(onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.exportFullBackup(currentUserId, isGuestMode)
                .onSuccess { payload ->
                    val md = AiReadableExportFormatter.exportToMarkdown(
                        context = repository.context,
                        recipes = payload.recipes,
                        pantry = payload.pantry,
                        shopping = payload.shopping,
                        productionLogs = payload.productionLogs,
                        currencyCode = payload.currencyCode ?: AppSettingsPreferences.getCurrency(repository.context).code,
                        marginPercent = payload.marginPercent ?: AppSettingsPreferences.getMarginPercent(repository.context)
                    )
                    onResult(md)
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_import, e.localizedMessage ?: ""))
                }
        }
    }

    fun exportAiReadableCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.exportFullBackup(currentUserId, isGuestMode)
                .onSuccess { payload ->
                    val csv = AiReadableExportFormatter.exportToCsv(
                        context = repository.context,
                        recipes = payload.recipes,
                        pantry = payload.pantry,
                        shopping = payload.shopping
                    )
                    onResult(csv)
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_import, e.localizedMessage ?: ""))
                }
        }
    }

    fun exportAiReadableHtmlDoc(onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.exportFullBackup(currentUserId, isGuestMode)
                .onSuccess { payload ->
                    val html = AiReadableExportFormatter.exportToGoogleDocHtml(
                        context = repository.context,
                        recipes = payload.recipes,
                        pantry = payload.pantry,
                        shopping = payload.shopping,
                        productionLogs = payload.productionLogs,
                        currencyCode = payload.currencyCode ?: AppSettingsPreferences.getCurrency(repository.context).code,
                        marginPercent = payload.marginPercent ?: AppSettingsPreferences.getMarginPercent(repository.context)
                    )
                    onResult(html)
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_import, e.localizedMessage ?: ""))
                }
        }
    }

    fun setDriveAutoSyncEnabled(enabled: Boolean) {
        AppSettingsPreferences.setDriveAutoSyncEnabled(repository.context, enabled)
        if (enabled && (!isGuestMode || driveSyncInfo.value.lastSyncMillis > 0L)) {
            scheduleAutoDriveSync()
        }
    }

    fun reportDriveSyncMessage(message: String, isError: Boolean = false) {
        if (isError) {
            _driveSyncError.value = message
        } else {
            _driveActivationUrl.value = null
        }
        viewModelScope.launch {
            _userMessage.emit(message)
        }
    }

    fun syncReadableInventoryToDriveWithToken(
        accessToken: String,
        notifyUser: Boolean = true
    ) {
        if (_driveSyncing.value) return
        viewModelScope.launch {
            _driveSyncing.value = true
            _driveSyncError.value = null
            _driveActivationUrl.value = null
            try {
                val payload = repository.exportFullBackup(currentUserId, isGuestMode).getOrThrow()
                val res = GoogleDriveSyncRepository.syncReadableFilesToDrive(
                    context = repository.context,
                    accessToken = accessToken,
                    recipes = payload.recipes,
                    pantry = payload.pantry,
                    shopping = payload.shopping,
                    productionLogs = payload.productionLogs,
                    currencyCode = payload.currencyCode ?: AppSettingsPreferences.getCurrency(repository.context).code,
                    marginPercent = payload.marginPercent ?: AppSettingsPreferences.getMarginPercent(repository.context),
                    linkedEmail = currentUserEmail
                )
                res.onSuccess {
                    _driveActivationUrl.value = null
                    if (notifyUser) {
                        _userMessage.emit(str(R.string.drive_ai_sync_success))
                    }
                }.onFailure { err ->
                    if (err is DriveApiDisabledException) {
                        val friendly = err.message ?: str(R.string.drive_api_disabled_friendly, "1034453362537")
                        _driveActivationUrl.value = err.activationUrl
                        _driveSyncError.value = friendly
                        if (notifyUser) {
                            _userMessage.emit(friendly)
                        }
                    } else {
                        val msg = str(R.string.drive_ai_sync_error, err.localizedMessage ?: "")
                        _driveSyncError.value = msg
                        if (notifyUser) {
                            _userMessage.emit(msg)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = str(R.string.drive_ai_sync_error, e.localizedMessage ?: "")
                _driveSyncError.value = msg
                if (notifyUser) {
                    _userMessage.emit(msg)
                }
            } finally {
                _driveSyncing.value = false
            }
        }
    }

    private fun scheduleAutoDriveSync() {
        val info = AppSettingsPreferences.getDriveSyncInfo(repository.context)
        if (!info.autoSyncEnabled) return
        if (isGuestMode && info.lastSyncMillis <= 0L) return

        autoDriveSyncJob?.cancel()
        autoDriveSyncJob = viewModelScope.launch {
            delay(2_200L)
            val silentToken = GoogleDriveSyncRepository.trySilentAccessToken(repository.context)
            if (!silentToken.isNullOrBlank()) {
                syncReadableInventoryToDriveWithToken(accessToken = silentToken, notifyUser = false)
            }
        }
    }

    fun importFullBackupJson(
        rawText: String,
        replaceExisting: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val parsed = FullBackupManager.parseFromJson(rawText)
        if (parsed == null) {
            viewModelScope.launch {
                _userMessage.emit(str(R.string.vm_backup_invalid))
            }
            onComplete(false)
            return
        }
        viewModelScope.launch {
            repository.importFullBackup(currentUserId, parsed, replaceExisting, isGuestMode)
                .onSuccess {
                    _userMessage.emit(str(R.string.vm_backup_imported, parsed.recipes.size, parsed.pantry.size))
                    onComplete(true)
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_import, e.localizedMessage ?: ""))
                    onComplete(false)
                }
        }
    }

    fun activateLicense(code: String) {
        viewModelScope.launch {
            if (isGuestMode) {
                _userMessage.emit(str(R.string.cloud_feature_requires_google))
                return@launch
            }
            repository.verifyOrClaimLicense(currentUserId, code)
                .onSuccess {
                    _licenseValidated.value = true
                    _userMessage.emit(str(R.string.license_activated_msg))
                }
                .onFailure {
                    _userMessage.emit(str(R.string.license_invalid_msg))
                }
        }
    }

    private fun resolveEffectiveGithubRepo(): String {
        val localRepo = AppSettingsPreferences.getGithubRepo(repository.context)
        if (localRepo.isNotBlank()) return GitHubUpdateChecker.normalizeRepoSlug(localRepo)
        val remoteRepo = _distributionConfig.value?.githubRepo.orEmpty()
        return GitHubUpdateChecker.normalizeRepoSlug(remoteRepo)
    }

    fun checkUpdatesManual(currentVersionCode: Int, currentVersionName: String, onUpdateFound: (DistributionConfig) -> Unit) {
        viewModelScope.launch {
            val firestoreCfg = if (!isGuestMode) repository.fetchDistributionConfig() else null
            if (firestoreCfg != null) {
                _distributionConfig.value = firestoreCfg
            }
            val repoSlug = AppSettingsPreferences.getGithubRepo(repository.context)
                .ifBlank { firestoreCfg?.githubRepo.orEmpty() }
                .let { GitHubUpdateChecker.normalizeRepoSlug(it) }

            val githubCfg = if (repoSlug.isNotBlank()) {
                GitHubUpdateChecker.fetchLatestRelease(
                    repoInput = repoSlug,
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                ).getOrNull()
            } else null

            val merged = listOfNotNull(firestoreCfg, githubCfg)
                .maxByOrNull { it.latestVersionCode }

            if (merged != null) {
                _distributionConfig.value = merged
            }

            if (merged != null && merged.latestVersionCode > currentVersionCode) {
                onUpdateFound(merged)
            } else if (repoSlug.isBlank() && isGuestMode) {
                _userMessage.emit(str(R.string.settings_github_missing_repo))
            } else {
                _userMessage.emit(str(R.string.update_up_to_date, currentVersionName))
            }
        }
    }

    fun deleteAccountAndCloudData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccountAndCloudData(currentUserId, isGuestMode)
                .onSuccess {
                    _userMessage.emit(str(R.string.vm_account_deleted))
                    onDeleted()
                }
                .onFailure { e ->
                    _userMessage.emit(str(R.string.vm_err_delete, e.localizedMessage ?: ""))
                }
        }
    }

    fun retryCloudSync() {
        viewModelScope.launch {
            repository.ensureSessionBootstrapped(currentUserId, isGuestMode, forceRefresh = true)
                .onSuccess {
                    val msg = if (isGuestMode) {
                        str(R.string.vm_sync_reloaded_guest)
                    } else {
                        str(R.string.vm_sync_reloaded_cloud)
                    }
                    _userMessage.emit(msg)
                }
                .onFailure { e ->
                    val errText = e.localizedMessage ?: ""
                    val msg = if (isGuestMode) {
                        str(R.string.vm_sync_err_guest, errText)
                    } else {
                        str(R.string.vm_sync_err_cloud, errText)
                    }
                    _userMessage.emit(msg)
                }
        }
    }
}
