package com.reader.engine.parser.txt

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class TxtParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val parser = TxtBookParser()

    @Test
    fun parseUtf8Novel_fullPipeline_succeeds() = runTest {
        val file = tempFolder.newFile("novel_utf8.txt")
        val novelContent = buildString {
            appendLine("第一章 少年游")
            appendLine("　　烟雨蒙蒙，江南古道之上，一位白衣少年仗剑独行。")
            appendLine("　　古道两旁柳色青青，微风拂过，落英缤纷。")
            appendLine("")
            appendLine("第二章 惊鸿照影")
            appendLine("　　客栈之内，人声鼎沸。")
            appendLine("　　少年按剑而立，目光如电，环视四周江湖豪杰。")
            appendLine("")
            appendLine("第三章 剑气凌霄")
            appendLine("　　长剑出鞘，龙吟阵阵。")
            appendLine("　　一剑寒芒先到，随后枪出如龙。")
        }
        file.writeText(novelContent, StandardCharsets.UTF_8)

        // 1. 字符集嗅探
        val charsetResult = parser.detectCharset(file)
        assertThat(charsetResult.charset).isEqualTo(StandardCharsets.UTF_8)

        // 2. 秒开首屏
        val firstScreen = parser.parseFirstScreen(file, bookId = 100L)
        assertThat(firstScreen.charset).isEqualTo(StandardCharsets.UTF_8)
        assertThat(firstScreen.firstChapter.title).isEqualTo("第一章 少年游")
        assertThat(firstScreen.previewText).isNotEmpty()

        // 3. 全本异步分章
        val chapters = parser.parseChapters(file, bookId = 100L)
        assertThat(chapters).hasSize(3)

        assertThat(chapters[0].title).isEqualTo("第一章 少年游")
        assertThat(chapters[0].index).isEqualTo(0)
        assertThat(chapters[0].startOffset).isEqualTo(0L)

        assertThat(chapters[1].title).isEqualTo("第二章 惊鸿照影")
        assertThat(chapters[1].index).isEqualTo(1)
        assertThat(chapters[1].startOffset).isEqualTo(chapters[0].endOffset)

        assertThat(chapters[2].title).isEqualTo("第三章 剑气凌霄")
        assertThat(chapters[2].index).isEqualTo(2)
        assertThat(chapters[2].endOffset).isEqualTo(file.length())

        // 4. 章节正文与段落提取
        val ch2Content = parser.loadChapterContent(file, chapters[1], charset = StandardCharsets.UTF_8)
        assertThat(ch2Content.chapterTitle).isEqualTo("第二章 惊鸿照影")
        assertThat(ch2Content.paragraphs).containsExactly(
            "客栈之内，人声鼎沸。",
            "少年按剑而立，目光如电，环视四周江湖豪杰。"
        ).inOrder()

        // 5. 段落流式提取
        val streamedParagraphs = parser.streamChapterParagraphs(file, chapters[2], StandardCharsets.UTF_8).toList()
        assertThat(streamedParagraphs).containsExactly(
            "长剑出鞘，龙吟阵阵。",
            "一剑寒芒先到，随后枪出如龙。"
        ).inOrder()
    }

    @Test
    fun parseGbkNovel_fullPipeline_noMojibake() = runTest {
        val gbkCharset = try {
            Charset.forName("GBK")
        } catch (_: Exception) {
            Charset.forName("GB18030")
        }

        val file = tempFolder.newFile("novel_gbk.txt")
        val novelContent = buildString {
            appendLine("第一章 苍穹之下")
            appendLine("大荒深处，异兽咆哮，震颤万里山川。")
            appendLine("第二章 破而后立")
            appendLine("叶枫盘膝而坐，体内经脉涅槃重生，气血如真龙升腾！")
        }
        file.writeBytes(novelContent.toByteArray(gbkCharset))

        // 1. 自动嗅探 GBK
        val charsetResult = parser.detectCharset(file)
        assertThat(charsetResult.charset.name()).isAnyOf("GB18030", "GBK", "GB2312")

        // 2. 分章
        val chapters = parser.parseChapters(file, bookId = 200L)
        assertThat(chapters).hasSize(2)
        assertThat(chapters[0].title).isEqualTo("第一章 苍穹之下")
        assertThat(chapters[1].title).isEqualTo("第二章 破而后立")

        // 3. 内容提取且中文无乱码
        val ch1Content = parser.loadChapterContent(file, chapters[0])
        assertThat(ch1Content.paragraphs).containsExactly(
            "大荒深处，异兽咆哮，震颤万里山川。"
        )

        val ch2Content = parser.loadChapterContent(file, chapters[1])
        assertThat(ch2Content.paragraphs).containsExactly(
            "叶枫盘膝而坐，体内经脉涅槃重生，气血如真龙升腾！"
        )
    }

    @Test
    fun parseNovel_withPrologueBeforeChapter1_createsPrologueChapter() = runTest {
        val file = tempFolder.newFile("novel_with_prologue.txt")
        val novelContent = buildString {
            appendLine("作品前言简介：这是一个关于修真与长生的宏伟传说故事。")
            appendLine("天地不仁，以万物为刍狗。")
            appendLine("")
            appendLine("第一章 踏歌行")
            appendLine("少年自东荒而来。")
        }
        file.writeText(novelContent, StandardCharsets.UTF_8)

        val chapters = parser.parseChapters(file, bookId = 300L)

        assertThat(chapters.size).isAtLeast(2)
        assertThat(chapters[0].title).isEqualTo("序章")
        assertThat(chapters[0].index).isEqualTo(0)
        assertThat(chapters[0].startOffset).isEqualTo(0L)

        assertThat(chapters[1].title).isEqualTo("第一章 踏歌行")
        assertThat(chapters[1].index).isEqualTo(1)
    }

    @Test
    fun parseNovel_noChapterPatterns_fallbackAutoSplits() = runTest {
        val file = tempFolder.newFile("plain_article.txt")
        val content = "这是一篇没有标准章节标识的纯文本小说故事。\n故事叙述了古老的故事。\n"
        file.writeText(content, StandardCharsets.UTF_8)

        val chapters = parser.parseChapters(file, bookId = 400L)

        assertThat(chapters).isNotEmpty()
        assertThat(chapters[0].title).isEqualTo("正文")
        assertThat(chapters[0].startOffset).isEqualTo(0L)
        assertThat(chapters[0].endOffset).isEqualTo(file.length())
    }

    @Test
    fun parseChaptersFlow_emitsCorrectly() = runTest {
        val file = tempFolder.newFile("flow_novel.txt")
        val content = buildString {
            appendLine("第一章 启航")
            appendLine("千帆竞发。")
            appendLine("第二章 远方")
            appendLine("海平线在召唤。")
        }
        file.writeText(content, StandardCharsets.UTF_8)

        val emittedList = parser.parseChaptersFlow(file, bookId = 500L).first()
        assertThat(emittedList).hasSize(2)
        assertThat(emittedList[0].title).isEqualTo("第一章 启航")
        assertThat(emittedList[1].title).isEqualTo("第二章 远方")
    }

    @Test
    fun readTextExcerpt_readsExactSnippet() {
        val file = tempFolder.newFile("excerpt.txt")
        file.writeText("0123456789ABCDEF", StandardCharsets.UTF_8)

        val excerpt = parser.readTextExcerpt(file, offset = 10L, length = 4, charset = StandardCharsets.UTF_8)
        assertThat(excerpt).isEqualTo("ABCD")
    }
}
