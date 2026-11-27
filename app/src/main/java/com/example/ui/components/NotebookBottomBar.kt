package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.CrimsonMarker
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItPink
import com.example.ui.theme.PostItSky
import com.example.ui.theme.PostItYellow

val TabBackgroundColorKey = SemanticsPropertyKey<Color>("TabBackgroundColor")
var SemanticsPropertyReceiver.tabBackgroundColor by TabBackgroundColorKey

data class NotebookTabItem(
    val title: String,
    val icon: ImageVector,
    val activeColor: Color,
    val inactiveColor: Color,
    val accentInkColor: Color,
    val testTag: String
)

@Composable
fun NotebookBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    pendingShoppingCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NotebookTabItem(
            title = stringResource(R.string.tab_recipes),
            icon = Icons.Default.MenuBook,
            activeColor = Color(0xFFFDBA74),
            inactiveColor = PostItPeach,
            accentInkColor = Color(0xFF7C2D12),
            testTag = "tab_recipes"
        ),
        NotebookTabItem(
            title = stringResource(R.string.tab_pantry),
            icon = Icons.Default.Inventory2,
            activeColor = Color(0xFF6EE7B7),
            inactiveColor = PostItMint,
            accentInkColor = Color(0xFF14532D),
            testTag = "tab_pantry"
        ),
        NotebookTabItem(
            title = stringResource(R.string.tab_shopping),
            icon = Icons.Default.Checklist,
            activeColor = Color(0xFF7DD3FC),
            inactiveColor = PostItSky,
            accentInkColor = Color(0xFF0C4A6E),
            testTag = "tab_shopping"
        ),
        NotebookTabItem(
            title = stringResource(R.string.tab_production),
            icon = Icons.Default.Scale,
            activeColor = Color(0xFFFDE047),
            inactiveColor = PostItYellow,
            accentInkColor = Color(0xFF713F12),
            testTag = "tab_production"
        ),
        NotebookTabItem(
            title = stringResource(R.string.tab_cooking),
            icon = Icons.Default.RecordVoiceOver,
            activeColor = Color(0xFFF9A8D4),
            inactiveColor = PostItPink,
            accentInkColor = Color(0xFF831843),
            testTag = "tab_assistant"
        )
    )

    val binderBorderColor = Color(0xFFD6D3D1)
    val bottomBgColor = Color(0xFFF5F0E6)
    val tabShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = binderBorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.5f * density
                )
            }
            .background(bottomBgColor)
            .navigationBarsPadding()
            .padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                val elevationDp by animateDpAsState(
                    targetValue = if (isSelected) 5.dp else 1.5.dp,
                    animationSpec = spring(),
                    label = "tabElevation"
                )
                val offsetY by animateDpAsState(
                    targetValue = if (isSelected) (-5).dp else 0.dp,
                    animationSpec = spring(),
                    label = "tabOffsetY"
                )
                val targetBg = if (isSelected) tab.activeColor else tab.inactiveColor
                val animatedBg by animateColorAsState(
                    targetValue = targetBg,
                    label = "tabBgColor"
                )
                val inkColor = if (isSelected) Color(0xFF18181B) else tab.accentInkColor
                val borderInk = if (isSelected) Color(0xFF1C1917) else Color(0xFF44403C)
                val (interactionSource, pressScale) = rememberStampPressState()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = offsetY)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                ) {
                    Surface(
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        interactionSource = interactionSource,
                        shape = tabShape,
                        color = animatedBg,
                        contentColor = inkColor,
                        tonalElevation = 0.dp,
                        shadowElevation = elevationDp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .sketchedBorder(
                                color = borderInk,
                                strokeWidthDp = if (isSelected) 2.2f else 1.4f,
                                cornerRadiusDp = 10f
                            )
                            .semantics { tabBackgroundColor = targetBg }
                            .tutorialTarget(tab.testTag)
                            .testTag(tab.testTag)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 7.dp),
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

                                if (index == 2 && pendingShoppingCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 10.dp, y = (-6).dp)
                                            .size(17.dp)
                                            .background(CrimsonMarker, CircleShape)
                                            .sketchedBorder(
                                                color = Color(0xFF18181B),
                                                strokeWidthDp = 1f,
                                                cornerRadiusDp = 9f
                                            ),
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
                                fontSize = if (isSelected) 16.sp else 15.sp,
                                color = inkColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(width = 30.dp, height = 2.5.dp)
                                        .background(inkColor, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
