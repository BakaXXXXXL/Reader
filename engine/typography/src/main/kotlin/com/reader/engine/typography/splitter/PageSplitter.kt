package com.reader.engine.typography.splitter

import com.reader.core.model.ReaderConfig
import com.reader.engine.typography.kinsoku.KinsokuRule
import com.reader.engine.typography.layout.JustifiedLayoutEngine
import com.reader.engine.typography.layout.TextMeasureEngine
import com.reader.engine.typography.model.PageDimensions
import com.reader.engine.typography.model.ReaderPage
import com.reader.engine.typography.model.TextLine

/**
 * 核心章节分页切割器 (PageSplitter)。
 *
 * 职责：
 * 1. 接收章节原始文本流与可视矩形区域 (viewWidth, viewHeight, padding)；
 * 2. 结合 [TextMeasureEngine] 字符测宽、[KinsokuRule] 中文避头尾标点禁则与 [JustifiedLayoutEngine] 两端网格微调；
 * 3. 将章节全文精确切割为离散的物理页面列表 `List<ReaderPage>`；
 * 4. 保证各物理页面的字符偏移区间 `[startCharOffset, endCharOffset)` 严格连续覆盖全文，
 *    为阅读器翻页、跳转以及基于 [com.reader.core.model.ReadLocator] 的无缝进度重排定位提供精准基石。
 */
class PageSplitter(
    val measureEngine: TextMeasureEngine
) {

    /**
     * 将指定章节文本流切分为物理页面列表。
     *
     * @param text 章节全部文本字符串
     * @param dimensions 页面与可视区域尺寸 (宽、高、内边距)
     * @param chapterIndex 章节序号 (从 0 开始)
     * @return 精确切割出的物理页面列表
     */
    fun splitChapter(
        text: String,
        dimensions: PageDimensions,
        chapterIndex: Int = 0
    ): List<ReaderPage> {
        val totalLength = text.length

        // 空文本边界处理：返回单张空白页
        if (totalLength == 0) {
            return listOf(
                ReaderPage(
                    pageIndex = 0,
                    chapterIndex = chapterIndex,
                    lines = emptyList(),
                    startCharOffset = 0,
                    endCharOffset = 0,
                    isFirstPageOfChapter = true,
                    isLastPageOfChapter = true
                )
            )
        }

        val contentWidth = dimensions.contentWidth
        val contentHeight = dimensions.contentHeight

        // 视口过小异常防护
        if (contentWidth <= 0f || contentHeight <= 0f) {
            return listOf(
                ReaderPage(
                    pageIndex = 0,
                    chapterIndex = chapterIndex,
                    lines = emptyList(),
                    startCharOffset = 0,
                    endCharOffset = totalLength,
                    isFirstPageOfChapter = true,
                    isLastPageOfChapter = true
                )
            )
        }

        val allLines = ArrayList<TextLine>(256)

        // 1. 扫描文本段落 (以 \r\n, \n, \r 分割)，精确维护原始字符偏移
        var offset = 0
        while (offset < totalLength) {
            var paraEnd = offset
            while (paraEnd < totalLength && text[paraEnd] != '\n' && text[paraEnd] != '\r') {
                paraEnd++
            }

            // 计算跳过换行符后的下一个段落起点及换行符占用的字符数
            val newlineLen = when {
                paraEnd < totalLength && text[paraEnd] == '\r' && paraEnd + 1 < totalLength && text[paraEnd + 1] == '\n' -> 2
                paraEnd < totalLength && (text[paraEnd] == '\n' || text[paraEnd] == '\r') -> 1
                else -> 0
            }
            val nextParaOffset = paraEnd + newlineLen

            // 对当前段落内容进行分行排版
            val paraLines = layoutParagraph(
                text = text,
                paraStart = offset,
                paraEnd = paraEnd,
                newlineLen = newlineLen,
                contentWidth = contentWidth,
                paddingLeft = dimensions.paddingLeft
            )
            allLines.addAll(paraLines)

            offset = nextParaOffset
        }

        // 若文本全是换行符或未产生任何行，兜底单空行
        if (allLines.isEmpty()) {
            return listOf(
                ReaderPage(
                    pageIndex = 0,
                    chapterIndex = chapterIndex,
                    lines = emptyList(),
                    startCharOffset = 0,
                    endCharOffset = totalLength,
                    isFirstPageOfChapter = true,
                    isLastPageOfChapter = true
                )
            )
        }

        // 2. 将所有排版行按照视口净高度 (contentHeight) 切割进物理页面
        return paginateLines(
            allLines = allLines,
            dimensions = dimensions,
            chapterIndex = chapterIndex,
            totalTextLength = totalLength
        )
    }

    /**
     * 将单个段落的文本按行宽切割并对齐。
     */
    private fun layoutParagraph(
        text: String,
        paraStart: Int,
        paraEnd: Int,
        newlineLen: Int,
        contentWidth: Float,
        paddingLeft: Float
    ): List<TextLine> {
        val result = ArrayList<TextLine>()

        // 空段落处理 (空行)
        if (paraStart >= paraEnd) {
            result.add(
                TextLine(
                    lineIndex = 0,
                    text = "",
                    startCharOffset = paraStart,
                    endCharOffset = paraEnd + newlineLen,
                    charPositions = emptyList(),
                    isFirstLineOfParagraph = true,
                    isLastLineOfParagraph = true,
                    x = paddingLeft,
                    y = 0f,
                    baselineY = measureEngine.baselineOffsetFromTop,
                    width = 0f,
                    height = measureEngine.lineHeightPx
                )
            )
            return result
        }

        var cursor = paraStart
        var isFirstLine = true

        while (cursor < paraEnd) {
            val indent = if (isFirstLine) measureEngine.firstLineIndentPx else 0f
            val availableLineWidth = (contentWidth - indent).coerceAtLeast(measureEngine.cjkCharWidth)
            val startX = paddingLeft + indent

            // 测算本行能容纳的最大字符数量
            var tentativeEnd = cursor
            var accumulatedWidth = 0f

            while (tentativeEnd < paraEnd) {
                val char = text[tentativeEnd]
                val charWidth = measureEngine.measurer.measureChar(char)
                val spacing = if (tentativeEnd > cursor) measureEngine.letterSpacingPx else 0f
                val nextWidth = accumulatedWidth + spacing + charWidth

                if (nextWidth > availableLineWidth) {
                    // 若首字本身就超宽，必须至少容纳 1 个字符
                    if (tentativeEnd == cursor) {
                        tentativeEnd++
                    }
                    break
                }

                accumulatedWidth = nextWidth
                tentativeEnd++
            }

            // 应用避头尾法则微调截断点
            val adjustedEnd = KinsokuRule.adjustLineBreak(
                text = text,
                lineStart = cursor,
                tentativeEnd = tentativeEnd,
                paragraphEnd = paraEnd
            )

            val isLastLineOfPara = (adjustedEnd >= paraEnd)

            // 若本行是该段最后一行，其 endCharOffset 包含紧随该段的换行符
            val finalEndOffset = if (isLastLineOfPara) adjustedEnd + newlineLen else adjustedEnd

            val line = JustifiedLayoutEngine.layoutLine(
                text = text,
                start = cursor,
                end = adjustedEnd,
                lineIndex = result.size,
                isFirstLineOfParagraph = isFirstLine,
                isLastLineOfParagraph = isLastLineOfPara,
                availableWidth = availableLineWidth,
                startX = startX,
                topY = 0f, // 临时 Y 坐标，在切页阶段统一更新
                measureEngine = measureEngine
            ).copy(endCharOffset = finalEndOffset)

            result.add(line)

            cursor = adjustedEnd
            isFirstLine = false
        }

        return result
    }

    /**
     * 将排版完成的文本行组装切割为物理页面。
     */
    private fun paginateLines(
        allLines: List<TextLine>,
        dimensions: PageDimensions,
        chapterIndex: Int,
        totalTextLength: Int
    ): List<ReaderPage> {
        val pages = ArrayList<ReaderPage>()
        val currentPageLines = ArrayList<TextLine>()
        val contentHeight = dimensions.contentHeight
        val paddingTop = dimensions.paddingTop

        var currentY = paddingTop

        for (rawLine in allLines) {
            val lineHeight = measureEngine.lineHeightPx
            val lineAdvance = lineHeight + if (rawLine.isLastLineOfParagraph) measureEngine.paragraphSpacingPx else 0f

            // 垂直切页判断：当前行高度超出页面可用净高度且当前页非空
            if (currentY + lineHeight > paddingTop + contentHeight && currentPageLines.isNotEmpty()) {
                val pageIndex = pages.size
                pages.add(
                    ReaderPage(
                        pageIndex = pageIndex,
                        chapterIndex = chapterIndex,
                        lines = ArrayList(currentPageLines),
                        startCharOffset = currentPageLines.first().startCharOffset,
                        endCharOffset = currentPageLines.last().endCharOffset,
                        isFirstPageOfChapter = (pageIndex == 0),
                        isLastPageOfChapter = false
                    )
                )
                currentPageLines.clear()
                currentY = paddingTop
            }

            // 更新本行在当前页面的绝对 Y 坐标与基线 Y
            val adjustedLine = rawLine.copy(
                lineIndex = currentPageLines.size,
                y = currentY,
                baselineY = currentY + measureEngine.baselineOffsetFromTop,
                charPositions = rawLine.charPositions.map { pos ->
                    pos.copy(y = currentY + measureEngine.baselineOffsetFromTop)
                }
            )

            currentPageLines.add(adjustedLine)
            currentY += lineAdvance
        }

        // 添加最后一页
        if (currentPageLines.isNotEmpty()) {
            val pageIndex = pages.size
            pages.add(
                ReaderPage(
                    pageIndex = pageIndex,
                    chapterIndex = chapterIndex,
                    lines = ArrayList(currentPageLines),
                    startCharOffset = currentPageLines.first().startCharOffset,
                    endCharOffset = totalTextLength, // 最后一页严格覆盖到章节总长度
                    isFirstPageOfChapter = (pageIndex == 0),
                    isLastPageOfChapter = true
                )
            )
        }

        if (pages.isNotEmpty()) {
            // 修正第一页与最后一页标记
            return pages.mapIndexed { index, page ->
                page.copy(
                    isFirstPageOfChapter = (index == 0),
                    isLastPageOfChapter = (index == pages.lastIndex)
                )
            }
        }

        return pages
    }
}
