package com.reader.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.core.designsystem.theme.ReaderTheme
import com.reader.core.model.ReaderThemePreset

/**
 * 5套预设主题选择器组件。
 *
 * 预置五大场景化护眼阅读配色：
 * 1. 日间纸白 (DEFAULT_LIGHT)
 * 2. 复古羊皮 (PARCHMENT)
 * 3. 护眼豆沙 (GREEN_TEA)
 * 4. 黑白水墨 (E_INK)
 * 5. 纯黑夜间 (DARK_NIGHT)
 *
 * 每个主题项呈现背景色块样本、字样缩略以及当前选中的高亮圈与勾选标识。
 *
 * @param selectedPreset 当前选中的主题预设
 * @param onPresetSelected 切换主题预设回调
 */
@Composable
fun ThemeSettingSection(
    selectedPreset: ReaderThemePreset,
    onPresetSelected: (ReaderThemePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPalette = ReaderTheme.readingPalette

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "阅读配色主题",
            style = MaterialTheme.typography.titleSmall,
            color = currentPalette.textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderThemePreset.entries.forEach { preset ->
                val isSelected = preset == selectedPreset
                val (bgColor, textColor, borderStrokeColor) = getThemeColors(preset)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onPresetSelected(preset) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) currentPalette.primaryAccent else borderStrokeColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选中",
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "文",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = preset.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) currentPalette.primaryAccent else currentPalette.secondaryTextColor
                    )
                }
            }
        }
    }
}

/**
 * 转换预设的主题色至 Compose Color
 */
private fun getThemeColors(preset: ReaderThemePreset): Triple<Color, Color, Color> {
    return when (preset) {
        ReaderThemePreset.DEFAULT_LIGHT -> Triple(
            Color(0xFFF8F6F1),
            Color(0xFF2B2824),
            Color(0xFFDDD8CE)
        )
        ReaderThemePreset.PARCHMENT -> Triple(
            Color(0xFFF0E5D0),
            Color(0xFF3D2F1F),
            Color(0xFFD8C7AA)
        )
        ReaderThemePreset.GREEN_TEA -> Triple(
            Color(0xFFD8E5D4),
            Color(0xFF1C2D1F),
            Color(0xFFB5C9B0)
        )
        ReaderThemePreset.E_INK -> Triple(
            Color(0xFFFFFFFF),
            Color(0xFF000000),
            Color(0xFFCCCCCC)
        )
        ReaderThemePreset.DARK_NIGHT -> Triple(
            Color(0xFF121212),
            Color(0xFFB0B0B0),
            Color(0xFF333333)
        )
    }
}
