package com.reader.feature.bookshelf.data

import android.content.Context
import com.reader.core.database.ReaderDatabase
import com.reader.core.database.entity.BookEntity
import com.reader.core.database.entity.ChapterEntity
import com.reader.core.database.entity.ReadLocatorEntity
import com.reader.core.model.BookFormat
import com.reader.engine.parser.epub.EpubBookParser
import com.reader.engine.parser.txt.TxtBookParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 初次启动预装经典公版示例书籍初始化器。
 * 确保用户首次安装进入应用即可体验真实的离线目录、避头尾排版与多模式翻页，杜绝空白与死锁。
 */
object SampleBookInitializer {

    suspend fun ensureSampleBooks(
        context: Context,
        database: ReaderDatabase
    ) = withContext(Dispatchers.IO) {
        if (database.bookDao().getBookCount() > 0) return@withContext

        val booksDir = File(context.filesDir, "books").apply { mkdirs() }

        // 1. 生成《阿Q正传》(公版经典 TXT)
        val txtFile = File(booksDir, "sample_aq_story.txt")
        if (!txtFile.exists()) {
            txtFile.writeText(SAMPLE_TXT_CONTENT, Charsets.UTF_8)
        }
        installTxtBook(txtFile, database)

        // 2. 生成《经典短篇小说选》(标准 EPUB)
        val epubFile = File(booksDir, "sample_classic_stories.epub")
        if (!epubFile.exists()) {
            val epubBytes = createSampleEpubArchive()
            epubFile.writeBytes(epubBytes)
        }
        installEpubBook(epubFile, database)
    }

    private suspend fun installTxtBook(file: File, database: ReaderDatabase) {
        val parser = TxtBookParser()
        val detection = parser.detectCharset(file)
        val chapters = parser.parseChapters(file, charset = detection.charset)

        val bookEntity = BookEntity(
            title = "阿Q正传",
            author = "鲁迅",
            coverPath = null,
            uriString = file.absolutePath,
            format = BookFormat.TXT,
            fileSize = file.length(),
            totalChapters = chapters.size,
            addTime = System.currentTimeMillis(),
            lastReadTime = System.currentTimeMillis()
        )
        val bookId = database.bookDao().insertBook(bookEntity)

        val chapterEntities = chapters.map { chapter ->
            ChapterEntity(
                bookId = bookId,
                index = chapter.index,
                title = chapter.title,
                startOffset = chapter.startOffset,
                endOffset = chapter.endOffset,
                contentPath = null
            )
        }
        database.chapterDao().insertChapters(chapterEntities)

        database.readLocatorDao().saveLocator(
            ReadLocatorEntity(
                bookId = bookId,
                chapterIndex = 0,
                chapterTitle = chapters.firstOrNull()?.title ?: "正文",
                charOffset = 0,
                progression = 0f,
                pageIndexInChapter = 0,
                totalPagesInChapter = 1,
                updateTime = System.currentTimeMillis()
            )
        )
    }

    private suspend fun installEpubBook(file: File, database: ReaderDatabase) {
        val parser = EpubBookParser()
        val epubBook = parser.parse(file)
        val book = parser.toBook(epubBook, file)

        val bookEntity = BookEntity(
            title = book.title,
            author = book.author,
            coverPath = null,
            uriString = file.absolutePath,
            format = BookFormat.EPUB,
            fileSize = file.length(),
            totalChapters = epubBook.spine.size,
            addTime = System.currentTimeMillis(),
            lastReadTime = System.currentTimeMillis() - 1000
        )
        val bookId = database.bookDao().insertBook(bookEntity)

        val chapters = parser.toChapters(epubBook, archive = null, bookId = bookId)
        val chapterEntities = chapters.map { ch ->
            ChapterEntity(
                bookId = bookId,
                index = ch.index,
                title = ch.title,
                startOffset = ch.startOffset,
                endOffset = ch.endOffset,
                contentPath = ch.contentPath
            )
        }
        database.chapterDao().insertChapters(chapterEntities)

        database.readLocatorDao().saveLocator(
            ReadLocatorEntity(
                bookId = bookId,
                chapterIndex = 0,
                chapterTitle = chapters.firstOrNull()?.title ?: "第一章",
                charOffset = 0,
                progression = 0f,
                pageIndexInChapter = 0,
                totalPagesInChapter = 1,
                updateTime = System.currentTimeMillis()
            )
        )
    }

    private fun createSampleEpubArchive(): ByteArray {
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
                        <dc:title>世界短篇经典集</dc:title>
                        <dc:creator>欧·亨利 & 契诃夫</dc:creator>
                        <dc:identifier id="BookId">urn:uuid:reader-app-sample-epub</dc:identifier>
                        <dc:language>zh-CN</dc:language>
                    </metadata>
                    <manifest>
                        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                        <item id="ch1" href="Text/ch1.xhtml" media-type="application/xhtml+xml"/>
                        <item id="ch2" href="Text/ch2.xhtml" media-type="application/xhtml+xml"/>
                    </manifest>
                    <spine toc="ncx">
                        <itemref idref="ch1"/>
                        <itemref idref="ch2"/>
                    </spine>
                </package>
            """.trimIndent())

            put("OEBPS/toc.ncx", """
                <?xml version="1.0" encoding="UTF-8"?>
                <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                    <head><meta name="dtb:uid" content="urn:uuid:reader-app-sample-epub"/></head>
                    <docTitle><text>世界短篇经典集</text></docTitle>
                    <navMap>
                        <navPoint id="np1" playOrder="1">
                            <navLabel><text>第一章 麦琪的礼物</text></navLabel>
                            <content src="Text/ch1.xhtml"/>
                        </navPoint>
                        <navPoint id="np2" playOrder="2">
                            <navLabel><text>第二章 变色龙</text></navLabel>
                            <content src="Text/ch2.xhtml"/>
                        </navPoint>
                    </navMap>
                </ncx>
            """.trimIndent())

            put("OEBPS/Text/ch1.xhtml", """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>第一章 麦琪的礼物</title></head>
                <body>
                    <h2>第一章 麦琪的礼物</h2>
                    <p>一元八角七分。这就是全部的钱。其中六角是一分一分的硬币。这些硬币是向杂货店老板、菜贩和肉店老板一分两分地讨价还价，扣留下来积攒起来的。</p>
                    <p>德拉哭了。她坐在那张破旧的小沙发上，抽抽搭搭地哭泣着。生活中由哭泣、抽噎和微笑组成，而抽噎占了大部分。</p>
                    <p>德拉有一头美丽的秀发。每当她站在镜子前，解开长发，那栗色的瀑布便倾泻而下，一直垂到膝盖以下。她深爱着她的丈夫吉姆，而今天正是圣诞前夜。</p>
                    <p>吉姆有一只金表，是他祖父传给父亲，父亲又传给他的传家宝。德拉决心用自己的长发为吉姆换一条配得上金表的白金表链。</p>
                </body>
                </html>
            """.trimIndent())

            put("OEBPS/Text/ch2.xhtml", """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>第二章 变色龙</title></head>
                <body>
                    <h2>第二章 变色龙</h2>
                    <p>警官奥楚蔑洛夫穿着新的军大衣，提着小包，穿过市集的广场。他身后跟着一个巡警，端着一个筛子，上面盛满了没收来的醋栗。</p>
                    <p>四周一片寂静。广场上连人影也没有。小铺和酒店敞开大门，无精打采地面向光明的天地，犹如饥饿的嘴巴；铺子附近甚至连乞丐也没有。</p>
                    <p>“你敢咬人，该死的东西！”奥楚蔑洛夫忽然听见叫喊声。“伙计们，别放走它！如今咬人可不行！逮住它！哎哟……哎哟！”</p>
                    <p>一条小猎狗正一瘸一拐地跑着，身后紧跟着一个首饰匠赫留金，手里举着流血的手指。随着围观群众的一言一语，警官的脸色与态度犹如变色龙般反复变换。</p>
                </body>
                </html>
            """.trimIndent())
        }
        return baos.toByteArray()
    }

    private val SAMPLE_TXT_CONTENT = """
作品简介：鲁迅先生创作的中篇小说代表作，中国现代文学史上的不朽丰碑。

第一章 序
　　我要给阿Ｑ做正传，已经不止一两年了。但一面要做，一面又往回想，这足见我不是一个“立言”的人，因为从来以做传闻的人，大抵要立言的。
　　古人云，“人病舍其田而芸人之田”，亦所谓多事也。然而阿Ｑ究竟不可不做传，虽然他的姓氏名号，生辰里居，并不可考。
　　阿Ｑ没有家，住在未庄的土谷祠里；也没有固定的职业，只给人家做短工，割麦便割麦，舂米便舂米，撑船便撑船。工作略长久时，他也或住在临时主人的家里，但一完就走了。
　　阿Ｑ虽然贫苦，然而心胸开阔，自尊自大。未庄的人常常同他开玩笑，但阿Ｑ却全不在乎，心里想着：“我总算被儿子打了，现在的世界真不像样……”于是也心满意足地得胜了。

第二章 优胜记略
　　阿Ｑ不仅是未庄第一等风流人物，而且在精神上更是无往而不胜的强者。
　　例如有人打了他，他便在心头默念：“我是第一个能够自轻自贱的人，除了‘自轻自贱’不算外，余下的就是‘第一个’。状元不也是‘第一个’么？你算是什么东西呢！”
　　有一回，他同赵太爷家的工钱起了争执。然而阿Ｑ是断不肯吃亏的。闲人揪住了他的辫子，往墙上撞了四五个响头，闲人得意地走了，阿Ｑ站了一刻，心里想，“我总算被儿子打了，现在的世界真不像样……”于是也心满意足的得胜的走了。
　　阿Ｑ走到土谷祠，肚子里想道，“现在的世界真不成话，儿子打老子……”想到这里，阿Ｑ的精神便又十分愉快起来。

第三章 续优胜记略
　　然而阿Ｑ虽然常优胜，却直待蒙了赵太爷的一个巴掌，才渐渐的有些名气。
　　赵太爷的儿子进了秀才，锣声镗镗的报捷。阿Ｑ正喝了两碗黄酒，便手舞足蹈的说，这于他也很光采，因为他和赵太爷原来是本家，细细的排起来他还比秀才长三辈呢。
　　第二天，赵太爷便叫地保唤了他来。赵太爷满脸通红，喝道：“阿Ｑ，你这浑小子，你说我是你的本家么？你怎么敢胡说！你怎么敢姓赵！”
　　阿Ｑ不开口。赵太爷跳过去给了他一个嘴巴，阿Ｑ捂着脸退了出去。未庄的人听见这件事，并不怜悯他，倒反十分敬畏他，因为他居然敢同赵太爷认亲。

第四章 恋爱的悲剧
　　阿Ｑ虽然常优胜，但未庄的人渐不提起他了。阿Ｑ很寂寞，在路上遇着小尼姑，便忍不住走上去摩挲她的头皮，哈哈大笑说：“和尚动得，我动不得？”
　　小尼姑满脸通红地跑了，阿Ｑ心里十分快活，飘飘然地飞到了赵太爷家里去春米。
　　在赵太爷家里，他遇见了吴妈。吴妈是赵府唯一的年轻女仆，洗了碗碟坐在长凳上同阿Ｑ闲话。
　　阿Ｑ忽然跪下道：“吴妈，我和你困觉，我和你困觉！”
　　吴妈惊叫一声，大哭着跑了出去。阿Ｑ的恋爱剧，便以这极简短的一跪与一声惊叫而告终。

第五章 生计问题
　　阿Ｑ因为求爱的事情，被赵秀才用竹杠打了一顿，衣服被典当，短工也无人雇他了。
　　未庄的酒店、肉铺都不肯赊欠他东西。阿Ｑ走投无路，肚里辘辘地响，只得走向城里去寻出路。
　　过了半年，阿Ｑ居然又回到了未庄，而且发了财，身上穿着簇新的夹袄，口袋里有沉甸甸的银洋，拿出来许多古旧的绸缎衣物卖给未庄的太太小姐们。
　　后来未庄的人才知道，原来阿Ｑ在城里给举人老爷家做小偷的帮手，只管在墙外接包裹。
    """.trimIndent()
}
