package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Recipe
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItTape
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockYellow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    feasibilityMatchPct: Int?,
    estimatedCost: Double? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val categoryGradient = when (recipe.category.lowercase()) {
        "desayunos" -> listOf(Color(0xFFEA580C), Color(0xFFFBBF24))
        "almuerzos" -> listOf(Color(0xFFDC2626), Color(0xFFFB923C))
        "cenas" -> listOf(Color(0xFF15803D), Color(0xFF84CC16))
        "postres" -> listOf(Color(0xFFBE185D), Color(0xFFF472B6))
        else -> listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
    }

    val foodIcon = when (recipe.category.lowercase()) {
        "desayunos" -> Icons.Default.BreakfastDining
        "almuerzos" -> Icons.Default.LunchDining
        "cenas" -> Icons.Default.DinnerDining
        "postres" -> Icons.Default.LocalCafe
        else -> Icons.Default.Restaurant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .sketchedBorder(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.65f else 0.85f),
                strokeWidthDp = 1.8f
            )
            .stampClickable(onClick = onClick)
            .testTag("recipe_card_${recipe.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Washi tape accent in top corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-4).dp)
                    .size(width = 38.dp, height = 10.dp)
                    .background(if (isDark) Color(0x55FFFFFF) else PostItTape, RoundedCornerShape(1.dp))
            )

            Column(modifier = Modifier.padding(14.dp)) {
                // Top row with Category and Feasibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category pill with hand drawn font
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = recipe.category.uppercase(),
                            fontFamily = CaveatFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Pantry Feasibility Indicator
                    if (feasibilityMatchPct != null) {
                        val isFull = feasibilityMatchPct >= 100
                        val statusColor by animateColorAsState(
                            targetValue = if (isFull) StockGreen else StockYellow,
                            animationSpec = spring(),
                            label = "statusColor"
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFull) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFull) "100% en despensa" else "$feasibilityMatchPct% en despensa",
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 14.sp,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notebook Layout: Polaroid / Sketched Food Frame + Handwritten Title & Info (like the reference image)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Polaroid Photo Frame with sketched border
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(2.dp, RoundedCornerShape(3.dp))
                            .background(
                                color = if (isDark) Color(0xFF27272A) else Color(0xFFFFFDF8),
                                shape = RoundedCornerShape(3.dp)
                            )
                            .sketchedBorder(
                                color = if (isDark) Color(0xFF52525B) else Color(0xFF18181B),
                                strokeWidthDp = 1.6f
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Brush.linearGradient(categoryGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = foodIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Recipe Title and Handwritten Notes
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.title,
                            fontFamily = CaveatFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Difficulty: ${recipe.difficulty}",
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detail Pills Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Estimated Cost Tag
                    if (estimatedCost != null && estimatedCost > 0.0) {
                        val perServing = estimatedCost / recipe.servings.coerceAtLeast(1)
                        DetailPill(
                            icon = Icons.Default.MonetizationOn,
                            text = "$${"%.2f".format(estimatedCost)} ($${"%.2f".format(perServing)}/p)"
                        )
                    }

                    // Servings Pill
                    DetailPill(
                        icon = Icons.Default.People,
                        text = "${recipe.servings} porc."
                    )

                    // Time Pill
                    DetailPill(
                        icon = Icons.Default.AccessTime,
                        text = "${recipe.totalTimeMinutes} min"
                    )

                    // Ingredients Pill
                    DetailPill(
                        icon = Icons.Default.LocalDining,
                        text = "${recipe.ingredients.size} ingr."
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontFamily = PatrickHandFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
