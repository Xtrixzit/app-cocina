package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow
import com.example.ui.theme.StockGreen

data class TutorialFeatureBullet(
    @StringRes val titleResId: Int,
    @StringRes val descResId: Int
)

data class TutorialStepSpec(
    val index: Int,
    val targetTab: Int,
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
            icon = Icons.Default.MenuBook,
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
 * Mandatory, non-skippable interactive walkthrough covering every screen, button, and capability.
 * Blocks free interaction and system back-dismissal until every step is acknowledged and completed.
 */
@Composable
fun GuidedTutorialOverlay(
    onSwitchTab: (Int) -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = GuidedTutorialCatalog.steps
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var stepConfirmed by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val step = steps[currentStepIndex]
    val totalSteps = steps.size
    val isLastStep = currentStepIndex == totalSteps - 1

    LaunchedEffect(currentStepIndex) {
        onSwitchTab(step.targetTab)
        scrollState.scrollTo(0)
    }

    // Prevent escaping the mandatory tutorial via the system Back button
    BackHandler(enabled = true) {
        if (currentStepIndex > 0) {
            currentStepIndex -= 1
            stepConfirmed = true
        }
    }

    val scrimInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("guided_tutorial_overlay"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .sketchedBorder(color = MaterialTheme.colorScheme.secondary, strokeWidthDp = 2.2f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Mandatory badge + Step X / 8 + Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = step.accentColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.tutorial_header_badge),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.tutorial_progress_fmt, currentStepIndex + 1, totalSteps),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("tutorial_progress_label")
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = stringResource(R.string.tutorial_must_finish_note, totalSteps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Step Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = step.accentColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = stringResource(step.titleResId),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(step.titleResId),
                            fontFamily = CaveatFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag("tutorial_step_title")
                        )
                        Text(
                            text = stringResource(step.subtitleResId),
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Detailed Breakdown of Every Button & Function on This Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 240.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    step.bullets.forEach { bullet ->
                        Surface(
                            color = step.accentColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    strokeWidthDp = 1f
                                )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(bullet.titleResId),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(bullet.descResId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Mandatory Acknowledgement Checkbox Per Step (forces user review before unlocking Next)
                Surface(
                    color = if (stepConfirmed) {
                        StockGreen.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    onClick = { stepConfirmed = !stepConfirmed },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tutorial_confirm_step_box")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = stepConfirmed,
                            onCheckedChange = { stepConfirmed = it },
                            colors = CheckboxDefaults.colors(checkedColor = StockGreen),
                            modifier = Modifier.testTag("tutorial_confirm_step_checkbox")
                        )
                        Text(
                            text = stringResource(R.string.tutorial_confirm_check),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Step Controls (Strictly Sequential — No Skip Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = {
                                currentStepIndex -= 1
                                stepConfirmed = true
                            },
                            modifier = Modifier.testTag("tutorial_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
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
                                stepConfirmed = false
                            }
                        },
                        enabled = stepConfirmed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastStep) StockGreen else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(if (isLastStep) "tutorial_finish_button" else "tutorial_next_button")
                    ) {
                        Icon(
                            imageVector = if (isLastStep) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
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
