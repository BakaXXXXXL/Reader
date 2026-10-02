package com.reader.engine.typography

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.reader.core.model.ReaderConfig
import com.reader.engine.typography.kinsoku.KinsokuRule
import com.reader.engine.typography.layout.TextMeasureEngine
import com.reader.engine.typography.model.PageDimensions
import com.reader.engine.typography.splitter.PageSplitter
import org.junit.Test

/**
 * 章节分页切割器 (PageSplitter) 单元测试。
 * 重点覆盖：
 * 1. 空文本与极小视口边界；
 * 2. 连续页面字符绝对偏移连续性断言；
 * 3. 避头尾标点禁则在整章切页中的无缝集成 (绝无非法行首或行尾标点)；
 * 4. 多段落、换行符 (\n, \r\n) 与首行缩进；
 * 5. 长文本多页跨页切割。
 */
class PageSplitterTest {

    private val fontSize = 20f
    private val config = ReaderConfig(
        fontSizeSp = fontSize,
        lineHeightMultiplier = 1.5f,
        paragraphSpacingDp = 10f,
        letterSpacingEm = 0f,
        firstLineIndentSpaces = 2
    )
    private val measureEngine = TextMeasureEngine.createStandard(fontSizePx = fontSize, config = config)
    private val splitter = PageSplitter(measureEngine)

    @Test
    fun `test split empty chapter returns single valid empty page`() {
        val dimensions = PageDimensions(viewWidth = 400f, viewHeight = 800f)
        val pages = splitter.splitChapter("", dimensions, chapterIndex = 0)

        assertThat(pages).hasSize(1)
        val page = pages.first()
        assertThat(page.pageIndex).isEqualTo(0)
        assertThat(page.startCharOffset).isEqualTo(0)
        assertThat(page.endCharOffset).isEqualTo(0)
        assertThat(page.isFirstPageOfChapter).isTrue()
        assertThat(page.isLastPageOfChapter).isTrue()
        assertThat(page.isEmpty).isTrue()
    }

    @Test
    fun `test character offset continuity across multiple pages`() {
        // 构建一段足够跨越 3~4 页的长文本
        val paragraph = "这是一段用于测试长文本分页的经典中文段落。通过不断重复该段落，我们将验证页面之间字符偏移量的严密连续性。"
        val chapterText = (1..20).joinToString("\n") { "$paragraph (第 $it 段)" }

        // 视口尺寸设定为较小高度，迫使其分为多页
        val dimensions = PageDimensions(
            viewWidth = 240f, // 净宽约容纳 10-12 字
            viewHeight = 300f, // 净高约容纳 6-8 行
            paddingLeft = 10f,
            paddingTop = 10f,
            paddingRight = 10f,
            paddingBottom = 10f
        )

        val pages = splitter.splitChapter(chapterText, dimensions, chapterIndex = 1)

        assertThat(pages.size).isGreaterThan(1)

        // 首页与尾页标识断言
        assertThat(pages.first().isFirstPageOfChapter).isTrue()
        assertThat(pages.first().isLastPageOfChapter).isFalse()
        assertThat(pages.last().isFirstPageOfChapter).isFalse()
        assertThat(pages.last().isLastPageOfChapter).isTrue()

        // 首尾字符偏移绝对覆盖全文
        assertThat(pages.first().startCharOffset).isEqualTo(0)
        assertThat(pages.last().endCharOffset).isEqualTo(chapterText.length)

        // 核心铁律：所有相邻页面之间必须无空隙、无重叠，严格无缝相接
        for (i in 0 until pages.lastIndex) {
            val currentPage = pages[i]
            val nextPage = pages[i + 1]
            assertWithMessage("Page $i endOffset must equal Page ${i + 1} startOffset")
                .that(currentPage.endCharOffset)
                .isEqualTo(nextPage.startCharOffset)
        }
    }

    @Test
    fun `test kinsoku rules strictly obeyed across all generated lines`() {
        // 构建包含各种标点禁则的复杂中文文本
        val textWithPunctuation = """
            赵客缦胡缨，吴钩霜雪明。银鞍照白马，飒沓如彗星。
            “十步杀一人，千里不留行。”事了拂衣去，深藏身与名！
            闲过信陵饮，脱剑膝前横。将炙啖朱亥，持觞劝侯嬴。
            三杯吐然诺，五岳倒为轻……眼花耳热后，意气素霓生。
            救赵挥金槌，邯郸先震惊（《史记·魏公子列传》）。
        """.trimIndent()

        // 设定临界行宽 (例如 8 个字宽)
        val dimensions = PageDimensions(
            viewWidth = 180f,
            viewHeight = 800f,
            paddingLeft = 10f,
            paddingTop = 10f,
            paddingRight = 10f,
            paddingBottom = 10f
        )

        val pages = splitter.splitChapter(textWithPunctuation, dimensions)

        assertThat(pages).isNotEmpty()

        for (page in pages) {
            for (line in page.lines) {
                if (line.text.isNotEmpty()) {
                    val firstChar = line.text.first()
                    val lastChar = line.text.last()

                    // 断言行首绝不出现行首禁则符
                    assertWithMessage("Line '${line.text}' starts with forbidden char '$firstChar'")
                        .that(KinsokuRule.isForbiddenLineStart(firstChar))
                        .isFalse()

                    // 若该行不是段落最后一行（后面还有文字被断到下一行），断言行末绝不出现行尾禁则符
                    if (!line.isLastLineOfParagraph) {
                        assertWithMessage("Line '${line.text}' ends with forbidden char '$lastChar'")
                            .that(KinsokuRule.isForbiddenLineEnd(lastChar))
                            .isFalse()
                    }
                }
            }
        }
    }

    @Test
    fun `test multi paragraph with windows CRLF preserved accurately`() {
        val text = "第一段文字。\r\n第二段文字。\r\n第三段文字。"
        val dimensions = PageDimensions(viewWidth = 400f, viewHeight = 800f)

        val pages = splitter.splitChapter(text, dimensions)
        assertThat(pages).hasSize(1)

        val page = pages.first()
        assertThat(page.lines).hasSize(3)

        // 校验每一段首行都正确标记 isFirstLineOfParagraph
        for (line in page.lines) {
            assertThat(line.isFirstLineOfParagraph).isTrue()
            assertThat(line.isLastLineOfParagraph).isTrue()
        }

        // 最后一页 endCharOffset 严格等于原始字符串长度
        assertThat(page.endCharOffset).isEqualTo(text.length)
    }
}
