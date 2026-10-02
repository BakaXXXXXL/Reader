package com.reader.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reader.core.designsystem.component.ReaderErrorStateView
import com.reader.core.designsystem.component.ReaderLoadingIndicator
import com.reader.core.designsystem.theme.ReaderTheme
import com.reader.core.designsystem.theme.ReadingThemeType
import com.reader.core.designsystem.util.KeepScreenOnEffect
import com.reader.core.designsystem.util.rememberSystemBarController
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderThemePreset
import com.reader.feature.reader.component.ChapterDrawerContent
import com.reader.feature.reader.component.ReaderBottomBar
import com.reader.feature.reader.component.ReaderTopBar
import com.reader.feature.reader.component.TypographySettingBottomSheet
import kotlinx.coroutines.launch

/**
 * 转换阅读配置预设至 DesignSystem 统一主题类型
 */
fun ReaderThemePreset.toReadingThemeType(): ReadingThemeType {
    return when (this) {
        ReaderThemePreset.DEFAULT_LIGHT -> ReadingThemeType.DEFAULT_LIGHT
        ReaderThemePreset.PARCHMENT -> ReadingThemeType.PARCHMENT
        ReaderThemePreset.GREEN_TEA -> ReadingThemeType.EYE_CARE_GREEN
        ReaderThemePreset.E_INK -> ReadingThemeType.E_INK
        ReaderThemePreset.DARK_NIGHT -> ReadingThemeType.OLED_BLACK
    }
}

/**
 * 阅读器沉浸全屏交互主屏幕 (ReaderScreen - Stateful Entry).
 *
 * @param onBackClick 返回书架或上一级页面导航回调
 * @param viewModel 阅读器 MVI 状态机 ViewModel
 */
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReaderScreenContent(
        uiState = uiState,
        onIntent = viewModel::sendIntent,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

/**
 * 阅读器沉浸式无状态展示组件 (ReaderScreenContent - Stateless).
 *
 * 聚合了：
 * 1. Android 15 Edge-to-Edge 沉浸全屏控制 (状态栏/导航栏随控制栏可见性动态显示/沉浸)；
 * 2. 屏幕常亮保持控制 [KeepScreenOnEffect]；
 * 3. 屏幕中心点击唤起/收起控制栏，两侧点击或水平滑动触控翻页；
 * 4. 浮层沉浸式顶部栏 [ReaderTopBar]；
 * 5. 浮层沉浸式底部进度与快捷菜单栏 [ReaderBottomBar]；
 * 6. 侧边目录/书签抽屉 [ChapterDrawerContent]；
 * 7. 底部排版设置弹窗 [TypographySettingBottomSheet]。
 */
@Composable
fun ReaderScreenContent(
    uiState: ReaderUiState,
    onIntent: (ReaderIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeType = uiState.readerConfig.themePreset.toReadingThemeType()
    val systemBarController = rememberSystemBarController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(
        initialValue = if (uiState.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed
    )
    val snackbarHostState = remember { SnackbarHostState() }

    // 屏幕常亮跟随配置开关
    KeepScreenOnEffect(enabled = uiState.readerConfig.keepScreenOn)

    // 沉浸模式同步：控制栏可见时展示系统状态栏与导航栏；沉浸阅读时完全隐藏进入全屏沉浸
    LaunchedEffect(uiState.isControlsVisible) {
        systemBarController.setImmersiveMode(!uiState.isControlsVisible)
    }

    // 抽屉状态双向同步
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen && drawerState.isClosed) {
            drawerState.open()
        } else if (!uiState.isDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        val isOpen = drawerState.isOpen
        if (isOpen != uiState.isDrawerOpen) {
            onIntent(ReaderIntent.SetDrawerOpen(isOpen))
        }
    }

    // 错误信息提示
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onIntent(ReaderIntent.ClearError)
        }
    }

    ReaderTheme(readingTheme = themeType) {
        val palette = ReaderTheme.readingPalette

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ChapterDrawerContent(
                    bookTitle = uiState.bookTitleDisplay,
                    totalChapters = uiState.chapters.size,
                    chapters = uiState.displayChapters,
                    currentChapterIndex = uiState.currentChapter?.index ?: 0,
                    isReversed = uiState.isChaptersReversed,
                    selectedTab = uiState.selectedDrawerTab,
                    bookmarks = uiState.bookmarks,
                    onTabSelected = { onIntent(ReaderIntent.SwitchDrawerTab(it)) },
                    onToggleOrderClick = { onIntent(ReaderIntent.ToggleChapterOrder) },
                    onChapterClick = { chapter ->
                        onIntent(ReaderIntent.JumpToChapter(chapter.index))
                        scope.launch { drawerState.close() }
                    },
                    onBookmarkClick = { bookmark ->
                        onIntent(ReaderIntent.JumpToBookmark(bookmark))
                        scope.launch { drawerState.close() }
                    },
                    onDeleteBookmarkClick = { onIntent(ReaderIntent.DeleteBookmark(it)) }
                )
            },
            gesturesEnabled = !uiState.isControlsVisible,
            modifier = modifier.fillMaxSize()
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = palette.canvasBackground,
                contentColor = palette.textColor
            ) { scaffoldPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.canvasBackground)
                        .padding(scaffoldPadding)
                ) {
                    if (uiState.isLoading) {
                        ReaderLoadingIndicator(
                            message = "正在排版载入...",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (uiState.book == null && uiState.errorMessage != null) {
                        ReaderErrorStateView(
                            title = "加载书籍失败",
                            errorMessage = uiState.errorMessage,
                            onRetry = {
                                onIntent(ReaderIntent.LoadBook(1L))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 1. 核心正文阅读渲染页面 (响应三区触控与滑动手势)
                        ReaderPageCanvas(
                            uiState = uiState,
                            onCenterTap = { onIntent(ReaderIntent.ToggleControls) },
                            onPrevPage = { onIntent(ReaderIntent.PrevPage) },
                            onNextPage = { onIntent(ReaderIntent.NextPage) },
                            modifier = Modifier.fillMaxSize()
                        )

                        // 2. 沉浸式顶部栏
                        ReaderTopBar(
                            visible = uiState.isControlsVisible,
                            title = uiState.chapterTitleDisplay,
                            subtitle = uiState.bookTitleDisplay,
                            isBookmarked = uiState.isCurrentPageBookmarked,
                            onBackClick = onBackClick,
                            onBookmarkClick = { onIntent(ReaderIntent.ToggleBookmark) },
                            onTypographyClick = { onIntent(ReaderIntent.SetTypographySheetVisible(true)) },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                        // 3. 沉浸式底部栏
                        ReaderBottomBar(
                            visible = uiState.isControlsVisible,
                            currentProgress = uiState.totalProgress,
                            chapterProgress = uiState.chapterProgress,
                            totalProgressPercent = uiState.totalProgressPercent,
                            pageIndicatorText = uiState.pageIndicatorText,
                            hasPrevChapter = uiState.hasPrevChapter,
                            hasNextChapter = uiState.hasNextChapter,
                            onProgressChange = { progress ->
                                // 拖拽中可选提供触感反馈
                            },
                            onProgressChangeFinished = { progress ->
                                onIntent(ReaderIntent.SeekToProgress(progress))
                            },
                            onPrevChapterClick = { onIntent(ReaderIntent.PrevChapter) },
                            onNextChapterClick = { onIntent(ReaderIntent.NextChapter) },
                            onMenuDrawerClick = {
                                onIntent(ReaderIntent.SetDrawerOpen(true))
                            },
                            onTypographyClick = {
                                onIntent(ReaderIntent.SetTypographySheetVisible(true))
                            },
                            onToggleNightModeClick = {
                                val nextPreset = if (palette.isDark) {
                                    ReaderThemePreset.DEFAULT_LIGHT
                                } else {
                                    ReaderThemePreset.DARK_NIGHT
                                }
                                onIntent(ReaderIntent.SelectThemePreset(nextPreset))
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        // 4. 排版设置底部弹窗
                        if (uiState.isTypographySheetVisible) {
                            TypographySettingBottomSheet(
                                config = uiState.readerConfig,
                                onFontSizeChangeDelta = { onIntent(ReaderIntent.ChangeFontSize(it)) },
                                onFontSizeChange = { onIntent(ReaderIntent.SetFontSize(it)) },
                                onLineHeightChange = { onIntent(ReaderIntent.SetLineHeight(it)) },
                                onLetterSpacingChange = { onIntent(ReaderIntent.SetLetterSpacing(it)) },
                                onParagraphSpacingChange = { onIntent(ReaderIntent.SetParagraphSpacing(it)) },
                                onHorizontalPaddingChange = { onIntent(ReaderIntent.SetHorizontalPadding(it)) },
                                onVerticalPaddingChange = { onIntent(ReaderIntent.SetVerticalPadding(it)) },
                                onThemePresetSelected = { onIntent(ReaderIntent.SelectThemePreset(it)) },
                                onPageTurnAnimationSelected = { onIntent(ReaderIntent.SelectPageTurnAnimation(it)) },
                                onKeepScreenOnChange = { onIntent(ReaderIntent.SetKeepScreenOn(it)) },
                                onVolumeKeyPageTurnChange = { onIntent(ReaderIntent.SetVolumeKeyPageTurn(it)) },
                                onDismissRequest = { onIntent(ReaderIntent.SetTypographySheetVisible(false)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 核心正文阅读展示容器。
 *
 * 实现了沉浸式手势与触控分区逻辑：
 * - 屏幕左侧 25% 区域点击：上一页；
 * - 屏幕右侧 25% 区域点击：下一页；
 * - 屏幕中央 50% 区域点击：唤醒或隐藏沉浸控制栏；
 * - 水平左右滑动手势检测：左滑翻入下一页，右滑翻入上一页。
 *
 * 同时严格基于 [ReaderConfig] 进行实时排版呈现：
 * 字号、行高倍率、段落间距、字间距、内边距与页眉页脚。
 */
@Composable
private fun ReaderPageCanvas(
    uiState: ReaderUiState,
    onCenterTap: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ReaderTheme.readingPalette
    val config = uiState.readerConfig

    val topSafeInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomSafeInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var dragAmountTotal by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasBackground)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val screenWidth = size.width
                    val leftThreshold = screenWidth * 0.25f
                    val rightThreshold = screenWidth * 0.75f

                    when {
                        offset.x < leftThreshold -> {
                            onPrevPage()
                        }
                        offset.x > rightThreshold -> {
                            onNextPage()
                        }
                        else -> {
                            onCenterTap()
                        }
                    }
                }
            }
            .pointerInput(config.pageTurnAnimation) {
                // 如果不是垂直滚动模式，捕获水平滑动翻页
                if (config.pageTurnAnimation != PageTurnAnimation.CONTINUOUS_SCROLL) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAmountTotal = 0f },
                        onDragEnd = {
                            if (dragAmountTotal > 60f) {
                                onPrevPage()
                            } else if (dragAmountTotal < -60f) {
                                onNextPage()
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAmountTotal += dragAmount
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = config.pagePaddingHorizontalDp.dp,
                    end = config.pagePaddingHorizontalDp.dp,
                    top = (config.pagePaddingVerticalDp.dp + topSafeInset),
                    bottom = (config.pagePaddingVerticalDp.dp + bottomSafeInset)
                )
        ) {
            // 页眉：章节名称
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.chapterTitleDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.headerFooterColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // 正文内容区域
            val scrollState = rememberScrollState()
            val textContentModifier = if (config.pageTurnAnimation == PageTurnAnimation.CONTINUOUS_SCROLL) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            } else {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            }

            Box(
                modifier = textContentModifier,
                contentAlignment = Alignment.TopStart
            ) {
                val paragraphs = remember(uiState.currentPageContent) {
                    uiState.currentPageContent.split("\n").filter { it.isNotBlank() }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(config.paragraphSpacingDp.dp)
                ) {
                    paragraphs.forEach { para ->
                        Text(
                            text = para,
                            style = TextStyle(
                                fontSize = config.fontSizeSp.sp,
                                lineHeight = (config.fontSizeSp * config.lineHeightMultiplier).sp,
                                letterSpacing = config.letterSpacingEm.em,
                                color = palette.textColor,
                                textAlign = TextAlign.Justify,
                                fontFamily = FontFamily.Default
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 页脚：全书进度百分比与本章页码
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.totalProgressPercent,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.headerFooterColor
                )
                Text(
                    text = uiState.pageIndicatorText,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.headerFooterColor
                )
            }
        }
    }
}
