package com.reader.feature.bookshelf.mvi

import com.reader.feature.bookshelf.model.BookshelfCategory
import com.reader.feature.bookshelf.model.BookshelfItem
import com.reader.feature.bookshelf.model.BookshelfSortOrder
import com.reader.feature.bookshelf.model.BookshelfViewMode
import com.reader.feature.bookshelf.model.ImportDialogState

/**
 * 书架单向数据流 UI 状态。
 * 不可变数据类，完全由 [BookshelfViewModel] 状态机驱动。
 *
 * @property allBooks 书架全部书籍集合 (数据基准源)
 * @property displayedBooks 经分类筛选、搜索关键字过滤与排序后的最终展示列表
 * @property selectedCategory 当前激活的分类 Tab
 * @property viewMode 当前视图呈现模式 (网格 / 列表)
 * @property sortOrder 当前排序规则
 * @property searchQuery 当前搜索关键字
 * @property isSearchActive 搜索框是否展开激活
 * @property isLoading 全局初始加载中状态
 * @property userMessage 错误或提示信息 (展示后自动清除)
 * @property importDialogState 本地导入对话框状态
 * @property bookToDelete 待删除确认的书籍 (非空时弹出二次确认对话框)
 * @property categoryCounts 各分类书籍数量聚合统计 (用于 Tab 气泡角标)
 * @property selectedBookIds 批量模式已勾选的书籍 ID 集合
 * @property isBatchMode 是否处于批量选择操作模式
 */
data class BookshelfUiState(
    val allBooks: List<BookshelfItem> = emptyList(),
    val displayedBooks: List<BookshelfItem> = emptyList(),
    val selectedCategory: BookshelfCategory = BookshelfCategory.ALL,
    val viewMode: BookshelfViewMode = BookshelfViewMode.GRID,
    val sortOrder: BookshelfSortOrder = BookshelfSortOrder.RECENT_READ,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val importDialogState: ImportDialogState = ImportDialogState(),
    val bookToDelete: BookshelfItem? = null,
    val categoryCounts: Map<BookshelfCategory, Int> = emptyMap(),
    val selectedBookIds: Set<Long> = emptySet(),
    val isBatchMode: Boolean = false
) {
    /** 书架展示是否为空 */
    val isEmpty: Boolean get() = displayedBooks.isEmpty() && !isLoading

    /** 总书籍数量 */
    val totalCount: Int get() = allBooks.size

    /** 搜索结果为空标志 */
    val isSearchEmpty: Boolean get() = searchQuery.isNotBlank() && displayedBooks.isEmpty()
}
