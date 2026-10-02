package com.reader.feature.reader.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reader.core.designsystem.theme.ReaderTheme

/**
 * 阅读器沉浸式顶部控制栏。
 *
 * 具有向上滑出/向下滑入的流畅过渡动效，集成了返回书架、章节名、当前页书签添加/取消与排版设置入口。
 *
 * @param visible 是否可见
 * @param title 章节名称
 * @param subtitle 书籍名称
 * @param isBookmarked 当前页是否已被标记为书签
 * @param onBackClick 点击返回回调
 * @param onBookmarkClick 点击书签按钮回调
 * @param onTypographyClick 点击排版设置按钮回调
 * @param onMoreClick 点击更多按钮回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    visible: Boolean,
    title: String,
    subtitle: String,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTypographyClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null
) {
    val palette = ReaderTheme.readingPalette
    val topInsetPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = palette.surfaceColor.copy(alpha = 0.96f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInsetPadding, start = 8.dp, end = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：返回箭头
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回书架",
                        tint = palette.textColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 中间：章节标题与书名
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 右侧功能按钮区：书签、排版设置、更多
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 书签按钮
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isBookmarked) "移除书签" else "添加书签",
                            tint = if (isBookmarked) palette.primaryAccent else palette.secondaryTextColor
                        )
                    }

                    // 排版设置按钮
                    IconButton(onClick = onTypographyClick) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = "排版设置",
                            tint = palette.textColor
                        )
                    }

                    // 更多选项按钮 (可选)
                    if (onMoreClick != null) {
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "更多设置",
                                tint = palette.textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
