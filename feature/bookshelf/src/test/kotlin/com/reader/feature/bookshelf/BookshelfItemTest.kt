package com.reader.feature.bookshelf

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.feature.bookshelf.model.BookshelfItem
import org.junit.Test

/**
 * [BookshelfItem] UI 模型单元测试。
 */
class BookshelfItemTest {

    @Test
    fun `test progress text formatting`() {
        val book = createTestBook(id = 1L, title = "测试书籍")

        val unreadItem = BookshelfItem(book = book, readingProgress = 0.0f)
        assertThat(unreadItem.progressText).isEqualTo("未读")
        assertThat(unreadItem.isReading).isFalse()
        assertThat(unreadItem.isCompleted).isFalse()

        val readingItem = BookshelfItem(book = book, readingProgress = 0.426f)
        assertThat(readingItem.progressText).isEqualTo("已读 42%")
        assertThat(readingItem.isReading).isTrue()
        assertThat(readingItem.isCompleted).isFalse()

        val completedItem = BookshelfItem(book = book, readingProgress = 1.0f)
        assertThat(completedItem.progressText).isEqualTo("已读完")
        assertThat(completedItem.isReading).isFalse()
        assertThat(completedItem.isCompleted).isTrue()

        val almostCompletedItem = BookshelfItem(book = book, readingProgress = 0.995f)
        assertThat(almostCompletedItem.isCompleted).isTrue()
    }

    @Test
    fun `test formatted file size`() {
        val book0 = createTestBook(fileSize = 0L)
        assertThat(BookshelfItem(book0).formattedFileSize).isEqualTo("0 B")

        val bookKb = createTestBook(fileSize = 1024L * 500)
        assertThat(BookshelfItem(bookKb).formattedFileSize).contains("KB")

        val bookMb = createTestBook(fileSize = 1024L * 1024 * 3)
        assertThat(BookshelfItem(bookMb).formattedFileSize).contains("MB")
    }

    @Test
    fun `test relative time formatting`() {
        val now = System.currentTimeMillis()
        assertThat(BookshelfItem.formatRelativeTime(0L)).isEqualTo("从未阅读")
        assertThat(BookshelfItem.formatRelativeTime(now - 10_000L)).isEqualTo("刚刚")
        assertThat(BookshelfItem.formatRelativeTime(now - 120_000L)).isEqualTo("2 分钟前")
        assertThat(BookshelfItem.formatRelativeTime(now - 7200_000L)).isEqualTo("2 小时前")
    }

    private fun createTestBook(
        id: Long = 1L,
        title: String = "书名",
        fileSize: Long = 1024L
    ): Book {
        return Book(
            id = id,
            title = title,
            author = "作者",
            coverPath = null,
            uriString = "file:///test.epub",
            format = BookFormat.EPUB,
            fileSize = fileSize,
            totalChapters = 10,
            addTime = System.currentTimeMillis(),
            lastReadTime = System.currentTimeMillis()
        )
    }
}
