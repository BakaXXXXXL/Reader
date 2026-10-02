package com.reader.feature.bookshelf

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.feature.bookshelf.data.DefaultBookshelfRepository
import com.reader.feature.bookshelf.model.BookshelfCategory
import com.reader.feature.bookshelf.model.BookshelfItem
import com.reader.feature.bookshelf.model.BookshelfSortOrder
import com.reader.feature.bookshelf.model.BookshelfViewMode
import com.reader.feature.bookshelf.model.ImportCandidate
import com.reader.feature.bookshelf.mvi.BookshelfIntent
import com.reader.feature.bookshelf.viewmodel.BookshelfViewModel
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
 * [BookshelfViewModel] MVI 状态机单元测试。
 * 覆盖状态流管道、分类过滤、搜索、排序、置顶、收藏、删除及本地导入生命周期。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookshelfViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DefaultBookshelfRepository
    private lateinit var viewModel: BookshelfViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = DefaultBookshelfRepository(
            initialBooks = DefaultBookshelfRepository.createSampleData(),
            ioDispatcher = testDispatcher
        )
        viewModel = BookshelfViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state loads sample data and category counts`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.allBooks).hasSize(4)
        assertThat(state.displayedBooks).hasSize(4)

        // 验证分类计数
        assertThat(state.categoryCounts[BookshelfCategory.ALL]).isEqualTo(4)
        assertThat(state.categoryCounts[BookshelfCategory.READING]).isAtLeast(1)
        assertThat(state.categoryCounts[BookshelfCategory.COMPLETED]).isAtLeast(1)
        assertThat(state.categoryCounts[BookshelfCategory.FAVORITE]).isAtLeast(1)
    }

    @Test
    fun `test category filtering`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 切换到 READING
        viewModel.onIntent(BookshelfIntent.SelectCategory(BookshelfCategory.READING))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertThat(state.selectedCategory).isEqualTo(BookshelfCategory.READING)
        assertThat(state.displayedBooks.all { it.isReading }).isTrue()

        // 切换到 COMPLETED
        viewModel.onIntent(BookshelfIntent.SelectCategory(BookshelfCategory.COMPLETED))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.selectedCategory).isEqualTo(BookshelfCategory.COMPLETED)
        assertThat(state.displayedBooks.all { it.isCompleted }).isTrue()

        // 切换到 FAVORITE
        viewModel.onIntent(BookshelfIntent.SelectCategory(BookshelfCategory.FAVORITE))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.selectedCategory).isEqualTo(BookshelfCategory.FAVORITE)
        assertThat(state.displayedBooks.all { it.isFavorite }).isTrue()
    }

    @Test
    fun `test search filtering and clear`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 搜索书名
        viewModel.onIntent(BookshelfIntent.Search("三国"))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertThat(state.searchQuery).isEqualTo("三国")
        assertThat(state.displayedBooks).hasSize(1)
        assertThat(state.displayedBooks.first().title).contains("三国")

        // 搜索作者
        viewModel.onIntent(BookshelfIntent.Search("曹雪芹"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.displayedBooks).hasSize(1)
        assertThat(state.displayedBooks.first().author).isEqualTo("曹雪芹")

        // 搜索无结果
        viewModel.onIntent(BookshelfIntent.Search("不存在的玄幻小说XYZ"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.displayedBooks).isEmpty()
        assertThat(state.isSearchEmpty).isTrue()

        // 清空搜索
        viewModel.onIntent(BookshelfIntent.ClearSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.searchQuery).isEmpty()
        assertThat(state.displayedBooks).hasSize(4)
    }

    @Test
    fun `test sort orders`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 切换为书名升序
        viewModel.onIntent(BookshelfIntent.ChangeSortOrder(BookshelfSortOrder.TITLE))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertThat(state.sortOrder).isEqualTo(BookshelfSortOrder.TITLE)

        // 切换为阅读进度降序
        viewModel.onIntent(BookshelfIntent.ChangeSortOrder(BookshelfSortOrder.PROGRESS))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.sortOrder).isEqualTo(BookshelfSortOrder.PROGRESS)
    }

    @Test
    fun `test switch view mode`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.viewMode).isEqualTo(BookshelfViewMode.GRID)

        viewModel.onIntent(BookshelfIntent.SwitchViewMode(BookshelfViewMode.LIST))
        assertThat(viewModel.uiState.value.viewMode).isEqualTo(BookshelfViewMode.LIST)
    }

    @Test
    fun `test toggle favorite and toggle pin`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetItem = viewModel.uiState.value.allBooks.first()
        val originalFav = targetItem.isFavorite

        viewModel.onIntent(BookshelfIntent.ToggleFavorite(targetItem.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedItem = viewModel.uiState.value.allBooks.first { it.id == targetItem.id }
        assertThat(updatedItem.isFavorite).isEqualTo(!originalFav)

        // 测试置顶
        val originalPin = targetItem.isPinned
        viewModel.onIntent(BookshelfIntent.TogglePin(targetItem.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val pinnedItem = viewModel.uiState.value.allBooks.first { it.id == targetItem.id }
        assertThat(pinnedItem.isPinned).isEqualTo(!originalPin)
    }

    @Test
    fun `test delete book flow`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val targetItem = viewModel.uiState.value.allBooks.first()

        // 请求删除 (弹出二次确认弹窗)
        viewModel.onIntent(BookshelfIntent.RequestDelete(targetItem))
        assertThat(viewModel.uiState.value.bookToDelete).isEqualTo(targetItem)

        // 取消删除
        viewModel.onIntent(BookshelfIntent.DismissDeleteDialog)
        assertThat(viewModel.uiState.value.bookToDelete).isNull()

        // 确认删除
        viewModel.onIntent(BookshelfIntent.ConfirmDelete(targetItem.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.bookToDelete).isNull()
        assertThat(state.allBooks.none { it.id == targetItem.id }).isTrue()
        assertThat(state.userMessage).contains("已移除")
    }

    @Test
    fun `test batch mode selection and batch delete`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 开启批量模式
        viewModel.onIntent(BookshelfIntent.SetBatchMode(true))
        var state = viewModel.uiState.value
        assertThat(state.isBatchMode).isTrue()

        val firstId = state.displayedBooks[0].id
        val secondId = state.displayedBooks[1].id

        viewModel.onIntent(BookshelfIntent.ToggleBatchSelect(firstId))
        viewModel.onIntent(BookshelfIntent.ToggleBatchSelect(secondId))
        state = viewModel.uiState.value
        assertThat(state.selectedBookIds).containsExactly(firstId, secondId)

        // 全选
        viewModel.onIntent(BookshelfIntent.SelectAllBooks)
        state = viewModel.uiState.value
        assertThat(state.selectedBookIds).hasSize(state.displayedBooks.size)

        // 清除选择
        viewModel.onIntent(BookshelfIntent.ClearBatchSelection)
        state = viewModel.uiState.value
        assertThat(state.selectedBookIds).isEmpty()

        // 选中一项后批量删除
        viewModel.onIntent(BookshelfIntent.ToggleBatchSelect(firstId))
        viewModel.onIntent(BookshelfIntent.BatchDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.isBatchMode).isFalse()
        assertThat(state.allBooks.none { it.id == firstId }).isTrue()
    }

    @Test
    fun `test import dialog and candidate management`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // 打开导入弹窗
        viewModel.onIntent(BookshelfIntent.OpenImportDialog)
        var state = viewModel.uiState.value
        assertThat(state.importDialogState.isVisible).isTrue()

        // 添加候选文件
        val candidates = listOf(
            ImportCandidate(
                uriString = "content://books/1",
                fileName = "史铁生 - 我与地坛.epub",
                fileSize = 1024L * 600,
                format = BookFormat.EPUB,
                isSelected = true
            ),
            ImportCandidate(
                uriString = "content://books/2",
                fileName = "钱钟书 - 围城.txt",
                fileSize = 1024L * 900,
                format = BookFormat.TXT,
                isSelected = true
            )
        )
        viewModel.onIntent(BookshelfIntent.AddCandidates(candidates))
        state = viewModel.uiState.value
        assertThat(state.importDialogState.candidates).hasSize(2)
        assertThat(state.importDialogState.selectedCount).isEqualTo(2)

        // 切换勾选
        viewModel.onIntent(BookshelfIntent.ToggleCandidate("content://books/1"))
        state = viewModel.uiState.value
        assertThat(state.importDialogState.selectedCount).isEqualTo(1)

        // 全选 / 取消全选
        viewModel.onIntent(BookshelfIntent.SetAllCandidatesSelected(true))
        state = viewModel.uiState.value
        assertThat(state.importDialogState.selectedCount).isEqualTo(2)

        // 执行导入
        viewModel.onIntent(BookshelfIntent.StartImport)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.importDialogState.isImporting).isFalse()
        assertThat(state.importDialogState.successCount).isEqualTo(2)
        assertThat(state.allBooks.map { it.title }).contains("我与地坛")
        assertThat(state.allBooks.map { it.title }).contains("围城")

        // 关闭弹窗
        viewModel.onIntent(BookshelfIntent.DismissImportDialog)
        assertThat(viewModel.uiState.value.importDialogState.isVisible).isFalse()
    }

    @Test
    fun `test direct book import and reorder`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val newBook = Book(
            id = 99L,
            title = "直接导入测试书",
            author = "作者测试",
            uriString = "file:///direct.epub",
            format = BookFormat.EPUB,
            fileSize = 1000L
        )

        viewModel.onIntent(BookshelfIntent.ImportBooks(listOf(newBook)))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertThat(state.allBooks.any { it.title == "直接导入测试书" }).isTrue()

        // 测试重新排序
        viewModel.onIntent(BookshelfIntent.ReorderBooks(0, 1))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.displayedBooks).isNotEmpty()
    }
}
