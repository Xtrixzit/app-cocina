package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItTape
import com.example.ui.theme.PostItYellow
import com.example.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormDialog(
    initialRecipe: Recipe? = null,
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit
) {
    val context = LocalContext.current
    val isEdit = initialRecipe != null

    var title by remember { mutableStateOf(initialRecipe?.title ?: "") }
    var category by remember { mutableStateOf(initialRecipe?.category ?: "Almuerzos") }
    var servings by remember { mutableIntStateOf(initialRecipe?.servings ?: 4) }
    var prepTime by remember { mutableIntStateOf(initialRecipe?.prepTimeMinutes ?: 15) }
    var cookTime by remember { mutableIntStateOf(initialRecipe?.cookTimeMinutes ?: 30) }
    var difficulty by remember { mutableStateOf(initialRecipe?.difficulty ?: "Fácil") }
    var notes by remember { mutableStateOf(initialRecipe?.notes ?: "") }
    var imageUrl by remember { mutableStateOf(initialRecipe?.imageUrl ?: "") }

    // Preparation editing mode: 0 = Por Pasos estructurados, 1 = Texto Libre / Multilínea
    var instructionsMode by remember { mutableIntStateOf(0) }
    var freeTextInstructions by remember {
        mutableStateOf(initialRecipe?.instructions?.joinToString("\n\n") ?: "")
    }

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

    // Android Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageUtils.saveImageLocally(context, uri)
            imageUrl = savedPath
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
                                    
                                    val finalInstructions = if (instructionsMode == 1) {
                                        freeTextInstructions.split("\n")
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                    } else {
                                        instructions.filter { it.isNotBlank() }
                                    }

                                    val newRecipe = (initialRecipe ?: Recipe()).copy(
                                        title = title.trim(),
                                        category = category,
                                        servings = servings.coerceAtLeast(1),
                                        prepTimeMinutes = prepTime.coerceAtLeast(0),
                                        cookTimeMinutes = cookTime.coerceAtLeast(0),
                                        difficulty = difficulty,
                                        ingredients = cleanedIngredients,
                                        instructions = finalInstructions,
                                        notes = notes.trim(),
                                        imageUrl = imageUrl.trim()
                                    )
                                    onSave(newRecipe)
                                    onDismiss()
                                }
                            },
                            backgroundColor = PostItMint,
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
                // --- SECTION: FOTO DEL PRODUCTO FINAL ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), strokeWidthDp = 1.6f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Foto del Producto Final",
                                fontFamily = com.example.ui.theme.CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            if (imageUrl.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    com.example.ui.components.PostItButton(
                                        text = "Cambiar",
                                        icon = Icons.Default.AddPhotoAlternate,
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        backgroundColor = PostItSky,
                                        fontSize = 16.sp
                                    )
                                    IconButton(
                                        onClick = { imageUrl = "" },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Quitar foto",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (imageUrl.isNotBlank()) {
                            // Polaroid / Photo frame preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Foto de la receta",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        } else {
                            // Clickable placeholder to add image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .sketchedBorder(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        strokeWidthDp = 1.8f
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("recipe_form_pick_photo"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "+ Toca para agregar foto del producto terminado",
                                        fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Elige una foto de tu galería o cámara",
                                        fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

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
                        label = { Text("Porciones base") },
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
                        backgroundColor = PostItPeach,
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

                // --- SECTION: EDICIÓN DE PREPARACIÓN ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Instrucciones de Preparación",
                                fontFamily = com.example.ui.theme.CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            // Toggle between Step Mode and Free Text Mode
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = instructionsMode == 0,
                                    onClick = {
                                        if (instructionsMode == 1) {
                                            // Sync free text back to structured steps
                                            val lines = freeTextInstructions.split("\n")
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                            instructions.clear()
                                            if (lines.isNotEmpty()) {
                                                instructions.addAll(lines)
                                            } else {
                                                instructions.add("")
                                            }
                                        }
                                        instructionsMode = 0
                                    },
                                    label = { Text("Por Pasos") },
                                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = instructionsMode == 1,
                                    onClick = {
                                        if (instructionsMode == 0) {
                                            // Sync structured steps to free text
                                            freeTextInstructions = instructions.filter { it.isNotBlank() }.joinToString("\n\n")
                                        }
                                        instructionsMode = 1
                                    },
                                    label = { Text("Texto Libre") },
                                    leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (instructionsMode == 0) {
                            // Step by step editor with reorder & delete
                            instructions.forEachIndexed { index, step ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedTextField(
                                            value = step,
                                            onValueChange = { instructions[index] = it },
                                            placeholder = { Text("Describe el paso ${index + 1}...") },
                                            modifier = Modifier.weight(1f),
                                            minLines = 2,
                                            maxLines = 5
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (index > 0) {
                                                IconButton(
                                                    onClick = {
                                                        val temp = instructions[index]
                                                        instructions[index] = instructions[index - 1]
                                                        instructions[index - 1] = temp
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Subir paso", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            if (index < instructions.size - 1) {
                                                IconButton(
                                                    onClick = {
                                                        val temp = instructions[index]
                                                        instructions[index] = instructions[index + 1]
                                                        instructions[index + 1] = temp
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar paso", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            if (instructions.size > 1) {
                                                IconButton(
                                                    onClick = { instructions.removeAt(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Borrar paso", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            com.example.ui.components.PostItButton(
                                text = "+ Añadir Siguiente Paso",
                                icon = Icons.Default.Add,
                                onClick = { instructions.add("") },
                                backgroundColor = PostItYellow,
                                fontSize = 17.sp,
                                modifier = Modifier.testTag("recipe_form_add_step")
                            )
                        } else {
                            // Free multiline text editor for easy preparation typing / pasting
                            OutlinedTextField(
                                value = freeTextInstructions,
                                onValueChange = { freeTextInstructions = it },
                                label = { Text("Escribe o edita la preparación completa") },
                                placeholder = { Text("Separa cada paso con un salto de línea (Enter). Ejemplo:\n\n1. Picar las cebollas y ajo.\n\n2. Sofreír en aceite de oliva durante 10 minutos.\n\n3. Añadir el arroz y caldo...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recipe_form_free_text_instructions"),
                                minLines = 8,
                                maxLines = 16
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 Consejo: Puedes escribir o pegar todo el procedimiento aquí; cada párrafo se tomará como un paso de preparación.",
                                fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
