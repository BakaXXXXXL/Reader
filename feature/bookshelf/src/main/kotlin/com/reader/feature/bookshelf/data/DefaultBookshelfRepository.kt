package com.reader.feature.bookshelf.data

import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.feature.bookshelf.model.BookshelfItem
import com.reader.feature.bookshelf.model.ImportCandidate
import com.reader.feature.bookshelf.model.ImportCandidateStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * [BookshelfRepository] 默认实现。
 * 具备线程安全的内部状态流与高效响应机制，支持完全离线操作、
 * 单元测试直接注入，并在持久化层组装就绪后可平滑过渡。
 */
class DefaultBookshelfRepository(
    initialBooks: List<BookshelfItem> = emptyList(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BookshelfRepository {

    private val idCounter = AtomicLong(
        initialBooks.maxOfOrNull { it.id } ?: 0L
    )

    private val _booksFlow = MutableStateFlow(initialBooks)

    override fun getBooks(): Flow<List<BookshelfItem>> = _booksFlow.asStateFlow()

    override suspend fun importBooks(books: List<Book>): List<Long> = withContext(ioDispatcher) {
        val currentList = _booksFlow.value.toMutableList()
        val generatedIds = mutableListOf<Long>()

        books.forEach { book ->
            val bookId = if (book.id <= 0L) idCounter.incrementAndGet() else book.id
            generatedIds.add(bookId)
            val persistedBook = book.copy(id = bookId)
            val item = BookshelfItem(
                book = persistedBook,
                readingProgress = 0.0f,
                isFavorite = false,
                isPinned = false,
                customOrder = currentList.size
            )
            // 避免重复 URI 导入覆盖
            val existingIndex = currentList.indexOfFirst { it.uriString == item.uriString }
            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }
        }

        _booksFlow.value = currentList
        generatedIds
    }

    override suspend fun importCandidate(candidate: ImportCandidate): Result<BookshelfItem> = withContext(ioDispatcher) {
        runCatching {
            val format = candidate.format
            val (author, title) = parseAuthorAndTitle(candidate.fileName)
            val newId = idCounter.incrementAndGet()
            val now = System.currentTimeMillis()

            val book = Book(
                id = newId,
                title = title,
                author = author,
                coverPath = null,
                uriString = candidate.uriString,
                format = format,
                fileSize = candidate.fileSize,
                totalChapters = 0,
                addTime = now,
                lastReadTime = 0L,
                archivePath = null
            )

            val item = BookshelfItem(
                book = book,
                readingProgress = 0.0f,
                isFavorite = false,
                isPinned = false,
                customOrder = _booksFlow.value.size
            )

            val currentList = _booksFlow.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.uriString == item.uriString }
            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(0, item) // 新书置于列表首位
            }
            _booksFlow.value = currentList
            item
        }
    }

    override suspend fun scanDirectory(dirPath: String): List<ImportCandidate> = withContext(ioDispatcher) {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            return@withContext emptyList()
        }

        val existingUris = _booksFlow.value.map { it.uriString }.toSet()
        val results = mutableListOf<ImportCandidate>()

        val files = dir.listFiles() ?: return@withContext emptyList()
        for (file in files) {
            if (file.isFile) {
                val format = BookFormat.fromFileName(file.name)
                if (format != null) {
                    val uri = file.toURI().toString()
                    val alreadyImported = existingUris.contains(uri)
                    results.add(
                        ImportCandidate(
                            uriString = uri,
                            fileName = file.name,
                            fileSize = file.length(),
                            format = format,
                            isSelected = !alreadyImported,
                            status = if (alreadyImported) {
                                ImportCandidateStatus.SKIPPED_ALREADY_EXISTS
                            } else {
                                ImportCandidateStatus.PENDING
                            }
                        )
                    )
                }
            }
        }

        results.sortedBy { it.fileName }
    }

    override suspend fun updateReadingProgress(
        bookId: Long,
        progress: Float,
        chapterTitle: String?
    ) = withContext(ioDispatcher) {
        val currentList = _booksFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == bookId }
        if (index >= 0) {
            val oldItem = currentList[index]
            val updatedBook = oldItem.book.copy(
                lastReadTime = System.currentTimeMillis()
            )
            currentList[index] = oldItem.copy(
                book = updatedBook,
                readingProgress = progress.coerceIn(0.0f, 1.0f),
                lastReadChapterTitle = chapterTitle ?: oldItem.lastReadChapterTitle
            )
            _booksFlow.value = currentList
        }
    }

    override suspend fun setFavorite(bookId: Long, isFavorite: Boolean) = withContext(ioDispatcher) {
        val currentList = _booksFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == bookId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(isFavorite = isFavorite)
            _booksFlow.value = currentList
        }
    }

    override suspend fun setPinned(bookId: Long, isPinned: Boolean) = withContext(ioDispatcher) {
        val currentList = _booksFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == bookId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(isPinned = isPinned)
            _booksFlow.value = currentList
        }
    }

    override suspend fun deleteBook(bookId: Long) = withContext(ioDispatcher) {
        val currentList = _booksFlow.value.toMutableList()
        currentList.removeAll { it.id == bookId }
        _booksFlow.value = currentList
    }

    override suspend fun deleteBooks(bookIds: List<Long>) = withContext(ioDispatcher) {
        val idSet = bookIds.toSet()
        val currentList = _booksFlow.value.toMutableList()
        currentList.removeAll { idSet.contains(it.id) }
        _booksFlow.value = currentList
    }

    override suspend fun updateCustomOrder(bookOrders: List<Pair<Long, Int>>) = withContext(ioDispatcher) {
        val orderMap = bookOrders.toMap()
        val currentList = _booksFlow.value.map { item ->
            val newOrder = orderMap[item.id]
            if (newOrder != null) item.copy(customOrder = newOrder) else item
        }
        _booksFlow.value = currentList
    }

    private fun parseAuthorAndTitle(fileName: String): Pair<String, String> {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        return when {
            nameWithoutExt.contains(" - ") -> {
                val parts = nameWithoutExt.split(" - ", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            nameWithoutExt.contains("——") -> {
                val parts = nameWithoutExt.split("——", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            nameWithoutExt.contains("_") -> {
                val parts = nameWithoutExt.split("_", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            else -> {
                Pair("未知作者", nameWithoutExt.trim())
            }
        }
    }

    companion object {
        /**
         * 预置演示与测试样本书籍集合。
         */
        fun createSampleData(): List<BookshelfItem> {
            val now = System.currentTimeMillis()
            return listOf(
                BookshelfItem(
                    book = Book(
                        id = 1L,
                        title = "三国演义",
                        author = "罗贯中",
                        coverPath = null,
                        uriString = "file:///storage/emulated/0/Books/三国演义.txt",
                        format = BookFormat.TXT,
                        fileSize = 1024 * 1024 * 2L,
                        totalChapters = 120,
                        addTime = now - 86400000L * 7,
                        lastReadTime = now - 3600000L * 2
                    ),
                    readingProgress = 0.35f,
                    isFavorite = true,
                    isPinned = true,
                    customOrder = 0,
                    lastReadChapterTitle = "第四十二回 张翼德大闹长坂桥"
                ),
                BookshelfItem(
                    book = Book(
                        id = 2L,
                        title = "红楼梦",
                        author = "曹雪芹",
                        coverPath = null,
                        uriString = "file:///storage/emulated/0/Books/红楼梦.epub",
                        format = BookFormat.EPUB,
                        fileSize = 1024 * 1024 * 3L,
                        totalChapters = 120,
                        addTime = now - 86400000L * 5,
                        lastReadTime = now - 3600000L * 10
                    ),
                    readingProgress = 0.12f,
                    isFavorite = true,
                    isPinned = false,
                    customOrder = 1,
                    lastReadChapterTitle = "第五回 贾宝玉神游太虚境"
                ),
                BookshelfItem(
                    book = Book(
                        id = 3L,
                        title = "西游记",
                        author = "吴承恩",
                        coverPath = null,
                        uriString = "file:///storage/emulated/0/Books/西游记.mobi",
                        format = BookFormat.MOBI,
                        fileSize = 1024 * 1024 * 2L,
                        totalChapters = 100,
                        addTime = now - 86400000L * 3,
                        lastReadTime = now - 86400000L
                    ),
                    readingProgress = 1.0f,
                    isFavorite = false,
                    isPinned = false,
                    customOrder = 2,
                    lastReadChapterTitle = "第一百回 径回东土 五圣成真"
                ),
                BookshelfItem(
                    book = Book(
                        id = 4L,
                        title = "水浒传",
                        author = "施耐庵",
                        coverPath = null,
                        uriString = "file:///storage/emulated/0/Books/水浒传.txt",
                        format = BookFormat.TXT,
                        fileSize = 1024 * 1024 * 2L,
                        totalChapters = 120,
                        addTime = now - 86400000L * 2,
                        lastReadTime = 0L
                    ),
                    readingProgress = 0.0f,
                    isFavorite = false,
                    isPinned = false,
                    customOrder = 3,
                    lastReadChapterTitle = null
                )
            )
        }
    }
}
