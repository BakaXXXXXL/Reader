package com.reader.feature.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlin.math.abs

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
 * @param bookId 当前阅读书籍的主键 ID
 * @param onBackClick 返回书架或上一级页面导航回调
 */
@Composable
fun ReaderScreen(
    bookId: Long = 1L,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember(bookId) {
        ReaderViewModel.create(bookId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReaderScreenContent(
        uiState = uiState,
        onIntent = viewModel::sendIntent,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

/**
 * 阅读器沉浸式展示组件 (ReaderScreenContent - Stateless).
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

    // 沉浸模式同步
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

    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && uiState.isDrawerOpen) {
            onIntent(ReaderIntent.SetDrawerOpen(false))
        }
    }

    // 错误状态 Snackbar 提示
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
            gesturesEnabled = !uiState.isControlsVisible,
            drawerContent = {
                ChapterDrawerContent(
                    bookTitle = uiState.bookTitleDisplay,
                    totalChapters = uiState.chapters.size,
                    chapters = uiState.displayChapters,
                    currentChapterIndex = uiState.currentChapter?.index ?: 0,
                    isReversed = uiState.isChaptersReversed,
                    selectedTab = uiState.selectedDrawerTab,
                    bookmarks = uiState.bookmarks,
                    onTabSelected = { tab ->
                        onIntent(ReaderIntent.SwitchDrawerTab(tab))
                    },
                    onToggleOrderClick = {
                        onIntent(ReaderIntent.ToggleChapterOrder)
                    },
                    onChapterClick = { chapter ->
                        scope.launch { drawerState.close() }
                        onIntent(ReaderIntent.JumpToChapter(chapter.index))
                    },
                    onBookmarkClick = { bookmark ->
                        scope.launch { drawerState.close() }
                        onIntent(ReaderIntent.JumpToBookmark(bookmark))
                    },
                    onDeleteBookmarkClick = { bookmarkId ->
                        onIntent(ReaderIntent.DeleteBookmark(bookmarkId))
                    }
                )
            },
            modifier = modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = palette.canvasBackground
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
                        // 1. 核心正文阅读渲染页面 (响应精准三区触控与滑动手势，实时排版)
                        ReaderPageCanvas(
                            uiState = uiState,
                            onIntent = onIntent,
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
                            onProgressChange = { _ -> },
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
 * 具备防误触热区划分与滑动手势互斥：
 * - 菜单呼出热区：严格限定为屏幕正中央矩形 (水平 35%..65%，垂直 35%..65%)；
 * - 左右侧与上下边沿轻触：右侧/下方翻下一页，左侧/上方翻上一页；
 * - 滑动手势检测：水平拖动超过 25px 触发翻页，绝对杜绝误触菜单！
 * - 多模式翻页动画：Cover 覆盖、Slide 平移、Simulation 3D 立体折页、Continuous Scroll 垂直滚动、None 瞬间切页。
 */
@Composable
private fun ReaderPageCanvas(
    uiState: ReaderUiState,
    onIntent: (ReaderIntent) -> Unit,
    onCenterTap: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ReaderTheme.readingPalette
    val config = uiState.readerConfig
    val density = LocalDensity.current

    val topSafeInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomSafeInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasBackground)
            .pointerInput(config.pageTurnAnimation) {
                val screenWidth = size.width.toFloat()
                val screenHeight = size.height.toFloat()

                // 核心菜单呼出区：仅限定在屏幕正中央小矩形 (水平 35%..65%，垂直 35%..65%)
                val menuMinX = screenWidth * 0.35f
                val menuMaxX = screenWidth * 0.65f
                val menuMinY = screenHeight * 0.35f
                val menuMaxY = screenHeight * 0.65f

                val prevZoneX = screenWidth * 0.35f
                val nextZoneX = screenWidth * 0.65f

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var isDrag = false

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            totalDragX += dx
                            totalDragY += dy
                            if ((abs(totalDragX) > 16f || abs(totalDragY) > 16f) &&
                                config.pageTurnAnimation != PageTurnAnimation.CONTINUOUS_SCROLL
                            ) {
                                isDrag = true
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (isDrag) {
                        // 滑动手势：水平拖动超过 25px 触发翻页，绝对不触发菜单！
                        if (totalDragX < -25f) {
                            onNextPage()
                        } else if (totalDragX > 25f) {
                            onPrevPage()
                        }
                    } else {
                        // 单击轻触判定
                        val tapX = down.position.x
                        val tapY = down.position.y

                        val isCenterTap = (tapX in menuMinX..menuMaxX) && (tapY in menuMinY..menuMaxY)

                        if (isCenterTap) {
                            onCenterTap()
                        } else if (tapX > nextZoneX) {
                            onNextPage()
                        } else if (tapX < prevZoneX) {
                            onPrevPage()
                        } else {
                            // 落在中央列但非正中央 (如顶部或底部)：下半屏翻下一页，上半屏翻上一页，杜绝误触菜单
                            if (tapY > menuMaxY) {
                                onNextPage()
                            } else {
                                onPrevPage()
                            }
                        }
                    }
                }
            }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // 根据真实屏幕视口尺寸驱动排版引擎
        LaunchedEffect(widthPx, heightPx) {
            if (widthPx > 100f && heightPx > 100f) {
                onIntent(ReaderIntent.UpdateViewport(widthPx, heightPx))
            }
        }

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
            val textContentModifier = Modifier
                .weight(1f)
                .fillMaxWidth()

            if (config.pageTurnAnimation == PageTurnAnimation.CONTINUOUS_SCROLL) {
                // 垂直无缝滚动模式
                val scrollState = rememberScrollState()
                Column(
                    modifier = textContentModifier.verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(config.paragraphSpacingDp.dp)
                ) {
                    val paragraphs = remember(uiState.fullChapterText) {
                        uiState.fullChapterText.split("\n").filter { it.isNotBlank() }
                    }
                    paragraphs.forEach { para ->
                        Text(
                            text = if (config.firstLineIndentSpaces > 0) "　　${para.trimStart()}" else para,
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
            } else {
                // 翻页模式：支持 Cover 覆盖、Slide 平移、Simulation 3D 立体折页、None 无动画
                AnimatedContent(
                    targetState = uiState.currentPageIndex to (uiState.currentChapter?.index ?: 0),
                    transitionSpec = {
                        val isForward = if (targetState.second != initialState.second) {
                            (targetState.second ?: 0) > (initialState.second ?: 0)
                        } else {
                            targetState.first >= initialState.first
                        }

                        val duration = 240
                        val animSpec = tween<IntOffset>(durationMillis = duration, easing = FastOutSlowInEasing)

                        when (config.pageTurnAnimation) {
                            PageTurnAnimation.COVER -> {
                                // 1. 平滑覆盖模式：上层书页滑入覆盖，下层微移视差（-15%），零淡入淡出，犹如物理纸张层叠
                                if (isForward) {
                                    slideInHorizontally(animSpec) { width -> width }.togetherWith(
                                        slideOutHorizontally(animSpec) { width -> -width / 6 }
                                    )
                                } else {
                                    slideInHorizontally(animSpec) { width -> -width / 6 }.togetherWith(
                                        slideOutHorizontally(animSpec) { width -> width }
                                    )
                                }
                            }

                            PageTurnAnimation.SLIDE -> {
                                // 2. 平移滑动模式：两页等速 1:1 紧贴平移滑动
                                if (isForward) {
                                    slideInHorizontally(animSpec) { width -> width }.togetherWith(
                                        slideOutHorizontally(animSpec) { width -> -width }
                                    )
                                } else {
                                    slideInHorizontally(animSpec) { width -> -width }.togetherWith(
                                        slideOutHorizontally(animSpec) { width -> width }
                                    )
                                }
                            }

                            PageTurnAnimation.SIMULATION -> {
                                // 3. 拟真 3D 仿真模式：带有深度缩放与立体透视折页动效
                                val simSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
                                if (isForward) {
                                    (slideInHorizontally(animSpec) { width -> width / 2 } +
                                     scaleIn(simSpec, initialScale = 0.90f)).togetherWith(
                                        slideOutHorizontally(animSpec) { width -> -width } +
                                        scaleOut(simSpec, targetScale = 0.95f)
                                    )
                                } else {
                                    (slideInHorizontally(animSpec) { width -> -width } +
                                     scaleIn(simSpec, initialScale = 0.95f)).togetherWith(
                                        slideOutHorizontally(animSpec) { width -> width / 2 } +
                                        scaleOut(simSpec, targetScale = 0.90f)
                                    )
                                }
                            }

                            PageTurnAnimation.NONE -> {
                                // 4. 无动画模式：瞬间 0ms 切页，专为水墨屏或极速阅读打造
                                EnterTransition.None togetherWith ExitTransition.None
                            }

                            PageTurnAnimation.CONTINUOUS_SCROLL -> {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        }
                    },
                    label = "PageContentTransition",
                    modifier = textContentModifier
                ) { _ ->
                    val pageModifier = if (config.pageTurnAnimation == PageTurnAnimation.COVER) {
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = 4.dp, shape = RectangleShape)
                            .background(palette.canvasBackground)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    Column(
                        modifier = pageModifier,
                        verticalArrangement = Arrangement.spacedBy(config.paragraphSpacingDp.dp)
                    ) {
                        val lines = uiState.currentPaginatedLines
                        if (lines.isNotEmpty()) {
                            lines.forEach { line ->
                                Text(
                                    text = line,
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
                        } else {
                            val pageParas = uiState.currentPageContent.split("\n").filter { it.isNotBlank() }
                            pageParas.forEach { para ->
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
