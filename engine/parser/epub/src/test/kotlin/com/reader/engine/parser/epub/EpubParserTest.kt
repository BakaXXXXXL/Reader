package com.reader.engine.parser.epub

import com.google.common.truth.Truth.assertThat
import com.reader.core.model.BookFormat
import com.reader.engine.parser.epub.extractor.PathResolver
import com.reader.engine.parser.epub.model.EpubContentElement
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * EPUB 2/3 解析引擎全流程测试集。
 * 覆盖：
 * 1. EPUB 2 标准解包、元数据提取、cover-image 识别与提取；
 * 2. EPUB 2 toc.ncx 多层级目录树解析与展开；
 * 3. 正文 XHTML 轻量提取、标签过滤 (<script>, <style>)、HTML 实体解析、内嵌图片相对路径重定向；
 * 4. EPUB 3 标准解包、properties="cover-image"、nav.xhtml 层级目录、SVG 内嵌图；
 * 5. 缺失目录时的 Spine 智能兜底机制；
 * 6. File 与 InputStream 流式解析一致性；
 * 7. 领域模型 Book 与 Chapter 映射验证；
 * 8. 边界相对路径解析算法。
 */
class EpubParserTest {

    private val parser = EpubBookParser()

    @Test
    fun testEpub2UnpackAndMetadata() {
        val epubBytes = EpubTestFixtureBuilder.createEpub2Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "epub2_test")

        val book = parser.parseSync(tempFile)

        // 元数据验证
        assertThat(book.metadata.title).isEqualTo("测试电子书二代")
        assertThat(book.metadata.author).isEqualTo("测试作者二号")
        assertThat(book.metadata.language).isEqualTo("zh-CN")
        assertThat(book.metadata.identifier).isEqualTo("urn:uuid:12345678-epub2")
        assertThat(book.opfPath).isEqualTo("OEBPS/content.opf")
        assertThat(book.opfDir).isEqualTo("OEBPS/")

        // Manifest 清单验证
        assertThat(book.manifest).containsKey("ch1")
        assertThat(book.manifest).containsKey("cover-image")
        assertThat(book.manifest["ch1"]?.fullPath).isEqualTo("OEBPS/Text/ch01.xhtml")

        // Spine 阅读序列验证
        assertThat(book.spine).hasSize(2)
        assertThat(book.spine[0].idRef).isEqualTo("ch1")
        assertThat(book.spine[1].idRef).isEqualTo("ch2")

        // 封面提取验证 (EPUB 2 meta name="cover" 规则)
        assertThat(book.coverImagePath).isEqualTo("OEBPS/Images/cover.jpg")
        val coverBytes = parser.extractCover(tempFile)
        assertThat(coverBytes).isNotNull()
        assertThat(coverBytes?.size).isEqualTo(6)

        tempFile.delete()
    }

    @Test
    fun testEpub2TocHierarchical() {
        val epubBytes = EpubTestFixtureBuilder.createEpub2Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "epub2_toc")

        val book = parser.parseSync(tempFile)

        // 顶层目录数量为 2
        assertThat(book.toc).hasSize(2)

        val ch1Toc = book.toc[0]
        assertThat(ch1Toc.title).isEqualTo("第一章 序章开启")
        assertThat(ch1Toc.contentPath).isEqualTo("OEBPS/Text/ch01.xhtml")
        assertThat(ch1Toc.children).hasSize(1)

        val subToc = ch1Toc.children[0]
        assertThat(subToc.title).isEqualTo("1.1 背景设定")
        assertThat(subToc.contentPath).isEqualTo("OEBPS/Text/ch01.xhtml")
        assertThat(subToc.fragment).isEqualTo("section1")

        val ch2Toc = book.toc[1]
        assertThat(ch2Toc.title).isEqualTo("第二章 踏上征途")
        assertThat(ch2Toc.contentPath).isEqualTo("OEBPS/Text/ch02.xhtml")

        // 展平总计 3 个目录项
        assertThat(book.flatToc).hasSize(3)

        tempFile.delete()
    }

    @Test
    fun testEpub2ChapterContentAndFiltering() {
        val epubBytes = EpubTestFixtureBuilder.createEpub2Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "epub2_chapter")

        val chapter = parser.parseChapter(tempFile, "OEBPS/Text/ch01.xhtml")

        // 标题推断验证
        assertThat(chapter.chapterTitle).isEqualTo("第一章 序章开启")

        // 过滤验证：脚本代码和样式绝不可出现
        assertThat(chapter.plainText).doesNotContain("should be filtered")
        assertThat(chapter.plainText).doesNotContain("color: #333")

        // 实体替换验证：&nbsp; 与 &mdash; 被正确转义，未发生 SAX 异常
        assertThat(chapter.plainText).contains("很久很久以前，在远古的大陆上发生了一场风暴。")
        assertThat(chapter.plainText).contains("文明由此诞生，伴随着不可磨灭的印记——传奇由此揭开篇章。")

        // 元素结构验证
        val headings = chapter.elements.filterIsInstance<EpubContentElement.Heading>()
        assertThat(headings).hasSize(2)
        assertThat(headings[0].text).isEqualTo("第一章 序章开启")
        assertThat(headings[0].level).isEqualTo(1)
        assertThat(headings[1].text).isEqualTo("1.1 背景设定")
        assertThat(headings[1].level).isEqualTo(2)

        // 图片相对路径重定向验证
        val images = chapter.elements.filterIsInstance<EpubContentElement.Image>()
        assertThat(images).hasSize(1)
        assertThat(images[0].imagePath).isEqualTo("OEBPS/Images/diagram.png")
        assertThat(images[0].altText).isEqualTo("大陆演变图")
        assertThat(chapter.imagePaths).containsExactly("OEBPS/Images/diagram.png")

        tempFile.delete()
    }

    @Test
    fun testEpub3UnpackAndNav() {
        val epubBytes = EpubTestFixtureBuilder.createEpub3Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "epub3_test")

        val book = parser.parseSync(tempFile)

        // EPUB 3 元数据
        assertThat(book.metadata.title).isEqualTo("EPUB3标准测试书籍")
        assertThat(book.metadata.author).isEqualTo("三体文明观察者")
        assertThat(book.metadata.identifier).isEqualTo("urn:isbn:9787123456789")
        assertThat(book.version).isEqualTo("3.0")

        // EPUB 3 properties="cover-image" 封面识别
        assertThat(book.coverImagePath).isEqualTo("EPUB/assets/cover.png")
        val coverBytes = parser.extractCover(tempFile)
        assertThat(coverBytes).isNotNull()
        assertThat(coverBytes?.size).isEqualTo(4)

        // EPUB 3 nav.xhtml 目录树解析
        assertThat(book.toc).hasSize(2)
        assertThat(book.toc[0].title).isEqualTo("第一卷：太阳系边际")
        assertThat(book.toc[0].contentPath).isEqualTo("EPUB/chapters/chapter1.xhtml")
        assertThat(book.toc[0].children).hasSize(1)
        assertThat(book.toc[0].children[0].title).isEqualTo("第一节 探测器信号")
        assertThat(book.toc[0].children[0].fragment).isEqualTo("p1")

        assertThat(book.toc[1].title).isEqualTo("第二卷：黑暗森林")

        tempFile.delete()
    }

    @Test
    fun testEpub3ChapterSvgImage() {
        val epubBytes = EpubTestFixtureBuilder.createEpub3Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "epub3_svg")

        val chapter2 = parser.parseChapter(tempFile, "EPUB/chapters/chapter2.xhtml")

        assertThat(chapter2.chapterTitle).isEqualTo("第二卷：黑暗森林")
        assertThat(chapter2.plainText).contains("宇宙就是一座黑暗森林，每个文明都是带枪的猎人。")

        // SVG 内嵌 <image href="../assets/forest.jpg"/> 路径解析验证
        val images = chapter2.elements.filterIsInstance<EpubContentElement.Image>()
        assertThat(images).hasSize(1)
        assertThat(images[0].imagePath).isEqualTo("EPUB/assets/forest.jpg")
        assertThat(chapter2.imagePaths).containsExactly("EPUB/assets/forest.jpg")

        tempFile.delete()
    }

    @Test
    fun testEpubFallbackWithoutToc() {
        val epubBytes = EpubTestFixtureBuilder.createEpubWithoutTocBytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "epub_fallback")

        val book = parser.parseSync(tempFile)

        // 验证即使完全没有 toc.ncx 与 nav.xhtml，也能从 Spine 自动推断构建章节目录
        assertThat(book.toc).hasSize(2)
        assertThat(book.toc[0].title).isEqualTo("自定义标题一")
        assertThat(book.toc[0].contentPath).isEqualTo("s1.xhtml")
        assertThat(book.toc[1].title).isEqualTo("自定义标题二")
        assertThat(book.toc[1].contentPath).isEqualTo("s2.xhtml")

        tempFile.delete()
    }

    @Test
    fun testStreamingInputParity() {
        val epubBytes = EpubTestFixtureBuilder.createEpub2Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "parity")

        val bookFromFile = parser.parseSync(tempFile)
        val bookFromStream = parser.parseSync(ByteArrayInputStream(epubBytes))

        assertThat(bookFromStream.metadata.title).isEqualTo(bookFromFile.metadata.title)
        assertThat(bookFromStream.metadata.author).isEqualTo(bookFromFile.metadata.author)
        assertThat(bookFromStream.spine.size).isEqualTo(bookFromFile.spine.size)
        assertThat(bookFromStream.toc.size).isEqualTo(bookFromFile.toc.size)
        assertThat(bookFromStream.coverImagePath).isEqualTo(bookFromFile.coverImagePath)

        tempFile.delete()
    }

    @Test
    fun testAsyncCoroutinesParse() = runTest {
        val epubBytes = EpubTestFixtureBuilder.createEpub2Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "async")

        val book = parser.parse(tempFile)
        assertThat(book.metadata.title).isEqualTo("测试电子书二代")

        tempFile.delete()
    }

    @Test
    fun testDomainModelMapping() {
        val epubBytes = EpubTestFixtureBuilder.createEpub2Bytes()
        val tempFile = EpubTestFixtureBuilder.writeToTempFile(epubBytes, "mapping")

        val book = parser.parseSync(tempFile)
        val domainBook = parser.toBook(book, tempFile, bookId = 1001L, coverCachePath = "/cache/cover.jpg")

        assertThat(domainBook.id).isEqualTo(1001L)
        assertThat(domainBook.title).isEqualTo("测试电子书二代")
        assertThat(domainBook.author).isEqualTo("测试作者二号")
        assertThat(domainBook.format).isEqualTo(BookFormat.EPUB)
        assertThat(domainBook.coverPath).isEqualTo("/cache/cover.jpg")
        assertThat(domainBook.totalChapters).isEqualTo(2)

        val chapters = parser.toChapters(book, bookId = 1001L)
        assertThat(chapters).hasSize(2)
        assertThat(chapters[0].bookId).isEqualTo(1001L)
        assertThat(chapters[0].index).isEqualTo(0)
        assertThat(chapters[0].title).isEqualTo("第一章 序章开启")
        assertThat(chapters[0].contentPath).isEqualTo("OEBPS/Text/ch01.xhtml")

        assertThat(chapters[1].index).isEqualTo(1)
        assertThat(chapters[1].title).isEqualTo("第二章 踏上征途")
        assertThat(chapters[1].contentPath).isEqualTo("OEBPS/Text/ch02.xhtml")

        tempFile.delete()
    }

    @Test
    fun testPathResolverEdgeCases() {
        assertThat(PathResolver.normalize("OEBPS/Text/../Images/cover.jpg")).isEqualTo("OEBPS/Images/cover.jpg")
        assertThat(PathResolver.normalize("./OEBPS//Text/./ch1.xhtml")).isEqualTo("OEBPS/Text/ch1.xhtml")
        assertThat(PathResolver.normalize("OEBPS%20Book/file%201.xhtml")).isEqualTo("OEBPS Book/file 1.xhtml")
        assertThat(PathResolver.resolve("OEBPS/Text/", "../Images/diagram.png")).isEqualTo("OEBPS/Images/diagram.png")
        assertThat(PathResolver.resolve("OEBPS/", "Text/ch01.xhtml")).isEqualTo("OEBPS/Text/ch01.xhtml")
        assertThat(PathResolver.getDirectory("OEBPS/content.opf")).isEqualTo("OEBPS/")
        assertThat(PathResolver.getDirectory("content.opf")).isEqualTo("")

        val (path, frag) = PathResolver.splitFragment("chapter1.xhtml#section_1")
        assertThat(path).isEqualTo("chapter1.xhtml")
        assertThat(frag).isEqualTo("section_1")
    }
}
