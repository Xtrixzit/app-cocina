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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PantryItem
import com.example.settings.AppSettingsPreferences
import com.example.settings.CurrencyOption
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockRed
import com.example.ui.theme.StockYellow
import com.example.util.LocalizedLabels
import com.example.util.UnitConverter

@Composable
fun PantryItemCard(
    item: PantryItem,
    onAdjustStock: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    currency: CurrencyOption = CurrencyOption.USD
) {
    val context = LocalContext.current

    val targetStatusColor = when {
        item.isOutOfStock -> StockRed
        item.isLowStock -> StockYellow
        else -> StockGreen
    }

    val statusColor by animateColorAsState(
        targetValue = targetStatusColor,
        animationSpec = spring(),
        label = "pantryStatusColor"
    )

    val statusText = when {
        item.isOutOfStock -> stringResource(R.string.pantry_status_out)
        item.isLowStock -> stringResource(R.string.pantry_status_low)
        else -> stringResource(R.string.pantry_status_ok)
    }

    val displayUnit = LocalizedLabels.unit(context, item.unit)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            .testTag("pantry_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = LocalizedLabels.pantryCategory(context, item.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pantry_edit_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pantry_delete_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.name,
                fontFamily = CaveatFontFamily,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            if (item.unitCost > 0.0) {
                val formattedUnitCost = AppSettingsPreferences.formatMoney(item.unitCost, currency, forceFourDecimalsIfTiny = true)
                val totalStockVal = AppSettingsPreferences.formatMoney(item.quantity * item.unitCost, currency)
                val macroRef = UnitConverter.macroUnitCostReference(item.unitCost, item.unit)
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏷 $formattedUnitCost / $displayUnit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.pantry_card_value, totalStockVal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (macroRef != null) {
                        Text(
                            text = stringResource(
                                R.string.pantry_form_macro_cost_fmt,
                                AppSettingsPreferences.formatMoney(macroRef.first, currency),
                                LocalizedLabels.unit(context, macroRef.second)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = StockGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.pantry_card_no_cost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val formattedQty = UnitConverter.formatQuantity(item.quantity)
                    val qtyEquiv = UnitConverter.humanEquivalent(item.quantity, item.unit)
                    val qtyText = if (qtyEquiv != null) {
                        "$formattedQty $displayUnit (${UnitConverter.formatQuantity(qtyEquiv.first)} ${LocalizedLabels.unit(context, qtyEquiv.second)})"
                    } else {
                        "$formattedQty $displayUnit"
                    }

                    AnimatedContent(
                        targetState = qtyText,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                        },
                        label = "pantryQtyAnimation"
                    ) { text ->
                        Text(
                            text = text,
                            fontFamily = CaveatFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isOutOfStock) StockRed else MaterialTheme.colorScheme.primary
                        )
                    }

                    if (item.minQuantity > 0.0) {
                        val minStr = UnitConverter.formatQuantity(item.minQuantity)
                        Text(
                            text = stringResource(R.string.pantry_card_min_alert, minStr, displayUnit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val step = UnitConverter.quickAdjustStep(item.quantity, item.unit)

                    FilledTonalIconButton(
                        onClick = { onAdjustStock(-step) },
                        modifier = Modifier
                            .size(38.dp)
                            .stampClickable { onAdjustStock(-step) }
                            .testTag("pantry_dec_${item.id}"),
                        enabled = item.quantity > 0.0,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.action_less), modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = { onAdjustStock(step) },
                        modifier = Modifier
                            .size(38.dp)
                            .stampClickable { onAdjustStock(step) }
                            .testTag("pantry_inc_${item.id}"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_more), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}
