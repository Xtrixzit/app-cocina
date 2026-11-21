package com.example.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.R
import com.example.settings.AppLanguage
import com.example.settings.AppSettingsPreferences
import java.util.Locale

object LocaleHelper {

    fun wrapContext(base: Context): Context {
        val code = AppSettingsPreferences.getLanguageCode(base)
        if (code.isBlank()) return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun localizedContext(context: Context): Context {
        return wrapContext(context.applicationContext ?: context)
    }

    fun applyLanguage(activity: Activity?, code: String) {
        val context = activity ?: return
        AppSettingsPreferences.setLanguageCode(context, code)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = if (code.isBlank()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(code)
                }
            } catch (_: Exception) {
            }
        }
        activity.recreate()
    }

    fun languageDisplayName(context: Context, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.SYSTEM -> context.getString(R.string.lang_system)
            AppLanguage.SPANISH -> lang.nativeName
            AppLanguage.ENGLISH -> lang.nativeName
            AppLanguage.PORTUGUESE -> lang.nativeName
            AppLanguage.FRENCH -> lang.nativeName
        }
    }
}

/**
 * Translates canonical stored DB keys (categories, difficulty, units) to the active display locale,
 * so Firestore/Room records keep stable filter keys regardless of the active UI language.
 */
object LocalizedLabels {

    fun recipeCategory(context: Context, canonical: String): String {
        return when (canonical.trim().lowercase()) {
            "todas", "all", "toutes" -> context.getString(R.string.cat_all)
            "desayunos", "breakfast", "café da manhã", "petit-déjeuner" -> context.getString(R.string.cat_breakfast)
            "almuerzos", "lunch", "almoço", "déjeuner" -> context.getString(R.string.cat_lunch)
            "cenas", "dinner", "jantar", "dîner" -> context.getString(R.string.cat_dinner)
            "postres", "desserts", "sobremesas" -> context.getString(R.string.cat_desserts)
            "snacks", "lanches", "collations" -> context.getString(R.string.cat_snacks)
            "bebidas", "drinks", "beverages", "boissons" -> context.getString(R.string.cat_drinks)
            "sopas", "soups", "soupes" -> context.getString(R.string.cat_soups)
            else -> canonical
        }
    }

    fun pantryCategory(context: Context, canonical: String): String {
        return when (canonical.trim().lowercase()) {
            "todas", "all", "toutes" -> context.getString(R.string.cat_all)
            "despensa", "pantry", "garde-manger" -> context.getString(R.string.cat_pantry)
            "verduras", "vegetables", "legumes", "légumes" -> context.getString(R.string.cat_vegetables)
            "frutas", "fruits" -> context.getString(R.string.cat_fruits)
            "carnes", "meats", "viandes" -> context.getString(R.string.cat_meats)
            "lácteos", "lacteos", "dairy", "laticínios", "produits laitiers" -> context.getString(R.string.cat_dairy)
            "granos", "grains", "grãos", "céréales" -> context.getString(R.string.cat_grains)
            "especias", "spices", "temperos", "épices" -> context.getString(R.string.cat_spices)
            "bebidas", "drinks", "beverages", "boissons" -> context.getString(R.string.cat_drinks)
            "congelados", "frozen", "surgelés" -> context.getString(R.string.cat_frozen)
            else -> canonical
        }
    }

    fun difficulty(context: Context, canonical: String): String {
        return when (canonical.trim().lowercase()) {
            "fácil", "facil", "easy", "facile" -> context.getString(R.string.diff_easy)
            "media", "medium", "média", "moyenne" -> context.getString(R.string.diff_medium)
            "difícil", "dificil", "hard", "difficile" -> context.getString(R.string.diff_hard)
            else -> canonical
        }
    }

    fun unit(context: Context, canonical: String): String {
        return when (canonical.trim().lowercase()) {
            "unidades", "unidad", "units", "unit", "unités", "u", "ud", "uds" -> context.getString(R.string.unit_units)
            "taza", "tazas", "cup", "cups", "xícara", "xícaras", "tasse", "tasses" -> context.getString(R.string.unit_cup)
            "cda", "cdas", "tbsp", "c.à.s" -> context.getString(R.string.unit_tbsp)
            "cdta", "cdtas", "tsp", "c.à.c" -> context.getString(R.string.unit_tsp)
            "pizca", "pizcas", "pinch", "pitada", "pincée" -> context.getString(R.string.unit_pinch)
            "dientes", "diente", "cloves", "clove", "dentes", "gousses" -> context.getString(R.string.unit_cloves)
            "latas", "lata", "cans", "can", "boîtes" -> context.getString(R.string.unit_cans)
            "paquetes", "paquete", "packs", "pack", "pacotes", "paquets" -> context.getString(R.string.unit_packs)
            "piezas", "pieza", "pieces", "piece", "peças", "pièces" -> context.getString(R.string.unit_pieces)
            "docenas", "docena", "dozens", "dozen", "dúzias", "douzaines" -> context.getString(R.string.unit_dozens)
            else -> canonical
        }
    }
}
