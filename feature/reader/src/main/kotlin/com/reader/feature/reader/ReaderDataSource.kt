package com.reader.feature.reader

import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.core.model.Bookmark
import com.reader.core.model.Chapter
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReaderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 阅读器数据源契约抽象接口。
 *
 * 遵循 Clean Architecture 规范，隔离数据持久化实现（Room / DataStore）与表现层 ViewModel。
 */
interface ReaderDataSource {
    suspend fun getBook(bookId: Long): Book?
    suspend fun getChapters(bookId: Long): List<Chapter>
    suspend fun getLocator(bookId: Long): ReadLocator?
    suspend fun saveLocator(locator: ReadLocator)
    suspend fun getBookmarks(bookId: Long): List<Bookmark>
    suspend fun saveBookmark(bookmark: Bookmark): Bookmark
    suspend fun deleteBookmark(bookmarkId: Long)
    suspend fun getReaderConfig(): ReaderConfig
    suspend fun saveReaderConfig(config: ReaderConfig)
    suspend fun getChapterContent(bookId: Long, chapterIndex: Int): String
}

/**
 * 内存化完备数据源实现。
 *
 * 既可在未绑定外置数据库时独立提供可交互预览与单测能力，
 * 亦可作为轻量级本地快速状态缓存。内部内置合规的经典示例章节，杜绝空指针与白屏。
 */
class InMemoryReaderDataSource(
    initialBooks: Map<Long, Book> = emptyMap(),
    initialChapters: Map<Long, List<Chapter>> = emptyMap(),
    initialLocators: Map<Long, ReadLocator> = emptyMap(),
    initialBookmarks: Map<Long, List<Bookmark>> = emptyMap(),
    initialConfig: ReaderConfig = ReaderConfig.DEFAULT
) : ReaderDataSource {

    private val books = initialBooks.toMutableMap()
    private val chaptersMap = initialChapters.toMutableMap()
    private val locators = initialLocators.toMutableMap()
    private val bookmarksMap = initialBookmarks.toMutableMap()
    private val configFlow = MutableStateFlow(initialConfig)

    init {
        // 如果未注入书籍，预置一本标准的合规离线示例书籍以便完整交互
        if (books.isEmpty()) {
            val defaultBook = Book(
                id = 1L,
                title = "离线典藏名著精选",
                author = "经典文学集萃",
                uriString = "file:///sample/classic.txt",
                format = BookFormat.TXT,
                fileSize = 1024 * 1024L,
                totalChapters = 12
            )
            books[1L] = defaultBook

            val sampleChapters = listOf(
                Chapter(id = 1L, bookId = 1L, index = 0, title = "第一章 序幕：风起青萍之末", startOffset = 0L, endOffset = 3000L),
                Chapter(id = 2L, bookId = 1L, index = 1, title = "第二章 寻道：山重水复疑无路", startOffset = 3000L, endOffset = 6500L),
                Chapter(id = 3L, bookId = 1L, index = 2, title = "第三章 悟真：柳暗花明又一村", startOffset = 6500L, endOffset = 10000L),
                Chapter(id = 4L, bookId = 1L, index = 3, title = "第四章 淬炼：大浪淘沙始见金", startOffset = 10000L, endOffset = 14500L),
                Chapter(id = 5L, bookId = 1L, index = 4, title = "第五章 沉潜：静水流深潜龙在渊", startOffset = 14500L, endOffset = 19000L),
                Chapter(id = 6L, bookId = 1L, index = 5, title = "第六章 破晓：东方欲晓莫道君行早", startOffset = 19000L, endOffset = 23000L),
                Chapter(id = 7L, bookId = 1L, index = 6, title = "第七章 纵横：踏遍青山人未老", startOffset = 23000L, endOffset = 28000L),
                Chapter(id = 8L, bookId = 1L, index = 7, title = "第八章 归真：万流归海返璞归真", startOffset = 28000L, endOffset = 33000L)
            )
            chaptersMap[1L] = sampleChapters

            locators[1L] = ReadLocator.initial(1L, sampleChapters.first().title)

            bookmarksMap[1L] = listOf(
                Bookmark(
                    id = 101L,
                    bookId = 1L,
                    chapterIndex = 0,
                    chapterTitle = "第一章 序幕：风起青萍之末",
                    charOffset = 120,
                    previewText = "风起于青萍之末，浪成于微澜之间。天地之大，浩浩汤汤，往者不可谏，来者犹可追。",
                    createTime = System.currentTimeMillis() - 3600_000L
                )
            )
        }
    }

    override suspend fun getBook(bookId: Long): Book? {
        return books[bookId] ?: books.values.firstOrNull()
    }

    override suspend fun getChapters(bookId: Long): List<Chapter> {
        return chaptersMap[bookId] ?: chaptersMap.values.firstOrNull() ?: emptyList()
    }

    override suspend fun getLocator(bookId: Long): ReadLocator? {
        return locators[bookId]
    }

    override suspend fun saveLocator(locator: ReadLocator) {
        locators[locator.bookId] = locator
    }

    override suspend fun getBookmarks(bookId: Long): List<Bookmark> {
        return bookmarksMap[bookId] ?: emptyList()
    }

    override suspend fun saveBookmark(bookmark: Bookmark): Bookmark {
        val list = bookmarksMap[bookmark.bookId]?.toMutableList() ?: mutableListOf()
        val assignedId = if (bookmark.id == 0L) (System.currentTimeMillis() + (0..999).random()) else bookmark.id
        val newBookmark = bookmark.copy(id = assignedId)
        list.add(0, newBookmark)
        bookmarksMap[bookmark.bookId] = list
        return newBookmark
    }

    override suspend fun deleteBookmark(bookmarkId: Long) {
        for ((bookId, list) in bookmarksMap) {
            val filtered = list.filterNot { it.id == bookmarkId }
            if (filtered.size != list.size) {
                bookmarksMap[bookId] = filtered
                break
            }
        }
    }

    override suspend fun getReaderConfig(): ReaderConfig {
        return configFlow.value
    }

    override suspend fun saveReaderConfig(config: ReaderConfig) {
        configFlow.update { config }
    }

    override suspend fun getChapterContent(bookId: Long, chapterIndex: Int): String {
        return """
            　　古人云：天地之道，博也，厚也，高也，明也，悠也，久也。
            　　大风泱泱，大潮滂滂。洪水图腾蛟龙，烈火涅槃凤凰。文明圣火，千古未绝者，唯我无双；和天地并存，与日月同光。
            　　凡作传者，必以立德、立功、立言为本，不可虚誉，不可妄谤。心之所向，素履以往，生如逆旅，一苇以航。
            　　静水流深，沧笙踏歌；三生阴晴圆缺，一朝悲欢离合。韶华倾负，怎奈何，世事漫随流水，算来一梦浮生。
            　　知行合一，致良知。古之立大事者，不惟有超世之才，亦必有坚忍不拔之志。披荆斩棘，行稳致远。
        """.trimIndent()
    }
}
