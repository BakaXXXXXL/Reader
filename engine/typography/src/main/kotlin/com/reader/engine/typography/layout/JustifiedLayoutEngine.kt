package com.reader.engine.typography.layout

import com.reader.engine.typography.model.CharPosition
import com.reader.engine.typography.model.TextLine

/**
 * 两端网格对齐 (Justified Alignment) 微调排版引擎。
 *
 * 算法原理：
 * 1. 当一行文本排版完成后，若该行不是段落最后一行且字符数 > 1，计算当前行剩余空白宽度 (remainingWidth)；
 * 2. 将剩余空白宽度均匀微调分配到行内各个字符的物理间距中 (extraGap = remainingWidth / (charCount - 1))；
 * 3. 使整行文字左右两端与版心边框完全平齐贴合，呈现整齐优雅的中文印刷网格质感；
 * 4. 段落最后一行保持自然左对齐排列，严禁强行拉伸分散对齐。
 */
object JustifiedLayoutEngine {

    /**
     * 将指定文本范围排版并计算精确字符坐标与两端对齐。
     *
     * @param text 源段落或文本序列
     * @param start 该行在 [text] 中的起始字符偏移 (包含)
     * @param end 该行在 [text] 中的结束字符偏移 (不包含)
     * @param lineIndex 当前页面内的行序号 (0-based)
     * @param isFirstLineOfParagraph 是否为段落首行
     * @param isLastLineOfParagraph 是否为段落尾行
     * @param availableWidth 该行正文允许的最大排版宽度 (px)
     * @param startX 该行左边缘在页面中的绝对起点 X 坐标 (px)
     * @param topY 该行顶部在页面中的绝对起点 Y 坐标 (px)
     * @param measureEngine 测量引擎
     * @return 包含精确定位各字符坐标的 [TextLine]
     */
    fun layoutLine(
        text: CharSequence,
        start: Int,
        end: Int,
        lineIndex: Int,
        isFirstLineOfParagraph: Boolean,
        isLastLineOfParagraph: Boolean,
        availableWidth: Float,
        startX: Float,
        topY: Float,
        measureEngine: TextMeasureEngine
    ): TextLine {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        val charCount = safeEnd - safeStart

        val lineText = if (charCount > 0) text.subSequence(safeStart, safeEnd).toString() else ""
        val baselineY = topY + measureEngine.baselineOffsetFromTop
        val lineHeight = measureEngine.lineHeightPx

        if (charCount == 0) {
            return TextLine(
                lineIndex = lineIndex,
                text = "",
                startCharOffset = safeStart,
                endCharOffset = safeEnd,
                charPositions = emptyList(),
                isFirstLineOfParagraph = isFirstLineOfParagraph,
                isLastLineOfParagraph = isLastLineOfParagraph,
                x = startX,
                y = topY,
                baselineY = baselineY,
                width = 0f,
                height = lineHeight
            )
        }

        // 1. 测算每个字符的基础净字宽
        val charWidths = FloatArray(charCount)
        var totalCharsWidth = 0f
        for (i in 0 until charCount) {
            val char = text[safeStart + i]
            val w = measureEngine.measurer.measureChar(char)
            charWidths[i] = w
            totalCharsWidth += w
        }

        // 2. 计算自然排版下的总宽度 (字宽总和 + 默认字间距)
        val defaultLetterSpacing = measureEngine.letterSpacingPx
        val totalSpacingWidth = if (charCount > 1) (charCount - 1) * defaultLetterSpacing else 0f
        val naturalWidth = totalCharsWidth + totalSpacingWidth

        // 3. 计算两端对齐增量间隙
        // 规则：段落最后一行或仅有单字符的行不执行两端分散对齐，左对齐自然排列
        val remainingWidth = availableWidth - naturalWidth
        val shouldJustify = !isLastLineOfParagraph && charCount > 1 && remainingWidth > 0f

        val extraGap = if (shouldJustify) {
            remainingWidth / (charCount - 1)
        } else {
            0f
        }

        val actualSpacing = defaultLetterSpacing + extraGap

        // 4. 遍历生成每个字符的绝对绘制坐标
        val charPositions = ArrayList<CharPosition>(charCount)
        var currentX = startX

        for (i in 0 until charCount) {
            val char = text[safeStart + i]
            val w = charWidths[i]
            val isLastChar = (i == charCount - 1)
            val spacingToNext = if (isLastChar) 0f else actualSpacing

            charPositions.add(
                CharPosition(
                    char = char,
                    charOffset = safeStart + i,
                    x = currentX,
                    y = baselineY,
                    width = w,
                    spacingToNext = spacingToNext
                )
            )

            currentX += (w + spacingToNext)
        }

        val lineWidth = currentX - startX

        return TextLine(
            lineIndex = lineIndex,
            text = lineText,
            startCharOffset = safeStart,
            endCharOffset = safeEnd,
            charPositions = charPositions,
            isFirstLineOfParagraph = isFirstLineOfParagraph,
            isLastLineOfParagraph = isLastLineOfParagraph,
            x = startX,
            y = topY,
            baselineY = baselineY,
            width = lineWidth,
            height = lineHeight
        )
    }
}
