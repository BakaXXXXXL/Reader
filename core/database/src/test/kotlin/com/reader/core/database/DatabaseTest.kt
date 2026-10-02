package com.reader.core.database

import com.reader.core.database.converter.Converters
import com.reader.core.database.mapper.asDomain
import com.reader.core.database.mapper.asEntity
import com.reader.core.model.Book
import com.reader.core.model.BookFormat
import com.reader.core.model.Chapter
import com.reader.core.model.PageTurnAnimation
import com.reader.core.model.ReadLocator
import com.reader.core.model.ReaderThemePreset
import com.reader.core.model.ReadingLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DatabaseTest {

    private val converters = Converters()

    @Test
    fun testConverters() {
        assertEquals("EPUB", converters.fromBookFormat(BookFormat.EPUB))
        assertEquals(BookFormat.EPUB, converters.toBookFormat("EPUB"))
        assertEquals(BookFormat.TXT, converters.toBookFormat("txt"))
        assertNull(converters.toBookFormat(null))

        assertEquals("COVER", converters.fromPageTurnAnimation(PageTurnAnimation.COVER))
        assertEquals(PageTurnAnimation.COVER, converters.toPageTurnAnimation("COVER"))
        assertEquals(PageTurnAnimation.SIMULATION, converters.toPageTurnAnimation("simulation"))

        assertEquals("DEFAULT_LIGHT", converters.fromReaderThemePreset(ReaderThemePreset.DEFAULT_LIGHT))
        assertEquals(ReaderThemePreset.DEFAULT_LIGHT, converters.toReaderThemePreset("DEFAULT_LIGHT"))
        assertEquals(ReaderThemePreset.PARCHMENT, converters.toReaderThemePreset("parchment"))
    }

    @Test
    fun testBookEntityMappingRoundTrip() {
        val domain = Book(
            id = 123L,
            title = "西游记",
            author = "吴承恩",
            coverPath = "/covers/xiyouji.jpg",
            uriString = "content://reader/123",
            format = BookFormat.EPUB,
            fileSize = 4096000L,
            totalChapters = 100,
            addTime = 1000L,
            lastReadTime = 2000L,
            archivePath = "/cache/xiyouji"
        )

        val entity = domain.asEntity()
        assertEquals(123L, entity.id)
        assertEquals("西游记", entity.title)
        assertEquals(BookFormat.EPUB, entity.format)

        val roundTrip = entity.asDomain()
        assertEquals(domain, roundTrip)
    }

    @Test
    fun testChapterEntityMappingRoundTrip() {
        val domain = Chapter(
            id = 1L,
            bookId = 10L,
            index = 0,
            title = "第一回 灵根育孕源流出 心性修持大道生",
            startOffset = 0L,
            endOffset = 5000L,
            contentPath = "OEBPS/ch1.xhtml"
        )

        val entity = domain.asEntity()
        assertEquals(10L, entity.bookId)
        assertEquals(0, entity.index)

        val roundTrip = entity.asDomain()
        assertEquals(domain, roundTrip)
    }

    @Test
    fun testReadLocatorMappingRoundTrip() {
        val domain = ReadLocator(
            bookId = 10L,
            chapterIndex = 1,
            chapterTitle = "第二回",
            charOffset = 300,
            progression = 0.45f,
            pageIndexInChapter = 2,
            totalPagesInChapter = 5,
            updateTime = 3000L
        )

        val entity = domain.asEntity()
        assertEquals(10L, entity.bookId)
        assertEquals(300, entity.charOffset)

        val roundTrip = entity.asDomain()
        assertEquals(domain, roundTrip)
    }

    @Test
    fun testReadingLogMappingRoundTrip() {
        val domain = ReadingLog(
            id = 5L,
            bookId = 10L,
            readDate = "2026-10-03",
            durationSeconds = 1800L,
            charactersRead = 5200,
            timestamp = 4000L
        )

        val entity = domain.asEntity()
        val roundTrip = entity.asDomain()
        assertEquals(domain, roundTrip)
    }
}
