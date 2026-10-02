package com.reader.feature.bookshelf

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.feature.bookshelf.data.DefaultBookshelfRepository
import com.reader.feature.bookshelf.model.BookshelfItem
import com.reader.feature.bookshelf.model.ImportCandidate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * [DefaultBookshelfRepository] 单元测试。
 */
class BookshelfRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `test importBooks and getBooks flow`() = runTest(testDispatcher) {
        val repository = DefaultBookshelfRepository(ioDispatcher = testDispatcher)

        val books = listOf(
            Book(
                title = "鲁迅 - 呐喊",
                uriString = "file:///storage/nahan.epub",
                format = BookFormat.EPUB,
                fileSize = 1024L * 500
            ),
            Book(
                title = "老舍 - 骆驼祥子",
                uriString = "file:///storage/xiangzi.txt",
                format = BookFormat.TXT,
                fileSize = 1024L * 800
            )
        )

        val ids = repository.importBooks(books)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(ids).hasSize(2)
        val loadedBooks = repository.getBooks().first()
        assertThat(loadedBooks).hasSize(2)
        assertThat(loadedBooks.map { it.title }).containsExactly("鲁迅 - 呐喊", "老舍 - 骆驼祥子")
    }

    @Test
    fun `test importCandidate parsing author and title`() = runTest(testDispatcher) {
        val repository = DefaultBookshelfRepository(ioDispatcher = testDispatcher)

        val candidate = ImportCandidate(
            uriString = "content://media/external/123",
            fileName = "刘慈欣 - 三体.epub",
            fileSize = 1024 * 1024 * 2L,
            format = BookFormat.EPUB
        )

        val result = repository.importCandidate(candidate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(result.isSuccess).isTrue()
        val item = result.getOrThrow()
        assertThat(item.author).isEqualTo("刘慈欣")
        assertThat(item.title).isEqualTo("三体")
        assertThat(item.format).isEqualTo(BookFormat.EPUB)
    }

    @Test
    fun `test updateReadingProgress and favorite and pin`() = runTest(testDispatcher) {
        val initialItem = BookshelfItem(
            book = Book(
                id = 10L,
                title = "测试书",
                uriString = "file:///test.txt",
                format = BookFormat.TXT,
                fileSize = 1000L
            )
        )
        val repository = DefaultBookshelfRepository(
            initialBooks = listOf(initialItem),
            ioDispatcher = testDispatcher
        )

        // 更新阅读进度
        repository.updateReadingProgress(10L, 0.75f, "第十章")
        testDispatcher.scheduler.advanceUntilIdle()
        var current = repository.getBooks().first().first()
        assertThat(current.readingProgress).isEqualTo(0.75f)
        assertThat(current.lastReadChapterTitle).isEqualTo("第十章")

        // 切换收藏
        repository.setFavorite(10L, true)
        testDispatcher.scheduler.advanceUntilIdle()
        current = repository.getBooks().first().first()
        assertThat(current.isFavorite).isTrue()

        // 切换置顶
        repository.setPinned(10L, true)
        testDispatcher.scheduler.advanceUntilIdle()
        current = repository.getBooks().first().first()
        assertThat(current.isPinned).isTrue()
    }

    @Test
    fun `test deleteBook and deleteBooks`() = runTest(testDispatcher) {
        val initial = DefaultBookshelfRepository.createSampleData()
        val repository = DefaultBookshelfRepository(
            initialBooks = initial,
            ioDispatcher = testDispatcher
        )

        val bookIdToDelete = initial[0].id
        repository.deleteBook(bookIdToDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        val afterSingleDelete = repository.getBooks().first()
        assertThat(afterSingleDelete.map { it.id }).doesNotContain(bookIdToDelete)

        val multipleIds = afterSingleDelete.take(2).map { it.id }
        repository.deleteBooks(multipleIds)
        testDispatcher.scheduler.advanceUntilIdle()

        val afterBatchDelete = repository.getBooks().first()
        assertThat(afterBatchDelete.map { it.id }).containsNoneIn(multipleIds)
    }
}
