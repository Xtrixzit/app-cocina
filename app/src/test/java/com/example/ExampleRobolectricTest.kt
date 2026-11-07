package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Recetario", appName)
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

        // Scale to 8 servings (2x)
        val targetServings = 8
        val scale = targetServings.toDouble() / baseRecipe.servings

        val scaledEggs = baseRecipe.ingredients[0].amount * scale
        val scaledPotatoes = baseRecipe.ingredients[1].amount * scale

        assertEquals(12.0, scaledEggs, 0.001)
        assertEquals(1200.0, scaledPotatoes, 0.001)

        // Scale to 2 servings (0.5x)
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

        // Pasta is sufficient (500 >= 200)
        val pastaInStock = pantry.find { it.name.equals(recipe.ingredients[0].name, true) }?.quantity ?: 0.0
        assertTrue(pastaInStock >= recipe.ingredients[0].amount)

        // Tomate is short (needed 150, have 50 -> shortage 100)
        val tomateInStock = pantry.find { it.name.equals(recipe.ingredients[1].name, true) }?.quantity ?: 0.0
        val shortage = recipe.ingredients[1].amount - tomateInStock
        assertEquals(100.0, shortage, 0.001)
    }
}
