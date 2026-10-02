package com.reader.engine.typography

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.reader.engine.typography.kinsoku.KinsokuRule
import org.junit.Test

/**
 * 中文避头尾法则 (Kinsoku Shori) 核心单元测试。
 * 重点覆盖：
 * 1. 行首禁则符号集完备性校验；
 * 2. 行尾禁则符号集完备性校验；
 * 3. 行首禁则（遇句号、逗号、闭引号等把前一字连同标点推至下一行首）；
 * 4. 连续行首禁则符（如“。”紧跟“”，破折号“——”，叹号“！”）；
 * 5. 行尾禁则（遇开引号、开括号等把符号连同后一字推至下一行首）；
 * 6. 连续行尾禁则符（如“（《”）；
 * 7. 极端单字防死循环边界。
 */
class KinsokuRuleTest {

    @Test
    fun `test forbidden line start characters contains core punctuation`() {
        val requiredStarts = listOf('，', '。', '、', '；', '：', '？', '！', '）', '』', '】', '〉', '》', '”', '’', '…', '—', '％')
        for (char in requiredStarts) {
            assertWithMessage("Character '$char' must be forbidden at line start")
                .that(KinsokuRule.isForbiddenLineStart(char))
                .isTrue()
        }
    }

    @Test
    fun `test forbidden line end characters contains core punctuation`() {
        val requiredEnds = listOf('（', '『', '【', '〈', '《', '“', '‘')
        for (char in requiredEnds) {
            assertWithMessage("Character '$char' must be forbidden at line end")
                .that(KinsokuRule.isForbiddenLineEnd(char))
                .isTrue()
        }
    }

    @Test
    fun `test line break pushes preceding character when next character is forbidden line start`() {
        // 场景：本行排下 "今天天气真好"，下一个字符是 "。"
        // 原 tentativeEnd = 6 ("。" 的索引)
        // 避头尾规则：下一行不能以 "。" 开头，必须把前一字 "好" 连同 "。" 一同推至下一行首
        val text = "今天天气真好。明天继续"
        val lineStart = 0
        val tentativeEnd = 6 // text[6] == '。'
        val paragraphEnd = text.length

        val adjustedEnd = KinsokuRule.adjustLineBreak(
            text = text,
            lineStart = lineStart,
            tentativeEnd = tentativeEnd,
            paragraphEnd = paragraphEnd
        )

        // 调整后截断点应回退到 "好" 之前 (索引 5)
        assertThat(adjustedEnd).isEqualTo(5)
        val currentLine = text.substring(lineStart, adjustedEnd)
        val nextLineStart = text.substring(adjustedEnd, adjustedEnd + 2)
        assertThat(currentLine).isEqualTo("今天天气真")
        assertThat(nextLineStart).isEqualTo("好。")
    }

    @Test
    fun `test line break pushes preceding character with consecutive forbidden line start symbols`() {
        // 场景：本行排到 "太棒了"，接下来是 "！”" (双重禁则符)
        val text = "太棒了！”大家齐声欢呼"
        // 索引: 0:太 1:棒 2:了 3:！ 4:”
        val lineStart = 0
        val tentativeEnd = 4 // tentative 切在 '”' 之前，即 text[0..3] = "太棒了！"，下一个是 '”'
        val paragraphEnd = text.length

        val adjustedEnd = KinsokuRule.adjustLineBreak(
            text = text,
            lineStart = lineStart,
            tentativeEnd = tentativeEnd,
            paragraphEnd = paragraphEnd
        )

        // 必须把 '了'、'！'、'”' 全部推到下一行，当前行只留 "太棒" (索引 2)
        assertThat(adjustedEnd).isEqualTo(2)
        assertThat(text.substring(lineStart, adjustedEnd)).isEqualTo("太棒")
        assertThat(text.substring(adjustedEnd, adjustedEnd + 3)).isEqualTo("了！”")
    }

    @Test
    fun `test line break pushes forbidden line end character to next line`() {
        // 场景：本行排到 "他说：“"，tentative 切在 '“' 之后 (索引 4)
        // '“' 是行尾禁则，严禁出现在行末，必须与后续第一个字一同推至下一行首
        val text = "他说：“你好世界”"
        // 索引: 0:他 1:说 2:： 3:“ 4:你
        val lineStart = 0
        val tentativeEnd = 4 // text[0..3] = "他说：“", text[tentativeEnd-1] == '“'
        val paragraphEnd = text.length

        val adjustedEnd = KinsokuRule.adjustLineBreak(
            text = text,
            lineStart = lineStart,
            tentativeEnd = tentativeEnd,
            paragraphEnd = paragraphEnd
        )

        // 调整后截断点应为 3 (不包含 '“')
        assertThat(adjustedEnd).isEqualTo(3)
        assertThat(text.substring(lineStart, adjustedEnd)).isEqualTo("他说：")
        assertThat(text.substring(adjustedEnd, adjustedEnd + 2)).isEqualTo("“你")
    }

    @Test
    fun `test line break pushes consecutive opening brackets to next line`() {
        // 场景：行尾出现连续左括号 "（《"
        val text = "参考资料（《现代汉语规范》）"
        // 0:参 1:考 2:资 3:料 4:（ 5:《 6:现
        val lineStart = 0
        val tentativeEnd = 6 // text[0..5] = "参考资料（《"
        val paragraphEnd = text.length

        val adjustedEnd = KinsokuRule.adjustLineBreak(
            text = text,
            lineStart = lineStart,
            tentativeEnd = tentativeEnd,
            paragraphEnd = paragraphEnd
        )

        // 两个连续开括号均不能在行末，全部推至下一行
        assertThat(adjustedEnd).isEqualTo(4)
        assertThat(text.substring(lineStart, adjustedEnd)).isEqualTo("参考资料")
        assertThat(text.substring(adjustedEnd, adjustedEnd + 3)).isEqualTo("（《现")
    }

    @Test
    fun `test line break prevents infinite loop on single character line`() {
        // 极端场景：行宽极其狭窄或遇到连续标点，确保 lineStart + 1 绝对兜底
        val text = "……”"
        val lineStart = 0
        val tentativeEnd = 1
        val paragraphEnd = text.length

        val adjustedEnd = KinsokuRule.adjustLineBreak(
            text = text,
            lineStart = lineStart,
            tentativeEnd = tentativeEnd,
            paragraphEnd = paragraphEnd
        )

        // 绝不返回 <= lineStart，杜绝死循环
        assertThat(adjustedEnd).isAtLeast(lineStart + 1)
    }

    @Test
    fun `test english word is not split midway when space is available`() {
        val text = "Hello Android Architecture"
        // 0..4:Hello, 5:' ', 6..12:Android, 13:' '
        val lineStart = 0
        val tentativeEnd = 10 // "Hello And" (腰斩在 And 内部)
        val paragraphEnd = text.length

        val adjustedEnd = KinsokuRule.adjustLineBreak(
            text = text,
            lineStart = lineStart,
            tentativeEnd = tentativeEnd,
            paragraphEnd = paragraphEnd
        )

        // 应回退到单词 "Android" 之前 (索引 6)
        assertThat(adjustedEnd).isEqualTo(6)
        assertThat(text.substring(lineStart, adjustedEnd).trimEnd()).isEqualTo("Hello")
    }
}
