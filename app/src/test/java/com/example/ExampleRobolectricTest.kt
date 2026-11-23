package com.example

import android.content.Context
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.example.ai.RecetarioAppFunctions
import com.example.ai.VoiceCommandRouter
import com.example.auth.SessionPreferences
import com.example.data.local.CookingDatabase
import com.example.data.local.entity.PantryEntity
import com.example.data.local.entity.ProductionLogEntity
import com.example.data.local.entity.RecipeEntity
import com.example.data.local.entity.ShoppingEntity
import com.example.data.model.PantryItem
import com.example.data.model.ProductionLog
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.ShoppingItem
import com.example.data.repository.CookingRepository
import com.example.data.sync.FullBackupManager
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.ui.MainViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CookingAssistantScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LocaleHelper
import com.example.util.UnitConverter
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.graphics.Bitmap
import androidx.compose.ui.test.performTextInput
import com.example.ai.RecipePhotoGenState
import com.example.ai.RecipePhotoGenerator
import com.example.ui.screens.RecipeFormDialog
import com.example.util.ImageUtils
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun testAppFunctionContext(appContext: Context): AppFunctionContext =
        object : AppFunctionContext {
            override val context: Context = appContext
        }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Recetario", appName)
    }

    @Test
    fun `sessionPreferences toggles and retains guest mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SessionPreferences.setGuestMode(context, false)
        assertFalse(SessionPreferences.isGuestMode(context))

        SessionPreferences.setGuestMode(context, true)
        assertTrue(SessionPreferences.isGuestMode(context))
        assertTrue(SessionPreferences.observeGuestMode(context).value)

        SessionPreferences.setGuestMode(context, false)
        assertFalse(SessionPreferences.isGuestMode(context))
    }

    @Test
    fun `guest room database persists recipes and pantry strictly on device`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = CookingDatabase.resetDatabase(context)

        val localRecipe = Recipe(
            id = "guest_rec_1",
            userId = CookingRepository.GUEST_USER_ID,
            title = "Pan Casero Invitado",
            servings = 4
        )
        val localPantry = PantryItem(
            id = "guest_pan_1",
            userId = CookingRepository.GUEST_USER_ID,
            name = "Harina Local",
            quantity = 1000.0,
            unit = "g"
        )

        db.recipeDao().insertOrUpdate(RecipeEntity.fromDomain(localRecipe))
        db.pantryDao().insertOrUpdate(PantryEntity.fromDomain(localPantry))

        val savedRecipes = db.recipeDao().getAllRecipesList().map { it.toDomain() }
        val savedPantry = db.pantryDao().getAllPantryItemsList().map { it.toDomain() }

        assertEquals(1, savedRecipes.size)
        assertEquals("guest_rec_1", savedRecipes.first().id)
        assertEquals("Pan Casero Invitado", savedRecipes.first().title)

        assertEquals(1, savedPantry.size)
        assertEquals("guest_pan_1", savedPantry.first().id)
        assertEquals("Harina Local", savedPantry.first().name)
        assertEquals(1000.0, savedPantry.first().quantity, 0.001)
    }

    @Test
    fun `auth screen exposes guest mode entry button and invokes callback`() {
        var guestClicked = false
        composeRule.setContent {
            MyApplicationTheme {
                AuthScreen(
                    onAuthSuccess = {},
                    onContinueAsGuest = { guestClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("guest_signin_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(guestClicked)
    }

    @Test
    fun `proportional recipe scaling calculation is accurate`() {
        val baseRecipe = Recipe(
            title = "Tortilla",
            servings = 4,
            prepTimeMinutes = 15,
            cookTimeMinutes = 20,
            ingredients = listOf(
                RecipeIngredient(name = "Huevos", amount = 6.0, unit = "unidades"),
                RecipeIngredient(name = "Patatas", amount = 600.0, unit = "g")
            )
        )

        val targetServings = 8
        val scale = targetServings.toDouble() / baseRecipe.servings

        assertEquals(12.0, baseRecipe.ingredients[0].amount * scale, 0.001)
        assertEquals(1200.0, baseRecipe.ingredients[1].amount * scale, 0.001)

        val halfScale = 2.0 / baseRecipe.servings
        assertEquals(3.0, baseRecipe.ingredients[0].amount * halfScale, 0.001)
        assertEquals(300.0, baseRecipe.ingredients[1].amount * halfScale, 0.001)
    }

    @Test
    fun `pantry shortage evaluation identifies missing items`() {
        val recipe = Recipe(
            title = "Pasta",
            servings = 2,
            ingredients = listOf(
                RecipeIngredient(name = "Pasta", amount = 200.0, unit = "g"),
                RecipeIngredient(name = "Tomate frito", amount = 150.0, unit = "g")
            )
        )

        val pantry = listOf(
            PantryItem(name = "Pasta", quantity = 500.0, unit = "g"),
            PantryItem(name = "Tomate frito", quantity = 50.0, unit = "g")
        )

        val pastaInStock = pantry.find { it.name.equals(recipe.ingredients[0].name, true) }?.quantity ?: 0.0
        assertTrue(pastaInStock >= recipe.ingredients[0].amount)

        val tomateInStock = pantry.find { it.name.equals(recipe.ingredients[1].name, true) }?.quantity ?: 0.0
        val shortage = recipe.ingredients[1].amount - tomateInStock
        assertEquals(100.0, shortage, 0.001)
    }

    @Test
    fun `appFunctions rejects unauthenticated non-guest queries cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseAuth.getInstance().signOut()
        SessionPreferences.setGuestMode(context, false)

        val appFunctions = RecetarioAppFunctions()
        var capturedError: Throwable? = null
        try {
            appFunctions.consultarStockDespensa(testAppFunctionContext(context), null)
        } catch (t: Throwable) {
            capturedError = t
        }

        assertTrue(capturedError is AppFunctionElementNotFoundException)
    }

    @Test
    fun `appFunctions answers all 5 voice provider queries from guest DB without opening app`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseAuth.getInstance().signOut()
        SessionPreferences.setGuestMode(context, true)
        AppSettingsPreferences.setLanguageCode(context, "es")

        val db = CookingDatabase.resetDatabase(context)
        db.pantryDao().insertOrUpdate(
            PantryEntity.fromDomain(
                PantryItem(
                    id = "p1",
                    userId = CookingRepository.GUEST_USER_ID,
                    name = "Huevos",
                    category = "Lácteos",
                    quantity = 12.0,
                    unit = "unidades",
                    minQuantity = 4.0,
                    cost = 3.60,
                    costBaseQuantity = 12.0
                )
            )
        )
        db.pantryDao().insertOrUpdate(
            PantryEntity.fromDomain(
                PantryItem(
                    id = "p2",
                    userId = CookingRepository.GUEST_USER_ID,
                    name = "Harina",
                    category = "Granos",
                    quantity = 100.0,
                    unit = "g",
                    minQuantity = 500.0
                )
            )
        )
        db.recipeDao().insertOrUpdate(
            RecipeEntity.fromDomain(
                Recipe(
                    id = "r1",
                    userId = CookingRepository.GUEST_USER_ID,
                    title = "Huevos Revueltos",
                    category = "Desayunos",
                    servings = 2,
                    prepTimeMinutes = 5,
                    cookTimeMinutes = 5,
                    ingredients = listOf(
                        RecipeIngredient(name = "Huevos", amount = 4.0, unit = "unidades")
                    )
                )
            )
        )
        db.shoppingDao().insertOrUpdate(
            ShoppingEntity.fromDomain(
                ShoppingItem(
                    id = "s1",
                    userId = CookingRepository.GUEST_USER_ID,
                    name = "Harina",
                    quantity = 1.0,
                    unit = "kg",
                    isPurchased = false
                )
            )
        )
        db.shoppingDao().insertOrUpdate(
            ShoppingEntity.fromDomain(
                ShoppingItem(
                    id = "s2",
                    userId = CookingRepository.GUEST_USER_ID,
                    name = "Sal",
                    quantity = 500.0,
                    unit = "g",
                    isPurchased = true
                )
            )
        )

        val appFunctions = RecetarioAppFunctions()
        val fnCtx = testAppFunctionContext(context)

        val stockResult = appFunctions.consultarStockDespensa(fnCtx, "huevos")
        assertEquals(1, stockResult.totalItemsCount)
        assertTrue(stockResult.summaryText.contains("Huevos: 12 unidades"))

        val lowResult = appFunctions.listarStockBajoDespensa(fnCtx)
        assertEquals(1, lowResult.totalItemsCount)
        assertEquals("Harina", lowResult.items.first().name)

        val shoppingPending = appFunctions.consultarListaCompras(fnCtx, soloPendientes = true)
        assertEquals(1, shoppingPending.totalCount)
        assertEquals(1, shoppingPending.pendingCount)
        assertTrue(shoppingPending.summaryText.contains("Harina"))

        val availableRecipes = appFunctions.listarRecetasDisponibles(fnCtx, null)
        assertEquals(1, availableRecipes.totalRecipesCount)
        assertEquals(1, availableRecipes.readyNowCount)
        assertTrue(availableRecipes.summaryText.contains("Huevos Revueltos"))

        val production = appFunctions.verificarProduccionReceta(fnCtx, "Huevos Revueltos", 4)
        assertTrue(production.canProduceAll)
        assertEquals(4, production.targetServings)

        SessionPreferences.setGuestMode(context, false)
        AppSettingsPreferences.setLanguageCode(context, "")
    }

    @Test
    fun `cooking assistant screen shows voice provider card and has no in-app AI chat`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        SessionPreferences.setGuestMode(context, true)
        val viewModel = MainViewModel(
            repository = CookingRepository(context),
            currentUserId = CookingRepository.GUEST_USER_ID,
            currentUserName = "Invitado",
            isGuestMode = true
        )

        composeRule.setContent {
            MyApplicationTheme {
                CookingAssistantScreen(
                    viewModel = viewModel,
                    recipes = emptyList(),
                    pantryItems = emptyList(),
                    onSelectRecipe = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("voice_provider_info_card").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_ask_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_manual_query_input").assertIsDisplayed()
        composeRule.onNodeWithTag("gemini_prompt_input").assertDoesNotExist()
        SessionPreferences.setGuestMode(context, false)
    }

    @Test
    fun `voiceCommandRouter parses multilingual queries and executes against guest DB`() = runBlocking {
        // Parsing checks across ES / EN / PT / FR
        val eggLookup = VoiceCommandRouter.parseIntent("¿Cuánto tengo de huevos en Recetario?")
        assertTrue(eggLookup is VoiceCommandRouter.ParsedVoiceIntent.PantryLookup)
        assertEquals("huevos", (eggLookup as VoiceCommandRouter.ParsedVoiceIntent.PantryLookup).ingredientQuery)

        assertTrue(VoiceCommandRouter.parseIntent("stock bajo en despensa") is VoiceCommandRouter.ParsedVoiceIntent.LowStock)
        assertTrue(VoiceCommandRouter.parseIntent("What pantry items are low or out?") is VoiceCommandRouter.ParsedVoiceIntent.LowStock)

        val shopIntent = VoiceCommandRouter.parseIntent("lista de compras pendiente")
        assertTrue(shopIntent is VoiceCommandRouter.ParsedVoiceIntent.ShoppingList)
        assertTrue((shopIntent as VoiceCommandRouter.ParsedVoiceIntent.ShoppingList).onlyPending)

        assertTrue(VoiceCommandRouter.parseIntent("What can I cook today?") is VoiceCommandRouter.ParsedVoiceIntent.AvailableRecipes)
        assertTrue(VoiceCommandRouter.parseIntent("O que posso cozinhar hoje?") is VoiceCommandRouter.ParsedVoiceIntent.AvailableRecipes)

        val feasIntent = VoiceCommandRouter.parseIntent("¿Me alcanza para 8 porciones de Tortilla en Recetario?")
        assertTrue(feasIntent is VoiceCommandRouter.ParsedVoiceIntent.RecipeFeasibility)
        val castFeas = feasIntent as VoiceCommandRouter.ParsedVoiceIntent.RecipeFeasibility
        assertEquals(8, castFeas.servings)
        assertEquals("tortilla", castFeas.recipeQuery)

        // End-to-end execution with singular/plural ingredient matching on Guest Room DB
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseAuth.getInstance().signOut()
        SessionPreferences.setGuestMode(context, true)
        AppSettingsPreferences.setLanguageCode(context, "es")

        val db = CookingDatabase.resetDatabase(context)
        db.pantryDao().insertOrUpdate(
            PantryEntity.fromDomain(
                PantryItem(
                    id = "p1",
                    userId = CookingRepository.GUEST_USER_ID,
                    name = "Huevos",
                    quantity = 10.0,
                    unit = "unidades",
                    minQuantity = 4.0
                )
            )
        )
        db.recipeDao().insertOrUpdate(
            RecipeEntity.fromDomain(
                Recipe(
                    id = "r1",
                    userId = CookingRepository.GUEST_USER_ID,
                    title = "Tortilla",
                    servings = 4,
                    ingredients = listOf(RecipeIngredient(name = "Huevos", amount = 4.0, unit = "unidades"))
                )
            )
        )

        val singularLookup = VoiceCommandRouter.executeVoiceQuery(context, "cuanto tengo de huevo")
        assertTrue(singularLookup.isSuccess)
        assertTrue(singularLookup.summaryText.contains("Huevos: 10"))

        val feasResult = VoiceCommandRouter.executeVoiceQuery(context, "¿Me alcanza para 8 porciones de Tortilla?")
        assertTrue(feasResult.isSuccess)
        assertTrue(feasResult.summaryText.contains("Tortilla"))

        SessionPreferences.setGuestMode(context, false)
        AppSettingsPreferences.setLanguageCode(context, "")
    }

    @Test
    fun `unitConverter converts mass volume and count dimensions and rejects mismatched dimensions`() {
        assertEquals(1500.0, UnitConverter.convert(1.5, "kg", "g")!!, 0.001)
        assertEquals(0.25, UnitConverter.convert(250.0, "ml", "l")!!, 0.001)
        assertEquals(240.0, UnitConverter.convert(1.0, "taza", "ml")!!, 0.001)
        assertEquals(24.0, UnitConverter.convert(2.0, "docenas", "unidades")!!, 0.001)
        assertNull(UnitConverter.convert(100.0, "g", "ml"))

        val flourKg = PantryItem(name = "Harina", quantity = 2.0, unit = "kg", cost = 4.0, costBaseQuantity = 2.0)
        assertEquals(2000.0, UnitConverter.availableInTargetUnit(flourKg, "g"), 0.001)
        assertEquals(0.002, UnitConverter.unitCostInTargetUnit(flourKg, "g"), 0.00001)
    }

    @Test
    fun `cookingDatabase v6 persists production logs and runs migration 5 to 6`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = CookingDatabase.resetDatabase(context)

        val log = ProductionLog(
            id = "log_1",
            recipeId = "r1",
            recipeTitle = "Brioche",
            targetServings = 12,
            scaleFactor = 2.0,
            totalCost = 10.0,
            currencyCode = "EUR",
            marginPercent = 30,
            suggestedPrice = 13.0
        )
        db.productionLogDao().insertOrUpdate(ProductionLogEntity.fromDomain(log))

        val savedLogs = db.productionLogDao().getAllLogsList().map { it.toDomain() }
        assertEquals(1, savedLogs.size)
        assertEquals("Brioche", savedLogs.first().recipeTitle)
        assertEquals("EUR", savedLogs.first().currencyCode)
        assertEquals(13.0, savedLogs.first().suggestedPrice, 0.001)

        // Verify MIGRATION_5_6 SQL executes cleanly
        val rawDb = db.openHelper.writableDatabase
        rawDb.execSQL("DROP TABLE IF EXISTS `production_logs`")
        CookingDatabase.MIGRATION_5_6.migrate(rawDb)
        rawDb.query(SimpleSQLiteQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='production_logs'")).use { c ->
            assertTrue(c.moveToFirst())
        }
    }

    @Test
    fun `fullBackupManager exports and parses complete backup round-trip`() {
        val json = FullBackupManager.exportToJson(
            recipes = listOf(Recipe(id = "r1", title = "Taco", servings = 4, ingredients = listOf(RecipeIngredient(name = "Maíz", amount = 200.0, unit = "g")))),
            pantry = listOf(PantryItem(id = "p1", name = "Maíz", quantity = 1000.0, unit = "g", cost = 2.5, costBaseQuantity = 1000.0)),
            shopping = listOf(ShoppingItem(id = "s1", name = "Limón", quantity = 4.0, unit = "unidades", isPurchased = false)),
            productionLogs = listOf(ProductionLog(id = "l1", recipeId = "r1", recipeTitle = "Taco", targetServings = 8, scaleFactor = 2.0, totalCost = 1.0, currencyCode = "USD", marginPercent = 40, suggestedPrice = 1.4)),
            currencyCode = "USD",
            marginPercent = 40
        )

        val parsed = FullBackupManager.parseFromJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.recipes.size)
        assertEquals("Taco", parsed.recipes.first().title)
        assertEquals(1, parsed.pantry.size)
        assertEquals(1, parsed.shopping.size)
        assertEquals(1, parsed.productionLogs.size)
        assertEquals(40, parsed.marginPercent)
        assertNull(FullBackupManager.parseFromJson("not-json-at-all"))
    }

    @Test
    fun `localeHelper resolves es en pt fr strings and appFunctions responds in active language`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseAuth.getInstance().signOut()
        SessionPreferences.setGuestMode(context, true)

        val expectations = mapOf(
            "es" to "Recetas",
            "en" to "Recipes",
            "pt" to "Receitas",
            "fr" to "Recettes"
        )
        expectations.forEach { (code, expectedTab) ->
            AppSettingsPreferences.setLanguageCode(context, code)
            val localized = LocaleHelper.localizedContext(context)
            assertEquals(expectedTab, localized.getString(R.string.tab_recipes))
        }

        // Verify AppFunctions uses English when language is set to "en"
        val db = CookingDatabase.resetDatabase(context)
        db.pantryDao().insertOrUpdate(
            PantryEntity.fromDomain(
                PantryItem(id = "p1", userId = CookingRepository.GUEST_USER_ID, name = "Eggs", quantity = 6.0, unit = "unidades", minQuantity = 2.0)
            )
        )
        AppSettingsPreferences.setLanguageCode(context, "en")
        val appFunctions = RecetarioAppFunctions()
        val stockResult = appFunctions.consultarStockDespensa(testAppFunctionContext(context), "eggs")
        assertTrue(stockResult.summaryText.contains("units"))

        AppSettingsPreferences.setLanguageCode(context, "")
        SessionPreferences.setGuestMode(context, false)
    }

    @Test
    fun `appSettingsPreferences formats currencies and computes profit margin pricing`() {
        assertEquals(130.0, AppSettingsPreferences.suggestedSellingPrice(100.0, 30), 0.001)
        assertEquals(30.0, AppSettingsPreferences.estimatedProfit(100.0, 30), 0.001)
        assertEquals("€12.50", AppSettingsPreferences.formatMoney(12.5, CurrencyOption.EUR))
        assertEquals("$0.0020", AppSettingsPreferences.formatMoney(0.002, CurrencyOption.USD, forceFourDecimalsIfTiny = true))
        assertEquals("R$10.00", AppSettingsPreferences.formatMoney(10.0, CurrencyOption.BRL))
    }

    @Test
    fun `commercial distribution and license models align with firestore schema`() {
        val dist = com.example.data.model.DistributionConfig(
            latestVersionCode = 14,
            latestVersionName = "3.1",
            minRequiredVersionCode = 13,
            updateUrl = "https://example.com/Recetario-v3.1.apk",
            updateNotes = "Mejoras",
            requireLicense = true,
            supportContact = "soporte@example.com"
        )
        assertEquals(14, dist.latestVersionCode)
        assertTrue(dist.requireLicense)

        val license = com.example.data.model.LicenseRecord(
            code = "REC-PRO-2026",
            active = true,
            assignedUid = "uid_123",
            assignedEmail = "buyer@example.com"
        )
        assertEquals("uid_123", license.assignedUid)
        assertTrue(license.active)
    }

    @Test
    fun `imageUtils resolves referential food photos https urls and encodes compact data uri`() {
        assertEquals(5, ImageUtils.referentialPhotoPresets.size)
        assertEquals(
            R.drawable.img_ref_tortilla_patatas,
            ImageUtils.toImageModel(ImageUtils.REF_TORTILLA)
        )
        assertEquals(
            R.drawable.img_ref_pasta_pollo,
            ImageUtils.toImageModel(ImageUtils.REF_PASTA)
        )
        assertEquals(
            "https://example.com/dish.jpg",
            ImageUtils.toImageModel("https://example.com/dish.jpg")
        )

        val sampleBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val dataUri = ImageUtils.encodeBitmapToDataUri(sampleBitmap, recycleSource = true)
        assertTrue(dataUri.startsWith("data:image/jpeg;base64,"))
        assertTrue(dataUri.length <= 80_000)
        assertTrue(ImageUtils.toImageModel(dataUri) is ByteArray)
    }

    @Test
    fun `recipePhotoGenerator builds photorealistic non cartoon culinary prompt`() {
        val prompt = RecipePhotoGenerator.buildPhotorealisticPrompt(
            title = "Paella Valenciana",
            category = "Almuerzos",
            ingredients = listOf(
                RecipeIngredient(name = "Arroz", amount = 300.0, unit = "g"),
                RecipeIngredient(name = "Azafrán", amount = 1.0, unit = "pizca")
            )
        )
        assertTrue(prompt.contains("Paella Valenciana"))
        assertTrue(prompt.contains("Arroz"))
        assertTrue(prompt.contains("Photorealistic DSLR food photography"))
        assertTrue(prompt.contains("NO cartoon"))
    }

    @Test
    fun `recipeFormDialog exposes AI Web Gallery and Referential photo controls and saves selected imageUrl`() {
        var savedRecipe: Recipe? = null
        composeRule.setContent {
            MyApplicationTheme {
                RecipeFormDialog(
                    initialRecipe = null,
                    photoGenState = RecipePhotoGenState.Idle,
                    onGenerateAiPhoto = { _, _, _ -> },
                    onDismiss = {},
                    onSave = { savedRecipe = it }
                )
            }
        }

        composeRule.onNodeWithTag("recipe_form_ai_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("recipe_form_web_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("recipe_form_gallery_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("recipe_form_ref_0").assertIsDisplayed().performClick()

        composeRule.onNodeWithTag("recipe_form_title")
            .performScrollTo()
            .performTextInput("Arroz con Pollo")
        composeRule.onNodeWithTag("recipe_form_save").performClick()

        assertNotNull(savedRecipe)
        assertEquals("Arroz con Pollo", savedRecipe!!.title)
        assertEquals(ImageUtils.referentialPhotoPresets[0].uriKey, savedRecipe!!.imageUrl)
    }
}
