package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CrimsonMarker
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow

data class NotebookTabItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val testTag: String
)

/**
 * A custom notebook section divider navigation bar that looks like physical
 * indexed sticky bookmarks / divider tabs sticking out along the bottom of the open recipe notebook.
 */
@Composable
fun NotebookBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    pendingShoppingCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val tabs = listOf(
        NotebookTabItem(
            title = "Recetas",
            icon = Icons.Default.MenuBook,
            color = PostItPeach,
            testTag = "tab_recipes"
        ),
        NotebookTabItem(
            title = "Despensa",
            icon = Icons.Default.Inventory2,
            color = PostItMint,
            testTag = "tab_pantry"
        ),
        NotebookTabItem(
            title = "Check list",
            icon = Icons.Default.Checklist,
            color = PostItSky,
            testTag = "tab_shopping"
        ),
        NotebookTabItem(
            title = "Producción",
            icon = Icons.Default.Scale,
            color = PostItYellow,
            testTag = "tab_production"
        ),
        NotebookTabItem(
            title = "Surprise me",
            icon = Icons.Default.AutoAwesome,
            color = PostItPink,
            testTag = "tab_assistant"
        )
    )

    val binderBorderColor = if (isDark) Color(0xFF27272A) else Color(0xFFD6D3D1)
    val bottomBgColor = if (isDark) Color(0xFF141417) else Color(0xFFF5F0E6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Top dividing line separating page from bottom tab binder
                drawLine(
                    color = binderBorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.5f * density
                )
            }
            .background(bottomBgColor)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                val elevationDp by animateDpAsState(
                    targetValue = if (isSelected) 6.dp else 1.dp,
                    animationSpec = spring(),
                    label = "tabElevation"
                )
                val offsetY by animateDpAsState(
                    targetValue = if (isSelected) (-6).dp else 0.dp,
                    animationSpec = spring(),
                    label = "tabOffsetY"
                )

                val tabBackground = if (isSelected) {
                    if (isDark) {
                        when (index) {
                            0 -> Color(0xFF3E180C) // Rich terracotta chalk
                            1 -> Color(0xFF0A341E) // Rich emerald mint chalk
                            2 -> Color(0xFF0B2E47) // Rich sapphire sky chalk
                            3 -> Color(0xFF382E0B) // Rich amber yellow chalk
                            4 -> Color(0xFF3B1228) // Rich berry pink chalk
                            else -> Color(0xFF27272A)
                        }
                    } else {
                        tab.color
                    }
                } else {
                    if (isDark) Color(0xFF1E1E22) else Color(0xFFECE7DB)
                }

                val inkColor = if (isSelected) {
                    if (isDark) {
                        when (index) {
                            0 -> Color(0xFFFDBA74) // Glowing peach chalk
                            1 -> Color(0xFF86EFAC) // Glowing mint chalk
                            2 -> Color(0xFF7DD3FC) // Glowing sky chalk
                            3 -> Color(0xFFFDE047) // Glowing yellow chalk
                            4 -> Color(0xFFF472B6) // Glowing pink chalk
                            else -> Color(0xFFF4F4F5)
                        }
                    } else {
                        Color(0xFF18181B) // Deep charcoal ink on light pastel
                    }
                } else {
                    if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                }

                val borderInk = if (isSelected) {
                    if (isDark) {
                        when (index) {
                            0 -> Color(0xFFEA580C)
                            1 -> Color(0xFF22C55E)
                            2 -> Color(0xFF0EA5E9)
                            3 -> Color(0xFFEAB308)
                            4 -> Color(0xFFEC4899)
                            else -> Color(0xFF71717A)
                        }
                    } else {
                        Color(0xFF1C1917)
                    }
                } else {
                    if (isDark) Color(0xFF3F3F46) else Color(0xFFA8A29E)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = offsetY)
                        .shadow(
                            elevation = elevationDp,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                            spotColor = if (isDark) Color(0x99000000) else Color(0x33000000)
                        )
                        .background(
                            color = tabBackground,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                        )
                        .sketchedBorder(
                            color = borderInk,
                            strokeWidthDp = if (isSelected) 2f else 1.2f,
                            cornerRadiusDp = 10f
                        )
                        .stampClickable(onClick = { onTabSelected(index) })
                        .testTag(tab.testTag)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = inkColor,
                                modifier = Modifier.size(if (isSelected) 22.dp else 20.dp)
                            )

                            // Shopping list badge
                            if (index == 2 && pendingShoppingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 10.dp, y = (-6).dp)
                                        .size(17.dp)
                                        .background(CrimsonMarker, CircleShape)
                                        .sketchedBorder(color = Color(0xFF18181B), strokeWidthDp = 1f, cornerRadiusDp = 9f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$pendingShoppingCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tab.title,
                            fontFamily = CaveatFontFamily,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = if (isSelected) 18.sp else 16.sp,
                            color = inkColor,
                            maxLines = 1
                        )

                        // If selected, draw an ink underline highlight
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(width = 30.dp, height = 2.dp)
                                    .background(inkColor, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
