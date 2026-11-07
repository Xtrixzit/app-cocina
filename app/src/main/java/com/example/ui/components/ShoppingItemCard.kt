package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PantryItem
import com.example.data.model.ShoppingItem
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockYellow

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    pantryItem: PantryItem? = null,
    onTogglePurchased: (Boolean) -> Unit,
    onAdjustQuantity: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (item.isPurchased) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(),
        label = "shoppingCardColor"
    )

    val formattedQty = if (item.quantity % 1.0 == 0.0) {
        item.quantity.toInt().toString()
    } else {
        "%.1f".format(item.quantity)
    }

    val step = when (item.unit.lowercase()) {
        "g", "ml" -> 50.0
        "kg", "l" -> 0.5
        else -> 1.0
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            .testTag("shopping_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Top Row: Checkbox, Name, and Action Buttons (Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isPurchased,
                        onCheckedChange = onTogglePurchased,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("shopping_checkbox_${item.id}")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = item.name,
                            fontFamily = com.example.ui.theme.CaveatFontFamily,
                            fontSize = 24.sp,
                            fontWeight = if (item.isPurchased) FontWeight.Normal else FontWeight.Bold,
                            textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (item.isPurchased) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )

                        if (item.recipeSource.isNotBlank()) {
                            Text(
                                text = "Para: ${item.recipeSource}",
                                fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("shopping_edit_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar cantidad",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("shopping_delete_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar de la lista",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Row: Inventory Quantity Available Pill & Steppers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Available Stock Pill
                Column {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Disponible:",
                                fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = "$formattedQty ${item.unit}",
                                transitionSpec = {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                            slideOutVertically { height -> -height } + fadeOut()
                                },
                                label = "shoppingQtyAnimation"
                            ) { text ->
                                Text(
                                    text = text,
                                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (pantryItem != null) {
                        val pQty = if (pantryItem.quantity % 1.0 == 0.0) pantryItem.quantity.toInt().toString() else "%.1f".format(pantryItem.quantity)
                        Text(
                            text = "En despensa: $pQty ${pantryItem.unit}",
                            fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                            fontSize = 13.sp,
                            color = StockGreen,
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                        )
                    }
                }

                // Interactive Counter Steppers (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onAdjustQuantity(-step) },
                        modifier = Modifier
                            .size(36.dp)
                            .stampClickable { onAdjustQuantity(-step) }
                            .testTag("shopping_dec_${item.id}"),
                        enabled = item.quantity > 0.0,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Disminuir cantidad disponible",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { onAdjustQuantity(step) },
                        modifier = Modifier
                            .size(36.dp)
                            .stampClickable { onAdjustQuantity(step) }
                            .testTag("shopping_inc_${item.id}"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar cantidad disponible",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
