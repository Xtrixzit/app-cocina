package com.example.util

import com.example.data.model.Recipe
import java.text.Normalizer
import java.util.Locale

object RecipeCategories {

    val DEFAULT_CATEGORIES: List<String> = listOf(
        "Desayunos",
        "Almuerzos",
        "Comidas",
        "Cenas",
        "Pan",
        "Postres",
        "Snacks",
        "Bebidas",
        "Sopas"
    )

    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

    private val BUILT_IN_EQUIVALENTS: Map<String, String> = mapOf(
        "todas" to "Todas",
        "all" to "Todas",
        "toutes" to "Todas",
        "desayunos" to "Desayunos",
        "desayuno" to "Desayunos",
        "breakfast" to "Desayunos",
        "cafe da manha" to "Desayunos",
        "petit-dejeuner" to "Desayunos",
        "petit dejeuner" to "Desayunos",
        "almuerzos" to "Almuerzos",
        "almuerzo" to "Almuerzos",
        "lunch" to "Almuerzos",
        "almoco" to "Almuerzos",
        "dejeuner" to "Almuerzos",
        "comidas" to "Comidas",
        "comida" to "Comidas",
        "meals" to "Comidas",
        "meal" to "Comidas",
        "refeicoes" to "Comidas",
        "refeicao" to "Comidas",
        "repas" to "Comidas",
        "cenas" to "Cenas",
        "cena" to "Cenas",
        "dinner" to "Cenas",
        "jantar" to "Cenas",
        "diner" to "Cenas",
        "pan" to "Pan",
        "panaderia" to "Pan",
        "bread" to "Pan",
        "pao" to "Pan",
        "pain" to "Pan",
        "postres" to "Postres",
        "postre" to "Postres",
        "desserts" to "Postres",
        "dessert" to "Postres",
        "sobremesas" to "Postres",
        "sobremesa" to "Postres",
        "snacks" to "Snacks",
        "snack" to "Snacks",
        "lanches" to "Snacks",
        "lanche" to "Snacks",
        "collations" to "Snacks",
        "collation" to "Snacks",
        "bebidas" to "Bebidas",
        "bebida" to "Bebidas",
        "drinks" to "Bebidas",
        "beverages" to "Bebidas",
        "boissons" to "Bebidas",
        "sopas" to "Sopas",
        "sopa" to "Sopas",
        "soups" to "Sopas",
        "soupes" to "Sopas"
    )

    fun normalizeKey(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun canonicalBuiltInOrNull(raw: String): String? {
        val norm = normalizeKey(raw)
        if (norm.isEmpty()) return null
        return BUILT_IN_EQUIVALENTS[norm]
    }

    fun isBuiltInCategory(raw: String): Boolean {
        return canonicalBuiltInOrNull(raw) != null
    }

    fun resolveStoredValue(raw: String): String {
        val clean = raw.trim().take(50)
        if (clean.isEmpty()) return ""
        return canonicalBuiltInOrNull(clean) ?: clean
    }

    fun mergeSelectableCategories(
        customCategories: List<String>,
        existingRecipeCategories: List<String> = emptyList(),
        selectedCategory: String = ""
    ): List<String> {
        val seen = LinkedHashMap<String, String>()
        fun addIfUnique(candidate: String) {
            val clean = candidate.trim().take(50)
            if (clean.isBlank()) return
            val canonical = canonicalBuiltInOrNull(clean) ?: clean
            if (canonical.equals("Todas", ignoreCase = true)) return
            val key = normalizeKey(canonical)
            if (!seen.containsKey(key)) {
                seen[key] = canonical
            }
        }

        customCategories.forEach(::addIfUnique)
        existingRecipeCategories.forEach(::addIfUnique)
        if (selectedCategory.isNotBlank()) {
            addIfUnique(selectedCategory)
        }
        DEFAULT_CATEGORIES.forEach(::addIfUnique)
        return seen.values.toList()
    }

    fun mergeFilterCategories(
        customCategories: List<String>,
        recipes: List<Recipe>
    ): List<String> {
        return listOf("Todas") + mergeSelectableCategories(
            customCategories = customCategories,
            existingRecipeCategories = recipes.map { it.category }
        )
    }
}
