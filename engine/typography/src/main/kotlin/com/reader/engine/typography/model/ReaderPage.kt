package com.reader.engine.typography.model

/**
 * 物理页面排版结果实体。
 * 记录页面切分后的行列表以及在章节全局文本流中的字符偏移区间 [startCharOffset, endCharOffset)。
 *
 * @property pageIndex 当前章节内的页码序号 (0-based)
 * @property chapterIndex 所属章节序号 (0-based)
 * @property lines 页面包含的所有文本行列表
 * @property startCharOffset 本页首字符在章节内的绝对偏移
 * @property endCharOffset 本页末字符后一位在章节内的绝对偏移 (开区间，有效文本区间 [start, end))
 * @property isFirstPageOfChapter 是否为章节首页
 * @property isLastPageOfChapter 是否为章节末页
 */
data class ReaderPage(
    val pageIndex: Int,
    val chapterIndex: Int = 0,
    val lines: List<TextLine> = emptyList(),
    val startCharOffset: Int,
    val endCharOffset: Int,
    val isFirstPageOfChapter: Boolean = false,
    val isLastPageOfChapter: Boolean = false
) {
    /** 本页包含的字符总数 */
    val charCount: Int
        get() = (endCharOffset - startCharOffset).coerceAtLeast(0)

    /** 本页是否为空页 */
    val isEmpty: Boolean
        get() = lines.isEmpty() || charCount == 0

    /**
     * 判断指定的章节内字符偏移量是否落在本页范围内。
     * 最后一页包含闭区间 endCharOffset。
     */
    fun containsCharOffset(offset: Int): Boolean {
        return if (isLastPageOfChapter) {
            offset in startCharOffset..endCharOffset
        } else {
            offset in startCharOffset until endCharOffset
        }
    }
}
