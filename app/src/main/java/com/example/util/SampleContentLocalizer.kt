package com.example.util

import android.content.Context
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.ProductionLog
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.ShoppingItem
import com.example.settings.AppSettingsPreferences
import java.text.Normalizer
import java.util.Locale

/**
 * Provides full localization (`es`, `en`, `pt`, `fr`) for all seeded starter recipes, ingredients,
 * instructions, chef notes, pantry products, and shopping sources — both when seeding new
 * workspaces and dynamically on screen/export/voice read when the user switches app language.
 * Also supports cross-language ingredient-to-pantry matching (e.g., "Huevos" <-> "Eggs" <-> "Ovos" <-> "Œufs").
 */
object SampleContentLocalizer {

    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val SERVINGS_SUFFIX_REGEX = Regex("""^(.*)\((\d+)\s*(?:porc\.?|serv\.?|porç\.?|port\.?|porciones|servings|porções|portions)\)$""", RegexOption.IGNORE_CASE)

    private data class Quad(
        val es: String,
        val en: String,
        val pt: String,
        val fr: String
    ) {
        fun forLang(lang: String): String = when (lang) {
            "en" -> en
            "pt" -> pt
            "fr" -> fr
            else -> es
        }
    }

    private fun normalize(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "").replace(Regex("\\s+"), " ").trim()
    }

    private fun resolveLang(context: Context): String {
        val explicit = AppSettingsPreferences.getLanguageCode(context).trim().lowercase(Locale.ROOT)
        val raw = explicit.ifBlank {
            context.resources.configuration.locales[0]?.language?.lowercase(Locale.ROOT)
                ?: Locale.getDefault().language.lowercase(Locale.ROOT)
        }
        return when {
            raw.startsWith("en") -> "en"
            raw.startsWith("pt") -> "pt"
            raw.startsWith("fr") -> "fr"
            else -> "es"
        }
    }

    // --- 3 Starter Recipe Titles ---
    private val titleTortilla = Quad(
        es = "Tortilla de Patatas Tradicional",
        en = "Traditional Spanish Potato Omelette",
        pt = "Tortilha de Batatas Tradicional",
        fr = "Omelette Espagnole Traditionnelle"
    )
    private val titlePasta = Quad(
        es = "Pasta Cremosa con Pollo y Champiñones",
        en = "Creamy Chicken & Mushroom Pasta",
        pt = "Massa Cremosa com Frango e Cogumelos",
        fr = "Pâtes Crémeuses au Poulet et Champignons"
    )
    private val titlePancakes = Quad(
        es = "Panqueques Esponjosos de Avena y Plátano",
        en = "Fluffy Oat & Banana Pancakes",
        pt = "Panquecas Fofinhas de Aveia e Banana",
        fr = "Pancakes Moelleux à l'Avoine et Banane"
    )
    private val allTitles = listOf(titleTortilla, titlePasta, titlePancakes)

    // --- Starter Ingredients & Pantry Items (with canonical keys for cross-language matching) ---
    private val itemEntries: List<Pair<String, Quad>> = listOf(
        "ing_potatoes" to Quad("Patatas", "Potatoes", "Batatas", "Pommes de terre"),
        "ing_eggs" to Quad("Huevos", "Eggs", "Ovos", "Œufs"),
        "ing_onion" to Quad("Cebolla", "Onion", "Cebola", "Oignon"),
        "ing_olive_oil" to Quad("Aceite de oliva", "Olive oil", "Azeite de oliva", "Huile d'olive"),
        "ing_salt" to Quad("Sal", "Salt", "Sal", "Sel"),
        "ing_pasta" to Quad("Pasta (Penne o Fettuccine)", "Pasta (Penne or Fettuccine)", "Massa (Penne ou Fettuccine)", "Pâtes (Penne ou Fettuccine)"),
        "ing_chicken_breast" to Quad("Pechuga de pollo", "Chicken breast", "Peito de frango", "Blanc de poulet"),
        "ing_mushrooms" to Quad("Champiñones", "Mushrooms", "Cogumelos", "Champignons"),
        "ing_cream" to Quad("Crema de leche / Nata", "Heavy cream", "Creme de leite / Nata", "Crème fraîche / liquide"),
        "ing_garlic" to Quad("Ajo", "Garlic", "Alho", "Ail"),
        "ing_parmesan" to Quad("Queso Parmesano", "Parmesan cheese", "Queijo Parmesão", "Fromage Parmesan"),
        "ing_oats" to Quad("Avena en copos", "Rolled oats", "Aveia em flocos", "Flocons d'avoine"),
        "ing_banana" to Quad("Plátano maduro", "Ripe banana", "Banana madura", "Banane mûre"),
        "ing_milk" to Quad("Leche", "Milk", "Leite", "Lait"),
        "ing_cinnamon" to Quad("Canela en polvo", "Ground cinnamon", "Canela em pó", "Cannelle moulue"),
        "ing_baking_powder" to Quad("Polvo de hornear", "Baking powder", "Fermento em pó", "Levure chimique")
    )

    // Extra common aliases (singulars/synonyms) so cross-language pantry matching works naturally
    private val aliasToCanonical: Map<String, String> = buildMap {
        itemEntries.forEach { (key, q) ->
            put(normalize(q.es), key)
            put(normalize(q.en), key)
            put(normalize(q.pt), key)
            put(normalize(q.fr), key)
        }
        // Singular / regional culinary aliases
        listOf("patata", "papa", "papas", "potato", "batata", "pomme de terre").forEach { put(it, "ing_potatoes") }
        listOf("huevo", "egg", "ovo", "oeuf", "oeufs").forEach { put(it, "ing_eggs") }
        listOf("cebollas", "onions", "cebolas", "oignons").forEach { put(it, "ing_onion") }
        listOf("aceite", "olive oil", "azeite", "huile dolive").forEach { put(it, "ing_olive_oil") }
        listOf("pasta", "penne", "fettuccine", "massa", "pates").forEach { put(it, "ing_pasta") }
        listOf("pollo", "chicken", "frango", "poulet").forEach { put(it, "ing_chicken_breast") }
        listOf("champinon", "champinones", "mushroom", "cogumelo", "champignon").forEach { put(it, "ing_mushrooms") }
        listOf("crema de leche", "nata", "cream", "creme de leite", "creme fraiche").forEach { put(it, "ing_cream") }
        listOf("ajos", "garlic cloves", "alhos", "ails").forEach { put(it, "ing_garlic") }
        listOf("parmesano", "parmesan", "queijo parmesao", "fromage parmesan").forEach { put(it, "ing_parmesan") }
        listOf("avena", "oats", "oatmeal", "aveia", "flocons davoine", "avoine").forEach { put(it, "ing_oats") }
        listOf("platano", "platanos", "banana", "bananas", "banane", "bananes").forEach { put(it, "ing_banana") }
        listOf("canela", "cinnamon", "cannelle").forEach { put(it, "ing_cinnamon") }
        listOf("levadura", "fermento", "levure").forEach { put(it, "ing_baking_powder") }
    }

    private val canonicalToQuad: Map<String, Quad> = itemEntries.toMap()

    // Direct lookup only for exact starter item names (so user-typed "Penne" isn't forcibly overwritten unless it matches one of the 16 seeded phrases)
    private val exactItemLookup: Map<String, Quad> = buildMap {
        itemEntries.forEach { (_, q) ->
            put(normalize(q.es), q)
            put(normalize(q.en), q)
            put(normalize(q.pt), q)
            put(normalize(q.fr), q)
        }
    }

    // --- 14 Starter Instructions ---
    private val instructionQuads: List<Quad> = listOf(
        // Tortilla (5)
        Quad(
            es = "Pelar y cortar las patatas y la cebolla en láminas finas.",
            en = "Peel and slice the potatoes and onion thinly.",
            pt = "Descasque e corte as batatas e a cebola em fatias finas.",
            fr = "Éplucher et couper les pommes de terre et l'oignon en fines lamelles."
        ),
        Quad(
            es = "Pocharlas en la sartén a fuego medio-bajo con abundante aceite de oliva durante 15 minutos hasta que estén tiernas.",
            en = "Slowly fry them in a skillet over medium-low heat with generous olive oil for 15 minutes until tender.",
            pt = "Refogue em uma frigideira em fogo médio-baixo com bastante azeite por 15 minutos até ficarem macias.",
            fr = "Les faire confire à la poêle à feu moyen-doux dans une bonne quantité d'huile d'olive pendant 15 minutes jusqu'à ce qu'elles soient tendres."
        ),
        Quad(
            es = "Batir los huevos en un bol grande y añadir una pizca de sal.",
            en = "Beat the eggs in a large bowl and add a pinch of salt.",
            pt = "Bata os ovos em uma tigela grande e adicione uma pitada de sal.",
            fr = "Battre les œufs dans un grand bol et ajouter une pincée de sel."
        ),
        Quad(
            es = "Escurrir las patatas y cebolla e incorporarlas al bol de huevos batidos. Dejar reposar 5 minutos para que absorban.",
            en = "Drain the potatoes and onion and fold them into the beaten eggs. Let rest 5 minutes to absorb.",
            pt = "Escorra as batatas e a cebola e misture aos ovos batidos. Deixe descansar por 5 minutos para absorver.",
            fr = "Égoutter les pommes de terre et l'oignon, puis les incorporer aux œufs battus. Laisser reposer 5 minutes."
        ),
        Quad(
            es = "En una sartén caliente con unas gotas de aceite, verter la mezcla y cuajar 2-3 minutos por cada lado dando la vuelta con un plato.",
            en = "Pour the mixture into a hot skillet with a few drops of oil and cook 2-3 minutes per side, flipping with a plate.",
            pt = "Em uma frigideira quente com um fio de azeite, despeje a mistura e cozinhe por 2-3 minutos de cada lado virando com um prato.",
            fr = "Dans une poêle chaude légèrement huilée, verser le mélange et cuire 2 à 3 minutes de chaque côté en retournant à l'aide d'une assiette."
        ),
        // Pasta (5)
        Quad(
            es = "Cocer la pasta en abundante agua con sal según las instrucciones del paquete.",
            en = "Boil the pasta in plenty of salted water according to package instructions.",
            pt = "Cozinhe a massa em bastante água com sal conforme as instruções da embalagem.",
            fr = "Cuire les pâtes dans un grand volume d'eau salée selon les instructions du paquet."
        ),
        Quad(
            es = "En una sartén, saltear el pollo cortado en tiras con el ajo picado hasta dorar.",
            en = "In a skillet, sauté the sliced chicken strips with minced garlic until golden brown.",
            pt = "Em uma frigideira, refogue o frango em tiras com o alho picado até dourar.",
            fr = "Dans une poêle, faire revenir les lanières de poulet avec l'ail haché jusqu'à coloration dorée."
        ),
        Quad(
            es = "Añadir los champiñones laminados y cocinar 4 minutos más.",
            en = "Add the sliced mushrooms and cook for 4 more minutes.",
            pt = "Adicione os cogumelos fatiados e cozinhe por mais 4 minutos.",
            fr = "Ajouter les champignons émincés et poursuivre la cuisson 4 minutes."
        ),
        Quad(
            es = "Verter la crema de leche y cocinar a fuego lento hasta espesar ligeramente.",
            en = "Pour in the heavy cream and simmer on low heat until slightly thickened.",
            pt = "Despeje o creme de leite e cozinhe em fogo baixo até engrossar levemente.",
            fr = "Verser la crème fraîche et laisser mijoter à feu doux jusqu'à léger épaississement."
        ),
        Quad(
            es = "Mezclar con la pasta escurrida y coronar con queso parmesano rallado.",
            en = "Toss with the drained pasta and top with grated Parmesan cheese.",
            pt = "Misture com a massa escorrida e finalize com queijo parmesão ralado.",
            fr = "Mélanger avec les pâtes égouttées et parsemer de parmesan râpé."
        ),
        // Pancakes (4)
        Quad(
            es = "Triturar en licuadora los copos de avena, plátanos, huevos, leche, canela y polvo de hornear hasta obtener una mezcla homogénea.",
            en = "Blend the rolled oats, bananas, eggs, milk, cinnamon, and baking powder until smooth.",
            pt = "Bata no liquidificador a aveia, as bananas, os ovos, o leite, a canela e o fermento até obter uma massa homogênea.",
            fr = "Mixer les flocons d'avoine, les bananes, les œufs, le lait, la cannelle et la levure jusqu'à obtenir une pâte lisse."
        ),
        Quad(
            es = "Calentar una sartén antiadherente a fuego medio y engrasar ligeramente.",
            en = "Heat a non-stick skillet over medium heat and lightly grease.",
            pt = "Aqueça uma frigideira antiaderente em fogo médio e unte levemente.",
            fr = "Chauffer une poêle antiadhésive à feu moyen et graisser légèrement."
        ),
        Quad(
            es = "Verter porciones de masa y cocinar 2 minutos hasta que aparezcan burbujas en la superficie.",
            en = "Pour batter portions and cook for 2 minutes until bubbles appear on the surface.",
            pt = "Despeje porções da massa e cozinhe por 2 minutos até surgirem bolhas na superfície.",
            fr = "Verser des louches de pâte et cuire 2 minutes jusqu'à l'apparition de bulles en surface."
        ),
        Quad(
            es = "Dar la vuelta con espátula y cocinar 1 minuto más. Servir con miel o frutas frescas.",
            en = "Flip with a spatula and cook 1 more minute. Serve with honey or fresh fruit.",
            pt = "Vire com uma espátula e cozinhe por mais 1 minuto. Sirva com mel ou frutas frescas.",
            fr = "Retourner avec une spatule et cuire 1 minute de plus. Servir avec du miel ou des fruits frais."
        )
    )

    // --- 3 Starter Notes ---
    private val noteQuads: List<Quad> = listOf(
        Quad(
            es = "Para que quede jugosa, no cocinar en exceso el huevo al dar la vuelta.",
            en = "For a juicy center, avoid overcooking the egg after flipping.",
            pt = "Para ficar suculenta, não cozinhe demais o ovo ao virar.",
            fr = "Pour qu'elle reste moelleuse, ne pas trop cuire l'œuf après l'avoir retournée."
        ),
        Quad(
            es = "Reserva media taza del agua de cocción de la pasta para aligerar la salsa si queda muy densa.",
            en = "Reserve half a cup of pasta cooking water to loosen the sauce if it becomes too thick.",
            pt = "Reserve meia xícara da água do cozimento da massa para suavizar o molho se ficar muito espesso.",
            fr = "Réserver une demi-tasse d'eau de cuisson des pâtes pour détendre la sauce si elle épaissit trop."
        ),
        Quad(
            es = "Usa plátanos con motas marrones para que aporten dulzor natural sin necesidad de añadir azúcar.",
            en = "Use spotted ripe bananas for natural sweetness without adding sugar.",
            pt = "Use bananas bem maduras para adoçar naturalmente sem precisar de açúcar.",
            fr = "Utiliser des bananes bien mûres pour sucrer naturellement sans ajouter de sucre."
        )
    )

    private val titleLookup: Map<String, Quad> = buildMap {
        allTitles.forEach { q ->
            put(normalize(q.es), q)
            put(normalize(q.en), q)
            put(normalize(q.pt), q)
            put(normalize(q.fr), q)
        }
    }

    private val instructionLookup: Map<String, Quad> = buildMap {
        instructionQuads.forEach { q ->
            put(normalize(q.es), q)
            put(normalize(q.en), q)
            put(normalize(q.pt), q)
            put(normalize(q.fr), q)
        }
    }

    private val noteLookup: Map<String, Quad> = buildMap {
        noteQuads.forEach { q ->
            put(normalize(q.es), q)
            put(normalize(q.en), q)
            put(normalize(q.pt), q)
            put(normalize(q.fr), q)
        }
    }

    fun canonicalItemKey(raw: String): String? {
        val norm = normalize(raw)
        if (norm.isEmpty()) return null
        return aliasToCanonical[norm]
    }

    /**
     * Compares two ingredient or pantry names regardless of accents, casing, or language translation
     * (e.g., "Huevos" matches "Eggs", "Ovos", "Œufs", "Huevo").
     */
    fun sameItemName(nameA: String, nameB: String): Boolean {
        val normA = normalize(nameA)
        val normB = normalize(nameB)
        if (normA.isEmpty() || normB.isEmpty()) return false
        if (normA == normB) return true
        val keyA = aliasToCanonical[normA]
        val keyB = aliasToCanonical[normB]
        return keyA != null && keyA == keyB
    }

    /**
     * Checks whether two recipe titles match directly or refer to the same localized starter recipe.
     */
    fun sameRecipeTitle(titleA: String, titleB: String): Boolean {
        val normA = normalize(titleA)
        val normB = normalize(titleB)
        if (normA.isEmpty() || normB.isEmpty()) return false
        if (normA == normB) return true
        val quadA = titleLookup[normA] ?: return false
        val quadB = titleLookup[normB] ?: return false
        return quadA == quadB
    }

    fun localizeItemName(context: Context, raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val norm = normalize(trimmed)
        val quad = exactItemLookup[norm] ?: return raw
        return quad.forLang(resolveLang(context))
    }

    fun localizeRecipeTitle(context: Context, raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val quad = titleLookup[normalize(trimmed)] ?: return raw
        return quad.forLang(resolveLang(context))
    }

    fun localizeInstruction(context: Context, raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val quad = instructionLookup[normalize(trimmed)] ?: return raw
        return quad.forLang(resolveLang(context))
    }

    fun localizeNote(context: Context, raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val quad = noteLookup[normalize(trimmed)] ?: return raw
        return quad.forLang(resolveLang(context))
    }

    fun localizeRecipeSource(context: Context, raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val loc = LocaleHelper.localizedContext(context)
        val match = SERVINGS_SUFFIX_REGEX.matchEntire(trimmed)
        if (match != null) {
            val baseTitle = match.groupValues[1].trim()
            val servings = match.groupValues[2].toIntOrNull() ?: 1
            val localizedTitle = localizeRecipeTitle(loc, baseTitle)
            return loc.getString(R.string.prod_short_servings_fmt, localizedTitle, servings)
        }
        return localizeRecipeTitle(loc, trimmed)
    }

    fun localizeRecipe(context: Context, recipe: Recipe): Recipe {
        return recipe.copy(
            title = localizeRecipeTitle(context, recipe.title),
            ingredients = recipe.ingredients.map { ing ->
                ing.copy(name = localizeItemName(context, ing.name))
            },
            instructions = recipe.instructions.map { step ->
                localizeInstruction(context, step)
            },
            notes = localizeNote(context, recipe.notes)
        )
    }

    fun localizePantryItem(context: Context, item: PantryItem): PantryItem {
        return item.copy(name = localizeItemName(context, item.name))
    }

    fun localizeShoppingItem(context: Context, item: ShoppingItem): ShoppingItem {
        return item.copy(
            name = localizeItemName(context, item.name),
            recipeSource = localizeRecipeSource(context, item.recipeSource)
        )
    }

    fun localizeProductionLog(context: Context, log: ProductionLog): ProductionLog {
        return log.copy(recipeTitle = localizeRecipeTitle(context, log.recipeTitle))
    }

    fun starterRecipes(context: Context): List<Recipe> {
        val lang = resolveLang(context)
        fun item(key: String) = canonicalToQuad[key]?.forLang(lang) ?: key

        return listOf(
            Recipe(
                title = titleTortilla.forLang(lang),
                category = "Almuerzos",
                servings = 4,
                prepTimeMinutes = 15,
                cookTimeMinutes = 20,
                difficulty = "Fácil",
                ingredients = listOf(
                    RecipeIngredient(name = item("ing_potatoes"), amount = 600.0, unit = "g", category = "Verduras", manualUnitCost = 0.002),
                    RecipeIngredient(name = item("ing_eggs"), amount = 6.0, unit = "unidades", category = "Lácteos", manualUnitCost = 0.30),
                    RecipeIngredient(name = item("ing_onion"), amount = 1.0, unit = "unidades", category = "Verduras", manualUnitCost = 0.45),
                    RecipeIngredient(name = item("ing_olive_oil"), amount = 150.0, unit = "ml", category = "Despensa", manualUnitCost = 0.008),
                    RecipeIngredient(name = item("ing_salt"), amount = 5.0, unit = "g", category = "Especias", manualUnitCost = 0.001)
                ),
                instructions = instructionQuads.subList(0, 5).map { it.forLang(lang) },
                notes = noteQuads[0].forLang(lang),
                imageUrl = ImageUtils.REF_TORTILLA
            ),
            Recipe(
                title = titlePasta.forLang(lang),
                category = "Cenas",
                servings = 2,
                prepTimeMinutes = 10,
                cookTimeMinutes = 15,
                difficulty = "Fácil",
                ingredients = listOf(
                    RecipeIngredient(name = item("ing_pasta"), amount = 200.0, unit = "g", category = "Granos", manualUnitCost = 0.003),
                    RecipeIngredient(name = item("ing_chicken_breast"), amount = 250.0, unit = "g", category = "Carnes", manualUnitCost = 0.009),
                    RecipeIngredient(name = item("ing_mushrooms"), amount = 150.0, unit = "g", category = "Verduras", manualUnitCost = 0.008),
                    RecipeIngredient(name = item("ing_cream"), amount = 200.0, unit = "ml", category = "Lácteos", manualUnitCost = 0.007),
                    RecipeIngredient(name = item("ing_garlic"), amount = 2.0, unit = "dientes", category = "Verduras", manualUnitCost = 0.15),
                    RecipeIngredient(name = item("ing_parmesan"), amount = 40.0, unit = "g", category = "Lácteos", manualUnitCost = 0.025)
                ),
                instructions = instructionQuads.subList(5, 10).map { it.forLang(lang) },
                notes = noteQuads[1].forLang(lang),
                imageUrl = ImageUtils.REF_PASTA
            ),
            Recipe(
                title = titlePancakes.forLang(lang),
                category = "Desayunos",
                servings = 2,
                prepTimeMinutes = 8,
                cookTimeMinutes = 7,
                difficulty = "Fácil",
                ingredients = listOf(
                    RecipeIngredient(name = item("ing_oats"), amount = 100.0, unit = "g", category = "Granos", manualUnitCost = 0.002),
                    RecipeIngredient(name = item("ing_banana"), amount = 2.0, unit = "unidades", category = "Frutas", manualUnitCost = 0.35),
                    RecipeIngredient(name = item("ing_eggs"), amount = 2.0, unit = "unidades", category = "Lácteos", manualUnitCost = 0.30),
                    RecipeIngredient(name = item("ing_milk"), amount = 60.0, unit = "ml", category = "Lácteos", manualUnitCost = 0.0015),
                    RecipeIngredient(name = item("ing_cinnamon"), amount = 2.0, unit = "g", category = "Especias", manualUnitCost = 0.02),
                    RecipeIngredient(name = item("ing_baking_powder"), amount = 5.0, unit = "g", category = "Despensa", manualUnitCost = 0.01)
                ),
                instructions = instructionQuads.subList(10, 14).map { it.forLang(lang) },
                notes = noteQuads[2].forLang(lang),
                imageUrl = ImageUtils.REF_PANCAKES
            )
        )
    }

    fun starterPantryItems(context: Context): List<PantryItem> {
        val lang = resolveLang(context)
        fun item(key: String) = canonicalToQuad[key]?.forLang(lang) ?: key

        return listOf(
            PantryItem(name = item("ing_eggs"), category = "Lácteos", quantity = 12.0, unit = "unidades", minQuantity = 4.0, cost = 3.60, costBaseQuantity = 12.0),
            PantryItem(name = item("ing_olive_oil"), category = "Despensa", quantity = 750.0, unit = "ml", minQuantity = 200.0, cost = 6.00, costBaseQuantity = 750.0),
            PantryItem(name = item("ing_salt"), category = "Especias", quantity = 1000.0, unit = "g", minQuantity = 200.0, cost = 1.00, costBaseQuantity = 1000.0),
            PantryItem(name = item("ing_potatoes"), category = "Verduras", quantity = 1000.0, unit = "g", minQuantity = 500.0, cost = 2.00, costBaseQuantity = 1000.0),
            PantryItem(name = item("ing_pasta"), category = "Granos", quantity = 500.0, unit = "g", minQuantity = 200.0, cost = 1.50, costBaseQuantity = 500.0)
        )
    }

    private data class StarterRecipeSignature(
        val titleQuad: Quad,
        val category: String,
        val servings: Int,
        val imageUrl: String,
        val ingredientKeys: List<String>
    )

    private val starterSignatures = listOf(
        StarterRecipeSignature(
            titleQuad = titleTortilla,
            category = "Almuerzos",
            servings = 4,
            imageUrl = ImageUtils.REF_TORTILLA,
            ingredientKeys = listOf("ing_potatoes", "ing_eggs", "ing_onion", "ing_olive_oil", "ing_salt")
        ),
        StarterRecipeSignature(
            titleQuad = titlePasta,
            category = "Cenas",
            servings = 2,
            imageUrl = ImageUtils.REF_PASTA,
            ingredientKeys = listOf("ing_pasta", "ing_chicken_breast", "ing_mushrooms", "ing_cream", "ing_garlic", "ing_parmesan")
        ),
        StarterRecipeSignature(
            titleQuad = titlePancakes,
            category = "Desayunos",
            servings = 2,
            imageUrl = ImageUtils.REF_PANCAKES,
            ingredientKeys = listOf("ing_oats", "ing_banana", "ing_eggs", "ing_milk", "ing_cinnamon", "ing_baking_powder")
        )
    )

    private data class StarterPantrySignature(
        val itemKey: String,
        val category: String,
        val quantity: Double,
        val unit: String,
        val minQuantity: Double,
        val cost: Double,
        val costBaseQuantity: Double
    )

    private val starterPantrySignatures = listOf(
        StarterPantrySignature("ing_eggs", "Lácteos", 12.0, "unidades", 4.0, 3.60, 12.0),
        StarterPantrySignature("ing_olive_oil", "Despensa", 750.0, "ml", 200.0, 6.00, 750.0),
        StarterPantrySignature("ing_salt", "Especias", 1000.0, "g", 200.0, 1.00, 1000.0),
        StarterPantrySignature("ing_potatoes", "Verduras", 1000.0, "g", 500.0, 2.00, 1000.0),
        StarterPantrySignature("ing_pasta", "Granos", 500.0, "g", 200.0, 1.50, 500.0)
    )

    /**
     * Identifies untouched auto-seeded starter recipes inserted by older versions of the app
     * so they can be cleaned up once without touching any user-created or edited recipes.
     */
    fun isLegacyStarterRecipe(recipe: Recipe): Boolean {
        val matchedQuad = titleLookup[normalize(recipe.title)] ?: return false
        val sig = starterSignatures.firstOrNull { it.titleQuad == matchedQuad } ?: return false
        val imageMatches = recipe.imageUrl.isBlank() || recipe.imageUrl == sig.imageUrl
        if (!imageMatches) return false
        if (recipe.servings != sig.servings) return false
        val canonicalCategory = RecipeCategories.canonicalBuiltInOrNull(recipe.category) ?: recipe.category.trim()
        if (!canonicalCategory.equals(sig.category, ignoreCase = true)) return false
        if (recipe.ingredients.size != sig.ingredientKeys.size) return false
        val actualKeys = recipe.ingredients.map { canonicalItemKey(it.name) }
        return actualKeys == sig.ingredientKeys
    }

    /**
     * Identifies untouched auto-seeded starter pantry items inserted by older versions of the app.
     */
    fun isLegacyStarterPantryItem(item: PantryItem): Boolean {
        val key = canonicalItemKey(item.name) ?: return false
        val sig = starterPantrySignatures.firstOrNull { it.itemKey == key } ?: return false
        return item.category.trim().equals(sig.category, ignoreCase = true) &&
            item.unit.trim().equals(sig.unit, ignoreCase = true) &&
            kotlin.math.abs(item.quantity - sig.quantity) < 0.001 &&
            kotlin.math.abs(item.minQuantity - sig.minQuantity) < 0.001 &&
            kotlin.math.abs(item.cost - sig.cost) < 0.001 &&
            kotlin.math.abs(item.costBaseQuantity - sig.costBaseQuantity) < 0.001
    }
}
