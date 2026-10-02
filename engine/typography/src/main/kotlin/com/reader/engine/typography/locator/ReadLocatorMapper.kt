package com.reader.engine.typography.locator

import com.reader.core.model.ReadLocator
import com.reader.engine.typography.model.ReaderPage

/**
 * 统一定位器自适应映射器 (ReadLocatorMapper)。
 *
 * 核心设计目标：
 * 当用户更改字号、行距、边距或旋转屏幕触发全章重新排版分页后，
 * 利用 [ReadLocator.charOffset] (字符级绝对偏移量) 在全新的物理分页列表 `List<ReaderPage>` 中
 * 瞬间毫秒级定位到目标页码，实现无感、无缝的阅读进度恢复。
 */
object ReadLocatorMapper {

    /**
     * 根据章节内字符绝对偏移量 [charOffset]，在重排后的物理页面列表中精确定位目标页码。
     *
     * @param pages 新排版生成的物理页面列表
     * @param charOffset 待定位的目标字符绝对偏移 (0-indexed)
     * @return 目标页面索引 (0-based，范围在 0 .. pages.lastIndex)
     */
    fun locatePageByCharOffset(pages: List<ReaderPage>, charOffset: Int): Int {
        if (pages.isEmpty()) return 0
        if (charOffset <= 0) return 0
        if (charOffset >= pages.last().endCharOffset) return pages.lastIndex

        // 二分查找：页面 startCharOffset 严格单调递增
        var low = 0
        var high = pages.lastIndex

        while (low <= high) {
            val mid = (low + high) ushr 1
            val page = pages[mid]

            when {
                charOffset < page.startCharOffset -> high = mid - 1
                charOffset >= page.endCharOffset && !page.isLastPageOfChapter -> low = mid + 1
                else -> return mid // 落在本页区间 [startCharOffset, endCharOffset) 内
            }
        }

        // 兜底保护
        return low.coerceIn(0, pages.lastIndex)
    }

    /**
     * 根据当前正在阅读的物理页面生成全新的 [ReadLocator]。
     *
     * @param bookId 书籍 ID
     * @param chapterIndex 章节序号
     * @param chapterTitle 章节标题
     * @param page 当前物理页面
     * @param totalPagesInChapter 章节总物理页数
     * @param totalChapterLength 章节全文总字符长度
     * @return 最新的 [ReadLocator] 实例
     */
    fun createLocator(
        bookId: Long,
        chapterIndex: Int,
        chapterTitle: String,
        page: ReaderPage,
        totalPagesInChapter: Int,
        totalChapterLength: Int
    ): ReadLocator {
        val safeTotalLength = totalChapterLength.coerceAtLeast(1)
        val safeCharOffset = page.startCharOffset.coerceIn(0, totalChapterLength)
        val progression = (safeCharOffset.toFloat() / safeTotalLength).coerceIn(0f, 1f)

        return ReadLocator(
            bookId = bookId,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            charOffset = safeCharOffset,
            progression = progression,
            pageIndexInChapter = page.pageIndex,
            totalPagesInChapter = totalPagesInChapter.coerceAtLeast(1),
            updateTime = System.currentTimeMillis()
        )
    }

    /**
     * 重排排版发生后，自适应更新 [ReadLocator] 的当前页码与总页数。
     * 保留原有 [ReadLocator.charOffset]，重新映射 [ReadLocator.pageIndexInChapter] 与 [ReadLocator.totalPagesInChapter]。
     *
     * @param originalLocator 重排前的历史定位器
     * @param newPages 重排后生成的全新物理页面列表
     * @param totalChapterLength 章节全文总字符数 (可选，用于重新核准 progression)
     * @return 刷新映射后的新 [ReadLocator]
     */
    fun updateLocatorForRepagination(
        originalLocator: ReadLocator,
        newPages: List<ReaderPage>,
        totalChapterLength: Int = 0
    ): ReadLocator {
        if (newPages.isEmpty()) {
            return originalLocator.copy(
                pageIndexInChapter = 0,
                totalPagesInChapter = 1,
                updateTime = System.currentTimeMillis()
            )
        }

        val newPageIndex = locatePageByCharOffset(newPages, originalLocator.charOffset)
        val totalPages = newPages.size

        val progression = if (totalChapterLength > 0) {
            (originalLocator.charOffset.toFloat() / totalChapterLength.toFloat()).coerceIn(0f, 1f)
        } else {
            originalLocator.progression
        }

        return originalLocator.copy(
            pageIndexInChapter = newPageIndex,
            totalPagesInChapter = totalPages,
            progression = progression,
            updateTime = System.currentTimeMillis()
        )
    }

    /**
     * 获取指定页面的首字符偏移量。
     */
    fun getCharOffsetForPage(pages: List<ReaderPage>, pageIndex: Int): Int {
        if (pages.isEmpty()) return 0
        val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
        return pages[safeIndex].startCharOffset
    }
}
