package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.PhotoAspectOption
import com.example.ai.RecipePhotoGenState
import com.example.data.model.PantryItem
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
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen
import com.example.util.ImageUtils
import com.example.util.LocalizedLabels
import com.example.util.SampleContentLocalizer
import com.example.util.SecurityUtils
import com.example.util.UnitConverter
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeFormDialog(
    initialRecipe: Recipe? = null,
    pantryItems: List<PantryItem> = emptyList(),
    photoGenState: RecipePhotoGenState = RecipePhotoGenState.Idle,
    onGenerateAiPhoto: (title: String, category: String, ingredients: List<RecipeIngredient>) -> Unit = { _, _, _ -> },
    onGenerateAiPhotoWithAspect: ((title: String, category: String, ingredients: List<RecipeIngredient>, aspectOption: PhotoAspectOption) -> Unit)? = null,
    onEnhanceRealPhoto: (sourceImageUrl: String, title: String, category: String, aspectOption: PhotoAspectOption) -> Unit = { _, _, _, _ -> },
    onResetPhotoGenState: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit
) {
    val context = LocalContext.current
    val currency by AppSettingsPreferences.observeCurrency(context).collectAsStateWithLifecycle()
    val marginPercent by AppSettingsPreferences.observeMarginPercent(context).collectAsStateWithLifecycle()
    val isEdit = initialRecipe != null

    val baseCategories = listOf("Desayunos", "Almuerzos", "Comidas", "Cenas", "Pan", "Postres", "Snacks", "Bebidas", "Sopas")
    val initialCat = initialRecipe?.category?.trim().orEmpty()
    val initialIsCustomCat = initialCat.isNotBlank() && baseCategories.none { it.equals(initialCat, ignoreCase = true) }

    var title by remember { mutableStateOf(initialRecipe?.title ?: "") }
    var category by remember { mutableStateOf(if (initialIsCustomCat) "" else initialCat) }
    var isCustomCategory by remember { mutableStateOf(initialIsCustomCat) }
    var customCategoryText by remember { mutableStateOf(if (initialIsCustomCat) initialCat else "") }
    var servingsText by remember {
        mutableStateOf(if (initialRecipe != null && initialRecipe.servings > 0) initialRecipe.servings.toString() else "")
    }
    var prepTimeText by remember {
        mutableStateOf(if (initialRecipe != null && initialRecipe.prepTimeMinutes > 0) initialRecipe.prepTimeMinutes.toString() else "")
    }
    var cookTimeText by remember {
        mutableStateOf(if (initialRecipe != null && initialRecipe.cookTimeMinutes > 0) initialRecipe.cookTimeMinutes.toString() else "")
    }
    var difficulty by remember { mutableStateOf(initialRecipe?.difficulty ?: "") }
    var notes by remember { mutableStateOf(initialRecipe?.notes ?: "") }
    var imageUrl by remember { mutableStateOf(initialRecipe?.imageUrl ?: "") }
    var showUrlPhotoDialog by remember { mutableStateOf(false) }
    var selectedAspectOption by remember { mutableStateOf(PhotoAspectOption.RATIO_4_3) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(photoGenState) {
        if (photoGenState is RecipePhotoGenState.Success) {
            imageUrl = photoGenState.dataUri
            onResetPhotoGenState()
        }
    }

    var instructionsMode by remember { mutableIntStateOf(0) }
    var freeTextInstructions by remember {
        mutableStateOf(initialRecipe?.instructions?.joinToString("\n\n") ?: "")
    }

    var nextRowKey by remember { mutableLongStateOf(1L) }
    val ingredientRowKeys = remember {
        mutableStateListOf<Long>().apply {
            val count = initialRecipe?.ingredients?.size?.coerceAtLeast(1) ?: 1
            repeat(count) { add(nextRowKey++) }
        }
    }
    val ingredients = remember {
        mutableStateListOf<RecipeIngredient>().apply {
            if (initialRecipe != null && initialRecipe.ingredients.isNotEmpty()) {
                addAll(initialRecipe.ingredients)
            } else {
                add(RecipeIngredient(name = "", amount = 0.0, unit = "g", category = "Despensa"))
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
            onResetPhotoGenState()
            cameraError = null
            val savedPath = ImageUtils.saveImageLocally(context, uri)
            imageUrl = savedPath
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val captureUri = pendingCameraUri
        if (success && captureUri != null) {
            onResetPhotoGenState()
            cameraError = null
            val savedPath = ImageUtils.saveImageLocally(context, captureUri)
            imageUrl = savedPath
        }
    }

    fun launchRealCameraCapture() {
        try {
            val outUri = ImageUtils.createCameraTempImageUri(context)
            pendingCameraUri = outUri
            cameraError = null
            cameraLauncher.launch(outUri)
        } catch (_: Exception) {
            cameraError = context.getString(R.string.recipe_photo_camera_unavailable)
        }
    }

    val categories = baseCategories
    val difficulties = listOf("Fácil", "Media", "Difícil")
    val units = UnitConverter.allRecipeUnits

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
                                    val cleanedIngredients = ingredients
                                        .filter { it.name.isNotBlank() && it.amount > 0 }
                                        .map {
                                            it.copy(
                                                name = it.name.trim(),
                                                amount = SecurityUtils.clampNonNegative(it.amount),
                                                manualUnitCost = SecurityUtils.clampNonNegative(it.manualUnitCost)
                                            )
                                        }
                                    val finalInstructions = if (instructionsMode == 1) {
                                        freeTextInstructions.split("\n")
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                    } else {
                                        instructions.filter { it.isNotBlank() }
                                    }

                                    val resolvedCategory = if (isCustomCategory) {
                                        customCategoryText.trim().take(50)
                                    } else {
                                        category.trim().take(50)
                                    }
                                    val resolvedServings = (servingsText.trim().toIntOrNull() ?: 1).coerceIn(1, 500)
                                    val resolvedPrepTime = (prepTimeText.trim().toIntOrNull() ?: 0).coerceIn(0, 1440)
                                    val resolvedCookTime = (cookTimeText.trim().toIntOrNull() ?: 0).coerceIn(0, 1440)
                                    val newRecipe = (initialRecipe ?: Recipe()).copy(
                                        title = title.trim(),
                                        category = resolvedCategory,
                                        servings = resolvedServings,
                                        prepTimeMinutes = resolvedPrepTime,
                                        cookTimeMinutes = resolvedCookTime,
                                        difficulty = difficulty.trim().take(30),
                                        ingredients = cleanedIngredients,
                                        instructions = finalInstructions,
                                        notes = notes.trim(),
                                        imageUrl = SecurityUtils.sanitizeImageUri(imageUrl)
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
                                IconButton(
                                    onClick = {
                                        onResetPhotoGenState()
                                        imageUrl = ""
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("recipe_form_remove_photo")
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

                        Spacer(modifier = Modifier.height(10.dp))

                        when {
                            photoGenState is RecipePhotoGenState.Generating -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .sketchedBorder(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .testTag("recipe_form_photo_ai_loading"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.recipe_form_photo_ai_loading),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            imageUrl.isNotBlank() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .testTag("recipe_form_photo_preview"),
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
                            }

                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(124.dp)
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
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(38.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.recipe_form_photo_add),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = stringResource(R.string.recipe_form_photo_sub),
                                            fontFamily = PatrickHandFontFamily,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        if (cameraError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cameraError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (photoGenState is RecipePhotoGenState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recipe_form_photo_ai_error")
                            ) {
                                Text(
                                    text = photoGenState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Aspect ratio chips for realistic AI photo generation & enhancement
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.recipe_form_aspect_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(PhotoAspectOption.entries) { _, option ->
                                    FilterChip(
                                        selected = selectedAspectOption == option,
                                        onClick = { selectedAspectOption = option },
                                        enabled = photoGenState !is RecipePhotoGenState.Generating,
                                        label = { Text(stringResource(option.labelResId)) },
                                        modifier = Modifier.testTag("recipe_form_aspect_${option.name}")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { launchRealCameraCapture() },
                                enabled = photoGenState !is RecipePhotoGenState.Generating,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("recipe_form_camera_photo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.recipe_form_photo_camera_btn))
                            }

                            FilledTonalButton(
                                onClick = {
                                    val aspectCallback = onGenerateAiPhotoWithAspect
                                    if (aspectCallback != null) {
                                        aspectCallback(title, category, ingredients.toList(), selectedAspectOption)
                                    } else {
                                        onGenerateAiPhoto(title, category, ingredients.toList())
                                    }
                                },
                                enabled = photoGenState !is RecipePhotoGenState.Generating,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("recipe_form_ai_photo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.recipe_form_photo_ai_btn))
                            }

                            if (imageUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        onEnhanceRealPhoto(imageUrl, title, category, selectedAspectOption)
                                    },
                                    enabled = photoGenState !is RecipePhotoGenState.Generating,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("recipe_form_ai_enhance_photo")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.recipe_form_photo_enhance_btn))
                                }
                            }

                            OutlinedButton(
                                onClick = { showUrlPhotoDialog = true },
                                enabled = photoGenState !is RecipePhotoGenState.Generating,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("recipe_form_web_photo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.recipe_form_photo_web_btn))
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = photoGenState !is RecipePhotoGenState.Generating,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("recipe_form_gallery_photo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.recipe_form_photo_gallery_btn))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.recipe_form_photo_refs_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(ImageUtils.referentialPhotoPresets) { index, preset ->
                                val selected = imageUrl.trim() == preset.uriKey
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    },
                                    modifier = Modifier
                                        .width(88.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onResetPhotoGenState()
                                            imageUrl = preset.uriKey
                                        }
                                        .testTag("recipe_form_ref_$index")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = preset.drawableResId),
                                            contentDescription = stringResource(preset.labelResId),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(preset.labelResId),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
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
                            value = if (isCustomCategory) {
                                stringResource(R.string.cat_custom)
                            } else if (category.isNotBlank()) {
                                LocalizedLabels.recipeCategory(context, category)
                            } else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.recipe_form_cat_label)) },
                            placeholder = { Text(stringResource(R.string.recipe_form_cat_select_hint)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                                .testTag("recipe_form_category_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(LocalizedLabels.recipeCategory(context, cat)) },
                                    onClick = {
                                        isCustomCategory = false
                                        category = cat
                                        catExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cat_custom)) },
                                onClick = {
                                    isCustomCategory = true
                                    catExpanded = false
                                }
                            )
                        }
                    }

                    var diffExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = diffExpanded,
                        onExpandedChange = { diffExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = if (difficulty.isNotBlank()) LocalizedLabels.difficulty(context, difficulty) else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.recipe_form_diff_label)) },
                            placeholder = { Text(stringResource(R.string.recipe_form_diff_select_hint)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diffExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                                .testTag("recipe_form_difficulty_dropdown")
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

                if (isCustomCategory) {
                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = { customCategoryText = it },
                        label = { Text(stringResource(R.string.recipe_form_cat_custom_label)) },
                        placeholder = { Text(stringResource(R.string.recipe_form_cat_custom_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recipe_form_custom_category"),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = servingsText,
                        onValueChange = { raw -> servingsText = raw.filter { it.isDigit() } },
                        label = { Text(stringResource(R.string.recipe_form_servings_label)) },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_servings"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = prepTimeText,
                        onValueChange = { raw -> prepTimeText = raw.filter { it.isDigit() } },
                        label = { Text(stringResource(R.string.recipe_form_prep_label)) },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_prep_time"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cookTimeText,
                        onValueChange = { raw -> cookTimeText = raw.filter { it.isDigit() } },
                        label = { Text(stringResource(R.string.recipe_form_cook_label)) },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recipe_form_cook_time"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val headerServings = (servingsText.trim().toIntOrNull() ?: 1).coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recipe_form_ingredients_header, headerServings),
                        fontFamily = CaveatFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    PostItButton(
                        text = stringResource(R.string.recipe_form_add_ingredient),
                        icon = Icons.Default.Add,
                        onClick = {
                            ingredientRowKeys.add(nextRowKey++)
                            ingredients.add(RecipeIngredient(name = "", amount = 0.0, unit = "g", category = "Despensa"))
                        },
                        backgroundColor = PostItPeach,
                        fontSize = 17.sp,
                        rotationDegrees = -1f,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("recipe_form_add_ingredient")
                    )
                }

                ingredients.forEachIndexed { index, ingredient ->
                    val rowKey = ingredientRowKeys.getOrElse(index) { index.toLong() }
                    key(rowKey) {
                        var amountText by remember {
                            mutableStateOf(
                                if (ingredient.amount > 0.0) UnitConverter.formatQuantity(ingredient.amount) else ""
                            )
                        }
                        var manualCostText by remember {
                            mutableStateOf(
                                if (ingredient.manualUnitCost > 0.0) {
                                    UnitConverter.formatQuantity(ingredient.manualUnitCost, maxDecimals = 5)
                                } else ""
                            )
                        }
                        var showBatchHelper by remember { mutableStateOf(false) }
                        var batchPaidText by remember { mutableStateOf("") }
                        var batchQtyText by remember { mutableStateOf("") }
                        var batchUnit by remember {
                            mutableStateOf(
                                UnitConverter.compatibleUnitsFor(ingredient.unit).let { compat ->
                                    if ("kg" in compat) "kg" else if ("l" in compat) "l" else ingredient.unit
                                }
                            )
                        }
                        var batchUnitExpanded by remember { mutableStateOf(false) }

                        val matchedPantry = remember(pantryItems, ingredient.name) {
                            val clean = ingredient.name.trim()
                            if (clean.isEmpty()) null
                            else pantryItems.find { SampleContentLocalizer.sameItemName(it.name, clean) }
                        }
                        val pantryRate = remember(matchedPantry, ingredient.unit) {
                            UnitConverter.unitCostInTargetUnit(matchedPantry, ingredient.unit)
                        }
                        val effectiveRate = when {
                            pantryRate > 0.0 -> pantryRate
                            ingredient.manualUnitCost > 0.0 -> ingredient.manualUnitCost
                            else -> 0.0
                        }
                        val lineCost = ingredient.amount * effectiveRate

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
                                            onClick = {
                                                ingredients.removeAt(index)
                                                if (index < ingredientRowKeys.size) {
                                                    ingredientRowKeys.removeAt(index)
                                                }
                                            },
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
                                        value = amountText,
                                        onValueChange = { raw ->
                                            amountText = raw
                                            val amt = UnitConverter.parseNonNegativeDecimal(raw) ?: 0.0
                                            ingredients[index] = ingredient.copy(amount = amt)
                                        },
                                        label = { Text(stringResource(R.string.recipe_form_qty_label)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("recipe_form_ing_qty_$index"),
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
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                                .testTag("recipe_form_ing_unit_$index")
                                        )
                                        ExposedDropdownMenu(
                                            expanded = unitExpanded,
                                            onDismissRequest = { unitExpanded = false }
                                        ) {
                                            units.forEach { u ->
                                                DropdownMenuItem(
                                                    text = { Text(LocalizedLabels.unit(context, u)) },
                                                    onClick = {
                                                        val currentAmt = UnitConverter.parseNonNegativeDecimal(amountText) ?: ingredient.amount
                                                        val convertedAmt = UnitConverter.convert(currentAmt, ingredient.unit, u)
                                                        val convertedManualCost = UnitConverter.convertCostPerUnit(
                                                            ingredient.manualUnitCost,
                                                            ingredient.unit,
                                                            u
                                                        )
                                                        val nextAmt = convertedAmt ?: currentAmt
                                                        val nextManualCost = convertedManualCost ?: ingredient.manualUnitCost
                                                        if (convertedAmt != null && amountText.isNotBlank()) {
                                                            amountText = UnitConverter.formatQuantity(convertedAmt)
                                                        }
                                                        if (convertedManualCost != null && ingredient.manualUnitCost > 0.0) {
                                                            manualCostText = UnitConverter.formatQuantity(convertedManualCost, maxDecimals = 5)
                                                        }
                                                        val newCompat = UnitConverter.compatibleUnitsFor(u)
                                                        if (batchUnit !in newCompat) {
                                                            batchUnit = if ("kg" in newCompat) "kg" else if ("l" in newCompat) "l" else u
                                                        }
                                                        ingredients[index] = ingredient.copy(
                                                            unit = u,
                                                            amount = nextAmt,
                                                            manualUnitCost = nextManualCost
                                                        )
                                                        unitExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                UnitConverter.humanEquivalent(ingredient.amount, ingredient.unit)?.let { (eqVal, eqUnit) ->
                                    Text(
                                        text = stringResource(
                                            R.string.pantry_form_equiv_fmt,
                                            UnitConverter.formatQuantity(eqVal),
                                            LocalizedLabels.unit(context, eqUnit)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = manualCostText,
                                    onValueChange = { raw ->
                                        manualCostText = raw
                                        val cost = UnitConverter.parseNonNegativeDecimal(raw) ?: 0.0
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recipe_form_ing_cost_$index"),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showBatchHelper = !showBatchHelper },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.testTag("recipe_form_batch_helper_toggle_$index")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.recipe_form_batch_helper_toggle),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (lineCost > 0.0) {
                                        Text(
                                            text = stringResource(
                                                R.string.recipe_form_line_cost_preview,
                                                AppSettingsPreferences.formatMoney(lineCost, currency)
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StockGreen
                                        )
                                    }
                                }

                                if (showBatchHelper) {
                                    val compatUnits = UnitConverter.compatibleUnitsFor(ingredient.unit)
                                    fun recalcFromBatch(paidStr: String, qtyStr: String, bUnit: String) {
                                        val paid = UnitConverter.parseNonNegativeDecimal(paidStr) ?: 0.0
                                        val qty = UnitConverter.parseNonNegativeDecimal(qtyStr) ?: 0.0
                                        if (paid > 0.0 && qty > 0.0) {
                                            val breakdown = UnitConverter.computePantryCostBreakdown(
                                                enteredPrice = paid,
                                                enteredBaseQty = qty,
                                                priceInputUnit = bUnit,
                                                itemUnit = ingredient.unit,
                                                isPerSingleUnitMode = false
                                            )
                                            val unitRate = breakdown.unitCostInItemUnit
                                            manualCostText = UnitConverter.formatQuantity(unitRate, maxDecimals = 5)
                                            ingredients[index] = ingredient.copy(manualUnitCost = unitRate)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = batchPaidText,
                                            onValueChange = {
                                                batchPaidText = it
                                                recalcFromBatch(it, batchQtyText, batchUnit)
                                            },
                                            label = { Text(stringResource(R.string.recipe_form_batch_paid_label, currency.symbol)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("recipe_form_batch_paid_$index"),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = batchQtyText,
                                            onValueChange = {
                                                batchQtyText = it
                                                recalcFromBatch(batchPaidText, it, batchUnit)
                                            },
                                            label = { Text(stringResource(R.string.recipe_form_batch_qty_label)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier
                                                .weight(0.8f)
                                                .testTag("recipe_form_batch_qty_$index"),
                                            singleLine = true
                                        )
                                        ExposedDropdownMenuBox(
                                            expanded = batchUnitExpanded,
                                            onExpandedChange = { batchUnitExpanded = it },
                                            modifier = Modifier.weight(0.9f)
                                        ) {
                                            OutlinedTextField(
                                                value = LocalizedLabels.unit(context, batchUnit),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text(stringResource(R.string.recipe_form_batch_unit_label)) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchUnitExpanded) },
                                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                                                singleLine = true
                                            )
                                            ExposedDropdownMenu(
                                                expanded = batchUnitExpanded,
                                                onDismissRequest = { batchUnitExpanded = false }
                                            ) {
                                                compatUnits.forEach { cu ->
                                                    DropdownMenuItem(
                                                        text = { Text(LocalizedLabels.unit(context, cu)) },
                                                        onClick = {
                                                            batchUnit = cu
                                                            batchUnitExpanded = false
                                                            recalcFromBatch(batchPaidText, batchQtyText, cu)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val recipeTotalEstimatedCost = ingredients.sumOf { ing ->
                    val matched = pantryItems.find { SampleContentLocalizer.sameItemName(it.name, ing.name) }
                    val pantryRate = UnitConverter.unitCostInTargetUnit(matched, ing.unit)
                    val rate = if (pantryRate > 0.0) pantryRate else ing.manualUnitCost
                    ing.amount * rate
                }
                if (recipeTotalEstimatedCost > 0.0) {
                    val safeServings = (servingsText.trim().toIntOrNull() ?: 1).coerceAtLeast(1)
                    val perServing = recipeTotalEstimatedCost / safeServings
                    val suggestedPrice = AppSettingsPreferences.suggestedSellingPrice(recipeTotalEstimatedCost, marginPercent)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recipe_form_cost_summary")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.recipe_form_summary_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (marginPercent > 0) {
                                    stringResource(
                                        R.string.recipe_form_summary_line,
                                        safeServings,
                                        AppSettingsPreferences.formatMoney(recipeTotalEstimatedCost, currency),
                                        AppSettingsPreferences.formatMoney(perServing, currency),
                                        marginPercent,
                                        AppSettingsPreferences.formatMoney(suggestedPrice, currency)
                                    )
                                } else {
                                    stringResource(
                                        R.string.recipe_form_summary_cost_only,
                                        safeServings,
                                        AppSettingsPreferences.formatMoney(recipeTotalEstimatedCost, currency),
                                        AppSettingsPreferences.formatMoney(perServing, currency)
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StockGreen
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

    if (showUrlPhotoDialog) {
        var urlDraft by remember(showUrlPhotoDialog) {
            mutableStateOf(
                if (imageUrl.startsWith("http://", ignoreCase = true) ||
                    imageUrl.startsWith("https://", ignoreCase = true)
                ) imageUrl else ""
            )
        }
        var urlError by remember(showUrlPhotoDialog) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showUrlPhotoDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.recipe_form_url_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val trimmedTitle = title.trim()
                            if (trimmedTitle.isBlank()) {
                                urlError = context.getString(R.string.recipe_form_photo_need_title_web)
                            } else {
                                urlError = null
                                val searchUri = Uri.parse(
                                    "https://www.google.com/search?tbm=isch&q=${Uri.encode("$trimmedTitle receta")}"
                                )
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, searchUri))
                                } catch (_: ActivityNotFoundException) {
                                    // Browser not available on device
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recipe_form_url_web_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.recipe_form_url_search_btn))
                    }

                    OutlinedTextField(
                        value = urlDraft,
                        onValueChange = {
                            urlDraft = it
                            urlError = null
                        },
                        label = { Text(stringResource(R.string.recipe_form_url_hint)) },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recipe_form_url_input")
                    )

                    if (!urlError.isNullOrBlank()) {
                        Text(
                            text = urlError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalized = urlDraft.trim()
                        if (SecurityUtils.isSafeHttpsUrl(normalized)) {
                            onResetPhotoGenState()
                            imageUrl = normalized
                            showUrlPhotoDialog = false
                        } else {
                            urlError = context.getString(R.string.url_https_only_error)
                        }
                    },
                    modifier = Modifier.testTag("recipe_form_url_apply")
                ) {
                    Text(stringResource(R.string.recipe_form_url_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlPhotoDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}
