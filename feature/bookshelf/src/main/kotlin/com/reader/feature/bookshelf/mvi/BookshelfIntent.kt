package com.reader.feature.bookshelf.mvi

import com.reader.core.model.Book
import com.reader.feature.bookshelf.model.BookshelfCategory
import com.reader.feature.bookshelf.model.BookshelfItem
import com.reader.feature.bookshelf.model.BookshelfSortOrder
import com.reader.feature.bookshelf.model.BookshelfViewMode
import com.reader.feature.bookshelf.model.ImportCandidate

/**
 * 书架用户交互与系统驱动 Intent (MVI 架构单向意图)。
 */
sealed interface BookshelfIntent {
    /** 刷新加载书架 */
    data object LoadBookshelf : BookshelfIntent

    /** 切换分类 Tab */
    data class SelectCategory(val category: BookshelfCategory) : BookshelfIntent

    /** 切换网格 / 列表视图模式 */
    data class SwitchViewMode(val viewMode: BookshelfViewMode) : BookshelfIntent

    /** 更改书籍排序方式 */
    data class ChangeSortOrder(val sortOrder: BookshelfSortOrder) : BookshelfIntent

    /** 搜索书籍关键字输入 */
    data class Search(val query: String) : BookshelfIntent

    /** 切换搜索栏展开/关闭状态 */
    data class ToggleSearch(val active: Boolean) : BookshelfIntent

    /** 清空搜索关键字 */
    data object ClearSearch : BookshelfIntent

    // ==================== 导入流程 Intent ====================
    /** 打开导入对话框 */
    data object OpenImportDialog : BookshelfIntent

    /** 关闭导入对话框 */
    data object DismissImportDialog : BookshelfIntent

    /** 添加 SAF 选择的文件为候选导入项 */
    data class AddCandidates(val candidates: List<ImportCandidate>) : BookshelfIntent

    /** 切换单个候选文件的选中状态 */
    data class ToggleCandidate(val uriString: String) : BookshelfIntent

    /** 全选 / 取消全选候选文件 */
    data class SetAllCandidatesSelected(val selected: Boolean) : BookshelfIntent

    /** 扫描指定本地目录 */
    data class ScanDirectory(val path: String) : BookshelfIntent

    /** 开始执行选中的候选文件导入 */
    data object StartImport : BookshelfIntent

    /** 直接导入单本或多本书籍对象 */
    data class ImportBooks(val books: List<Book>) : BookshelfIntent

    // ==================== 书籍管理 Intent ====================
    /** 请求删除单本书籍 (弹出确认框) */
    data class RequestDelete(val item: BookshelfItem) : BookshelfIntent

    /** 取消删除对话框 */
    data object DismissDeleteDialog : BookshelfIntent

    /** 确认删除单本书籍 */
    data class ConfirmDelete(val bookId: Long) : BookshelfIntent

    /** 切换收藏星标 */
    data class ToggleFavorite(val bookId: Long) : BookshelfIntent

    /** 置顶书籍 / 取消置顶 */
    data class TogglePin(val bookId: Long) : BookshelfIntent

    /** 拖拽或手动重排序 */
    data class ReorderBooks(val fromIndex: Int, val toIndex: Int) : BookshelfIntent

    /** 更新阅读进度 (供阅读器联动或测试) */
    data class UpdateReadingProgress(val bookId: Long, val progress: Float, val chapterTitle: String? = null) : BookshelfIntent

    // ==================== 批量操作 Intent ====================
    /** 切换批量模式 */
    data class SetBatchMode(val active: Boolean) : BookshelfIntent

    /** 批量模式下勾选 / 取消勾选单本 */
    data class ToggleBatchSelect(val bookId: Long) : BookshelfIntent

    /** 批量模式全选 */
    data object SelectAllBooks : BookshelfIntent

    /** 清除所有勾选 */
    data object ClearBatchSelection : BookshelfIntent

    /** 批量删除已选书籍 */
    data object BatchDelete : BookshelfIntent

    // ==================== 提示与信息 ====================
    /** 清除用户消息提示 */
    data object DismissMessage : BookshelfIntent
}
