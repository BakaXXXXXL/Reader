package com.reader.feature.reader

import com.reader.core.database.ReaderDatabase
import com.reader.core.database.mapper.asDomain
import com.reader.core.database.mapper.asEntity
import com.reader.core.datastore.ReaderPreferencesDataStore
import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.core.model.Bookmark
import com.reader.core.model.Chapter
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReaderConfig
import com.reader.engine.parser.epub.EpubBookParser
import com.reader.engine.parser.txt.TxtBookParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 生产级 Room SQLite 与本地电子书解析数据源实现。
 * 真实从数据库读取书籍元数据、章节列表、定位记录与书签，
 * 真实从磁盘文件流式读取并解析 TXT / EPUB 正文内容。
 */
class RoomReaderDataSource(
    private val database: ReaderDatabase,
    private val preferencesDataStore: ReaderPreferencesDataStore? = null,
    private val txtParser: TxtBookParser = TxtBookParser(),
    private val epubParser: EpubBookParser = EpubBookParser()
) : ReaderDataSource {

    override suspend fun getBook(bookId: Long): Book? = withContext(Dispatchers.IO) {
        database.bookDao().getBookById(bookId)?.asDomain()
            ?: database.bookDao().getAllBooks().firstOrNull()?.asDomain()
    }

    override suspend fun getChapters(bookId: Long): List<Chapter> = withContext(Dispatchers.IO) {
        val chapters = database.chapterDao().getChapters(bookId)
        if (chapters.isNotEmpty()) {
            return@withContext chapters.map { it.asDomain() }
        }

        // 若数据库中暂无章节（例如外部文件直接调起），动态从物理文件解析并存入数据库
        val book = getBook(bookId) ?: return@withContext emptyList()
        val file = File(book.uriString)
        if (!file.exists()) return@withContext emptyList()

        val extracted = when (book.format) {
            BookFormat.TXT -> {
                val detection = txtParser.detectCharset(file)
                txtParser.parseChapters(file, charset = detection.charset)
            }
            BookFormat.EPUB -> {
                val epubBook = epubParser.parse(file)
                epubParser.toChapters(epubBook, archive = null, bookId = book.id)
            }
            else -> emptyList()
        }

        if (extracted.isNotEmpty()) {
            val entities = extracted.map { it.asEntity().copy(bookId = bookId) }
            database.chapterDao().insertChapters(entities)
        }
        extracted
    }

    override suspend fun getLocator(bookId: Long): ReadLocator? = withContext(Dispatchers.IO) {
        database.readLocatorDao().getLocator(bookId)?.asDomain()
    }

    override suspend fun saveLocator(locator: ReadLocator) = withContext(Dispatchers.IO) {
        database.readLocatorDao().saveLocator(locator.asEntity())
        database.bookDao().updateLastReadTime(locator.bookId, System.currentTimeMillis())
    }

    override suspend fun getBookmarks(bookId: Long): List<Bookmark> = withContext(Dispatchers.IO) {
        database.bookmarkDao().getBookmarks(bookId).map { it.asDomain() }
    }

    override suspend fun saveBookmark(bookmark: Bookmark): Bookmark = withContext(Dispatchers.IO) {
        val newId = database.bookmarkDao().insertBookmark(bookmark.asEntity())
        bookmark.copy(id = newId)
    }

    override suspend fun deleteBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
        database.bookmarkDao().deleteBookmarkById(bookmarkId)
    }

    override suspend fun getReaderConfig(): ReaderConfig = withContext(Dispatchers.IO) {
        val prefs = preferencesDataStore ?: return@withContext ReaderConfig.DEFAULT
        prefs.readerConfigFlow.firstOrNull() ?: ReaderConfig.DEFAULT
    }

    override suspend fun saveReaderConfig(config: ReaderConfig) = withContext(Dispatchers.IO) {
        preferencesDataStore?.updateConfig(config)
        Unit
    }

    override suspend fun getChapterContent(bookId: Long, chapterIndex: Int): String = withContext(Dispatchers.IO) {
        val book = getBook(bookId) ?: return@withContext ""
        val chapters = getChapters(bookId)
        val chapter = chapters.find { it.index == chapterIndex }
            ?: chapters.getOrNull(chapterIndex)
            ?: return@withContext ""

        val file = File(book.uriString)
        if (!file.exists()) {
            return@withContext "【提示】未找到本地书籍文件：${book.uriString}"
        }

        return@withContext try {
            when (book.format) {
                BookFormat.TXT -> {
                    val detection = txtParser.detectCharset(file)
                    val content = txtParser.loadChapterContent(file, chapter, detection.charset)
                    content.paragraphs.joinToString("\n\n")
                }
                BookFormat.EPUB -> {
                    val contentPath = chapter.contentPath ?: ""
                    val content = epubParser.parseChapter(file, contentPath, chapter.title)
                    content.plainText
                }
                else -> {
                    if (file.length() < 2 * 1024 * 1024) file.readText() else "文件过大，无法直接显示正文"
                }
            }
        } catch (e: Exception) {
            "解析章节内容出错: ${e.message}"
        }
    }
}
