package com.reader.engine.parser.txt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TxtChapterRuleMatcherTest {

    private val matcher = TxtChapterRuleMatcher()

    @Test
    fun match_standardChineseChapterNumbers() {
        assertThat(matcher.match("第一章 初始之地")).isEqualTo("第一章 初始之地")
        assertThat(matcher.match("第二回 桃园三结义")).isEqualTo("第二回 桃园三结义")
        assertThat(matcher.match("第123章 巅峰大战")).isEqualTo("第123章 巅峰大战")
        assertThat(matcher.match("第 1 章 启程")).isEqualTo("第 1 章 启程")
        assertThat(matcher.match("第一百零八节 突破境界")).isEqualTo("第一百零八节 突破境界")
        assertThat(matcher.match("第一卷 凡人起步")).isEqualTo("第一卷 凡人起步")
    }

    @Test
    fun match_specialChapters() {
        assertThat(matcher.match("楔子")).isEqualTo("楔子")
        assertThat(matcher.match("序章 天地初开")).isEqualTo("序章 天地初开")
        assertThat(matcher.match("引子")).isEqualTo("引子")
        assertThat(matcher.match("尾声")).isEqualTo("尾声")
        assertThat(matcher.match("番外一 那些少年")).isEqualTo("番外一 那些少年")
        assertThat(matcher.match("大结局")).isEqualTo("大结局")
        assertThat(matcher.match("结语")).isEqualTo("结语")
    }

    @Test
    fun match_numberBullets() {
        assertThat(matcher.match("1. 踏上征途")).isEqualTo("1. 踏上征途")
        assertThat(matcher.match("二、风云突变")).isEqualTo("二、风云突变")
        assertThat(matcher.match("12、重逢")).isEqualTo("12、重逢")
    }

    @Test
    fun match_englishChapters() {
        assertThat(matcher.match("Chapter 1")).isEqualTo("Chapter 1")
        assertThat(matcher.match("CHAPTER 2: The Return")).isEqualTo("CHAPTER 2: The Return")
        assertThat(matcher.match("chapter 10 - Final Stand")).isEqualTo("chapter 10 - Final Stand")
        assertThat(matcher.match("Section 3")).isEqualTo("Section 3")
    }

    @Test
    fun match_sentenceWithEndingPunctuation_returnsNullToPreventFalsePositive() {
        // 普通正文即使开头含有“第一章”，只要带有句号、感叹号或逗号等，都不应作为章节标题
        assertThat(matcher.match("第一章的内容非常引人入胜。")).isNull()
        assertThat(matcher.match("第一回听说这件事，大家都惊呆了！")).isNull()
        assertThat(matcher.match("第10章，我们讲过这个道理。")).isNull()
    }

    @Test
    fun match_tooLongLine_returnsNull() {
        val longLine = "第一章 " + "很长很长".repeat(30)
        assertThat(matcher.match(longLine)).isNull()
    }

    @Test
    fun match_ordinaryText_returnsNull() {
        assertThat(matcher.match("他站起身来，望向窗外朦胧的雨夜。")).isNull()
        assertThat(matcher.match("")).isNull()
        assertThat(matcher.match(" ")).isNull()
    }

    @Test
    fun match_customPattern_worksCorrectly() {
        val customRegex = Regex("""^第\s*[0-9]+\s*关\s+.*$""")
        val customMatcher = TxtChapterRuleMatcher(customPatterns = listOf(customRegex))

        assertThat(customMatcher.match("第 1 关 初试锋芒")).isEqualTo("第 1 关 初试锋芒")
        assertThat(customMatcher.match("第 99 关 最终试炼")).isEqualTo("第 99 关 最终试炼")
    }
}
