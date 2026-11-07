package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.ui.components.notebookPaperBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormDialog(
    initialRecipe: Recipe? = null,
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit
) {
    val isEdit = initialRecipe != null

    var title by remember { mutableStateOf(initialRecipe?.title ?: "") }
    var category by remember { mutableStateOf(initialRecipe?.category ?: "Almuerzos") }
    var servings by remember { mutableIntStateOf(initialRecipe?.servings ?: 4) }
    var prepTime by remember { mutableIntStateOf(initialRecipe?.prepTimeMinutes ?: 15) }
    var cookTime by remember { mutableIntStateOf(initialRecipe?.cookTimeMinutes ?: 30) }
    var difficulty by remember { mutableStateOf(initialRecipe?.difficulty ?: "Fácil") }
    var notes by remember { mutableStateOf(initialRecipe?.notes ?: "") }

    val ingredients = remember {
        mutableStateListOf<RecipeIngredient>().apply {
            if (initialRecipe != null && initialRecipe.ingredients.isNotEmpty()) {
                addAll(initialRecipe.ingredients)
            } else {
                add(RecipeIngredient(name = "", amount = 100.0, unit = "g", category = "Despensa"))
            }
        }
    }

    val instructions = remember {
        mutableStateListOf<String>().apply {
            if (initialRecipe != null && initialRecipe.instructions.isNotEmpty()) {
                addAll(initialRecipe.instructions)
            } else {
                add("")
            }
        }
    }

    val categories = listOf("Desayunos", "Almuerzos", "Cenas", "Postres", "Snacks", "Bebidas", "Sopas")
    val difficulties = listOf("Fácil", "Media", "Difícil")
    val units = listOf("g", "kg", "ml", "l", "unidades", "taza", "cda", "cdta", "pizca", "dientes")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isEdit) "Editar Receta" else "Nueva Receta",
                            fontFamily = com.example.ui.theme.CaveatFontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("recipe_form_close")) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        com.example.ui.components.PostItButton(
                            text = "Guardar",
                            onClick = {
                                if (title.isNotBlank()) {
                                    val cleanedIngredients = ingredients.filter { it.name.isNotBlank() && it.amount > 0 }
                                    val cleanedInstructions = instructions.filter { it.isNotBlank() }
                                    val newRecipe = (initialRecipe ?: Recipe()).copy(
                                        title = title.trim(),
                                        category = category,
                                        servings = servings.coerceAtLeast(1),
                                        prepTimeMinutes = prepTime.coerceAtLeast(0),
                                        cookTimeMinutes = cookTime.coerceAtLeast(0),
                                        difficulty = difficulty,
                                        ingredients = cleanedIngredients,
                                        instructions = cleanedInstructions,
                                        notes = notes.trim()
                                    )
                                    onSave(newRecipe)
                                    onDismiss()
                                }
                            },
                            backgroundColor = com.example.ui.theme.PostItMint,
                            fontSize = 19.sp,
                            rotationDegrees = -1f,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("recipe_form_save")
                        )
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .notebookPaperBackground()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre de la receta *") },
                    placeholder = { Text("Ej. Paella de Mariscos") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_form_title"),
                    singleLine = true
                )

                // Category & Difficulty Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var catExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = catExpanded,
                        onExpandedChange = { catExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var diffExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = diffExpanded,
                        onExpandedChange = { diffExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = difficulty,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dificultad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diffExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = diffExpanded,
                            onDismissRequest = { diffExpanded = false }
                        ) {
                            difficulties.forEach { diff ->
                                DropdownMenuItem(
                                    text = { Text(diff) },
                                    onClick = {
                                        difficulty = diff
                                        diffExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Servings, Prep Time, Cook Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = servings.toString(),
                        onValueChange = { servings = it.toIntOrNull() ?: 1 },
                        label = { Text("Porciones") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_servings"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = prepTime.toString(),
                        onValueChange = { prepTime = it.toIntOrNull() ?: 0 },
                        label = { Text("Prep. (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_prep_time"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cookTime.toString(),
                        onValueChange = { cookTime = it.toIntOrNull() ?: 0 },
                        label = { Text("Cocción (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_cook_time"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- INGREDIENTS SECTION ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ingredientes base (para $servings porciones)",
                        fontFamily = com.example.ui.theme.CaveatFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    com.example.ui.components.PostItButton(
                        text = "+ Ingrediente",
                        icon = Icons.Default.Add,
                        onClick = {
                            ingredients.add(RecipeIngredient(name = "", amount = 100.0, unit = "g", category = "Despensa"))
                        },
                        backgroundColor = com.example.ui.theme.PostItPeach,
                        fontSize = 17.sp,
                        rotationDegrees = -1f,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("recipe_form_add_ingredient")
                    )
                }

                ingredients.forEachIndexed { index, ingredient ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = ingredient.name,
                                    onValueChange = { ingredients[index] = ingredient.copy(name = it) },
                                    label = { Text("Ingrediente #${index + 1}") },
                                    placeholder = { Text("Ej. Harina de trigo") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (ingredients.size > 1) {
                                    IconButton(
                                        onClick = { ingredients.removeAt(index) },
                                        modifier = Modifier.testTag("recipe_form_remove_ingredient_$index")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Quitar ingrediente",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (ingredient.amount % 1.0 == 0.0) ingredient.amount.toInt().toString() else ingredient.amount.toString(),
                                    onValueChange = {
                                        val amt = it.toDoubleOrNull() ?: 0.0
                                        ingredients[index] = ingredient.copy(amount = amt)
                                    },
                                    label = { Text("Cantidad") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                var unitExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = unitExpanded,
                                    onExpandedChange = { unitExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = ingredient.unit,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Unidad") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = unitExpanded,
                                        onDismissRequest = { unitExpanded = false }
                                    ) {
                                        units.forEach { u ->
                                            DropdownMenuItem(
                                                text = { Text(u) },
                                                onClick = {
                                                    ingredients[index] = ingredient.copy(unit = u)
                                                    unitExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Manual unit cost field (optional fallback)
                            OutlinedTextField(
                                value = if (ingredient.manualUnitCost > 0.0) {
                                    if (ingredient.manualUnitCost < 0.01) "%.4f".format(ingredient.manualUnitCost) else ingredient.manualUnitCost.toString()
                                } else "",
                                onValueChange = {
                                    val cost = it.toDoubleOrNull() ?: 0.0
                                    ingredients[index] = ingredient.copy(manualUnitCost = cost)
                                },
                                label = { Text("Costo unitario manual ($/${ingredient.unit}) - Opcional") },
                                placeholder = { Text("Si está en 0, usa el costo de tu despensa") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- INSTRUCTIONS SECTION ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Instrucciones de preparación",
                        fontFamily = com.example.ui.theme.CaveatFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    com.example.ui.components.PostItButton(
                        text = "+ Paso",
                        icon = Icons.Default.Add,
                        onClick = { instructions.add("") },
                        backgroundColor = com.example.ui.theme.PostItMint,
                        fontSize = 17.sp,
                        rotationDegrees = 1f,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("recipe_form_add_step")
                    )
                }

                instructions.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = step,
                            onValueChange = { instructions[index] = it },
                            label = { Text("Paso ${index + 1}") },
                            placeholder = { Text("Describe este paso de la receta...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 4
                        )
                        if (instructions.size > 1) {
                            IconButton(onClick = { instructions.removeAt(index) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Quitar paso",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Consejos y notas del chef (opcional)") },
                    placeholder = { Text("Trucos para que quede perfecto, alternativas de ingredientes...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
