package com.reader.engine.typography

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.ReaderConfig
import com.reader.engine.typography.layout.TextMeasureEngine
import com.reader.engine.typography.measurer.StandardCharMeasurer
import org.junit.Test

/**
 * 文本排版测量引擎 (TextMeasureEngine) 单元测试。
 */
class TextMeasureEngineTest {

    @Test
    fun `test character measurement with CJK and ASCII`() {
        val fontSize = 20f
        val measurer = StandardCharMeasurer(fontSize = fontSize, halfWidthRatio = 0.5f)
        val engine = TextMeasureEngine(measurer)

        // 中文字符与全角标点应为完整 1 em (20px)
        assertThat(measurer.measureChar('中')).isEqualTo(20f)
        assertThat(measurer.measureChar('国')).isEqualTo(20f)
        assertThat(measurer.measureChar('，')).isEqualTo(20f)
        assertThat(measurer.measureChar('。')).isEqualTo(20f)

        // 半角英文字母、数字和普通空格应为 0.5 em (10px)
        assertThat(measurer.measureChar('a')).isEqualTo(10f)
        assertThat(measurer.measureChar('Z')).isEqualTo(10f)
        assertThat(measurer.measureChar('1')).isEqualTo(10f)
        assertThat(measurer.measureChar(' ')).isEqualTo(10f)
    }

    @Test
    fun `test first line indent uses standard CJK width rather than ASCII spaces`() {
        val fontSize = 24f
        val config = ReaderConfig(
            fontSizeSp = fontSize,
            firstLineIndentSpaces = 2
        )
        val engine = TextMeasureEngine.createStandard(fontSizePx = fontSize, config = config)

        // 标准中文首行缩进 2 格应为 2 * 24px = 48px
        assertThat(engine.cjkCharWidth).isEqualTo(24f)
        assertThat(engine.firstLineIndentPx).isEqualTo(48f)

        // 两个半角空格的宽度仅为 2 * 12px = 24px
        val twoAsciiSpacesWidth = engine.measurer.measureText("  ")
        assertThat(twoAsciiSpacesWidth).isEqualTo(24f)

        // 断言首行缩进严格大于且双倍于两个半角空格
        assertThat(engine.firstLineIndentPx).isEqualTo(twoAsciiSpacesWidth * 2f)
    }

    @Test
    fun `test line height multiplier and letter spacing calculation`() {
        val fontSize = 20f
        val config = ReaderConfig(
            fontSizeSp = fontSize,
            lineHeightMultiplier = 1.8f,
            letterSpacingEm = 0.1f
        )
        val engine = TextMeasureEngine.createStandard(fontSizePx = fontSize, config = config)

        // 字间距 = 0.1 * 20px = 2px
        assertThat(engine.letterSpacingPx).isEqualTo(2f)

        // 行高应按 1.8 倍放缩且大于字号
        assertThat(engine.lineHeightPx).isAtLeast(fontSize * 1.8f)
    }

    @Test
    fun `test natural line width measurement with letter spacing`() {
        val fontSize = 20f
        val config = ReaderConfig(
            fontSizeSp = fontSize,
            letterSpacingEm = 0.05f // 1px
        )
        val engine = TextMeasureEngine.createStandard(fontSizePx = fontSize, config = config)

        val text = "中文排版" // 4 个字，每个 20px，3 个字间距，每个 1px -> 总计 80 + 3 = 83px
        val measured = engine.measureLineWidth(text, 0, text.length, isFirstLine = false)
        assertThat(measured).isEqualTo(83f)

        // 若为首行，附加 2 格中文缩进 (40px) -> 123px
        val firstLineMeasured = engine.measureLineWidth(text, 0, text.length, isFirstLine = true)
        assertThat(firstLineMeasured).isEqualTo(123f)
    }
}
