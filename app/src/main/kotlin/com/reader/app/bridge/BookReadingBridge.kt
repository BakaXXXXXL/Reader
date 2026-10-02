package com.reader.app.bridge

import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.core.model.Chapter
import com.reader.core.model.ReaderConfig
import com.reader.engine.parser.epub.EpubBookParser
import com.reader.engine.parser.txt.TxtBookParser
import com.reader.engine.typography.layout.TextMeasureEngine
import com.reader.engine.typography.measurer.StandardCharMeasurer
import com.reader.engine.typography.model.PageDimensions
import com.reader.engine.typography.model.ReaderPage
import com.reader.engine.typography.splitter.PageSplitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 书籍解析、排版分页与阅读器数据装配桥接服务。
 * 纯离线安全驱动，连接本地解析引擎、避头尾排版算法与阅读状态机。
 */
class BookReadingBridge(
    private val txtParser: TxtBookParser = TxtBookParser(),
    private val epubParser: EpubBookParser = EpubBookParser()
) {

    /**
     * 打开并初始化一本书：解析目录树，若为首次阅读则定位至第 0 章
     */
    suspend fun openBook(
        file: File,
        format: BookFormat
    ): BookParseResult = withContext(Dispatchers.IO) {
        when (format) {
            BookFormat.TXT -> {
                val detection = txtParser.detectCharset(file)
                val chapters = txtParser.parseChapters(file, charset = detection.charset)
                BookParseResult(
                    title = file.nameWithoutExtension,
                    author = "本地文本",
                    format = format,
                    charset = detection.charset.name(),
                    chapters = chapters
                )
            }
            BookFormat.EPUB -> {
                val epubBook = epubParser.parse(file)
                val book = epubParser.toBook(epubBook, file)
                val chapters = epubParser.toChapters(epubBook, archive = null, bookId = book.id)
                val coverBytes = epubParser.extractCover(file)
                BookParseResult(
                    title = book.title,
                    author = book.author,
                    format = format,
                    charset = "UTF-8",
                    chapters = chapters,
                    coverData = coverBytes
                )
            }
            else -> {
                BookParseResult(
                    title = file.nameWithoutExtension,
                    author = "未知作者",
                    format = format,
                    charset = "UTF-8",
                    chapters = listOf(
                        Chapter(
                            id = 1L,
                            bookId = 1L,
                            index = 0,
                            title = "正文",
                            startOffset = 0L,
                            endOffset = file.length()
                        )
                    )
                )
            }
        }
    }

    /**
     * 加载指定章节正文并执行中文避头尾法则分页计算
     */
    suspend fun loadAndPaginateChapter(
        file: File,
        format: BookFormat,
        chapter: Chapter,
        charset: String,
        dimensions: PageDimensions,
        config: ReaderConfig
    ): ChapterPaginationResult = withContext(Dispatchers.Default) {
        val chapterText = when (format) {
            BookFormat.TXT -> {
                val cs = java.nio.charset.Charset.forName(charset)
                val content = txtParser.loadChapterContent(file, chapter, cs)
                content.paragraphs.joinToString("\n\n")
            }
            BookFormat.EPUB -> {
                val contentPath = chapter.contentPath ?: "content.xhtml"
                val chapterContent = epubParser.parseChapter(file, contentPath, chapter.title)
                chapterContent.plainText
            }
            else -> "格式暂不支持排版"
        }

        // 使用自研中文避头尾法则与网格对齐引擎进行精确物理分页
        val measurer = StandardCharMeasurer(fontSize = config.fontSizeSp)
        val measureEngine = TextMeasureEngine(measurer = measurer, config = config)
        val splitter = PageSplitter(measureEngine)
        val pages = splitter.splitChapter(
            text = chapterText,
            dimensions = dimensions,
            chapterIndex = chapter.index
        )

        ChapterPaginationResult(
            chapterIndex = chapter.index,
            chapterTitle = chapter.title,
            fullText = chapterText,
            pages = pages
        )
    }
}

/**
 * 书籍打开结果
 */
data class BookParseResult(
    val title: String,
    val author: String,
    val format: BookFormat,
    val charset: String,
    val chapters: List<Chapter>,
    val coverData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BookParseResult
        return title == other.title && author == other.author &&
                format == other.format && chapters == other.chapters
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + chapters.hashCode()
        return result
    }
}

/**
 * 章节排版与物理分页计算结果
 */
data class ChapterPaginationResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val fullText: String,
    val pages: List<ReaderPage>
)
