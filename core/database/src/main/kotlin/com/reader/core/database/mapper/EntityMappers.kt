package com.reader.core.database.mapper

import com.reader.core.database.entity.BookEntity
import com.reader.core.database.entity.BookmarkEntity
import com.reader.core.database.entity.ChapterEntity
import com.reader.core.database.entity.ReadLocatorEntity
import com.reader.core.database.entity.ReadingLogEntity
import com.reader.core.model.Book
import com.reader.core.model.Bookmark
import com.reader.core.model.Chapter
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReadingLog

/**
 * 数据库实体与核心领域模型之间的无损转换映射扩展。
 */

fun BookEntity.asDomain(): Book = Book(
    id = id,
    title = title,
    author = author,
    coverPath = coverPath,
    uriString = uriString,
    format = format,
    fileSize = fileSize,
    totalChapters = totalChapters,
    addTime = addTime,
    lastReadTime = lastReadTime,
    archivePath = archivePath
)

fun Book.asEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    coverPath = coverPath,
    uriString = uriString,
    format = format,
    fileSize = fileSize,
    totalChapters = totalChapters,
    addTime = addTime,
    lastReadTime = lastReadTime,
    archivePath = archivePath
)

fun ChapterEntity.asDomain(): Chapter = Chapter(
    id = id,
    bookId = bookId,
    index = index,
    title = title,
    startOffset = startOffset,
    endOffset = endOffset,
    contentPath = contentPath
)

fun Chapter.asEntity(): ChapterEntity = ChapterEntity(
    id = id,
    bookId = bookId,
    index = index,
    title = title,
    startOffset = startOffset,
    endOffset = endOffset,
    contentPath = contentPath
)

fun ReadLocatorEntity.asDomain(): ReadLocator = ReadLocator(
    bookId = bookId,
    chapterIndex = chapterIndex,
    chapterTitle = chapterTitle,
    charOffset = charOffset,
    progression = progression,
    pageIndexInChapter = pageIndexInChapter,
    totalPagesInChapter = totalPagesInChapter,
    updateTime = updateTime
)

fun ReadLocator.asEntity(): ReadLocatorEntity = ReadLocatorEntity(
    bookId = bookId,
    chapterIndex = chapterIndex,
    chapterTitle = chapterTitle,
    charOffset = charOffset,
    progression = progression,
    pageIndexInChapter = pageIndexInChapter,
    totalPagesInChapter = totalPagesInChapter,
    updateTime = updateTime
)

fun ReadingLogEntity.asDomain(): ReadingLog = ReadingLog(
    id = id,
    bookId = bookId,
    readDate = readDate,
    durationSeconds = durationSeconds,
    charactersRead = charactersRead,
    timestamp = timestamp
)

fun ReadingLog.asEntity(): ReadingLogEntity = ReadingLogEntity(
    id = id,
    bookId = bookId,
    readDate = readDate,
    durationSeconds = durationSeconds,
    charactersRead = charactersRead,
    timestamp = timestamp
)

fun BookmarkEntity.asDomain(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    chapterTitle = chapterTitle,
    charOffset = charOffset,
    previewText = previewText,
    createTime = createTime
)

fun Bookmark.asEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    chapterTitle = chapterTitle,
    charOffset = charOffset,
    previewText = previewText,
    createTime = createTime
)
