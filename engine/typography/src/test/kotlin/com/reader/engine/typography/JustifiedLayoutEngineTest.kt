package com.reader.engine.typography

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.ReaderConfig
import com.reader.engine.typography.layout.JustifiedLayoutEngine
import com.reader.engine.typography.layout.TextMeasureEngine
import org.junit.Test

/**
 * 两端网格对齐引擎 (JustifiedLayoutEngine) 单元测试。
 */
class JustifiedLayoutEngineTest {

    @Test
    fun `test justified layout evenly distributes remaining width across character gaps`() {
        val fontSize = 20f
        val config = ReaderConfig(fontSizeSp = fontSize, letterSpacingEm = 0f)
        val engine = TextMeasureEngine.createStandard(fontSizePx = fontSize, config = config)

        val text = "一二三四五" // 5 个汉字，每字 20px，自然宽 100px
        val availableWidth = 140f // 剩余空白 40px，分配到 4 个间隙，每个间隙 10px
        val startX = 10f

        val line = JustifiedLayoutEngine.layoutLine(
            text = text,
            start = 0,
            end = 5,
            lineIndex = 0,
            isFirstLineOfParagraph = false,
            isLastLineOfParagraph = false, // 非段落尾行，必须分散两端对齐
            availableWidth = availableWidth,
            startX = startX,
            topY = 0f,
            measureEngine = engine
        )

        // 字符数必须完整
        assertThat(line.charPositions).hasSize(5)

        // 字符坐标严格递增且间距均匀为 20 + 10 = 30px
        assertThat(line.charPositions[0].x).isEqualTo(10f)
        assertThat(line.charPositions[1].x).isEqualTo(40f)
        assertThat(line.charPositions[2].x).isEqualTo(70f)
        assertThat(line.charPositions[3].x).isEqualTo(100f)
        assertThat(line.charPositions[4].x).isEqualTo(130f)

        // 最后一个字符右边缘 (130 + 20 = 150) 与起始 X (10) 之差必须精准等于 availableWidth (140)
        val lineRightEdge = line.charPositions[4].x + line.charPositions[4].width
        assertThat(lineRightEdge - startX).isEqualTo(availableWidth)
        assertThat(line.width).isEqualTo(availableWidth)
    }

    @Test
    fun `test last line of paragraph is left-aligned without artificial stretch`() {
        val fontSize = 20f
        val config = ReaderConfig(fontSizeSp = fontSize, letterSpacingEm = 0f)
        val engine = TextMeasureEngine.createStandard(fontSizePx = fontSize, config = config)

        val text = "这是尾行" // 4 个汉字，总宽 80px
        val availableWidth = 200f // 宽版心，剩余 120px

        val line = JustifiedLayoutEngine.layoutLine(
            text = text,
            start = 0,
            end = 4,
            lineIndex = 1,
            isFirstLineOfParagraph = false,
            isLastLineOfParagraph = true, // 段落末行，禁止拉伸！
            availableWidth = availableWidth,
            startX = 0f,
            topY = 0f,
            measureEngine = engine
        )

        // 宽度应保持自然宽度 80px，不强行填满 200px
        assertThat(line.width).isEqualTo(80f)
        assertThat(line.charPositions[0].x).isEqualTo(0f)
        assertThat(line.charPositions[1].x).isEqualTo(20f)
        assertThat(line.charPositions[2].x).isEqualTo(40f)
        assertThat(line.charPositions[3].x).isEqualTo(60f)
    }

    @Test
    fun `test single character line handles without division by zero`() {
        val fontSize = 20f
        val engine = TextMeasureEngine.createStandard(fontSizePx = fontSize)
        val text = "字"

        val line = JustifiedLayoutEngine.layoutLine(
            text = text,
            start = 0,
            end = 1,
            lineIndex = 0,
            isFirstLineOfParagraph = false,
            isLastLineOfParagraph = false,
            availableWidth = 200f,
            startX = 0f,
            topY = 0f,
            measureEngine = engine
        )

        assertThat(line.charPositions).hasSize(1)
        assertThat(line.width).isEqualTo(20f)
    }
}
