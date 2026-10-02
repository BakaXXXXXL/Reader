package com.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.core.model.Bookmark
import com.reader.core.model.Chapter
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 阅读器核心状态机 ViewModel (MVI 架构)。
 *
 * 严格遵从单向数据流原则：通过 [sendIntent] 接收用户交互事件，
 * 在后台驱动状态机流转，并统一向表现层 Composable 单向暴露不可变 [uiState]。
 *
 * @param dataSource 阅读器领域数据源接口，解耦数据库持久化与 UI
 * @param defaultBookId 初始默认加载的书籍 ID
 */
open class ReaderViewModel(
    private val dataSource: ReaderDataSource = InMemoryReaderDataSource(),
    defaultBookId: Long = 1L
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState(isLoading = true))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        sendIntent(ReaderIntent.LoadBook(defaultBookId))
    }

    /**
     * 发送并处理用户交互意图 (MVI Intent Dispatcher)
     */
    fun sendIntent(intent: ReaderIntent) {
        viewModelScope.launch {
            when (intent) {
                is ReaderIntent.LoadBook -> handleLoadBook(intent.bookId)
                is ReaderIntent.ToggleControls -> handleToggleControls()
                is ReaderIntent.SetControlsVisible -> handleSetControlsVisible(intent.visible)
                is ReaderIntent.PrevPage -> handlePrevPage()
                is ReaderIntent.NextPage -> handleNextPage()
                is ReaderIntent.JumpToPage -> handleJumpToPage(intent.pageIndex)
                is ReaderIntent.PrevChapter -> handlePrevChapter()
                is ReaderIntent.NextChapter -> handleNextChapter()
                is ReaderIntent.JumpToChapter -> handleJumpToChapter(intent.chapterIndex)
                is ReaderIntent.SeekToProgress -> handleSeekToProgress(intent.progress)
                is ReaderIntent.SetDrawerOpen -> handleSetDrawerOpen(intent.isOpen)
                is ReaderIntent.SwitchDrawerTab -> handleSwitchDrawerTab(intent.tab)
                is ReaderIntent.ToggleChapterOrder -> handleToggleChapterOrder()
                is ReaderIntent.SetTypographySheetVisible -> handleSetTypographySheetVisible(intent.visible)
                is ReaderIntent.ChangeFontSize -> handleChangeFontSize(intent.deltaSp)
                is ReaderIntent.SetFontSize -> handleSetFontSize(intent.fontSizeSp)
                is ReaderIntent.SetLineHeight -> handleSetLineHeight(intent.multiplier)
                is ReaderIntent.SetLetterSpacing -> handleSetLetterSpacing(intent.letterSpacingEm)
                is ReaderIntent.SetParagraphSpacing -> handleSetParagraphSpacing(intent.spacingDp)
                is ReaderIntent.SetHorizontalPadding -> handleSetHorizontalPadding(intent.paddingDp)
                is ReaderIntent.SetVerticalPadding -> handleSetVerticalPadding(intent.paddingDp)
                is ReaderIntent.SelectThemePreset -> handleSelectThemePreset(intent.preset)
                is ReaderIntent.SelectPageTurnAnimation -> handleSelectPageTurnAnimation(intent.animation)
                is ReaderIntent.SetKeepScreenOn -> handleSetKeepScreenOn(intent.enabled)
                is ReaderIntent.SetVolumeKeyPageTurn -> handleSetVolumeKeyPageTurn(intent.enabled)
                is ReaderIntent.ToggleBookmark -> handleToggleBookmark()
                is ReaderIntent.DeleteBookmark -> handleDeleteBookmark(intent.bookmarkId)
                is ReaderIntent.JumpToBookmark -> handleJumpToBookmark(intent.bookmark)
                is ReaderIntent.ClearError -> handleClearError()
            }
        }
    }

    private suspend fun handleLoadBook(bookId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val book = dataSource.getBook(bookId)
            val chapters = dataSource.getChapters(bookId)
            val savedLocator = dataSource.getLocator(bookId)
            val config = dataSource.getReaderConfig()
            val bookmarks = dataSource.getBookmarks(bookId)

            val currentChapterIndex = savedLocator?.chapterIndex ?: 0
            val currentChapter = chapters.getOrNull(currentChapterIndex)
                ?: chapters.firstOrNull()

            val content = if (currentChapter != null) {
                dataSource.getChapterContent(bookId, currentChapter.index)
            } else ""

            val totalPagesInChapter = calculatePagesForChapter(content)
            val pageIndex = (savedLocator?.pageIndexInChapter ?: 0)
                .coerceIn(0, (totalPagesInChapter - 1).coerceAtLeast(0))

            val totalProgress = calculateTotalProgress(
                chapterIndex = currentChapter?.index ?: 0,
                totalChapters = chapters.size,
                pageIndex = pageIndex,
                totalPagesInChapter = totalPagesInChapter
            )
            val chapterProgress = calculateChapterProgress(pageIndex, totalPagesInChapter)

            val isBookmarked = checkIsBookmarked(
                bookmarks = bookmarks,
                chapterIndex = currentChapter?.index ?: 0,
                pageIndex = pageIndex
            )

            val locator = ReadLocator(
                bookId = bookId,
                chapterIndex = currentChapter?.index ?: 0,
                chapterTitle = currentChapter?.title ?: "",
                charOffset = pageIndex * 200,
                progression = totalProgress,
                pageIndexInChapter = pageIndex,
                totalPagesInChapter = totalPagesInChapter
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    book = book,
                    currentChapter = currentChapter,
                    chapters = chapters,
                    currentLocator = locator,
                    currentPageIndex = pageIndex,
                    totalPagesInChapter = totalPagesInChapter,
                    totalProgress = totalProgress,
                    chapterProgress = chapterProgress,
                    currentPageContent = content,
                    readerConfig = config,
                    bookmarks = bookmarks,
                    isCurrentPageBookmarked = isBookmarked
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "加载书籍内容失败: ${e.message}"
                )
            }
        }
    }

    private fun handleToggleControls() {
        _uiState.update { state ->
            val nextVisible = !state.isControlsVisible
            state.copy(
                isControlsVisible = nextVisible,
                // 若收起控制栏，一并收起子弹窗与抽屉
                isTypographySheetVisible = if (!nextVisible) false else state.isTypographySheetVisible,
                isDrawerOpen = if (!nextVisible) false else state.isDrawerOpen
            )
        }
    }

    private fun handleSetControlsVisible(visible: Boolean) {
        _uiState.update { state ->
            state.copy(
                isControlsVisible = visible,
                isTypographySheetVisible = if (!visible) false else state.isTypographySheetVisible,
                isDrawerOpen = if (!visible) false else state.isDrawerOpen
            )
        }
    }

    private suspend fun handlePrevPage() {
        val state = _uiState.value
        if (state.currentPageIndex > 0) {
            // 当前章节内上一页
            val newPageIndex = state.currentPageIndex - 1
            updatePageState(state.currentChapter?.index ?: 0, newPageIndex)
        } else if (state.hasPrevChapter) {
            // 翻入上一章节末页
            val prevChapterIndex = (state.currentChapter?.index ?: 1) - 1
            val prevChapter = state.chapters.getOrNull(prevChapterIndex)
            if (prevChapter != null) {
                val prevContent = dataSource.getChapterContent(prevChapter.bookId, prevChapter.index)
                val totalPages = calculatePagesForChapter(prevContent)
                val lastPageIndex = (totalPages - 1).coerceAtLeast(0)
                updateChapterAndPageState(prevChapter, prevContent, lastPageIndex, totalPages)
            }
        }
    }

    private suspend fun handleNextPage() {
        val state = _uiState.value
        if (state.currentPageIndex < state.totalPagesInChapter - 1) {
            // 当前章节内下一页
            val newPageIndex = state.currentPageIndex + 1
            updatePageState(state.currentChapter?.index ?: 0, newPageIndex)
        } else if (state.hasNextChapter) {
            // 翻入下一章节首页
            val nextChapterIndex = (state.currentChapter?.index ?: 0) + 1
            val nextChapter = state.chapters.getOrNull(nextChapterIndex)
            if (nextChapter != null) {
                val nextContent = dataSource.getChapterContent(nextChapter.bookId, nextChapter.index)
                val totalPages = calculatePagesForChapter(nextContent)
                updateChapterAndPageState(nextChapter, nextContent, 0, totalPages)
            }
        }
    }

    private suspend fun handleJumpToPage(pageIndex: Int) {
        val state = _uiState.value
        val clamped = pageIndex.coerceIn(0, (state.totalPagesInChapter - 1).coerceAtLeast(0))
        updatePageState(state.currentChapter?.index ?: 0, clamped)
    }

    private suspend fun handlePrevChapter() {
        val state = _uiState.value
        if (state.hasPrevChapter) {
            val prevIdx = (state.currentChapter?.index ?: 1) - 1
            handleJumpToChapter(prevIdx)
        }
    }

    private suspend fun handleNextChapter() {
        val state = _uiState.value
        if (state.hasNextChapter) {
            val nextIdx = (state.currentChapter?.index ?: 0) + 1
            handleJumpToChapter(nextIdx)
        }
    }

    private suspend fun handleJumpToChapter(chapterIndex: Int) {
        val state = _uiState.value
        val targetChapter = state.chapters.getOrNull(chapterIndex) ?: return
        val content = dataSource.getChapterContent(targetChapter.bookId, targetChapter.index)
        val totalPages = calculatePagesForChapter(content)
        updateChapterAndPageState(targetChapter, content, 0, totalPages)
        // 跳转章节后关闭抽屉
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    private suspend fun handleSeekToProgress(progress: Float) {
        val state = _uiState.value
        if (state.chapters.isEmpty()) return
        val clamped = progress.coerceIn(0.0f, 1.0f)
        val targetChapterIdx = ((clamped * state.chapters.size).toInt())
            .coerceIn(0, state.chapters.size - 1)
        val targetChapter = state.chapters[targetChapterIdx]
        val content = dataSource.getChapterContent(targetChapter.bookId, targetChapter.index)
        val totalPages = calculatePagesForChapter(content)

        // 估算本章内偏移页
        val chapterProgressWeight = 1.0f / state.chapters.size
        val chapterStartProgress = targetChapterIdx * chapterProgressWeight
        val progressInChapter = if (chapterProgressWeight > 0f) {
            ((clamped - chapterStartProgress) / chapterProgressWeight).coerceIn(0.0f, 1.0f)
        } else 0.0f
        val targetPage = (progressInChapter * totalPages).toInt().coerceIn(0, (totalPages - 1).coerceAtLeast(0))

        updateChapterAndPageState(targetChapter, content, targetPage, totalPages)
    }

    private fun handleSetDrawerOpen(isOpen: Boolean) {
        _uiState.update { state ->
            state.copy(
                isDrawerOpen = isOpen,
                // 打开抽屉时收起控制栏，保持纯净阅读体验
                isControlsVisible = if (isOpen) false else state.isControlsVisible
            )
        }
    }

    private fun handleSwitchDrawerTab(tab: DrawerTab) {
        _uiState.update { it.copy(selectedDrawerTab = tab) }
    }

    private fun handleToggleChapterOrder() {
        _uiState.update { it.copy(isChaptersReversed = !it.isChaptersReversed) }
    }

    private fun handleSetTypographySheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isTypographySheetVisible = visible) }
    }

    private suspend fun handleChangeFontSize(deltaSp: Float) {
        val current = _uiState.value.readerConfig.fontSizeSp
        val target = (current + deltaSp).coerceIn(
            ReaderConfig.MIN_FONT_SIZE_SP,
            ReaderConfig.MAX_FONT_SIZE_SP
        )
        handleSetFontSize(target)
    }

    private suspend fun handleSetFontSize(fontSizeSp: Float) {
        val clamped = fontSizeSp.coerceIn(
            ReaderConfig.MIN_FONT_SIZE_SP,
            ReaderConfig.MAX_FONT_SIZE_SP
        )
        updateReaderConfig { it.copy(fontSizeSp = clamped) }
    }

    private suspend fun handleSetLineHeight(multiplier: Float) {
        val clamped = multiplier.coerceIn(
            ReaderConfig.MIN_LINE_HEIGHT,
            ReaderConfig.MAX_LINE_HEIGHT
        )
        updateReaderConfig { it.copy(lineHeightMultiplier = clamped) }
    }

    private suspend fun handleSetLetterSpacing(letterSpacingEm: Float) {
        val clamped = letterSpacingEm.coerceIn(0.0f, 0.5f)
        updateReaderConfig { it.copy(letterSpacingEm = clamped) }
    }

    private suspend fun handleSetParagraphSpacing(spacingDp: Float) {
        val clamped = spacingDp.coerceIn(0.0f, ReaderConfig.MAX_PARAGRAPH_SPACING_DP)
        updateReaderConfig { it.copy(paragraphSpacingDp = clamped) }
    }

    private suspend fun handleSetHorizontalPadding(paddingDp: Float) {
        val clamped = paddingDp.coerceIn(8.0f, 48.0f)
        updateReaderConfig { it.copy(pagePaddingHorizontalDp = clamped) }
    }

    private suspend fun handleSetVerticalPadding(paddingDp: Float) {
        val clamped = paddingDp.coerceIn(12.0f, 64.0f)
        updateReaderConfig { it.copy(pagePaddingVerticalDp = clamped) }
    }

    private suspend fun handleSelectThemePreset(preset: ReaderThemePreset) {
        updateReaderConfig { it.copy(themePreset = preset) }
    }

    private suspend fun handleSelectPageTurnAnimation(animation: PageTurnAnimation) {
        updateReaderConfig { it.copy(pageTurnAnimation = animation) }
    }

    private suspend fun handleSetKeepScreenOn(enabled: Boolean) {
        updateReaderConfig { it.copy(keepScreenOn = enabled) }
    }

    private suspend fun handleSetVolumeKeyPageTurn(enabled: Boolean) {
        updateReaderConfig { it.copy(volumeKeyPageTurn = enabled) }
    }

    private suspend fun handleToggleBookmark() {
        val state = _uiState.value
        val bookId = state.book?.id ?: 1L
        val chapterIdx = state.currentChapter?.index ?: 0
        val chapterTitle = state.currentChapter?.title ?: "未知章节"
        val pageIdx = state.currentPageIndex

        val existing = state.bookmarks.firstOrNull {
            it.chapterIndex == chapterIdx && (it.charOffset / 200) == pageIdx
        }

        if (existing != null) {
            // 已存在 -> 删除书签
            dataSource.deleteBookmark(existing.id)
            val updated = state.bookmarks.filterNot { it.id == existing.id }
            _uiState.update {
                it.copy(
                    bookmarks = updated,
                    isCurrentPageBookmarked = false
                )
            }
        } else {
            // 不存在 -> 新建书签
            val previewSnippet = state.currentPageContent.take(60).trim().ifBlank {
                "书签记录于第 ${pageIdx + 1} 页"
            }
            val newBookmark = Bookmark(
                id = 0L,
                bookId = bookId,
                chapterIndex = chapterIdx,
                chapterTitle = chapterTitle,
                charOffset = pageIdx * 200,
                previewText = previewSnippet,
                createTime = System.currentTimeMillis()
            )
            val saved = dataSource.saveBookmark(newBookmark)
            val updated = listOf(saved) + state.bookmarks
            _uiState.update {
                it.copy(
                    bookmarks = updated,
                    isCurrentPageBookmarked = true
                )
            }
        }
    }

    private suspend fun handleDeleteBookmark(bookmarkId: Long) {
        dataSource.deleteBookmark(bookmarkId)
        val state = _uiState.value
        val updated = state.bookmarks.filterNot { it.id == bookmarkId }
        val isBookmarked = checkIsBookmarked(
            bookmarks = updated,
            chapterIndex = state.currentChapter?.index ?: 0,
            pageIndex = state.currentPageIndex
        )
        _uiState.update {
            it.copy(
                bookmarks = updated,
                isCurrentPageBookmarked = isBookmarked
            )
        }
    }

    private suspend fun handleJumpToBookmark(bookmark: Bookmark) {
        val state = _uiState.value
        val targetChapter = state.chapters.getOrNull(bookmark.chapterIndex) ?: return
        val content = dataSource.getChapterContent(targetChapter.bookId, targetChapter.index)
        val totalPages = calculatePagesForChapter(content)
        val pageIdx = (bookmark.charOffset / 200).coerceIn(0, (totalPages - 1).coerceAtLeast(0))

        updateChapterAndPageState(targetChapter, content, pageIdx, totalPages)
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    private fun handleClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- 内部状态辅助计算与持久化同步 ---

    private suspend fun updatePageState(chapterIndex: Int, pageIndex: Int) {
        val state = _uiState.value
        val totalProgress = calculateTotalProgress(
            chapterIndex = chapterIndex,
            totalChapters = state.chapters.size,
            pageIndex = pageIndex,
            totalPagesInChapter = state.totalPagesInChapter
        )
        val chapterProgress = calculateChapterProgress(pageIndex, state.totalPagesInChapter)
        val isBookmarked = checkIsBookmarked(state.bookmarks, chapterIndex, pageIndex)

        val locator = ReadLocator(
            bookId = state.book?.id ?: 1L,
            chapterIndex = chapterIndex,
            chapterTitle = state.currentChapter?.title ?: "",
            charOffset = pageIndex * 200,
            progression = totalProgress,
            pageIndexInChapter = pageIndex,
            totalPagesInChapter = state.totalPagesInChapter
        )
        dataSource.saveLocator(locator)

        _uiState.update {
            it.copy(
                currentPageIndex = pageIndex,
                totalProgress = totalProgress,
                chapterProgress = chapterProgress,
                currentLocator = locator,
                isCurrentPageBookmarked = isBookmarked
            )
        }
    }

    private suspend fun updateChapterAndPageState(
        chapter: Chapter,
        content: String,
        pageIndex: Int,
        totalPages: Int
    ) {
        val state = _uiState.value
        val totalProgress = calculateTotalProgress(
            chapterIndex = chapter.index,
            totalChapters = state.chapters.size,
            pageIndex = pageIndex,
            totalPagesInChapter = totalPages
        )
        val chapterProgress = calculateChapterProgress(pageIndex, totalPages)
        val isBookmarked = checkIsBookmarked(state.bookmarks, chapter.index, pageIndex)

        val locator = ReadLocator(
            bookId = chapter.bookId,
            chapterIndex = chapter.index,
            chapterTitle = chapter.title,
            charOffset = pageIndex * 200,
            progression = totalProgress,
            pageIndexInChapter = pageIndex,
            totalPagesInChapter = totalPages
        )
        dataSource.saveLocator(locator)

        _uiState.update {
            it.copy(
                currentChapter = chapter,
                currentPageContent = content,
                currentPageIndex = pageIndex,
                totalPagesInChapter = totalPages,
                totalProgress = totalProgress,
                chapterProgress = chapterProgress,
                currentLocator = locator,
                isCurrentPageBookmarked = isBookmarked
            )
        }
    }

    private suspend fun updateReaderConfig(updater: (ReaderConfig) -> ReaderConfig) {
        val newConfig = updater(_uiState.value.readerConfig)
        dataSource.saveReaderConfig(newConfig)
        _uiState.update { it.copy(readerConfig = newConfig) }
    }

    private fun calculatePagesForChapter(content: String): Int {
        // 估算分页：每页约 350 字
        val length = content.length
        return (length / 350 + 1).coerceAtLeast(1)
    }

    private fun calculateTotalProgress(
        chapterIndex: Int,
        totalChapters: Int,
        pageIndex: Int,
        totalPagesInChapter: Int
    ): Float {
        if (totalChapters <= 0) return 0.0f
        val chapterProgressFraction = if (totalPagesInChapter > 1) {
            pageIndex.toFloat() / (totalPagesInChapter - 1)
        } else 0.0f
        val weighted = (chapterIndex + chapterProgressFraction) / totalChapters.toFloat()
        return weighted.coerceIn(0.0f, 1.0f)
    }

    private fun calculateChapterProgress(pageIndex: Int, totalPages: Int): Float {
        if (totalPages <= 1) return 1.0f
        return (pageIndex.toFloat() / (totalPages - 1)).coerceIn(0.0f, 1.0f)
    }

    private fun checkIsBookmarked(
        bookmarks: List<Bookmark>,
        chapterIndex: Int,
        pageIndex: Int
    ): Boolean {
        return bookmarks.any {
            it.chapterIndex == chapterIndex && (it.charOffset / 200) == pageIndex
        }
    }
}
