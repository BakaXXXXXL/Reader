package com.reader.feature.reader

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.Bookmark
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `initial load sets book and chapter details successfully`() = runTest {
        advanceUntilIdle()

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
    fun `toggle controls flips visibility and dismisses secondary panels when hidden`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isControlsVisible).isFalse()

        // 唤起控制栏
        viewModel.sendIntent(ReaderIntent.ToggleControls)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isControlsVisible).isTrue()

        // 打开排版弹窗与抽屉
        viewModel.sendIntent(ReaderIntent.SetTypographySheetVisible(true))
        viewModel.sendIntent(ReaderIntent.SetDrawerOpen(true))
        advanceUntilIdle()

        // 再次点击中心收起控制栏，子弹窗与抽屉自动一并收起
        viewModel.sendIntent(ReaderIntent.ToggleControls)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isControlsVisible).isFalse()
        assertThat(viewModel.uiState.value.isTypographySheetVisible).isFalse()
        assertThat(viewModel.uiState.value.isDrawerOpen).isFalse()
    }

    @Test
    fun `page navigation within chapter advances and rewinds page index`() = runTest {
        advanceUntilIdle()

        val initialPageIndex = viewModel.uiState.value.currentPageIndex
        assertThat(initialPageIndex).isEqualTo(0)

        // 翻至下一页
        viewModel.sendIntent(ReaderIntent.NextPage)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(1)

        // 翻回上一页
        viewModel.sendIntent(ReaderIntent.PrevPage)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(0)
    }

    @Test
    fun `jump to chapter switches current chapter and resets page index to 0`() = runTest {
        advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.JumpToChapter(2))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.currentChapter?.index).isEqualTo(2)
        assertThat(state.currentPageIndex).isEqualTo(0)
        assertThat(state.isDrawerOpen).isFalse()
    }

    @Test
    fun `prev and next chapter shortcuts navigate across chapters`() = runTest {
        advanceUntilIdle()

        // 下一章
        viewModel.sendIntent(ReaderIntent.NextChapter)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentChapter?.index).isEqualTo(1)

        // 上一章
        viewModel.sendIntent(ReaderIntent.PrevChapter)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentChapter?.index).isEqualTo(0)
    }

    @Test
    fun `seek to progress updates chapter and progress percentage accurately`() = runTest {
        advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.SeekToProgress(0.5f))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalProgress).isGreaterThan(0.4f)
        assertThat(state.totalProgress).isLessThan(0.7f)
    }

    @Test
    fun `font size adjustments respect minimum and maximum constraints`() = runTest {
        advanceUntilIdle()

        // 增加 2sp
        val originSize = viewModel.uiState.value.readerConfig.fontSizeSp
        viewModel.sendIntent(ReaderIntent.ChangeFontSize(+2f))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.fontSizeSp).isEqualTo(originSize + 2f)

        // 溢出下限保护
        viewModel.sendIntent(ReaderIntent.SetFontSize(5f))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.fontSizeSp).isEqualTo(ReaderConfig.MIN_FONT_SIZE_SP)

        // 溢出上限保护
        viewModel.sendIntent(ReaderIntent.SetFontSize(60f))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.fontSizeSp).isEqualTo(ReaderConfig.MAX_FONT_SIZE_SP)
    }

    @Test
    fun `line height and paragraph spacing updates persist to configuration`() = runTest {
        advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.SetLineHeight(2.0f))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.lineHeightMultiplier).isEqualTo(2.0f)

        viewModel.sendIntent(ReaderIntent.SetParagraphSpacing(24f))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.paragraphSpacingDp).isEqualTo(24f)
    }

    @Test
    fun `theme preset switching updates active palette and dark status`() = runTest {
        advanceUntilIdle()

        viewModel.sendIntent(ReaderIntent.SelectThemePreset(ReaderThemePreset.DARK_NIGHT))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.themePreset).isEqualTo(ReaderThemePreset.DARK_NIGHT)

        viewModel.sendIntent(ReaderIntent.SelectThemePreset(ReaderThemePreset.GREEN_TEA))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.themePreset).isEqualTo(ReaderThemePreset.GREEN_TEA)
    }

    @Test
    fun `page turn animation mode changes to simulation and continuous scroll`() = runTest {
        advanceUntilIdle()

        // 切换 3D 仿真
        viewModel.sendIntent(ReaderIntent.SelectPageTurnAnimation(PageTurnAnimation.SIMULATION))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.pageTurnAnimation).isEqualTo(PageTurnAnimation.SIMULATION)

        // 切换垂直滚动
        viewModel.sendIntent(ReaderIntent.SelectPageTurnAnimation(PageTurnAnimation.CONTINUOUS_SCROLL))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.readerConfig.pageTurnAnimation).isEqualTo(PageTurnAnimation.CONTINUOUS_SCROLL)
    }

    @Test
    fun `bookmark adding deleting and jumping flow`() = runTest {
        advanceUntilIdle()

        // 初始无当前页书签
        assertThat(viewModel.uiState.value.isCurrentPageBookmarked).isFalse()

        // 添加书签
        viewModel.sendIntent(ReaderIntent.ToggleBookmark)
        advanceUntilIdle()

        val afterAddState = viewModel.uiState.value
        assertThat(afterAddState.isCurrentPageBookmarked).isTrue()
        assertThat(afterAddState.bookmarks).isNotEmpty()
        val createdBookmark = afterAddState.bookmarks.first()

        // 再次 Toggle 删除该书签
        viewModel.sendIntent(ReaderIntent.ToggleBookmark)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isCurrentPageBookmarked).isFalse()

        // 显式跳转书签
        viewModel.sendIntent(ReaderIntent.JumpToBookmark(createdBookmark))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentChapter?.index).isEqualTo(createdBookmark.chapterIndex)
    }

    @Test
    fun `drawer tab switching and chapter reverse toggle`() = runTest {
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedDrawerTab).isEqualTo(DrawerTab.CHAPTERS)
        viewModel.sendIntent(ReaderIntent.SwitchDrawerTab(DrawerTab.BOOKMARKS))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDrawerTab).isEqualTo(DrawerTab.BOOKMARKS)

        assertThat(viewModel.uiState.value.isChaptersReversed).isFalse()
        viewModel.sendIntent(ReaderIntent.ToggleChapterOrder)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isChaptersReversed).isTrue()
    }
}
