package com.reader.feature.reader.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.core.designsystem.theme.ReaderTheme

/**
 * 阅读器沉浸式底部控制栏。
 *
 * 包含：
 * 1. 进度显示与跨章节跳转区 (上一章、进度条 Slider、下一章)；
 * 2. 进度细节统计 (本章进度百分比、全书进度百分比、章节页码指示)；
 * 3. 底部导航栏快捷入口 (目录抽屉、排版设置、夜间模式快速切换)。
 *
 * @param visible 是否可见
 * @param currentProgress 全书阅读进度 (0.0f .. 1.0f)
 * @param chapterProgress 本章阅读进度 (0.0f .. 1.0f)
 * @param totalProgressPercent 全书进度格式化字符串 (如 "35.8%")
 * @param pageIndicatorText 当前章节页码文本 (如 "3 / 15")
 * @param hasPrevChapter 是否可跳转上一章
 * @param hasNextChapter 是否可跳转下一章
 * @param onProgressChange 进度滑动过程触发
 * @param onProgressChangeFinished 进度滑动结束提交跳转
 * @param onPrevChapterClick 点击上一章
 * @param onNextChapterClick 点击下一章
 * @param onMenuDrawerClick 点击目录按钮打开抽屉
 * @param onTypographyClick 点击排版设置按钮
 * @param onToggleNightModeClick 点击快捷切换夜间/日间模式
 */
@Composable
fun ReaderBottomBar(
    visible: Boolean,
    currentProgress: Float,
    chapterProgress: Float,
    totalProgressPercent: String,
    pageIndicatorText: String,
    hasPrevChapter: Boolean,
    hasNextChapter: Boolean,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (Float) -> Unit,
    onPrevChapterClick: () -> Unit,
    onNextChapterClick: () -> Unit,
    onMenuDrawerClick: () -> Unit,
    onTypographyClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleNightModeClick: (() -> Unit)? = null
) {
    val palette = ReaderTheme.readingPalette
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var sliderPosition by remember(currentProgress) { mutableFloatStateOf(currentProgress) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = palette.surfaceColor.copy(alpha = 0.96f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomInset)
            ) {
                // 第一行：进度概览文本
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "本章: $pageIndicatorText",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryTextColor
                    )
                    Text(
                        text = "全书: $totalProgressPercent",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primaryAccent
                    )
                }

                // 第二行：上一章 + 进度滑块 + 下一章
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevChapterClick,
                        enabled = hasPrevChapter
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "上一章",
                            tint = if (hasPrevChapter) palette.textColor else palette.secondaryTextColor.copy(alpha = 0.4f)
                        )
                    }

                    Slider(
                        value = if (isDraggingSlider) sliderPosition else currentProgress,
                        onValueChange = { newValue ->
                            isDraggingSlider = true
                            sliderPosition = newValue
                            onProgressChange(newValue)
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onProgressChangeFinished(sliderPosition)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primaryAccent,
                            activeTrackColor = palette.primaryAccent,
                            inactiveTrackColor = palette.secondaryTextColor.copy(alpha = 0.3f)
                        )
                    )

                    IconButton(
                        onClick = onNextChapterClick,
                        enabled = hasNextChapter
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "下一章",
                            tint = if (hasNextChapter) palette.textColor else palette.secondaryTextColor.copy(alpha = 0.4f)
                        )
                    }
                }

                HorizontalDivider(
                    color = palette.dividerColor.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // 第三行：底部操作栏 (目录、夜间模式、排版)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 目录抽屉
                    BottomActionItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "目录",
                                tint = palette.textColor
                            )
                        },
                        label = "目录",
                        onClick = onMenuDrawerClick
                    )

                    // 夜间/日间模式切换
                    if (onToggleNightModeClick != null) {
                        BottomActionItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Brightness4,
                                    contentDescription = "模式",
                                    tint = palette.textColor
                                )
                            },
                            label = if (palette.isDark) "日间" else "夜间",
                            onClick = onToggleNightModeClick
                        )
                    }

                    // 排版设置
                    BottomActionItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "排版",
                                tint = palette.textColor
                            )
                        },
                        label = "排版",
                        onClick = onTypographyClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ReaderTheme.readingPalette

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            icon()
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = palette.secondaryTextColor,
            fontWeight = FontWeight.Normal
        )
    }
}
