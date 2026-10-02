package com.reader.feature.bookshelf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.core.model.Book
import com.reader.feature.bookshelf.data.BookshelfRepository
import com.reader.feature.bookshelf.data.DefaultBookshelfRepository
import com.reader.feature.bookshelf.model.BookshelfCategory
import com.reader.feature.bookshelf.model.BookshelfItem
import com.reader.feature.bookshelf.model.BookshelfSortOrder
import com.reader.feature.bookshelf.model.BookshelfViewMode
import com.reader.feature.bookshelf.model.ImportCandidate
import com.reader.feature.bookshelf.model.ImportCandidateStatus
import com.reader.feature.bookshelf.model.ImportDialogState
import com.reader.feature.bookshelf.mvi.BookshelfIntent
import com.reader.feature.bookshelf.mvi.BookshelfUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 书架业务逻辑 ViewModel。
 * 遵循 MVI 架构单向数据流契约，管理书籍列表流、分类过滤、排序、搜索、批量操作及本地导入。
 */
open class BookshelfViewModel(
    private val repository: BookshelfRepository = DefaultBookshelfRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookshelfUiState(isLoading = true))
    val uiState: StateFlow<BookshelfUiState> = _uiState.asStateFlow()

    // 内部过滤与排序状态流
    private val _categoryFlow = MutableStateFlow(BookshelfCategory.ALL)
    private val _sortOrderFlow = MutableStateFlow(BookshelfSortOrder.RECENT_READ)
    private val _searchQueryFlow = MutableStateFlow("")

    init {
        observeBooksAndFilter()
    }

    /**
     * 响应 UI 层意图分发。
     */
    fun onIntent(intent: BookshelfIntent) {
        when (intent) {
            is BookshelfIntent.LoadBookshelf -> {
                // 观察者流已自动保持最新，无需额外重复拉取
            }

            is BookshelfIntent.SelectCategory -> {
                _categoryFlow.value = intent.category
                _uiState.update { it.copy(selectedCategory = intent.category) }
            }

            is BookshelfIntent.SwitchViewMode -> {
                _uiState.update { it.copy(viewMode = intent.viewMode) }
            }

            is BookshelfIntent.ChangeSortOrder -> {
                _sortOrderFlow.value = intent.sortOrder
                _uiState.update { it.copy(sortOrder = intent.sortOrder) }
            }

            is BookshelfIntent.Search -> {
                _searchQueryFlow.value = intent.query
                _uiState.update { it.copy(searchQuery = intent.query) }
            }

            is BookshelfIntent.ToggleSearch -> {
                _uiState.update { state ->
                    state.copy(
                        isSearchActive = intent.active,
                        searchQuery = if (!intent.active) "" else state.searchQuery
                    )
                }
                if (!intent.active) {
                    _searchQueryFlow.value = ""
                }
            }

            is BookshelfIntent.ClearSearch -> {
                _searchQueryFlow.value = ""
                _uiState.update { it.copy(searchQuery = "") }
            }

            // ==================== 导入流程 ====================
            is BookshelfIntent.OpenImportDialog -> {
                _uiState.update { state ->
                    state.copy(
                        importDialogState = state.importDialogState.copy(
                            isVisible = true,
                            isImporting = false,
                            currentIndex = 0,
                            successCount = 0,
                            failureCount = 0,
                            skippedCount = 0,
                            scanErrorMessage = null
                        )
                    )
                }
            }

            is BookshelfIntent.DismissImportDialog -> {
                _uiState.update { state ->
                    state.copy(
                        importDialogState = state.importDialogState.copy(isVisible = false)
                    )
                }
            }

            is BookshelfIntent.AddCandidates -> {
                _uiState.update { state ->
                    val existingUris = state.importDialogState.candidates.map { it.uriString }.toSet()
                    val newUniqueCandidates = intent.candidates.filterNot { existingUris.contains(it.uriString) }
                    val merged = state.importDialogState.candidates + newUniqueCandidates
                    state.copy(
                        importDialogState = state.importDialogState.copy(candidates = merged)
                    )
                }
            }

            is BookshelfIntent.ToggleCandidate -> {
                _uiState.update { state ->
                    val updated = state.importDialogState.candidates.map { candidate ->
                        if (candidate.uriString == intent.uriString) {
                            candidate.copy(isSelected = !candidate.isSelected)
                        } else {
                            candidate
                        }
                    }
                    state.copy(
                        importDialogState = state.importDialogState.copy(candidates = updated)
                    )
                }
            }

            is BookshelfIntent.SetAllCandidatesSelected -> {
                _uiState.update { state ->
                    val updated = state.importDialogState.candidates.map { candidate ->
                        if (candidate.status != ImportCandidateStatus.SKIPPED_ALREADY_EXISTS) {
                            candidate.copy(isSelected = intent.selected)
                        } else {
                            candidate
                        }
                    }
                    state.copy(
                        importDialogState = state.importDialogState.copy(candidates = updated)
                    )
                }
            }

            is BookshelfIntent.ScanDirectory -> {
                scanDirectory(intent.path)
            }

            is BookshelfIntent.StartImport -> {
                executeBatchImport()
            }

            is BookshelfIntent.ImportBooks -> {
                importBooksDirectly(intent.books)
            }

            // ==================== 书籍管理 ====================
            is BookshelfIntent.RequestDelete -> {
                _uiState.update { it.copy(bookToDelete = intent.item) }
            }

            is BookshelfIntent.DismissDeleteDialog -> {
                _uiState.update { it.copy(bookToDelete = null) }
            }

            is BookshelfIntent.ConfirmDelete -> {
                confirmDelete(intent.bookId)
            }

            is BookshelfIntent.ToggleFavorite -> {
                toggleFavorite(intent.bookId)
            }

            is BookshelfIntent.TogglePin -> {
                togglePin(intent.bookId)
            }

            is BookshelfIntent.ReorderBooks -> {
                reorderBooks(intent.fromIndex, intent.toIndex)
            }

            is BookshelfIntent.UpdateReadingProgress -> {
                updateReadingProgress(intent.bookId, intent.progress, intent.chapterTitle)
            }

            // ==================== 批量操作 ====================
            is BookshelfIntent.SetBatchMode -> {
                _uiState.update {
                    it.copy(
                        isBatchMode = intent.active,
                        selectedBookIds = if (!intent.active) emptySet() else it.selectedBookIds
                    )
                }
            }

            is BookshelfIntent.ToggleBatchSelect -> {
                _uiState.update { state ->
                    val newSelection = state.selectedBookIds.toMutableSet()
                    if (newSelection.contains(intent.bookId)) {
                        newSelection.remove(intent.bookId)
                    } else {
                        newSelection.add(intent.bookId)
                    }
                    state.copy(selectedBookIds = newSelection)
                }
            }

            is BookshelfIntent.SelectAllBooks -> {
                _uiState.update { state ->
                    val allIds = state.displayedBooks.map { it.id }.toSet()
                    state.copy(selectedBookIds = allIds)
                }
            }

            is BookshelfIntent.ClearBatchSelection -> {
                _uiState.update { it.copy(selectedBookIds = emptySet()) }
            }

            is BookshelfIntent.BatchDelete -> {
                executeBatchDelete()
            }

            is BookshelfIntent.DismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    /**
     * 响应式流管道：组合书籍集合、分类过滤、排序和搜索关键字。
     */
    private fun observeBooksAndFilter() {
        combine(
            repository.getBooks(),
            _categoryFlow,
            _sortOrderFlow,
            _searchQueryFlow
        ) { books, category, sortOrder, query ->
            val counts = calculateCategoryCounts(books)
            val filtered = filterAndSortBooks(books, category, sortOrder, query)
            FilteredResult(books, filtered, counts)
        }.onEach { result ->
            _uiState.update { state ->
                state.copy(
                    allBooks = result.allBooks,
                    displayedBooks = result.filteredBooks,
                    categoryCounts = result.counts,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    /**
     * 计算各分类徽章数量。
     */
    private fun calculateCategoryCounts(books: List<BookshelfItem>): Map<BookshelfCategory, Int> {
        val allCount = books.size
        val readingCount = books.count { it.isReading }
        val completedCount = books.count { it.isCompleted }
        val favoriteCount = books.count { it.isFavorite }

        return mapOf(
            BookshelfCategory.ALL to allCount,
            BookshelfCategory.READING to readingCount,
            BookshelfCategory.COMPLETED to completedCount,
            BookshelfCategory.FAVORITE to favoriteCount
        )
    }

    /**
     * 执行过滤与排序算法。
     */
    private fun filterAndSortBooks(
        books: List<BookshelfItem>,
        category: BookshelfCategory,
        sortOrder: BookshelfSortOrder,
        query: String
    ): List<BookshelfItem> {
        // 1. 分类过滤
        val categoryFiltered = when (category) {
            BookshelfCategory.ALL -> books
            BookshelfCategory.READING -> books.filter { it.isReading }
            BookshelfCategory.COMPLETED -> books.filter { it.isCompleted }
            BookshelfCategory.FAVORITE -> books.filter { it.isFavorite }
        }

        // 2. 搜索关键字过滤 (不区分大小写，同时匹配书名与作者)
        val searchFiltered = if (query.isBlank()) {
            categoryFiltered
        } else {
            val trimmed = query.trim().lowercase()
            categoryFiltered.filter { item ->
                item.title.lowercase().contains(trimmed) || item.author.lowercase().contains(trimmed)
            }
        }

        // 3. 排序 (置顶书籍统一居前)
        val pinned = searchFiltered.filter { it.isPinned }
        val unpinned = searchFiltered.filter { !it.isPinned }

        val sortComparator: Comparator<BookshelfItem> = when (sortOrder) {
            BookshelfSortOrder.RECENT_READ -> compareByDescending<BookshelfItem> { it.lastReadTime }
                .thenByDescending { it.addTime }

            BookshelfSortOrder.TITLE -> compareBy { it.title.lowercase() }

            BookshelfSortOrder.ADD_TIME -> compareByDescending { it.addTime }

            BookshelfSortOrder.PROGRESS -> compareByDescending { it.readingProgress }

            BookshelfSortOrder.MANUAL -> compareBy { it.customOrder }
        }

        return pinned.sortedWith(sortComparator) + unpinned.sortedWith(sortComparator)
    }

    /**
     * 扫描本地目录下的受支持书籍。
     */
    private fun scanDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    importDialogState = state.importDialogState.copy(
                        isScanning = true,
                        scanPath = path,
                        scanErrorMessage = null
                    )
                )
            }

            try {
                val candidates = repository.scanDirectory(path)
                _uiState.update { state ->
                    state.copy(
                        importDialogState = state.importDialogState.copy(
                            isScanning = false,
                            candidates = candidates,
                            scanErrorMessage = if (candidates.isEmpty()) "未找到受支持的电子书文件" else null
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        importDialogState = state.importDialogState.copy(
                            isScanning = false,
                            scanErrorMessage = "扫描目录失败: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    /**
     * 批量执行选中的候选文件导入任务。
     */
    private fun executeBatchImport() {
        val selectedCandidates = _uiState.value.importDialogState.candidates.filter { it.isSelected }
        if (selectedCandidates.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    importDialogState = state.importDialogState.copy(
                        isImporting = true,
                        currentIndex = 0,
                        successCount = 0,
                        failureCount = 0,
                        skippedCount = 0
                    )
                )
            }

            var success = 0
            var failure = 0
            var skipped = 0

            for ((index, candidate) in selectedCandidates.withIndex()) {
                // 更新当前正在处理的文件名与进度索引
                _uiState.update { state ->
                    val updatedCandidates = state.importDialogState.candidates.map {
                        if (it.uriString == candidate.uriString) {
                            it.copy(status = ImportCandidateStatus.IMPORTING)
                        } else {
                            it
                        }
                    }
                    state.copy(
                        importDialogState = state.importDialogState.copy(
                            candidates = updatedCandidates,
                            currentIndex = index + 1,
                            currentFileName = candidate.fileName
                        )
                    )
                }

                if (candidate.status == ImportCandidateStatus.SKIPPED_ALREADY_EXISTS) {
                    skipped++
                    continue
                }

                val result = repository.importCandidate(candidate)
                if (result.isSuccess) {
                    success++
                    _uiState.update { state ->
                        val updatedCandidates = state.importDialogState.candidates.map {
                            if (it.uriString == candidate.uriString) {
                                it.copy(status = ImportCandidateStatus.SUCCESS)
                            } else {
                                it
                            }
                        }
                        state.copy(
                            importDialogState = state.importDialogState.copy(
                                candidates = updatedCandidates,
                                successCount = success
                            )
                        )
                    }
                } else {
                    failure++
                    val errorMsg = result.exceptionOrNull()?.message ?: "导入失败"
                    _uiState.update { state ->
                        val updatedCandidates = state.importDialogState.candidates.map {
                            if (it.uriString == candidate.uriString) {
                                it.copy(status = ImportCandidateStatus.FAILED, errorMessage = errorMsg)
                            } else {
                                it
                            }
                        }
                        state.copy(
                            importDialogState = state.importDialogState.copy(
                                candidates = updatedCandidates,
                                failureCount = failure
                            )
                        )
                    }
                }
            }

            _uiState.update { state ->
                state.copy(
                    importDialogState = state.importDialogState.copy(
                        isImporting = false,
                        currentFileName = null
                    ),
                    userMessage = "成功导入 $success 本书" + if (failure > 0) "，失败 $failure 本" else ""
                )
            }
        }
    }

    /**
     * 直接导入书籍对象。
     */
    private fun importBooksDirectly(books: List<Book>) {
        if (books.isEmpty()) return
        viewModelScope.launch {
            try {
                val ids = repository.importBooks(books)
                _uiState.update {
                    it.copy(userMessage = "成功添加 ${ids.size} 本书籍")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "导入失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 确认删除单本书籍。
     */
    private fun confirmDelete(bookId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteBook(bookId)
                _uiState.update {
                    it.copy(
                        bookToDelete = null,
                        userMessage = "已移除书籍"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        bookToDelete = null,
                        userMessage = "删除失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 切换收藏状态。
     */
    private fun toggleFavorite(bookId: Long) {
        val item = _uiState.value.allBooks.find { it.id == bookId } ?: return
        viewModelScope.launch {
            repository.setFavorite(bookId, !item.isFavorite)
        }
    }

    /**
     * 切换置顶状态。
     */
    private fun togglePin(bookId: Long) {
        val item = _uiState.value.allBooks.find { it.id == bookId } ?: return
        viewModelScope.launch {
            repository.setPinned(bookId, !item.isPinned)
        }
    }

    /**
     * 拖拽或调整书籍排序。
     */
    private fun reorderBooks(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.displayedBooks.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return

        val movedItem = currentList.removeAt(fromIndex)
        currentList.add(toIndex, movedItem)

        // 重新分配 customOrder
        val orders = currentList.mapIndexed { index, item ->
            Pair(item.id, index)
        }

        viewModelScope.launch {
            repository.updateCustomOrder(orders)
        }
    }

    /**
     * 更新阅读进度。
     */
    private fun updateReadingProgress(bookId: Long, progress: Float, chapterTitle: String?) {
        viewModelScope.launch {
            repository.updateReadingProgress(bookId, progress, chapterTitle)
        }
    }

    /**
     * 批量删除已选书籍。
     */
    private fun executeBatchDelete() {
        val selectedIds = _uiState.value.selectedBookIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.deleteBooks(selectedIds)
                _uiState.update {
                    it.copy(
                        selectedBookIds = emptySet(),
                        isBatchMode = false,
                        userMessage = "已删除 ${selectedIds.size} 本书籍"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "批量删除失败: ${e.message}")
                }
            }
        }
    }

    private data class FilteredResult(
        val allBooks: List<BookshelfItem>,
        val filteredBooks: List<BookshelfItem>,
        val counts: Map<BookshelfCategory, Int>
    )
}
