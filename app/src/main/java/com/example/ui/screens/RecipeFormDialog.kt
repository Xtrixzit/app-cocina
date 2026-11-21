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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.settings.AppSettingsPreferences
import com.example.ui.components.PostItButton
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.util.ImageUtils
import com.example.util.LocalizedLabels
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormDialog(
    initialRecipe: Recipe? = null,
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit
) {
    val context = LocalContext.current
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val isEdit = initialRecipe != null

    var title by remember { mutableStateOf(initialRecipe?.title ?: "") }
    var category by remember { mutableStateOf(initialRecipe?.category ?: "Almuerzos") }
    var servings by remember { mutableIntStateOf(initialRecipe?.servings ?: 4) }
    var prepTime by remember { mutableIntStateOf(initialRecipe?.prepTimeMinutes ?: 15) }
    var cookTime by remember { mutableIntStateOf(initialRecipe?.cookTimeMinutes ?: 30) }
    var difficulty by remember { mutableStateOf(initialRecipe?.difficulty ?: "Fácil") }
    var notes by remember { mutableStateOf(initialRecipe?.notes ?: "") }
    var imageUrl by remember { mutableStateOf(initialRecipe?.imageUrl ?: "") }

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
                            if (isEdit) stringResource(R.string.recipe_form_edit_title) else stringResource(R.string.recipe_form_new_title),
                            fontFamily = CaveatFontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("recipe_form_close")) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        PostItButton(
                            text = stringResource(R.string.action_save),
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
                    colors = TopAppBarDefaults.topAppBarColors(
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
                                text = stringResource(R.string.recipe_form_photo_title),
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            if (imageUrl.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PostItButton(
                                        text = stringResource(R.string.action_change),
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
                                            contentDescription = stringResource(R.string.recipe_form_photo_remove_cd),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (imageUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageUtils.toImageModel(imageUrl),
                                    contentDescription = stringResource(R.string.recipe_detail_photo_cd, title),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        } else {
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
                                        text = stringResource(R.string.recipe_form_photo_add),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.recipe_form_photo_sub),
                                        fontFamily = PatrickHandFontFamily,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.recipe_form_title_label)) },
                    placeholder = { Text(stringResource(R.string.recipe_form_title_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_form_title"),
                    singleLine = true
                )

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
                            value = LocalizedLabels.recipeCategory(context, category),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.recipe_form_cat_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(LocalizedLabels.recipeCategory(context, cat)) },
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
                            value = LocalizedLabels.difficulty(context, difficulty),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.recipe_form_diff_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diffExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = diffExpanded,
                            onDismissRequest = { diffExpanded = false }
                        ) {
                            difficulties.forEach { diff ->
                                DropdownMenuItem(
                                    text = { Text(LocalizedLabels.difficulty(context, diff)) },
                                    onClick = {
                                        difficulty = diff
                                        diffExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = servings.toString(),
                        onValueChange = { servings = it.toIntOrNull() ?: 1 },
                        label = { Text(stringResource(R.string.recipe_form_servings_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_servings"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = prepTime.toString(),
                        onValueChange = { prepTime = it.toIntOrNull() ?: 0 },
                        label = { Text(stringResource(R.string.recipe_form_prep_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_prep_time"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cookTime.toString(),
                        onValueChange = { cookTime = it.toIntOrNull() ?: 0 },
                        label = { Text(stringResource(R.string.recipe_form_cook_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_cook_time"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recipe_form_ingredients_header, servings),
                        fontFamily = CaveatFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    PostItButton(
                        text = stringResource(R.string.recipe_form_add_ingredient),
                        icon = Icons.Default.Add,
                        onClick = {
                            ingredients.add(RecipeIngredient(name = "", amount = 100.0, unit = "g", category = "Despensa"))
                        },
                        backgroundColor = PostItPeach,
                        fontSize = 17.sp,
                        rotationDegrees = -1f,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
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
                                    label = { Text(stringResource(R.string.recipe_form_ingredient_num, index + 1)) },
                                    placeholder = { Text(stringResource(R.string.recipe_form_ingredient_hint)) },
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
                                            contentDescription = stringResource(R.string.recipe_form_remove_ing_cd),
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
                                    label = { Text(stringResource(R.string.recipe_form_qty_label)) },
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
                                        value = LocalizedLabels.unit(context, ingredient.unit),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.recipe_form_unit_label)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = unitExpanded,
                                        onDismissRequest = { unitExpanded = false }
                                    ) {
                                        units.forEach { u ->
                                            DropdownMenuItem(
                                                text = { Text(LocalizedLabels.unit(context, u)) },
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
                                    if (ingredient.manualUnitCost < 0.01) {
                                        String.format(Locale.US, "%.4f", ingredient.manualUnitCost)
                                    } else {
                                        ingredient.manualUnitCost.toString()
                                    }
                                } else "",
                                onValueChange = {
                                    val cost = it.toDoubleOrNull() ?: 0.0
                                    ingredients[index] = ingredient.copy(manualUnitCost = cost)
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.recipe_form_manual_cost_label,
                                            currency.symbol,
                                            LocalizedLabels.unit(context, ingredient.unit)
                                        )
                                    )
                                },
                                placeholder = { Text(stringResource(R.string.recipe_form_manual_cost_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                text = stringResource(R.string.recipe_form_instructions_title),
                                fontFamily = CaveatFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = instructionsMode == 0,
                                    onClick = {
                                        if (instructionsMode == 1) {
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
                                    label = { Text(stringResource(R.string.recipe_form_mode_steps)) },
                                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = instructionsMode == 1,
                                    onClick = {
                                        if (instructionsMode == 0) {
                                            freeTextInstructions = instructions.filter { it.isNotBlank() }.joinToString("\n\n")
                                        }
                                        instructionsMode = 1
                                    },
                                    label = { Text(stringResource(R.string.recipe_form_mode_free)) },
                                    leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (instructionsMode == 0) {
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
                                            placeholder = { Text(stringResource(R.string.recipe_form_step_hint, index + 1)) },
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
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.recipe_form_step_up_cd), modifier = Modifier.size(16.dp))
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
                                                    Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.recipe_form_step_down_cd), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            if (instructions.size > 1) {
                                                IconButton(
                                                    onClick = { instructions.removeAt(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.recipe_form_step_del_cd), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            PostItButton(
                                text = stringResource(R.string.recipe_form_add_step),
                                icon = Icons.Default.Add,
                                onClick = { instructions.add("") },
                                backgroundColor = PostItYellow,
                                fontSize = 17.sp,
                                modifier = Modifier.testTag("recipe_form_add_step")
                            )
                        } else {
                            OutlinedTextField(
                                value = freeTextInstructions,
                                onValueChange = { freeTextInstructions = it },
                                label = { Text(stringResource(R.string.recipe_form_free_label)) },
                                placeholder = { Text(stringResource(R.string.recipe_form_free_hint)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recipe_form_free_text_instructions"),
                                minLines = 8,
                                maxLines = 16
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.recipe_form_free_tip),
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.recipe_form_notes_label)) },
                    placeholder = { Text(stringResource(R.string.recipe_form_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
