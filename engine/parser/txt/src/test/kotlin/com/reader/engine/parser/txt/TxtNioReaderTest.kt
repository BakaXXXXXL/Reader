package com.reader.engine.parser.txt

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets

class TxtNioReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun forEachLine_readsAllLinesWithAccurateOffsets() {
        val file = tempFolder.newFile("sample.txt")
        val lines = listOf(
            "第一章 初始",
            "这是正文第一段。",
            "这是正文第二段，内容较长。",
            "第二章 进阶"
        )
        file.writeText(lines.joinToString("\n"), StandardCharsets.UTF_8)

        val reader = TxtNioReader(bufferSize = 32) // 小缓冲区模拟跨块处理
        val readLines = mutableListOf<TxtLine>()

        reader.forEachLine(file, StandardCharsets.UTF_8) { line ->
            readLines.add(line)
            true
        }

        assertThat(readLines).hasSize(4)
        for (i in lines.indices) {
            assertThat(readLines[i].text).isEqualTo(lines[i])
        }

        // 验证偏移量连续性
        assertThat(readLines[0].startByteOffset).isEqualTo(0L)
        assertThat(readLines[1].startByteOffset).isEqualTo(readLines[0].endByteOffset)
        assertThat(readLines[2].startByteOffset).isEqualTo(readLines[1].endByteOffset)
        assertThat(readLines[3].startByteOffset).isEqualTo(readLines[2].endByteOffset)
        assertThat(readLines[3].endByteOffset).isEqualTo(file.length())
    }

    @Test
    fun forEachLine_crlfNewlines_handledCorrectly() {
        val file = tempFolder.newFile("crlf_sample.txt")
        val content = "Line 1\r\nLine 2\r\nLine 3"
        file.writeText(content, StandardCharsets.UTF_8)

        val reader = TxtNioReader(bufferSize = 16)
        val readLines = mutableListOf<String>()

        reader.forEachLine(file, StandardCharsets.UTF_8) { line ->
            readLines.add(line.text)
            true
        }

        assertThat(readLines).containsExactly("Line 1", "Line 2", "Line 3").inOrder()
    }

    @Test
    fun readString_accurateSubstringExtraction() {
        val file = tempFolder.newFile("read_string.txt")
        file.writeText("0123456789ABCDEF", StandardCharsets.UTF_8)

        val reader = TxtNioReader()
        val text = reader.readString(file, startOffset = 5L, length = 5, charset = StandardCharsets.UTF_8)

        assertThat(text).isEqualTo("56789")
    }
}
