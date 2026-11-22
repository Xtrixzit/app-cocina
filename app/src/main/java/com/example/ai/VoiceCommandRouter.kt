package com.example.ai

import android.content.Context
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import com.example.R
import com.example.util.LocaleHelper
import java.text.Normalizer

/**
 * Deterministic multilingual router for voice commands and Android search/deep-link intents.
 * Reuses [RecetarioAppFunctions] directly with zero in-app AI or cloud LLM dependency,
 * bridging Android 7-16 assistants (SEARCH_ACTION, GET_THING, Bixby deep links,
 * and OS speech recognition) with local Room / Cloud Firestore data.
 */
object VoiceCommandRouter {

    const val EXTRA_VOICE_LISTEN = "extra_voice_listen"
    const val DEEP_LINK_SCHEME = "recetario"
    const val DEEP_LINK_HOST_VOICE = "voice"

    sealed class ParsedVoiceIntent {
        data object LowStock : ParsedVoiceIntent()
        data class ShoppingList(val onlyPending: Boolean = true) : ParsedVoiceIntent()
        data class AvailableRecipes(val category: String? = null) : ParsedVoiceIntent()
        data class RecipeFeasibility(val recipeQuery: String, val servings: Int = 0) : ParsedVoiceIntent()
        data class PantryLookup(val ingredientQuery: String? = null) : ParsedVoiceIntent()
    }

    data class VoiceExecutionResult(
        val query: String,
        val intentType: ParsedVoiceIntent,
        val summaryText: String,
        val isSuccess: Boolean
    )

    internal fun normalizeForRouting(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^\\p{L}\\p{N}\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun parseIntent(rawQuery: String): ParsedVoiceIntent {
        val norm = normalizeForRouting(rawQuery)
        if (norm.isBlank()) {
            return ParsedVoiceIntent.PantryLookup(null)
        }

        // 1. Low stock / restock alerts
        val lowStockMarkers = listOf(
            "stock bajo", "bajo stock", "estan bajos", "están bajos", "bajos o agotados",
            "agotad", "reponer", "reposicion", "falta en despensa", "faltan en despensa",
            "por acabarse", "se acabo", "escasea",
            "low stock", "running low", "out of stock", "low or out", "are low", "restock",
            "estoque baixo", "em falta", "estao baixos", "reposicao", "acabando",
            "stock bas", "sont bas", "bas ou epuise", "epuise", "rupture", "reapprovisionn"
        )
        if (lowStockMarkers.any { norm.contains(it) }) {
            return ParsedVoiceIntent.LowStock
        }

        // 2. Shopping list queries
        val shoppingMarkers = listOf(
            "lista de compras", "lista compras", "por comprar", "check list", "checklist",
            "shopping list", "to buy", "lista de super",
            "lista de compra", "itens para comprar",
            "liste de courses", "a acheter", "mes courses"
        )
        val hasShoppingWord = shoppingMarkers.any { norm.contains(it) } ||
            Regex("\\b(compras|shopping|courses|comprar)\\b").containsMatchIn(norm)
        if (hasShoppingWord) {
            val pendingMarkers = listOf("pendiente", "pending", "falta", "faltan", "pendente", "restant")
            val allMarkers = listOf("todo", "todas", "todos", "completa", "all items", "tudo", "tout", "toutes")
            val onlyPending = pendingMarkers.any { norm.contains(it) } || allMarkers.none { norm.contains(it) }
            return ParsedVoiceIntent.ShoppingList(onlyPending = onlyPending)
        }

        // 3. Available / cookable recipes today
        val availableRecipesMarkers = listOf(
            "que puedo cocinar", "puedo cocinar", "recetas disponibles", "que cocino", "cocinar hoy",
            "what can i cook", "can i cook", "cook today", "available recipes", "what recipes",
            "o que posso cozinhar", "receitas disponiveis", "posso cozinhar",
            "que puis je cuisiner", "puis je cuisiner", "recettes disponibles", "recettes possibles"
        )
        if (availableRecipesMarkers.any { norm.contains(it) }) {
            val category = extractCategoryFilter(norm)
            return ParsedVoiceIntent.AvailableRecipes(category = category)
        }

        // 4. Recipe feasibility & cost with optional servings
        val feasibilityMarkers = listOf(
            "me alcanza", "alcanza para", "puedo hacer", "puedo preparar", "viabilidad",
            "cuanto cuesta hacer", "verificar receta", "verificar produccion",
            "can i make", "enough to make", "enough for", "feasibility", "can i prepare",
            "consigo fazer", "da para fazer", "posso fazer",
            "ai je assez", "puis je faire", "assez pour", "faisabilite"
        )
        val servingsRegex = Regex("\\b(\\d+)\\s*(?:porcion(?:es)?|servings?|porcoes|porcao|portions?|raciones?)\\b")
        val servingsMatch = servingsRegex.find(norm)
        if (feasibilityMarkers.any { norm.contains(it) } || servingsMatch != null) {
            val servings = servingsMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val recipeTitle = extractRecipeTitle(norm, servingsMatch?.value)
            return if (recipeTitle.isNotBlank()) {
                ParsedVoiceIntent.RecipeFeasibility(recipeQuery = recipeTitle, servings = servings)
            } else {
                ParsedVoiceIntent.AvailableRecipes(category = null)
            }
        }

        // 5. General pantry summary or specific ingredient lookup
        val cleanedIngredient = extractIngredientQuery(norm)
        return ParsedVoiceIntent.PantryLookup(ingredientQuery = cleanedIngredient)
    }

    private fun extractCategoryFilter(norm: String): String? {
        val known = mapOf(
            "desayuno" to "Desayunos", "breakfast" to "Desayunos", "cafe da manha" to "Desayunos", "petit dejeuner" to "Desayunos",
            "postre" to "Postres", "dessert" to "Postres", "sobremesa" to "Postres",
            "panaderia" to "Panadería", "bakery" to "Panadería", "padaria" to "Panadería", "boulangerie" to "Panadería",
            "pasteleria" to "Pastelería", "pastry" to "Pastelería", "confeitaria" to "Pastelería", "patisserie" to "Pastelería",
            "bebida" to "Bebidas", "drink" to "Bebidas", "boisson" to "Bebidas",
            "almuerzo" to "Almuerzo", "lunch" to "Almuerzo", "almoco" to "Almuerzo", "dejeuner" to "Almuerzo",
            "cena" to "Cena", "dinner" to "Cena", "jantar" to "Cena", "diner" to "Cena"
        )
        return known.entries.firstOrNull { (k, _) -> norm.contains(k) }?.value
    }

    private fun extractRecipeTitle(norm: String, servingsChunk: String?): String {
        var text = norm
        if (!servingsChunk.isNullOrBlank()) {
            text = text.replace(servingsChunk, " ")
        }
        val stripPhrases = listOf(
            "en la aplicacion recetario", "en la app recetario", "en recetario", "in recetario",
            "no recetario", "dans recetario", "recetario",
            "me alcanza para hacer", "me alcanza para preparar", "me alcanza para", "alcanza para",
            "puedo hacer", "puedo preparar", "cuanto cuesta hacer", "verificar produccion de",
            "verificar produccion", "verificar receta", "viabilidad de", "la receta de", "la receta",
            "do i have enough to make", "can i make", "can i prepare", "enough to make", "enough for",
            "consigo fazer", "da para fazer", "posso fazer",
            "ai je assez pour faire", "ai je assez pour", "puis je faire", "assez pour",
            "para hacer", "para preparar", "to make", "to cook"
        )
        for (phrase in stripPhrases) {
            text = text.replace(Regex("\\b${Regex.escape(phrase)}\\b"), " ")
        }
        val leadingFillers = setOf(
            "de", "del", "la", "el", "los", "las", "un", "una", "unos", "unas", "para", "con", "en",
            "of", "for", "the", "a", "an", "in",
            "da", "do", "das", "dos", "um", "uma", "no", "na",
            "du", "des", "le", "les", "pour", "dans"
        )
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val trimmedTokens = tokens
            .dropWhile { it in leadingFillers }
            .dropLastWhile { it in leadingFillers }
        return trimmedTokens.joinToString(" ").trim()
    }

    private fun extractIngredientQuery(norm: String): String? {
        var text = norm
        val stripPhrases = listOf(
            "en la aplicacion recetario", "en la app recetario", "en recetario", "in recetario",
            "no recetario", "dans recetario", "en mi despensa", "in my pantry", "na minha despensa",
            "dans mon garde manger", "en despensa", "de la despensa", "mi despensa", "la despensa",
            "mon garde manger", "garde manger",
            "cuanto tengo de", "cuantos tengo de", "cuanta tengo de", "cuantas tengo de",
            "cuanto hay de", "cuantos hay de", "cuanta hay de", "cuantas hay de",
            "cuanto queda de", "cuantos quedan de", "que tengo de", "tengo algo de",
            "consultar stock de", "consultar despensa", "revisar stock de", "buscar stock de",
            "buscar ingredientes", "buscar ingrediente", "buscar en despensa", "buscar",
            "how much do i have of", "how many do i have", "how much", "how many", "do i have any",
            "do i have", "check stock of", "check pantry for", "search for", "search",
            "quantos tenho de", "quanto tenho de", "tem", "tenho",
            "combien ai je de", "combien d", "ai je des", "ai je du", "ai je de", "est ce que j ai",
            "recetario"
        )
        for (phrase in stripPhrases) {
            text = text.replace(Regex("\\b${Regex.escape(phrase)}\\b"), " ")
        }
        val stopWords = setOf(
            "de", "del", "la", "el", "los", "las", "un", "una", "unos", "unas", "en", "mi", "mis",
            "tengo", "hay", "queda", "quedan", "tienes", "stock", "despensa", "inventario", "todo",
            "toda", "todos", "todas", "productos", "insumos", "ingredientes", "por", "favor",
            "of", "the", "a", "an", "in", "my", "is", "are", "there", "pantry", "inventory", "all",
            "items", "left", "have",
            "do", "da", "dos", "das", "no", "na", "meu", "minha", "estoque", "itens",
            "du", "des", "le", "les", "mon", "ma", "mes", "dans", "tout", "tous", "articles"
        )
        val tokens = text.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in stopWords }
        val result = tokens.joinToString(" ").trim()
        return result.ifBlank { null }
    }

    suspend fun executeVoiceQuery(
        context: Context,
        rawQuery: String,
        appFunctions: RecetarioAppFunctions = RecetarioAppFunctions()
    ): VoiceExecutionResult {
        val appContext = context.applicationContext
        val loc = LocaleHelper.localizedContext(appContext)
        val fnContext = object : AppFunctionContext {
            override val context: Context = appContext
        }
        val parsed = parseIntent(rawQuery)
        return try {
            val summary = when (parsed) {
                is ParsedVoiceIntent.LowStock ->
                    appFunctions.listarStockBajoDespensa(fnContext).summaryText
                is ParsedVoiceIntent.ShoppingList ->
                    appFunctions.consultarListaCompras(fnContext, soloPendientes = parsed.onlyPending).summaryText
                is ParsedVoiceIntent.AvailableRecipes ->
                    appFunctions.listarRecetasDisponibles(fnContext, categoria = parsed.category).summaryText
                is ParsedVoiceIntent.RecipeFeasibility ->
                    appFunctions.verificarProduccionReceta(
                        fnContext,
                        nombreReceta = parsed.recipeQuery,
                        porcionesDeseadas = parsed.servings
                    ).summaryText
                is ParsedVoiceIntent.PantryLookup ->
                    appFunctions.consultarStockDespensa(fnContext, nombreIngrediente = parsed.ingredientQuery).summaryText
            }
            VoiceExecutionResult(
                query = rawQuery,
                intentType = parsed,
                summaryText = summary,
                isSuccess = true
            )
        } catch (e: AppFunctionElementNotFoundException) {
            VoiceExecutionResult(
                query = rawQuery,
                intentType = parsed,
                summaryText = e.errorMessage ?: loc.getString(R.string.voice_error_generic),
                isSuccess = false
            )
        } catch (e: AppFunctionInvalidArgumentException) {
            VoiceExecutionResult(
                query = rawQuery,
                intentType = parsed,
                summaryText = e.errorMessage ?: loc.getString(R.string.voice_error_generic),
                isSuccess = false
            )
        } catch (e: Exception) {
            VoiceExecutionResult(
                query = rawQuery,
                intentType = parsed,
                summaryText = e.localizedMessage ?: loc.getString(R.string.voice_error_generic),
                isSuccess = false
            )
        }
    }
}
