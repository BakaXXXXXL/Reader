package com.reader.feature.reader

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * 针对 [ReaderViewModel] MVI 状态机与全部用户意图流转的单元测试。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataSource: InMemoryReaderDataSource
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataSource = InMemoryReaderDataSource()
        viewModel = ReaderViewModel(dataSource = dataSource, defaultBookId = 1L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load sets book and chapter details successfully`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.book).isNotNull()
        assertThat(state.currentChapter).isNotNull()
        assertThat(state.currentChapter?.index).isEqualTo(0)
        assertThat(state.chapters).isNotEmpty()
        assertThat(state.currentPageContent).isNotEmpty()
        assertThat(state.currentPageIndex).isEqualTo(0)
        assertThat(state.readerConfig.pageTurnAnimation).isEqualTo(PageTurnAnimation.COVER)
    }

    @Test
    fun `toggle controls flips visibility and dismisses secondary panels when hidden`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 初始状态下控制栏隐藏
        val initialVisible = viewModel.uiState.value.isControlsVisible

        // 翻转控制栏状态
        viewModel.sendIntent(ReaderIntent.ToggleControls)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isControlsVisible).isEqualTo(!initialVisible)

        // 打开排版弹窗
        viewModel.sendIntent(ReaderIntent.SetTypographySheetVisible(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isTypographySheetVisible).isTrue()

        // 再次点击中心收起控制栏，排版弹窗自动一并收起
        viewModel.sendIntent(ReaderIntent.SetControlsVisible(false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isControlsVisible).isFalse()
        assertThat(viewModel.uiState.value.isTypographySheetVisible).isFalse()

        // 打开抽屉：验证抽屉打开且自动收起控制栏
        viewModel.sendIntent(ReaderIntent.SetDrawerOpen(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isDrawerOpen).isTrue()
        assertThat(viewModel.uiState.value.isControlsVisible).isFalse()
    }

    @Test
    fun `page navigation within chapter advances and rewinds page index`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val initialPageIndex = viewModel.uiState.value.currentPageIndex
        assertThat(initialPageIndex).isEqualTo(0)

        // 翻至下一页：如果本章仅1页则翻入下一章第0页，否则翻到第1页
        viewModel.sendIntent(ReaderIntent.NextPage)
        testDispatcher.scheduler.advanceUntilIdle()

        val afterNext = viewModel.uiState.value
        if (afterNext.totalPagesInChapter > 1) {
            assertThat(afterNext.currentPageIndex).isEqualTo(1)
            assertThat(afterNext.currentChapter?.index).isEqualTo(0)

            // 翻回上一页
            viewModel.sendIntent(ReaderIntent.PrevPage)
            testDispatcher.scheduler.advanceUntilIdle()
            assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(0)
        } else {
            // 翻入第1章首页
            assertThat(afterNext.currentChapter?.index).isEqualTo(1)
            assertThat(afterNext.currentPageIndex).isEqualTo(0)

            // 翻回上一章
            viewModel.sendIntent(ReaderIntent.PrevPage)
            testDispatcher.scheduler.advanceUntilIdle()
            assertThat(viewModel.uiState.value.currentChapter?.index).isEqualTo(0)
        }
    }

    @Test
    fun `jump to chapter switches current chapter and resets page index to 0`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.JumpToChapter(2))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.currentChapter?.index).isEqualTo(2)
        assertThat(state.currentPageIndex).isEqualTo(0)
        assertThat(state.isDrawerOpen).isFalse()
    }

    @Test
    fun `prev and next chapter shortcuts navigate across chapters`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 下一章
        viewModel.sendIntent(ReaderIntent.NextChapter)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentChapter?.index).isEqualTo(1)

        // 上一章
        viewModel.sendIntent(ReaderIntent.PrevChapter)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentChapter?.index).isEqualTo(0)
    }

    @Test
    fun `seek to progress updates chapter and progress percentage accurately`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.SeekToProgress(0.5f))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalProgress).isGreaterThan(0.4f)
        assertThat(state.totalProgress).isLessThan(0.7f)
    }

    @Test
    fun `font size adjustments respect minimum and maximum constraints`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 增加 2sp
        val originSize = viewModel.uiState.value.readerConfig.fontSizeSp
        viewModel.sendIntent(ReaderIntent.ChangeFontSize(+2f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.fontSizeSp).isEqualTo(originSize + 2f)

        // 溢出下限保护
        viewModel.sendIntent(ReaderIntent.SetFontSize(5f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.fontSizeSp).isEqualTo(ReaderConfig.MIN_FONT_SIZE_SP)

        // 溢出上限保护
        viewModel.sendIntent(ReaderIntent.SetFontSize(60f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.fontSizeSp).isEqualTo(ReaderConfig.MAX_FONT_SIZE_SP)
    }

    @Test
    fun `line height and paragraph spacing updates persist to configuration`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.SetLineHeight(2.0f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.lineHeightMultiplier).isEqualTo(2.0f)

        viewModel.sendIntent(ReaderIntent.SetParagraphSpacing(24f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.paragraphSpacingDp).isEqualTo(24f)
    }

    @Test
    fun `theme preset selection updates reading palette`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.SelectThemePreset(ReaderThemePreset.GREEN_TEA))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.themePreset).isEqualTo(ReaderThemePreset.GREEN_TEA)
    }

    @Test
    fun `page turn animation mode changes to simulation and continuous scroll`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 切换 3D 仿真
        viewModel.sendIntent(ReaderIntent.SelectPageTurnAnimation(PageTurnAnimation.SIMULATION))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.pageTurnAnimation).isEqualTo(PageTurnAnimation.SIMULATION)

        // 切换垂直滚动
        viewModel.sendIntent(ReaderIntent.SelectPageTurnAnimation(PageTurnAnimation.CONTINUOUS_SCROLL))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.pageTurnAnimation).isEqualTo(PageTurnAnimation.CONTINUOUS_SCROLL)
    }

    @Test
    fun `bookmark adding deleting and jumping flow`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val initialBookmarked = viewModel.uiState.value.isCurrentPageBookmarked

        // 切换书签状态
        viewModel.sendIntent(ReaderIntent.ToggleBookmark)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isCurrentPageBookmarked).isEqualTo(!initialBookmarked)

        // 再次切换书签恢复
        viewModel.sendIntent(ReaderIntent.ToggleBookmark)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isCurrentPageBookmarked).isEqualTo(initialBookmarked)

        // 若有书签，测试显式跳转
        val bookmark = viewModel.uiState.value.bookmarks.firstOrNull()
        if (bookmark != null) {
            viewModel.sendIntent(ReaderIntent.JumpToBookmark(bookmark))
            testDispatcher.scheduler.advanceUntilIdle()
            assertThat(viewModel.uiState.value.currentChapter?.index).isEqualTo(bookmark.chapterIndex)
        }
    }

    @Test
    fun `drawer tab and reversed ordering toggles operate correctly`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 切换至书签 Tab
        viewModel.sendIntent(ReaderIntent.SwitchDrawerTab(DrawerTab.BOOKMARKS))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDrawerTab).isEqualTo(DrawerTab.BOOKMARKS)

        // 切换章节倒序
        assertThat(viewModel.uiState.value.isChaptersReversed).isFalse()
        viewModel.sendIntent(ReaderIntent.ToggleChapterOrder)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.isChaptersReversed).isTrue()
        assertThat(viewModel.uiState.value.displayChapters.first().index).isEqualTo(
            viewModel.uiState.value.chapters.last().index
        )
    }
}
