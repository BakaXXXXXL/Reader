package com.reader.engine.parser.epub

import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.core.model.Chapter
import com.reader.engine.parser.epub.extractor.ContainerXmlParser
import com.reader.engine.parser.epub.extractor.NavXhtmlParser
import com.reader.engine.parser.epub.extractor.OpfPackageParser
import com.reader.engine.parser.epub.extractor.PathResolver
import com.reader.engine.parser.epub.extractor.TocNcxParser
import com.reader.engine.parser.epub.extractor.XhtmlContentExtractor
import com.reader.engine.parser.epub.model.EpubBook
import com.reader.engine.parser.epub.model.EpubChapterContent
import com.reader.engine.parser.epub.model.EpubTocItem
import com.reader.engine.parser.epub.zip.EpubZipArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * EPUB 2/3 标准流式解包与提取引擎。
 *
 * 核心功能：
 * 1. 基于 ZipFile / ZipInputStream 的高效流式解包；
 * 2. 解析 META-INF/container.xml 动态寻址 content.opf；
 * 3. 解析 content.opf (提取 Metadata 元数据、Manifest 资源清单、Spine 阅读序列)；
 * 4. 协同解析 EPUB 3 nav.xhtml 与 EPUB 2 toc.ncx，构建完整多层级目录树 (具备兜底生成机制)；
 * 5. 智能识别与抽取封面图片 (cover-image) 原始字节；
 * 6. 轻量抽取 XHTML 章节正文，提取层级标题、纯净段落与内嵌图片，彻底过滤脚本与无用标签；
 * 7. 无缝映射为系统核心领域模型 [Book] 与 [Chapter]。
 */
class EpubBookParser {

    /**
     * 同步解析 EPUB 文件。
     */
    fun parseSync(file: File): EpubBook {
        return EpubZipArchive.fromFile(file).use { archive ->
            parseArchive(archive)
        }
    }

    /**
     * 同步流式解析 EPUB 输入流。
     */
    fun parseSync(inputStream: InputStream): EpubBook {
        return EpubZipArchive.fromStream(inputStream).use { archive ->
            parseArchive(archive)
        }
    }

    /**
     * 协程异步解析 EPUB 文件。
     */
    suspend fun parse(file: File): EpubBook = withContext(Dispatchers.IO) {
        parseSync(file)
    }

    /**
     * 协程异步解析 EPUB 输入流。
     */
    suspend fun parse(inputStream: InputStream): EpubBook = withContext(Dispatchers.IO) {
        parseSync(inputStream)
    }

    /**
     * 核心解包流水线：解析 OPF、Manifest、Spine 与 TOC 目录树。
     */
    fun parseArchive(archive: EpubZipArchive): EpubBook {
        // 1. 解析 META-INF/container.xml 定位 OPF 文件路径
        val opfPath = ContainerXmlParser.parseOpfPath(archive)
        val opfDir = PathResolver.getDirectory(opfPath)

        val opfContent = archive.getEntryText(opfPath)
            ?: throw IllegalArgumentException("OPF package file not found in archive: $opfPath")

        // 2. 解析 OPF (元数据、资源清单、阅读主链)
        val opfResult = OpfPackageParser.parse(opfContent, opfPath)

        // 3. 解析目录树 (优先 EPUB 3 Nav -> 降级 EPUB 2 NCX -> 兜底 Spine 生成)
        val toc = resolveToc(archive, opfResult, opfDir)

        return EpubBook(
            metadata = opfResult.metadata,
            opfPath = opfPath,
            opfDir = opfDir,
            manifest = opfResult.manifest,
            spine = opfResult.spine,
            toc = toc,
            coverImagePath = opfResult.coverImagePath,
            version = opfResult.version
        )
    }

    /**
     * 提取封面图片二进制数据。
     */
    fun extractCover(file: File): ByteArray? {
        return EpubZipArchive.fromFile(file).use { archive ->
            val book = parseArchive(archive)
            extractCoverBytes(archive, book)
        }
    }

    /**
     * 从输入流提取封面图片二进制数据。
     */
    fun extractCover(inputStream: InputStream): ByteArray? {
        return EpubZipArchive.fromStream(inputStream).use { archive ->
            val book = parseArchive(archive)
            extractCoverBytes(archive, book)
        }
    }

    /**
     * 根据归档和书籍信息提取封面二进制数据。
     */
    fun extractCoverBytes(archive: EpubZipArchive, book: EpubBook): ByteArray? {
        val coverPath = book.coverImagePath ?: return null
        return archive.getEntryBytes(coverPath)
    }

    /**
     * 解析指定章节的结构化内容。
     *
     * @param file EPUB 文件
     * @param contentPath 章节在 ZIP 压缩包中的路径 (如 "OEBPS/Text/ch01.xhtml")
     * @param defaultTitle 默认章节标题 (可选)
     */
    fun parseChapter(file: File, contentPath: String, defaultTitle: String? = null): EpubChapterContent {
        return EpubZipArchive.fromFile(file).use { archive ->
            parseChapterContent(archive, contentPath, defaultTitle)
        }
    }

    /**
     * 从已有归档解析指定章节内容。
     */
    fun parseChapterContent(
        archive: EpubZipArchive,
        contentPath: String,
        defaultTitle: String? = null
    ): EpubChapterContent {
        val xhtml = archive.getEntryText(contentPath)
            ?: throw IllegalArgumentException("Chapter content file not found: $contentPath")

        return XhtmlContentExtractor.extract(
            xhtmlContent = xhtml,
            chapterPath = contentPath,
            defaultTitle = defaultTitle
        )
    }

    /**
     * 转换为核心通用领域模型 [Book]。
     */
    fun toBook(
        epubBook: EpubBook,
        file: File,
        bookId: Long = 0L,
        coverCachePath: String? = null
    ): Book {
        return Book(
            id = bookId,
            title = epubBook.metadata.title,
            author = epubBook.metadata.author,
            coverPath = coverCachePath,
            uriString = file.absolutePath,
            format = BookFormat.EPUB,
            fileSize = file.length(),
            totalChapters = epubBook.spine.size,
            addTime = System.currentTimeMillis(),
            lastReadTime = System.currentTimeMillis(),
            archivePath = null
        )
    }

    /**
     * 转换为核心通用领域模型 [Chapter] 列表。
     * 基于 Spine 阅读主序列顺序展开，结合 TOC 目录树映射最佳章节标题。
     */
    fun toChapters(
        epubBook: EpubBook,
        archive: EpubZipArchive? = null,
        bookId: Long = 0L
    ): List<Chapter> {
        val flatToc = epubBook.flatToc
        // 优先匹配无 fragment 的父级章节标题，其次保留首个出现的条目
        val tocMap = mutableMapOf<String, EpubTocItem>()
        for (item in flatToc) {
            val existing = tocMap[item.contentPath]
            if (existing == null) {
                tocMap[item.contentPath] = item
            } else if (existing.fragment != null && item.fragment == null) {
                tocMap[item.contentPath] = item
            }
        }

        var cumulativeOffset = 0L

        return epubBook.spineItems.mapIndexed { index, manifestItem ->
            val contentPath = manifestItem.fullPath
            val tocItem = tocMap[contentPath]

            // 优先匹配 TOC 标题，否则如果传入了归档，尝试从正文首行抽取标题
            val title = tocItem?.title ?: if (archive != null) {
                try {
                    val rawXhtml = archive.getEntryText(contentPath)
                    if (rawXhtml != null) {
                        XhtmlContentExtractor.extract(rawXhtml, contentPath).chapterTitle
                    } else "第 ${index + 1} 章"
                } catch (_: Exception) {
                    "第 ${index + 1} 章"
                }
            } else {
                "第 ${index + 1} 章"
            }

            // 计算粗略字节长度 (以 ZIP 内条目字节为准)
            val entrySize = archive?.getEntryBytes(contentPath)?.size?.toLong() ?: 1024L
            val start = cumulativeOffset
            val end = start + entrySize
            cumulativeOffset = end

            Chapter(
                id = 0L,
                bookId = bookId,
                index = index,
                title = title,
                startOffset = start,
                endOffset = end,
                contentPath = contentPath
            )
        }
    }

    /**
     * 目录解析策略：优先 EPUB 3 nav -> 次选 EPUB 2 NCX -> 兜底由 Spine 生成。
     */
    private fun resolveToc(
        archive: EpubZipArchive,
        opfResult: OpfPackageParser.OpfResult,
        opfDir: String
    ): List<EpubTocItem> {
        // 1. 尝试解析 EPUB 3 nav.xhtml
        val navItem = opfResult.manifest.values.firstOrNull { it.isNav }
            ?: opfResult.manifest.values.firstOrNull { it.href.endsWith("nav.xhtml", ignoreCase = true) }

        if (navItem != null) {
            val navXml = archive.getEntryText(navItem.fullPath)
            if (navXml != null) {
                try {
                    val toc = NavXhtmlParser.parse(navXml, navItem.fullPath)
                    if (toc.isNotEmpty()) return toc
                } catch (_: Exception) {
                    // 解析失败时降级
                }
            }
        }

        // 2. 尝试解析 EPUB 2 toc.ncx
        val ncxItem = if (opfResult.tocManifestId != null) {
            opfResult.manifest[opfResult.tocManifestId]
        } else {
            opfResult.manifest.values.firstOrNull { it.isNcx }
        }

        if (ncxItem != null) {
            val ncxXml = archive.getEntryText(ncxItem.fullPath)
            if (ncxXml != null) {
                try {
                    val toc = TocNcxParser.parse(ncxXml, ncxItem.fullPath)
                    if (toc.isNotEmpty()) return toc
                } catch (_: Exception) {
                    // 解析失败时降级
                }
            }
        }

        // 3. 兜底策略：基于 Spine 与 Manifest 生成扁平目录
        return opfResult.spine.mapIndexedNotNull { index, spineItem ->
            val manifestItem = opfResult.manifest[spineItem.idRef] ?: return@mapIndexedNotNull null
            val fullPath = manifestItem.fullPath

            // 尝试读取章节标题
            var chapterTitle = "第 ${index + 1} 章"
            val xhtml = archive.getEntryText(fullPath)
            if (xhtml != null) {
                try {
                    chapterTitle = XhtmlContentExtractor.extract(xhtml, fullPath).chapterTitle
                } catch (_: Exception) {}
            }

            EpubTocItem(
                id = "spine-toc-$index",
                title = chapterTitle,
                href = manifestItem.href,
                contentPath = fullPath,
                playOrder = index + 1
            )
        }
    }
}
