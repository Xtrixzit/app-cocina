package com.example.util

import com.example.data.model.PantryItem
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

object UnitConverter {

    enum class Dimension {
        MASS,
        VOLUME,
        COUNT
    }

    private data class UnitSpec(
        val canonical: String,
        val dimension: Dimension,
        val toBaseFactor: Double
    )

    data class PantryCostBreakdown(
        val costInItemUnit: Double,
        val baseQuantityInItemUnit: Double,
        val unitCostInItemUnit: Double
    )

    private val unitMap: Map<String, UnitSpec> = mapOf(
        // Mass (base unit: g)
        "mg" to UnitSpec("mg", Dimension.MASS, 0.001),
        "miligramo" to UnitSpec("mg", Dimension.MASS, 0.001),
        "miligramos" to UnitSpec("mg", Dimension.MASS, 0.001),
        "milligram" to UnitSpec("mg", Dimension.MASS, 0.001),
        "milligrams" to UnitSpec("mg", Dimension.MASS, 0.001),
        "g" to UnitSpec("g", Dimension.MASS, 1.0),
        "gr" to UnitSpec("g", Dimension.MASS, 1.0),
        "grs" to UnitSpec("g", Dimension.MASS, 1.0),
        "gramo" to UnitSpec("g", Dimension.MASS, 1.0),
        "gramos" to UnitSpec("g", Dimension.MASS, 1.0),
        "gram" to UnitSpec("g", Dimension.MASS, 1.0),
        "grams" to UnitSpec("g", Dimension.MASS, 1.0),
        "gramme" to UnitSpec("g", Dimension.MASS, 1.0),
        "grammes" to UnitSpec("g", Dimension.MASS, 1.0),
        "kg" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kgs" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilo" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilos" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilogramo" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilogramos" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilogram" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilograms" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilogramme" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "kilogrammes" to UnitSpec("kg", Dimension.MASS, 1000.0),
        "oz" to UnitSpec("oz", Dimension.MASS, 28.349523125),
        "onza" to UnitSpec("oz", Dimension.MASS, 28.349523125),
        "onzas" to UnitSpec("oz", Dimension.MASS, 28.349523125),
        "ounce" to UnitSpec("oz", Dimension.MASS, 28.349523125),
        "ounces" to UnitSpec("oz", Dimension.MASS, 28.349523125),
        "lb" to UnitSpec("lb", Dimension.MASS, 453.59237),
        "lbs" to UnitSpec("lb", Dimension.MASS, 453.59237),
        "libra" to UnitSpec("lb", Dimension.MASS, 453.59237),
        "libras" to UnitSpec("lb", Dimension.MASS, 453.59237),
        "pound" to UnitSpec("lb", Dimension.MASS, 453.59237),
        "pounds" to UnitSpec("lb", Dimension.MASS, 453.59237),

        // Volume (base unit: ml)
        "ml" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "cc" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "mililitro" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "mililitros" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "milliliter" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "milliliters" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "millilitre" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "millilitres" to UnitSpec("ml", Dimension.VOLUME, 1.0),
        "cl" to UnitSpec("cl", Dimension.VOLUME, 10.0),
        "dl" to UnitSpec("dl", Dimension.VOLUME, 100.0),
        "l" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "lt" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "lts" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "litro" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "litros" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "liter" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "liters" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "litre" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "litres" to UnitSpec("l", Dimension.VOLUME, 1000.0),
        "cdta" to UnitSpec("cdta", Dimension.VOLUME, 5.0),
        "cdtas" to UnitSpec("cdta", Dimension.VOLUME, 5.0),
        "tsp" to UnitSpec("cdta", Dimension.VOLUME, 5.0),
        "cda" to UnitSpec("cda", Dimension.VOLUME, 15.0),
        "cdas" to UnitSpec("cda", Dimension.VOLUME, 15.0),
        "tbsp" to UnitSpec("cda", Dimension.VOLUME, 15.0),
        "fl oz" to UnitSpec("fl oz", Dimension.VOLUME, 29.5735),
        "floz" to UnitSpec("fl oz", Dimension.VOLUME, 29.5735),
        "fl_oz" to UnitSpec("fl oz", Dimension.VOLUME, 29.5735),
        "taza" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "tazas" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "cup" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "cups" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "xícara" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "xícaras" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "tasse" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "tasses" to UnitSpec("taza", Dimension.VOLUME, 240.0),
        "vaso" to UnitSpec("vaso", Dimension.VOLUME, 240.0),
        "vasos" to UnitSpec("vaso", Dimension.VOLUME, 240.0),
        "gal" to UnitSpec("gal", Dimension.VOLUME, 3785.41),
        "galon" to UnitSpec("gal", Dimension.VOLUME, 3785.41),
        "galón" to UnitSpec("gal", Dimension.VOLUME, 3785.41),
        "gallon" to UnitSpec("gal", Dimension.VOLUME, 3785.41),
        "gallons" to UnitSpec("gal", Dimension.VOLUME, 3785.41),

        // Count (base unit: 1 unit)
        "u" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "ud" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "uds" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "unidad" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "unidades" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "unit" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "units" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "unité" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "unités" to UnitSpec("unidades", Dimension.COUNT, 1.0),
        "pieza" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "piezas" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "piece" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "pieces" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "peça" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "peças" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "pièce" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "pièces" to UnitSpec("piezas", Dimension.COUNT, 1.0),
        "docena" to UnitSpec("docenas", Dimension.COUNT, 12.0),
        "docenas" to UnitSpec("docenas", Dimension.COUNT, 12.0),
        "dozen" to UnitSpec("docenas", Dimension.COUNT, 12.0),
        "dozens" to UnitSpec("docenas", Dimension.COUNT, 12.0),
        "dúzia" to UnitSpec("docenas", Dimension.COUNT, 12.0),
        "dúzias" to UnitSpec("docenas", Dimension.COUNT, 12.0),
        "douzaine" to UnitSpec("docenas", Dimension.COUNT, 12.0),
        "douzaines" to UnitSpec("docenas", Dimension.COUNT, 12.0),

        // Culinary discrete pack / item aliases
        "diente" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "dientes" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "clove" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "cloves" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "dente" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "dentes" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "gousse" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "gousses" to UnitSpec("dientes", Dimension.COUNT, 1.0),
        "lata" to UnitSpec("latas", Dimension.COUNT, 1.0),
        "latas" to UnitSpec("latas", Dimension.COUNT, 1.0),
        "can" to UnitSpec("latas", Dimension.COUNT, 1.0),
        "cans" to UnitSpec("latas", Dimension.COUNT, 1.0),
        "boîte" to UnitSpec("latas", Dimension.COUNT, 1.0),
        "boîtes" to UnitSpec("latas", Dimension.COUNT, 1.0),
        "paquete" to UnitSpec("paquetes", Dimension.COUNT, 1.0),
        "paquetes" to UnitSpec("paquetes", Dimension.COUNT, 1.0),
        "pack" to UnitSpec("paquetes", Dimension.COUNT, 1.0),
        "packs" to UnitSpec("paquetes", Dimension.COUNT, 1.0),
        "pacote" to UnitSpec("paquetes", Dimension.COUNT, 1.0),
        "pacotes" to UnitSpec("paquetes", Dimension.COUNT, 1.0),
        "paquet" to UnitSpec("paquetes", Dimension.COUNT, 1.0),
        "paquets" to UnitSpec("paquetes", Dimension.COUNT, 1.0)
    )

    val allPantryUnits: List<String> = listOf(
        "g", "kg", "lb", "oz", "mg",
        "ml", "l", "taza", "cda", "cdta", "fl oz",
        "unidades", "docenas", "piezas", "latas", "paquetes"
    )

    val allRecipeUnits: List<String> = listOf(
        "g", "kg", "lb", "oz", "mg",
        "ml", "l", "taza", "cda", "cdta", "fl oz",
        "unidades", "docenas", "piezas", "pizca", "dientes", "latas", "paquetes"
    )

    val allShoppingUnits: List<String> = listOf(
        "unidades", "kg", "g", "lb", "oz", "l", "ml", "paquetes", "latas", "piezas", "docenas"
    )

    private fun specOf(unit: String): UnitSpec? = unitMap[unit.trim().lowercase()]

    fun canonicalUnit(unit: String): String {
        val cleaned = unit.trim().lowercase()
        return specOf(cleaned)?.canonical ?: unit.trim()
    }

    fun dimensionOf(unit: String): Dimension? = specOf(unit)?.dimension

    fun isSameOrCompatible(unitA: String, unitB: String): Boolean {
        val a = unitA.trim().lowercase()
        val b = unitB.trim().lowercase()
        if (a == b) return true
        val specA = specOf(a) ?: return false
        val specB = specOf(b) ?: return false
        return specA.dimension == specB.dimension
    }

    fun compatibleUnitsFor(unit: String): List<String> {
        return when (dimensionOf(unit)) {
            Dimension.MASS -> listOf("g", "kg", "lb", "oz", "mg")
            Dimension.VOLUME -> listOf("ml", "l", "taza", "cda", "cdta", "fl oz")
            Dimension.COUNT -> {
                val c = canonicalUnit(unit)
                if (c in listOf("latas", "paquetes", "dientes")) {
                    listOf(c)
                } else {
                    listOf("unidades", "docenas", "piezas")
                }
            }
            null -> listOf(unit.trim().ifBlank { "unidades" })
        }
    }

    /**
     * Parses numeric user input tolerating regional decimal commas ("1,5") and grouping separators ("1,250.50").
     */
    fun parseDecimal(raw: String): Double? {
        val cleaned = raw.trim().replace(" ", "")
        if (cleaned.isEmpty()) return null
        val commaCount = cleaned.count { it == ',' }
        val dotCount = cleaned.count { it == '.' }
        val normalized = when {
            commaCount == 0 && dotCount <= 1 -> cleaned
            dotCount == 0 && commaCount == 1 -> cleaned.replace(',', '.')
            dotCount == 0 && commaCount > 1 -> cleaned.replace(",", "")
            commaCount == 0 && dotCount > 1 -> cleaned.replace(".", "")
            else -> {
                val lastComma = cleaned.lastIndexOf(',')
                val lastDot = cleaned.lastIndexOf('.')
                if (lastComma > lastDot) {
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "")
                }
            }
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /**
     * Parses numeric user input requiring a finite, non-negative value capped at [max].
     * Negative values, NaN, and Infinity return null.
     */
    fun parseNonNegativeDecimal(
        raw: String,
        max: Double = SecurityUtils.MAX_NUMERIC_VALUE
    ): Double? {
        val parsed = parseDecimal(raw) ?: return null
        if (!parsed.isFinite() || parsed < 0.0) return null
        return parsed.coerceAtMost(max)
    }

    /**
     * Formats quantities cleanly without losing sub-unit precision (e.g. 1.25 kg instead of 1.3 kg).
     */
    fun formatQuantity(value: Double, maxDecimals: Int = 3): String {
        if (!value.isFinite()) return "0"
        val clean = if (abs(value) < 1e-6) 0.0 else value
        val factor = 10.0.pow(maxDecimals.coerceIn(0, 6))
        val rounded = round(clean * factor) / factor
        if (abs(rounded - rounded.toLong()) < 1e-6) {
            return rounded.toLong().toString()
        }
        val formatted = String.format(Locale.US, "%.${maxDecimals}f", rounded)
        return formatted.trimEnd('0').trimEnd('.')
    }

    /**
     * Converts [amount] from [fromUnit] into [toUnit].
     * Returns null if units belong to incompatible dimensions (e.g. grams vs liters).
     */
    fun convert(amount: Double, fromUnit: String, toUnit: String): Double? {
        val cleanFrom = fromUnit.trim().lowercase()
        val cleanTo = toUnit.trim().lowercase()
        if (cleanFrom == cleanTo || cleanFrom.isBlank() || cleanTo.isBlank()) return amount

        val fromSpec = specOf(cleanFrom) ?: return null
        val toSpec = specOf(cleanTo) ?: return null
        if (fromSpec.dimension != toSpec.dimension) return null

        return (amount * fromSpec.toBaseFactor) / toSpec.toBaseFactor
    }

    /**
     * Converts a price quoted per 1 [sourceUnit] into price per 1 [targetUnit].
     * Example: $4.00 / kg -> $0.004 / g, or $1.814 / lb.
     */
    fun convertCostPerUnit(costPerSourceUnit: Double, sourceUnit: String, targetUnit: String): Double? {
        if (costPerSourceUnit <= 0.0) return 0.0
        val targetUnitInSourceUnits = convert(1.0, targetUnit, sourceUnit) ?: return null
        return costPerSourceUnit * targetUnitInSourceUnits
    }

    /**
     * Converts a batch or per-unit price entered in [priceInputUnit] into `cost` & `costBaseQuantity`
     * anchored in the Pantry item's native [itemUnit].
     */
    fun computePantryCostBreakdown(
        enteredPrice: Double,
        enteredBaseQty: Double,
        priceInputUnit: String,
        itemUnit: String,
        isPerSingleUnitMode: Boolean
    ): PantryCostBreakdown {
        if (enteredPrice <= 0.0) {
            return PantryCostBreakdown(0.0, 0.0, 0.0)
        }
        val effectiveBase = if (isPerSingleUnitMode) 1.0 else enteredBaseQty.coerceAtLeast(0.0001)
        val convertedBaseInItemUnit = convert(effectiveBase, priceInputUnit, itemUnit) ?: effectiveBase
        val safeBaseInItemUnit = convertedBaseInItemUnit.coerceAtLeast(0.0001)
        val unitCost = enteredPrice / safeBaseInItemUnit
        return PantryCostBreakdown(
            costInItemUnit = enteredPrice,
            baseQuantityInItemUnit = safeBaseInItemUnit,
            unitCostInItemUnit = unitCost
        )
    }

    /**
     * Suggests a natural human-friendly secondary unit hint when reading quantities
     * (e.g. 1500 g -> 1.5 kg, 0.45 kg -> 450 g, 1200 ml -> 1.2 l, 2 lb -> 907.185 g, 2 docenas -> 24 unidades).
     */
    fun humanEquivalent(amount: Double, unit: String): Pair<Double, String>? {
        if (amount <= 0.0) return null
        val spec = specOf(unit) ?: return null
        return when (spec.canonical) {
            "g" -> if (amount >= 1000.0) (amount / 1000.0) to "kg" else null
            "mg" -> if (amount >= 1000.0) (amount / 1000.0) to "g" else null
            "kg" -> if (amount < 1.0) (amount * 1000.0) to "g" else null
            "lb" -> {
                val grams = convert(amount, "lb", "g") ?: return null
                if (grams >= 1000.0) (grams / 1000.0) to "kg" else grams to "g"
            }
            "oz" -> {
                val grams = convert(amount, "oz", "g") ?: return null
                if (grams >= 1000.0) (grams / 1000.0) to "kg" else grams to "g"
            }
            "ml" -> if (amount >= 1000.0) (amount / 1000.0) to "l" else null
            "l" -> if (amount < 1.0) (amount * 1000.0) to "ml" else null
            "fl oz", "taza", "cda" -> {
                val ml = convert(amount, spec.canonical, "ml") ?: return null
                if (ml >= 1000.0) (ml / 1000.0) to "l" else ml to "ml"
            }
            "docenas" -> (amount * 12.0) to "unidades"
            "unidades" -> if (amount >= 12.0 && abs(amount % 12.0) < 0.01) (amount / 12.0) to "docenas" else null
            else -> null
        }
    }

    /**
     * Returns a standard bulk reference unit cost (e.g., per kg, per L, per docena) when item is priced
     * in smaller units so users can immediately sanity-check pricing.
     */
    fun macroUnitCostReference(unitCostInItemUnit: Double, itemUnit: String): Pair<Double, String>? {
        if (unitCostInItemUnit <= 0.0) return null
        val spec = specOf(itemUnit) ?: return null
        return when {
            spec.dimension == Dimension.MASS && spec.canonical != "kg" -> {
                convertCostPerUnit(unitCostInItemUnit, spec.canonical, "kg")?.let { it to "kg" }
            }
            spec.dimension == Dimension.VOLUME && spec.canonical != "l" -> {
                convertCostPerUnit(unitCostInItemUnit, spec.canonical, "l")?.let { it to "l" }
            }
            spec.canonical == "unidades" || spec.canonical == "piezas" -> {
                (unitCostInItemUnit * 12.0) to "docenas"
            }
            else -> null
        }
    }

    /**
     * Smart increment/decrement step depending on the item's unit and current magnitude.
     */
    fun quickAdjustStep(quantity: Double, unit: String): Double {
        return when (specOf(unit)?.canonical ?: unit.trim().lowercase()) {
            "mg" -> 100.0
            "g", "ml" -> when {
                quantity < 25.0 -> 5.0
                quantity < 100.0 -> 10.0
                else -> 50.0
            }
            "kg", "l", "lb" -> 0.25
            "oz", "fl oz" -> 1.0
            else -> 1.0
        }
    }

    /**
     * Sums total mass (in grams) and total volume (in ml) from scaled ingredient quantities.
     */
    fun sumMassAndVolume(amountsWithUnits: List<Pair<Double, String>>): Pair<Double, Double> {
        var totalGrams = 0.0
        var totalMl = 0.0
        for ((amt, unit) in amountsWithUnits) {
            if (amt <= 0.0) continue
            when (dimensionOf(unit)) {
                Dimension.MASS -> totalGrams += convert(amt, unit, "g") ?: 0.0
                Dimension.VOLUME -> totalMl += convert(amt, unit, "ml") ?: 0.0
                else -> Unit
            }
        }
        return totalGrams to totalMl
    }

    /**
     * Converts [pantryItem]'s current stock into [targetUnit] so shortages & sufficiency checks
     * remain accurate even when pantry uses kg/l/lb/oz and recipes use g/ml/tbsp/cup.
     */
    fun availableInTargetUnit(pantryItem: PantryItem?, targetUnit: String): Double {
        if (pantryItem == null) return 0.0
        val converted = convert(pantryItem.quantity, pantryItem.unit, targetUnit)
        return converted ?: 0.0
    }

    /**
     * Calculates how much must be subtracted from [pantryUnit] given [neededAmount] in [recipeUnit].
     */
    fun deductAmountInPantryUnit(neededAmount: Double, recipeUnit: String, pantryUnit: String): Double? {
        return convert(neededAmount, recipeUnit, pantryUnit)
    }

    /**
     * Converts [pantryItem.unitCost] (cost per 1 [pantryItem.unit]) into cost per 1 [targetUnit].
     */
    fun unitCostInTargetUnit(pantryItem: PantryItem?, targetUnit: String): Double {
        if (pantryItem == null || pantryItem.unitCost <= 0.0) return 0.0
        return convertCostPerUnit(pantryItem.unitCost, pantryItem.unit, targetUnit) ?: 0.0
    }
}
