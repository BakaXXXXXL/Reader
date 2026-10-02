package com.reader.app

import com.google.common.truth.Truth.assertThat
import com.reader.app.bridge.BookReadingBridge
import com.reader.app.navigation.AppRoute
import com.reader.core.model.BookFormat
import com.reader.core.model.ReaderConfig
import com.reader.engine.typography.model.PageDimensions
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 应用端到端业务全链路集成测试。
 * 覆盖：本地文件 -> 编码探测/ZIP解包 -> 智能分章 -> 避头尾排版物理分页 -> 阅读器数据模型。
 */
class AppIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val bridge = BookReadingBridge()

    @Test
    fun `test app route state modeling`() {
        val bookshelfRoute = AppRoute.Bookshelf
        val readerRoute = AppRoute.Reader(bookId = 1001L, initialChapterIndex = 2)

        assertThat(bookshelfRoute).isInstanceOf(AppRoute::class.java)
        assertThat(readerRoute.bookId).isEqualTo(1001L)
        assertThat(readerRoute.initialChapterIndex).isEqualTo(2)
    }

    @Test
    fun `test end-to-end TXT import, chapter splitting and typography pagination`() = runTest {
        // 1. 创建包含网络小说标准格式的测试 TXT 文件 (UTF-8)
        val txtFile = tempFolder.newFile("test_novel.txt")
        val novelContent = buildString {
            append("作品简介：这是一个测试小说作品。\n\n")
            append("第一章 踏入征途\n")
            append("晨曦初破，天地一片苍茫。少年背负长剑，驻足于悬崖绝巅，凝望远方。\n")
            append("山风拂过，青衫微动。他知道，这条修行之路注定充满荆棘与坎坷。\n\n")
            append("第二章 风云激荡\n")
            append("城关之下，烽火连天，数以万计的铁骑奔腾而过，大地震颤！\n")
        }
        txtFile.writeText(novelContent, Charsets.UTF_8)

        // 2. 调用桥接器打开书籍
        val parseResult = bridge.openBook(txtFile, BookFormat.TXT)
        assertThat(parseResult.title).isEqualTo("test_novel")
        assertThat(parseResult.format).isEqualTo(BookFormat.TXT)
        assertThat(parseResult.chapters).isNotEmpty()

        // 验证分章结果：智能识别序章与第一章
        assertThat(parseResult.chapters.any { it.title.contains("第一章") }).isTrue()
        val firstChapter = parseResult.chapters.first { it.title.contains("第一章") }

        // 3. 执行物理分页计算 (中文避头尾规则 + 网格对齐)
        val dimensions = PageDimensions(
            viewWidth = 800f,
            viewHeight = 1200f,
            paddingLeft = 40f,
            paddingTop = 60f,
            paddingRight = 40f,
            paddingBottom = 60f
        )
        val readerConfig = ReaderConfig(
            fontSizeSp = 18f,
            lineHeightMultiplier = 1.6f
        )

        val paginationResult = bridge.loadAndPaginateChapter(
            file = txtFile,
            format = BookFormat.TXT,
            chapter = firstChapter,
            charset = parseResult.charset,
            dimensions = dimensions,
            config = readerConfig
        )

        assertThat(paginationResult.chapterTitle).isEqualTo(firstChapter.title)
        assertThat(paginationResult.pages).isNotEmpty()

        // 验证分页覆盖范围与连续性
        val page1 = paginationResult.pages.first()
        assertThat(page1.lines).isNotEmpty()
        assertThat(page1.startCharOffset).isEqualTo(0)
    }

    @Test
    fun `test end-to-end EPUB import, container extraction and typography pagination`() = runTest {
        // 1. 创建合法标准 EPUB 测试文件
        val epubFile = tempFolder.newFile("test_ebook.epub")
        val archiveBytes = createSimpleEpubArchive(
            title = "修真指南",
            creator = "青莲剑仙",
            chapterHtml = """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>第一卷 筑基初成</title></head>
                <body>
                    <h2>第一卷 筑基初成</h2>
                    <p>天地灵气，运行周天。修行之道，贵在宁心静气，不可急躁。</p>
                </body>
                </html>
            """.trimIndent()
        )
        epubFile.writeBytes(archiveBytes)

        // 2. 调用桥接器打开 EPUB
        val parseResult = bridge.openBook(epubFile, BookFormat.EPUB)
        assertThat(parseResult.title).isEqualTo("修真指南")
        assertThat(parseResult.author).isEqualTo("青莲剑仙")
        assertThat(parseResult.chapters).isNotEmpty()

        // 3. 排版分页第一章
        val dimensions = PageDimensions(
            viewWidth = 720f,
            viewHeight = 1280f,
            paddingLeft = 32f,
            paddingTop = 48f,
            paddingRight = 32f,
            paddingBottom = 48f
        )
        val paginationResult = bridge.loadAndPaginateChapter(
            file = epubFile,
            format = BookFormat.EPUB,
            chapter = parseResult.chapters.first(),
            charset = parseResult.charset,
            dimensions = dimensions,
            config = ReaderConfig()
        )

        assertThat(paginationResult.fullText).contains("筑基初成")
        assertThat(paginationResult.pages).isNotEmpty()
    }

    private fun createSimpleEpubArchive(
        title: String,
        creator: String,
        chapterHtml: String
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun put(name: String, content: String) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            put("mimetype", "application/epub+zip")
            put("META-INF/container.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                    <rootfiles>
                        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                    </rootfiles>
                </container>
            """.trimIndent())

            put("OEBPS/content.opf", """
                <?xml version="1.0" encoding="utf-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
                    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:title>$title</dc:title>
                        <dc:creator>$creator</dc:creator>
                        <dc:identifier id="BookId">urn:uuid:test-e2e-epub-001</dc:identifier>
                        <dc:language>zh-CN</dc:language>
                    </metadata>
                    <manifest>
                        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                        <item id="ch1" href="Text/ch1.xhtml" media-type="application/xhtml+xml"/>
                    </manifest>
                    <spine toc="ncx">
                        <itemref idref="ch1"/>
                    </spine>
                </package>
            """.trimIndent())

            put("OEBPS/toc.ncx", """
                <?xml version="1.0" encoding="UTF-8"?>
                <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                    <head><meta name="dtb:uid" content="urn:uuid:test-e2e-epub-001"/></head>
                    <docTitle><text>$title</text></docTitle>
                    <navMap>
                        <navPoint id="np1" playOrder="1">
                            <navLabel><text>第一卷 筑基初成</text></navLabel>
                            <content src="Text/ch1.xhtml"/>
                        </navPoint>
                    </navMap>
                </ncx>
            """.trimIndent())

            put("OEBPS/Text/ch1.xhtml", chapterHtml)
        }
        return baos.toByteArray()
    }
}
