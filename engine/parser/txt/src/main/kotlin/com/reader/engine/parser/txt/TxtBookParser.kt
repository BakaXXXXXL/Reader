package com.reader.engine.parser.txt

import com.reader.core.model.Book
import com.reader.core.model.Chapter
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.nio.charset.Charset

/**
 * TXT 电子书解析引擎核心门面 (Facade)。
 *
 * 聚合了：
 * - [TxtCharsetDetector]：基于 BOM 与 juniversalchardet 的三层多编码自动嗅探器；
 * - [TxtNioReader]：基于 NIO FileChannel / RandomAccessFile 的分块非阻塞轻量读取器；
 * - [TxtChapterSplitter]：中文网络小说异步多规则正则分章与首屏秒开引擎；
 * - [TxtParagraphExtractor]：针对指定章节的高性能段落抽取与文字清洗规整器。
 *
 * 彻底遵循本地离线架构与大文件内存防护原则，杜绝全量加载 OOM。
 */
class TxtBookParser(
    private val nioReader: TxtNioReader = TxtNioReader(),
    private val chapterSplitter: TxtChapterSplitter = TxtChapterSplitter(nioReader),
    private val paragraphExtractor: TxtParagraphExtractor = TxtParagraphExtractor(nioReader)
) {

    /**
     * 自动嗅探目标 TXT 文件的字符编码。
     *
     * @param file 目标文件
     * @return [CharsetDetectionResult] 包含识别出的字符集、置信度以及 BOM 信息
     */
    fun detectCharset(file: File): CharsetDetectionResult {
        return TxtCharsetDetector.detect(file)
    }

    /**
     * 【首屏秒开】：在 20ms 内瞬间提取首屏初始内容和第一章节信息。
     *
     * 供阅读器打开大文件时立即呈现画面，彻底消除转圈等待。
     *
     * @param file 目标书籍文件
     * @param bookId 书籍 ID
     * @param previewBytes 初始预览字节长度 (默认 64KB)
     * @return [TxtFirstScreenResult] 首屏秒开结果
     */
    fun parseFirstScreen(
        file: File,
        bookId: Long = 0L,
        previewBytes: Int = TxtChapterSplitter.FIRST_SCREEN_SAMPLE_BYTES
    ): TxtFirstScreenResult {
        val detection = detectCharset(file)
        return chapterSplitter.extractFirstScreen(
            file = file,
            bookId = bookId,
            charset = detection.charset,
            bomOffset = detection.bomLength
        )
    }

    /**
     * 后台异步流式提取全书章节目录。
     *
     * 在后台协程中流式分块扫描，持续发射新构建的目录供 UI 即时更新。
     *
     * @param file 目标文件
     * @param bookId 所属书籍 ID
     * @param charset 指定字符编码 (若为 null 则自动嗅探)
     * @param customPattern 用户自定义章节正则规则 (可选)
     * @return [Flow] 章节目录流
     */
    fun parseChaptersFlow(
        file: File,
        bookId: Long = 0L,
        charset: Charset? = null,
        customPattern: Regex? = null
    ): Flow<List<Chapter>> {
        val detection = charset?.let {
            CharsetDetectionResult(it, it.name(), 1.0f, false, 0)
        } ?: detectCharset(file)

        val customPatterns = if (customPattern != null) listOf(customPattern) else emptyList()

        return chapterSplitter.splitChaptersFlow(
            file = file,
            bookId = bookId,
            charset = detection.charset,
            bomOffset = detection.bomLength,
            customPatterns = customPatterns
        )
    }

    /**
     * 挂起函数：同步解析完整章节目录列表。
     *
     * @param file 目标文件
     * @param bookId 所属书籍 ID
     * @param charset 指定字符编码 (若为 null 则自动嗅探)
     * @param customPattern 用户自定义章节正则规则 (可选)
     * @return 解析完成的 [Chapter] 列表
     */
    suspend fun parseChapters(
        file: File,
        bookId: Long = 0L,
        charset: Charset? = null,
        customPattern: Regex? = null
    ): List<Chapter> {
        val detection = charset?.let {
            CharsetDetectionResult(it, it.name(), 1.0f, false, 0)
        } ?: detectCharset(file)

        val customPatterns = if (customPattern != null) listOf(customPattern) else emptyList()

        return chapterSplitter.splitChapters(
            file = file,
            bookId = bookId,
            charset = detection.charset,
            bomOffset = detection.bomLength,
            customPatterns = customPatterns
        )
    }

    /**
     * 提取指定章节的正文内容与规整段落列表。
     *
     * 精确通过 [Chapter.startOffset] 与 [Chapter.endOffset] 基于 NIO FileChannel 进行微秒级定位与分块读取，
     * 杜绝加载除目标章节外的任何冗余数据。
     *
     * @param file 目标书籍文件
     * @param chapter 目标章节
     * @param charset 指定字符编码 (若为 null 则自动嗅探)
     * @param trimIndent 是否剥离原始行首空格交由排版引擎统一处理 (默认 true)
     * @return [TxtChapterContent] 章节内容实体
     */
    suspend fun loadChapterContent(
        file: File,
        chapter: Chapter,
        charset: Charset? = null,
        trimIndent: Boolean = true
    ): TxtChapterContent {
        val targetCharset = charset ?: detectCharset(file).charset
        return paragraphExtractor.extractChapterContent(
            file = file,
            chapter = chapter,
            charset = targetCharset,
            trimIndent = trimIndent
        )
    }

    /**
     * 提取指定章节的正文段落流 (响应式 Flow)。
     *
     * @param file 目标书籍文件
     * @param chapter 目标章节
     * @param charset 指定字符编码 (若为 null 则自动嗅探)
     * @return [Flow] 段落字符串流
     */
    fun streamChapterParagraphs(
        file: File,
        chapter: Chapter,
        charset: Charset? = null
    ): Flow<String> {
        val targetCharset = charset ?: detectCharset(file).charset
        return paragraphExtractor.extractParagraphsFlow(
            file = file,
            chapter = chapter,
            charset = targetCharset
        )
    }

    /**
     * 高速读取指定文件任意字节区间的文本片段。
     *
     * 适用于排版引擎按页预览、书签摘要截取等场景。
     *
     * @param file 目标书籍文件
     * @param offset 字节偏移量
     * @param length 读取字节长度
     * @param charset 指定字符编码 (若为 null 则自动嗅探)
     * @return 解码后的字符串
     */
    fun readTextExcerpt(
        file: File,
        offset: Long,
        length: Int,
        charset: Charset? = null
    ): String {
        val targetCharset = charset ?: detectCharset(file).charset
        return nioReader.readString(file, offset, length, targetCharset)
    }
}
