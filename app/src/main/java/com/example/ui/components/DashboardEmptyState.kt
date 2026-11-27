package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BlueprintNavy
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.CrimsonMarker
import com.example.ui.theme.ForestGreenMarker
import com.example.ui.theme.NotebookMarginRed
import com.example.ui.theme.NotebookRuledLine
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow

/**
 * Full-dashboard empty state shown when the user has neither recipes nor pantry items yet.
 * Highlights a clear call-to-action to register the user's first recipe without any seeded data.
 */
@Composable
fun DashboardEmptyState(
    onAddFirstRecipe: () -> Unit,
    onGoToPantry: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true
) {
    val containerModifier = if (scrollable) {
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .padding(bottom = 84.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .padding(bottom = 84.dp)
    }
    Column(
        modifier = containerModifier
            .testTag("dashboard_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFFDF7))
                .sketchedBorder(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    strokeWidthDp = 1.8f,
                    cornerRadiusDp = 16f
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom sketchbook recipe-book & pantry jar illustration
            EmptyKitchenNotebookIllustration(
                modifier = Modifier.size(116.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = PostItMint.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, CharcoalInk.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ForestGreenMarker,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.dashboard_empty_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalInk
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.dashboard_empty_title),
                fontFamily = CaveatFontFamily,
                fontSize = 31.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlueprintNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.dashboard_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalInk.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Concise 3-step roadmap card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PostItYellow.copy(alpha = 0.42f))
                    .sketchedBorder(
                        color = CharcoalInk.copy(alpha = 0.45f),
                        strokeWidthDp = 1.2f,
                        cornerRadiusDp = 10f
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmptyStepRow(
                    icon = Icons.Default.MenuBook,
                    tint = CrimsonMarker,
                    text = stringResource(R.string.dashboard_empty_step1)
                )
                EmptyStepRow(
                    icon = Icons.Default.Inventory2,
                    tint = ForestGreenMarker,
                    text = stringResource(R.string.dashboard_empty_step2)
                )
                EmptyStepRow(
                    icon = Icons.Default.Calculate,
                    tint = BlueprintNavy,
                    text = stringResource(R.string.dashboard_empty_step3)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Call-To-Action: register first recipe
            PostItButton(
                text = stringResource(R.string.dashboard_empty_cta_recipe),
                icon = Icons.Default.Add,
                onClick = onAddFirstRecipe,
                backgroundColor = PostItPeach,
                textColor = CharcoalInk,
                rotationDegrees = -0.7f,
                fontSize = 21.sp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("dashboard_empty_add_recipe_cta")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary action: jump directly to Pantry if desired
            OutlinedButton(
                onClick = onGoToPantry,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.2.dp, BlueprintNavy.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PostItSky.copy(alpha = 0.25f),
                    contentColor = BlueprintNavy
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("dashboard_empty_go_pantry_cta")
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dashboard_empty_cta_pantry),
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyStepRow(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = CharcoalInk,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyKitchenNotebookIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PostItPeach.copy(alpha = 0.55f),
                        PostItYellow.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 0.52f
                )
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = 2.2f * density

            // Open notebook base
            val bookLeft = w * 0.14f
            val bookTop = h * 0.22f
            val bookW = w * 0.72f
            val bookH = h * 0.58f

            drawRoundRect(
                color = Color(0xFFFFFBF2),
                topLeft = Offset(bookLeft, bookTop),
                size = Size(bookW, bookH),
                cornerRadius = CornerRadius(10f * density, 10f * density)
            )
            drawRoundRect(
                color = CharcoalInk.copy(alpha = 0.85f),
                topLeft = Offset(bookLeft, bookTop),
                size = Size(bookW, bookH),
                cornerRadius = CornerRadius(10f * density, 10f * density),
                style = Stroke(width = stroke)
            )

            // Spine down center
            drawLine(
                color = BlueprintNavy.copy(alpha = 0.65f),
                start = Offset(w * 0.5f, bookTop + 4f * density),
                end = Offset(w * 0.5f, bookTop + bookH - 4f * density),
                strokeWidth = 1.6f * density,
                cap = StrokeCap.Round
            )

            // Red margin and notebook lines on left page
            drawLine(
                color = NotebookMarginRed.copy(alpha = 0.65f),
                start = Offset(bookLeft + bookW * 0.16f, bookTop + 6f * density),
                end = Offset(bookLeft + bookW * 0.16f, bookTop + bookH - 6f * density),
                strokeWidth = 1.2f * density
            )
            for (i in 1..4) {
                val lineY = bookTop + bookH * (0.22f * i)
                drawLine(
                    color = NotebookRuledLine,
                    start = Offset(bookLeft + 8f * density, lineY),
                    end = Offset(w * 0.47f, lineY),
                    strokeWidth = 1.1f * density
                )
                drawLine(
                    color = NotebookRuledLine,
                    start = Offset(w * 0.53f, lineY),
                    end = Offset(bookLeft + bookW - 8f * density, lineY),
                    strokeWidth = 1.1f * density
                )
            }

            // Plus spark badge on top-right corner of the book
            val badgeCenter = Offset(bookLeft + bookW - 4f * density, bookTop + 4f * density)
            val badgeRadius = 12f * density
            drawCircle(
                color = PostItMint,
                radius = badgeRadius,
                center = badgeCenter
            )
            drawCircle(
                color = CharcoalInk.copy(alpha = 0.85f),
                radius = badgeRadius,
                center = badgeCenter,
                style = Stroke(width = 1.6f * density)
            )
            val plusArm = 5f * density
            drawLine(
                color = CharcoalInk,
                start = Offset(badgeCenter.x - plusArm, badgeCenter.y),
                end = Offset(badgeCenter.x + plusArm, badgeCenter.y),
                strokeWidth = 2f * density,
                cap = StrokeCap.Round
            )
            drawLine(
                color = CharcoalInk,
                start = Offset(badgeCenter.x, badgeCenter.y - plusArm),
                end = Offset(badgeCenter.x, badgeCenter.y + plusArm),
                strokeWidth = 2f * density,
                cap = StrokeCap.Round
            )

            // Small wooden spoon accent angled gently at bottom-left
            val spoonPath = Path().apply {
                moveTo(w * 0.22f, h * 0.72f)
                lineTo(w * 0.36f, h * 0.48f)
            }
            drawPath(
                path = spoonPath,
                color = CrimsonMarker,
                style = Stroke(width = 2.4f * density, cap = StrokeCap.Round)
            )
            drawCircle(
                color = PostItPeach,
                radius = 5.5f * density,
                center = Offset(w * 0.38f, h * 0.45f)
            )
            drawCircle(
                color = CrimsonMarker,
                radius = 5.5f * density,
                center = Offset(w * 0.38f, h * 0.45f),
                style = Stroke(width = 1.8f * density)
            )
        }
    }
}
