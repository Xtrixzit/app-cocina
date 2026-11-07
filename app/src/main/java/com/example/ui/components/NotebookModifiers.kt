package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.CharcoalInkDark
import com.example.ui.theme.NotebookMarginRed
import com.example.ui.theme.NotebookMarginRedDark
import com.example.ui.theme.NotebookRuledLine
import com.example.ui.theme.NotebookRuledLineDark
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItTape
import com.example.ui.theme.PostItYellow

/**
 * Draws realistic notebook ruled lines and a left red margin line.
 * Automatically adapts between Light ivory paper and Dark chalkboard paper.
 */
@Composable
fun Modifier.notebookPaperBackground(
    lineSpacingDp: Float = 32f,
    marginOffsetDp: Float = 40f,
    showBinderHoles: Boolean = true
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val lineColor = if (isDark) NotebookRuledLineDark else NotebookRuledLine
    val marginColor = if (isDark) NotebookMarginRedDark else NotebookMarginRed

    return this.drawBehind {
        val spacingPx = lineSpacingDp * density
        val marginPx = marginOffsetDp * density

        // Draw horizontal ruled lines
        var y = spacingPx
        while (y < size.height) {
            drawLine(
                color = lineColor.copy(alpha = if (isDark) 0.65f else 0.6f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = (if (isDark) 1.0f else 1.2f) * density
            )
            y += spacingPx
        }

        // Draw vertical red margin line on the left
        drawLine(
            color = marginColor.copy(alpha = if (isDark) 0.75f else 0.5f),
            start = Offset(marginPx, 0f),
            end = Offset(marginPx, size.height),
            strokeWidth = 1.8f * density
        )

        // Draw notebook binder punch holes along the left margin
        if (showBinderHoles && marginPx > 20f) {
            val holeX = marginPx * 0.45f
            val holeRadius = 4.5f * density
            var holeY = 48f * density
            val holeSpacing = 80f * density

            while (holeY < size.height - 24f * density) {
                // Outer shadow rim
                drawCircle(
                    color = if (isDark) Color(0xFF09090B) else Color(0xFFD6D3D1),
                    radius = holeRadius,
                    center = Offset(holeX, holeY)
                )
                // Inner punched hole
                drawCircle(
                    color = if (isDark) Color(0xFF141417) else Color(0xFFE8E2D5),
                    radius = holeRadius - 1.2f * density,
                    center = Offset(holeX, holeY)
                )
                holeY += holeSpacing
            }
        }
    }
}

/**
 * Draws a hand-drawn sketched ink box with a subtle double offset stroke,
 * mimicking the reference image boxes.
 */
@Composable
fun Modifier.sketchedBorder(
    color: Color = Color.Unspecified,
    strokeWidthDp: Float = 2f,
    cornerRadiusDp: Float = 4f
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val effectiveColor = if (color != Color.Unspecified) {
        color
    } else {
        if (isDark) CharcoalInkDark.copy(alpha = 0.75f) else CharcoalInk.copy(alpha = 0.85f)
    }

    return this.drawBehind {
        val strokeWidthPx = strokeWidthDp * density
        val cornerRadiusPx = cornerRadiusDp * density

        // Primary rectangle
        drawRoundRect(
            color = effectiveColor,
            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
            size = androidx.compose.ui.geometry.Size(
                size.width - strokeWidthPx,
                size.height - strokeWidthPx
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = strokeWidthPx)
        )

        // Secondary subtle sketch line for hand-drawn character
        drawRoundRect(
            color = effectiveColor.copy(alpha = if (isDark) 0.45f else 0.35f),
            topLeft = Offset(strokeWidthPx + 1.5f, strokeWidthPx + 1.5f),
            size = androidx.compose.ui.geometry.Size(
                size.width - (strokeWidthPx * 2) - 2f,
                size.height - (strokeWidthPx * 2) - 2f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = 1f * density)
        )
    }
}

/**
 * Bouncy stamp-press interaction animation for buttons.
 */
@Composable
fun Modifier.stampClickable(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "stampScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * A tactile, handwritten "Post-It" sticky note button that looks like a real
 * note taped or pinned to the notebook.
 * Optimally contrasted and visible on both light and dark backgrounds.
 */
@Composable
fun PostItButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = PostItYellow,
    textColor: Color = Color(0xFF18181B), // Dark charcoal ink on light pastel
    rotationDegrees: Float = 0f,
    showTape: Boolean = true,
    fontSize: TextUnit = 21.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // In dark mode, map pastels into deep chalkboard cards with luminous chalk ink and chalk borders
    val (effectiveBg, effectiveText, effectiveBorder) = if (isDark) {
        when (backgroundColor) {
            PostItYellow -> Triple(Color(0xFF382E0B), Color(0xFFFDE047), Color(0xFFEAB308))
            PostItMint -> Triple(Color(0xFF0D3320), Color(0xFF86EFAC), Color(0xFF22C55E))
            PostItPeach -> Triple(Color(0xFF381A0E), Color(0xFFFDBA74), Color(0xFFF97316))
            PostItSky -> Triple(Color(0xFF0C2B42), Color(0xFF7DD3FC), Color(0xFF0EA5E9))
            com.example.ui.theme.PostItPink -> Triple(Color(0xFF3B1226), Color(0xFFF472B6), Color(0xFFEC4899))
            else -> Triple(Color(0xFF27272A), Color(0xFFF4F4F5), Color(0xFF71717A))
        }
    } else {
        Triple(backgroundColor, textColor, CharcoalInk.copy(alpha = 0.85f))
    }

    val shadowColor = if (isDark) Color(0x99000000) else Color(0x33000000)

    Box(
        modifier = modifier
            .rotate(rotationDegrees)
            .shadow(
                elevation = if (isDark) 4.dp else 3.dp,
                shape = RoundedCornerShape(3.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(effectiveBg, RoundedCornerShape(3.dp))
            .sketchedBorder(color = effectiveBorder, strokeWidthDp = if (isDark) 2.2f else 1.8f)
            .stampClickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        // Optional top frosted washi-tape band
        if (showTape) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
                    .size(width = 34.dp, height = 8.dp)
                    .background(if (isDark) Color(0x44FFFFFF) else PostItTape, RoundedCornerShape(1.dp))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveText,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = text,
                fontFamily = CaveatFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = effectiveText
            )
        }
    }
}

/**
 * A tactile sticky note chip for filters, categories, and toggles.
 * Adapted for crisp visibility in both Light and Dark modes.
 */
@Composable
fun PostItFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selectedColor: Color = PostItYellow,
    rotationDegrees: Float = if (selected) -0.8f else 0f
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val (effectiveBg, effectiveText, strokeColor) = if (selected) {
        if (isDark) {
            when (selectedColor) {
                PostItYellow -> Triple(Color(0xFF382E0B), Color(0xFFFDE047), Color(0xFFEAB308))
                PostItMint -> Triple(Color(0xFF0D3320), Color(0xFF86EFAC), Color(0xFF22C55E))
                PostItPeach -> Triple(Color(0xFF381A0E), Color(0xFFFDBA74), Color(0xFFF97316))
                PostItSky -> Triple(Color(0xFF0C2B42), Color(0xFF7DD3FC), Color(0xFF0EA5E9))
                com.example.ui.theme.PostItPink -> Triple(Color(0xFF3B1226), Color(0xFFF472B6), Color(0xFFEC4899))
                else -> Triple(Color(0xFF2E2E33), Color(0xFFF4F4F5), Color(0xFF71717A))
            }
        } else {
            Triple(selectedColor, Color(0xFF18181B), CharcoalInk.copy(alpha = 0.9f))
        }
    } else {
        if (isDark) {
            Triple(Color(0xFF222226), Color(0xFFA1A1AA), Color(0xFF3F3F46))
        } else {
            Triple(Color(0xFFFBF8F0), CharcoalInk.copy(alpha = 0.75f), CharcoalInk.copy(alpha = 0.35f))
        }
    }

    Box(
        modifier = modifier
            .rotate(rotationDegrees)
            .shadow(
                elevation = if (selected) (if (isDark) 4.dp else 3.dp) else 1.dp,
                shape = RoundedCornerShape(3.dp)
            )
            .background(effectiveBg, RoundedCornerShape(3.dp))
            .sketchedBorder(
                color = strokeColor,
                strokeWidthDp = if (selected) 2f else 1.2f
            )
            .stampClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveText,
                    modifier = Modifier
                        .size(15.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = text,
                fontFamily = CaveatFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = if (selected) 19.sp else 17.sp,
                color = effectiveText
            )
        }
    }
}

/**
 * A Post-It note card container for expandable sections, tips, or callouts.
 */
@Composable
fun PostItCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PostItYellow,
    rotationDegrees: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val effectiveBg = if (isDark) {
        when (backgroundColor) {
            PostItYellow -> Color(0xFF2D2408)
            PostItMint -> Color(0xFF092817)
            PostItPeach -> Color(0xFF2E150B)
            PostItSky -> Color(0xFF082236)
            else -> Color(0xFF1E1E22)
        }
    } else {
        backgroundColor
    }

    val borderInk = if (isDark) {
        when (backgroundColor) {
            PostItYellow -> Color(0xFFCA8A04)
            PostItMint -> Color(0xFF16A34A)
            PostItPeach -> Color(0xFFEA580C)
            PostItSky -> Color(0xFF0284C7)
            else -> Color(0xFF52525B)
        }
    } else {
        CharcoalInk.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .rotate(rotationDegrees)
            .shadow(elevation = if (isDark) 4.dp else 3.dp, shape = RoundedCornerShape(4.dp))
            .background(effectiveBg, RoundedCornerShape(4.dp))
            .sketchedBorder(color = borderInk, strokeWidthDp = if (isDark) 2.2f else 1.8f)
    ) {
        // Frosted tape at top center
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-5).dp)
                .size(width = 44.dp, height = 10.dp)
                .background(if (isDark) Color(0x44FFFFFF) else PostItTape, RoundedCornerShape(1.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}

/**
 * Authentic circular notebook back stamp button matching the bottom-left
 * element in the reference image: a deep red outer ring, parchment center, and red hand-drawn arrow.
 */
@Composable
fun NotebookBackStamp(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val outerRingColor = if (isDark) Color(0xFF991B1B) else Color(0xFF8B1E1E)
    val innerDiscColor = if (isDark) Color(0xFF382E0B) else Color(0xFFFDE68A)
    val arrowTintColor = if (isDark) Color(0xFFFDE047) else Color(0xFF8B1E1E)
    val borderColor = if (isDark) Color(0xFFEAB308) else Color(0xFF18181B)

    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(elevation = 3.dp, shape = CircleShape)
            .background(outerRingColor, CircleShape)
            .sketchedBorder(color = borderColor, strokeWidthDp = 1.6f, cornerRadiusDp = 23f)
            .stampClickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(innerDiscColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atrás",
                tint = arrowTintColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


