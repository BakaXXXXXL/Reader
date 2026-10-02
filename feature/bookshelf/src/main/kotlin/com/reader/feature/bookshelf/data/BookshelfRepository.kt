package com.reader.feature.bookshelf.data

import com.reader.core.model.Book
import com.reader.feature.bookshelf.model.BookshelfItem
import com.reader.feature.bookshelf.model.ImportCandidate
import kotlinx.coroutines.flow.Flow

/**
 * 书架数据层契约接口。
 * 遵循 Clean Architecture 规范，向下解耦底层持久化实现。
 */
interface BookshelfRepository {
    /** 观察所有书架书籍列表流 */
    fun getBooks(): Flow<List<BookshelfItem>>

    /** 批量直接导入书籍 */
    suspend fun importBooks(books: List<Book>): List<Long>

    /** 导入单个候选文件 */
    suspend fun importCandidate(candidate: ImportCandidate): Result<BookshelfItem>

    /** 扫描指定本地目录下的受支持电子书文件 */
    suspend fun scanDirectory(dirPath: String): List<ImportCandidate>

    /** 更新阅读进度 */
    suspend fun updateReadingProgress(bookId: Long, progress: Float, chapterTitle: String? = null)

    /** 设置收藏状态 */
    suspend fun setFavorite(bookId: Long, isFavorite: Boolean)

    /** 设置置顶状态 */
    suspend fun setPinned(bookId: Long, isPinned: Boolean)

    /** 删除单本书籍 */
    suspend fun deleteBook(bookId: Long)

    /** 批量删除书籍 */
    suspend fun deleteBooks(bookIds: List<Long>)

    /** 更新书籍排序权重 */
    suspend fun updateCustomOrder(bookOrders: List<Pair<Long, Int>>)
}
