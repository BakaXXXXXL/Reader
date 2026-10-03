package com.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.core.model.Bookmark
import com.reader.core.model.Chapter
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReaderConfig
import com.reader.core.model.ReaderThemePreset
import com.reader.engine.typography.layout.TextMeasureEngine
import com.reader.engine.typography.locator.ReadLocatorMapper
import com.reader.engine.typography.measurer.StandardCharMeasurer
import com.reader.engine.typography.model.PageDimensions
import com.reader.engine.typography.model.ReaderPage
import com.reader.engine.typography.splitter.PageSplitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 阅读器核心状态机 ViewModel (MVI 架构)。
 *
 * 严格遵从单向数据流原则：通过 [sendIntent] 接收用户交互事件，
 * 在后台驱动状态机流转，结合 [PageSplitter] 物理分页算法与自适应定位恢复，
 * 并统一向表现层 Composable 单向暴露不可变 [uiState]。
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

    private var currentChapterText: String = ""
    private var currentPages: List<ReaderPage> = emptyList()
    private var currentDimensions: PageDimensions = PageDimensions(
        viewWidth = 1080f,
        viewHeight = 2200f,
        paddingLeft = 48f,
        paddingTop = 80f,
        paddingRight = 48f,
        paddingBottom = 80f
    )

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
                is ReaderIntent.UpdateViewport -> handleUpdateViewport(intent.width, intent.height)
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

            val currentChapterIndex = (savedLocator?.chapterIndex ?: 0).coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
            val currentChapter = chapters.getOrNull(currentChapterIndex)
                ?: chapters.firstOrNull()

            val content = if (currentChapter != null) {
                dataSource.getChapterContent(bookId, currentChapter.index)
            } else ""

            currentChapterText = content
            val targetPageIndex = savedLocator?.pageIndexInChapter ?: 0

            _uiState.update {
                it.copy(
                    book = book,
                    chapters = chapters,
                    currentChapter = currentChapter,
                    readerConfig = config,
                    bookmarks = bookmarks,
                    fullChapterText = content
                )
            }

            paginateCurrentChapter(
                chapterIndex = currentChapter?.index ?: 0,
                targetPageIndex = targetPageIndex
            )
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
            updatePageIndex(newPageIndex)
        } else if (state.hasPrevChapter) {
            // 翻入上一章节末页
            val prevChapterIndex = (state.currentChapter?.index ?: 1) - 1
            loadChapterAndSeek(prevChapterIndex, seekToLastPage = true)
        }
    }

    private suspend fun handleNextPage() {
        val state = _uiState.value
        if (state.currentPageIndex < state.totalPagesInChapter - 1) {
            // 当前章节内下一页
            val newPageIndex = state.currentPageIndex + 1
            updatePageIndex(newPageIndex)
        } else if (state.hasNextChapter) {
            // 翻入下一章节首页
            val nextChapterIndex = (state.currentChapter?.index ?: 0) + 1
            loadChapterAndSeek(nextChapterIndex, seekToLastPage = false)
        }
    }

    private suspend fun handleJumpToPage(pageIndex: Int) {
        val state = _uiState.value
        val clamped = pageIndex.coerceIn(0, (state.totalPagesInChapter - 1).coerceAtLeast(0))
        updatePageIndex(clamped)
    }

    private suspend fun handlePrevChapter() {
        val state = _uiState.value
        if (state.hasPrevChapter) {
            val prevIdx = (state.currentChapter?.index ?: 1) - 1
            loadChapterAndSeek(prevIdx, seekToLastPage = false)
        }
    }

    private suspend fun handleNextChapter() {
        val state = _uiState.value
        if (state.hasNextChapter) {
            val nextIdx = (state.currentChapter?.index ?: 0) + 1
            loadChapterAndSeek(nextIdx, seekToLastPage = false)
        }
    }

    private suspend fun handleJumpToChapter(chapterIndex: Int) {
        loadChapterAndSeek(chapterIndex, seekToLastPage = false)
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    private suspend fun loadChapterAndSeek(chapterIndex: Int, seekToLastPage: Boolean) {
        val state = _uiState.value
        val targetChapter = state.chapters.find { it.index == chapterIndex }
            ?: state.chapters.getOrNull(chapterIndex)
            ?: return

        val bookId = targetChapter.bookId
        val content = dataSource.getChapterContent(bookId, targetChapter.index)
        currentChapterText = content

        _uiState.update {
            it.copy(
                currentChapter = targetChapter,
                fullChapterText = content
            )
        }

        paginateCurrentChapter(
            chapterIndex = targetChapter.index,
            targetPageIndex = if (seekToLastPage) Int.MAX_VALUE else 0
        )
    }

    private suspend fun handleSeekToProgress(progress: Float) {
        val state = _uiState.value
        if (state.chapters.isEmpty()) return
        val clamped = progress.coerceIn(0.0f, 1.0f)
        val targetChapterIdx = ((clamped * state.chapters.size).toInt())
            .coerceIn(0, state.chapters.size - 1)
        loadChapterAndSeek(targetChapterIdx, seekToLastPage = false)
    }

    private fun handleSetDrawerOpen(isOpen: Boolean) {
        _uiState.update { state ->
            state.copy(
                isDrawerOpen = isOpen,
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
        repaginateAndMaintainOffset()
    }

    private suspend fun handleSetLineHeight(multiplier: Float) {
        val clamped = multiplier.coerceIn(1.0f, 3.0f)
        updateReaderConfig { it.copy(lineHeightMultiplier = clamped) }
        repaginateAndMaintainOffset()
    }

    private suspend fun handleSetLetterSpacing(letterSpacingEm: Float) {
        val clamped = letterSpacingEm.coerceIn(-0.05f, 0.5f)
        updateReaderConfig { it.copy(letterSpacingEm = clamped) }
        repaginateAndMaintainOffset()
    }

    private suspend fun handleSetParagraphSpacing(spacingDp: Float) {
        val clamped = spacingDp.coerceIn(0f, 48f)
        updateReaderConfig { it.copy(paragraphSpacingDp = clamped) }
        repaginateAndMaintainOffset()
    }

    private suspend fun handleSetHorizontalPadding(paddingDp: Float) {
        val clamped = paddingDp.coerceIn(8f, 48f)
        updateReaderConfig { it.copy(pagePaddingHorizontalDp = clamped) }
        repaginateAndMaintainOffset()
    }

    private suspend fun handleSetVerticalPadding(paddingDp: Float) {
        val clamped = paddingDp.coerceIn(8f, 64f)
        updateReaderConfig { it.copy(pagePaddingVerticalDp = clamped) }
        repaginateAndMaintainOffset()
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
        val chapter = state.currentChapter ?: return

        val existing = state.bookmarks.find {
            it.chapterIndex == chapter.index &&
                    ((state.currentPageIndex == 0 && it.charOffset == 0) ||
                            (state.currentLocator != null && it.charOffset == state.currentLocator.charOffset))
        }

        if (existing != null) {
            dataSource.deleteBookmark(existing.id)
            val updated = state.bookmarks.filterNot { it.id == existing.id }
            _uiState.update {
                it.copy(
                    bookmarks = updated,
                    isCurrentPageBookmarked = false
                )
            }
        } else {
            val snippet = state.currentPageContent.take(60).replace("\n", " ")
            val newBookmark = Bookmark(
                bookId = bookId,
                chapterIndex = chapter.index,
                chapterTitle = chapter.title,
                charOffset = state.currentLocator?.charOffset ?: 0,
                previewText = snippet.ifBlank { "第 ${state.currentPageIndex + 1} 页标记" },
                createTime = System.currentTimeMillis()
            )
            val saved = dataSource.saveBookmark(newBookmark)
            _uiState.update {
                it.copy(
                    bookmarks = listOf(saved) + it.bookmarks,
                    isCurrentPageBookmarked = true
                )
            }
        }
    }

    private suspend fun handleDeleteBookmark(bookmarkId: Long) {
        dataSource.deleteBookmark(bookmarkId)
        _uiState.update { state ->
            val updated = state.bookmarks.filterNot { it.id == bookmarkId }
            val isCurrentMarked = state.currentChapter?.let { ch ->
                checkIsBookmarked(updated, ch.index, state.currentPageIndex)
            } ?: false
            state.copy(bookmarks = updated, isCurrentPageBookmarked = isCurrentMarked)
        }
    }

    private suspend fun handleJumpToBookmark(bookmark: Bookmark) {
        loadChapterAndSeek(bookmark.chapterIndex, seekToLastPage = false)
        val targetPage = ReadLocatorMapper.locatePageByCharOffset(currentPages, bookmark.charOffset)
        updatePageIndex(targetPage)
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    private fun handleUpdateViewport(width: Float, height: Float) {
        if (width <= 50f || height <= 50f) return
        val config = _uiState.value.readerConfig
        currentDimensions = PageDimensions(
            viewWidth = width,
            viewHeight = height,
            paddingLeft = config.pagePaddingHorizontalDp * 2.5f,
            paddingRight = config.pagePaddingHorizontalDp * 2.5f,
            paddingTop = config.pagePaddingVerticalDp * 2.5f,
            paddingBottom = config.pagePaddingVerticalDp * 2.5f
        )
        repaginateAndMaintainOffset()
    }

    private fun handleClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ==================== 核心分页与状态派生 ====================

    private fun paginateCurrentChapter(chapterIndex: Int, targetPageIndex: Int) {
        val config = _uiState.value.readerConfig
        val measurer = StandardCharMeasurer(fontSize = config.fontSizeSp)
        val measureEngine = TextMeasureEngine(measurer = measurer, config = config)
        val splitter = PageSplitter(measureEngine)

        currentPages = if (currentChapterText.isNotBlank()) {
            splitter.splitChapter(currentChapterText, currentDimensions, chapterIndex)
        } else {
            emptyList()
        }

        val totalPages = currentPages.size.coerceAtLeast(1)
        val pageIndex = targetPageIndex.coerceIn(0, totalPages - 1)
        applyPage(pageIndex, totalPages)
    }

    private fun repaginateAndMaintainOffset() {
        val currentOffset = _uiState.value.currentLocator?.charOffset ?: 0
        val chapterIdx = _uiState.value.currentChapter?.index ?: 0

        val config = _uiState.value.readerConfig
        val measurer = StandardCharMeasurer(fontSize = config.fontSizeSp)
        val measureEngine = TextMeasureEngine(measurer = measurer, config = config)
        val splitter = PageSplitter(measureEngine)

        currentPages = if (currentChapterText.isNotBlank()) {
            splitter.splitChapter(currentChapterText, currentDimensions, chapterIdx)
        } else {
            emptyList()
        }

        val totalPages = currentPages.size.coerceAtLeast(1)
        val newPageIndex = if (currentPages.isNotEmpty()) {
            ReadLocatorMapper.locatePageByCharOffset(currentPages, currentOffset)
        } else {
            0
        }
        applyPage(newPageIndex, totalPages)
    }

    private fun applyPage(pageIndex: Int, totalPages: Int) {
        val state = _uiState.value
        val chapter = state.currentChapter
        val page = currentPages.getOrNull(pageIndex)

        val pageLines = page?.lines?.map { it.text } ?: emptyList()
        val pageContent = if (pageLines.isNotEmpty()) {
            pageLines.joinToString("\n")
        } else {
            currentChapterText
        }

        val totalProgress = calculateTotalProgress(
            chapterIndex = chapter?.index ?: 0,
            totalChapters = state.chapters.size,
            pageIndex = pageIndex,
            totalPagesInChapter = totalPages
        )
        val chapterProgress = calculateChapterProgress(pageIndex, totalPages)
        val isBookmarked = checkIsBookmarked(state.bookmarks, chapter?.index ?: 0, pageIndex)

        val locator = ReadLocator(
            bookId = chapter?.bookId ?: state.book?.id ?: 1L,
            chapterIndex = chapter?.index ?: 0,
            chapterTitle = chapter?.title ?: "正文",
            charOffset = page?.startCharOffset ?: (pageIndex * 200),
            progression = totalProgress,
            pageIndexInChapter = pageIndex,
            totalPagesInChapter = totalPages
        )

        viewModelScope.launch {
            dataSource.saveLocator(locator)
        }

        _uiState.update {
            it.copy(
                currentPageIndex = pageIndex,
                totalPagesInChapter = totalPages,
                currentPageContent = pageContent,
                currentPaginatedLines = pageLines,
                totalProgress = totalProgress,
                chapterProgress = chapterProgress,
                currentLocator = locator,
                isCurrentPageBookmarked = isBookmarked,
                isLoading = false
            )
        }
    }

    private fun updatePageIndex(pageIndex: Int) {
        val totalPages = _uiState.value.totalPagesInChapter
        applyPage(pageIndex.coerceIn(0, totalPages - 1), totalPages)
    }

    private suspend fun updateReaderConfig(updater: (ReaderConfig) -> ReaderConfig) {
        val newConfig = updater(_uiState.value.readerConfig)
        dataSource.saveReaderConfig(newConfig)
        _uiState.update { it.copy(readerConfig = newConfig) }
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

        val rawProgress = (chapterIndex.toFloat() + chapterProgressFraction) / totalChapters.toFloat()
        return rawProgress.coerceIn(0.0f, 1.0f)
    }

    private fun calculateChapterProgress(pageIndex: Int, totalPagesInChapter: Int): Float {
        if (totalPagesInChapter <= 1) return 1.0f
        return (pageIndex.toFloat() / (totalPagesInChapter - 1).toFloat()).coerceIn(0.0f, 1.0f)
    }

    private fun checkIsBookmarked(bookmarks: List<Bookmark>, chapterIndex: Int, pageIndex: Int): Boolean {
        return bookmarks.any {
            it.chapterIndex == chapterIndex &&
                    ((pageIndex == 0 && it.charOffset == 0) ||
                            (_uiState.value.currentLocator != null && it.charOffset == _uiState.value.currentLocator?.charOffset))
        }
    }

    companion object {
        var defaultDataSourceProvider: ((Long) -> ReaderDataSource)? = null

        fun createDefaultDataSource(bookId: Long): ReaderDataSource {
            defaultDataSourceProvider?.let { return it(bookId) }
            return try {
                val appClass = Class.forName("com.reader.app.ReaderApplication")
                val instanceProp = appClass.getMethod("getInstance").invoke(null)
                val db = appClass.getMethod("getDatabase").invoke(instanceProp) as com.reader.core.database.ReaderDatabase
                val prefs = appClass.getMethod("getPreferencesDataStore").invoke(instanceProp) as com.reader.core.datastore.ReaderPreferencesDataStore
                RoomReaderDataSource(database = db, preferencesDataStore = prefs)
            } catch (_: Throwable) {
                InMemoryReaderDataSource()
            }
        }

        fun create(bookId: Long): ReaderViewModel {
            return ReaderViewModel(
                dataSource = createDefaultDataSource(bookId),
                defaultBookId = bookId
            )
        }
    }
}
