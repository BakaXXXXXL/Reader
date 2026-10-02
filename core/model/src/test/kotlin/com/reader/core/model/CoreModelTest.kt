package com.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreModelTest {

    @Test
    fun testBookFormatResolution() {
        assertEquals(BookFormat.TXT, BookFormat.fromExtension("txt"))
        assertEquals(BookFormat.TXT, BookFormat.fromExtension(".TXT"))
        assertEquals(BookFormat.EPUB, BookFormat.fromFileName("novel.epub"))
        assertEquals(BookFormat.MOBI, BookFormat.fromFileName("classic.mobi"))
        assertEquals(BookFormat.AZW3, BookFormat.fromFileName("book.azw3"))
        assertEquals(BookFormat.PDF, BookFormat.fromFileName("document.pdf"))
        assertNull(BookFormat.fromFileName("unknown.docx"))

        assertEquals(BookFormat.EPUB, BookFormat.fromMimeType("application/epub+zip"))
    }

    @Test
    fun testReadLocatorValidationAndProgression() {
        val locator = ReadLocator(
            bookId = 1L,
            chapterIndex = 2,
            chapterTitle = "第一回 宴桃园豪杰三结义",
            charOffset = 150,
            progression = 0.25f,
            pageIndexInChapter = 1,
            totalPagesInChapter = 4
        )

        assertEquals(25.0f, locator.progressionPercentage, 0.01f)
        assertEquals(1L, locator.bookId)
        assertEquals(2, locator.chapterIndex)
    }

    @Test
    fun testReaderConfigDefaultsAndConstraints() {
        val config = ReaderConfig.DEFAULT

        // Out-of-box default page turn animation must be COVER per spec!
        assertEquals(PageTurnAnimation.COVER, config.pageTurnAnimation)
        assertEquals(18f, config.fontSizeSp, 0.01f)
        assertEquals(1.6f, config.lineHeightMultiplier, 0.01f)
        assertEquals(2, config.firstLineIndentSpaces)
        assertEquals(ReaderThemePreset.DEFAULT_LIGHT, config.themePreset)
        assertTrue(config.keepScreenOn)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testReaderConfigInvalidFontSize() {
        ReaderConfig(fontSizeSp = 5f)
    }

    @Test
    fun testPageTurnAnimationResolution() {
        assertEquals(PageTurnAnimation.COVER, PageTurnAnimation.fromId("cover"))
        assertEquals(PageTurnAnimation.SIMULATION, PageTurnAnimation.fromId("simulation"))
        assertEquals(PageTurnAnimation.SLIDE, PageTurnAnimation.fromId("slide"))
        assertEquals(PageTurnAnimation.CONTINUOUS_SCROLL, PageTurnAnimation.fromId("scroll"))
        assertEquals(PageTurnAnimation.NONE, PageTurnAnimation.fromId("none"))
        // Fallback to default
        assertEquals(PageTurnAnimation.COVER, PageTurnAnimation.fromId("invalid_id"))
    }

    @Test
    fun testReaderThemePresets() {
        val presets = ReaderThemePreset.entries
        assertEquals(5, presets.size)

        val light = ReaderThemePreset.DEFAULT_LIGHT
        assertFalse(light.isDark)
        assertEquals("日间纸白", light.displayName)

        val dark = ReaderThemePreset.DARK_NIGHT
        assertTrue(dark.isDark)
        assertEquals("纯黑夜间", dark.displayName)

        assertEquals(ReaderThemePreset.PARCHMENT, ReaderThemePreset.fromId("parchment"))
        assertEquals(ReaderThemePreset.GREEN_TEA, ReaderThemePreset.fromId("green_tea"))
        assertEquals(ReaderThemePreset.E_INK, ReaderThemePreset.fromId("e_ink"))
    }

    @Test
    fun testBookEntityHelpers() {
        val book = Book(
            id = 10L,
            title = "三体",
            author = "刘慈欣",
            uriString = "content://books/10",
            format = BookFormat.EPUB,
            fileSize = 1024L * 1024L
        )

        assertTrue(book.isEpub)
        assertFalse(book.isTxt)
        assertEquals(10L, book.id)
    }
}
