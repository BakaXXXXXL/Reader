package com.reader.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.core.designsystem.theme.ReaderTheme
import com.reader.core.model.Bookmark
import com.reader.core.model.Chapter
import com.reader.feature.reader.DrawerTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 目录与书签侧边抽屉组件。
 *
 * 包含：
 * 1. 顶部书籍信息与 [DrawerTab] 标签页切换 (目录 / 书签)；
 * 2. 目录页：章节总数指示、正序/逆序快速反转、高亮当前阅读章节、自动滚动定位；
 * 3. 书签页：书签摘录预览、创建时间、快速跳转与书签删除。
 *
 * @param bookTitle 书籍标题
 * @param totalChapters 章节总数
 * @param chapters 展示的章节列表 (根据排序规则处理后)
 * @param currentChapterIndex 当前正在阅读的章节索引
 * @param isReversed 目录是否逆序排列
 * @param selectedTab 当前选中的抽屉 Tab (目录 / 书签)
 * @param bookmarks 书签列表
 * @param onTabSelected 切换 Tab 回调
 * @param onToggleOrderClick 切换正序/倒序回调
 * @param onChapterClick 点击章节跳转回调
 * @param onBookmarkClick 点击书签跳转回调
 * @param onDeleteBookmarkClick 删除书签回调
 */
@Composable
fun ChapterDrawerContent(
    bookTitle: String,
    totalChapters: Int,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    isReversed: Boolean,
    selectedTab: DrawerTab,
    bookmarks: List<Bookmark>,
    onTabSelected: (DrawerTab) -> Unit,
    onToggleOrderClick: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmarkClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ReaderTheme.readingPalette
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        color = palette.surfaceColor,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, bottom = bottomInset)
        ) {
            // 抽屉头部：书名
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "共 $totalChapters 章 · ${bookmarks.size} 条书签",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryTextColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // TabRow 切换 (目录 / 书签)
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = palette.surfaceColor,
                contentColor = palette.primaryAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = palette.primaryAccent
                    )
                },
                divider = {
                    HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.5f))
                }
            ) {
                Tab(
                    selected = selectedTab == DrawerTab.CHAPTERS,
                    onClick = { onTabSelected(DrawerTab.CHAPTERS) },
                    text = {
                        Text(
                            text = "目录",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selectedTab == DrawerTab.CHAPTERS) palette.primaryAccent else palette.secondaryTextColor
                        )
                    }
                )
                Tab(
                    selected = selectedTab == DrawerTab.BOOKMARKS,
                    onClick = { onTabSelected(DrawerTab.BOOKMARKS) },
                    text = {
                        Text(
                            text = "书签 (${bookmarks.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selectedTab == DrawerTab.BOOKMARKS) palette.primaryAccent else palette.secondaryTextColor
                        )
                    }
                )
            }

            when (selectedTab) {
                DrawerTab.CHAPTERS -> {
                    ChaptersListSection(
                        chapters = chapters,
                        currentChapterIndex = currentChapterIndex,
                        isReversed = isReversed,
                        onToggleOrderClick = onToggleOrderClick,
                        onChapterClick = onChapterClick
                    )
                }
                DrawerTab.BOOKMARKS -> {
                    BookmarksListSection(
                        bookmarks = bookmarks,
                        onBookmarkClick = onBookmarkClick,
                        onDeleteBookmarkClick = onDeleteBookmarkClick
                    )
                }
            }
        }
    }
}

/**
 * 章节目录列表视图
 */
@Composable
private fun ChaptersListSection(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    isReversed: Boolean,
    onToggleOrderClick: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ReaderTheme.readingPalette
    val listState = rememberLazyListState()

    // 当展开抽屉时，自动滚动到当前阅读章节
    LaunchedEffect(currentChapterIndex, isReversed) {
        val targetIndex = if (isReversed) {
            chapters.indexOfFirst { it.index == currentChapterIndex }
        } else {
            currentChapterIndex
        }
        if (targetIndex in chapters.indices) {
            listState.scrollToItem((targetIndex - 2).coerceAtLeast(0))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 章节排序工具条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "正文卷 (${chapters.size} 章)",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryTextColor
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleOrderClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "正序/倒序",
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isReversed) "倒序" else "正序",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryAccent
                )
            }
        }

        HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)

        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无目录信息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryTextColor
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = chapters,
                    key = { _, chapter -> chapter.id.takeIf { it != 0L } ?: chapter.index.toLong() }
                ) { _, chapter ->
                    val isCurrent = chapter.index == currentChapterIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(chapter) }
                            .background(
                                if (isCurrent) palette.primaryAccent.copy(alpha = 0.12f)
                                else palette.surfaceColor
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isCurrent) palette.primaryAccent else palette.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "当前阅读",
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = palette.dividerColor.copy(alpha = 0.15f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 书签列表视图
 */
@Composable
private fun BookmarksListSection(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmarkClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ReaderTheme.readingPalette
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    if (bookmarks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = palette.secondaryTextColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "暂无书签",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.secondaryTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "在阅读时点击右上角书签图标可随时记录精彩位置",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryTextColor.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            itemsIndexed(
                items = bookmarks,
                key = { _, bookmark -> bookmark.id }
            ) { _, bookmark ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookmarkClick(bookmark) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bookmark.chapterTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.primaryAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bookmark.previewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateFormat.format(Date(bookmark.createTime)),
                            fontSize = 10.sp,
                            color = palette.secondaryTextColor.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = { onDeleteBookmarkClick(bookmark.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除书签",
                            tint = palette.secondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = palette.dividerColor.copy(alpha = 0.2f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
