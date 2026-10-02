package com.reader.engine.typography

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReaderConfig
import com.reader.engine.typography.layout.TextMeasureEngine
import com.reader.engine.typography.locator.ReadLocatorMapper
import com.reader.engine.typography.model.PageDimensions
import com.reader.engine.typography.model.ReaderPage
import com.reader.engine.typography.splitter.PageSplitter
import org.junit.Test

/**
 * 统一定位器自适应映射器 (ReadLocatorMapper) 单元测试。
 */
class ReadLocatorMapperTest {

    @Test
    fun `test locate page by char offset with discrete pages`() {
        val pages = listOf(
            ReaderPage(pageIndex = 0, startCharOffset = 0, endCharOffset = 100, isFirstPageOfChapter = true),
            ReaderPage(pageIndex = 1, startCharOffset = 100, endCharOffset = 250),
            ReaderPage(pageIndex = 2, startCharOffset = 250, endCharOffset = 400, isLastPageOfChapter = true)
        )

        // 边界与内部测试
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 0)).isEqualTo(0)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 50)).isEqualTo(0)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 99)).isEqualTo(0)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 100)).isEqualTo(1)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 200)).isEqualTo(1)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 249)).isEqualTo(1)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 250)).isEqualTo(2)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 399)).isEqualTo(2)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 400)).isEqualTo(2)

        // 超界安全防御
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, -5)).isEqualTo(0)
        assertThat(ReadLocatorMapper.locatePageByCharOffset(pages, 1000)).isEqualTo(2)
    }

    @Test
    fun `test repagination restores accurate reading page on font size change`() {
        val chapterText = (1..15).joinToString("\n") {
            "这是第 $it 个用于排版测试的段落，我们将通过字号从 16sp 调大到 24sp，验证排版重排后字符偏移精确定位新页码的能力。"
        }
        val dimensions = PageDimensions(viewWidth = 300f, viewHeight = 400f, paddingLeft = 16f, paddingTop = 16f, paddingRight = 16f, paddingBottom = 16f)

        // 1. 初始排版：字号 16sp
        val smallEngine = TextMeasureEngine.createStandard(fontSizePx = 16f)
        val smallSplitter = PageSplitter(smallEngine)
        val initialPages = smallSplitter.splitChapter(chapterText, dimensions)

        // 假定用户读到第 350 个字符
        val targetCharOffset = 350
        val initialPageIndex = ReadLocatorMapper.locatePageByCharOffset(initialPages, targetCharOffset)
        val initialLocator = ReadLocator(
            bookId = 1001L,
            chapterIndex = 0,
            chapterTitle = "第一章",
            charOffset = targetCharOffset,
            pageIndexInChapter = initialPageIndex,
            totalPagesInChapter = initialPages.size
        )

        assertThat(initialPages[initialPageIndex].containsCharOffset(targetCharOffset)).isTrue()

        // 2. 用户调大字号到 26sp，触发全章重排
        val largeEngine = TextMeasureEngine.createStandard(fontSizePx = 26f)
        val largeSplitter = PageSplitter(largeEngine)
        val newPages = largeSplitter.splitChapter(chapterText, dimensions)

        // 字号调大后，总页数必然增加
        assertThat(newPages.size).isGreaterThan(initialPages.size)

        // 3. 执行自适应重排恢复
        val restoredLocator = ReadLocatorMapper.updateLocatorForRepagination(
            originalLocator = initialLocator,
            newPages = newPages,
            totalChapterLength = chapterText.length
        )

        // 核心验证：
        // (1) charOffset 严格保持 350 不变；
        // (2) 新页码在 newPages 中必定覆盖 charOffset = 350；
        // (3) totalPagesInChapter 准确刷新为 newPages.size。
        assertThat(restoredLocator.charOffset).isEqualTo(targetCharOffset)
        assertThat(restoredLocator.totalPagesInChapter).isEqualTo(newPages.size)

        val newTargetPage = newPages[restoredLocator.pageIndexInChapter]
        assertThat(newTargetPage.containsCharOffset(targetCharOffset)).isTrue()
    }

    @Test
    fun `test create locator from page`() {
        val page = ReaderPage(
            pageIndex = 2,
            chapterIndex = 0,
            startCharOffset = 200,
            endCharOffset = 300
        )

        val locator = ReadLocatorMapper.createLocator(
            bookId = 42L,
            chapterIndex = 0,
            chapterTitle = "序章",
            page = page,
            totalPagesInChapter = 5,
            totalChapterLength = 500
        )

        assertThat(locator.bookId).isEqualTo(42L)
        assertThat(locator.charOffset).isEqualTo(200)
        assertThat(locator.pageIndexInChapter).isEqualTo(2)
        assertThat(locator.totalPagesInChapter).isEqualTo(5)
        assertThat(locator.progression).isEqualTo(200f / 500f)
    }
}
