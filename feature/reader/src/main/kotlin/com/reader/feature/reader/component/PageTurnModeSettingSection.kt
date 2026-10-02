package com.reader.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.core.designsystem.theme.ReaderTheme
import com.reader.core.model.PageTurnAnimation

/**
 * 翻页动效模式切换选择器。
 *
 * 包含：
 * 1. 平滑覆盖 (COVER) —— 【出厂默认/推荐】，横向层叠遮盖
 * 2. 3D 仿真 (SIMULATION) —— 经典拟真仿真贝塞尔曲面翻页
 * 3. 平移滑动 (SLIDE) —— 双向水平并排平移滑动
 * 4. 垂直滚动 (CONTINUOUS_SCROLL) —— 现代长文垂直连续无缝滚动
 * 5. 无动画 (NONE) —— 瞬间切页，极速省电与水墨屏优选
 *
 * @param selectedAnimation 当前选中的翻页动效模式
 * @param onAnimationSelected 切换翻页动效回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageTurnModeSettingSection(
    selectedAnimation: PageTurnAnimation,
    onAnimationSelected: (PageTurnAnimation) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPalette = ReaderTheme.readingPalette

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "翻页交互模式",
                style = MaterialTheme.typography.titleSmall,
                color = currentPalette.textColor
            )
            Text(
                text = "出厂默认: 平滑覆盖",
                fontSize = 11.sp,
                color = currentPalette.secondaryTextColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 翻页模式芯片卡片网格
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PageTurnAnimation.entries.forEach { anim ->
                val isSelected = anim == selectedAnimation
                val icon = getAnimationIcon(anim)
                val isDefault = anim == PageTurnAnimation.COVER

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) currentPalette.primaryAccent.copy(alpha = 0.15f)
                            else currentPalette.surfaceColor
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) currentPalette.primaryAccent
                            else currentPalette.dividerColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onAnimationSelected(anim) }
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = anim.displayName,
                            tint = if (isSelected) currentPalette.primaryAccent else currentPalette.secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = anim.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) currentPalette.primaryAccent else currentPalette.textColor
                        )

                        if (isDefault) {
                            Text(
                                text = "默认",
                                fontSize = 9.sp,
                                color = if (isSelected) currentPalette.primaryAccent else currentPalette.secondaryTextColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getAnimationIcon(animation: PageTurnAnimation): ImageVector {
    return when (animation) {
        PageTurnAnimation.COVER -> Icons.Default.Layers
        PageTurnAnimation.SIMULATION -> Icons.Default.AutoStories
        PageTurnAnimation.SLIDE -> Icons.Default.SwapHoriz
        PageTurnAnimation.CONTINUOUS_SCROLL -> Icons.Default.UnfoldMore
        PageTurnAnimation.NONE -> Icons.Default.Block
    }
}
