package com.reader.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.core.designsystem.theme.ReaderTheme
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset

/**
 * 阅读排版设置底部弹窗 (TypographySettingBottomSheet)。
 *
 * 聚合阅读排版全量调节选项：
 * 1. 字号微调与滑块 (12sp - 36sp)；
 * 2. 行距倍数调节 (紧凑 1.3x、适中 1.6x、宽松 2.0x、超大 2.4x)；
 * 3. 字间距调节 (0.00em, 0.05em, 0.10em, 0.15em)；
 * 4. 段间距调节 (紧凑 6dp, 标准 12dp, 宽松 20dp, 超大 30dp)；
 * 5. 页边距调节 (紧凑 12dp, 标准 16dp, 宽松 24dp)；
 * 6. 5套预设主题选择 ([ThemeSettingSection])；
 * 7. 翻页动效模式切换 ([PageTurnModeSettingSection])；
 * 8. 阅读高级偏好 (屏幕常亮保持、音量键翻页)。
 *
 * @param config 当前阅读排版配置实体
 * @param onFontSizeChangeDelta 步进增减字号 (+1f / -1f)
 * @param onFontSizeChange 直接设定字号
 * @param onLineHeightChange 设定行高倍数
 * @param onLetterSpacingChange 设定字间距
 * @param onParagraphSpacingChange 设定段落间距
 * @param onHorizontalPaddingChange 设定水平边距
 * @param onVerticalPaddingChange 设定垂直边距
 * @param onThemePresetSelected 切换主题预设
 * @param onPageTurnAnimationSelected 切换翻页动画模式
 * @param onKeepScreenOnChange 切换屏幕常亮
 * @param onVolumeKeyPageTurnChange 切换音量键翻页
 * @param onDismissRequest 关闭底部弹窗请求
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypographySettingBottomSheet(
    config: ReaderConfig,
    onFontSizeChangeDelta: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onParagraphSpacingChange: (Float) -> Unit,
    onHorizontalPaddingChange: (Float) -> Unit,
    onVerticalPaddingChange: (Float) -> Unit,
    onThemePresetSelected: (ReaderThemePreset) -> Unit,
    onPageTurnAnimationSelected: (PageTurnAnimation) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onVolumeKeyPageTurnChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = ReaderTheme.readingPalette
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = palette.surfaceColor,
        contentColor = palette.textColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = bottomInset + 20.dp)
        ) {
            // 标题
            Text(
                text = "阅读与排版设置",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- 1. 字号调节 ---
            FontSizeSettingRow(
                fontSizeSp = config.fontSizeSp,
                onFontSizeChangeDelta = onFontSizeChangeDelta,
                onFontSizeChange = onFontSizeChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. 行间距调节 ---
            LineHeightSettingRow(
                currentMultiplier = config.lineHeightMultiplier,
                onLineHeightChange = onLineHeightChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. 字间距与段落间距调节 ---
            SpacingSettingRow(
                letterSpacingEm = config.letterSpacingEm,
                paragraphSpacingDp = config.paragraphSpacingDp,
                onLetterSpacingChange = onLetterSpacingChange,
                onParagraphSpacingChange = onParagraphSpacingChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. 页面边距调节 ---
            PaddingSettingRow(
                horizontalPaddingDp = config.pagePaddingHorizontalDp,
                onHorizontalPaddingChange = onHorizontalPaddingChange,
                onVerticalPaddingChange = onVerticalPaddingChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. 5套预设主题选择器 ---
            ThemeSettingSection(
                selectedPreset = config.themePreset,
                onPresetSelected = onThemePresetSelected
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- 6. 翻页交互模式切换器 ---
            PageTurnModeSettingSection(
                selectedAnimation = config.pageTurnAnimation,
                onAnimationSelected = onPageTurnAnimationSelected
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // --- 7. 阅读高级偏好 (屏幕常亮、音量键翻页) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "阅读时屏幕保持常亮",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textColor
                )
                Switch(
                    checked = config.keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.surfaceColor,
                        checkedTrackColor = palette.primaryAccent
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "使用音量键翻页",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textColor
                )
                Switch(
                    checked = config.volumeKeyPageTurn,
                    onCheckedChange = onVolumeKeyPageTurnChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.surfaceColor,
                        checkedTrackColor = palette.primaryAccent
                    )
                )
            }
        }
    }
}

/**
 * 字号行组件
 */
@Composable
private fun FontSizeSettingRow(
    fontSizeSp: Float,
    onFontSizeChangeDelta: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit
) {
    val palette = ReaderTheme.readingPalette

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "正文字号",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textColor
            )
            Text(
                text = "${fontSizeSp.toInt()} sp",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = palette.primaryAccent
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onFontSizeChangeDelta(-1f) },
                enabled = fontSizeSp > ReaderConfig.MIN_FONT_SIZE_SP
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "缩小字号",
                    tint = if (fontSizeSp > ReaderConfig.MIN_FONT_SIZE_SP) palette.textColor else palette.secondaryTextColor.copy(alpha = 0.3f)
                )
            }

            Slider(
                value = fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = ReaderConfig.MIN_FONT_SIZE_SP..ReaderConfig.MAX_FONT_SIZE_SP,
                steps = (ReaderConfig.MAX_FONT_SIZE_SP - ReaderConfig.MIN_FONT_SIZE_SP).toInt() - 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = palette.primaryAccent,
                    activeTrackColor = palette.primaryAccent,
                    inactiveTrackColor = palette.secondaryTextColor.copy(alpha = 0.2f)
                )
            )

            IconButton(
                onClick = { onFontSizeChangeDelta(+1f) },
                enabled = fontSizeSp < ReaderConfig.MAX_FONT_SIZE_SP
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "放大字号",
                    tint = if (fontSizeSp < ReaderConfig.MAX_FONT_SIZE_SP) palette.textColor else palette.secondaryTextColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * 行距调节行组件
 */
@Composable
private fun LineHeightSettingRow(
    currentMultiplier: Float,
    onLineHeightChange: (Float) -> Unit
) {
    val palette = ReaderTheme.readingPalette
    val options = listOf(
        1.3f to "紧凑 (1.3x)",
        1.6f to "标准 (1.6x)",
        2.0f to "宽松 (2.0x)",
        2.4f to "宽裕 (2.4x)"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "行间距倍率",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (multiplier, label) ->
                val isSelected = kotlin.math.abs(currentMultiplier - multiplier) < 0.1f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) palette.primaryAccent.copy(alpha = 0.15f)
                            else palette.surfaceColor
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.primaryAccent else palette.dividerColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onLineHeightChange(multiplier) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) palette.primaryAccent else palette.textColor
                    )
                }
            }
        }
    }
}

/**
 * 字间距与段落间距调节
 */
@Composable
private fun SpacingSettingRow(
    letterSpacingEm: Float,
    paragraphSpacingDp: Float,
    onLetterSpacingChange: (Float) -> Unit,
    onParagraphSpacingChange: (Float) -> Unit
) {
    val palette = ReaderTheme.readingPalette

    Column(modifier = Modifier.fillMaxWidth()) {
        // 字间距
        Text(
            text = "字间距",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val letterOptions = listOf(
            0.00f to "紧凑",
            0.05f to "默认",
            0.10f to "适中",
            0.15f to "宽裕"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            letterOptions.forEach { (em, label) ->
                val isSelected = kotlin.math.abs(letterSpacingEm - em) < 0.02f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) palette.primaryAccent.copy(alpha = 0.15f)
                            else palette.surfaceColor
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.primaryAccent else palette.dividerColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onLetterSpacingChange(em) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) palette.primaryAccent else palette.textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 段间距
        Text(
            text = "段落间距",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val paragraphOptions = listOf(
            6f to "微小",
            12f to "标准",
            20f to "宽松",
            30f to "超大"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paragraphOptions.forEach { (dpVal, label) ->
                val isSelected = kotlin.math.abs(paragraphSpacingDp - dpVal) < 1f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) palette.primaryAccent.copy(alpha = 0.15f)
                            else palette.surfaceColor
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.primaryAccent else palette.dividerColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onParagraphSpacingChange(dpVal) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) palette.primaryAccent else palette.textColor
                    )
                }
            }
        }
    }
}

/**
 * 页面边距调节
 */
@Composable
private fun PaddingSettingRow(
    horizontalPaddingDp: Float,
    onHorizontalPaddingChange: (Float) -> Unit,
    onVerticalPaddingChange: (Float) -> Unit
) {
    val palette = ReaderTheme.readingPalette

    val paddingOptions = listOf(
        Triple(12f, 16f, "窄边距 (沉浸)"),
        Triple(16f, 24f, "标准边距"),
        Triple(24f, 32f, "宽边距 (留白)")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "页面留白边距",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paddingOptions.forEach { (hPadding, vPadding, label) ->
                val isSelected = kotlin.math.abs(horizontalPaddingDp - hPadding) < 1f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) palette.primaryAccent.copy(alpha = 0.15f)
                            else palette.surfaceColor
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.primaryAccent else palette.dividerColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onHorizontalPaddingChange(hPadding)
                            onVerticalPaddingChange(vPadding)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) palette.primaryAccent else palette.textColor
                    )
                }
            }
        }
    }
}
