package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.VoiceLaunchRequest
import com.example.ai.VoiceCommandRouter
import com.example.ai.VoiceSpeechPlayer
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.settings.AppSettingsPreferences
import com.example.ui.MainViewModel
import com.example.ui.RecipeFeasibility
import com.example.ui.components.CookingTimerDialog
import com.example.ui.components.PostItButton
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockRed
import com.example.ui.theme.StockYellow
import com.example.util.ImageUtils
import com.example.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CookingAssistantScreen(
    viewModel: MainViewModel,
    recipes: List<Recipe>,
    pantryItems: List<PantryItem>,
    onSelectRecipe: (Recipe) -> Unit,
    voiceLaunchRequest: VoiceLaunchRequest = VoiceLaunchRequest(),
    onConsumeVoiceLaunch: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var resumePersistedTimer by remember { mutableStateOf(false) }

    val speechPlayer = remember { VoiceSpeechPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { speechPlayer.release() }
    }
    val isSpeaking by speechPlayer.isSpeaking.collectAsStateWithLifecycle()

    var manualQueryInput by remember { mutableStateOf("") }
    var isExecutingVoice by remember { mutableStateOf(false) }
    var voiceResult by remember { mutableStateOf<VoiceCommandRouter.VoiceExecutionResult?>(null) }
    var recognizerError by remember { mutableStateOf<String?>(null) }

    fun executeVoiceCommand(rawQuery: String, speakAloud: Boolean = true) {
        val cleaned = rawQuery.trim()
        if (cleaned.isBlank()) return
        manualQueryInput = cleaned
        recognizerError = null
        isExecutingVoice = true
        coroutineScope.launch {
            val result = VoiceCommandRouter.executeVoiceQuery(context, cleaned)
            voiceResult = result
            isExecutingVoice = false
            if (speakAloud && result.summaryText.isNotBlank()) {
                speechPlayer.speak(result.summaryText)
            }
        }
    }

    val noRecognizerMsg = stringResource(R.string.voice_no_recognizer_msg)
    val speechPrompt = stringResource(R.string.voice_prompt_speech)

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spokenList = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val bestMatch = spokenList?.firstOrNull()?.trim().orEmpty()
            if (bestMatch.isNotBlank()) {
                executeVoiceCommand(bestMatch, speakAloud = true)
            }
        }
    }

    fun launchSpeechRecognizer() {
        recognizerError = null
        val langTag = when (AppSettingsPreferences.getLanguageCode(context)) {
            "es" -> "es-ES"
            "en" -> "en-US"
            "pt" -> "pt-BR"
            "fr" -> "fr-FR"
            else -> java.util.Locale.getDefault().toLanguageTag()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            recognizerError = noRecognizerMsg
        } catch (_: Exception) {
            recognizerError = noRecognizerMsg
        }
    }

    LaunchedEffect(voiceLaunchRequest.nonce) {
        val pendingQuery = voiceLaunchRequest.query
        when {
            !pendingQuery.isNullOrBlank() -> {
                executeVoiceCommand(pendingQuery, speakAloud = true)
                onConsumeVoiceLaunch()
            }
            voiceLaunchRequest.autoListen -> {
                launchSpeechRecognizer()
                onConsumeVoiceLaunch()
            }
        }
    }

    val persistedTimer by AppSettingsPreferences.observeTimerState(context).collectAsStateWithLifecycle()
    var bannerRemainingSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(persistedTimer) {
        if (persistedTimer.isRunning && persistedTimer.endTimeMillis > 0L) {
            while (true) {
                val rem = ((persistedTimer.endTimeMillis - System.currentTimeMillis() + 999L) / 1000L)
                    .toInt()
                    .coerceAtLeast(0)
                bannerRemainingSec = rem
                if (rem <= 0) break
                delay(1000L)
            }
        } else {
            bannerRemainingSec = persistedTimer.pausedRemainingSeconds.coerceAtLeast(0)
        }
    }

    val feasibilities = remember(recipes, pantryItems) {
        viewModel.calculateRecipeFeasibilities(recipes, pantryItems)
    }

    val readyToCook = feasibilities.filter { it.matchPercentage == 100 }
    val almostReady = feasibilities.filter { it.matchPercentage in 50..99 }
    val needSupplies = feasibilities.filter { it.matchPercentage < 50 }

    val exposedTools = listOf(
        stringResource(R.string.cook_voice_tool1_title) to stringResource(R.string.cook_voice_tool1_sample),
        stringResource(R.string.cook_voice_tool2_title) to stringResource(R.string.cook_voice_tool2_sample),
        stringResource(R.string.cook_voice_tool3_title) to stringResource(R.string.cook_voice_tool3_sample),
        stringResource(R.string.cook_voice_tool4_title) to stringResource(R.string.cook_voice_tool4_sample),
        stringResource(R.string.cook_voice_tool5_title) to stringResource(R.string.cook_voice_tool5_sample)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .notebookPaperBackground(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .sketchedBorder(color = MaterialTheme.colorScheme.onSurface, strokeWidthDp = 2f)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.cook_header),
                    fontFamily = CaveatFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (persistedTimer.durationSeconds > 0 && bannerRemainingSec > 0) {
            item {
                Surface(
                    color = PostItYellow.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .sketchedBorder(color = MaterialTheme.colorScheme.primary, strokeWidthDp = 1.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlarmOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val mins = bannerRemainingSec / 60
                            val secs = bannerRemainingSec % 60
                            val lbl = persistedTimer.label.ifBlank { stringResource(R.string.timer_default_title) }
                            Text(
                                text = stringResource(R.string.cook_timer_running_banner, lbl, mins, secs),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = {
                                resumePersistedTimer = true
                                activeTimerMinutes = (persistedTimer.durationSeconds / 60).coerceAtLeast(1)
                            }
                        ) {
                            Text(stringResource(R.string.cook_timer_open), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Universal Background Voice & AI Agent Provider Card (NO in-app AI, NO Widget)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PostItSky.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .sketchedBorder(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), strokeWidthDp = 1.8f)
                    .testTag("voice_provider_info_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = stringResource(R.string.cook_voice_title),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.cook_voice_title),
                            fontFamily = CaveatFontFamily,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val statusLabel = if (viewModel.isGuestMode) {
                        stringResource(R.string.cook_voice_source_guest, pantryItems.size, recipes.size)
                    } else {
                        val accountLabel = viewModel.currentUserEmail
                            .ifBlank { viewModel.currentUserName }
                            .ifBlank { stringResource(R.string.account_cloud_active) }
                        stringResource(R.string.cook_voice_source_cloud, accountLabel, pantryItems.size, recipes.size)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.cook_voice_desc),
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { launchSpeechRecognizer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_ask_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.voice_ask_btn),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.voice_ask_btn),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualQueryInput,
                        onValueChange = { manualQueryInput = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.voice_manual_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { executeVoiceCommand(manualQueryInput, speakAloud = true) }
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = { executeVoiceCommand(manualQueryInput, speakAloud = true) },
                                enabled = manualQueryInput.isNotBlank() && !isExecutingVoice,
                                modifier = Modifier.testTag("voice_manual_query_send")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.voice_manual_send),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_manual_query_input")
                    )

                    if (recognizerError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = recognizerError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("voice_recognizer_error")
                        )
                    }

                    if (isExecutingVoice) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.voice_loading_answer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    val activeResult = voiceResult
                    if (activeResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = PostItMint.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(
                                    color = if (activeResult.isSuccess) StockGreen else MaterialTheme.colorScheme.error,
                                    strokeWidthDp = 1.5f
                                )
                                .testTag("voice_answer_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.voice_answer_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    IconButton(
                                        onClick = {
                                            speechPlayer.stop()
                                            voiceResult = null
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.action_close),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.voice_answer_query_prefix, activeResult.query),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = activeResult.summaryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            if (isSpeaking) speechPlayer.stop() else speechPlayer.speak(activeResult.summaryText)
                                        },
                                        modifier = Modifier.testTag("voice_replay_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSpeaking) {
                                                stringResource(R.string.voice_stop_btn)
                                            } else {
                                                stringResource(R.string.voice_replay_btn)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    exposedTools.forEachIndexed { idx, (toolLabel, sampleVoice) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                val cleanedSample = sampleVoice.trim().trim('"', '“', '”', '«', '»')
                                executeVoiceCommand(cleanedSample, speakAloud = true)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .testTag("voice_sample_${idx + 1}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = toolLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = stringResource(R.string.cook_voice_try, sampleVoice),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.cook_voice_compat),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Timer Section with Post-It buttons
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.sketchedBorder(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.cook_timer_section),
                            fontFamily = CaveatFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timerColors = listOf(
                            PostItYellow,
                            PostItPeach,
                            PostItMint,
                            PostItSky,
                            PostItPink,
                            PostItYellow
                        )
                        listOf(3, 5, 10, 15, 20, 30).forEachIndexed { idx, mins ->
                            PostItButton(
                                text = "${mins}m",
                                onClick = {
                                    resumePersistedTimer = false
                                    activeTimerMinutes = mins
                                },
                                backgroundColor = timerColors[idx % timerColors.size],
                                fontSize = 17.sp,
                                rotationDegrees = if (idx % 2 == 0) -1f else 1f,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Category 1: 100% Ready to cook
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StockGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.cook_ready_title, readyToCook.size),
                    fontFamily = CaveatFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (readyToCook.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.cook_ready_empty),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(readyToCook) { item ->
                FeasibilityRecipeCard(
                    feasibility = item,
                    onClick = { onSelectRecipe(item.recipe) },
                    onAddMissingToShopping = {}
                )
            }
        }

        // Category 2: Almost ready (50% - 99%)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = StockYellow, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.cook_almost_title, almostReady.size),
                    fontFamily = CaveatFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (almostReady.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.cook_almost_empty),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(almostReady) { item ->
                FeasibilityRecipeCard(
                    feasibility = item,
                    onClick = { onSelectRecipe(item.recipe) },
                    onAddMissingToShopping = {
                        viewModel.addMissingRecipeIngredientsToShoppingList(
                            item.recipe,
                            item.recipe.servings,
                            pantryItems
                        )
                    }
                )
            }
        }

        // Category 3: Need more supplies (< 50%)
        if (needSupplies.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.cook_need_title, needSupplies.size),
                    fontFamily = CaveatFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(needSupplies) { item ->
                FeasibilityRecipeCard(
                    feasibility = item,
                    onClick = { onSelectRecipe(item.recipe) },
                    onAddMissingToShopping = {
                        viewModel.addMissingRecipeIngredientsToShoppingList(
                            item.recipe,
                            item.recipe.servings,
                            pantryItems
                        )
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (activeTimerMinutes != null) {
        val defaultTitle = if (resumePersistedTimer && persistedTimer.label.isNotBlank()) {
            persistedTimer.label
        } else {
            stringResource(R.string.timer_title_mins, activeTimerMinutes ?: 5)
        }
        CookingTimerDialog(
            initialMinutes = activeTimerMinutes ?: 5,
            timerTitle = defaultTitle,
            resumeExisting = resumePersistedTimer,
            onDismiss = { activeTimerMinutes = null }
        )
    }
}

@Composable
private fun FeasibilityRecipeCard(
    feasibility: RecipeFeasibility,
    onClick: () -> Unit,
    onAddMissingToShopping: () -> Unit
) {
    val recipe = feasibility.recipe
    val pct = feasibility.matchPercentage
    val barColor = when {
        pct >= 100 -> StockGreen
        pct >= 50 -> StockYellow
        else -> StockRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("assistant_recipe_${recipe.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cardPhotoUri = recipe.imageUrl.ifBlank {
                    ImageUtils.defaultUriForCategory(recipe.category)
                }
                AsyncImage(
                    model = ImageUtils.toImageModel(cardPhotoUri),
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )

                Text(
                    text = recipe.title,
                    fontFamily = CaveatFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = barColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(R.string.cook_pct_badge, pct),
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = pct / 100f,
                animationSpec = tween(700),
                label = "feasibilityProgress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.cook_card_sub,
                        feasibility.availableCount,
                        feasibility.totalCount,
                        recipe.totalTimeMinutes
                    ),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (feasibility.missingIngredients.isNotEmpty()) {
                    PostItButton(
                        text = stringResource(R.string.cook_buy_btn, feasibility.missingIngredients.size),
                        icon = Icons.Default.ShoppingCart,
                        onClick = onAddMissingToShopping,
                        backgroundColor = PostItMint,
                        fontSize = 16.sp,
                        rotationDegrees = -1f,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("buy_missing_${recipe.id}")
                    )
                }
            }

            if (feasibility.missingIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.cook_missing_line,
                        feasibility.missingIngredients.joinToString(", ") { it.first }
                    ),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
