package com.reader.feature.reader

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.core.model.Chapter
import org.junit.Test

/**
 * 针对 [ReaderUiState] 核心衍生属性与边界保护的单元测试。
 */
class ReaderUiStateTest {

    private val testBook = Book(
        id = 1L,
        title = "测试书籍",
        author = "测试作者",
        uriString = "file:///test.txt",
        format = BookFormat.TXT,
        fileSize = 1024L,
        totalChapters = 3
    )

    private val testChapters = listOf(
        Chapter(id = 1L, bookId = 1L, index = 0, title = "第1章", startOffset = 0L, endOffset = 100L),
        Chapter(id = 2L, bookId = 1L, index = 1, title = "第2章", startOffset = 100L, endOffset = 200L),
        Chapter(id = 3L, bookId = 1L, index = 2, title = "第3章", startOffset = 200L, endOffset = 300L)
    )

    @Test
    fun `displayChapters returns natural order when isChaptersReversed is false`() {
        val state = ReaderUiState(
            chapters = testChapters,
            isChaptersReversed = false
        )
        assertThat(state.displayChapters).containsExactlyElementsIn(testChapters).inOrder()
    }

    @Test
    fun `displayChapters returns reversed order when isChaptersReversed is true`() {
        val state = ReaderUiState(
            chapters = testChapters,
            isChaptersReversed = true
        )
        assertThat(state.displayChapters).containsExactly(
            testChapters[2],
            testChapters[1],
            testChapters[0]
        ).inOrder()
    }

    @Test
    fun `totalProgressPercent formats accurately and clamps within 0 to 100 percent`() {
        val state1 = ReaderUiState(totalProgress = 0.3582f)
        assertThat(state1.totalProgressPercent).isEqualTo("35.8%")

        val stateZero = ReaderUiState(totalProgress = 0.0f)
        assertThat(stateZero.totalProgressPercent).isEqualTo("0.0%")

        val stateFull = ReaderUiState(totalProgress = 1.0f)
        assertThat(stateFull.totalProgressPercent).isEqualTo("100.0%")

        val stateOver = ReaderUiState(totalProgress = 1.5f)
        assertThat(stateOver.totalProgressPercent).isEqualTo("100.0%")

        val stateUnder = ReaderUiState(totalProgress = -0.5f)
        assertThat(stateUnder.totalProgressPercent).isEqualTo("0.0%")
    }

    @Test
    fun `chapterProgressPercent formats accurately`() {
        val state = ReaderUiState(chapterProgress = 0.75f)
        assertThat(state.chapterProgressPercent).isEqualTo("75.0%")
    }

    @Test
    fun `pageIndicatorText formats 1-based page index correctly`() {
        val state = ReaderUiState(
            currentPageIndex = 2,
            totalPagesInChapter = 10
        )
        assertThat(state.pageIndicatorText).isEqualTo("3 / 10")
    }

    @Test
    fun `hasPrevChapter and hasNextChapter boundary checks`() {
        // 第一章
        val stateFirst = ReaderUiState(
            chapters = testChapters,
            currentChapter = testChapters[0]
        )
        assertThat(stateFirst.hasPrevChapter).isFalse()
        assertThat(stateFirst.hasNextChapter).isTrue()

        // 中间章
        val stateMiddle = ReaderUiState(
            chapters = testChapters,
            currentChapter = testChapters[1]
        )
        assertThat(stateMiddle.hasPrevChapter).isTrue()
        assertThat(stateMiddle.hasNextChapter).isTrue()

        // 最终章
        val stateLast = ReaderUiState(
            chapters = testChapters,
            currentChapter = testChapters[2]
        )
        assertThat(stateLast.hasPrevChapter).isTrue()
        assertThat(stateLast.hasNextChapter).isFalse()
    }
}
