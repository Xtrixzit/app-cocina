package com.example.data.sync

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.ProductionLog
import com.example.data.model.Recipe
import com.example.data.model.ShoppingItem
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.util.LocaleHelper
import com.example.util.LocalizedLabels
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats Recetario inventory, shopping list, recipes, and production logs into
 * clean, AI-readable formats (Markdown, Google Docs-compatible HTML, and CSV for Google Sheets)
 * without JSON boilerplate or bloated base64 image payloads.
 */
object AiReadableExportFormatter {

    private fun fmtNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun escapeHtml(raw: String): String {
        return buildString(raw.length) {
            for (ch in raw) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(ch)
                }
            }
        }
    }

    private fun escapeCsv(raw: String): String {
        val cleaned = raw.replace("\r\n", " ").replace("\n", " ")
        return if (cleaned.contains(',') || cleaned.contains('"') || cleaned.contains(';')) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }

    private fun pantryStatusLabel(loc: Context, item: PantryItem): String = when {
        item.quantity <= 0.0 -> loc.getString(R.string.ai_doc_status_out)
        item.minQuantity > 0.0 && item.quantity <= item.minQuantity -> loc.getString(R.string.ai_doc_status_low)
        else -> loc.getString(R.string.ai_doc_status_ok)
    }

    fun exportToMarkdown(
        context: Context,
        recipes: List<Recipe>,
        pantry: List<PantryItem>,
        shopping: List<ShoppingItem>,
        productionLogs: List<ProductionLog>,
        currencyCode: String,
        marginPercent: Int,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val loc = LocaleHelper.localizedContext(context)
        val currency = CurrencyOption.fromCode(currencyCode)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestampMillis))
        val sb = StringBuilder()

        sb.appendLine("# ${loc.getString(R.string.ai_doc_title)}")
        sb.appendLine()
        sb.appendLine("_${loc.getString(R.string.ai_doc_generated_at, dateStr, currency.code, marginPercent)}_")
        sb.appendLine()

        // 1. Pantry Inventory
        sb.appendLine("## ${loc.getString(R.string.ai_doc_pantry_section, pantry.size)}")
        sb.appendLine()
        if (pantry.isEmpty()) {
            sb.appendLine("- ${loc.getString(R.string.ai_doc_pantry_empty)}")
        } else {
            val grouped = pantry.sortedBy { it.name.lowercase() }.groupBy { it.category }
            for ((cat, items) in grouped) {
                val catDisp = LocalizedLabels.pantryCategory(loc, cat)
                sb.appendLine("### $catDisp")
                for (item in items) {
                    val unitDisp = LocalizedLabels.unit(loc, item.unit)
                    val status = pantryStatusLabel(loc, item)
                    val minNote = if (item.minQuantity > 0.0) {
                        " | min: ${fmtNumber(item.minQuantity)} $unitDisp"
                    } else {
                        ""
                    }
                    val costNote = if (item.cost > 0.0) {
                        " | ${AppSettingsPreferences.formatMoney(item.cost, currency)}"
                    } else {
                        ""
                    }
                    val expNote = if (item.expirationDate.isNotBlank()) " | exp: ${item.expirationDate}" else ""
                    sb.appendLine("- **${item.name}**: ${fmtNumber(item.quantity)} $unitDisp [$status]$minNote$costNote$expNote")
                }
                sb.appendLine()
            }
        }

        // 2. Shopping List
        sb.appendLine("## ${loc.getString(R.string.ai_doc_shopping_section, shopping.size)}")
        sb.appendLine()
        if (shopping.isEmpty()) {
            sb.appendLine("- ${loc.getString(R.string.ai_doc_shopping_empty)}")
        } else {
            for (item in shopping.sortedWith(compareBy<ShoppingItem> { it.isPurchased }.thenBy { it.name.lowercase() })) {
                val check = if (item.isPurchased) "[x]" else "[ ]"
                val stateLabel = if (item.isPurchased) {
                    loc.getString(R.string.ai_doc_shopping_done)
                } else {
                    loc.getString(R.string.ai_doc_shopping_pending)
                }
                val unitDisp = LocalizedLabels.unit(loc, item.unit)
                val source = if (item.recipeSource.isNotBlank()) " (${item.recipeSource})" else ""
                sb.appendLine("- $check **${item.name}**: ${fmtNumber(item.quantity)} $unitDisp — $stateLabel$source")
            }
        }
        sb.appendLine()

        // 3. Saved Recipes
        sb.appendLine("## ${loc.getString(R.string.ai_doc_recipes_section, recipes.size)}")
        sb.appendLine()
        if (recipes.isEmpty()) {
            sb.appendLine("- ${loc.getString(R.string.ai_doc_recipes_empty)}")
        } else {
            for (recipe in recipes.sortedBy { it.title.lowercase() }) {
                val catDisp = LocalizedLabels.recipeCategory(loc, recipe.category)
                val diffDisp = LocalizedLabels.difficulty(loc, recipe.difficulty)
                sb.appendLine("### ${recipe.title} ($catDisp)")
                sb.appendLine(
                    loc.getString(
                        R.string.ai_doc_servings_fmt,
                        recipe.servings,
                        recipe.prepTimeMinutes,
                        recipe.cookTimeMinutes,
                        diffDisp
                    )
                )
                sb.appendLine()
                if (recipe.ingredients.isNotEmpty()) {
                    sb.appendLine("**${loc.getString(R.string.ai_doc_ingredients_label)}:**")
                    for (ing in recipe.ingredients) {
                        val unitDisp = LocalizedLabels.unit(loc, ing.unit)
                        sb.appendLine("- ${ing.name}: ${fmtNumber(ing.amount)} $unitDisp")
                    }
                    sb.appendLine()
                }
                if (recipe.instructions.isNotEmpty()) {
                    sb.appendLine("**${loc.getString(R.string.ai_doc_steps_label)}:**")
                    recipe.instructions.forEachIndexed { idx, step ->
                        sb.appendLine("${idx + 1}. ${step.trim()}")
                    }
                    sb.appendLine()
                }
                if (recipe.notes.isNotBlank()) {
                    sb.appendLine("**${loc.getString(R.string.ai_doc_notes_label)}:** ${recipe.notes.trim()}")
                    sb.appendLine()
                }
            }
        }

        // 4. Recent Production & Cost Logs
        if (productionLogs.isNotEmpty()) {
            sb.appendLine("## ${loc.getString(R.string.ai_doc_production_section)}")
            sb.appendLine()
            for (log in productionLogs.take(25)) {
                val logCurrency = CurrencyOption.fromCode(log.currencyCode)
                sb.appendLine(
                    "- **${log.recipeTitle}**: ${log.targetServings} serv. (x${fmtNumber(log.scaleFactor)}) | " +
                        "${AppSettingsPreferences.formatMoney(log.totalCost, logCurrency)} -> " +
                        "${AppSettingsPreferences.formatMoney(log.suggestedPrice, logCurrency)} (+${log.marginPercent}%)"
                )
            }
        }

        return sb.toString().trim() + "\n"
    }

    fun exportToGoogleDocHtml(
        context: Context,
        recipes: List<Recipe>,
        pantry: List<PantryItem>,
        shopping: List<ShoppingItem>,
        productionLogs: List<ProductionLog>,
        currencyCode: String,
        marginPercent: Int,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val loc = LocaleHelper.localizedContext(context)
        val currency = CurrencyOption.fromCode(currencyCode)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestampMillis))

        return buildString {
            appendLine("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>")
            appendLine("<style>body{font-family:Arial,sans-serif;line-height:1.45;}table{border-collapse:collapse;width:100%;margin-bottom:16px;}th,td{border:1px solid #d0d0d0;padding:6px 8px;text-align:left;}th{background-color:#f4efe6;}</style>")
            appendLine("</head><body>")
            appendLine("<h1>${escapeHtml(loc.getString(R.string.ai_doc_title))}</h1>")
            appendLine("<p><em>${escapeHtml(loc.getString(R.string.ai_doc_generated_at, dateStr, currency.code, marginPercent))}</em></p>")

            // Pantry table
            appendLine("<h2>${escapeHtml(loc.getString(R.string.ai_doc_pantry_section, pantry.size))}</h2>")
            if (pantry.isEmpty()) {
                appendLine("<p>${escapeHtml(loc.getString(R.string.ai_doc_pantry_empty))}</p>")
            } else {
                appendLine("<table><tr><th>Categoría</th><th>Producto</th><th>Cantidad</th><th>Estado</th><th>Notas / Costo</th></tr>")
                for (item in pantry.sortedBy { it.name.lowercase() }) {
                    val catDisp = LocalizedLabels.pantryCategory(loc, item.category)
                    val unitDisp = LocalizedLabels.unit(loc, item.unit)
                    val status = pantryStatusLabel(loc, item)
                    val details = buildList {
                        if (item.minQuantity > 0.0) add("min ${fmtNumber(item.minQuantity)} $unitDisp")
                        if (item.cost > 0.0) add(AppSettingsPreferences.formatMoney(item.cost, currency))
                        if (item.expirationDate.isNotBlank()) add("exp ${item.expirationDate}")
                    }.joinToString(" | ")
                    appendLine(
                        "<tr><td>${escapeHtml(catDisp)}</td><td><b>${escapeHtml(item.name)}</b></td>" +
                            "<td>${escapeHtml(fmtNumber(item.quantity))} ${escapeHtml(unitDisp)}</td>" +
                            "<td>${escapeHtml(status)}</td><td>${escapeHtml(details)}</td></tr>"
                    )
                }
                appendLine("</table>")
            }

            // Shopping list
            appendLine("<h2>${escapeHtml(loc.getString(R.string.ai_doc_shopping_section, shopping.size))}</h2>")
            if (shopping.isEmpty()) {
                appendLine("<p>${escapeHtml(loc.getString(R.string.ai_doc_shopping_empty))}</p>")
            } else {
                appendLine("<ul>")
                for (item in shopping.sortedWith(compareBy<ShoppingItem> { it.isPurchased }.thenBy { it.name.lowercase() })) {
                    val stateLabel = if (item.isPurchased) {
                        loc.getString(R.string.ai_doc_shopping_done)
                    } else {
                        loc.getString(R.string.ai_doc_shopping_pending)
                    }
                    val unitDisp = LocalizedLabels.unit(loc, item.unit)
                    val source = if (item.recipeSource.isNotBlank()) " (${item.recipeSource})" else ""
                    appendLine("<li><b>${escapeHtml(item.name)}</b>: ${escapeHtml(fmtNumber(item.quantity))} ${escapeHtml(unitDisp)} — ${escapeHtml(stateLabel)}${escapeHtml(source)}</li>")
                }
                appendLine("</ul>")
            }

            // Recipes
            appendLine("<h2>${escapeHtml(loc.getString(R.string.ai_doc_recipes_section, recipes.size))}</h2>")
            if (recipes.isEmpty()) {
                appendLine("<p>${escapeHtml(loc.getString(R.string.ai_doc_recipes_empty))}</p>")
            } else {
                for (recipe in recipes.sortedBy { it.title.lowercase() }) {
                    val catDisp = LocalizedLabels.recipeCategory(loc, recipe.category)
                    val diffDisp = LocalizedLabels.difficulty(loc, recipe.difficulty)
                    appendLine("<h3>${escapeHtml(recipe.title)} (${escapeHtml(catDisp)})</h3>")
                    appendLine(
                        "<p>${escapeHtml(loc.getString(R.string.ai_doc_servings_fmt, recipe.servings, recipe.prepTimeMinutes, recipe.cookTimeMinutes, diffDisp))}</p>"
                    )
                    if (recipe.ingredients.isNotEmpty()) {
                        appendLine("<p><b>${escapeHtml(loc.getString(R.string.ai_doc_ingredients_label))}:</b></p><ul>")
                        for (ing in recipe.ingredients) {
                            val unitDisp = LocalizedLabels.unit(loc, ing.unit)
                            appendLine("<li>${escapeHtml(ing.name)}: ${escapeHtml(fmtNumber(ing.amount))} ${escapeHtml(unitDisp)}</li>")
                        }
                        appendLine("</ul>")
                    }
                    if (recipe.instructions.isNotEmpty()) {
                        appendLine("<p><b>${escapeHtml(loc.getString(R.string.ai_doc_steps_label))}:</b></p><ol>")
                        for (step in recipe.instructions) {
                            appendLine("<li>${escapeHtml(step.trim())}</li>")
                        }
                        appendLine("</ol>")
                    }
                    if (recipe.notes.isNotBlank()) {
                        appendLine("<p><b>${escapeHtml(loc.getString(R.string.ai_doc_notes_label))}:</b> ${escapeHtml(recipe.notes.trim())}</p>")
                    }
                }
            }

            if (productionLogs.isNotEmpty()) {
                appendLine("<h2>${escapeHtml(loc.getString(R.string.ai_doc_production_section))}</h2><ul>")
                for (log in productionLogs.take(25)) {
                    val logCurrency = CurrencyOption.fromCode(log.currencyCode)
                    appendLine(
                        "<li><b>${escapeHtml(log.recipeTitle)}</b>: ${log.targetServings} serv. — " +
                            "${escapeHtml(AppSettingsPreferences.formatMoney(log.totalCost, logCurrency))} -> " +
                            "${escapeHtml(AppSettingsPreferences.formatMoney(log.suggestedPrice, logCurrency))} (+${log.marginPercent}%)</li>"
                    )
                }
                appendLine("</ul>")
            }

            appendLine("</body></html>")
        }
    }

    fun exportToCsv(
        context: Context,
        recipes: List<Recipe>,
        pantry: List<PantryItem>,
        shopping: List<ShoppingItem>
    ): String {
        val loc = LocaleHelper.localizedContext(context)
        val currency = AppSettingsPreferences.getCurrency(context)
        val sb = StringBuilder()
        // UTF-8 BOM so Excel / Google Sheets parse Spanish/French/Portuguese accents cleanly
        sb.append('\uFEFF')
        sb.appendLine("Tipo,Categoria,Nombre,Cantidad,Unidad,Estado_o_Minimo,Costo,Detalle_Ingredientes")

        for (item in pantry.sortedBy { it.name.lowercase() }) {
            val catDisp = LocalizedLabels.pantryCategory(loc, item.category)
            val unitDisp = LocalizedLabels.unit(loc, item.unit)
            val status = pantryStatusLabel(loc, item)
            val detail = listOfNotNull(
                if (item.minQuantity > 0.0) "min ${fmtNumber(item.minQuantity)} $unitDisp" else null,
                if (item.expirationDate.isNotBlank()) "exp ${item.expirationDate}" else null
            ).joinToString(" | ")
            val costStr = if (item.cost > 0.0) AppSettingsPreferences.formatMoney(item.cost, currency) else ""
            sb.appendLine(
                listOf(
                    "Despensa",
                    escapeCsv(catDisp),
                    escapeCsv(item.name),
                    fmtNumber(item.quantity),
                    escapeCsv(unitDisp),
                    escapeCsv(status),
                    escapeCsv(costStr),
                    escapeCsv(detail)
                ).joinToString(",")
            )
        }

        for (item in shopping.sortedWith(compareBy<ShoppingItem> { it.isPurchased }.thenBy { it.name.lowercase() })) {
            val catDisp = LocalizedLabels.pantryCategory(loc, item.category)
            val unitDisp = LocalizedLabels.unit(loc, item.unit)
            val status = if (item.isPurchased) {
                loc.getString(R.string.ai_doc_shopping_done)
            } else {
                loc.getString(R.string.ai_doc_shopping_pending)
            }
            sb.appendLine(
                listOf(
                    "Compras",
                    escapeCsv(catDisp),
                    escapeCsv(item.name),
                    fmtNumber(item.quantity),
                    escapeCsv(unitDisp),
                    escapeCsv(status),
                    "",
                    escapeCsv(item.recipeSource)
                ).joinToString(",")
            )
        }

        for (recipe in recipes.sortedBy { it.title.lowercase() }) {
            val catDisp = LocalizedLabels.recipeCategory(loc, recipe.category)
            val ingList = recipe.ingredients.joinToString("; ") { ing ->
                "${ing.name}: ${fmtNumber(ing.amount)} ${LocalizedLabels.unit(loc, ing.unit)}"
            }
            sb.appendLine(
                listOf(
                    "Receta",
                    escapeCsv(catDisp),
                    escapeCsv(recipe.title),
                    recipe.servings.toString(),
                    "porciones",
                    escapeCsv("${recipe.totalTimeMinutes} min"),
                    "",
                    escapeCsv(ingList)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    fun writeShareableExportFile(
        context: Context,
        fileName: String,
        content: String
    ): Uri {
        val dir = File(context.cacheDir, "ai_exports").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(dir, safeName)
        outFile.writeText(content, Charsets.UTF_8)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }
}
