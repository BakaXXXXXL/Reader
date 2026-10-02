package com.reader.engine.typography.measurer

import com.reader.engine.typography.model.FontMetricsInfo

/**
 * 字符与文本宽度精确测量抽象接口。
 * 解耦排版算法与底层绘制引擎 (Paint / TextPaint / 纯数学度量)，
 * 保证算法在纯 JVM 单测与 Android 真实渲染之间的一致性。
 */
interface CharMeasurer {

    /**
     * 当前配置的基础字号 (px)。
     */
    val fontSize: Float

    /**
     * 当前字体的度量指标 (ascent, descent, leading, height 等)。
     */
    val fontMetrics: FontMetricsInfo

    /**
     * 测量单个字符的净渲染宽度 (px)。
     */
    fun measureChar(char: Char): Float

    /**
     * 测量指定字符序列片段的连续渲染宽度 (px)。
     *
     * @param text 待测量字符序列
     * @param start 起始字符索引 (包含)
     * @param end 结束字符索引 (不包含)
     */
    fun measureText(text: CharSequence, start: Int = 0, end: Int = text.length): Float
}
