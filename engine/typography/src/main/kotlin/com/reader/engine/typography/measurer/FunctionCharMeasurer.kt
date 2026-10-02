package com.reader.engine.typography.measurer

import com.reader.engine.typography.model.FontMetricsInfo

/**
 * 通用委托字符测量器实现。
 * 支持通过 Android [Paint] / [android.text.TextPaint] 的函数引用直接桥接测量。
 * 内置单字符测宽轻量缓存，避免在长文本切页循环中高频触发 JNI measureText 开销。
 *
 * 在 Android 端的使用方式：
 * ```kotlin
 * val measurer = FunctionCharMeasurer(
 *     fontSize = paint.textSize,
 *     fontMetrics = FontMetricsInfo(
 *         ascent = paint.fontMetrics.ascent,
 *         descent = paint.fontMetrics.descent,
 *         leading = paint.fontMetrics.leading,
 *         top = paint.fontMetrics.top,
 *         bottom = paint.fontMetrics.bottom
 *     ),
 *     charMeasureFunc = { char -> paint.measureText(char.toString()) },
 *     textMeasureFunc = { text, start, end -> paint.measureText(text, start, end) }
 * )
 * ```
 */
class FunctionCharMeasurer(
    override val fontSize: Float,
    override val fontMetrics: FontMetricsInfo,
    private val charMeasureFunc: (Char) -> Float,
    private val textMeasureFunc: ((CharSequence, Int, Int) -> Float)? = null
) : CharMeasurer {

    private val asciiWidthCache = FloatArray(128) { -1f }
    private val cjkSampleCache = HashMap<Char, Float>(512)

    override fun measureChar(char: Char): Float {
        if (char == '\n' || char == '\r') return 0f
        val code = char.code
        if (code in 0..127) {
            val cached = asciiWidthCache[code]
            if (cached >= 0f) return cached
            val measured = charMeasureFunc(char)
            asciiWidthCache[code] = measured
            return measured
        }

        val cached = cjkSampleCache[char]
        if (cached != null) return cached

        val measured = charMeasureFunc(char)
        if (cjkSampleCache.size < 4096) {
            cjkSampleCache[char] = measured
        }
        return measured
    }

    override fun measureText(text: CharSequence, start: Int, end: Int): Float {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        if (safeStart >= safeEnd) return 0f
        if (textMeasureFunc != null) {
            return textMeasureFunc.invoke(text, safeStart, safeEnd)
        }
        var total = 0f
        for (i in safeStart until safeEnd) {
            total += measureChar(text[i])
        }
        return total
    }

    fun invalidateCache() {
        asciiWidthCache.fill(-1f)
        cjkSampleCache.clear()
    }
}
