package com.reader.engine.parser.txt

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class TxtCharsetDetectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun detect_utf8WithBom_returnsUtf8AndBomLength3() {
        val file = tempFolder.newFile("utf8_bom.txt")
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val content = "第一章 初始\n这是正文内容。".toByteArray(StandardCharsets.UTF_8)
        file.writeBytes(bom + content)

        val result = TxtCharsetDetector.detect(file)

        assertThat(result.charset).isEqualTo(StandardCharsets.UTF_8)
        assertThat(result.hasBom).isTrue()
        assertThat(result.bomLength).isEqualTo(3)
        assertThat(result.confidence).isEqualTo(1.0f)
    }

    @Test
    fun detect_utf8WithoutBom_returnsUtf8() {
        val file = tempFolder.newFile("utf8_nobom.txt")
        file.writeText("第一章 少年游\n江南春风拂面来，桃花潭水深千尺。", StandardCharsets.UTF_8)

        val result = TxtCharsetDetector.detect(file)

        assertThat(result.charset).isEqualTo(StandardCharsets.UTF_8)
        assertThat(result.hasBom).isFalse()
        assertThat(result.bomLength).isEqualTo(0)
    }

    @Test
    fun detect_gbkFile_returnsGbkOrGb18030() {
        val gbkCharset = try {
            Charset.forName("GBK")
        } catch (_: Exception) {
            Charset.forName("GB18030")
        }

        val file = tempFolder.newFile("gbk_sample.txt")
        val novelText = buildString {
            appendLine("第一章 宗门大比")
            appendLine("青云门广场之上，万千弟子齐聚，声势浩大，剑气纵横。")
            appendLine("第二章 绝世天骄")
            appendLine("林尘一剑破空，惊艳全场所有人，掌门拂须而笑。")
        }
        file.writeBytes(novelText.toByteArray(gbkCharset))

        val result = TxtCharsetDetector.detect(file)

        assertThat(result.charset.name()).isAnyOf("GB18030", "GBK", "GB2312")
        assertThat(result.hasBom).isFalse()
    }

    @Test
    fun detect_utf16LeWithBom_returnsUtf16Le() {
        val file = tempFolder.newFile("utf16le_sample.txt")
        val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val content = "第一章 乾坤一剑\n这是正文内容。".toByteArray(StandardCharsets.UTF_16LE)
        file.writeBytes(bom + content)

        val result = TxtCharsetDetector.detect(file)

        assertThat(result.charset).isEqualTo(StandardCharsets.UTF_16LE)
        assertThat(result.hasBom).isTrue()
        assertThat(result.bomLength).isEqualTo(2)
    }

    @Test
    fun detect_utf16BeWithBom_returnsUtf16Be() {
        val file = tempFolder.newFile("utf16be_sample.txt")
        val bom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val content = "第一章 乾坤一剑\n这是正文内容。".toByteArray(StandardCharsets.UTF_16BE)
        file.writeBytes(bom + content)

        val result = TxtCharsetDetector.detect(file)

        assertThat(result.charset).isEqualTo(StandardCharsets.UTF_16BE)
        assertThat(result.hasBom).isTrue()
        assertThat(result.bomLength).isEqualTo(2)
    }

    @Test
    fun detect_asciiText_returnsUtf8Compatible() {
        val file = tempFolder.newFile("ascii.txt")
        file.writeText("Chapter 1: The Adventure Begins\nIt was a dark and stormy night.", StandardCharsets.US_ASCII)

        val result = TxtCharsetDetector.detect(file)

        assertThat(result.charset).isAnyOf(StandardCharsets.UTF_8, StandardCharsets.US_ASCII)
    }

    @Test
    fun detect_emptyFile_returnsUtf8Default() {
        val file = tempFolder.newFile("empty.txt")

        val result = TxtCharsetDetector.detect(file)

        assertThat(result.charset).isEqualTo(StandardCharsets.UTF_8)
        assertThat(result.hasBom).isFalse()
    }
}
