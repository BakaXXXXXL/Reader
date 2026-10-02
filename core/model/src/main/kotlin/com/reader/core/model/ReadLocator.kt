package com.reader.core.model

/**
 * 统一定位器 (Unified Locator)。
 * 借鉴 W3C / Readium 规范设计，基于 [charOffset] 与 [progression] 确保无论字体、字号、
 * 行距如何动态调整或横竖屏重排，均能无缝精确恢复阅读位置。
 *
 * @property bookId 所属书籍 ID
 * @property chapterIndex 当前章节索引 (从 0 开始)
 * @property chapterTitle 当前章节标题
 * @property charOffset 当前章节内的字符绝对偏移量 (0 .. chapterTextLength)
 * @property progression 全书或当前章节进度比例 (0.0f .. 1.0f)
 * @property pageIndexInChapter 当前排版配置下的页码快照 (0-indexed，用于即时恢复)
 * @property totalPagesInChapter 当前排版配置下该章节总页数
 * @property updateTime 定位更新时间戳 (毫秒)
 */
data class ReadLocator(
    val bookId: Long,
    val chapterIndex: Int = 0,
    val chapterTitle: String = "",
    val charOffset: Int = 0,
    val progression: Float = 0.0f,
    val pageIndexInChapter: Int = 0,
    val totalPagesInChapter: Int = 1,
    val updateTime: Long = System.currentTimeMillis()
) {
    init {
        require(chapterIndex >= 0) { "chapterIndex must be non-negative: $chapterIndex" }
        require(charOffset >= 0) { "charOffset must be non-negative: $charOffset" }
        require(pageIndexInChapter >= 0) { "pageIndexInChapter must be non-negative: $pageIndexInChapter" }
        require(totalPagesInChapter >= 1) { "totalPagesInChapter must be at least 1: $totalPagesInChapter" }
    }

    /** 规范化进度百分比 (0.0% ~ 100.0%) */
    val progressionPercentage: Float
        get() = (progression.coerceIn(0.0f, 1.0f) * 100.0f)

    companion object {
        fun initial(bookId: Long, title: String = ""): ReadLocator {
            return ReadLocator(
                bookId = bookId,
                chapterIndex = 0,
                chapterTitle = title,
                charOffset = 0,
                progression = 0.0f,
                pageIndexInChapter = 0,
                totalPagesInChapter = 1,
                updateTime = System.currentTimeMillis()
            )
        }
    }
}
