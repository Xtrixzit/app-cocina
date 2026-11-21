package com.example.util

import com.example.data.model.PantryItem

object UnitConverter {

    private enum class Dimension {
        MASS,
        VOLUME,
        COUNT
    }

    private data class UnitSpec(
        val dimension: Dimension,
        val toBaseFactor: Double
    )

    private val unitMap: Map<String, UnitSpec> = mapOf(
        // Mass (base unit: g)
        "mg" to UnitSpec(Dimension.MASS, 0.001),
        "g" to UnitSpec(Dimension.MASS, 1.0),
        "gr" to UnitSpec(Dimension.MASS, 1.0),
        "gramo" to UnitSpec(Dimension.MASS, 1.0),
        "gramos" to UnitSpec(Dimension.MASS, 1.0),
        "grams" to UnitSpec(Dimension.MASS, 1.0),
        "kg" to UnitSpec(Dimension.MASS, 1000.0),
        "kilo" to UnitSpec(Dimension.MASS, 1000.0),
        "kilos" to UnitSpec(Dimension.MASS, 1000.0),
        "oz" to UnitSpec(Dimension.MASS, 28.349523125),
        "lb" to UnitSpec(Dimension.MASS, 453.59237),
        "lbs" to UnitSpec(Dimension.MASS, 453.59237),

        // Volume (base unit: ml)
        "ml" to UnitSpec(Dimension.VOLUME, 1.0),
        "cl" to UnitSpec(Dimension.VOLUME, 10.0),
        "dl" to UnitSpec(Dimension.VOLUME, 100.0),
        "l" to UnitSpec(Dimension.VOLUME, 1000.0),
        "lt" to UnitSpec(Dimension.VOLUME, 1000.0),
        "litro" to UnitSpec(Dimension.VOLUME, 1000.0),
        "litros" to UnitSpec(Dimension.VOLUME, 1000.0),
        "liter" to UnitSpec(Dimension.VOLUME, 1000.0),
        "liters" to UnitSpec(Dimension.VOLUME, 1000.0),
        "cdta" to UnitSpec(Dimension.VOLUME, 5.0),
        "cdtas" to UnitSpec(Dimension.VOLUME, 5.0),
        "tsp" to UnitSpec(Dimension.VOLUME, 5.0),
        "cda" to UnitSpec(Dimension.VOLUME, 15.0),
        "cdas" to UnitSpec(Dimension.VOLUME, 15.0),
        "tbsp" to UnitSpec(Dimension.VOLUME, 15.0),
        "taza" to UnitSpec(Dimension.VOLUME, 240.0),
        "tazas" to UnitSpec(Dimension.VOLUME, 240.0),
        "cup" to UnitSpec(Dimension.VOLUME, 240.0),
        "cups" to UnitSpec(Dimension.VOLUME, 240.0),
        "vaso" to UnitSpec(Dimension.VOLUME, 240.0),
        "vasos" to UnitSpec(Dimension.VOLUME, 240.0),

        // Count (base unit: 1 unit)
        "u" to UnitSpec(Dimension.COUNT, 1.0),
        "ud" to UnitSpec(Dimension.COUNT, 1.0),
        "uds" to UnitSpec(Dimension.COUNT, 1.0),
        "unidad" to UnitSpec(Dimension.COUNT, 1.0),
        "unidades" to UnitSpec(Dimension.COUNT, 1.0),
        "unit" to UnitSpec(Dimension.COUNT, 1.0),
        "units" to UnitSpec(Dimension.COUNT, 1.0),
        "pieza" to UnitSpec(Dimension.COUNT, 1.0),
        "piezas" to UnitSpec(Dimension.COUNT, 1.0),
        "piece" to UnitSpec(Dimension.COUNT, 1.0),
        "pieces" to UnitSpec(Dimension.COUNT, 1.0),
        "docena" to UnitSpec(Dimension.COUNT, 12.0),
        "docenas" to UnitSpec(Dimension.COUNT, 12.0),
        "dozen" to UnitSpec(Dimension.COUNT, 12.0),
        "dozens" to UnitSpec(Dimension.COUNT, 12.0),

        // Culinary small / discrete pack aliases
        "diente" to UnitSpec(Dimension.COUNT, 1.0),
        "dientes" to UnitSpec(Dimension.COUNT, 1.0),
        "clove" to UnitSpec(Dimension.COUNT, 1.0),
        "cloves" to UnitSpec(Dimension.COUNT, 1.0),
        "lata" to UnitSpec(Dimension.COUNT, 1.0),
        "latas" to UnitSpec(Dimension.COUNT, 1.0),
        "can" to UnitSpec(Dimension.COUNT, 1.0),
        "cans" to UnitSpec(Dimension.COUNT, 1.0),
        "paquete" to UnitSpec(Dimension.COUNT, 1.0),
        "paquetes" to UnitSpec(Dimension.COUNT, 1.0),
        "pack" to UnitSpec(Dimension.COUNT, 1.0),
        "packs" to UnitSpec(Dimension.COUNT, 1.0)
    )

    private fun specOf(unit: String): UnitSpec? = unitMap[unit.trim().lowercase()]

    fun isSameOrCompatible(unitA: String, unitB: String): Boolean {
        val a = unitA.trim().lowercase()
        val b = unitB.trim().lowercase()
        if (a == b) return true
        val specA = specOf(a) ?: return false
        val specB = specOf(b) ?: return false
        return specA.dimension == specB.dimension
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
     * Converts [pantryItem]'s current stock into [targetUnit] so shortages & sufficiency checks
     * remain accurate even when pantry uses kg/l and recipes use g/ml/tbsp/cup.
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
        val targetInPantryUnits = convert(1.0, targetUnit, pantryItem.unit) ?: return 0.0
        return pantryItem.unitCost * targetInPantryUnits
    }
}
