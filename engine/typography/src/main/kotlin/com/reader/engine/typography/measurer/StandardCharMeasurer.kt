package com.reader.engine.typography.measurer

import com.reader.engine.typography.model.FontMetricsInfo

/**
 * 跨平台标准字符测量器实现。
 * 遵循中文方块字等宽原则与西方半角字符比例测量，用于纯 JVM 环境、单元测试以及离线脱机排版。
 *
 * - 标准汉字、全角标点、中文全角空格 (\u3000): 宽度固定为 [fontSize] (1.0 em)；
 * - 英文半角字符、半角数字、普通空格 (\u0020): 宽度为 [fontSize] * [halfWidthRatio] (默认 0.5 em)；
 * - 制表符 (\t): 宽度为 [fontSize] * 2.0 em。
 *
 * @property fontSize 基础字号大小 (px)
 * @property halfWidthRatio 半角字符相对全角字宽的比例 (默认 0.5f)
 */
class StandardCharMeasurer(
    override val fontSize: Float = 18f,
    val halfWidthRatio: Float = 0.5f
) : CharMeasurer {

    init {
        require(fontSize > 0f) { "fontSize must be positive: $fontSize" }
        require(halfWidthRatio > 0f) { "halfWidthRatio must be positive: $halfWidthRatio" }
    }

    override val fontMetrics: FontMetricsInfo = FontMetricsInfo(
        ascent = -fontSize * 0.85f,
        descent = fontSize * 0.25f,
        leading = fontSize * 0.05f,
        top = -fontSize * 0.95f,
        bottom = fontSize * 0.35f
    )

    private val fullWidth: Float = fontSize
    private val halfWidth: Float = fontSize * halfWidthRatio

    override fun measureChar(char: Char): Float {
        return when {
            char == '\t' -> fullWidth * 2.0f
            char == '\n' || char == '\r' -> 0f
            char == '\u3000' -> fullWidth // 全角空格
            isFullWidthCharacter(char) -> fullWidth
            else -> halfWidth
        }
    }

    override fun measureText(text: CharSequence, start: Int, end: Int): Float {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        var total = 0f
        for (i in safeStart until safeEnd) {
            total += measureChar(text[i])
        }
        return total
    }

    /**
     * 判断字符是否属于东亚全角字符集 (CJK、全角标点、全角符号)。
     */
    private fun isFullWidthCharacter(c: Char): Boolean {
        val code = c.code
        return when {
            // CJK 统一表意符号 (常用汉字)
            code in 0x4E00..0x9FFF -> true
            // CJK 扩展 A
            code in 0x3400..0x4DBF -> true
            // CJK 兼容表意文字
            code in 0xF900..0xFAFF -> true
            // CJK 符号和标点 (，。、『』等)
            code in 0x3000..0x303F -> true
            // 全角 ASCII 与全角标点 (！＂＃＄％等)
            code in 0xFF01..0xFF60 -> true
            // 全角符号补充
            code in 0xFFE0..0xFFE6 -> true
            // 常见中文双引号、省略号、破折号 (“ ” ‘ ’ … —)
            c == '“' || c == '”' || c == '‘' || c == '’' ||
            c == '…' || c == '—' || c == '～' || c == '·' ||
            c == '‰' || c == '℃' || c == '℉' -> true
            // 日文平假名、片假名
            code in 0x3040..0x309F || code in 0x30A0..0x30FF -> true
            // 默认西文字符为半角
            else -> false
        }
    }
}
