package com.reader.engine.typography.layout

import com.reader.core.model.ReaderConfig
import com.reader.engine.typography.measurer.CharMeasurer
import com.reader.engine.typography.measurer.StandardCharMeasurer

/**
 * 文本排版测量引擎。
 * 负责字号转换、字符字宽精确测量、字间距 (letterSpacing)、行高倍数 (lineHeightMultiplier)、
 * 中文首行空两格 (firstLineIndent) 以及段落间距计算。
 *
 * @property measurer 底层字符测量接口
 * @property config 阅读排版参数配置
 * @property density 屏幕逻辑像素密度比 (dp/sp 转换为 px，默认为 1.0f)
 */
class TextMeasureEngine(
    val measurer: CharMeasurer,
    val config: ReaderConfig = ReaderConfig.DEFAULT,
    val density: Float = 1.0f
) {
    /** 基础字号 (px) */
    val fontSizePx: Float = measurer.fontSize

    /** 字符间距 (px) = config.letterSpacingEm * fontSizePx */
    val letterSpacingPx: Float = (config.letterSpacingEm * fontSizePx).coerceAtLeast(0f)

    /** 单个标准全角中文字符宽度 (px)，以 '中' 字为标准基准 */
    val cjkCharWidth: Float = measurer.measureChar('中')

    /**
     * 中文段落首行缩进宽度 (px)。
     * 严格按标准中文字宽测量倍数计算，杜绝使用不可靠的半角空格。
     */
    val firstLineIndentPx: Float = (config.firstLineIndentSpaces * cjkCharWidth).coerceAtLeast(0f)

    /**
     * 单行计算物理行高 (px)。
     * 基于字体实际文字高度与行高倍数综合算得。
     */
    val lineHeightPx: Float = run {
        val baseHeight = measurer.fontMetrics.textHeight
        (baseHeight * config.lineHeightMultiplier).coerceAtLeast(fontSizePx)
    }

    /** 段落末尾额外附加的段间距 (px) */
    val paragraphSpacingPx: Float = (config.paragraphSpacingDp * density).coerceAtLeast(0f)

    /**
     * 文字基线 (Baseline) 相对单行顶部 Y 轴的向下偏移量 (px)。
     * 采用专业排版半行距 (half-leading) 算法使文字在单行高度矩形内垂直居中。
     */
    val baselineOffsetFromTop: Float = run {
        val textHeight = measurer.fontMetrics.textHeight
        val halfLeading = ((lineHeightPx - textHeight) / 2f).coerceAtLeast(0f)
        halfLeading + (-measurer.fontMetrics.ascent)
    }

    /**
     * 测量字符本身宽度加上当前配置的字间距。
     */
    fun measureCharWithSpacing(char: Char): Float {
        return measurer.measureChar(char) + letterSpacingPx
    }

    /**
     * 精确测量指定文本片段在未对齐状态下的自然排版宽度。
     *
     * @param text 文本序列
     * @param start 起始字符索引
     * @param end 结束字符索引 (不包含)
     * @param isFirstLine 是否为首行 (若为首行则计入首行缩进)
     */
    fun measureLineWidth(
        text: CharSequence,
        start: Int,
        end: Int,
        isFirstLine: Boolean = false
    ): Float {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        if (safeStart >= safeEnd) {
            return if (isFirstLine) firstLineIndentPx else 0f
        }

        var width = if (isFirstLine) firstLineIndentPx else 0f
        val charCount = safeEnd - safeStart
        for (i in safeStart until safeEnd) {
            width += measurer.measureChar(text[i])
        }
        if (charCount > 1) {
            width += (charCount - 1) * letterSpacingPx
        }
        return width
    }

    companion object {
        /**
         * 基于指定字号创建脱机标准测量引擎 (纯 JVM / 单测常用)。
         */
        fun createStandard(
            fontSizePx: Float = 18f,
            config: ReaderConfig = ReaderConfig.DEFAULT,
            density: Float = 1.0f
        ): TextMeasureEngine {
            val measurer = StandardCharMeasurer(fontSize = fontSizePx)
            return TextMeasureEngine(measurer, config, density)
        }
    }
}
