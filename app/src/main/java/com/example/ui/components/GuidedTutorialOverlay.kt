package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.ui.theme.BlueprintNavy
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.NotebookPaperBackground
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen
import com.example.util.UnitConverter

data class TutorialFeatureBullet(
    @StringRes val titleResId: Int,
    @StringRes val descResId: Int
)

data class TutorialStepSpec(
    val index: Int,
    val targetTab: Int,
    val spotlightTargetId: String,
    val defaultCardAtTop: Boolean,
    val icon: ImageVector,
    val accentColor: Color,
    @StringRes val titleResId: Int,
    @StringRes val subtitleResId: Int,
    val bullets: List<TutorialFeatureBullet>
)

object GuidedTutorialCatalog {
    val steps: List<TutorialStepSpec> = listOf(
        TutorialStepSpec(
            index = 0,
            targetTab = 0,
            spotlightTargetId = "topbar_actions",
            defaultCardAtTop = false,
            icon = Icons.Default.Tune,
            accentColor = PostItYellow,
            titleResId = R.string.tutorial_step1_title,
            subtitleResId = R.string.tutorial_step1_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step1_b1_title, R.string.tutorial_step1_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step1_b2_title, R.string.tutorial_step1_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step1_b3_title, R.string.tutorial_step1_b3_desc),
                TutorialFeatureBullet(R.string.tutorial_step1_b4_title, R.string.tutorial_step1_b4_desc)
            )
        ),
        TutorialStepSpec(
            index = 1,
            targetTab = 0,
            spotlightTargetId = "add_recipe_fab",
            defaultCardAtTop = true,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = PostItPeach,
            titleResId = R.string.tutorial_step2_title,
            subtitleResId = R.string.tutorial_step2_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step2_b1_title, R.string.tutorial_step2_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step2_b2_title, R.string.tutorial_step2_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step2_b3_title, R.string.tutorial_step2_b3_desc)
            )
        ),
        TutorialStepSpec(
            index = 2,
            targetTab = 0,
            spotlightTargetId = "tab_recipes",
            defaultCardAtTop = true,
            icon = Icons.Default.RestaurantMenu,
            accentColor = PostItSky,
            titleResId = R.string.tutorial_step3_title,
            subtitleResId = R.string.tutorial_step3_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step3_b1_title, R.string.tutorial_step3_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step3_b2_title, R.string.tutorial_step3_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step3_b3_title, R.string.tutorial_step3_b3_desc)
            )
        ),
        TutorialStepSpec(
            index = 3,
            targetTab = 1,
            spotlightTargetId = "tab_pantry",
            defaultCardAtTop = true,
            icon = Icons.Default.Inventory2,
            accentColor = PostItMint,
            titleResId = R.string.tutorial_step4_title,
            subtitleResId = R.string.tutorial_step4_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step4_b1_title, R.string.tutorial_step4_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step4_b2_title, R.string.tutorial_step4_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step4_b3_title, R.string.tutorial_step4_b3_desc)
            )
        ),
        TutorialStepSpec(
            index = 4,
            targetTab = 2,
            spotlightTargetId = "tab_shopping",
            defaultCardAtTop = true,
            icon = Icons.Default.ShoppingCart,
            accentColor = PostItYellow,
            titleResId = R.string.tutorial_step5_title,
            subtitleResId = R.string.tutorial_step5_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step5_b1_title, R.string.tutorial_step5_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step5_b2_title, R.string.tutorial_step5_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step5_b3_title, R.string.tutorial_step5_b3_desc)
            )
        ),
        TutorialStepSpec(
            index = 5,
            targetTab = 3,
            spotlightTargetId = "tab_production",
            defaultCardAtTop = true,
            icon = Icons.Default.Calculate,
            accentColor = PostItPeach,
            titleResId = R.string.tutorial_step6_title,
            subtitleResId = R.string.tutorial_step6_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step6_b1_title, R.string.tutorial_step6_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step6_b2_title, R.string.tutorial_step6_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step6_b3_title, R.string.tutorial_step6_b3_desc)
            )
        ),
        TutorialStepSpec(
            index = 6,
            targetTab = 4,
            spotlightTargetId = "voice_ask_mic_button",
            defaultCardAtTop = true,
            icon = Icons.Default.RecordVoiceOver,
            accentColor = PostItSky,
            titleResId = R.string.tutorial_step7_title,
            subtitleResId = R.string.tutorial_step7_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step7_b1_title, R.string.tutorial_step7_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step7_b2_title, R.string.tutorial_step7_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step7_b3_title, R.string.tutorial_step7_b3_desc)
            )
        ),
        TutorialStepSpec(
            index = 7,
            targetTab = 4,
            spotlightTargetId = "action_settings",
            defaultCardAtTop = false,
            icon = Icons.Default.CloudSync,
            accentColor = PostItMint,
            titleResId = R.string.tutorial_step8_title,
            subtitleResId = R.string.tutorial_step8_sub,
            bullets = listOf(
                TutorialFeatureBullet(R.string.tutorial_step8_b1_title, R.string.tutorial_step8_b1_desc),
                TutorialFeatureBullet(R.string.tutorial_step8_b2_title, R.string.tutorial_step8_b2_desc),
                TutorialFeatureBullet(R.string.tutorial_step8_b3_title, R.string.tutorial_step8_b3_desc)
            )
        )
    )
}

/**
 * Spotlight coach-mark tutorial:
 * Softly dims & frames everything except the highlighted real button/tab in focus,
 * while floating a compact interactive Post-It card on the opposite half of the screen.
 * Allows skipping immediately when [canSkipTutorial] is true (e.g., previously logged-in user).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuidedTutorialOverlay(
    onSwitchTab: (Int) -> Unit,
    onCompleted: () -> Unit,
    isGuestMode: Boolean = true,
    currentUserEmail: String = "",
    canSkipTutorial: Boolean = true,
    onSkip: () -> Unit = onCompleted,
    tutorialTargets: Map<String, Rect> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val steps = GuidedTutorialCatalog.steps
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    val trialDone = remember {
        mutableStateListOf(false, false, false, false, false, false, false, false)
    }

    // Per-step interactive sandbox states (ephemeral practice data)
    var previewAsGuest by remember(isGuestMode) { mutableStateOf(isGuestMode) }

    var sampleRecipeTitle by remember { mutableStateOf("") }
    var sampleRecipeCategory by remember { mutableStateOf("Almuerzos") }
    var samplePhotoRatio by remember { mutableStateOf("4:3") }

    var sampleServings by remember { mutableIntStateOf(2) }

    var samplePantryKgText by remember { mutableStateOf("") }
    var samplePantryCostText by remember { mutableStateOf("") }

    var sampleShoppingItem by remember { mutableStateOf("Leche (2 L)") }
    var sampleShoppingBought by remember { mutableStateOf(false) }

    var sampleProdMode by remember { mutableStateOf("Porciones") }
    var sampleMarginText by remember { mutableStateOf("30") }

    var sampleVoiceQuery by remember { mutableStateOf("") }

    var sampleTimerMins by remember { mutableIntStateOf(0) }
    var sampleSettingsDriveTested by remember { mutableStateOf(false) }

    val step = steps[currentStepIndex]
    val totalSteps = steps.size
    val isLastStep = currentStepIndex == totalSteps - 1
    val currentTrialCompleted = trialDone[currentStepIndex]

    val activeTargetBounds = tutorialTargets[step.spotlightTargetId]

    val pulseTransition = rememberInfiniteTransition(label = "spotlightPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightPulseAlpha"
    )

    LaunchedEffect(currentStepIndex) {
        onSwitchTab(step.targetTab)
        scrollState.scrollTo(0)
    }

    BackHandler(enabled = true) {
        if (currentStepIndex > 0) {
            currentStepIndex -= 1
        }
    }

    val scrimInteraction = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("guided_tutorial_overlay")
    ) {
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val placeCardAtTop = when {
            activeTargetBounds != null && activeTargetBounds.height > 0f -> {
                activeTargetBounds.center.y >= maxHeightPx * 0.44f
            }
            else -> step.defaultCardAtTop
        }
        val bodyScrollCap = (maxHeight * 0.44f).coerceIn(170.dp, 310.dp)

        // Spotlight cutout scrim: smoothly dims the rest while keeping the highlighted button sharp
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = {}
                )
                .testTag("tutorial_spotlight_canvas")
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.66f))

            if (activeTargetBounds != null && activeTargetBounds.width > 0f && activeTargetBounds.height > 0f) {
                val padPx = 8.dp.toPx()
                val haloPadPx = 16.dp.toPx()
                val left = (activeTargetBounds.left - padPx).coerceAtLeast(0f)
                val top = (activeTargetBounds.top - padPx).coerceAtLeast(0f)
                val right = (activeTargetBounds.right + padPx).coerceAtMost(size.width)
                val bottom = (activeTargetBounds.bottom + padPx).coerceAtMost(size.height)
                val rectW = (right - left).coerceAtLeast(4f)
                val rectH = (bottom - top).coerceAtLeast(4f)
                val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())

                // Soft outer diffused ring around the spotlight window
                drawRoundRect(
                    color = step.accentColor.copy(alpha = 0.28f * pulseAlpha),
                    topLeft = Offset((left - (haloPadPx - padPx)).coerceAtLeast(0f), (top - (haloPadPx - padPx)).coerceAtLeast(0f)),
                    size = Size(
                        width = (rectW + (haloPadPx - padPx) * 2f).coerceAtMost(size.width),
                        height = (rectH + (haloPadPx - padPx) * 2f).coerceAtMost(size.height)
                    ),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )

                // Crystal-clear punched hole revealing the real UI element untouched
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(rectW, rectH),
                    cornerRadius = corner,
                    blendMode = BlendMode.Clear
                )

                // Sketchy dashed marker highlight border around the illuminated target
                drawRoundRect(
                    color = step.accentColor.copy(alpha = pulseAlpha),
                    topLeft = Offset(left, top),
                    size = Size(rectW, rectH),
                    cornerRadius = corner,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                    )
                )
            }
        }

        // Compact Floating Post-It Coach-Mark positioned on the opposite side of the spotlighted target
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = if (placeCardAtTop) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NotebookPaperBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .sketchedBorder(color = CharcoalInk, strokeWidthDp = 2.2f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact Header Row: Spotlight badge (shrinks gracefully) + Always-visible Skip + Step X / 8
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = step.accentColor.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = CharcoalInk,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stringResource(
                                        if (canSkipTutorial) R.string.tutorial_header_badge_optional
                                        else R.string.tutorial_header_badge
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (canSkipTutorial) {
                                val skipDesc = stringResource(R.string.tutorial_skip_returning_btn)
                                TextButton(
                                    onClick = onSkip,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .semantics { contentDescription = skipDesc }
                                        .testTag("tutorial_skip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = BlueprintNavy,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(R.string.tutorial_skip_btn),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueprintNavy,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.tutorial_progress_fmt, currentStepIndex + 1, totalSteps),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BlueprintNavy,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.testTag("tutorial_progress_label")
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (currentStepIndex + 1).toFloat() / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BlueprintNavy,
                        trackColor = CharcoalInk.copy(alpha = 0.14f),
                        strokeCap = StrokeCap.Round
                    )

                    // Compact Scrollable Coach Body (User Scope + Spotlighted Guide + Hands-On Trial)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = bodyScrollCap)
                            .verticalScroll(scrollState)
                            .testTag("tutorial_step_scroll_container"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Active User Permissions Scope Banner
                        Surface(
                            color = if (isGuestMode) PostItYellow.copy(alpha = 0.5f) else PostItMint.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tutorial_user_scope_banner")
                        ) {
                            Text(
                                text = if (isGuestMode) {
                                    stringResource(R.string.tutorial_mode_guest_banner)
                                } else {
                                    stringResource(
                                        R.string.tutorial_mode_cloud_banner,
                                        currentUserEmail.ifBlank { stringResource(R.string.account_cloud_active) }
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        // Step Title + Spotlight Hint
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = step.accentColor,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = step.icon,
                                        contentDescription = stringResource(step.titleResId),
                                        tint = BlueprintNavy,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(step.titleResId),
                                    fontFamily = CaveatFontFamily,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueprintNavy,
                                    modifier = Modifier.testTag("tutorial_step_title")
                                )
                                Text(
                                    text = stringResource(step.subtitleResId),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 13.sp,
                                    color = CharcoalInk.copy(alpha = 0.85f)
                                )
                            }
                        }

                        if (activeTargetBounds != null && activeTargetBounds.width > 0f) {
                            Text(
                                text = "🔦 ${stringResource(R.string.tutorial_spotlight_hint)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StockGreen
                            )
                        }

                        // Compact Button Explanations
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = CharcoalInk.copy(alpha = 0.25f), strokeWidthDp = 1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                step.bullets.forEach { bullet ->
                                    Text(
                                        text = "${stringResource(bullet.titleResId)}: ${stringResource(bullet.descResId)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        lineHeight = 14.sp,
                                        color = CharcoalInk
                                    )
                                }
                            }
                        }

                        // Compact Hands-On Interactive Sandbox Card for Current Step
                        PostItCard(
                            backgroundColor = step.accentColor.copy(alpha = 0.62f),
                            rotationDegrees = 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tutorial_sandbox_card")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.tutorial_try_header),
                                        fontFamily = CaveatFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueprintNavy,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AssistChip(
                                        onClick = {
                                            when (currentStepIndex) {
                                                0 -> {
                                                    previewAsGuest = !previewAsGuest
                                                    trialDone[0] = true
                                                }
                                                1 -> {
                                                    if (sampleRecipeTitle.isBlank()) {
                                                        sampleRecipeTitle = context.getString(R.string.tutorial_s2_recipe_hint)
                                                            .removePrefix("Ej. ")
                                                            .removePrefix("e.g. ")
                                                            .removePrefix("Ex. ")
                                                    }
                                                    trialDone[1] = true
                                                }
                                                2 -> {
                                                    sampleServings = 4
                                                    trialDone[2] = true
                                                }
                                                3 -> {
                                                    samplePantryKgText = "1.5"
                                                    samplePantryCostText = "6.00"
                                                    trialDone[3] = true
                                                }
                                                4 -> {
                                                    if (sampleShoppingItem.isBlank()) {
                                                        sampleShoppingItem = "Leche (2 L)"
                                                    }
                                                    sampleShoppingBought = true
                                                    trialDone[4] = true
                                                }
                                                5 -> {
                                                    sampleMarginText = "40"
                                                    trialDone[5] = true
                                                }
                                                6 -> {
                                                    sampleVoiceQuery = context.getString(R.string.tutorial_s7_q1)
                                                    trialDone[6] = true
                                                }
                                                7 -> {
                                                    sampleTimerMins = 5
                                                    sampleSettingsDriveTested = true
                                                    trialDone[7] = true
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(R.string.tutorial_try_sample_btn),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        modifier = Modifier.testTag("tutorial_try_sample_button")
                                    )
                                }

                                when (currentStepIndex) {
                                    0 -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s1_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = previewAsGuest,
                                                onClick = {
                                                    previewAsGuest = true
                                                    trialDone[0] = true
                                                },
                                                label = { Text(stringResource(R.string.tutorial_s1_guest_chip)) },
                                                modifier = Modifier.testTag("tutorial_s1_guest_chip")
                                            )
                                            FilterChip(
                                                selected = !previewAsGuest,
                                                onClick = {
                                                    previewAsGuest = false
                                                    trialDone[0] = true
                                                },
                                                label = { Text(stringResource(R.string.tutorial_s1_google_chip)) },
                                                modifier = Modifier.testTag("tutorial_s1_google_chip")
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    if (previewAsGuest) R.string.tutorial_s1_guest_scope else R.string.tutorial_s1_google_scope
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = BlueprintNavy,
                                                modifier = Modifier.padding(7.dp)
                                            )
                                        }
                                    }

                                    1 -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s2_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        OutlinedTextField(
                                            value = sampleRecipeTitle,
                                            onValueChange = {
                                                sampleRecipeTitle = it
                                                if (it.isNotBlank()) trialDone[1] = true
                                            },
                                            label = { Text(stringResource(R.string.tutorial_s2_recipe_label)) },
                                            placeholder = { Text(stringResource(R.string.tutorial_s2_recipe_hint)) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("tutorial_s2_recipe_input")
                                        )
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("Desayunos", "Almuerzos", "Postres").forEach { cat ->
                                                FilterChip(
                                                    selected = sampleRecipeCategory == cat,
                                                    onClick = {
                                                        sampleRecipeCategory = cat
                                                        if (sampleRecipeTitle.isBlank()) {
                                                            sampleRecipeTitle = "Plato $cat"
                                                        }
                                                        trialDone[1] = true
                                                    },
                                                    label = { Text(cat) }
                                                )
                                            }
                                            listOf("4:3", "16:9").forEach { ratio ->
                                                FilterChip(
                                                    selected = samplePhotoRatio == ratio,
                                                    onClick = { samplePhotoRatio = ratio },
                                                    label = { Text(ratio) }
                                                )
                                            }
                                        }
                                        if (sampleRecipeTitle.isNotBlank()) {
                                            Text(
                                                text = stringResource(
                                                    R.string.tutorial_s2_preview,
                                                    sampleRecipeTitle.trim(),
                                                    sampleRecipeCategory,
                                                    samplePhotoRatio
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StockGreen
                                            )
                                        }
                                    }

                                    2 -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s3_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    sampleServings = (sampleServings - 1).coerceAtLeast(1)
                                                    trialDone[2] = true
                                                },
                                                modifier = Modifier.testTag("tutorial_s3_minus")
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.action_less))
                                            }
                                            Text(
                                                text = stringResource(R.string.prod_servings_count, sampleServings),
                                                fontFamily = CaveatFontFamily,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BlueprintNavy
                                            )
                                            FilledTonalIconButton(
                                                onClick = {
                                                    sampleServings = (sampleServings + 1).coerceAtMost(20)
                                                    trialDone[2] = true
                                                },
                                                modifier = Modifier.testTag("tutorial_s3_plus")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_more))
                                            }
                                        }
                                        val pastaGrams = sampleServings * 100
                                        val cheeseGrams = sampleServings * 30
                                        Text(
                                            text = stringResource(R.string.tutorial_s3_scaled_fmt, sampleServings, pastaGrams, cheeseGrams),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BlueprintNavy
                                        )
                                    }

                                    3 -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s4_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = samplePantryKgText,
                                                onValueChange = {
                                                    samplePantryKgText = it
                                                    val q = UnitConverter.parseNonNegativeDecimal(it) ?: 0.0
                                                    val p = UnitConverter.parseNonNegativeDecimal(samplePantryCostText) ?: 0.0
                                                    if (q > 0.0 && p > 0.0) trialDone[3] = true
                                                },
                                                label = { Text(stringResource(R.string.tutorial_s4_qty_label)) },
                                                placeholder = { Text("1.5") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tutorial_s4_qty_input")
                                            )
                                            OutlinedTextField(
                                                value = samplePantryCostText,
                                                onValueChange = {
                                                    samplePantryCostText = it
                                                    val q = UnitConverter.parseNonNegativeDecimal(samplePantryKgText) ?: 0.0
                                                    val p = UnitConverter.parseNonNegativeDecimal(it) ?: 0.0
                                                    if (q > 0.0 && p > 0.0) trialDone[3] = true
                                                },
                                                label = { Text(stringResource(R.string.tutorial_s4_price_label)) },
                                                placeholder = { Text("6.00") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tutorial_s4_price_input")
                                            )
                                        }
                                        val kgVal = UnitConverter.parseNonNegativeDecimal(samplePantryKgText) ?: 0.0
                                        val costVal = UnitConverter.parseNonNegativeDecimal(samplePantryCostText) ?: 0.0
                                        if (kgVal > 0.0 && costVal > 0.0) {
                                            val gramsVal = UnitConverter.convert(kgVal, "kg", "g") ?: (kgVal * 1000.0)
                                            val perKg = costVal / kgVal
                                            val perGram = costVal / gramsVal
                                            Text(
                                                text = stringResource(
                                                    R.string.tutorial_s4_result_fmt,
                                                    UnitConverter.formatQuantity(gramsVal),
                                                    AppSettingsPreferences.formatMoney(perKg, CurrencyOption.USD),
                                                    AppSettingsPreferences.formatMoney(perGram, CurrencyOption.USD, forceFourDecimalsIfTiny = true)
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StockGreen
                                            )
                                        }
                                    }

                                    4 -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s5_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        OutlinedTextField(
                                            value = sampleShoppingItem,
                                            onValueChange = { sampleShoppingItem = it },
                                            label = { Text(stringResource(R.string.tutorial_s5_item_label)) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("tutorial_s5_item_input")
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                if (sampleShoppingItem.isBlank()) sampleShoppingItem = "Leche (2 L)"
                                                sampleShoppingBought = true
                                                trialDone[4] = true
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("tutorial_s5_mark_button")
                                        ) {
                                            Text(stringResource(R.string.tutorial_s5_mark_btn))
                                        }
                                        if (sampleShoppingBought) {
                                            Text(
                                                text = stringResource(R.string.tutorial_s5_result_fmt, sampleShoppingItem.trim()),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StockGreen
                                            )
                                        }
                                    }

                                    5 -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s6_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                stringResource(R.string.prod_mode_servings),
                                                stringResource(R.string.prod_mode_ingredient),
                                                stringResource(R.string.prod_mode_batches)
                                            ).forEach { modeLabel ->
                                                FilterChip(
                                                    selected = sampleProdMode == modeLabel,
                                                    onClick = {
                                                        sampleProdMode = modeLabel
                                                        trialDone[5] = true
                                                    },
                                                    label = { Text(modeLabel) }
                                                )
                                            }
                                        }
                                        OutlinedTextField(
                                            value = sampleMarginText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }
                                                sampleMarginText = digits
                                                if ((digits.toIntOrNull() ?: 0) >= 0 && digits.isNotEmpty()) {
                                                    trialDone[5] = true
                                                }
                                            },
                                            label = { Text(stringResource(R.string.tutorial_s6_margin_label)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth(0.6f)
                                                .testTag("tutorial_s6_margin_input")
                                        )
                                        val marginPct = (sampleMarginText.toIntOrNull() ?: 30).coerceIn(0, 500)
                                        val sale = AppSettingsPreferences.suggestedSellingPrice(10.0, marginPct)
                                        val profit = AppSettingsPreferences.estimatedProfit(10.0, marginPct)
                                        Text(
                                            text = stringResource(
                                                R.string.tutorial_s6_result_fmt,
                                                sampleProdMode,
                                                AppSettingsPreferences.formatMoney(sale, CurrencyOption.USD),
                                                AppSettingsPreferences.formatMoney(profit, CurrencyOption.USD)
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BlueprintNavy
                                        )
                                    }

                                    6 -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s7_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        val q1 = stringResource(R.string.tutorial_s7_q1)
                                        val q2 = stringResource(R.string.tutorial_s7_q2)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = sampleVoiceQuery == q1,
                                                onClick = {
                                                    sampleVoiceQuery = q1
                                                    trialDone[6] = true
                                                },
                                                label = { Text(q1) },
                                                modifier = Modifier.testTag("tutorial_s7_q1_chip")
                                            )
                                            FilterChip(
                                                selected = sampleVoiceQuery == q2,
                                                onClick = {
                                                    sampleVoiceQuery = q2
                                                    trialDone[6] = true
                                                },
                                                label = { Text(q2) },
                                                modifier = Modifier.testTag("tutorial_s7_q2_chip")
                                            )
                                        }
                                        OutlinedTextField(
                                            value = sampleVoiceQuery,
                                            onValueChange = {
                                                sampleVoiceQuery = it
                                                if (it.isNotBlank()) trialDone[6] = true
                                            },
                                            label = { Text(stringResource(R.string.voice_manual_hint)) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("tutorial_s7_query_input")
                                        )
                                        if (sampleVoiceQuery.isNotBlank()) {
                                            Text(
                                                text = stringResource(R.string.tutorial_s7_reply_prefix, sampleVoiceQuery.trim()),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StockGreen
                                            )
                                        }
                                    }

                                    else -> {
                                        Text(
                                            text = stringResource(R.string.tutorial_s8_prompt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalInk
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(3, 5, 10).forEach { mins ->
                                                FilterChip(
                                                    selected = sampleTimerMins == mins,
                                                    onClick = {
                                                        sampleTimerMins = mins
                                                        trialDone[7] = true
                                                    },
                                                    label = { Text("${mins}m") },
                                                    modifier = Modifier.testTag("tutorial_s8_timer_${mins}m")
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (sampleTimerMins == 0) sampleTimerMins = 5
                                                sampleSettingsDriveTested = true
                                                trialDone[7] = true
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("tutorial_s8_drive_button")
                                        ) {
                                            Text(stringResource(R.string.tutorial_s8_drive_btn))
                                        }
                                        if (sampleTimerMins > 0 || sampleSettingsDriveTested) {
                                            Text(
                                                text = stringResource(
                                                    R.string.tutorial_s8_result_fmt,
                                                    sampleTimerMins.coerceAtLeast(5)
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StockGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Trial Status Strip
                    Surface(
                        color = if (currentTrialCompleted) {
                            StockGreen.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tutorial_trial_status_banner")
                    ) {
                        Text(
                            text = stringResource(
                                if (currentTrialCompleted) R.string.tutorial_try_completed else R.string.tutorial_try_pending
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentTrialCompleted) StockGreen else CharcoalInk,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    // Sequential Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex -= 1 },
                                modifier = Modifier.testTag("tutorial_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.tutorial_prev_btn))
                            }
                        }

                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onCompleted()
                                } else {
                                    currentStepIndex += 1
                                }
                            },
                            enabled = currentTrialCompleted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastStep) StockGreen else BlueprintNavy,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag(if (isLastStep) "tutorial_finish_button" else "tutorial_next_button")
                        ) {
                            Icon(
                                imageVector = if (isLastStep) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLastStep) {
                                    stringResource(R.string.tutorial_finish_btn)
                                } else {
                                    stringResource(R.string.tutorial_next_btn)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
