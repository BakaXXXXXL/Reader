package com.reader.engine.parser.txt

import com.reader.core.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

/**
 * 章节内容提取实体。
 *
 * @property chapterTitle 章节标题
 * @property paragraphs 清洗后的正文段落列表 (不含章节标题行)
 * @property rawText 原始解密/解码后的纯文本正文
 * @property charCount 正文总有效字符数 (不计空白符)
 */
data class TxtChapterContent(
    val chapterTitle: String,
    val paragraphs: List<String>,
    val rawText: String,
    val charCount: Int
)

/**
 * TXT 章节文本段落流式提取器。
 *
 * 特性：
 * 1. 【NIO 按区间精准读取】：仅根据 [Chapter.startOffset] 与 [Chapter.endOffset] 读取目标章节区间，
 *    绝对不会触发全书加载，seek 耗时仅需微秒级；
 * 2. 【智能段落清洗与规整】：
 *    - 剥离首部重复出现的章节标题；
 *    - 滤除连续多余的纯空行；
 *    - 清理段首段尾杂乱 ASCII 空格或制表符，以便排版引擎运用两端网格对齐和标准中文缩进；
 * 3. 【双模提取】：支持高并发响应式 `Flow<String>` 段落流，以及一站式 `TxtChapterContent` 数据结构。
 */
class TxtParagraphExtractor(
    private val nioReader: TxtNioReader = TxtNioReader()
) {

    /**
     * 流式提取指定章节的每一个清洗后的段落。
     *
     * @param file 目标文件
     * @param chapter 章节信息
     * @param charset 编码
     * @param trimIndent 是否清理段首空格 (交由排版引擎标准缩进，默认 true)
     */
    fun extractParagraphsFlow(
        file: File,
        chapter: Chapter,
        charset: Charset,
        trimIndent: Boolean = true
    ): Flow<String> = flow {
        val length = chapter.length
        if (!file.exists() || length <= 0L) return@flow

        var isFirstNonEmptyLine = true

        nioReader.forEachLineSuspend(
            file = file,
            charset = charset,
            startOffset = chapter.startOffset,
            maxOffset = chapter.endOffset
        ) { line ->
            val cleaned = cleanParagraph(line.text, trimIndent)
            if (cleaned.isNotEmpty()) {
                // 如果第一行完全匹配或高度吻合本章节标题，则跳过首行以防正文重复标题
                if (isFirstNonEmptyLine && isTitleLine(cleaned, chapter.title)) {
                    isFirstNonEmptyLine = false
                } else {
                    isFirstNonEmptyLine = false
                    emit(cleaned)
                }
            }
            true
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 挂起读取指定章节的完整段落列表与文本实体。
     */
    suspend fun extractChapterContent(
        file: File,
        chapter: Chapter,
        charset: Charset,
        trimIndent: Boolean = true
    ): TxtChapterContent = withContext(Dispatchers.IO) {
        val length = chapter.length
        if (!file.exists() || length <= 0L) {
            return@withContext TxtChapterContent(
                chapterTitle = chapter.title,
                paragraphs = emptyList(),
                rawText = "",
                charCount = 0
            )
        }

        val paragraphs = mutableListOf<String>()
        var isFirstNonEmptyLine = true
        val rawBuilder = StringBuilder()

        nioReader.forEachLine(
            file = file,
            charset = charset,
            startOffset = chapter.startOffset,
            maxOffset = chapter.endOffset
        ) { line ->
            val lineText = line.text
            rawBuilder.append(lineText).append('\n')

            val cleaned = cleanParagraph(lineText, trimIndent)
            if (cleaned.isNotEmpty()) {
                if (isFirstNonEmptyLine && isTitleLine(cleaned, chapter.title)) {
                    isFirstNonEmptyLine = false
                } else {
                    isFirstNonEmptyLine = false
                    paragraphs.add(cleaned)
                }
            }
            true
        }

        val rawText = rawBuilder.toString()
        val charCount = paragraphs.sumOf { p -> p.count { !it.isWhitespace() } }

        TxtChapterContent(
            chapterTitle = chapter.title,
            paragraphs = paragraphs,
            rawText = rawText,
            charCount = charCount
        )
    }

    /**
     * 清理段落中的异常不可见字符与冗余空格。
     */
    private fun cleanParagraph(raw: String, trimIndent: Boolean): String {
        // 过滤不可见控制符 (保留换行和制表符以外的字符)
        var s = raw.replace("\uFEFF", "") // 去除可能混入的零宽无断空白
            .replace("\u200B", "")
            .replace("\u0000", "")

        return if (trimIndent) {
            // 剥除行首行尾的半角/全角空格与制表符
            s.trim { it.isWhitespace() || it == '　' }
        } else {
            s.trimEnd { it.isWhitespace() || it == '　' }
        }
    }

    /**
     * 校验某行是否为章节标题本身，避免阅读界面标题与正文首行重叠。
     */
    private fun isTitleLine(line: String, chapterTitle: String): Boolean {
        val normLine = line.replace(" ", "").trim()
        val normTitle = chapterTitle.replace(" ", "").trim()
        return normLine == normTitle || normLine.startsWith(normTitle)
    }
}
