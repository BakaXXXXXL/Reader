package com.reader.engine.parser.epub

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 构造用于单元测试的标准 EPUB 2 与 EPUB 3 归档测试夹具构建器。
 */
object EpubTestFixtureBuilder {

    /**
     * 构建标准 EPUB 2 ZIP 二进制字节数组。
     */
    fun createEpub2Bytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // mimetype
            addZipEntry(zos, "mimetype", "application/epub+zip".toByteArray())

            // META-INF/container.xml
            val containerXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent()
            addZipEntry(zos, "META-INF/container.xml", containerXml.toByteArray())

            // OEBPS/content.opf
            val opfContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <package version="2.0" xmlns="http://www.idpf.org/2007/opf" unique-identifier="book-id">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
                    <dc:title>测试电子书二代</dc:title>
                    <dc:creator>测试作者二号</dc:creator>
                    <dc:language>zh-CN</dc:language>
                    <dc:identifier id="book-id">urn:uuid:12345678-epub2</dc:identifier>
                    <meta name="cover" content="cover-image"/>
                  </metadata>
                  <manifest>
                    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                    <item id="cover-image" href="Images/cover.jpg" media-type="image/jpeg"/>
                    <item id="ch1" href="Text/ch01.xhtml" media-type="application/xhtml+xml"/>
                    <item id="ch2" href="Text/ch02.xhtml" media-type="application/xhtml+xml"/>
                    <item id="img1" href="Images/diagram.png" media-type="image/png"/>
                  </manifest>
                  <spine toc="ncx">
                    <itemref idref="ch1"/>
                    <itemref idref="ch2"/>
                  </spine>
                </package>
            """.trimIndent()
            addZipEntry(zos, "OEBPS/content.opf", opfContent.toByteArray())

            // OEBPS/toc.ncx (EPUB 2 层级目录)
            val ncxContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                  <docTitle><text>测试电子书二代</text></docTitle>
                  <navMap>
                    <navPoint id="np-1" playOrder="1">
                      <navLabel><text>第一章 序章开启</text></navLabel>
                      <content src="Text/ch01.xhtml"/>
                      <navPoint id="np-1-1" playOrder="2">
                        <navLabel><text>1.1 背景设定</text></navLabel>
                        <content src="Text/ch01.xhtml#section1"/>
                      </navPoint>
                    </navPoint>
                    <navPoint id="np-2" playOrder="3">
                      <navLabel><text>第二章 踏上征途</text></navLabel>
                      <content src="Text/ch02.xhtml"/>
                    </navPoint>
                  </navMap>
                </ncx>
            """.trimIndent()
            addZipEntry(zos, "OEBPS/toc.ncx", ncxContent.toByteArray())

            // OEBPS/Text/ch01.xhtml
            val ch1Content = """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <title>第一章</title>
                  <script type="text/javascript">alert('should be filtered');</script>
                  <style type="text/css">p { color: #333; }</style>
                </head>
                <body>
                  <h1>第一章 序章开启</h1>
                  <p>很久很久以前，在远古的大陆上发生了一场风暴。&nbsp;这是测试文本。</p>
                  <div id="section1">
                    <h2>1.1 背景设定</h2>
                    <p>文明由此诞生，伴随着不可磨灭的印记&mdash;&mdash;传奇由此揭开篇章。</p>
                    <p><img src="../Images/diagram.png" alt="大陆演变图"/></p>
                  </div>
                </body>
                </html>
            """.trimIndent()
            addZipEntry(zos, "OEBPS/Text/ch01.xhtml", ch1Content.toByteArray())

            // OEBPS/Text/ch02.xhtml
            val ch2Content = """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <body>
                  <h1>第二章 踏上征途</h1>
                  <p>年轻的勇者整装待发，迎向未知的黎明。</p>
                </body>
                </html>
            """.trimIndent()
            addZipEntry(zos, "OEBPS/Text/ch02.xhtml", ch2Content.toByteArray())

            // Dummy Images
            val dummyCoverBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x12, 0x34)
            val dummyImgBytes = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x56, 0x78)
            addZipEntry(zos, "OEBPS/Images/cover.jpg", dummyCoverBytes)
            addZipEntry(zos, "OEBPS/Images/diagram.png", dummyImgBytes)
        }
        return baos.toByteArray()
    }

    /**
     * 构建标准 EPUB 3 ZIP 二进制字节数组。
     */
    fun createEpub3Bytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            addZipEntry(zos, "mimetype", "application/epub+zip".toByteArray())

            val containerXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="EPUB/package.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent()
            addZipEntry(zos, "META-INF/container.xml", containerXml.toByteArray())

            val opfContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <package version="3.0" xmlns="http://www.idpf.org/2007/opf" unique-identifier="uid">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>EPUB3标准测试书籍</dc:title>
                    <dc:creator>三体文明观察者</dc:creator>
                    <dc:language>zh</dc:language>
                    <dc:identifier id="uid">urn:isbn:9787123456789</dc:identifier>
                  </metadata>
                  <manifest>
                    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="cover" href="assets/cover.png" media-type="image/png" properties="cover-image"/>
                    <item id="c1" href="chapters/chapter1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c2" href="chapters/chapter2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                    <itemref idref="c2"/>
                  </spine>
                </package>
            """.trimIndent()
            addZipEntry(zos, "EPUB/package.opf", opfContent.toByteArray())

            // EPUB 3 nav.xhtml
            val navContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                <head><title>目录</title></head>
                <body>
                  <nav epub:type="toc" id="toc">
                    <h1>目录</h1>
                    <ol>
                      <li>
                        <a href="chapters/chapter1.xhtml">第一卷：太阳系边际</a>
                        <ol>
                          <li><a href="chapters/chapter1.xhtml#p1">第一节 探测器信号</a></li>
                        </ol>
                      </li>
                      <li>
                        <a href="chapters/chapter2.xhtml">第二卷：黑暗森林</a>
                      </li>
                    </ol>
                  </nav>
                </body>
                </html>
            """.trimIndent()
            addZipEntry(zos, "EPUB/nav.xhtml", navContent.toByteArray())

            // EPUB 3 chapter 1
            val c1Content = """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>第一卷</title></head>
                <body>
                  <h1>第一卷：太阳系边际</h1>
                  <p>引力波天线静默伫立，跨越星河的微弱脉冲终于被捕获。</p>
                  <div id="p1">
                    <h2>第一节 探测器信号</h2>
                    <p>这绝不是来自自然天体的规律辐射。<br/>那是一个警告。</p>
                  </div>
                </body>
                </html>
            """.trimIndent()
            addZipEntry(zos, "EPUB/chapters/chapter1.xhtml", c1Content.toByteArray())

            // EPUB 3 chapter 2 (with SVG image)
            val c2Content = """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <body>
                  <h1>第二卷：黑暗森林</h1>
                  <p>宇宙就是一座黑暗森林，每个文明都是带枪的猎人。</p>
                  <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                    <image href="../assets/forest.jpg" width="100" height="100"/>
                  </svg>
                </body>
                </html>
            """.trimIndent()
            addZipEntry(zos, "EPUB/chapters/chapter2.xhtml", c2Content.toByteArray())

            val dummyCoverBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            addZipEntry(zos, "EPUB/assets/cover.png", dummyCoverBytes)
            addZipEntry(zos, "EPUB/assets/forest.jpg", byteArrayOf(0x05, 0x06))
        }
        return baos.toByteArray()
    }

    /**
     * 构建无任何 TOC (无 toc.ncx 亦无 nav.xhtml) 的极简 EPUB，用于测试兜底逻辑。
     */
    fun createEpubWithoutTocBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            addZipEntry(zos, "mimetype", "application/epub+zip".toByteArray())

            val containerXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent()
            addZipEntry(zos, "META-INF/container.xml", containerXml.toByteArray())

            val opfContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <package version="2.0" xmlns="http://www.idpf.org/2007/opf" unique-identifier="id">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>无目录兜底书</dc:title>
                    <dc:creator>兜底作者</dc:creator>
                  </metadata>
                  <manifest>
                    <item id="s1" href="s1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="s2" href="s2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="s1"/>
                    <itemref idref="s2"/>
                  </spine>
                </package>
            """.trimIndent()
            addZipEntry(zos, "content.opf", opfContent.toByteArray())

            val s1Content = """
                <html><body><h1>自定义标题一</h1><p>内容一</p></body></html>
            """.trimIndent()
            val s2Content = """
                <html><body><h2>自定义标题二</h2><p>内容二</p></body></html>
            """.trimIndent()

            addZipEntry(zos, "s1.xhtml", s1Content.toByteArray())
            addZipEntry(zos, "s2.xhtml", s2Content.toByteArray())
        }
        return baos.toByteArray()
    }

    /**
     * 将字节数组写入指定临时文件。
     */
    fun writeToTempFile(bytes: ByteArray, prefix: String = "test_epub", suffix: String = ".epub"): File {
        val temp = File.createTempFile(prefix, suffix)
        temp.deleteOnExit()
        FileOutputStream(temp).use { it.write(bytes) }
        return temp
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, data: ByteArray) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }
}
